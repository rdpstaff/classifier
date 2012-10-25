
/*
 * DecisionMakerTest.java
 * NetBeans JUnit based test
 *
 * Created on June 25, 2002, 3:59 PM
 */                

package edu.msu.cme.rdp.classifier.train.validation;

import edu.msu.cme.rdp.classifier.train.LineageSequence;
import edu.msu.cme.rdp.classifier.train.LineageSequenceParser;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

         
/**
 *
 * @author wangqion
 */
public class DecisionMakerTest extends TestCase {

    public DecisionMakerTest(java.lang.String testName) {
        super(testName);
    }        
        
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    /** Test of getBestClasspath method, of class Classification.DecisionMaker. */
    public void testGetBestClasspath() throws IOException{
      System.out.println("testGetBestClasspath");
      Reader taxReader = new FileReader(System.class.getResource("/test/classifier/testDMtaxon.txt").getFile()); 
      TreeFactory factory = new TreeFactory(taxReader);
      
      File infileReader = new File(System.class.getResource("/test/classifier/testDMtrain.fasta").getFile());  
    
      LineageSequenceParser parser = new LineageSequenceParser(infileReader);     
       
      while ( parser.hasNext() ){
        factory.addSequence( (LineageSequence)parser.next());        
      }   
      //after all the training set is being parsed, calculate the prior probability for all the words.
      
      factory.calculateWordPrior();            
      
      List nodeList = new ArrayList();
      HierarchyTree root = factory.getRoot();
      //root.getNodeList("GENUS", nodeList);
            
      DecisionMaker dm = new DecisionMaker(factory);
      //test addResult()
        
        HashMap determinedMap = new HashMap ();
		
		HierarchyTree Gammaproteobacteria = root.getSubclassbyName("Proteobacteria").getSubclassbyName("Gammaproteobacteria");
		
		HierarchyTree Enterobacteriales = Gammaproteobacteria.getSubclassbyName("Enterobacteriales");
        HierarchyTree determinedGenusNode = Enterobacteriales.getSubclassbyName("Enterobacteriaceae").getSubclassbyName("Enterobacter");   //Enterobacter
        
		HierarchyTree aNode = determinedGenusNode;
		while (aNode != null){			
			determinedMap.put(aNode, new ValidationClassificationResult(aNode, 0, 0));
			aNode = aNode.getParent();
		}
		
		HierarchyTree bestNode = Gammaproteobacteria.getSubclassbyName("Vibrionales").getSubclassbyName("Vibrionaceae").getSubclassbyName("Vibrio");  // Vibrio
		dm.addResult(new ValidationClassificationResult(bestNode, 1), determinedMap);
		assertEquals( ((ValidationClassificationResult)determinedMap.get(determinedGenusNode)).getNumOfVotes(), 0.0f, 0.0001);	//Enterobacter
		assertEquals( ((ValidationClassificationResult)determinedMap.get(Enterobacteriales)).getNumOfVotes(), 0.0f, 0.0001);  //Enterobacteriales
		assertEquals( ((ValidationClassificationResult)determinedMap.get(Gammaproteobacteria)).getNumOfVotes(), 1.0f, 0.0001);  //Gammaproteobacteria
        
        //Enterobacter
		dm.addResult(new ValidationClassificationResult(determinedGenusNode, 1), determinedMap);
		assertEquals( ((ValidationClassificationResult)determinedMap.get(determinedGenusNode)).getNumOfVotes(), 1.0f, 0.0001);	//Enterobacter
		assertEquals( ((ValidationClassificationResult)determinedMap.get(Enterobacteriales)).getNumOfVotes(), 1.0f, 0.0001);  //Enterobacteriales
		assertEquals( ((ValidationClassificationResult)determinedMap.get(Gammaproteobacteria)).getNumOfVotes(), 2.0f, 0.0001);  //Gammaproteobacteria
		
		dm.addResult(new ValidationClassificationResult(determinedGenusNode, 1), determinedMap);
               
        bestNode = root.getSubclassbyName("Firmicutes").getSubclassbyName("Clostridia").getSubclassbyName("Clostridiales").getSubclassbyName("Clostridiaceae").getSubclassbyName("Clostridium");  // Clostridium
        dm.addResult(new ValidationClassificationResult(bestNode, 1), determinedMap);
		assertEquals( ((ValidationClassificationResult)determinedMap.get(determinedGenusNode)).getNumOfVotes(), 2.0f, 0.0001);	//Enterobacter
		assertEquals( ((ValidationClassificationResult)determinedMap.get(Enterobacteriales)).getNumOfVotes(), 2.0f, 0.0001);  //Enterobacteriales
		assertEquals( ((ValidationClassificationResult)determinedMap.get(Gammaproteobacteria)).getNumOfVotes(), 3.0f, 0.0001);  //Gammaproteobacteria
		assertEquals( ((ValidationClassificationResult)determinedMap.get(root)).getNumOfVotes(), 4.0f, 0.0001);  //Bacteria
        // end test addResult()
     
    }
  
    public static Test suite() {
      TestSuite suite = new TestSuite(DecisionMakerTest.class);
      
      return suite;
    }
      

}
