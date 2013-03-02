/*
  InstructionFactory9900.java

  (c) 2011-2012 Edward Swartz

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.
 
  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  General Public License for more details.
 
  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
 */
package v9t9.machine.ti99.asm;

import v9t9.common.asm.IDecompileInfo;
import v9t9.common.asm.IInstructionFactory;
import v9t9.common.asm.RawInstruction;
import v9t9.common.cpu.ICpuState;
import v9t9.common.memory.IMemoryDomain;
import v9t9.machine.ti99.cpu.InstTable9900;
import v9t9.machine.ti99.cpu.Instruction9900;

/**
 * @author ejs
 *
 */
public class InstructionFactory9900 implements IInstructionFactory {

	public static final IInstructionFactory INSTANCE = new InstructionFactory9900();

	/**
	 * 
	 */
	public InstructionFactory9900() {
		super();
	}

	@Override
	public byte[] encodeInstruction(RawInstruction instruction) {
		short[] words = InstTable9900.encode(instruction);
		byte[] bytes = new byte[words.length * 2];
		for (int idx = 0; idx < words.length; idx++) {
			bytes[idx*2] = (byte) (words[idx] >> 8);
			bytes[idx*2+1] = (byte) (words[idx] & 0xff);
		}
		return bytes;
	}

	@Override
	public RawInstruction decodeInstruction(int pc, IMemoryDomain domain) {
		return new Instruction9900(InstTable9900.decodeInstruction(domain.flatReadWord(pc), pc, domain));
	}

	public boolean isByteInst(int inst) {
		return InstTable9900.isByteInst(inst);
	}

	@Override
	public boolean isJumpInst(int inst) {
		return InstTable9900.isJumpInst(inst);
	}

	@Override
	public String getInstName(int inst) {
		return InstTable9900.getInstName(inst);
	}

	@Override
	public int getInstructionFlags(RawInstruction inst) {
		return Instruction9900.getInstructionFlags(inst);
	}

	@Override
	public IDecompileInfo createDecompileInfo(ICpuState cpuState) {
		return new HighLevelCodeInfo(cpuState, this);
	}

}