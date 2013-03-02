/*
  IVdpModeRedrawHandler.java

  (c) 2008-2011 Edward Swartz

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
package v9t9.video;

import v9t9.common.video.RedrawBlock;

/**
 * The redraw handler manages the efficient update of the
 * VDP canvas ("background") given changes made to the VDP
 * memory.  
 * 
 * Any modifying write to VDP memory will be either
 * to one or more mapped areas (screen image table,
 * pattern table, sprite table, etc) or will be invisible
 * (i.e. not mapped to any memory that participates in drawing,
 * or e.g. in simpler graphics modes, affecting a pattern for 
 * which no character is in the screen image table).
 * 
 * The implementor of this interface then is responsible
 * for tracking what has changed on screen.  Effectively
 * this comes down to what screen blocks must be redrawn.
 * These may be directly obvious or may be propagated through
 * other changes (e.g. to the pattern table).
 * @author Ed
 *
 */
public interface IVdpModeRedrawHandler {
	/**
	 * Record that the VDP memory at addr was changed.
	 * @param addr
	 * @return true if change will be visible on-screen
	 */
	boolean touch(int addr);
	
	/**
	 * Update the changed blocks (on the screen) according to relationships
	 * between the various update areas.
	 */
	void prepareUpdate();

	/** 
	 * Given the touched blocks, redraw the bitmap,
	 * generate the redraw blocks representing the changed bits of the bitmap,
	 * and return the number of blocks updated.
	 * @param blocks array of at most 1024 blocks
	 * @return number of blocks changed
	 */
	int updateCanvas(RedrawBlock[] blocks);

	/**
	 * Clear the canvas
	 */
	void clear();

}