/*
  TestBitInputStream.java

  (c) 2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package ejs.base.internal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;

import ejs.base.utils.BitInputStream;


/**
 * @author ejs
 *
 */
public class TestBitInputStream {

	final static byte[] stockBytes = { 0x30, 0x0e, (byte) 0xcc, (byte) 0xba, (byte) 0x80, 0x75, 0x26, 0 };
	
	final ByteArrayInputStream stockBis = new ByteArrayInputStream(stockBytes);
	final ByteArrayInputStream emptyBis = new ByteArrayInputStream(new byte[0]);
	
	@Test
	public void testEmpty() throws Exception {
		BitInputStream bs = new BitInputStream(emptyBis);
		try {
			bs.readBits(1);
			fail();
		} catch (IOException e) {
			// eof
		}
		bs.close();
	}
	@Test
	public void testIllegal() throws Exception  {
		BitInputStream bs = new BitInputStream(emptyBis);
		try {
			bs.readBits(-1);
			fail();
		} catch (IllegalArgumentException e) {
			
		}
		try {
			bs.readBits(16);
			fail();
		} catch (UnsupportedOperationException e) {
		} catch (IllegalArgumentException e) {
			
		}
		bs.close();
	}
	
	@Test
	public void testRead() throws Exception {
		BitInputStream bs = new BitInputStream(stockBis);
		assertEquals(3, bs.readBits(4));
		assertEquals(0, bs.readBits(1));
		assertEquals(0, bs.readBits(6));
		assertEquals(14, bs.readBits(5));
		assertEquals(25, bs.readBits(5));
		assertEquals(9, bs.readBits(4));
		assertEquals(7, bs.readBits(4));
		
		assertEquals(5, bs.readBits(4));
		assertEquals(0, bs.readBits(1));
		assertEquals(0, bs.readBits(6));
		assertEquals(14, bs.readBits(5));
		assertEquals(20, bs.readBits(5));
		assertEquals(9, bs.readBits(4));
		assertEquals(8, bs.readBits(4));
		
		try {
			bs.readBits(6);
		} catch (IOException e) {
			
		}
		bs.close();
	}
	
}
