/**
 * 
 */
package v9t9.emulator.clients.builtin.swt;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.ejs.coffee.core.utils.HexUtils;

import v9t9.emulator.clients.builtin.BaseKeyboardHandler;
import v9t9.emulator.common.Machine;
import v9t9.keyboard.KeyboardState;

/**
 * SWT keyboard control. 
 * 
 * We establish a display-level filter because SWT doesn't route keyboard events
 * predictably to the widgets you'd expect.
 * <p>
 * Sadly, due to criminally bad support for keyup events in Win32, we need to
 * generically assume they won't come in, using a linked list of "known pressed"
 * keys along with timeouts for each so we know when to assume a key is dead.
 * (Luckily, key repeats come in for most keys, except shift type keys.)
 * <p>
 * This can happen when, for instance, you hold down E, then press X, then
 * release E -- you'll only see keydowns for E and X. 
 * <p>
 * 
 * @author ejs
 * 
 */
public class SwtKeyboardHandler extends BaseKeyboardHandler implements ISwtKeyboardHandler {

	
	static class KeyInfo {
		public KeyInfo(int keyCode, long when) {
			this.keyCode = keyCode;
			this.when = when;
		}
		int keyCode;
		public long when;
	}

	private LinkedList<KeyInfo> pressedKeys = new LinkedList<KeyInfo>();
	private int pressedStateMask;
	
	private Timer pasteTimer;

	public SwtKeyboardHandler(KeyboardState keyboardState, Machine machine) {
		super(keyboardState, machine);
		
	}

	/**
	 * Update the information about pressed keys
	 * @param pressed
	 * @param stateMask
	 * @param keyCode
	 */
	private void recordKey(boolean pressed, int stateMask, int keyCode, long now) {
		//long now = System.currentTimeMillis();
		if (pasteTimer != null && pressed && keyCode == SWT.ESC) {
			cancelPaste();
			return;
		}
		
		//System.out.println("recordKey: pressed="+pressed+"; statemask="+Integer.toHexString(stateMask)
		//		+"; keyCode="+keyCode);

		synchronized (pressedKeys) {
			for (Iterator<KeyInfo> iter = pressedKeys.iterator();
				iter.hasNext(); ) { 
				KeyInfo info = iter.next();
				if (info.keyCode == keyCode) {
					if (!pressed) {
						iter.remove();
					}
				}
			}
			
			if (pressed) {
				pressedKeys.add(new KeyInfo(keyCode, now));
			}
			
			// shift keys are reported sometimes in keycode and sometimes in stateMask
			if (keyCode >= 0x10000 && (keyCode & SWT.KEYCODE_BIT) == 0)
				if (pressed)
					pressedStateMask |= keyCode;
				else
					pressedStateMask &= ~keyCode;
			else
				pressedStateMask = stateMask;
		
		}
		
		// immediately record it
		synchronized (keyboardState) {
			updateKey(pressed, stateMask, keyCode, now);
		}
	}
	
	private void updateKey(boolean pressed, int stateMask, int keyCode, long when) {
		
		//System.out.println("keyCode="+keyCode+"; stateMask="+stateMask+"; pressed="+pressed);
		byte shift = 0;
		
		int realKey = keyCode;
		
		// separately pressed keys show up in keycode sometimes
		
		if (((stateMask | keyCode) & SWT.CTRL) != 0)
			shift |= KeyboardState.CTRL;
		if (((stateMask | keyCode) & SWT.SHIFT) != 0)
			shift |= KeyboardState.SHIFT;
		if (((stateMask | keyCode) & SWT.ALT) != 0)
			shift |= KeyboardState.FCTN;
		
		if ((keyCode & SWT.KEYCODE_BIT) == 0) {
			keyCode &= 0xff;
			if (Character.isLowerCase(keyCode))
				keyCode = Character.toUpperCase(keyCode);
		}
		
		//byte realshift = keyboardState.getRealShift();
		//byte realshift = shift;
		
		int joy = (shift & KeyboardState.SHIFT) != 0 ? 2 : 1;
		
		if (keyCode > 128 || !keyboardState.postCharacter(machine, realKey, pressed, false, shift, (char) keyCode, when)) {
			if (keyCode == 0)
				keyCode = shift;
			
			int fctnShifted = shift | KeyboardState.FCTN;
			
			//System.out.println("Handling non-postable key: " + keyCode + "; shift="+shift);
			switch (keyCode) {

				// shifts
			case SWT.SHIFT:
			case KeyboardState.SHIFT:
				keyboardState.setKey(realKey, pressed, false, KeyboardState.SHIFT, 0, when);
				break;
			case SWT.CONTROL:
			case KeyboardState.CTRL:
				keyboardState.setKey(realKey, pressed, false, KeyboardState.CTRL, 0, when);
				break;
			case SWT.ALT:
			case KeyboardState.FCTN:
				keyboardState.setKey(realKey, pressed, false, KeyboardState.FCTN, 0, when);
				break;

			case SWT.CAPS_LOCK:
				if (pressed) {
					keyboardState.setAlpha(!keyboardState.getAlpha());
				}
				break;
			case SWT.BREAK:
				if (pressed)
					System.exit(0);
				break;
				
			case SWT.ESC:
				keyboardState.setKey(realKey, pressed, false, KeyboardState.FCTN, '9', when);
				break;

			case SWT.F1:
			case SWT.F2:
			case SWT.F3:
			case SWT.F4:
			case SWT.F5:
			case SWT.F6:
			case SWT.F7:
			case SWT.F8:
			case SWT.F9:
				keyboardState.setKey(realKey, pressed, false, fctnShifted, '1' + SWT.F1 - keyCode, when);	
				break;
				
			case SWT.ARROW_UP:
				keyboardState.setKey(realKey, pressed, false, fctnShifted, 'E', when);
				break;
			case SWT.ARROW_DOWN:
				keyboardState.setKey(realKey, pressed, false, fctnShifted, 'X', when);
				break;
			case SWT.ARROW_LEFT:
				keyboardState.setKey(realKey, pressed, false, fctnShifted, 'S', when);
				break;
			case SWT.ARROW_RIGHT:
				keyboardState.setKey(realKey, pressed, false, fctnShifted, 'D', when);
				break;
				
				
			//case SWT.DEL:
			//	keyboardState.setKey(pressed, fctnShifted, '1');	
			//	break;
			case SWT.INSERT:
				keyboardState.setKey(realKey, pressed, false, fctnShifted, '2', when);	
				break;
				
			case SWT.PAGE_UP:
				keyboardState.setKey(realKey, pressed, false, fctnShifted, '6', when); // (as per E/A and TI Writer)
				break;
			case SWT.PAGE_DOWN:
				keyboardState.setKey(realKey, pressed, false, fctnShifted, '4', when); // (as per E/A and TI Writer)
				break;

			case SWT.HOME:
				keyboardState.setKey(realKey, pressed, false, fctnShifted, '5', when);		// BEGIN
				break;
			case SWT.END:
				keyboardState.setKey(realKey, pressed, false, fctnShifted, '0', when);		// Fctn-0
				break;
				

			case SWT.KEYPAD_8:
				keyboardState.setJoystick(joy,
						KeyboardState.JOY_Y,
						0, pressed ? -1 : 0, false, when);
				break;
			case SWT.KEYPAD_2:
				keyboardState.setJoystick(joy,
						KeyboardState.JOY_Y,
						 0, pressed ? 1 : 0, false, when);
				break;
			case SWT.KEYPAD_4:
				keyboardState.setJoystick(joy,
						KeyboardState.JOY_X,
						pressed ? -1 : 0, 0, false, when);
				break;
			case SWT.KEYPAD_6:
				keyboardState.setJoystick(joy,
						KeyboardState.JOY_X,
						pressed ? 1 : 0, 0, false, when);
				break;
				
			case SWT.KEYPAD_7:
				keyboardState.setJoystick(joy,
						KeyboardState.JOY_Y | KeyboardState.JOY_X,
						pressed ? -1 : 0, pressed ? -1 : 0, false, when);
				break;
				
			case SWT.KEYPAD_0:
				keyboardState.setJoystick(joy,
						KeyboardState.JOY_B,
						0, 0, pressed, when);
				break;
				
			case SWT.KEYPAD_9:
				keyboardState.setJoystick(joy,
						KeyboardState.JOY_Y | KeyboardState.JOY_X,
						pressed ? 1 : 0, pressed ? -1 : 0, false, when);
				break;
			case SWT.KEYPAD_3:
				keyboardState.setJoystick(joy,
						KeyboardState.JOY_Y | KeyboardState.JOY_X,
						pressed ? 1 : 0, pressed ? 1 : 0, false, when);
				break;
			case SWT.KEYPAD_1:
				keyboardState.setJoystick(joy,
						KeyboardState.JOY_Y | KeyboardState.JOY_X,
						pressed ? -1 : 0, pressed ? 1 : 0, false, when);
				break;
				

			default:
				if (keyCode != shift) {
					System.out.println(keyCode);
				}
				
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see v9t9.emulator.handlers.KeyboardHandler#scan(v9t9.keyboard.KeyboardState)
	 */
	public void scan(KeyboardState state) {
		if (pasteTimer != null)
			return;

		if (true) return;
		
		synchronized(state) {
			state.resetKeyboard();
		
			synchronized (pressedKeys) {
				boolean first = true;
				for (KeyInfo info : pressedKeys) {
					if (first) {
						updateKey(true, pressedStateMask, 0, info.when);
						first = false;
					}
					updateKey(true, pressedStateMask, info.keyCode, info.when);
				}
				if (first) {
					updateKey(true, pressedStateMask, 0, System.currentTimeMillis());
					first = false;
				}
			}
		}
	}
	
	
	private int lastKeyPressedCode = -1;
	public void init(final Control control) {
		Shell shell = control.getShell();
	 	shell.getDisplay().addFilter(SWT.KeyDown, new Listener() {


			public void handleEvent(Event event) {
				if (!control.isFocusControl())
					return;
				
		        // System.out.println("keyPressed(" + SwtKey.findByCode(event.keyCode) + ")");
		        if (event.keyCode == lastKeyPressedCode) {
		            // ignore if this is a repeat event
		        	//return;
		        }

		        if (lastKeyPressedCode != -1 && event.keyCode != lastKeyPressedCode) {
		            // if this is a different key to the last key that was pressed, then
		            // add an 'up' even for the previous one - SWT doesn't send an 'up' event for the
		            // first key in the below scenario:
		            // 1. key 1 down
		            // 2. key 2 down
		            // 3. key 1 up
		        	//recordKey(false, event.stateMask, lastKeyPressedCode, System.currentTimeMillis());
		        }

		        lastKeyPressedCode = event.keyCode;

				recordKey(true, event.stateMask, event.keyCode, System.currentTimeMillis());
				event.doit = false;
				
			}
			
		});
		shell.getDisplay().addFilter(SWT.KeyUp, new Listener() {

			public void handleEvent(Event event) {
				recordKey(false, event.stateMask, event.keyCode, System.currentTimeMillis());
				event.doit = false;
				lastKeyPressedCode = -1;
			}
			
		});
		
	}



	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		/*
		shell.addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent e) {
				System.out.println("PRESSED " + Utils.toHex4(e.keyCode) + " / " + Utils.toHex4(e.stateMask));
			}

			public void keyReleased(KeyEvent e) {
				System.out.println("RELEASE " + Utils.toHex4(e.keyCode) + " / " + Utils.toHex4(e.stateMask));
			}
			
		});
		*/
		display.addFilter(SWT.KeyUp, new Listener() {

			public void handleEvent(Event e) {
				System.out.println("RELEASE " + HexUtils.toHex4(e.keyCode) + " / " + HexUtils.toHex4(e.stateMask));
				e.doit = false;
			}
			
		});
		display.addFilter(SWT.KeyDown, new Listener() {

			public void handleEvent(Event e) {
				System.out.println("PRESSED " + HexUtils.toHex4(e.keyCode) + " / " + HexUtils.toHex4(e.stateMask));
				e.doit = false;
			}
			
		});
		shell.open();
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose ();

	}

}