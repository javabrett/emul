package v9t9.emulator.hardware.memory;

import java.io.File;
import java.io.IOException;

import v9t9.emulator.common.IEventNotifier;
import v9t9.emulator.common.Machine;
import v9t9.emulator.common.IEventNotifier.Level;
import v9t9.engine.files.DataFiles;
import v9t9.engine.memory.ByteMemoryArea;
import v9t9.engine.memory.DiskMemoryEntry;
import v9t9.engine.memory.MemoryEntry;


/**
 * F99b console memory model.
 * @author ejs
 */
public class F99bMemoryModel extends BaseTI994AMemoryModel {

	public F99bMemoryModel() {
		super();
	}

	@Override
	protected void initSettings() {
		DataFiles.addSearchPath("../../build/forth99");
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.memory.StandardConsoleMemoryModel#loadMemory()
	 */
	@Override
	public void loadMemory(IEventNotifier eventNotifier) {
		DiskMemoryEntry cpuRomEntry;
		String filename = "f99brom.bin";
    	try {
			cpuRomEntry = DiskMemoryEntry.newByteMemoryFromFile(
	    			0x400, 0, "CPU ROM",
	        		CPU,
	                filename, 
	                0x400, false);
			cpuRomEntry.load();
			memory.addAndMap(cpuRomEntry);
			cpuRomEntry.copySymbols(CPU);
			
    	} catch (IOException e) {
    		reportLoadError(eventNotifier, filename, e);
    	}
    	
    	// see if we need to merge the GROM dictionary
    	String FORTH_GROM = "forth99.grm";
    	String GROM_DICT = "f99bgromdict.bin";
    	File dictFile = null;

    	File gromFile = DataFiles.resolveFile(FORTH_GROM);
    	dictFile = DataFiles.resolveFile(GROM_DICT);
    	if (gromFile.exists() && dictFile.exists() && dictFile.lastModified() > gromFile.lastModified()) {

    		byte[] grom = new byte[(int) gromFile.length()];
    		
			int gromDictSize = (int) dictFile.length();

			try {
				DataFiles.readMemoryImage(FORTH_GROM, 0, grom.length, grom);
				int gromDictBase = (grom[2] << 8) | (grom[3] & 0xff);
				
				if (gromDictSize + gromDictBase > 16 * 1024) {
					reportLoadError(eventNotifier, GROM_DICT, new IOException("GROM dictionary too big!  GROM plus dictionary maxes out at 16k."));
				}
				
				byte[] gromDict = new byte[gromDictSize];
				DataFiles.readMemoryImage(GROM_DICT, 0, gromDictSize, gromDict);
				
				for (int i = 0; i < gromDictSize; i++) {
					grom[i + gromDictBase] = gromDict[i];
				}
				
				int end = gromDictSize + gromDictBase;
				grom[4] = (byte) (end >> 8);
				grom[5] = (byte) (end & 0xff);
				
				DataFiles.writeMemoryImage(gromFile.getAbsolutePath(), grom.length, grom);
				
				eventNotifier.notifyEvent(null, Level.INFO, "Merged dictionary into GROM, changed " + gromFile);
			} catch (IOException e) {
				reportLoadError(eventNotifier, filename, 
						(IOException) new IOException("Failed to merge dictionary into GROM").initCause(e));
				
			}
    	}
    	
    	
    	// GROM consists of ROM for 16k
		loadConsoleGrom(eventNotifier, FORTH_GROM);
		
		// then 16k of volatile GRAM for new dictionary
		MemoryEntry gramDictEntry = new MemoryEntry("16K GRAM Dictionary", GRAPHICS, 
				0x4000, 0x4000, new ByteMemoryArea(0, new byte[0x4000]));
		gramDictEntry.getArea().setLatency(0);
		memory.addAndMap(gramDictEntry);
		
		// then 32k of GRAM storage
		try {
			DiskMemoryEntry entry = DiskMemoryEntry.newByteMemoryFromFile(
	    			0x8000, 0x8000, "GRAM", 
	    			GRAPHICS,
	    			// use full path so changes are saved 
	    			DataFiles.resolveFile("f99bgram.bin").getAbsolutePath(), 
	    			0x0, true);
			memory.addAndMap(entry);
		} catch (IOException e) {
			reportLoadError(eventNotifier, filename, e);
		}
	}
	
	protected void defineConsoleMemory(Machine machine) {
		MemoryEntry entry = new MemoryEntry("64K RAM", CPU, 
				0x0400, 0xFC00, new EnhancedRamByteArea(0, 0xFC00));
		entry.getArea().setLatency(0);
		memory.addAndMap(entry);
	}
	
	protected void defineMmioMemory(Machine machine) {
		this.memory.addAndMap(new MemoryEntry("MMIO", CPU, 0x0000, 0x0400,
                new F99ConsoleMmioArea(machine)));
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.memory.TI994AStandardConsoleMemoryModel#resetMemory()
	 */
	@Override
	public void resetMemory() {
		
	}
}