/*
  ImageImportDialogOptions.java

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
package v9t9.video.imageimport;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import v9t9.common.hardware.IVdpChip;
import v9t9.common.video.IVdpCanvas;
import v9t9.common.video.VdpFormat;
import ejs.base.properties.FieldProperty;
import ejs.base.properties.PropertySource;

/**
 * @author ejs
 *
 */
public class ImageImportDialogOptions extends ImageImportOptions {
	protected boolean scaleSmooth = true;
	protected boolean keepAspect = true;

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
	
	private ImageFrame[] frames;
	private Rectangle clip;
	
	private FieldProperty scaleSmoothProperty;
	private FieldProperty keepAspectProperty;
	private FieldProperty asGreyScaleProperty;
	private FieldProperty imagesProperty;
	private FieldProperty clipProperty;
	
	/**
	 * @param iVdpChip 
	 * @param canvas 
	 * 
	 */
	public ImageImportDialogOptions(IVdpCanvas canvas, IVdpChip iVdpChip) {
		super(canvas, iVdpChip);
		scaleSmoothProperty = new FieldProperty(this, "scaleSmooth", "Smooth Scaling");
		keepAspectProperty = new FieldProperty(this, "keepAspect", "Keep Aspect Ratio");
		asGreyScaleProperty = new FieldProperty(this, "asGreyScale", "Convert To Greyscale");
		imagesProperty = new FieldProperty(this, "frames", "Last Image");
		clipProperty = new FieldProperty(this, "clip", "Clip Region");
		clipProperty.setHidden(true);
	}
	/**
	 * @return
	 */
	public void addToPropertySource(PropertySource ps) {
		ps.addProperty(scaleSmoothProperty);
		ps.addProperty(keepAspectProperty);
		ps.addProperty(asGreyScaleProperty);
		super.addToPropertySource(ps);
		ps.addProperty(imagesProperty);
		ps.addProperty(clipProperty);
	}
	
	public void setImages(ImageFrame[] images) {
		if (images != this.frames) {
			if (clip != null && !clip.isEmpty()) {
				clip = null;
				clipProperty.firePropertyChange();
			}
			this.frames = images;
			imagesProperty.firePropertyChange();
		}
	}
	public ImageFrame[] getImages() {
		if (frames == null || clip == null || clip.isEmpty())
			return frames;
		
		ImageFrame[] clips = new ImageFrame[frames.length];
		for (int i = 0; i < clips.length; i++) {
	        ColorModel cm = frames[i].image.getColorModel();
	        WritableRaster wr = frames[i].image.getRaster().createCompatibleWritableRaster(clip.width, clip.height);
	        wr.setRect(-clip.x, -clip.y, frames[i].image.getRaster());
	        clips[i] = new ImageFrame(
	        		new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null),
	        		frames[i].isLowColor,
	        		frames[i].delayMs);
		}

        return clips;

	}
	
	public int getWidth() {
		if (frames == null)
			return 1;
		return clip != null && !clip.isEmpty() ? clip.width : frames[0].image.getWidth();
	}
	public int getHeight() {
		if (frames == null)
			return 1;
		return clip != null && !clip.isEmpty() ? clip.height : frames[0].image.getHeight();
	}

	
	public Rectangle getClip() {
		return clip;
	}
	public void setClip(Rectangle clip) {
		this.clip = clip;
	}
	/**
	 * Use this when the image has been dragged/dropped.
	 */
	public void updateFrom(ImageFrame[] images) {
		octree = null;
		setImages(images);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.video.imageimport.ImageImportOptions#resetOptions()
	 */
	@Override
	public void resetOptions() {
		super.resetOptions();
		
		///////
		VdpFormat format = canvas.getFormat();
		boolean isLowColor = false;
		
		if (format == VdpFormat.COLOR16_8x1 || format == VdpFormat.COLOR16_8x1_9938) {
			if (!canvas.getColorMgr().isGreyscale())
				isLowColor = true;
		}
		
		setScaleSmooth(!isLowColor);
	}
	
	
}
