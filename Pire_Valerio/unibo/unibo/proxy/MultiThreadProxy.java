package unibo.proxy;

import java.io.IOException;
import java.net.InetAddress;
import javax.media.IncompatibleSourceException;
import javax.media.protocol.DataSource;
import unibo.core.multiplexer.RTPMultiplexer;
import unibo.core.parser.Parser;
import unibo.core.parser.RTPParser;
import unibo.core.rtp.RTPReceiver;
import unibo.core.rtp.RTPSender;
import unibo.core.thread.MultiplexerThread;
import unibo.core.thread.ParserThread;
import unibo.core.*;

/**
 * Inizializza e avvia il proxy per l'inoltro dei dati RTP.
 * Il proxy agisce da forwarder dei dati RTP, inoltrando i pacchetti ricevuti da un mittente verso un ricevente
 * (entrambi gli endpoint sono specificati come parametri del main).
 * Questa classe si basa sulle classi definite nel package unibo e nei suoi sottopackage.
 * @author Alessandro Falchi
 */
public class MultiThreadProxy extends Thread{
	private int serverPort, clientPort, inPort, outPort;
	private String serverIP, clientIP;
	private RTPReceiver rtpRx;
	private DataSource dsInput;
	private ParserThread parserThread;
	private RTPSender transmitter;
	private MultiplexerThread muxThread;
	private RTPMultiplexer mux;
	private Parser rtpParser;
	private CircularBuffer buffer;
	private int bufferSize, window, timeToWait;
	private boolean movieOn=false;
	private long id;
	
	
	public MultiThreadProxy(int serverPort, int clientPort, int inPort, int outPort, String serverIP, String clientIP,int bufferSize,int window,int timeToWait,long id){
		this.serverPort=serverPort;
		this.clientPort=clientPort;
		this.inPort=inPort;
		this.outPort=outPort;
		this.serverIP=serverIP;
		this.clientIP=clientIP;
		this.bufferSize=bufferSize;
		this.window=window;
		this.timeToWait=timeToWait;
		this.id=id;
		System.out.println("MultiThreadProxy creato con:\nserverPort: "+serverPort+"\nclientPort: "+clientPort);
		System.out.println("inPort: "+inPort+"\noutPort: "+outPort);
		System.out.println("serverIP: "+serverIP+"\nclientIP: "+clientIP);
	}

    /* inPort------>il numero di porta su cui il proxy ascolta l'arrivo dei dati RTP
     * serverIP---->l'indirizzo IP del mittente
     * serverPort-->la porta da cui trasmette il mittente
     * outPort----->la porta usata dal proxy per inoltrare i dati ricevuti
     * clientIP---->l'indirizzo IP del destinatario del forwarding
     * clientPort-->la porta di ascolto del destinatario
     */
    public void run() {
        try {
        	
    		rtpRx=new RTPReceiver(inPort);
            rtpRx.setSender(InetAddress.getByName(serverIP),serverPort);
            dsInput=rtpRx.receiveData();
            

            // ***** PARSER *****
            rtpParser=new RTPParser(dsInput);
            parserThread=new ParserThread(rtpParser,bufferSize,window,timeToWait);
            CircularBuffer[] b=parserThread.getOutputBufferSet();
            buffer=b[0];
           
            
            // ***** MULTIPLEXER *****
            mux=new RTPMultiplexer(rtpParser.getTracksFormat());
            muxThread=new MultiplexerThread(mux,b,id);
            muxThread.setTimeToWait(timeToWait);

            transmitter=new RTPSender(outPort);
            transmitter.addDestination(InetAddress.getByName(clientIP),clientPort);
            transmitter.sendData(mux.getDataOutput());  
            
            
            parserThread.start();
            System.out.println("Proxy: parser started");
            
            while(!buffer.isFull()){
            	try{
            		sleep(10);
            	}catch(InterruptedException e){
            		System.out.println(e);
            	}
            }
            
            muxThread.start();
            movieOn=true;
            System.out.println("Proxy started");
        }
        catch (IOException e) { System.err.println(e); }
        catch (IncompatibleSourceException e) { System.err.println(e); }
    }
    
    public void close(){   
    	if(muxThread!=null)muxThread.close();
    	mux.close(); 	
    	if(parserThread!=null){
    		parserThread.close();
    	} 
    	if(dsInput!=null)dsInput.disconnect();
    	if(rtpRx!=null)rtpRx.close();   	 	    	
    	if(transmitter!=null)transmitter.close();   	
    }
    
    public CircularBuffer getBuffer(){	
    	return buffer;
    }
    
    public int rewind(long timestamp){
    	return buffer.rewind(timestamp);
    }
    
    public synchronized void setRewindWindow(int window){
    	buffer.setRewindWindow(window);
    }
    
    
    public void accelerate(int i){
    	muxThread.accelerate(i);
    }
    
    /**
     * Return true if the trasmission form the server is started
     * @return
     */
    public boolean isPlaying(){
    	return movieOn;
    }
    
    public void writeLog(String s){
    	muxThread.writeLog(s);
    }
    
    public void setTimeToWait(int timeToWait){
    	muxThread.setTimeToWait(timeToWait);
    }
}
