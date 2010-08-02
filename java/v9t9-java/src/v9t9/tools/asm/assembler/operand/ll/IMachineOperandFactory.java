package v9t9.tools.asm.assembler.operand.ll;

import v9t9.engine.cpu.MachineOperand;
import v9t9.tools.asm.assembler.ResolveException;

public interface IMachineOperandFactory {
	//MachineOperand createMachineOperand(LLOperand op) throws ResolveException;
	MachineOperand createRegisterOperand(LLRegisterOperand op) throws ResolveException;
	MachineOperand createAddressOperand(LLAddrOperand op) throws ResolveException;
	MachineOperand createCountOperand(LLCountOperand op) throws ResolveException;
	MachineOperand createImmedOperand(LLImmedOperand op) throws ResolveException;
	MachineOperand createJumpOperand(LLJumpOperand op) throws ResolveException;
	MachineOperand createOffsetOperand(LLOffsetOperand op) throws ResolveException;
	MachineOperand createRegIncOperand(LLRegIncOperand op) throws ResolveException;
	MachineOperand createRegIndOperand(LLRegIndOperand op) throws ResolveException;
	MachineOperand createEmptyOperand();
		
}
