/*
 * Ztable.java
 *
 * Created on April 12, 2005, 3:09 PM
 */

package edu.msu.cme.rdp.classifier.comparison;

import java.util.Properties;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.text.DecimalFormat;


/**
 *
 * @author  wangqion
 */
public class Ztable {
    
	/**The default data directory, relative to the classpath. */
    private static String ztable_dataDir = "/data/classifier/ztable.txt";
   
    private static DecimalFormat df = new DecimalFormat("0.00");
    private Hashtable zHash = null;
    private static final double MAX_Z = 7.5;
    private static final double DELIMITER_Z = 4.0;
   
    /** Creates a new instance of Ztable */
    public Ztable(){
        if ( zHash == null){
            zHash = new Hashtable();   
            InputStreamReader in = new InputStreamReader(Ztable.class.getResourceAsStream(ztable_dataDir));            
           
            readZtable(in);
           
        }
    }
    
    
   /* Reads in the z tables
    *  if z < 4.0, the key is #.##
    *  if z >= 4.0, the key is #.#0
    */
    private void readZtable(InputStreamReader in) {
        try{
            BufferedReader reader = new BufferedReader(in);
            String line = "";
                        
            while ( (line = reader.readLine()) != null){
                StringTokenizer tokenizer = new StringTokenizer(line);
                if ( tokenizer.countTokens() == 0){
                    continue;
                }
                String initial_value = tokenizer.nextToken();
               
                int k = 0;
                while( tokenizer.hasMoreTokens()){
                    double orig = Double.parseDouble(tokenizer.nextToken());
                    double pvalue = 2.0 * (1.0 - orig);                       
                    zHash.put( initial_value + k, new Double(pvalue));
                    k++;
                }                
            }
            
        } catch(IOException ex){
            throw new RuntimeException(ex);
        }
       
    }
    
    
    /* max z is 7.5.
     * if z < 4.0, the key is #.##
     * if z >= 4.0, the key is #.#0
    */     
    public double getPvalue(double z){
        z = Math.abs(z);
        if ( z > MAX_Z){
            z = MAX_Z;
        }
        
        String key = (df.format(z)).toString();
        if ( z >= DELIMITER_Z){
            key = key.substring(0, 3) + "0";
        }                
        return ((Double)zHash.get(key)).doubleValue();
    }
    
    
}
