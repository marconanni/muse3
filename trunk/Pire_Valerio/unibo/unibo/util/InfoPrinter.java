package unibo.util;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.protocol.DataSource;

import com.ibm.media.codec.video.VideoCodec;

/**
 * @author Alessandro Falchi
 */
public class InfoPrinter {
    public static void codecInfo(VideoCodec codec) {
        Format[] list;

        System.out.println("--== "+codec.getName().toUpperCase()+" ==--");
        list=codec.getSupportedInputFormats();
        System.out.println("* Supported Input Formats");
        for (int i=0; i<list.length; i++) {
            System.out.println("\t"+i+" - "+list[i]);
            System.out.println("    "+list[i].getClass().getName());
        }
        list=codec.getSupportedOutputFormats(null);
        System.out.println("\n* Supported Output Formats");
        for (int i=0; i<list.length; i++) {
            System.out.println("\t"+i+" - "+list[i]);
            System.out.println("    "+list[i].getClass().getName());
        }
    }
    
    public static void frameInfo(Buffer b) {
        System.out.println("FRAME #"+b.getSequenceNumber());
        System.out.println("format="+b.getFormat());
        System.out.print("flags="+b.getFlags());
    }
    
    public static void dataSourceInfo(DataSource ds) {
        System.out.println("Instance of: "+ds.getClass().getName());
        System.out.println("Locator: "+ds.getLocator());
        System.out.println("Content Type: "+ds.getContentType());
    }

}
