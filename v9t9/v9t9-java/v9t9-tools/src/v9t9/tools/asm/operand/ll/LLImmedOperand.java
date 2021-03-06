/*
  LLImmedOperand.java

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
import v9t9.tools.asm.operand.hl.AssemblerOperand;

/**
 * A low-level immediate
 * @author Ed
 *
 */
public class LLImmedOperand extends LLOperand {

	private int value;
	
	public LLImmedOperand(AssemblerOperand original, int value) {
		super(original);
		setValue(value);
	}

	public LLImmedOperand(int value) {
		this(null, value);
	}
	
	@Override
	public String toString() {
		return ">" + HexUtils.toHex4(value);
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + value;
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
		LLImmedOperand other = (LLImmedOperand) obj;
		if (value != other.value)
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

	
	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.LLOperand#getSize()
	 */
	@Override
	public int getSize() {
		return 2;
	}

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.LLOperand#hasImmediate()
	 */
	@Override
	public boolean hasImmediate() {
		return true;
	}

	@Override
	public int getImmediate() {
		return value;
	}

	@Override
	public IMachineOperand createMachineOperand(IAsmMachineOperandFactory opFactory) throws ResolveException {
		return opFactory.createImmedOperand(this);
	}
	
	@Override
	public boolean isConstant() {
		return getOriginal() == null;
	}
}
