/*
 * RawSequenceParserTest.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 *
 * Created on June 27, 2002, 6:48 PM
 */

package edu.msu.cme.rdp.classifier.train;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * A test class for RawSequenceParser.
 * @author wangqion
 */
public class RawSequenceParserTest extends TestCase {
  
  public RawSequenceParserTest(java.lang.String testName) {
    super(testName);
  }
  
  public static void main(java.lang.String[] args) {
    junit.textui.TestRunner.run(suite());
  }
  
  /**
   * Tests RawSequenceParser next() method.
   */
  public void testNext() throws FileNotFoundException, IOException{
    System.out.println("testNext()");  
    InputStream aStream = System.class.getResourceAsStream("/test/classifier/testSeqParser.fasta");
    
    LineageSequenceParser parser = new LineageSequenceParser(aStream);
    boolean next = parser.hasNext();
    assertTrue(next);
    // test the first sequence
    LineageSequence pSeq = parser.next();
    String name = "X53199";
    assertEquals(name, pSeq.getSeqName());
    assertEquals("AZOSPIRILLUM_GROUP", (String) pSeq.getAncestors().get(4));  
    assertEquals("PROTEOBACTERIA", (String) pSeq.getAncestors().get(2));   
    assertEquals("GAEA", (String) pSeq.getAncestors().get(0) );
    
    //test the last sequence
    parser.next();
    pSeq = parser.next();
    name = "AB002485";
    assertEquals(name, pSeq.getSeqName());
    assertEquals("M.RRR_SUBGROUP", (String) pSeq.getAncestors().get(5) );
    assertEquals("TTTT_GROUP", (String) pSeq.getAncestors().get(4) );
    assertEquals("BELTA_SUBDIVISION", (String) pSeq.getAncestors().get(3) );
    assertEquals("GAEA", (String) pSeq.getAncestors().get(0) );
    String sequence = "AAAAUAtttAGUCCCCCCCCUG";
    assertEquals(sequence, pSeq.getSeqString());
    assertTrue(!parser.hasNext());
  }
  
  

  public static Test suite() {
    TestSuite suite = new TestSuite(RawSequenceParserTest.class);
    
    return suite;
  }
  
 
}
