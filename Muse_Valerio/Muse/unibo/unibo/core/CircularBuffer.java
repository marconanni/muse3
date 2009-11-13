package unibo.core;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import javax.media.Buffer;

import com.sun.media.ExtBuffer;

/**
 * Questa classe implementa un buffer circolare. I singoli elementi del buffer sono istanze di ExtBuffer, ossia
 * oggetti che contengono ciascuno un frame e i relativi parametri.<p>
 * Sono già implementati i meccanismi di sincronizzazione per l'accesso concorrente alla risorsa.
 * @author Alessandro Falchi
 */
public class CircularBuffer
{
	private final byte EMPTY=0, PARTIAL=1, FULL=2, UNDERRUN=3; 
	private byte status;
	private static final long maxTime=4294967295L;
	/* La variabile status indica lo stato di riempimento del buffer; i valori che pu? assumere sono quelli definiti
	 * dalle costanti EMPTY, PARTIAL e FULL. Ho preferito questa soluzione rispetto ad flag boolean da abbinare al
	 * confronto sui puntatori perch? credo ne guadagni la leggibilit? del codice 
	 */
/*****************************************************************************/
	private float min=100;
/*****************************************************************************/

	private int maxIndex,writableBlockIndex,readableBlockIndex,rewindWindow,timeToWait=0,bufferSize;
	private ExtBuffer[] buf;
	private long timestamp,prevRequest=0;
	public int contatore=0,readBuffer=0,targetWindow=-1;
	private boolean rewind=false;

//*****LOG*****
	private PrintWriter p=null;


	/**
	 * Costruttore.
	 * @param numberOfFrames la dimensione del buffer circolare in termini di massimo numero di frames memorizzazibili 
	 */
	public CircularBuffer(int numberOfFrames)
	{
		buf=new ExtBuffer[numberOfFrames];
		initializeBuf(buf);
		bufferSize=numberOfFrames;
		maxIndex=numberOfFrames;
		rewindWindow=0;
		this.reset();
	}

	/**
	 * Build a CircularBuffer with a window for the rewind. "NumberOfFrames"
	 * indicates the number of frames not used for rewind so, the real 
	 * dimention of the Buffer is NumberOfFrames + rewind
	 * @param numberOfFrames : the dimention of the Buffer.
	 * @param rewindWindow : the dimention of the window for rewind.
	 */
	public CircularBuffer(int numberOfFrames, int rewindWindow,int timeToWait)
	{
		if(rewindWindow>=0)this.rewindWindow=rewindWindow;
		else this.rewindWindow=0;
		if(timeToWait<0)timeToWait=0;
		maxIndex=numberOfFrames+rewindWindow;
		bufferSize=numberOfFrames;
		buf=new ExtBuffer[maxIndex];
		initializeBuf(buf);
		this.timeToWait=timeToWait;
		Random rand=new Random();
		int i=rand.nextInt(4000/timeToWait);
		timestamp=i*1000000;	
		this.reset();
		
		FileWriter w=null;
		p=null;

/* //[CODICE MODIFICATO] 24/1/06 ho modificato questa parte di logging

		try
		{
			//w = new FileWriter("C:/Documents and Settings/Roberto/Desktop/Test/ProxyLog.csv", true);
			w = new FileWriter("Test/ProxyLog.csv", true);
			p = new PrintWriter(w);		
		}
		catch (IOException e){ System.out.println(e); }
*/
	}

/**
* Metodo per l'accesso in lettura del buffer. Una volta chiamato il metodo, il frame letto è rimosso dal buffer.
* @return il frame che risiede da più tempo nel buffer o null se il buffer è vuoto
*/
	public synchronized ExtBuffer getFrame()
	{
		ExtBuffer output=null;
		while (status==EMPTY)
		{

/***************************************************************************/
			System.err.println("BUFFER VUOTO");
/***************************************************************************/

			try { wait(); }
			catch (InterruptedException e) {}
		}
		output=buf[readableBlockIndex];	
		readableBlockIndex=(readableBlockIndex+1)%maxIndex;
		contatore--;

/***************************************************************************/
		if (min>(100.0*contatore/maxIndex)) min=100*contatore/maxIndex;
		//System.err.println("BUFFER CAPACITY: "+contatore+" MIN: "+min);
/***************************************************************************/

		status=getState();
		notify();	
		if(targetWindow>0)this.setRewindWindow(targetWindow);
		return output;
	}

/**
* Permette di conoscere la dimensione del buffer.
* @return il massimo numero di frames che il buffer può contenere
*/
	public int getBufferSize(){ return maxIndex; }

/**
* Questo metodo è utile per operazioni di logging, poichè ritorna una stringa con la percentuale di occupazione
* del buffer al momento dell'invocazione del metodo.
* @return una stringa indicante il valore % di occupazione del buffer
*/
	public synchronized String getStatusString()
	{
		if (status==FULL) return "100%";
		else
		{
			float diff;
			if (readableBlockIndex<writableBlockIndex) diff=writableBlockIndex-readableBlockIndex;
			else diff=maxIndex-(readableBlockIndex-writableBlockIndex);
			float v=(diff/maxIndex)*100;
			return new String(v+"%");
		}
	}

	public synchronized int checkMaxStep()
	{
		int maxSteps;
		if (readableBlockIndex<writableBlockIndex) maxSteps=maxIndex-(writableBlockIndex-readableBlockIndex);
		else maxSteps=readableBlockIndex-writableBlockIndex;
		notifyAll();
		return maxSteps;
	}

/**
* Questo metodo è utile per operazioni di monitoring, poichè ritorna un float che specifica la percentuale di
* occupazione del buffer al momento dell'invocazione del metodo.
* @return un float con la percentuale di occupazione del buffer (pieno=1)
*/
	public synchronized float getStatus()
	{
		if (status==EMPTY) return 0;
		if (status==FULL) return 1;
		else
		{
			float diff;
			if (readableBlockIndex<writableBlockIndex) diff=writableBlockIndex-readableBlockIndex;
			else diff=maxIndex-(readableBlockIndex-writableBlockIndex);
			float v=(diff/maxIndex);
			return v;
		}
	}

	public synchronized int getStatusFrame(){ return contatore; }

/**
* Metodo per l'accesso in scrittura del buffer.
* @param frame l'istanza di ExtBuffer che incapsula il frame che si vuole bufferizzare
*/
	public synchronized void setFrame(ExtBuffer frame)
	{
		while (status==FULL)
		{
			System.err.println("BUFFER PIENO");
			try{ wait(); }
			catch(InterruptedException e){}
		}
		buf[writableBlockIndex]=frame;

/***************************************************************************/
			//System.err.println("frame PRE timestamp "+frame.getTimeStamp());
			//System.err.println(" flag \nRTP_MARKER "+((frame.getFlags() & Buffer.FLAG_RTP_MARKER)!=0));
			//System.err.println("\nNO_DROP "+((frame.getFlags() & Buffer.FLAG_NO_DROP)!=0));
			//System.err.println("\nNO_SYNC "+((frame.getFlags() & Buffer.FLAG_NO_SYNC)!=0));
			//System.err.println("\nNO_WAIT "+((frame.getFlags() & Buffer.FLAG_NO_WAIT)!=0));
			//System.err.println("\nRELATIVE_TIME "+((frame.getFlags() & Buffer.FLAG_RELATIVE_TIME)!=0));
			//System.err.println("\nRTP_TIME "+((frame.getFlags() & Buffer.FLAG_RTP_TIME)!=0));
			//System.err.println("\nSYSTEM_TIME "+((frame.getFlags() & Buffer.FLAG_SYSTEM_TIME)!=0));
			//System.err.println("\nSILENCE "+((frame.getFlags() & Buffer.FLAG_SILENCE)!=0));
			//System.err.println("\nDISCARD "+frame.isDiscard());
			//System.err.println("\nEOM "+frame.isEOM());
			//System.err.println("\nFLUSH "+((frame.getFlags() & Buffer.FLAG_FLUSH)!=0));
			//System.err.println("\nBUF_OVERFLOW "+((frame.getFlags() & Buffer.FLAG_BUF_OVERFLOWN)!=0));
			//System.err.println("\nBUF_UNDERFLOF "+((frame.getFlags() & Buffer.FLAG_BUF_UNDERFLOWN)!=0));
			//System.err.println("\n\n");
/*****************************************************************************/

		if(timeToWait!=0)
		{
			buf[writableBlockIndex].setTimeStamp(timestamp);
			if((frame.getFlags() & Buffer.FLAG_RTP_MARKER)!=0)
			{
				timestamp=timestamp+timeToWait*1000000;
				timestamp=timestamp%maxTime;
			}
		}

/***************************************************************************/
		//System.err.println("frame POST timestamp "+buf[writableBlockIndex].getTimeStamp()+"\n");
/*****************************************************************************/

		writableBlockIndex=(writableBlockIndex+1)%maxIndex;
		contatore++;
		status=getState();	
		notify();
		if(targetWindow>0)this.setRewindWindow(targetWindow);
	}

/**
* Svuota il buffer: in ogni elemento viene inserito il valore null. Lo stato del buffer è EMPTY.
*/
	public synchronized void reset()
	{
		status=EMPTY;
		writableBlockIndex=0;
		readableBlockIndex=0;
		contatore=0;
		notify();
	}
	
/**
* @return true se il buffer è vuoto, false altrimenti
*/
	public boolean isEmpty(){ return (status==EMPTY); }

/**
* @return true se il buffer è pieno, false altrimenti
*/
	public boolean isFull(){ return (status==FULL); }

/**
* @return true se si e' verificato un underrun
*/
	public boolean isUnderrun(){ return (status==UNDERRUN); }

/**
* Questo metodo svuota una parte del buffer, scartando un certo numero di frame in sequenza tra quelli
* bufferizzati a partire dal più vecchio (ossia quello da più tempo nel buffer).<p>
* In pratica, il metodo forward sposta "in avanti" il puntatore al frame da leggere, invalidando implicitamente 
* quei frame che vengono superati.
* @param numberOfSteps il numero dei frame da scartare
* @return il numero di frame effettivamente invalidati
*/
	public synchronized int forward(int numberOfSteps)
	{
		if ((numberOfSteps==0) || (status==EMPTY)) return 0;

		int maxSteps;
		if (readableBlockIndex<writableBlockIndex) maxSteps=writableBlockIndex-readableBlockIndex;
		else maxSteps=maxIndex-(readableBlockIndex-writableBlockIndex);
		if (numberOfSteps<maxSteps)
		{
			readableBlockIndex=(readableBlockIndex+numberOfSteps)%maxIndex;
			status=PARTIAL;
			notify();
			return numberOfSteps;
		}
		else
		{
			readableBlockIndex=writableBlockIndex;
			status=EMPTY;
			notify();
			return maxSteps;
		}
	}

/**
* Questo metodo riavvolge una parte del buffer, rendendo nuovamente leggibili 
* un certo numero di frame in sequenza tra quelli bufferizzati a partire dal piu' nuovo vecchio 
* (ossia quello da più tempo nel buffer).<br>
* In pratica, il metodo rewind sposta "indietro" il puntatore al frame da leggere, rendendo implicitamente
* nuovamente validi quei frame che vengono nuovamente resi leggibili.
*
* @param numberOfSteps il numero dei frame da riabilitare
* @return il numero di frame effettivamente riabilitati
*/

/*
	public synchronized int rewind(int frameTarget)
	{
		int actualPointed=firstNum+readBuffer;
		int numberOfSteps=actualPointed-frameTarget;
		if ((numberOfSteps<=0) || (status==EMPTY)) return 0;		
		int maxSteps;

		if (readableBlockIndex<writableBlockIndex) maxSteps=maxIndex-(writableBlockIndex-readableBlockIndex);
		else maxSteps=readableBlockIndex-writableBlockIndex;
		if (numberOfSteps<maxSteps)
		{
			int newReadable = readableBlockIndex-numberOfSteps;
			if( newReadable>=0 ) readableBlockIndex=newReadable;
			else readableBlockIndex = maxIndex+1+newReadable;
			status=PARTIAL;
			notifyAll();
			System.out.println("Rewind");
			toMark=true;
			return firstNum+readBuffer;
		}
		else
		{
/*
* NOTA: non voglio comunque sovrascrivere altri frame gia' presenti,
* mi limito a mettermi nella posizione seguente il writer. Potrei quindi
* avere dei salti nella visualizzazione. Aggiorno lo stato con un UNDERRUN
*/

/*
			readableBlockIndex=(writableBlockIndex+1)%maxIndex;
			status=UNDERRUN;
			notifyAll();
			System.out.println("Rewind");
			toMark=true;
			return firstNum+readBuffer;
		}
	}
*/

	public synchronized int rewind(long timeStamp)
	{
//perche' non posso effettuare due volte lo stesso riavvolgimento?
		if(prevRequest==timeStamp)return 0;
		prevRequest=timeStamp;
		int passi=0;
		int index=readableBlockIndex;
		int indexPre=index-1;
		if(indexPre<0) indexPre=(maxIndex-1);
		//System.out.println("Buffer: Rewind: MaxSteps= "+rewindWindow);
		if(buf[indexPre]==null||index==writableBlockIndex)
//la condizione sopra e' errata in quanto esclude il caso del buffer pieno in cui non posso fare rewind in quanto  i frame dietro a readableBlock non sono quelli vecchi bensi' gli ultimi inseriti, ma esclude pure il caso in cui il buffer si sia "vuotato" in quanto sono stati prevelati i frame ma che essi siano ovviamente ancora presenti. Sarebbe piu' corretto sostituire la seconda parte della condizione con la verifica che il buffer sia pieno
		{
			if(rewindWindow>5)
			{
				p.println(";;;"+1);
				p.flush();
			}
			return 0;
		}
		long actualTimeStamp=buf[index].getTimeStamp();
		long prevTimestamp=buf[indexPre].getTimeStamp();
		while(!(actualTimeStamp==timeStamp && prevTimestamp!=timeStamp))
		{
			passi++;
			index--;
			indexPre--;
			if(index<0)index=(maxIndex-1);
			if(indexPre<0)indexPre=(maxIndex-1);
			if(buf[indexPre]==null || index==writableBlockIndex)
			{
				readableBlockIndex=index;
				contatore=contatore+passi;
				status=getState();
				notify();
				if(rewindWindow>5)
				{
					p.println(";;;"+1);
					p.flush();
				}
				return passi;

			}
			actualTimeStamp=buf[index].getTimeStamp();
			prevTimestamp=buf[indexPre].getTimeStamp();
		}
		readableBlockIndex=index;
		contatore=contatore+passi;
		status=getState();
		notify();
		return passi;	
	}
	
/**
* Questo metodo permette l'ispezione del contenuto del buffer senza gli effetti collaterali di getFrame(), ossia
* la lettura non comporta alcuna rimozione dei frame letti dal buffer.<p>
* Il metodo non rappresenta una deroga al data hiding di questo ADT poichè l'istanza ritornata è una copia di
* quella memorizzata nel buffer, evitando così errori causati da riferimenti multipli.
* @param position l'indice che specifica la posizione del frame all'interno del buffer
* @return il frame memorizzato all'indice specificato. Si noti che potrebbe essere un dato non valido o null
*/
	public synchronized ExtBuffer readFrame(int position)
	{
		System.out.println("Position: "+position+" maxIndex: "+maxIndex);
		if ((position<0) | (position>maxIndex)) return null;
		ExtBuffer output=new ExtBuffer();
		output.copy(buf[position]);	// per evitare riferimenti multipli
		return output;
	}

	public synchronized ExtBuffer readFrame()
	{
		ExtBuffer output=new ExtBuffer();
		output.copy(buf[readableBlockIndex]);	// per evitare riferimenti multipli
		notify();
		return output;
	}

/**
* Changes the dimention of the buffer and the rewind window.
* @param size must be >=0
* @param window must be < size
* @throws IllegalArgumentException
*/

	public synchronized void setRewindWindow(int window)throws IllegalArgumentException
	{
		if(window<0)throw new IllegalArgumentException("window must be >=0");
		//System.out.println("Circular Buffer: setWindow-->"+window);
		if(window==rewindWindow)
		{
			targetWindow=-1;
			return;
		}
		int size=maxIndex+(window-rewindWindow);
		ExtBuffer[] temp=new ExtBuffer[size];
		initializeBuf(temp);
		//System.out.println("CirculaBufferPre: Write="+writableBlockIndex+" read="+readableBlockIndex+" contatore="+contatore+" size="+maxIndex);
		if(size>maxIndex)
		{
			int index=readableBlockIndex;
			int i=0;
			while(index!=writableBlockIndex ||(i==0&&status==FULL))
			{
				ExtBuffer b=buf[index];
				temp[i]=b;
				i++;index++;
				index=index%maxIndex;
			}
			int write=i;
			if(readableBlockIndex!=writableBlockIndex)
			{
				index=readableBlockIndex-1;
				if(index<0)index=maxIndex-1;
				i=1;
				while(index!=writableBlockIndex)
				{
					temp[size-i]=buf[index];
					i++;index--;
					if(index<0)index=maxIndex-1;
				}
				temp[size-i]=buf[index];
			}
			readableBlockIndex=0;
			writableBlockIndex=write;		
			buf=temp;
			maxIndex=size;
			rewindWindow=window;
			targetWindow=-1;
			//System.out.println("CirculaBufferPost: Write="+writableBlockIndex+" read="+readableBlockIndex+" contatore="+contatore+" size="+maxIndex);
		}
		else
		{
			if(this.getStatusFrame()>bufferSize)
			{
				targetWindow=window;
				return;
			}
			int index=readableBlockIndex;
			int i=0;
			while(index!=writableBlockIndex ||(i==0&&status==FULL))
			{
				ExtBuffer b=buf[index];
				temp[i]=b;
				i++;index++;
				index=index%maxIndex;
			}
			writableBlockIndex=i%size;
			index=readableBlockIndex-1;
			if(index<0)index=maxIndex-1;
			for(int j=1;j<=window;j++)
			{
				temp[size-j]=buf[index];
				index--;
				if(index<0)index=maxIndex-1;
			}
			buf=temp;
			readableBlockIndex=0;
			maxIndex=size;
			rewindWindow=window;
			targetWindow=-1;
			//System.out.println("CirculaBufferPost: Write="+writableBlockIndex+" read="+readableBlockIndex+" contatore="+contatore+" size="+maxIndex);
		}
	}

/**
* Return the dimention of the RewindWondow
* @return
*/
	public int getRewindWindows(){ return rewindWindow; }

/**
* Tells if the buffer is actually in a state of transition due to 
* a Rewind operation;
* @return
*/
	public synchronized boolean isRewind(){ return rewind; }

	private void initializeBuf(ExtBuffer[] buffer){ for(int i=0;i<buffer.length;i++)buffer[i]=null; }

	private byte getState()
	{
		if(contatore<=0)
		{
			rewind=false;
			return EMPTY;
		}
		if(contatore+rewindWindow==maxIndex)
		{
			rewind=false;
			return FULL;	
		}
		if(contatore+rewindWindow>maxIndex)
		{
			rewind=true;
			return FULL;	
		}
		rewind=false;
		return PARTIAL;
	}

	public void close()
	{
		if(p!=null)p.close();
		buf=null;
	}
}