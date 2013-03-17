/*
  MultiImageSizeProvider.java

  (c) 2010-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.gui.client.swt.bars;

import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;


import ejs.base.utils.Pair;



/**
 * Get an image which are available in multiple sizes
 * @author ejs
 *
 */
public class MultiImageSizeProvider implements ImageProvider {
	protected TreeMap<Integer, Image> iconMap;
	
	/**
	 * 
	 */
	public MultiImageSizeProvider(TreeMap<Integer, Image> iconMap) {
		this.iconMap = iconMap;
	}
	/**
	 */
	public Pair<Double, Image> getImage(final int sx, final int sy) {
		int sz = Math.max(sx, sy);
		SortedMap<Integer, Image> tailMap = iconMap.tailMap(sz);
		Image icon;
		if (tailMap.isEmpty())
			if (!iconMap.isEmpty())
				icon = iconMap.lastEntry().getValue();
			else
				icon = null;
		else
			icon = tailMap.values().iterator().next();
		int min = iconMap.values().iterator().next().getBounds().width;
		double ratio = (double) icon.getBounds().width / min;
		return new Pair<Double, Image>(ratio, icon);
	}

	@Override
	public void drawImage(GC gc, Rectangle drawRect, Rectangle imgRect) {
		double ratio;
		Pair<Double, Image> iconInfo = getImage(drawRect.width, drawRect.height);
		ratio = iconInfo.first;
		Image icon = iconInfo.second;
		if (drawRect.width > 0 && imgRect.width > 0 && ratio > 0 && imgRect.x >= 0 && imgRect.y >= 0)
			gc.drawImage(icon, (int)(imgRect.x * ratio), (int)(imgRect.y * ratio), 
				(int)(imgRect.width * ratio), (int) (imgRect.height * ratio), 
				drawRect.x, drawRect.y, drawRect.width, drawRect.height);
	}

	/**
	 * @param i
	 * @return
	 */
	public Rectangle imageIndexToBounds(int iconIndex) {
		Rectangle bounds = iconMap.values().iterator().next().getBounds();
		int unit = bounds.width;
		return new Rectangle(0, unit * iconIndex, unit, unit); 
	}
}