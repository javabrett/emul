package v9t9.engine.compiler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import ejs.base.properties.IProperty;
import ejs.base.utils.HexUtils;


import v9t9.common.asm.IDecompileInfo;
import v9t9.common.asm.IHighLevelInstruction;
import v9t9.common.asm.InstTableCommon;
import v9t9.common.asm.RawInstruction;
import v9t9.common.compiler.ICompiledCode;
import v9t9.common.compiler.ICompiler;
import v9t9.common.cpu.AbortedException;
import v9t9.common.cpu.IExecutor;
import v9t9.common.memory.IMemoryEntry;
import v9t9.common.settings.Settings;

/** This represents a compiled block of code. */
public class CodeBlock implements ICompiledCode, v9t9.common.memory.IMemoryListener {
	IDecompileInfo highLevel;
    /** basic class name */
    String className;
    /** unique class name, for rebuilds */
    String uniqueClassName;
    /** tell whether this is being built */
    boolean building;
    /** ignore this block in the future? */
    boolean ignore;
    /** generated bytecode */
    byte[] bytecode;
    /** implementation */
    CompiledCode code;
    String baseName;
    private boolean running;
	private IExecutor exec;
	private DirectLoader loader;
	int addr;
	int size;
	IMemoryEntry ent;
	private ICompiler compiler;
    
    static int uniqueClassSuffix;
	private IProperty optimize;
	private IProperty optimizeStatus;

    public CodeBlock(ICompiler compiler, IExecutor exec, DirectLoader loader, IMemoryEntry ent, int addr, int size) {
        this.compiler = compiler;
		this.exec = exec;
        this.loader = loader;
        this.ent = ent;
        this.addr = addr;
        this.size = size;
        this.baseName = createBaseIdentifier(ent.getUniqueName()); 
        if (CompilerBase.DEBUG)
        	this.className = baseName;
        else
        	this.className = this.getClass().getName() + "$" + baseName + "_";
        exec.getCpu().getMachine().getMemory().addListener(this);
        this.highLevel = compiler.getHighLevelCode(ent);
        
	    optimize = Settings.get(exec.getCpu(), ICompiler.settingOptimize);
		optimizeStatus = Settings.get(exec.getCpu(), ICompiler.settingOptimizeStatus);

    }
    

	String createBaseIdentifier(String entName) {
        if (entName == null) {
            return HexUtils.toHex4(addr);
        }
        StringBuilder copy = new StringBuilder();
        for (int i = 0; i < entName.length(); i++) {
            char c = entName.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
				copy.append(c);
			} else {
				copy.append('_');
			}
        }
        return copy + HexUtils.toHex4(addr);
    }

    public boolean matches(IMemoryEntry ent_) {
        return this.ent == ent_;
    }

    private void getUniqueClassName() {
        // we may be reloading the class due to changes,
        // and we need a unique name for this
        uniqueClassName = className;
        try {
            if (Class.forName(className, true, loader) != null) {
                // if we get here, we already loaded this class and 
                // need to rename it
                System.out.print("Rewriting class file " + className);
                uniqueClassName += "$" + uniqueClassSuffix;
                uniqueClassSuffix++;
                System.out.println(" as " + uniqueClassName);
            } 
        } catch (ClassNotFoundException e) {
            
        }
    }
    
	/** Build the bytecode for a block of memory */
    boolean build() {
        if (bytecode == null) {
            if (ignore) {
				return false;
			}

            if (building) {
				return false;
			}
            
            getUniqueClassName();

            building = true;
            
            try {
                clear();
                System.out.println("compiling code block at >"
                        + HexUtils.toHex4(addr) + ":"
                        + HexUtils.toHex4(size) + "/" + ent.getUniqueName());

                highLevel.analyze();
                
                int numinsts = size / 2;
                RawInstruction insts[] = new RawInstruction[numinsts];
        	    
        	    for (int i = 0; i < numinsts; i++) {
        	    	insts[i] = highLevel.getInstruction(addr + i * 2);
        	    }
        	    
				if (optimize.getBoolean() 
        	    		&& optimizeStatus.getBoolean()) {
        	    	HLInstructionOptimizer.peephole_status(insts, numinsts);
        	    }
        	    
            	bytecode = compiler.compile(uniqueClassName, baseName,
            			highLevel,
            			insts, null);
            	
            	if (bytecode == null) {
            		ignore = true;
            		return false;
            	}
            } finally {
            	building = false;
            }
        }
        if (!load()) {
			return false;
		}
        return true;
    }

    void clear() {
        code = null;
        bytecode = null;
    }

     public boolean run() throws AbortedException {
        /* build the code if necessary... */
        if (!build()) {
			return false;
		}
            
        /* verify preconditions */
        if (!compiler.validCpuState()) {
			return false;
		}
        
        /* now execute */
        running = true;
        code.nInstructions = 0;
        code.nCycles = 0;
        boolean ret = true;
        AbortedException abort = null;
        try {
        	//System.out.println(code.getClass());
            ret = code.run();
        }
        catch (AbortedException e) {
        	exec.recordSwitch();
        	abort = e;
        }
        finally {
        	
        	// throws due to AbortedException usually 
	        running = false;
	        if (code != null) {
	        	if (code.nInstructions == 0 && abort == null) {
	        		ret = false;
	        	}
	        	exec.recordCompileRun(code.nInstructions, code.nCycles);
	        }
	        //System.out.println("invoked "+code.nInstructions+" at "+Utils.toHex4(origpc)+" to "+Utils.toHex4(exec.cpu.getPC()));
	        
	        if (abort == null && !ret) {
        		// target the PC later
        		int pc = exec.getCpu().getState().getPC() & 0xffff;
        		IHighLevelInstruction inst = highLevel.getLLInstructions().get(pc);
        		if (inst != null && inst.getInst().getInst() != InstTableCommon.Idata) {
        			if ((inst.getFlags() & IHighLevelInstruction.fStartsBlock) == 0) {
        				inst.setFlags(inst.getFlags() | IHighLevelInstruction.fStartsBlock);
        				/*
        				highLevel.markDirty();
        				if (origpc >= addr && origpc < addr + size && pc >= addr && pc < addr + size) {
        					clear();
        				}
        				*/
        			}
        		}
        	}
	        if (abort != null)
	        	throw abort;
        }
        return ret;
    }


    /**
     * @param exec
     * @return
     */
	private boolean load() {
        if (code != null) {
			return true;
		}
            
        if (bytecode == null) {
			return false;
		}

        // load and construct an instance of the class
        Class<?> clas = loader.load(uniqueClassName, bytecode);
        try {
            Constructor<?> cons = clas.getConstructor(new Class[] { IExecutor.class });
            code = (CompiledCode)cons.newInstance(new Object[] { exec });
            return true;
        } catch (InvocationTargetException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
            return false;
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
            return false;
        } catch (IllegalAccessException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
            return false;
        } catch (InstantiationException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
            return false;
        }
    }

    /* (non-Javadoc)
     * @see v9t9.Memory.Listener#notifyMemoryChanged(v9t9.MemoryEntry)
     */
    public void logicalMemoryMapChanged(IMemoryEntry entry) {
    	//System.out.println("Memory map changed");
        //clear();
        if (running) {
			throw new AbortedException();
		} 
    }
    public void physicalMemoryMapChanged(IMemoryEntry entry) {
    	//System.out.println("Memory map changed");
    	//clear();
    	if (running) {
    		throw new AbortedException();
    	} 
    }


}
