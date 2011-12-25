package ejs.base.sound;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;



/**
 * Java sound output handler.
 * 
 * This blocks on {@link #played(SoundChunk)} if data is coming too fast.
 * @author ejs
 * 
 */
public class JavaSoundListener implements ISoundListener {

	private SourceDataLine soundGeneratorLine;
	private AudioFormat soundFormat;
	//private int soundFramesPerTick;

	private Thread soundWritingThread;

	private BlockingQueue<AudioChunk> soundQueue;
	private int ticksPerSec;
	private double volume;

	public JavaSoundListener(int ticksPerSec) {
		this.ticksPerSec = ticksPerSec;
		volume = 1.0;
		
	}

	/* (non-Javadoc)
	 * @see org.ejs.chiprocksynth.SoundListener#stopped()
	 */
	public synchronized void stopped() {
		waitUntilSilent();
		
		if (soundGeneratorLine != null) {
			soundGeneratorLine.close();
			soundGeneratorLine = null;
		}
		
		if (soundWritingThread != null) {
			soundWritingThread.interrupt();
			try {
				soundWritingThread.join();
			} catch (InterruptedException e) {
				
			}
			soundWritingThread = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.ejs.chiprocksynth.SoundListener#started(javax.sound.sampled.AudioFormat)
	 */
	public void started(AudioFormat format) {
		if (soundGeneratorLine != null) {
			if (soundFormat.equals(format))
				return;
			stopped();
		}
		
		soundQueue = new LinkedBlockingQueue<AudioChunk>(20);

		soundFormat = format;
		Line.Info slInfo = new DataLine.Info(SourceDataLine.class, soundFormat);
		if (!AudioSystem.isLineSupported(slInfo)) {
			System.err.println("Line not supported: " + soundFormat);
			return;
		}

		try {
			int soundFramesPerTick = (int) (soundFormat.getFrameRate() / ticksPerSec);
			soundGeneratorLine = (SourceDataLine) AudioSystem.getLine(slInfo);
			soundGeneratorLine.open(soundFormat, soundFramesPerTick * 20 * 4);
			
			//System.out.println("Sound format: " + soundFormat);

		} catch (LineUnavailableException e) {
			System.err.println("Line not available");
			e.printStackTrace();
			return;
		}


		soundWritingThread = new Thread(new Runnable() {

			public void run() {
				while (true) {
					AudioChunk chunk = null;

					try {
						chunk = soundQueue.take();
					} catch (InterruptedException e2) {
						return;
					}
					
					//if (chunk != null) dft(chunk.soundToWrite);
					
					// toss extra chunks if too many arrive
					/*
						while (chunk != null && soundQueue.size() > 2) {
							chunk = soundQueue.poll();
						}
					*/

					if (soundGeneratorLine == null)
						return;
					
					if (chunk.soundData != null) {
						soundGeneratorLine.write(chunk.soundData, 0,
								chunk.soundData.length);
					}
				}
			}

			

		}, "Sound Writing");
		soundWritingThread.setDaemon(true);
		soundWritingThread.start();

		soundGeneratorLine.start();
	}

	/* (non-Javadoc)
	 * @see v9t9.base.core.sound.ISoundListener#setVolume(double)
	 */
	public void setVolume(double loudness) {
		this.volume = Math.max(0.0, Math.min(1.0, loudness));
	}
	
	public void dispose() {
		waitUntilSilent();
		if (soundWritingThread != null)
			soundWritingThread.interrupt();
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.chiprocksynth.SoundListener#played(org.ejs.chiprocksynth.AudioChunk)
	 */
	public void played(SoundChunk chunk) {
		try {
			if (soundWritingThread == null) {
				if (soundQueue.remainingCapacity() == 0)
					soundQueue.remove();
			}
			// will block if sound is too fast
			AudioChunk o = new AudioChunk(chunk, volume);
			//System.out.println("Got chunk " + o + " at " + System.currentTimeMillis());
			soundQueue.put(o);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * 
	 */
	public void waitUntilEmpty() {
		if (soundGeneratorLine != null)
			soundQueue.clear();
		while (!soundQueue.isEmpty()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				break;
			}
		}
		
	}

	/**
	 * 
	 */
	public void waitUntilSilent() {
		if (soundGeneratorLine == null)
			return;
		soundGeneratorLine.drain();
	}

}