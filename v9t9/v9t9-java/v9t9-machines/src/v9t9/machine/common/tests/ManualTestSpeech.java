/**
 * 
 */
package v9t9.machine.common.tests;

import java.io.FileOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.SourceDataLine;

import v9t9.common.client.ISettingsHandler;
import v9t9.common.files.DataFiles;
import v9t9.common.hardware.ISpeechChip;
import v9t9.common.machine.IMachine;
import v9t9.common.settings.BasicSettingsHandler;
import v9t9.common.speech.ISpeechDataSender;
import v9t9.common.speech.TMS5220Consts;
import v9t9.engine.speech.LPCSpeech;
import v9t9.engine.speech.SpeechTMS5220;
import v9t9.machine.ti99.machine.StandardMachineModel;

/**
 * @author ejs
 *
 */
public class ManualTestSpeech {

	/**
	 * @author ejs
	 *
	 */
	private final class SpeechDataSender implements ISpeechDataSender {
		/**
		 * 
		 */
		private final FileOutputStream fos;

		/**
		 * @param fos
		 */
		private SpeechDataSender(FileOutputStream fos) {
			this.fos = fos;
		}

		public void sendSample(short val, int pos, int length) {
			
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
//				if (pos == 0)
//					System.out.println();
//				System.out.print(val + " ");
		}

		/* (non-Javadoc)
		 * @see v9t9.common.speech.ISpeechDataSender#speechDone()
		 */
		@Override
		public void speechDone() {
			System.out.println("\n// done");
			

			speechLine.write(speechWaveForm, 0, speechIdx);
			speechLine.flush();
			
		}
	}
	/**
	 * 
	 */
	private static final short[] THAT_IS_RIGHT = new short[] {
	//	118,
		166, 209,
		198,37,104,82,151,
		206,91,138,224,232,
		116,186,18,85,130,
		204,247,169,124,180,
		116,239,185,183,184,
		197,45,20,32,131,
		7,7,90,29,179,
		6,90,206,91,77,
		136,166,108,126,167,
		181,81,155,177,233,
		230,0,4,170,236,
		1,11,0,170,100,
		53,247,66,175,185,
		104,185,26,150,25,
		208,101,228,106,86,
		121,192,234,147,57,
		95,83,228,141,111,
		118,139,83,151,106,
		102,156,181,251,216,
		167,58,135,185,84,
		49,209,106,4,0,
		6,200,54,194,0,
		59,176,192,3,0,
		0
		
	};

	public static void main(String[] args) throws Exception {
		ManualTestSpeech ts = new ManualTestSpeech();
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
		
		ISettingsHandler settings = new BasicSettingsHandler();
		
		DataFiles.addSearchPath(settings, "/usr/local/src/v9t9-data/roms");
		DataFiles.addSearchPath(settings, "l:/src/v9t9-data/roms");
		IMachine machine = new StandardMachineModel().createMachine(settings);
		SpeechTMS5220 tms5220 = (SpeechTMS5220) machine.getSpeech();
		
		settings.get(ISpeechChip.settingLogSpeech).setInt(1);
		settings.get(ISpeechChip.settingTalkSpeed).setDouble(1.5);
		
		LPCSpeech speech = tms5220.getLpcSpeech();
		speech.init();
		
		machine.start();
		machine.setPaused(true);
		
		final FileOutputStream fos = new FileOutputStream("/tmp/speech.raw");
		
		ISpeechDataSender sender = new SpeechDataSender(fos);
		
		tms5220.addSpeechListener(sender);
		
		// reset
		tms5220.command((byte) 0x70);
		
		if (true) {
			// should exit quickly (or not...)
			//sayPhrase(tms5220, 0xfff0);
			
			sayPhrase(tms5220, 0x351a);	// HELLO
			sayPhrase(tms5220, 0x71f4);	// UHOH
			
			sayDirect(tms5220, THAT_IS_RIGHT);
			
			sayPhrase(tms5220, 0x4642);	// MORE
	
			sayDirect(tms5220, THAT_IS_RIGHT);
			sayDirect(tms5220, THAT_IS_RIGHT);
		}
		
		///
		
		// bad usage, no waiting -- should delay anyway!
		sayPhrase(tms5220, 0x1D82);	// CHECK
		sayPhrase(tms5220, 0x2612);	// DRAW
		sayPhrase(tms5220, 0x1c48);	// BYE
		sayPhrase(tms5220, 0x3148);	// GOODBYE
		
		if (true) {
		sayPhrase(tms5220, 0x1a42, false);	// BE
		sayPhrase(tms5220, 0x4642, false);	// MORE
		sayPhrase(tms5220, 0x51b3, false);	// POSITIVE
		sayPhrase(tms5220, 0x1714, false);	// ABOUT
		sayPhrase(tms5220, 0x69b6, false);	// THE1
		sayPhrase(tms5220, 0x208b, false);	// CONNECTED
		sayPhrase(tms5220, 0x2034, false);	// COMPUTER
		sayPhrase(tms5220, 0x24ea, false);	// DOING
		sayPhrase(tms5220, 0x2599, false);	// DOUBLE
		sayPhrase(tms5220, 0x6e69, false);	// TIME
		sayPhrase(tms5220, 0x1769, false);	// AFTER
		sayPhrase(tms5220, 0x70ce, false);	// TWELVE
		sayPhrase(tms5220, 0x4e66, false);	// P
		sayPhrase(tms5220, 0x4233, false);	// M
		
		Thread.sleep(2000);
		}
		
		System.exit(0);
	}

	/**
	 * @param tms5220
	 * @param s
	 * @throws InterruptedException 
	 */
	private void sayDirect(SpeechTMS5220 tms5220, short[] s) throws InterruptedException {
	
		// wait for previous phrase to end
		while ((tms5220.read() & TMS5220Consts.SS_TS) != 0) {
			Thread.sleep(10);
		}

		// speak external
		tms5220.command((byte) 0x60);
		
		int toCopy = 16;
		for (int idx = 0; idx < s.length; ) {
			for (int cnt = 0; cnt < toCopy && idx < s.length; cnt++) {
				tms5220.write((byte) s[idx++]);
			}
			toCopy = 8;
			
			if (idx >= s.length)
				break;
			
			while ((tms5220.read() & TMS5220Consts.SS_BL + TMS5220Consts.SS_TS) == 
					TMS5220Consts.SS_TS) 
			{
				Thread.sleep(1);
				
			}
		}
		
	}
	private void sayPhrase(SpeechTMS5220 tms5220, int addr) throws InterruptedException {
		sayPhrase(tms5220, addr, true);
	}
	private void sayPhrase(SpeechTMS5220 tms5220, int addr, boolean wait) throws InterruptedException {
		if (wait) {
			// wait for previous phrase to end
			int stat;
			while (true) {
				stat = tms5220.read() & TMS5220Consts.SS_BL + TMS5220Consts.SS_TS;
				if ((stat & TMS5220Consts.SS_TS) == 0 || stat == 0 /* (stat & TMS5220Consts.SS_BL) != 0*/)
					break;
	//			if ((stat & TMS5220Consts.SS_BL) != 0)
	//				break;
				Thread.sleep(10);
			}
		} else {
			Thread.sleep((long) (Math.random() * 30 + 10));
		}
		
		// read to reset addr pointers
		tms5220.command((byte) 0x10);
		// set phrase addr
		//tms5220.setAddr(addr);
		
		for (int i = 0; i < 5; i++) {
			tms5220.command((byte) (0x40 | ((addr >> (i * 4)) & 0xf) ));
			
		}
		
		// speak
		tms5220.command((byte) 0x50);
		
		if (wait) {
			while ((tms5220.read() & TMS5220Consts.SS_TS) != 0) {
				Thread.sleep(100);
			}
		}
	}
}
