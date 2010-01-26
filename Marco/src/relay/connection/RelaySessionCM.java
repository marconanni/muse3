package relay.connection;

import java.net.InetAddress;
import java.util.Observer;

/**
 * @author Luca Campeti, Marco Nanni
 *classe vecchia, non più usata, sostituita da RelayCM
 */


public class RelaySessionCM extends AConnectionManager {

	/**
	 * 
	* @param localAdHocAddress è l'indirizzo che il relay ha sulla rete di cui è il relay di riferimeno
	 * @param localAdHocSessionInputPort è la porta di ricezione usata dal relay sulla rete di cui è ilrelay di riferimento
	 * @param localAdHocSessionOutputPort è la porta di invio  usata dal relay sulla rete di cui è ilrelay di riferimento
	 * @param enableAdhocBcasReception se true abilita la ricezione dei messaggi in broadcast sulla rete di cui il nodo è il relay di riferimento ( attualemente non serve: mattere false)

	 * @param observer l'oggetto di cui verrà richiamato il metodo update una volta ricevuto un messaggio
	 */
	
	public RelaySessionCM(InetAdress localAdHocAddress,String localAdHocBcastAdress, int localAdHocSessionInputPort, int localAdHocSessionOutputPort, boolean enableAdhocBcasReception,  Observer observer){
		super(localAdHocAddress,localAdHocBcastAdress,localAdHocSessionInputPort,localAdHocSessionOutputPort,observer,enableAdhocBcasReception);
		setNameManager("RelaySessionCM");
	}
}