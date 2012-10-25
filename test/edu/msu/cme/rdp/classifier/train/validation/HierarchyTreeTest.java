
/*
 * HierarchyTreeTest.java
 *
 * Created on June 25, 2002, 3:59 PM
 */
package edu.msu.cme.rdp.classifier.train.validation;

import edu.msu.cme.rdp.classifier.train.GoodWordIterator;
import edu.msu.cme.rdp.classifier.train.LineageSequence;
import edu.msu.cme.rdp.classifier.train.LineageSequenceParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author wangqion
 */
public class HierarchyTreeTest extends TestCase {

    public HierarchyTreeTest(java.lang.String testName) {
        super(testName);
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testInitWordOccurrence() throws FileNotFoundException, IOException {
        float[] wordPriorArr = new float[65536];
        Taxonomy rootTaxon = new Taxonomy(1, "ROOT", 0, 0, "FAMILY");
        HierarchyTree root = new HierarchyTree("ROOT", null, rootTaxon);

        Taxonomy c1Taxon = new Taxonomy(2, "child1",1, 1, "GENUS");
        HierarchyTree c1 = new HierarchyTree("child1", root, c1Taxon);
        Taxonomy c2Taxon = new Taxonomy(3,"child2", 1, 1, "GENUS");
        HierarchyTree c2 = new HierarchyTree("child2", root, c2Taxon);

        File in = new File(System.class.getResource("/test/classifier/testTrainingSet.fasta").getFile());

        LineageSequenceParser parser = new LineageSequenceParser(in);
        // test the first sequence
        LineageSequence pSeq = parser.next();
        c1.initWordOccurrence(pSeq, wordPriorArr);

        pSeq = parser.next();
        c1.initWordOccurrence(pSeq, wordPriorArr);

        pSeq = parser.next();
        c2.initWordOccurrence(pSeq, wordPriorArr);

        assertEquals(2, c1.getNumOfLeaves());
        assertEquals(1, c2.getNumOfLeaves());
        assertEquals(3, root.getNumOfLeaves());
        assertEquals(2, root.getSizeofSubclasses());

        assertEquals(1, c1.getWordOccurrence(0)); //AAAAAAAA
        assertEquals(2, c1.getWordOccurrence(2)); //AAAAAAAG
        assertEquals(1, c1.getWordOccurrence(65535)); //CCCCCCCC


        assertEquals(1, c2.getWordOccurrence(64)); //AAAAUAAA
        assertEquals(0, c2.getWordOccurrence(0));

        assertTrue(!root.isWordOccurDone());

        root.createWordOccurrenceFromSubclasses();
        assertEquals(1, root.getWordOccurrence(0));
        assertEquals(2, root.getWordOccurrence(65535));
        assertEquals(3, root.getWordOccurrence(39));   //AAAAAGUC

        for (int i = 0; i < wordPriorArr.length; i++) {
            if (wordPriorArr[i] != 0) {
                //System.err.println("word: " + i + "   Ccc : " + wordPriorArr[i] ); 
            }
            wordPriorArr[i] = (wordPriorArr[i] + 0.5f) / (3f + 1f);

            if (i == 0) {
                assertEquals(wordPriorArr[i], 0.375f, 0.5f);
            }
        }

    }

    /** Test of getWordOccurrence method, of class Classification.HierarchyTree. */
    public void testHideSeq() throws IOException {
        System.out.println("testHideSeq");

        float[] wordPriorArr = new float[65536];
        Taxonomy rootTaxon = new Taxonomy(1, "ROOT", 0, 0, "FAMILY");
        HierarchyTree root = new HierarchyTree("ROOT", null, rootTaxon);

        Taxonomy c1Taxon = new Taxonomy(2, "child1", 1, 1, "GENUS");
        HierarchyTree c1 = new HierarchyTree("child1", root, c1Taxon);
        Taxonomy c2Taxon = new Taxonomy(3, "child2", 1, 1, "GENUS");
        HierarchyTree c2 = new HierarchyTree("child2", root, c2Taxon);
        File in = new File(System.class.getResource("/test/classifier/testTrainingSet.fasta").getFile());

        LineageSequenceParser parser = new LineageSequenceParser(in);
        // test the first sequence
        LineageSequence pSeq = parser.next();
        c1.initWordOccurrence(pSeq, wordPriorArr);

        pSeq = parser.next();
        c1.initWordOccurrence(pSeq, wordPriorArr);

        pSeq = parser.next();
        c2.initWordOccurrence(pSeq, wordPriorArr);

        assertEquals(2, c1.getNumOfLeaves());
        assertEquals(1, c2.getNumOfLeaves());
        assertEquals(3, root.getNumOfLeaves());

        assertEquals(1, c1.getWordOccurrence(0)); //AAAAAAAA
        assertEquals(2, c1.getWordOccurrence(2)); //AAAAAAAG
        assertEquals(1, c1.getWordOccurrence(65535)); //CCCCCCCC    

        assertEquals(1, c2.getWordOccurrence(64)); //AAAAUAAA
        assertEquals(0, c2.getWordOccurrence(0));

        root.createWordOccurrenceFromSubclasses();
        assertEquals(1, root.getWordOccurrence(0));
        assertEquals(2, root.getWordOccurrence(65535));
        assertEquals(3, root.getWordOccurrence(39));   //AAAAAGUC


        //test hide and unhide sequence 1 which belong to child1
        String seq1 = "AAAAAAAAAGUCACCCCCCCCUGA";     // belong to child1      

        GoodWordIterator seq_iterator = new GoodWordIterator(seq1);
        assertEquals(2, c1.getNumOfLeaves());
        c1.hideSeq(seq_iterator);
        assertEquals(1, c1.getNumOfLeaves());
        assertEquals(1, c2.getNumOfLeaves());
        assertEquals(2, root.getNumOfLeaves());

        assertEquals(0, c1.getWordOccurrence(0)); //AAAAAAAA
        assertEquals(1, c1.getWordOccurrence(2)); //AAAAAAAG
        // assertEquals(2, root.getWordOccurrence(39) );   //AAAAAGUC

        c1.unhideSeq(seq_iterator);
        assertEquals(2, c1.getNumOfLeaves());
        assertEquals(3, root.getNumOfLeaves());

        assertEquals(1, c1.getWordOccurrence(0)); //AAAAAAAA     
        // assertEquals(2, root.getWordOccurrence(65535) );   //CCCCCCCC

        //est hide and unhide sequence 2 which belong to child2
        String seq2 = "AAAAUAAAAAGUCCCCCCCCUG"; // belong to child2
        seq_iterator = new GoodWordIterator(seq2);
        c2.hideSeq(seq_iterator);
        assertEquals(2, c1.getNumOfLeaves());
        assertEquals(0, c2.getNumOfLeaves());
        assertEquals(2, root.getNumOfLeaves());

        assertEquals(1, c1.getWordOccurrence(0)); //AAAAAAAA
        assertEquals(0, c2.getWordOccurrence(2)); //AAAAAAAG
        // assertEquals(2, root.getWordOccurrence(39) );   //AAAAAGUC

        c2.unhideSeq(seq_iterator);
        assertEquals(2, c1.getNumOfLeaves());
        assertEquals(1, c2.getNumOfLeaves());
        assertEquals(3, root.getNumOfLeaves());

        assertEquals(1, c2.getWordOccurrence(64)); //AAAAUAAA
        //assertEquals(3, root.getWordOccurrence(39) );   //AAAAAGUC      

    }

    public static Test suite() {
        TestSuite suite = new TestSuite(HierarchyTreeTest.class);

        return suite;
    }
}
