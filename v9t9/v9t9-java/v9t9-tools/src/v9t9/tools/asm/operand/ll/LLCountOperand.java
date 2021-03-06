/*
  LLCountOperand.java

  (c) 2008-2013 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.tools.asm.operand.ll;


import ejs.base.utils.HexUtils;
import v9t9.common.asm.IMachineOperand;
import v9t9.common.asm.ResolveException;

/**
 * A shift count
 * @author Ed
 *
 */
public class LLCountOperand extends LLNonImmediateOperand {

	int count;
	public LLCountOperand(int value) {
		super(null);
		setCount(value);
	}

	
	@Override
	public String toString() {
		return ">" + HexUtils.toHex4(count);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + count;
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
		LLCountOperand other = (LLCountOperand) obj;
		if (count != other.count)
			return false;
		return true;
	}

	@Override
	public boolean isMemory() {
		return false;
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
		return true;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.ll.LLOperand#createMachineOperand(v9t9.tools.asm.operand.ll.IMachineOperandFactory)
	 */
	@Override
	public IMachineOperand createMachineOperand(IAsmMachineOperandFactory opFactory)
			throws ResolveException {
		return opFactory.createCountOperand(this);
	}

}
