/**
 * 
 */
package v9t9.emulator.hardware;

import java.util.Collections;
import java.util.List;

import v9t9.emulator.clients.builtin.SoundProvider;
import v9t9.emulator.clients.builtin.swt.IDeviceIndicatorProvider;
import v9t9.emulator.clients.builtin.video.v9938.VdpV9938;
import v9t9.emulator.common.Machine;
import v9t9.emulator.hardware.dsrs.DsrSettings;
import v9t9.emulator.hardware.memory.MFP201MemoryModel;
import v9t9.emulator.hardware.memory.mmio.Vdp9938Mmio;
import v9t9.emulator.hardware.sound.MultiSoundTMS9919B;
import v9t9.emulator.runtime.compiler.NullCompilerStrategy;
import v9t9.emulator.runtime.cpu.Cpu;
import v9t9.emulator.runtime.cpu.CpuMFP201;
import v9t9.emulator.runtime.cpu.CpuMetrics;
import v9t9.emulator.runtime.cpu.DumpFullReporterMFP201;
import v9t9.emulator.runtime.cpu.DumpReporterMFP201;
import v9t9.emulator.runtime.cpu.Executor;
import v9t9.emulator.runtime.interpreter.InterpreterMFP201;
import v9t9.engine.VdpHandler;
import v9t9.engine.memory.BankedMemoryEntry;
import v9t9.engine.memory.MemoryModel;
import v9t9.engine.memory.WindowBankedMemoryEntry;
import v9t9.tools.asm.assembler.IInstructionFactory;
import v9t9.tools.asm.assembler.InstructionFactoryMFP201;

/**
 * This is the MFP201 machine model.
 * @author ejs
 *
 */
public class MFP201MachineModel implements MachineModel {

	public static final String ID = "MFP201";
	private MFP201MemoryModel memoryModel;
	private Vdp9938Mmio vdpMmio;
	private BankedMemoryEntry cpuBankedVideo;
	private VdpV9938 vdp;
	//protected MemoryEntry currentMemory;
	
	public MFP201MachineModel() {
		memoryModel = new MFP201MemoryModel();
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.MachineModel#getIdentifier()
	 */
	@Override
	public String getIdentifier() {
		return ID;
	}
	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.MachineModel#createMachine()
	 */
	@Override
	public Machine createMachine() {
		return new MFP201Machine(this);
	}
	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.MachineModel#createCPU(v9t9.emulator.common.Machine)
	 */
	@Override
	public Cpu createCPU(Machine machine) {
		return new CpuMFP201(machine, 1000 / machine.getCpuTicksPerSec(), machine.getVdp());
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.MachineModel#getMemoryModel()
	 */
	public MemoryModel getMemoryModel() {
		return memoryModel;
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.MachineModel#getVdp()
	 */
	public VdpHandler createVdp(Machine machine) {
		vdp = new VdpV9938(machine);
		vdpMmio = new Vdp9938Mmio(machine.getMemory(), vdp, 0x20000);
		return vdp;
	}

	public SoundProvider createSoundProvider(Machine machine) {
		return new MultiSoundTMS9919B(machine);
	}
	
	public void defineDevices(final Machine machine) {
		//machine.getCpu().setCruAccess(new InternalCru9901(machine, machine.getKeyboardState()));
		
		//EmuDiskDsr dsr = new EmuDiskDsr(DiskDirectoryMapper.INSTANCE);
		//machine.getDsrManager().registerDsr(dsr);
		
		defineCpuVdpBanks(machine);
	}
	

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.MachineModel#getDsrSettings()
	 */
	@Override
	public List<DsrSettings> getDsrSettings(Machine machine) {
		return Collections.emptyList();
	}
	@Override
	public List<IDeviceIndicatorProvider> getDeviceIndicatorProviders(Machine machine) {
		return Collections.emptyList();
	}
	private void defineCpuVdpBanks(final Machine machine) {
		
		cpuBankedVideo = new WindowBankedMemoryEntry(machine.getMemory(),
				"CPU VDP Bank", 
				machine.getConsole(),
				0xA000,
				0x4000,
				vdpMmio.getMemoryArea()) {
			@Override
			public void writeByte(int addr, byte val) {
				super.writeByte(addr, val);
				vdp.touchAbsoluteVdpMemory((addr & 0x3fff) + getBankOffset(), val);
			}
			@Override
			public void writeWord(int addr, short val) {
				super.writeWord(addr, val);
				vdp.touchAbsoluteVdpMemory((addr & 0x3fff) + getBankOffset(), (byte) (val >> 8));
			}
		};

		cpuBankedVideo.domain.mapEntry(cpuBankedVideo);
		
		/*
		machine.getCruManager().add(0x1402, 1, new CruWriter() {

			public int write(int addr, int data, int num) {
				if (data == 1) {
					vdpCpuBanked = true;
					//currentMemory = machine.getMemory().map.lookupEntry(machine.getConsole(), 0xc000);
					cpuBankedVideo.domain.mapEntry(cpuBankedVideo);
				} else {
					vdpCpuBanked = false;
					cpuBankedVideo.domain.unmapEntry(cpuBankedVideo);
					//if (currentMemory != null)
					//	currentMemory.map();
				}
				return 0;
			}
			
		});
		
		CruWriter bankSelector = new CruWriter() {

			public int write(int addr, int data, int num) {
				// independent banking from VDP
				if (vdpCpuBanked) {
					int currentBank = cpuBankedVideo.getCurrentBank();
					int bit = (addr - 0x1404) >> 1;
					currentBank = (currentBank & ~(1 << bit)) | (data << bit);
					cpuBankedVideo.selectBank(currentBank);
				}
				return 0;
			}
			
		};
		machine.getCruManager().add(0x1404, 1, bankSelector);
		machine.getCruManager().add(0x1406, 1, bankSelector);
		machine.getCruManager().add(0x1408, 1, bankSelector);
		*/
	}

	@Override
	public Executor createExecutor(Cpu cpu, CpuMetrics metrics) {
		return new Executor(cpu, metrics, 
				new InterpreterMFP201(cpu.getMachine()),
				new NullCompilerStrategy(),
				new DumpFullReporterMFP201((CpuMFP201) cpu),
				new DumpReporterMFP201((CpuMFP201) cpu));
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.MachineModel#getInstructionFactory()
	 */
	@Override
	public IInstructionFactory getInstructionFactory() {
		return InstructionFactoryMFP201.INSTANCE;
	}
}