/*
  RealDiskDsrSettings.java

  (c) 2011-2013 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.machine.ti99.dsr.rs232;

import v9t9.common.client.ISettingsHandler;
import v9t9.common.settings.SettingSchema;
import v9t9.engine.dsr.rs232.RS232Controllers;

/**
 * @author ejs
 *
 */
public class RS232Settings {

	public static final SettingSchema settingRS232Debug = new SettingSchema(
			ISettingsHandler.MACHINE,
			"RS232Debug",
			"Debug RS232 Support",
			"When set, log RS232 operation information.",
			Boolean.TRUE
			);

	public static final SettingSchema rs232Controller = new SettingSchema(
			ISettingsHandler.MACHINE,
			"RS232Controller",
			"RS232 Controller",
			"Select the controller to emulate for RS232 support.  This affects which DSR ROMs are " +
			"used and whether parallel ports (PIO) are available.",
					RS232Controllers.RS232_ONLY
			);

}
