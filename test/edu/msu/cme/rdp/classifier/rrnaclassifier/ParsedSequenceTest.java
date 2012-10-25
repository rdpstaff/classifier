/*
 * ParsedSequenceTest.java
 * 
 *  Copyright 2006 Michigan State University Board of Trustees
 *
 * Created on September 8, 2004, 2:29 PM
 */

package edu.msu.cme.rdp.classifier.rrnaclassifier;

import edu.msu.cme.rdp.classifier.utils.ClassifierSequence;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * A test class for ParsedSequence.
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
     * Test of getReversedSeq method, of class edu.msu.cme.rdp.classifier.readseqwrapper.ParsedSequence.
     */
    public void testGetReversedSeq() {
        System.out.println("testGetReversedSeq");
        
        String seqString = "AAAAAAAAAG-CCCCCCCCUGAGGGUUACnAA";                
        ClassifierSequence aSeq = new ClassifierSequence("test", "", seqString);
        ClassifierSequence revSeq = aSeq.getReversedSeq();
        assertTrue(revSeq.isReverse());        
        assertEquals("TTNGTAACCCTCAGGGGGGGG-CTTTTTTTTT", revSeq.getSeqString());
    }
    
    /**
     * Test of getReversedWord method, of class edu.msu.cme.rdp.classifier.readseqwrapper.ParsedSequence.
     */
    public void testGetReversedWord() {
        System.out.println("testGetReversedWord");
        int [] word = new int [8];
        word[0] = 2;
        word[1] = 1;
        word[2] = 1;
        word[3] = 3;
        word[4] = 0;
        word[5] = 3;
        word[6] = 3;
        word[7] = 1;
        
        int[] revWord = ClassifierSequence.getReversedWord(word);
        assertEquals(revWord[0], 0);
        assertEquals(revWord[3], 1);
        assertEquals(revWord[7], 3);
    }
    
    /**
     * Test of getWordIndex method, of class edu.msu.cme.rdp.classifier.readseqwrapper.ParsedSequence.
     */
    public void testGetWordIndex() {
        System.out.println("testGetWordIndex");
        int [] word = new int [8];
        word[0] = 1;
        word[1] = 1;
        word[2] = 3;
        word[3] = 3;
        word[4] = 3;
        word[5] = 3;
        word[6] = 3;
        word[7] = 1;
        int wordIndex = ClassifierSequence.getWordIndex(word);       
        assertEquals(wordIndex, (24573));
               
        int[] revWord = ClassifierSequence.getReversedWord(word);
        wordIndex = ClassifierSequence.getWordIndex(revWord);       
        assertEquals(wordIndex, (10912));
       
    }
    
    /**
     * Test of createWordIndexArr method, of class edu.msu.cme.rdp.classifier.readseqwrapper.ParsedSequence.
     */
    public void testCreateWordIndexArr() {
        System.out.println("testCreateWordIndexArr");
      
        String seqString = "AAAAAAAAAG-CCCCCCCCUGAGGGUUACnAA";                
        ClassifierSequence aSeq = new ClassifierSequence("test", "", seqString);
        
        int[] wordIndexArr = aSeq.createWordIndexArr();        
        
        assertEquals( wordIndexArr[0], 0);   //AAAAAAAA
        
        assertEquals( wordIndexArr[2],2);    //AAAAAAAG
        
        assertEquals( wordIndexArr[3], ( 256*256 -1));   //CCCCCCCC
        
        assertEquals(wordIndexArr[4], (256*256 -3));   //CCCCCCCU
    }
    
        
}
