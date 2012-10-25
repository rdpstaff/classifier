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
package edu.msu.cme.rdp.classifier.train.validation.movingwindow;

import java.util.*;
import java.io.*;
import java.text.*;

import edu.msu.cme.rdp.classifier.train.validation.ValidationClassificationResult;
import edu.msu.cme.rdp.classifier.train.validation.ValidClassificationResultFacade;
import edu.msu.cme.rdp.classifier.train.validation.CorrectAssignment;
import edu.msu.cme.rdp.classifier.train.validation.DecisionMaker;
import edu.msu.cme.rdp.classifier.train.GoodWordIterator;
import edu.msu.cme.rdp.classifier.train.LineageSequence;
import edu.msu.cme.rdp.classifier.train.validation.HierarchyTree;
import edu.msu.cme.rdp.classifier.train.validation.Taxonomy;
import edu.msu.cme.rdp.classifier.train.validation.TreeFactory;

/** */
public class WindowTester {

    BufferedWriter outFile;
    private boolean bootstrap = true;
    private int windowIndex = 0;
    private String testRank = Taxonomy.GENUS;
    private Map num_hierLevel = new HashMap();  // key is the hierarchy level, value is
    // he number of correctly classified sequences at that level
    List missSeqList = new ArrayList();  // this is only for test purpose, if the sequence

    /** Creates new Tester */
    public WindowTester(Writer writer) throws IOException {
        outFile = (BufferedWriter) writer;
    }

    /** classify each sequence from a parser */
    public void classify(TreeFactory factory, ArrayList seqList, Window window, int windowIndex, int min_bootstrap_words)
            throws IOException {
        this.windowIndex = windowIndex;

        //for each sequence with name, and or true path
        List resultList = new ArrayList();

        DecisionMaker dm = new DecisionMaker(factory);
        HierarchyTree root = factory.getRoot();

        HashMap<String, HierarchyTree> genusNodeMap = new HashMap<String, HierarchyTree>();
        factory.getRoot().getNodeMap(testRank, genusNodeMap);

        if (genusNodeMap.isEmpty()) {
            throw new IllegalArgumentException("\nThere is no node in GENUS level!");
        }

        int i = 0;
        Iterator seqIt = seqList.iterator();
        while (seqIt.hasNext()) {
            LineageSequence pSeq = (LineageSequence) seqIt.next();

            GoodWordIterator wordIterator = getPartialSeqIteratorbyWindow(pSeq, window); // full sequence  

            if (wordIterator == null) {
                continue;
            }

            //for leave-one-out testing, we need to remove the word occurrance for
            //the current sequence. This is similiar to hiding a sequence leaf.
            HierarchyTree curTree = genusNodeMap.get((String) pSeq.getAncestors().get(pSeq.getAncestors().size() - 1));


            curTree.hideSeq(wordIterator);
            List result = dm.getBestClasspath( wordIterator, genusNodeMap, false, min_bootstrap_words);

            ValidClassificationResultFacade resultFacade = new ValidClassificationResultFacade(pSeq, result);
            resultFacade.setLabeledNode(curTree);
            compareClassificationResult(resultFacade);

            resultList.add(resultFacade);
            i++;
            // recover the wordOccurrence of the genus node, unhide
            curTree.unhideSeq(wordIterator);

        }

        displayStat();
    }

    private GoodWordIterator getPartialSeqIteratorbyWindow(LineageSequence pSeq, Window w) throws IOException {
        int firstbase = findFirstBase(pSeq.getSeqString());
        int lastbase = findLastBase(pSeq.getSeqString());

        if (firstbase > w.getStart() || lastbase < w.getStop()) {
            return null;
        }


        int stop = w.getStop();
        if (stop > pSeq.getSeqString().length()) {
            stop = pSeq.getSeqString().length();
        }
        String seqString = pSeq.getSeqString().substring(w.getStart() - 1, stop);
        seqString = seqString.replaceAll("-", "");
        // at least half of the window size.

        if (seqString.length() < FindWindowFrame.window_size / 2) {
            return null;
        }
        GoodWordIterator wordIterator = new GoodWordIterator(seqString);
        if (wordIterator.getNumofWords() == 0) {
            wordIterator = null;
        }
        return wordIterator;
    }

    private int findFirstBase(String s) throws IOException {
        StringReader reader = new StringReader(s);
        int c;
        int index = 1;
        while ((c = reader.read()) != -1) {
            char ch = (char) c;
            if (ch != '-') {
                return index;
            }
            index++;
        }
        reader.close();
        return -1;
    }

    private int findLastBase(String s) throws IOException {
        StringReader reader = new StringReader(s);
        int c;
        int index = 1;
        int lastBase = 0;
        while ((c = reader.read()) != -1) {
            char ch = (char) c;
            if (ch != '-') {
                lastBase = index;
            }
            index++;
        }
        reader.close();
        return lastBase;
    }

    /** get all the lowest level nodes in given hierarchy level starting from the given root
     *
    public void getNodeList(HierarchyTree root, String level, List nodeList) {
        if (root == null) {
            return;
        }

        if (((Taxonomy) root.getTaxonomy()).getHierLevel().equalsIgnoreCase(level)) {
            nodeList.add(root);
            return;
        }
        //start from the root of the tree, get the subclasses.
        Collection al = new ArrayList();

        if ((al = root.getSubclasses()).isEmpty()) {
            return;
        }
        Iterator i = al.iterator();
        while (i.hasNext()) {
            getNodeList((HierarchyTree) i.next(), level, nodeList);
        }
    } */

    /** search the tree node that the current test getSequence() belongs to
     */
    /*
    public HierarchyTree getTreeNode(LineageSequence pSeq, List nodeList) {
        Iterator i = nodeList.iterator();
        String genusNode = (String) pSeq.getAncestors().get(pSeq.getAncestors().size() - 1);

        while (i.hasNext()) {
            HierarchyTree tree = (HierarchyTree) i.next();
            if ((tree.getName()).equals(genusNode)) {
                return tree;
            }
        }

        throw new IllegalArgumentException("Test getSequence() name: "
                + pSeq.getSeqName() + " is not found in the tree. Can not continue testing");

    } */

    public void displayTreeErrorRate(HierarchyTree root, int indent) throws IOException {
        int k = 0;
        while (k < indent) {
            outFile.write("  ");
            k++;
        }
        outFile.write(root.getName() + "\t" + root.getTotalSeqs() + "\t" + root.getMissCount() + "\n");
        Iterator i = root.getSubclasses().iterator();

        while (i.hasNext()) {
            displayTreeErrorRate((HierarchyTree) i.next(), indent + 1);
        }
    }

    private void displayResult(List seqs, List aPath) throws IOException {
        int i = 0;
        int size1 = seqs.size();

        for (i = 0; i < size1; i++) {
            outFile.write((String) seqs.get(i) + "*");
            int size = ((List) aPath.get(i)).size();
            if (size == 0) {
                outFile.write("\n");
                continue;
            }
            ValidationClassificationResult result = (ValidationClassificationResult) ((List) aPath.get(i)).get(size - 1);

            outFile.write(((Taxonomy) ((HierarchyTree) result.getBestClass()).getTaxonomy()).getTaxID() + "\n");
        }
    }

    /** Displays the results of the classification, true path, assigned path and the
     * error rate
     */
    private void displayClassification(List seqs)
            throws IOException {
        int i = 0;

        outFile.write("\n missclassified getSequence()s: \n");
        for (i = 0; i < seqs.size(); i++) {
            ValidClassificationResultFacade resultFacade = (ValidClassificationResultFacade) seqs.get(i);
            if (resultFacade.isMissed()) {
                printPath(resultFacade, outFile);
            }
        }

        DecimalFormat df = new DecimalFormat("0.###");

        outFile.write("\n\n**The statistics for each hierarchy level: \n 1: number of correct assigned getSequence()s / number of total getSequence()s for that bin rage\n");
        outFile.write("Level\t100-95\t\t94-90");
        for (i = 9; i > 0; i--) {
            outFile.write("\t\t" + (i * 10 - 1) + "-" + (i - 1) * 10);
        }
        outFile.write("\n");

        System.err.print("\t");
        Iterator it = num_hierLevel.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            outFile.write(name + "\t");
            if (this.windowIndex == MainMovingWindow.getBeginIndex() && !name.equals("domain") && !name.equals("norank") && !name.startsWith("sub")) {
                System.err.print(name + "\t");
            }
            CorrectAssignment assign = (CorrectAssignment) num_hierLevel.get(name);

            calStandardError(assign);

            for (i = assign.bins - 1; i >= 0; i--) {
                outFile.write(assign.numCorrect[i] + "\t" + assign.numTotal[i] + "\t");
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
    }

    private void displayStat() throws IOException {
        outFile.write("\t");
        Iterator it = num_hierLevel.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            if (this.windowIndex == MainMovingWindow.getBeginIndex() && !name.equals("domain") && !name.equals("norank") && !name.startsWith("sub")) {
                outFile.write(name + "\t");
            }
            CorrectAssignment assign = (CorrectAssignment) num_hierLevel.get(name);

            calStandardError(assign);

        }

        outFile.write("\n");
        // print to std err
        outFile.write("V\t" + this.windowIndex);
        it = num_hierLevel.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();

            if (name.equals("domain") || name.equals("norank") || name.startsWith("sub")) {
                continue;
            }

            CorrectAssignment assign = (CorrectAssignment) num_hierLevel.get(name);
            outFile.write("\t" + getAvgVotes(assign));

        }


        outFile.write("\nC\t" + this.windowIndex);
        it = num_hierLevel.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();

            if (name.equals("domain") || name.equals("norank") || name.startsWith("sub")) {
                continue;
            }

            CorrectAssignment assign = (CorrectAssignment) num_hierLevel.get(name);

            outFile.write("\t" + getCorrectRate(assign));

        }

    }

    /** print the true path and the assigned path
     * Note: the true path is list of string of getAncestors() for a getSequence(),
     * the assigned path is a list of ClassificationResult for a getSequence().
     */
    private void printPath(ValidClassificationResultFacade resultFacade, BufferedWriter out)
            throws IOException {
        out.write("SEQ: " + resultFacade.getSeqName() + "\n");
        Iterator i = resultFacade.getAncestors().iterator();
        while (i.hasNext()) {
            out.write(i.next() + "\t");
        }
        out.write("\n");

        i = resultFacade.getRankAssignment().iterator();
        while (i.hasNext()) {
            ValidationClassificationResult result = (ValidationClassificationResult) i.next();
            out.write(((HierarchyTree) result.getBestClass()).getName() + "\t");

        }

        out.write("\n");
        i = resultFacade.getRankAssignment().iterator();
        while (i.hasNext()) {
            out.write(((ValidationClassificationResult) i.next()).getNumOfVotes() + "\t");
        }
        out.write("\n");
    }

    /** Compare the assigned path with the true path for the test getSequence(),
     * counts the number of correct classes and the number of getSequence()s for
     * each path level.
     */
    private void compareClassificationResult(ValidClassificationResultFacade resultFacade) {
        HierarchyTree trueParent = resultFacade.getLabeledNode();
        List hitList = resultFacade.getRankAssignment();

        // compare the true taxon and the hit taxon with same rank.
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

                if (trueParent.getTaxonomy().getHierLevel().equals(testRank)) {
                    trueParent.incNumTotalTestedseq();
                }
                boolean correct = false;


                if (hit != null && trueParent.getTaxonomy().getTaxID() == hit.getBestClass().getTaxonomy().getTaxID()) {
                    correct = true;
                } else {
                    if (trueParent.getTaxonomy().getHierLevel().equals(testRank)) {
                        trueParent.incMissCount();
                    }
                    resultFacade.setMissedRank(trueParent.getTaxonomy().getHierLevel());
                }
                if (hit != null) {
                    increaseCount(hit, trueParent.getTaxonomy().getHierLevel(), correct, num_hierLevel);
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

    private float getCorrectRate(CorrectAssignment assign) {
        int total = 0;
        int numCorrect = 0;
        for (int i = assign.bins - 1; i >= 0; i--) {
            total += assign.numTotal[i];
            numCorrect += assign.numCorrect[i];
        }

        float retval = 0;
        if (total > 0) {
            retval = (float) numCorrect / (float) total;
        }

        return retval;
    }

    private float getAvgVotes(CorrectAssignment assign) {
        int totalNumOfSeq = 0;
        float sumOfVotes = 0.0f;
        for (int i = 0; i < assign.bins; i++) {
            totalNumOfSeq += assign.numTotal[i];
            sumOfVotes += assign.sumOfVotes[i];
        }

        float avg = 0.0f;
        if (totalNumOfSeq > 0) {
            avg = sumOfVotes / (float) totalNumOfSeq;
        }

        return avg;
    }
}
