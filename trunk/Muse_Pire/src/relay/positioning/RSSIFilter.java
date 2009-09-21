package relay.positioning;


/**
 * RSSIFilter.java
 * Interfaccia che descrive un generico modello per il filtraggio e la previsione dei valori futuri di RSSI.
 * @author Zapparoli Pamela
 * @version 1.0
 *
 */

public interface RSSIFilter 
{
	/**
	 * Imposta i valori originali del segnale
	 * @param RSSIRealValues i valori originali del segnale
	 * @throws InvalidParameter se l'elenco dei valori e' vuoto o contiene valori negativi
	 */
	public void setOriginalValues(double[] RSSIRealValues)throws InvalidParameter;
	
	
	/**
	 * Predice il valore che il segnale avra' fra time secondi 
	 * @param time l'istante in cui effettuare la predizione
	 * @param samplingTime tempo di campionamento del segnale originale
	 * @return il valore predetto del segnale
	 * @throws InvalidParameter se il tempo d predizione o di campionamento sono negativi
	 */
	public double predictRSSI(int timeSec, long samplingTime) throws InvalidParameter;
	
	/**modifica mia
	 */
	public double predictRSSI();

	
	/**
	 * Indica il tempo della predizione in secondi a partire da ora
	 * @return l'istente della predizione in secondi o -1 se non e' ancora stata fatta la predizione
	 */
	public long predictTime();
}
