package unibo.core.multiplexer;

import javax.media.protocol.DataSource;

import com.sun.media.ExtBuffer;

/**
 * Interfaccia con le signature dei metodi base di un multiplexer, ossia di un modulo che costituisce lo stadio
 * finale di una catena di plugin e che si occupa di convogliare i contenuti delle traccie fornitegli in input in un
 * unico flusso in output. Tale output è accessibile mediante l'istanza di DataSource ottenuta dall'invocazione di
 * getDataOutput(). 
 * @author Alessandro Falchi
 */
public interface Multiplex {
    /**
     * Valore ritornato dal metodo <code>multiplexFrame</code> nel caso l'operazione sia stata eseguita correttamente.
     */
    public static final int MULTIPLEX_OK=0;

    /**
     * Valore ritornato dal metodo <code>multiplexFrame</code> nel caso il plugin JMF usato internamente per il
     * multiplexing sia stato terminato, rendendo quindi impossibile portare a termine il processing dei frame.
     */
    public static final int PLUGIN_TERMINATED=8;

    /**
     * <p>Metodo per il multiplexing dei singoli frame.</p>
     * <p><b><i>Nota 1:</i></b> attraverso il metodo multiplexFrame <b>non</b> viene effettuata alcuna operazione in
     * merito alla regolazione del frame rate. Dunque, è necessario gestire opportunamente le chiamate a
     * multiplexFrame per evitare di inserire frame in modo continuo nello stream multiplexato, cosa che potrebbe
     * sovraccaricare il sistema e compromettere la riproduzione lato client. Ad esempio, è possibile inviare i frame
     * con cadenza pari al frame rate del contenuto considerato.</p>
     * <p><b><i>Nota 2:</i></b> nel caso l'istanza di ExtBuffer sia un frame contenente dati per la trasmissione
     * RTP, occorre verificare se il flag FLAG_RTP_MARKER è zero oppure no. Ad esempio, la condizione:
     * <ul><li>(frame.getFlags() & Buffer.FLAG_RTP_MARKER)==0) ...</ul><p>
     * è vera se il flag è resettato (cioè vale zero). Se FLAG_RTP_MARKER è zero significa che il frame è in realtà
     * un subframe. Per non compromettere la fluidità della riproduzione a valle, è necessario inviare tutti i
     * subframe senza interporre pause legate al framerate. La sequenza dei subframe che compongono uno stesso frame
     * si chiude con il primo frame che ha FLAG_RTP_MARKER settato ad 1 (anzichè zero come i precedenti).</p>
     * @param frame il frame che si vuole inserire nel flusso da inoltrare
     * @param trackNumber il numero della traccia a cui appartiene il frame
     * @return MULTIPLEX_OK se il multiplexing dei frame è andato a buon fine, PLUGIN_TERMINATED in caso il plugin
     * interno che si occupa del multiplexing sia terminato
     */
    public int multiplexFrame(ExtBuffer frame, int trackNumber); 
    
    /**
     * Metodo per accedere al DataSource necessario per passare l'output del multiplexer ad un altro componente di
     * JMF. Infatti, il multiplexer è lo stadio finale di una catena di plugin e non è previsto che vi siano connessi
     * direttamente altri plugin. Pertanto, per passare l'output del multiplexer ad un'altra catena o ad un componente
     * per la trasmissione RTP, è vincolante il passaggio per il DataSource fornito dal presente metodo. 
     * @return il DataSource che permette di accedere all'output del multiplexer
     */
    public DataSource getDataOutput();

    /**
     * Chiude il multiplexer. Una volta invocato il metodo close(), non sarà più possibile riattivare il componente.
     */
    public void close();
}
