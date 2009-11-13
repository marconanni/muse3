package sv60122.vDevice;

import java.io.*;


/**
definisce l'interfaccia del dispositivo virtuale di ingresso e uscita
@version 1.0
@author  Sergio Valisi
*/
public interface iVIODevice extends iVIDevice, iVODevice{ 
	/** equivalente al metodo leggiStringa() con la possibilità
		di dare un'indicazione all'utente del tipo di input richiesto
		@return stringa letta
		@throws IOException in caso di errore
	*/
	public String leggiStringa(String mex)throws IOException;
	
	/** equivalente al metodo leggiChar() con la possibilità
		di dare un'indicazione all'utente del tipo di input richiesto
		@return carattere letto
		@throws IOException in caso di errore
	*/
  	public char leggiChar(String mex)throws IOException;
}