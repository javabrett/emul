/**
 * 
 */
package v9t9.emulator.clients.builtin;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import v9t9.emulator.Machine;
import v9t9.keyboard.KeyboardState;

/**
 * @author Ed
 *
 */
public class AwtKeyboardHandler extends BaseKeyboardHandler {

	private long lastKeystrokeTime;

	public AwtKeyboardHandler(Component component, final KeyboardState keyboardState, Machine machine) {
		super(keyboardState, machine);
		component.addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent e) {
				synchronized (keyboardState) {
					//keyboardState.pushQueuedKey();
					handleKey(true, e.getModifiers(), e.getKeyCode(), e.getKeyChar());
				}
			}

			public void keyReleased(KeyEvent e) {
				synchronized (keyboardState) {
					//keyboardState.pushQueuedKey();
					handleKey(false, e.getModifiers(), e.getKeyCode(), e.getKeyChar());
				}
			}

			public void keyTyped(KeyEvent e) {
			}
			
		});
	}

	protected void handleKey(boolean pressed, int modifiers, int keyCode, char ascii) {
		if (pasteTimer != null && pressed && keyCode == KeyEvent.VK_ESCAPE) {
			cancelPaste();
			return;
		}
		
		if (pasteTimer == null)
			lastKeystrokeTime = System.currentTimeMillis();
		
		//System.out.println("pressed="+pressed+"; modifiers="+Integer.toHexString(modifiers)+"; keyCode="+keyCode+"; ascii="+(int)ascii);
		
		if (ascii == KeyEvent.CHAR_UNDEFINED && keyCode < 128 && keyboardState.isAsciiDirectKey((char) keyCode)) {
			ascii = (char) keyCode;
		}
		if (ascii <= 32 && (modifiers & KeyEvent.CTRL_MASK) != 0) {
			// control char
			ascii = (char) keyCode;
		}
		if (ascii < 128) {
			if (Character.isLowerCase(ascii))
				ascii = Character.toUpperCase(ascii);
		}
		
		byte shift = 0;
		if ((modifiers & KeyEvent.SHIFT_DOWN_MASK + KeyEvent.SHIFT_MASK) != 0)
			shift |= KeyboardState.SHIFT;
		if ((modifiers & KeyEvent.CTRL_DOWN_MASK + KeyEvent.CTRL_MASK) != 0)
			shift |= KeyboardState.CTRL;
		if ((modifiers & KeyEvent.ALT_DOWN_MASK + KeyEvent.META_DOWN_MASK + KeyEvent.ALT_MASK + KeyEvent.META_MASK) != 0)
			shift |= KeyboardState.FCTN;
		
		boolean synthetic = true;
		 
		if ((ascii == 0 || ascii == 0xffff) || !keyboardState.postCharacter(pressed, synthetic, shift, ascii)) {
			byte fctn = (byte) (KeyboardState.FCTN | shift);
			
			switch (keyCode) {
			case KeyEvent.VK_SHIFT:
				keyboardState.setKey(pressed, synthetic, KeyboardState.SHIFT, 0);
				break;
			case KeyEvent.VK_CONTROL:
				keyboardState.setKey(pressed, synthetic, KeyboardState.CTRL, 0);
				break;
			case KeyEvent.VK_ALT:
			case KeyEvent.VK_META:
				keyboardState.setKey(pressed, synthetic, KeyboardState.FCTN, 0);
				break;
			case KeyEvent.VK_ENTER:
				keyboardState.setKey(pressed, synthetic, shift, '\r');
				break;
				
			case KeyEvent.VK_CAPS_LOCK:
				if (pressed) {
					keyboardState.setAlpha(!keyboardState.getAlpha());
				}
				break;
			case KeyEvent.VK_PAUSE:
				if (pressed && (shift & KeyboardState.CTRL) != 0)
					System.exit(0);
				break;
			case KeyEvent.VK_F1:
			case KeyEvent.VK_F2:
			case KeyEvent.VK_F3:
			case KeyEvent.VK_F4:
			case KeyEvent.VK_F5:
			case KeyEvent.VK_F6:
			case KeyEvent.VK_F7:
			case KeyEvent.VK_F8:
			case KeyEvent.VK_F9:
				keyboardState.setKey(pressed, synthetic, fctn, '1' + KeyEvent.VK_F1 - keyCode);	
				break;
				
			case KeyEvent.VK_UP:
			case KeyEvent.VK_KP_UP:
				keyboardState.setKey(pressed, synthetic, fctn, 'E');
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_KP_DOWN:
				keyboardState.setKey(pressed, synthetic, fctn, 'X');
				break;
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_KP_LEFT:
				keyboardState.setKey(pressed, synthetic, fctn, 'S');
				break;
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_KP_RIGHT:
				keyboardState.setKey(pressed, synthetic, fctn, 'D');
				break;
				
				
			case KeyEvent.VK_INSERT:
				keyboardState.setKey(pressed, synthetic, fctn, '2');	
				break;
				
			case KeyEvent.VK_PAGE_UP:
				keyboardState.setKey(pressed, synthetic, fctn, '6'); // (as per E/A and TI Writer)
				break;
			case KeyEvent.VK_PAGE_DOWN:
				keyboardState.setKey(pressed, synthetic, fctn, '4'); // (as per E/A and TI Writer)
				break;

			case KeyEvent.VK_HOME:
				keyboardState.setKey(pressed, synthetic, fctn, '5');		// BEGIN
				break;
			case KeyEvent.VK_END:
				keyboardState.setKey(pressed, synthetic, fctn, '0');		// Fctn-0
				break;
				
			default:
				System.out.println("Unhandled keycode: " + keyCode);
			}
		}
	}

	/* (non-Javadoc)
	 * @see v9t9.engine.KeyboardHandler#scan(v9t9.keyboard.KeyboardState)
	 */
	public void scan(KeyboardState state) {
		// all handled incrementally, but just in case something goes goofy...
		if (lastKeystrokeTime + 500 < System.currentTimeMillis()) {
			lastKeystrokeTime = System.currentTimeMillis();
			state.resetKeyboard();
		}
	}

}
