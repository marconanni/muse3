package unibo.core.parser;

import javax.media.Format;
import javax.media.Track;

import com.sun.media.ExtBuffer;

/**
 * <p>La classe astratta Parser specifica i metodi caratteristici di un oggetto per il demultiplexing delle diverse
 * tracce che fanno parte del contenuto multimediale trattato e l'estrazione singoli frame da ciascuna di esse.
 * Si è scelto di ricorrere ad una classe astratta (e non ad un'interfaccia) per poter già implementare quei metodi
 * comuni a tutti i tipi di parser, indipendentemente dal formato del contenuto trattato.</p>
 * <p>Il contenuto multimediale di cui si vuole eseguire il parsing viene specificato attraverso un'istanza di
 * DataSource; tuttavia, tra i metodi elencati non ne è presente alcuno che permetta di svolgere tale funzione,
 * lasciando la scelta alla sottoclasse. Ciò perchè può avere senso, anzichè prevedere un metodo ad hoc, specificare
 * il DataSource come parametro del costruttore, semplificando così il protocollo di inizializzazione dell'oggetto
 * (si dichiara implicitamente che è necessario specificare l'oggetto DataSource da cui prelevare i dati).
 * Inoltre, poiché JMF presenta alcuni limiti nella variazione dinamica della sorgente dei dati da elaborare,
 * si evidenzia che, usando i DataSource previsti dal framework, per passare da un tipo di dati ad un altro bisogna
 * creare una nuova istanza per il nuovo DataSource.</p>
 * @author Alessandro Falchi
 */
public abstract class Parser {
    protected int numberOfTracks;
    protected Track[] trackList;

    /**
     * @return il numero delle tracce di cui è formato il contenuto multimediale in ingresso al parser
     */
    public int getNumberOfTracks() {
        return numberOfTracks;
    }

    /**
     * @return un array di oggetti Format. Ogni elemento descrive il formato della traccia il cui indice
     * corrisponde a quello dell'elemento considerato
     */
    public Format[] getTracksFormat() {
        Format[] array=new Format[numberOfTracks];
        for (int i=0; i<numberOfTracks; i++) {
            array[i]=trackList[i].getFormat();
        }
        return array;
    }

    /**
     * @param trackNumber il numero della traccia di cui si vuole conoscere il formato
     * @return un oggetto di classe Format che descrive il formato della traccia identificata da trackNumber
     */
    public Format getTrackFormat(int trackNumber) {
        return trackList[trackNumber].getFormat();
    }
    
    /**
     * Esegue l'operazione di estrazione dei frame da una traccia del contenuto multimediale trattato dall'istanza di
     * Parser.
     * @param trackNumber l'indice della traccia dalla quale si vuole estrarre il frame
     * @return un oggetto di classe ExtBuffer contenente i dati sul frame letto o null se la traccia non è abilitata
     */
    public abstract ExtBuffer getFrame(int trackNumber);
    
    /**
     * Verifica l'attributo boolean enabled della traccia specificata.
     * @param trackNumber in numero della traccia di cui si vuole verificare l'abilitazione
     * @return true se la traccia è abilitata, false in caso contrario
     */
    public boolean isEnabled(int trackNumber) {
        return trackList[trackNumber].isEnabled();
    }
    
    /**
     * <p>Metodo per abilitare o disabilitare l'attributo "enabled" alle singole tracce che compongono il contenuto
     * multimediale trattato.</p>
     * <p>Nota: la variazione dello stato di abilitazione della traccia non ha alcuna ripercussione implicita sulla
     * elaborazione eseguita dagli stadi della catena "custom". Pertanto, l'implementazione del metodo ExtBuffer
     * deve esplicitamente prevedere la verifica dell'attributo "enabled" delle tracce.</p>
     * <p>Per la stessa ragione, occorre prestare attenzione ai casi in cui il contenuto multimediale sia trattato da
     * classi JMF create secondo le modalità previste dal framework: l'abilitazione o meno delle tracce può produrre
     * effetti che dipendono dalla specifica situazione.</p>
     * @param enabled il flag boolean il cui valore specifica se abilitare (true) o disabilitare (false) la traccia 
     * @param trackNumber il numero della traccia a cui applicare il flag
     */
    public void setEnabled(boolean enabled, int trackNumber) {
        trackList[trackNumber].setEnabled(enabled);
    }
    
    /**
     * Chiude il parser. Dopo la chiamata a close(), il parser non può più essere riutilizzato.
     */
    public abstract void close();
}
