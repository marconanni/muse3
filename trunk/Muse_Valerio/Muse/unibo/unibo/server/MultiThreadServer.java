package unibo.server;

import java.io.IOException;
import java.net.InetAddress;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.protocol.DataSource;
import unibo.core.CircularBuffer;
import unibo.core.multiplexer.RTPMultiplexer;
import unibo.core.parser.Parser;
import unibo.core.parser.QuickTimeFileParser;
import unibo.core.rtp.RTPSender;
import unibo.core.thread.MultiplexerThread;
import unibo.core.thread.ParserThread;
import unibo.core.thread.TranscodeThread;
import unibo.core.transcoder.H263toYUVDecoder;
import unibo.core.transcoder.Transcode;
import unibo.core.transcoder.YUVtoH263Encoder;


/**
 * Inizializza e avvia un server per l'invio con protocollo RTP del contenuto multimediale prelevato da un file locale.
 * Questa classe si basa sulle classi definite nel package unibo e nei suoi sottopackage.
 * NOTA: questo server è unicamente in grado di trattare un file QuickTime monotraccia codificato con formato H263,
 * poichè costruisce e inizializza una catena di plugin specifica.
 * @author Alessandro Falchi
 */
public class MultiThreadServer {
    
    /* args[0] è il percorso del file contenente il contenuto multimediale da spedire. Tale percorso è in formato URL;
     * ad esempio:
     * 		file://C:/test/video/starwars.mov
     * 		file://video//starwars.mov
     * args[1] è la porta da cui trasmette il server
     * args[2] è l'indirizzo IP del destinatario
     * args[3] è la porta di ascolto del destinatario
     */
    public static void main(String args[]) {
    	try {
    	    DataSource ds=new com.sun.media.protocol.file.DataSource();
            ds.setLocator(new MediaLocator(args[0]));
            
            // ***** PARSER *****
            Parser qtParser=new QuickTimeFileParser(ds);
            ParserThread parserThread=new ParserThread(qtParser,5); 
            
            // ***** DECODER DA H263 A YUV *****
            Transcode decoder=new H263toYUVDecoder(qtParser.getTrackFormat(0));
            TranscodeThread decoderThread=new TranscodeThread(decoder,
                    parserThread.getOutputBufferSet()[0],
                    new CircularBuffer(5));

            // ***** ENCODER DA YUV A H263/RTP *****
            Transcode encoder=new YUVtoH263Encoder(decoder.getOutputFormat());
            TranscodeThread encoderThread=new TranscodeThread(encoder,
                    decoderThread.getOuputBuffer(),
                    new CircularBuffer(300));
            
            // ***** MULTIPLEXER *****
            Format[] muxInputsFormat={ encoder.getOutputFormat() };
            RTPMultiplexer mux=new RTPMultiplexer(muxInputsFormat);
            CircularBuffer[] muxInputsBuffer={ encoderThread.getOuputBuffer() };
            MultiplexerThread muxThread=new MultiplexerThread(mux,muxInputsBuffer,args[2]);
            muxThread.setFrameRate(7);
            
            
            
            RTPSender transmitter=new RTPSender(Integer.parseInt(args[1]));
            transmitter.addDestination(InetAddress.getByName(args[2]),Integer.parseInt(args[3]));
            transmitter.addDestination(InetAddress.getLocalHost(),10500);
            transmitter.sendData(mux.getDataOutput());
            
            parserThread.start();
            decoderThread.start();
            encoderThread.start();
            while(!((CircularBuffer)encoderThread.getOuputBuffer()).isFull())
            {
            	Thread.sleep(1);
            }
            System.out.println("Buffer pieno: rallento il thread di immissione...");
            encoderThread.setWait(500);
            //LoggerThread log=new LoggerThread(((CircularBuffer)encoderThread.getOuputBuffer()));
            muxThread.start();
            
            //log.start();
            while(((CircularBuffer)encoderThread.getOuputBuffer()).getStatus()>=0.6)
            {
            	Thread.sleep(100);
            }
            
            System.out.println("i thread ora vengono pareggiati...");
            encoderThread.setWait(142);
            encoderThread.setLevelManager(180);
            transmitter.removeTarget(InetAddress.getLocalHost(),10500);
            
    	}
        catch (IOException e) { System.err.println(e); }
        catch(Exception e ){System.err.println(e);}
    }
}
