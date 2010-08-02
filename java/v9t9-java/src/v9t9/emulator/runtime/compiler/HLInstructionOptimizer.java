/**
 * 
 */
package v9t9.emulator.runtime.compiler;

import v9t9.engine.cpu.Instruction9900;

/**
 * Optimizer for instruction arrays
 * @author ejs
 *
 */
public class HLInstructionOptimizer {

	/**
	 * Reset status setting flags for instructions whose results aren't
	 * read (and are destroyed) by the following instruction.
	 * This algorithm ignores flow control, but that's okay, because
	 * jumps do not destroy status.
     */
    public static void peephole_status(Instruction9900[] insts, int numinsts) {
        Instruction9900 prev = null;
        int i;
        i = 0;
        while (i < numinsts) {
            Instruction9900 ins = insts[i];
            if (ins == null) {
                i++;
                continue;
            }
            if (prev != null) {
                int prevBits = Instruction9900.getStatusBits(prev.info.stsetBefore)
                    | Instruction9900.getStatusBits(prev.info.stsetAfter);
                int bits  = Instruction9900.getStatusBits(ins.info.stsetBefore)
                    | Instruction9900.getStatusBits(ins.info.stsetAfter);
                // we're not looking at flow of control here,
                // so just turn off status writes for instructions
                // where the next instruction writes the same bits
                if (prevBits != 0 && (prevBits & ~bits) == 0) {
                	//System.out.println("Clear status bits for " + prev);
                    prev.info.stsetBefore = Instruction9900.st_NONE;
                    prev.info.stsetAfter = Instruction9900.st_NONE;
                }
            }
            prev = ins;
            if (ins.size <= 0) {
				throw new AssertionError("WTF? " + ins);
			}
            i += ins.size / 2;
        }

    }
}
