package unibo.core.thread;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.media.Buffer;

import com.sun.media.ExtBuffer;

import unibo.core.CircularBuffer;
import unibo.core.multiplexer.Multiplex;

import sv60122.dbgUtil.*;

//Created 28/02/2006
//Last modified 28/02/2006

/**
* <p>Thread che incapsula un componente conforme all'interfaccia Multiplex, definita nel package multiplexer.</p>
* <p>Essenzialmente, il thread estrae i frame componenti le tracce dai rispettivi buffer e li passa al modulo che
* si occupa dwl multiplexing. Di fatto, il thread agisce come consumatore in uno schema produttore/consumatore in
* cui la risorsa condivisa e' il buffer specificato come parametro del costruttore.</p>
* @author Alessandro Falchi
*/
//Classe che controlla il thread del multiplexer
public class MultiplexerThreadPS
{
	//variabili gestione thread di parsing stream RTP
	private Object multiplexingSync;
	private Thread runner;
	private boolean close, closed, pauseMultiplexing;
	private iMultiplexerSupendedListener muxSuspended;

	//variabile contente il multiplexer usato
	private Multiplex plugin;

	//variabile per la gestione del CircularBuffer
	protected CircularBuffer[] bufferSet;

	//variabili per la gestione del periodo (e quindi della frequenza) di invio di frame al trasmettitore
	private int sleep_millis, sleep_nanos;
	private long timeToWait,timeAccelerate=0;
	private int numFrameAccelerate;
	private boolean accelerate=false;

	//variabili che identificano proxy e client ??????????
	private String clientAddress;
	private long id;

	//variabili per le statistiche sulla gestione e processo dei frame
	private int frameNumber=1;
	private long timePre,timePost,timeStart,processTime,sleepTime;

	//variabili per la gestione del log su file
	private boolean log=false;
	private PrintWriter p=null;

	//interfaccia del dispositivo di debug
	private iDebugWriter dbg;

//metodo costruttore che inizializza la classe thread col multiplexer, l'array di CirclarBuffer e il parametro id
	public MultiplexerThreadPS(Multiplex multiplexer, CircularBuffer[] buffer, iMultiplexerSupendedListener muxSusp, long id) throws IOException
	{
		this.id=id;
		//non visualizza alcun messaggio di debug
		constructorAssigment(new DebugWriterVDev(0), multiplexer, buffer, muxSusp);
	}

//metodo costruttore analogo al precedente con la differenza che imposta come debug messanger quello passato dal proprietario dell'oggetto
	public MultiplexerThreadPS(iDebugWriter debugger, Multiplex multiplexer, CircularBuffer[] buffer, iMultiplexerSupendedListener muxSusp, long id) throws IOException
	{
		this.id=id;
		constructorAssigment(debugger, multiplexer, buffer, muxSusp);
	}

//metodo costruttore che inizializza la classe thread col multiplexer, l'array di CirclarBuffer e il parametro address del client
	public MultiplexerThreadPS(Multiplex multiplexer, CircularBuffer[] buffer, iMultiplexerSupendedListener muxSusp, String address) throws IOException
	{
		clientAddress=address;
		//non visualizza alcun messaggio di debug
		constructorAssigment(new DebugWriterVDev(0), multiplexer, buffer, muxSusp);
	}

//metodo che si occupa di effettuare gli assegnamenti sempre presenti nelle diverse tipologie di metodi costruttori
	private void constructorAssigment(iDebugWriter debugger, Multiplex multiplexer, CircularBuffer[] buffer, iMultiplexerSupendedListener muxSusp)
	{
		close=false;
		closed=false;
		pauseMultiplexing=false;
		bufferSet=buffer;
		sleep_millis=67;
		sleep_nanos=0;
		plugin=multiplexer;
		dbg=debugger;
		muxSuspended=muxSusp;
		multiplexingSync=new Object();
	}


/**
* Metodo per la modulazione dell'inoltro dei frame da parte del multiplexer. I frame sono inseriti nel flusso
* multiplexed con cadenza corrispondente al frame rate. Di default, il thread assume un frame rate di 15 frame
* al secondo.
* @param frameRate il valore del frame rate del contenuto multimediale
* @throws IllegalArgumentException se il frame rate e' negativo o nullo
*/
	public synchronized void setFrameRate(double frameRate) throws IllegalArgumentException
	{
//TODO: versione multitraccia del metodo
		if (frameRate<=0) throw new IllegalArgumentException("Frame Rate must be > 0");
		double milliseconds=1/frameRate*1000;
		sleep_millis=(int)Math.floor(milliseconds);
		double nanoseconds=(milliseconds-sleep_millis)*1000000;
		sleep_nanos=(int)Math.floor(nanoseconds);
		timeToWait=sleep_millis;
	}

//metodo che permette di vedere se il thread ha terminato il suo lavoro
	public boolean isClosed(){ return closed; }

//avvia un nuovo thread per effettuare il multiplexing con uscita RTP dei dati presenti nel CircularBuffer.
	public void start()
	{
		runner=new Thread(){ public void run(){ multiplexing(); } };
		runner.start();
		dbg.dbgWrite(100,"Avviato MultiplexerThreadPS thread\n");
	}

//metodo che permette di chiedere la sospensione del thread di multiplexin (una volta che ha completato il ciclo di multiplexing eventualmente in atto)
	public void suspend()
	{
		pauseMultiplexing=true;
		System.out.println("MultiplexerThreadPS suspend method: richiesta sospensione del thread del multiplexer");
	}

//metodo che sblocca il thread del multiplexer precedentemente sospeso da suspend, se non era stato sospeso non fa nulla
	public synchronized void restart()
	{
		if(!pauseMultiplexing) return;
		pauseMultiplexing=false;
		synchronized(multiplexingSync){ multiplexingSync.notifyAll(); }
		System.out.println("MultiplexerThreadPS restart method: riavvio del thread del multiplexer");
	}

// WARN: il metodo run considera uno stream monotraccia
// TODO: modifica per multitraccia
	public void multiplexing()
	{
		int muxResult;
		ExtBuffer frame=null;
		CircularBuffer buffer=bufferSet[0]; 
/*
		ServerControllerRewind controller=new ServerControllerRewind(wait,"1202",buffer,this,clientAddress);
		controller.start();
*/
		//FileWriter w=null;
		//p=null;
//		if(id%10==0) log=true;
//		if(log)
//		{
//			try
//			{
//				//w = new FileWriter("C:/Documents and Settings/Roberto/Desktop/Test/"+id+"BufferServerLog.csv", true);
//				w = new FileWriter("Test/"+id+"BufferServerLog.csv", true);
//				p = new PrintWriter(w);
//			}
//			catch(IOException e){ }
//		}

		timeStart=System.currentTimeMillis();
		do
		{
			if(pauseMultiplexing)
			{
				if(muxSuspended!=null) muxSuspended.multiplexerSupended();
				//System.out.println("MultiplexerThreadPS multiplexing method: sospendo il thread del multiplexer");
				synchronized(multiplexingSync)
				{
					try{ multiplexingSync.wait(); }
					catch(InterruptedException e){ System.err.println(e); }
				}
				//System.out.println("MultiplexerThreadPS multiplexing method: riavvio il thread del multiplexer");
			}

			if(log && frame!=null)
			{
				p.println((System.currentTimeMillis()-timeStart)+";"+frame.getTimeStamp()+";"+buffer.getBufferSize()+";"+buffer.getRewindWindows()+";"+buffer.getStatusFrame()+";"+(timeToWait-100));
				p.flush();
			}

			timePre=System.currentTimeMillis();
			//System.out.println("MultiplexerThreadPS multiplexing method: estraggo da CircularBuffer il frame "+frameNumber);
/**************************************************************************/
			//System.err.println("estraggo da CircularBuffer il frame "+frameNumber);
/*************************************************************************/

			frame=buffer.getFrame();

			//System.out.println("MultiplexerThreadPS multiplexing method: estratto frame "+frameNumber+" da CircularBuffer\nduration "+frame.getDuration()+"\nlength "+frame.getLength()+"\nseqnumb "+frame.getSequenceNumber() +"\nEOM "+frame.isEOM()+"\ntimestamp "+frame.getTimeStamp());
/***************************************************************************/
			long ts=frame.getTimeStamp();
			System.err.println("MULTIPLEXER PRESO FRAME "+frameNumber/*);+" duration "+frame.getDuration()+" length "+frame.getLength()+"\nseqnumb "+frame.getSequenceNumber()+" EOM "+frame.isEOM()+"\ntimestamp "+frame.getTimeStamp()*/);
/***************************************************************************/

			//ExtBuffer appoggio=new ExtBuffer();

			//if(test==50)for(int i=0;i<6;i++)frame=buffer.getFrame();
//non ho capito perche' effettuo il multiplexing sulla copia di frame
			//synchronized(frame){ appoggio.copy(frame); }
			//muxResult=plugin.multiplexFrame(appoggio,0);
			muxResult=plugin.multiplexFrame(frame,0);

/***************************************************************************/

			//frame.setFlags((frame.getFlags() & ~Buffer.FLAG_RELATIVE_TIME) | Buffer.FLAG_RTP_TIME);


			//System.err.println("frame POST multiplexing "/*+frameNumber);*/+" duration "+frame.getDuration()/*+" length "+frame.getLength()*/+" seqnumb "+frame.getSequenceNumber()+/*" EOM "+frame.isEOM()+*/" timestamp "+frame.getTimeStamp());
			//System.err.println("PMUXRTP_MARKER "+((frame.getFlags() & Buffer.FLAG_RTP_MARKER)!=0));
			//System.err.println("PMUXNO_DROP "+((frame.getFlags() & Buffer.FLAG_NO_DROP)!=0));
			//System.err.println("PMUXNO_SYNC "+((frame.getFlags() & Buffer.FLAG_NO_SYNC)!=0));
			//System.err.println("PMUXNO_WAIT "+((frame.getFlags() & Buffer.FLAG_NO_WAIT)!=0));
			//System.err.println("PMUXRELATIVE_TIME "+((frame.getFlags() & Buffer.FLAG_RELATIVE_TIME)!=0));
			//System.err.println("PMUXRTP_TIME "+((frame.getFlags() & Buffer.FLAG_RTP_TIME)!=0));
			//System.err.println("PMUXSYSTEM_TIME "+((frame.getFlags() & Buffer.FLAG_SYSTEM_TIME)!=0));
			//System.err.println("PMUXSILENCE "+((frame.getFlags() & Buffer.FLAG_SILENCE)!=0));
			//System.err.println("PMUXDISCARD "+frame.isDiscard());
			//System.err.println("PMUXEOM "+frame.isEOM());
			//System.err.println("PMUXFLUSH "+((frame.getFlags() & Buffer.FLAG_FLUSH)!=0));
			//System.err.println("PMUXBUF_OVERFLOW "+((frame.getFlags() & Buffer.FLAG_BUF_OVERFLOWN)!=0));
			//System.err.println("PMUXBUF_UNDERFLOF "+((frame.getFlags() & Buffer.FLAG_BUF_UNDERFLOWN)!=0));
//			if((frame.getTimeStamp()-ts)==1000000) System.err.println("AGGIUNTA 1ms TRUE");
//			else System.err.println("AGGIUNTA FALSE");
//			if(frame.getTimeStamp()==ts) System.err.println("STESSO TS TRUE");
//			else System.err.println("STESSO TS FALSE");
//			System.err.println();
/***************************************************************************/

			frameNumber++;

			if (muxResult==Multiplex.PLUGIN_TERMINATED)
			{
				System.err.println("Error: plugin terminated");
				return;
			}
			//if((!(muxResult==Multiplex.MULTIPLEX_OK))||appoggio.isDiscard())System.out.println("Errore nel processing del frame");
			if(!(muxResult==Multiplex.MULTIPLEX_OK))System.out.println("Errore nel processing del frame");
			processTime=System.currentTimeMillis()-timePre;
			sleepTime=timeToWait-processTime;


/************************************************************************************/
//nei stream video ci sono frame molto grossi e il protocollo RTP li spezza in vari pacchetti mettendo il flag RTP_MARKER ad ogni frame effettivo in modo che sia possibile sapere quando inizia un nuovo frame del video, per questo viene fatto il controllo sul flag per determinare se sospendersi: nei strem video se ho il contenuto di un pacchetto non marcato dal flag significa che fa sempre parte del frame di cui ho gia' ricevuto altri pacchetti e quindi non devo attendere a spedirlo per rispettare il frame rate altrimenti andrei in buffer underrun. Ma con gli stream MP3 poiche' i frame sono comunque piccoli, ed hanno un modo loro per distribuirsi tra i pacchetti il flag RTP_MARKER non e' mai settato, devo quindi disabilitar eil controllo altrimenti non avrei la possibilita' di impostare un frame rate. Caso mai puo' essere il caso di mettere un ulteriore flag che indichi se lo stream trattato e' video o audio e in base ad esso far eil controllo opportuno.
			//if (((frame.getFlags() & Buffer.FLAG_RTP_MARKER)!=0)&& sleepTime>0){  // Nota - vedi sotto
			if (sleepTime>0)
			{
				try
				{
//forse e' il caso di togliere il controllo sullo stato di accelerazione, a differenza dello scenario di semplice handoff io spedisco sempre al frame rate piu' alto possibile per effettuare power saving quindi anche una richiesta di spedizione di frame persi avverrebbe alla massima velocita' senza dover richiedere che alcuni frame siano spediti piu' velocemente
					if(!accelerate){ Thread.sleep(sleepTime); }
					else
					{
						Thread.sleep(timeAccelerate);
						numFrameAccelerate--;
						if(numFrameAccelerate==0)accelerate=false;
					}
				}
				catch(InterruptedException e){ System.err.println("INTERRUPTED!"); }
			}
			
			dbg.dbgWrite(100,"MultiplexerThreadPS multiplexing method: multiplexerThread sleepTime effettivo "+sleepTime+" sleepTime richiesto "+timeToWait+" tempo di processo "+processTime);
/*************************************************************************/
			//System.err.println("multiplexerThread sleepTime effettivo "+sleepTime+" sleepTime richiesto "+timeToWait+" tempo di processo "+processTime);
/****************************************************************************/

		} while(!frame.isEOM()&&!close);
		plugin.close();
		closed=true;
		System.out.println("***Multiplexer chiuso***");
	}

//vedi considerazione di sopra sull'utilita' dell'accelerazione
	public synchronized void accelerate(int numFrame)
	{
		if(log)
		{
			long time=System.currentTimeMillis()-timeStart;
			p.println(time+";;;;;Accelerate"+numFrame+";;"+30);
			p.println(time+";;;;;Accelerate;;"+0);
			p.println(time+";;;;;Accelerate;;"+30);
		}
		if(numFrame<=0) return;
		numFrameAccelerate=numFrameAccelerate+numFrame;
		timeAccelerate=timeToWait*2/3;
		accelerate=true;
	}

	public synchronized void writeLog(String s)
	{
		if(!log)return;
		long time=System.currentTimeMillis()-timeStart;
		p.println(time+";;;;;"+s+";;"+50);
		p.println(time+";;;;;;;"+0);
		p.println(time+";;;;;;;"+50);
	}

//metodo che termina il thread rendendo falsa la condizione di while del metodo running (multiplexing)
	public void close()
	{
		close=true;
		if(p!=null)p.close();
	}

//metodo che mette il periodo minimo tra un'estrazione del frame dal buffer e l'altra salvo richieste di spedizioni accelerate
	public void setTimeToWait(int timeToWait){ this.timeToWait=timeToWait; }
}
/* Nota: il thread sceglie di inoltrare i frame con cadenza pari al frame rate del contenuto video trattato.
 * Nel caso il frame considerato sia un subframe (FLAG_RTP_MARKER==0) i frame vengono spediti senza alcuna pausa,
 * cosa che permette di riprodurre il filmato in maniera fluida sul client ma senza sovraccaricare la CPU.
*/