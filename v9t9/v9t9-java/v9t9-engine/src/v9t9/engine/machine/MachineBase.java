/*
  MachineBase.java

  (c) 2008-2013 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.machine;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import v9t9.common.asm.IRawInstructionFactory;
import v9t9.common.client.IClient;
import v9t9.common.client.IKeyboardHandler;
import v9t9.common.client.ISettingsHandler;
import v9t9.common.cpu.CpuMetrics;
import v9t9.common.cpu.ICpu;
import v9t9.common.cpu.ICpuMetrics;
import v9t9.common.cpu.IExecutor;
import v9t9.common.demos.IDemoHandler;
import v9t9.common.demos.IDemoManager;
import v9t9.common.events.IEventNotifier;
import v9t9.common.events.NotifyEvent;
import v9t9.common.events.IEventNotifier.Level;
import v9t9.common.files.DataFiles;
import v9t9.common.files.IEmulatedFileHandler;
import v9t9.common.files.IFileExecutionHandler;
import v9t9.common.files.IPathFileLocator;
import v9t9.common.files.PathFileLocator;
import v9t9.common.hardware.ICruChip;
import v9t9.common.hardware.ISoundChip;
import v9t9.common.hardware.ISpeechChip;
import v9t9.common.hardware.IVdpChip;
import v9t9.common.keyboard.IKeyboardMapping;
import v9t9.common.keyboard.IKeyboardModeListener;
import v9t9.common.keyboard.IKeyboardState;
import v9t9.common.keyboard.NullKeyboardHandler;
import v9t9.common.machine.IMachine;
import v9t9.common.machine.IMachineModel;
import v9t9.common.machine.TerminatedException;
import v9t9.common.memory.IMemory;
import v9t9.common.memory.IMemoryDomain;
import v9t9.common.memory.IMemoryEntry;
import v9t9.common.memory.IMemoryModel;
import v9t9.common.modules.IModule;
import v9t9.common.modules.IModuleManager;
import v9t9.common.settings.SettingSchemaProperty;
import v9t9.engine.demos.DemoManager;
import v9t9.engine.events.RecordingEventNotifier;
import v9t9.engine.files.EmulatedFileHandler;
import v9t9.engine.files.directory.DiskDirectoryMapper;
import v9t9.engine.files.directory.EmuDiskSettings;
import v9t9.engine.files.image.DiskImageMapper;
import v9t9.engine.keyboard.KeyboardState;
import ejs.base.properties.IProperty;
import ejs.base.properties.IPropertyListener;
import ejs.base.settings.ISettingSection;
import ejs.base.timer.FastTimer;
import ejs.base.utils.ListenerList;
import ejs.base.utils.ListenerList.IFire;

/** Encapsulate all the information about a running emulated machine.
 * @author ejs
 */
abstract public class MachineBase implements IMachine {

	private static final Logger log = Logger.getLogger(MachineBase.class);
	
	private volatile boolean alive;

	private final int cpuTicksPerSec = 100;
	
	protected IMemory memory;
	protected IMemoryDomain console;
	protected ICpu cpu;
	protected IExecutor executor;
	protected IClient client;

	protected Timer timer;
	protected FastTimer fastTimer;
	private IVdpChip vdp;

	protected boolean allowInterrupts;
	protected TimerTask clientTask;
	protected Runnable baseTimingTask;
	protected IMemoryModel memoryModel;
	protected TimerTask videoUpdateTask;
	protected Thread machineRunner;
	protected Thread videoRunner;
	protected int throttleCount;
	protected IKeyboardState keyboardState;
	protected ISoundChip sound;
	protected ISpeechChip speech;
	private ICpuMetrics cpuMetrics;

	protected TimerTask memorySaverTask;
	protected IModuleManager moduleManager;

	private ICruChip cru;

	protected RecordingEventNotifier recordingNotifier = new RecordingEventNotifier();
	private IRawInstructionFactory instructionFactory;
	private final IMachineModel machineModel;
	private IPropertyListener pauseListener;

	protected IProperty pauseMachine;
	private final ISettingsHandler settings;
	private IPathFileLocator locator;

	private IDemoHandler demoHandler;
	private IDemoManager demoManager;
	private IKeyboardHandler keyboardHandler;
	private IKeyboardMapping keyboardMapping;
	private ListenerList<IKeyboardModeListener> keyboardModeListeners;
	
	private IEmulatedFileHandler emulatedFileHandler;
	

	
    public MachineBase(ISettingsHandler settings, IMachineModel machineModel) {
    	this.settings = settings;
		pauseMachine = settings.get(settingPauseMachine);
    	
    	this.machineModel = machineModel;
    	
    	locator = new PathFileLocator();
    	locator.setReadWritePathProperty(settings.get(DataFiles.settingStoredRamPath));
    	locator.addReadOnlyPathProperty(settings.get(DataFiles.settingBootRomsPath));
    	locator.addReadOnlyPathProperty(settings.get(DataFiles.settingUserRomsPath));
    	locator.addReadOnlyPathProperty(settings.get(DataFiles.settingShippingRomsPath));
    	try {
    		if (getModel().getDataURL() != null) {
    			locator.addReadOnlyPathProperty(new SettingSchemaProperty("BuiltinPath", Collections.singletonList(
					//"jar:file:/home/ejs/devel/emul/v9t9/build/bin/v9t9/v9t9j.jar!/ti99/"
					getModel().getDataURL().toURI().toString()
					)));
    		}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
    	
    	this.memoryModel = machineModel.createMemoryModel(this);
    	this.memory = memoryModel.getMemory();
    	this.console = memoryModel.getConsole();

    	timer = new Timer(true);
    	fastTimer = new FastTimer("Machine");

    	keyboardModeListeners = new ListenerList<IKeyboardModeListener>();
    	init(machineModel);

    	DiskDirectoryMapper fileMapper = new DiskDirectoryMapper(settings.get(EmuDiskSettings.emuDiskDsrEnabled));
    	DiskImageMapper imageMapper = new DiskImageMapper(settings);
    	IFileExecutionHandler execHandler = createFileExecutionHandler();
    	emulatedFileHandler = new EmulatedFileHandler(settings, fileMapper, imageMapper, execHandler);
    	
    	machineModel.defineDevices(this);
    	
    	if (cpu != null) {
    		cpuMetrics = new CpuMetrics();
    		executor = cpu.createExecutor();
    		executor.setMetrics(cpuMetrics);
    	}

    	pauseListener = new IPropertyListener() {
    		
    		public void propertyChanged(IProperty setting) {
    			if (executor != null)
    				executor.setExecuting(!setting.getBoolean());
    		}
    		
    	};
		settings.get(settingPauseMachine).addListenerAndFire(pauseListener);
    	//executor.addInstructionListener(new DebugConditionListener(cpu));
    	//executor.addInstructionListener(new DebugConditionListenerF99b(cpu));

	}

	abstract protected IFileExecutionHandler createFileExecutionHandler();

	/**
	 * @return the settings
	 */
	public ISettingsHandler getSettings() {
		return settings;
	}

	protected void init(IMachineModel machineModel) {
		demoManager = new DemoManager(this);
		
    	vdp = machineModel.createVdp(this);
    	sound = machineModel.createSoundChip(this);
    	speech = machineModel.createSpeechChip(this);
    	memoryModel.initMemory(this);
    	
    	moduleManager = machineModel.createModuleManager(this);
    	
    	cpu = machineModel.createCPU(this); 
		keyboardState = new KeyboardState(this);
		
		keyboardHandler = new NullKeyboardHandler(keyboardState, this);

    	this.instructionFactory = cpu != null ? cpu.getInstructionFactory() : null;
	}
    
	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#notifyEvent(v9t9.emulator.common.IEventNotifier.Level, java.lang.String)
	 */
	@Override
	public void notifyEvent(IEventNotifier.Level level, String string) {
		if (client != null)
			client.getEventNotifier().notifyEvent(this, level, string);
		else
			recordingNotifier.notifyEvent(this, level, string);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#notifyEvent(v9t9.emulator.common.NotifyEvent)
	 */
	@Override
	public void notifyEvent(NotifyEvent event) {
		if (client != null)
			client.getEventNotifier().notifyEvent(event);
		else
			recordingNotifier.notifyEvent(event);
	}

	/* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
	protected void finalize() throws Throwable {
    	pauseMachine.removeListener(pauseListener);
    	
    	if (recordingNotifier != null) {
    		NotifyEvent event;
    		while ((event = recordingNotifier.getNextEvent()) != null) {
    			
    			System.err.println(event);
    			
				System.err.println(event);
				if (event.level == Level.INFO)
					log.debug(event);
				else if (event.level == Level.WARNING)
					log.warn(event);
				else
					log.error(event);

    		}
    	}
        super.finalize();
        client = null;
        memory = null;
        executor = null;
    }
    
    /* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#getMemory()
	 */
    @Override
	public IMemory getMemory() {
        return memory;
    }
    
    /* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#isAlive()
	 */
    @Override
	public boolean isAlive() {
        return alive;
    }
    
    /* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#start()
	 */
    @Override
	public void start() {
    	allowInterrupts = true;
    	
        alive = true;
        
        if (client != null)
        	client.start();

        // the base machine running task 
        baseTimingTask = new Runnable() {

			@Override
        	public void run() {
				if (client != null)
					client.tick();
				executor.tick();
        	}
        };
        getFastMachineTimer().scheduleTask(baseTimingTask, getTicksPerSec());


    	executor.start();

    }
    
	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#stop()
	 */
    @Override
	public void stop() {
    	alive = false;
    	executor.stop();
		timer.cancel();
        fastTimer.cancel();
		
		memory.save();        
		
        throw new TerminatedException();
    }

    
	/* (non-Javadoc)
	 * @see v9t9.common.settings.IBaseMachine#reset()
	 */
	@Override
	public void reset() {
		keyboardState.resetKeyboard();
		executor.getCompilerStrategy().reset();
		
		IMemoryDomain domain = getMemory().getDomain(IMemoryDomain.NAME_CPU);
		for (IMemoryEntry entry : domain.getFlattenedMemoryEntries()) {
			if (entry.isVolatile()) {
				int addr = entry.mapAddress(entry.getAddr());
				//System.out.println("Wiping " + entry +  "@" + HexUtils.toHex4(addr));
				for (int i = 0; i < entry.getSize(); i+= 2)
					domain.writeWord(i + addr, (short) 0);
			}
		}
		
		if (cru != null)
			cru.reset();
				
		cpu.reset();
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#getCpu()
	 */
	@Override
	public ICpu getCpu() {
        return cpu;
    }
    /* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#setCpu(v9t9.emulator.runtime.cpu.Cpu)
	 */
    @Override
	public void setCpu(ICpu cpu) {
        this.cpu = cpu;
    }
    /* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#getClient()
	 */
    @Override
	public IClient getClient() {
        return client;
    }
    /* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#setClient(v9t9.engine.Client)
	 */
    @Override
	public void setClient(IClient client) {
        this.client = client;
        if (client != null) {
        	if (recordingNotifier != null) {
        		NotifyEvent event;
        		while ((event = recordingNotifier.getNextEvent()) != null) {
        			client.getEventNotifier().notifyEvent(event);
        		}
        		recordingNotifier = null;
        	}
        } else {
        	recordingNotifier = new RecordingEventNotifier();
        }
    }
    /* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#getExecutor()
	 */
    @Override
	public IExecutor getExecutor() {
        return executor;
    }
    /* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#setExecutor(v9t9.emulator.runtime.cpu.Executor)
	 */
    @Override
	public void setExecutor(IExecutor executor) {
        this.executor = executor;
    }
    
    /* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#getConsole()
	 */
    @Override
	public IMemoryDomain getConsole() {
		return console;
	}
    
    /* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#getMemoryModel()
	 */
    @Override
	public IMemoryModel getMemoryModel() {
        return memoryModel;
    }

    /* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#getKeyboardState()
	 */
    @Override
	public IKeyboardState getKeyboardState() {
		return keyboardState;
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#saveState(v9t9.base.core.settings.ISettingSection)
	 */
	@Override
	public synchronized void saveState(ISettingSection settings) {
		
		boolean wasExecuting = executor.setExecuting(false);
		boolean wasPaused = setPaused(true);
		
		settings.put("Class", getClass());
		
		doSaveState(settings);
		
		this.settings.get(DataFiles.settingUserRomsPath).saveState(settings);
		
		ISettingSection workspace = settings.addSection("Workspace");
		this.settings.getMachineSettings().save(workspace);
		//WorkspaceSettings.CURRENT.saveState(settings);
		
		setPaused(wasPaused);
		executor.setExecuting(wasExecuting);
	}


	protected void doSaveState(ISettingSection settings) {
		settings.put("MachineModel", machineModel.getIdentifier());
		
		cpu.saveState(settings.addSection("CPU"));
		memory.saveState(settings.addSection("Memory"));
		vdp.saveState(settings.addSection("VDP"));
		if (sound != null)
			sound.saveState(settings.addSection("Sound"));
		if (speech != null)
			speech.saveState(settings.addSection("Speech"));
		if (moduleManager != null)
			moduleManager.saveState(settings.addSection("Modules"));

		if (cru != null)
			cru.saveState(settings.addSection("CRU"));

	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#loadState(v9t9.base.core.settings.ISettingSection)
	 */
	@Override
	public synchronized void loadState(ISettingSection section) {
		/*
		machineRunner.interrupt();
		videoRunner.interrupt();
		timer.cancel();
		cpuTimer.cancel();
		videoTimer.cancel();
		*/
		
		boolean wasExecuting = executor.setExecuting(false);
		
		settings.get(DataFiles.settingUserRomsPath).loadState(section);
		
		doLoadState(section);
		
		//Executor.settingDumpFullInstructions.setBoolean(true);
		
		//start();

		executor.setExecuting(wasExecuting);
	}


	protected void doLoadState(ISettingSection section) {
		memory.getModel().resetMemory();
		if (moduleManager != null)
			moduleManager.loadState(section.getSection("Modules"));
		memory.loadState(section.getSection("Memory"));
		cpu.loadState(section.getSection("CPU"));
		vdp.loadState(section.getSection("VDP"));
		if (sound != null)
			sound.loadState(section.getSection("Sound"));
		if (speech != null)
			speech.loadState(section.getSection("Speech"));
		keyboardState.resetKeyboard();
		keyboardState.resetJoystick();
		if (cru != null)
			cru.loadState(section.getSection("CRU"));
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#getSound()
	 */
	@Override
	public ISoundChip getSound() {
		return sound;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.machine.IMachine#getSpeech()
	 */
	@Override
	public ISpeechChip getSpeech() {
		return speech;
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#getCpuTicksPerSec()
	 */
	@Override
	public int getTicksPerSec() {
		return cpuTicksPerSec;
	}


	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#isExecuting()
	 */
	@Override
	public boolean isExecuting() {
		return executor.isExecuting();
	}

	/* (non-Javadoc)
	 * @see v9t9.common.machine.IBaseMachine#isPaused()
	 */
	@Override
	public boolean isPaused() {
		return pauseMachine.getBoolean();
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IBaseMachine#setPaused(boolean)
	 */
	@Override
	public boolean setPaused(boolean paused) {
		boolean wasPaused = pauseMachine.getBoolean();
		pauseMachine.setBoolean(paused);
		return wasPaused;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#asyncExec(java.lang.Runnable)
	 */
	@Override
	public void asyncExec(Runnable runnable) {
		executor.asyncExec(runnable);
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#getCpuMetrics()
	 */
	@Override
	public ICpuMetrics getCpuMetrics() {
		return cpuMetrics;
	}

	public IModuleManager getModuleManager() {
		return moduleManager;
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#getVdp()
	 */
	@Override
	public IVdpChip getVdp() {
		return vdp;
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#getInstructionFactory()
	 */
	@Override
	public IRawInstructionFactory getInstructionFactory() {
		return instructionFactory;
	}


	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#getModel()
	 */
	@Override
	public IMachineModel getModel() {
		return machineModel;
	}


	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#keyStateChanged()
	 */
	@Override
	public void keyStateChanged() {
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IMachine#getMachineTimer()
	 */
	@Override
	public Timer getMachineTimer() {
		return timer;
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.common.IBaseMachine#interrupt()
	 */
	@Override
	public void interrupt() {
		executor.interruptExecution();
	}

	/**
	 * @return the cruAccess
	 */
	public ICruChip getCru() {
		return cru;
	}
	/**
	 * @param cru the cruAccess to set
	 */
	public void setCru(ICruChip cru) {
		this.cru = cru;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachine#getFileHandler()
	 */
	@Override
	public IEmulatedFileHandler getEmulatedFileHandler() {
		return emulatedFileHandler;
	}
	
	public void setFileHandler(IEmulatedFileHandler fileHandler) {
		this.emulatedFileHandler = fileHandler;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachine#getPathFileLocator()
	 */
	@Override
	public IPathFileLocator getRomPathFileLocator() {
		return locator;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachine#getEventNotifier()
	 */
	@Override
	public IEventNotifier getEventNotifier() {
		return client != null ? client.getEventNotifier() : recordingNotifier;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachine#getDemoHandler()
	 */
	@Override
	public IDemoHandler getDemoHandler() {
		return demoHandler;
	}
	/**
	 * @param demoHandler the demoHandler to set
	 */
	public void setDemoHandler(IDemoHandler demoHandler) {
		this.demoHandler = demoHandler;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachine#getDemoManager()
	 */
	@Override
	public IDemoManager getDemoManager() {
		return demoManager;
	}
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachine#setDemoManager(v9t9.common.demo.IDemoManager)
	 */
	@Override
	public void setDemoManager(IDemoManager manager) {
		this.demoManager = manager;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IBaseMachine#getFastMachineTimer()
	 */
	@Override
	public FastTimer getFastMachineTimer() {
		return fastTimer;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachine#getKeyboardMapping()
	 */
	@Override
	public IKeyboardMapping getKeyboardMapping() {
		return keyboardMapping;
	}
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachine#setKeyboardMapping(v9t9.common.keyboard.IKeyboardMapping)
	 */
	@Override
	public void setKeyboardMapping(IKeyboardMapping mapping) {
		this.keyboardMapping = mapping;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachine#setKeyboardHandler(v9t9.common.client.IKeyboardHandler)
	 */
	@Override
	public void setKeyboardHandler(IKeyboardHandler keyboardHandler) {
		this.keyboardHandler = keyboardHandler;
		
	}
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachine#getKeyboardHandler()
	 */
	@Override
	public IKeyboardHandler getKeyboardHandler() {
		return keyboardHandler;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachine#addKeyboardModeListener(v9t9.common.keyboard.IKeyboardModeListener)
	 */
	@Override
	public synchronized void addKeyboardModeListener(IKeyboardModeListener listener) {
		keyboardModeListeners.add(listener);
		if (!keyboardModeListeners.isEmpty()) {
			attachKeyboardModeListener();
		}
		
	}
	

	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachine#removeKeyboardModeListener(v9t9.common.keyboard.IKeyboardModeListener)
	 */
	@Override
	public synchronized void removeKeyboardModeListener(IKeyboardModeListener listener) {
		keyboardModeListeners.remove(listener);
		if (keyboardModeListeners.isEmpty()) {
			detachKeyboardModeListener();
		}
	}
	
	/**
	 * Start listening for keyboard mode changes
	 * @see #fireKeyboardModeChanged(String)
	 */
	protected void attachKeyboardModeListener() {
		
	}
	/**
	 * Stop listening for keyboard mode changes
	 */
	protected void detachKeyboardModeListener() {
		
	}

	protected void fireKeyboardModeChanged(final String modeId) {
		keyboardModeListeners.fire(new IFire<IKeyboardModeListener>() {

			@Override
			public void fire(IKeyboardModeListener listener) {
				listener.keyboardModeChanged(modeId);
			}
		});
	}
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachine#getKeyboardMode()
	 */
	@Override
	public String getKeyboardMode() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachine#scanModules(java.net.URI, java.io.File)
	 */
	@Override
	public Collection<IModule> scanModules(URI databaseURI, File base) {
		return Collections.emptyList();
	}
}


