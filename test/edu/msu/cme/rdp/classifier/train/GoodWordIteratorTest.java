/*
 * GoodWordIteratorTest.java
 * Copyright 2006 Michigan State University Board of Trustees
 *
 * Created on June 28, 2002, 12:10 PM
 */

package edu.msu.cme.rdp.classifier.train;

import edu.msu.cme.rdp.classifier.train.GoodWordIterator;
import junit.framework.*;
import java.io.*;


/**
 * A test class for GoodWordIterator.
 * @author wangqion
 */
public class GoodWordIteratorTest extends TestCase {
  
  private String seq = "AAAAAAAAAG-CCCCCCCCUGAGGGUUACnAA";
  private GoodWordIterator wordIt;
  
  public GoodWordIteratorTest(java.lang.String testName) throws IOException{
    super(testName);    
    wordIt = new GoodWordIterator(seq);
  }
  
  public static void main(java.lang.String[] args) {
    junit.textui.TestRunner.run(suite());
  }
  
  public static Test suite() {
    TestSuite suite = new TestSuite(GoodWordIteratorTest.class);
    
    return suite;
  }
  
    /** Test of next() method, of class GoodWordIterator. */
  public void testNext() {
    System.out.println("testNext");
    int seqLen = seq.length();
    int numOfWords = wordIt.getNumofWords();
    
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
    
   
  }  
    
}
