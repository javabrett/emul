/*
  IImageButtonAreaHandler.java

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
package v9t9.gui.client.swt.bars;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * This allows extending the behavior of an image button given a particular
 * area of the button.
 * <p/>
 * The handler's methods are called whenever mouse events are detected or refresh() is invoked.
 * @author ejs
 *
 */
public interface IImageButtonAreaHandler {

	/**
	 * Attach self to the button -- for instance, to register image overlay(s)
	 * or property listener(s)
	 * @param button
	 */
	void attach(ImageButton button);
	/**
	 * Detach self from the button, removing image overlay(s) and property listener(s)
	 * @param button
	 */
	void detach(ImageButton button);
	
	/**
	 * Get the area covered by the image, with respect to the button's size.
	 * @return
	 */
	Rectangle getBounds(Point size);
	
	/** Tell whether the area is currently active.  If so, other
	 * queries may be performed.  If not, control passes
	 * to the handler underneath this one, or to the button itself.
	 * @return
	 */
	boolean isActive();

	/**
	 * Tell whether the area acts as a menu.  If true, then button-1 events will
	 * trigger a MenuDetect event.
	 */
	boolean isMenu();

	/**
	 * Get the tooltip for the area
	 * @return string or <code>null</code>
	 */
	String getTooltip();
	
	/**
	 * Called when the area is active and the mouse enters the area
	 */
	void mouseEnter();
	/**
	 * Called when the area is active and the mouse hovers over the area
	 */
	void mouseHover();
	/**
	 * Called when the area is active and the mouse exits the area
	 */
	void mouseExit();
	/**
	 * Called when the area is active and the mouse clicks in the area 
	 * @return true if handled 
	 */
	boolean mouseDown(int button);
	/**
	 * Called when the area is active and the mouse button is released in the area 
	 * @return true if handled 
	 */
	boolean mouseUp(int button);
	
}
