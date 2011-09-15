/**
 * 
 */
package v9t9.emulator.clients.builtin.video.v9938;

import v9t9.emulator.clients.builtin.video.IBitmapPixelAccess;
import v9t9.emulator.clients.builtin.video.RedrawBlock;
import v9t9.emulator.clients.builtin.video.VdpModeInfo;
import v9t9.emulator.clients.builtin.video.VdpRedrawInfo;
import v9t9.engine.memory.ByteMemoryAccess;

/**
 * Redraw graphics 7 mode content (256x192x256)
 * <p>
 * Bitmapped mode where pattern table contains one pixel per byte in RGB 3-3-2 format.  
 * Every row is linear in memory and every row is adjacent to the next.  
 * @author ejs
 *
 */
public class Graphics7ModeRedrawHandler extends PackedBitmapGraphicsModeRedrawHandler {

	public Graphics7ModeRedrawHandler(VdpRedrawInfo info, VdpModeInfo modeInfo) {
		super(info, modeInfo);
	}

	@Override
	protected void init() {
		rowstride = 256;
		blockshift = 3;
		blockstride = 32;
		blockcount = (info.vdpregs[9] & 0x80) != 0 ? 32*27 : 768;		
	}
	
	protected void drawBlock(RedrawBlock block, int pageOffset, boolean interlaced) {
		info.canvas.draw8x8BitmapRGB332ColorBlock(
				block.c + (interlaced ? 256 : 0), block.r,
			 info.vdp.getByteReadMemoryAccess(
					(modeInfo.patt.base + rowstride * block.r + block.c) ^ pageOffset),
			rowstride);
	}

	/** Backdrop isn't a normal color */
	public void clear() {
		byte [] rgb = { 0, 0, 0 };
		info.canvas.getGRB332(rgb, (byte) info.canvas.getClearColor());
		info.canvas.clear(rgb);
	}
	

	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.video.BaseRedrawHandler#importImageData()
	 */
	@Override
	public void importImageData(IBitmapPixelAccess access) {
		ByteMemoryAccess patt = info.vdp.getByteReadMemoryAccess(modeInfo.patt.base);
		
		int my =  (info.vdpregs[9] & 0x80) != 0 ? 212 : 192;
		for (int y = 0; y < my; y++) {
			for (int x = 0; x < 256; x++) {
				
				int poffs = y * rowstride + x; 
				patt.memory[patt.offset + poffs] = access.getPixel(x, y);
				touch(patt.offset + poffs);
			}
		}
		
	}
}