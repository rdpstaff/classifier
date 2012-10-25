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

import java.util.Comparator;
import java.util.TreeSet;

/**
 *
 * @author wangqion
 */
public class SortedROCSet extends TreeSet< SortedROCSet.PredictionCount>{
    
    public SortedROCSet(){
        super(new ResultComparator());
    }
     
        
    public static class ResultComparator implements Comparator<PredictionCount>{
        public int compare(PredictionCount lhs, PredictionCount rhs){
            if ( Double.isNaN(lhs.fpr) || Double.isNaN(lhs.se) || Double.isNaN(rhs.fpr) || Double.isNaN(rhs.se))
                return 0;
            
            if ( lhs.fpr == rhs.fpr){
                if ( lhs.se == rhs.se){
                    return 0;
                }
                return (lhs.se > rhs.se ? 1: -1);
            }
            if ( lhs.fpr > rhs.fpr){
                return 1;
            }
            return -1;
        }
    }
    
    
    public static class PredictionCount{
        double se;
        double fpr;
        
        public PredictionCount(double f, double s){
            se = s;
            fpr = f;
        }
        
        public double getSe(){
            return se;
        }
        
        public double getFPR(){
            return fpr;
        }
    }
}
