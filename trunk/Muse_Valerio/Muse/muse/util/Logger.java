package util;
/**
 * Logger.java
 * Classe contenente metodi statici per la scrittura di un file di log
 * @author Ambra Montecchia
 * @version 1.0
 * */
import java.io.*;

public class Logger {
	
	//File di log
	private static BufferedWriter bw = null;
	
	/**
	 * Metodo per la registrazione di un evento nel file di log
	 * @param line - Stringa relativa all'evento
	 * */
	public static void write(String line){
		
		if(bw == null){
			try{
				bw = new BufferedWriter(new FileWriter("log.txt"));
			}
			catch(Exception ex){
				System.out.println("Impossibile creare file di log: "+ex.getMessage());
			}
		}
		
		try{
			bw.write(line+"\n");
		}
		catch(Exception e){
			System.out.println("Impossibile scrivere nel file di log: "+e.getMessage());
		}
	}
	
	/**
	 * Metodo per la chiusura del file di log
	 **/
	public static void close(){
		
		try{
			bw.close();
		}
		catch(Exception ex){
			System.out.println("Impossibile chiudere file di log: "+ex.getMessage());
		}
	}
	
}
