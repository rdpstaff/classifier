/*
 * CorrectAssignment.java
 *
 * Created on August 25, 2003, 3:04 PM
 */
package edu.msu.cme.rdp.classifier.train.validation;

/**
 *
 * @author  wangqion
 */
public class CorrectAssignment {

    public final int bins = 11;  // bin range: 100-95, 94-90, 89-80, 79-70.....9-0
    public int[] numCorrect = new int[bins];  // number of correctly assignment for each bin range
    public int[] numTotal = new int[bins];   // number of total assignment for each bin range
    public float[] standardError = new float[bins];  // sqrt(p(1-p)/n), p = correct/total
    public float[] sumOfVotes = new float[bins];  // the sum of the percent of votes for each bin range

    /** Creates a new instance of CorrectAssignment */
    public CorrectAssignment() {
    }
}
