/*
  TestTCFMemoryV2.java

  (c) 2011-2013 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.server.tcf.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.tm.tcf.protocol.IToken;
import org.eclipse.tm.tcf.protocol.Protocol;
import org.eclipse.tm.tcf.services.IMemory;
import org.eclipse.tm.tcf.services.IMemory.MemoryError;
import org.junit.Before;
import org.junit.Test;

import ejs.base.utils.Pair;

import v9t9.common.memory.IMemoryDomain;
import v9t9.server.tcf.services.IMemoryV2;
import v9t9.server.tcf.services.IMemoryV2.MemoryChange;

/**
 * @author ejs
 *
 */
public class TestTCFMemoryV2 extends BaseTCFTest {

	private IMemoryV2 memV2;
	
	@Before
	public void getServices() {
		memV2 = (IMemoryV2) getService(IMemoryV2.NAME);
	}
	

	static class MemListener implements IMemoryV2.MemoryContentChangeListener {
		final List<Pair<String, MemoryChange[]>> changeMap = new ArrayList<Pair<String, MemoryChange[]>>();
		@Override
		public void contentChanged(String notifyId, MemoryChange[] change) {
			changeMap.add(new Pair<String, MemoryChange[]>(notifyId,
					change));
		}
	};

	
	@Test
	public void testMemoryNotifySimple() throws Throwable {
		// start...
		final int DELAY = 100;
		int memMode = 0;
		final MemListener listener = new MemListener();
		doVideoMemWrite(memMode, DELAY, "hello", listener, "HELLO");
		
		assertTrue("expected event", !listener.changeMap.isEmpty());
		
		// expect only one change and one range
		assertEquals(1, listener.changeMap.size());
		assertEquals("hello", listener.changeMap.get(0).first);
		
		MemoryChange[] changes = listener.changeMap.get(0).second;
		assertNotNull(changes);
		assertEquals(1, changes.length);
		
		MemoryChange change = changes[0];
		assertEquals(0, change.addr);
		assertEquals(5, change.size);
		assertTrue(new String(change.data), Arrays.equals("HELLO".getBytes(), change.data));
	}
	

	@Test
	public void testMemoryNoNotifySimple() throws Throwable {
		final int DELAY = 100;
		final int memMode = IMemoryV2.MODE_FLAT;		// no events should be generated

		final MemListener listener = new MemListener();		
		doVideoMemWrite(memMode, DELAY, "none", listener, "blargh");
		
		assertTrue("expected no events: " + listener.changeMap, listener.changeMap.isEmpty());
	}

	protected void doVideoMemWrite(final int memMode, final int delay,
			final String notifyId,
			final MemListener listener,
			final String data)
			throws Throwable {
		doVideoMemWrite(memMode, notifyId, delay, 1, listener, data.getBytes(), 0, data.length(), 0, 0);	
	}
	
	/**
	 * @param memMode
	 * @param notifyId TODO
	 * @param delay
	 * @param granularity 
	 * @param listener
	 * @param method 
	 * @throws Throwable
	 */
	protected void doVideoMemWrite(final int memMode, 
			final String notifyId, final int delay, final int granularity, 
			final MemListener listener, final byte[] data,
			final int addr_, final int size, final int skip, final int skipLength)
			throws Throwable {
		
		Protocol.invokeAndWait(new Runnable() {
			public void run() {
				memV2.addListener(listener);
			}
		});

		
		final IMemory.MemoryContext video = getMemoryContext(memV2, IMemoryDomain.NAME_VIDEO);

		// save...
		final byte[] orig = new byte[size + 4];
		new TCFCommandWrapper() {
			public IToken run() throws Exception {
				return video.get((Integer) (addr_ - 2), 1, orig, 0, size + 4, 0,  
					new IMemoryV2.DoneMemory() {

						@Override
						public void doneMemory(IToken token, MemoryError error) {
							try {
								assertNoError(error);
							} catch (Throwable t) {
								excs[0] = t;
							} finally {
								tcfDone();
							}								
						}
				});
			}		
		};

		
		try {
			new TCFCommandWrapper() {
				public IToken run() throws Exception {
					return memV2.startChangeNotify(
							notifyId, IMemoryDomain.NAME_VIDEO, addr_, size,
							delay, granularity,
						new IMemoryV2.DoneCommand() {
							
							@Override
							public void done(Exception error) {
								try {
									assertNoError(error);
								} catch (Throwable t) {
									excs[0] = t;
								} finally {
									tcfDone();
								}								
							}
					});
				}		
			};

			// we should get one initial contentChanged report for the full range
			validateInitialContentChanged(notifyId, addr_, size, granularity, listener.changeMap);
			
			// this set tracks outstanding Memory#set events
			final boolean[] finished = { true };
			final Set<IToken> waiting = new HashSet<IToken>();
			
			final IMemory.DoneMemory done = new IMemory.DoneMemory() {
	
				@Override
				public void doneMemory(IToken token, MemoryError error) {
					assertNoError(error);
	
					synchronized (waiting) {
						waiting.remove(token);
						if (waiting.isEmpty())
							finished[0] = true;
					}
				}
			};
			
			int addr = addr_;
			int idx = 0;
			int end = addr + size;
			while (addr < end) {
				if (idx >= data.length)
					idx = 0;
				
				final int theAddr = addr;
				final int theIdx = idx;
				int toUse;
				
				toUse = Math.min(end - addr, Math.min(data.length - idx, size - idx));
				if (skip > 0)
					toUse = Math.min(skip, toUse);
				
				final int toUse_ = toUse;
				
				System.out.println("set: " + theAddr + "+" + toUse + " @ " + theIdx);
				new TCFCommandWrapper() {
					public IToken run() throws Exception {
						try {
							IToken token = video.set(Integer.valueOf(theAddr), 1, data, theIdx, toUse_, memMode, done);
							synchronized (waiting) {
								waiting.add(token);
							}
							return token;
						} finally {
							tcfDone();
						}
					}		
				};
				
				addr += toUse;
				idx += toUse;
				if (skip > 0) {
					if (toUse < skip) {
						// use more next time
						idx = 0;
					} else {
						addr += skipLength;
					}
				} else {
					if (toUse < size)
						idx = 0;
				}
			}

			// set outside range
			if (addr_ >= 2) {
				new TCFCommandWrapper() {
					public IToken run() throws Exception {
						try {
							IToken token = video.set(Integer.valueOf(addr_ - 2), 1, 
									new byte[] { '?', '!' }, 0, 2, memMode, done);
							synchronized (waiting) {
								waiting.add(token);
							}
							return token;
						} finally {
							tcfDone();
						}
					}		
				};
			}
			new TCFCommandWrapper() {
				public IToken run() throws Exception {
					try {
						IToken token = video.set(Integer.valueOf(addr_ + size), 1, 
								new byte[] { '?', '!' }, 0, 2, memMode, done);
						synchronized (waiting) {
							waiting.add(token);
						}
						return token;
					} finally {
						tcfDone();
					}
				}		
			};
			
			long timeout = System.currentTimeMillis() + 10 * 1000;
			while (!finished[0]) {
				if (System.currentTimeMillis() > timeout)
					fail("timed out waiting for memory writes");
				Thread.sleep(500);
			}
			
			new TCFCommandWrapper() {
				public IToken run() throws Exception {
					return memV2.stopChangeNotify(notifyId, 
						new IMemoryV2.DoneCommand() {
							
							@Override
							public void done(Exception error) {
								try {
									assertNoError(error);
								} catch (Throwable t) {
									excs[0] = t;
								} finally {
									tcfDone();
								}								
							}
					});
				}		
			};
			
			
			// await the event
			Thread.sleep(delay * 5);
		} finally {
			Protocol.invokeAndWait(new Runnable() {
				public void run() {
					memV2.removeListener(listener);
				}
			});
			
			// restore...
			new TCFCommandWrapper() {
				public IToken run() throws Exception {
					return video.set((Integer) (addr_ - 2), 1, orig, 0, (size + 4), 0,  
						new IMemoryV2.DoneMemory() {

							@Override
							public void doneMemory(IToken token, MemoryError error) {
								try {
									assertNoError(error);
								} catch (Throwable t) {
									excs[0] = t;
								} finally {
									tcfDone();
								}								
							}
					});
				}		
			};

		}
	}
	

	/**
	 * @param changeMap
	 * @param i
	 * @param j
	 */
	private void validateInitialContentChanged(String expId,
			int addr, int size, int granularity,
			List<Pair<String, MemoryChange[]>> changeMap) {

		assertEquals("initial change", 1, changeMap.size());
		Pair<String, MemoryChange[]> ent = changeMap.get(0);
		assertEquals(expId, ent.first);
		assertEquals("initial change", 1, ent.second.length);
		for (MemoryChange change : ent.second) {
			
			int alignedAddr = addr & -granularity;
			int alignedSize = (addr + size) - alignedAddr;
			alignedSize = (alignedSize + granularity - 1) & -granularity;
			
			assertEquals("initial change", alignedAddr, change.addr);
			assertEquals("initial change", alignedSize, change.size);
			assertEquals(change.toString(), change.size, change.data.length);
		}
		
		changeMap.clear();
	}


	/**
	 * @param changeMap
	 * @param i
	 * @param j
	 */
	private void validateRanges(String expId,
			byte[] data, int addr, int size,
			int skip, int skipLength, 
			List<Pair<String, MemoryChange[]>> changeMap) {
	
		// ensure the pairs are all contiguous
		for (Pair<String, MemoryChange[]> ent : changeMap) {
			Arrays.sort(ent.second, new Comparator<MemoryChange>() {
	
				@Override
				public int compare(MemoryChange o1, MemoryChange o2) {
					return o1.addr.intValue() - o2.addr.intValue();
				}
				
			});
	
		}
		
		// ensure each piece is in order
		Collections.sort(changeMap, new Comparator<Pair<String, MemoryChange[]>>() {
	
			@Override
			public int compare(Pair<String, MemoryChange[]> o1,
					Pair<String, MemoryChange[]> o2) {
				return o1.second[0].addr.intValue() - o2.second[0].addr.intValue();
			}
			
		});
		//int end = addr + size;
		int idx = 0;
		for (Pair<String, MemoryChange[]> ent : changeMap) {
			assertEquals(expId, ent.first);
			for (MemoryChange change : ent.second) {
				
				assertEquals(addr, change.addr);
				//assertEquals(change.toString(), skip > 0 ? Math.min(end - addr, skip) : end - addr, change.size);
				assertEquals(change.toString(), change.size, change.data.length);
				
				if (change.size < skipLength)
					addr += change.size;
				else
					addr += change.size + skipLength;
				idx = (idx + (int) change.size) % data.length;
				
			}
		}
		
		changeMap.clear();
	}

	@Test
	public void testMemoryNotifyScatter() throws Throwable {
		// start...
		final int DELAY = 100;
		int memMode = 0;
		final MemListener listener = new MemListener();
		
		byte[] data = { 42, 43, 44, 45, 46, 47, 48, 49, 50, 51 };
		
		// write 377 to 768 by 2
		doVideoMemWrite(memMode, "scatter1", DELAY, 1, listener, data, 377, 768 - 377, 2, 1);
		
		assertTrue("expected event", !listener.changeMap.isEmpty());
		
		validateRanges("scatter1", 
				data, 377, 768 - 377, 2, 1, listener.changeMap);
		
		
		// write 377 to 768 by 2, no gaps
		doVideoMemWrite(memMode, "scatter2", DELAY, 2, listener, data, 377, 768 - 377, 2, 1);
		
		assertTrue("expected event", !listener.changeMap.isEmpty());
		
		validateRanges("scatter2", 
				data, 376, 768 - 376, 0, 0, listener.changeMap);
		
		// huge chunk
		doVideoMemWrite(memMode, "scatter3", DELAY, 1024, listener, data, 123, 15, 2, 7);
		
		assertTrue("expected event", !listener.changeMap.isEmpty());
		
		validateRanges("scatter3", 
				data, 0, 1024, 0, 0, listener.changeMap);
	}
	

}
