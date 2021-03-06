/*
  SoundMmioDataDemoActor.java

  (c) 2012-2016 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.demos.actors;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import v9t9.common.demos.IDemoActorProvider;
import v9t9.common.demos.IDemoEvent;
import v9t9.common.demos.IDemoPlaybackActor;
import v9t9.common.demos.IDemoPlayer;
import v9t9.common.demos.IDemoRecorder;
import v9t9.common.demos.IDemoRecordingActor;
import v9t9.common.demos.IDemoReversePlaybackActor;
import v9t9.common.hardware.ISoundChip;
import v9t9.common.machine.IMachine;
import v9t9.common.memory.FullMemoryWriteTracker;
import v9t9.common.memory.IMemoryDomain;
import v9t9.engine.demos.events.SoundWriteDataEvent;
import v9t9.engine.demos.events.SoundWriteRegisterEvent;
import v9t9.engine.sound.SoundTMS9919;

/**
 * @author ejs
 *
 */
public class SoundMmioDataDemoActor extends BaseDemoActor implements IDemoReversePlaybackActor {
	public static class Provider implements IDemoActorProvider {

		private final int mmioAddr;

		public Provider(int mmioAddr) {
			this.mmioAddr = mmioAddr;
		}

		@Override
		public String getEventIdentifier() {
			return SoundWriteDataEvent.ID;
		}
		
		@Override
		public IDemoPlaybackActor createForPlayback() {
			return new SoundMmioDataDemoActor(mmioAddr);
		}

		@Override
		public IDemoRecordingActor createForRecording() {
			return new SoundMmioDataDemoActor(mmioAddr);
		}

		@Override
		public IDemoReversePlaybackActor createForReversePlayback() {
			return new SoundMmioDataDemoActor(mmioAddr);
		}
		
	}


	public static class ReverseProvider implements IDemoActorProvider {
		public ReverseProvider() {
		}

		@Override
		public String getEventIdentifier() {
			return SoundWriteRegisterEvent.ID;
		}
		
		@Override
		public IDemoPlaybackActor createForPlayback() {
			return new SoundRegisterDemoActor();
		}

		@Override
		public IDemoRecordingActor createForRecording() {
			return null;
		}

		@Override
		public IDemoReversePlaybackActor createForReversePlayback() {
			return null;
		}
		
	}

	private FullMemoryWriteTracker soundDataListener;
	private int soundMmioAddr;
	private byte[] soundBytes = new byte[256];
	private int soundIdx;
	private IMemoryDomain console;
	
	private List<IDemoEvent> reversedEventsList;
	
	/**
	 * 
	 */
	public SoundMmioDataDemoActor(int mmioAddr) {
		this.soundMmioAddr = mmioAddr;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.demo.IDemoActor#getEventIdentifier()
	 */
	@Override
	public String getEventIdentifier() {
		return SoundWriteDataEvent.ID;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.demo.IDemoActor#setup(v9t9.common.machine.IMachine)
	 */
	@Override
	public void setup(IMachine machine) {
		console = machine.getConsole();
	}

	/* (non-Javadoc)
	 * @see v9t9.common.demo.IDemoActor#connectForRecording(v9t9.common.demo.IDemoRecorder)
	 */
	@Override
	public void connectForRecording(IDemoRecorder recorder) throws IOException {
		soundDataListener = new FullMemoryWriteTracker(console, 0);
		soundDataListener.addMemoryRange(soundMmioAddr, 1);
		soundDataListener.addMemoryListener();

		// send silence
		recorder.getOutputStream().writeEvent(createSilenceEvent());
	}

	/**
	 * @return
	 */
	protected SoundWriteDataEvent createSilenceEvent() {
		return new SoundWriteDataEvent(soundMmioAddr, 
				new byte[] { (byte) 0x9f, (byte) 0xbf, (byte) 0xdf, (byte) 0xff });
	}

	/* (non-Javadoc)
	 * @see v9t9.common.demo.IDemoActor#flushRecording(v9t9.common.demo.IDemoRecorder)
	 */
	@Override
	public void flushRecording(IDemoRecorder recorder) throws IOException {
		if (soundDataListener == null) return;
		synchronized (soundDataListener) {
			List<Integer> changes = soundDataListener.getChanges();
			synchronized (changes) {
				for (Integer chg : changes) { 
					if (soundIdx >= soundBytes.length) {
						recorder.getOutputStream().writeEvent(
								new SoundWriteDataEvent(soundMmioAddr, soundBytes, soundIdx));
						soundIdx = 0;
					}
					soundBytes[soundIdx++] = (byte) (chg & 0xff);
				}
				if (soundIdx > 0) {
					recorder.getOutputStream().writeEvent(
							new SoundWriteDataEvent(soundMmioAddr, soundBytes, soundIdx));
					soundIdx = 0;
				}
			}
			soundDataListener.clearChanges();
		}
	}

	/* (non-Javadoc)
	 * @see v9t9.common.demo.IDemoActor#disconnectFromRecording(v9t9.common.demo.IDemoRecorder)
	 */
	@Override
	public void disconnectFromRecording(IDemoRecorder recorder) {
		soundDataListener.removeMemoryListener();
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.demo.IDemoActor#executeEvent(v9t9.common.demo.IDemoPlayer, v9t9.common.demo.IDemoEvent)
	 */
	@Override
	public void executeEvent(IDemoPlayer player, IDemoEvent event)
			throws IOException {
		SoundWriteDataEvent ev = (SoundWriteDataEvent) event;
		for (int i = 0; i < ev.getLength(); i++) {
			console.writeByte(ev.getAddress(), ev.getData()[i]);
		}
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.demos.actors.BaseDemoActor#cleanupPlayback(v9t9.common.demo.IDemoPlayer)
	 */
	@Override
	public void cleanupPlayback(IDemoPlayer player) {
		super.cleanupPlayback(player);
		player.getMachine().getSound().reset();

	}
	
	/* (non-Javadoc)
	 * @see v9t9.engine.demos.actors.BaseDemoActor#setupReversePlayback(v9t9.common.demos.IDemoPlayer)
	 */
	@Override
	public void setupReversePlayback(IDemoPlayer player) {
		reversedEventsList = new LinkedList<IDemoEvent>();
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.demos.IDemoReversePlaybackActor#queueEventForReversing(v9t9.common.demos.IDemoPlayer, v9t9.common.demos.IDemoEvent)
	 */
	@Override
	public void queueEventForReversing(IDemoPlayer player, IDemoEvent event)
			throws IOException {
		ISoundChip sound = player.getMachine().getSound();
		if (sound instanceof SoundTMS9919) {
			SoundTMS9919 tms9919 = ((SoundTMS9919) sound);
			
			SoundWriteDataEvent ev = (SoundWriteDataEvent) event;
			for (int i = 0; i < ev.getLength(); i++) {
				byte byt = ev.getData()[i];
				int reg = tms9919.convertMmioToRegister(ev.getAddress(), byt);
				if (reg >= 0) {
					int origVal = sound.getRegister(reg);
					reversedEventsList.add(0, new SoundWriteRegisterEvent(reg, origVal));
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.demos.IDemoReversePlaybackActor#emitReversedEvents(v9t9.common.demos.IDemoPlayer)
	 */
	@Override
	public IDemoEvent[] emitReversedEvents(IDemoPlayer player)
			throws IOException {
		IDemoEvent[] evs = (IDemoEvent[]) reversedEventsList.toArray(new IDemoEvent[reversedEventsList.size()]);
		reversedEventsList.clear();
		return evs;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.demos.IDemoReversePlaybackActor#cleanupReversePlayback(v9t9.common.demos.IDemoPlayer)
	 */
	@Override
	public void cleanupReversePlayback(IDemoPlayer player) {
		
	}
}
