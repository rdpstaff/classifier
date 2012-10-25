/*
 * TreeFileParserTest.java
 * 
 * Copyright 2006 Michigan State University Board of Trustees
 *
 * Created on September 11, 2003, 12:07 PM
 */

package edu.msu.cme.rdp.classifier.rrnaclassifier;

import edu.msu.cme.rdp.classifier.HierarchyTree;
import edu.msu.cme.rdp.classifier.utils.HierarchyVersion;
import edu.msu.cme.rdp.classifier.io.TreeFileParser;
import java.io.*;
import junit.framework.*;
import java.util.Iterator;

/**
 * A test class for TreeFileParser.
 * @author wangqion
 */
public class TreeFileParserTest extends TestCase {
  
  public TreeFileParserTest(java.lang.String testName) {
    super(testName);
  }
  
  public static void main(java.lang.String[] args) {
    junit.textui.TestRunner.run(suite());
  }
  
  public static Test suite() {
    TestSuite suite = new TestSuite(TreeFileParserTest.class);
    
    return suite;
  }
  
  /** Test of parseTreeFile method, of class classification.TrainingFileParser. */
  public void testParseTreeFile() throws Exception{
    System.out.println("testParseTreeFile");
    TreeFileParser parser = new TreeFileParser();
    InputStream dstream = System.class.getResourceAsStream("/test/classifier/testTreeFile.xml");
    HierarchyVersion hVersion = null;
    
    Reader in =  new InputStreamReader( dstream );
    HierarchyTree root = parser.createTree(in, null);
    assertEquals(root.getName(), "BACTERIA");
    assertEquals(root.getLeaveCount(), 4123);
    assertEquals(root.getSizeofSubclasses(), 3);
    //displayTrainingTree(root);
    
    Iterator i = root.getSubclasses().iterator();
    
    while (i.hasNext()){
      HierarchyTree child = (HierarchyTree)i.next();
      if (child.getTaxid() == 1247){  //PLANCTOMYCETES
         assertEquals(child.getName(), "PLANCTOMYCETES");
         assertEquals(child.getRank(), "PHYLUM");
         assertEquals(child.getLeaveCount(), 11);
         assertEquals(child.getSizeofSubclasses(), 1);
      }
    }
  }
  
  private void displayTrainingTree(HierarchyTree root) throws IOException{
    
    System.err.print(" name=" + root.getName() + " leaveCount=" +
    root.getLeaveCount() + " genusIndex=" + root.getGenusIndex() + " taxid=" + root.getTaxid() );
    if (root.getParent() != null){
      System.err.print(" parentTaxid=" + root.getParent().getTaxid() + " rank="+ root.getRank() +"\n");
    }else {
      System.err.print( " rank="+ root.getRank() +"\n");
    }
    
    Iterator i = root.getSubclasses().iterator();
    
    while (i.hasNext()){
      displayTrainingTree( (HierarchyTree)i.next() );
    }
  }
  
  
}
