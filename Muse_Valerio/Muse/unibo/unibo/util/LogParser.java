package unibo.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Parser del file di testo prodotto dallo script Solaris per il testing delle performance
 * dell'applicazione
 * @author Alessandro Falchi
 */
public class LogParser {
    
    public static void parseFile(String filePath) throws FileNotFoundException, IOException {
        String line, token;
        int lineCount=0;
        float cpuUsage,memUsage,totCpuUsage=0,totMemUsage=0,
        		maxCpuUsage=0,minCpuUsage=0,maxMemUsage=0,minMemUsage=0;

        BufferedReader sourceReader=new BufferedReader(new FileReader(filePath));
        line=sourceReader.readLine();
        do {
            lineCount++;
            StringTokenizer tokenizer=new StringTokenizer(line," ");
            tokenizer.nextToken();
            tokenizer.nextToken();
            tokenizer.nextToken();
            cpuUsage=Float.parseFloat(tokenizer.nextToken());
            totCpuUsage=totCpuUsage+cpuUsage;
            if (cpuUsage>maxCpuUsage) maxCpuUsage=cpuUsage;
            else if (cpuUsage<minCpuUsage) minCpuUsage=cpuUsage;
            
            memUsage=Float.parseFloat(tokenizer.nextToken());
            totMemUsage=totMemUsage+memUsage;
            if (memUsage>maxMemUsage) maxMemUsage=memUsage;
            else if (memUsage<minMemUsage) minMemUsage=memUsage;
            line=sourceReader.readLine();
        } while(line!=null);
        System.out.println(filePath);
        System.out.println("Uso CPU %: medio="+totCpuUsage/lineCount+" | max= "+maxCpuUsage+" | min="+minCpuUsage);
        System.out.println("Uso MEM %: medio="+totMemUsage/lineCount+" | max= "+maxMemUsage+" | min="+minMemUsage);
        System.out.println("Linee lette "+lineCount);
    }

    public static void main(String[] args) {
        try { LogParser.parseFile("C:\\afalchi\\Projects\\ClassicServerRU.log"); }
        catch (FileNotFoundException e) { System.err.println("File not found"); }
        catch (IOException e) { System.err.println("File reading error"); }
    }
}
