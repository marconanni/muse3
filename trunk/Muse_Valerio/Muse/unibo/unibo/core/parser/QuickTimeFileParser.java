package unibo.core.parser;

import java.io.IOException;

import javax.media.BadHeaderException;
import javax.media.Buffer;
import javax.media.IncompatibleSourceException;
import javax.media.Time;
import javax.media.Track;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;

import com.sun.media.ExtBuffer;
import com.sun.media.parser.video.QuicktimeParser;


/**
 * <p>Sottoclasse della classe Parser per il demultiplexing di un file in formato Quicktime.</p>
 * Questo componente è basato sul plugin com.sun.media.parser.video.QuicktimeParser<p>
 * Si noti che, dai test effettuati, il modulo risulta compatibile con i file QuickTime che presentano codifica H263.
 * @author Alessandro Falchi
 */
public class QuickTimeFileParser extends Parser {
    private QuicktimeParser qtp;
    private long sequenceNum[], timeStamp[], timeInterval[];
    
    
    /**
     * Costruttore.
     * @param dataSource l'istanza di DataSource associata al file QuickTime di cui si vuole eseguire il parsing
     * @throws IOException nel caso vi sia un problema con la lettura del file
     */
    public QuickTimeFileParser(DataSource dataSource) throws IOException {
    	
        try {
            dataSource.connect();
            
            qtp=new QuicktimeParser();
            qtp.setSource(dataSource);
            //qtp.open(); //l'implementazione di BasicPullStream è vuota
            //qtp.start(); //si invoca start() sul DataSource, che nel caso di protocollo 'file' non fa niente
            qtp.stop(); // solo perchè l'ingresso non è in formato RTP

            trackList=qtp.getTracks(); // solleva l'eccezione BadHeaderException
            numberOfTracks=trackList.length;
            
            sequenceNum=new long[numberOfTracks]; //memorizza il numero di sequenza dei vari frame
            timeStamp=new long[numberOfTracks]; //memorizza il timestamp dell'ultimo frame estratto per ciascuna traccia 
            timeInterval=new long[numberOfTracks]; //Nota1:
            for (int i=0; i<numberOfTracks; i++) { 
                sequenceNum[i]=0;
                timeStamp[i]=0;
            }

            // TODO: modificare la seguente porzione di codice per:
            // 		1)eseguire la ricerca automatica della traccia video;
            //		2)calcolare la durata dei frame delle altre tracce.
            VideoFormat videoTrack=(VideoFormat)trackList[0].getFormat();
            double frameTimeInterval=1/videoTrack.getFrameRate()*1000000; // durata del frame in nanosecondi
            timeInterval[0]=(long)Math.floor(frameTimeInterval);
            
            
        }
        catch (BadHeaderException e) { throw new IOException("bad header - tracks unreadable: "+e); }
        catch (IncompatibleSourceException e) { throw new IOException("Cannot set DataSource: "+e); }
    }
    
    
    public void close() {
        qtp.close();
    }


    public ExtBuffer getFrame(int trackNumber) {
        if (!trackList[trackNumber].isEnabled()) return null;
        
        Track videoTrack=(Track)trackList[trackNumber];
        
        
        ExtBuffer frame=new ExtBuffer();
        videoTrack.readFrame(frame);
        frame.setSequenceNumber(sequenceNum[trackNumber]);
        //frame.setTimeStamp(timeStamp[trackNumber]);
        

        // specifica il timestamp è calcolato a partire dall'istante zero e non rispetto ad un orologio assoluto
        frame.setFlags(frame.getFlags() | Buffer.FLAG_RELATIVE_TIME); 

        timeStamp[trackNumber]=timeStamp[trackNumber]+timeInterval[trackNumber];
        sequenceNum[trackNumber]=sequenceNum[trackNumber]+1;
        //System.out.println("Qui Parser, timestamp------> "+timeStamp[trackNumber]);
        //System.out.println("Qui Parser, sequenceN------> "+sequenceNum[trackNumber]);
        return frame;
    }
    public void setMediaTime(Time time){
    	qtp.setPosition(time,1);
    }
    
    public void resumeFromTime(Time time){
    	int frame=trackList[0].mapTimeToFrame(time);
    	long nano=(trackList[0].mapFrameToTime(frame+1)).getNanoseconds();
    	this.setMediaTime(new Time(nano));
    }
    
}

/* Nota1: il timeInterval è la durata di ciascun frame e corrisponde all'intervallo di campionamento dei frame. 
 * Attraverso il timeinterval è possibile calcolare il timestamp dei frame: il timestamp è infatti l'istante di
 * campionamento di ciascun frame (per maggiori dettagli: RFC 2190 - RTP Payload Format for H.263 Video Streams)
 * Dunque, ogni frame ha timestamp pari al timestamp del frame precedente (0 per il primo) più l'intervallo di
 * campionamento.
 *  
 */
