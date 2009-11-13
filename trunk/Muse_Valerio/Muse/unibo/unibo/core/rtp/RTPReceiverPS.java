package unibo.core.rtp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.media.control.BufferControl;
import javax.media.protocol.DataSource;
import javax.media.rtp.event.ActiveReceiveStreamEvent;
import javax.media.rtp.event.ByeEvent;
import javax.media.rtp.event.InactiveReceiveStreamEvent;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.RemotePayloadChangeEvent;
import javax.media.rtp.InvalidSessionAddressException;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.RemoteListener;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SessionAddress;


/**
 * <p>Questa classe e' un semplice ricevitore di traffico RTP basato su un'istanza di una classe che implementa
 * l'interfaccia javax.media.rtp.RTPManager. Di fatto, questa classe implementa un wrapper di RTPManager che
 * semplifica le chiamate per l'inizializzazione e la configurazione di RTPManager nel caso si vogliano ricevere
 * dati RTP in modalita' unicast sul nodo locale.</p>
 * <p>Inoltre, e' implementata l'interfaccia ReceiveStreamListener, cosi' che siano intercettati gli eventi relativi
 * alla ricezione di un flusso RTP.</p>
 * <p>E' stato aggiunto un metodo che permette di registrarsi xome listener di eventi Receive Stream a chi possiede il riferimento dell'istanza della classe.</p>
 * @author Sergio Valisi
 */

//Created 24/02/2006
//Last modified 03/03/2006  inserito bufferControl di JMF per aumentare il buffer usato
//inseriti metodi per legger ee impostare parametre del buffer (bufferControl) di JMF

//La classe si occupa di incapsulare i dettagli della costruzione e gestione di un ricevitore di stream RTP, permette inoltre di registrarsi come listener di eventi ReceiveStream e Remote, e di leggere e impostare i parametri di gestione del buffer di JMF
public class RTPReceiverPS implements ReceiveStreamListener
{
	//variabili getsione ricezione stream RTP
	private RTPManager rtpManager;
	private DataSource receivedDataSource;

	//variabile di sincronizzazione attesa arrivo stream
	private Object dataSync;

	//variabile di gestione dei buffer di JMF
	private boolean bufferReady;
	private BufferControl buffCtl;

/**
* Costruttore. L'indirizzo del ricevitore e' quello del host locale.
* @param localPort la porta locale su cui attendere il traffico RTP
* @throws IOException se l'inizializzazione fallisce
*/
	public RTPReceiverPS(int localPort) throws IOException
	{
		bufferReady=false;
		dataSync=new Object();
		receivedDataSource=null;
		rtpManager=RTPManager.newInstance();
		rtpManager.addReceiveStreamListener(this);
		
		try
		{
			System.out.println(InetAddress.getLocalHost());
			SessionAddress localAddr=new SessionAddress(InetAddress.getLocalHost(),localPort);
			SessionAddress serverAddr=new SessionAddress(InetAddress.getLocalHost(),20000);
			rtpManager.initialize(localAddr);
			rtpManager.addTarget(serverAddr);
		}
		catch(IOException e){ throw new IOException("RTP initializing failed:\n"+e); }
		catch(InvalidSessionAddressException e){ throw new IOException("Invalid Local Address:\n"+e); }
		//imposto un valor minimo per il buffer JMF altrimenti la riproduzione del flusso risulta disturbata
		buffCtl = (BufferControl)rtpManager.getControl("javax.media.control.BufferControl");
		if(buffCtl != null)
		{
			bufferReady=true;
			buffCtl.setBufferLength(1050);  //valor minimo per il client
			buffCtl.setMinimumThreshold(0);
		}
		else System.err.println(" RTPReceiver: no buffer control");
	}

	/**
	* Costruttore. L'indirizzo del ricevitore e' quello del host locale.
	* @param localPort la porta locale su cui attendere il traffico RTP
	* @param local indirizzo IP locale
	* @throws IOException se l'inizializzazione fallisce
	*/
		public RTPReceiverPS(int localPort, InetAddress local) throws IOException
		{
			bufferReady=false;
			dataSync=new Object();
			receivedDataSource=null;
			rtpManager=RTPManager.newInstance();
			rtpManager.addReceiveStreamListener(this);

			try
			{
				System.out.println("RTPReceiverPS: local address: "+local.getHostAddress()+" local port "+localPort);
				SessionAddress localAddr=new SessionAddress(local,localPort);
				SessionAddress serverAddr=new SessionAddress(InetAddress.getLocalHost(),20000);
				rtpManager.initialize(localAddr);
				rtpManager.addTarget(serverAddr);
			}
			catch(IOException e){ throw new IOException("RTP initializing failed:\n"+e); }
			catch(InvalidSessionAddressException e){ throw new IOException("Invalid Local Address:\n"+e); }
			//imposto un valor minimo per il buffer JMF altrimenti la riproduzione del flusso risulta disturbata
			buffCtl = (BufferControl)rtpManager.getControl("javax.media.control.BufferControl");
			if(buffCtl != null)
			{
				bufferReady=true;
				buffCtl.setBufferLength(1050);  //valor minimo per il client
				buffCtl.setMinimumThreshold(0);
			}
			else System.err.println(" RTPReceiver: no buffer control");
		}
/**
* Specifica l'endpoint del trasmittente dei pacchetti RTP.
* @param host l'indirizzo del mittente
* @param port la porta da cui il mittente trasmette
* @throws IOException se l'indirizzo non e' valido o non e' possibile aggiungere tale host alla sessione RTP
*/
//se e' prevista la possibilita' di chiamare piu' volte questo metodo assegnando vari trasmettitori da cui ricevere allora sarebbe il caso di spostare nel costruttore l'inizializzazione di dataSynch
	public void setSender(InetAddress host, int port) throws IOException
	{
		SessionAddress serverAddr=new SessionAddress(host,port);

		try{ rtpManager.addTarget(serverAddr); }
		catch(UnknownHostException e){ throw new IOException("Destination Host Unknown:\n"+e); }
		catch(InvalidSessionAddressException e){ throw new IOException("Adding target failed:\n"+e); }
	}

/**
* <p>La chiamata a questo metodo blocca il ricevitore finche' non viene ricevuto un flusso RTP.</p>
* <p>All'arrivo dei dati, viene restituita un'istanza di DataSource per potervi accedere.</p>
* @return un'istanza di DataSource da cui leggere i dati ricevuti via RTP
*/
	public DataSource receiveData()
	{
		if (receivedDataSource==null)
		{
			System.out.println("Waiting RTP Data...");
			try
			{
				synchronized (dataSync){ while (receivedDataSource==null) dataSync.wait(); }
			}
			catch (InterruptedException e) { return null; } // non si verifica
		}
		return receivedDataSource;
	}

/**
* Chiude il ricevitore, che in seguito a questa chiamata non potra' piu' essere utilizzato.
*/
	public void close()
	{
		rtpManager.removeTargets("Receiver closed");
		rtpManager.dispose();
	}

//metodo che permette a chi possiede l'istanza della classe di farsi registrare come ulteriore listener di eventi ReceiveStream per poterli gestire
	public void addReceiveStreamEventListener(ReceiveStreamListener listener) { rtpManager.addReceiveStreamListener(listener); }

//metodo che permette a chi possiede l'istanza della classe di farsi registrare come listener di eventi Remote per poterli gestire
	public void addRemoteEventListener(RemoteListener listener) { rtpManager.addRemoteListener(listener); }

//metodo che, se presente, restisce il livello di soglia minimo (espresso in millisecondi) del buffer di JMF utilizzato dal ricevitore di stream RTP, se il buffer non e' disponibile restituisce -1
	public long getMinimumThreshold()
	{
		if(bufferReady) return buffCtl.getMinimumThreshold();
		else return -1;
	}

//metodo che, se presente, imposta il livello di soglia minimo (espresso in millisecondi) del buffer di JMF utilizzato dal ricevitore di stream RTP, restiusce il flag che indica la disponibilita' del Buffer ad essere impostato e di conseguenza, l'andamento a buon fine, o meno, dell'operazione
	public boolean setMinimumThreshold(long minTreshold)
	{
		if(bufferReady) buffCtl.setMinimumThreshold(minTreshold);
		return bufferReady;
	}

//metodo che, se presente, restisce la capienza (espressa in millisecondi) del buffer di JMF utilizzato dal ricevitore di stream RTP, se il buffer non e' disponibile restituisce -1
	public long getBufferLength()
	{
		if(bufferReady) return buffCtl.getBufferLength();
		else return -1;
	}

//metodo che, se presente, imposta il livello di soglia minimo (espressa in millisecondi) del buffer di JMF utilizzato dal ricevitore di stream RTP, restiusce il flag che indica la disponibilita' del Buffer ad essere impostato e di conseguenza, l'andamento a buon fine, o meno, dell'operazione
	public boolean setBufferLength(long buffLength)
	{
		if(bufferReady) buffCtl.setBufferLength(buffLength);
		return bufferReady;
	}

//metodo che gestisce gli eventi ReceiveStream
	public void update (ReceiveStreamEvent event)
	{
		if (event instanceof NewReceiveStreamEvent)
		{
			System.out.println("New Stream Received");
			ReceiveStream rs=((NewReceiveStreamEvent)event).getReceiveStream();
			receivedDataSource=rs.getDataSource();
			synchronized(dataSync){ dataSync.notify(); }
			return;
		}

		if (event instanceof ActiveReceiveStreamEvent)
		{
			System.out.println("Active Stream");
			return;
		}

		if (event instanceof RemotePayloadChangeEvent)
		{
			System.out.println("Remote Payload Change");
			return;
		}

		if (event instanceof InactiveReceiveStreamEvent)
		{
//TODO: nel caso la trasmissione sia sospesa e poi ripresa, viene prima generato un evento di classe
//NewReceiveStreamEvent e poi uno di classe InactiveReceiveStreamEvent.
//Verificare il motivo di questa situazione ed inserire il codice appropriato per gestirla.
			System.out.println("Inactive Stream");
			rtpManager.removeTargets("Session ended");
			rtpManager.dispose();
			System.exit(0);
		}

		if (event instanceof ByeEvent)
		{
			rtpManager.removeTargets("Session ended");
			rtpManager.dispose();
		}
	}
}