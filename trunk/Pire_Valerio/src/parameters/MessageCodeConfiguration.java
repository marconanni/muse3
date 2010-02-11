package parameters;

public class MessageCodeConfiguration {
	
	public static final int TYPERELAY = 0;
	public static final int TYPECLIENT = 1;
	public static final int TYPERELAYPASSIVE = 2;
	
	public static final int WHO_IS_RELAY = 3;
	public static final int IM_RELAY = 4;
	
	public static final int ACK_CONNECTION = 5;

	public static final int REQUEST_RSSI = 6;
	public static final int NOTIFY_RSSI = 7;
	
	public static final int ELECTION_REQUEST = 8;			//Messaggio inviato da un relay attualmente attivo che deve essere sostituito
	public static final int ELECTION_BEACON = 9;			//Messaggio inviato dai client con sessione RTP in corso destinato ai possibili relay sostituti
	public static final int ELECTION_BEACON_RELAY = 10;
	public static final int ELECTION_RESPONSE = 11;
	public static final int ELECTION_DONE = 12;				//Messagio inviato dal nuovo relay appena eletto
	
	public static final int EM_EL_DET_CLIENT = 13;
	public static final int EM_EL_DET_RELAY = 14;
	public static final int EM_ELECTION = 15;
	
	public static final int LOSE_MESSAGE = 80;
	
	//VALERIO
	public static final int REQUEST_LIST = 16;
	public static final int FORWARD_REQ_LIST = 17;
	public static final int LIST_RESPONSE = 18;
	public static final int FORWARD_LIST_RESPONSE = 19;
	public static final int ACK_REQUEST_FILE = 20;
	public static final int FORWARD_ACK_REQ = 21;
	
	public static final int REQUEST_FILE = 22;
	public static final int FORWARD_REQ_FILE = 23;
	public static final int ACK_RELAY_FORW = 24;
	public static final int ACK_CLIENT_REQ = 25;
	
	public static final int START_TX = 26;
	public static final int STOP_TX = 27;
	
	public static final int ACK = 28;	
	
	//MARCO
	public static final int REQUEST_SESSION= 29;
	public static final int SESSION_INFO = 30;
	public static final int ACK_SESSION = 31;
	public static final int REDIRECT = 32;
	public static final int LEAVE = 33;

	public static final int SERVER_UNREACHEABLE = 50;
	public static final int SESSION_INVALIDATION = 51;
	
}
