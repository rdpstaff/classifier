/*
 * GenusWordConditionalProb.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 * 
 * Created on September 8, 2003, 1:47 PM
 */
package edu.msu.cme.rdp.classifier;

/**
 * A GenusWordConditionalProb holds the index of the genus node,
 * and the conditional probability that genus node contains the word.
 * @author  wangqion
 */
public class GenusWordConditionalProb {

    private int genusIndex;
    private float wordConditionalProb;

    /** Creates a new instance of GenusWordConditionalProb 
     * For a word indexed in wordConditionalProbIndexArr, it holds the index of the genus node,
     * and the conditional probability that genus node contains the word.
     */
    public GenusWordConditionalProb(int index, float prob) {
        genusIndex = index;
        wordConditionalProb = prob;
    }

    /** Returns the index of the genus node
     */
    public int getGenusIndex() {
        return genusIndex;
    }

    /** Returns the conditional probability that the genus node contains the word.
     */
    public float getProbability() {
        return wordConditionalProb;
    }
}
