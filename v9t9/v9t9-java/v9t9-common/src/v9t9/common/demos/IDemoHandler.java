/*
  IDemoHandler.java

  (c) 2012 Edward Swartz

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.
 
  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  General Public License for more details.
 
  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
 */
package v9t9.common.demos;

import java.net.URI;

import v9t9.common.client.ISettingsHandler;
import v9t9.common.events.NotifyEvent;
import v9t9.common.events.NotifyException;
import v9t9.common.settings.SettingSchema;

/**
 * This interface covers the low-level details of recording and playing demos. 
 * @author ejs
 *
 */
public interface IDemoHandler {

	
	/** This setting is true while a demo is being recorded. */
	SettingSchema settingRecordDemo = new SettingSchema(
				ISettingsHandler.TRANSIENT,
				"RecordDemo", Boolean.FALSE);

	/** This setting is true while a demo is being played back. */
	SettingSchema settingPlayingDemo = new SettingSchema(
			ISettingsHandler.TRANSIENT,
			"PlayingDemo", Boolean.FALSE);

	/** This setting is true while a demo is paused:
	 * 
	 * -- when recording, nothing is recorded, but changes are tracked to allow a clean
	 * transition when recording continues.
	 * 
	 * -- when playing, machine timer events are ignored.
	 *  */
	SettingSchema settingDemoPaused = new SettingSchema(
			ISettingsHandler.TRANSIENT,
			"DemoPaused", Boolean.FALSE);

	/** This setting is the playback rate (multiplier) for demo playback. */
	SettingSchema settingDemoPlaybackRate = new SettingSchema(
			ISettingsHandler.TRANSIENT,
			"DemoPlaybackRate", 1.0);

	/** 
	 * This setting is true while a demo is playing backwards.
	 *  */
	SettingSchema settingDemoReversing = new SettingSchema(
			ISettingsHandler.TRANSIENT,
			"DemoReversing", Boolean.FALSE);


	interface IDemoListener {
		void firedEvent(NotifyEvent event);
	}
	interface IDemoPlaybackListener extends IDemoListener {
		void started(IDemoPlayer player);
		void updatedPosition(double playClock);
		void stopped();
	}
	
	void dispose();

	void addListener(IDemoListener listener);
	void removeListener(IDemoListener listener);
	
	void startRecording(URI uri) throws NotifyException;
	void stopRecording() throws NotifyException;
	URI getRecordingURI();
	
	void startPlayback(URI uri) throws NotifyException;
	void stopPlayback() throws NotifyException;
	URI getPlaybackURI();

	/** Tell if the demo's contents are recognized by this machine */
	boolean isDemoSupported(URI uri);
}
