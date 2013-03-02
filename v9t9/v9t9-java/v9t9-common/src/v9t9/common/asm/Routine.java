/*
  Routine.java

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
package v9t9.common.asm;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;



/**
 * A routine, i.e. an entry point which is branched to with a return
 * address.
 * @author ejs
 */
public abstract class Routine implements Comparable<Routine> {
	
	private Label mainLabel;
    private TreeSet<Label> labels;
    
    /** Routine goes not exit in an expected way */
    public static final int fUnknownExit = 1;
    /** Routine is part of another routine */
    public static final int fSubroutine = 2;
    public int flags;
    
    protected int dataWords;
    
    public Routine() {
        labels = new TreeSet<Label>();
    }
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mainLabel == null) ? 0 : mainLabel.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Routine other = (Routine) obj;
		if (mainLabel == null) {
			if (other.mainLabel != null)
				return false;
		} else if (!mainLabel.equals(other.mainLabel))
			return false;
		return true;
	}

	public int compareTo(Routine o) {
    	return mainLabel.compareTo(o.mainLabel);
    }
    
    @Override
    public String toString() {
        return "Routine " + mainLabel;
    }
    abstract public boolean isReturn(IHighLevelInstruction inst);
    
    @Deprecated
    public Block getBlock(Label lab) {
    	return lab.getBlock();
    }

    /**
     * Examine code appearing at the entry of a routine
     */
    abstract public void examineEntryCode();

    /** Get the blocks spanned by the routine.  Requires that the flowgraph info is complete. */
	public Collection<Block> getSpannedBlocks() {
		Collection<Block> spannedBlocks = new TreeSet<Block>();
		if (labels.size() == 1) {
			spannedBlocks = mainLabel.getBlock().getSpannedBlockSet();
		} else {
			for (Label label : labels) {
				spannedBlocks.addAll(label.getBlock().getSpannedBlockSet());
			}
		}
		return Collections.unmodifiableCollection(spannedBlocks);
	}

	/** Get the entry point(s) */
	public Collection<Block> getEntries() {
		Set<Block> blocks = new TreeSet<Block>();
		for (Label label : labels) {
			blocks.add(label.getBlock());
		}
		return blocks;
	}

	public void addEntry(Label label) {
		labels.add(label);
		mainLabel = labels.iterator().next();
	}

	public Label getMainLabel() {
		return mainLabel;
	}
	
	public int getDataWords() {
		return dataWords;
	}

	public Set<Integer> getSpannedPcs() {
		Set<Integer> pcSet = new TreeSet<Integer>();
		for (Block block : getSpannedBlocks()) {
			Set<Integer> blockPcSet = block.getSpannedPcs();
			pcSet.addAll(blockPcSet);
		}
		return pcSet;
	}

}
