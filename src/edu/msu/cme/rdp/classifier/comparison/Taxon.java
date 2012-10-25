 /*
 * Taxon.java
 *
 * Created on November 10, 2003, 4:43 PM
 */

package edu.msu.cme.rdp.classifier.comparison;

/**
 *
 * @author  wangqion
 */
public interface Taxon {
    // the methods used by the JSP
    String getName();
    String getRank();
    int getTaxid();
    int getS1Count();
    int getS2Count();
    int getIndent();
    String getSignificance();
    
}
