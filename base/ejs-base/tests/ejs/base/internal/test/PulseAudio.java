/*
  PulseAudio.java

  (c) 2010-2012 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package ejs.base.internal.test;


import com.sun.jna.ptr.IntByReference;

import ejs.base.sound.PulseAudioLibrary;
 
/** Simple example of native library declaration and usage. */
public class PulseAudio {
	public static void main(String[] args) throws InterruptedException {
    	PulseAudioLibrary.pa_sample_spec ss = new PulseAudioLibrary.pa_sample_spec();
    	ss.format = PulseAudioLibrary.PA_SAMPLE_S16LE;
    	ss.rate = 44100;
    	ss.channels = 2;
    	
    	IntByReference error = new IntByReference();
    	
    	PulseAudioLibrary.pa_simple simple = PulseAudioLibrary.INSTANCE.pa_simple_new(
    			null,
    			"Java Pulse User",
    			PulseAudioLibrary.PA_STREAM_PLAYBACK,
    			null,
    			"Java Pulse",
    			ss,
    			null,
    			null,
    			error);

    	System.out.println(simple);
    	
    	byte[] wave = new byte[44100 * 2 * 5 * 2];
    	for (int i = 0; i < 44100 * 2 * 5; i += 4) {
    		short left = (short) (Math.sin(i * 440 * Math.PI / 2 / 44100.) * 32768); 
    		short right = (short) (Math.sin(i * 330 * Math.PI / 2 / 44100.) * 32768); 
    		wave[i] = (byte) (left & 0xff); 
    		wave[i + 1] = (byte) (left >> 8); 
    		wave[i + 2] = (byte) (right & 0xff);
    		wave[i + 3] = (byte) (right >> 8);
    	}
    	
    	for (int index = 0; index < 44100 * 2 * 5 * 2; index += 4410 * 2 * 2) {
    		//ByteBuffer buffer = memory.getByteBuffer(0, 4410 * 2 * 2);
    		//buffer.rewind();
    		//buffer.put(wave, index, 4410 * 2 * 2);
    		PulseAudioLibrary.INSTANCE.pa_simple_write(
    				simple, 
    				wave, 
    				4410 * 2 * 2, 
    				error);
    		//System.out.println(error.getValue());
    	}
    	
    	PulseAudioLibrary.INSTANCE.pa_simple_free(simple);
    	
    }
}
