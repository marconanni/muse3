package unibo.core.transcoder;

import javax.media.Format;

import com.sun.media.ExtBuffer;


/**
 * @author Alessandro Falchi
 */
public interface Transcode {
    /**
     * <p>Valore che indica il successo nel completamento dell'operazione di processing dell'input.</p>
     * <p>L'output prodotto è da ritenersi valido.</p> 
     */
    public static final int TRANSCODE_OK=0;
    
    /**
     * <p>Valore che indica il parziale completamento dell'operazione di processing dell'input.</p>
     * <p>L'output prodotto è da ritenersi valido; tuttavia, è necessario eseguire nuovamente il processing del
     * medesimo input.</p> 
     */
    public static final int PROCESS_INPUT_AGAIN=2;

    
    /**
     * <p>Valore prodotto qualora l'output dell'operazione di processing non sia da ritenersi valido.</p>
     * <p>La successiva operazione di processing deve considerare un nuovo input.</p>
     */
    public static final int OUTPUT_NOT_VALID=3;

    /**
     * <p>Valore ritornato in caso la fase di processing fallisca.</p>
     * <p>L'output prodotto non è da ritenersi valido.</p>
     */
    public static final int PROCESS_FAILED=4;

    /**
     * <p>Valore ritornato in caso il componente che si occupa della transcodifica sia stato terminato.</p>
     * <p>Ogni successiva chiamata a processFrame non è in grado di produrre alcun risultato ed è necessario
     * istanziare un nuovo oggetto Transcode per poter eseguire nuove transcodifiche.</p> 
     */
    public static final int PLUGIN_TERMINATED=8;
    
    /**
     * Il metodo che esegue l'elaborazione per la transcodifica.
     * @param inputFrame il frame che si vuole processare 
     * @param outputFrame l'oggetto prodotto come risultato dell'elaborazione su inputFrame 
     * @return si vedano le costanti definite dall'interfaccia Transcode
     */
    public int processFrame(ExtBuffer inputFrame, ExtBuffer outputFrame);
    
    /**
     * Permette di conoscere il formato di output del transcoder, parametro necessario alla configurazione del
     * plugin eventualmente presente a valle.
     * @return un'istanza di format con la descrizione del formato prodotto in uscita elaborando i frame in ingresso
     */
    public Format getOutputFormat();
    
    /**
     * Chiude il transcoder. Dopo la chiamata a close(), il modulo non può più essere riutilizzato.
     */
    public void close();
}
