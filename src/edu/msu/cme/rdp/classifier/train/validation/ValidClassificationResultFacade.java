/*
 * ClassificationResultFacade.java
 *
 * Created on May 11, 2007, 4:02 PM
 */
package edu.msu.cme.rdp.classifier.train.validation;

import edu.msu.cme.rdp.classifier.train.LineageSequence;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author  wangqion
 */
public class ValidClassificationResultFacade {

    private String seqName;
    private List<String> ancestors;
    private HierarchyTree labeledNode;  // the treenode it was labelled, may not in the training set
    private List<ValidationClassificationResult> rankAssignment;
    private String highestMissedRank = null;  // the highest rank that misclassified at

    /** Creates a new instance of ClassificationResultFacade */
    public ValidClassificationResultFacade(LineageSequence seq, List<ValidationClassificationResult> assignment) {
        this.seqName = seq.getSeqName();
        this.ancestors = seq.getAncestors();
        this.rankAssignment = assignment;
    }

    public String getSeqName() {
        return this.seqName;
    }

    public List<String> getAncestors() {
        return this.ancestors;
    }

    public List<ValidationClassificationResult> getRankAssignment() {
        return rankAssignment;
    }

    public boolean isMissed() {
        return (highestMissedRank != null);
    }

    public void setMissedRank(String missedRank) {
        highestMissedRank = missedRank;
    }

    public HierarchyTree getLabeledNode() {
        return labeledNode;
    }

    public void setLabeledNode(HierarchyTree n) {
        labeledNode = n;
    }

    /** print the labelled path and the assigned path
     * Note: the true path is list of string of ancestors for a sequence,
     * the assigned path is a list of ClassificationResult for a sequence.
     */
    public String getPath() throws IOException {
        StringBuilder str = new StringBuilder();
        str.append("SEQ: ").append(this.seqName).append("\t");
        if ( isMissed()){
            str.append(highestMissedRank);
        }
        str.append("\n");
        Iterator i = this.getAncestors().iterator();
        while (i.hasNext()) {
            str.append(i.next()).append("\t");
        }
        str.append("\n");

        i = this.getRankAssignment().iterator();
        while (i.hasNext()) {
            ValidationClassificationResult result = (ValidationClassificationResult) i.next();
            str.append(((HierarchyTree) result.getBestClass()).getName()).append("\t");

        }

        str.append("\n");
        i = this.getRankAssignment().iterator();
        while (i.hasNext()) {
            str.append(((ValidationClassificationResult) i.next()).getNumOfVotes()).append("\t");
        }
        return str.toString();
    }
}
