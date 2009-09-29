/**
 * @author Leo Di Carlo, Pire Dejaco
 * @version 1.1
 *
 */

package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import debug.DebugConsole;
import parameters.Parameters;
import server.connection.ServerConnectionFactory;
import server.connection.ServerCM;



/* Classe Singeltone che implementa la classe Observer. Viene registrata sotto la classe Observable AConnectionReceiver.
 * Appena gli arriva una richiesta da parte di qualcuno, viene richiamato il metodo update.
 */
public class ServerSessionManager implements Observer{


	//private String status;
	private int numberOfSession;
	private Hashtable<String, String> ssReferences;
	private DatagramPacket message;
	public static final ServerSessionManager INSTANCE = new ServerSessionManager();
	private String newRelayAddress;
	private ServerCM connManager;
	private DebugConsole console;
	
	
	public ServerSessionManager()
	{
		//this.status = "Idle";
		numberOfSession = 0;
		ssReferences = new Hashtable<String, String>();
		console = new DebugConsole();
		console.setTitle("DEBUG CONSOLLE SERVER SESSION MANAGER");
		connManager = ServerConnectionFactory.getSessionConnectionManager(this,console);
		connManager.start();
	}

	public static ServerSessionManager getInstance() {
		return ServerSessionManager.INSTANCE;
	}

	@Override
	public void update(Observable receiver, Object arg) {
		//StreamingServer sS;
		if(arg instanceof DatagramPacket)
		{
			message = (DatagramPacket) arg;
			try {
				ServerMessageReader.readContent(message);
			} catch (IOException e) {
				console.debugMessage(Parameters.DEBUG_ERROR,"SERVER_SESSION_MANAGER: Errore nella lettura del Datagramma");
				e.printStackTrace();
			}
			
			//Viene fatta una richiesta di un file da parte di un cliente e inoltrata avanti dal relay
			if(ServerMessageReader.getCode() == Parameters.FORWARD_REQ_FILE)
			{
				console.debugMessage(Parameters.DEBUG_INFO,"SERVER_SESSION_MANAGER: Arrivato un FORWARD_REQ_FILE");
				//try {
					//Qui posso creare lo streaming
					//Rispondo all'indirizzo che ha richiesto il file (il relay principale)
					console.debugMessage(Parameters.DEBUG_WARNING,"File Richiesto: "+ServerMessageReader.getFileName());
					console.debugMessage(Parameters.DEBUG_WARNING,"Input streaming relay [ADDRESS::PORT]:"+ ServerMessageReader.getProxyReceivingStreamPort()+"::"+message.getAddress().getHostAddress());
					console.debugMessage(Parameters.DEBUG_WARNING,"Porta di controllo del relay [PORT]: "+ServerMessageReader.getProxyControlPort());
					console.debugMessage(Parameters.DEBUG_WARNING,"Indirizzo del cliente [ADDRESS]: "+ServerMessageReader.getClientAddress());
					
					//TODO CREO STREAMING
					//sS = new StreamingServer(ServerMessageReader.getFileName(), message.getAddress().getHostAddress(), ServerMessageReader.getProxyControlPort(), ServerMessageReader.getProxyReceivingStreamPort(), this, this.consolle);
					numberOfSession++;
					ssReferences.put(ServerMessageReader.getClientAddress(), "Sessione_"+numberOfSession);
					
				/*} catch (NoDataSourceException e) {
					console.debugMessage(Parameters.DEBUG_ERROR,"STREAMING_SERVER(COSTRUTTORE): Errore nel DataSource creato...");
					e.printStackTrace();

				} catch (IOException e) {e.printStackTrace();}*/
			}
			if(ServerMessageReader.getCode() == Parameters.REDIRECT)
			{
				console.debugMessage(Parameters.DEBUG_INFO,"SERVER_SESSION_MANAGER: Arrivata una richiesta di REDIRECT");
				newRelayAddress = message.getAddress().getHostAddress();
				if(!ssReferences.isEmpty())
				{
					//TODO Aggiornare le sessioni streaming con il nuovo indirizzo relay
					
					Enumeration<String> keys = ssReferences.keys();
					while(keys.hasMoreElements())
					{
						String chiave = keys.nextElement().toString();
						console.debugMessage(Parameters.DEBUG_WARNING,"Aggiorno "+ ssReferences.get(chiave)+" con l'indirizzo del nuovo relay [ADDRESS]:"+ newRelayAddress);
						System.out.println(chiave);
						//if(!ssReferences.get(chiave).redirect(this.newRelayAddress))
						//{
							//console.debugMessage(Parameters.DEBUG_ERROR,"Errore nella REDIRECT di uno StreamingServer...");
						//}
						
					}
				}
			}
		}
	}


	/*public void updateSessions()
	{
		if(this.numberOfSession != 0)
		{
			numberOfSession--;
			if(this.numberOfSession == 0)
				this.status = "Idle";
		}
	}*/
}
