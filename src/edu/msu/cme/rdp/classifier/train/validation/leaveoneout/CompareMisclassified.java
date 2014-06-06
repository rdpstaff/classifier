/*
 * Copyright (C) 2014 wangqion
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package edu.msu.cme.rdp.classifier.train.validation.leaveoneout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author wangqion
 */
public class CompareMisclassified {
    private HashMap<String, String> misSeqMap = new HashMap<String, String>();
    private String testrank = null; 
    private int totalMissed = 0;
    
    public CompareMisclassified(String lootFile, String distFile)throws IOException{
        readLootResult(lootFile);
        readTaxonDistance(distFile);
    }
    
    private void readLootResult(String lootFile) throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(new File(lootFile)));
        String line = null;               
        boolean add = false;
        boolean singleton = false;
        while ( (line=reader.readLine()) != null){
            if ( line.startsWith("test rank")){
                testrank = line.split(":")[1].trim();
                break;
            }
        }
        while ( (line=reader.readLine()) != null){
            if ( line.startsWith("**misclassified sequences")){
                add = true;
                continue;
            }else if ( line.startsWith("**singleton sequences")){
                 add = false;
                 singleton = true;
                 continue;
            }
            
            if ( !line.startsWith("SEQ")) continue;          
            
            String[] values = line.split("\\s+");
            if ( add){
                line = reader.readLine().trim();
                String[] lineage = line.split("\\t");
                misSeqMap.put(values[1], lineage[lineage.length-2]); // save the genus
                //System.err.println(lineage[lineage.length-2]);
                totalMissed ++;
            } else {
                if ( singleton){                    
                    misSeqMap.remove(values[1]);
                }
            }
        }
        reader.close();
        
    }
    
    
    public void readTaxonDistance(String distFile)throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(new File(distFile)));
        String line = null;    
        int closerOutgrp =  0;
        int closerInGrp = 0;
        int equalGrp = 0;
        while ( (line=reader.readLine()) != null){
            if ( line.startsWith("#")){               
                break;
            }
            String[] values = line.split("\t");
            int ingrpSab = Integer.parseInt(values[1]);
            int outgrpinSab = Integer.parseInt(values[2]);
            
            if ( misSeqMap.containsKey(values[0])){
                //System.err.println(line + "\t" + misSeqMap.get(values[0]));
                if (ingrpSab > outgrpinSab){
                    closerInGrp ++;                    
                }else if (ingrpSab == outgrpinSab){
                    equalGrp ++;
                }else {
                    closerOutgrp++; 
                }
            }
        }
        reader.close();
        System.out.println("totalMissed\t"  + "non-singleton\t"+  "closerOutgrp\t" + "closerInGrp\t" + "equalGrp");
        System.out.println(totalMissed + "\t" +  misSeqMap.size() + "\t" + closerOutgrp + "\t" + closerInGrp + "\t" + equalGrp);
    }
    
    public static void main (String[] args ) throws IOException{
        CompareMisclassified theobj = new CompareMisclassified(args[0], args[1]);
    }
    
}
