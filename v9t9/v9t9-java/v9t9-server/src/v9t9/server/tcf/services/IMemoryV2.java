/**
 * 
 */
package v9t9.server.tcf.services;

import java.util.Map;

import org.eclipse.tm.tcf.protocol.IToken;
import org.eclipse.tm.tcf.protocol.JSON;
import org.eclipse.tm.tcf.services.IMemory;

/**
 * This service extends the standard TCF IMemory service
 * with support for guaranteed memory change tracking.
 *
 * contentChanged events will be generated for the given
 * memory context at the given rate (when changes occur),
 * providing a report of the deltas in memory since the
 * previous event.  
 *
 * These events must be requested during startChangeNotify
 * and stopChangeNotify.  
 * 
 * @author ejs
 *
 */
public interface IMemoryV2 extends IMemory {

	class MemoryChange {
		
		public MemoryChange(Number addr, long size, byte[] data) {
			this.addr = addr;
			this.size = size;
			this.data = data;
		}
		/**
		 * @param map
		 */
		public MemoryChange(Map<String, Object> map) {
			addr = (Number) map.get(PROP_ADDR);
			size = ((Number) map.get(PROP_SIZE)).longValue();
			data = JSON.toByteArray(map.get(PROP_DATA));
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "MemoryChange: " + addr.intValue() + "+" + size + " = " + new String(data);
		}
		public final Number addr;
		public final long size;
		public final byte[] data;
	}
	
	interface MemoryContentChangeListener {
		void contentChanged(String contextId, MemoryChange[] memChanges);
	}
	
	String NAME = "MemoryV2";
	
	String COMMAND_START_CHANGE_NOTIFY = "startChangeNotify";
	String COMMAND_UPDATE_CHANGE_NOTIFY = "updateChangeNotify";
	String COMMAND_STOP_CHANGE_NOTIFY = "stopChangeNotify";

	String EVENT_CONTENT_CHANGED = "contentChanged";
	
	String PROP_ADDR = "addr";
	String PROP_SIZE = "size";
	String PROP_DATA = "data";
	
	/**
	 * 	As a mode to {@link MemoryContext#get(Number, int, byte[], int, int, int, org.eclipse.tm.tcf.services.IMemory.DoneMemory)}
	 * 	or {@link MemoryContext#set(Number, int, byte[], int, int, int, org.eclipse.tm.tcf.services.IMemory.DoneMemory)},
	 * 	specifies that memory be written without side effects (e.g., memory-mapped I/O or event notification).
	 * 
	 *  The default behavior is to read/write memory as a target program would.
	 */
	int MODE_FLAT = 0x4;
	
	interface DoneCommand {
		void done(Exception error);
	}
	
	/**
	 * Start notifying full memory change information for the given 
	 * context ID.  Fires contentChanged events on change each 
	 * "msDelay" milliseconds.
	 * 
	 * @param notifyId ID used to track this notifier
	 * @param contextId ID for a memory domain
	 * @param msDelay delay in ms between events
	 * @param granularity minimum gap in bytes reported changed, to minimize
	 * size of delta information. E.g., if this 2, and bytes 0 and 2 change,
	 * then the memoryChanged event will report that bytes 0-3 changed.
	 * @param addr address to start monitoring  
	 * @param size size in bytes to start monitoring  
	 * @param done
	 * @return
	 */
	IToken startChangeNotify(String notifyId, String contextId,
			int msDelay, int granularity, 
			int addr, int size, 
			DoneCommand done);

	/**
	 * Update the notification delay or gap.
	 * 
	 * @param notifyId previously registered ID
	 * @param msDelay delay in ms between events
	 * @param granularity minimum gap in bytes reported changed, to minimize
	 * size of delta information. E.g., if this 2, and bytes 0 and 2 change,
	 * then the memoryChanged event will report that bytes 0-3 changed.  
	 * @param addr address to start monitoring  
	 * @param size size in bytes to start monitoring  
	 * @param done
	 * @return
	 */
	IToken updateChangeNotify(String notifyId, 
			int msDelay, int granularity,
			int addr, int size,
			DoneCommand done);

	/**
	 * Stop notifying full memory change information for the given
	 * context ID, so no more contentChanged events will be generated.
	 * @param notifyId previously registered ID
	 * @param done
	 * @return
	 */
	IToken stopChangeNotify(String notifyId, DoneCommand done);
	
	void addListener(MemoryContentChangeListener listener);
	void removeListener(MemoryContentChangeListener listener);
}
