package unibo.core.multiplexer;

import java.io.IOException;

import javax.media.Format;
import javax.media.PlugIn;
import javax.media.ResourceUnavailableException;
import javax.media.Time;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;

import com.sun.media.ExtBuffer;
import com.sun.media.multiplexer.RawSyncBufferMux;

/**
 * <p>Classe che definisce l'implementazione del costruttore e dei metodi di un multiplexer conforme all'interfaccia
 * Multiplex e che si occupa del multiplexing di tracce il cui contentuto è in un formato compatibile con RTP.</p>
 * <p>Il content-type in output è di tipo RAW/RTP.</p>
 * @author Alessandro Falchi
 */
public class PlayerMultiplexer implements Multiplex {
	private RawSyncBufferMux rtpMux;
	private DataSource dataOutput;

/**
* Costruisce e inizializza un modulo per il multiplexing di tracce il cui formato è compatibile per essere renderizzate. 
* @param inputFormats array i cui elementi contengono un oggetto di classe Format con la descrizione del formato
* della traccia a cui si riferiscono (ogni traccia corrisponde all'elemento dell'array che ha come indice il
* numero della traccia stessa).    
* @throws IOException se non è possibile aprire il plugin.
*/
	public PlayerMultiplexer(Format[] inputFormats) throws IOException {
		rtpMux=new RawSyncBufferMux();
		rtpMux.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW));
		rtpMux.setNumTracks(inputFormats.length);
		for (int i=0; i<inputFormats.length; i++) {
			if (rtpMux.setInputFormat(inputFormats[i],i)==null)
			throw new IOException("Mux Input non compatible");
		}
		try { rtpMux.open(); }
		catch (ResourceUnavailableException e) { throw new IOException("loading of library jmvh263 failed: "+e); }
		dataOutput=rtpMux.getDataOutput();
	}


	public DataSource getDataOutput() { return dataOutput; }

	public int multiplexFrame(ExtBuffer frame, int trackNumber) {
		int muxResult;

		do { 
			muxResult=rtpMux.process(frame,trackNumber);
			if ((muxResult & PlugIn.PLUGIN_TERMINATED)!=0) return PLUGIN_TERMINATED;
		}
		while (muxResult == PlugIn.INPUT_BUFFER_NOT_CONSUMED);

		return MULTIPLEX_OK;
	}


	public void close() {
		rtpMux.stop();
		rtpMux.close();
	}

	public void setMediaTime(long time){ rtpMux.setMediaTime(new Time(time)); }

}