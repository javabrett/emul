/**
 * 
 */
package v9t9.common.video;


/**
 * @author ejs
 *
 */
public interface IVdpCanvasRenderer {

	void dispose();
	
	boolean update();
	
	VdpModeInfo getModeInfo();
	

	/**
	 * @return
	 */
	IVdpCanvas getCanvas();

}