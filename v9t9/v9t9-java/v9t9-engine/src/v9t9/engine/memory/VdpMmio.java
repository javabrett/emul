/*
  VdpMmio.java

  (c) 2008-2013 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.memory;

import v9t9.common.hardware.IVdpChip;
import v9t9.common.memory.ByteMemoryAccess;

public abstract class VdpMmio implements IConsoleMmioReader, IConsoleMmioWriter {

	protected int currentaccesscycles;
	protected IVdpChip vdpHandler;
	protected VdpRamArea fullRamArea;
	private int fullRamMask;

	public VdpMmio(VdpRamArea fullRamArea) {
		this.fullRamArea = fullRamArea;
		this.fullRamMask = fullRamArea.memory.length - 1;
		fullRamMask |= (fullRamMask >> 1) | (fullRamMask >> 2) | (fullRamMask >> 3);
	}


    public ByteMemoryArea getMemoryArea() {
    	return fullRamArea;
    }
	abstract public int getAddr();

	public int getMemoryAccessCycles() {
		return currentaccesscycles;
	}
	
	/** Set the number of extra access cycles */
	public void setMemoryAccessCycles(int i) {
		currentaccesscycles = i;
	}

	public void setVdpHandler(IVdpChip vdp) {
		this.vdpHandler = vdp;
	}

	public byte readFlatMemory(int vdpaddr) {
		return fullRamArea.memory[vdpaddr & fullRamMask];
	}

	public void writeFlatMemory(int vdpaddr, byte byt) {
		if (vdpaddr >= 0 && vdpaddr < fullRamArea.memory.length)
			fullRamArea.memory[vdpaddr & fullRamMask] = byt;
		vdpHandler.touchAbsoluteVdpMemory(vdpaddr);
	}

	public ByteMemoryAccess getByteReadMemoryAccess(int addr) {
		return new ByteMemoryAccess(fullRamArea.memory, addr);
	}

	public int getMemorySize() {
		return fullRamArea.memory.length;
	}

	/**
	 * Get the base address for the current bank
	 * @return
	 */
	public int getBankAddr() {
		return 0;
	}


	abstract public BankedMemoryEntry getMemoryBank();
}