package muse.client;

import java.io.IOException;
import java.awt.Frame;
import java.awt.event.*;
import java.net.*;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.IncompatibleSourceException;
import javax.media.Manager;
import javax.media.Player;
import javax.media.protocol.DataSource;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.event.ByeEvent;
import javax.media.rtp.event.ReceiveStreamEvent;

import muse.client.gui.IClientView;

import unibo.core.multiplexer.PlayerMultiplexer;
import unibo.core.parser.Parser;
import unibo.core.parser.RTPParser;
import unibo.core.rtp.RTPReceiverPS;
import unibo.core.thread.MultiplexerThread;
import unibo.core.thread.MultiplexerThreadPS;
import unibo.core.thread.ParserThreadEV;
import unibo.core.thread.ParserThreadPS;
import unibo.core.*;

import sv60122.dbgUtil.*;



public class ClientBufferDataPlaying implements ControllerListener, ReceiveStreamListener, BufferFullListener
{
	//MODIFICA AMBRA: booleano che indica se � stato inviato un messaggio di buffer full al proxy
	private boolean bfSent = true; //inizialmente non � possibile inviare messaggi di buffer full
	
	//variabili gestione thread di controllo flusso RTP
	private Thread runner;

	//variabili di informazione sul proxy
	private int proxyPortRTP;
	private InetAddress proxyIP;

	//variabili di informazione sul client
	private int clientPortRTP;

	//variabile ricezione stream RTP
	private RTPReceiverPS rtpRx;

	//variabili gestione parsing
	//MODIFICA: si utilizza un ParserThreadEV per ottenere un EventCircularBuffer
	private ParserThreadEV parserThread;
	//private ParserThreadPS parserThread;
	private Parser rtpParser;

	//variabili gestione data buffer path
	private DataSource dsInput;
	//MODIFICA: non pi� CircularBuffer, ma EventCircularBuffer
	private EventCircularBuffer buffer;
	private int clientBufferSize;
	private boolean playing=false;

	//variabili gestione multiplexing
	private MultiplexerThread muxThread;
	private PlayerMultiplexer mux;
	private int timeToWait;

	//variabile del player
	private Player player;
	
	//MODIFICA: variabile controller per la corretta gestione dell'evento di buffer pieno
	private ClientController controller;
	
	private IClientView view;

//metodo costruttore che prende come parametri l'indirizzo (IP + Port) del proxy per il flusso RTP e la porta di ricezione del client per tale flusso, la capacita' del buffer e l'intervallo tra l'elaborazione dei frame da parte del multiplexer, un'interfaccia per il piano di controllo
	public ClientBufferDataPlaying(int sendingPortRTP, int inPortRTP, InetAddress proxyAddress, int bufferSize, int timeToWait, IClientView view)
	{
		constructorAssigment(sendingPortRTP, inPortRTP, proxyAddress, bufferSize, timeToWait, view);
	}

	// ***** //
	//	metodo costruttore che prende come parametri l'indirizzo (IP + Port) del proxy per il flusso RTP e la porta di ricezione del client per tale flusso, la capacita' del buffer e l'intervallo tra l'elaborazione dei frame da parte del multiplexer, un'interfaccia per il piano di controllo
	public ClientBufferDataPlaying(int sendingPortRTP, int inPortRTP, InetAddress proxyAddress, int bufferSize, int timeToWait, ClientController cc, IClientView view)
	{
		constructorAssigment(sendingPortRTP, inPortRTP, proxyAddress, bufferSize, timeToWait, cc, view);
	}
	
//metodo costruttore analogo al precedente con la differenza che imposta come debug messanger quello passato dal proprietario dell'oggetto
	public ClientBufferDataPlaying(iDebugWriter debugger, int sendingPortRTP, int inPortRTP, InetAddress proxyAddress, int bufferSize, int timeToWait, IClientView view)
	{
		constructorAssigment(sendingPortRTP, inPortRTP, proxyAddress, bufferSize, timeToWait, view);
	}

/*
proxyIP---->l'indirizzo IP del mittente
* proxyPortRTP-->la porta da cui trasmette il mittente
* clientPortRTP-->il numero di porta su cui il client ascolta l'arrivo dei dati RTP
*/

//metodo che si occupa di effettuare gli assegnamenti sempre presenti nelle diverse tipologie di metodi costruttori
	private void constructorAssigment(int sendingPortRTP, int inPortRTP, InetAddress proxyAddress, int bufferSize, int multiplexerInterCycleWait, IClientView view)
	{
		proxyPortRTP=sendingPortRTP;
		clientPortRTP=inPortRTP;
		proxyIP=proxyAddress;
		clientBufferSize=bufferSize;
		timeToWait=multiplexerInterCycleWait;
		this.view = view;

		System.out.println("ClientChainBufferDataPlaying creato con:\nproxyPortRTP: "+proxyPortRTP+"\nclientPortRTP: "+clientPortRTP+"\nproxyIP: "+proxyIP);
	}

//	metodo che si occupa di effettuare gli assegnamenti sempre presenti nelle diverse tipologie di metodi costruttori
	private void constructorAssigment(int sendingPortRTP, int inPortRTP, InetAddress proxyAddress, int bufferSize, int multiplexerInterCycleWait, ClientController cont, IClientView view)
	{
		proxyPortRTP=sendingPortRTP;
		clientPortRTP=inPortRTP;
		proxyIP=proxyAddress;
		clientBufferSize=bufferSize;
		timeToWait=multiplexerInterCycleWait;
		controller = cont;
		this.view = view;

		System.out.println("ClientChainBufferDataPlaying creato con:\nproxyPortRTP: "+proxyPortRTP+"\nclientPortRTP: "+clientPortRTP+"\nproxyIP: "+proxyIP);
	}

//avvia un nuovo thread per gestire lo stream in arrivo dal proxy, trasformarlo in flusso RAW e passarlo al player
	public void start()
	{
		
		runner=new Thread(){ public void run(){ playingStreamRTP(); } };
		runner.start();
	}

//metodo che gestisce l'accesso in lettura sincrono (mutuamente esclusivo) alla variabile flag playing
	private synchronized boolean getPlayingFlag(){ return playing; }

//metodo che gestisce l'accesso in scrittura sincrono (mutuamente esclusivo) alla variabile flag playing
	private synchronized void setPlayingFlag(boolean flag){ playing=flag; }

//metodo che avvia la riproduzione dello stream, la avvia solo se il multiplexer e' stato creato e se e' la prima volta, ulteriori chiamate non avranno effetto cosi' come non avranno effetto chiamate effettuate prima dell'arrivo dello stream RTP e della conseguente creazione dell'istanza di multiplexer
	//public synchronized boolean startPlaying()
	public boolean startPlaying() //limito la sincronizzazione al solo accesso alla variabile flag playing che determina se l'avvio e' avvenuto o no, in questo modo allegerisco l'elaborazione
	{
		//if(!playing)
		if(!getPlayingFlag())
		{
			setPlayingFlag(true);
			if(mux==null)
			{
				setPlayingFlag(false);
				return false;
			}
			muxThread.start();
			System.out.println("ClientChainBufferDataPlaying: muxThread started");

			try{
				Manager.setHint(Manager.PLUGIN_PLAYER, new Boolean(true));
				player = Manager.createRealizedPlayer(mux.getDataOutput()); }
			catch(Exception e){
				System.out.println("ClientChainBufferDataPlaying: impossibile abilitare il player\n");
				e.printStackTrace(); }
			System.out.println("ClientChainBufferDataPlaying: Player abilitato ed in stato realized\n");

			player.addControllerListener(this);  //registro il listener per EOM event
			
			view.setPlayer(player);
			//
			
/*			Frame frame = new Frame("Now Playing...");
			if(player.getVisualComponent()!=null)
			{
				frame.add(player.getVisualComponent());
				frame.setSize(230, 150);
			}
			else frame.setSize(230, 50);
			frame.add(player.getControlPanelComponent());

			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) 
				{
					if (player!=null) { player.stop(); player.close();}
					close();
					System.exit(0);
				}
			});

			frame.setVisible(true);*/
			//
			player.start();
		}
		return playing;
	}

//metodo che si occupa di impostare la datapath della ricezione, elaborazione e riproduzione del flusso
	public void playingStreamRTP()
	{
		try
		{
			System.out.println("ClientBufferDataPlaying metodo playingStreamRTP()");
			Manager.setHint(Manager.PLUGIN_PLAYER,new Boolean(true));
			rtpRx=new RTPReceiverPS(clientPortRTP);
			rtpRx.setSender(proxyIP,proxyPortRTP);//da cambiare solo i nomi in realtà sono indirizzo e porta del server
			dsInput=rtpRx.receiveData();
			System.out.println("ricevuto uno stream, si è sbloccato il thread playingStreamRTP");
			//clReport.startPSTransmission();
			rtpRx.addReceiveStreamEventListener(this);
			// ***** PARSER *****
			rtpParser=new RTPParser(dsInput);
		//parserThread=new ParserThreadPS(rtpParser,clientBufferSize,timeToWait);
			parserThread=new ParserThreadEV(rtpParser,clientBufferSize,view);
			EventCircularBuffer[] b= parserThread.getOutputBufferSet();
			buffer=b[0];
			//il ClientChainBuffer viene settato come listener degli eventi generati dal buffer
			buffer.addBufferFullEventListener(this);

			// ***** MULTIPLEXER *****
			mux=new PlayerMultiplexer(rtpParser.getTracksFormat());
			muxThread=new MultiplexerThread(mux,b,5);
			//muxThread=new MultiplexerThreadPS(mux,b,null,5);
			muxThread.setTimeToWait(timeToWait);
			
			parserThread.start();
			System.out.println("ClientChainBufferDataPlaying: parser started");
			
			

		}
		catch(IOException e)
		{
			System.err.println(e); 
			e.printStackTrace();
		}
		catch(IncompatibleSourceException e)
		{
			System.err.println(e);
			e.printStackTrace();
		}
		catch (Exception e)
		{
			System.err.println(e); 
			e.printStackTrace();
		}
	}

	//metodo che gestisce l'evento di EndOfMedia
	public void controllerUpdate(ControllerEvent ce)
	{
		if(ce instanceof EndOfMediaEvent)
		{
			System.out.println("ClientChainBufferDataPlaying: media finito");
			if (player!=null) { player.stop(); player.close();}
			close();
			//segnalo al componente di controllo che la riproduzione del multimedia e' terminata
			controller.killAll();
		}
	}

//metodo che gestisce l'evento di Bye
	public void update (ReceiveStreamEvent event)
	{
//avendo ricevuto l'evento di BYE chiudo il parser e attendo che il buffer si svuoti per chiudere anche gli altri thread (il receiver si chiude da solo quando riceve l'evento di BYE)
		if (event instanceof ByeEvent)
		{
			System.out.println("__BYE MESSAGE in ClientPlayer__");
			//segnalo al componente di controllo che il proxy ha terminato di inviare lo stream
			//if(iClientControlPS!=null) iClientControlPS.closeTransmission();
			if(parserThread!=null)
			{
				parserThread.close();
				parserThread=null;
			}
			if(dsInput!=null)
			{
				dsInput.disconnect();
				dsInput=null;
			}
			
			com.sun.media.ExtBuffer frame=new com.sun.media.ExtBuffer();
			frame.setEOM(true);
			try{ Thread.sleep(100); }
			catch(InterruptedException e){ System.err.println(e); }
			buffer.setFrame(frame); //inserisco un frame fittizio perche' segnali l'EndOfMedia, attendo un poco per lasciare il tempo di arrivare ad atri pacchetti.
			System.out.println("ClientChainBufferDataPlaying update method: Inserito nel CircularBuffer il frame con flag EOM\n");

		}
	}

//metodo che si occupa di fermare i vari thread del datapath e di liberare le risorse
	public void close()
	{
		if(muxThread!=null)muxThread.close();
		if(mux!=null) mux.close();
		if(parserThread!=null) parserThread.close();
		if(dsInput!=null)dsInput.disconnect();
		if(rtpRx!=null)rtpRx.close();
		System.out.println("ClientChainBufferDataPlaying close method: I thread lanciati dal Client sono stati terminati\n");
	}

	//metodo che reinizializza il buffer quando viene ritrasmesso l'inizio del multemedia dopo la fase di first probing
	public void resetPlaying(){ buffer.reset(); }

	//metodo di accesso che restituisce il numero di frame presenti sul buffer
	public int getFrameOnBuffer()
	{
		if(buffer==null) return 0;
		return buffer.contatore;
	}

	//metodo che restituisce il buffer utilizzato, non so se e' il caso di mantenerlo
	public CircularBuffer getBuffer(){ return buffer; }
	
	//metodo che restituisce la dimensione del buffer
	public int getBufferSize(){ return buffer.getBufferSize(); }

/**
* Return true if the trasmission form the server is started
* @return
*/
//metodo che informa sullo stato di playing, in realta' dovrebbe essere definito di trasmissione, del proxy. Dovra' essere cambiato quando implementero' lo stato del proxy credo
	public boolean isPlaying(){ return playing; }

//metodo che imposta il periodo del ciclo di prelevamento frame da parte del multiplexer
	public void setTimeToWait(int timeToWait){ muxThread.setTimeToWait(timeToWait); }
	
	public long getSequenceNumber(){
		System.out.println("Contatore: "+buffer.getStatusFrame());
		if (buffer.getStatusFrame() > 0) {
			return buffer.readFrame(buffer.getStatusFrame() - 1).getSequenceNumber();
		}
		else return buffer.readFrame(clientBufferSize - 1).getSequenceNumber();
	}
	
	//MODIFICA AMBRA
	public void bufferFullEventOccurred(BufferFullEvent e) {
		
		if (!bfSent) {
			//se non � stato gi� inviato, viene inviato al proxy un messaggio di buffer full
			controller.bufferControl();
			bfSent = true;
		}		
	}
//	MODIFICA AMBRA
	public void stopTransmission(){
		bfSent = true;
	}
	//MODIFICA AMBRA
	public void resumeTransmission(){
		bfSent = false;
	}
	
}//End of ClientChainBufferDataPlaying