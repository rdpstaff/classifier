/*
 * SequenceParserTest.java
 * JUnit based test
 *
 * Created on June 27, 2002, 6:48 PM
 */
package edu.msu.cme.rdp.classifier.train.validation;

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
public class SequenceParserTest extends TestCase {

    public SequenceParserTest(java.lang.String testName) {
        super(testName);
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testHasNext() {
        System.out.println("testHasNext");

    }

    public void testNext() throws FileNotFoundException, IOException {
        System.err.println("testNext()");
        File in = new File(System.class.getResource("/test/classifier/testSeqParser.fasta").getFile());

        LineageSequenceParser parser = new LineageSequenceParser(in);
        boolean next = parser.hasNext();
        assertTrue(next);
        // test the first sequence
        LineageSequence pSeq = parser.next();
        String name = "X53199";
        assertEquals(name.toUpperCase(), pSeq.getSeqName());
        assertEquals("AZOSPIRILLUM_GROUP", (String) pSeq.getAncestors().get(4));
        assertEquals("PROTEOBACTERIA", (String) pSeq.getAncestors().get(2));
        assertEquals("GAEA", (String) pSeq.getAncestors().get(0));

        //test the last sequence
        parser.next();
        pSeq = parser.next();
        name = "AB002485";
        assertEquals(name.toUpperCase(), pSeq.getSeqName());
        assertEquals("M.RRR_SUBGROUP", (String) pSeq.getAncestors().get(5));
        assertEquals("TTTT_GROUP", (String) pSeq.getAncestors().get(4));
        assertEquals("BELTA_SUBDIVISION", (String) pSeq.getAncestors().get(3));
        assertEquals("GAEA", (String) pSeq.getAncestors().get(0));
        String sequence = "AAAAUAtttAGUCCCCCCCCUG";

        assertEquals(sequence, pSeq.getSeqString());
        assertTrue(!parser.hasNext());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(SequenceParserTest.class);

        return suite;
    }
    // Add test methods here, they have to start with 'test' name.
    // for example:
    // public void testHello() {}
}
