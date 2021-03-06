/*
  IVdpCanvas.java

  (c) 2011-2014 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.video;

import v9t9.common.memory.ByteMemoryAccess;


/**
 * @author ejs
 *
 */
public interface IVdpCanvas extends ICanvas {
	VdpFormat getFormat();
	
	void setFormat(VdpFormat format);

	void setOffset(int xoffs, int yoffs);

	void setSize(int width, int height, boolean interlacedEvenOdd);

	int getBitmapOffset(int x, int y);

	boolean isInterlacedEvenOdd();

	void writeRow(int y, byte[] rowData);


	/** Clear the canvas to the clear color, if the rgb is not used.  
	 */
	void clear();
	void clearToEvenOddClearColors();

//	void setBlank(boolean b);
	
	/** Get the virtual scanline */
	int getCurrentY();
	/** Get the virtual scanline */
	void setCurrentY(int y);
	/**
	 * Draw eight pixels in a row, where pixels corresponding to an "on"
	 * bit in "mem" are painted with the "fg" color, otherwise with the "bg" color.
	 * @param offs
	 * @param mem
	 * @param fg foreground; use 16 for the vdpreg[7] fg 
	 * @param bg background; use 0 for the vdpreg[7] bg
	 * @see #getBitmapOffset(int, int)
	 */
	void drawEightPixels(int offs, byte mem, byte fg, byte bg); 
	/**
	 * Draw six pixels in a row, where pixels corresponding to an "on"
	 * bit in "mem" are painted with the "fg" color, otherwise with the "bg" color.
	 * @param offs
	 * @param mem
	 * @param fg foreground; use 16 for the vdpreg[7] fg 
	 * @param bg background; use 0 for the vdpreg[7] bg
	 * @see #getBitmapOffset(int, int)
	 */
	void drawSixPixels(int offs, byte mem, byte fg, byte bg); 
	/**
	 * Blit an 8x8 block defined by a pattern and a foreground/background color to the bitmap
	 * @param r
	 * @param c
	 * @param pattern
	 * @param fg foreground; use 16 for the vdpreg[7] fg 
	 * @param bg background; use 0 for the vdpreg[7] bg
	 */
	void draw8x8TwoColorBlock(int r, int c, ByteMemoryAccess pattern,
			byte fg, byte bg);

	/**
	 * Blit an 8x6 block defined by a pattern and a foreground/background color to the bitmap
	 * @param r
	 * @param c
	 * @param pattern
	 * @param fg foreground; use 16 for the vdpreg[7] fg 
	 * @param bg background; use 0 for the vdpreg[7] bg
	 */
	void draw8x6TwoColorBlock(int r, int c, ByteMemoryAccess pattern,
			byte fg, byte bg);

	/**
	 * Blit an 8x8 block defined by a pattern and colors to the bitmap
	 * @param r
	 * @param c
	 * @param pattern
	 * @param colors array of 0x&lt;fg&gt;&lt;bg&gt; pixels; bg may be 0 for vdpreg[7] bg
	 */
	void draw8x8MultiColorBlock(int r, int c,
			ByteMemoryAccess pattern, ByteMemoryAccess colors);


	/**
	 * Draw 8x1 pixels from the given memory, arranged as
	 * &lt;color;&gt;&lt;color&gt; in nybbles. 
	 * @param offs
	 * @param offs
	 * @param access
	 */
	void draw8x8BitmapTwoColorByte(
			int x, int y,
			ByteMemoryAccess access);

	/**
	 * Draw 8x1 pixels from the given memory, arranged as
	 * &lt;color;&gt;&lt;color&gt;&lt;color;&gt;&lt;color&gt; in two-bit pieces. 
	 * @param offs
	 * @param access
	 */
	void draw8x8BitmapFourColorByte(int x, int y,
			ByteMemoryAccess access);

	/**
	 * Draw 8x1 pixels from the given memory, arranged as
	 * RGB 3-3-2 pixels. 
	 * @param offset
	 * @param access
	 */
	void draw8x8BitmapRGB332ColorByte(int x, int y,
			ByteMemoryAccess access);

	/**
	 * Draw an 8x8 block of pixels from the given memory, arranged as
	 * &lt;color;&gt;&lt;color&gt; in nybbles. 
	 * @param offs
	 * @param offs
	 * @param access
	 * @param rowstride access stride between rows
	 */
	void draw8x8BitmapTwoColorBlock(
			int x, int y,
			ByteMemoryAccess access,
			int rowstride);

	/**
	 * Draw an 8x8 block of pixels from the given memory, arranged as
	 * &lt;color;&gt;&lt;color&gt;&lt;color;&gt;&lt;color&gt; in two-bit pieces. 
	 * @param offs
	 * @param access
	 * @param rowstride access stride between rows
	 */
	void draw8x8BitmapFourColorBlock(int x, int y,
			ByteMemoryAccess access, int rowstride);

	/**
	 * Draw an 8x8 block of pixels from the given memory, arranged as
	 * RGB 3-3-2 pixels. 
	 * @param offset
	 * @param rowstride access stride between rows
	 * @param access
	 */
	void draw8x8BitmapRGB332ColorBlock(int x, int y,
			ByteMemoryAccess byteReadMemoryAccess, int rowstride);


	/** Draw eight pixels of an 8x1 row. 
	 * @param bitmask mask of rows visible from top-down 
	 * @param isLogicalOr */
	void drawEightSpritePixels(int x, int y, byte mem, byte fg, byte bitmask, boolean isLogicalOr); 
	/** Draw 16 (8 magnified) pixels of an 8x1 row. 
	 * @param bitmask mask of rows visible from top-down 
	 * @param isLogicalOr */
	void drawEightMagnifiedSpritePixels(int x, int y, byte mem, byte fg, short bitmask, boolean isLogicalOr);
	/** Draw 32 (8 magnified) pixels of an 8x1 row. 
	 * @param bitmask mask of rows visible from top-down 
	 * @param isLogicalOr */
	void drawEightDoubleMagnifiedSpritePixels(int x, int y, byte mem, byte fg, short bitmask, boolean isLogicalOr);

	void blitSpriteBlock(ISpriteVdpCanvas spriteCanvas, int i, int j, int blockMag);

	void blitFourColorSpriteBlock(ISpriteVdpCanvas spriteCanvas, int i, int j,
			int blockMag);


	VdpColorManager getColorMgr();
	void setClearColor(int c);
	void syncColors();


}