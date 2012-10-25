/*
 * Tester.java
 *
 * Created on June 24, 2002, 6:26 PM
 */
/**
 *
 * @author  wangqion
 * @version
 */
package edu.msu.cme.rdp.classifier.train.validation.leaveoneout;

import java.util.*;
import java.io.*;
import java.text.*;

import edu.msu.cme.rdp.classifier.train.validation.ValidationClassificationResult;
import edu.msu.cme.rdp.classifier.train.validation.ValidClassificationResultFacade;
import edu.msu.cme.rdp.classifier.train.validation.CorrectAssignment;
import edu.msu.cme.rdp.classifier.train.validation.DecisionMaker;
import edu.msu.cme.rdp.classifier.train.GoodWordIterator;
import edu.msu.cme.rdp.classifier.train.LineageSequence;
import edu.msu.cme.rdp.classifier.train.LineageSequenceParser;
import edu.msu.cme.rdp.classifier.train.validation.*;

/** */
public class LeaveOneOutTester {

    BufferedWriter outFile;
    private Map<String, CorrectAssignment> num_hierLevel = new HashMap<String, CorrectAssignment>();  // key is the hierarchy level, value is CorrectAssignment
    private String testRank = Taxonomy.GENUS;
    private int numGoodBases = 0;  // 0 indicates all the bases from the query sequence

    /**
     * 
     * @param writer
     * @param numGoodBases 0 for all bases from the query, positive integer for partial length testing
     * @throws IOException
     */
    public LeaveOneOutTester(Writer writer, int numGoodBases) throws IOException {
        outFile = new BufferedWriter(writer);
        this.numGoodBases = numGoodBases;
    }

    /**
     * perform leave-one-out test. For each query sequence, remove it from the training set and test the classifier,
     * put it back after each testing.
     * 
     * The accuracy, misclassified sequences and the misclassified count for each taxon is written into the output file.
     * @param factory
     * @param parser
     * @param useSeed
     * @throws IOException
     */
    public ArrayList<HashMap<String, StatusCount>> classify(TreeFactory factory, LineageSequenceParser parser, boolean useSeed, int min_bootstrap_words)
            throws IOException {

        testRank = factory.getLowestRank();
        List<ValidClassificationResultFacade> resultList = new ArrayList<ValidClassificationResultFacade>();
        HashMap<String, HierarchyTree> nodeMap = new HashMap<String, HierarchyTree>();

        DecisionMaker dm = new DecisionMaker(factory);
        HierarchyTree root = factory.getRoot();
        root.getNodeMap(testRank, nodeMap);

        if (nodeMap.isEmpty()) {
            throw new IllegalArgumentException("\nThere is no node in " + testRank + " level!");
        }

       
        ArrayList<HashMap<String, StatusCount>> statusCountList = new ArrayList<HashMap<String, StatusCount>>();
        // initialize a list of statusCount, one for each bootstrap from 0 to 100
        for ( int b = 0; b <= 100; b++){
             HashMap<String, StatusCount> statusCountMap = new HashMap<String, StatusCount>();
             statusCountList.add(statusCountMap);
             for (String rank: factory.getRankSet()){
                 statusCountMap.put(rank, new StatusCount());
             }
         }
        
        
        int i = 0;
        while (parser.hasNext()) {
            LineageSequence pSeq = parser.next();
            if (pSeq.getSeqString().length() == 0) {
                continue;
            }
            GoodWordIterator wordIterator = null;
            if (numGoodBases > 0) {
                wordIterator = pSeq.getPartialSeqIteratorbyGoodBases(numGoodBases);  // test partial sequences with good words only

            } else {
                wordIterator = new GoodWordIterator(pSeq.getSeqString()); // full sequence  
            }

            if (wordIterator == null || wordIterator.getNumofWords() == 0) {
                //System.err.println(pSeq.getSeqName() + " unable to find good subsequence with length " + numGoodBases);
                continue;
            }

            //for leave-one-out testing, we need to remove the word occurrence for
            //the current sequence. This is similar to hide a sequence leaf.
            HierarchyTree curTree = nodeMap.get((String) pSeq.getAncestors().get(pSeq.getAncestors().size() - 1));
            curTree.hideSeq(wordIterator);
            //
            if ( curTree.isSingleton()){
                nodeMap.remove((String) pSeq.getAncestors().get(pSeq.getAncestors().size() - 1));
            }
            List<ValidationClassificationResult> result = dm.getBestClasspath( wordIterator, nodeMap, useSeed, min_bootstrap_words);

            ValidClassificationResultFacade resultFacade = new ValidClassificationResultFacade(pSeq, result);
            resultFacade.setLabeledNode(curTree);
            compareClassificationResult(resultFacade);

            // for ROC curve
            labelAssignmentStatus(factory, resultFacade, statusCountList);
          
            resultList.add(resultFacade);

            //System.out.print(i + " ");
            i++;
            // recover the wordOccurrence of the genus node, unhide the sequence leaf
            curTree.unhideSeq(wordIterator);
            if ( curTree.isSingleton()){
                nodeMap.put((String) pSeq.getAncestors().get(pSeq.getAncestors().size() - 1), curTree);
            }
        }


        displayClassification(resultList);
        //
        outFile.write("\nRank" + "\t" + "Name" + "\t" + "Total Seqs" + "\t" + "Tested Seqs (non-singleton)" + "\t" + "misClassified" +"\t" + "pct misclassified"+ "\n");

        displayTreeErrorRate(root, 0);
        
        System.err.println( StatusCountUtils.calROCMatrix(Integer.toString(min_bootstrap_words), statusCountList));
        System.err.println( StatusCountUtils.calAUC(Integer.toString(min_bootstrap_words), statusCountList));

        outFile.close();
        return statusCountList;
    }


    public void displayTreeErrorRate(HierarchyTree root, int indent) throws IOException {

        double pct_miss = 0;
        if ( root.getNumTotalTestedseq() > 0){
            pct_miss = (double)root.getMissCount()/ (double)root.getNumTotalTestedseq();
        }
        outFile.write(root.getTaxonomy().getHierLevel() + "\t" + root.getName() + "\t" + root.getTotalSeqs() 
                + "\t" + root.getNumTotalTestedseq() + "\t" + root.getMissCount() + "\t" + pct_miss + "\n");
        Iterator i = root.getSubclasses().iterator();

        while (i.hasNext()) {
            displayTreeErrorRate((HierarchyTree) i.next(), indent + 1);
        }
    }

    /** Displays the results of the classification, true path, assigned path and the
     * error rate
     */
    private void displayClassification(List seqs) throws IOException {
        int i = 0;

        DecimalFormat df = new DecimalFormat("0.###");

        outFile.write("\n\n**The statistics for each hierarchy level: \n 1: number of correct assigned sequences / number of total sequences for that bin rage\n");
        outFile.write("Level\t100-95\t\t94-90");
        for (i = 9; i > 0; i--) {
            outFile.write("\t\t" + (i * 10 - 1) + "-" + (i - 1) * 10);
        }

        outFile.write("\t\t" + "Correct Assigned\tTotal Seqs\tCorrect/Total" + "\n");

        Iterator it = num_hierLevel.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            outFile.write(name + "\t");
            CorrectAssignment assign = (CorrectAssignment) num_hierLevel.get(name);

            calStandardError(assign);
            int total = 0;
            int correct = 0;
            for (i = assign.bins - 1; i >= 0; i--) {
                outFile.write(assign.numCorrect[i] + "\t" + assign.numTotal[i] + "\t");
                total += assign.numTotal[i];
                correct += assign.numCorrect[i];
            }
            if (total > 0) {
                outFile.write(correct + "\t" + total + "\t" + (double) correct / ((double) total) + "\t");
            } else {
                outFile.write(correct + "\t" + total + "\t" + 0 + "\t");
            }
            outFile.write("\n");
        }
                

        outFile.write("\n\n** 2. The average votes for each bin range \n");
        it = num_hierLevel.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            outFile.write(name + " \t ");
            CorrectAssignment assign = (CorrectAssignment) num_hierLevel.get(name);

            for (i = assign.bins - 1; i >= 0; i--) {
                if (assign.numTotal[i] == 0) {
                    outFile.write("0\t");
                } else {
                    outFile.write(df.format(assign.sumOfVotes[i] * 100 / assign.numTotal[i]) + "\t");
                }
            }
            outFile.write(" \n");
        }

        outFile.write("\n\n** 3. The percentage of correctness for each bin range (the percentage of #1)\n");
        it = num_hierLevel.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            outFile.write(name + " \t ");
            CorrectAssignment assign = (CorrectAssignment) num_hierLevel.get(name);

            for (i = assign.bins - 1; i >= 0; i--) {
                if (assign.numTotal[i] == 0) {
                    outFile.write("0\t");
                } else {
                    outFile.write(df.format((float) assign.numCorrect[i] / (float) assign.numTotal[i]) + "\t");
                }
            }
            outFile.write(" \n");
        }

        outFile.write("\n\n** 4. The standard error for each bin range \n");

        it = num_hierLevel.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            outFile.write(name + " \t ");
            CorrectAssignment assign = (CorrectAssignment) num_hierLevel.get(name);

            for (i = assign.bins - 1; i >= 0; i--) {
                outFile.write(df.format(assign.standardError[i]) + "\t");
            }
            outFile.write(" \n");

        }

        outFile.write("\n missclassified sequences: \n");
        for (i = 0; i < seqs.size(); i++) {
            ValidClassificationResultFacade resultFacade = (ValidClassificationResultFacade) seqs.get(i);
            if (resultFacade.isMissed()) {
                outFile.write(resultFacade.getPath() + "\n");

            }
        }
        
        outFile.write("\n singleton sequences: \n");
        for (i = 0; i < seqs.size(); i++) {
            ValidClassificationResultFacade resultFacade = (ValidClassificationResultFacade) seqs.get(i);
            if (resultFacade.getLabeledNode().isSingleton()) {
                outFile.write(resultFacade.getPath() + "\n");

            }
        }

    }

    private void compareClassificationResult(ValidClassificationResultFacade resultFacade) {
        HierarchyTree trueParent = resultFacade.getLabeledNode();
        List hitList = resultFacade.getRankAssignment();

        // compare the true taxon and the hit taxon with same rank. mark the sequence that is misclassified
        // update the total count and misclassified count

        while (trueParent != null) {

            if (!trueParent.isSingleton()) {
                ValidationClassificationResult hit = null;
                for (int i = 0; i < hitList.size(); i++) {
                    ValidationClassificationResult tmp = (ValidationClassificationResult) hitList.get(i);
                    if ((trueParent.getTaxonomy().getHierLevel()).equals(tmp.getBestClass().getTaxonomy().getHierLevel())) {
                        hit = tmp;
                        break;
                    }
                }

                if (trueParent.getTaxonomy().getHierLevel().equalsIgnoreCase(testRank)) {
                    trueParent.incNumTotalTestedseq();
                }

                boolean correct = false;

                if ( hit != null){
                    if( trueParent.getTaxonomy().getTaxID() == hit.getBestClass().getTaxonomy().getTaxID()){
                         correct = true;
                    }else {
                        resultFacade.setMissedRank(trueParent.getTaxonomy().getHierLevel());
                    }
                    increaseCount(hit, trueParent.getTaxonomy().getHierLevel(), correct, num_hierLevel);
                }
                if ( !correct && trueParent.getTaxonomy().getHierLevel().equalsIgnoreCase(testRank)) {
                    trueParent.incMissCount();
                }
                               

            } else {
                // System.err.println(" singleton: " + trueParent.getName() + " " + trueParent.getTaxonomy().getHierLevel() );
            }

            trueParent = trueParent.getParent();

        }

    }

    private void increaseCount(ValidationClassificationResult aResult, String level, boolean correct, Map aMap) {
        // bin range: 100-95, 94-90, 89-80, 79-70.....9-0
        CorrectAssignment assign = (CorrectAssignment) aMap.get(level);
        if (assign == null) {
            assign = new CorrectAssignment();
            aMap.put(level, assign);
        }

        int binIndex = (int) Math.floor(aResult.getNumOfVotes() * 10);
        if (binIndex == 9) {
            if (aResult.getNumOfVotes() >= 0.95) {
                binIndex = 10;   // we put 100-95 to the bin #10
            }
        }
        assign.numTotal[binIndex]++;
        assign.sumOfVotes[binIndex] += aResult.getNumOfVotes();

        if (correct) {
            assign.numCorrect[binIndex]++;
        }

    }

    private void calStandardError(CorrectAssignment assign) {
        for (int i = 0; i < assign.bins; i++) {
            if (assign.numTotal[i] == 0) {
                assign.standardError[i] = (float) 0.0;
            } else {
                float p = (float) assign.numCorrect[i] / (float) assign.numTotal[i];
                assign.standardError[i] = (float) Math.sqrt(p * (1 - p) / assign.numTotal[i]);
            }

        }
    }


    /** there are four status
     * TP: bootstrap above cutoff, labeled taxon matches the label in training set
     * FN: bootstrap below cutoff, labeled taxon matches the label in training set
     * FP: bootstrap above cutoff, labeled taxon NOT match the label in in training set
     * TN: bootstrap below cutoff, labeled taxon NOT match the label in training set
     * 
     **/
     private void labelAssignmentStatus( TreeFactory factory, ValidClassificationResultFacade resultFacade,
        ArrayList<HashMap<String, StatusCount>> statusCountList ) throws IOException{
         // determine assignment status
        //System.err.println(resultFacade.getPath());

        // find all the labeled taxa in the training set
        HierarchyTree trueParent = resultFacade.getLabeledNode();
        HashMap<String, HierarchyTree> labeledTreeMap = new HashMap<String, HierarchyTree>();
        while (trueParent != null) {
            labeledTreeMap.put(trueParent.getTaxonomy().getHierLevel(), trueParent);
            trueParent= trueParent.getParent();
        }
       
        List<ValidationClassificationResult> hitList = resultFacade.getRankAssignment();
        for ( ValidationClassificationResult curRankResult: hitList){
            
            
            String curRank = curRankResult.getBestClass().getTaxonomy().getHierLevel();
            boolean match = true;
            // find the corresponding ancestor at the current rank
            HierarchyTree matchingRankTreeNode = labeledTreeMap.get(curRank);
            
            if ( matchingRankTreeNode != null &&  matchingRankTreeNode.isSingleton()){  // ignore singletons
                continue;
            }
            if ( matchingRankTreeNode == null) {  // no TreeNode with matching rank found
                match = false;
            }else if (!curRankResult.getBestClass().getName().equalsIgnoreCase(matchingRankTreeNode.getName()) ){
                match = false;
            }
                  
            int bootstrap = (int)(curRankResult.getNumOfVotes()*100);
            //System.err.println( "assigned rank: " + curRank + "\t" + curRankResult.getBestClass().getName() + "\t match=" + match + "\t" + bootstrap +"\t");
            if ( match){  // TP or FN
                for( int b = 0; b <= bootstrap; b++){
                    ((StatusCount)statusCountList.get(b).get(curRank)).incNumTP(1);
                }
                for( int b = bootstrap+1; b < statusCountList.size(); b++){
                    ((StatusCount)statusCountList.get(b).get(curRank)).incNumFN(1);
                }

            }else {// TN or FP
                for( int b = 0; b <= bootstrap; b++){
                    ((StatusCount)statusCountList.get(b).get(curRank)).incNumFP(1);
                }
                for( int b = bootstrap+1; b < statusCountList.size(); b++){
                    ((StatusCount)statusCountList.get(b).get(curRank)).incNumTN(1);
                }
            }

        }
     }
     
   
    protected Map getNum_hierLevelMap() {
        return num_hierLevel;
    }
}
