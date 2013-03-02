/*
  GplMmio.java

  (c) 2005-2012 Edward Swartz

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
package v9t9.engine.memory;


import java.io.PrintWriter;

import ejs.base.properties.IPersistable;
import ejs.base.properties.IProperty;
import ejs.base.settings.ISettingSection;
import ejs.base.settings.Logging;
import ejs.base.utils.HexUtils;

import v9t9.common.client.ISettingsHandler;
import v9t9.common.cpu.ICpu;
import v9t9.common.machine.IBaseMachine;
import v9t9.common.memory.IMemoryDomain;
import v9t9.common.settings.SettingSchema;
import v9t9.common.settings.Settings;

/** GPL chip entry
 * @author ejs
 */
public class GplMmio implements IConsoleMmioReader, IConsoleMmioWriter, IPersistable {
	static public final SettingSchema settingDumpGplAccess = new SettingSchema(
			ISettingsHandler.TRANSIENT,
			"DumpGplAccess", Boolean.FALSE);
    
    private IMemoryDomain domain;
    
    short gromaddr;
    boolean gromwaddrflag, gromraddrflag;
    byte buf;

	private IProperty dumpGplAccess;

	private IProperty dumpFullInstructions;

    /**
     * @param machine TODO
     * @param machine
     */
    public GplMmio(IBaseMachine machine, IMemoryDomain domain) {
        if (domain == null) {
			throw new IllegalArgumentException();
		}
        this.domain = domain;
        
        dumpFullInstructions = Settings.get(machine, ICpu.settingDumpFullInstructions);
        dumpGplAccess = Settings.get(machine, settingDumpGplAccess);
     }

    /*	GROM has a strange banking scheme where the upper portion
	of the address does not change when incremented;
	this acts like an 8K bank. */
    private short getNextAddr(short addr) {
        return (short) (addr+1 & 0x1fff | gromaddr & 0xe000);
    }
    
    public int getAddr() {
        return gromaddr;
    }
    
    public byte getAddrByte() {
        return (byte) (getNextAddr(gromaddr) >> 8);        
    }
    
    public void setAddr(short addr) {
        gromaddr = addr;
    }

    public boolean addrIsComplete() {
        return !gromwaddrflag;
    }
    
    /**
     * @see v9t9.common.memory.Memory.IConsoleMmioReader#read
     */
    public byte read(int addr) {
    	byte ret;
    	
    	if ((addr & 2) != 0) {
    	    /* >9802, address read */
    		gromwaddrflag = false;
    		if (gromraddrflag)
    			ret = (byte) (gromaddr & 0xff);
    		else
    			ret = (byte) (gromaddr >> 8);
    	    gromraddrflag = !gromraddrflag;
    	} else {
    	    /* >9800, memory read */
    		if (dumpGplAccess.getBoolean() && dumpFullInstructions.getBoolean()) {
    			PrintWriter pw = Logging.getLog(dumpFullInstructions);
				if (pw != null)
					pw.println(
    					"Read GPL >" + HexUtils.toHex4(gromaddr - 1) + " = >" + HexUtils.toHex2(buf));
    		}

    	    ret = readGrom();
    	}
    	return ret;
    }

    /**
	 * @return
	 */
	private byte readGrom() {
		byte ret = buf;
	    buf = domain.readByte(gromaddr);
	    gromaddr = getNextAddr(gromaddr);
		return ret;
	}

	/**
     * @see v9t9.common.memory.Memory.IConsoleMmioWriter#write 
     */
    public void write(int addr, byte val) {
    	if ((addr & 2) != 0) {				
    	    /* >9C02, address write */
    	    
    		gromraddrflag = false;
    		if (gromwaddrflag) {
    			gromaddr = (short) (gromaddr & 0xff00 | val & 0xff);
    			readGrom();
    		}
    		else
    			gromaddr = (short) (((val & 0xff) << 8) | gromaddr & 0xff);
    		gromwaddrflag = !gromwaddrflag;
    	} else {					
    	    /* >9C00, data write */
    		gromraddrflag = gromwaddrflag = false;

    		domain.writeByte(gromaddr - 1, val);
    		gromaddr = getNextAddr(gromaddr);
    	}    
    }

	public void loadState(ISettingSection section) {
		if (section == null)
			return;
		gromaddr = (short) section.getInt("Addr");
		gromraddrflag = section.getBoolean("ReadAddrFlag");
		gromwaddrflag = section.getBoolean("WriteAddrFlag");
		buf = domain.readByte(((gromaddr - 1) & 0x1fff) | (gromaddr & 0xe000));
	}

	public void saveState(ISettingSection section) {
		section.put("Addr", gromaddr);
		section.put("ReadAddrFlag", gromraddrflag);
		section.put("WriteAddrFlag", gromwaddrflag);
	}
}
