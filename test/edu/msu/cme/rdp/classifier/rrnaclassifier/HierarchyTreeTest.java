
/*
 * HierarchyTreeTest.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 * 
 * Created on June 25, 2002, 3:59 PM
 */                

package edu.msu.cme.rdp.classifier.rrnaclassifier;
 
import junit.framework.*;
import java.io.*;
         
/**
 * A test class for HierarchyTree.
 * @author wangqion
 */
public class HierarchyTreeTest extends TestCase {

    public HierarchyTreeTest(java.lang.String testName) {
        super(testName);
    }        
        
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
       
    public static Test suite() {
      TestSuite suite = new TestSuite(HierarchyTreeTest.class);
      
      return suite;
    }
   
    public void testGetName() throws IOException{
      System.err.println("testGetName");
      // tested in class TrainingInfoTest.java
    }

}
