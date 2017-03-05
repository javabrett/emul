/*
  IControllerHandler.java

  (c) 2017 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.gui.client.swt;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import v9t9.common.keyboard.JoystickRole;

public interface IControllerHandler {
	
	static class State {
		public int x;
		public int y;
		public boolean button;
	}
	
	Controller getController();
	Component getComponent();
	JoystickRole getRole();
	
	void setJoystick(int joy, State state);
	
	boolean isFailedLast();
	void setFailedLast(boolean failedLast);
}