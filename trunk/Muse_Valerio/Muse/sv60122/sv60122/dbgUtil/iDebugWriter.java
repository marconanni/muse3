package sv60122.dbgUtil;

/**
definisce l'interfaccia della classe che gestisce i messaggi di debug
@version 1.0
@author  Sergio Valisi 60122
*/
public interface iDebugWriter {

	public void dbgWrite(int dbgLevel, String dbgMessage);
	
	public void setEnableChgLevel(boolean chgLevel);
	public boolean getEnableChgLevel();
	public void setDbgLevel(int dbgLevel);
	public int getDbgLevel ();
}