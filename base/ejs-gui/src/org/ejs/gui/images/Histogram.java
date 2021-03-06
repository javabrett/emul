/*
  Histogram.java

  (c) 2011-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package org.ejs.gui.images;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ejs.base.utils.Pair;


public class Histogram {
	final Map<Integer, Integer> hist;
	final Map<Integer, Integer> pixelToColor;
	final List<Integer> indices;
	final int[] mappedColors;
	private int mapped;
	private IColorMapper paletteMapper;
	private int maxDist;
	
	private long lumSum;
	private int lumCount;
	
	/**
	 * Build a histogram of the colors in the image once the 
	 * colors are reduced to the given palette with the given
	 * error.
	 * @param paletteMapper the color mapper
	 * @param width maximum width of images
	 * @param height maximum height of images
	 * @param maxDist maximum distance for a color to be considered to match
	 */
	public Histogram(IColorMapper paletteMapper, int width, int height, int maxDist) {
		this.paletteMapper = paletteMapper;
		this.maxDist = maxDist;
		hist = new TreeMap<Integer, Integer>();
		indices = new ArrayList<Integer>();
		mappedColors = new int[width * height];
		pixelToColor = new TreeMap<Integer, Integer>();
	}
	
	/**
	 * Build a histogram of the colors in the image once the 
	 * colors are reduced to the given palette with the given
	 * error.
	 * @param paletteMapper the color mapper
	 * @return the number of colors that map directly (within maxDist)
	 */
	public int generate(BufferedImage image) {
		hist.clear();
		indices.clear();
		pixelToColor.clear();
		Arrays.fill(mappedColors, 0);
		mapped = 0;
		lumSum = 0;
		lumCount = 0;
		
		add(image);
		
		sort();
			
		return mapped;
	}


	/**
	 * Add the given image to the histogram.
	 */
	public void add(BufferedImage img) {
		int[] distA = { 0 };
		int offs = 0;
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int pixel = img.getRGB(x, y);
				int c;
				Integer color = pixelToColor.get(pixel);
				if (color == null) {
					c = paletteMapper.mapColor(pixel, distA);
					if (distA[0] <= maxDist) {
						pixelToColor.put(pixel, c);
						
						Integer count = hist.get(c);
						if (count == null)
							count = 1;
						else
							count++;
						hist.put(c, count);
					} else {
						c = -1;
					}
				} else {
					c = color;
					hist.put(color, hist.get(color) + 1);
				}
				mappedColors[offs++] = c;
				if (c != -1) {
					mapped++;
				}
				lumSum += ColorMapUtils.getPixelLum(pixel);
				lumCount++;
			}
		}
	}

	public int getAverageLuminance() {
		return (int) (lumCount == 0 ? 128 : lumSum / lumCount);
	}
	private void sort() {
		List<Pair<Integer, Integer>> sorted = new ArrayList<Pair<Integer,Integer>>();
		for (Map.Entry<Integer, Integer> entry : hist.entrySet()) {
			sorted.add(new Pair<Integer, Integer>(entry.getKey(), entry.getValue()));
		}
		Collections.sort(sorted, new Comparator<Pair<Integer, Integer>>() {

			@Override
			public int compare(Pair<Integer, Integer> o1,
					Pair<Integer, Integer> o2) {
				return o2.second - o1.second;
			}
			
		});
		
		indices.clear();
		for (Pair<Integer, Integer> pair : sorted) {
			indices.add(pair.first);
		}
	}

	/**
	 * Get histogram size, or number of colors mapped.
	 * @return
	 */
	public int size() {
		return hist.size();
	}

	/**
	 * Get the sorted palette indices in prominence order.
	 * @return list of indices into palette from {@link IPaletteMapper#mapColor(int[], int[])}
	 */
	public List<Integer> getColorIndices() {
		return indices;
	}
	/**
	 * Get the palette index of the i'th most prominent color.
	 * @param i
	 * @return index into palette from {@link IPaletteMapper#mapColor(int[], int[])}
	 */
	public int getColorIndex(int i) {
		return indices.get(i);
	}

	/**
	 * Get the number of appearances of the i'th most prominent color.
	 * @param idx
	 * @return count of pixels
	 */
	public int getColorCount(int idx) {
		return hist.get(idx);
	}
	
	/**
	 * @return the mapping of each RGB pixel to its count
	 */
	public Map<Integer, Integer> colorToCountMap() {
		return hist;
	}
	
	/**
	 * @return the mapping of RGB pixel to palette color;
	 * entry present if color selected, else null
	 */
	public Map<Integer, Integer> pixelToColor() {
		return pixelToColor;
	}

	/**
	 * @return the mapping, per pixel (width x height)
	 * for which palette index was selected.  May be -1
	 * if nothing chosen.
	 */
	public int[] mappedColors() {
		return mappedColors;
	}
}