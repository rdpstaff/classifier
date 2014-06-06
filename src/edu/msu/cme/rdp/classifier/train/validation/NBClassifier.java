/*
 * NBClassifier.java
 *
 * Created on June 24, 2002, 5:52 PM
 */
/**
 *
 * @author wangqion
 * @version
 */
package edu.msu.cme.rdp.classifier.train.validation;

import static edu.msu.cme.rdp.readseq.utils.orientation.GoodWordIterator.PERCENT_SELECTION;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class NBClassifier {

    public static final int MIN_BOOTSTRSP_WORDS = 5;
    private static final float F2 = (float) 1;
    private static final float F1 = (float) 0.001;  // the uniform word prior
    private static Random randomSelectGenera = new Random();
    private final Random randomGenerator = new Random();
    public static final long seed = 1;
    
    private final TreeFactory factory;
    private final int[] wordList ;
    private final int nodeListSize;
    private float[][] querySeq_wordProbArr ;
    private final ArrayList<HierarchyTree> nodeList ;
    private float[] accumulateProbArr ;
    private int numOfSelection;
    private float prior = 0;

    public NBClassifier(TreeFactory f, int[] wordList, ArrayList<HierarchyTree> nodes, boolean useSeed, int min_bootstrap_words) throws IOException {
        factory = f;
        this.wordList = wordList;
        nodeListSize = nodes.size();        
        prior = (float) Math.log(nodeListSize);
        accumulateProbArr = new float[nodeListSize];
        nodeList= nodes;
        if (useSeed) {
            randomGenerator.setSeed(seed);
        }
        numOfSelection = Math.max((int) (wordList.length * PERCENT_SELECTION), min_bootstrap_words);
    }
    /**
     * for a array of subclasses, gets the probabilities of each subclass,
     * returns the classification whose subclass has the highest probability.
     * formula: prob = sum( log(p (wi|c) ) ) i = 1 to n for n words denominator
     * = log ( sum( exp (prob)j ) ) for j = 1 to m for m classes final log
     * posterior prob = prob + p(c) - denominator
     */
    public ValidationClassificationResult assignClass() {
        
        float result = Float.NEGATIVE_INFINITY;
        HierarchyTree bestClass = null;
        float sum = Float.NEGATIVE_INFINITY;        
        float tmp = 0f;

        HashMap<HierarchyTree, Float> tempScoreMap = new HashMap<HierarchyTree, Float>();
        // will fill the matrix for random sampling
        querySeq_wordProbArr = new float[wordList.length][nodeListSize];
        boolean tied = false;
        for ( int i = 0; i < nodeListSize; i++){
            tmp = calculateProb( i) - prior;
            tempScoreMap.put(nodeList.get(i), tmp);
            sum = (float) AddLogs.add(tmp, sum);
            if (tmp >= result) {
                if (tmp != result) {
                    tied = false;
                    result = tmp;
                    bestClass = nodeList.get(i);
                } else {
                    tied = true;
                }
            }
        }
        // if scores tied between nodes, randomly pick one    
        if (tied) {
            ArrayList<HierarchyTree> possibleSet = new ArrayList<HierarchyTree>();
            for (HierarchyTree tree : tempScoreMap.keySet()) {
                if (tempScoreMap.get(tree) == result) {
                    possibleSet.add(tree);
                }
            }

            bestClass = possibleSet.get(randomSelectGenera.nextInt(possibleSet.size()));
        }
        result = result - sum;
        
        return (new ValidationClassificationResult(bestClass, result, 1.0f));
    }

    /**
     * Calculates the log probability for one node using all the words 
     * 
     */
    private float calculateProb( int nodeIndex) {
        HierarchyTree tree = nodeList.get(nodeIndex);
        float prob = 0;
        float size_f2 = (float) Math.log((float) tree.getNumOfLeaves() + F2);        
        
        for (int i = 0; i < wordList.length; i++) {
            float temp = tree.getWordOccurrence(wordList[i]) + factory.getWordPrior(wordList[i]);
            float logprob = (float) Math.log(temp) - size_f2;
            querySeq_wordProbArr[i][nodeIndex] = logprob;   
            
            prob += logprob ;   //specific word prior              
        }
        return prob ;
    }
    
    
    /**
     * This method random select certain number of words and calculate the probability
     * This can only be called after the assignClass() filled the querySeq_wordProbArr matrix
     * @return the classification whose subclass has the highest probability.
     */
    public ValidationClassificationResult assignClassRandomsample(){
        if (querySeq_wordProbArr == null){
            throw new IllegalStateException("Have to call assignClass() first before calling assignClassRandomsample()");
        }
        float maxPosteriorProb = Float.NEGATIVE_INFINITY;   
        for (int j = 0; j < numOfSelection; j++) {
            int randomIndex = randomGenerator.nextInt(wordList.length);
            for (int node = 0; node < nodeListSize; node++) {
                accumulateProbArr[node] += querySeq_wordProbArr[randomIndex][node];
            }
        }
        int bestNodeIndex = 0;
        boolean tied = false;

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
        // normally we should adjust the final prob value as the same way in assignClass()
        // but we ignore the calculation since we don't use this posterior prob value, it saves time
         maxPosteriorProb -= prior;
         return (new ValidationClassificationResult(nodeList.get(bestNodeIndex), maxPosteriorProb, 1.0f));
    }
    

}
