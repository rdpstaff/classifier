
/*
 * NBClassifierTest.java
 * NetBeans JUnit based test
 *
 * Created on June 25, 2002, 3:59 PM
 */
package edu.msu.cme.rdp.classifier.train.validation;

import edu.msu.cme.rdp.classifier.train.GoodWordIterator;
import edu.msu.cme.rdp.classifier.train.LineageSequence;
import edu.msu.cme.rdp.classifier.train.LineageSequenceParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author wangqion
 */
public class NBClassifierTest extends TestCase {

    public NBClassifierTest(java.lang.String testName) {
        super(testName);
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /** Test of calculateProb method, of class Classification.NBClassifier. */
    public void testAssignClass() throws FileNotFoundException, IOException {
        System.out.println("testassignClass");

        int min_bootstrap_words = 20;
        Reader taxReader = new FileReader(System.class.getResource("/test/classifier/testTaxon.txt").getFile());

        TreeFactory factory = new TreeFactory(taxReader);

        File infileReader = new File(System.class.getResource("/test/classifier/testNBClassifierSet.fasta").getFile());

        LineageSequenceParser parser = new LineageSequenceParser(infileReader);

        while (parser.hasNext()) {
            factory.addSequence((LineageSequence) parser.next());
        }
        //after all the training set is being parsed, calculate the prior probability for all the words.

        factory.calculateWordPrior();

        HashMap<String, HierarchyTree> nodeMap = new HashMap<String, HierarchyTree>();
        HierarchyTree root = factory.getRoot();
        root.getNodeMap("GENUS", nodeMap);

        // test the first sequence
        File queryReader = new File(System.class.getResource("/test/classifier/testNBClassifierSet.fasta").getFile());

        parser = new LineageSequenceParser(queryReader);
        LineageSequence pSeq = parser.next();

        assertEquals(pSeq.getSeqName(), "XG1_child1");
        Random randomGenerator = new Random(DecisionMaker.seed);
        GoodWordIterator iterator = new GoodWordIterator(pSeq.getSeqString());
        int [] testWordList = GoodWordIterator.getRdmWordArr(iterator.getWordArr(), min_bootstrap_words, randomGenerator);
        assertEquals(testWordList.length, min_bootstrap_words);
        ValidationClassificationResult result = NBClassifier.assignClass(testWordList, factory, nodeMap);
        assertEquals("G1", ((HierarchyTree) result.getBestClass()).getName());
        assertTrue(0.1 > result.getPosteriorProb());


        pSeq = parser.next();
        // test the 3rd getSequence()
        pSeq = parser.next();
        assertEquals(pSeq.getSeqName(), "XG2_child1");
        iterator = new GoodWordIterator(pSeq.getSeqString());
        testWordList = GoodWordIterator.getRdmWordArr(iterator.getWordArr(), min_bootstrap_words, randomGenerator);
        result = NBClassifier.assignClass(testWordList, factory,  nodeMap);
        assertEquals("G2", ((HierarchyTree) result.getBestClass()).getName());
        assertTrue(0.2 > result.getPosteriorProb());


        pSeq = parser.next();
        // test the 5th getSequence()
        pSeq = parser.next();
        assertEquals(pSeq.getSeqName(), "XPh2G6_child1");
        iterator = new GoodWordIterator(pSeq.getSeqString());
        result = NBClassifier.assignClass(iterator.getWordArr(), factory, nodeMap);
        assertEquals("G1", ((HierarchyTree) result.getBestClass()).getName());
        assertTrue(0.2 > result.getPosteriorProb());

        //test the 8th sequence in G7, it is the same as the 9th sequence in G8
        // the classifier should randomly choose a genus (either G7 or G8) because the score will be tie
        parser.next();
        parser.next();
        pSeq = parser.next();
        assertEquals(pSeq.getSeqName(), "XPh2G7_child1");
        iterator = new GoodWordIterator(pSeq.getSeqString());
        int G7_count = 0;
        int G8_count = 0;
        for ( int run = 0; run < DecisionMaker.NUM_OF_RUNS; run++){
            result = NBClassifier.assignClass(iterator.getWordArr(), factory, nodeMap);
            if ( ((HierarchyTree) result.getBestClass()).getName().equals("G7")){
                G7_count ++;
            }else if ( ((HierarchyTree) result.getBestClass()).getName().equals("G8")){
                G8_count ++;
            }
        }
        assertEquals( G7_count + G8_count, DecisionMaker.NUM_OF_RUNS, 0.1);
        // each genus should be choosen at least once
        assertTrue(G7_count > 1);
        assertTrue(G8_count > 1);
       
        
    }


    public static Test suite() {
        TestSuite suite = new TestSuite(NBClassifierTest.class);

        return suite;
    }
}
