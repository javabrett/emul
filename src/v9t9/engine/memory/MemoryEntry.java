/*
 * (c) Ed Swartz, 2005
 * 
 * Created on Dec 16, 2004
 *
 */
package v9t9.engine.memory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TreeMap;

import v9t9.utils.Utils;

/**
 * These enums and struct define a higher-level organization of the memory map,
 * used to allow large-scale customization of the emulated computer's architecture.
 * 
 * A MemoryEntry deals with larger ranges of memory than a MemoryArea but
 * smaller ones than a MemoryDomain. It represents an unbroken range of memory
 * with the same characteristics and origin.  Each MemoryEntry may be associated with a
 * file on disk, either as ROM or a nonvolatile RAM image. 
 * 
 * A set of MemoryEntrys in a MemoryMap covers the entire span of addressable
 * memory. Multiple MemoryEntrys may cover parts of each other (and this is a
 * necessity for DSR ROMs, banked memory, etc). The Memory / MemoryDomains
 * structurally allow only one MemoryArea to be active at any given location,
 * though. To determine what MemoryEntrys contribute to the actual Memory, use
 * the backlink from MemoryArea to MemoryEntry.
 * 
 * @author ejs
 */
public class MemoryEntry {
    /** start address */
    public int addr;

    /** size in bytes */
    public int size;

    /** name of entry for debugging */
    public String name;

    /** where the memory lives */
    public MemoryDomain domain;

    /** how the memory acts */
    public MemoryArea area;
    
    /** is the memory accessed as words or as bytes? */
    public boolean bWordAccess = true;

	private TreeMap<Short, String> symbols;

    public MemoryEntry(String name, MemoryDomain domain, int addr,
            int size, MemoryArea area) {
        if (size < 0 || addr < 0 || addr + size > MemoryDomain.PHYSMEMORYSIZE) {
			throw new AssertionError("illegal address range");
		}
        if ((addr & MemoryArea.AREASIZE-1) != 0) {
			throw new AssertionError("illegal address: must live on " + MemoryArea.AREASIZE + " byte boundary");
		}
        if (domain == null || area == null) {
			throw new NullPointerException();
		}
        if ((size & MemoryArea.AREASIZE-1) != 0) {
        	size += MemoryArea.AREASIZE - (size & MemoryArea.AREASIZE-1);
        }
        
        this.addr = addr;
        this.size = size;
        this.name = name;
        this.domain = domain;
        this.area = area;
        area.entry = this;
    }

    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + addr;
		result = prime * result + ((area == null) ? 0 : area.hashCode());
		result = prime * result + (bWordAccess ? 1231 : 1237);
		result = prime * result + ((domain == null) ? 0 : domain.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + size;
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MemoryEntry other = (MemoryEntry) obj;
		if (addr != other.addr) {
			return false;
		}
		if (area == null) {
			if (other.area != null) {
				return false;
			}
		} else if (!area.equals(other.area)) {
			return false;
		}
		if (bWordAccess != other.bWordAccess) {
			return false;
		}
		if (domain == null) {
			if (other.domain != null) {
				return false;
			}
		} else if (!domain.equals(other.domain)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (size != other.size) {
			return false;
		}
		return true;
	}
    
    @Override
	public String toString() {
        if (name != null) {
			return name;
		}
        return "[memory area >" + Utils.toHex4(addr) + "..." + Utils.toHex4(addr+size) + "]";
    }
    
    /** Tell if entry is mapped (does not tell if something else may. */
    //public boolean isMapped() {
   // 	return domain.isEntryMapped(this);
    	/*
        MemoryDomain.AreaIterator iter = domain.new AreaIterator(addr, size);
        while (iter.hasNext()) {
            MemoryArea theArea = (MemoryArea)iter.next();
            if (theArea.equals(area)) {
                return true;
            }
        }
        return false;
        */
    //}
    
    /** Map entry into address space */
    public void onMap() {
    	//domain.mapEntry(this);
        load();
    }

    /** Unmap entry from address space */
    public void onUnmap() {
        try {
			save();
		} catch (IOException e) {
			e.printStackTrace();
		}
        //unload();
        //domain.unmapEntry(this);
        //domain.setArea(addr, size, new WordMemoryArea());
    }

    /** Save entry, if applicable 
     * @throws IOException */
    public void save() throws IOException {
        /* nothing */
    }

    /** Load entry, if applicable */
    public void load() {
        /* nothing */
    }

    /** Unload entry, if applicable */
    public void unload() {
        /* nothing */
    }

    /** Load symbols from file in the form:
     * 
     * &lt;addr&gt; &lt;name&gt;
     * @throws IOException 
     */
    public void loadSymbols(InputStream is) throws IOException {
    	try {
    		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    		String line;
    		while ((line = reader.readLine()) != null) {
    			int idx = line.indexOf(' ');
    			if (idx > 0) {
    				int addr = Integer.parseInt(line.substring(0, idx), 16);
    				String name = line.substring(idx+1);
    				defineSymbol(addr, name);
    			} 
    		}
    		
    	} finally {
    		is.close();
    	}
    }

	public void defineSymbol(int addr, String name) {
		if (addr < this.addr || addr >= this.addr + this.size) {
			return;
		}
		if (symbols == null) {
			symbols = new TreeMap<Short, String>();
		}
		symbols.put((short) addr, name);
	}
	
	public String lookupSymbol(short addr) {
		if (symbols == null) return null;
		return symbols.get(addr);
	}

	public MemoryDomain getDomain() {
		return domain;
	}
	
}
