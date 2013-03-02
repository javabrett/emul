/*
  InstInfo.java

  (c) 2010-2011 Edward Swartz

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
package v9t9.common.asm;

/**
 * @author  Ed
 */
public class InstInfo {
	/**
	 * machine cycles 
	 */
	public short cycles;
	/**
	 * bit mask from Status.ST_xxx
	 */
	public int stsetBefore;
	/**
	 * bit mask from Status.ST_xxx
	 */
	public int stsetAfter;
	/**
	 * bit mask from Status.ST_xxx
	 */
	public int stReads;
	/**
	 * bit mask from Status.ST_xxx
	 */
	public int stWrites;
	/** operand is a jump (INST_JUMP_COND = conditional) */
	public int jump;
	/**
	 * INST_RSRC_xxx mask
	 */
	public int reads;
	/**
	 * INST_RSRC_xxx mask
	 * 
	 */
	public int writes;
	/** Instruction does not jump */
	public static final int INST_JUMP_FALSE = 0;
	/** Instruction always jumps */
	public static final int INST_JUMP_TRUE = 1;
	/** Instruction jumps conditionally */
	public static final int INST_JUMP_COND = 2;
	public static final int INST_RSRC_PC = 1;	// program counter
	public static final int INST_RSRC_WP = 2;	// workspace pointer (9900)
	public static final int INST_RSRC_ST = 4;	// status
	public static final int INST_RSRC_IO = 8;	// I/O
	public static final int INST_RSRC_EMU = 16; // emulator itself (builtin)
	public static final int INST_RSRC_CTX = 32;	// context switch (writer only)

	/**
	 * 
	 */
	public InstInfo() {
	}
}