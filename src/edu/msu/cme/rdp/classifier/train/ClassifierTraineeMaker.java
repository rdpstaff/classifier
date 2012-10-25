/*
 * Created on June 24, 2002, 2:11 PM
 * 
 * Copyright 2006 Michigan State University Board of Trustees
 * 
 * ClassifierTraineeMaker is used to create training files to be used by the classifier
 * 
 */
package edu.msu.cme.rdp.classifier.train;

import java.io.*;

/**
 * A command line class to create training information from the raw data.
 * @author  wangqion
 * @version 
 */
public class ClassifierTraineeMaker {

    /** Creates a new ClassifierTraineeMaker 
     * @param taxFile contains the hierarchical taxonomy information in the following format:
     * taxid*taxon name*parent taxid*depth*rank".
     * taxid, the parent taxid and depth should be in integer format.
     * depth indicates the depth from the root taxon.
     * @param seqFile contains the raw training sequences in fasta format.
     * The header of this fasta file starts with ">", followed by the sequence name, white space(s)
     * and a list taxon names seperated by ';' with highest rank taxon first. 
     * For example: >seq1     ROOT;Ph1;Fam1;G1;
     * <br>Note: a sequence can only be assigned to the lowest rank taxon.
     * @param trainset_no is used to mark the training files generated.
     * @param version indicates the version of the hierarchical taxonomy.
     * @param modification holds the modification information of the taxonomy if any.
     * @param outdir specifies the output directory.
     * The parsed training information will be saved into four files in the given output directory.
     */
    public ClassifierTraineeMaker(String taxFile, String seqFile, int trainset_no, String version, String modification, String outdir) throws FileNotFoundException, IOException {
        Reader tax = new FileReader(taxFile);

        try {
            TreeFactory factory = new TreeFactory(tax, trainset_no, version, modification);
            long startTime = System.currentTimeMillis();
            LineageSequenceParser parser = new LineageSequenceParser(new File(seqFile));

            while (parser.hasNext()) {
                factory.addSequence(parser.next());
            }
            //after parsing all the sequences in training set, calculates the prior probability for each word
            factory.createGenusWordConditionalProb();
            outdir = outdir + File.separator;
            factory.printTrainingFiles(outdir);
            factory.printWordPriors(outdir);
            factory.printWordConditionalProbIndexArr(outdir);
            factory.printGenusIndex_WordProbArr(outdir);
        } catch (NameRankDupException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Prints the license information to std err.
     */
    public static void printLicense() {
        String license = "Copyright 2006 Michigan State University Board of Trustees.\n\n"
                + "This program is free software; you can redistribute it and/or modify it under the "
                + "terms of the GNU General Public License as published by the Free Software Foundation; "
                + "either version 2 of the License, or (at your option) any later version.\n\n"
                + "This program is distributed in the hope that it will be useful, "
                + "but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY "
                + "or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.\n\n"
                + "You should have received a copy of the GNU General Public License along with this program; "
                + "if not, write to the Free Software Foundation, Inc., 59 Temple Place, "
                + "Suite 330, Boston, MA 02111-1307 USA\n\n"
                + "Authors's mailng address:\n"
                + "Center for Microbial Ecology\n"
                + "2225A Biomedical Physical Science\n"
                + "Michigan State University\n"
                + "East Lansing, Michigan USA 48824-4320\n"
                + "E-mail: James R. Cole at colej@msu.edu\n"
                + "\tQiong Wang at wangqion@msu.edu\n"
                + "\tJames M. Tiedje at tiedjej@msu.edu\n\n";

        System.err.println(license);
    }

    /** This is the main method to create training files from raw taxonomic information.
     * <p>
     * Usage: java ClassifierTraineeMaker tax_file rawseq.fa trainsetNo version version_modification output_directory.
     * See the ClassifierTraineeMaker constructor for more detail.
     * @param args
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void main(String[] args) throws FileNotFoundException,
            IOException {
        if (args.length != 6) {
            System.err.println("Usage: java ClassifierTraineeMaker <tax_file> <rawseq.fa> <trainsetNo> <version> <version_modification> <output_directory>");
            System.err.println("This program will create 4 output training files to be used by the classifier: ");
            System.err.println("\tbergeyTrainingTree.xml, genus_wordConditionalProbList.txt, logWordPrior.txt and wordConditionalProbIndexArr.txt");
            System.err.println("Command line arguments:");
            System.err.println("--tax_file contains the hierarchical taxonomy information in the following format:");
            System.err.println("\ttaxid*taxon name*parent taxid*depth*rank");
            System.err.println("\tFields taxid, the parent taxid and depth should be in integer format");
            System.err.println("\tdepth indicates the depth from the root taxon.");
            System.err.println("\tNote: the depth for the root is 0");
            System.err.println("\tEX: 44*ROOT*1*0*domain");
            System.err.println("--rawseq.fa contains the raw training sequences in fasta format");
            System.err.println("\tThe header of this fasta file starts with \">\", "
                    + "\n\tfollowed by the sequence name, white space(s) "
                    + "\n\tand a list taxon names seperated by ';' with highest rank taxon first.");
            System.err.println("\tEx: >seq1     ROOT;Ph1;Fam1;G1");
            System.err.println("\tNote: a sequence can only be assigned to the lowest rank taxon.");
            System.err.println("--trainsetNo is a integer. It's used to marked the training information.");
            System.err.println("--version indicates the version of the hierarchical taxonomy");
            System.err.println("\tEx: Bacteria Nomenclature");
            System.err.println("--version_modification holds the modifcation information of the taxonomy if any");
            System.err.println("\tEx: Acidobacterium Added");
            System.err.println("--output_directory specifies the output directory.");

            System.exit(-1);
        }

        int trainset_no = 1;
        try {
            trainset_no = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            System.err.println("Error: trainset_no needs to be an integer.");
            System.exit(-1);
        }

        printLicense();
        ClassifierTraineeMaker maker = new ClassifierTraineeMaker(args[0], args[1], trainset_no, args[3], args[4], args[5]);
    }
}
