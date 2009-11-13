/*
 * Created on Sep 10, 2004
 */
package javax.media.pim;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.media.Format;


/**
 * @author afalchi
 */
public class Test {

    public static void main(String[] args) {

//	    String classpath = System.getProperty("java.class.path");
//	    String userhome = System.getProperty("user.home");
//	    System.out.println("CP="+classpath+"  UH:"+userhome);


        // CREO UNA HASHTABLE CON LE PROPERTIES DI JMF
        
        String jmfdir="c:\\java\\JMF21~1.1E\\lib"; // vedi C:\WINDOWS\JAVA\.jmfdir
	    File file = new File(jmfdir+"\\jmf.properties");
	    InputStream registryInputStream=null;
	    Hashtable properties = new Hashtable();

		try {
	        registryInputStream=new FileInputStream(file.getPath());
		    ObjectInputStream ois = new ObjectInputStream(registryInputStream);
		    

		    // NB: queste due righe vanno lasciate anche se non si fa uso delle variabili int, poichè altrimenti
		    // la lettura seriale del file non avviene correttamente.
		    int tableSize = ois.readInt();
		    int version = ois.readInt();
		    // System.out.println("tablesize="+tableSize+"  version="+version);

		    try {
			    for (int i = 0; i < tableSize; i++) {
			        String key = ois.readUTF();
//			        System.out.println("Chiave letta dal file: "+key);
		            Object value = ois.readObject();
//		            System.out.println("Valore letto dal file: "+value.toString());
		            properties.put(key, value);
			    }
	        }
		    catch (ClassNotFoundException cnfe) { System.out.println("class not found"); }
		    ois.close();
		    registryInputStream.close();
		}
	    catch (FileNotFoundException e) {
	        System.err.println(file.getPath()+" not found");
	    }
		catch (IOException ioe) {
		    System.err.println("IOException in readRegistry: " + ioe);
		}
     
		// SCANSIONE DELLA TABELLA ALLA RICERCA DEI PLUGIN
		
		// la proprietà di chiave "PIM.lists" corrisponde ad una hashtable contenente 5 liste di plugin,
		// corrispondenti alle 5 diverse tipologie (mux, demux, codec...)

		Hashtable pluginLists=(Hashtable)properties.get("PIM.lists");

		/* la hashtable associa ad una chiave di classe Integer un'istanza di ListInfo, descrittore relativo ai
		 * plugin corrispondenti al valore numerico di Integer specificato
		 * 
		 * DEMULTIPLEXER = 1
		 * CODEC = 2
		 * EFFECT = 3
		 * RENDERER = 4
		 * MULTIPLEXER = 5
		 * 
		 * ListInfo contiene un campo int con il valore conforme alla lista qui sopra ed un Vector di oggetti
		 * ClassNameInfo, altro descrittore che contiene il nome della classe del plugin e un codice hash 
		 */
		
	    ListInfo muxList = (ListInfo) pluginLists.get(new Integer(5));
		Vector classNames = muxList.classNames;
		Enumeration eClassNames = classNames.elements();
		while (eClassNames.hasMoreElements()) {
		    ClassNameInfo cni = (ClassNameInfo) eClassNames.nextElement();
			System.out.println("******* NOME: "+cni.className);
			String key="PIM.5_"+cni.className+".in";
			Format[] inFormats=(Format[])properties.get(key);
			key="PIM.5_"+cni.className+".out";
			Format[] outFormats=(Format[])properties.get(key);
			System.out.println("\tn. Formati di input: "+inFormats.length);
			for (int i=0; i<inFormats.length; i++) {
			    System.out.print("\tType:"+inFormats[i].getDataType().getName());
			    System.out.println(" - Encoding:"+inFormats[i].getEncoding());
			}
			System.out.println("\tn. Formati di output: "+outFormats.length);
			for (int i=0; i<outFormats.length; i++) {
			    System.out.print("\tType required:"+outFormats[i].getDataType().getName());
			    System.out.println(" - Encoding:"+outFormats[i].getEncoding());
			}
		}
    }
}
