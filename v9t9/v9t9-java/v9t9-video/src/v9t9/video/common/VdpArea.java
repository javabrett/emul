/**
 * 
 */
package v9t9.video.common;

import ejs.base.utils.HexUtils;

public class VdpArea
{
	public int	base, size;
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Base: " + Integer.toHexString(base) + "; Size: " + HexUtils.toHex4(size);
	}
}