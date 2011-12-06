/**
 * 
 */
package v9t9.engine.memory;

import java.util.Collections;
import java.util.List;

import v9t9.common.events.IEventNotifier;
import v9t9.common.machine.IBaseMachine;
import v9t9.common.memory.IMemory;
import v9t9.common.memory.IMemoryDomain;
import v9t9.common.memory.IMemoryModel;
import v9t9.common.modules.IModule;

/**
 * @author ejs
 *
 */
public class StockMemoryModel implements IMemoryModel {

	private Memory memory;
	private MemoryDomain CPU;

	public StockMemoryModel() {
		memory = new Memory(this);
		CPU = new MemoryDomain("Console");
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.memory.MemoryModel#resetMemory()
	 */
	@Override
	public void resetMemory() {
		
	}
	/* (non-Javadoc)
	 * @see v9t9.engine.memory.MemoryModel#getConsole()
	 */
	public IMemoryDomain getConsole() {
		return CPU;
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.memory.MemoryModel#getLatency(int)
	 */
	/**
	 * @param addr  
	 */
	public int getLatency(int addr) {
		return 0;
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.memory.MemoryModel#getMemory()
	 */
	public IMemory getMemory() {
		return memory;
	}

	public void initMemory(IBaseMachine machine) {
		
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.memory.MemoryModel#loadMemory(v9t9.emulator.clients.builtin.IEventNotifier)
	 */
	@Override
	public void loadMemory(IEventNotifier eventNotifier) {
		
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
	
	/* (non-Javadoc)
	 * @see v9t9.engine.memory.MemoryModel#getModules()
	 */
	public List<IModule> getModules() {
		return Collections.emptyList();
	}
}