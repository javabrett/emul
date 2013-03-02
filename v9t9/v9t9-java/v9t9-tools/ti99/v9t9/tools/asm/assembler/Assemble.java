/*
  Assemble.java

  (c) 2008-2013 Edward Swartz

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
package v9t9.tools.asm.assembler;

import gnu.getopt.Getopt;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import v9t9.common.files.PathFileLocator;
import v9t9.common.memory.IMemoryEntry;
import v9t9.common.memory.MemoryEntryInfo;
import v9t9.common.settings.SettingSchemaProperty;
import v9t9.engine.memory.MemoryEntryInfoBuilder;
import v9t9.engine.memory.MemoryEntryFactory;
import v9t9.tools.asm.assembler.inst9900.Assembler9900;

public class Assemble {

    private static final String PROGNAME = Assemble.class.getName();
	private static MemoryEntryFactory memoryEntryFactory;
    
    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        Assembler9900 assembler = new Assembler9900();
        
        assembler.setList(null);
        
        PathFileLocator locator = new PathFileLocator();
		memoryEntryFactory = new MemoryEntryFactory(null, assembler.getMemory(), locator);
        locator.setReadWritePathProperty(new SettingSchemaProperty("Output", "."));
        
        boolean selectedProcessor = false;
        int romStart = 0, romSize = 0x2000;
        
        Getopt getopt = new Getopt(PROGNAME, args, "?r:m:d:g:l:D:e:v92");
        int opt;
        while ((opt = getopt.getopt()) != -1) {
            switch (opt) {
            case '?':
                help();
                break;
            case 'r': {
            	String file = getopt.getOptarg();
            	int size = romSize;
            	int idx = file.lastIndexOf('#');
            	if (idx >= 0) {
            		size = Integer.parseInt(file.substring(idx+1));
            		file = file.substring(0, idx);
            	}
            	assembler.addMemoryEntry(createMemoryEntry(romStart, size, "CPU ROM", file));
            	break;
            }
            case 'e': {
            	assembler.addMemoryEntry(
            			createMemoryEntry(romStart, romSize * 2, "CPU ROM", getopt.getOptarg()));
            	break;
            }
            case 'm': {
            	assembler.addMemoryEntry(
            			createMemoryEntry(0x6000, 0x2000, "Module ROM", getopt.getOptarg()));
            	break;
            }
            case 'd': {
            	assembler.addMemoryEntry(
            			createMemoryEntry(0x4000, 0x2000, "DSR ROM", getopt.getOptarg()));
            	break;  
            }
            case 'g': {
            	String file = getopt.getOptarg();
            	int size = 0x6000;
            	int idx = file.lastIndexOf('#');
            	if (idx >= 0) {
            		size = Integer.parseInt(file.substring(idx+1));
            		file = file.substring(0, idx);
            	}
            	assembler.addMemoryEntry(
            			createMemoryEntry(0x0000, size, "GROM", file));
            	break; 
            }
            case 'l':
            	String name = getopt.getOptarg();
            	if (name.equals("-"))
            		assembler.setList(System.out);
            	else
	            	try {
	            		assembler.setList(new PrintStream(new File(name)));
	            	} catch (IOException e) {
	            		System.err.println("Failed to create list file: " + e.getMessage());
	            		System.exit(1);
	            	}
            	break;   
            case 'D': {
            	String equ = getopt.getOptarg();
            	assembler.defineEquate(equ);
            	break;    
            }
            case '9':
            	assembler.setProcessor(Assembler.PROC_9900);
            	romStart = 0;
            	romSize = 0x2000;
            	selectedProcessor = true;
            	break;
            case '2':
            	assembler.setProcessor(Assembler.PROC_MFP201);
            	romStart = 0xf000;
            	romSize = 0x1000;
            	selectedProcessor = true;
            	break;
            default:
            	//throw new AssertionError();
    
            }
        }
        
        if (!selectedProcessor) {
        	assembler.setProcessor(Assembler.PROC_9900);
        }
        
        if (getopt.getOptind() < args.length) {
        	FileContentEntry entry;
        	String name = args[getopt.getOptind()]; 
        	try {
        		entry = new FileContentEntry(new File(name));
        		assembler.pushContentEntry(entry);
        		getopt.setOptind(getopt.getOptind() + 1);
        	} catch (IOException e) {
        		System.err.println(PROGNAME + ": failed to read: " + name);
                System.exit(1);
            }
        } else {
        	System.err.println(PROGNAME + ": no files specified");
        }
        
        if (!assembler.assemble()) {
        	System.err.println("Errors during assembly");
        	System.exit(1);
        }
    }

	private static IMemoryEntry createMemoryEntry(int romStart,
			int size, String string, String file) throws IOException {
		MemoryEntryInfo info = MemoryEntryInfoBuilder.wordMemoryEntry()
			.withAddress(romStart).withSize(size).withFilename(file).
			storable(true).create(string);

		return memoryEntryFactory.newMemoryEntry(info);
	}

	private static void help() {
        System.out
                .println("\n"
                        + "9900 Assembler v2.0\n"
                        + "\n" 
                        +
                        PROGNAME + " <input file> [-r|e <console ROM output>[#size]] [-m <module ROM output>]\n" +
           			 "[-d <DSR ROM output>] [-g <console GROM output>[#size]] [-Dequ=val] [-l<list file>]\n" +
           			 "\n"+
           			 "-r|e saves the default 8k/16k memory block at >0000.\n" +
           			 "-m saves the 8k memory block at >6000.\n" +
           			 "-d saves the 8k memory block at >4000.\n" +
           			 "-g saves the default 24k memory block at >0000.\n"+
           			 "-l sends a listing to the given file (- for stdout)");

    }

}
