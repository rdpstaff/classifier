
/*
 * TreeFactoryTest.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 * Created on June 25, 2002, 3:59 PM
 */
package edu.msu.cme.rdp.classifier.train;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * A test class for TreeFactory.
 * @author wangqion
 */
public class TreeFactoryTest extends TestCase {

    public TreeFactoryTest(java.lang.String testName) {
        super(testName);
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /** Test of addSequence method, of class edu.msu.cme.rdp.classifier.train.TreeFactory. 
     * @throws NameRankDupException */
    public void testAddSequence() throws IOException, NameRankDupException {
        System.out.println("testAddSequence");

        InputStream aStream = System.class.getResourceAsStream("/test/classifier/testTaxon.txt");
        Reader taxReader = new InputStreamReader(aStream);

        TreeFactory factory = new TreeFactory(taxReader, 1, "version", "mod1");

        InputStream inStream = System.class.getResourceAsStream("/test/classifier/testNBClassifierSet.fasta");

        LineageSequenceParser parser = new LineageSequenceParser(inStream);

        while (parser.hasNext()) {
            factory.addSequence((LineageSequence) parser.next());
        }
        //after all the training set is being parsed, calculate the prior probability for all the words.

        factory.createGenusWordConditionalProb();
        assertEquals(factory.getRoot().getName(), "ROOT");

        List genusNodeList = factory.getGenusNodeList();
        assertEquals(genusNodeList.size(), 6);
        assertEquals(((RawHierarchyTree) genusNodeList.get(1)).getName(), "G2");

        HashSet aSet = new HashSet();
        aSet.add("G1");
        aSet.add("G6");

        int start = factory.getStartIndex(0);  // AAAAAAAA in G1 and G6
        int stop = factory.getStopIndex(0);
        assertEquals((stop - start), 2);
        RawGenusWordConditionalProb p = factory.getWordConditionalProb(start);
        RawHierarchyTree aTree = (RawHierarchyTree) genusNodeList.get(p.getGenusIndex());
        assertTrue(aSet.contains(aTree.getName()));
        aSet.remove(aTree.getName());

        p = factory.getWordConditionalProb(start + 1);
        aTree = (RawHierarchyTree) genusNodeList.get(p.getGenusIndex());
        assertTrue(aSet.contains(aTree.getName()));

        start = factory.getStartIndex(33675);    //GAACGAGC only in G6
        stop = factory.getStopIndex(33675);
        assertEquals((stop - start), 1);

        p = factory.getWordConditionalProb(start);
        aTree = (RawHierarchyTree) genusNodeList.get(p.getGenusIndex());

        assertEquals(aTree.getName(), "G6");


    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TreeFactoryTest.class);

        return suite;
    }
}
