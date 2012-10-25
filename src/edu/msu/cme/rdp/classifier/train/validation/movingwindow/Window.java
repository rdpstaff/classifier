/*
 * Window.java
 *
 * Created on November 15, 2006, 10:57 AM
 */

package edu.msu.cme.rdp.classifier.train.validation.movingwindow;

/**
 *
 * @author  wangqion
 */
public class Window {
    private int start;
    private int stop;
    /** Creates a new instance of Window */
    public Window(int start, int stop) {
        this.start = start;
        this.stop = stop;
    }
    
    public int getStart(){
        return start;
    }
    
    public int getStop(){
        return stop;
    }
    
}
