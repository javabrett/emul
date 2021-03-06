/*
  EnhancedVoice.java

  (c) 2009-2013 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.audio.sound;

import ejs.base.sound.ISoundVoice;

/**
 * @author ejs
 *
 */
public interface EnhancedVoice extends ISoundVoice {
	EffectsController getEffectsController();
}
