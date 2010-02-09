package unibo.util;

import java.io.IOException;

import javax.media.Buffer;
import javax.media.MediaLocator;
import javax.media.protocol.DataSource;

import unibo.core.CircularBuffer;
import unibo.core.parser.Parser;
import unibo.core.parser.QuickTimeFileParser;
import unibo.core.thread.ParserThread;
import unibo.core.thread.TranscodeThread;
import unibo.core.transcoder.H263toYUVDecoder;
import unibo.core.transcoder.Transcode;
import unibo.core.transcoder.YUVtoH263Encoder;

import com.sun.media.ExtBuffer;

/**
 * @author afalchi
 */
public class FrameStatistics {

    public static void main(String[] args) {
        DataSource ds=new com.sun.media.protocol.file.DataSource();
        ds.setLocator(new MediaLocator("file://C:/afalchi/misc/starwars.mov"));
        
        try {
            // ***** PARSER *****
            Parser qtParser=new QuickTimeFileParser(ds);
            ParserThread parserThread=new ParserThread(qtParser,10);
            
            //CircularBuffer buffer=parserThread.getOuputBuffer();

            // ***** DECODER DA H263 A YUV *****
            Transcode decoder=new H263toYUVDecoder(qtParser.getTrackFormat(0));
            TranscodeThread decoderThread=new TranscodeThread(decoder,
                    parserThread.getOutputBufferSet()[0],
                    new CircularBuffer(20));
            
            //CircularBuffer buffer=decoderThread.getOuputBuffer();
            
            // ***** ENCODER DA YUV A H263/RTP *****
            Transcode encoder=new YUVtoH263Encoder(decoder.getOutputFormat());
            TranscodeThread encoderThread=new TranscodeThread(encoder,
                    decoderThread.getOuputBuffer(),
                    new CircularBuffer(40));
            
            CircularBuffer buffer=encoderThread.getOuputBuffer();

            parserThread.start();
            decoderThread.start();
            encoderThread.start();

            ExtBuffer frame=buffer.getFrame();
            int zeroMarkedFrame=0;
            int minFrameSize=frame.getLength();
            int maxFrameSize=frame.getLength();
            int frameAx=frame.getLength();
            int frameCounter=1;
            if ((frame.getFlags() & Buffer.FLAG_RTP_MARKER)==0) zeroMarkedFrame++;
            
            do {
                frame=buffer.getFrame();
                if (frame.getLength()>maxFrameSize) maxFrameSize=frame.getLength();
                else if (frame.getLength()<minFrameSize) minFrameSize=frame.getLength();
                frameCounter++;
                frameAx=frameAx+frame.getLength();
                if ((frame.getFlags() & Buffer.FLAG_RTP_MARKER)==0) zeroMarkedFrame++;
            }
            while (!frame.isEOM());
            
            System.out.println("Format: "+frame.getFormat());
            System.out.println("Processed frames: "+frameCounter);
            System.out.println("min frame size (bytes): "+minFrameSize);
            System.out.println("med frame size (bytes): "+frameAx/frameCounter);
            System.out.println("max frame size (bytes): "+maxFrameSize);
            System.out.println("Zero Marked Frame: "+zeroMarkedFrame);
        }
        catch (IOException e) {
            System.err.println("Init Failed: "+e);
            System.exit(1);
        }
    }
}