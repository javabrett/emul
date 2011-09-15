/**
 * 
 */
package v9t9.emulator.clients.builtin.video;

import v9t9.engine.memory.ByteMemoryAccess;

/**
 * An abstraction for the video memory access
 * @author ejs
 *
 */
public interface VideoMemory {
	ByteMemoryAccess getByteReadMemoryAccess(int offset);
	
	short flatReadByte(int addr);
}