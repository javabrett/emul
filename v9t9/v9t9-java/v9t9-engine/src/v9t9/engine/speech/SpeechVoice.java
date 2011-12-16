/**
 * 
 */
package v9t9.engine.speech;

import ejs.base.sound.ISoundVoice;

public class SpeechVoice implements ISoundVoice {
	
	private short lastSample;
	private short sample;

	public void setSoundClock(int soundClock) {
	}
	
	public void reset() {
		sample = 0;
		lastSample = 0;
	}
	
	public boolean isActive() {
		return true;
	}
	
	public boolean generate(float[] soundGeneratorWorkBuffer, int from, int to) {
		if (sample == 0 && lastSample == 0)
			return false;
		
		if (true) {
			// smoothly alter voltage
			float delta = (sample - lastSample) / 32768.f / (to - from);
			float samp = lastSample / 32768.f;
			
			while (from < to) {
				soundGeneratorWorkBuffer[from++] += samp;
				samp += delta;
			}
		} else {
			float delta = sample / 32768.f;
			
			while (from < to) {
				soundGeneratorWorkBuffer[from++] += delta;
			}
		}
		
		this.lastSample = this.sample;
		return true;
	}
	
	public void setSample(short sample) {
		this.sample = sample;
	}
}