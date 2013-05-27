/*
  IEmulatorContentSource.java

  (c) 2013 Ed Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.client;

import v9t9.common.machine.IMachine;


/**
 * This interface represents sources of content usable by the emulator,
 * either through a file, dragged-in content, etc., intended for
 * use in the client in interactive usage.
 * @author ejs
 *
 */
public interface IEmulatorContentSource {
	IEmulatorContentSource[] EMPTY = new IEmulatorContentSource[0];

	/**
	 * Get the machine
	 */
	IMachine getMachine();
	
	/**
	 * Get the content object
	 */
	Object getContent();

	/**
	 * @return
	 */
	String getLabel();
}
