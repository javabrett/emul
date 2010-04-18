/**
 * 
 */
package org.ejs.eulang.llvm.ops;

import org.ejs.eulang.types.LLType;

/**
 * An LLVM temporary (%0, %1, etc)
 * @author ejs
 *
 */
public class LLTempOp implements LLOperand {
	private int number;
	private final LLType type;
	public LLTempOp(int number, LLType type) {
		this.number = number;
		this.type = type;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "%" + number;
	}
	
	
	public String getName() {
		return "%" + number;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + number;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LLTempOp other = (LLTempOp) obj;
		if (number != other.number)
			return false;
		return true;
	}
	
	/**
	 * @return the number
	 */
	public int getNumber() {
		return number;
	}
	/**
	 * @return the type
	 */
	public LLType getType() {
		return type;
	}
	
}
