/*
  IDemoActor.java

  (c) 2012 Edward Swartz

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.
 
  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  General Public License for more details.
 
  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
 */
package v9t9.common.demos;

import v9t9.common.machine.IMachine;

/**
 * This is the base interface implemented by an actor in the emulator
 * that wants to participate in a demo.
 * @author ejs
 *
 */
public interface IDemoActor {
	/**
	 * Get the identifier for the {@link IDemoEvent#getIdentifier()}
	 * created and consumed by this actor.
	 * @return
	 */
	String getEventIdentifier();
	
	/**
	 * Set up the actor for this machine
	 * @param machine
	 */
	void setup(IMachine machine);
}
