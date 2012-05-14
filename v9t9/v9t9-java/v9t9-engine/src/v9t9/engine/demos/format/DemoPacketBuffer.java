/**
 * 
 */
package v9t9.engine.demos.format;

import java.io.IOException;
import java.io.OutputStream;

import v9t9.engine.demos.format.DemoFormat.BufferType;

class DemoPacketBuffer extends DemoOutBuffer {
	private final BufferType type;

	public DemoPacketBuffer(OutputStream stream, DemoFormat.BufferType type, int size) {
		super(stream, size);
		this.type = type;
	}
	protected void writeHeader() throws IOException {
		stream.write(type.getCode());
		super.writeHeader();
	}
}