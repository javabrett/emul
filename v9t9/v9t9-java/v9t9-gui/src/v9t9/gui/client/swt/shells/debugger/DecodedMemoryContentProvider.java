/*
  DecodedMemoryContentProvider.java

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
package v9t9.gui.client.swt.shells.debugger;

import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

import v9t9.gui.common.IMemoryDecoder;

/**
 * This decodes memory according to the attached memory decoders for
 * each row of memory.   
 * @author ejs
 *
 */
public class DecodedMemoryContentProvider implements ILazyContentProvider {

	private MemoryRange range;
	private MemoryRangeChanges changes;
	private TableViewer tableViewer;
	private final IMemoryDecoder decoderProvider;
	private int firstIndex;
	
	public DecodedMemoryContentProvider(IMemoryDecoder decoder) {
		this.decoderProvider = decoder;
	}
	public void dispose() {
		
	}
	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.tableViewer = (TableViewer) viewer;
		if (changes != null) {
			changes.removeMemoryListener();
//			range.getEntry().getDomain().removeWriteListener(this);
			decoderProvider.reset();
		}
		
		range = (MemoryRange) newInput;
		changes = null;
		if (range != null) {
			changes = new MemoryRangeChanges(range);
			changes.attachMemoryListener();
//			range.getEntry().getDomain().addWriteListener(this);

			decoderProvider.addRange(range.getAddress(), range.getSize());
			
			// clear
			tableViewer.setItemCount(0);
			tableViewer.refresh(); 
			
			updateRange();
			
		} else {
			decoderProvider.reset();
			tableViewer.setItemCount(0);
			tableViewer.refresh(true);
		}
	}

	public void updateElement(int index) {
		if (range == null)
			return;
		
		/*
		//System.out.println(index);
		int addr = index * chunkSize;
		DecodedRow row = (DecodedRow) tableViewer.getElementAt(index);
		if (row == null) {
			Pair<Object, Integer> info = decoderProvider.decode(addr,chunkSize);
			row = new DecodedRow(addr, range, info.first, info.second);
		}
		*/
		
		DecodedRow row = (DecodedRow) tableViewer.getElementAt(index);
		if (row != null) {
			IDecodedContent content = row.getContent();
			if (changes.isTouched(content.getAddr(), content.getSize())) {
				row = null;
			}
		}
		if (row == null) {
			IDecodedContent content = decoderProvider.decodeItem(index + firstIndex);
			byte[] bytes = new byte[content.getSize()];
			for (int i = 0; i < bytes.length; i++)
				bytes[i] = range.getEntry().flatReadByte(content.getAddr() + i);
			row = new DecodedRow(content, bytes);
		}
		
		tableViewer.replace(row, index);
	}
	/**
	 * 
	 */
	public void refresh() {
		changes.fetchChanges();
		if (changes.isTouched(range.getAddress(), range.getSize())) {
			
			decoderProvider.updateRange(changes.getChangeSet());

			updateRange();
		}
	}
	private synchronized void updateRange() {
		firstIndex = decoderProvider.getFirstItemIndex(range.getAddress());
		tableViewer.setItemCount(decoderProvider.getItemCount(range.getAddress(), range.getSize()));
		tableViewer.refresh();
	}
	
	
}