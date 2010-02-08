/**
 * 
 */
package dummies;

import java.util.Vector;

import javax.swing.event.EventListenerList;

import unibo.core.BufferEmptyListener;
import unibo.core.BufferFullListener;

/**
 * @author Marco Nanni
 *
 */
public class DummyBuffer  {
	
	public Vector<Byte> vector;
	public int size;
	
	public int sogliaInferiore;
	public int sogliaSuperiore;
	
	public EventListenerList listeners= new EventListenerList();

	public DummyBuffer( int size,int sogliaInferiore, int sogliaSuperiore) {
		super();
		this.vector = new Vector<Byte> ();
		this.size = size;
		this.sogliaInferiore = sogliaInferiore;
		this.sogliaSuperiore = sogliaSuperiore;
	}
	
	

	/**
	 * Inserisce un byte nel buffer; lancia l'evento di bufferFull
	 * se la dimensione del buffer supera la soglia superiore
	 * @param bite il byte da inserire
	 */
	public void put(Byte bite){
		vector.add(bite);
		if (vector.size()>sogliaSuperiore)
			this.throwBufferFullEvent();
		
	}
	
	public Byte get(){
		Byte bt = vector.firstElement();
		vector.remove(bt);
		if (vector.size()<this.sogliaInferiore)
			this.throwBufferEmptyEvent();
		return bt;
		
	}
	

		
	

	public void throwBufferFullEvent() {
		Object[] list= listeners.getListenerList();
		for(int i=0;i<list.length;i+=2)
		{
			if(list[i]==BufferFullListener.class)
			{
				
				((BufferFullListener)list[i+1]).bufferFullEventOccurred(null);
				
			}
		}
		
	}
	
	private void throwBufferEmptyEvent() {
		Object[] list= listeners.getListenerList();
		for(int i=0;i<list.length;i+=2)
		{
			if(list[i]==BufferEmptyListener.class)
			{
				((BufferEmptyListener)list[i+1]).bufferEmptyEventOccurred(null);
				System.out.println("Ossrvatore "+i+" : "+(BufferEmptyListener)list[i+1]);
			}
		}
	

	}
	
	
	
	
	
	//////////////////////////////////////////////////////////////////////
	//....................CODICE COPIATO.e Getters Setters....///////////
	//////////////////////////////////////////////////////////////////
	
	public void addBufferFullEventListener(BufferFullListener listener)
	{
		if(listener!=null)
			listeners.add(BufferFullListener.class, listener);
	}
	
	public void addBufferEmptyEventListener(BufferEmptyListener listener)
	{
		if(listener!=null)
			listeners.add(BufferEmptyListener.class, listener);
	}

	public void removeBufferFullEventListener(BufferFullListener listener)
	{
		if(listener!=null)
			listeners.remove(BufferFullListener.class, listener);
	}
	
	public void removeBufferEmptyEventListener(BufferEmptyListener listener)
	{
		if(listener!=null)
			listeners.remove(BufferEmptyListener.class, listener);
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getSogliaInferiore() {
		return sogliaInferiore;
	}

	public void setSogliaInferiore(int sogliaInferiore) {
		this.sogliaInferiore = sogliaInferiore;
	}

	public int getSogliaSuperiore() {
		return sogliaSuperiore;
	}

	public void setSogliaSuperiore(int sogliaSuperiore) {
		this.sogliaSuperiore = sogliaSuperiore;
	}
	
	
	
	public Vector<Byte> getVector() {
		return vector;
	}

	public void setVector(Vector<Byte> vector) {
		this.vector = vector;
	}



	public void setNormalMode(boolean normalMode) {
		;
		
	}
	
	
	
	
	
	
}
