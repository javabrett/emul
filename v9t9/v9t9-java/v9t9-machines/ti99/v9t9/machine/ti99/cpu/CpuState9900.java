/*
  CpuState9900.java

  (c) 2010-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.machine.ti99.cpu;

import java.util.HashMap;
import java.util.Map;

import ejs.base.utils.HexUtils;
import ejs.base.utils.ListenerList;
import v9t9.common.cpu.CycleCounts;
import v9t9.common.cpu.ICpuState;
import v9t9.common.cpu.IStatus;
import v9t9.common.machine.IRegisterAccess;
import v9t9.common.memory.IMemoryDomain;

/**
 * @author Ed
 *
 */
public class CpuState9900 implements ICpuState {

	private final static Map<Integer, String> regNames = new HashMap<Integer, String>();
	private final static Map<String, Integer> regIds = new HashMap<String, Integer>();
	private static void register(int reg, String id) {
		regNames.put(reg, id);
		regIds.put(id, reg);
	}
	
	static {
		for (int i = 0; i < 16; i++) {
			register(i, "R" + i);
		}
		register(Cpu9900.REG_PC, "PC");
		register(Cpu9900.REG_ST, "ST");
		register(Cpu9900.REG_WP, "WP");
	}
	
	/** program counter */
	protected short PC;
	/** workspace pointer */
	protected short WP;
	private IMemoryDomain console;
	private Status9900 status;
	private ListenerList<IRegisterWriteListener> listeners = new ListenerList<IRegisterAccess.IRegisterWriteListener>();
	private CycleCounts cycleCounts = new CycleCounts();
	private int noIntCount;

	
	public CpuState9900(IMemoryDomain console) {
		this.console = console;
		this.status = createStatus();
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.cpu.ICpuState#getCycleCounts()
	 */
	@Override
	public CycleCounts getCycleCounts() {
		return cycleCounts;
	}
	

	@Override
	public String toString() {
		return "PC=" + HexUtils.toHex4(PC) + ", WP=" + HexUtils.toHex4(WP) + ", status=" + status;
	}



	protected final void fireRegisterChanged(int reg, int value) {
		if (!listeners.isEmpty()) {
			for (IRegisterWriteListener listener : listeners) {
				try {
					listener.registerChanged(reg, value);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}
	public short getPC() {
	    return PC;
	}

	public void setPC(short pc) {
	    PC = pc;
	    fireRegisterChanged(Cpu9900.REG_PC, PC);
	}

	public short getWP() {
	    return WP;
	}

	public void setWP(short i) {
	    // TODO: verify
	    WP = i;
	    fireRegisterChanged(Cpu9900.REG_WP, WP);
	}
	
    public String getGroupName() {
    	return "9900 Registers";
    }
    /* (non-Javadoc)
     * @see v9t9.common.machine.IRegisterAccess#getFirstRegister()
     */
    @Override
    public int getFirstRegister() {
    	return 0;
    }
	/* (non-Javadoc)
	 * @see v9t9.emulator.runtime.cpu.Cpu#getRegisterCount()
	 */
	@Override
	public int getRegisterCount() {
		return 16 + 3;
	}
	protected String getRegisterId(int reg) {
		return regNames.get(reg);
	}
	

	/* (non-Javadoc)
	 * @see v9t9.common.machine.IRegisterAccess#getRegisterNumber(java.lang.String)
	 */
	@Override
	public int getRegisterNumber(String id) {
		Integer num = regIds.get(id);
		if (num == null)
			return Integer.MIN_VALUE;
		return num;

	}

	
	protected String getRegisterName(int reg) {
		switch (reg) {
		case Cpu9900.REG_ST:
			return "Status register";
		case Cpu9900.REG_WP:
			return "Workspace pointer";
		case Cpu9900.REG_PC:
			return "Program counter";
		case 11:
			return "BL return address";
		case 12:
			return "CRU Base";
		case 13:
			return "BLWP saved WP";
		case 14:
			return "BLWP saved PC";
		case 15:
			return "BLWP saved ST";
		}
		return null;
	}
	protected int getRegisterFlags(int reg) {
		switch (reg) {
		case Cpu9900.REG_ST:
			return IRegisterAccess.FLAG_ROLE_ST;
		case Cpu9900.REG_WP:
			return IRegisterAccess.FLAG_ROLE_FP + IRegisterAccess.FLAG_SIDE_EFFECTS;
		case Cpu9900.REG_PC:
			return IRegisterAccess.FLAG_ROLE_PC;
		case 11:
			return IRegisterAccess.FLAG_ROLE_RET;
		case 14:
			return IRegisterAccess.FLAG_ROLE_RET;
		default:
			return IRegisterAccess.FLAG_ROLE_GENERAL;
		}
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IRegisterAccess#getRegisterInfo(int)
	 */
	@Override
	public RegisterInfo getRegisterInfo(int reg) {
		String id = getRegisterId(reg);
		if (id == null)
			return null;
		RegisterInfo info = new RegisterInfo(id, 
				getRegisterFlags(reg), 2, getRegisterName(reg));
		if (reg < 16) {
			info.domain = console;
			info.addr = getWP() & 0xffff;
			// TODO: need notification of this change
		}
		return info;
	}
	
	@Override
	public int getRegister(int reg) {
		if (reg >= 0 && reg < 16)
			return console.flatReadWord(WP + reg*2);
		
		if (reg == Cpu9900.REG_PC)
			return PC;
		else if (reg == Cpu9900.REG_WP)
			return WP;
		else if (reg == Cpu9900.REG_ST)
			return status.flatten();
		
		return 0;
	}

	public int readWorkspaceRegister(int reg) {
		return console.readWord(WP + reg*2);
	}

	@Override
	public int setRegister(int reg, int val) {
		int old;
		if (reg >= 0 && reg < 16) {
			old = console.flatReadWord(WP + reg*2);
			console.flatWriteWord(WP + reg*2, (short) val);
		} else {
			if (reg == Cpu9900.REG_PC) {
				old = PC;
				PC = (short) val;
			} else if (reg == Cpu9900.REG_WP) {
				old = WP;
				WP = (short) val;
			} else if (reg == Cpu9900.REG_ST) {
				old = status.flatten();
				status.expand((short) val);
			} else {
				old = 0;
			}
		}
		fireRegisterChanged(reg, val);
		return old & 0xffff;
	}
	
	public int writeWorkspaceRegister(int reg, int val) {
		int old;
		old = console.flatReadWord(WP + reg*2);
		console.writeWord(WP + reg*2, (short) val);
		fireRegisterChanged(reg, val);
		return old & 0xffff;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.runtime.cpu.CpuState#getRegisterTooltip(int)
	 */
	@Override
	public String getRegisterTooltip(int reg) {
		boolean isGplWs = (getRegister(Cpu9900.REG_WP) & 0xffff) == 0x83e0;
		switch (reg) {
		case Cpu9900.REG_ST:
			return "Status register: " + getStatus().toString();
		case Cpu9900.REG_WP:
			return "Workspace pointer";
		case Cpu9900.REG_PC:
			return "Program counter";
		case 11:
			return "BL return address";
		case 13:
			return isGplWs ? "GROM Read Data Address" : "BLWP saved WP";
		case 14:
			return isGplWs ? "System Flags" : "BLWP saved PC";
		case 15:
			return isGplWs ? "VDP Address Write Address" : "BLWP saved ST";
		}
		return null;
	}

	@Override
	public Status9900 createStatus() {
		return new Status9900();
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.runtime.cpu.CpuState#getConsole()
	 */
	@Override
	public IMemoryDomain getConsole() {
		return console;
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.runtime.cpu.CpuState#getStatus()
	 */
	@Override
	public Status9900 getStatus() {
		return status;
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.runtime.cpu.CpuState#setStatus(v9t9.engine.cpu.Status)
	 */
	@Override
	public void setStatus(IStatus status) {
		this.status = (Status9900) status;
	}

	public short getST() {
	    return getStatus().flatten();
	}

	public void setST(short st) {
		getStatus().expand(st);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IRegisterAccess#addWriteListener(v9t9.common.machine.IRegisterAccess.IRegisterWriteListener)
	 */
	@Override
	public void addWriteListener(IRegisterWriteListener listener) {
		listeners.add(listener);
	}
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IRegisterAccess#removeWriteListener(v9t9.common.machine.IRegisterAccess.IRegisterWriteListener)
	 */
	@Override
	public void removeWriteListener(IRegisterWriteListener listener) {
		listeners.remove(listener);
	}
	
	public void contextSwitch(short newwp, short newpc) {
        short oldwp = getWP();
        short oldpc = getPC();
        setWP(newwp);
        setPC(newpc);
        getConsole().writeWord(newwp + 13 * 2, oldwp);
        getConsole().writeWord(newwp + 14 * 2, oldpc);
        getConsole().writeWord(newwp + 15 * 2, getST());
        noIntCount = 2;
	}
	
	public boolean areIntsFrozen() {
		if (noIntCount <= 0)
			return false;
		noIntCount--;
		return true;
	}
}