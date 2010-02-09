/**
 * 
 */
package unibo.core;

import java.util.EventListener;

/**
 * @author Leo Di Carlo
 *
 */
public interface BufferEmptyListener extends EventListener{

	public void bufferEmptyEventOccurred(BufferEmptyEvent e);
}
