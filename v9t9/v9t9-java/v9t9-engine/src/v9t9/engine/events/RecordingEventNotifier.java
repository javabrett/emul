/*
  RecordingEventNotifier.java

  (c) 2010-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.events;

import v9t9.common.events.BaseEventNotifier;
import v9t9.common.events.NotifyEvent;

/**
 * @author ejs
 *
 */
public class RecordingEventNotifier extends BaseEventNotifier {

	public RecordingEventNotifier() {
		super();
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.events.BaseEventNotifier#consumeEvent(v9t9.common.events.NotifyEvent)
	 */
	@Override
	protected void consumeEvent(NotifyEvent event) {
		// don't
	}

}
