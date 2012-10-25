/*
 * RawGenusWordConditionalProb.java
 * Copyright 2006 Michigan State University Board of Trustees
 * Created on September 8, 2003, 1:47 PM
 */

package edu.msu.cme.rdp.classifier.train;

/**
 * A RawGenusWordConditionalProb contains the index of the genus node and
 * the conditional probability of the node contains the word.
 * @author  wangqion
 */
public class RawGenusWordConditionalProb {
  private int genusIndex;
  private float wordConditionalProb;
  
  /** Creates a new instance of RawGenusWordConditionalProb. */
 public RawGenusWordConditionalProb(int index, float prob) {
    genusIndex = index;
    wordConditionalProb = prob;
  }
  
  /** Returns the index of the genus.
   */
 public  int getGenusIndex(){
    return genusIndex;
  }
  
  /** Returns the word conditional probability of the genus.
   */
  public float getProbability(){
    return wordConditionalProb;
  }
  
  
}
