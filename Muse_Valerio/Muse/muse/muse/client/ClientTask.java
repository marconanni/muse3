package muse.client;

import java.net.DatagramPacket;

/**
 * Interfaccia che descrive un gestore di messaggi UDP lato client.
 * <br/>I messaggi vengono recapitati al gestore da un broker attraverso il metodo <code>addRequest(DatagramPacket message)<code>
 * @author Zapparoli Pamela
 * @version 1.0
 *
 */

public interface ClientTask extends Runnable
{
	/**
	 * Notifica al processo una rischiesta da gestire
	 * @param message la richiesta da gestire
	 */
	public void addRequest(DatagramPacket message);
	
	/**
	 * Termina il processo alla fine del ciclo corrente
	 */
	public  void endAll();
	
	/**
	 * Esegue il ciclo del processo
	 */
	public void run();
	
}
