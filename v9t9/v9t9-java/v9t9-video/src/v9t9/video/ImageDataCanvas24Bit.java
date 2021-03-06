/*
  ImageDataCanvas24Bit.java

  (c) 2008-2016 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.video;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import v9t9.common.memory.ByteMemoryAccess;
import v9t9.common.video.ISpriteVdpCanvas;

/**
 * Render video content into an ImageData
 * @author ejs
 *
 */
public class ImageDataCanvas24Bit extends ImageDataCanvas {
	private static PaletteData stockPaletteData = new PaletteData(0xFF0000, 0xFF00, 0xFF);

	protected byte[][] colorRGBMap;
	protected byte[][] spriteColorRGBMap;

	private byte[][][] fourColorRGBMap;

	private Buffer vdpCanvasBuffer;

	public ImageDataCanvas24Bit() {
		super();
		setSize(256, 192);
	}
	
	protected PaletteData getPaletteData() {
		return stockPaletteData;
	}

	@Override
	protected void createImageData() {
		int allocHeight = height;
		if ((height & 7) != 0)
			allocHeight += 8;
		
		imageData = new ImageData(width, allocHeight * (isInterlacedEvenOdd() ? 2 : 1), 24, getPaletteData());
		if (isInterlacedEvenOdd())
			bytesPerLine = imageData.bytesPerLine * 2;
		else
			bytesPerLine = imageData.bytesPerLine;
		
		pixSize = (imageData.depth >> 3);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.video.IVdpCanvas#writeRow(byte[])
	 */
	@Override
	public void writeRow(int y, byte[] rowData) {
		if (colorRGBMap == null)
			return;
		
		byte[] data = imageData.data;
		int offs = getBitmapOffset(0, y);
		for (int i = 0; i < rowData.length; i++) {
			byte[] fgRGB = colorRGBMap[rowData[i]];
			data[offs] = fgRGB[0];
			data[offs+1] = fgRGB[1];
			data[offs+2] = fgRGB[2];
			offs += 3;
		}
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.video.VdpCanvas#clear()
	 */
	@Override
	public void clear() {
		byte[] rgb = getClearRGB();
		
		int bpp = imageData.depth >> 3;
		for (int i = 0; i < imageData.data.length; i += bpp) {
			imageData.data[i] = rgb[0];
			imageData.data[i + 1] = rgb[1];
			imageData.data[i + 2] = rgb[2];
		}
		if (imageData.alphaData != null) {
			Arrays.fill(imageData.alphaData, 0, imageData.alphaData.length, (byte)-1);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.video.VdpCanvas#clear()
	 */
	@Override
	public void clearToEvenOddClearColors() {
		byte cc = (byte) getColorMgr().getClearColor();
		byte cc1 = (byte) getColorMgr().getClearColor1();
		
		if (colorRGBMap == null)
			return;
		
		byte[] fgRGB = colorRGBMap[cc];
		byte[] bgRGB = colorRGBMap[cc1];
		
		int bpp = imageData.depth >> 3;
		for (int i = 0; i < imageData.data.length; i += bpp + bpp) {
			imageData.data[i + 0] = fgRGB[0];
			imageData.data[i + 1] = fgRGB[1];
			imageData.data[i + 2] = fgRGB[2];
			imageData.data[i + 3] = bgRGB[0];
			imageData.data[i + 4] = bgRGB[1];
			imageData.data[i + 5] = bgRGB[2];
		}
		if (imageData.alphaData != null) {
			Arrays.fill(imageData.alphaData, 0, imageData.alphaData.length, (byte)-1);
		}
	}
	


	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.video.VdpCanvas#getBitmapOffset(int, int)
	 */
	@Override
	final public int getBitmapOffset(int x, int y) {
		return getLineStride() * (y) + x * pixSize;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.video.VdpCanvas#syncColors()
	 */
	@Override
	public void syncColors() {
		super.syncColors();

		if (colorRGBMap == null) 
			colorRGBMap = new byte[16][];
		if (spriteColorRGBMap == null) 
			spriteColorRGBMap = new byte[16][];
		if (fourColorRGBMap == null) { 
			fourColorRGBMap = new byte[2][][];
			fourColorRGBMap[0] = new byte[16][];
			fourColorRGBMap[1] = new byte[16][];
		}
		
		for (int i = 0; i < 16; i++) {
			colorRGBMap[i] = getColorMgr().getRGB(colorMap[i]);
			spriteColorRGBMap[i] = getColorMgr().getRGB(spriteColorMap[i]);
			fourColorRGBMap[0][i] = getColorMgr().getRGB(fourColorMap[0][i]);
			fourColorRGBMap[1][i] = getColorMgr().getRGB(fourColorMap[1][i]);
		}

	}

	public void drawEightPixels(int offs, byte mem, byte fg, byte bg) {
		byte[] fgRGB = colorRGBMap[fg];
		byte[] bgRGB = colorRGBMap[bg];
		for (int i = 0; i < 8; i++) {
			byte[] rgb = (mem & 0x80) != 0 ? fgRGB : bgRGB;
			imageData.data[offs] = rgb[0];
			imageData.data[offs + 1] = rgb[1];
			imageData.data[offs + 2] = rgb[2];
			mem <<= 1;
			offs += 3;
		}
	}

	public void drawSixPixels(int offs, byte mem, byte fg, byte bg) {
		byte[] fgRGB = colorRGBMap[fg];
		byte[] bgRGB = colorRGBMap[bg];
		for (int i = 0; i < 6; i++) {
			byte[] rgb = (mem & 0x80) != 0 ? fgRGB : bgRGB;
			imageData.data[offs++] = rgb[0];
			imageData.data[offs++] = rgb[1];
			imageData.data[offs++] = rgb[2];
			mem <<= 1;
		}
	}

	public void drawEightSpritePixels(int x, int y, byte mem, byte fg, byte bitmask, boolean isLogicalOr) {
		int offs = getBitmapOffset(x, y);
		int endOffs = getBitmapOffset(256, y);
		byte[] fgRGB = colorRGBMap[fg];
		for (int i = 0; i < 8; i++) {
			if (offs >= endOffs)
				break;
			if ((mem & bitmask & 0x80) != 0) {
				imageData.data[offs] = fgRGB[0];
				imageData.data[offs + 1] = fgRGB[1];
				imageData.data[offs + 2] = fgRGB[2];
			}
			bitmask <<= 1;
			mem <<= 1;
			offs += 3;
		}
	}

	public void drawEightMagnifiedSpritePixels(int x, int y, byte mem_, byte fg, short bitmask, boolean isLogicalOr) {
		int offs = getBitmapOffset(x, y);
		int endOffs = getBitmapOffset(256, y);
		byte[] fgRGB = colorRGBMap[fg];
		short mem = (short) (mem_ << 8);
		for (int i = 0; i < 8; i++) {
			if (offs >= endOffs)
				break;
			if ((mem & bitmask & 0x8000) != 0) {
				imageData.data[offs] = fgRGB[0];
				imageData.data[offs + 1] = fgRGB[1];
				imageData.data[offs + 2] = fgRGB[2];
			}
			bitmask <<= 1;
			offs += 3;
			if (offs >= endOffs)
				break;
			if ((mem & bitmask & 0x8000) != 0) {
				imageData.data[offs] = fgRGB[0];
				imageData.data[offs + 1] = fgRGB[1];
				imageData.data[offs + 2] = fgRGB[2];
			}
			bitmask <<= 1;
			offs += 3;
			mem <<= 1;
		}
	}

	public void drawEightDoubleMagnifiedSpritePixels(int x, int y, byte mem_, byte fg, short bitmask, boolean isLogicalOr) {
		int offs = getBitmapOffset(x, y);
		int endOffs = getBitmapOffset(256, y);
		byte[] fgRGB = colorRGBMap[fg];
		short mem = (short) (mem_ << 8);
		for (int i = 0; i < 8; i++) {
			if (offs >= endOffs)
				break;
			if ((mem & bitmask & 0x8000) != 0) {
				imageData.data[offs] = fgRGB[0];
				imageData.data[offs + 1] = fgRGB[1];
				imageData.data[offs + 2] = fgRGB[2];
				if (offs + 3 >= endOffs)
					return;
				imageData.data[offs + 3] = fgRGB[0];
				imageData.data[offs + 4] = fgRGB[1];
				imageData.data[offs + 5] = fgRGB[2];
			}
			bitmask <<= 1;
			offs += 6;
			if (offs >= endOffs)
				break;
			if ((mem & bitmask & 0x8000) != 0) {
				imageData.data[offs] = fgRGB[0];
				imageData.data[offs + 1] = fgRGB[1];
				imageData.data[offs + 2] = fgRGB[2];
				if (offs + 3 >= endOffs)
					return;
				imageData.data[offs + 3] = fgRGB[0];
				imageData.data[offs + 4] = fgRGB[1];
				imageData.data[offs + 5] = fgRGB[2];
			}
			bitmask <<= 1;
			offs += 6;
			mem <<= 1;
		}
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.video.IVdpCanvas#draw8x8BitmapTwoColorByte(int, int, v9t9.common.memory.ByteMemoryAccess)
	 */
	@Override
	public void draw8x8BitmapTwoColorByte(int x, int y, ByteMemoryAccess access) {
		int offs = getBitmapOffset(x, y);
		for (int j = 0; j < 4; j++) {
			byte mem;
			
			byte pix;
			byte[] rgb;

			mem = access.memory[access.offset + j];

			pix = (byte) ((mem >> 4) & 0xf);
			rgb = colorRGBMap[pix];
			imageData.data[offs] = rgb[0];
			imageData.data[offs + 1] = rgb[1];
			imageData.data[offs + 2] = rgb[2];
			
			offs += 3;
			
			pix = (byte) (mem & 0xf);
			rgb = colorRGBMap[pix];
			imageData.data[offs] = rgb[0];
			imageData.data[offs + 1] = rgb[1];
			imageData.data[offs + 2] = rgb[2];
			
			offs += 3;
		}
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.video.IVdpCanvas#draw8x8BitmapFourColorByte(int, int, v9t9.common.memory.ByteMemoryAccess)
	 */
	@Override
	public void draw8x8BitmapFourColorByte(int x, int y, ByteMemoryAccess access) {
		int offs = getBitmapOffset(x, y);
		for (int j = 0; j < 2; j++) {
			byte mem;
			
			byte pix;
			byte[] rgb;

			mem = access.memory[access.offset + j];

			pix = (byte) ((mem >> 6) & 0x3);
			rgb = fourColorRGBMap[0][pix];
			imageData.data[offs] = rgb[0];
			imageData.data[offs + 1] = rgb[1];
			imageData.data[offs + 2] = rgb[2];
			
			offs += 3;
			
			pix = (byte) ((mem >> 4) & 0x3);
			rgb = fourColorRGBMap[1][pix];
			imageData.data[offs] = rgb[0];
			imageData.data[offs + 1] = rgb[1];
			imageData.data[offs + 2] = rgb[2];
			
			offs += 3;
			
			pix = (byte) ((mem >> 2) & 0x3);
			rgb = fourColorRGBMap[0][pix];
			imageData.data[offs] = rgb[0];
			imageData.data[offs + 1] = rgb[1];
			imageData.data[offs + 2] = rgb[2];
			
			offs += 3;
			
			pix = (byte) (mem & 0x3);
			rgb = fourColorRGBMap[1][pix];
			imageData.data[offs] = rgb[0];
			imageData.data[offs + 1] = rgb[1];
			imageData.data[offs + 2] = rgb[2];
			
			offs += 3;
		}
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.video.IVdpCanvas#draw8x8BitmapRGB332ColorByte(int, int, v9t9.common.memory.ByteMemoryAccess)
	 */
	@Override
	public void draw8x8BitmapRGB332ColorByte(int x, int y,
			ByteMemoryAccess access) {
		int offs = getBitmapOffset(x, y);
		byte[] rgb = { 0, 0, 0 };
		for (int j = 0; j < 8; j++) {
			byte mem;
			
			mem = access.memory[access.offset + j];

			getColorMgr().getGRB332(rgb, mem);
			imageData.data[offs] = rgb[0];
			imageData.data[offs + 1] = rgb[1];
			imageData.data[offs + 2] = rgb[2];
			
			offs += 3;
		}
			
	}
	
	@Override
	public void blitSpriteBlock(ISpriteVdpCanvas spriteCanvas, int x, int y,
			int blockMag) {
		int sprOffset = spriteCanvas.getBitmapOffset(x, y);
		int bitmapOffset = getBitmapOffset(x * blockMag, y);
		try {
			for (int i = 0; i < 8; i++) {
				for (int j = 0; j < 8; j++) {
					byte cl = spriteCanvas.getColorAtOffset(sprOffset + j);
					if (cl != 0) {
						byte[] rgb = spriteColorRGBMap[cl];
						imageData.data[bitmapOffset] = rgb[0];
						imageData.data[bitmapOffset + 1] = rgb[1];
						imageData.data[bitmapOffset + 2] = rgb[2];
						if (blockMag > 1 && bitmapOffset < imageData.data.length) {
							imageData.data[bitmapOffset + 3] = rgb[0];
							imageData.data[bitmapOffset + 4] = rgb[1];
							imageData.data[bitmapOffset + 5] = rgb[2];
						}
					}
					bitmapOffset += 3 * blockMag;
				}
				sprOffset += spriteCanvas.getLineStride();
				bitmapOffset += getLineStride() - 3 * 8 * blockMag;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			// ignore
		}
	}
	
	@Override
	public void blitFourColorSpriteBlock(ISpriteVdpCanvas spriteCanvas, int x, int y,
			int blockMag) {
		int sprOffset = spriteCanvas.getBitmapOffset(x, y);
		int bitmapOffset = getBitmapOffset(x * blockMag, y);
		try {
			for (int i = 0; i < 8; i++) {
				int colorColumn = x % 2;
				for (int j = 0; j < 8; j++) {
					byte col = spriteCanvas.getColorAtOffset(sprOffset + j);
					byte cl;
					if (colorColumn == 0)
						cl = (byte) ((col & 0xc) >> 2);
					else
						cl = (byte) (col & 0x3);
					if (cl != 0) {
						byte[] rgb = spriteColorRGBMap[cl];
						imageData.data[bitmapOffset] = rgb[0];
						imageData.data[bitmapOffset + 1] = rgb[1];
						imageData.data[bitmapOffset + 2] = rgb[2];
					}
					bitmapOffset += 3;
					colorColumn ^= 1;
					//System.out.println(j+","+(j * blockMag + x + 1));
					if (blockMag > 1) {
						if (colorColumn == 0)
							cl = (byte) ((col & 0xc) >> 2);
						else
							cl = (byte) (col & 0x3);
						if (cl != 0) {
							byte[] rgb = spriteColorRGBMap[cl];
							imageData.data[bitmapOffset] = rgb[0];
							imageData.data[bitmapOffset + 1] = rgb[1];
							imageData.data[bitmapOffset + 2] = rgb[2];
						}
						bitmapOffset += 3;
						colorColumn ^= 1;
					}
				}
				sprOffset += spriteCanvas.getLineStride();
				bitmapOffset += getLineStride() - 3 * 8 * blockMag;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			// ignore
		}			
	}

	/* (non-Javadoc)
	 * @see v9t9.video.IGLDataCanvas#copyIntoTexture()
	 */
	@Override
	public Buffer getBuffer() {
		vdpCanvasBuffer = copy(vdpCanvasBuffer);
		return vdpCanvasBuffer;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.video.BitmapVdpCanvas#getNextRGB(java.nio.Buffer, byte[])
	 */
	@Override
	public void getNextRGB(Buffer buffer, byte[] rgb) {
		((ByteBuffer) buffer).get(rgb);
	}
}
