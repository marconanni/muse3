package unibo.core;

import java.util.EventListener;

public interface BufferFullListener extends EventListener{
	
	public void bufferFullEventOccurred(BufferFullEvent e);

}
