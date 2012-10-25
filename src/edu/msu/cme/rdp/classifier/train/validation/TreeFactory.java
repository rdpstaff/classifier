/*
 * TreeFactory.java
 *
 * Created on June 24, 2002, 5:01 PM
 */
/**
 *
 * @author  wangqion
 * @version
 */
package edu.msu.cme.rdp.classifier.train.validation;

import edu.msu.cme.rdp.classifier.train.GoodWordIterator;
import edu.msu.cme.rdp.classifier.train.LineageSequence;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class TreeFactory {

    private HierarchyTree root;
    private Map taxidMap = new HashMap();  // contains the taxname and the taxonomy
    private Set<String> rankSet = new HashSet<String>();  // contains the taxname and the taxonomy
    private float[] wordPriorArr = new float[(int) Math.pow(4, GoodWordIterator.WORDSIZE)];    // an array of prior for words
    // the index is the integer form of the word . for size 8, 65536 possible words
    private int totalSequences = 0;
    private final float WF1 = (float) 0.5;   // assume uniform prior for all the words
    private final float WF2 = (float) 1;
    private final int ROOT_DEPTH = 0;
    private String lowest_rank = null; // default

    /** Creates new TreeFactory */
    public TreeFactory(Reader taxReader) throws IOException {
        creatTaxidMap(taxReader);
    }

    /** It reads in a file containing the taxonomy information for all the nodes.
     * The taxonomy format is taxid(int), taxname(string), parentid(int),
     * depth(int) and hierarchy level(string) seperated by * in one line.
     * The information are kept in a hashMap, key: taxname, value: an array of
     * the Taxonomy(taxid, parentid, depth and hierarchy level).
     * example: 1*Bacteria*0*0*domain
     * Note: the depth for the root is 0.
     */
    private void creatTaxidMap(Reader taxReader) throws IOException {
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

                int taxid = Integer.parseInt(st.nextToken());
                String taxname = st.nextToken();
                int pid = Integer.parseInt(st.nextToken());
                int depth = Integer.parseInt(st.nextToken());
                List taxList = (ArrayList) taxidMap.get(taxname);
                if (taxList == null) {  // if this is an empty list, create a new one
                    taxList = new ArrayList();
                    taxidMap.put(taxname, taxList);
                }

                Taxonomy tax = new Taxonomy(taxid, taxname, pid, depth, st.nextToken().trim());
                taxList.add(tax);


                // check the root tree
                if (tax.depth == this.ROOT_DEPTH) {
                    if (root == null) {
                        root = new HierarchyTree(taxname, null, tax);
                    } else {
                        throw new IllegalArgumentException("Error: taxon " + tax.getTaxID() + " has the depth set to '0'. Only the root taxon can have the depth set to '0'");
                    }
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("\nError: "
                        + "The value for taxid, parentid and the depth should be integer in : " + line);
            }
        }
        if (root == null) {
            throw new IllegalArgumentException("Error: no root taxon with depth '0' defined in the taxonomy file.");
        }
    }

    /** For the given sequence name, its ancestors, and the sequence, creates a
     * HierarchyTree for each ancestor,
     * If the root does not exist, creates the root with a null parent.
     * If the root is already exist, checks the ParsedSequence to see if its
     * oldest ancestor is the same as the previous root.
     */
    public HierarchyTree addSequence(LineageSequence pSeq) throws IOException {

        int size = pSeq.getAncestors().size();
        if (size == 0) {
            throw new IllegalStateException("Sequence " + pSeq.getSeqName() + " does not have ancestors\n");
        } else if (!((String) pSeq.getAncestors().get(0)).equals(root.getName())) {
            throw new IllegalArgumentException("Sequence " + pSeq.getSeqName()
                    + " has conflicting root name." + " root is " + root.getName());
        }
        HierarchyTree curTree = root;
        rankSet.add(curTree.getTaxonomy().hierLevel);
        // add each ancestor
        for (int i = 1; i < size; i++) {
            HierarchyTree tmp = curTree.getSubclassbyName((String) pSeq.getAncestors().get(i));
            if (tmp == null) {
                Taxonomy tax = getTaxonomy(pSeq.getSeqName(), (String) pSeq.getAncestors().get(i), ((Taxonomy) curTree.getTaxonomy()).taxID, i);
                curTree = new HierarchyTree((String) pSeq.getAncestors().get(i), curTree, tax);
                rankSet.add(tax.hierLevel);
            } else {
                curTree = tmp;
            }
            // for the lowest level, count the word occurrence.
            if (i == size - 1) {
                curTree.initWordOccurrence(pSeq, wordPriorArr);
                curTree.increTotalSeqs();
                totalSequences++;
                if ( this.lowest_rank == null){
                    this.lowest_rank = curTree.getTaxonomy().hierLevel.toUpperCase();
                }else if (!this.lowest_rank.equalsIgnoreCase(curTree.getTaxonomy().getHierLevel())) {
                    throw new IllegalArgumentException("Sequence " + pSeq.getSeqName()
                            + " has different lowest rank: " + curTree.getTaxonomy().getHierLevel() + " from the previous lowest rank: " + this.lowest_rank);
                }
            }
        }

        return curTree;
    }

    /*
     *Gets the Taxonomy for the tree node in the ancestor list
     */
    public Taxonomy getTaxonomy(String seqName, String ancestorName, int pid, int index) {
        if (ancestorName == null) {
            throw new IllegalArgumentException("Error: No ancestors found for sequence: " + seqName
                    + "! Please check the source file.");
        }
        List taxList = (ArrayList) taxidMap.get(ancestorName);
        if (taxList == null) {
            throw new IllegalArgumentException("\nThe taxID for ancestor '" + ancestorName + "' of sequence '" + seqName
                    + "' at depth '" + index + "' with parent id '" + pid + "' is not found!");
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
            throw new IllegalArgumentException("\nThe taxID for ancestor '" + ancestorName + "' of sequence '" + seqName
                    + "' at depth '" + index + "' with parent id '" + pid + "' is not found!");
        }
        return result;
    }

    /** Gets the root of the tree */
    public HierarchyTree getRoot() {
        return root;
    }

    public Set<String> getRankSet() {
        return rankSet;
    }
    
    public String getLowestRank(){
        return this.lowest_rank;
    }
    

    /** display the phylogenetic tree
     * for each node, display the index and the name
     * for each sequence, diaplay the name and the description
     */
    public void displayTreePhylo(HierarchyTree root, String index, int indent) {
        //System.out.println(  root.getTaxonomy().getHierLevel());
        if (root.isSingleton()) {
            System.out.println(root.getTaxonomy().getHierLevel() + " " + root.getName());
        }

        Iterator i = root.getSubclasses().iterator();

        int curNum = 1;
        while (i.hasNext()) {
            displayTreePhylo((HierarchyTree) i.next(), index + "." + curNum, indent + 1);
            curNum++;
        }
    }

    // calculates the log prior for each word from the training set
    public void calculateWordPrior() {
        for (int i = 0; i < wordPriorArr.length; i++) {
            wordPriorArr[i] = (wordPriorArr[i] + WF1) / (totalSequences + WF2);
        }
    }

    public void printWordPriors() throws IOException {
        BufferedWriter outfile = new BufferedWriter(new FileWriter("wordPrior.txt"));
        outfile.write("word \t Prior\n");
        for (int i = 0; i < wordPriorArr.length; i++) {
            outfile.write(i + "\t" + wordPriorArr[i] + "\n");
        }

        outfile.close();
    }

    public float getWordPrior(int wordIndex) {
        return wordPriorArr[wordIndex];
    }
}
