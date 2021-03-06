/*
  DiskImageSetting.java

  (c) 2011-2015 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.files.image;

import java.net.URL;

import v9t9.common.client.ISettingsHandler;
import v9t9.common.dsr.DeviceEditorIdConstants;
import v9t9.common.files.IDiskDriveSetting;
import v9t9.common.settings.IconSettingProperty;
import v9t9.common.settings.SettingSchema;
import v9t9.engine.files.directory.EmuDiskSettings;
import ejs.base.properties.IProperty;

public class DiskImageSetting extends IconSettingProperty implements IDiskDriveSetting {
	private IProperty realDsrEnabled;
	private IProperty emuDsrEnabled;
	private int drive;

	public DiskImageSetting(ISettingsHandler settings, SettingSchema dsrEnabled, 
			String name, Object storage, URL iconPath) {
		super(new SettingSchema(ISettingsHandler.TRANSIENT,
				name, "DSK" + name.charAt(name.length() - 1) + " Image",
				"Specify the full path of the image for this disk.\n\n"+
				"The extension selects the image type when creating a new image.\n\nUse *.dsk for sector-image disks and *.trk for track image disks.",
				DeviceEditorIdConstants.ID_DISK_IMAGE,
				storage, String.class), iconPath);
		
		drive = Integer.parseInt(name.substring(name.length() - 1));
		
		realDsrEnabled = settings.get(dsrEnabled);
		emuDsrEnabled = settings.get(EmuDiskSettings.emuDiskDsrEnabled);
		addEnablementDependency(emuDsrEnabled);
		addEnablementDependency(realDsrEnabled);
		addEnablementDependency(settings.get(RealDiskSettings.diskImageRealTime));
		addEnablementDependency(settings.get(RealDiskSettings.diskImageDebug));
	}

	@Override
	public boolean isEnabled() {
		if (!realDsrEnabled.getBoolean())
			return false;
		if (!emuDsrEnabled.getBoolean())
			return true;
		
		// only DSK1 and DSK2 are real disks if emu disk also enabled
		return getName().compareTo(RealDiskSettings.getDiskImageSetting(3)) < 0;
	}
	
	public int getDrive() {
		return drive;
	}
}