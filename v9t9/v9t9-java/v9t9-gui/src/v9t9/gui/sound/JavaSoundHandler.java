/**
 * 
 */
package v9t9.gui.sound;

import javax.sound.sampled.AudioFormat;


import v9t9.base.properties.IProperty;
import v9t9.base.properties.IPropertyListener;
import v9t9.base.properties.SettingProperty;
import v9t9.base.sound.AlsaSoundListener;
import v9t9.base.sound.ISoundListener;
import v9t9.base.sound.ISoundOutput;
import v9t9.base.sound.ISoundVoice;
import v9t9.base.sound.SoundFactory;
import v9t9.engine.EmulatorSettings;
import v9t9.engine.client.ISoundHandler;
import v9t9.engine.hardware.ISoundChip;
import v9t9.engine.hardware.ISpeechChip;
import v9t9.engine.machine.IMachine;

/**
 * Handle sound generation for output with Java APIs
 * @author ejs
 *
 */
public class JavaSoundHandler implements ISoundHandler {
	public static SettingProperty settingRecordSoundOutputFile = new SettingProperty("RecordSoundOutputFile", String.class, null);
	public static SettingProperty settingRecordSpeechOutputFile = new SettingProperty("RecordSpeechOutputFile", String.class, null);

	private SoundRecordingHelper soundRecordingHelper;
	private SoundRecordingHelper speechRecordingHelper;
	private AudioFormat soundFormat;
	private ISoundOutput output;
	private int lastUpdatedPos;
	private int soundFramesPerTick;
	private AudioFormat speechFormat;
	private ISoundOutput speechOutput;
	private ISoundListener audio;
	private ISoundListener speechAudio;
	private final IMachine machine;
	
	public JavaSoundHandler(final IMachine machine) {

		this.machine = machine;
		soundFormat = new AudioFormat(55930, 16, 2, true, false);
		speechFormat = new AudioFormat(8000, 16, 1, true, false);
		
		output = SoundFactory.createSoundOutput(soundFormat, machine.getCpuTicksPerSec());
		speechOutput = SoundFactory.createSoundOutput(speechFormat, machine.getCpuTicksPerSec());
		
		audio = SoundFactory.createAudioListener();
		if (audio instanceof AlsaSoundListener)
			((AlsaSoundListener) audio).setBlockMode(false);
		
		speechAudio = SoundFactory.createAudioListener();
		output.addListener(audio);
		speechOutput.addListener(speechAudio);
		
		EmulatorSettings.settingSoundVolume.addListener(new IPropertyListener() {
			
			@Override
			public void propertyChanged(IProperty setting) {
				output.setVolume(setting.getInt() / 10.0);
			}
		});

		EmulatorSettings.INSTANCE.register(EmulatorSettings.settingSoundVolume);
		EmulatorSettings.INSTANCE.register(EmulatorSettings.settingPlaySound);
		
		soundRecordingHelper = new SoundRecordingHelper(output, settingRecordSoundOutputFile, "sound");
		speechRecordingHelper = new SoundRecordingHelper(speechOutput, settingRecordSpeechOutputFile, "speech");
		
		// frames in ALSA means samples per channel, but raw freq in javax
		//soundFramesPerTick = (int) ((soundFormat.getFrameRate()
		//		+ machine.getCpuTicksPerSec() - 1) / machine.getCpuTicksPerSec());
		soundFramesPerTick = output.getSamples((1000 + machine.getCpuTicksPerSec() - 1) / machine.getCpuTicksPerSec());
		

		
		
		EmulatorSettings.settingPlaySound.addListener(new IPropertyListener() {

			public void propertyChanged(IProperty setting) {
				toggleSound(setting.getBoolean());
			}
			
		});
		
		toggleSound(EmulatorSettings.settingPlaySound.getBoolean());
	}

	public void toggleSound(boolean enabled) {
		if (enabled) {
			output.start();
			speechOutput.start();
		} else {
			output.stop();
			speechOutput.stop();
		}
	}

	public synchronized void generateSound() {
		int pos = machine.getCpu().getCurrentCycleCount();
		int total = machine.getCpu().getCurrentTargetCycleCount();
		
		if (total == 0)
			return;
		
		int currentPos = (int) ((long) (pos * soundFramesPerTick * soundFormat.getChannels() + total - 1 ) / total);
		if (currentPos < 0)
			currentPos = 0;
		updateSoundGenerator(lastUpdatedPos, currentPos);
		lastUpdatedPos = currentPos;
	}

	protected synchronized void updateSoundGenerator(int from, int to) {
		ISoundChip sound = machine.getSound();
		if (sound == null)
			return;
		
		if (to > soundFramesPerTick)
			to = soundFramesPerTick;
		if (from >= to)
			return;

		ISoundVoice[] vs = sound.getSoundVoices();

		output.generate(vs, to - from);
	}

	public void dispose() {
		toggleSound(false);

		if (soundRecordingHelper != null) {
			soundRecordingHelper.dispose();
			soundRecordingHelper = null;
		}
		if (speechRecordingHelper != null) {
			speechRecordingHelper.dispose();
			speechRecordingHelper = null;
		}
		if (output != null) {
			output.dispose();
			output = null;
		}
		
		if (speechOutput != null) {
			speechOutput.dispose();
			speechOutput = null;
		}
	}

	public synchronized void speech() {
		ISpeechChip speech = machine.getSpeech();
		if (speech == null)
			return;

		ISoundVoice[] vs = speech.getSpeechVoices();
		speechOutput.generate(vs, 1);
	}

	public synchronized void flushAudio() {
		if (output != null) {
			// hmm, it would be nice if the audio gate could work perfectly,
			// but the calculations of its data have assumed the "limit" would
			// match reality, when sometimes the tick comes earlier or later.
			// int length = lastUpdatedPos;
			/*
			 * int length = (int) ((long)pos soundGeneratorWaveForm.length / limit);
			 * if (length < lastUpdatedPos) length = lastUpdatedPos; if (length >
			 * soundGeneratorWaveForm.length) length =
			 * soundGeneratorWaveForm.length; byte[] part = new byte[length];
			 * System.arraycopy(soundGeneratorWaveForm, 0, part, 0, length);
			 * Arrays.fill(soundGeneratorWaveForm, (byte) 0); soundQueue.add(new
			 * AudioChunk(part, null, null));
			 */
	
			updateSoundGenerator(lastUpdatedPos, soundFramesPerTick);
			lastUpdatedPos = 0;
	
			output.flushAudio();
		}
		
		if (speechOutput != null) {
			speechOutput.flushAudio();
		}
	}

	/**
	 * @return the soundRecordingHelper
	 */
	public SoundRecordingHelper getSoundRecordingHelper() {
		return soundRecordingHelper;
	}
	
	/**
	 * @return the speechRecordingHelper
	 */
	public SoundRecordingHelper getSpeechRecordingHelper() {
		return speechRecordingHelper;
	}
}
