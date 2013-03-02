/*
  ByteMemoryAccess.java

  (c) 2008-2011 Edward Swartz

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.
 
  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  General Public License for more details.
 
  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
 */
package v9t9.common.memory;


/**
 * @author ejs
 *
 */
public class ByteMemoryAccess {
	public final byte[] memory;
	public int offset;

	public ByteMemoryAccess(byte[] memory, int offset) {
		this.memory = memory;
		this.offset = offset;
	}

	public ByteMemoryAccess(ByteMemoryAccess pattern) {
		this.memory = pattern.memory;
		this.offset = pattern.offset;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + memory.hashCode();
		result = prime * result + offset;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ByteMemoryAccess other = (ByteMemoryAccess) obj;
		if (memory != other.memory) {
			return false;
		}
		if (offset != other.offset) {
			return false;
		}
		return true;
	}
	
	
}
