/*
  DiskLikeDirectoryInfo.java

  (c) 2010-2012 Edward Swartz

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
package v9t9.engine.files.directory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import v9t9.common.files.IFileMapper;
import v9t9.common.files.NativeFile;
import v9t9.common.files.NativeFileFactory;
import v9t9.common.memory.ByteMemoryAccess;
import v9t9.engine.dsr.DsrException;

class DiskLikeDirectoryInfo extends DirectoryInfo {

	static class DiskFileRange {
		int fdrSector;
		int start;
		int len;
		NativeFile file;
	}
	Map<File, DiskFileRange> sectorRanges = new LinkedHashMap<File, DiskFileRange>();
	private String devname;
	int lastSector;
	
	public DiskLikeDirectoryInfo(File dir, IFileMapper mapper, String devname) {
		super(dir, mapper);
		this.devname = devname;
		
		int sec = 2;
		for (File entry : entries) {
			if (sec >= 65536)
				break;
			
			DiskFileRange range = new DiskFileRange();
			try {
				range.file = NativeFileFactory.INSTANCE.createNativeFile(entry);
				range.fdrSector = sec++;
				range.start = sec;
				range.len = range.file.getSectorsUsed();
				sec += range.len;
				sectorRanges.put(entry, range);
			} catch (IOException e) {
			}
		}
		this.lastSector = sec;
	}
	
	/**
	 * @return the lastSector
	 */
	public int getLastSector() {
		return lastSector;
	}

	public void synthesizeFDRSector(ByteMemoryAccess access, DiskFileRange range) throws DsrException {
		int offset = access.offset;
		
		Arrays.fill(access.memory, offset, offset + 256, (byte) 0);
		
		if (range == null) {
			return;
		}
		
		String name = mapper.getDsrFileName(range.file.getFile().getName());
		for (int i = 0; i < 10; i++) {
			if (i < name.length())
				access.memory[offset++] = (byte) name.charAt(i);
			else
				access.memory[offset++] = (byte) 0x20;
		}
		
		// reserved
		access.memory[offset++] = (byte) 0x0;
		access.memory[offset++] = (byte) 0x0;
		
		// file type
		access.memory[offset++] = (byte) range.file.getFlags();
		access.memory[offset++] = (byte) range.file.getRecordsPerSector();
		
		int numsecs = (int) range.file.getSectorsUsed();
		access.memory[offset++] = (byte) (numsecs / 256);
		access.memory[offset++] = (byte) (numsecs % 256);
		
		access.memory[offset++] = (byte) range.file.getByteOffset();
		access.memory[offset++] = (byte) range.file.getRecordLength();
		
		int numrecs = range.file.getNumberRecords();
		access.memory[offset++] = (byte) (numrecs % 256);
		access.memory[offset++] = (byte) (numrecs / 256);
		
		while (offset< 0x1C) {
			access.memory[offset++] = (byte) 0;
		}
		
		// sectors per track
		access.memory[offset++] = (byte) 18;
		
		int left = range.len;
		int ofs = range.start;
		while (left > 0) {
			// >UM >SN >OF == >NUM >OFS
			int num = Math.min(0xfff, left);
			access.memory[offset++] = (byte) (num & 0xff);
			access.memory[offset++] = (byte) (((num >> 8) & 0x0f) | ((ofs & 0xf) << 4));
			access.memory[offset++] = (byte) ((ofs >> 4) & 0xff);
			left -= num;
		}
		
	}

	public void synthesizeVolumeSector(ByteMemoryAccess access) throws DsrException {
		int offset = access.offset;
		
		File localFile = mapper.getLocalFile(devname, null);
		if (localFile == null)
			throw new DsrException(EmuDiskConsts.es_hardware, "No directory for " + devname);
		
		String diskname = mapper.getDsrFileName(localFile.getName());
		for (int i = 0; i < 10; i++) {
			if (i < diskname.length())
				access.memory[offset++] = (byte) diskname.charAt(i);
			else
				access.memory[offset++] = (byte) 0x20;
		}
		
		// # sectors
		long numsecs = Math.min(65535, (localFile.getTotalSpace() / 256));
		access.memory[offset++] = (byte) (numsecs / 256);
		access.memory[offset++] = (byte) (numsecs % 256);
		
		// sectors per track
		access.memory[offset++] = (byte) 18;
		
		// DSR mark
		access.memory[offset++] = 'D';
		access.memory[offset++] = 'S';
		access.memory[offset++] = 'K';
		
		// protection
		access.memory[offset++] = ' ';
		
		// # tracks/side
		access.memory[offset++] = 40;

		// # sides
		access.memory[offset++] = 2;
		
		// density
		access.memory[offset++] = 2;
	
		// reserved
		while (offset < access.offset + 0x38) {
			access.memory[offset++] = 0;
		}
		
		// bitmap
		while (offset < access.offset + 0xec) {
			access.memory[offset++] = (byte) 0xaa;
		}
		
		// reserved
		while (offset < access.offset + 0x100) {
			access.memory[offset++] = (byte) 0xff;
		}
	}
	
	public void synthesizeIndexSector(ByteMemoryAccess access) {
		int offset = access.offset;
		
		int endOffset = offset + 256;
		int cnt = 127;
		for (Map.Entry<File, DiskFileRange> entry : sectorRanges.entrySet()) {
			int fdrSector = entry.getValue().fdrSector;
			access.memory[offset++] = (byte) (fdrSector / 256);
			access.memory[offset++] = (byte) (fdrSector % 256);
			if (--cnt == 0)
				break;
		}
		while (offset < endOffset) {
			access.memory[offset++] = (byte) 0;
			access.memory[offset++] = (byte) 0;
		}
	}

	public void synthesizeSector(ByteMemoryAccess access, int secnum) throws IOException {
		for (Map.Entry<File, DiskFileRange> entry : sectorRanges.entrySet()) {
			DiskFileRange range = entry.getValue();
			if (secnum == range.fdrSector) {
				synthesizeFDRSector(access, range);
				return;
			} else if (secnum >= range.start && secnum < range.start + range.len) {
				range.file.readContents(access.memory, access.offset, 
						(secnum - range.start) * 256, 256);
				return;
			}
		}
		Arrays.fill(access.memory, access.offset, access.offset + 256, (byte) 0xe5);
	}
}