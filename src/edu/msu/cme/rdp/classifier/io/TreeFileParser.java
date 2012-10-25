/*
 * TreeFileParser.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 *
 * Created on September 11, 2003, 11:12 AM
 */
package edu.msu.cme.rdp.classifier.io;

import edu.msu.cme.rdp.classifier.HierarchyTree;
import edu.msu.cme.rdp.classifier.TrainingDataException;
import edu.msu.cme.rdp.classifier.utils.HierarchyVersion;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Stack;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A parser to parse a reader containing taxonomic training information.
 * Note: The first TreeNode is the root TreeNode.
 * @author  wangqion
 */
public class TreeFileParser extends org.xml.sax.helpers.DefaultHandler {

    private Stack treeNodeStack = new Stack();
    private HierarchyTree root;

    /** Creates a new instance of TreeFileParser. */
    public TreeFileParser() {
    }

    /** Reads from a reader that contains the information for each treenode.
     * Creates all the HierarchyTrees and returns the root of the trees.
     * Note: The first TreeNode is the root TreeNode.
     * The version information should be obtained from the other files first.
     */
    public HierarchyTree createTree(Reader in, HierarchyVersion version) throws IOException, TrainingDataException {
        BufferedReader infile = new BufferedReader(in);
        // the first line contains the version information
        // check if it's the same as version from the other training files

        String line = infile.readLine();
        if (line != null) {
            HierarchyVersion thisVersion = new HierarchyVersion(line);
            int trainsetNo = thisVersion.getTrainsetNo();

            if (thisVersion.getVersion() == null) {
                throw new TrainingDataException("Error: There is no version information "
                        + "in the bergeyTree file");
            }
            if (version == null) {
                version = thisVersion;
            } else if (!version.getVersion().equals(thisVersion.getVersion()) || version.getTrainsetNo() != thisVersion.getTrainsetNo()) {
                throw new TrainingDataException("Error: The version information in the bergeyTree file is different from the version of the other training files.");
            }
        }

        while ((line = infile.readLine()) != null) {
            load(line);
        }
        infile.close();
        return root;
    }

    private void load(String document) throws TrainingDataException, IOException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(new InputSource(new StringReader(document)), this);
        } catch (ParserConfigurationException e) {
            throw new TrainingDataException(e);
        } catch (SAXException e) {
            throw new TrainingDataException(e);
        }
    }

    public void startElement(String namespaceURI,
            String lName, // local name
            String qName, // qualified name
            Attributes attrs) throws SAXException {
        try {
            if (attrs == null || attrs.getLength() != 6) {
                throw new TrainingDataException("Error: the attribute for element: "
                        + qName + " is missing or do not have exactly 6 attributes");
            }
            int taxid = Integer.parseInt(attrs.getValue(1));
            int parentTaxid = Integer.parseInt(attrs.getValue(3));
            int leaveCount = Integer.parseInt(attrs.getValue(4));
            int genusIndex = Integer.parseInt(attrs.getValue(5));

            HierarchyTree aTree = new HierarchyTree(attrs.getValue(0), taxid, attrs.getValue(2), leaveCount, genusIndex);
            // The first TreeNode is the root
            if (root == null) {
                aTree.addParent(null);
                root = aTree;
            } else {
                HierarchyTree parent = null;
                while (!treeNodeStack.empty()) {
                    HierarchyTree topNode = (HierarchyTree) treeNodeStack.peek();
                    if (topNode.getTaxid() == parentTaxid) {
                        parent = topNode;
                        break;
                    }
                    treeNodeStack.pop();
                }
                if (parent == null) {
                    throw new TrainingDataException("Error: The parent for treenode name=: "
                            + attrs.getValue(0) + " rank=" + attrs.getValue(2) + " parentTaxid=" + parentTaxid
                            + " can not be found in the input file");
                }
                //System.err.println("parent: " + parent.getName() + " root=" + root.getName());
                aTree.addParent(parent);
            }

            // if this node is not genus node, push it to the stack
            if (genusIndex == -1) {
                treeNodeStack.push(aTree);
            }
        } catch (TrainingDataException e) {
            throw new SAXException(e);
        }
    }

    public void endElement(String str, String str1, String str2) throws org.xml.sax.SAXException {
    }

    public void error(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {
        throw new SAXException(e);
    }
}
