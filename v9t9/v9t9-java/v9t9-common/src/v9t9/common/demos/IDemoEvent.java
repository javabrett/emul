/*
  IDemoEvent.java

  (c) 2012-2013 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.demos;

/**
 * This interface underlies events that represent the behavior
 * of a demo.
 * @author ejs
 *
 */
public interface IDemoEvent {

	/**
	 * Get a unique identifier for the event, which can identify it
	 * for purposes of serialization and playback
	 * @return
	 */
	String getIdentifier();
	
}
