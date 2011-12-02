/**
 * 
 */
package v9t9.emulator.hardware.dsrs.pcode;

import v9t9.emulator.hardware.CruWriter;
import v9t9.emulator.hardware.TI99Machine;
import v9t9.emulator.hardware.memory.mmio.GplMmio;
import v9t9.engine.memory.Memory;
import v9t9.engine.memory.MemoryEntry;
import v9t9.engine.memory.MultiBankedMemoryEntry;

/**
 * @author ejs
 *
 */
public class PCodeDsrRomBankedMemoryEntry extends MultiBankedMemoryEntry {

	private GplMmio pcodeGromMmio;

	public PCodeDsrRomBankedMemoryEntry() {
	}
	public PCodeDsrRomBankedMemoryEntry(Memory memory, String name,
			MemoryEntry[] banks) {
		super(memory, name, banks);
		

	}
	
	public void setup(TI99Machine machine, GplMmio pcodeGromMmio) {
		this.pcodeGromMmio = pcodeGromMmio;
		
		// bit 0 (0x1f00) handled as DSR
		machine.getCruManager().removeWriter(0x1f80, 1);
		machine.getCruManager().removeWriter(0x1f86, 1);
		
		machine.getCruManager().add(0x1f80, 1, 
				new CruWriter() {

					@Override
					public int write(int addr, int data, int num) {
						selectBank(data & 1);
						return 0;
					}
			
		});
		
		machine.getCruManager().add(0x1f86, 1, 
				new CruWriter() {

					@Override
					public int write(int addr, int data, int num) {
						// blink light
						System.out.println("*** PCODE blink " + (data == 0 ? "off" : "on"));
						return 0;
					}
			
		});
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.memory.MemoryEntry#readByte(int)
	 */
	@Override
	public byte readByte(int addr) {
		if (addr >= 0x5bfc && addr <= 0x5bff) {
			return pcodeGromMmio.read(addr);
		}
		return super.readByte(addr);
	}

	@Override
	public void writeByte(int addr, byte val) {
		if (addr >= 0x5ffc && addr <= 0x5fff) {
			pcodeGromMmio.write(addr, val);
			return;
		}
		super.writeByte(addr, val);
	}

}