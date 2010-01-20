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
 * @author Leo Di Carlo, Marco Nanni
 *
 */
public class ServerSessionManager implements Observer{

	//Marco: Il server Session manager gira sull Access Point

	private String status;
	private int numberOfSession;  // Marco: è il numero delle sessioni aperte
	private Hashtable<String, StreamingServer> ssReferences;   // Marco: c'è uno Streaming server per ogni flusso, quindi uno per ogni client servito nella rete. l'Ip dovrebbe essere l'IP del client, che rappresenta un identificatore univoco per la sessione di streaming
	private DatagramPacket message;
	public static final ServerSessionManager INSTANCE = new ServerSessionManager(); // Marco: il Server Session Manager è un singleton
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
			if(ServerMessageReader.getCode() == Parameters.FORWARD_REQ_FILE)	//Marco: arriva una richiesta di un nuovo file, bisogna aprire un nuovo canale di streaming 
			{
				consolle.debugMessage("SERVER_SESSION_MANAGER: Arrivato un FORWARD_REQ_FILE");
				try {
					sS = new StreamingServer(ServerMessageReader.getFileName(), this.message.getAddress().getHostAddress(), ServerMessageReader.getProxyControlPort(), ServerMessageReader.getProxyReceivingStreamPort(), this, this.consolle); //Marco: creo un nuovo streaming server passandogli le informazioni di cui necessita: il nome del file della canzone; l'indirizzo del relay sul quale gira il proxy su cui mandare lo streaming, la prota di controllo e quella di streaming, un riferimento a se stesso e alla consolle di debug 
					ssReferences.put(ServerMessageReader.getClientAddress(), sS); // Marco: inserisce lo Streaming server nella tabella usando come chiave l'indirizzo del client
					numberOfSession++; // Marco: aumenta di uno il numero delle sessioni attualmente aprete
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
				/*
				           ricevuto il REDIRECT, CONSIDERA IL MITTENTE DEL MESSAGGIO (ottenuto dal

  						header del pacchetto UDP) come NEW RELAY e in seguito consulta la sua HashTable
						  riempita con coppie (ip_client, StreamingServer), e per ogni sua entry impone al relativo
						  StreamingServer di fermare temporaneamente la produzione di frames, di ridirigere la
						  propria sessione RTP alle stesse porte ma su l’IP del NEW RELAY, e, in seguito, di
						  riprendere la trasmissione.

				 */
			{
				consolle.debugMessage("SERVER_SESSION_MANAGER: Arrivata una richiesta di REDIRECT");
				this.newRelayAddress = this.message.getAddress().getHostAddress(); // Marco: estrae dal messaggio il mittente: è il nuovo relay
				if(!ssReferences.isEmpty())  //Marco: ridirige ogni stream verso il nuovo relay: ricorda che c'è uno straming server per ogni flusso
				{
					Enumeration keys = ssReferences.keys();
					while(keys.hasMoreElements())
					{
						String chiave = keys.nextElement().toString(); // Marco la chiave è l'ip che fa da identificatore per la sessione
						System.out.println(chiave);
						if(!ssReferences.get(chiave).redirect(this.newRelayAddress)) // Marco: chiama il metodo redirect dello Streaming server collegato a quella chiave, passandogli come parametro l'indirizzo del nuovo relay.
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
