package muse.client;



//interfaccia che permette di richiedere l'avvio della riproduzione dello stream RPT 
//a chi controlla il raggiungimento dell'istante di startpoint
public interface IStartpointListener
{
	//metodo che richiede l'avvio della riproduzione dello stream RTP
	public void startStreamPlaying();
}