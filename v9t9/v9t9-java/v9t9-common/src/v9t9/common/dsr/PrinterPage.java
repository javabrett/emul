/*
  PrinterPage.java

  (c) 2015 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.dsr;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a single page of printer output
 * @author ejs
 *
 */
public class PrinterPage {
	
	public static class Dot {
		public final float x, y;
		public final float ink;
		
		public Dot(double x, double y, double inkLevel) {
			this.x = (float) x;
			this.y = (float) y;
			this.ink = (float) inkLevel;
		}
	}

	private int vertDots;
	private int horizDots;
	
	private double vertInches;
	private double horizInches;
	
	private TreeMap<Float, TreeMap<Float, Float>> dots;
	private List<Dot> dotList;
	
	private Date date;

	public PrinterPage(double horizInches, double vertInches, int horizDots, int vertDots) {
		this.date = Calendar.getInstance().getTime();
		
		this.horizInches = horizInches;
		this.vertInches = vertInches;
		this.horizDots = horizDots;
		this.vertDots = vertDots;
		
		dots = new TreeMap<Float, TreeMap<Float,Float>>();
		
		dotList = new ArrayList<Dot>();
	}
	
	/**
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}
	
	/**
	 * @return the horizDots
	 */
	public int getHorizontalDots() {
		return horizDots;
	}
	/**
	 * @return the vertDots
	 */
	public int getVerticalDots() {
		return vertDots;
	}

	public void set(double x, double y, double inkLevel) {
		if (x >= 0 && x < horizDots
				&& y >= 0 && y < vertDots) {
			
			TreeMap<Float, Float> row = dots.get((float) y);
			if (row == null) {
				row = new TreeMap<Float, Float>();
				dots.put((float) y, row);
			}
			Float val = row.get((float) x);
			if (val == null)
				val = (float) inkLevel;
			else
				val += (float) inkLevel;
			row.put((float) x, val);
			
			dotList.add(new Dot(x, y, inkLevel));
		}
	}

	public float[] getRows() {
		float[] rowa = new float[dots.size()];
		Iterator<Float> iter = dots.keySet().iterator();
		for (int i = 0; i < rowa.length; i++) {
			rowa[i] = iter.next();
		}
		return rowa;
	}
	
	@SuppressWarnings("unchecked")
	public Map.Entry<Float, Float>[] getDotsOnRow(double y) {
		if (y >= 0 && y < vertDots) {
			TreeMap<Float, Float> rows = dots.get((float) y);
			if (rows != null) {
				return rows.entrySet().toArray(new Map.Entry[rows.entrySet().size()]);
			}
		}
		return new Map.Entry[0];
	}

	public double getPageWidthInches() {
		return horizInches;
	}

	public double getPageHeightInches() {
		return vertInches;
	}
	
	public int getDotCount() {
		return dotList.size();
	}
	
	public List<Dot> getDotsFrom(int index) {
		return Collections.unmodifiableList(dotList.subList(index, dotList.size()));
	}
}
