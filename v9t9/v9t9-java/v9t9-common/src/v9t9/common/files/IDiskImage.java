/**
 * Mar 1, 2011
 */
package v9t9.common.files;

import java.io.IOException;


public interface IDiskImage {
	void readImageHeader() throws IOException;
	void openDiskImage() throws IOException;
	void closeDiskImage() throws IOException;
	void writeImageHeader() throws IOException;
	byte[] readCurrentTrackData() throws IOException;
	long getTrackDiskOffset();
	void growImageForContent() throws IOException;
	

	String getDiskType();
	int getHeaderSize();
}