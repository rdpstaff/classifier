
/*
 * ClassifierTestSuite.java
 * NetBeans JUnit based test
 *
 * Created on June 25, 2002, 3:59 PM
 */                

package edu.msu.cme.rdp.classifier.train.validation;
 
import junit.framework.*;

         
/**
 *
 * @author wangqion
 */
public class ClassifierTestSuite extends TestCase {

    public ClassifierTestSuite(java.lang.String testName) {
        super(testName);
    }        
        
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
      //--JUNIT:
      //This block was automatically generated and can be regenerated again.
      //Do NOT change lines enclosed by the --JUNIT: and :JUNIT-- tags.
      
      TestSuite suite = new TestSuite("ClassifierTestSuite");
      suite.addTest(edu.msu.cme.rdp.classifier.train.validation.SequenceParserTest.suite());
      suite.addTest(edu.msu.cme.rdp.classifier.train.validation.HierarchyTreeTest.suite());
      suite.addTest(edu.msu.cme.rdp.classifier.train.validation.TreeFactoryTest.suite());
      suite.addTest(edu.msu.cme.rdp.classifier.train.validation.NBClassifierTest.suite());
      suite.addTest(edu.msu.cme.rdp.classifier.train.validation.DecisionMakerTest.suite());

      suite.addTest(edu.msu.cme.rdp.classifier.train.validation.GoodWordIteratorTest.suite());

      return suite;
    }
    
 
}
