
/*
 * TreeFactoryTest.java
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
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author wangqion
 */
public class TreeFactoryTest extends TestCase {

    public TreeFactoryTest(java.lang.String testName) {
        super(testName);
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /** Test of addSequence method, of class Classification.TreeFactory. */
    public void testAddSequence() throws IOException {
        System.out.println("testAddSequence");
        Reader taxReader = new FileReader(System.class.getResource("/test/classifier/testTaxon.txt").getFile());

        TreeFactory factory = new TreeFactory(taxReader);

        File infileReader = new File(System.class.getResource("/test/classifier/testNBClassifierSet.fasta").getFile());

        LineageSequenceParser parser = new LineageSequenceParser(infileReader);

        while (parser.hasNext()) {
            factory.addSequence((LineageSequence) parser.next());
        }

        HierarchyTree root = factory.getRoot();
        assertEquals(root.getName(), "ROOT");
        assertEquals(root.getTotalSeqs(), 9);
        assertFalse(root.isSingleton());

        HierarchyTree ph1 = root.getSubclassbyName("Ph1");
        assertEquals(ph1.getName(), "Ph1");
        assertEquals(ph1.getTotalSeqs(), 4);
        assertFalse(ph1.isSingleton());

        HierarchyTree g1 = ph1.getSubclassbyName("Fam1").getSubclassbyName("G1");
        assertEquals(g1.getName(), "G1");
        assertEquals(g1.getTotalSeqs(), 2);
        assertFalse(g1.isSingleton());

        HierarchyTree fam2 = ph1.getSubclassbyName("Fam2");
        assertEquals(fam2.getName(), "Fam2");
        assertEquals(fam2.getTotalSeqs(), 1);
        assertTrue(fam2.isSingleton());

        HierarchyTree g3 = fam2.getSubclassbyName("G3");
        assertEquals(g3.getTotalSeqs(), 1);
        assertTrue(g3.isSingleton());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TreeFactoryTest.class);

        return suite;
    }
}
