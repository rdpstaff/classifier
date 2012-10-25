/*
 * ClassifierTest.java
 * 
 * Copyright 2006 Michigan State University Board of Trustees
 *
 * Created on September 19, 2003, 10:23 AM
 */

package edu.msu.cme.rdp.classifier.rrnaclassifier;

import junit.framework.*;

/**
 * A test class for Classifier.
 * @author wangqion
 */
public class ClassifierTest extends TestCase {
    
    public ClassifierTest(java.lang.String testName) {
        super(testName);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(ClassifierTest.class);
        
        return suite;
    }
    
    
    public void testClassify(){
        System.out.println("testClassify");
       // the classify() is tested in class TrainingInfoTest.java
    }
       
}
