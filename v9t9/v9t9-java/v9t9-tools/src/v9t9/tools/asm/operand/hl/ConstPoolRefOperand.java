/*
  ConstPoolRefOperand.java

  (c) 2008-2013 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.tools.asm.operand.hl;

import v9t9.common.asm.ICpuInstruction;
import v9t9.common.asm.IInstruction;
import v9t9.common.asm.ResolveException;
import v9t9.tools.asm.IAssembler;
import v9t9.tools.asm.operand.ll.LLForwardOperand;
import v9t9.tools.asm.operand.ll.LLImmedOperand;
import v9t9.tools.asm.operand.ll.LLOperand;

/**
 * A request for a const
 * @author Ed
 *
 */
public class ConstPoolRefOperand extends ImmediateOperand {

	public ConstPoolRefOperand(AssemblerOperand op) {
		super(op);
	}

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.ImmediateOperand#toString()
	 */
	@Override
	public String toString() {
		return "#" + super.toString();
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.AssemblerOperand#isMemory()
	 */
	@Override
	public boolean isMemory() {
		return true;
	}
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.AssemblerOperand#isRegister()
	 */
	@Override
	public boolean isRegister() {
		return false;
	}
	public LLOperand resolve(IAssembler assembler, IInstruction inst) throws ResolveException {
		LLOperand op = immed.resolve(assembler, inst);
		if (op instanceof LLForwardOperand)
			return new LLForwardOperand(this, 2);
		if (!(op instanceof LLImmedOperand)) {
			throw new ResolveException(op, "Expected an immediate");
		}
		
		int value = op.getImmediate();
		AssemblerOperand addr;
		boolean isByte = inst instanceof ICpuInstruction &&
			assembler.getInstructionFactory().isByteInst(((ICpuInstruction) inst).getInst());
		if (isByte) {
			addr = assembler.getConstPool().allocateByte(value);
		} else {
			addr = assembler.getConstPool().allocateWord(value);
		}
		
		LLOperand resOp = addr.resolve(assembler, inst);
		resOp.setOriginal(this);
		return resOp;
	}

	/**
	 * @return
	 */
	public Integer getValue() {
		return ((NumberOperand) immed).getValue();
	}
	

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.BaseOperand#replaceOperand(v9t9.tools.asm.operand.hl.AssemblerOperand, v9t9.tools.asm.operand.hl.AssemblerOperand)
	 */
	@Override
	public AssemblerOperand replaceOperand(AssemblerOperand src,
			AssemblerOperand dst) {
		if (src.equals(this))
			return dst;
		AssemblerOperand newVal = immed.replaceOperand(src, dst);
		if (newVal != immed) {
			return new ConstPoolRefOperand(newVal);
		}
		return this;
	}
	
}
