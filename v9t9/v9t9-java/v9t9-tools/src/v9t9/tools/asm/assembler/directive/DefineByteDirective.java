/*
  DefineByteDirective.java

  (c) 2008-2011 Edward Swartz

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
package v9t9.tools.asm.assembler.directive;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import v9t9.common.asm.IInstruction;
import v9t9.common.asm.ResolveException;
import v9t9.tools.asm.assembler.IAsmInstructionFactory;
import v9t9.tools.asm.assembler.IAssembler;
import v9t9.tools.asm.assembler.operand.hl.AssemblerOperand;
import v9t9.tools.asm.assembler.operand.hl.NumberOperand;
import v9t9.tools.asm.assembler.operand.hl.StringOperand;
import v9t9.tools.asm.assembler.operand.ll.LLForwardOperand;
import v9t9.tools.asm.assembler.operand.ll.LLImmedOperand;
import v9t9.tools.asm.assembler.operand.ll.LLOperand;

/**
 * @author Ed
 *
 */
public class DefineByteDirective extends Directive {

	private List<AssemblerOperand> ops;

	public DefineByteDirective(List<AssemblerOperand> ops) {
		this.ops = new ArrayList<AssemblerOperand>();
		for (AssemblerOperand op : ops) {
			if (op instanceof StringOperand) {
				decodeString(this.ops, ((StringOperand) op).getString());
			} else {
				this.ops.add(op);
			}
		}
	}
	
	private void decodeString(List<AssemblerOperand> ops, String string) {
		int idx = 0;
		while (idx < string.length()) {
			char ch = string.charAt(idx++);
			if (ch == '\\' && idx < string.length()) {
				ch = string.charAt(idx++);
				switch (ch) {
				case 'n': ch = '\n'; break;
				case 't': ch = '\t'; break;
				case 'r': ch = '\r'; break;
				case '\\': ch = '\\'; break;
				default:
					ops.add(new NumberOperand('\\'));
				}
			}
			ops.add(new NumberOperand(ch));
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DB ");
		boolean first = true;
		for (AssemblerOperand op : ops) {
			if (first)
				first = false;
			else
				builder.append(", ");
			builder.append(op.toString());
		}			
		return builder.toString();
	}

	public IInstruction[] resolve(IAssembler assembler, IInstruction previous, boolean finalPass) throws ResolveException {
		setPc(assembler.getPc());

		for (ListIterator<AssemblerOperand> iterator = ops.listIterator(); iterator.hasNext();) {
			AssemblerOperand op = iterator.next();
			LLOperand lop = op.resolve(assembler, this);
			if (!(lop instanceof LLForwardOperand)) {
				if (!(lop instanceof LLImmedOperand))
					throw new ResolveException(op, "Expected number");
				iterator.set(lop);
			}
			assembler.setPc(assembler.getPc() + 1);
		}
		return new IInstruction[] { this };
	}
	
	public byte[] getBytes(IAsmInstructionFactory factory) throws ResolveException {
		byte[] bytes = new byte[ops.size()];
		int idx = 0;
		for (AssemblerOperand op : ops) {
			if (op instanceof LLForwardOperand)
				throw new ResolveException(op);
			LLOperand lop = (LLOperand) op;
			bytes[idx++] = (byte) (lop.getImmediate() & 0xff);
		}
		return bytes;
	}
	
}
