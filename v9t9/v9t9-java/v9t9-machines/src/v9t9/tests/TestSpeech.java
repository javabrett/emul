/**
 * 
 */
package v9t9.tests;

import java.io.FileOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.SourceDataLine;

import v9t9.emulator.hardware.speech.LPCSpeech;
import v9t9.emulator.hardware.speech.LPCSpeech.Sender;
import v9t9.emulator.hardware.speech.TMS5220;
import v9t9.engine.files.DataFiles;
import v9t9.engine.memory.MemoryDomain;

/**
 * @author ejs
 *
 */
public class TestSpeech {

	public static void main(String[] args) throws Exception {
		TestSpeech ts = new TestSpeech();
		ts.run();
		
	}

	private SourceDataLine speechLine;
	private byte[] speechWaveForm;
	private int speechIdx;

	private void run() throws Exception {
		AudioFormat speechFormat = new AudioFormat(8000, 16, 1, true, false);
		Line.Info spInfo = new DataLine.Info(SourceDataLine.class,
				speechFormat);
		if (!AudioSystem.isLineSupported(spInfo)) {
			System.err.println("Line not supported: " + speechFormat);
			System.exit(1);
		}
		
		
		int speechFramesPerTick = (int) (speechFormat.getFrameRate() / 100);
		speechLine = (SourceDataLine) AudioSystem.getLine(spInfo);
		speechLine.open(speechFormat, speechFramesPerTick * 10);
		speechLine.start();
		
		speechWaveForm = new byte[200 * 2];
		speechIdx = 0;
		
		MemoryDomain speechMem = new MemoryDomain("Speech");
		DataFiles.addSearchPath("/usr/local/src/v9t9-data/roms");
		DataFiles.addSearchPath("l:/src/v9t9-data/roms");
		TMS5220 tms5220 = new TMS5220(speechMem);
		
		TMS5220.settingLogSpeech.setInt(1);
		LPCSpeech speech = new LPCSpeech();
		speech.init();
		
		final FileOutputStream fos = new FileOutputStream("/tmp/speech.raw");
		
		Sender sender = new Sender() {

			public void send(short val, int pos, int length) {
				
				//val ^= 0x8000;
				if (speechIdx >= speechWaveForm.length) {
					speechLine.write(speechWaveForm, 0, speechWaveForm.length);
					speechIdx = 0;
				}
				speechWaveForm[speechIdx++] = (byte) (val & 0xff);
				speechWaveForm[speechIdx++] = (byte) (val >> 8);
				
				try {
					fos.write(val & 0xff);
					fos.write(val >> 8);
				} catch (IOException e) {
				}
				if (pos == 0)
					System.out.println();
				System.out.print(val + " ");
			}
			
		};
		
		tms5220.setSender(sender);
		
		tms5220.command((byte) 0x70);
		tms5220.command((byte) 0x10);
		tms5220.setAddr(0x71f4);
		tms5220.command((byte) 0x50);
		
		speechLine.write(speechWaveForm, 0, speechIdx);
		speechLine.flush();
		
	}
}