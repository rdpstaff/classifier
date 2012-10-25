/*
 * TrainingDataException.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 *
 * Created on November 13, 2003, 3:35 PM
 */

package edu.msu.cme.rdp.classifier;

/**
 * Handles the exception occurred when parsing training information.
 * @author  wangqion
 */
public class TrainingDataException extends java.lang.Exception {
  
  /**
   * Creates a new instance of TrainingDataException without detail message.
   */
  public TrainingDataException() {
  }
  
  
  /**
   * Constructs an instance of TrainingDataException with the specified detail message.
   */
  public TrainingDataException(String msg) {
    super(msg);
  }
  
  /**
   * Constructs an instance of TrainingDataException with the specified root cause.
   */
  public TrainingDataException(Throwable rootCause){
    super(rootCause);
  }
  
}
