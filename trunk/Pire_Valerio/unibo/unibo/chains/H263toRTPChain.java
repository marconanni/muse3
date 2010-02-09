package unibo.chains;

import java.io.IOException;

import javax.media.Format;
import javax.media.Time;
import javax.media.protocol.DataSource;

import unibo.core.parser.QuickTimeFileParser;
import unibo.core.transcoder.H263toYUVDecoder;
import unibo.core.transcoder.Transcode;
import unibo.core.transcoder.YUVtoH263Encoder;

import com.sun.media.ExtBuffer;

/**
 * Questa classe realizza un componente per la lettura dei frame da un file video monotraccia codificato in formato
 * H263. I frame acquisiti sono automaticamente adattati per la trasmissione RTP (ovvero H263/RTP).
 * @author Alessandro Falchi
 */
public class H263toRTPChain {
    private QuickTimeFileParser parser;
    private H263toYUVDecoder decoder;
    private YUVtoH263Encoder encoder;
    private ExtBuffer[] frameStore; // contiene di 3 frame: il primo è quello ottenuto dal parser, il secondo dal
    								// decoder e il terzo (output del metodo fetFrame()) dall'encoder
    /**
     * Costruttore.
     * @param source il DataSource relativo al file contenente il video
     * @throws IOException se non è possibile inizializzare uno degli stadi della catena (il messaggio associato
     * all'eccezione permette di avere maggiori informazioni a riguardo)
     */
    public H263toRTPChain(DataSource source) throws IOException {
        parser=new QuickTimeFileParser(source);
        decoder=new H263toYUVDecoder(parser.getTrackFormat(0));
        encoder=new YUVtoH263Encoder(decoder.getOutputFormat());
        frameStore=new ExtBuffer[3];
        for (int i=0; i<3; i++) frameStore[i]=null;
        
    }
    
    
    /**
     * Metodo funzionale all'inizializzazione dello stadio connesso a valle, che deve conoscere il formato dei frame
     * prodotti da questa catena.
     * @return un'istanza di Format che specifica il formato dei frame letti dal metodo getFrame
     */
    public Format getOutputFormat() {
        return encoder.getOutputFormat();
    }
    
    /**
     * Permette l'accesso sequenziale ai frame del video monotraccia a cui si riferisce il DataSource specificato
     * come parametro del cstruttore.  
     * @return il frame successivo, in formato H263/RTP
     */
    public ExtBuffer getFrame() throws IOException {
        do {
            demultiplex();
            if (decode()!=-1) encode();
        }
        while (frameStore[2]==null);
        return frameStore[2];
    }
    
    
    private void demultiplex() {
        if (frameStore[0]==null) frameStore[0]=parser.getFrame(0); //WARN: conoscenza statica, caso monotraccia
    }

    /* ritorna 0 se il frame in frameQueue[1] è valido, altrimenti ritorna -1 */
    private int decode() throws IOException {
        if (frameStore[1]==null) {
            frameStore[1]=new ExtBuffer();
            int decResult=decoder.processFrame(frameStore[0],frameStore[1]);
            switch(decResult) {
            	case Transcode.PLUGIN_TERMINATED:
            	    throw new IOException("Decoding plugin terminated");
            	case Transcode.OUTPUT_NOT_VALID:
            	case Transcode.PROCESS_FAILED:
            	    frameStore[0]=null; // frame prodotto dal parser (H263) invalidato
            		frameStore[1]=null; // frame prodotto dal decoder (YUV) invalidato
            		return -1;
            	case Transcode.TRANSCODE_OK:
            		frameStore[0]=null;	// frame prodotto dal parser (H263) consumato
            	case Transcode.PROCESS_INPUT_AGAIN:
            	    // non fa niente, poichè l'input dovrà essere riutilizzato in seguito 
            		return 0;
            }
        }
        return 0;
    }


    /* ritorna 0 se il frame in frameQueue[2] è valido, altrimenti ritorna -1 */
    private int encode() throws IOException {
        frameStore[2]=new ExtBuffer();
        int encResult=encoder.processFrame(frameStore[1],frameStore[2]);
        
        switch(encResult) {
        	case Transcode.PLUGIN_TERMINATED:
        	    throw new IOException("Encoding plugin terminated");
        	case Transcode.OUTPUT_NOT_VALID:
        	case Transcode.PROCESS_FAILED:
        	    frameStore[1]=null; // frame prodotto dal decoder (YUV) invalidato
            	frameStore[2]=null; // frame prodotto dall'encoder (H263/RTP) invalidato
    			return -1;
        	case Transcode.TRANSCODE_OK:
        	    frameStore[1]=null; // frame prodotto dal decoder (YUV) consumato
        	case Transcode.PROCESS_INPUT_AGAIN:
        	    // non fa niente, poichè l'input dovrà essere riutilizzato in seguito 
    			return 0;
        }
        return 0;
    }
    
    public void setMediaTime(Time time){
    	parser.setMediaTime(time);
    }
    
    public void resumeFromTime(Time time){
    	parser.resumeFromTime(time);
    }
}