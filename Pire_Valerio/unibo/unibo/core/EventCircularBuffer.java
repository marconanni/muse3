package unibo.core;

/*
 * LA CLASSE È STATA MODIFICATA PER IL PROGETTO MUSE 
 */


import javax.swing.event.EventListenerList;

import client.gui.IClientView;
//import relay.gui.ProxyFrameController;

import com.sun.media.ExtBuffer;

public class EventCircularBuffer extends CircularBuffer{
	
	private EventListenerList listeners= new EventListenerList();
	private IClientView view;
	
	//Variabile booleana settata per discriminare il caso in cui ci si trova in modalità elezione in corso oppure in modalità normale.
	private boolean normalMode;
	//Varaibile che tiene viene inizializzato al valore di soglia prescelto in Parameters
	private int sogliaInferiore;
	private int sogliaSuperiore;
	private boolean recovery = false;
	/**
	 * @return the sogliaSuperiore
	 */
	public synchronized int getSogliaSuperiore() {
		return sogliaSuperiore;
	}

	/**
	 * @param sogliaSuperiore the sogliaSuperiore to set
	 */
	public synchronized void setSogliaSuperiore(int sogliaSuperiore) {
	//	this.sogliaSuperiore = this.getSogliaSuperiore();
		this.sogliaSuperiore = sogliaSuperiore;
	}

	/**
	 * @return the normalMode
	 */
	public boolean isNormalMode() {
		return normalMode;
	}

	/**
	 * @param normalMode the normalMode to set
	 */
	public void setNormalMode(boolean normalMode) {
		this.normalMode = normalMode;
	}

	
	/**
	 * @return the sogliaInferiore
	 */
	public synchronized int getSogliaInferiore() {
		return sogliaInferiore;
	}

	/**
	 * @param sogliaInferiore the sogliaInferiore to set tale setter deve essere richiamato all'atto dell'arrivo di un election request
	 * a quel punto il valore di soglia deve essere incrementato per tener conto del fatto che la dimensione del buffer è raddoppiata
	 */
	public synchronized void setSogliaInferiore(int sogliaInferiore) {
		//this.sogliaInferiore = this.getSogliaInferiore();
		this.sogliaInferiore = sogliaInferiore;
	}

	public EventCircularBuffer(int numFrames, IClientView view){
		super(numFrames);
		this.view = view;
	}
	
	public EventCircularBuffer(boolean recovery,int numFrames, IClientView view){
		super(numFrames);
		this.view = view;
		this.recovery = recovery;
	}
	
	public EventCircularBuffer(int numFrames, IClientView view, int sogliaInferiore, int sogliaSuperiore){
		super(numFrames);
		this.sogliaInferiore = sogliaInferiore;
		this.sogliaSuperiore = sogliaSuperiore;
		if(view!=null)
		this.view = view;
		this.normalMode = true;
	}
	
	
	public EventCircularBuffer(int x, int y, int z){
		super(x,y,z);
	}
	
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
	
	private void fireBufferFullEvent(BufferFullEvent ev)
	{
		
	
		Object[] list= listeners.getListenerList();
		for(int i=0;i<list.length;i+=2)
		{
			if(list[i]==BufferFullListener.class)
			{
				System.out.println("Osservatore "+i+" : "+(BufferEmptyListener)list[i+1]);
				((BufferFullListener)list[i+1]).bufferFullEventOccurred(ev);
				
			}
		}
	}

	/**
	 * Il metedo è stato aggiunto al framework UNIBO per poter gestire l'evento di "soglia inferiore" inferiore del buffer sul client:
	 * quando si scende sotto questa soglia stabilita all'intenro della classe Parameters, viene sollevato un evento di buffer empty.
	 * ATTENZIONE: Ovviamente sono state create anche le interfacce di BufferEmptyListener e le classi di BufferEmptyEvent entrambe si trovano 
	 * nel package unibo.core (Modifiche effettuate da Leo Di Carlo)
	 * @param ev
	 */
	private void fireBufferEmptyEvent(BufferEmptyEvent ev)
	{
		Object[] list= listeners.getListenerList();
		for(int i=0;i<list.length;i+=2)
		{
			if(list[i]==BufferEmptyListener.class)
			{
				((BufferEmptyListener)list[i+1]).bufferEmptyEventOccurred(ev);
				System.out.println("Ossrvatore "+i+" : "+(BufferEmptyListener)list[i+1]);
			}
		}
	}
	@Override
	public synchronized void setFrame(ExtBuffer frame) 
	{
		super.setFrame(frame);
		if(super.isFull() || (super.getContatore() == this.sogliaSuperiore && this.normalMode)){
			System.err.println("#### evento buffer full!" + this.getContatore()+"= "+ this.sogliaSuperiore);
			fireBufferFullEvent(new BufferFullEvent(this));
		}
		if(this.view!=null)
		setBufferPercentage();
	}
	
	@Override
	public synchronized ExtBuffer getFrame() {		
		ExtBuffer frame = super.getFrame();
		setBufferPercentage();
		if(super.getContatore() == this.sogliaInferiore || super.isEmpty()){
			System.err.println("#### evento buffer empty!" + this.getContatore()+"= "+ this.sogliaInferiore);	
		
			fireBufferEmptyEvent(new BufferEmptyEvent(this));
		}
			return frame;
	}
	
	private void setBufferPercentage()
	{
		float f = (float)super.getBufferSize() / super.getStatusFrame();
		if(!this.recovery && this.view!=null)
		{
			view.setBufferValue((int)(100/f));
		}
		else
		{
			if(this.view!=null)
			view.setBufferRecValue((int)(100/f));
		}
	}
	public float getBufferPercentage(){
		float f = (float)super.getBufferSize() / super.getStatusFrame();
		return (100/f);
	}
}
