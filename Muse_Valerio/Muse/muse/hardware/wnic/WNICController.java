package hardware.wnic;

import java.net.InetAddress;
import java.util.Vector;

import hardware.wnic.exception.InvalidAccessPoint;
import hardware.wnic.exception.WNICException;

/**
 * WNICController.java
 * Interfaccia che descrive i metodi che deve mettere a disposizione
 * il controller della scheda wireless, in un determinato sistema operativo.
 * 
 * @author Zapparoli Pamela
 * @version 1.0
 *
 */
public interface WNICController 
{
	/**
	 * Accende la scheda wireless
	 * @return true se la scheda si e' accesa
	 */
	public boolean setOn();
	
	/**
	 * Spegne la scheda wireless
	 * @return true se si e' spenta
	 */
	public boolean setOff();
	
	/**
	 * Indica se la scheda wireless e' accesa
	 * @return true se la scheda wireless e' accesa
	 * @throws WNICException se non si riesce a ottenere lo stato della scheda
	 */
	public boolean isActive() throws WNICException;
	
	/**
	 * Indica da quanto tempo la scheda e' accesa
	 * @return se la scheda e' accesa indica da quanto e' accesa, altrimenti restituisce la durata del periodo di on precedente, -1 se non e' ancora iniziato un periodo di on
	 * @throws WNICException se non si riesce a ottenere lo stato della scheda
	 */
	public long getActiveTime() throws WNICException;
	
	/**
	 * Indica da quanto tempo la scheda e' spenta
	 * @return se la scheda e' accesa indica da quanto e' spenta, altrimenti restituisce la durata del periodo di off precedente, -1 se non e' ancora iniziato un periodo di off
	 * @throws WNICException se non si riesce a ottenere lo stato della scheda
	 */
	public long getInactiveTime() throws WNICException;
	
	/**
	 * Restituisce il timestamp dell'istante di inizio dello stato corrente (di on o di off)
	 * @return istante di inizio dello stato corrente
	 */
	public long getCurrentStateStartTime();
	
	/**
	 * Indica se la scheda wireless e' connessa ad un access point
	 * @return true se la scheda e' connessa, flase altrimenti
	 * @throws WNICException se non si riesce a ottenere lo stato della scheda
	 */
	public boolean isConnected()throws WNICException;
	
	/**
	 * Restituisce l'indirizzo IP corrente della scheda wireless
	 * @return l'indirizzo IP corrente della scheda wireless o null se la scheda non e' connessa
	 * @throws WNICException se non si riesce a ottenere lo stato della scheda
	 */
	public InetAddress getAssociatedIP()throws WNICException;
	
	/**
	 * Restituisce l'ultimo RSSI registrato per l'access point corrente
	 * @return l'ultimo RSSI registrato per l'access point corrente o 0 se non e' connessa
	 * @throws WNICException se non si riesce a ottenere lo stato della scheda
	 * @throws InvalidAccessPoint se l'access point corrente non e' valido
	 */
	public int getSignalLevel()throws WNICException, InvalidAccessPoint;
	
	/**
	 * Restituisce l'access point a cui la scheda e' connessa
	 * @return l'access point a cui la scheda e' connessa o null se non e' connessa
	 * @throws WNICException se non si riesce a ottenere lo stato della scheda
	 * @throws InvalidAccessPoint se l'access point corrente non e' valido
	 */
	public AccessPointData getAssociatedAccessPoint() throws WNICException, InvalidAccessPoint;
	
	/**
	 * Restituisce un vettore con gli access point visibili
	 * @return un vettore con gli access point visibili
	 * @throws WNICException se non si riesce a ottenere lo stato della scheda
	 * @throws InvalidAccessPoint se uno degli access point non e' valido
	 */
	public Vector<AccessPointData> getVisibleAccessPoints() throws WNICException, InvalidAccessPoint;
	
	/**
	 * Restituisce l'access point col segnale piu' alto
	 * @return l'access point col segnale piu' alto o null se non vi sono access point visibili o la scheda e' spenta
	 * @throws WNICException se non si riesce a ottenere lo stato della scheda
	 * @throws InvalidAccessPoint se l'access point non e' valido
	 */
	public AccessPointData getAccessPointWithMaxRSSI() throws WNICException, InvalidAccessPoint;
	
	/**
	 * Consente di collegare la scheda wireless all'access point specificato
	 * @param ap l'access point a cui connettersi
	 * @return true se la scheda e' riuscita a connettersi, false altrimenti
	 * @throws WNICException se non si riesce a ottenere lo stato della scheda
	 * @throws InvalidAccessPoint se l'access point non e' valido
	 */
	public boolean connectToAccessPoint(AccessPointData ap) throws WNICException, InvalidAccessPoint;
}
