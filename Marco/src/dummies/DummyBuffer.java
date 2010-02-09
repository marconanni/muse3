/**
 * 
 */
package dummies;

import java.util.Vector;

import javax.swing.event.EventListenerList;

import client.gui.IClientView;

import unibo.core.BufferEmptyListener;
import unibo.core.BufferFullListener;

/**
 * @author Marco Nanni
 *
 */
public class DummyBuffer extends relay.ExtensibleEventCircularBuffer  {
	
	public Vector<Byte> vector;
	public int capacity;
	
	
	
	
	public EventListenerList listeners= new EventListenerList();

	
	
	/**
	 * Costruttore usato per fare il buffer nomale
	 * @param numFrames
	 * @param view
	 * @param sogliaInferioreNormal
	 * @param sogliaInferioreElection
	 * @param sogliaSuperioreNormal
	 * @param sogliaSuperioreElection
	 */

	public DummyBuffer(int numFrames, IClientView view,
			int sogliaInferioreNormal, int sogliaInferioreElection,
			int sogliaSuperioreNormal, int sogliaSuperioreElection) {
		super(numFrames, view, sogliaInferioreNormal, sogliaInferioreElection,
				sogliaSuperioreNormal, sogliaSuperioreElection);
		vector =new Vector<Byte> ();
		capacity = numFrames;
		
		
		
	}
	
	/**
	 * E' il costruttore del recoveryByffer
	 * crea un buffer con soglia inferiore 0
	 * e soglia superiore pari alla dimensione del 
	 * buffer
	 * 
	 * @param recovery
	 * @param numFrames l'unico utilizzato è la dimensione del buffer
	 * @param view
	 */
	
	public DummyBuffer(boolean recovery,int numFrames, IClientView view){
		// questa riga è quadi inutile, ma devo metterla sennò non complia. ad ogni modo il
		//buffer ha entrambe le soglie minime a zero e entrambe le soglie massime pari alla dimensione
		// del buffe; ne segue che si svuota e si riempie del tutto
		super(numFrames,view,0,0,numFrames,numFrames);
		vector =new Vector<Byte> ();
		capacity= numFrames;
		
	}
	

	/**
	 * Inserisce un byte nel buffer; lancia l'evento di bufferFull
	 * se la dimensione del buffer supera la soglia superiore
	 * @param bite il byte da inserire
	 */
	public void put(Byte bite){
		vector.add(bite);
		if (vector.size()>=super.getSogliaSuperiore())
			this.throwBufferFullEvent();
		
	}
	
	public Byte get(){
		Byte bt = vector.firstElement();
		vector.remove(bt);
		if (vector.size()<=this.getSogliaInferiore())
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
		return capacity;
	}

	public void setSize(int size) {
		this.capacity = size;
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
