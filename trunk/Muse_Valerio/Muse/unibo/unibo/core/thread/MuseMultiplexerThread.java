package unibo.core.thread;

import sv60122.dbgUtil.iDebugWriter;
import unibo.core.CircularBuffer;
import unibo.core.multiplexer.Multiplex;

public class MuseMultiplexerThread extends MultiplexerThreadPS {

	/**
	 * Costruttori
	 * */
	public MuseMultiplexerThread(Multiplex multiplexer, CircularBuffer[] buffer, iMultiplexerSupendedListener muxSusp, long id) throws Exception{
		super(multiplexer, buffer, muxSusp, id);
	}
	
	public MuseMultiplexerThread(iDebugWriter debugger, Multiplex multiplexer, CircularBuffer[] buffer, iMultiplexerSupendedListener muxSusp, long id) throws Exception{
		super(debugger, multiplexer, buffer, muxSusp, id);
	}
	
	public MuseMultiplexerThread(Multiplex multiplexer, CircularBuffer[] buffer, iMultiplexerSupendedListener muxSusp, String address) throws Exception{
		super(multiplexer, buffer, muxSusp, address);
	}
	
	/**
	 * Metodo per il rewind
	 * */
	public void rewind(long timeStamp){
		this.bufferSet[0].rewind(timeStamp);
	}
	
	/**
	 * Metodo per il rewind basato su sequence number
	 * */
	public void rewindSN(long sequenceNumber){
		this.bufferSet[0].rewind(sequenceNumber);
	}
}
