/**
 * 
 */
package dummies;

import java.util.Vector;

import javax.swing.event.EventListenerList;

import client.gui.IClientView;

import unibo.core.BufferEmptyEvent;
import unibo.core.BufferEmptyListener;
import unibo.core.BufferFullEvent;
import unibo.core.BufferFullListener;

/**
 * @author Marco Nanni
 *
 */


public class DummyBuffer  {
	
	public Vector<Byte> vector;
	public int capacity;
	private int sogliaSuperioreElection; // soglia oltre alla quale , in modalit� recovery viene lanciato 
	//l'evento di bufferFull
	private int sogliaSuperioreNormal;		 // soglia oltre alla quale , in modalit� normale viene lanciato 
		//l'evento di bufferFull
	
	
	private int sogliaInferioreElection;  //// soglia oltre alla quale , in modalit� elezione viene lanciato 
			//l'evento di bufferEmpty
	private int sogliaInferioreNormal;  //// soglia oltre alla quale , in modalit� notmale viene lanciato 
			//l'evento di bufferEmpty
	
	
	
	
	private boolean normalMode;
	
	
	
	
	public EventListenerList listeners= new EventListenerList();


	/**
	 * Costruisce un  DummyBuffer passandogli tutti i parametri
	 * @param capacity la dimensione massima del buffer
	 * @param sogliaSuperioreElection la soglia  oltre alla quale viene lanciato un evento di BufferFull durante una rielezione
	 * @param sogliaSuperioreNormal la soglia  oltre alla quale viene lanciato un evento di BufferFull durante il funzionamento normale
	 * @param sogliaInferioreElection la soglia sotto alla quale viene lanciato un evento di BufferEmpty durante una rielezione
	 * @param sogliaInferioreNormal  la soglia sotto alla quale viene lanciato un evento di BufferEmpty durante  il funzionamento normale
	 */

	public DummyBuffer(int capacity, int sogliaSuperioreElection,
			int sogliaSuperioreNormal, int sogliaInferioreElection,
			int sogliaInferioreNormal) {
		super();
		this.capacity = capacity;
		this.normalMode= true;
		this.sogliaSuperioreElection = sogliaSuperioreElection;
		this.sogliaSuperioreNormal = sogliaSuperioreNormal;
		this.sogliaInferioreElection = sogliaInferioreElection;
		this.sogliaInferioreNormal = sogliaInferioreNormal;
		this.vector= new Vector<Byte>();
		
	}
	
	/**
	 * 
	 * @return il numero di elementi contenuti nel buffer
	 */
	
	public int getSize(){
		return vector.size();
	}
	
	/**
	 * 
	 * @return true se il buffer è completamente pieno
	 * il numero di componenti è pari alla capacità
	 */
	public boolean isFull(){
		return this.getSize()>=this.capacity;
	}
	
	/**
	 * 
	 * @return true se il buffer è completamente vuoto
	 * il numero di componenti è pari a zero
	 */
	public boolean isEmpty(){
		return this.getSize()==0;
	}
	
	/**
	 * 
	 * @return la soglia oltre alla quale viene lanciato lìevento di BufferFull
	 */
	
	public int getSogliaSuperiore(){
		if (normalMode=true)
			return this.sogliaSuperioreNormal;
		else
			return this.sogliaSuperioreElection;
	}
	

	/**
	 * 
	 * @return la soglia sotto alla quale viene lanciato lìevento di BufferEmpty
	 */
	
	public int getSogliaInferiore(){
		if (normalMode=true)
			return this.sogliaInferioreNormal;
		else
			return this.sogliaInferioreElection;
	}
	
	/**
	 * Inserisce un elemento in fondo al buffer,
	 * se la dimensione del buffer raggiunge o supera la soglia superiore
	 * viene lanciato l'evento di BufferFull
	 * @param bt il Byte da inserire
	 */
	public void put (Byte bt){
		vector.add(bt);
		if (this.getSize()>= this.getSogliaSuperiore())
			this.fireBufferFullEvent(new BufferFullEvent(this));
	}
	
	
	/**
	 * Metodo preleva il primo elelmento del buffer, se il numero di elementi
	 * rimasti è pari o inferire alla soglia inferiore lancia l'evento bufferEmpty. 
	 * @return il primo elemento del buffer, o null, se questo è completamente vuoto
	 */
	public Byte get() {
		
		if (this.isEmpty()){
			System.err.println("Buffer Completamente vuoto, resituisco null");
			return null;
		}
		else{
			Byte bt = vector.firstElement();
			vector.remove(bt);
			if(this.getSize()<=this.getSogliaInferiore())
				this.fireBufferEmptyEvent(new BufferEmptyEvent(this));
			return bt;
		}
	}
	
	
	
	
	
	
	/////////////METODI COPIATI PER LA PARTE DEGLI EVENTI
	
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
	
	
	
		//////////////////////GETTERS SETTERS||||||||||||||||||||||||||

	public Vector<Byte> getVector() {
		return vector;
	}

	public void setVector(Vector<Byte> vector) {
		this.vector = vector;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public int getSogliaSuperioreElection() {
		return sogliaSuperioreElection;
	}

	public void setSogliaSuperioreElection(int sogliaSuperioreElection) {
		this.sogliaSuperioreElection = sogliaSuperioreElection;
	}

	public int getSogliaSuperioreNormal() {
		return sogliaSuperioreNormal;
	}

	public void setSogliaSuperioreNormal(int sogliaSuperioreNormal) {
		this.sogliaSuperioreNormal = sogliaSuperioreNormal;
	}

	public int getSogliaInferioreElection() {
		return sogliaInferioreElection;
	}

	public void setSogliaInferioreElection(int sogliaInferioreElection) {
		this.sogliaInferioreElection = sogliaInferioreElection;
	}

	public int getSogliaInferioreNormal() {
		return sogliaInferioreNormal;
	}

	public void setSogliaInferioreNormal(int sogliaInferioreNormal) {
		this.sogliaInferioreNormal = sogliaInferioreNormal;
	}

	public boolean isNormalMode() {
		return normalMode;
	}

	public void setNormalMode(boolean normalMode) {
		this.normalMode = normalMode;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
