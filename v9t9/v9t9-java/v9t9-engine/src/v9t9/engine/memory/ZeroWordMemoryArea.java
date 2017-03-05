/*
  ZeroWordMemoryArea.java

  (c) 2005-2013 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.memory;

/**
 * @author ejs
 */
public class ZeroWordMemoryArea extends WordMemoryArea {
	/* can neither read nor write directly */
	/* for reads, return zero */
	/* for writes, ignore */
    public static short zeroes[] = new short[0x10000/2];
    public ZeroWordMemoryArea() {
		this(0);
	}
	public ZeroWordMemoryArea(int latency) {
		super(latency);
		memory = zeroes;
	}
}

