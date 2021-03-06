/*
  AddrOperand.java

  (c) 2008-2013 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.tools.asm.operand.hl;

import v9t9.common.asm.IInstruction;
import v9t9.common.asm.ResolveException;
import v9t9.tools.asm.IAssembler;
import v9t9.tools.asm.operand.ll.LLAddrOperand;
import v9t9.tools.asm.operand.ll.LLForwardOperand;
import v9t9.tools.asm.operand.ll.LLImmedOperand;
import v9t9.tools.asm.operand.ll.LLOperand;
import v9t9.tools.asm.operand.ll.LLPCRelativeOperand;

/**
 * @author ejs
 *
 */
public class AddrOperand extends BaseOperand {

	private final AssemblerOperand addr;

	public AddrOperand(AssemblerOperand addr) {
		this.addr = addr;
	}

	@Override
	public String toString() {
		return "@" + getAddr();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getAddr() == null) ? 0 : getAddr().hashCode());
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
		AddrOperand other = (AddrOperand) obj;
		if (getAddr() == null) {
			if (other.getAddr() != null) {
				return false;
			}
		} else if (!getAddr().equals(other.getAddr())) {
			return false;
		}
		return true;
	}
	
	@Override
	public boolean isMemory() {
		return true;
	}
	@Override
	public boolean isRegister() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.AssemblerOperand#isConst()
	 */
	@Override
	public boolean isConst() {
		return false;
	}
	

	public LLOperand resolve(IAssembler assembler, IInstruction inst)
			throws ResolveException {
		LLOperand lop = getAddr().resolve(assembler, inst);
		if (lop instanceof LLForwardOperand)
			return new LLForwardOperand(this, 2);
		
		if (lop instanceof LLPCRelativeOperand) {
			// XXX
			//lop = new LLAddrOperand(this, inst.getPc() + ((LLPCRelativeOperand)lop).getOffset());
			return lop;
		} else if (lop instanceof LLImmedOperand) {
			lop = new LLAddrOperand(this, lop.getImmediate());
		} else
			throw new ResolveException(lop, "Expected an immediate");
		return lop;
	}
	
	public AssemblerOperand getAddr() {
		return addr;
	}

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.BaseOperand#replaceOperand(v9t9.tools.asm.operand.hl.AssemblerOperand, v9t9.tools.asm.operand.hl.AssemblerOperand)
	 */
	@Override
	public AssemblerOperand replaceOperand(AssemblerOperand src,
			AssemblerOperand dst) {
		if (src.equals(this))
			return dst;
		AssemblerOperand newAddr = addr.replaceOperand(src, dst);
		if (newAddr != addr) {
			return new AddrOperand(newAddr);
		}
		return this;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.AssemblerOperand#getChildren()
	 */
	@Override
	public AssemblerOperand[] getChildren() {
		return new AssemblerOperand[] { addr };
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.hl.AssemblerOperand#addOffset(int)
	 */
	@Override
	public AssemblerOperand addOffset(int i) {
		if (addr instanceof BinaryOperand && ((BinaryOperand) addr).getRight() instanceof NumberOperand
				&& ((BinaryOperand) addr).getKind() == '+') {
			AssemblerOperand offs = (NumberOperand) ((BinaryOperand) addr).getRight();
			offs = offs.addOffset(i); 
			return new AddrOperand(new BinaryOperand('+',  ((BinaryOperand) addr).getLeft(), offs)); 
		}
		return new AddrOperand(new BinaryOperand('+', addr, new NumberOperand(i)));
	}
}
