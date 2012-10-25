/*
 * SeqErrorBean.java
 *
 * Created on November 17, 2003, 2:56 PM
 */

package edu.msu.cme.rdp.classifier.comparison;

/**
 *
 * @author  siddiq15
 */
public class SeqErrorBean {
    
    private String description;
    private String message;
    
    /** Creates a new instance of SeqErrorBean */
    public SeqErrorBean(String desc, String message) {
        this.description = desc;
        this.message = message;
    }
    
    public String getDescription() {
        return this.description;
    }
    
    public String getMessage() {
        return this.message;
    }
}
