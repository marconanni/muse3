package unibo.core.rtp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.media.format.UnsupportedFormatException;
import javax.media.protocol.DataSource;
import javax.media.rtp.InvalidSessionAddressException;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SendStreamListener;
import javax.media.rtp.RemoteListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.event.InactiveSendStreamEvent;
import javax.media.rtp.event.NewSendStreamEvent;
import javax.media.rtp.event.SendStreamEvent;
import javax.media.rtp.event.StreamClosedEvent;

import javax.media.control.BufferControl;
import javax.media.control.PacketSizeControl;

import parameters.NetConfiguration;


//Created 02/03/2006
//Last modified 02/03/2006

/**
* <p>Questa classe e' un semplice trasmettitore di traffico RTP basato su un'istanza di una classe che implementa
* l'interfaccia javax.media.rtp.RTPManager. Di fatto, questa classe implementa un wrapper di RTPManager che
* semplifica le chiamate per l'inizializzazione e la configurazione di RTPManager nel caso si vogliano trasmettere
* dati RTP in modalita' unicast dal nodo locale.</p>
* <p>Inoltre, e' implementata l'interfaccia SendStreamEvent, cosi' che questa classe possa catturare gli eventi
* associati alla trasmissione del flusso RTP.</p> 
* @author Sergio Valisi
*/
//La classe si occupa di incapsulare i dettagli della costruzione e gestione di un ricevitore di stream RTP, permette inoltre di registrarsi come listener di eventi SendStream e Remote
public class RTPSenderPS implements SendStreamListener
{
	private RTPManager rtpManager;

/**
* Costruttore. L'indirizzo del trasmettitore e' quello del host locale.
* @param localPort la porta locale da cui trasmettere il traffico RTP
* @throws IOException se l'inizializzazione fallisce
*/
	public RTPSenderPS(int localPort) throws IOException
	{
		
		System.out.println("----------APPENA DENTRO RTPSENDERPS");
		rtpManager=RTPManager.newInstance();
		rtpManager.addSendStreamListener(this);
		

		System.out.println("		RTPSenderPS: localaddr ==> "+InetAddress.getLocalHost()+":"+localPort);
		//SessionAddress localAddr=new SessionAddress(InetAddress.getByName(Parameters.RELAY_AD_HOC_ADDRESS),localPort);//Valerio:non era commentato
		
		//SessionAddress localAddr=new SessionAddress(InetAddress.getLocalHost(),localPort);//Valerio: era commentato
		SessionAddress localAddr=new SessionAddress(InetAddress.getByName(NetConfiguration.SERVER_ADDRESS),localPort);//Valerio:l'ho messo io
		System.out.println("RTPSENDERPS INDIRIZZO LOCALE:"+InetAddress.getByName(NetConfiguration.SERVER_ADDRESS).getHostAddress());
		System.out.println("RTPSENDERPS INDIRIZZO LOCALE SESSIONE:"+InetAddress.getByName(NetConfiguration.SERVER_ADDRESS));

		try{ 
			//SessionAddress serverAddr=new SessionAddress(InetAddress.getLocalHost(),21000);//Valerio: non era commentato
			SessionAddress serverAddr=new SessionAddress(InetAddress.getByName(NetConfiguration.SERVER_ADDRESS),21000);//Valerio:l'ho aggiunto io
			System.out.println("dopo servAddr");
			rtpManager.initialize(localAddr); 
			System.out.println("dopo initialize");
			rtpManager.addTarget(serverAddr);
			System.out.println("dopo addTarget");
			System.out.println("RTPSENDERPS dentro blocco try, localAddr: "+localAddr+", serverAddr: "+serverAddr);
		}
		catch(IOException e){ throw new IOException("RTP initializing failed:\n"+e); }
		catch(InvalidSessionAddressException e){ throw new IOException("Invalid Local Address:\n"+e); }
	}


	
	public RTPSenderPS(int localPort, InetAddress localAddress) throws IOException
	{
		rtpManager=RTPManager.newInstance();
		rtpManager.addSendStreamListener(this);
		SessionAddress localAddr=new SessionAddress(localAddress,localPort);
		System.out.println("INDIRIZZO LOCALE:"+localAddress.getHostAddress());
		System.out.println("INDIRIZZO LOCALE SESSIONE:"+localAddr.toString());
		try{ 
			SessionAddress serverAddr=new SessionAddress(localAddress,21000);
			rtpManager.initialize(localAddr); 
			rtpManager.addTarget(serverAddr);
			}
		catch(IOException e){ throw new IOException("RTP initializing failed:\n"+e); }
		catch(InvalidSessionAddressException e){ throw new IOException("Invalid Local Address:\n"+e); }
	}
	
/**
* Specifica l'endpoint del partecipante alla sessione destinatario dei pacchetti RTP.
* @param host l'indirizzo del ricevente
* @param port la porta su cui ascolta il ricevente
* @throws IOException se l'indirizzo non e' valido o non e' possibile aggiungere tale host alla sessione RTP
*/
	public void addDestination(InetAddress host, int port) throws IOException
	{
		SessionAddress destAddr=new SessionAddress(host,port);
		System.err.println("CLIENT ADDRESS: "+host.getHostName()+" PORT: "+port);
		try{ rtpManager.addTarget(destAddr); }
		catch (UnknownHostException e) { throw new IOException("Destination Host Unknown:\n"+e); }
		catch (InvalidSessionAddressException e) { throw new IOException("Adding target failed:\n"+e); }
	}

/**
* Fa partire l'invio dei dati.
* @param source il DataSource che riferisce il contenuto multimediale che si vuole trasmettere
* @throws IOException se il DataSource fornisce dati in un formato non compatibile con la trasmissione RTP 
*/
	public void sendData(DataSource source) throws IOException
	{
		try
		{
			SendStream sendStream=rtpManager.createSendStream(source,0);
			sendStream.start();
		}
		catch (UnsupportedFormatException e) { throw new IOException("DataSource format non supported by RTP: "+e); }

/****************************************************************************************/
/*			BufferControl bc = (BufferControl)rtpManager.getControl("javax.media.control.BufferControl");
			if (bc != null){
				 bc.setBufferLength(2150);
				 bc.setMinimumThreshold(0);
				 System.out.println("Buffer threshold: "+bc.getMinimumThreshold());
				 System.out.println("Buffer lenght: "+bc.getBufferLength());
			}
			else System.out.println("*** No buffer control ***");
			
			PacketSizeControl psc = (PacketSizeControl)rtpManager.getControl("javax.media.control.PacketSizeControl");
			if (psc != null){ System.out.println("PacketSize: "+psc.getPacketSize()); }
			else System.out.println("*** No PacketSizeControl *** ");*/
/****************************************************************************************/

	}

/**
* Chiude il trasmettitore, che in seguito a questa chiamata non potra' piu' essere utilizzato.
*/
	public void close()
	{
		rtpManager.removeTargets("Sender closed");
		rtpManager.dispose();
	}

//metodo che permette a chi possiede l'istanza della classe di farsi registrare come ulteriore listener di eventi ReceiveStream per poterli gestire
	public void addSendStreamEventListener(SendStreamListener listener) { rtpManager.addSendStreamListener(listener); }

//metodo che permette a chi possiede l'istanza della classe di farsi registrare come listener di eventi Remote per poterli gestire
	public void addRemoteEventListener(RemoteListener listener) { rtpManager.addRemoteListener(listener); }

//metodo che gestisce gli eventi SendStream
	public void update(SendStreamEvent event)
	{
		System.err.println("RTPRECEIVERPS.update: ricevuto evento: " + event.toString());
		
		if(event instanceof NewSendStreamEvent)
		{
			System.out.println("Send Stream Ready");
			return;
		}

		if(event instanceof InactiveSendStreamEvent)
		{
			System.out.println("Inactive Send Stream");
			rtpManager.removeTargets("Stream ended");
			rtpManager.dispose();
			System.exit(0);
		}

		if(event instanceof StreamClosedEvent)
		{
			System.out.println("Stream Closed");
			rtpManager.removeTargets("Stream ended");
			rtpManager.dispose();
			//System.exit(0);
		}
	}

//metodo che rimuove il destinatario della lista, mandandogli un BYE message
	public void removeTarget(InetAddress host, int port)throws IOException
	{
		try
		{
			SessionAddress destAddr=new SessionAddress(host,port);
			rtpManager.removeTarget(destAddr,"null");
		}
		catch (InvalidSessionAddressException e) { throw new IOException("Remove target failed:\n"+e); }
	}
}