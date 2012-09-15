/**
 * 
 */
package v9t9.gui.client.swt.shells.debugger;


import ejs.base.utils.HexUtils;
import v9t9.common.asm.BaseMachineOperand;
import v9t9.common.asm.IMachineOperand;
import v9t9.common.cpu.InstructionWorkBlock;
import v9t9.common.memory.IMemoryEntry;

/**
 * @author ejs
 *
 */
public class InstRow {

	private static int gCounter;
	private final int count = gCounter++;
	private final InstructionWorkBlock before;
	private final InstructionWorkBlock after;
	private boolean isGeneric;
	public InstRow(InstructionWorkBlock before) {
		this.before = before;
		this.after = before;
		this.isGeneric = true;
	}
	public InstRow(InstructionWorkBlock before, InstructionWorkBlock after) {
		this.before = before;
		this.after = after;
	}

	/**
	 * @return
	 */
	public String getAddress() {
		String addr = ">" + HexUtils.toHex4(before.inst.pc);
		
		IMemoryEntry entry = before.domain.getEntryAt(before.inst.pc);
		if (entry != null) { 
			String name = entry.lookupSymbol((short) (before.inst.pc & 0xfffe));
			if (name != null) {
				return name + " " + addr;
			}
		}
		return addr;
	}

	/**
	 * @return
	 */
	public String getInst() {
		return before.inst.toString();
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
		InstRow other = (InstRow) obj;
		if (count != other.count) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return count;
	}
	/**
	 * @return
	 */
	public String getOp1() {
		BaseMachineOperand mop1 = (BaseMachineOperand) before.inst.getOp1();
		if (mop1 == null || mop1.type == IMachineOperand.OP_NONE) {
			return "";
		}
		return before.formatOpChange(1, after);
	}

	public String getOp2() {
		BaseMachineOperand mop2 = (BaseMachineOperand) before.inst.getOp2();
		if (mop2 == null || mop2.type == IMachineOperand.OP_NONE) {
			return "";
		}
		return before.formatOpChange(2, after);
	}

	public String getOp3() {
		BaseMachineOperand mop3 = (BaseMachineOperand) before.inst.getOp3();
		if (mop3 == null || mop3.type == IMachineOperand.OP_NONE) {
			return "";
		}
		return before.formatOpChange(3, after);
	}

	/**
	 * @return
	 */
	public boolean isGeneric() {
		return isGeneric;
	}
}
