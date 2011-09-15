/**
 * 
 */
package v9t9.tools.asm.assembler.operand.ll;

import v9t9.engine.cpu.IInstruction;
import v9t9.engine.cpu.MachineOperand;
import v9t9.tools.asm.assembler.Assembler;
import v9t9.tools.asm.assembler.ResolveException;
import v9t9.tools.asm.assembler.operand.hl.AssemblerOperand;

/**
 * A reference to a forward-declared operand: the original operand contains
 * the symbol which not resolved.
 * @author Ed
 *
 */
public class LLForwardOperand extends LLOperand {

	private int size;

	public LLForwardOperand(AssemblerOperand original, int size) {
		super(original);
		if (original == null)
			throw new IllegalArgumentException();
		this.size = size;
	}

	@Override
	public String toString() {
		return "{" + getOriginal() + "}";
	}

	/* (non-Javadoc)
	 * @see v9t9.tools.asm.operand.ll.LLOperand#createMachineOperand()
	 */
	@Override
	public MachineOperand createMachineOperand(IMachineOperandFactory opFactory) throws ResolveException {
		throw new ResolveException(this, "Unresolved forward reference: " + getOriginal());
	}
	
	@Override
	public LLOperand resolve(Assembler assembler, IInstruction inst)
			throws ResolveException {
		return getOriginal().resolve(assembler, inst);
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
	 * @see v9t9.tools.asm.assembler.operand.hl.AssemblerOperand#isConst()
	 */
	@Override
	public boolean isConst() {
		return false;
	}

	
	@Override
	public int getImmediate() {
		return 0;
	}

	@Override
	public boolean hasImmediate() {
		return false;
	}
	
	@Override
	public int getSize() {
		return size;
	}

	
}