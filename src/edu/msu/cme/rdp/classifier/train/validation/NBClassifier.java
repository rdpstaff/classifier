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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public class NBClassifier {

    public static final int MIN_BOOTSTRSP_WORDS = 5;
    private static final float F2 = (float) 1;
    private static final float F1 = (float) 0.001;  // the uniform word prior
    private static Random randomSelectGenera = new Random();
    


    /**
     * for a array of subclasses, gets the probabilities of each subclass,
     * returns the classification whose subclass has the highest probability.
     * formula: prob = sum( log(p (wi|c) ) ) i = 1 to n for n words denominator
     * = log ( sum( exp (prob)j ) ) for j = 1 to m for m classes final log
     * posterior prob = prob + p(c) - denominator
     */
    static ValidationClassificationResult assignClass(int[] wordList, TreeFactory f, HashMap<String, HierarchyTree> nodeMap) {
        
        Iterator<HierarchyTree> it = nodeMap.values().iterator();
        float result = Float.NEGATIVE_INFINITY;
        HierarchyTree bestClass = null;
        float sum = Float.NEGATIVE_INFINITY;
        float prior = (float) Math.log(nodeMap.size());
        float tmp;

        HashMap<String, Float> tempScoreMap = new HashMap<String, Float>();
        boolean tied = false;
        while (it.hasNext()) {
            HierarchyTree tree = it.next();

            tmp = calculateProb(f, tree, wordList) - prior;
            tempScoreMap.put(tree.getName(), tmp);
            sum = (float) AddLogs.add(tmp, sum);
            if (tmp >= result) {
                if (tmp != result) {
                    tied = false;
                    result = tmp;
                    bestClass = tree;
                } else {
                    tied = true;
                }

            }
        }
        // if scores tied between nodes, randomly pick one    
        if (tied) {
            ArrayList<HierarchyTree> possibleSet = new ArrayList<HierarchyTree>();
            for (String name : tempScoreMap.keySet()) {
                if (tempScoreMap.get(name) == result) {
                    possibleSet.add(nodeMap.get(name));
                }
            }

            bestClass = possibleSet.get(randomSelectGenera.nextInt(possibleSet.size()));
        }

        result = result - sum;

        return (new ValidationClassificationResult(bestClass, result, 1.0f));
    }

    /**
     * Calculates the log probability for the tree node
     */
    private static float calculateProb(TreeFactory factory, HierarchyTree tree, int[] testWordList) {
        float prob = 0;
        float size = (float) tree.getNumOfLeaves();

        for (int i = 0; i < testWordList.length; i++) {
            int wordOccurrence = tree.getWordOccurrence(testWordList[i]);
            float wordPrior = factory.getWordPrior(testWordList[i]);
            prob += Math.log((wordOccurrence + wordPrior) / (size + F2));   //specific word prior
            // System.err.println(">>"+  testWordList[i] + " w=" + wordOccurrence + " prior: " + wordPrior + " s=" + size + " logprob: " + prob + " p=" + (wordOccurrence + wordPrior) / (size + F2));
        }
        return prob;
    }

    
}
