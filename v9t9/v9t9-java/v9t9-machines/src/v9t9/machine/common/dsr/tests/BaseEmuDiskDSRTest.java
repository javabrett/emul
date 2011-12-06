/**
 * 
 */
package v9t9.machine.common.dsr.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

import org.junit.BeforeClass;

import v9t9.base.properties.IProperty;
import v9t9.base.utils.HexUtils;
import v9t9.common.client.ISettingsHandler;
import v9t9.common.cpu.ICpu;
import v9t9.common.dsr.IMemoryTransfer;
import v9t9.common.files.FDR;
import v9t9.common.files.FDRFactory;
import v9t9.common.memory.ByteMemoryAccess;
import v9t9.engine.dsr.DsrException;
import v9t9.engine.dsr.PabStruct;
import v9t9.engine.dsr.emudisk.EmuDiskConsts;
import v9t9.engine.dsr.emudisk.EmuDiskDsrSettings;
import v9t9.engine.dsr.emudisk.EmuDiskPabHandler;
import v9t9.engine.dsr.realdisk.Dumper;
import v9t9.engine.dsr.realdisk.RealDiskDsrSettings;
import v9t9.machine.common.dsr.emudisk.DiskDirectoryMapper;
import v9t9.machine.common.dsr.emudisk.EmuDiskDsr;

/**
 * @author ejs
 *
 */
public class BaseEmuDiskDSRTest {

	protected static DiskDirectoryMapper mymapper = new DiskDirectoryMapper();
	protected static File dsk1Path;
	
	protected static ISettingsHandler settings;
	protected static IProperty diskImageDsrEnabled;
	
	@BeforeClass
	public static void setupSearch() {
		String path = TestEmuDiskDSRDiskLike.class.getName().replaceAll("\\.", "/");
		String cwd = System.getProperty("user.dir");
		File dir = new File(cwd + "/src/" + path);
		dir = new File(dir.getParentFile(), "data");
		assertTrue(dir+"", dir.exists());
		dsk1Path = dir;
		mymapper.setDiskPath("DSK1", dir);
		
		dir = new File(dir.getParentFile(), mymapper.getLocalFileName("EXTRA/LALA"));
		mymapper.setDiskPath("DSK2", dir);

		diskImageDsrEnabled = settings.get(RealDiskDsrSettings.diskImageDsrEnabled);

		emuDiskDsrEnabled = settings.get(EmuDiskDsrSettings.emuDiskDsrEnabled);
		emuDiskDsrEnabled.setBoolean(true);
	}
	
	static class FakeMemory implements IMemoryTransfer {
		byte[] vdp = new byte[0x4000];
		private boolean[] vdpTouched = new boolean[0x4000];
		
		private byte[] param = new byte[0x100];
		
		public byte readParamByte(int offset) {
			return param[offset];
		}

		public short readParamWord(int offset) {
			return (short) (((param[offset] & 0xff) << 8) | (param[offset+1] & 0xff));
		}
		public void writeParamByte(int offset, byte val) {
			param[offset] = val;
		}
		
		public void writeParamWord(int offset, short val) {
			param[offset] = (byte) (val >> 8);
			param[offset + 1] = (byte) (val & 0xff);
		}

		public byte readVdpByte(int vaddr) {
			return vdp[vaddr];
		}

		public short readVdpShort(int vaddr) {
			return (short) ((vdp[vaddr] & 0xff << 8) | (vdp[vaddr + 1] & 0xff));
		}

		public void writeVdpByte(int vaddr, byte byt) {
			vdp[vaddr] = byt;
			vdpTouched[vaddr] = true;
		}

		public void dirtyVdpMemory(int vaddr, int read) {
			Arrays.fill(vdpTouched, vaddr, vaddr + read, true);
		}

		public ByteMemoryAccess getVdpMemory(int vaddr) {
			return new ByteMemoryAccess(vdp, vaddr);
		}

		/**
		 * 
		 */
		public void reset() {
			Arrays.fill(vdpTouched, false);
		}

		public void assertTouched(int addr, int count, int notbeyond) {
			int start = -1;
			int end = -1;
			int fin = addr + count;
			int finboundary = addr + notbeyond;
			while (addr < fin) {
				if (!vdpTouched[addr]) {
					if (start < 0)
						start = addr;
					end = addr;
				} else {
					if (start  >= 0)
						fail("VDP not touched: " + HexUtils.toHex4(start) + "-" + HexUtils.toHex4(end));
				}
				addr++;
			}
			
			start = end = -1;
			while (addr < finboundary) {
				if (vdpTouched[addr]) {
					if (start < 0)
						start = addr;
					end = addr;
				} else {
					if (start  >= 0)
						fail("VDP touched: " + HexUtils.toHex4(start) + "-" + HexUtils.toHex4(end));
				}
				addr++;
			}
		}
		
	}
	
	protected FakeMemory xfer = new FakeMemory();
	{
		xfer.writeParamWord(0x70, (short) 0x3fff);
	}
	protected EmuDiskDsr dsr = new EmuDiskDsr(settings, mymapper);
	private Dumper dumper;
	private static IProperty emuDiskDsrEnabled;
	{
		dumper = new Dumper(settings, RealDiskDsrSettings.diskImageDebug, ICpu.settingDumpFullInstructions); 
		dsr.handleDSR(xfer, (short) EmuDiskConsts.D_INIT);
	}
	
	protected EmuDiskPabHandler runCase(PabStruct pab) throws DsrException {
		EmuDiskPabHandler handler = new EmuDiskPabHandler(
				dumper,
				(short)0x1000, xfer, mymapper, pab, (short) 0x3ff5);
		xfer.reset();
		handler.run();
		return handler;
	}
	
	protected PabStruct createBinaryPab(int opcode, int addr, int count, String path) {
		PabStruct pab = new PabStruct();
		pab.opcode = opcode;
		pab.bufaddr = addr;
		pab.recnum = count;
		pab.path = path;
		return pab;
	}
	
	protected void copyFile(String devNameTo, String devNameFrom) throws Exception {
		File dst = mymapper.getLocalDottedFile(devNameTo);
		File src = mymapper.getLocalDottedFile(devNameFrom);
		
		// put something there
		dst.setWritable(true);
		FileOutputStream os = new FileOutputStream(dst);
		FileInputStream is = new FileInputStream(src);
		int ch;
		while ((ch = is.read()) != -1)
			os.write(ch);
		os.close();
		is.close();
		
		// rewrite FDR
		FDR fdr = FDRFactory.createFDR(src);
		assertNotNull(fdr);
		
		fdr.setFileName(mymapper.getDsrFileName(dst.getName()));
		fdr.writeFDR(dst);
	}
	

}
