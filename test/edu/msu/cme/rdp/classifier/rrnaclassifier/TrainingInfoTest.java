/*
 * TrainingInfoTest.java
 * 
 * Copyright 2006 Michigan State University Board of Trustees
 *
 * Created on September 19, 2003, 10:23 AM
 */

package edu.msu.cme.rdp.classifier.rrnaclassifier;

import edu.msu.cme.rdp.classifier.ClassificationResult;
import edu.msu.cme.rdp.classifier.Classifier;
import edu.msu.cme.rdp.classifier.GenusWordConditionalProb;
import edu.msu.cme.rdp.classifier.HierarchyTree;
import edu.msu.cme.rdp.classifier.RankAssignment;
import edu.msu.cme.rdp.classifier.TrainingInfo;
import edu.msu.cme.rdp.classifier.utils.ClassifierSequence;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * A test class for TrainingInfo.
 * @author wangqion
 */
public class TrainingInfoTest extends TestCase {
    
    public TrainingInfoTest(java.lang.String testName) {
        super(testName);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(TrainingInfoTest.class);
        
        return suite;
    }
    
    /** Test of createTree method, of class classification.TrainingInfo. */
    public void testCreateTree() {
        System.out.println("testCreateTree");
        
    }
    
    /** Test of createLogWordPriorArr method, of class classification.TrainingInfo. */
    public void testCreateLogWordPriorArr() throws Exception{
        System.out.println("testCreateLogWordPriorArr");
        InputStream dstream = System.class.getResourceAsStream("/test/classifier/testLogWordPrior.txt");
        Reader in =  new InputStreamReader( dstream );
        
        TrainingInfo train = new TrainingInfo();
        train.createLogWordPriorArr(in);
        float wordPrior = train.getLogWordPrior(10);
        assertEquals(wordPrior, -2.77, 0.1);
        wordPrior = train.getLogWordPrior(55);
        assertEquals(wordPrior, -1.67, 0.1);
        
        // test getWordPairPriorDiff()
        wordPrior = train.getLogWordPrior(24573);
        assertEquals(wordPrior, -2.77, 0.01);
        float revWordPrior = train.getLogWordPrior(10912);
        assertEquals(revWordPrior, -1.67, 0.01);        
        
        float wordPriorDiff = train.getWordPairPriorDiff(24573);
        assertEquals(wordPriorDiff, (wordPrior - revWordPrior), 0.01);
        float revWordPriorDiff = train.getWordPairPriorDiff(10912);
        assertEquals(revWordPriorDiff, (revWordPrior - wordPrior), 0.01);
        assertEquals( (wordPriorDiff + revWordPriorDiff), 0, 0.01);
        
    }
    
    /** Test of createGenusWordProbLis method, of class classification.TrainingInfo. */
    public void testCreateGenusWordProbList() throws Exception{
        System.out.println("testCreateGenusWordConditionalProbList");
        InputStream dstream = System.class.getResourceAsStream("/test/classifier/testGenus_probList.txt");
        Reader in =  new InputStreamReader( dstream );
        TrainingInfo train = new TrainingInfo();
        train.createGenusWordProbList(in);
        GenusWordConditionalProb gProb = train.getWordConditionalProbObject(8);
        int genusIndex = gProb.getGenusIndex();
        float prob = gProb.getProbability();
        assertEquals(genusIndex, 4);
        assertEquals(prob, -0.15, 0.1);
        
        gProb = train.getWordConditionalProbObject(1865);
        genusIndex = gProb.getGenusIndex();
        prob = gProb.getProbability();
        assertEquals(genusIndex, 0);
        assertEquals(prob, -0.5, 0.1);
        
    }
    
    
    /** Test of makeProbIndexArr method, of class classification.TrainingInfo. */
    public void testCreateProbIndexArr() throws Exception {
        System.out.println("testCreateProbIndexArr");
        InputStream dstream = System.class.getResourceAsStream("/test/classifier/testProbIndex.txt");
        Reader in =  new InputStreamReader( dstream );
        TrainingInfo train = new TrainingInfo();
        train.createProbIndexArr(in);
        int start = train.getStartIndex(100);
        int stop = train.getStopIndex(65535);
        assertEquals(start, 1);
        assertEquals(stop, 1866);
        
    }
    
    /** Test of getRootTree method, of class classification.TrainingInfo. */
    public void testCreateClassifier() throws Exception{
        System.out.println("testCreateClassifier");
        TrainingInfo train = new TrainingInfo();
        
        InputStream dstream = System.class.getResourceAsStream("/test/classifier/testGenus_probList.txt");
        Reader in =  new InputStreamReader( dstream );
        train.createGenusWordProbList(in);
        
        dstream = System.class.getResourceAsStream("/test/classifier/test_bergeyTrainingTree.xml");
        in =  new InputStreamReader( dstream );
        train.createTree(in);
        
        dstream = System.class.getResourceAsStream("/test/classifier/testProbIndex.txt");
        in =  new InputStreamReader( dstream );
        train.createProbIndexArr(in);
        
        dstream = System.class.getResourceAsStream("/test/classifier/testLogWordPrior.txt");
        in =  new InputStreamReader( dstream );
        train.createLogWordPriorArr(in);
        
        int genusNodeListSize = train.getGenusNodeListSize();
        assertEquals(genusNodeListSize, 6);
        
        HierarchyTree genusNode = train.getGenusNodebyIndex(3);
        assertEquals(genusNode.getGenusIndex(), 3);
        assertEquals(genusNode.getName(), "Pseudomonas");
        int leaveCount = genusNode.getLeaveCount();
        float logLeaveCount = train.getLogLeaveCount(1);
        assertEquals((double)leaveCount, Math.exp(logLeaveCount) -1, 0.1);
        
        HierarchyTree rootTree = train.getRootTree();
        assertEquals(rootTree.getName(), "Bacteria");
        assertNotNull( rootTree.getSubclasses());
        
        Classifier aClassifier = train.createClassifier();
        
        //test addConfidence()
        
        HashMap<HierarchyTree, RankAssignment> determinedMap = new HashMap <HierarchyTree, RankAssignment>();
		HierarchyTree determinedGenusNode = train.getGenusNodebyIndex(4);
		HierarchyTree Enterobacteriales = determinedGenusNode.getParent().getParent();
		HierarchyTree Gammaproteobacteria = Enterobacteriales.getParent();
		
		HierarchyTree aNode = determinedGenusNode;
		while (aNode != null){			
			determinedMap.put(aNode, new RankAssignment(aNode, 0));
			aNode = aNode.getParent();
		}
		
		HierarchyTree bestNode = train.getGenusNodebyIndex(1);  // Vibrio
		aClassifier.addConfidence(bestNode, determinedMap);
		assertEquals( determinedMap.get(determinedGenusNode).getConfidence(), 0.0f, 0.0001);	//Enterobacter
		assertEquals( determinedMap.get(Enterobacteriales).getConfidence(), 0.0f, 0.0001);  //Enterobacteriales
		assertEquals( determinedMap.get(Gammaproteobacteria).getConfidence(), 1.0f, 0.0001);  //Gammaproteobacteria
        
		bestNode = train.getGenusNodebyIndex(4);   //Enterobacter
		aClassifier.addConfidence(bestNode, determinedMap);
		assertEquals( determinedMap.get(determinedGenusNode).getConfidence(), 1.0f, 0.0001);	//Enterobacter
		assertEquals( determinedMap.get(Enterobacteriales).getConfidence(), 1.0f, 0.0001);  //Enterobacteriales
		assertEquals( determinedMap.get(Gammaproteobacteria).getConfidence(), 2.0f, 0.0001);  //Gammaproteobacteria
		
		aClassifier.addConfidence(train.getGenusNodebyIndex(4), determinedMap);  //Enterobacter
		aClassifier.addConfidence(train.getGenusNodebyIndex(0), determinedMap);  //Clostridium
		assertEquals( determinedMap.get(determinedGenusNode).getConfidence(), 2.0f, 0.0001);	//Enterobacter
		assertEquals( determinedMap.get(Enterobacteriales).getConfidence(), 2.0f, 0.0001);  //Enterobacteriales
		assertEquals( determinedMap.get(Gammaproteobacteria).getConfidence(), 3.0f, 0.0001);  //Gammaproteobacteria
		assertEquals( determinedMap.get(rootTree).getConfidence(), 4.0f, 0.0001);  //Bacteria
        // end test addConfidence()
        
        
        // end of 
        dstream = System.class.getResourceAsStream("/test/classifier/testQuerySeq.fasta");
        in =  new InputStreamReader( dstream );
        BufferedReader infile = new BufferedReader(in);
        // test the first sequence
        String sequence = "";
        infile.readLine();
        sequence = infile.readLine();
        sequence = sequence.toUpperCase();
        ClassifierSequence pSeq = new ClassifierSequence("name", "title", sequence);
        ClassificationResult result = aClassifier.classify(pSeq);
        
        Iterator it = result.getAssignments().iterator();
        RankAssignment classResult = (RankAssignment) it.next();
        assertEquals(classResult.getBestClass().getName() , rootTree.getName());
        assertEquals(classResult.getConfidence(), 1.0, 0.1);
        classResult = (RankAssignment) it.next();
        assertEquals(classResult.getBestClass().getName() , "Proteobacteria");
        it.next();
        classResult = (RankAssignment) it.next();
        assertEquals(classResult.getBestClass().getName() , "Rhizobiales");
        it.next();
        classResult = (RankAssignment) it.next();
        assertEquals(classResult.getBestClass().getName() , "Rhizobium");
        
        //displayResult(result);
        // end of test first sequence
        
        // test the second sequence
        infile.readLine();
        sequence = infile.readLine();
        sequence = sequence.toUpperCase();
        pSeq = new ClassifierSequence("name", "title", sequence);
        result = aClassifier.classify(pSeq);
        
        
        it = result.getAssignments().iterator();
        classResult = (RankAssignment) it.next();
        assertEquals(classResult.getBestClass().getName() , rootTree.getName());
        assertEquals(classResult.getConfidence(), 1.0, 0.1);
        classResult = (RankAssignment) it.next();
        assertEquals(classResult.getBestClass().getName() , "Firmicutes");
        it.next();
        classResult = (RankAssignment) it.next();
        assertEquals(classResult.getBestClass().getName() , "Clostridiales");
        it.next();
        classResult = (RankAssignment) it.next();
        assertEquals(classResult.getBestClass().getName() , "Clostridium");
        // end of test second sequence
    }
    
    
    
    private void displayResult(ClassificationResult result){
        List assignments = result.getAssignments();
        Iterator it = assignments.iterator();
        while (it.hasNext()){
            RankAssignment classResult = (RankAssignment) it.next();
            System.err.print("\n" + classResult.getBestClass().getName() + "  " + classResult.getConfidence());
        }
    }
    
    
}
