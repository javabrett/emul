/**
 * 
 */
package v9t9.audio.sound;

/**
 * Tone generator 
 * @author ejs
 *
 */
public class EnhancedToneGeneratorVoice extends ToneGeneratorVoice implements EnhancedVoice {

	private EffectsController effectsController;

	public EnhancedToneGeneratorVoice(String name, int number) {
		super(name, number);
		effectsController = new EffectsController(this);
	}

	public EffectsController getEffectsController() {
		return effectsController;
	}

	@Override
	protected boolean updateAccumulator() {
		return effectsController.updateDivisor();
	}
	@Override
	public float getCurrentMagnitude() {
		return (float) effectsController.getCurrentSample() / 0x007FFFFF;
	}
	@Override
	protected void updateEffect() {
		effectsController.updateEffect();
	}
	@Override
	public boolean isActive() {
		return super.isActive() || effectsController.isActive();
	}
	

	/* (non-Javadoc)
	 * @see v9t9.audio.sound.ClockedSoundVoice#setPeriod(int)
	 */
	@Override
	public void setPeriod(int period) {
		super.setPeriod(period);
		effectsController.updateFrequency();
	}
	
	/* (non-Javadoc)
	 * @see v9t9.audio.sound.SoundVoice#setVolume(int)
	 */
	@Override
	public void setVolume(int volume) {
		super.setVolume(volume);
		if (volume == 0)
			getEffectsController().stopEnvelope();
		else
			getEffectsController().updateVoice();

	}
	
}
