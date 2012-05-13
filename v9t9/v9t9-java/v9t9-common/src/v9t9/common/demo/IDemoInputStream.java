/**
 * 
 */
package v9t9.common.demo;

import java.io.Closeable;

import v9t9.common.events.NotifyException;

/**
 * @author ejs
 *
 */
public interface IDemoInputStream extends Closeable {
	/**
	 * Read next event.
	 * @return event, or <code>null</code> once the stream is exhausted
	 * @throws NotifyException 
	 */
	IDemoEvent readNext() throws NotifyException;

	/**
	 * Get the rate at which the timer ticks.
	 * @return Hz
	 */
	int getTimerRate();
}