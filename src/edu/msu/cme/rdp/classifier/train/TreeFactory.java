/*
 * TreeFactory.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 * Created on June 24, 2002, 5:01 PM
 */
package edu.msu.cme.rdp.classifier.train;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * A TreeFactory reads the raw taxonomic information and creates the RawHierarchyTree nodes.
 * @author  wangqion
 * @version
 */
public class TreeFactory {

    private static String dformat = "%1$.2f";
    private List genusNodeList = null;   // list of all the nodes at the trained rank, default is genus
    private List genus_wordConditionalProbList = new ArrayList(); // list of word
    //conditional probability and the corresponding genus node index,
    //starting from word 0 to word 65535.
    private int[] wordProbPointerArr = new int[65537];   // an array of index, each points to the start of
    // GenusIndexWordConditionalProb for each word in the ArrayList
    private float[] logArr;  // a list of log value of integers
    private RawHierarchyTree rootTree;
    private Map<String, List> taxnameMap = new HashMap<String, List>();  // contains the taxoname and the taxonomy
    private Map<Integer, Taxonomy> taxidMap = new HashMap<Integer, Taxonomy>();  // contains the taxon id and the taxonomy
    private Map<String, Integer> taxnameRankMap = new HashMap<String, Integer>();  //assume taxon name and rank together is unique,
    //contains the taxon name and rank and count, for sanity check purpose
    private float[] wordPriorArr = new float[65536];    // an array of prior for words
    // the index is the integer form of the word . for size 8, 65536 possible words
    /** The depth of root of the RawHierarchyTree is set to 0 */
    private int ROOT_DEPTH = 0;
    private int totalSequences = 0;
    /** A factor for probability correction*/
    private final float WF1 = (float) 0.5;
    /** A factor for probability correction*/
    private final float WF2 = (float) 1;
    private BufferedWriter treeFile;
    private String trainingVersion;
    private String trained_rank = null; // default

    /** Creates new TreeFactory. */
    public TreeFactory(Reader taxReader, int trainsetNo, String version, String modification) throws IOException, NameRankDupException {
        trainingVersion = "<trainsetNo>" + trainsetNo + "</trainsetNo>" + "<version>" + version + "</version><modversion>" + modification + "</modversion>";

        creatTaxidMap(taxReader);
    }

    /** It reads in a file containing the taxonomy information for all the nodes.
     * The taxonomy format is taxid(int), taxname(string), parentid(int),
     * depth(int) and hierarchy level(string) seperated by * in one line.
     * The information are kept in a hashMap, key: taxname, value: an array of
     * the Taxonomy(taxid, parentid, depth and hierarchy rank level).
     * Note: the depth for the root is 0.
     */
    private void creatTaxidMap(Reader taxReader) throws IOException, NameRankDupException {
        BufferedReader reader = new BufferedReader(taxReader);
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.length() == 0) { // skip the empty line
                continue;
            }

            StringTokenizer st = new StringTokenizer(line, "*");
            if (st.countTokens() < 5) {
                throw new IllegalArgumentException("\nIllegal taxonomy format at " + line);
            }
            try {
                int taxid = Integer.parseInt(st.nextToken().trim());
                String taxname = st.nextToken().trim();
                int pid = Integer.parseInt(st.nextToken().trim());
                int depth = Integer.parseInt(st.nextToken().trim());
                List taxList = (ArrayList) taxnameMap.get(taxname);
                if (taxList == null) {  // if this is an empty list, create a new one
                    taxList = new ArrayList();
                }

                Taxonomy tax = new Taxonomy(taxid, taxname, pid, depth, st.nextToken().trim());
                taxList.add(tax);
                taxnameMap.put(taxname, taxList);
                if ( taxidMap.containsKey(taxid)){
                    throw new NameRankDupException("Error: duplicate taxid found : " + taxid);
                }
                taxidMap.put(new Integer(taxid), tax);
                String name_rank = (taxname + "\t" + tax.hierLevel).toLowerCase();
                Integer existCount = taxnameRankMap.get(name_rank);
                if (existCount == null) {
                    existCount = new Integer(0);
                }
                taxnameRankMap.put(name_rank, existCount.intValue() + 1);
                // check the root tree
                if (tax.depth == this.ROOT_DEPTH) {
                    if (rootTree == null) {
                        rootTree = new RawHierarchyTree(taxname, null, tax);
                    } else {
                        throw new IllegalArgumentException("Error: taxon " + tax.getTaxID() + " has the depth set to '0'. Only the root taxon can have the depth set to '0'");
                    }
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("\nError: "
                        + "The value for taxid, parentid and the depth should be integer in : " + line);
            }
        }

        if (rootTree == null) {
            throw new IllegalArgumentException("Error: no root taxon with depth '0' defined in the taxonomy file.");
        }
        String errors = nameRankSanityCheck();
        if (errors != null) {
            throw new NameRankDupException("Error: duplicate taxon name and rank in the taxonomy file.\n" + errors);
        }

    }

    private String nameRankSanityCheck() {
        StringBuilder buf = new StringBuilder();
        for (String name_rank : taxnameRankMap.keySet()) {
            int count = taxnameRankMap.get(name_rank);
            if (count > 1) {
                buf.append(name_rank).append("\t").append(count).append("\n");
            }
        }
        if (buf.length() == 0) {
            return null;
        }
        return buf.toString();
    }

   
    public void parseSequenceFile(LineageSequenceParser parser) throws IOException {
        while (parser.hasNext()) {
            LineageSequence pSeq = parser.next();
            if (pSeq.getAncestors().size() == 1) {  // only contains the taxid of the immediate taxon
                addSequencewithTaxid(pSeq);
            } else {
                addSequencewithLineage(pSeq);
            }
        }
        parser.close();
    }
    
    /**
     * parse the copy number file
     * first step, take the copy number info from the lowest rank taxa, then calculate the copy number of the parent taxa based on the immediate children
     * second step, for any taxon without copy number info, uses the 
     * @param cnFile
     * @throws IOException 
     */
    public void parseCopyNumberFile(String cnFile) throws IOException {
         HashMap<String, CopyNumberParser.TaxonCopyNumber> cnMap = new CopyNumberParser().parse(cnFile);
         this.getGenusNodeList();
         for (int index = 0; index < genusNodeList.size(); index++) {
                RawHierarchyTree aTree = ((RawHierarchyTree) genusNodeList.get(index));
                String key = (aTree.getName() + aTree.getTaxonomy().getHierLevel()).toLowerCase();
                CopyNumberParser.TaxonCopyNumber cn = cnMap.get(key);
                if ( cn != null){
                    aTree.setCopyNumber(cn.getCopyNumber());
                    cnMap.remove(key);
                }
         }
         // need to throw errors for lowest level taxon not found in the tree, might be spelling error or something
         StringBuilder msg = new StringBuilder();
         for ( CopyNumberParser.TaxonCopyNumber cn: cnMap.values()){
             if ( !cn.rank.equalsIgnoreCase(this.trained_rank)) continue;
             msg.append(cn.name).append("\n");
         }
         if (msg.length() > 0){
             throw new IllegalArgumentException("Error: The following taxon names at rank " + this.trained_rank + " in the copynumber file are not found in the input taxonomy file\n" + msg);
         }         
         // fill the copy number for the ancestor taxa of the lowest level taxa
         bottomUpSetCopyNumber(this.rootTree);
         
         //fill the copy number for the taxa not in the copy number files
         if ( this.rootTree.hasCopyNumber()){
            topDownFillCopyNumber(this.rootTree);
         } else {
             throw new IllegalArgumentException("Error: Problem setting copy number, no taxon at rank " + this.trained_rank + " found in the copynumber file");
         }
    }

    /** For the given sequence name, its assigned taxid, and the sequence string, creates a
     * HierarchyTree for each ancestor,
     * If the root does not exist, creates the root with a null parent.
     * If the root already exists, checks the ParsedSequence to see if its
     * highest rank ancestor is the same as the previous root.
     */
    private void addSequencewithTaxid(LineageSequence pSeq) throws IOException {

        //start from bottom up
        ArrayList<Taxonomy> taxonList = new ArrayList<Taxonomy>();
        Taxonomy tax = this.taxidMap.get(new Integer((String) pSeq.getAncestors().get(0)));

        while (tax != null) {
            taxonList.add(tax);
            if (tax.getTaxID() == rootTree.getTaxonomy().getTaxID()) {
                break;
            }
            tax = this.taxidMap.get(new Integer(tax.getParentID()));
        }

        if (tax == null) {
            throw new IllegalArgumentException("Problem retrieving ancestor taxon for Sequence " + pSeq.getSeqName());
        }
        if (tax.getTaxID() != rootTree.getTaxonomy().getTaxID()) {
            throw new IllegalArgumentException("Sequence " + pSeq.getSeqName()
                    + " has conflicting ancestor root name: " + tax.getTaxName());
        }
        RawHierarchyTree curTree = rootTree;
        // add each ancestor, starting from the child under root
        for (int i = taxonList.size() - 2; i >= 0; i--) {

            RawHierarchyTree tmp = curTree.getSubclassbyName(taxonList.get(i).getTaxName());
            if (tmp == null) {
                curTree = new RawHierarchyTree(taxonList.get(i).getTaxName(), curTree, taxonList.get(i));
            } else {
                curTree = tmp;
            }
            // for the lowest level, count the word occurrence.
            if (i == 0) {
                curTree.initWordOccurrence(pSeq, wordPriorArr);
                if (this.trained_rank == null) {
                    this.trained_rank = curTree.getTaxonomy().getHierLevel();
                } else if (!this.trained_rank.equalsIgnoreCase(curTree.getTaxonomy().getHierLevel())) {
                    throw new IllegalArgumentException("Sequence " + pSeq.getSeqName()
                            + " has different lowest rank: " + curTree.getTaxonomy().getHierLevel() + " from the previous lowest rank: " + this.trained_rank);
                }
                totalSequences++;
            }
        }
    }

    /** For the given sequence name, its ancestors, and the sequence string, creates a
     * HierarchyTree for each ancestor,
     * If the root does not exist, creates the root with a null parent.
     * If the root already exists, checks the ParsedSequence to see if its
     * highest rank ancestor is the same as the previous root.
     */
    private void addSequencewithLineage(LineageSequence pSeq) throws IOException {

        int size = pSeq.getAncestors().size();
        if (!((String) pSeq.getAncestors().get(0)).equalsIgnoreCase(rootTree.getName())) {
            throw new IllegalArgumentException("Sequence " + pSeq.getSeqName()
                    + " has conflicting root name: " + pSeq.getAncestors().get(0));
        }
        RawHierarchyTree curTree = rootTree;
        // add each ancestor
        for (int i = 1; i < size; i++) {
            RawHierarchyTree tmp = curTree.getSubclassbyName(pSeq.getAncestors().get(i));
            if (tmp == null) {
                Taxonomy tax = getTaxonomy(pSeq, ((Taxonomy) curTree.getTaxonomy()).taxID, i);
                curTree = new RawHierarchyTree((String) pSeq.getAncestors().get(i), curTree, tax);
            } else {
                curTree = tmp;
            }
            // for the lowest level, count the word occurrence.
            if (i == size - 1) {
                curTree.initWordOccurrence(pSeq, wordPriorArr);
                totalSequences++;
                if (this.trained_rank == null) {
                    this.trained_rank = curTree.getTaxonomy().getHierLevel();
                } else if (!this.trained_rank.equalsIgnoreCase(curTree.getTaxonomy().getHierLevel())) {
                    throw new IllegalArgumentException("Sequence " + pSeq.getSeqName()
                            + " has different lowest rank: " + curTree.getTaxonomy().getHierLevel() + " from the previous lowest rank: " + this.trained_rank);
                }
            }
        }
    }

    /**
     * Gets the Taxonomy for the tree node in the ancestor list.
     */
    private Taxonomy getTaxonomy(LineageSequence pSeq, int pid, int index) {
        List ancestor = pSeq.getAncestors();
        if (ancestor.isEmpty()) {
            throw new IllegalArgumentException("Error: No ancestors found for sequence: " + pSeq.getSeqName()
                    + "! Please check the source file.");
        }
        String name = (String) ancestor.get(index);
        List taxList = (ArrayList) taxnameMap.get(name);
        if (taxList == null) {
            throw new IllegalArgumentException("\nThe taxID for ancestor: " + name + " of sequence: " + pSeq.getSeqName()
                    + " at depth: " + index + " with parent id: " + pid + " is not found!");
        }

        Taxonomy result = null;
        for (int i = 0; i < taxList.size(); i++) {
            Taxonomy tax = (Taxonomy) taxList.get(i);
            if (tax.parentID == pid && tax.depth == index) {
                result = tax;
                break;
            }
        }
        if (result == null) {
            throw new IllegalArgumentException("\nThe taxID for ancestor: " + name + " of sequence: " + pSeq.getSeqName()
                    + " at depth: " + index + " with parent id: " + pid + " is not found!");
        }
        return result;
    }

    /** Gets the root of the tree */
    public RawHierarchyTree getRoot() {
        return rootTree;
    }

    /** This method does all the setup work for wordPrior and word conditional probability.
     * 1. It calculates the prior for each word and keeps the value in an array
     * 2. for each word, it calculates the conditional probability for
     *    non-zero occurrence genus, and keeps the value in an array.
     */
    void createGenusWordConditionalProb() {
        getGenusNodeList();
        if (genusNodeList.isEmpty()) {
            throw new IllegalArgumentException("\nThere is no node at " + this.trained_rank);
        }
        int maxNumOfLeaves = 0;

        for (int i = 0; i < wordPriorArr.length; i++) {
            //calculate the prior probability for each word
            wordPriorArr[i] = (wordPriorArr[i] + WF1) / (totalSequences + WF2);

            //calculate the conditional probability for each word in each genus.
            wordProbPointerArr[i] = genus_wordConditionalProbList.size();
            for (int index = 0; index < genusNodeList.size(); index++) {
                RawHierarchyTree aTree = ((RawHierarchyTree) genusNodeList.get(index));
                int wordOccurrence = aTree.getWordOccurrence(i);
                int numOfLeaves = aTree.getLeaveCount();
                if (wordOccurrence > 0) {
                    float prob = (float) Math.log((wordOccurrence + wordPriorArr[i]) / (numOfLeaves + WF2));
                    genus_wordConditionalProbList.add(new RawGenusWordConditionalProb(index, prob));
                    //System.err.println("word=" + i + " genus=" + index + "  wo=" + wordOccurrence + " wp=" + wordPriorArr[i] + " leaves=" + aTree.getNumOfLeaves());
                }
                if (numOfLeaves > maxNumOfLeaves) {
                    maxNumOfLeaves = numOfLeaves;
                }
            }
            // change the wordPrior to log wordPrior for each calculate later
            wordPriorArr[i] = (float) Math.log(wordPriorArr[i]);
        }

        // the last pointer in wordProbPointerArr should have the value of the
        // size of the genus_wordConditionalProbList
        wordProbPointerArr[wordProbPointerArr.length - 1] = genus_wordConditionalProbList.size();

        // release the space occupied by wordOccurrence for the genus nodes.
        for (int index = 0; index < genusNodeList.size(); index++) {
            ((RawHierarchyTree) genusNodeList.get(index)).releaseWordOccurrence();
        }

        // make a list of log value of integers for convenience
        logArr = new float[++maxNumOfLeaves];
        for (int i = 0; i < maxNumOfLeaves; i++) {
            logArr[i] = (float) Math.log(i + WF2);
        }
    }

    /**
     * Returns the log value for word prior probability for the given word index.
     */
    float getLogWordPrior(int wordIndex) {
        return wordPriorArr[wordIndex];
    }

    /**
     * Return the list of geneus nodes.
     */
    List getGenusNodeList() {
        if (this.trained_rank == null) {
            throw new IllegalArgumentException("Need to read a sequence file to set the rank to train on");
        }
        if ( genusNodeList != null) return genusNodeList;      
        
        genusNodeList = new ArrayList();        
        createNodeList(this.getRoot(), this.trained_rank, genusNodeList);
        return genusNodeList;
    }

    /**
     * Returns the log value of ( number of leaves plus 1 ).
     */
    float getLogLeaveCount(int i) {
        return logArr[i];
    }

    /** Returns the start index of RawGenusWordConditionalProb in the array for the
     * given wordIndex.
     */
    int getStartIndex(int wordIndex) {
        return wordProbPointerArr[wordIndex];
    }

    /** Returns the stop index of RawGenusWordConditionalProb in the array for the
     * given wordIndex.
     */
    int getStopIndex(int wordIndex) {
        return wordProbPointerArr[wordIndex + 1];
    }

    /** Returns a GenusWordConditionalProb from the array given the position.
     */
    RawGenusWordConditionalProb getWordConditionalProb(int posIndex) {
        return (RawGenusWordConditionalProb) genus_wordConditionalProbList.get(posIndex);
    }

    /** Gets all the lowest level nodes in given hierarchy level starting from the given root.
     */
    void createNodeList(RawHierarchyTree node, String level, List nodeList) {
        if (node == null) {
            return;
        }

        if (((Taxonomy) node.getTaxonomy()).hierLevel.equalsIgnoreCase(level)) {
            nodeList.add(node);
            node.setGenusIndex(nodeList.size() - 1);
            return;
        }
        //start from the root of the tree, get the subclasses.
        Collection al = new ArrayList();

        if ((al = node.getSubclasses()).isEmpty()) {
            return;
        }
        Iterator i = al.iterator();
        while (i.hasNext()) {
            createNodeList((RawHierarchyTree) i.next(), level, nodeList);
        }
    }

   
    /** calculate the copy number from the children for the ancestor node
     */
    void bottomUpSetCopyNumber(RawHierarchyTree node) {
        if ( node.getTaxonomy().hierLevel.equals(this.trained_rank)) return;
        //start from the root of the tree, get the subclasses.
        Collection<RawHierarchyTree> al = node.getSubclasses();
        if (al.isEmpty())  return;
       
        Iterator i = al.iterator();
        while (i.hasNext()) {
            bottomUpSetCopyNumber((RawHierarchyTree) i.next());
        }
        float sum = 0f;
        int childwithcn = 0;
        for (RawHierarchyTree c: al){
            if ( c.hasCopyNumber()){
                sum += c.getCopyNumber();
                childwithcn ++;
            }
        }
        node.setCopyNumber(sum/childwithcn);
    }
    
     /** For any node that does not have children with copy number info, copy the one from its parent.
     */
    void topDownFillCopyNumber(RawHierarchyTree node) {
        if ( !node.hasCopyNumber()){
            node.setCopyNumber(node.getParent().getCopyNumber());
        }
        //start from the root of the tree, get the subclasses.
        Collection<RawHierarchyTree> al = node.getSubclasses();
        if (al.isEmpty())     return;
       
        Iterator i = al.iterator();
        while (i.hasNext()) {
            topDownFillCopyNumber((RawHierarchyTree) i.next());
        }
    }
    
    /**
     * Writes the entire phylogenetic taxonomic information to a file. 
     */
    void printTrainingFiles(String outdir) throws IOException {
        treeFile = new BufferedWriter(new FileWriter(outdir + "bergeyTrainingTree.xml"));
        treeFile.write(trainingVersion + "<file>bergeyTrainingTree</file>\n");
        displayTrainingTree(this.rootTree);
        treeFile.close();
    }

    /** Writes the phylogenetic taxonmic information of the given root and all the descendant nodes to a file.
     * For each node, display the index and the name.
     * For each sequence, display the name and the description.
     */
    private void displayTrainingTree(RawHierarchyTree root) throws IOException {
        Taxonomy taxon = ((Taxonomy) root.getTaxonomy());

        // need to remove the & sign in the taxon names
        treeFile.write("<TreeNode name=\"" + root.getName().replaceAll("&", "").replaceAll("\"", "&quot;") + "\" taxid=\""
                + taxon.taxID + "\" rank=\"" + taxon.hierLevel + "\" parentTaxid=\""
                + taxon.parentID + "\" leaveCount=\""
                + root.getLeaveCount() + "\" genusIndex=\"" + root.getGenusIndex() + "\"");
        if (rootTree.hasCopyNumber()){
            treeFile.write(" cpNumber=\"" + String.format(dformat, root.getCopyNumber()) + "\"");
        }        
        treeFile.write("></TreeNode>\n");

        Iterator i = root.getSubclasses().iterator();

        while (i.hasNext()) {
            displayTrainingTree((RawHierarchyTree) i.next());
        }
    }

    /**
     * Writes the log values of the word prior probabilities to a file.
     */
    void printWordPriors(String outdir) throws IOException {
        BufferedWriter outfile = new BufferedWriter(new FileWriter(outdir + "logWordPrior.txt"));
        outfile.write(trainingVersion + "<file>logWordPrior</file>\n");
        for (int i = 0; i < wordPriorArr.length; i++) {
            outfile.write(i + "\t" + wordPriorArr[i] + "\n");
        }

        outfile.close();
    }

    /**
     * Writes the indices of words and the start indices of conditional probability of 
     * the genera containing these words to a file.
     */
    void printWordConditionalProbIndexArr(String outdir) throws IOException {
        BufferedWriter outfile = new BufferedWriter(new FileWriter(outdir + "wordConditionalProbIndexArr.txt"));
        outfile.write(trainingVersion + "<file>wordConditionalProbIndexArr</file>\n");
        for (int i = 0; i < wordProbPointerArr.length; i++) {
            outfile.write(i + "\t" + wordProbPointerArr[i] + "\n");
        }

        outfile.close();
    }

    /**
     * Writes the indices of genus nodes and the conditional probabilities of words occurred in these
     *  genus nodes to a file.
     */
    void printGenusIndex_WordProbArr(String outdir) throws IOException {
        BufferedWriter outfile = new BufferedWriter(new FileWriter(outdir + "genus_wordConditionalProbList.txt"));
        outfile.write(trainingVersion + "<file>genus_wordConditionalProbList</file>\n");
        for (int i = 0; i < genus_wordConditionalProbList.size(); i++) {
            RawGenusWordConditionalProb prob = (RawGenusWordConditionalProb) genus_wordConditionalProbList.get(i);
            outfile.write(prob.getGenusIndex() + "\t" + prob.getProbability() + "\n");
        }

        outfile.close();
    }
}
