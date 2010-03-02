/**
 * 
 */
package client;

import java.awt.FileDialog;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import parameters.BufferConfiguration;
import parameters.MessageCodeConfiguration;
import parameters.NetConfiguration;
import parameters.PortConfiguration;
import parameters.SessionConfiguration;
import parameters.TimeOutConfiguration;

import client.gui.IClientView;

import unibo.core.BufferEmptyEvent;
import unibo.core.BufferEmptyListener;
import unibo.core.BufferFullEvent;
import unibo.core.BufferFullListener;
import unibo.core.EventCircularBuffer;
import client.connection.ClientConnectionFactory;
import client.connection.ClientCM;
import client.connection.ClientPortMapper;
import client.gui.ClientFrameController;
import client.messages.ClientMessageFactory;
import client.messages.ClientMessageReader;
import client.timeout.ClientTimeoutFactory;
import client.timeout.TimeOutSingleWithMessage;
/**
 * @author Leo Di Carlo
 * 
 */
public class ClientSessionManager implements Observer, BufferFullListener,
		BufferEmptyListener {

	private ClientFrameController frameController;
	private String filename;
	private ClientCM sessionCM;
	private DatagramPacket msg;
	private String eventType;
	private TimeOutSingleWithMessage timeoutSearch;
	private TimeOutSingleWithMessage timeoutFileRequest;
	private String relayAddress; //provo lo styream diretto server->client

	//private String serverAddress;
	private String[] files;
	private String listaFile;
	private int msgIdx = 0;
	
	private String status;
	private ClientBufferDataPlaying clientPlaying;
	private ClientPortMapper portMapper;
	private int myStreamingPort;
	private int proxyStreamingPort = -1;
	private int proxyCtrlPort = -1;
	private ClientMessageReader messageReader;
	private ClientElectionManager electionManager;
	private boolean bfSent = true;
	private Timer runner;
	private String newrelay;
	
	private Valerio valerio;
	
	/**
	 * @param electionManager
	 *            the electionManager to set
	 */
	
	 public void setElectionManager(ClientElectionManager electionManager) {
	 this.electionManager = electionManager; }


	public static final ClientSessionManager INSTANCE = new ClientSessionManager();

	/**
	 * @param frameController
	 *            the frameController to set
	 */
	public void setFrameController(ClientFrameController frameController) {
		System.out.println("ho fatto serFrameController");
		this.frameController = frameController;
	}

	public ClientSessionManager() {
		this.sessionCM = ClientConnectionFactory.getSessionConnectionManager(this,false);
		this.sessionCM.start();
		this.status = "Idle";
		this.myStreamingPort = PortConfiguration.CLIENT_PORT_RTP_IN;
		
	//	this.relayAddress=NetConfiguration.RELAY_CLUSTER_ADDRESS;//Valerio: simulo che il client sia collegato ad un relay
														//e non a bigboss, questa parte va modificata, non posso sapere a priori
														//a chi mi collego
		System.out.println("finito costruttore clientsessionmanager");
	}

	public static ClientSessionManager getInstance() {
		return ClientSessionManager.INSTANCE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object) Il
	 * metodo può essere richiamato da diverse entità all'interno del sistema:
	 * ClientElectionManager, ClientBufferDataPlaying ConnectionManager e dai
	 * relativi TimoOut rintracciabili all'interno della TimeOutFactory. GLi
	 * eventi possono essere o avvisi come nel caso dei timeout oppure
	 * pacchetti. gli eventi possono essere
	 */
	@Override
	public synchronized void update(Observable arg, Object event) {
		// TODO Auto-generated method stub
		
		
//		try {
//			this.clientPlaying=new ClientBufferDataPlaying(PortConfiguration.CLIENT_PORT_SESSION_OUT, this.myStreamingPort, InetAddress.getByName(this.relayAddress), BufferConfiguration.CLIENT_BUFFER, 0, this.frameController);
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			System.err.println("ERRORE NELLA CREAZIONE DI CLIENTPLAYING");
//		}
		
		
		
		this.messageReader = new ClientMessageReader();
		
		if(event instanceof String) {
			System.out.println("ClientSessionManager: metodo update, è arrivato una STRINGA"+((String)event)+" stato:"+status);
			this.eventType = (String)event;
						
			if(eventType.equals("TIMEOUTSEARCH")){
				System.err.println("SCATTATO TIMEOUTSEARCH...");
				this.status ="Idle";
				this.frameController.debugMessage("SCATTATO TIMEOUTSEARCH...");
				this.frameController.debugMessage("Relay irraggiungibile...RIPROVARE PIU' TARDI");
			}
		 
			if(eventType.equals("TIMEOUTFILEREQUEST")){
				System.err.println("SCATTATO TIMEOUTFILEREQUEST...");
				this.frameController.debugMessage("SCATTATO TIMEOUT_FILE_REQUEST...");
			}
		  
			if(eventType.equals("BUFFER_FULL") && status.equals("WaitingForPlaying")) {
				frameController.debugMessage("Buffer Full");
//				try {
//					Thread.sleep(2000);
//				} catch (InterruptedException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
				if(this.clientPlaying != null)
					this.clientPlaying.startPlaying();
				status = "Playing";
				try {
					if(this.relayAddress!= null ||this.proxyCtrlPort != -1){
						this.msg =ClientMessageFactory.buildStopTX(0,InetAddress.getByName(relayAddress),this.proxyCtrlPort);
						sessionCM.sendTo(this.msg);
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if(eventType.equals("BUFFER_FULL") && status.equals("Playing")) {
				frameController.debugMessage("Buffer Full");
				try {
					if(this.relayAddress!= null || this.proxyCtrlPort != -1){
						this.msg =ClientMessageFactory.buildStopTX(0,InetAddress.getByName(relayAddress), this.proxyCtrlPort);
						sessionCM.sendTo(this.msg);
					}
				}
				catch (UnknownHostException e) { 
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if(eventType.equals("BUFFER_EMPTY") && status.equals("Playing")) {
				frameController.debugMessage("Buffer Empty");
					try {
						if(this.relayAddress!= null || this.proxyCtrlPort != -1){
							this.msg =ClientMessageFactory.buildStartTX(0,InetAddress.getByName(relayAddress), this.proxyCtrlPort);
							sessionCM.sendTo(this.msg);
						}
					}
					catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}			
			
			if(eventType.equals("RECIEVED_ELECTION_REQUEST") && this.status.equals("Playing")) {
				System.out.println("ricevuta richiesta di elezione quando lo status era playing");
				this.frameController.debugMessage("ricevuta richiesta di elezione quando lo status era playing");

				// disabilitato ingrandimento del buffer
//				getClientBufferDataPlaying().setThdOnBuffer(BufferConfiguration.BUFFER_THS_START_TX + ((BufferConfiguration.CLIENT_BUFFER/2)%2 == 0 ? BufferConfiguration.CLIENT_BUFFER/2:BufferConfiguration.CLIENT_BUFFER/2+1), false);

				//  temporaneamente disabilitato ingrandimento buffer;
//				this.clientPlaying.setThdOnBuffer(BufferConfiguration.BUFFER_THS_START_TX + ((BufferConfiguration.CLIENT_BUFFER/2)%2 == 0 ? BufferConfiguration.CLIENT_BUFFER/2:BufferConfiguration.CLIENT_BUFFER/2+1), false);

				}
		 
			if(eventType.equals("EMERGENCY_ELECTION")) {
				this.clientPlaying.close();
				this.clientPlaying = null;
				this.relayAddress = null;
				this.frameController.debugMessage("CLIENT SESSION MANAGER: ELEZIONE D'EMERGENZA, SESSIONE INVALIDATA, RIPROVARE PIÙ TARDI CON UNA NUOVA RICHIESTA");
				System.out.println("CLIENT SESSION MANAGER: ELEZIONE D'EMERGENZA, SESSIONE INVALIDATA, RIPROVARE PIÙ TARDI CON UNA NUOVA RICHIESTA");
			}
		 
			if(eventType.contains("NEW_RELAY")){
				StringTokenizer st = new StringTokenizer(eventType, ":");
				st.nextToken();
				this.newrelay =st.nextToken();
			}
		  
			if(eventType.equals("PLAYER_PAUSED")) { this.status = "Paused"; }
			
			if(eventType.equals("PLAYER_STARTED")) { this.status = "Playing"; }
		 
			if(eventType.equals("RELAY_FOUND") && this.status.equals("SearchingRelay")) {
				this.frameController.debugMessage("RELAY CONTATTATO, INOLTRO LA RICHIESTA...");
				StringTokenizer st = new StringTokenizer(eventType, ":");
				st.nextToken();
				this.relayAddress = st.nextToken();
				try {
					this.msg = ClientMessageFactory.buildRequestFile(0, this.filename, this.myStreamingPort, InetAddress.getByName(relayAddress), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
					sessionCM.sendTo(msg);
					this.status = "WaitingForResponse"; this.timeoutFileRequest =ClientTimeoutFactory.getSingeTimeOutWithMessage(this,TimeOutConfiguration.TIMEOUT_FILE_REQUEST, TimeOutConfiguration.TIME_OUT_FILE_REQUEST); } catch (UnknownHostException e) {
					}
				catch(IOException e) {
					e.printStackTrace();
				} 
			}
		 
			if(eventType.equals("RELAY_FOUND") && this.status.equals("Idle")) {
				StringTokenizer st = new StringTokenizer(eventType, ":");
				st.nextToken();
				this.relayAddress = st.nextToken();
				this.frameController.debugMessage("CLIENT SESSION MANAGER: l'indirizzo del relay trovato da CLIENT ELECION MANAGER è "+this.relayAddress);
			}
		
			if(eventType.equals("END_OF_MEDIA")) {
				this.clientPlaying.close(); this.clientPlaying = null;
				this.electionManager.setServed(false);
			}
				 
		}
		
		
		if (event instanceof DatagramPacket) {
			this.msg = (DatagramPacket) event;
			try {
				messageReader.readContent(this.msg);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.println("CLIENT_SESSION_MANAGER: Errore nella lettura del Datagramma");
				//Logger.write("CLIENT_SESSION_MANAGER: Errore nella lettura del Datagramma");
				e.printStackTrace();
			}
			
			System.err.println("ClientSessionManager: codice del messaggio ricevuto "+messageReader.getCode()+", ricevuto da "+messageReader.getPacketAddress());
			if (messageReader.getCode() == MessageCodeConfiguration.LIST_RESPONSE) {
				listaFile = messageReader.getListaFile();
				files = listaFile.split(",");
				System.out.println("Lista file:");
				frameController.debugMessage("Lista File:");
				for (int i = 0; i < files.length; i++) {
					System.out.println(files[i]);
					frameController.debugMessage(files[i]);
				}
				String fileScelto = frameController.getFrame().openDialogFiles(files); //nome del file scelto dall'utente
				System.out.println("File scelto:"+fileScelto);
				frameController.debugMessage("File scelto: "+fileScelto);
				requestFile(fileScelto);//metodo che fa partire la richiesta del file al server
			}

//			if(messageReader.getCode()==MessageCodeConfiguration.ACK_REQUEST_FILE){//con questo ack il server ci dice che gli è arrivata la richiesta del file
//				System.out.println("è arrivato l'ack della richiesta stream");
//				frameController.debugMessage("è arrivato l'ack della richiesta stream");
//				
//				//ora mando al server il messaggio per far partire lo streaming
//				try {
//					System.err.println("E' arrivato ACK_REQUEST_FILE da "+relayAddress+":"+messageReader.getRelayControlPort());
//					this.msg=ClientMessageFactory.buildStartTX(msgIdx++, InetAddress.getByName(this.relayAddress),messageReader.getRelayControlPort());//da rinominare
//					sessionCM.sendTo(this.msg);
//					
//					long inizio = System.currentTimeMillis();
//					System.out.println("INIZIO (OVVERO ISTANTE IN CUI MANDO START_TX): "+inizio);
//				} catch (UnknownHostException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				System.out.println("mandato all'indirizzo "+this.relayAddress+", sulla porta "+21001+" il messaggio di invio trasmissione");
//				frameController.debugMessage("mandato all'indirizzo "+this.relayAddress+", sulla porta "+21001+" il messaggio di invio trasmissione");
//				
//				System.out.println("dovrebbe iniziare a suonare qui");
//				clientPlaying.start();
//				
//				
//			}
			
			if (messageReader.getCode() == MessageCodeConfiguration.SERVER_UNREACHEABLE
					&& status.equals("WaitingForResponse")) {
				if (this.timeoutFileRequest != null)
					this.timeoutFileRequest.cancelTimeOutSingleWithMessage();
				this.frameController
						.debugMessage("CLIENT SESSION MESSAGE: SERVER IRRAGGIUNGIBILE");
			}
			if (messageReader.getCode() == MessageCodeConfiguration.ACK_CLIENT_REQ && status.equals("WaitingForResponse")) {

				System.out.println("ACK_CLIENT_REQ "+messageReader.getCode());
				if(this.timeoutFileRequest !=null) this.timeoutFileRequest.cancelTimeOutSingleWithMessage();
				this.proxyStreamingPort = messageReader.getRelaySendingPort();
				this.proxyCtrlPort = messageReader.getRelayControlPort();
				try { 
					this.clientPlaying = new ClientBufferDataPlaying(this.proxyStreamingPort, this.myStreamingPort, InetAddress.getByName(relayAddress),BufferConfiguration.CLIENT_BUFFER, BufferConfiguration.TTW, this, this.frameController);
					String[] data = new String[7];
					data[0] = InetAddress.getByName(NetConfiguration.CLIENT_ADDRESS).getHostAddress();
					data[1] = String.valueOf(PortConfiguration.CLIENT_PORT_SESSION_IN);
					data[2] = String.valueOf(this.myStreamingPort);
					data[3] = this.relayAddress;
					data[4] = String.valueOf(this.proxyCtrlPort);
					data[5] = String.valueOf(this.proxyStreamingPort);
					data[6] = this.filename;
					this.frameController.setDataText(data);
					//this.clientPlaying.preparingSession();//perparingSession non esiste, forse è nel vecchio databuffer play
					this.clientPlaying.start();
				 
				/*
					runner=new Timer();
					runner.schedule(new	TimerTask(){
						public void voidrun(){System.out.println("PARTITO IL PLAYER - MULTIPLEXER...PASSATI "+Parameters.PLAYBACK_DELAY_START);
						clientPlaying.startPlaying();}}, Parameters.PLAYBACK_DELAY_START);
				*/
					//this.clientPlaying.startPlaying(); this.status = "Playing";
					try {
						this.msg = ClientMessageFactory.buildStartTX(0, InetAddress.getByName(relayAddress), this.proxyCtrlPort);
						System.out.println("costruito messaggio StartTX da inviare a "+InetAddress.getByName(relayAddress)+":"+this.proxyCtrlPort);
						sessionCM.sendTo(msg);
						long inizio = System.currentTimeMillis();
						System.out.println("INIZIO (OVVERO ISTANTE IN CUI MANDO START_TX): "+inizio);
						status = "WaitingForPlaying";
						
						valerio=new Valerio("/home/valerio/statisticabufferclient.txt",clientPlaying.getEventBuffer());
						valerio.start();
						
						if(this.electionManager!=null)
							this.electionManager.setServed(true);
						}
					catch (IOException e) {
						e.printStackTrace();
				 	} 
				}
				catch (Exception e1) {
					e1.printStackTrace();
			 	}  
			}
			if (messageReader.getCode() == MessageCodeConfiguration.LEAVE) {
				System.err.println("LEAVE");
				
				this.relayAddress =this.newrelay;
				this.frameController.setNewRelayIP(this.relayAddress);


				
				// disabilitato rimpicciolimento del buffer
//				this.clientPlaying.setThdOnBuffer(BufferConfiguration.BUFFER_THS_START_TX, true);

				// disabilitato rimpicciolimento buffer
//				this.clientPlaying.setThdOnBuffer(BufferConfiguration.BUFFER_THS_START_TX, true);

				this.clientPlaying.redirectSource(this.relayAddress);

				
				
				

				
				// aggiunto invio di startTX
//				try {
//					this.msg = ClientMessageFactory.buildStartTX(0, InetAddress.getByName(relayAddress), this.proxyCtrlPort);
//				} catch (UnknownHostException e) {
//					//  Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					//  Auto-generated catch block
//					e.printStackTrace();
//				}
//				try {
//					System.out.println("costruito messaggio StartTX da inviare a "+InetAddress.getByName(relayAddress)+":"+this.proxyCtrlPort);
//				} catch (UnknownHostException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				sessionCM.sendTo(msg);
//			
//
				}

			
		 }
	}
		 
	
	// VALERIO SANDRI
	public void requestList() {
		try {
			//this.msg = ClientMessageFactory.buildRequestList(msgIdx++, InetAddress.getByName(this.serverAddress),Parameters.SERVER_SESSION_PORT_IN);
			this.msg = ClientMessageFactory.buildRequestList(msgIdx++, InetAddress.getByName(this.relayAddress),PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
			sessionCM.sendTo(this.msg);
			System.out.println("Inviata richiesta lista file a "+this.relayAddress+":"+PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
			frameController.debugMessage("Inviata richiesta lista file a "+this.relayAddress+":"+PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	/* 
	 * @param filename
	 */
	public void requestFile(String filename) {
		System.out.println("ClientSessionManager: requestFile");
		if(this.relayAddress!=null)	{
			 System.out.println("relayAddress: " + this.relayAddress);
			this.filename=filename;
			try {
				this.msg = ClientMessageFactory.buildRequestFile(msgIdx++,this.filename, this.myStreamingPort, InetAddress.getByName(this.relayAddress),PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
				sessionCM.sendTo(this.msg);
				System.out.println("waiting for response");
				frameController.debugMessage("waiting for response");
			
				this.status = "WaitingForResponse";
/*				this.timeoutFileRequest = ClientTimeoutFactory
						.getTimeOutFileRequest(this,
								Parameters.TIMEOUT_FILE_REQUEST);
*/
			
				
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.err.println("l'indirizzo del relay è null");
			// this.electionManager.tryToSearchTheRelay();
			// this.frameController.debugMessage("RELAY DISCOVERING...\nSE NON SI RICEVONO NOTIFICHE ENTRO BREVE SI CONSIGLIA DI CAMBIARE POSIZIONE E RIPROVARE.");
		}
	}

	public void bufferFullEventOccurred(BufferFullEvent e) {

		if (bfSent) {
			// se non � stato gi� inviato, viene inviato al proxy un messaggio
			// di buffer full
			//this.bufferControl();
			//setChanged();
			//this.update(this, "BUFFER_FULL");
			this.update(null, "BUFFER_FULL");
			bfSent = false;
		}
	}

	public void bufferEmptyEventOccurred(BufferEmptyEvent e) {
		// LUCA: tolto il ! qui
	//	if (!bfSent) {
			// se non � stato gi� inviato, viene inviato al proxy un messaggio
			// di buffer full
			this.update(null, "BUFFER_EMPTY");
			// LUCA: messo false qui
			bfSent = true;
		//}
	}

	public String getListaFile() {
		return listaFile;
	}

	public static void main(String[] args) {
		ClientSessionManager.getInstance();
	}
	
	public void setRelayAddress(String relayAddress){this.relayAddress = relayAddress;}

	public void setClientBufferDataPlaying(ClientBufferDataPlaying player){this.clientPlaying = player;}
	
	public ClientBufferDataPlaying getClientBufferDataPlaying(){return this.clientPlaying;}
}
