package unibo.core.parser;

import java.io.IOException;

import javax.media.IncompatibleSourceException;
import javax.media.Track;
import javax.media.protocol.DataSource;

import com.sun.media.ExtBuffer;
import com.sun.media.parser.RawBufferParser;

/**
 * <p>Sottoclasse della classe Parser per il demultiplexing di uno stream RTP.</p>
 * Sono trattati tutti gli stream di tipo RTP, indipendentemente dalla codifica del contenuto multimediale ricevuto
 * (formato RAW), come da specifiche del plugin com.sun.media.parser.RawBufferParser su cui è fondato questo modulo.
 * @author Alessandro Falchi
 */
public class RTPParser extends Parser {
    private RawBufferParser dataParser;

    /**
     * Costruttore.
     * @param dataSource l'istanza di DataSource associata al flusso RTP di cui si vuole eseguire il parsing
     * @throws IOException nel caso vi sia un problema con l'inizializzazione del plugin interno
     * @throws IncompatibleSourceException se il DataSource in ingresso non considera un formato dati compatibile
     */
    public RTPParser(DataSource input) throws IOException, IncompatibleSourceException {
        dataParser=new RawBufferParser();
        dataParser.setSource(input);
        dataParser.open();
        dataParser.start(); // esegue anche input.start()

        trackList=dataParser.getTracks();
        numberOfTracks=trackList.length;
    }

    
    public void close() {
        dataParser.stop();
        dataParser.close();
    }

    
    public ExtBuffer getFrame(int trackNumber) {
        if (!trackList[trackNumber].isEnabled()) return null;
        
        ExtBuffer frame=new ExtBuffer();
        Track videoTrack=trackList[trackNumber];
        videoTrack.readFrame(frame);
        return frame;
        // il parser non interviene sui valori dei flag o dei campi quali timestamp o numero di sequenza (a
        // differenza per esempio di un parser che legge da un file) poichè i frames ricevuti via RTP sono già stati
        // caratterizzati dalla catena che li ha prodotti.
    }
}
