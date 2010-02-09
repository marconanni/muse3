package unibo.core.thread;

import unibo.core.CircularBuffer;
import unibo.core.parser.Parser;

import com.sun.media.ExtBuffer;

/**
 * <p>Thread che incapsula un'istanza di una sottoclasse della classe Parser, implementata nel package parser.</p>
 * <p>Essenzialmente, il thread si occupa di bufferizzare i frame facenti parte del contenuto multimediale a cui è
 * collegato il plugin che esegue il parsing. Il buffering avviene associando una istanza di CircularBuffer ad
 * ogni traccia di cui è composto il contenuto multimediale.</p>
 * <p>IMPORTANTE: questo thread è stato realizzato per testare le funzionalità dei componenti di parsing in
 * relazione ai meccanismi di buffering dei frames. Dunque, a differenza delle altre classi del package core, non è
 * stato sviluppato nell'ottica di realizzare un componente di uso generale. Nonostante questo, si è deciso di
 * inserirlo ugualmente nel package core perchè riutilizzabile in acluni ambiti o perchè rappresenta un 
 * esempio di possibile realizzazione threaded di un parser.</p>
 * <p>In ogni caso, pur avendo esibito un funzionamento corretto nei test effettuati, è bene analizzare il codice per
 * verificare se il comportamento del thread corrisponde alle proprie esigenze.</p>
 * @author Alessandro Falchi
 */
public class ParserThread extends Thread {
	private Parser plugin;
	private CircularBuffer[] bufferSet;
	private CircularBuffer buffer;
	private ExtBuffer frame;
	private boolean close=false;

/**
* Inizializza il thread. 
* @param parser il componente che si occupa di estrarre i frame
* @param buffersize la dimensione, in numero di frame contenibili, delle singole istanze di CiruclarBuffer
* associate alle diverse tracce di cui è composto il contenuto multimediale.
*/
	public ParserThread(Parser parser, int buffersize)
	{
		plugin=parser;
		int numberOfTracks=plugin.getNumberOfTracks();
		bufferSet=new CircularBuffer[numberOfTracks];
		for (int i=0; i<numberOfTracks; i++) bufferSet[i]=new CircularBuffer(buffersize);
    }

	public ParserThread(Parser parser, int buffersize, int window, int timeToWait)
	{
		plugin=parser;
		int numberOfTracks=plugin.getNumberOfTracks();    
		bufferSet=new CircularBuffer[numberOfTracks];
		for (int i=0; i<numberOfTracks; i++) bufferSet[i]=new CircularBuffer(buffersize,window,timeToWait);
    }


/**
*
* @return un array i cui elementi sono le istanze di CircularBuffer corri
*/
	public CircularBuffer[] getOutputBufferSet(){ return bufferSet; }


// WARN: il metodo run considera uno stream monotraccia
// TODO: modifica per multitraccia
	public void run()
	{

		System.out.println("Parser: "+this.getName());
		buffer=bufferSet[0]; //WARN: conoscenza statica - il file mov considerato ha una traccia sola

/*****************************************************************************/
		int test=1;
		long timePreviousFrameArrive=0, timeFrameArrive=0;
		int mediumTimeArrive=0;
/****************************************************************************/

		try
		{
			do
			{


/**************************************************************************/
				//System.err.println("parser metto frame "+test);
				timePreviousFrameArrive=timeFrameArrive;
/*************************************************************************/


				frame=plugin.getFrame(0); //WARN: conoscenza statica - il file mov considerato ha una traccia sola  


/****************************************************************************/
				timeFrameArrive=System.currentTimeMillis();
				mediumTimeArrive+=timeFrameArrive-timePreviousFrameArrive;
				//System.err.println("PARSE TIME ARRIVE "+test+" FRAME:  "+(timeFrameArrive-timePreviousFrameArrive));
				if((test%10)==0)
				{
					//System.err.println("\n\nPARSE MEDIUM TIME ARRIVE FRAME:  "+(mediumTimeArrive/10)+"\n\n");
					mediumTimeArrive=0;
				}
/*****************************************************************************/

				//System.err.println("parser ricevuto frame timestamp "+frame.getTimeStamp()+"PARSER buffer status "+buffer.getStatus());

				if(frame!=null&&buffer!=null)buffer.setFrame(frame);

/***************************************************************************/
				//System.err.println("PARSER MESSO FRAME "+test+/*" duration "+frame.getDuration()+"  length "+frame.getLength()+*/" seqnumb "+frame.getSequenceNumber()/*+" EOM "+frame.isEOM()+ " timestamp "+frame.getTimeStamp()*/);
				test++;
				//System.err.println("PARSER buffer status "+buffer.getStatus());
/***************************************************************************/


			} while (!close &&!frame.isEOM());
			if(plugin!=null)plugin.close();
		}
		catch(Exception e)
		{
			System.out.println("ParserThread: "+e);
			e.printStackTrace();
		}
		System.out.println("Parser chiuso");
	}

	public synchronized void close()
	{
		close=true;
		if(plugin!=null)
		{
			plugin.setEnabled(false,0);
			plugin.close();
		}
		frame=null;
		plugin=null;
		buffer=null;
	}
}