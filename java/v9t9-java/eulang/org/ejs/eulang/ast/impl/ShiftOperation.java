/**
 * 
 */
package org.ejs.eulang.ast.impl;

import org.ejs.eulang.ast.IBinaryOperation;
import org.ejs.eulang.ast.TypeEngine;
import org.ejs.eulang.types.LLType;
import org.ejs.eulang.types.TypeException;
import org.ejs.eulang.types.LLType.BasicType;

/**
 * @author ejs
 *
 */
public class ShiftOperation extends Operation implements IBinaryOperation {

	/**
	 * @param name
	 */
	public ShiftOperation(String name) {
		super(name, false);
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IBinaryOperation#inferTypes(org.ejs.eulang.ast.IBinaryOperation.OpTypes)
	 */
	@Override
	public void inferTypes(TypeEngine typeEngine, OpTypes types) throws TypeException {
		// first, check errors
		if (types.left != null && types.left.getBasicType() != BasicType.INTEGRAL)
			throw new TypeException(getName() + " requires an integer left operand, got " + types.left.toString());
		if (types.right != null && types.right.getBasicType() != BasicType.INTEGRAL)
			throw new TypeException(getName() + " requires an integer right operand, got " + types.right.toString());
		
		// now, prefer integers
		if (types.left == null) {
			types.left = typeEngine.INT;
		}
		if (types.right == null) {
			types.right = typeEngine.INT;
		}
		if (types.result == null) {
			types.result = types.left;
			if (types.result.getBits() == 0)
				types.result = typeEngine.INT;
		}
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IBinaryOperation#castTypes(org.ejs.eulang.ast.TypeEngine, org.ejs.eulang.ast.IBinaryOperation.OpTypes)
	 */
	@Override
	public void castTypes(TypeEngine typeEngine, OpTypes types)
			throws TypeException {
		LLType newLeft = typeEngine.getPromotionType(types.left, types.result);
		LLType newRight = typeEngine.getPromotionType(types.right, typeEngine.INT);
		if (newLeft == null || newRight == null)
			throw new TypeException("cannot convert result of '" + getName() + "' on " 
					+ types.left.toString() + " and " + types.right.toString() + " to " + types.result.toString());
		types.left = newLeft;
		types.right = newRight;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IBinaryOperation#getPreferredLeftType(org.ejs.eulang.ast.TypeEngine)
	 */
	@Override
	public LLType getPreferredLeftType(TypeEngine typeEngine, LLType lhsType, LLType leftType, LLType rightType) {
		if (lhsType != null && lhsType.getBasicType() == BasicType.INTEGRAL)
			return lhsType;
		if (leftType != null && leftType.getBasicType() == BasicType.INTEGRAL)
			return leftType;
		return typeEngine.INT;
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IBinaryOperation#getPreferredRightType(org.ejs.eulang.ast.TypeEngine)
	 */
	@Override
	public LLType getPreferredRightType(TypeEngine typeEngine, LLType lhsType, LLType leftType, LLType rightType) {
		if (rightType != null && rightType.getBasicType() == BasicType.INTEGRAL)
			return rightType;
		return typeEngine.INT_ANY;
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IBinaryOperation#getResultType(org.ejs.eulang.ast.TypeEngine, org.ejs.eulang.types.LLType, org.ejs.eulang.types.LLType)
	 */
	@Override
	public LLType getResultType(TypeEngine typeEngine, LLType leftType,
			LLType rightType) throws TypeException {
		if (leftType.getBasicType() != BasicType.INTEGRAL)
			throw new TypeException(getName() + " requires an integer left side, got " + leftType.toString());
		if (rightType.getBasicType() != BasicType.INTEGRAL)
			throw new TypeException(getName() + " requires an integer left side, got " + rightType.toString());
		
		if (leftType.getBits() == 0)
			return typeEngine.INT;
		
		return leftType;
	}

}
