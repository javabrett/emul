/**
 * 
 */
package org.ejs.eulang.llvm.ops;

import org.ejs.eulang.llvm.ILLCodeVisitor;
import org.ejs.eulang.types.LLType;

/**
 * @author ejs
 *
 */
public abstract class BaseLLOperand implements LLOperand {

	protected LLType type;

	public BaseLLOperand(LLType type) {
		this.type = type;
	}
	/* (non-Javadoc)
	 * @see org.ejs.eulang.llvm.ops.LLOperand#getType()
	 */
	@Override
	public LLType getType() {
		return type;
	}
	/* (non-Javadoc)
	 * @see org.ejs.eulang.llvm.ops.LLOperand#setType(org.ejs.eulang.types.LLType)
	 */
	@Override
	public void setType(LLType type) {
		this.type = type;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.llvm.ops.LLOperand#isConstant()
	 */
	@Override
	public boolean isConstant() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.llvm.ops.LLOperand#accept(org.ejs.eulang.llvm.ILLCodeVisitor)
	 */
	@Override
	public void accept(ILLCodeVisitor visitor) {
		try {
			if (!visitor.enterOperand(this)) {
				
			}
			visitor.exitOperand(this);
		} catch (ILLCodeVisitor.Terminate e) {
			
		}
	}
}
