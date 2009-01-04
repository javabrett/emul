/*
 * (c) Ed Swartz, 2005
 * 
 * Created on Feb 21, 2006
 *
 */
package v9t9.emulator.hardware.memory;

import v9t9.emulator.Machine;
import v9t9.emulator.hardware.TI994A;
import v9t9.emulator.hardware.memory.mmio.ConsoleGramWriteArea;
import v9t9.emulator.hardware.memory.mmio.ConsoleGromReadArea;
import v9t9.emulator.hardware.memory.mmio.ConsoleSoundArea;
import v9t9.emulator.hardware.memory.mmio.ConsoleSpeechReadArea;
import v9t9.emulator.hardware.memory.mmio.ConsoleSpeechWriteArea;
import v9t9.emulator.hardware.memory.mmio.ConsoleVdpReadArea;
import v9t9.emulator.hardware.memory.mmio.ConsoleVdpWriteArea;
import v9t9.emulator.hardware.memory.mmio.GplMmio;
import v9t9.emulator.hardware.memory.mmio.SoundMmio;
import v9t9.emulator.hardware.memory.mmio.SpeechMmio;
import v9t9.emulator.hardware.memory.mmio.VdpMmio;
import v9t9.engine.memory.Memory;
import v9t9.engine.memory.MemoryDomain;
import v9t9.engine.memory.MemoryEntry;
import v9t9.engine.memory.MemoryModel;

/**
 * The standard TI-99/4[A] console memory map.
 * @author ejs
 */
public class StandardConsoleMemoryModel implements MemoryModel {
    /* CPU ROM/RAM */
    protected MemoryDomain CPU;

    /* GPL ROM/RAM */
    private MemoryDomain GRAPHICS;

    /* VDP RAM */
    private MemoryDomain VIDEO;

    /* Speech ROM */
    private MemoryDomain SPEECH;

    public SoundMmio soundMmio;
    public SpeechMmio speechMmio;
    public GplMmio gplMmio;

	protected Memory memory;

	private VdpMmio vdpMmio;
    
    public StandardConsoleMemoryModel() {
    	initSettings();
    }
    
    public Memory createMemory() {
    	this.memory = new Memory(this);
    	
        CPU = new MemoryDomain(4);
        GRAPHICS = new MemoryDomain();
        VIDEO = new MemoryDomain();
        SPEECH = new MemoryDomain();
        
        memory.addDomain("CPU", CPU);
        memory.addDomain("GRAPHICS", GRAPHICS);
        memory.addDomain("VIDEO", VIDEO);
        memory.addDomain("SPEECH", SPEECH);
        return memory;
        
    }
    
    public void initMemory(Machine machine) {
        defineConsoleMemory(machine);
     
        soundMmio = new SoundMmio(machine.getSound());
        gplMmio = new GplMmio(GRAPHICS);
        speechMmio = new SpeechMmio(machine);
        
        if (machine instanceof TI994A) {
        	vdpMmio = ((TI994A) machine).getVdpMmio();
        	defineMmioMemory((TI994A) machine);
        }
    }

	protected void initSettings() {
		ExpRamArea.settingExpRam.setBoolean(true);
		ConsoleRamArea.settingEnhRam.setBoolean(false);
	}
 
	protected void defineConsoleMemory(Machine machine) {
	    memory.addAndMap(new MemoryEntry("Low 8K expansion RAM", CPU, 0x2000,
	            0x2000, new ExpRamArea(0x2000)));
	    memory.addAndMap(new MemoryEntry("Console RAM", CPU, 0x8000, 0x0400,
	            new ConsoleRamArea()));
	    memory.addAndMap(new MemoryEntry("High 24K expansion RAM", CPU, 0xA000,
	            0x6000, new ExpRamArea(0x6000)));
		
	}

   protected void defineMmioMemory(TI994A machine) {
        
        this.memory.addAndMap(new MemoryEntry("Sound MMIO", CPU, 0x8400, 0x0400,
                new ConsoleSoundArea(soundMmio)));
        this.memory.addAndMap(new MemoryEntry("VDP Read MMIO", CPU, 0x8800, 0x0400,
                new ConsoleVdpReadArea(machine.getVdpMmio())));
        this.memory.addAndMap(new MemoryEntry("VDP Write MMIO", CPU, 0x8C00, 0x0400,
                new ConsoleVdpWriteArea(machine.getVdpMmio())));
        this.memory.addAndMap(new MemoryEntry("Speech Read MMIO", CPU, 0x9000, 0x0400,
                new ConsoleSpeechReadArea(speechMmio)));
        this.memory.addAndMap(new MemoryEntry("Speech Write MMIO", CPU, 0x9400, 0x0400,
                new ConsoleSpeechWriteArea(speechMmio)));
        this.memory.addAndMap(new MemoryEntry("GROM Read MMIO", CPU, 0x9800, 0x0400,
                new ConsoleGromReadArea(gplMmio)));
        this.memory.addAndMap(new MemoryEntry("GRAM Write MMIO", CPU, 0x9C00, 0x0400,
                new ConsoleGramWriteArea(gplMmio)));
		
	}


	public MemoryDomain getConsole() {
    	return CPU;
    }
    
    public GplMmio getGplMmio() {
    	return gplMmio;
    }
    
    public SoundMmio getSoundMmio() {
    	return soundMmio;
    }
    
    public SpeechMmio getSpeechMmio() {
    	return speechMmio;
    }
    
    public VdpMmio getVdpMmio() {
    	return vdpMmio;
    }
}
