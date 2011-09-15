/**
 * 
 */
package org.ejs.eulang.llvm.instrs;

import org.ejs.eulang.llvm.ops.LLOperand;
import org.ejs.eulang.types.LLType;

/**
 * @author ejs
 *
 */
public class LLCastInstr extends LLAssignInstr {

	private LLType toType;
	private final ECast cast;

	/**
	 * @param temp
	 * @param valueType
	 * @param value
	 * @param rEFPTR
	 */
	public LLCastInstr(LLOperand temp, ECast cast, LLType fromType, LLOperand value,
			LLType toType) {
		super(cast.getOp(), temp, fromType, value);
		this.cast = cast;
		this.toType = toType;
	}
	
	/**
	 * @return the cast
	 */
	public ECast getCast() {
		return cast;
	}
	/* (non-Javadoc)
	 * @see org.ejs.eulang.llvm.instrs.LLBaseInstr#appendInstrString(java.lang.StringBuilder)
	 */
	@Override
	protected void appendInstrString(StringBuilder sb) {
		super.appendInstrString(sb);
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.llvm.instrs.LLAssignInstr#appendFinalString(java.lang.StringBuilder)
	 */
	@Override
	protected void appendFinalString(StringBuilder sb) {
		sb.append(" to " + toType.getLLVMName());
		super.appendFinalString(sb);
	}
	
	/**
	 * @return
	 */
	public LLType getToType() {
		return toType;
	}

}