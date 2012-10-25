/*
 * TrainingInfo.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 *
 * Created on September 18, 2002, 5:01 PM
 */
package edu.msu.cme.rdp.classifier;

import edu.msu.cme.rdp.classifier.io.ProbIndexFileParser;
import edu.msu.cme.rdp.classifier.io.GenusWordProbFileParser;
import edu.msu.cme.rdp.classifier.io.TreeFileParser;
import edu.msu.cme.rdp.classifier.io.LogWordPriorFileParser;
import edu.msu.cme.rdp.classifier.utils.ClassifierSequence;
import edu.msu.cme.rdp.classifier.utils.HierarchyVersion;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The TrainingInfo holds all the training information and taxonomy hierarchy information.
 * @author  wangqion
 * @version 1.0
 */
public class TrainingInfo {

    private List<HierarchyTree> genusNodeList = new ArrayList();   // list of all the genus nodes
    private List<GenusWordConditionalProb> genus_wordConditionalProbList = new ArrayList();
    // list of genus node index and the corresponding word conditional probability,
    // for word index starting from 0 to 65535.
    private static int NUM_OF_WORDS = 65536;
    private int[] wordConditionalProbIndexArr = new int[NUM_OF_WORDS + 1];   // an array of index, each points to the start of
    // GenusIndexWordConditionalProb for each word in the ArrayList
    private float[] logLeaveCountArr;  // an array of log value of genus leaveCount
    private HierarchyTree rootTree;    // the root of the trees
    private float[] logWordPriorArr = new float[NUM_OF_WORDS];
    // an array of log value of priors for words, the index is the integer
    // form of the word . for word size 8, there are 65536 possible words
    private float[] wordPairPriorDiffArr = new float[NUM_OF_WORDS];
    // the difference between log prior of word W+ and it's reverse complement W-
    private boolean isTreeDone = false;
    private boolean isWordPriorArrDone = false;
    private boolean isProbIndexArrDone = false;
    private boolean isGenusWordProbListDone = false;
    private HierarchyVersion hierarchyVersion;

    /** Creates new TrainingInfo. */
    public TrainingInfo() {
    }

    /** Reads in the tree information from a reader and create all the HierarchyTrees.
     * Note: the tree information has to be read after at least one of the other
     * three files because we need to set the version information.
     */
    public void createTree(Reader reader) throws IOException, TrainingDataException {
        if (!(isProbIndexArrDone || isGenusWordProbListDone || isWordPriorArrDone)) {
            throw new IllegalStateException("Error: The bergeyTree file should be read after "
                    + "at least one of the other training files");
        }

        TreeFileParser parser = new TreeFileParser();
        rootTree = parser.createTree(reader, hierarchyVersion);

        createGenusNodeList(rootTree);

        logLeaveCountArr = new float[genusNodeList.size()];

        for (HierarchyTree node : genusNodeList) {
            logLeaveCountArr[node.getGenusIndex()] = (float) Math.log(node.getLeaveCount() + 1);
        }
        isTreeDone = true;
    }

    /** Reads in the log value of the word prior probability and saves to an array LogWordPriorArr.
     */
    public void createLogWordPriorArr(Reader reader) throws IOException, TrainingDataException {
        hierarchyVersion = LogWordPriorFileParser.createLogWordPriorArr(reader, logWordPriorArr, hierarchyVersion);
        isWordPriorArrDone = true;

        int[] origWord = new int[ClassifierSequence.WORDSIZE];
        generateWordPairDiffArr(origWord, 0);
    }

    /** For a given word w1 and the reverse complement word w2,
     * calculates the difference between the log word prior of w1 and w2 and saves to an array.
     * Repeats for every possible word of size 8. 
     */
    public void generateWordPairDiffArr(int[] word, int beginIndex) {

        if (beginIndex < 0 || beginIndex > word.length) {
            return;
        }

        int origWordIndex = ClassifierSequence.getWordIndex(word);
        int revWordIndex = ClassifierSequence.getWordIndex(ClassifierSequence.getReversedWord(word));

        float origWordPrior = this.getLogWordPrior(origWordIndex);
        float revWordPrior = this.getLogWordPrior(revWordIndex);
        wordPairPriorDiffArr[origWordIndex] = origWordPrior - revWordPrior;

        for (int i = beginIndex; i < word.length; i++) {
            int origBase = word[i];
            for (int j = 0; j < ClassifierSequence.RNA_BASES; j++) {
                if (word[i] == j) {
                    continue;
                }
                word[i] = j;

                //then find the other mismatches recursively.
                generateWordPairDiffArr(word, i + 1);
                // change that char back to the original char
                word[i] = origBase;

            }
        }
    }

    /** Reads in the index of the genus treenode and conditional probability that genus contains a word.
     * Saves the data into a list genus_wordConditionalProbList.
     */
    public void createGenusWordProbList(Reader reader) throws IOException, TrainingDataException {
        hierarchyVersion = GenusWordProbFileParser.createGenusWordProbList(reader, genus_wordConditionalProbList, hierarchyVersion);
        isGenusWordProbListDone = true;
    }

    /** Reads in start index of the conditional probability of each genus,
     *  saves to an array wordConditionalProbIndexArr.
     */
    public void createProbIndexArr(Reader reader) throws IOException, TrainingDataException {
        hierarchyVersion = ProbIndexFileParser.createProbIndexArr(reader, wordConditionalProbIndexArr, hierarchyVersion);
        isProbIndexArrDone = true;
    }

    /** Creates a new Classifier if all the train information have been completed,
     * throws exception if not.
     */
    public Classifier createClassifier() {
        if (isTreeDone && isProbIndexArrDone && isGenusWordProbListDone && isWordPriorArrDone) {
            Classifier aClassifier = new Classifier(this);
            return aClassifier;
        } else {
            throw new IllegalStateException("Error: Can not create a Classifier! "
                    + "Training information have not been created.\n ");
        }
    }

    /** Returns the root of the trees. */
    public HierarchyTree getRootTree() {
        return rootTree;
    }

    /** Returns the number of the genus nodes.
     */
    public int getGenusNodeListSize() {
        return genusNodeList.size();
    }

    /** Returns a genus node from the genusNodeList at the specified position.
     */
    public HierarchyTree getGenusNodebyIndex(int i) {
        return genusNodeList.get(i);
    }

    /** Returns the log value of the prior probability of a word.
     */
    public float getLogWordPrior(int wordIndex) {
        return logWordPriorArr[wordIndex];
    }

    /** Returns the difference between given word and its reverse complement word.
     */
    public float getWordPairPriorDiff(int wordIndex) {
        return wordPairPriorDiffArr[wordIndex];
    }

    /** Returns the log value of (number of leaves + 1) of a genus
     */
    public float getLogLeaveCount(int i) {
        return logLeaveCountArr[i];
    }

    /** Returns the start index of GenusIndexWordConditionalProb in the array for the
     * specified wordIndex.
     */
    public int getStartIndex(int wordIndex) {
        return wordConditionalProbIndexArr[wordIndex];
    }

    /** Returns the stop index of GenusIndexWordConditionalProb in the array for the
     * specified wordIndex.
     */
    public int getStopIndex(int wordIndex) {
        return wordConditionalProbIndexArr[wordIndex + 1];
    }

    /** Returns a GenusIndexWordConditionalProb from the genusIndex_wordConditionalProbList
     * at the specified postion in the list.
     */
    public GenusWordConditionalProb getWordConditionalProbObject(int posIndex) {
        return (GenusWordConditionalProb) genus_wordConditionalProbList.get(posIndex);
    }

    /** Returns the version of the taxonomical hierarchy.
     */
    public String getHierarchyVersion() {
        return hierarchyVersion.getVersion();
    }

    /** Returns the info of the taxonomy hierarchy from of the training file.
     */
    public HierarchyVersion getHierarchyInfo() {
        return hierarchyVersion;
    }

    /** Returns a list of all the genus rank nodes.
     * It searches the genus nodes starting from the root. It puts each genus
     * node into genusNodeList in the order defined by its genusIndex.
     */
    private void createGenusNodeList(HierarchyTree root) {
        if (root == null) {
            return;
        }

        int genusIndex = root.getGenusIndex();
        if (genusIndex != -1) {
            genusNodeList.add(genusIndex, root);
            return;
        }

        //start from the root of the tree, get the subclasses.
        Collection<HierarchyTree> al = new ArrayList();
        if ((al = root.getSubclasses()).isEmpty()) {
            return;
        }

        for (HierarchyTree ht : al) {
            createGenusNodeList(ht);
        }
    }

    /**
     * Returns true if the sequence is in reverse orientation.
     * Sums the difference between all the overlapping words from the query sequence
     * and the reverse complements of those word. If the summation is
     * less that zero, the query sequence is in reverse orientation.
     */
    public boolean isSeqReversed(ClassifierSequence seq) {
        int[] wordIndexArr = seq.createWordIndexArr();
        boolean reverse = false;
        float priorDiff = 0;
        for (int offset = 0; offset < wordIndexArr.length; offset++) {
            int wordIndex = wordIndexArr[offset];
            if (wordIndex >= 0) {
                priorDiff += getWordPairPriorDiff(wordIndex);
            }
        }
        if (priorDiff < 0) {
            reverse = true;
        }
        return reverse;

    }
}
