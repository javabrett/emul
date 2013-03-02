/*
  TargetConstant.java

  (c) 2010-2011 Edward Swartz

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
package v9t9.tools.forthcomp.words;

import v9t9.tools.forthcomp.AbortException;
import v9t9.tools.forthcomp.DictEntry;
import v9t9.tools.forthcomp.HostContext;
import v9t9.tools.forthcomp.ISemantics;
import v9t9.tools.forthcomp.ITargetWord;

/**
 * @author ejs
 *
 */
public class TargetConstant extends TargetWord implements ITargetWord {

	private final int value;
	private final int width;

	/**
	 * @param entry
	 */
	public TargetConstant(String name, int value_, int width_) {
		super(new DictEntry(0, 0, name));
		this.value = value_;
		this.width = width_;
		
		setCompilationSemantics(new ISemantics() {
			
			public void execute(HostContext hostContext, TargetContext targetContext)
					throws AbortException {
				if (getEntry().canInline()) {
					if (getWidth() == 1)
						targetContext.compileLiteral(getValue(), false, true);
					else if (getWidth() == 2 && targetContext.getCellSize() == 2)
						targetContext.compileDoubleLiteral(getValue() & 0xffff, getValue() >> 16, false, true);
					else
						assert false;
				} else {
					targetContext.compile(TargetConstant.this);
				}
			}
		});
		setExecutionSemantics(new ISemantics() {
			
			public void execute(HostContext hostContext, TargetContext targetContext)
					throws AbortException {
				hostContext.pushData(value & 0xffff);
				if (width == 2)
					hostContext.pushData(value >> 16);
				
			}
		});
	}

	/**
	 * @return
	 */
	public int getValue() {
		return value;
	}
	
	/**
	 * @return the width
	 */
	public int getWidth() {
		return width;
	}
}
