/*
 * DecisionMaker.java
 *
 * Created on June 24, 2002, 6:05 PM
 */
/**
 *
 * @author  wangqion
 * @version 
 */
package edu.msu.cme.rdp.classifier.train.validation;

import edu.msu.cme.rdp.classifier.train.GoodWordIterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class DecisionMaker {

    public final static int NUM_OF_RUNS = 100;  // 100 bootstraps
    private int numOfRuns = NUM_OF_RUNS;   // default is 100 for bootstrap
    private TreeFactory factory ;
    private Random randomGenerator = new Random();
    public static final long seed = 1;

    /** Creates new DecisionMaker */
    public DecisionMaker(TreeFactory factory) {
        this.factory = factory;
    }

    /** For each sequence, find the best class among the list of genera using all the overlapping words.
     * Then select a subset of words to get the assignment, repeats 100 times, count the number of votes
     */
    public List<ValidationClassificationResult> getBestClasspath( GoodWordIterator iterator, 
            HashMap<String, HierarchyTree> nodeMap, boolean useSeed, int min_bootstrap_words) throws IOException {

        ValidationClassificationResult result = null;

        try {
            int[] wordList = iterator.getWordArr();
            if (useSeed) {
                randomGenerator.setSeed(seed);
            }
           
            // first determine the assignment with all the words.
            ValidationClassificationResult determinedResult = NBClassifier.assignClass(wordList, factory, nodeMap);
            //System.err.println( " determinedResult=" + determinedResult.getBestClass().getName() + " posteriorProb=" +  determinedResult.getPosteriorProb());
            HashMap determinedMap = new HashMap();
            HierarchyTree aNode = determinedResult.getBestClass();
            while (aNode != null) {
                determinedMap.put(aNode, new ValidationClassificationResult(aNode, 0, 0));
                aNode = aNode.getParent();
            }

            numOfRuns = NUM_OF_RUNS;

            for (int i = 0; i < numOfRuns; i++) {
                int [] testWordList = GoodWordIterator.getRdmWordArr(wordList, min_bootstrap_words, randomGenerator);
                result = NBClassifier.assignClass(testWordList, factory, nodeMap);
                addResult(result, determinedMap);
            }
           
            
            return getFinalResultList(determinedMap, determinedResult);

        } catch (IllegalStateException ex) {
            System.err.println(ex.toString());
            return new ArrayList();
        }

    }
    
    
    

    void addResult(ValidationClassificationResult result, HashMap map) {
        HierarchyTree node = result.getBestClass();
        while (node != null) {
            ValidationClassificationResult assign = (ValidationClassificationResult) map.get(node);
            if (assign != null) {
                assign.setNumOfVotes(assign.getNumOfVotes() + 1.0f);
            }
            node = node.getParent();
        }
    }
    
    private List getFinalResultList(HashMap map, ValidationClassificationResult determinedResult) {
        List finalAssigns = new ArrayList();
        HierarchyTree aNode = determinedResult.getBestClass();
        while (aNode != null) {
            ValidationClassificationResult assign = (ValidationClassificationResult) map.get(aNode);
            assign.setPosteriorProb(determinedResult.getPosteriorProb());
            // change the numberOfVotes to the percent of the total
            assign.setNumOfVotes(assign.getNumOfVotes() / numOfRuns);
            finalAssigns.add(0, assign);
            aNode = aNode.getParent();
        }
        return finalAssigns;
    }
}
