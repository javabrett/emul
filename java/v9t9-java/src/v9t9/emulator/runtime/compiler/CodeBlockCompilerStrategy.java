/**
 * 
 */
package v9t9.emulator.runtime.compiler;

import java.util.Map;
import java.util.TreeMap;

import org.ejs.coffee.core.utils.Pair;

import v9t9.emulator.runtime.cpu.Cpu;
import v9t9.emulator.runtime.cpu.Cpu9900;
import v9t9.emulator.runtime.cpu.Executor9900;
import v9t9.engine.memory.MemoryArea;
import v9t9.engine.memory.MemoryEntry;

/**
 * @author ejs
 *
 */
public class CodeBlockCompilerStrategy implements ICompilerStrategy {
	// map of address blocks to CodeBlocks
    //
    // this size seems best for JVM HotSpot compiler:  too few reduces ability to optimize
    // and too many makes classes that are too large to optimize
    static final short BLOCKSIZE = 0x100;   

    Map<Pair<MemoryArea, Integer>, CodeBlock> codeblocks;
    DirectLoader loader;

	private Executor9900 executor;

	private Compiler compiler;

	public CodeBlockCompilerStrategy(Executor9900 executor) {
		this.executor = executor;
		this.compiler = new Compiler((Cpu9900)executor.cpu);
        codeblocks = new TreeMap<Pair<MemoryArea, Integer>, CodeBlock>();
        loader = new DirectLoader();

	}
	
	public ICompiledCode getCompiledCode(Cpu cpu) {
        CodeBlock cb = getCodeBlock(((Cpu9900) cpu).getPC(), ((Cpu9900)cpu).getWP());
		return cb;
	}
	
   /**
     * @param ent
     * @return
     */
    public boolean isCompilable(MemoryEntry ent) {
        return ent != null && ent.getDomain().isEntryFullyMapped(ent) && ent.getArea().hasReadAccess()
         //&& !ent.area.hasWriteAccess()
        ; // for now
    }


	/**
	 * Get a code block to represent the eventual compiled code
	 * for this block.
	 * @param pc entry point PC
	 * @return code block or null if not compilable
	 */
	private CodeBlock getCodeBlock(int pc, short wp) {
	    MemoryEntry ent = executor.cpu.getConsole().getEntryAt(pc);
	    if (!isCompilable(ent)) {
			return null;
		}
	    
	    Integer blockaddr = new Integer(pc & ~(BLOCKSIZE - 1));
	    Pair<MemoryArea, Integer> key = new Pair<MemoryArea, Integer>(ent.getArea(), blockaddr);
	    CodeBlock cb;
	    if ((cb = codeblocks.get(key)) == null
	            || !cb.matches(ent)) {
	        cb = new CodeBlock(executor, loader, ent, blockaddr.shortValue(), BLOCKSIZE);
	        cb.compile(compiler);
	        executor.nCompiles++;
	        codeblocks.put(key, cb);
	    }
	    return cb;
	}

}
