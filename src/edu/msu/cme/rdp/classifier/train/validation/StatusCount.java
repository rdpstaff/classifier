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


/**
 *
 * @author wangqion
 */
public class StatusCount {
    private int numTP = 0;
    private int numFP = 0;
    private int numTN = 0;
    private int numFN = 0;

    public void incNumTP(int i){
        numTP += i;
    }

    public void incNumFP(int i){
        numFP += i;
    }

    public void incNumTN(int i){
        numTN += i;
    }

    public void incNumFN(int i){
        numFN += i;
    }

    public int getNumTP(){
        return numTP;
    }

    public int getNumTN(){
        return numTN;
    }

    public int getNumFP(){
        return numFP;
    }

    public int getNumFN(){
        return numFN;
    }

    /**
     * sensitivity = #TP / (#TP + #FN)
     */
    public double calSensitivity(){
        return (double)numTP / ( (double) (numTP +numFN));
    }

    /**
     * specificity = #TN / (#TN + #FP)
     * @return
     */
    public double calSpecificity(){
        return (double)numTN / ( (double) (numTN +numFP));
    }
    
    /**
     * false positive rate = #FP / (#TN + #FP)
     * @return
     */
    public double calFPR(){
        return (double)numFP / ( (double) (numTN +numFP));
    }
    
    /**
     * F1 score = 2#TP / (2#TP + #FP + #FN)
     * @return
     */
    public double calF1score(){
        return 2.0 * (double)numTP / ( (double) (2*numTP + +numFP + numFN));
    }
    
    
}
