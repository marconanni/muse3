package relay;

/**
 * 
 * @author Marco Nanni
 * 
 * Classe che contiene le informazioni sulle sessioni attive nel relay.
 * guardare le note sugli attributi per maggiori informanzioni
 * le porte sono settabili in un colpo solo tramite un vettore di interi
 * se non specificato il parametro relaySecondario viene posto a null
 *
 */

public class Session {
	
	private String id;						//( l�indirizzo Ip del client che si serve, magari anche grazie alla mediazione di un relay secondario)
	private Proxy proxy;					//riferimento al Proxy che gestisce lo streaming
	private String relaySecondario;			//Una stringa che contiene l�indirizzo dell�eventuale relay secondario che ottiene lo streamig dal big boss per poi ridirigerlo verso un suo client
	private int senderStreamPort;			 //porta dalla quale il server eroga lo stream
	private int inStreamPort;				//porta sulla quale il proxy riceve lo stream
	private int outStreamPort;				//porta dalla quale il proxy eroga lo steam verso il client
	private int receiverStreamPort;			//porta sulla quale il client riceve lo stream dal proxy
	private int streamingServerCtrlPort;	//porta di controllo del server
	private int proxyStreamingCtrlPort;		//porta di controllo del proxy
	
	/****getters --- setters*****/
	
	// TODO aggiungi documentazione su cosa ritornano
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Proxy getProxy() {
		return proxy;
	}
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}
	public String getRelaySecondario() {
		return relaySecondario;
	}
	public void setRelaySecondario(String relaySecondario) {
		this.relaySecondario = relaySecondario;
	}
	public int getSenderStreamPort() {
		return senderStreamPort;
	}
	public void setSenderStreamPort(int senderStreamPort) {
		this.senderStreamPort = senderStreamPort;
	}
	public int getInStreamPort() {
		return inStreamPort;
	}
	public void setInStreamPort(int inStreamPort) {
		this.inStreamPort = inStreamPort;
	}
	public int getOutStreamPort() {
		return outStreamPort;
	}
	public void setOutStreamPort(int outStreamPort) {
		this.outStreamPort = outStreamPort;
	}
	public int getReceiverStreamPort() {
		return receiverStreamPort;
	}
	public void setReceiverStreamPort(int receiverStreamPort) {
		this.receiverStreamPort = receiverStreamPort;
	}
	public int getStreamingServerCtrlPort() {
		return streamingServerCtrlPort;
	}
	public void setStreamingServerCtrlPort(int streamingServerCtrlPort) {
		this.streamingServerCtrlPort = streamingServerCtrlPort;
	}
	public int getProxyStreamingCtrlPort() {
		return proxyStreamingCtrlPort;
	}
	public void setProxyStreamingCtrlPort(int proxyStreamingCtrlPort) {
		this.proxyStreamingCtrlPort = proxyStreamingCtrlPort;
	}
	

	
	/**
	 * 
	 * @param id	 l�indirizzo Ip del client che si serve, magari anche grazie alla mediazione di un relay secondario)
	* @param proxy riferimento al Proxy che gestisce lo streaming
	* @param relaySecondario Una stringa che contiene l�indirizzo dell�eventuale relay secondario che ottiene lo streamig dal big boss per poi ridirigerlo verso un suo client
	* @param serverStreamPort porta dalla quale il server eroga lo stream
	* @param inStreamPort porta sulla quale il proxy riceve lo stream
	* @param outStreamPort porta dalla quale il proxy eroga lo steam verso il client
	* @param clientStreamPort porta sulla quale il client riceve lo stream dal proxy
	* @param streamingServerCtrlPort porta di controllo del server
	* @param int proxyStreamingCtrlPort porta di controllo del proxy
	 * 
	 */
	
	public Session(String id, Proxy proxy, String relaySecondario,
			int serverStreamPort, int inStreamPort, int outStreamPort,
			int clientStreamPort, int streamingServerCtrlPort,
			int proxyStreamingCtrlPort) {
		super();
		this.id = id;
		this.proxy = proxy;
		this.relaySecondario = relaySecondario;
		this.senderStreamPort = serverStreamPort;
		this.inStreamPort = inStreamPort;
		this.outStreamPort = outStreamPort;
		this.receiverStreamPort = clientStreamPort;
		this.streamingServerCtrlPort = streamingServerCtrlPort;
		this.proxyStreamingCtrlPort = proxyStreamingCtrlPort;
	}
	
	/**
	 * costruttore senza relay indicazione del relay secondario ( il parametro viene messo a null)
	 * @param id	 l�indirizzo Ip del client che si serve, magari anche grazie alla mediazione di un relay secondario)
	* @param proxy riferimento al Proxy che gestisce lo streaming
	* @param serverStreamPort porta dalla quale il server eroga lo stream
	* @param inStreamPort porta sulla quale il proxy riceve lo stream
	* @param outStreamPort porta dalla quale il proxy eroga lo steam verso il client
	* @param clientStreamPort porta sulla quale il client riceve lo stream dal proxy
	* @param streamingServerCtrlPort porta di controllo del server
	* @param int proxyStreamingCtrlPort porta di controllo del proxy
	 * 
	 */
	
	public Session(String id, Proxy proxy, 
			int serverStreamPort, int inStreamPort, int outStreamPort,
			int clientStreamPort, int streamingServerCtrlPort,
			int proxyStreamingCtrlPort) {
		super();
		this.id = id;
		this.proxy = proxy;
		this.relaySecondario = null;
		this.senderStreamPort = serverStreamPort;
		this.inStreamPort = inStreamPort;
		this.outStreamPort = outStreamPort;
		this.receiverStreamPort = clientStreamPort;
		this.streamingServerCtrlPort = streamingServerCtrlPort;
		this.proxyStreamingCtrlPort = proxyStreamingCtrlPort;
	}
	
	/**
	 * 
	 * @param id l�indirizzo Ip del client che si serve, magari anche grazie alla mediazione di un relay secondario)
	 * @param proxy  riferimento al Proxy che gestisce lo streaming
	 * @param relaySecondario Una stringa che contiene l�indirizzo dell�eventuale relay secondario che ottiene lo streamig dal big boss per poi ridirigerlo verso un suo client
	 * @param sessionInfo i dati sulle porte utilizzate per questa sessione
	 */
	
	public Session(String id, Proxy proxy, String relaySecondario,int[]sessionInfo) {
		super();
		this.id = id;
		this.proxy = proxy;
		this.relaySecondario = relaySecondario;
		this.senderStreamPort = sessionInfo[0];
		this.inStreamPort = sessionInfo[1];
		this.outStreamPort = sessionInfo[2];
		this.receiverStreamPort = sessionInfo[3];
		this.streamingServerCtrlPort = sessionInfo[4];
		this.proxyStreamingCtrlPort = sessionInfo[5];
	}
	
	/**
	 * versione per la costruzione senza relay secondario e vettore sessioninfo
	 * 
	 * @param id l�indirizzo Ip del client che si serve, magari anche grazie alla mediazione di un relay secondario)
	 * @param proxy  riferimento al Proxy che gestisce lo streaming
	 * @param sessionInfo i dati sulle porte utilizzate per questa sessione
	 */
	public Session(String id, Proxy proxy, int[]sessionInfo) {
		super();
		this.id = id;
		this.proxy = proxy;
		this.relaySecondario = null;
		this.senderStreamPort = sessionInfo[0];
		this.inStreamPort = sessionInfo[1];
		this.outStreamPort = sessionInfo[2];
		this.receiverStreamPort = sessionInfo[3];
		this.streamingServerCtrlPort = sessionInfo[4];
		this.proxyStreamingCtrlPort = sessionInfo[5];
	}
	
	/**
	 * Costruttore che non inizializza le porte, le imposta a -1, successivamente andranno
	 * impostate o singolaremente o tutte insieme tramite il metodo setSessionInfo
	 * 
	 * 
	 * @param id l�indirizzo Ip del client che si serve, magari anche grazie alla mediazione di un relay secondario)
	 * @param proxy  riferimento al Proxy che gestisce lo streaming
	 * @param relaySecondario Una stringa che contiene l�indirizzo dell�eventuale relay secondario che ottiene lo streamig dal big boss per poi ridirigerlo verso un suo client
	 */
	public Session(String id, Proxy proxy, String relaySecondario) 
	{
		super();
		this.id = id;
		this.proxy = proxy;
		this.relaySecondario = relaySecondario;
		this.senderStreamPort = -1;
		this.inStreamPort = -1;
		this.outStreamPort = -1;
		this.receiverStreamPort = -1;
		this.streamingServerCtrlPort = -1;
		this.proxyStreamingCtrlPort = -1;
	}
	
	
	/**
	 * Costruttore che non inizializza le porte, le imposta a -1 successivamente andranno
	 * impostate o singolaremente o tutte insieme tramite il metodo setSessionInfo
	 * 
	 * 
	 * @param id l�indirizzo Ip del client che si serve, magari anche grazie alla mediazione di un relay secondario)
	 * @param proxy  riferimento al Proxy che gestisce lo streaming
	 */
	public Session(String id, Proxy proxy) 
	{
		super();
		this.id = id;
		this.proxy = proxy;
		this.relaySecondario = null;
		this.senderStreamPort = -1;
		this.inStreamPort = -1;
		this.outStreamPort = -1;
		this.receiverStreamPort = -1;
		this.streamingServerCtrlPort = -1;
		this.proxyStreamingCtrlPort = -1;
	}
	
	
	///// TODO MEDTODI AGGIUNTIVI
	/**
	 * 
	 * @return true se il flusso di questa sessione viene diretto verso un relay secondario che poi lo mander� al client
	 */
	
	public boolean isMediata(){
		return (this.relaySecondario==null);
	}
	
	/**
	 * 
	 * @param relaySecondario l'indirizzo ip del relay secondario 
	 * @return true se il flusso di questa sessione viene diretto verso il relay secondario specificato 
	 */
	
	public boolean isMediataTramite(String relaySecondario){
		if (this.relaySecondario==null)
			return false;
		else return (this.relaySecondario.equals(relaySecondario));
			
	}
	
	/**
	 * 
	 * @return tutte le porte della sessione in un unico vettore ( per maggiori info guardare la descrizione dei campi della classe)
	 */
	
	public int[] getSessionInfo(){
		int[] sessionInfo = new int[6];
		sessionInfo[0] = this.senderStreamPort;
		sessionInfo[1] = this.inStreamPort;
		sessionInfo[2] = this.outStreamPort;
		sessionInfo[3] = this.receiverStreamPort;
		sessionInfo[4] = this.streamingServerCtrlPort;
		sessionInfo[5] = this.proxyStreamingCtrlPort;
		return sessionInfo;
	}
	
	/**
	 * 
	 * @param sessionInfo tutte le porte della sessione in un unico vettore ( per maggiori info guardare la descrizione dei campi della classe)
	 * sessionInfo[0]=  porta dalla quale il server eroga lo stream
	 * sessionInfo[1] = porta sulla quale il proxy riceve lo stream
	 * sessionInfo[2] = porta dalla quale il proxy eroga lo steam verso il client
	 * sessionInfo[3] = porta sulla quale il client riceve lo stream dal proxy
	 * sessionInfo[4] = porta di controllo del server
	 * sessionInfo[5] = porta di controllo del proxy
	 */
	
	public void setSessionInfo(int [] sessionInfo){
		this.senderStreamPort = sessionInfo[0];
		this.inStreamPort = sessionInfo[1];
		this.outStreamPort = sessionInfo[2];
		this.receiverStreamPort = sessionInfo[3];
		this.streamingServerCtrlPort = sessionInfo[4];
		this.proxyStreamingCtrlPort = sessionInfo[5];
		
	}
	@Override
	public String toString()
	{		
			
		return ("id : \t" + this.getId()+
				"\n InStreamPort : \t" +this.getInStreamPort()+
				"\n OutStreamPort : \t" +this.getOutStreamPort()+
				"\n ProxyStreamingCtrlPort : \t" +this.getProxyStreamingCtrlPort()+
				"\n ReceiverStreamPort : \t" +this.getReceiverStreamPort()+
				"\n sessione Mediata? \t" +this.isMediata());
		
	}
	
	
	

}
