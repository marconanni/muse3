package unibo.core.transcoder;

import java.io.IOException;

import javax.media.Format;
import javax.media.PlugIn;
import javax.media.ResourceUnavailableException;

import com.sun.media.ExtBuffer;
import com.sun.media.codec.video.vh263.NativeDecoder;

/**
 * Transcoder di formato, da H263 a YUV, basato sul plugin com.sun.media.codec.video.vh263.NativeDecoder<p>
 * I formati di ingresso e di uscita compatibili con questo componente sono quelli previsti dal plugin (cfr.
 * JMFRegistry). In particolare, in ingresso sono supportati:
 * <ul>
 * <li>H263
 * <li>H263/RTP
 * <li>H263-1998/RTP
 * </ul><p>
 * mentre il formato di uscita è <b>YUV</b>
 * @author Alessandro Falchi
 */
public class H263toYUVDecoder implements Transcode {
    private NativeDecoder h263Decoder;
    private Format outputFormat;
    
    /**
     * Costruttore.
     * @param inputFormat il formato della traccia passato in ingresso e da cui saranno estratti i frame che verranno
     * elaborati dal metodo processFrame
     * @throws IOException se il formato di input non è compatibile o non è possibile aprire il plugin interno
     */
    public H263toYUVDecoder(Format inputFormat) throws IOException {
        h263Decoder=new NativeDecoder();
        if (h263Decoder.setInputFormat(inputFormat)==null)
            throw new IOException("Input non compatible with NativeDecoder");
        outputFormat=(h263Decoder.getSupportedOutputFormats(inputFormat))[0];
        h263Decoder.setOutputFormat(outputFormat);
        try { h263Decoder.open(); }
        catch (ResourceUnavailableException e) { throw new IOException("Cannot open NativeDecoder\n"+e); }
    }

    
    public void close() {
        h263Decoder.close();
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
		int decResult=h263Decoder.process(inputFrame,outputFrame);
        //TODO: verify del contenuto del buffer - vedi verifyBuffer in BasicModule
		if ((decResult & PlugIn.PLUGIN_TERMINATED)!=0) return PLUGIN_TERMINATED;
		if ((decResult & PlugIn.BUFFER_PROCESSED_FAILED)!=0) return PROCESS_FAILED;
		if ((decResult & PlugIn.OUTPUT_BUFFER_NOT_FILLED)!=0) return OUTPUT_NOT_VALID;
		if ((decResult & PlugIn.INPUT_BUFFER_NOT_CONSUMED)!=0) return PROCESS_INPUT_AGAIN;
		else return TRANSCODE_OK;
    }
}
