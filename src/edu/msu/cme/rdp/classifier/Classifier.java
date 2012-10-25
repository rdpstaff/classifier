/*
 * Classifier.java 
 * 
 * Copyright 2006 Michigan State University Board of Trustees
 *
 * Created on September 18, 2003, 6:05 PM
 */
package edu.msu.cme.rdp.classifier;

import edu.msu.cme.rdp.classifier.utils.ClassifierSequence;
import edu.msu.cme.rdp.readseq.readers.Sequence;
import java.util.*;


/**
 * This is the class to do the classification.
 * @author  wangqion
 * @version 1.0
 */
public class Classifier {

    private TrainingInfo trainingInfo;
    /** The number of bootstrap trials. Initially set to 100. */
    private final int NUM_OF_RUNS = 100;
    /** The assumed maximum number of words per sequence. Initially set to 5000. */
    private final int MAX_NUM_OF_WORDS = 5000;
    /** The minimum number of bases per sequence. Initially set to 200. */
    public static final int MIN_SEQ_LEN = 50;
    
    public static final int MIN_GOOD_WORDS = MIN_SEQ_LEN - ClassifierSequence.WORDSIZE;
    public static final int MIN_BOOTSTRSP_WORDS = 5;   // mininum number of words needs for bootstrap
    
    private float[][] querySeq_wordProbArr;  // 2-D array for the query sequence
    // the 1st dim is the MAX_NUM_OF_WORDS, the 2nd dim is the number of genus nodes
    private float[] accumulateProbArr;   // an array to temporary store the accumulative probability
    private long seed = 1;
    private Random randomGenerator = new Random(seed);
    private Random randomSelectGenera = new Random();

    /** Creates new Classifier.  */
    Classifier(TrainingInfo t) {
        trainingInfo = t;
        int nodeListSize = trainingInfo.getGenusNodeListSize();
        querySeq_wordProbArr = new float[MAX_NUM_OF_WORDS][nodeListSize];
        accumulateProbArr = new float[nodeListSize];
    }

    /**Takes a query sequence, returns the classification result.
     * For each query sequence, first assign it to a genus node using all the words for calculation.
     * Then randomly chooses one-eighth of the all overlapping words in the query 
     * to calculate the joint probability. The number of times a genus was selected out of 
     * the number of bootstrap trials was used as an estimate of confidence in the assignment to that genus. 
     * @throws ShortSequenceException if the sequence length is less than the minimum sequence length.
     */
    public ClassificationResult classify(Sequence seq) {
        return classify(new ClassifierSequence(seq));
    }

    public ClassificationResult classify(ClassifierSequence seq) {
        return classify(seq, MIN_BOOTSTRSP_WORDS );
    }
     
    /**Takes a query sequence, returns the classification result.
     * For each query sequence, first assign it to a genus node using all the words for calculation.
     * Then randomly chooses one-eighth of the all overlapping words in the query 
     * to calculate the joint probability. The number of times a genus was selected out of 
     * the number of bootstrap trials was used as an estimate of confidence in the assignment to that genus. 
     * @throws ShortSequenceException if the sequence length is less than the minimum sequence length.
     */
    public ClassificationResult classify(ClassifierSequence seq, int min_bootstrap_words) {
        GenusWordConditionalProb gProb = null;
        int nodeListSize = trainingInfo.getGenusNodeListSize();
        boolean reversed = false;
        
        if (trainingInfo.isSeqReversed(seq)) {
            seq = seq.getReversedSeq();
            reversed = true;
        }
        
        int[] wordIndexArr = seq.createWordIndexArr();
        int goodWordCount = seq.getGoodWordCount();

        if (seq.getSeqString().length() < MIN_SEQ_LEN) {
            throw new ShortSequenceException(seq.getSeqName(), "ShortSequenceException: The length of sequence with recordID="
                    + seq.getSeqName() + " is less than " + MIN_SEQ_LEN);
        }

        if (goodWordCount < MIN_GOOD_WORDS) {
            throw new ShortSequenceException(seq.getSeqName(), "ShortSequenceException: The sequence with recordID="
                    + seq.getSeqName() + " does not have enough valid words, minimum " + MIN_GOOD_WORDS + " are required");
        }

        if (goodWordCount > MAX_NUM_OF_WORDS) {
            querySeq_wordProbArr = new float[goodWordCount][nodeListSize];
            System.err.println("increase the array size to " + goodWordCount);
        }

        int NUM_OF_SELECTIONS = Math.max( goodWordCount / ClassifierSequence.WORDSIZE, min_bootstrap_words);
        
        for (int offset = 0; offset < goodWordCount; offset++) {
            int wordIndex = wordIndexArr[offset];
            float wordPrior = trainingInfo.getLogWordPrior(wordIndex);
            // first set the value for zero occurrence word
            for (int node = 0; node < nodeListSize; node++) {
                querySeq_wordProbArr[offset][node] = wordPrior - trainingInfo.getLogLeaveCount(node);
            }

            // find the non-zero occurrence value
            int start = trainingInfo.getStartIndex(wordIndex);
            int stop = trainingInfo.getStopIndex(wordIndex);
            for (int n = start; n < stop; n++) {
                gProb = trainingInfo.getWordConditionalProbObject(n);
                querySeq_wordProbArr[offset][gProb.getGenusIndex()] = gProb.getProbability();
            }
        }

        // first use all the word to determine the genus assignment
        for (int j = 0; j < goodWordCount; j++) {
            for (int node = 0; node < nodeListSize; node++) {
                accumulateProbArr[node] += querySeq_wordProbArr[j][node];
            }
        }

        float maxPosteriorProb = Float.NEGATIVE_INFINITY;
        int determinedNodeIndex = 0;
        for (int node = 0; node < nodeListSize; node++) {
            if (accumulateProbArr[node] > maxPosteriorProb) {
                determinedNodeIndex = node;
                maxPosteriorProb = accumulateProbArr[node];
            }            
            accumulateProbArr[node] = 0; // reset to 0
        }                     
        
        HashMap<HierarchyTree, RankAssignment> determinedMap = new HashMap<HierarchyTree, RankAssignment>();
        HierarchyTree aNode = trainingInfo.getGenusNodebyIndex(determinedNodeIndex);
        while (aNode != null) {
            determinedMap.put(aNode, new RankAssignment(aNode, 0));
            aNode = aNode.getParent();
        }


        // for each run, pick the genus node which has the highest posterior probability
        boolean tied = false;
        for (int i = 0; i < NUM_OF_RUNS; i++) {
            for (int j = 0; j < NUM_OF_SELECTIONS; j++) {
                int randomIndex = randomGenerator.nextInt(goodWordCount);

                for (int node = 0; node < nodeListSize; node++) {
                    accumulateProbArr[node] += querySeq_wordProbArr[randomIndex][node];
                }
            }

            maxPosteriorProb = Float.NEGATIVE_INFINITY;
            int bestNodeIndex = 0;
            
            for (int node = 0; node < nodeListSize; node++) {
                if (accumulateProbArr[node] >= maxPosteriorProb) {
                    if (accumulateProbArr[node] > maxPosteriorProb){
                    // if the maxPosteriorProb tied between multiple genera, we need save all genera with tied score and radomly pick one later
                        bestNodeIndex = node;
                        maxPosteriorProb = accumulateProbArr[node];
                        tied = false;
                    }else{
                        tied = true;
                    }
                }
            }
            
            // if the maxPosteriorProb tied between multiple genera, we need to radomly pick one
            if (tied){
                ArrayList<Integer> possibleSet = new ArrayList<Integer>();
                for (int node = 0; node < nodeListSize; node++) {
                    if (accumulateProbArr[node] == maxPosteriorProb) {
                        possibleSet.add(node);
                    }
                }
                
                bestNodeIndex = possibleSet.get(randomSelectGenera.nextInt(possibleSet.size()));               
            }
            
             for (int node = 0; node < nodeListSize; node++) {
                 accumulateProbArr[node] = 0; // reset to 0
             }
            
            addConfidence(trainingInfo.getGenusNodebyIndex(bestNodeIndex), determinedMap);
        }
        
        
        List finalAssigns = getFinalResultList(determinedMap, trainingInfo.getGenusNodebyIndex(determinedNodeIndex));
        ClassificationResult finalResult = new ClassificationResult(seq, reversed, finalAssigns, trainingInfo.getHierarchyInfo());

        return finalResult;
    }

    /**
     * increase the count of the RankAssignment in the map if match that node or any ancestor of that node.
     * @param node
     * @param map
     */
    public void addConfidence(HierarchyTree node, HashMap map) {
        while (node != null) {
            RankAssignment assign = (RankAssignment) map.get(node);
            if (assign != null) {
                assign.setConfidence(assign.getConfidence() + 1.0f);
            }
            node = node.getParent();
        }
    }

    private List getFinalResultList(HashMap map, HierarchyTree aNode) {
        List<RankAssignment> finalAssigns = new ArrayList<RankAssignment>();
        while (aNode != null) {
            RankAssignment assign = (RankAssignment) map.get(aNode);
            assign.setConfidence(assign.getConfidence() / (float) NUM_OF_RUNS);
            finalAssigns.add(0, assign);
            aNode = aNode.getParent();
        }
        return finalAssigns;
    }

    /** Adds a single RankAssignment to the list of the classification results
     *  if a treenode is already included in the list, simply increases the
     *  confidence for that RankAssignment by 1, for easy calculation.
     *   Later this confidence will be divided by NUM_OF_RUNS.
     */
    private void addBestGenusNode(HierarchyTree node, List resultList) {
        int node_taxid = node.getTaxid();
        Iterator it = resultList.iterator();
        boolean found = false;
        while (it.hasNext()) {
            RankAssignment result = (RankAssignment) it.next();
            // if this result is already in the list, increase the confidence
            if (node_taxid == result.getBestClass().getTaxid()) {
                result.setConfidence(result.getConfidence() + 1);
                found = true;
                break;
            }
        }

        if (!found) {
            RankAssignment result = new RankAssignment(node, 1);
            resultList.add(result);
        }
    }

    /** Finds the ancestors of the given node up to the root.
     * The ancestors are kept in a list in which the root is the first item.
     */
    private void findAncestor(HierarchyTree root, RankAssignment curNode, List ancestorList) {
        if (curNode == null) {
            return;
        }
        if (curNode.getBestClass().getTaxid() == root.getTaxid()) {
            ancestorList.add(curNode);
            return;
        }

        RankAssignment parent = new RankAssignment(curNode.getBestClass().getParent(), curNode.getConfidence());
        findAncestor(root, parent, ancestorList);
        ancestorList.add(curNode);
    }

    /** Returns a list of RankAssignment in which root is the first item.
     * Each node in the list is the one which has the highest confidence
     * among the children of the previous node. 
     * Algorithm: From the top rank, select the child that has the highest votes,
     * traverse down to the bottom rank level.
     */
    private List getGreedyPath(HierarchyTree root, List resultList) {
        List votingList = new ArrayList();  // a list of ancestorLists,
        // each ancestorList cotains a list RankAssignment (from root to genus)

        // first gets all the ancestors for each genus node in the resultList
        Iterator genusIt = resultList.iterator();
        while (genusIt.hasNext()) {
            RankAssignment aResult = (RankAssignment) genusIt.next();
            List ancestorList = new ArrayList();
            findAncestor(root, aResult, ancestorList);
            votingList.add(ancestorList);
        }

        if (votingList.size() == 0) {
            return null;
        }

        // calculates the confidence for the common treenode at the votingList
        // then finds the RankAssignment which has highest confidence at each level
        int ancestorIndex = 0;
        List greedyList = new ArrayList();
        // we assume that every voter will agree with the root assignment
        greedyList.add(((List) votingList.get(0)).get(ancestorIndex));
        ((RankAssignment) ((List) votingList.get(0)).get(ancestorIndex)).setConfidence(1);

        while (root.getSizeofSubclasses() > 0) {
            ancestorIndex++;
            RankAssignment bestNode = null;
            List tempList = new ArrayList();

            for (int i = 0; i < votingList.size(); i++) {
                boolean found = false;
                Iterator tmpIt = tempList.iterator();
                RankAssignment aResult = null;
                if (((List) votingList.get(i)).size() > ancestorIndex) {
                    aResult = (RankAssignment) (((List) votingList.get(i)).get(ancestorIndex));
                } else {
                    continue;
                }

                while (tmpIt.hasNext()) {
                    RankAssignment tmp = (RankAssignment) tmpIt.next();
                    if (aResult.getBestClass().getTaxid() == tmp.getBestClass().getTaxid()) {
                        tmp.setConfidence(tmp.getConfidence() + aResult.getConfidence());
                        if (tmp.getBestClass().getParent().getTaxid() == root.getTaxid()
                                && tmp.getConfidence() > bestNode.getConfidence()) {
                            bestNode = tmp;
                        }
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    tempList.add(aResult);
                    if (aResult.getBestClass().getParent().getTaxid() == root.getTaxid()) {
                        if (bestNode == null) {
                            bestNode = aResult;
                        } else if (aResult.getConfidence() > bestNode.getConfidence()) {
                            bestNode = aResult;
                        }
                    }
                }
            }
            bestNode.setConfidence(bestNode.getConfidence() / (float) NUM_OF_RUNS);
            greedyList.add(bestNode);
            root = bestNode.getBestClass();

        }
        return greedyList;
    }
}
