/*
 * ShortSequenceException.java
 * 
 * Copyright 2006 Michigan State University Board of Trustees
 *
 * Created on November 24, 2003, 5:05 PM
 */

package edu.msu.cme.rdp.classifier;

/**
 * Handles exception for a short sequence.
 * @author  wangqion
 * @version
 */
public class ShortSequenceException extends IllegalArgumentException{
  private String message;
  private String seqDoc;
  
  /** Creates a new instance of ShortSequenceException to handle
   * short sequence.
   */
  public ShortSequenceException(String seqDoc, String msg) {
    this.seqDoc = seqDoc;
    message = msg;
  }
  
  /** Returns the sequence identifier. 
   */
  public String getSeqDoc(){
    return seqDoc;
  }
  
  /** Returns the error message.
   * 
   */
  public String getMessage(){
    return message;
  }
  
  
}
