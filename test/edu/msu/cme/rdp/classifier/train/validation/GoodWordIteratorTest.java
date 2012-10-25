/*
 * GoodWordIteratorTest.java
 * NetBeans JUnit based test
 *
 * Created on June 28, 2002, 12:10 PM
 */

package edu.msu.cme.rdp.classifier.train.validation;

import edu.msu.cme.rdp.classifier.train.GoodWordIterator;
import edu.msu.cme.rdp.classifier.train.validation.GoodWordIteratorTest;
import junit.framework.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author wangqion
 */
public class GoodWordIteratorTest extends TestCase { 
  
  public GoodWordIteratorTest(java.lang.String testName) throws IOException{
    super(testName);        
  }
  
  public static void main(java.lang.String[] args) {
    junit.textui.TestRunner.run(suite());
  }
  
  public static Test suite() {
    TestSuite suite = new TestSuite(GoodWordIteratorTest.class);
    
    return suite;
  }
  
    /** Test of next method, of class classification.GoodWordIterator. */
  public void testNext() throws IOException {
    System.out.println("testNext");
   
    String seq = "AAAAAAAAAG-CCCCCCCCUGAGGGUUACnAA";
    String seq2 = "NNNNNNNNNNNNNNNNNNNAUGACGGUAGCGACAGAAGAAGNNNNNNNNN";
    GoodWordIterator wordIt;
    int seqLen = seq.length();
    wordIt = new GoodWordIterator(seq);
    int numOfWords = wordIt.getNumofWords();
    //System.err.println("seqlen: " +seqLen + "  numOfWords: " + numOfWords );
    assertEquals( numOfWords, 14);
    if( wordIt.hasNext() ){
      assertEquals( wordIt.next(), 0);   //AAAAAAAA
    }
    
    wordIt.next();
    
    if( wordIt.hasNext() ){
      assertEquals( wordIt.next(),2);    //AAAAAAAG
    }
    if( wordIt.hasNext() ){
      assertEquals( wordIt.next(), ( 256*256 -1));   //CCCCCCCC
    }
    if( wordIt.hasNext() ){
      assertEquals(wordIt.next(), (256*256 -3));   //CCCCCCCU
    }
    
   
    wordIt = new GoodWordIterator(seq, 5);
    numOfWords = wordIt.getNumofWords();
    
    assertEquals( numOfWords, 5);
    
    
  }
  
}
