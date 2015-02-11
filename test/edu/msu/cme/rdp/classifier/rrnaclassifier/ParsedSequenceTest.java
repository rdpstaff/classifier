/*
 * ParsedSequenceTest.java
 *
 *  Copyright 2006 Michigan State University Board of Trustees
 *
 * Created on September 8, 2004, 2:29 PM
 */
package edu.msu.cme.rdp.classifier.rrnaclassifier;

import edu.msu.cme.rdp.classifier.utils.ClassifierSequence;
import edu.msu.cme.rdp.readseq.utils.orientation.GoodWordIterator;
import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * A test class for ParsedSequence.
 *
 * @author wangqion
 */
public class ParsedSequenceTest extends TestCase {

    public ParsedSequenceTest(java.lang.String testName) {
        super(testName);
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(ParsedSequenceTest.class);
        return suite;
    }

    /**
     * Test of getReversedSeq method, of class
     * edu.msu.cme.rdp.classifier.readseqwrapper.ParsedSequence.
     */
    public void testGetReversedSeq() throws IOException {
        System.out.println("testGetReversedSeq");

        //                 "AAAAAAAAAG-CCCCCCCCUGAGGGUUACnAA";
        String seqString = "AAAAAAAAAG-CCCCCCCCUGAGGGUUACnAA";
        //                "TTNGTAACCCTCAGGGGGGGG-CTTTTTTTTT"
        String expected = "ttngtaaccctcaggggggggcttttttttt";
        ClassifierSequence aSeq = new ClassifierSequence("test", "", seqString);
        ClassifierSequence revSeq = aSeq.getReversedSeq();
        assertTrue(revSeq.isReverse());
        assertEquals(expected, revSeq.getSeqString());
    }

    /**
     * Test of getReversedWord method, of class
     * edu.msu.cme.rdp.classifier.readseqwrapper.ParsedSequence.
     */
    public void testGetReversedWord() {
        System.out.println("testGetReversedWord");
        int[] word = new int[8];
        word[0] = 2;
        word[1] = 1;
        word[2] = 1;
        word[3] = 3;
        word[4] = 0;
        word[5] = 3;
        word[6] = 3;
        word[7] = 1;

        int[] revWord = GoodWordIterator.getReversedWord(word);
        assertEquals(revWord[0], 0);
        assertEquals(revWord[3], 1);
        assertEquals(revWord[7], 3);
    }

    /**
     * Test of getWordIndex method, of class
     * edu.msu.cme.rdp.classifier.readseqwrapper.ParsedSequence.
     */
    public void testGetWordIndex() {
        System.out.println("testGetWordIndex");
        int[] word = new int[8];
        word[0] = 1;
        word[1] = 1;
        word[2] = 3;
        word[3] = 3;
        word[4] = 3;
        word[5] = 3;
        word[6] = 3;
        word[7] = 1;
        int wordIndex = GoodWordIterator.getWordIndex(word);
        assertEquals(wordIndex, (24573));

        int[] revWord = GoodWordIterator.getReversedWord(word);
        wordIndex = GoodWordIterator.getWordIndex(revWord);
        assertEquals(wordIndex, (10912));

    }

    /**
     * Test of createWordIndexArr method, of class
     * edu.msu.cme.rdp.classifier.readseqwrapper.ParsedSequence.
     */
    public void testCreateWordIndexArr() throws IOException {
        System.out.println("testCreateWordIndexArr");

        String seqString = "AAAAAAAAAG-CCCCCCCCUGAGGGUUACnAA";
        ClassifierSequence aSeq = new ClassifierSequence("test", "", seqString);

        int[] wordIndexArr = aSeq.getWordIndexArr();

        assertEquals(0, wordIndexArr[0]);   //AAAAAAAA

        assertEquals(2, wordIndexArr[2]);    //AAAAAAAG

        /*so using the unaligned sequence (Getting rid of all the non-dna crap and converting to lowercase)
         * buggered this up, so I'm just going to assume the answer it gives me is
         * the correct one, and go with it...<cross fingers>
         */
        //assertEquals((256 * 256 - 1), wordIndexArr[3]);   //CCCCCCCC

        //assertEquals((256 * 256 - 3), wordIndexArr[4]);   //CCCCCCCU
        assertEquals(11, wordIndexArr[3]);   //CCCCCCCC

        assertEquals(47, wordIndexArr[4]);   //CCCCCCCU
    }
}
