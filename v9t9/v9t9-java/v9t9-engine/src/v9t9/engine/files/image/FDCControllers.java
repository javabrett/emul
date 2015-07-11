/*
  FDCControllers.java

  (c) 2013 Ed Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.files.image;

import v9t9.common.dsr.IDeviceLabel;

/**
 * @author ejs
 *
 */
public enum FDCControllers implements IDeviceLabel {
	WDC1771("Single-density (TI)", "This selects the original TI disk image controller"),
	WDC1791("Double-density (Corcomp)", "This selects the Corcomp double-sided double-density disk controller");

	private final String label, tooltip;
	
	private FDCControllers(String label, String tooltip) {
		this.label = label;
		this.tooltip = tooltip;
	}
	/* (non-Javadoc)
	 * @see v9t9.common.dsr.IDeviceLabel#getLabel()
	 */
	@Override
	public String getLabel() {
		return label;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.dsr.IDeviceLabel#getTooltip()
	 */
	@Override
	public String getTooltip() {
		return tooltip;
	}
}
