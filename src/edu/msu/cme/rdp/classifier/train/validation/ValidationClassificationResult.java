/*
 * ClassificationResult.java
 *
 * Created on June 25, 2002, 11:35 AM
 */
/**
 *
 * @author  wangqion
 * @version
 */
package edu.msu.cme.rdp.classifier.train.validation;

public class ValidationClassificationResult {

    private HierarchyTree bestClass;
    private double posteriorProb;
    private float numOfVotes;  // percent of voters which agrees with this node

    /** Creates new ClassificationResult */
    public ValidationClassificationResult(HierarchyTree t, float f, float vote) {
        bestClass = t;
        posteriorProb = f;
        numOfVotes = vote;
    }

    public ValidationClassificationResult(HierarchyTree t, float vote) {
        bestClass = t;
        numOfVotes = vote;
    }

    public HierarchyTree getBestClass() {
        return bestClass;
    }

    public float getNumOfVotes() {
        return numOfVotes;
    }

    public void setNumOfVotes(float f) {
        numOfVotes = f;
    }

    public void setPosteriorProb(double p) {
        posteriorProb = p;
    }

    public double getPosteriorProb() {
        return posteriorProb;
    }
}
