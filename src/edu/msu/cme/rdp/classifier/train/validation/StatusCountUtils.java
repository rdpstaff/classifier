/*
 * Copyright (C) 2012 Michigan State University <rdpstaff at msu.edu>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.msu.cme.rdp.classifier.train.validation;

import java.util.ArrayList;
import java.util.HashMap;


/**
 *
 * @author wangqion
 */
public class StatusCountUtils {
    
      /** return the ROC matrix, for each rank, each bootstrap value
      * sensitivity = #TP / (#TP + #FN)
      * specificity = #TN / (#TN + #FP)
      * @param statusCountList
      **/
    public static String calROCMatrix(String label, ArrayList<HashMap<String, StatusCount>> statusCountList){
        StringBuilder matrix = new StringBuilder();
        matrix.append("label\t" + label+"\n");
        matrix.append("bootstrap");
        HashMap<String, StatusCount> statusCountMap = statusCountList.get(0);
        for ( String rank: statusCountMap.keySet()){
            if ( rank.startsWith("sub")) continue;
            matrix.append("\t" + rank + "_FPR" +"\t" + rank + "_TPR" + "\t" + rank + "_F1score" );
        }
        matrix.append("\n");

        for( int b = 0; b < statusCountList.size(); b++){
            matrix.append(b );
            statusCountMap = statusCountList.get(b);
            for ( String rank: statusCountMap.keySet()){
                if ( rank.startsWith("sub")) continue;
                StatusCount st = statusCountMap.get(rank);
                matrix.append("\t" + st.calFPR() + "\t" + st.calSensitivity() + "\t" + st.calF1score() );
            }
            matrix.append("\n");
        }

        return matrix.toString();
     }
    
    /**
      * calculate Area under curve 
      * 
      * sensitivity = #TP / (#TP + #FN)
      * specificity = #TN / (#TN + #FP)
      * @param statusCountList
      **/
    public static String calAUC(String label, ArrayList<HashMap<String, StatusCount>> statusCountList){
        StringBuilder matrix = new StringBuilder();
        HashMap<String, StatusCount> statusCountMap = statusCountList.get(0);
        matrix.append("AUC");
        for ( String rank: statusCountMap.keySet()){
            if ( rank.startsWith("sub")) continue;
            matrix.append("\t" + rank );
        }
        matrix.append("\n");
        matrix.append("label_" + label);
         
        for ( String rank: statusCountMap.keySet()){
            if ( rank.startsWith("sub")) continue;
            SortedROCSet sortedSet = new SortedROCSet();
            sortedSet.add( new SortedROCSet.PredictionCount(0.0, 0.0) ); // if the point don't go all the way to the beginning
            sortedSet.add( new SortedROCSet.PredictionCount(1.0, 1.0) ); // if the point don't go all the way to the end
            // get all the values, sort by FPR
            for( int b = 0; b < statusCountList.size(); b++){
                StatusCount st = statusCountList.get(b).get(rank);
                double se = st.calSensitivity();
                double fpr = st.calFPR();
                
                sortedSet.add( new SortedROCSet.PredictionCount(fpr, se));
            }
                      
            double area = 0;
            Object[] pcArray =  sortedSet.toArray();
            for (int i = 0; i < pcArray.length -1 ; i++){
                SortedROCSet.PredictionCount lpc = (SortedROCSet.PredictionCount) pcArray[i];
                SortedROCSet.PredictionCount hpc = (SortedROCSet.PredictionCount) pcArray[i+1];
                //System.err.println("pcArray\t" + lpc.fpr + "\t" + lpc.se);
                double trapezoid = 0.5 * Math.abs( ( hpc.fpr - lpc.fpr ) * ( hpc.se + lpc.se ) );
                if ( !Double.isNaN(trapezoid)){
                    area += trapezoid;
                }
            }
            matrix.append("\t" + area );
        }
        matrix.append("\n");

        return matrix.toString();
     }
    
    
    
}
