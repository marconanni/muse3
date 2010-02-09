package unibo.core.transcoder;

import java.io.IOException;

import javax.media.Format;
import javax.media.PlugIn;
import javax.media.ResourceUnavailableException;

import com.ibm.media.codec.video.h263.NativeEncoder;
import com.sun.media.ExtBuffer;

/**
 * Transcoder di formato, da YUV a H263/RTP, basato sul plugin com.ibm.media.codec.video.h263.NativeEncoder<p>
 * I formati di ingresso e di uscita compatibili con questo componente sono quelli previsti dal plugin (cfr.
 * JMFRegistry). In particolare, in ingresso è supportato il formato <b>YUV</b>, mentre in uscita sono possibili i
 * seguenti:
 * <ul>
 * <li>H263
 * <li>H263/RTP
 * </ul><p>
 * @author Alessandro Falchi
 */
public class YUVtoH263Encoder implements Transcode {
    private NativeEncoder h263Encoder;
    private Format outputFormat;

    /**
     * Costruttore.
     * @param inputFormat il formato della traccia passato in ingresso e da cui saranno estratti i frame che verranno
     * elaborati dal metodo processFrame
     * @throws IOException se il formato di input non è compatibile o non è possibile aprire il plugin interno
     */
    public YUVtoH263Encoder(Format inputFormat) throws IOException {
        h263Encoder=new NativeEncoder();
        if (h263Encoder.setInputFormat(inputFormat)==null)
            throw new IOException("Input not compatible with NativeEncoder");
        outputFormat=(h263Encoder.getSupportedOutputFormats(inputFormat))[1];
        h263Encoder.setOutputFormat(outputFormat);
        try { h263Encoder.open(); }
        catch (ResourceUnavailableException e) { throw new IOException("Cannot open NativeEncoder\n"+e); }
    }

    
    public void close() {
        h263Encoder.close();
    }
    
    
    public Format getOutputFormat() {
        return outputFormat;
    }

    
    public int processFrame(ExtBuffer inputFrame, ExtBuffer outputFrame) {
        outputFrame.setTimeStamp(inputFrame.getTimeStamp());
        outputFrame.setDuration(inputFrame.getDuration());
        outputFrame.setSequenceNumber(inputFrame.getSequenceNumber());
        outputFrame.setFlags(inputFrame.getFlags());
        outputFrame.setHeader(inputFrame.getHeader());
		int encResult=h263Encoder.process(inputFrame,outputFrame);
        //TODO: verify del contenuto del buffer - vedi verifyBuffer in BasicModule
		if ((encResult & PlugIn.PLUGIN_TERMINATED)!=0) return PLUGIN_TERMINATED;
		if ((encResult & PlugIn.BUFFER_PROCESSED_FAILED)!=0) return PROCESS_FAILED;
		if ((encResult & PlugIn.OUTPUT_BUFFER_NOT_FILLED)!=0) return OUTPUT_NOT_VALID;
		if ((encResult & PlugIn.INPUT_BUFFER_NOT_CONSUMED)!=0) return PROCESS_INPUT_AGAIN;
		else return TRANSCODE_OK;
    }
}
