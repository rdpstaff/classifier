
/*
 * ClassificationSuite.java
 * 
 * Copyright 2006 Michigan State University Board of Trustees
 *
 * Created on June 25, 2002, 3:59 PM
 */                

package edu.msu.cme.rdp.classifier.train;
 
import junit.framework.*;
         
/**
 * A test suite for package edu.msu.cme.rdp.classifier.train.
 * @author wangqion
 */
public class TrainSuite extends TestCase {

    public TrainSuite(java.lang.String testName) {
        super(testName);
    }        
        
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    /**
     * Test suite of the package edu.msu.cme.rdp.classifier.train.
     */
    public static Test suite() {
      
      TestSuite suite = new TestSuite("TrainSuite");
      suite.addTest(RawSequenceParserTest.suite());
      suite.addTest(RawHierarchyTreeTest.suite());
      suite.addTest(TreeFactoryTest.suite());
      suite.addTest(GoodWordIteratorTest.suite());
     
      return suite;
    }
    
   
}
