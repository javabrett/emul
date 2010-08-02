/**
 * 
 */
package v9t9.engine.cpu;

import static v9t9.engine.cpu.InstEncodePattern.CNT;
import static v9t9.engine.cpu.InstEncodePattern.GEN;
import static v9t9.engine.cpu.InstEncodePattern.IMM;
import static v9t9.engine.cpu.InstEncodePattern.NONE;
import static v9t9.engine.cpu.InstEncodePattern.OFF;
import static v9t9.engine.cpu.InstEncodePattern.REG;

import java.util.HashMap;
import java.util.Map;

import v9t9.engine.memory.MemoryDomain;

/**
 * This class takes an Instruction and generates its opcode.
 * 
 * @author ejs
 * 
 */
public class InstTable9900 {
	final static InstEncodePattern NONE_NONE = new InstEncodePattern(NONE, NONE);
	final static InstEncodePattern IMM_NONE = new InstEncodePattern(IMM, NONE);
	final static InstEncodePattern REG_IMM = new InstEncodePattern(REG, IMM);
	final static InstEncodePattern GEN_NONE = new InstEncodePattern(GEN, NONE);
	final static InstEncodePattern REG_CNT = new InstEncodePattern(REG, CNT, 4);
	final static InstEncodePattern JMP_NONE = new InstEncodePattern(OFF, NONE);
	final static InstEncodePattern GEN_REG = new InstEncodePattern(GEN, REG, 6);
	final static InstEncodePattern GEN6_REG = new InstEncodePattern(GEN, REG, 6);
	final static InstEncodePattern GEN_CNT = new InstEncodePattern(GEN, CNT, 6);
	final static InstEncodePattern GEN_GEN = new InstEncodePattern(GEN, GEN, 6);
	final static InstEncodePattern REG_NONE = new InstEncodePattern(REG, NONE);
	final static InstEncodePattern CNT_NONE = new InstEncodePattern(CNT, NONE);
	final static InstEncodePattern OFF_NONE = new InstEncodePattern(OFF, NONE);

	static Map<Integer, InstEncodePattern> instEntries = new HashMap<Integer, InstEncodePattern>();
	static Map<Integer, Integer> instOpcodes = new HashMap<Integer, Integer>();
	static Map<Integer, Integer> instMasks = new HashMap<Integer, Integer>();

	private static void register(int inst, int opcode, InstEncodePattern entry) {
		instEntries.put(inst, entry);
		instOpcodes.put(inst, opcode);
		instMasks.put(inst, entry == NONE_NONE || entry == IMM_NONE ? opcode : 0xffff);
	}
	private static void register(int inst, int opcode, InstEncodePattern entry, int mask) {
		instEntries.put(inst, entry);
		instOpcodes.put(inst, opcode);
		instMasks.put(inst, mask);
	}
	static {
		register(InstTableCommon.Idata, 0x0000, IMM_NONE, 0xffff);
		register(InstTableCommon.Ibyte, 0x0000, IMM_NONE, 0xffff);

		register(Inst9900.Ili, 0x0200, REG_IMM, 0x20f);
		register(Inst9900.Iai, 0x0220, REG_IMM, 0x22f);
		register(Inst9900.Iandi, 0x0240, REG_IMM, 0x24f);
		register(Inst9900.Iori, 0x0260, REG_IMM, 0x26f);
		register(Inst9900.Ici, 0x0280, REG_IMM, 0x28f);

		register(Inst9900.Istwp, 0x02a0, REG_NONE, 0x2af);
		register(Inst9900.Istst, 0x02c0, REG_NONE, 0x2cf);
		register(Inst9900.Ilwpi, 0x02e0, IMM_NONE);
		register(Inst9900.Ilimi, 0x0300, IMM_NONE);
		register(Inst9900.Iidle, 0x0340, NONE_NONE);
		register(Inst9900.Irset, 0x0360, NONE_NONE);
		register(Inst9900.Irtwp, 0x0380, NONE_NONE);
		register(Inst9900.Ickon, 0x03a0, NONE_NONE);
		register(Inst9900.Ickof, 0x03c0, NONE_NONE);
		register(Inst9900.Ilrex, 0x03e0, NONE_NONE);

		register(Inst9900.Iblwp, 0x0400, GEN_NONE);
		register(Inst9900.Ib, 0x0440, GEN_NONE);
		register(Inst9900.Ix, 0x0480, GEN_NONE);
		register(Inst9900.Iclr, 0x04c0, GEN_NONE);
		register(Inst9900.Ineg, 0x0500, GEN_NONE);
		register(Inst9900.Iinv, 0x0540, GEN_NONE);
		register(Inst9900.Iinc, 0x0580, GEN_NONE);
		register(Inst9900.Iinct, 0x05c0, GEN_NONE);
		register(Inst9900.Idec, 0x0600, GEN_NONE);
		register(Inst9900.Idect, 0x0640, GEN_NONE);
		register(Inst9900.Ibl, 0x0680, GEN_NONE);
		register(Inst9900.Iswpb, 0x06c0, GEN_NONE);
		register(Inst9900.Iseto, 0x0700, GEN_NONE);
		register(Inst9900.Iabs, 0x0740, GEN_NONE);

		register(Inst9900.Isra, 0x0800, REG_CNT);
		register(Inst9900.Isrl, 0x0900, REG_CNT);
		register(Inst9900.Isla, 0x0a00, REG_CNT);
		register(Inst9900.Isrc, 0x0b00, REG_CNT);

		register(InstTableCommon.Idsr, 0x0c00, OFF_NONE, 0xcff);
		register(InstTableCommon.Ikysl, 0x0d40, NONE_NONE, 0xd40);
		register(InstTableCommon.Iticks, 0x0d60, REG_NONE, 0xd60);
		register(InstTableCommon.Iemitchar, 0x0dc0, REG_CNT, 0xdcf);
		register(InstTableCommon.Idbg, 0x0de0, NONE_NONE, 0xde0);
		register(InstTableCommon.Idbgf, 0x0de1, NONE_NONE, 0xde1);

		register(Inst9900.Ijmp, 0x1000, JMP_NONE);
		register(Inst9900.Ijlt, 0x1100, JMP_NONE);
		register(Inst9900.Ijle, 0x1200, JMP_NONE);
		register(Inst9900.Ijeq, 0x1300, JMP_NONE);
		register(Inst9900.Ijhe, 0x1400, JMP_NONE);
		register(Inst9900.Ijgt, 0x1500, JMP_NONE);
		register(Inst9900.Ijne, 0x1600, JMP_NONE);
		register(Inst9900.Ijnc, 0x1700, JMP_NONE);
		register(Inst9900.Ijoc, 0x1800, JMP_NONE);
		register(Inst9900.Ijno, 0x1900, JMP_NONE);
		register(Inst9900.Ijl, 0x1a00, JMP_NONE);
		register(Inst9900.Ijh, 0x1b00, JMP_NONE);
		register(Inst9900.Ijop, 0x1c00, JMP_NONE);

		register(Inst9900.Isbo, 0x1d00, OFF_NONE);
		register(Inst9900.Isbz, 0x1e00, OFF_NONE);
		register(Inst9900.Itb, 0x1f00, OFF_NONE);

		register(Inst9900.Icoc, 0x2000, GEN6_REG);
		register(Inst9900.Iczc, 0x2400, GEN6_REG);
		register(Inst9900.Ixor, 0x2800, GEN6_REG);
		register(Inst9900.Ixop, 0x2c00, GEN6_REG);
		
		register(Inst9900.Ildcr, 0x3000, GEN_CNT);
		register(Inst9900.Istcr, 0x3400, GEN_CNT);
		register(Inst9900.Impy, 0x3800, GEN_REG);
		register(Inst9900.Idiv, 0x3c00, GEN_REG);


		register(Inst9900.Iszc, 0x4000, GEN_GEN);
		register(Inst9900.Iszcb, 0x5000, GEN_GEN);
		register(Inst9900.Is, 0x6000, GEN_GEN);
		register(Inst9900.Isb, 0x7000, GEN_GEN);
		register(Inst9900.Ic, 0x8000, GEN_GEN);
		register(Inst9900.Icb, 0x9000, GEN_GEN);
		register(Inst9900.Ia, 0xa000, GEN_GEN);
		register(Inst9900.Iab, 0xb000, GEN_GEN);
		register(Inst9900.Imov, 0xc000, GEN_GEN);
		register(Inst9900.Imovb, 0xd000, GEN_GEN);
		register(Inst9900.Isoc, 0xe000, GEN_GEN);
		register(Inst9900.Isocb, 0xf000, GEN_GEN);

	};

	public static short[] encode(RawInstruction rawInstruction) throws IllegalArgumentException {
		Integer opcodeI = instOpcodes.get(rawInstruction.inst);
		if (opcodeI == null)
			throw new IllegalArgumentException("Non-machine instruction");
		int opcode = opcodeI;
		
		InstEncodePattern pattern = instEntries.get(rawInstruction.inst);
		if (pattern == null)
			throw new IllegalArgumentException("Non-encoded instruction");
		
		if (!(rawInstruction.getOp1() instanceof MachineOperand))
			throw new IllegalArgumentException("Non-machine operand 1: " + rawInstruction.getOp1());
		if (!(rawInstruction.getOp2() instanceof MachineOperand))
			throw new IllegalArgumentException("Non-machine operand 2: " + rawInstruction.getOp1());
		
		rawInstruction.size = 2;	// at least (for jumps)
		coerceOperandTypes(rawInstruction);
		
		MachineOperand9900 mop1 = (MachineOperand9900) rawInstruction.getOp1();
		MachineOperand9900 mop2 = (MachineOperand9900) rawInstruction.getOp2();
		
		assertOperandMatches(mop1, pattern.op1);
		assertOperandMatches(mop2, pattern.op2);
		
		opcode |= (mop2.getBits() << pattern.off1) | mop1.getBits();
		
		short[] words = { (short)opcode, 0, 0 };
		int wordCount = rawInstruction.inst != InstTableCommon.Idata ? 1 : 0;
		if (mop1.hasImmediate()) {
			words[wordCount++] = mop1.immed;
		}
		if (mop2.hasImmediate()) {
			words[wordCount++] = mop2.immed;
		}
		
		rawInstruction.size = wordCount * 2;
		
		if (wordCount == 1)
			return new short[] { words[0] };
		else if (wordCount == 2)
			return new short[] { words[0], words[1] };
		else
			return words;
	}

	private static void assertOperandMatches(BaseMachineOperand mop, int op) {
		switch (op) {
		case NONE:
			if (mop.type != MachineOperand.OP_NONE
					&& mop.type != InstTable9900.OP_STATUS
					&& mop.type != InstTable9900.OP_INST)
				throw new IllegalArgumentException("Unexpected operand: " + mop);
			break;
		case IMM:
			if (mop.type != InstTable9900.OP_IMMED)
				throw new IllegalArgumentException("Expected immediate: " + mop);
			break;
		case CNT:
			if (mop.type != InstTable9900.OP_CNT && mop.type != InstTable9900.OP_IMMED)
				throw new IllegalArgumentException("Expected count: " + mop);
			break;
		case OFF:
			if (mop.type != InstTable9900.OP_CNT && mop.type != InstTable9900.OP_IMMED
					&& mop.type != InstTable9900.OP_OFFS_R12
					&& mop.type != InstTable9900.OP_JUMP)
				throw new IllegalArgumentException("Expected offset: " + mop);
			break;
		case REG:
			if (mop.type != InstTable9900.OP_REG && mop.type != InstTable9900.OP_REG0_SHIFT_COUNT)
				throw new IllegalArgumentException("Expected immediate: " + mop);
			break;
		case GEN:
			if (mop.type != InstTable9900.OP_REG 
					&& mop.type != InstTable9900.OP_INC
					&& mop.type != InstTable9900.OP_IND
					&& mop.type != InstTable9900.OP_ADDR)
				throw new IllegalArgumentException("Expected general operand: " + mop);
			break;
		}
	}

	/** 
	 * Ensure that operands have the final types needed for machine code.
	 * operands as needed
	 * @param instruction
	 * @param finalPass if true, performs final coercions before generating
	 * machine code; otherwise, just overcomes 
	 */
	public static void coerceOperandTypes(RawInstruction instruction) {
		InstEncodePattern pattern = instEntries.get(instruction.inst);
		if (pattern == null)
			throw new IllegalArgumentException("Non-encoded instruction");
		
		if (instruction.getOp1() instanceof MachineOperand) {
			coerceOperandType(instruction, (BaseMachineOperand) instruction.getOp1(), pattern.op1);
		}
		if (instruction.getOp2() instanceof MachineOperand) {
			coerceOperandType(instruction, (BaseMachineOperand) instruction.getOp2(), pattern.op2);
		}		
		
	}
	private static void coerceOperandType(RawInstruction instruction, BaseMachineOperand mop, int op) {
		switch (op) {
		case NONE:
			if (mop.type == InstTable9900.OP_STATUS
					|| mop.type == InstTable9900.OP_INST
					|| (instruction.inst >= Inst9900.Iinc && instruction.inst <= Inst9900.Idect))
				mop.type = MachineOperand.OP_NONE;
			break;
		case IMM:
			if (mop.type == InstTable9900.OP_REG)
				mop.type = InstTable9900.OP_IMMED;
			break;
		case CNT:
			if (mop.type == InstTable9900.OP_REG
					|| mop.type == InstTable9900.OP_IMMED
					|| mop.type == InstTable9900.OP_REG0_SHIFT_COUNT)
				mop.type = InstTable9900.OP_CNT;
			if (mop.val == 16)
				mop.val = 0;
			break;
		case OFF:
			if (instruction.isJumpInst()) {
				if (mop.type == InstTable9900.OP_IMMED) {
					// convert address to offset from this inst
					mop.type = InstTable9900.OP_JUMP;
					mop.val = (mop.val - instruction.pc);
				}
			} else {
				if (mop.type == InstTable9900.OP_IMMED) {
					mop.type = InstTable9900.OP_OFFS_R12;
					mop.val = mop.immed;
				}
			}
			break;
		case REG:
			if (mop.type == InstTable9900.OP_IMMED)
				mop.type = InstTable9900.OP_REG;
			break;
		case GEN:
			if (mop.type == InstTable9900.OP_IMMED)
				mop.type = InstTable9900.OP_REG;
			break;
		}
	}
	
	public static int coerceInstructionOpcode(int inst, int opcode) {
		Integer mask = instMasks.get(inst);
		if (mask == null)
			return opcode;
		return opcode & mask;
	}
	public static InstEncodePattern lookupEncodePattern(int inst) {
		return instEntries.get(inst);
	}
	
	public static Map<String, Integer> nameToInst = new HashMap<String, Integer>();
	public static Map<Integer, String> instToName = new HashMap<Integer, String>();
	
	static { registerInstruction(InstTableCommon.Idata, "data"); }
	static { registerInstruction(Inst9900.Ili, "li"); }
	static { registerInstruction(Inst9900.Iai, "ai"); }
	static { registerInstruction(Inst9900.Iandi, "andi"); }
	static { registerInstruction(Inst9900.Iori, "ori"); }
	static { registerInstruction(Inst9900.Ici, "ci"); }
	static { registerInstruction(Inst9900.Istwp, "stwp"); }
	static { registerInstruction(Inst9900.Istst, "stst"); }
	static { registerInstruction(Inst9900.Ilwpi, "lwpi"); }
	static { registerInstruction(Inst9900.Ilimi, "limi"); }
	static { registerInstruction(Inst9900.Iidle, "idle"); }
	static { registerInstruction(Inst9900.Irset, "rset"); }
	static { registerInstruction(Inst9900.Irtwp, "rtwp"); }
	static { registerInstruction(Inst9900.Ickon, "ckon"); }
	static { registerInstruction(Inst9900.Ickof, "ckof"); }
	static { registerInstruction(Inst9900.Ilrex, "lrex"); }
	static { registerInstruction(Inst9900.Iblwp, "blwp"); }
	static { registerInstruction(Inst9900.Ib, "b"); }
	static { registerInstruction(Inst9900.Ix, "x"); }
	static { registerInstruction(Inst9900.Iclr, "clr"); }
	static { registerInstruction(Inst9900.Ineg, "neg"); }
	static { registerInstruction(Inst9900.Iinv, "inv"); }
	static { registerInstruction(Inst9900.Iinc, "inc"); }
	static { registerInstruction(Inst9900.Iinct, "inct"); }
	static { registerInstruction(Inst9900.Idec, "dec"); }
	static { registerInstruction(Inst9900.Idect, "dect"); }
	static { registerInstruction(Inst9900.Ibl, "bl"); }
	static { registerInstruction(Inst9900.Iswpb, "swpb"); }
	static { registerInstruction(Inst9900.Iseto, "seto"); }
	static { registerInstruction(Inst9900.Iabs, "abs"); }
	static { registerInstruction(Inst9900.Isra, "sra"); }
	static { registerInstruction(Inst9900.Isrl, "srl"); }
	static { registerInstruction(Inst9900.Isla, "sla"); }
	static { registerInstruction(Inst9900.Isrc, "src"); }
	static { registerInstruction(Inst9900.Ijmp, "jmp"); }
	static { registerInstruction(Inst9900.Ijlt, "jlt"); }
	static { registerInstruction(Inst9900.Ijle, "jle"); }
	static { registerInstruction(Inst9900.Ijeq, "jeq"); }
	static { registerInstruction(Inst9900.Ijhe, "jhe"); }
	static { registerInstruction(Inst9900.Ijgt, "jgt"); }
	static { registerInstruction(Inst9900.Ijne, "jne"); }
	static { registerInstruction(Inst9900.Ijnc, "jnc"); }
	static { registerInstruction(Inst9900.Ijoc, "joc"); }
	static { registerInstruction(Inst9900.Ijno, "jno"); }
	static { registerInstruction(Inst9900.Ijl, "jl"); }
	static { registerInstruction(Inst9900.Ijh, "jh"); }
	static { registerInstruction(Inst9900.Ijop, "jop"); }
	static { registerInstruction(Inst9900.Isbo, "sbo"); }
	static { registerInstruction(Inst9900.Isbz, "sbz"); }
	static { registerInstruction(Inst9900.Itb, "tb"); }
	static { registerInstruction(Inst9900.Icoc, "coc"); }
	static { registerInstruction(Inst9900.Iczc, "czc"); }
	static { registerInstruction(Inst9900.Ixor, "xor"); }
	static { registerInstruction(Inst9900.Ixop, "xop"); }
	static { registerInstruction(Inst9900.Impy, "mpy"); }
	static { registerInstruction(Inst9900.Idiv, "div"); }
	static { registerInstruction(Inst9900.Ildcr, "ldcr"); }
	static { registerInstruction(Inst9900.Istcr, "stcr"); }
	static { registerInstruction(Inst9900.Iszc, "szc"); }
	static { registerInstruction(Inst9900.Iszcb, "szcb"); }
	static { registerInstruction(Inst9900.Is, "s"); }
	static { registerInstruction(Inst9900.Isb, "sb"); }
	static { registerInstruction(Inst9900.Ic, "c"); }
	static { registerInstruction(Inst9900.Icb, "cb"); }
	static { registerInstruction(Inst9900.Ia, "a"); }
	static { registerInstruction(Inst9900.Iab, "ab"); }
	static { registerInstruction(Inst9900.Imov, "mov"); }
	static { registerInstruction(Inst9900.Imovb, "movb"); }
	static { registerInstruction(Inst9900.Isoc, "soc"); }
	static { registerInstruction(Inst9900.Isocb, "socb"); }
	static { registerInstruction(InstTableCommon.Idsr, "dsr"); }
	static { registerInstruction(InstTableCommon.Ikysl, "kysl"); }
	static { registerInstruction(InstTableCommon.Iticks, "ticks"); }
	static { registerInstruction(InstTableCommon.Iemitchar, "emitchar"); }
	static { registerInstruction(InstTableCommon.Idbg, "dbg"); }
	static { registerInstruction(InstTableCommon.Idbgf, "dbgf"); }
	static { registerInstruction(InstTableCommon.Ibyte, "byte"); }
	
	static { registerAlias(Inst9900.Ijeq, "je"); }
	static { registerAlias(Inst9900.Ijoc, "jc"); }

	public static void registerInstruction(int inst, String str) {
	    Integer i = new Integer(inst);
	    nameToInst.put(str.toUpperCase(), i);
	    instToName.put(i, str.toUpperCase());
	}
	public static void registerAlias(int inst, String str) {
	    Integer i = new Integer(inst);
	    nameToInst.put(str.toUpperCase(), i);
	}
	/**
	 * Get the instruction code for the given instruction
	 * @param str
	 * @return
	 */
	public static Integer lookupInst(String str) {
		str = str.toUpperCase();
		Integer inst = nameToInst.get(str);
		return inst;
	}
	/**
	 * Get an instruction by name
	 * @param inst
	 * @return
	 */
	public static String getInstName(int inst) {
		return instToName.get(inst);
	}
	
   /**
     * Decode instruction with opcode 'op' at 'addr' into 'ins'.
     * 
     * @param domain
     *            provides read access to memory, to decode registers and
     *            instructions
     */
    public static RawInstruction decodeInstruction(int op, int pc, MemoryDomain domain) {
    	RawInstruction inst = new RawInstruction();
    	inst.pc = pc;
    	
        /* deal with it unsigned */
        op &= 0xffff;
    
        inst.opcode = (short) op;
        inst.inst = InstTableCommon.Idata;
        inst.size = 0;
        MachineOperand9900 mop1 = new MachineOperand9900(MachineOperand.OP_NONE);
        MachineOperand9900 mop2 = new MachineOperand9900(MachineOperand.OP_NONE);
        inst.setOp1(mop1);
        inst.setOp2(mop2);
    
        // Collect the instruction name
        // and operand structure.
    
        // Initially, inst.op?.val is incomplete, and is whatever
        // raw data from the opcode we can decode;
        // inst.op?.ea is that of the instruction or immediate
        // if the operand needs it.
    
        // after decoding the instruction, we complete
        // the operand, making inst.op?.val and inst.op?.ea valid.
    
        if (op < 0x200) {
        } else if (op < 0x2a0) {
            mop1.type = InstTable9900.OP_REG;
            mop1.val = op & 15;
            mop2.type = InstTable9900.OP_IMMED;
            switch ((op & 0x1e0) >> 5) {
            case 0:
                //inst.name = "LI";
                inst.inst = Inst9900.Ili;
                break;
            case 1:
                //inst.name = "AI";
                inst.inst = Inst9900.Iai;
                break;
            case 2:
                //inst.name = "ANDI";
                inst.inst = Inst9900.Iandi;
                break;
            case 3:
                //inst.name = "ORI";
                inst.inst = Inst9900.Iori;
                break;
            case 4:
                //inst.name = "CI";
                inst.inst = Inst9900.Ici;
                break;
            }
    
        } else if (op < 0x2e0) {
            mop1.type = InstTable9900.OP_REG;
            mop1.val = op & 15;
            switch ((op & 0x1e0) >> 5) {
            case 5:
                //inst.name = "STWP";
                inst.inst = Inst9900.Istwp;
                break;
            case 6:
                //inst.name = "STST";
                inst.inst = Inst9900.Istst;
                break;
            }
    
        } else if (op < 0x320) {
            mop1.type = InstTable9900.OP_IMMED;
    
            switch ((op & 0x1e0) >> 5) {
            case 7:
                //inst.name = "LWPI";
                inst.inst = Inst9900.Ilwpi;
                break;
            case 8:
                //inst.name = "LIMI";
                inst.inst = Inst9900.Ilimi;
                break;
            }
    
        } else if (op < 0x400) {
            switch ((op & 0x1e0) >> 5) {
            case 10:
                //inst.name = "IDLE";
                inst.inst = Inst9900.Iidle;
                break;
            case 11:
                //inst.name = "RSET";
                inst.inst = Inst9900.Irset;
                break;
            case 12:
                //inst.name = "RTWP";
                inst.inst = Inst9900.Irtwp;
                break;
            case 13:
                //inst.name = "CKON";
                inst.inst = Inst9900.Ickon;
                break;
            case 14:
                //inst.name = "CKOF";
                inst.inst = Inst9900.Ickof;
                break;
            case 15:
                //inst.name = "LREX";
                inst.inst = Inst9900.Ilrex;
                break;
            }
    
        } else if (op < 0x800) {
            mop1.type = (op & 0x30) >> 4;
            mop1.val = op & 15;
    
            switch ((op & 0x3c0) >> 6) {
            case 0:
                //inst.name = "BLWP";
                inst.inst = Inst9900.Iblwp;
                break;
            case 1:
                //inst.name = "B";
                inst.inst = Inst9900.Ib;
                break;
            case 2:
                //inst.name = "X";
                inst.inst = Inst9900.Ix;
                break;
            case 3:
                //inst.name = "CLR";
                inst.inst = Inst9900.Iclr;
                break;
            case 4:
                //inst.name = "NEG";
                inst.inst = Inst9900.Ineg;
                break;
            case 5:
                //inst.name = "INV";
                inst.inst = Inst9900.Iinv;
                break;
            case 6:
                //inst.name = "INC";
                inst.inst = Inst9900.Iinc;
                break;
            case 7:
                //inst.name = "INCT";
                inst.inst = Inst9900.Iinct;
                break;
            case 8:
                //inst.name = "DEC";
                inst.inst = Inst9900.Idec;
                break;
            case 9:
                //inst.name = "DECT";
                inst.inst = Inst9900.Idect;
                break;
            case 10:
                //inst.name = "BL";
                inst.inst = Inst9900.Ibl;
                break;
            case 11:
                //inst.name = "SWPB";
                inst.inst = Inst9900.Iswpb;
                break;
            case 12:
                //inst.name = "SETO";
                inst.inst = Inst9900.Iseto;
                break;
            case 13:
                //inst.name = "ABS";
                inst.inst = Inst9900.Iabs;
                break;
            }
    
        } else if (op < 0xc00) {
            mop1.type = InstTable9900.OP_REG;
            mop1.val = op & 15;
            mop2.type = InstTable9900.OP_CNT;
            mop2.val = (op & 0xf0) >> 4;
    
            switch ((op & 0x700) >> 8) {
            case 0:
                //inst.name = "SRA";
                inst.inst = Inst9900.Isra;
                break;
            case 1:
                //inst.name = "SRL";
                inst.inst = Inst9900.Isrl;
                break;
            case 2:
                //inst.name = "SLA";
                inst.inst = Inst9900.Isla;
                break;
            case 3:
                //inst.name = "SRC";
                inst.inst = Inst9900.Isrc;
                break;
            }
    
        } else if (op < 0x1000) {
            switch ((op & 0x1e0) >> 5) {
        	// 0xc00
    		case 0:				/* DSR, OP_DSR */
    			inst.inst = InstTableCommon.Idsr;
    			mop1.type = InstTable9900.OP_OFFS_R12;
    			mop1.val = (byte) (op & 0xff);
    			break;
  			// 0xd60
    		case 11:			/* TICKS */
    			inst.inst = InstTableCommon.Iticks;
    			mop1.type = InstTable9900.OP_REG;
                mop1.val = (byte) (op & 0xf);
    			break;
    		// 0xde0
    		case 15:			/* DBG, -DBG */
    			inst.inst = InstTableCommon.Idbg;
    			mop1.type = InstTable9900.OP_CNT;
    			mop1.val = (byte) (op & 0xf);
    			break;
    			
            // TODO: extended instructions
            }
    
        } else if (op < 0x2000) {
            mop1.type = InstTable9900.OP_IMMED;
            mop1.val = (byte) (op & 0xff);
            if (op < 0x1D00) {
                mop1.val = (mop1.val << 1) + 2;
                mop1.type = InstTable9900.OP_JUMP;
            } else {
            	mop1.type = InstTable9900.OP_OFFS_R12;
            }
    
            switch ((op & 0xf00) >> 8) {
            case 0:
                //inst.name = "JMP";
                inst.inst = Inst9900.Ijmp;
                break;
            case 1:
                //inst.name = "JLT";
                inst.inst = Inst9900.Ijlt;
                break;
            case 2:
                //inst.name = "JLE";
                inst.inst = Inst9900.Ijle;
                break;
            case 3:
                //inst.name = "JEQ";
                inst.inst = Inst9900.Ijeq;
                break;
            case 4:
                //inst.name = "JHE";
                inst.inst = Inst9900.Ijhe;
                break;
            case 5:
                //inst.name = "JGT";
                inst.inst = Inst9900.Ijgt;
                break;
            case 6:
                //inst.name = "JNE";
                inst.inst = Inst9900.Ijne;
                break;
            case 7:
                //inst.name = "JNC";
                inst.inst = Inst9900.Ijnc;
                break;
            case 8:
                //inst.name = "JOC";
                inst.inst = Inst9900.Ijoc;
                break;
            case 9:
                //inst.name = "JNO";
                inst.inst = Inst9900.Ijno;
                break;
            case 10:
                //inst.name = "JL";
                inst.inst = Inst9900.Ijl;
                break;
            case 11:
                //inst.name = "JH";
                inst.inst = Inst9900.Ijh;
                break;
            case 12:
                //inst.name = "JOP";
                inst.inst = Inst9900.Ijop;
                break;
            case 13:
                //inst.name = "SBO";
                inst.inst = Inst9900.Isbo;
                break;
            case 14:
                //inst.name = "SBZ";
                inst.inst = Inst9900.Isbz;
                break;
            case 15:
                //inst.name = "TB";
                inst.inst = Inst9900.Itb;
                break;
            }
    
        } else if (op < 0x4000 && !(op >= 0x3000 && op < 0x3800)) {
            mop1.type = (op & 0x30) >> 4;
            mop1.val = op & 15;
            mop2.type = InstTable9900.OP_REG;
            mop2.val = (op & 0x3c0) >> 6;
    
            switch ((op & 0x1c00) >> 10) {
            case 0:
                //inst.name = "COC";
                inst.inst = Inst9900.Icoc;
                break;
            case 1:
                //inst.name = "CZC";
                inst.inst = Inst9900.Iczc;
                break;
            case 2:
                //inst.name = "XOR";
                inst.inst = Inst9900.Ixor;
                break;
            case 3:
                //inst.name = "XOP";
                inst.inst = Inst9900.Ixop;
                break;
            case 6:
                //inst.name = "MPY";
                inst.inst = Inst9900.Impy;
                break;
            case 7:
                //inst.name = "DIV";
                inst.inst = Inst9900.Idiv;
                break;
            }
    
        } else if (op >= 0x3000 && op < 0x3800) {
            mop1.type = (op & 0x30) >> 4;
            mop1.val = op & 15;
            mop2.type = InstTable9900.OP_CNT;
            mop2.val = (op & 0x3c0) >> 6;
    
            if (op < 0x3400) {
                //inst.name = "LDCR";
                inst.inst = Inst9900.Ildcr;
            } else {
                //inst.name = "STCR";
                inst.inst = Inst9900.Istcr;
            }
    
        } else {
            mop1.type = (op & 0x30) >> 4;
            mop1.val = op & 15;
            mop2.type = (op & 0x0c00) >> 10;
            mop2.val = (op & 0x3c0) >> 6;
    
            switch ((op & 0xf000) >> 12) {
            case 4:
                //inst.name = "SZC";
                inst.inst = Inst9900.Iszc;
                break;
            case 5:
                //inst.name = "SZCB";
                inst.inst = Inst9900.Iszcb;
                break;
            case 6:
                //inst.name = "S";
                inst.inst = Inst9900.Is;
                break;
            case 7:
                //inst.name = "SB";
                inst.inst = Inst9900.Isb;
                break;
            case 8:
                //inst.name = "C";
                inst.inst = Inst9900.Ic;
                break;
            case 9:
                //inst.name = "CB";
                inst.inst = Inst9900.Icb;
                break;
            case 10:
                //inst.name = "A";
                inst.inst = Inst9900.Ia;
                break;
            case 11:
                //inst.name = "AB";
                inst.inst = Inst9900.Iab;
                break;
            case 12:
                //inst.name = "MOV";
                inst.inst = Inst9900.Imov;
                break;
            case 13:
                //inst.name = "MOVB";
                inst.inst = Inst9900.Imovb;
                break;
            case 14:
                //inst.name = "SOC";
                inst.inst = Inst9900.Isoc;
                break;
            case 15:
                //inst.name = "SOCB";
                inst.inst = Inst9900.Isocb;
                break;
            }
        }
    
        if (inst.inst == 0) // data
        {
            mop1.type = InstTable9900.OP_IMMED;
            mop1.val = mop1.immed = (short) op;
            //inst.name = "DATA";
            inst.size = 2;
        } else {
        	// inst.completeInstruction(pc);
            // Finish reading operand immediates
            pc += 2;
            pc = mop1.fetchOperandImmediates(domain, (short)pc);
            pc = mop2.fetchOperandImmediates(domain, (short)pc);
            inst.size = pc - inst.pc;
        }

        return inst;
    }
    
    public static void calculateInstructionSize(RawInstruction target) {
    	if (target.inst == InstTableCommon.Idata) {
    		target.size = 2;
    		return;
    	} else if (target.inst == InstTableCommon.Ibyte) {
    		target.size = 1;
    		return;
    	}
    	target.size = 2;
    	InstEncodePattern pattern = lookupEncodePattern(target.inst);
		if (pattern == null)
			return;
		
		if ((pattern.op1 == GEN && ((BaseMachineOperand)target.getOp1()).type == InstTable9900.OP_ADDR)
				|| (pattern.op1 == IMM)) 
			target.size += 2;
		if ((pattern.op2 == GEN && ((BaseMachineOperand)target.getOp2()).type == InstTable9900.OP_ADDR)
				|| (pattern.op2 == IMM)) 
			target.size += 2;
	
    }
	
	//  from ts/td field of opcode, don't change order
	/** register Rx */
	public static final int OP_REG = 0;
	/** indirect *Rx */
	public static final int OP_IND = 1;
	/** address @>xxxx or @>xxxx(Rx) */ 
	public static final int OP_ADDR = 2;
	/**  register increment *Rx+ */
	public static final int OP_INC = 3;
	// these depend on the actual instruction
	public static final int OP_IMMED = 4; // immediate >xxxx (for jump, the target addr)
	public static final int OP_CNT = 5; // shift count x (4 bits)
	public static final int OP_JUMP = 6; // jump target >xxxx (offset in bytes from PC)
	public static final int OP_OFFS_R12 = 7; // offset >xxxx or .xxxx
	public static final int OP_STATUS = 8; // status word >xxxx
	public static final int OP_INST = 9; // instruction for X
	public static final int OP_REG0_SHIFT_COUNT = 10;	// shift count from R0
}
