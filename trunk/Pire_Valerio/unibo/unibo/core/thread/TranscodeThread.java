package unibo.core.thread;

import unibo.util.Waiter;
import unibo.core.CircularBuffer;
import unibo.core.transcoder.Transcode;

import com.sun.media.ExtBuffer;

/**
 * <p>Thread che incapsula un componente conforme all'interfaccia Transcode, definita nel package transcoder.</p>
 * <p>Essenzialmente, estrae i frame da un CircularBuffer di ingresso e, una volta elaborati, li ripone in un
 * CircularBuffer di uscita, nascondendo all'utilizzatore la gestione dei codici di ritorno della chiamata al metodo
 * processFrame di Transcode.</p>
 * <p>IMPORTANTE: questo thread è stato realizzato per testare le funzionalità dei componenti di transcoding in
 * relazione ai meccanismi di buffering dei frames. Dunque, a differenza delle altre classi del package core, non è
 * stato sviluppato nell'ottica di realizzare un componente di uso generale. Nonostante questo, si è deciso di
 * inserirlo ugualmente nel package core perchè riutilizzabile in acluni ambiti o perchè rappresenta un 
 * esempio di possibile realizzazione threaded di un transcoder.</p>
 * <p>In ogni caso, pur avendo esibito un funzionamento corretto nei test effettuati, è bene analizzare il codice per
 * verificare se il comportamento del thread corrisponde alle proprie esigenze.</p>
 * @author Alessandro Falchi
 */
public class TranscodeThread extends Thread {
    private Transcode plugin;
    private CircularBuffer inputBuffer,outputBuffer;
    private Waiter wait;
    private int timeToWait;
    private int level,actualLevel;
    private boolean manageLevel=false;
    
    
    /**
     * Costruttore.
     * @param plugin l'istanza dell'oggetto responsabile del processing dei frame
     * @param inBuffer il buffer da cui vengono prelevati i frame da elaborare
     * @param outBuffer il buffer in cui vengono memorizzati i frame transcodificati
     */
    public TranscodeThread(Transcode transcoder, CircularBuffer inBuffer,
            CircularBuffer outBuffer) {
        plugin=transcoder;
        inputBuffer=inBuffer;
        outputBuffer=outBuffer;
        wait=new Waiter(0);
        
        
    }

    
    /**
     * Permette l'accesso al buffer circolare in cui vengono riposti i frame elaborati durante lo stato running del
     * thread
     * @return il riferimento al buffer circolare di uscita
     */
    public CircularBuffer getOuputBuffer() {
        return outputBuffer;
    }
 
    
    public void run() {
        boolean again=true, readFromBuffer=true;
        int result;
        ExtBuffer inputFrame=null, outputFrame=null, 
        	storedFrame=null;	// accumulatore per un'istanza di ExtBuffer 

        do {
            if (readFromBuffer) inputFrame=inputBuffer.getFrame();
            if (storedFrame==null) outputFrame=new ExtBuffer();
            else {
                outputFrame=storedFrame;
                storedFrame=null;
            }
            result=plugin.processFrame(inputFrame,outputFrame);
            
            switch (result) {
            	case Transcode.PLUGIN_TERMINATED:
            	    again=false;
            		break;
            	case Transcode.PROCESS_FAILED:	// Nota1
        	    	readFromBuffer=true;
            		storedFrame=null;
        			System.err.println("Warning: "+plugin.getClass().getName()+" process failed");
            		break;
            	case Transcode.OUTPUT_NOT_VALID:
                    storedFrame=outputFrame;
        	    	readFromBuffer=true;
            		break;
            	case Transcode.TRANSCODE_OK:
            		outputBuffer.setFrame(outputFrame);      	
        	    	readFromBuffer=true;
            		storedFrame=null;
                    if (outputFrame.isEOM()) again=false;
                    timeToWait=wait.getBaseWaiter();
                    if(manageLevel){
                    	actualLevel=outputBuffer.getStatusFrame();
                    	if(actualLevel>level)timeToWait=timeToWait+15;
                    	if(actualLevel<level-5)timeToWait=timeToWait-25;
                    }
                    try{Thread.sleep(timeToWait);}catch(Exception e){}
            		break;
            	case Transcode.PROCESS_INPUT_AGAIN:
            	    outputBuffer.setFrame(outputFrame);
            	    
            	
            		readFromBuffer=false;
        			storedFrame=null;
            }
            
        } while(again);
        plugin.close();
    }
    
    public synchronized void setWait(int milliseconds)
    {wait.setBaseWaiter(milliseconds);
    }
    
    public synchronized void setLevelManager(int level){
    	this.level=level;
    	manageLevel=true;
    }
    
    
}


/* Nota1: qui è fatta una scelta specifica: se il processo di processing fallisce, il thread non viene interrotto,
 * ma si procede solo a segnalarlo all'utente e a scartare il frame che ha causato l'errore.
 * Questo perchè per i test effettuati non si è mai verificato il fallimento del processing di un frame, evento
 * probabilmente legato alla corruzione dei dati relativi al frame o all'elaborazione di fotogrammi in un formato
 * non trattato dal plugin. 
 */