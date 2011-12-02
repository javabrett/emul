/**
 * 
 */
package v9t9.emulator.clients.builtin.video.v9938;

import org.ejs.coffee.core.properties.SettingProperty;
import org.ejs.coffee.core.settings.ISettingSection;
import org.ejs.coffee.core.utils.HexUtils;

import v9t9.emulator.clients.builtin.video.VdpCanvas;
import v9t9.emulator.clients.builtin.video.VdpColorManager;
import v9t9.emulator.clients.builtin.video.VdpModeInfo;
import v9t9.emulator.clients.builtin.video.tms9918a.VdpTMS9918A;
import v9t9.emulator.common.IMachine;
import v9t9.emulator.runtime.cpu.Cpu;
import v9t9.engine.memory.BankedMemoryEntry;

/**
 * V9938 video chip support.  This functions as a superset of the TMS9918A.
 * <p>
 * Mode bits:
 * <p>
 * R0:	M3 @ 1, M4 @ 2, M5 @ 3
 * R1:	M1 @ 4, M2 @ 3 
 * <p>
 * <pre>
 *                   M1  M2  M3  M4  M5     Mode #
 * Text 1 mode:      1   0   0   0   0		= 1		>81F0, >8000
 * Text 2 mode:      1   0   0   1   0		= 9		>81F0, >8004
 * Multicolor:       0   1   0   0   0		= 2		>81C0, >8000
 * Graphics 1 mode:  0   0   0   0   0		= 0		>81E0, >8000
 * Graphics 2 mode:  0   0   1   0   0		= 4		>81E0, >8002
 * Graphics 3 mode:  0   0   0   1   0		= 8		>81E0, >8004 (bitmap + new sprites)
 * Graphics 4 mode:  0   0   1   1   0		= 12	>81E0, >8006 (bitmap 256x192x16)
 * Graphics 5 mode:  0   0   0   0   1		= 16	>81E0, >8008 (bitmap 512x192x4)
 * Graphics 6 mode:  0   0   1   0   1		= 20	>81E0, >800A (bitmap 512x192x16)
 * Graphics 7 mode:  0   0   1   1   1		= 28	>81E0, >800E (bitmap 256x192x256)
 * </pre>
 * TODO: toying with row masking
 * TODO: acceleration: YMMV, SRCH, test HMMM and LMMM; set registers to expected values when done
 * @author ejs  
 *
 */
public class VdpV9938 extends VdpTMS9918A {

	private boolean palettelatched;
	private byte palettelatch;
	private int statusidx;
	private byte[] statusvec = new byte[16];
	private int indreg;
	private boolean indinc;
	/** ms */
	private int blinkPeriod;
	/** ms */
	private int blinkOnPeriod;
	/** ms */
	private int blinkOffPeriod;
	boolean blinkOn;
	private boolean isEnhancedMode;
	
	private int rowstride;	// stride in bytes from row to row in modes 4-7
	private int pixperbyte;	// pixels per byte
	private int pixshift;	// shift in bits from an X coord to the colors for that bit
	private int pixmask;	// mask from a byte to a color
	
	/** Working variables for command execution */
	class CommandVars {
		int dx, dy;
		int sx, sy;
		/** nx ny, or maj/min for lines */
		int nx, ny;
		/** command, masked */
		byte cmd;
		/** tell if we handle S2_TR */
		boolean isDataMoveCommand;
		/** argument  */
		byte arg;
		/** color byte */
		byte clr;
		/** operation, masked */
		byte op;
		/** line step fraction: 16.16 */
		int frac;
		/** countdown */
		int cnt;
		int ycnt;
		public int dix;
		public int diy;
		/** destination memory select */
		int mxd;
		/** sourcememory select */
		int mxs;
		/** cycles used per op */
		int cycleUse;
	}
	
	private CommandVars cmdState = new CommandVars();
	
	// the MSX 2 speed is 21 MHz (21477270 hz)
	
	static public final int CLOCK_RATE = 21477270;
	
	// cycles for commands execute in 3579545 Hz (from blueMSX)
	/* 3579545 / 60 target # cycles to be executed per tick */
    static public final SettingProperty settingMsxClockDivisor = 
    	new SettingProperty("MsxClockDivisor", new Integer(6));

	private int currentcycles = 0; // current cycles left
	private int pageOffset;
	private int pageSize;
	
	/* from mame and blueMSX:
	 * 
     */
	final static int command_cycles[][] = {
			// SRCH x6
			{ 92, 125, 92, 92 },
			// LINE x7
			{ 120, 147, 120, 120 },
			// LMMV x8
			{ 98, 137, 98, 124 },
			// LMMM x9
			{ 129, 197, 129, 132 },
			// LMCM xA
			{ 129, 197, 129, 132 },
			// LMMC xB
			{ 129, 197, 129, 132 },
			// HMMV xC
			{ 49, 65, 49, 62 },
			// HMMM xD
			{ 92, 136, 92, 97 },
			// YMMM xE
			{ 65, 125, 65, 68 },
			// HMMC xF
			{ 49, 65, 49, 62 },
	};

	public VdpV9938(IMachine machine) {
		super(machine);
	}

	protected byte[] allocVdpRegs() {
		return new byte[48];
	}
	
	public void resetPalette() {
		VdpColorManager cm = vdpCanvas.getColorMgr();
		cm.setGRB333(0, 0, 0, 0); 
		cm.setGRB333(1, 0, 0, 0); 
		cm.setGRB333(2, 6, 1, 1); 
		cm.setGRB333(3, 7, 3, 3); 
		cm.setGRB333(4, 1, 1, 7); 
		cm.setGRB333(5, 3, 2, 7); 
		cm.setGRB333(6, 1, 5, 1); 
		cm.setGRB333(7, 6, 2, 7); 
		cm.setGRB333(8, 1, 7, 1); 
		cm.setGRB333(9, 3, 7, 3); 
		cm.setGRB333(10, 6, 6, 1); 
		cm.setGRB333(11, 6, 6, 4); 
		cm.setGRB333(12, 4, 4, 1); 
		cm.setGRB333(13, 2, 6, 5); 
		cm.setGRB333(14, 5, 5, 5); 
		cm.setGRB333(15, 7, 7, 7);
		
		// color burst regs(pg 149)
		vdpregs[20] = 0;
		vdpregs[21] = 0x3b;
		vdpregs[22] = 0x05;
	}
	
	public void writeRegisterIndirect(byte val) {
		if (indreg != 17) {
			writeVdpReg(indreg, val);
		}
		if (indinc) {
			indreg = (indreg + 1) & 0x3f;
		}
	}

	/** 1: color bus in input mode, enable mouse */
	//final public public static int R8_MS = 0x80;
	/** 1: enable light pen */
	//final static int R8_LP = 0x40;
	/** 1: color 0 is from palette; 0: clear */
	final public static int R8_TP = 0x20;
	/** 1: color bus in input mode, 0: output mode */
	//final public static int R8_CB = 0x10;
	/** 1: video RAM type: 64k, 0: 16k */
	final public static int R8_VR = 0x08;
	/** 1: sprites off */
	final public static int R8_SPD = 0x02;
	/** 1: black & white, 0: color */
	final public static int R8_BW = 0x01;

	
	final public static int R0_M4 = 0x4;
	final public static int R0_M5 = 0x8;

	
	/** 1: 212, 0: 192 lines */
	final public static int R9_LN = 0x80;
	/** 1: simultaneous mode */
	//final public static int R9_S1 = 0x20;
	/** 1: simultaneous mode */
	//final public static int R9_S0 = 0x10;
	/** 1: interlace */
	final public static int R9_IL = 0x8;
	/** 1: interlace two screens on the even/odd field */
	final public static int R9_EO = 0x4;
	/** 1: PAL, 0: NTSC */
	//final public static int R9_NT = 0x2;
	/** 1: *DLCLK in input mode, else output */
	//final public static int R9_DC = 0x1;
	
	/** 1: expansion RAM, 0: video RAM */
	final public static int R45_MXC = 0x40;
	final public static int R45_MXD = 0x20;
	final public static int R45_MXS = 0x10;
	final public static int R45_DIY = 0x8;
	final public static int R45_DIX = 0x4;
	final public static int R45_EQ = 0x2;
	/** 0=X long, 1=Y long */
	final public static int R45_MAJ = 0x1;
	
	final public static int R32_SX_LO = 32;	// 0x20 1
	final public static int R33_SX_HI = 33;	// 0x21
	final public static int R34_SY_LO = 34;	// 0x22 2
	final public static int R35_SY_HI = 35;	// 0x23
	final public static int R36_DX_LO = 36;	// 0x24 1
	final public static int R37_DX_HI = 37;	// 0x25
	final public static int R38_DY_LO = 38;	// 0x26 2
	final public static int R39_DY_HI = 39;	// 0x27
	final public static int R40_NX_LO = 40;	// 0x28 1
	final public static int R41_NX_HI = 41;	// 0x29
	final public static int R42_NY_LO = 42;	// 0x2A 2
	final public static int R43_NY_HI = 43;	// 0x2B
	final public static int R44_CLR = 44;	// 0x2C
	final public static int R45_ARG = 45;	// 0x2D
	
	final public static int R46_CMD = 46;	// 0x2E
	final public static int R46_CMD_MASK = 0xf0;
	final public static byte R46_CMD_HMMC = (byte) 0xf0;
	final public static byte R46_CMD_YMMM = (byte) 0xe0;
	final public static byte R46_CMD_HMMM = (byte) 0xd0;
	final public static byte R46_CMD_HMMV = (byte) 0xc0;
	final public static byte R46_CMD_LMMC = (byte) 0xb0;
	final public static byte R46_CMD_LMCM = (byte) 0xa0;
	final public static byte R46_CMD_LMMM = (byte) 0x90;
	final public static byte R46_CMD_LMMV = (byte) 0x80;
	final public static byte R46_CMD_LINE = 0x70;
	final public static byte R46_CMD_SRCH = 0x60;
	final public static byte R46_CMD_PSET = 0x50;
	final public static byte R46_CMD_POINT = 0x40;
	final public static byte R46_CMD_STOP = 0x00;
	
	final public static int R46_LOGOP_MASK = 0xf;
	final public static int R46_LOGOP_INP = 0x0;
	final public static int R46_LOGOP_AND = 0x1;
	final public static int R46_LOGOP_OR  = 0x2;
	final public static int R46_LOGOP_EOR = 0x3;
	final public static int R46_LOGOP_NOT = 0x4;
	final public static int R46_LOGOP_TIMF = 0x8;
	final public static int R46_LOGOP_TAND = 0x9;
	final public static int R46_LOGOP_TOR  = 0xA;
	final public static int R46_LOGOP_TEOR = 0xB;
	final public static int R46_LOGOP_TNOT = 0xC;
	
	
	final public static int S2_TR = 0x80;
	final public static int S2_BD = 0x10;
	final public static int S2_EO = 0x2;
	final public static int S2_CE = 0x1;
	
	final public static int S3_COL_LO = 3;
	final public static int S4_COL_HI = 4;
	final public static int S5_ROW_LO = 5;
	final public static int S6_ROW_HI = 6;
	final public static int S7_CLR = 7;
	final public static int S8_BOR_HI = 8;
	final public static int S9_BOR_LO = 9;
	
	public final static int MODE_TEXT2 = 9;
	public final static int MODE_GRAPHICS3 = 8;
	public final static int MODE_GRAPHICS4 = 12;
	public final static int MODE_GRAPHICS5 = 16;
	public final static int MODE_GRAPHICS6 = 20;
	public final static int MODE_GRAPHICS7 = 28;
	
	private static final int REG_SR0 = 48;
	private static final int REG_COUNT = 48 + 9;
	
	@Override
	protected int doWriteVdpReg(int reg, byte old, byte val) {
		//System.out.println(Utils.toHex2(reg) + " = " + Utils.toHex2(val));
		int redraw = super.doWriteVdpReg(reg, old, val);
		
		if (reg != 16 && reg != 17 && reg < 32 && old == val)
			return redraw;
		
		switch (reg) {
		case 0:
			if (CHANGED(old, val, R0_M4 + R0_M5)) {
				redraw |= REDRAW_MODE;
			}
			break;
		case 2:
			// page
			if (CHANGED(old, val, 0x60)) {
				redraw |= REDRAW_MODE;
			}
			break;
		case 8:
			// memory, input bus, display mode, line count
			if (CHANGED(old, val, R8_VR)) {
				switchBank();
				redraw |= REDRAW_MODE;
			}
			if (CHANGED(old, val, R8_BW)) {
				vdpCanvas.getColorMgr().setGreyscale((val & R8_BW) != 0);
				redraw |= REDRAW_PALETTE;
			}
			if (CHANGED(old, val, R8_SPD)) {
				drawSprites = (val & R8_SPD) == 0;
				redraw |= REDRAW_SPRITES;
			}
			if (CHANGED(old, val, R8_TP)) {
				vdpCanvas.getColorMgr().setClearFromPalette((val & R8_TP) != 0);
				redraw |= REDRAW_PALETTE + REDRAW_SPRITES;
			}
			break;
		case 9:
			// lines and stuff
			if (CHANGED(old, val, R9_LN)) {
				//vdpCanvas.setSize(256, (val & R9_LN) != 0 ? 212 : 192);
				redraw |= REDRAW_MODE;
			}
			if (CHANGED(old, val, R9_EO + R9_IL)) {
				redraw |= REDRAW_MODE;
			}
			break;
		case 10:
		case 11:
			// color, sprite table high byte
			redraw |= REDRAW_MODE;
			break;
		case 12:
			// text 2 blinky (pg 6)
			redraw |= REDRAW_PALETTE;
			break;
			
		case 13:
			// blinking period (pg 6)
			blinkOnPeriod = ((val >> 4) & 0xf) * 1000 / 6;
			blinkOffPeriod = (val & 0xf) * 1000 / 6;
			blinkPeriod = blinkOnPeriod + blinkOffPeriod;
			blinkOn = false;
			redraw |= REDRAW_PALETTE;
			// no redraw right now
			break;
			
			
		case 14:
			// memory bank 
			switchBank();
			break;
		case 15:
			// status register pointer
			statusidx = val & 0xf;
			break;
		case 16:
			// palette #
			palettelatched = false;
			break;
		case 17:
			// indirect register access: sets the next register whose value
			// can be set through port 3 (though, reg 17 itself will not be modified)
			indreg = val & 0x3f;
			indinc = (val & 0x80) == 0;
			break;
		case 18: {
			// display adjust register (pg 6) / pan
			updateOffset();
			break;
		}
		case 19:
			// set to the line # on which to generate an interrupt (?)
			// R0 bit 4 enables
			// http://map.tni.nl/articles/split_guide.php
			break;
		case 20:
		case 21:
		case 22:
			// color burst registers
			break;
		case 23: {
			updateOffset();
			break;
		}
		case 32: // sx lo
		case 33: // sx hi (2)
		case 34: // sx lo	   or sy lo
		case 35: // sx hi (2)  or sy hi (1)
		case 36: // dx lo
		case 37: // dx hi (2)
		case 38: // dy lo
		case 39: // dy hi (1)
		case 40: // nx lo
		case 41: // nx hi (2)
		case 42: // ny lo
		case 43: // ny hi (1)
			break;
		case 44: // CLR (data to transfer)
			if (accelActive() && cmdState.isDataMoveCommand) {
				// got the next byte
				statusvec[2] &= ~S2_TR;
				work();
			}
			break;
		case 45: // ARG
			if (CHANGED(old, val, R45_MXC)) {
				// doesn't affect video rendering
				switchBank();
			}
			break;
		case 46: // CMD
			if ((statusvec[2] & S2_CE) == 0 && pixperbyte != 0) {
				setupCommand();
				setAccelActive(true);
				work();
			}
			//}
			break;
		}
		return redraw;
	}

	/**
	 * 
	 */
	private void updateOffset() {
		int xoffs = (byte)(vdpregs[18] << 4) >> 4;
		int yoffs = ((vdpregs[18] & 0xf0) >> 4) + (vdpregs[23]);
		vdpCanvas.setOffset(xoffs, yoffs);
		// V9990 reference says:
		// (P1 and B1 by 1 pixel unit, P2, B2 and B3 by 2-pixel unit, B4, B5 and B6 by 4-pixel unit) 
		dirtyAll();
		
	}

	protected void loadVdpReg(int num, byte val) {
		if (num != 44 && num != 46) {
			writeVdpReg(num, val);
		}
		else {
			vdpregs[num] = val;
		}
	}

	private void switchBank() {
		BankedMemoryEntry memoryBank = getVdpMmio().getMemoryBank();
		if (memoryBank == null)
			return;
		
		int vdpbank = (vdpregs[14] & 0x7);
		
		// 16k mode?
		if (/*(vdpregs[8] & R8_VR) == 0 ||*/ memoryBank.getBankCount() == 1) {
			vdpbank = 0;
		} else {
			// expansion RAM?
			if ((vdpregs[45] & R45_MXC) != 0 && memoryBank.getBankCount() >= 8) {
				vdpbank = (vdpbank & 0x3) | 0x8;
			}
		}
		
		memoryBank.selectBank(vdpbank);
		//System.out.println("-->vdpbank " + vdpbank);
	}

	public void writeColorData(byte val) {
		if (!palettelatched) {
			palettelatch = val;
			palettelatched = true;
		} else {
			// first byte: red red/blue, second: green
			int r = (palettelatch >> 4) & 0x7;
			int b = palettelatch & 0x7;
			int g = val & 0x7;
			//System.out.println("palette " + paletteidx + ": " + g +"|"+ r + "|"+ b);
			vdpCanvas.getColorMgr().setGRB333(vdpregs[16] & 0xf, g, r, b);
			dirtyAll();
			
			vdpregs[16] = (byte) ((vdpregs[16]+1)&0xf);
			palettelatched = false;
		}
	}

	

	/* (non-Javadoc)
     * @see v9t9.handlers.VdpHandler#readVdpStatus()
     */
    public byte readVdpStatus() {
    	if (statusidx == 0)
    		return super.readVdpStatus();
    	
    	byte ret = statusvec[statusidx];
        return ret;
    }

	public boolean isEnhancedMode() {
		return isEnhancedMode;
	}

	/**
	 * Yields one of the MODE_xxx enums based on the current vdpregs[]
	 * @return
	 */
	public int calculateModeNumber() {
		int reg0 = vdpregs[0] & R0_M3 + R0_M4 + R0_M5;
		int reg1 = vdpregs[1] & R1_M1 + R1_M2;
		
		if (reg1 == 0) {
			if (reg0 == R0_M4)
				return MODE_GRAPHICS3;
			if (reg0 == R0_M3 + R0_M4)
				return MODE_GRAPHICS4;
			if (reg0 == R0_M5)
				return MODE_GRAPHICS5;
			if (reg0 == R0_M3 + R0_M5)
				return MODE_GRAPHICS6;
			if (reg0 == R0_M3 + R0_M4 + R0_M5)
				return MODE_GRAPHICS7;
		} else if (reg1 == R1_M1 && reg0 == R0_M4) {
			return MODE_TEXT2;
		}
		return super.calculateModeNumber();
	}
	
	public String getModeName() {
		switch (modeNumber) {
		case MODE_GRAPHICS3: return "Graphics 3";
		case MODE_GRAPHICS4: return "Graphics 4";
		case MODE_GRAPHICS5: return "Graphics 5";
		case MODE_GRAPHICS6: return "Graphics 6";
		case MODE_GRAPHICS7: return "Graphics 7";
		case MODE_TEXT2: return "Text 2";
		}
		return super.getModeName();
	}
	
	@Override
	protected void establishVideoMode() {
		modeNumber = calculateModeNumber();
		isEnhancedMode = true;
		pageSize = 0x8000;
		switch (modeNumber) {
		case MODE_TEXT2:
			setText2Mode();
			dirtyAll();	// for border
			break;
		case MODE_GRAPHICS3:
			setGraphics3Mode();
			break;
		case MODE_GRAPHICS4:
			setGraphics4Mode();
			break;
		case MODE_GRAPHICS5:
			setGraphics5Mode();
			break;
		case MODE_GRAPHICS6:
			setGraphics6Mode();
			pageSize = 0x10000;
			break;
		case MODE_GRAPHICS7:
			setGraphics7Mode();
			pageSize = 0x10000;
			break;
		default:
			isEnhancedMode = false;
			super.establishVideoMode();
			break;
		}
	}
	
	@Override
	protected void setupBackdrop() {
		if (modeNumber == MODE_GRAPHICS5) {
			// even-odd tiling function
			vdpCanvas.getColorMgr().setClearColor((vdpbg >> 2) & 0x3);
			vdpCanvas.getColorMgr().setClearColor1((vdpbg) & 0x3);
			vdpCanvas.clearToEvenOddClearColors();
		} else if (modeNumber == MODE_GRAPHICS7) {
			// an GRB 332 value is here
			//byte[] rgb = { 0, 0, 0 };
			///vdpCanvas.getColorMgr().getGRB332(rgb, vdpregs[7]);
			vdpCanvas.getColorMgr().setClearColor(vdpregs[7] & 0xff);
			vdpCanvas.clear();
		} else {
			super.setupBackdrop();
		}
	}
	@Override
	protected int getMaxRedrawblocks() {
		return 80 * 27;
	}
	
	protected VdpModeInfo createBitmapModeInfo() {
		VdpModeInfo vdpModeInfo = super.createBitmapModeInfo(); 
		int ramsize = getModeAddressMask();

		vdpModeInfo.color.base = (vdpModeInfo.color.base | ((vdpregs[10] & 0x7) * 0x4000)) & ramsize;
		vdpModeInfo.patt.base = (vdpModeInfo.patt.base | ((vdpregs[4] & 0x38) * 0x800)) & ramsize;
		
		return vdpModeInfo;
	}

	
	protected void setText2Mode() {
		vdpCanvas.setFormat(VdpCanvas.Format.TEXT);
		vdpCanvas.setSize(512, getVideoHeight());
		vdpModeInfo = createText2ModeInfo();
		vdpModeRedrawHandler = new Text2ModeRedrawHandler(vdpRedrawInfo, vdpModeInfo);
		spriteRedrawHandler = null;
		vdpMmio.setMemoryAccessCycles(1);
		initUpdateBlocks(6);
	}

	protected VdpModeInfo createText2ModeInfo() {
		VdpModeInfo vdpModeInfo = new VdpModeInfo(); 
		int ramsize = getModeAddressMask();
		
		vdpModeInfo.screen.base = ((vdpregs[2] & 0x7c) * 0x400) & ramsize;
		vdpModeInfo.screen.size = 80 * 27;	// last 2.5 rows only visible in 212-line mode
		vdpModeInfo.patt.base = getPatternTableBase();
		vdpModeInfo.patt.size = 2048;
		vdpModeInfo.color.base = (((vdpregs[10] << 8) | (vdpregs[3] & 0xf8)) << 6) & ramsize;
		vdpModeInfo.color.size = 2160 / 8;
		return vdpModeInfo;
	}
	
	protected int getPatternTableBase() {
		return ((vdpregs[4] & 0xff) * 0x800) & getModeAddressMask();
	}


	@Override
	protected void setBitmapMode() {
		super.setBitmapMode();
		vdpMmio.setMemoryAccessCycles(2);
		
	}
	protected void setGraphics3Mode() {
		super.setBitmapMode();
		spriteRedrawHandler = createSprite2RedrawHandler(false);
		vdpMmio.setMemoryAccessCycles(2);
	}

	private Sprite2RedrawHandler createSprite2RedrawHandler(boolean wide) {
		
		return new Sprite2RedrawHandler(vdpRedrawInfo, createSpriteModeInfo());
	}
	
	protected void setGraphics4Mode() {
		vdpCanvas.setFormat(VdpCanvas.Format.COLOR16_1x1);
		vdpCanvas.setSize(256, getVideoHeight(), isInterlacedEvenOdd());
		vdpModeInfo = createGraphics45ModeInfo();
		vdpModeRedrawHandler = new Graphics4ModeRedrawHandler(vdpRedrawInfo, vdpModeInfo);
		spriteRedrawHandler = createSprite2RedrawHandler(false);
		vdpMmio.setMemoryAccessCycles(4);
		rowstride = 256 / 2;
		pixperbyte = 2;
		pixshift = 4;
		pixmask = 0xf;
		initUpdateBlocks(8);
	}

	
	protected VdpModeInfo createGraphics45ModeInfo() {
		VdpModeInfo vdpModeInfo = new VdpModeInfo(); 
		int ramsize = getModeAddressMask();
		
		vdpModeInfo.screen.base = 0;
		vdpModeInfo.screen.size = 0;
		vdpModeInfo.color.base = 0;
		vdpModeInfo.color.size = 0;
		
		// paging! (A15, A16)
		vdpModeInfo.patt.base = ((vdpregs[2] & 0x60) << 10) & ramsize;
		vdpModeInfo.patt.size = 27136;
		
		vdpModeInfo.sprite.base = getSpriteTableBase() & ramsize;
		vdpModeInfo.sprite.size = 128;
		vdpModeInfo.sprpat.base = (vdpregs[6] * 0x800) & ramsize;
		vdpModeInfo.sprpat.size = 2048;
		return vdpModeInfo;
	}
	
	protected void setGraphics5Mode() {
		vdpCanvas.setFormat(VdpCanvas.Format.COLOR4_1x1);
		vdpCanvas.setSize(512, getVideoHeight(), isInterlacedEvenOdd());
		vdpModeInfo = createGraphics45ModeInfo();
		vdpModeRedrawHandler = new Graphics5ModeRedrawHandler(vdpRedrawInfo, vdpModeInfo);
		spriteRedrawHandler = createSprite2RedrawHandler(true);
		rowstride = 512 / 4;
		pixperbyte = 4;
		pixshift = 2;
		pixmask = 0x3;
		vdpMmio.setMemoryAccessCycles(4);
		initUpdateBlocks(8);
	}

	protected void setGraphics6Mode() {
		vdpCanvas.setFormat(VdpCanvas.Format.COLOR16_1x1);
		vdpCanvas.setSize(512, getVideoHeight(), isInterlacedEvenOdd());
		vdpModeInfo = createGraphics67ModeInfo();
		vdpModeRedrawHandler = new Graphics6ModeRedrawHandler(vdpRedrawInfo, vdpModeInfo);
		spriteRedrawHandler = createSprite2RedrawHandler(true);
		rowstride = 512 / 2;
		pixperbyte = 2;
		pixshift = 4;
		pixmask = 0xf;
		vdpMmio.setMemoryAccessCycles(8);
		initUpdateBlocks(8);
	}

	protected VdpModeInfo createGraphics67ModeInfo() {
		VdpModeInfo vdpModeInfo = new VdpModeInfo(); 
		int ramsize = getModeAddressMask();
		
		vdpModeInfo.screen.base = 0;
		vdpModeInfo.screen.size = 0;
		vdpModeInfo.color.base = 0;
		vdpModeInfo.color.size = 0;
		
		// paging!  (A16)
		vdpModeInfo.patt.base = ((vdpregs[2] & 0x20) << 11) & ramsize;
		vdpModeInfo.patt.size = 54272;
		
		vdpModeInfo.sprite.base = getSpriteTableBase() & ramsize;
		vdpModeInfo.sprite.size = 128;
		vdpModeInfo.sprpat.base = (vdpregs[6] * 0x800) & ramsize;
		vdpModeInfo.sprpat.size = 2048;
		
		return vdpModeInfo;
	}
	
	protected void setGraphics7Mode() {
		vdpCanvas.setFormat(VdpCanvas.Format.COLOR256_1x1);
		vdpCanvas.setSize(256, getVideoHeight(), isInterlacedEvenOdd());
		vdpModeInfo = createGraphics67ModeInfo();
		vdpModeRedrawHandler = new Graphics7ModeRedrawHandler(vdpRedrawInfo, vdpModeInfo);
		spriteRedrawHandler = createSprite2RedrawHandler(false);
		rowstride = 256;
		pixperbyte = 1;
		pixshift = 8;
		pixmask = 0xff;
		vdpMmio.setMemoryAccessCycles(8);
		initUpdateBlocks(8);
	}

	
	/** For the V9938, only the on-board 128K is used for display.
	 * Also, when the graphics mode is a standard 9918A mode, the
	 * old masking applies. */
	@Override
	protected int getModeAddressMask() {
		return isEnhancedMode() || (vdpregs[8] & R8_VR) != 0 ? 0x1ffff : 0x3fff;
	}
	@Override
	protected int getSpriteTableBase() {
		return ((((vdpregs[11] & 0x3) << 8) | (vdpregs[5] & 0xff)) << 7) & getModeAddressMask(); 
	}
	
	@Override
	protected int getColorTableBase() {
		return ((((vdpregs[10] & 0x7) << 8) | (vdpregs[3] & 0xff)) << 6) & getModeAddressMask();
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.video.tms9918a.VdpTMS9918A#doTick()
	 */
	@Override
	protected void doTick() {
		super.doTick();
		
		// The "blink" controls either the r7/r12 selection for text mode
		// or the page selection for graphics 4-7 modes.
		
		// We don't redraw for interlacing; we just detect changes on both
		// pages and draw them into an interleaved image.  There's not
		// enough speed in this implementation to allow redrawing at 60 fps.
		int prevPageOffset = pageOffset;
		pageOffset = 0;
		if (isEnhancedMode && vdpregs[13] != 0) {
			boolean isBlinking = modeNumber == MODE_TEXT2;
			boolean isPageFlipping = (vdpregs[2] & 0x20) != 0 && !isBlinking;
			
			boolean isAltMode = System.currentTimeMillis() % blinkPeriod >= blinkOffPeriod;
			if (isPageFlipping) {
				pageOffset = isAltMode ? pageSize : 0;
				if (prevPageOffset != pageOffset) {
					//System.out.println("dirtying " + pageOffset);
					vdpModeInfo.patt.base = getPatternTableBase() ^ pageOffset;
					dirtyAll();
				}
			} else if (isBlinking) {
				boolean wasOn = blinkOn;
				blinkOn = isAltMode;
				if (blinkOn != wasOn) {
					if (vdpModeRedrawHandler instanceof Text2ModeRedrawHandler) {
						((Text2ModeRedrawHandler) vdpModeRedrawHandler).updateForBlink();
					}
				}
			}
			
		}
		
		int targetRate = CLOCK_RATE / settingMsxClockDivisor.getInt() / settingVdpInterruptRate.getInt();
		
		if (/*!Cpu.settingRealTime.getBoolean() ||*/ currentcycles < 0)
			currentcycles += targetRate;
		else
			currentcycles = targetRate;
	}
	
	@Override
	public boolean isThrottled() {
		return !accelActive() || (currentcycles <= 0);
	}
	
	@Override
	public synchronized void work() {
		if (!isThrottled())
			handleCommand();
	}

	private boolean accelActive() {
		return (statusvec[2] & S2_CE) != 0;
	}
	
	private void setAccelActive(boolean flag) {
		if (flag) {
			statusvec[2] |= S2_CE;
		} else {
			statusvec[2] &= ~S2_CE;
			cmdState.cmd = 0;
			cmdState.isDataMoveCommand = false;
			if (Cpu.settingDumpFullInstructions.getBoolean() && settingDumpVdpAccess.getBoolean())
				log("MSX command done");
		}
	}

	private void setupCommand() {
		byte val = vdpregs[46];
		cmdState.cmd = (byte) (val & R46_CMD_MASK);
		cmdState.op = (byte) (val & R46_LOGOP_MASK);
		cmdState.clr = vdpregs[R44_CLR];
		cmdState.dx = readDX();
		cmdState.dy = readDY();
		cmdState.arg = vdpregs[R45_ARG];
		cmdState.dix = (cmdState.arg & R45_DIX) == 0 ? 1 : -1; 
		cmdState.diy = (cmdState.arg & R45_DIY) == 0 ? 1 : -1;
		cmdState.mxs = cmdState.arg & R45_MXS;
		cmdState.mxd = cmdState.arg & R45_MXD;
		cmdState.cnt = cmdState.ycnt = 0;
		if (cmdState.cmd == R46_CMD_LINE) {
			// in line mode, X/Y are biased into 16:16
			int maj = readMaj();
			int min = readMin();
			if (Cpu.settingDumpFullInstructions.getBoolean() && settingDumpVdpAccess.getBoolean())
				log("Line: x="+cmdState.dx+",y="+cmdState.dy+",dix="+cmdState.dix+",diy="+cmdState.diy
						+",maj="+maj+",min="+min+",axis="+(vdpregs[45]&R45_MAJ));
			int frac = maj != 0 ? min * 0x10000 / maj : 0;
			if ((vdpregs[45] & R45_MAJ) == 0) {
				cmdState.nx = 0x10000 * cmdState.dix;
				cmdState.ny = frac * cmdState.diy;
			} else {
				cmdState.nx = frac * cmdState.dix;
				cmdState.ny = 0x10000 * cmdState.diy;
			}
			cmdState.dx <<= 16;
			cmdState.dy <<= 16;
			cmdState.cnt = maj;
		} else {
			cmdState.nx = readNX();
			cmdState.ny = readNY();
		}
		
		if (cmdState.cmd == R46_CMD_HMMC
				|| cmdState.cmd == R46_CMD_HMMM
				|| cmdState.cmd == R46_CMD_HMMV) {
			cmdState.op = R46_LOGOP_INP;
			
			// the high-speed moves are aligned to bytes
			cmdState.sx -= cmdState.sx % pixperbyte;
			cmdState.dx -= cmdState.dx % pixperbyte;
			cmdState.nx -= cmdState.nx % pixperbyte;
			
			cmdState.isDataMoveCommand = (cmdState.cmd == R46_CMD_HMMC); 
		} else if (cmdState.cmd == R46_CMD_LMMC
				|| cmdState.cmd == R46_CMD_LMMM
				|| cmdState.cmd == R46_CMD_LMMV) {
			
			cmdState.isDataMoveCommand = (cmdState.cmd == R46_CMD_LMMC);
		}

		// from MAME
		int cmdIdx = (cmdState.cmd - R46_CMD_SRCH) >> 4;
		if (cmdIdx >= 0 && cmdIdx < command_cycles.length) {
			//int modeSelect = ((vdpregs[1]>>6)&1)|(vdpregs[8]&2)|((vdpregs[9]<<1)&4);
			int modeSelect = (((vdpregs[1] >> 6)&1) | (vdpregs[8] & 2));
			cmdState.cycleUse = command_cycles[cmdIdx][modeSelect];
		} else {
			cmdState.cycleUse = 0;
		}
		
		// all clear
		statusvec[2] = 0;
		
		if (Cpu.settingDumpFullInstructions.getBoolean() && settingDumpVdpAccess.getBoolean())
			log("MSX command " + HexUtils.toHex2(cmdState.cmd)
					+ " arg=" + HexUtils.toHex2(cmdState.arg) 
					+ " op=" + HexUtils.toHex2(cmdState.op)
				+ " clr=" + HexUtils.toHex2(cmdState.clr)
				+ " DX,DY= " + cmdState.dx + "," + cmdState.dy +"; NX,NY=" + cmdState.nx +","+ cmdState.ny);
	}
	/**
	 * Do some work of the acceleration command.
	 * We execute one cycle of the command each tick.
	 */
	private void handleCommand() {
		int addr, saddr, daddr;
		
		if (pixperbyte == 0) {
			// not a supported mode
			setAccelActive(false);
			return;
		}
		
		switch (cmdState.cmd) {
		case R46_CMD_STOP:
			setAccelActive(false);
			return;
			
		case R46_CMD_PSET:
			// instantaneous!
			setPixel(cmdState.dx, cmdState.dy, cmdState.clr & pixmask, cmdState.op);
			setAccelActive(false);
			break;
			
		case R46_CMD_POINT:
			// instantaneous!
			byte pixel = getPixel(cmdState.dx, cmdState.dy);
			statusvec[7] = pixel;
			setAccelActive(false);
			break;
			
		case R46_CMD_LINE:
			while ((currentcycles -= cmdState.cycleUse) > 0) {
				setPixel(cmdState.dx >> 16, cmdState.dy >> 16, 
					cmdState.clr, cmdState.op);
				cmdState.dx += cmdState.nx;
				cmdState.dy += cmdState.ny;
				if (cmdState.cnt-- <= 0) {
					setAccelActive(false);
					break;
				}
			}
			break;
			
			// send a series of CLR bytes to rectangle
		case R46_CMD_HMMC:
			if ((statusvec[2] & S2_TR) == 0) {
				// always ready for next byte
				statusvec[2] |= S2_TR;
				addr = getAbsAddr(cmdState.dx + cmdState.cnt * cmdState.dix, 
						cmdState.dy + cmdState.ycnt * cmdState.diy, cmdState.mxd);
				//System.out.println(cmdState.cnt+","+cmdState.ycnt+","+addr);
				vdpMmio.writeFlatMemory(addr, vdpregs[R44_CLR]);
				cmdState.cnt += pixperbyte;
				if (cmdState.cnt > cmdState.nx) {
					cmdState.cnt = 0;
					if (++cmdState.ycnt > cmdState.ny) {
						setAccelActive(false);
						statusvec[2] &= ~S2_TR;
					}
				}
				currentcycles -= cmdState.cycleUse;
			}
			break;

			// send byte rectangle from vram to vram
		case R46_CMD_HMMM:
			while ((currentcycles -= cmdState.cycleUse) > 0) {
				saddr = getAbsAddr(cmdState.sx + cmdState.cnt * cmdState.dix, 
						cmdState.sy + cmdState.ycnt * cmdState.diy, cmdState.mxs);
				daddr = getAbsAddr(cmdState.dx + cmdState.cnt * cmdState.dix, 
						cmdState.dy + cmdState.ycnt * cmdState.diy, cmdState.mxd);
				vdpMmio.writeFlatMemory(daddr, 
						vdpMmio.readFlatMemory(saddr));
				cmdState.cnt += pixperbyte;
				if (cmdState.cnt > cmdState.nx) {
					cmdState.cnt = 0;
					if (++cmdState.ycnt > cmdState.ny) {
						setAccelActive(false);
						break;
					}
				}
			}
			break;

			// send single CLR byte to rectangle
		case R46_CMD_HMMV:
			while ((currentcycles -= cmdState.cycleUse) > 0) {
				addr = getAbsAddr(cmdState.dx + cmdState.cnt * cmdState.dix, 
						cmdState.dy + cmdState.ycnt * cmdState.diy, cmdState.mxd);
				vdpMmio.writeFlatMemory(addr, cmdState.clr);
				cmdState.cnt += pixperbyte;
				if (cmdState.cnt > cmdState.nx) {
					cmdState.cnt = 0;
					if (++cmdState.ycnt > cmdState.ny) {
						setAccelActive(false);
						break;
					}
				}
			}
			break;
			
			// send series of CLR pixels to rectangle 
		case R46_CMD_LMMC:
			if ((statusvec[2] & S2_TR) == 0) {
				// always ready for next byte
				statusvec[2] |= S2_TR;
				setPixel(cmdState.dx + cmdState.cnt * cmdState.dix, 
						cmdState.dy + cmdState.ycnt * cmdState.diy,
						vdpregs[R44_CLR] & pixmask, cmdState.op);
				currentcycles -= cmdState.cycleUse;
				if (++cmdState.cnt > cmdState.nx) {
					cmdState.cnt = 0;
					if (++cmdState.ycnt > cmdState.ny) {
						setAccelActive(false);
						statusvec[2] &= ~S2_TR;
					}
				}
			}
			break;			
			
			// send single CLR pixel to rectangle 
		case R46_CMD_LMMV:
			while ((currentcycles -= cmdState.cycleUse) > 0) {
				setPixel(cmdState.dx + cmdState.cnt * cmdState.dix, 
						cmdState.dy + cmdState.ycnt * cmdState.diy,
						cmdState.clr & pixmask, cmdState.op);
				if (++cmdState.cnt > cmdState.nx) {
					cmdState.cnt = 0;
					if (++cmdState.ycnt > cmdState.ny) {
						setAccelActive(false);
						break;
					}
				}
			}
			break;
			
			// send pixel rectangle from vram to vram
		case R46_CMD_LMMM:
			while ((currentcycles -= cmdState.cycleUse) > 0) {
				byte color = getPixel(cmdState.sx + cmdState.cnt * cmdState.dix, 
						cmdState.sy + cmdState.ycnt * cmdState.diy);
				setPixel(cmdState.dx + cmdState.cnt * cmdState.dix, 
						cmdState.dy + cmdState.ycnt * cmdState.diy, cmdState.op, color);
				cmdState.cnt += pixperbyte;
				if (cmdState.cnt > cmdState.nx) {
					cmdState.cnt = 0;
					if (++cmdState.ycnt > cmdState.ny) {
						setAccelActive(false);
						break;
					}
				}
			}
			break;
			
		}
		
		
	}

	/**
	 * Get the absolute address from these absolute pixels
	 * @param x
	 * @param y
	 * @return
	 */
	private int getAbsAddr(int x, int y, int ramSelect) {
		if (ramSelect == 0)
			return (y * rowstride + (x / pixperbyte)) & 0x1ffff;
		else
			return ((y * rowstride + (x / pixperbyte)) & 0xffff) + 0x20000;
	}
	
	/**
	 * Get a logical pixel
	 *
	 * @param x
	 * @param y
	 */
	private byte getPixel(int x, int y) {
		x &= 0x1ff;
		y &= 0x3ff;
		int addr = getAbsAddr(x, y, cmdState.mxs);
		byte current = vdpMmio.readFlatMemory(addr);
		int xmod = pixperbyte != 0 ? pixperbyte - 1 - x % pixperbyte : 0; 
		int xshift = (xmod * pixshift);
		
		byte mask = (byte) (pixmask << xshift);
		
		return (byte) ((current & mask) >> xshift); 
	}

	/**
	 * Set a logical pixel
	 *
	 * @param x
	 * @param y
	 * @param color
	 */
	private void setPixel(int x, int y, int color, int op) {
		x &= 0x1ff;
		y &= 0x3ff;
		int addr = getAbsAddr(x, y, cmdState.mxd);
		byte current = vdpMmio.readFlatMemory(addr);
		int xmod = pixperbyte != 0 ? pixperbyte - 1 - x % pixperbyte : 0; 
		int xshift = (xmod * pixshift);
		byte updated = current;
		
		byte mask = (byte) (pixmask << xshift);
		color = (color & pixmask) << xshift;
		
		// test if color is non-blank for TEST operations
		if ((op & 0x8) != 0 && color == 0)
			return;
			
		switch (op) {
			
		case R46_LOGOP_TIMF:
		case R46_LOGOP_INP:
			updated = (byte) ((current & ~mask) | color);
			break;
		case R46_LOGOP_TOR:
		case R46_LOGOP_OR:
			updated = (byte) (current | color);
			break;
		case R46_LOGOP_TAND:
		case R46_LOGOP_AND:
			updated = (byte) (current & color);
			break;
		case R46_LOGOP_TEOR:
		case R46_LOGOP_EOR:
			updated = (byte) (current ^ color);
			break;
		case R46_LOGOP_TNOT:
		case R46_LOGOP_NOT:
			updated = (byte) ((current & ~mask) | (color ^ pixmask));
			break;
		}
		
		vdpMmio.writeFlatMemory(addr, updated);  
	}

	private int readDY() {
		return (vdpregs[R38_DY_LO] & 0xff) | ((vdpregs[R39_DY_HI] & 0x3) << 8);
	}

	private int readDX() {
		return (vdpregs[R36_DX_LO] & 0xff) | ((vdpregs[R37_DX_HI] & 0x1) << 8);
	}
	
	private int readNY() {
		return (vdpregs[R42_NY_LO] & 0xff) | ((vdpregs[R43_NY_HI] & 0x3) << 8);
	}
	
	private int readNX() {
		return (vdpregs[R40_NX_LO] & 0xff) | ((vdpregs[R41_NX_HI] & 0x1) << 8);
	}
	
	/** Note: swap the # of bits wrt NX and NY */
	private int readMin() {
		return (vdpregs[R42_NY_LO] & 0xff) | ((vdpregs[R43_NY_HI] & 0x1) << 8);
	}
	
	private int readMaj() {
		return (vdpregs[R40_NX_LO] & 0xff) | ((vdpregs[R41_NX_HI] & 0x3) << 8);
	}


	@Override
	protected int getVideoHeight() {
		int baseHeight = ((vdpregs[9] & R9_LN) != 0 ? 212 : 192);
		//return ((vdpregs[9] & R9_IL) != 0) ? baseHeight * 2 : baseHeight;
		return baseHeight;
	}

	public synchronized int getGraphicsPageOffset() {
		return pageOffset;
	}
	public int getGraphicsPageSize() {
		return pageSize;
	}

	/**
	 * Tell whether interlacing is active.
	 * 
	 * For use in rendering, we need to know whether raw R9_IL (interlace) bit is set
	 * and also the R9_EO (even/odd) bit is set, which would provide the page flipping
	 * required to *see* two pages.  Finally, the "odd" graphics page must be visible
	 * for the flipping and interlacing to occur.
	 * @return
	 */
	
	public boolean isInterlacedEvenOdd() {
		return (vdpregs[9] & R9_EO + R9_IL) == R9_EO + R9_IL 
			&& (vdpregs[2] & 0x20) != 0;

	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.video.tms9918a.VdpTMS9918A#saveState(org.ejs.coffee.core.settings.ISettingSection)
	 */
	@Override
	public void saveState(ISettingSection section) {
		super.saveState(section);

		ISettingSection statuses = section.addSection("Statuses");
		for (int i = 0; i < statusvec.length; i++)
			statuses.put(HexUtils.toHex2(i), HexUtils.toHex2(statusvec[i]));
		
		section.put("AccelActive", accelActive());
		
	}
	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.video.tms9918a.VdpTMS9918A#loadState(org.ejs.coffee.core.settings.ISettingSection)
	 */
	@Override
	public void loadState(ISettingSection section) {
		super.loadState(section);
		
		if (section == null)
			return;

		setAccelActive(section.getBoolean("AccelActive"));
		if (accelActive()) {
			setupCommand();
		}

		ISettingSection statuses = section.getSection("Statuses");
		if (statuses != null) {
			for (String name : statuses.getSettingNames()) {
				try {
					statusvec[Integer.parseInt(name, 16)] = 
						(byte) Integer.parseInt(statuses.get(name), 16);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		}
		
		blinkOn = true;
	}


	/* (non-Javadoc)
	 * @see v9t9.engine.VdpHandler#getRegisterCount()
	 */
	@Override
	public int getRegisterCount() {
		return REG_COUNT;
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.video.tms9918a.VdpTMS9918A#getRegister(int)
	 */
	@Override
	public byte getRegister(int reg) {
		if (reg > REG_SR0)
			return statusvec[reg - REG_SR0];
		else
			return super.getRegister(reg);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.video.tms9918a.VdpTMS9918A#setRegister(int, byte)
	 */
	@Override
	public void setRegister(int reg, byte value) {
		if (reg > REG_SR0)
			statusvec[reg - REG_SR0] = value;
		else
			super.setRegister(reg, value);
	}
	
	

	protected String getStatusString(byte s) {
		return caten(yOrN("Int", s & 0x80),
				yOrN("9 Sprites", s & 0x40),
				yOrN("Coinc", s & 0x20))
				+ " | 9th: " + (s & 0x1f);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.video.tms9918a.VdpTMS9918A#getRegisterName(int)
	 */
	@Override
	public String getRegisterName(int reg) {
		if (reg >= REG_SR0)
			return "SR" + (reg - REG_SR0);
		else
			return super.getRegisterName(reg);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.video.tms9918a.VdpTMS9918A#getRegisterTooltip(int)
	 */
	@Override
	public String getRegisterTooltip(int reg) {
		if (reg == REG_ST)
			return getStatusString(vdpStatus);
		byte val = 0;
		if (reg < REG_SR0) {
			val = vdpregs[reg - 1];
		} else {
			val = statusvec[reg - REG_SR0];
		}
		switch (reg < REG_SR0 ? reg - 1 : reg) {
		case 0:
			return caten(yOrN("DG", val & 0x40), yOrN("IE2", val & 0x40),
					yOrN("IE1", val & 0x20), yOrN("M5", val & R0_M5),
					yOrN("M4", val & R0_M4), yOrN("M3", val & R0_M3))
					+ " (" + getModeName() + ")";
		case 1:
			return caten((val & R1_NOBLANK) != 0 ? "Show" : "Blank",
					yOrN("IE0", val & R1_INT), yOrN("M1", val & R1_M1),
					yOrN("M2", val & R1_M2),
					yOrN("Size 4", val & R1_SPR4), yOrN("Mag", val & R1_SPRMAG))
					+ " (" + getModeName() + ")";
		case 8:
			return caten(yOrN("Mouse", val & 0x80), yOrN("Light Pen", val & 0x40),
					yOrN("NoTransp", val & R8_TP), yOrN("ColorBus", val & 0x10), 
					(val & R8_VR) != 0 ? "64K" : "16K",
					yOrN("Sprites", val & R8_SPD), yOrN("B&W", val & R8_BW));
		case 9:
			return caten((val & R9_LN) != 0 ? "212ln" : "192ln",
					yOrN("Simul1", val & 0x20),
					yOrN("Simul0", val & 0x10),
					yOrN("Interlace", val & R9_IL),
					yOrN("EvenOdd", val & R9_EO),
					(val & 0x02) != 0 ? "PAL" : "NTSC",
					yOrN("DC", val & 0x01));
		case 10:
			return super.getRegisterTooltip(3);
		case 11:
			return super.getRegisterTooltip(6);
		case 12:
			return "Blink Color BG: " + HexUtils.toHex2(val & 0x7) 
			+ " | FG: " + HexUtils.toHex2((val & 0xf0) >> 4);
		case 13:
			return "Blink period on: " + blinkOnPeriod + " ms"
			+ " | Off: " + blinkOffPeriod + " ms";
		case 14:
			return "Page base: " + HexUtils.toHex4(val << 14);
		case 15:
			return "Status register #";
		case 16:
			return "Color palette address";
		case 17:
			return caten("Control register pointer: " + (val & 0x7f), yOrN("AutoInc", val & 0x80));
		case 18:
			return "Display adjust Vert: " + ((val & 0xf0) >> 4)
			+ " | Horiz: " + (val & 0xf);
		case 19:
			return "Interrupt line";
		case 23:
			return "Display offset";
		case 20:
			return "Color burst 1";
		case 21:
			return "Color burst 2";
		case 22:
			return "Color burst 3";
		case 32:
			return "Source X Low";
		case 33:
			return "Source X High";
		case 34:
			return "Source Y Low";
		case 35:
			return "Source Y High";
		case 36:
			return "Dest X Low";
		case 37:
			return "Dest X High";
		case 38:
			return "Dest Y Low";
		case 39:
			return "Dest Y High";
		case 40:
			return "# Dots X Low";
		case 41:
			return "# Dots X High";
		case 42:
			return "# Dots Y Low";
		case 43:
			return "# Dots Y High";
		case 44:
			return "Color High: " + ((val & 0xf0) >> 4) + " | Low: " + (val & 0xf);
		case 45:
			return "Arguments: " +
			caten(yOrN("MXC", val & R45_MXC),
					yOrN("MXD", val & R45_MXD),
					yOrN("MXS", val & R45_MXS),
					yOrN("DIY", val & R45_DIY),
					yOrN("DIX", val & R45_DIX),
					yOrN("EQ", val & R45_EQ),
					yOrN("MAJ", val & R45_MAJ));
		case 46:
			return "Command: " + ((val & 0xf0) >> 4) + " | Lo: " + (val & 0xf);
			
		case REG_SR0: // already handled
			return null;
		case REG_SR0 + 1:
			return "Status register 1: " + 
			caten(yOrN("FL", val & 0x80),
					yOrN("LPS", val & 0x40),
					"ID: " + ((val & 0x3e) >> 1),
					yOrN("FH", val & 0x01));
		case REG_SR0 + 2:
			return "Status register 2: " + 
			caten(yOrN("TransRdy", val & 0x80),
					yOrN("VertScan", val & 0x40),
					yOrN("HorizScan", val & 0x20),
					yOrN("BoundDetect", val & 0x10),
					yOrN("EvenOdd", val & 0x02),
					yOrN("CmdExec", val & 0x01)
					);
		case REG_SR0 + 3:
			return "Column register low";
		case REG_SR0 + 4:
			return "Column register high";
		case REG_SR0 + 5:
			return "Row register low";
		case REG_SR0 + 6:
			return "Row register high";
		case REG_SR0 + 7:
			return "Color register";
		case REG_SR0 + 8:
			return "Border X register low";
		case REG_SR0 + 9:
			return "Border X register high";
		}
		
		if (reg >= REG_SR0)
			return null;
		
		return super.getRegisterTooltip(reg);
	}
}