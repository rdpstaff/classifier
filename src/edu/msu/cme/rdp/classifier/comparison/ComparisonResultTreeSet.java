/*
 * ComparisonResultTreeSet.java
 *
 * Created on February 11, 2005, 1:22 PM
 */

package edu.msu.cme.rdp.classifier.comparison;

import java.util.TreeSet;
import java.util.Comparator;
/**
 *
 * @author  wangqion
 */
public class ComparisonResultTreeSet extends TreeSet{
    
    public ComparisonResultTreeSet(){
        super(new ComparisonResultComparator());        
    }
    
    public static class ComparisonResultComparator implements Comparator {
        
        public int compare( Object x, Object y ) {
            int retVal = 0;
            AbstractNode lhs = (AbstractNode) x;
            AbstractNode rhs = (AbstractNode) y;
            if ( lhs.getDoubleSignificance() == rhs.getDoubleSignificance() ) {
                retVal = lhs.getName().compareTo(rhs.getName() );
            } else {
                retVal = ( lhs.getDoubleSignificance() < rhs.getDoubleSignificance() ) ? -1 : 1 ;
            }
            return retVal;
        }
    }
}
