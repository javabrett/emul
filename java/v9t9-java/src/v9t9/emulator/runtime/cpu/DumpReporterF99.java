/**
 * 
 */
package v9t9.emulator.runtime.cpu;

import java.io.PrintWriter;

import org.ejs.coffee.core.utils.HexUtils;

import v9t9.emulator.runtime.InstructionListener;
import v9t9.engine.cpu.BaseInstructionWorkBlock;
import v9t9.engine.cpu.RawInstruction;

/**
 * @author ejs
 *
 */
public class DumpReporterF99 implements InstructionListener {
	private final CpuF99 cpu;

	/**
	 * 
	 */
	public DumpReporterF99(CpuF99 cpu) {
		this.cpu = cpu;
	}
	/* (non-Javadoc)
	 * @see v9t9.emulator.runtime.InstructionListener#executed(v9t9.engine.cpu.InstructionAction.Block, v9t9.engine.cpu.InstructionAction.Block)
	 */
	public void executed(BaseInstructionWorkBlock before, BaseInstructionWorkBlock after) {
		PrintWriter dump = Executor.getDump();
		if (dump == null)
			return;
		RawInstruction ins = before.inst;
	    dump.println(HexUtils.toHex4(ins.pc) 
	            + " "
	            + HexUtils.toHex4(((CpuStateF99)cpu.getState()).getSP())
	            + " "
	            + HexUtils.toHex4(((CpuStateF99)cpu.getState()).getRP())
	    );
		dump.flush();

	}

}
