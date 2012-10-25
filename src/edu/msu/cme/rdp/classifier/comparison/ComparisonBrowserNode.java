/*
 * ComparisonBrowserNode.java
 *
 * Created on November 17, 2003, 4:11 PM
 */

package edu.msu.cme.rdp.classifier.comparison;
import java.util.Iterator;

/**
 *
 * @author  wangqion
 */
public interface ComparisonBrowserNode  {     
    // the methods used by the ComparisonBrowserBean
    void changeConfidence(SigCalculator cal);
    ComparisonBrowserNode findNode(int id);   
    Iterator getTaxonIterator(int depth);
    Iterator getLineageIterator();
    Iterator getDetailIterator(float conf);
}
