/**
 * 
 */
package v9t9.tools.asm.assembler;

import v9t9.engine.cpu.MachineOperand;
import v9t9.engine.cpu.MachineOperandMFP201;
import v9t9.tools.asm.assembler.operand.ll.IMachineOperandFactory;
import v9t9.tools.asm.assembler.operand.ll.LLAddrOperand;
import v9t9.tools.asm.assembler.operand.ll.LLCountOperand;
import v9t9.tools.asm.assembler.operand.ll.LLImmedOperand;
import v9t9.tools.asm.assembler.operand.ll.LLPCRelativeOperand;
import v9t9.tools.asm.assembler.operand.ll.LLOffsetOperand;
import v9t9.tools.asm.assembler.operand.ll.LLRegDecOperand;
import v9t9.tools.asm.assembler.operand.ll.LLRegIncOperand;
import v9t9.tools.asm.assembler.operand.ll.LLRegIndOperand;
import v9t9.tools.asm.assembler.operand.ll.LLRegOffsOperand;
import v9t9.tools.asm.assembler.operand.ll.LLRegisterOperand;
import v9t9.tools.asm.assembler.operand.ll.LLScaledRegOffsOperand;

/**
 * @author Ed
 *
 */
public class MachineOperandFactoryMFP201 implements IMachineOperandFactory {

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.operand.ll.IMachineOperandFactory#createEmptyOperand()
	 */
	@Override
	public MachineOperand createEmptyOperand() {
		return MachineOperandMFP201.createEmptyOperand();
	}

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.operand.ll.IMachineOperandFactory#createRegisterOperand(v9t9.tools.asm.assembler.operand.ll.LLRegisterOperand)
	 */
	@Override
	public MachineOperand createRegisterOperand(LLRegisterOperand op)
			throws ResolveException {
		return MachineOperandMFP201.createGeneralOperand(MachineOperandMFP201.OP_REG, 
				(short) op.getRegister());
	}

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.operand.ll.IMachineOperandFactory#createImmedOperand(v9t9.tools.asm.assembler.operand.ll.LLImmedOperand)
	 */
	@Override
	public MachineOperand createImmedOperand(LLImmedOperand op)
			throws ResolveException {
		return MachineOperandMFP201.createImmediate(op.getValue());
	}

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.operand.ll.IMachineOperandFactory#createAddressOperand(v9t9.tools.asm.assembler.operand.ll.LLAddrOperand)
	 */
	@Override
	public MachineOperand createAddressOperand(LLAddrOperand op)
			throws ResolveException {
		throw new ResolveException(op, "bare address not supported");
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.operand.ll.IMachineOperandFactory#createRegIndOperand(v9t9.tools.asm.assembler.operand.ll.LLRegIndOperand)
	 */
	@Override
	public MachineOperand createRegIndOperand(LLRegIndOperand op)
			throws ResolveException {
		return MachineOperandMFP201.createGeneralOperand(
				MachineOperandMFP201.OP_IND, 
				(short) op.getRegister());

	}
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.operand.ll.IMachineOperandFactory#createRegIndOperand(v9t9.tools.asm.assembler.operand.ll.LLRegIndOperand)
	 */
	@Override
	public MachineOperand createRegOffsOperand(LLRegOffsOperand op)
			throws ResolveException {
		return MachineOperandMFP201.createGeneralOperand(
				MachineOperandMFP201.OP_OFFS, 
				(short) op.getRegister(), (short) op.getOffset());

	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.operand.ll.IMachineOperandFactory#createRegIncOperand(v9t9.tools.asm.assembler.operand.ll.LLRegIncOperand)
	 */
	@Override
	public MachineOperand createRegIncOperand(LLRegIncOperand op)
			throws ResolveException {
		return MachineOperandMFP201.createGeneralOperand(MachineOperandMFP201.OP_INC, 
				(short) op.getRegister());
	}
	

	@Override
	public MachineOperand createRegDecOperand(LLRegDecOperand op)
			throws ResolveException {
		return MachineOperandMFP201.createGeneralOperand(MachineOperandMFP201.OP_DEC, 
				(short) op.getRegister());
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.operand.ll.IMachineOperandFactory#createOffsetOperand(v9t9.tools.asm.assembler.operand.ll.LLOffsetOperand)
	 */
	@Override
	public MachineOperand createOffsetOperand(LLOffsetOperand op)
			throws ResolveException {
		throw new ResolveException(op, "R12-based offset not supported");
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.operand.ll.IMachineOperandFactory#createJumpOperand(v9t9.tools.asm.assembler.operand.ll.LLJumpOperand)
	 */
	@Override
	public MachineOperand createPCRelativeOperand(LLPCRelativeOperand op)
			throws ResolveException {
		MachineOperandMFP201 mop = new MachineOperandMFP201(MachineOperandMFP201.OP_PCREL);
		mop.val = op.getOffset();
		return mop;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.operand.ll.IMachineOperandFactory#createCountOperand(v9t9.tools.asm.assembler.operand.ll.LLCountOperand)
	 */
	@Override
	public MachineOperand createCountOperand(LLCountOperand op)
			throws ResolveException {
		return MachineOperandMFP201.createGeneralOperand(MachineOperandMFP201.OP_CNT, 
				(short) op.getCount());
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.asm.assembler.operand.ll.IMachineOperandFactory#createScaledRegOffsOperand(v9t9.tools.asm.assembler.operand.ll.LLScaledRegOffsOperand)
	 */
	@Override
	public MachineOperand createScaledRegOffsOperand(LLScaledRegOffsOperand op)
			throws ResolveException {
		return MachineOperandMFP201.createScaledRegOffsOperand(
				op.getOffset(),
				op.getAddReg(),
				op.getRegister(),
				op.getScale());
	}
}