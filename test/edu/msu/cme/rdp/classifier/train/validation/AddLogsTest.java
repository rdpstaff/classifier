/*
 * AddLogsTest.java
 * JUnit based test
 *
 * Created on June 28, 2002, 12:19 PM
 */

package edu.msu.cme.rdp.classifier.train.validation;

import edu.msu.cme.rdp.classifier.train.validation.AddLogsTest;
import edu.msu.cme.rdp.classifier.train.validation.AddLogs;
import junit.framework.*;

/**
 *
 * @author wangqion
 */
public class AddLogsTest extends TestCase {
  
  public AddLogsTest(java.lang.String testName) {
    super(testName);
  }
  
  public static void main(java.lang.String[] args) {
    junit.textui.TestRunner.run(suite());
  }
  
  public static Test suite() {
    TestSuite suite = new TestSuite(AddLogsTest.class);
    return suite;
  }
  
  // Add test methods here, they have to start with 'test' name.
  // for example:
  
  public void testAdd(){
    double d1 = Math.log(0.2);
    double d2 = Math.log(0.8);
    
    double result = AddLogs.add(d1,d2);
    assertEquals(0.0 , result, 0.1);
    
    result = AddLogs.add(Math.log(2.0) , Math.log(0.7) );
    assertEquals(1, result, 0.01);
    
    result = AddLogs.add( -3684 , -1655 );
    assertEquals(-1655, result, 0.01);
  }
  
}
