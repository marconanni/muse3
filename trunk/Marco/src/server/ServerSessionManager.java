/**
 * 
 */
package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;

import javax.media.NoDataSourceException;

import debug.DebugConsolle;

import parameters.Parameters;
import util.Logger;
import server.connection.ServerConnectionFactory;
import server.connection.ServerCM;

/**
 * @author Leo Di Carlo
 *
 */
public class ServerSessionManager implements Observer{


	private String status;
	private int numberOfSession;
	private Hashtable<String, StreamingServer> ssReferences;
	private DatagramPacket message;
	public static final ServerSessionManager INSTANCE = new ServerSessionManager();
	private String newRelayAddress;
	private ServerCM connManager;
	private DebugConsolle consolle;
	
	
	public ServerSessionManager()
	{
		this.status = "Idle";
		this.numberOfSession = 0;
		ssReferences = new Hashtable();
		connManager = ServerConnectionFactory.getSessionConnectionManager(this);
		connManager.start();
		this.consolle = new DebugConsolle();
		this.consolle.setTitle("DEBUG CONSOLLE SERVER SESSION MANAGER");
	}

	public static ServerSessionManager getInstance() {
		return ServerSessionManager.INSTANCE;
	}


	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable receiver, Object arg) {
		// TODO Auto-generated method stub
		StreamingServer sS;
		if(arg instanceof DatagramPacket)
		{
			this.message = (DatagramPacket) arg;
			try {
				ServerMessageReader.readContent(this.message);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.println("SERVER_SESSION_MANAGER: Errore nella lettura del Datagramma");
				Logger.write("SERVER_SESSION_MANAGER: Errore nella lettura del Datagramma");
				consolle.debugMessage("SERVER_SESSION_MANAGER: Errore nella lettura del Datagramma");
				e.printStackTrace();
			}
			if(ServerMessageReader.getCode() == Parameters.FORWARD_REQ_FILE)
			{
				consolle.debugMessage("SERVER_SESSION_MANAGER: Arrivato un FORWARD_REQ_FILE");
				try {
					sS = new StreamingServer(ServerMessageReader.getFileName(), this.message.getAddress().getHostAddress(), ServerMessageReader.getProxyControlPort(), ServerMessageReader.getProxyReceivingStreamPort(), this, this.consolle);
					ssReferences.put(ServerMessageReader.getClientAddress(), sS);
					numberOfSession++;
				} catch (NoDataSourceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Logger.write("STREAMING_SERVER(COSTRUTTORE): Errore nel DataSource creato...");
					this.consolle.debugMessage("STREAMING_SERVER(COSTRUTTORE): Errore nel DataSource creato...");

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(ServerMessageReader.getCode() == Parameters.REDIRECT)
			{
				consolle.debugMessage("SERVER_SESSION_MANAGER: Arrivata una richiesta di REDIRECT");
				this.newRelayAddress = this.message.getAddress().getHostAddress();
				if(!ssReferences.isEmpty())
				{
					Enumeration keys = ssReferences.keys();
					while(keys.hasMoreElements())
					{
						String chiave = keys.nextElement().toString();
						System.out.println(chiave);
						if(!ssReferences.get(chiave).redirect(this.newRelayAddress))
						{
							System.err.println("Errore nella REDIRECT di uno StreamingServer...");
							Logger.write("Errore nella REDIRECT di uno StreamingServer...");
							consolle.debugMessage("Errore nella REDIRECT di uno StreamingServer...");
						}
						
					}
				}
			}
		}
	}


	public void updateSessions()
	{
		if(this.numberOfSession != 0)
		{
			numberOfSession--;
			if(this.numberOfSession == 0)
				this.status = "Idle";
		}
	}
}
