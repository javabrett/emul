/**
 * 
 */
package v9t9.video.imageimport;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import ejs.base.properties.FieldProperty;
import ejs.base.properties.IPropertySource;
import ejs.base.properties.PropertySource;


import v9t9.common.hardware.IVdpChip;
import v9t9.common.hardware.IVdpTMS9918A;
import v9t9.common.video.IVdpCanvas;
import v9t9.common.video.VdpColorManager;
import v9t9.common.video.VdpFormat;

/**
 * @author ejs
 *
 */
public class ImageImportOptions {

	public enum Dither {
		NONE("None"),
		ORDERED("Ordered"),
		FS("Floyd-Steinberg");
		
		private final String label;

		/**
		 * 
		 */
		private Dither(String label) {
			this.label = label;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return label;
		}
	}
	
	
	private boolean scaleSmooth = true;
	private boolean keepAspect = true;
	private boolean asGreyScale;
	private boolean optimizePalette = true;
	private boolean ditherMono;
	private Dither ditherType = Dither.NONE;
	
	private byte[][] origPalette;
	private BufferedImage image;
	private Rectangle clip;
	
	private ColorOctree octree;
	
	private FieldProperty scaleSmoothProperty;
	private FieldProperty keepAspectProperty;
	private FieldProperty asGreyScaleProperty;
	private FieldProperty optimizePaletteProperty;
	private FieldProperty ditheringProperty;
	private FieldProperty ditherMonoProperty;
	private FieldProperty imageProperty;
	private FieldProperty clipProperty;
	
	/**
	 * 
	 */
	public ImageImportOptions() {
		scaleSmoothProperty = new FieldProperty(this, "scaleSmooth", "Smooth Scaling");
		keepAspectProperty = new FieldProperty(this, "keepAspect", "Keep Aspect Ratio");
		asGreyScaleProperty = new FieldProperty(this, "asGreyScale", "Convert To Greyscale");
		optimizePaletteProperty = new FieldProperty(this, "optimizePalette", "Optimize Palette");
		ditheringProperty = new FieldProperty(this, "ditherType", "Dithering");
		ditherMonoProperty = new FieldProperty(this, "ditherMono", "Dither Monochrome");
		imageProperty = new FieldProperty(this, "image", "Last Image");
		clipProperty = new FieldProperty(this, "clip", "Clip Region");
		clipProperty.setHidden(true);
	}
	/**
	 * @return
	 */
	public IPropertySource createPropertySource() {
		PropertySource ps = new PropertySource();
		ps.addProperty(scaleSmoothProperty);
		ps.addProperty(keepAspectProperty);
		ps.addProperty(asGreyScaleProperty);
		ps.addProperty(optimizePaletteProperty);
		ps.addProperty(ditheringProperty);
		ps.addProperty(ditherMonoProperty);
		ps.addProperty(imageProperty);
		ps.addProperty(clipProperty);
		return ps;
	}
	
	public boolean isScaleSmooth() {
		return scaleSmooth;
	}
	public void setScaleSmooth(boolean scaleSmooth) {
		this.scaleSmooth = scaleSmooth;
	}
	public boolean isKeepAspect() {
		return keepAspect;
	}
	public void setKeepAspect(boolean keepAspect) {
		this.keepAspect = keepAspect;
	}
	public boolean isAsGreyScale() {
		return asGreyScale;
	}
	public void setAsGreyScale(boolean asGreyScale) {
		this.asGreyScale = asGreyScale;
	}
	public boolean isOptimizePalette() {
		return optimizePalette;
	}
	public void setOptimizePalette(boolean optimizePalette) {
		this.optimizePalette = optimizePalette;
	}
	public Dither getDitherType() {
		return ditherType;
	}
	public void setDitherType(Dither dither) {
		this.ditherType = dither;
	}
	
	public boolean isDitherMono() {
		return ditherMono;
	}
	public void setDitherMono(boolean ditherMono) {
		this.ditherMono = ditherMono;
	}
	public void setImage(BufferedImage image) {
		if (image != this.image) {
			if (clip != null && !clip.isEmpty()) {
				clip = null;
				clipProperty.firePropertyChange();
			}
			this.image = image;
			imageProperty.firePropertyChange();
		}
	}
	public BufferedImage getImage() {
		if (image == null || clip == null || clip.isEmpty())
			return image;
		
        ColorModel cm = image.getColorModel();
        WritableRaster wr = image.getRaster().createCompatibleWritableRaster(clip.width, clip.height);
        wr.setRect(-clip.x, -clip.y, image.getRaster());

        return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);

	}

	
	public Rectangle getClip() {
		return clip;
	}
	public void setClip(Rectangle clip) {
		this.clip = clip;
	}
	public void setOrigPalette(byte[][] thePalette) {
		byte[][] newP = new byte[thePalette.length][];
		for (int i = 0; i < thePalette.length; i++) {
			newP[i] = Arrays.copyOf(thePalette[i], 3);
		}
		this.origPalette = newP;
	}
	public byte[][] getOrigPalette() {
		return origPalette;
	}
	/**
	 * Use this when the image has been dragged/dropped.
	 */
	public void updateFrom(BufferedImage image) {
		octree = null;
		setImage(image);
	}
	
	/**
	 * Call to reset options to the presumed best ones for the
	 * current video mode.
	 */
	public void resetOptions(IVdpCanvas canvas, IVdpChip vdp) {
		boolean canSetPalette;
		
		VdpFormat format = canvas.getFormat();
		
		if (vdp.getRegisterCount() > 10) {
			// hack: graphics mode 2 allows setting the palette too, 
			// but for comparison shopping, pretend we can't.
			if (format == VdpFormat.COLOR16_8x1 && (vdp.getRegister(0) & 0x6) == 0x2) {
				canSetPalette = false;
			} else {
				canSetPalette = format != VdpFormat.COLOR256_1x1;
			}
		} else {
			canSetPalette = false;
		}
		
		///////
		
		boolean isLowColor = false;
		
		if (format == VdpFormat.COLOR16_8x1) {
			if (!canvas.getColorMgr().isGreyscale())
				isLowColor = true;
		}
		
		setScaleSmooth(!isLowColor);
		
		////
		
		setOptimizePalette(canSetPalette);
		
		/////
		boolean isMonoMode = vdp instanceof IVdpTMS9918A && ((IVdpTMS9918A) vdp).isBitmapMonoMode();
		
		setDitherMono(isMonoMode);
		setDitherType(format == VdpFormat.COLOR16_8x1 && !canSetPalette ? Dither.ORDERED : Dither.FS);
		
		if (!canSetPalette) {
			setOrigPalette(vdp.getRegisterCount() > 10 ? VdpColorManager.stockPaletteV9938 : VdpColorManager.stockPalette);
		}
		else
			setOrigPalette(canvas.getColorMgr().getPalette());
		
		octree = null;
	}
	
	/**
	 * @return the octree
	 */
	public ColorOctree getOctree() {
		if (octree == null)
			octree = new ColorOctree(3, true, false);
		return octree;
	}
	/**
	 * @param octree the octree to set
	 */
	public void setOctree(ColorOctree octree) {
		this.octree = octree;
	}
}