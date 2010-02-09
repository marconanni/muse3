package sv60122.vDevice;

import java.io.*;


/**
definisce l'interfaccia del dispositivo virtuale di ingresso
@version 1.1
@author  Sergio Valisi 60122
*/
public interface iVIDevice
{ /** metodo di lettura di una stringa mediante il dispositivo di input virtuale.
	  @return stringa letta
	  @throws IOException in caso di errore
  */
  public String leggiStringa()throws IOException;
  
  /** metodo di lettura di un carattere mediante il dispositivo di input virtuale.
      @return carattere letto
  	  @throws IOException in caso di errore
  */
  public char leggiChar()throws IOException;
}