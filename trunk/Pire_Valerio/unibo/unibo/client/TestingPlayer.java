package unibo.client;

import java.awt.Frame;
import java.net.InetAddress;

import javax.media.Manager;
import javax.media.Player;
import javax.media.protocol.DataSource;

import unibo.core.rtp.RTPReceiver;

/**
 * @author afalchi
 */
public class TestingPlayer {

    /* args[0] è il numero di porta su cui il client ascolta l'arrivo dei dati RTP
     * args[1] è l'indirizzo IP del mittente
     * args[2] è la porta da cui trasmette il mittente
     */
    public static void main(String[] args) {
        try {
    		RTPReceiver rtpRx=new RTPReceiver(Integer.parseInt(args[0]));
            rtpRx.setSender(InetAddress.getByName(args[1]),Integer.parseInt(args[2]));
            DataSource dsInput=rtpRx.receiveData();

            Player player = Manager.createRealizedPlayer(dsInput);
            Frame frame = new Frame("Now Playing...");
            frame.add(player.getVisualComponent());
            frame.setSize(230, 150);
            frame.add(player.getControlPanelComponent());
            frame.show();
            player.start();
        }
        catch (Exception e) { System.err.println(e); }
    }
}
