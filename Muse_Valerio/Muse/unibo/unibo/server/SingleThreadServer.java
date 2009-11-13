package unibo.server;

import java.io.IOException;
import java.net.InetAddress;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.protocol.DataSource;

import unibo.chains.H263toRTPChain;
import unibo.core.multiplexer.Multiplex;
import unibo.core.multiplexer.RTPMultiplexer;
import unibo.core.rtp.RTPSender;

import com.sun.media.ExtBuffer;

/**
 * @author afalchi
 */
public class SingleThreadServer {

    public static void main(String args[]) {
    	try {
    	    DataSource ds=new com.sun.media.protocol.file.DataSource();
            ds.setLocator(new MediaLocator(args[0]));
            
            H263toRTPChain chain=new H263toRTPChain(ds);
            Format[] muxInput={ chain.getOutputFormat() };
            RTPMultiplexer mux=new RTPMultiplexer(muxInput);
            
            RTPSender transmitter=new RTPSender(Integer.parseInt(args[1]));
            transmitter.addDestination(InetAddress.getByName(args[2]),Integer.parseInt(args[3]));
            transmitter.sendData(mux.getDataOutput());
            
            ExtBuffer frame;
            int muxResult;
            do {
                frame=chain.getFrame();
                muxResult=mux.multiplexFrame(frame,0);
                if (muxResult==Multiplex.PLUGIN_TERMINATED) {
                    System.err.println("Error: plugin terminated");
                    return;
                }
                if ((frame.getFlags() & Buffer.FLAG_RTP_MARKER)!=0) Thread.sleep(66);
            }
            while (!frame.isEOM());
    	}
    	catch (IOException e) { System.err.println("IOEXCEPTION "+e); }
    	catch (InterruptedException e) { System.err.println(e); }
    }
    	

}
