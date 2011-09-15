/**
 * 
 */
package org.ejs.eulang.llvm.tms9900.asm;

import org.ejs.eulang.types.LLType;

import v9t9.tools.asm.assembler.operand.hl.AssemblerOperand;

/**
 * Represents zero-initialized memory of the given type.  This is usually
 * intended for structured types (otherwise use NumberOperand(0)).
 * @author ejs
 *
 */
public class ZeroInitOperand extends BaseHLOperand {

	private final LLType type;

	/**
	 * @param type
	 */
	public ZeroInitOperand(LLType type) {
		super();
		this.type = type;
	}

	// note: no equals or hashcode -- all zeros are the same
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "0";
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.operand.hl.AssemblerOperand#getChildren()
	 */
	@Override
	public AssemblerOperand[] getChildren() {
		return new AssemblerOperand[0];
	}

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.operand.hl.AssemblerOperand#isMemory()
	 */
	@Override
	public boolean isMemory() {
		return false;
	}

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.operand.hl.AssemblerOperand#isRegister()
	 */
	@Override
	public boolean isRegister() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.llvm.tms9900.asm.AsmOperand#isConst()
	 */
	@Override
	public boolean isConst() {
		return true;
	}

	/**
	 * @return
	 */
	public LLType getType() {
		return type;
	}
}