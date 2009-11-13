package unibo.core;

import javax.swing.event.EventListenerList;

import muse.client.gui.IClientView;

import com.sun.media.ExtBuffer;

public class EventCircularBuffer extends CircularBuffer{
	
	private EventListenerList listeners= new EventListenerList();
	private IClientView view;
	
	public EventCircularBuffer(int numFrames, IClientView view){
		super(numFrames);
		this.view = view;
	}
	
	public EventCircularBuffer(int x, int y, int z){
		super(x,y,z);
	}
	
	public void addBufferFullEventListener(BufferFullListener listener)
	{
		if(listener!=null)
			listeners.add(BufferFullListener.class, listener);
	}

	public void removeBufferFullEventListener(BufferFullListener listener)
	{
		if(listener!=null)
			listeners.remove(BufferFullListener.class, listener);
	}
	
	private void fireBufferFullEvent(BufferFullEvent ev)
	{
		Object[] list= listeners.getListenerList();
		for(int i=0;i<list.length;i+=2)
		{
			if(list[i]==BufferFullListener.class)
			{
				((BufferFullListener)list[i+1]).bufferFullEventOccurred(ev);
			}
		}
	}

	@Override
	public synchronized void setFrame(ExtBuffer frame) 
	{
		super.setFrame(frame);
		if(super.isFull())
			fireBufferFullEvent(new BufferFullEvent(this));
		
		setBufferPercentage();
	}
	
	@Override
	public synchronized ExtBuffer getFrame() {		
		ExtBuffer frame = super.getFrame();
		setBufferPercentage();
		return frame;
	}
	
	private void setBufferPercentage()
	{
		float f = (float)super.getBufferSize() / super.getStatusFrame();
		view.setBufferValue((int)(100/f));
	}
}
