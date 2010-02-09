package parameters;

public class MessageCodeConfiguration {
	
	public static final int TYPERELAY = 0;
	public static final int TYPECLIENT = 1;
	public static final int TYPERELAYPASSIVE = 2;
	
	public static final int WHO_IS_RELAY = 3;
	public static final int IM_RELAY = 4;
	
	public static final int ACK_CONNECTION = 7;

	public static final int REQUEST_RSSI = 8;
	public static final int NOTIFY_RSSI = 9;
	
	public static final int ELECTION_REQUEST = 11;			//Messaggio inviato da un relay attualmente attivo che deve essere sostituito
	public static final int ELECTION_BEACON = 12;			//Messaggio inviato dai client con sessione RTP in corso destinato ai possibili relay sostituti
	public static final int ELECTION_BEACON_RELAY = 13;
	public static final int ELECTION_RESPONSE = 14;
	public static final int ELECTION_DONE = 15;				//Messagio inviato dal nuovo relay appena eletto
	
	public static final int EM_EL_DET_CLIENT = 18;
	public static final int EM_EL_DET_RELAY = 19;
	public static final int EM_ELECTION = 20;
	
	public static final int LOSE_MESSAGE = 80;
	
	//VALERIO
	public static final int REQUEST_LIST = 90;
	public static final int FORWARD_REQ_LIST = 91;
	public static final int LIST_RESPONSE = 92;
	public static final int FORWARD_LIST_RESPONSE = 93;
	public static final int ACK_REQUEST_FILE = 94;
	public static final int FORWARD_ACK_REQ = 95;
	
	public static final int REQUEST_FILE = 14;
	public static final int FORWARD_REQ_FILE = 15;
	public static final int ACK_RELAY_FORW = 16;
	public static final int ACK_CLIENT_REQ = 17;
	
	public static final int START_TX = 18;
	public static final int STOP_TX = 19;
	
	public static final int ACK = 40;	
	
	//MARCO
	public static final int REQUEST_SESSION= 30;
	public static final int SESSION_INFO = 31;
	public static final int ACK_SESSION = 32;
	public static final int REDIRECT = 33;
	public static final int LEAVE = 34;

	public static final int SERVER_UNREACHEABLE = 50;
	public static final int SESSION_INVALIDATION = 51;
	
}
