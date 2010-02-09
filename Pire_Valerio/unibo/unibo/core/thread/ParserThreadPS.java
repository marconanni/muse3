package unibo.core.thread;

import unibo.core.CircularBuffer;
import unibo.core.parser.Parser;

import com.sun.media.ExtBuffer;
import javax.media.Buffer;

import sv60122.dbgUtil.*;

//Created 27/02/2006
//Last modified 27/02/2006

/**
* <p>Thread che incapsula un'istanza di una sottoclasse della classe Parser, implementata nel package parser.</p>
* <p>Essenzialmente, il thread si occupa di bufferizzare i frame facenti parte del contenuto multimediale a cui e'
* collegato il plugin che esegue il parsing. Il buffering avviene associando una istanza di CircularBuffer ad
* ogni traccia di cui e' composto il contenuto multimediale.</p>
* @author Sergio Valisi
*/
//Classe che controlla il thread del parser dello stream RTP
public class ParserThreadPS
{
	//variabili gestione thread di parsing stream RTP
	private Thread runner;
	private boolean close=false;

	//variabile contente il parser usato
	private Parser plugin;

	//variabili per la gestione del CircularBuffer
	private CircularBuffer[] bufferSet;
	private CircularBuffer buffer;
	private ExtBuffer frame;
	private long sequenceNumber = 0;

	//variabili per le statistiche sull'arrivo dei frame
	private int frameNumber=1;
	private long timePreviousFrameArrive=0, timeFrameArrive=0, timeWaited;
	private int mediumTimeArrive=0;

	//interfaccia del dispositivo di debug
	private iDebugWriter dbg;

/**
* Inizializza il thread.
* @param parser il componente che si occupa di estrarre i frame
* @param buffersize la dimensione, in numero di frame contenibili, delle singole istanze di CiruclarBuffer
* associate alle diverse tracce di cui e' composto il contenuto multimediale.
*/
//metodo costruttore che inizializza la classe thread col parser e la dimensione del CirclarBuffer
	public ParserThreadPS(Parser parser, int buffersize)
	{
		dbg=new DebugWriterVDev(0); //non visualizza alcun messaggio di debug
		plugin=parser;
		int numberOfTracks=plugin.getNumberOfTracks();
		bufferSet=new CircularBuffer[numberOfTracks];
		for (int i=0; i<numberOfTracks; i++) bufferSet[i]=new CircularBuffer(buffersize);
    }

//metodo costruttore analogo al precedente con la differenza che imposta come debug messanger quello passato dal proprietario dell'oggetto
	public ParserThreadPS(iDebugWriter debugger, Parser parser, int buffersize)
	{
		dbg=debugger;
		plugin=parser;
		int numberOfTracks=plugin.getNumberOfTracks();
		bufferSet=new CircularBuffer[numberOfTracks];
		for (int i=0; i<numberOfTracks; i++) bufferSet[i]=new CircularBuffer(buffersize);
		dbg.dbgWrite(100,"ParserThreadPS creato con parametri:\nparser: "+parser.toString()+"\nbuffer capacity: "+buffersize);
    }

//metodo costruttore che inizializza la classe thread col parser, la dimensione del CirclarBuffer e il time to wait
	public ParserThreadPS(Parser parser, int buffersize, int timeToWait)
	{
		dbg=new DebugWriterVDev(0); //non visualizza alcun messaggio di debug
		plugin=parser;
		int numberOfTracks=plugin.getNumberOfTracks();
		bufferSet=new CircularBuffer[numberOfTracks];
		for (int i=0; i<numberOfTracks; i++) bufferSet[i]=new CircularBuffer(buffersize,0,timeToWait);
    }

	
	//costruttore a cui viene passato l'istanza di un circularbuffer (nel nostro caso eventcircularbuffer)
	public ParserThreadPS(Parser parser, int buffersize, CircularBuffer buffer)
	{
		dbg=new DebugWriterVDev(0); //non visualizza alcun messaggio di debug
		plugin=parser;
	//	int numberOfTracks=plugin.getNumberOfTracks();
		bufferSet=new CircularBuffer[1];
	//	for (int i=0; i<numberOfTracks; i++) bufferSet[i]=new CircularBuffer(buffersize);
		bufferSet[0] = buffer;
    }
/**
* @return un array i cui elementi sono le istanze di CircularBuffer corri
*/
	public CircularBuffer[] getOutputBufferSet(){ return bufferSet; }

//avvia un nuovo thread per effettuare il parsing dello stream RTP in arrivo.
	public void start()
	{
		runner=new Thread(){ public void run(){ parsing(); } };
		close=false;
		runner.start();
		dbg.dbgWrite(100,"Avviato ParserThreadPS thread\n");
	}

// WARN: il metodo run considera uno stream monotraccia
// TODO: modifica per multitraccia
//metodo che esegue il parser della prima traccia dello stream RTP
	public void parsing()
	{
		buffer=bufferSet[0]; //WARN: conoscenza statica - il file mov considerato ha una traccia sola
		try
		{
			do
			{
				//dbg.dbgWrite(100,"ParserThreadPS parsing method: estraggo "+frameNumber+" frame\n");
				timePreviousFrameArrive=timeFrameArrive;
				frame=plugin.getFrame(0); //WARN: conoscenza statica - il file mov considerato ha una traccia sola
				if(getSequenceNumber() == 0){
					setSequenceNumber(frame.getSequenceNumber());
				}
				else{
					long previous = getSequenceNumber();
					previous++;
					if(frame.getSequenceNumber() == previous){
						setSequenceNumber(frame.getSequenceNumber());
					}
				}
				timeFrameArrive=System.currentTimeMillis();
				timeWaited=timeFrameArrive-timePreviousFrameArrive;
				mediumTimeArrive+=timeWaited;
				//dbg.dbgWrite(100,"ParserThreadPS parsing method: time waited "+timeWaited+" for "+frameNumber+" frame\n");
/*****************************************************************************/
				//System.err.println("PARSE TIME ARRIVE "+timeWaited+" FRAME: "+frameNumber);
				
				
				
				//System.err.println(/*"PARSER FRAME PRE BUFFER "+frameNumber+*/"\nduration "+frame.getDuration()+/*"\nlength "+frame.getLength()+*/"seqnumb "+frame.getSequenceNumber()+/*"\nEOM "+frame.isEOM()+ */"\ntimestamp "+frame.getTimeStamp());
			//System.err.println("RTP_MARKER "+((frame.getFlags() & Buffer.FLAG_RTP_MARKER)!=0));
			//System.err.println("NO_DROP "+((frame.getFlags() & Buffer.FLAG_NO_DROP)!=0));
			//System.err.println("NO_SYNC "+((frame.getFlags() & Buffer.FLAG_NO_SYNC)!=0));
			//System.err.println("NO_WAIT "+((frame.getFlags() & Buffer.FLAG_NO_WAIT)!=0));
			//System.err.println("RELATIVE_TIME "+((frame.getFlags() & Buffer.FLAG_RELATIVE_TIME)!=0));
			//System.err.println("RTP_TIME "+((frame.getFlags() & Buffer.FLAG_RTP_TIME)!=0));
			//System.err.println("\nSYSTEM_TIME "+((frame.getFlags() & Buffer.FLAG_SYSTEM_TIME)!=0));
			//System.err.println("SILENCE "+((frame.getFlags() & Buffer.FLAG_SILENCE)!=0));
			//System.err.println("DISCARD "+frame.isDiscard());
			//System.err.println("EOM "+frame.isEOM());
			//System.err.println("FLUSH "+((frame.getFlags() & Buffer.FLAG_FLUSH)!=0));
			//System.err.println("BUF_OVERFLOW "+((frame.getFlags() & Buffer.FLAG_BUF_OVERFLOWN)!=0));
			//System.err.println("BUF_UNDERFLOF "+((frame.getFlags() & Buffer.FLAG_BUF_UNDERFLOWN)!=0));
			//System.err.println("\n\n");
/*****************************************************************************/

				if((frameNumber%10)==0)
				{
					mediumTimeArrive/=10;
				//	dbg.dbgWrite(100,"ParserThreadPS parsing method: time medium waited "+mediumTimeArrive+" for frame extracting\n");
/*****************************************************************************/
					//System.err.println("\n\nPARSE MEDIUM TIME ARRIVE FRAME:  "+mediumTimeArrive+"\n\n");
/*****************************************************************************/
					mediumTimeArrive=0;
				}
				if(frame!=null&&buffer!=null)buffer.setFrame(frame);

			//	System.err.println("ParserThreadPS parsing method: inserito "+frameNumber+" in CircularBuffer ");//\tduration "+frame.getDuration()+"\tlength "+frame.getLength()+"\tseqnumb "+frame.getSequenceNumber()+"\tEOM "+frame.isEOM()+ "\ttimestamp "+frame.getTimeStamp());
				frameNumber++;
			} while (!close &&!frame.isEOM()); //IN COSA CONSISTE IL METODO frame.isEOM()??
		}
		catch(Exception e)
		{
			System.err.println("Eccezione ParserThread: "+e);
			e.printStackTrace();
		}
		if(plugin!=null)
		{
			plugin.setEnabled(false,0);
			plugin.close();
		}
		frame=null;
		buffer=null;
		close=true;
		System.out.println("***Parser chiuso***");
	}

//metodo che si occupa di impostare la terminazione del thread e liberare le risorse
	public synchronized void close()
	{
		if(!close)
		{
			close=true;
			if(plugin!=null)
			{
				plugin.setEnabled(false,0);
				plugin.close();
			}
			//frame=null;
			plugin=null;
			buffer=null;
		}
	}

//metodo che restituisce il tempo impiegato ad estrarre l'ultimo frame giunto, in caso tale tempo non sia inferiore a 10 secondi (ad esempio primo frame estratto per cui non si possiede il tempo di estrazione del precedente o altre attese che altererebbero la valutazione) viene restituito -1
	public int getTimeFrameExtracting()
	{
		if(timeWaited<10000) return (int)timeWaited;
		else return -1;
	}
	
//metodo che restituisce il tempo medio (mediato su 10 arrivi) impiegato ad estrarre l'ultimo frame giunto, in caso tale tempo non sia inferiore a 10 secondi (ad esempio primo frame estratto per cui non si possiede il tempo di estrazione del precedente o altre attese che altererebbero la valutazione) viene restituito -1
	public int getMediumTimeFrameExtracting()
	{
		if(mediumTimeArrive<10000) return (int)mediumTimeArrive;
		else return -1;
	}
	
	public synchronized void setSequenceNumber(long sn){
		sequenceNumber = sn;
	}
	
	public synchronized long getSequenceNumber(){
		return sequenceNumber;
	}
}