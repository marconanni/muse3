/**
 * 
 */
package relay;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.StringTokenizer;

import javax.media.NoDataSourceException;

import debug.DebugConsole;
import debug.DebugConsolle;

import parameters.*;
import relay.connection.RelayCM;
import relay.connection.RelayConnectionFactory;
import relay.timeout.RelayTimeoutFactory;
import relay.timeout.TimeOutAckSessionInfo;
import relay.timeout.TimeOutSessionInfo;
import relay.timeout.TimeOutSessionRequest;
import server.ServerMessageReader;
import server.ServerSessionManager;
import server.StreamingServer;
import util.Logger;


/**
 * @author Leo Di Carlo
 *
 */
public class RelaySessionManager implements Observer{
	private SessionManagerStatus status; // Marco: lo stato attuale del relay
	private int numberOfSession; // Marco: il numero di sessioni aperte ( il numero di client che sta attualmente servendo)
	private Hashtable<String, Session> sessions; // tablella che contiene le info sulle sessioni: la chiave � l'Ip del cliente finale che ascolta la musica ( anche se mediato da un relay secondario) - per maggiori info guardare la classe Session
	/*
	 * nota: nei commenti si parla di indirizzo inferiore per indicare l'indirizzo che un relay ha sul suo cluster (LocalClusterAddress)
	 * 		e di indirizzo superiore per indicare l'indirizzo che un relay ha nel cluster di chi gli eroga il flusso ( big boss se relay, server se big boss)-> ClusterHeadAddress
	 * 
	 * Marco: composizione della tabella sessionInfo ( ancora non verificato, mi baso sui nomi che vengono assegnati)
	 * chiave: indirizzo del client
	 * valore[0]=  porta dalla quale il server eroga lo stream
	 * valore[1] = porta sulla quale il proxy riceve lo stream
	 * valore[2] = porta dalla quale il proxy eroga lo steam verso il client
	 * valore[3] = porta sulla quale il client riceve lo stream dal proxy
	 * valore[4] = porta di controllo del server
	 * valore[5] = porta di controllo del proxy
	 * nota hai bisogno di due porte di controllo perchè il proxy sta in mezzo, riceve un flusso dal server e ne eroga uno al client.,
	 * quindi hai due connessioni rtp e due porte di controllo, visto che c'è una porta di controllo associata ad ogni connessione rtp
	 */
	private DatagramPacket message;
	public static final RelaySessionManager INSTANCE = new RelaySessionManager(); // Marco: il relay è un singleton
	private String clientAddress; //Marco: variabile di appoggio che si usa per capire chi ha mandato un certo messaggio
	private String relayAddress; // Marco: è l'indirizzo locale al cluster dell'attuale relay;
	private String maxWnextRelay;  // Marco : è l'indirizzo del nuovo relay sul cluster 
	private String oldRelayLocalClusterAddress; // è l'indirizzo del vecchio relay all'interno del suo cluster.
	private String oldRelayClusterHeadAddress; // è l'indirizzo sulla rete superiore ( cluster del big boss / rete managed) dell'attuale relay
	private String connectedClusterHeadAddress; // è l'indirizzo di chi sta erogando il flusso al vecchio relay (big boss/server)
	
	
	//numero di ritrasmissioni sia per il messaggio REQUEST SESSION SIA PER IL MESSAGGIO SESSION INFO prima di adottare azioni di emergenza
	private int numberOfRetrasmissions = 1;
	
	private String event;
	private TimeOutSessionRequest toSessionRequest;  //Marco: questi qui sono i vari timeout
	private TimeOutAckSessionInfo toAckSessionInfo;
	private TimeOutSessionInfo toSessionInfo;
	
	/*
	 * Per la natura dei protocollifinora creati il sessionmanager non comunica mai dirttamente con la rete sovrastante:
	 * quando arrivano i messaggi di rielezione dal server o dal big boss, questi vengono intercettati dall'election manager,
	 * mentre sono i proxy che inviano al server i messaggi di forwardrequestfile e di redirect. ne segue che
	 * per la connessione bsta un RelayCm che invia invia e riceve messaggi sulla sola rete inferiore 
	 * ossia quella sulla quale il relay è il nodo id rierimento.
	 */
	private RelayCM sessionCM; // Marco: è chi si occupa di spedire  e ricevere i messaggi.
	private RelayMessageReader messageReader;
	private DebugConsole consolle;
	
	private RelayElectionManager electionManager;
	
	/**
	 * @param electionManager the electionManager to set
	 */
	public void setElectionManager(RelayElectionManager electionManager) {
		this.electionManager = electionManager;
	}

	// TODO costruttore
	public RelaySessionManager()
	{
		this.numberOfSession = 0;
		this.sessions= new Hashtable<String, Session>();
		
		this.sessionCM = RelayConnectionFactory.getSessionConnectionManager(this);
		this.messageReader = new RelayMessageReader();
		this.sessionCM.start();
		this.consolle = new DebugConsole();
		this.consolle.setTitle("RELAY SESSION MANAGER DEBUG CONSOLLE");
		
	}

	public static RelaySessionManager getInstance() {
		return RelaySessionManager.INSTANCE; // Marco: come già detto il relay session manager è un singleton.
	}


	

	
	
	

	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	// Marco: dovrebbe funzionare sostanzialmente così: il Session Manager si registra presso il receiver in merito alla ricezione
	//di certi messaggi. Quando questi arrivano il receiver chiama il metodo update del Session manager passandogli come argomento (arg)
	// il messaggio stesso, poi in base al messaggio si decide cosa fare.
	@Override
	public synchronized void update(Observable receiver, Object arg) {  //Marco: dovrebbe funzionare sostanzialmente così: 
		
		Proxy proxy;
		/**
		 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		 * +++++++++++++++++++MESSAGGI RICEVUTI ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		 */
		System.out.println("Tipo di Classe dell'arg: "+ arg.getClass());
		if(arg instanceof DatagramPacket)
		{
			/**
			 * un messaggio è appena arrivato e richiamo il reader per la lettura dello stesso
			 */
			this.message = (DatagramPacket) arg;
			try {
				messageReader.readContent(this.message);
			} catch (IOException e) {
				// Auto-generated catch block
				System.err.println("RELAY_SESSION_MANAGER: Errore nella lettura del Datagramma");
				Logger.write("RELAY_SESSION_MANAGER: Errore nella lettura del Datagramma");
				consolle.debugMessage("RELAY_SESSION_MANAGER: Errore nella lettura del Datagramma");
				e.printStackTrace();
			}

			/**
			 * arrivato messaggio di richiesta da parte del client
			 */
			
			/*Marco: è arrivata una richiesta da parte di un client ed io sono un relay secondario.
			 * creo un nuovo proxy che gestisce lo stream verso il client
			 * creo una nuova sessione che metto nella tabella delle sessioni con come indice l'indirizzo del client
			 * NOTA: la sessione � "incompleta" gli indirizzi delle porte ( settate dal costruttore a -1) verranno
			 * specificati dall'evento lanciato dal proxy quando quiesti ricever� il messaggio di ack request
			 * dal server/bigboss
			*/
			
			// TODO REQUEST_FILE && this.isRelay()
			
			if(this.messageReader.getCode() == MessageCodeConfiguration.REQUEST_FILE && this.isRelay())
			{
				this.status = SessionManagerStatus.Active;
				this.clientAddress = message.getAddress().getHostAddress();
				consolle.debugMessage("RELAY_SESSION_MANAGER: Arrivata la richiesta di "+messageReader.getFilename()+" da "+ this.clientAddress);
				proxy = new Proxy(this, true, messageReader.getFilename(), this.clientAddress, messageReader.getPortStreamingClient(),this.connectedClusterHeadAddress,false);
				Session sessione= new Session(this.clientAddress,proxy);

				sessions.put(this.clientAddress, sessione);
				this.numberOfSession++;
			}
			
			// TODO arrivo request session dal nuovo relay

			/**
			 * gestito l'arrivo della richiesta di passaggio della sessione da parte del nuovo relay appena eletto
			 */
			
			/* Marco:
			 * manda al nuovo relay i dati relativi a tutte le sessioni aperte rispondendo con il messaggio sessioninfo che non si vede
			 *  perchè si è incapsulato il tutto in un metodo stratico della classe Relay message factory
			 *  successivamente cambio lo stato in Attendingack session, ossia il successivo messaggio che il vecchio relay
			 *  deve ricevere nel protocollo di scambio sessione.
			 *  
			 *  Il messaggio SessionInfo contiene per ogni sessione: 
			 *  	l'indirizzo del client
			 *  	le porte di streaming ( per maggiori informazioni vedi la classe Session)
			 *  	l'eventuale indirizzo del relay secondario
			 *  
			 */
			
			
			// unico dubbio: serve proprio il controllo sullo stato del SessionManager?
			
			
			if(messageReader.getCode() == MessageCodeConfiguration.REQUEST_SESSION && this.status==SessionManagerStatus.AttendingRequestSession )
			{
				if(toSessionRequest!=null) // Marco: viene disattivato il timeout request session: il messaggio è arrivato
					toSessionRequest.cancelTimeOutSessionRequest();
				consolle.debugMessage("RELAY_SESSION_MANAGER: ricevuto SESSION_REQUEST dal nuovo RELAY");
				try {
					if(this.sessions != null || !this.sessions.isEmpty())
					{
						
						
						
						this.message = RelayMessageFactory.buildSessionInfo(0, sessions, InetAddress.getByName(this.maxWnextRelay), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
						
					}
					// se sessions � vuoto passo come parametro null: build sessioninfo creer� un pacchetto senza le indicazioni delle sessioni, tuttavia non dovrei finirci se non ho sessioni attive...
					else{this.message = RelayMessageFactory.buildSessionInfo(0, null, InetAddress.getByName(this.maxWnextRelay), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);}

					this.sessionCM.sendTo(this.message); //Marco: invio il messaggio preparato
					this.status = SessionManagerStatus.AttendingAckSession;
					this.toAckSessionInfo = RelayTimeoutFactory.getTimeOutAckSessionInfo(this, TimeOutConfiguration.TIMEOUT_ACK_SESSION_INFO);
				} catch (UnknownHostException e) {
					
					e.printStackTrace();
				} catch (IOException e) {
					
					e.printStackTrace();
				}
			}
			
			// TODO arrivo ACK_SESSION dal nuovo relay
			if(messageReader.getCode() == MessageCodeConfiguration.ACK_SESSION && this.status.equals(SessionManagerStatus.AttendingAckSession))
				
				/*
				 * il nuovo relay mi ha mandato il messaggio di ACK_SESSION: significa che il nuovo relay che già ottenuto i dati relativi
				 * alle sessioni: 
				 *  ridirigono il flusso sui recovery buffer dei proxy sul nuovo relay
				 *  Una volta svuotati tutti i buffer di tutti i proxy questi  mandano il messaggio di LEAVE a tutti i client serviti
				 */
			{
				if(this.toAckSessionInfo!=null)
					toAckSessionInfo.cancelTimeOutAckSessionInfo();
				consolle.debugMessage("RELAY_SESSION_MANAGER: Ricevuto ACK_SESSION dal nuovo RELAY");
				// Marco: il metyodo change proxy Session invoca il metodo Start handoff di ogni proxy che, in sostanza dovrebbe gestisce
				// la redirezione del flusso verso il nuovo relay ( per i dettagli guarda i commenti ai metodi).
				// il metodo getProxyInfo restituisce una tabella che ha per ogni chiave ( l'ip del client) la porta sulla
				//quale ridirigere il flusso.
				
				this.changeProxySession(messageReader.getProxyInfo()); 
			}
			
			// TODO arrivo messaggio SESSION_INFO da parte del vecchio relay
			if(messageReader.getCode() == MessageCodeConfiguration.SESSION_INFO && this.status.equals(SessionManagerStatus.RequestingSession))
			{
				
				/*
				 * fa parte del nuovo relay: arrivano i dati del messaggio Session Info 
				 * Marco: composizione della tabella sessionInfo ( ancora non verificato, mi baso sui nomi che vengono assegnati)
				 * chiave: indirizzo del client
				 * valore[0]=  porta dalla quale il server eroga lo stream
				 * valore[1] = porta sulla quale il proxy riceve lo stream
				 * valore[2] = porta dalla quale il proxy eroga lo steam verso il client
				 * valore[3] = porta sulla quale il client riceve lo stream dal proxy
				 * valore[4] = porta di controllo del server
				 * valore[5] = porta di controllo del relay
				 * nota hai bisogno di due porte di controllo perchè il proxy sta in mezzo, riceve un flusso dal server e ne eroga uno al client.,
				 * quindi hai due connessioni rtp e due porte di controllo, visto che c'è una porta di controllo associata ad ogni connessione rtp
				 *  sfruttando il metodo createProxyfromSession creo i nuovi proxy dai dati del messaggio SessionInfo 
				 *  
				 *
				 */
				
				/*
				 * Marco: successivamente mando il messaggio di ack session al vecchio relay
				 * sembra che a mandare il messaggio di REDIRECT al server perchè questi dirotti il flusso sul nuovo relay sia 
				 * ogni nuovo proxy relativamente alla stessa sessione
				 */
				 
				
				
				this.toSessionInfo.cancelTimeOutSessionInfo();
				consolle.debugMessage("RELAY_SESSION_MANAGER: Ricevuto SESSION_INFO dal vecchio RELAY");
				this.sessions = messageReader.getSessions(); // creo le sessioni ggio dal messa attualmente per� non ci sono i proxy, li metto con il 
				//metodo
				String proxyInfo = this.createProxyFromSession(sessions); // Nota: proxyInfo contiene le porte di recovery dei relay sostituivi sulle quali ridirigere le varie sessioni
				try {
					this.message = RelayMessageFactory.buildAckSession(0, proxyInfo, InetAddress.getByName(this.relayAddress), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
					this.sessionCM.sendTo(this.message);
					this.status = SessionManagerStatus.Active;
				} catch (UnknownHostException e) {
					
					e.printStackTrace();
				} catch (IOException e) {
					
					e.printStackTrace();
				}
			}

		}
		/**
		 * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		 * ++++++++++++++++++++++++++++++EVENTI ARRIVATI -- STRINGHE ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		 * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		 */
		if(arg instanceof String)
		{
			this.event = (String)arg;
			
			// TODO "end of media" un proxy mi comunica che ha finito
			if(this.event.equals("End_Of_Media") && status.equals(SessionManagerStatus.Active) && (this.isRelay()||this.isBigBoss()))
			{
				/*
				 * fine di una canzone  
				 * tolgo le informazioni relative alla sessione terminata dalle relative tabelle 
				 * (sia quella sessione- proxy che quella sessione--sessioninfo)
				 * se non mi restano altre sessioni attive ( le tabelle sono quindi vuote) metto il Relay in stato di Idle
				 */
				consolle.debugMessage("RELAY_SESSION_MANAGER: Evento di END_OF_MEDIA da parte di un proxy");
				proxy = (Proxy)receiver; // l'evento mi � comunicato dal proxy, tramite il receiver ottengo un riferimento a lui.
				/*
				 *
				//rimuovo i riferimenti della sessione all'interno delle relative hashtable 
				// c'� una sola sessione per ogni proxy. trovo la sesione corrispondente al proxy e l'elimino
				 * a differenza della versione precendenet non posso usare il ClientAddress del proxy: potrebbe
				 * anche essere l'indirizzo del relay secondario al quale eroga lo streaming: 
				 * un proxy difatti non sa se sta erogando il flusso ad un client o ad un relay secondario
				 * 
				*/
				Enumeration<String> keys = sessions.keys();
				while(keys.hasMoreElements()){
					String chiave = keys.nextElement();
					Session sessione = sessions.get(chiave);
					if ( sessione.getProxy().equals(proxy)){
						sessions.remove(chiave);
						// in teoria qui ci andrebbe un break, non ha senso ciclare per tutti gli altri elementi
						// per stare dalla parte dei bottoni lascio tutto cos�.
					}
				}
				
				this.numberOfSession--;
				if(numberOfSession == 0)
					this.status = SessionManagerStatus.Idle;
			}
			
			/**
			 * l'electionmanager mi ha comunica chi è il relay attuale a seguito di un messaggio di WHO_IS_RELAY
			 */
			
			// TODO "relay found" l'election manager comunica chi � il relay attuale
			if(this.event.contains("RELAY_FOUND"))
			{
				StringTokenizer st = new StringTokenizer(this.event, ":");
				st.nextToken();
				this.relayAddress = st.nextToken();
				consolle.debugMessage("RELAY_SESSION_MANAGER: Evento di RELAY_FOUND, il RELAY attuale è: "+this.relayAddress);
			}
			
			//TODO il nuovo relay non ha risposto, ne provo ad eleggere un altro
			if(this.event.equals("TIMEOUTSESSIONREQUEST") || this.event.equals("TIMEOUTACKSESSIONINFO")) //VECCHIO RELAY: il nuovo relay non ha risposto
			{
				if(this.event.equals("TIMEOUTSESSIONREQUEST") && status.equals(SessionManagerStatus.Active))
					consolle.debugMessage("RELAY_SESSION_MANAGER: Scattato il TIMEOUT_SESSION_REQUEST");
				if(this.event.equals("TIMEOUTACKSESSIONINFO") && status.equals(SessionManagerStatus.AttendingAckSession))
					consolle.debugMessage("RELAY_SESSION_MANAGER: Scattato il TIMEOUT_ACK_SESSION_INFO");
				
				/*
				 * Marco: in sostanza, se il nuovo relay non risponde, cerco di nominare il secondo
				 * classificato alle elezioni
				 * 
				 */
				if(this.electionManager!=null)
				{
					electionManager.chooseAnotherRelay();
				}

			}
			
			/**
			 * l'electionmanager mi comunica chi è il relay appena eletto
			 */
			/* Nella vecchia versione era cos�
			 *se  io sono il relay attualmente attivo salvo il nome di chi ha vinto nella variabile maxWnextRelay
			 *		cambio stato indicando che non sono più io il relay di riferimento della rete 
			 *		e faccio partire il timeout mentre attendo il messaggio di REQUEST SESSION da parte dell'altro relay
			 *
			 *se invece non sono il relay attualmente attivo confronto l'indirizzo del vincitore con il mio: se ho vinto
			 *	cambio il mio stato per indicare che sono il relay attivo, 
			 *	mando il messaggio di Session Request al vecchiorelay 
			 *	e mi metto in attesa delle informazioni sulle sessioni
			 *
			 */
			// ******E' UN GRAN CASINO! RIPENSA TUTTO CON CALMA!*****//
			// TODO "NEW_RELAY " l'elctionManager mi comunica chi ha vinto l'elezione
			if(this.event.contains("NEW_RELAY")) 
				/*
				 * NOTA: indirizzo inferiore = indirizzo all'interno del cluster"inferiore"
				 * 		indirizzo superiore = indirizzo all'interno del cluster "superiore"
				 * 
				 * Marco: l'election manager quando c'� un election done scatena un evento che mi fa ricevere la stringa 
				 * NEW_RELAY:ip del vincitore dell'elezione:ip del vecchio relay sul suo cluster:ip del vecchio relay sul cluster superiore:ip del nodo che eroga ilflusso al vecchio relay( e che lo erogherò anche al nuovo)
				 * 
				 * Cosa fare dipende dal ruolo che il nodo ha in quel momento:
				 * vecchio relay, vicitore dell'elezione, relay secondario che  o candidato che non ha vinto.
				 * 
				 * In ogni modo devo aggiornare le variabili.
				 * 
				 * Se l'idirizzo del vecchio relay � uguale al suo ( � il vecchio relay)
				 * si mette in attesa del request session da parte del nuovo relay
				 * 
				 * registra nella variabile maxWnexRelay l'idirizzo locale del suo successore 
				 * 
				 * Se il nodo � un non � attivo controlla l'indirizzo del vincitore
					Se � pari al suo indirizzo sul cluster locale
					. Manda al vecchio relay il messaggio di 
						Request Session
						
					Se � un relay secondario e l'indirizzo del cluster locale del vincitore � uguale a quello del nodo 
					da cui riceveva il flusso ( � stato rieletto il Big Boss)
						
						Il relay si comporta �da client�: informa tutti I suoi proxy dell'avvenuta rielezione affinch� questi salvino 
						l'indirizzo del nuovo bigBoss, per poi considerarlo come nuova fonte di flusso quando il proxy sul vecchio relay 
						che mandava il flusso invier� il messaggio di leave.
					
					Altrimenti ( sono un relay inattivo e non ho vinto) 
					aggiorno gli indirizzi e basta...
				 * 
				 * 
				 */
			{
				StringTokenizer st = new StringTokenizer(this.event, ":");
				st.nextToken();
				String newRelay = st.nextToken(); // Marco: estraggo l'indirizzo del nuovo relay
				consolle.debugMessage("RELAY_SESSION_MANAGER: Evento di NEW_RELAY, il nuovo RELAY è "+newRelay);
				System.out.println("RELAY_SESSION_MANAGER: Evento di NEW_RELAY, il nuovo RELAY è "+newRelay);
				// TODO aggiungi le scritte di debug sull'acquisizione dei nuovi campi
				 // estaggo l'indirizzo di chi eroga il flusso al vecchio relay.
				String vecchioRelay = st.nextToken();
				String indsupVecchioRelay = st.nextToken();
				
				String indFornitoreFlussiVecchioRelay=st.nextToken();
				
				
				//io sono un relay seconario ed � stato rieletto il big boss, devo avvertire i Proxy.
				//nota. è imposibile che un relay secondario con delle sessioni attive diventi big boss.
				//possono diventarlo solo nodi senza sessioni attive, 
				if(this.connectedClusterHeadAddress.equals(vecchioRelay)&&this.isRelay()){
					// posso gi� permettermi di sostituire il riferimento al big boss all'interno di sessionManager 
					// perchè so che ci pu� essere una sola rielezione in corso alla volta, si interagirebbe con il 
					// big boss solo se nel caso di una rielezione di questo proxy secondario, che
					// per� non pu� aver luogo prima che la rielezione del big boss sia finita.
					this.connectedClusterHeadAddress=newRelay;
					
					/*
					 * setto il parametro futureStreamingServer di tutti i proxy, in modo che
					 * quando riceveranno il messaggio di LEAVE dal proxy sul vecchio big boss
					 * che erogava loro il flusso settino come loro nuovo mittente il nuovo 
					 * proxy sul nuovo big boss (che eroga il flusso dalla stessa porta)
					 */
					
					Enumeration<String> chiavi = sessions.keys();
					
					while (chiavi.hasMoreElements()){
						Session sessione = sessions.get(chiavi.nextElement());
						sessione.getProxy().setFutureStreamingAddress(newRelay);
					}
					
					
					
				}
					
				else{
					// il nodo � il vecchio relay
					
					if (this.getLocalClusterAddress().equals(oldRelayLocalClusterAddress)){
						this.maxWnextRelay = newRelay;
						this.status= SessionManagerStatus.AttendingRequestSession;
						this.toSessionRequest = RelayTimeoutFactory.getTimeOutSessionRequest(this, TimeOutConfiguration.TIMEOUT_SESSION_REQUEST);
	
					}
					else{
						// il nodo � il vincitore!
						if(this.getLocalClusterAddress().equals(newRelay)){
							this.message = RelayMessageFactory.buildRequestSession(0, InetAddress.getByName(oldRelayLocalClusterAddress), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
							this.sessionCM.sendTo(message);
							this.toSessionInfo = RelayTimeoutFactory.getTimeOutSessionInfo(this, TimeOutConfiguration.TIMEOUT_SESSION_INFO);
							this.status = SessionManagerStatus.RequestingSession;
							this.oldRelayLocalClusterAddress=vecchioRelay;
							this.oldRelayClusterHeadAddress=indsupVecchioRelay;
							this.connectedClusterHeadAddress=indFornitoreFlussiVecchioRelay;
						}
						else{
							// in questo caso ci finisco se sono un relay che non ha vinto e se non sono un relay secondario con dei flussi
							// attivi provenienti dal vecchio big boss, mi limito a registrare i dati della rielezione, ma non li user� neanche
							this.relayAddress= newRelay;
							this.oldRelayClusterHeadAddress= vecchioRelay;
							this.oldRelayClusterHeadAddress= indsupVecchioRelay;
							this.connectedClusterHeadAddress=indFornitoreFlussiVecchioRelay;
							
						}
					}
				}
				
				

			
			} // Fine evento new Relay
			
			// TODO nuovo relay eletto tramite elezione di emergenza, commentato, per ora non abbiamo elezioni di emergenza
//			if(this.event.contains("NEW_EM_RELAY"))
//			{
//				consolle.debugMessage("RELAY_SESSION_MANAGER: Evento di NEW_EM_RELAY");
//				StringTokenizer st = new StringTokenizer(this.event, ":");
//				st.nextToken();
//				String newRelay = st.nextToken();
//				try {
//					if(InetAddress.getLocalHost().toString().equals(newRelay))
//					{
//						this.imRelay = true;
//						this.status = SessionManagerStatus.Active;
//					}
//					else
//					{
//						this.relayAddress = newRelay;
//						this.status = SessionManagerStatus.Waiting;
//					}
//				} catch (UnknownHostException e) {
//					
//					e.printStackTrace();
//				}
//			}
			
			//TODO scattato il timeout su SessionInfo , ma posso ancora provare a ritrasmettere
			
			if(this.event.equals("TIMEOUTSESSIONINFO") && this.numberOfRetrasmissions != 0 && this.status.equals(SessionManagerStatus.RequestingSession)) // Nuovo Realy: è scattato il timout sull'attesa di SessionInfo
			{
				/*
				 * Marco: sono il nuovo relay: è scattato il timeout sulla ricezione del messaggio SessionInfo.
				 * il parametro numberOf ritrasmissions idica quante volte posso chiedere la ritrasmissione del messaggio Sessioninfo
				 * fortunatamente qui posso ritrasmetterlo diminuendo il numero di ritrasmissioni rimaste
				 */
				consolle.debugMessage("RELAY_SESSION_MANAGER: Scattato il TIMEOUT_SESSION_INFO");
				try {
					this.message = RelayMessageFactory.buildRequestSession(0, InetAddress.getByName(this.relayAddress), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
					this.sessionCM.sendTo(message);
					this.toSessionInfo = RelayTimeoutFactory.getTimeOutSessionInfo(this, TimeOutConfiguration.TIMEOUT_SESSION_INFO);
					this.numberOfRetrasmissions--;
				} catch (IOException e) {
					
					e.printStackTrace();
				}

			}
			
			// TODO scattato il timeut sul sessioninfo e non ho pi� ritrasmissioni; invalido tutto
			if(this.event.equals("TIMEOUTSESSIONINFO") && this.numberOfRetrasmissions == 0)
			{
				/*updateSessions
				 * Marco: sono il nuovo relay: è scattato il timeout sulla ricezione del messaggio SessionInfo.
				 * il parametro numberOf ritrasmissions idica quante volte posso chiedere la ritrasmissione del messaggio Sessioninfo
				 * sfortunatamente non posso ritrasmetterlo: non mi resta che mandare IN BROADCAST
				 *  ai client il messaggio di invalidare le sessioni attive, ipotizzo quindi che il vecchio relay non ci sia più
				 */
				consolle.debugMessage("RELAY_SESSION_MANAGER: Scattato il TIMEOUT_SESSION_INFO e numero di ritrasmissioni a 0");
				this.status = SessionManagerStatus.Waiting;
				//Invio il messaggio di invalidazione della sessione ai client che conoscono l'identità del nuovo relay ma non si ha modo di recuperare la sessione
				try {
					this.message = RelayMessageFactory.buildSessionInvalidation(0, InetAddress.getByName(NetConfiguration.RELAY_CLUSTER_BROADCAST_ADDRESS), PortConfiguration.CLIENT_PORT_SESSION_IN);
					this.sessionCM.sendTo(this.message);
					this.status = SessionManagerStatus.Active;
				} catch (UnknownHostException e) {
					
					e.printStackTrace();
				} catch (IOException e) {
					
					e.printStackTrace();
				}
			}
			// TODO ELECTION_REQUEST_RECEIVED si inizia la fase di rielezione
			if(this.event.equals("ELECTION_REQUEST_RECEIVED")&&isRelay())// Marco aggiungi condizione sono il relay secondario e non il big boss
			{
				/*
				 * Marco:entri in questo blocco quando parte la fase di rielezione del BigBoss e il nodo � un relay secondario
				 * servito dal big boss.
				 * Qui il relay secondario deve ingrandire i buffer (alzando la soglia superiore) dei propri proxy, analogamente
				 * a quello che fanno i clients.
				 * Nota il nodo che lancia l'electionRequest non riceve questo messaggio, sei quindi tutelato nel caso
				 * sia il relay secondario ad indire la rielezione per il suo cluster.
				 * 
				 * Comunico ai proxy di ingrandire i propri buffers.
				 */
				Enumeration <String> chiavi;
				while(chiavi.hasMoreElements()){
					(sessions.get(chiavi.nextElement()).getProxy()).enlargeNormalBuffer();
				}
				
				
				
				
			}
			
		}
		
		// TODO : il proxy, dopo aver creato la sessione comunica al sessionManager le porte che usa
		/**
		 * il proxy dopo aver creato la sessione ha il compito di avvertire il sessionmanager
		 * ++++++++++++++++++++++++++EVENTO SOLLEVATO DAI PROXY++++++++++++++++++++++++++++++
		 */
		if(arg instanceof int[])
		{
			
			/*
			 * Il proxy creato ex-novo, comunca al sessionManager le porte che
			 * usa. Il session Manager inserisce le porte nella tabella session.
			 * trovo il record grazie al fatto che c'� un'unico proxy per sessione;
			 * non posso usare l'indirizzo del client come si poteva fare nella vecchia versione
			 * perch� il proxy pu� erogare anche ad un relay secondario, e quindi 
			 * posso avere pi� proxy con lo stesso client address
			 */
			//CONTROLLARE SE il seguente cast è giusto altrimenti si cambia...
			System.out.println("Il Proxy mi ha notificato i parametri di sessione...");
			
			int[] sessionPorts = (int[])arg;
			System.err.println("RelaySessionManager- Porta di Stream del server: " +sessionPorts[0]);
			System.err.println("RelaySessionManager- Porta di Stream del relay IN: " +sessionPorts[1]);
			System.err.println("RelaySessionManager- Porta di Stream del relay OUT: " +sessionPorts[2]);
			System.err.println("RelaySessionManager- Porta di Stream del Client IN: " +sessionPorts[3]);
			System.err.println("RelaySessionManager- Porta di Stream del Server control: " +sessionPorts[4]);
			System.err.println("RelaySessionManager- Porta di Stream del Proxy control: " +sessionPorts[5]);
			Enumeration<String> keys = sessions.keys();
			while(keys.hasMoreElements()){
				String chiave = keys.nextElement();
				Session sessione = sessions.get(chiave);
				if ( sessione.getProxy().equals(proxy)){
					sessione.setSessionInfo(sessionPorts);
					// in teoria qui ci andrebbe un break, non ha senso ciclare per tutti gli altri elementi
					// per stare dalla parte dei bottoni lascio tutto cos�.
					// ps ho controllato effettivamente va bene, l'equals è true solo se si tratta della 
					// stessa istanza del proxy
				}
			}
			
			
			System.out.println("AGGIUNTO IL RIFERIMENTO IN SESSION_INFO");
		}
	}


	/**
	 * @return the maxWnextRelay
	 */
	public String getMaxWnextRelay() {
		return maxWnextRelay;
	}

	/**
	 * @param maxWnextRelay the maxWnextRelay to set
	 */
	public void setMaxWnextRelay(String maxWnextRelay) {
		this.maxWnextRelay = maxWnextRelay;
	}
	

	public void updateSessions()
	{
		if(this.numberOfSession != 0)
		{
			numberOfSession--;
			if(this.numberOfSession == 0)
				this.status = SessionManagerStatus.Idle;
		}
	}
	
	/**
	 * Metdo chiamato dal nuovo proxy quando gli arriva il messaggio SESSION_INFO per creare
	 * i proxy
	 * Crea i proxy dalla tabella di sessioni ottenuta dal messaggio di Sessioninfo,
	 *  inserisce i proxy nelle sessioni e restituisceun'altra tabella che indica
	 * per ogni sessione le porte di recovery sulle quali il vecchio relay deve ridirigere il flusso,
	 * inoltre riempie i campi proxy delle sessioni presenti nella tabella con i proxy appena creati
	 * @param sessions � una tabella che contiene i dati come tabella di Session, la chiave � l'indirizzo del client
	 * 
	 * @return una stringona che contiene per ogni sessione l'indirizzo ip del client ( la chiave della sessione) e la porta di recovery del 
	 * proxy che gestisce quella sessione
	 * es (192.168.1.1_5000_162.168.1.2_6000)
	 */
	private String createProxyFromSession(Hashtable sessions) 
	{
		/*
		 * 
		 * 
		 * Marco: composizione della tabella sessionInfo (
		 * valore[0]=  porta dalla quale il server eroga lo stream
		 * valore[1] = porta sulla quale il proxy riceve lo stream
		 * valore[2] = porta dalla quale il proxy eroga lo steam verso il client
		 * valore[3] = porta sulla quale il client riceve lo stream dal proxy
		 * valore[4] = porta di controllo del server
		 * valore[5] = porta di controllo del relay
		 * nota hai bisogno di due porte di controllo perchè il proxy sta in mezzo, riceve un flusso dal server e ne eroga uno al client.,
		 * quindi hai due connessioni rtp e due porte di controllo, visto che c'è una porta di controllo associata ad ogni connessione rtp
		 */
		Proxy proxy;
		String chiave;
		String recStreamInports = "";
		int serverPortStreamOut = 0;
		int proxyPortStreamIn = 0;
		int proxyPortStreamOut = 0;
		int clientPortStreamIn = 0;
		int serverCtrlPort = 0;
		int proxyCtrlPort = 0;
		if(!sessions.isEmpty())
		{
			Enumeration keys = sessions.keys();
			while(keys.hasMoreElements())
			{
				chiave = keys.nextElement().toString();
				Session session = (Session)sessions.get(chiave);
				int[] values =session.getSessionInfo();
				if(values.length == 6)
				{
					serverPortStreamOut = values[0];
					proxyPortStreamIn = values[1];
					//Notifica al portmapper di occupare le porte in maniera sia garantita la loro coerenza
					RelayPortMapper.getInstance().setRangePortInRTPProxy(proxyPortStreamIn);
					proxyPortStreamOut = values[2];
					//Notifica al portmapper di occupare le porte in maniera sia garantita la loro coerenza
					RelayPortMapper.getInstance().setRangePortOutRTPProxy(proxyPortStreamOut);
					clientPortStreamIn = values[3];
					serverCtrlPort = values[4];
					proxyCtrlPort = values[5];
				}
				try {
					/*
					 * nel costruttore del proxy è stato inserito il valore di proxy PortStreamOut in 2 punti diversi proprio perchè il vecchio proxy quando avvia
					 * la trasmissione di recovery verso il nuovo riutilizza la medesima porta.
					 * Marco: quindi il numero di porta sulla quale il proxy sul nuovo relay riceve il flusso da inserire nel suo recovery buffer è lo stesso
					 *  numero della porta dalla quale sia il vecchio che il nuovo relay erogano il flusso.
					 * creo il proxy e  metto l'istanza nella sessione
					 * 
					 * determino il flag servigClient del relay che gli indica se sto servendo direttamente un client o meno nel seguente modo:
					 * 	sto servendo un relay secondario ( flag a false) solo se la sessione è mediata e io sono un big boss
					 * 
					 */
					boolean servingClient;
					if (this.isBigBoss()&& session.isMediata())
						servingClient=false;
					else
						servingClient=true;
					
					
					proxy = new Proxy(this, false, chiave, clientPortStreamIn, proxyPortStreamOut, proxyPortStreamIn, serverPortStreamOut, proxyPortStreamOut,this.connectedClusterHeadAddress ,InetAddress.getByName(this.oldRelayLocalClusterAddress), serverCtrlPort, proxyCtrlPort, servingClient);
					session.setProxy(proxy);
					recStreamInports = recStreamInports+"_"+chiave+"_"+proxy.getRecoveryStreamInPort();
				} catch (UnknownHostException e) {
					
					e.printStackTrace();
				}
			}// fine while	
		}
	//	recStreamInports.replaceFirst("_", "");
	//	recStreamInports = recStreamInports.substring(arg0, arg1)
		return recStreamInports;
	}
	
	/**
	 * 
	 * Metodo che permette di ridirigere il flusso verso il nuovo relay.
	 * 
	 * @param sessionEndpoint tabella contenente, per ogni sessione la porta del proxy sul nuovo relay sulla quale ridirigere
	 * il flusso
	 */
	
	private void changeProxySession(Hashtable sessionEndpoint)
	{
		/*Marco:
		 * le tuple della tabella session Endpoint contengono come chiave l'indirizzo del client ( come id di sessione)
		 * e come secondo argomento un vettore di interi con un solo elemento:
		 *  la porta sulla quale il nuovo proxy attende il flusso rtp dal vecchio proxy
		 * quindi per ogni sessione si invoca il metodo handoff del relativo roxy che, appunto ridirige il flusso sull'indirizzo e sulla
		 * porta indicata.
		 * inoltre setta il flag ending per ongi proxy
		 * 
		 */
		String chiave;
		if(!sessionEndpoint.isEmpty())
		{
			if(!sessions.isEmpty())
			{
				Enumeration keys = sessionEndpoint.keys();
				while(keys.hasMoreElements())
				{
					chiave = keys.nextElement().toString();
					int[] values =(int[]) sessionEndpoint.get(chiave);
					try {
						Proxy proxy = sessions.get(chiave).getProxy();
						proxy.startHandoff(values[0], InetAddress.getByName(this.maxWnextRelay));
						proxy.setEnding(true);
					} catch (UnknownHostException e) {
						
						e.printStackTrace();
					}
				}
			}
		}
	}


/**
 * 
 * @return l'indirizzo in stringa che il relay ha all'interno del suo cluster
 */

public String getLocalClusterAddress(){
	return electionManager.getLocalClusterAddress();
}

/**
 * 
 * @return l'indirizzo in stringa che il relay ha all'interno del cluster superiore
 */

public String getLocalClusterHeadAddress(){
	return electionManager.getLocalClusterHeadAddress();
}

/**
 * 
 * @return l'indirizzo in stringa del big boss se il relay � un relay secondario, o l'indirizzo del server se � il big boss
 */
public String getConnectedClusterHeadAddress(){
	return this.getConnectedClusterHeadAddress();
}

/**
 * 
 * @return l'indirizzo in forma INET che il relay ha all'interno del suo cluster
 */

public InetAddress getLocalClusterInetAddress(){
	return electionManager.getLocalClusterInetAddress();
}

/**
 * 
 * @return l'indirizzo in forma INET che il relay ha all'interno del cluster superiore
 */

public InetAddress getLocalClusterHeadInetAddress(){
	return electionManager.getLocalClusterHeadInetAddress();
}

/**
 * 
 * @return l'indirizzo in forma INET del big boss se il relay � un relay secondario, o l'indirizzo del server se � il big boss
 */
public InetAddress getConnectedClusterHeadInetAddress(){
	return InetAddress.getByName(this.connectedClusterHeadAddress);
}

/**
 * 
 * @return true se il nodo è un relay secondario.
 */

public boolean isRelay(){
	return electionManager.isRELAY();
	
}

/**
 * 
 * @return true se il nodo è il big boss attuale
 */

public boolean isBigBoss(){
	return electionManager.isBIGBOSS();
}


//////////////////////////////////////////////////////////////////////////////////////
//...........................METODI CREATI PER I TEST..............................\\
////////////////////////////////////////////////////////////////////////////////////


public void tAddSession(Session session){
	this.sessions.put(session.getId(), session);
	consolle.debugMessage(1, "Sessionmanager: forzato l'inserimento della sessione "+session );
}	

public void tRemoveSession(Session session){
	this.sessions.remove(session.getId());
	consolle.debugMessage(2, "Sessionmanager: forzato la rimozione della sessione "+session );
}




}

enum SessionManagerStatus{
	Idle,  // Stato del relay quando è attivo 8 è il relay di riferimento per la rete, ma non sta attualmente servendo alcun client.
	Waiting,  // Marco: Stato tipico di un possiblie relay che non è il relay di riferimento. Sta a far niente, pronto a candidarsi se scatta la fase di elezione ( anche se la fase di elezione non si vede nel sessionManager, ma  nell'ElectionManager
	Active, // Marco: stato del relay quando è attivo e sta erogando dei flussi verso i client.
	AttendingAckSession, // Marco: un vecchio relay è in questo stato quando ha inviato il messaggio di SessionInfo, contente i dati relativi alle sessioni aperte di cui fare l'handoff ed attende l'ack_ session da parte del nuovo relay, con l'indicazione delle porte dei proxy sul nuovo relay sulle quali ridirigere lo stream 
	RequestingSession, // Marco: stato in cui si trova il nuovo relay dopo aver mandato il messaggio di REQUEST SESSION in attesa del SESSION INFO
	AttendingRequestSession // Marco: un vecchio relay si trova in questo stato quando attende il request session dal suo sostituto.
	
}

