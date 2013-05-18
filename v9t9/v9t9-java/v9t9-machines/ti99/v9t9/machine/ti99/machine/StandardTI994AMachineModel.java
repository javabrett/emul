/*
  StandardMachineModel.java

  (c) 2008-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.machine.ti99.machine;


import java.net.URL;

import v9t9.common.client.ISettingsHandler;
import v9t9.common.hardware.ISoundChip;
import v9t9.common.hardware.IVdpChip;
import v9t9.common.keyboard.KeyboardConstants;
import v9t9.common.machine.IMachine;
import v9t9.common.memory.IMemoryModel;
import v9t9.common.modules.IModuleManager;
import v9t9.common.settings.Settings;
import v9t9.engine.modules.ModuleManager;
import v9t9.engine.sound.SoundTMS9919;
import v9t9.engine.video.tms9918a.VdpTMS9918A;
import v9t9.machine.EmulatorMachinesData;
import v9t9.machine.ti99.dsr.emudisk.EmuDiskDsr;
import v9t9.machine.ti99.dsr.pcode.PCodeDsr;
import v9t9.machine.ti99.dsr.realdisk.RealDiskImageDsr;
import v9t9.machine.ti99.memory.TI994AStandardConsoleMemoryModel;

/**
 * @author ejs
 *
 */
public class StandardTI994AMachineModel extends BaseTI99MachineModel {

	public static final String ID = "StandardTI994A";

	public StandardTI994AMachineModel() {
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.MachineModel#getIdentifier()
	 */
	@Override
	public String getIdentifier() {
		return ID;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachineModel#getName()
	 */
	@Override
	public String getName() {
		return "TI-99/4A";
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.MachineModel#createMachine()
	 */
	@Override
	public IMachine createMachine(ISettingsHandler settings) {
		return new TI994A(settings, this);
	}
	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.MachineModel#getMemoryModel()
	 */
	public IMemoryModel createMemoryModel(IMachine machine) {
		return new TI994AStandardConsoleMemoryModel(machine);
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.hardware.MachineModel#getVdp()
	 */
	public IVdpChip createVdp(IMachine machine) {
		VdpTMS9918A vdp = new VdpTMS9918A(machine);
		return vdp;
	}
	
	public void defineDevices(IMachine machine_) {
		machine_.getKeyboardState().registerMapping(KeyboardConstants.KEY_BACKSPACE,
				new int[] { KeyboardConstants.KEY_ALT, 'S' });
		
		machine_.setKeyboardMapping(new StandardTI994AKeyboardMapping());
		
		if (machine_ instanceof TI99Machine) {
			TI99Machine machine = (TI99Machine) machine_;
			machine.setCru(new InternalCru9901(machine));
			
			EmuDiskDsr emudsr = new EmuDiskDsr(Settings.getSettings(machine), 
					machine.getEmulatedFileHandler().getFilesInDirectoryMapper());
			machine.getDsrManager().registerDsr(emudsr);
			RealDiskImageDsr diskdsr = new RealDiskImageDsr(machine, (short) 0x1100);
			machine.getDsrManager().registerDsr(diskdsr);
			
			PCodeDsr pcodedsr = new PCodeDsr(machine);
			machine.getDsrManager().registerDsr(pcodedsr);
		}
	}

	public ISoundChip createSoundChip(IMachine machine) {
		return new SoundTMS9919(machine);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachineModel#createModuleManager(v9t9.common.machine.IMachine)
	 */
	@Override
	public IModuleManager createModuleManager(IMachine machine) {
		return new ModuleManager(machine, "stock_modules.xml");
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachineModel#getDataURL()
	 */
	@Override
	public URL getDataURL() {
		return EmulatorMachinesData.getDataURL("ti99/");
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.machine.IMachineModel#isModelCompatible(java.lang.String)
	 */
	@Override
	public boolean isModelCompatible(String machineModel) {
		return machineModel.equals(ID);
	}
}