/**
 * 
 */
package v9t9.engine.memory;

import v9t9.emulator.Machine;
import v9t9.emulator.hardware.memory.mmio.GplMmio;
import v9t9.emulator.hardware.memory.mmio.SoundMmio;
import v9t9.emulator.hardware.memory.mmio.SpeechMmio;
import v9t9.emulator.hardware.memory.mmio.VdpMmio;
import v9t9.engine.Client;

/**
 * @author ejs
 *
 */
public class StockMemoryModel implements MemoryModel {

	private Memory memory;
	private MemoryDomain CPU;

	public StockMemoryModel() {
		memory = new Memory(this);
		CPU = new MemoryDomain();
	}
	/* (non-Javadoc)
	 * @see v9t9.engine.memory.MemoryModel#connectClient(v9t9.engine.Client)
	 */
	public void connectClient(Client client) {

	}

	/* (non-Javadoc)
	 * @see v9t9.engine.memory.MemoryModel#getConsole()
	 */
	public MemoryDomain getConsole() {
		return CPU;
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.memory.MemoryModel#getLatency(int)
	 */
	public int getLatency(int addr) {
		return 0;
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.memory.MemoryModel#getMemory()
	 */
	public Memory createMemory() {
		return memory;
	}

	public void initMemory(Machine machine) {
		
	}
	
	public GplMmio getGplMmio() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public SoundMmio getSoundMmio() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public SpeechMmio getSpeechMmio() {
		// TODO Auto-generated method stub
		return null;
	}
	public VdpMmio getVdpMmio() {
		// TODO Auto-generated method stub
		return null;
	}
}
