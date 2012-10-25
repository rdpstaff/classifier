
/*
 * RawHierarchyTreeTest.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 * 
 * Created on June 25, 2002, 3:59 PM
 */
package edu.msu.cme.rdp.classifier.train;

import junit.framework.*;
import java.io.*;

/**
 * A test class for RawHierarchyTree.
 * @author wangqion
 */
public class RawHierarchyTreeTest extends TestCase {

    public RawHierarchyTreeTest(java.lang.String testName) {
        super(testName);
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Tests the RawHierarchyTree initWordOccurrence() methods.
     */
    public void testInitWordOccurrence() throws FileNotFoundException, IOException {
        System.out.println("testInitWordOccurrence()");
        float[] wordPriorArr = new float[65536];
        Taxonomy rootTaxon = new Taxonomy(1, "Fam1", 0, 0, "FAMILY");
        RawHierarchyTree root = new RawHierarchyTree("ROOT", null, rootTaxon);

        Taxonomy c1Taxon = new Taxonomy(2, "G1", 1, 1, "GENUS");
        RawHierarchyTree c1 = new RawHierarchyTree("child1", root, c1Taxon);
        Taxonomy c2Taxon = new Taxonomy(3, "G2", 1, 1, "GENUS");
        RawHierarchyTree c2 = new RawHierarchyTree("child2", root, c2Taxon);

        LineageSequenceParser parser = new LineageSequenceParser(System.class.getResourceAsStream("/test/classifier/testTrainingSet.fasta"));
        // test the first sequence
        LineageSequence pSeq = parser.next();
        c1.initWordOccurrence(pSeq, wordPriorArr);

        pSeq = parser.next();
        c1.initWordOccurrence(pSeq, wordPriorArr);

        pSeq = parser.next();
        c2.initWordOccurrence(pSeq, wordPriorArr);

        assertEquals(2, c1.getLeaveCount());
        assertEquals(1, c2.getLeaveCount());
        assertEquals(3, root.getLeaveCount());
        assertEquals(2, root.getSizeofSubclasses());

        assertEquals(1, c1.getWordOccurrence(0)); //AAAAAAAA
        assertEquals(2, c1.getWordOccurrence(2)); //AAAAAAAG
        assertEquals(1, c1.getWordOccurrence(65535)); //CCCCCCCC


        assertEquals(1, c2.getWordOccurrence(64)); //AAAAUAAA
        assertEquals(0, c2.getWordOccurrence(0));

        assertEquals(c1.getParent().getName(), root.getName());

        for (int i = 0; i < wordPriorArr.length; i++) {
            if (wordPriorArr[i] != 0) {
                // System.err.println("word: " + i + "   Ccc : " + wordPriorArr[i] ); 
            }
            wordPriorArr[i] = (float) ((wordPriorArr[i] + 0.5) / (3 + 1));

            if (i == 0) {
                assertEquals(wordPriorArr[i], 0.375, 0.5);
            }
        }

    }

    public static Test suite() {
        TestSuite suite = new TestSuite(RawHierarchyTreeTest.class);

        return suite;
    }
}
