/*
 * HierarchyTree.java
 *
 * Created on June 24, 2002, 2:36 PM
 */
/**
 *
 * @author  wangqion
 * @version
 */
package edu.msu.cme.rdp.classifier.train.validation;

import edu.msu.cme.rdp.classifier.train.GoodWordIterator;
import edu.msu.cme.rdp.classifier.train.LineageSequence;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class HierarchyTree {

    private String name;
    private int numOfLeaves;
    private HierarchyTree parent;
    private Map<String, HierarchyTree> subclasses = new HashMap();
    private Map leaves = new HashMap();
    private int[] wordOccurrence;  //size is 64k for word size 8
    private boolean wordOccurDone = false;
    private int totalSeqs = 0; // total number of train sequences
    private int numTotalTestedseq = 0;   // no singleton included
    private int missCount = 0; // the number of missclassified at and below this node (indicates all the genera under this node)
    private Taxonomy taxon;    // the unique id for each node

    /** Creates new HierarchyTree given the name and its parent */
    public HierarchyTree(String n, HierarchyTree p, Taxonomy tax) {
        name = n;
        taxon = tax;
        numOfLeaves = -1;
        addParent(p);
    }

    /** Adds the parent HierarchyTree, also add this node to the parent tree as a child */
    private void addParent(HierarchyTree p) {
        parent = p;
        if (parent != null) {
            parent.addSubclass(this);
        }
    }

    /** Adds a subclass */
    private void addSubclass(HierarchyTree c) {
        subclasses.put(c.getName(), c);
    }

    /** Gets the name of the treenode */
    public String getName() {
        return name;
    }

    /** Gets the parent treenode */
    public HierarchyTree getParent() {
        return parent;
    }

    /** Gets the array of the subclasses if any */
    public Collection<HierarchyTree> getSubclasses() {
        return subclasses.values();
    }

    /** Gets the array of Sequenceleaves if any */
    public Collection getLeaves() {
        return leaves.values();
    }

    /** Gets the subclass with the given name */
    public HierarchyTree getSubclassbyName(String n) {
        return subclasses.get(n);
    }

    /** Gets the size of the children */
    public int getSizeofChildren() {
        int size;
        if ((size = subclasses.size()) > 0) {
            return size;
        }
        return getSizeofLeaves();
    }

    /** Gets the size of the subclasses */
    public int getSizeofSubclasses() {
        return subclasses.size();
    }

    /** Gets the size of sequence leaves directly belong to this tree, not
     * including the hidden leaves */
    public int getSizeofLeaves() {
        return numOfLeaves;
    }

    /** This method initiate the word occurrence from the sequences for the
     * lowest level of the hierarchy tree
     */
    public void initWordOccurrence(LineageSequence pSeq, float[] wordPriorArr) throws IOException {
        if (numOfLeaves < 0) {
            numOfLeaves = 1;
        } else {
            numOfLeaves++;
        }

        GoodWordIterator iterator = new GoodWordIterator(pSeq.getSeqString());
        if (wordOccurrence == null) {
            wordOccurrence = new int[iterator.getMask() + 1];
        }

        // create a temporary list and initialize the value to be -1;
        int[] wordList = new int[iterator.getNumofWords()];

        for (int i = 0; i < wordList.length; i++) {
            wordList[i] = -1;
        }

        int numUniqueWords = 0;  // indicate the number of unique words
        // duplicated words in one sequence are only counted once
        while (iterator.hasNext()) {
            int index = iterator.next();  // index is the actual integer representation of the word
            
            if (!isWordExist(wordList, index)) {
                wordList[numUniqueWords] = index;
                wordOccurrence[index]++;
                numUniqueWords++;
                // now add the word to the wordPriorArr
                // duplicated words in one sequence are only counted once
                wordPriorArr[index]++;
            }
        }
        
        // change the flag to be true
        wordOccurDone = true;
    }

    /** This method initiate the word occurrence from the sequences for the
     * lowest level of the hierarchy tree
     */
    public void unhideSeq(GoodWordIterator iterator) throws IOException {
        if (numOfLeaves < 0) {
            numOfLeaves = 1;
        } else {
            numOfLeaves++;
        }

        iterator.resetCurIndex();

        // create a temporary list and initialize the value to be -1;
        int[] wordList = new int[iterator.getNumofWords()];

        for (int i = 0; i < wordList.length; i++) {
            wordList[i] = -1;
        }

        int numUniqueWords = 0;  // indicate the number of unique words
        // duplicated words in one sequence are only counted once
        while (iterator.hasNext()) {
            int index = iterator.next();  // index is the actual integer representation of the word

            if (!isWordExist(wordList, index)) {
                wordList[numUniqueWords] = index;
                wordOccurrence[index]++;
                numUniqueWords++;
            }
        }

        //also need to unhide parent,
        changeParentSeqCount(1);
    }

    /** This method hides a sequence by removing the words of that sequence
     * from the total wordOccurrence
     */
    public void hideSeq(GoodWordIterator iterator) throws IOException {
        if (wordOccurrence == null) {
            throw new IllegalStateException("unable to hide the sequence, the word occurrence is null ");
        }

        iterator.resetCurIndex();

        // create a temporary list and initialize the value to be -1;
        int[] wordList = new int[iterator.getNumofWords()];

        for (int i = 0; i < wordList.length; i++) {
            wordList[i] = -1;
        }

        int num = 0;
        // duplicated words in one sequence are only counted once

        while (iterator.hasNext()) {
            int index = iterator.next();
            if (!isWordExist(wordList, index)) {
                wordList[num] = index;
                wordOccurrence[index]--;
                num++;
            }
        }

        // also need to hide parent, since parent doesn't count word occurrence
        // we only reduce the number of leaves.
        //  reduce the number of leaves   
        numOfLeaves--;
        changeParentSeqCount(-1);
    }

    public void changeParentSeqCount(int i) {

        if (parent != null) {
            parent.numOfLeaves += i;
            parent.changeParentSeqCount(i);
        }

    }

    /** check if this word already been added to the wordOccurrence
     */
    private boolean isWordExist(int[] wordList, int wordIndex) {
        for (int i = 0; i < wordList.length; i++) {
            if (wordList[i] == wordIndex) {
                return true;
            }
            if (wordList[i] == -1) {
                return false;
            }
        }
        return false;
    }

    public boolean isWordOccurDone() {
        return wordOccurDone;
    }

    public int getWordOccurrenceSize() {
        return wordOccurrence.length;
    }

    public int getNumberofUniqueWords() {
        int count = 0;
        for (int i = 0; i < wordOccurrence.length; i++) {
            if (wordOccurrence[i] > 0) {
                count++;
            }
        }
        return count;
    }

    /** creates the word occurrence array from its children if the word occurrence
     * for this node does not exist
     */
    public void createWordOccurrenceFromSubclasses() {
        if (isWordOccurDone()) {
            return;
        }

        if (subclasses.size() > 0) {
            int len = 0;
            for(HierarchyTree child : subclasses.values()) {
                if(!child.isWordOccurDone()) {
                    child.createWordOccurrenceFromSubclasses();;
                }
                
                len = child.getWordOccurrenceSize();
            }
            
            wordOccurrence = new int[len];
            for (int i = 0; i < len; i++) {
                for(HierarchyTree child : subclasses.values()) {
                    wordOccurrence[i] += child.getWordOccurrence(i);
                }
            }
        }
    }

    /** Gets the word occurrence for the given word index
     */
    public int getWordOccurrence(int wordIndex) {
        return wordOccurrence[wordIndex];
    }

    /** Counts the number of sequence leaves below this tree */
    public int getNumOfLeaves() {
        if (!(numOfLeaves < 0)) {
            return numOfLeaves;
        }
        if (getSizeofSubclasses() <= 0) {
            numOfLeaves = getSizeofLeaves();
            return numOfLeaves;
        }
        numOfLeaves = 0;
        for(HierarchyTree child : subclasses.values()) {
            numOfLeaves += child.getNumOfLeaves();
        }
        return numOfLeaves;
    }

    public void increTotalSeqs() {
        totalSeqs++;
        if (parent != null) {
            parent.increTotalSeqs();
        }
    }

    public int getTotalSeqs() {
        return totalSeqs;
    }

    public int getMissCount() {
        return this.missCount;
    }

    public int getNumTotalTestedseq() {
        return this.numTotalTestedseq;
    }

    public void incNumTotalTestedseq() {
        numTotalTestedseq++;
        if (parent != null) {
            parent.incNumTotalTestedseq();
        }
    }

    public void incMissCount() {
        missCount++;
        if (parent != null) {
            parent.incMissCount();
        }
    }

    public Taxonomy getTaxonomy() {
        return taxon;
    }

    public boolean isSingleton() {
        return (totalSeqs > 1) ? false : true;

    }

    /** get all the lowest level nodes in given hierarchy level starting from the given root
     */
    
    public void getNodeList(String level, List nodeList) {

        if (this.taxon.getHierLevel().equalsIgnoreCase(level)) {
            nodeList.add(this);
            return;
        }
        //start from the root of the tree, get the subclasses.
        Collection al = new ArrayList();

        if ((al = this.getSubclasses()).isEmpty()) {
            return;
        }
        Iterator i = al.iterator();
        while (i.hasNext()) {
            ((HierarchyTree) i.next()).getNodeList(level, nodeList);
        }
    } 
    
    /** get all the lowest level nodes in given hierarchy level starting from the given root
     */
    public void getNodeMap(String level, HashMap<String, HierarchyTree> nodeMap) {

        if (this.taxon.getHierLevel().equalsIgnoreCase(level)) {
            nodeMap.put(this.name, this);
            return;
        }
        //start from the root of the tree, get the subclasses.
        Collection al = new ArrayList();

        if ((al = this.getSubclasses()).isEmpty()) {
            return;
        }
        Iterator i = al.iterator();
        while (i.hasNext()) {
            ((HierarchyTree) i.next()).getNodeMap(level, nodeMap);
        }
    }
}
