package sv60122.dbgUtil;

import sv60122.vDevice.iVODevice;
/**
definisce la classe che gestisce i messaggi di debug
@version 1.0
@author  Sergio Valisi 60122
*/
public class DebugWriterVDev implements iDebugWriter{

	private iVODevice device=null;
	private int dbgLevel;
	private boolean chgEnable=true;
	
	public DebugWriterVDev(int dbgLevel){
		this.dbgLevel=dbgLevel;
	}
	public DebugWriterVDev(int dbgLevel, iVODevice vDevice){
		this.dbgLevel=dbgLevel;
		device=vDevice;
	}
	
	public void dbgWrite(int level, String dbgMessage){
		if(level<=0) return;
		if(level<=dbgLevel){
			if(device==null) System.err.println("[DeBugMsg] "+dbgMessage);
			else device.scriviStringa("[DeBugMsg] "+dbgMessage);
		}
	}
	
	public void setEnableChgLevel(boolean enable){ chgEnable=enable; }
	public boolean getEnableChgLevel(){ return chgEnable; }
	public void setDbgLevel(int dLevel){ 
		if(chgEnable) dbgLevel=dLevel; 
		else System.err.println("Non è possibile modificare il livello di message-debuggin");
	}
	public int getDbgLevel (){ return dbgLevel; }
}