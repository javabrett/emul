package org.ejs.coffee.internal.core.sound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.AudioFormat;

import org.ejs.coffee.core.sound.ISoundListener;
import org.ejs.coffee.core.sound.ISoundOutput;
import org.ejs.coffee.core.sound.ISoundVoice;
import org.ejs.coffee.core.sound.SoundChunk;




/**
 * Mixing and output for sound
 * 
 * @author ejs
 * 
 */
public class SoundOutput implements ISoundOutput {

	private volatile float[] soundGeneratorWorkBuffer;
	private int soundClock;

	// private boolean audioSilence;

	protected volatile int lastUpdatedPos;
	private boolean anyChanged;

	List<ISoundListener> listeners;
	// samples * channels
	private final int bufferSize;
	private ISoundListener[] listenerArray;
	private AudioFormat format;

	public SoundOutput(AudioFormat format, int tickRate) {
		this.format = format;
		listeners = new ArrayList<ISoundListener>();
		int b = (int) (format.getFrameSize() * format.getFrameRate() / tickRate);
		this.bufferSize = (b + 3) & ~3;
		soundGeneratorWorkBuffer = new float[bufferSize];
		soundClock = (int) format.getSampleRate();
	}

	public void addListener(ISoundListener listener) {
		synchronized (this) {
			listeners.add(listener);
			listenerArray = (ISoundListener[]) listeners.toArray(new ISoundListener[listeners.size()]);
		}
		try {
			listener.started(format);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void removeListener(ISoundListener listener) {
		try {
			listener.stopped();
		} catch (Exception e) {
			e.printStackTrace();
		}
		synchronized (this) {
			listeners.remove(listener);
			listenerArray = (ISoundListener[]) listeners.toArray(new ISoundListener[listeners.size()]);
		}
	}
	public void fireListeners(SoundChunk chunk) {
		ISoundListener[] listeners;
		synchronized (this) {
			listeners = listenerArray;
		}
		if (listeners != null) {
			for (ISoundListener listener : listeners) {
				try {
					listener.played(chunk);
				} catch (Exception e ) {
					e.printStackTrace();
				}
			}
		}
	}

	public void setVolume(double loudness) {
		for (ISoundListener listener : listeners) {
			listener.setVolume(loudness);
		}
	}
	
	/**
	 * @return the soundClock
	 */
	public int getSoundClock() {
		return soundClock;
	}

	public void start() {
		
		ISoundListener[] listeners;
		synchronized (this) {
			listeners = listenerArray;
		}
		if (listeners != null) {
			for (ISoundListener listener : listeners)
			{
				try {
					listener.started(format);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void stop() {
		ISoundListener[] listeners;
		synchronized (this) {
			listeners = listenerArray;
		}
		if (listeners != null) {
			for (ISoundListener listener : listeners) {
				try {
					listener.stopped();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public int getSamples(int ms) {
		int samples = (int) (((long) ms * format.getSampleRate() * format.getChannels() + 999) / 1000);
		if (format.getChannels() > 1)
			samples -= samples % format.getChannels();
		return samples;
	}
	/**
	 * Generate samples
	 */
	public  void generate(ISoundVoice[] voices, int samples) {
		if (samples <= 0)
			return;
		
		// ensure stereo-safe
		int mask = ~(format.getChannels() - 1);
		samples = (samples + format.getChannels() - 1) & mask;
		while (samples > 0) {
			synchronized (this) {
				int endPos = lastUpdatedPos + samples;
				endPos &= mask;
				int to = endPos;
				if (to > bufferSize)
					to = bufferSize;
				if (lastUpdatedPos < to) {
				
					int active = 0;
					for (ISoundVoice v : voices) {
						if (v.isActive()) {
							//Arrays.fill(soundGeneratorWorkBuffer2, 0);
							anyChanged |= v.generate(soundGeneratorWorkBuffer, lastUpdatedPos, to);
							active++;
						}
					}
				}
				int generated = to - lastUpdatedPos;
				samples -= generated;
				lastUpdatedPos += generated;
			}
			//System.out.println(generated);
			if (samples > 0)
				flushAudio();
		}
	}

	public void dispose() {

	}

	public void flushAudio() {
		SoundChunk chunk;
		chunk = createChunk();
		if (chunk != null)
			fireListeners(chunk);
	}

	private SoundChunk createChunk() {
		SoundChunk chunk;
		synchronized (this) {
			if (soundGeneratorWorkBuffer == null || soundGeneratorWorkBuffer.length == 0 || lastUpdatedPos == 0)
				return null;
	
			if (anyChanged) {
				if (lastUpdatedPos < soundGeneratorWorkBuffer.length) {
					chunk = new SoundChunk(soundGeneratorWorkBuffer, lastUpdatedPos, format);
					soundGeneratorWorkBuffer = new float[bufferSize];
				} else  {
					chunk = new SoundChunk(soundGeneratorWorkBuffer, format);
					soundGeneratorWorkBuffer = new float[bufferSize];
				}
			} else {
				chunk = new SoundChunk(lastUpdatedPos, format);
			}
			lastUpdatedPos = 0;
			anyChanged = false;
		}
		return chunk;
	}


}
