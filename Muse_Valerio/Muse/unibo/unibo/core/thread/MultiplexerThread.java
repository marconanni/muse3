package unibo.core.thread;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import javax.media.Buffer;
import unibo.core.CircularBuffer;
import unibo.core.multiplexer.Multiplex;
import com.sun.media.ExtBuffer;

/**
 * <p>Thread che incapsula un componente conforme all'interfaccia Multiplex, definita nel package multiplexer.</p>
 * <p>Essenzialmente, il thread estrae i frame componenti le tracce dai rispettivi buffer e li passa al modulo che 
 * si occupa dwl multiplexing. Di fatto, il thread agisce come consumatore in uno schema produttore/consumatore in
 * cui la risorsa condivisa ?? il buffer specificato come parametro del costruttore.</p>
 * <p>IMPORTANTE: questo thread ?? stato realizzato per testare le funzionalit?? dei componenti di multiplexing in
 * relazione ai meccanismi di buffering dei frames. Dunque, a differenza delle altre classi del package core, non ??
 * stato sviluppato nell'ottica di realizzare un componente di uso generale. Nonostante questo, si ?? deciso di
 * inserirlo ugualmente nel package core perch?? riutilizzabile in acluni ambiti o perch?? rappresenta un 
 * esempio di possibile realizzazione threaded di un multiplexer.</p>
 * <p>In ogni caso, pur avendo esibito un funzionamento corretto nei test effettuati, ?? bene analizzare il codice per
 * verificare se il comportamento del thread corrisponde alle proprie esigenze.</p>
 * @author Alessandro Falchi
 */

public class MultiplexerThread extends Thread {
    private Multiplex plugin;
    private CircularBuffer[] bufferSet;
    private int sleep_millis, sleep_nanos;
    private long timeToWait,timeAccelerate=0;    
    private int numFrameAccelerate;
    private boolean accelerate=false;
    private String clientAddress;
    private boolean close=false;
    private PrintWriter p=null;
    private long id;
    private long timePre,timePost,timeStart,processTime,sleepTime;
    private boolean log=false;

    public MultiplexerThread(Multiplex multiplexer, CircularBuffer[] buffer, long id) throws IOException {
        plugin=multiplexer;
        bufferSet=buffer;
        sleep_millis=67;
        sleep_nanos=0;
        this.id=id;
    }
    
    public MultiplexerThread(Multiplex multiplexer, CircularBuffer[] buffer, String address) throws IOException {
        plugin=multiplexer;
        bufferSet=buffer;
        sleep_millis=67;
        sleep_nanos=0;
        clientAddress=address;
        
    }
    
    
    /**
     * Metodo per la modulazione dell'inoltro dei frame da parte del multiplexer. I frame sono inseriti nel flusso
     * multiplexed con cadenza corrispondente al frame rate. Di default, il thread assume un frame rate ?? di 15 frame
     * al secondo. 
     * @param frameRate il valore del frame rate del contenuto multimediale
     * @throws IllegalArgumentException se il frame rate ?? negativo o nullo
     */
    public synchronized void setFrameRate(double frameRate) throws IllegalArgumentException {
        //TODO: versione multitraccia del metodo
    	
    	if (frameRate<=0) throw new IllegalArgumentException("Frame Rate must be > 0");
        double milliseconds=1/frameRate*1000;
        sleep_millis=(int)Math.floor(milliseconds);
        double nanoseconds=(milliseconds-sleep_millis)*1000000;
        sleep_nanos=(int)Math.floor(nanoseconds);
        timeToWait=sleep_millis;
    }

    
    // WARN: il metodo run considera uno stream monotraccia
    // TODO: modifica per multitraccia
    public void run() {
        int muxResult;
        ExtBuffer frame=null;
        CircularBuffer buffer=bufferSet[0]; 

        /*
        ServerControllerRewind controller=new ServerControllerRewind(wait,"1202",buffer,this,clientAddress);
        controller.start();
        */
        FileWriter w=null;
        p=null;
        if(id%10==0)log=true;
        if(log){
	        try {
	        	 //w = new FileWriter("C:/Documents and Settings/Roberto/Desktop/Test/"+id+"BufferServerLog.csv", true);
	        	 w = new FileWriter("Test/"+id+"BufferServerLog.csv", true);
	        	 p = new PrintWriter(w);		
			} catch (IOException e) {
			}
        }
	/***************************************************************************/	
		int test=1;
	/****************************************************************************/	
        timeStart=System.currentTimeMillis();
        do {
        	if(log && frame!=null){
	            p.println((System.currentTimeMillis()-timeStart)+";"+frame.getTimeStamp()+";"+buffer.getBufferSize()+";"+buffer.getRewindWindows()+";"+buffer.getStatusFrame()+";"+(timeToWait-100));
	            p.flush();
            }
        	timePre=System.currentTimeMillis();
        	/**************************************************************************/
        	System.err.println("prendo frame "+test);
        	/*************************************************************************/
            frame=buffer.getFrame();
          /***************************************************************************/
          System.err.println("PRESO FRAME "+test/*);+" duration "+frame.getDuration()+"  length "+frame.getLength()*/+" seqnumb "+frame.getSequenceNumber() +" EOM "+frame.isEOM()/*+" timestamp "+frame.getTimeStamp()*/);

          /***************************************************************************/
            ExtBuffer appoggio=new ExtBuffer();
            //if(test==50)for(int i=0;i<6;i++)frame=buffer.getFrame();
            //test++;
            synchronized(frame){appoggio.copy(frame);}



            /***************************************************************************/
          //System.err.println("PASSO DI QUI [1] "+test);
          /***************************************************************************/


            muxResult=plugin.multiplexFrame(appoggio,0);
            
            /***************************************************************************/
          //System.err.println("PASSO DI QUI [2] "+test);
          //System.err.println("MULTIPLEXER FRAME "+test+" seqnumb "+frame.getSequenceNumber());
          test++;
          /***************************************************************************/
            
            if (muxResult==Multiplex.PLUGIN_TERMINATED) {
                System.err.println("Error: plugin terminated");
                return;
            }

            if((!(muxResult==Multiplex.MULTIPLEX_OK))||appoggio.isDiscard())System.out.println("Errore nel processing del frame");         
            processTime=System.currentTimeMillis()-timePre;
            sleepTime=timeToWait-processTime;
            
          /***************************************************************************/
          //System.err.println("PASSO DI QUI [4] "+test);
          /***************************************************************************/
            
            
/************************************************************************************/
/** devo controllare (frame.getFlags() & Buffer.FLAG_RTP_MARKER)!=0) in quanto potrebbe essere sempre falso e quindi il multiplexer potrebbe non sospendersi mai a prescindere dal valore di timeToWait che inserisco in quanto io ho modificato il multiplexer da RAW_RTP a RAW*/
            //if (((frame.getFlags() & Buffer.FLAG_RTP_MARKER)!=0)&& sleepTime>0){  // Nota - vedi sotto
            if (sleepTime>0){
                try {                         
                    if(!accelerate){
                    /****************************************************************/
                    	//long pre=System.currentTimeMillis();
                    /*****************************************************************/
                    	Thread.sleep(sleepTime);
                    /*****************************************************************/
                    	//long post=System.currentTimeMillis();
                    	//System.err.println("["+test+"] debug multiplexerThread sleep for "+(post-pre));	
				/*******************************************************************/
                    }else{
                    	Thread.sleep(timeAccelerate);
                    	numFrameAccelerate--;
                    	if(numFrameAccelerate==0)accelerate=false;
                    }            
                }
                catch (InterruptedException e) { System.err.println("INTERRUPTED!"); }
            }
            /*************************************************************************/
            //else System.err.println("debug multiplexerThread sleep time "+sleepTime);
            System.err.println("debug multiplexerThread ultima istruzione ciclo while "+sleepTime);
            /****************************************************************************/
        } while (!frame.isEOM()&&!close);
        plugin.close();
        System.err.println("Multiplexer Thread terminato, multiplexer chiuso.");

    }
    
    public synchronized void accelerate(int numFrame){
    	if(log){
	    	long time=System.currentTimeMillis()-timeStart;
	    	p.println(time+";;;;;Accelerate"+numFrame+";;"+30);
	    	p.println(time+";;;;;Accelerate;;"+0);
	    	p.println(time+";;;;;Accelerate;;"+30);
    	}
    	if(numFrame<=0)	return;	
    	numFrameAccelerate=numFrameAccelerate+numFrame;
    	timeAccelerate=timeToWait*2/3;
    	accelerate=true;
    }
    
    public synchronized void writeLog(String s){
    	if(!log)return;
    	long time=System.currentTimeMillis()-timeStart;
    	p.println(time+";;;;;"+s+";;"+50);
    	p.println(time+";;;;;;;"+0);
    	p.println(time+";;;;;;;"+50);
    }
    
    public void close(){
    	close=true;
    	if(p!=null)p.close();
    }
    
    public void setTimeToWait(int timeToWait){
    	this.timeToWait=timeToWait;
    }
}

/* Nota: il thread sceglie di inoltrare i frame con cadenza pari al frame rate del contenuto video trattato.
 * Nel caso il frame considerato sia un subframe (FLAG_RTP_MARKER==0) i frame vengono spediti senza alcuna pausa,
 * cosa che permette di riprodurre il filmato in maniera fluida sul client ma senza sovraccaricare la CPU.
 */
