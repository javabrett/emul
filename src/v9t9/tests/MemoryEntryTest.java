/*
 * (c) Ed Swartz, 2005
 * 
 * Created on Dec 16, 2004
 *
 */
package v9t9.tests;

import junit.framework.TestCase;
import v9t9.engine.memory.ByteMemoryArea;
import v9t9.engine.memory.MemoryArea;
import v9t9.engine.memory.MemoryDomain;
import v9t9.engine.memory.MemoryEntry;
import v9t9.engine.memory.ZeroWordMemoryArea;


/**
 * @author ejs
 */
public class MemoryEntryTest extends TestCase {
    private MemoryDomain CPU;
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(MemoryEntryTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
	protected void setUp() throws Exception {
        super.setUp();
        CPU = new MemoryDomain();
   }

    /*
     * @see TestCase#tearDown()
     */
    @Override
	protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMemoryEntry() {
        MemoryArea anArea = new ZeroWordMemoryArea(); 
        MemoryEntry ent = new MemoryEntry("most mem", CPU, 2048, MemoryDomain.PHYSMEMORYSIZE-2048, 
                anArea);
        assertTrue(ent != null);
        assertEquals(ent.name, "most mem");
        //assertTrue(!ent.isMapped());
        assertTrue(ent.addr == 2048);
        assertTrue(ent.size == MemoryDomain.PHYSMEMORYSIZE-2048);
        assertTrue(ent.domain == CPU);

        boolean bCaught;
        
        /* illegal start addr */
        bCaught = false;
        try {
            ent = new MemoryEntry("error 1", CPU, 1234, 1024, anArea);
        } catch (Throwable e) {
            bCaught = true;
        }
        assertTrue(bCaught);
        
        /* illegal size #2 */
        bCaught = false;
        try {
            ent = new MemoryEntry("error 1", CPU, 1024, 123102, anArea);
        } catch (Throwable e) {
            bCaught = true;
        }
        assertTrue(bCaught);

        /* illegal size for normal entry */
        bCaught = false;
        try {
            ent = new MemoryEntry("error 1", CPU, 1024, -1024, anArea);
        } catch (Throwable e) {
            bCaught = true;
        }
        assertTrue(bCaught);

        /* null params */
        bCaught = false;
        try {
            ent = new MemoryEntry("error 1", CPU, 1024, 1024, null);
        } catch (Throwable e) {
            bCaught = true;
        }
        assertTrue(bCaught);

        /* null params */
        bCaught = false;
        try {
            ent = new MemoryEntry("error 1", null, 1024, 1024, anArea);
        } catch (Throwable e) {
            bCaught = true;
        }
        assertTrue(bCaught);
    }

    public void testIsMapped() {
        MemoryArea anArea = new ZeroWordMemoryArea(); 
        MemoryEntry ent = new MemoryEntry("most mem", CPU, 2048, MemoryDomain.PHYSMEMORYSIZE-2048, 
                anArea);
        CPU.mapEntry(ent);
        assertTrue(CPU.isEntryFullyMapped(ent));
        assertTrue(CPU.isEntryMapped(ent));
    }

    public void testMapAndUnmap() {
        MemoryArea zArea = new ZeroWordMemoryArea(); 
        MemoryEntry zEnt = new MemoryEntry("all mem", CPU, 0, MemoryDomain.PHYSMEMORYSIZE, 
                zArea);
        assertTrue(zEnt != null);
        CPU.mapEntry(zEnt);
        
        ByteMemoryArea anArea = new ByteMemoryArea();
        anArea.memory = new byte[1024];
        for (int i = 0; i < anArea.memory.length; i++) {
            anArea.memory[i] = (byte)0xaa;
        }
        anArea.read = anArea.memory;
        anArea.write = anArea.memory;
        MemoryEntry ent = new MemoryEntry("block", CPU, 2048, 1024, 
                anArea);
        assertTrue(ent != null);
        CPU.mapEntry(ent);
        
        byte val = CPU.readByte(2048); 
        assertTrue(val == (byte)0xaa);
        assertTrue(CPU.readByte(2047) == 0x0);
        assertTrue(CPU.readByte(2048+1024) == 0x0);

        /* allow map on top */
        CPU.mapEntry(zEnt);
        assertTrue(CPU.isEntryMapped(zEnt));
        assertTrue(CPU.isEntryFullyMapped(zEnt));
        assertTrue(CPU.readByte(2048) == 0x0);
        assertFalse(CPU.isEntryFullyMapped(ent));
        assertTrue(CPU.isEntryMapped(ent));
        
        /* unmapping leaves old stuff */
        CPU.unmapEntry(zEnt);
        assertFalse(CPU.isEntryMapped(zEnt));
        assertFalse(CPU.isEntryFullyMapped(zEnt));
        assertTrue(CPU.isEntryFullyMapped(ent));
        assertTrue(CPU.isEntryMapped(ent));
        assertEquals((byte)0xaa, CPU.readByte(2048));
        
        CPU.unmapEntry(ent);
        assertFalse(CPU.isEntryFullyMapped(zEnt));
        assertFalse(CPU.isEntryMapped(zEnt));
        assertEquals(0x0, CPU.readByte(2048));
        
    }


}
