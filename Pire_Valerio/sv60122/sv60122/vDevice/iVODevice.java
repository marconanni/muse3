package sv60122.vDevice;


/**
definisce l'interfaccia del dispositivo virtuale di uscita
@version 1.0
@author  Sergio Valisi 60122
*/
public interface iVODevice
{   /** metodo di scrittura di una stringa mediante il dispositivo di output virtuale,
		scrive la stringa e va a capo.
		@param s stringa indirizzata all'output
	*/
	public void scriviStringa(String s);
	
	/** metodo di scrittura di una stringa mediante il dispositivo di output virtuale.
		@param s stringa indirizzata all'output
	*/
	public void printStringa(String s);
}
