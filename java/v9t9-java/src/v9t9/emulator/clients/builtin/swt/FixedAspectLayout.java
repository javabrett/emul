/**
 * 
 */
package v9t9.emulator.clients.builtin.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;

/**
 * This layout ensures that the aspect ratio remains consistent with the provided
 * width and height, allowing for zoom.
 * @author ejs
 *
 */
public class FixedAspectLayout extends Layout {

	private int w;
	private int h;
	private double zoomx;
	private double zoomy;
	private double aspect;
	private final double quantum;
	private final double maxforquantum;

	public FixedAspectLayout(int w, int h, double zoomx, double zoomy, double quantum, double max) {
		this.w = w;
		this.h = h;
		this.quantum = quantum;
		this.maxforquantum = max;
		this.zoomx = zoomx;
		this.zoomy = zoomy;
		this.aspect = (double) w / h;
	}
	
	public int getHeight() {
		return h;
	}
	
	public int getWidth() {
		return w;
	}

	public void setSize(int w, int h) {
		this.w = w;
		this.h = h;
		this.aspect = (double) w / h;
	}
	
	public void setAspect(double aspect) {
		this.aspect = aspect;
	}
	
	public double getZoomX() {
		return zoomx;
	}
	
	public double getZoomY() {
		return zoomy;
	}
	
	@Override
	protected Point computeSize(Composite composite, int wHint, int hHint,
			boolean flushCache) {
		Rectangle area = composite.getClientArea();
		Rectangle bounds = composite.getParent().getClientArea();
		//System.out.println("cursize: " + area + " vs " +bounds);
		
		int neww, newh;
		if (wHint == SWT.DEFAULT) {
			neww = fixup(area.width, bounds.width, w);
		} else {
			neww = fixup(wHint, wHint, w);
		}
		if (hHint == SWT.DEFAULT) {
			newh = fixup(area.height, bounds.height, h);
		} else {
			newh = fixup(hHint, hHint, h);
		}
		
		if (neww < newh * aspect) {
			newh = (int) (newh / aspect);
		}
		else if (neww > newh * aspect) {
			neww = (int) (newh * aspect);
		}
		
		Point desired = new Point(neww, newh);
		
		//System.out.println("desired at " + desired);
		
		return desired;
	}

	private int fixup(@SuppressWarnings("unused") int hint, int max, int base) {
		// get the hint close to a multiple of base
		int q = base;
		if (max / base <= maxforquantum)
			q = (int) (base * quantum);
		int val = q >> 1;
		if (val * 2 > max)
			return val;
		
		return (max / q) * q;
	}
	
	@Override
	protected void layout(Composite composite, boolean flushCache) {
		Rectangle area = composite.getClientArea();
		//System.out.println("layout at " + area);
		zoomx = (double) area.width / w;
		if (zoomx < 1)
			zoomx = 0.5;
		else
			zoomx = (int) (Math.round(zoomx / quantum) * quantum);
		zoomy = (double) area.height / h;
		if (zoomy < 1)
			zoomy = 0.5;
		else
			zoomy = (int) (Math.round(zoomy / quantum) * quantum);
	}

	/**
	 * @param zoom
	 */
	public void setZoomX(double zoom) {
		this.zoomx = zoom;
	}
	public void setZoomY(double zoom) {
		this.zoomy = zoom;
	}
	
}