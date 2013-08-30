/*
 * Created on June 24, 2002, 2:11 PM
 * 
 * Copyright 2006 Michigan State University Board of Trustees
 * 
 * ClassifierTraineeMaker is used to create training files to be used by the classifier
 * 
 */
package edu.msu.cme.rdp.classifier.train;

import edu.msu.cme.rdp.classifier.cli.CmdOptions;
import static edu.msu.cme.rdp.classifier.comparison.ComparisonCmd.COMPARE_OUTFILE_SHORT_OPT;
import static edu.msu.cme.rdp.classifier.comparison.ComparisonCmd.QUERYFILE1_SHORT_OPT;
import static edu.msu.cme.rdp.classifier.comparison.ComparisonCmd.QUERYFILE2_SHORT_OPT;
import edu.msu.cme.rdp.classifier.io.ClassificationResultFormatter;
import edu.msu.cme.rdp.classifier.utils.ClassifierFactory;
import java.io.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

/**
 * A command line class to create training information from the raw data.
 * @author  wangqion
 * @version 
 */
public class ClassifierTraineeMaker {
    private static final Options options = new Options();

    static {
        options.addOption(new Option("t", "tax_file", true, "contains the hierarchical taxonomy information in the following format:\n" +
                "taxid*taxon name*parent taxid*depth*rank\nFields taxid, the parent taxid and depth should be in integer format\n" +
                "The taxid, or the combination of taxon name and rank is unique\n" +
                "depth indicates the depth from the root taxon.\n Note: the depth for the root is 0"));
        options.addOption(new Option("s", "seq", true, "training sequences in FASTA format with lineage in the header:\n" +
                "a list taxon names seperated by ';' with highest rank taxon first.\n" +
                "The lowest rank of the lineage have to be the same for all sequence.\n" +
                "The lowest rank is not limited to genus"));
        options.addOption(new Option("n", "version_no", true, "an integer used to refer to a training set"));
        options.addOption(new Option("v", "version", true, "the version of the hierarchical taxonomy"));
        options.addOption(new Option("m", "mod", true, "the modifcation information of the taxonomy"));
        options.addOption(new Option("o", "out_dir", true, "the output directory"));
    }

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
            if ( !(new File(outdir)).exists()){
                (new File(outdir)).mkdir();
            }
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
        String taxFile;
        String seqFile;
        int trainset_no = 1;
        String version = null;
        String modification = null;
        String outdir = null;
        
        try {
            CommandLine line = new PosixParser().parse(options, args);

            if (line.hasOption("t")) {
                taxFile = line.getOptionValue("t");
            } else {
                throw new Exception("taxon file must be specified");
            }
            if (line.hasOption("s")) {
                seqFile = line.getOptionValue("s");
            } else {
                throw new Exception("seq file must be specified");
            }

            if (line.hasOption("n")) {
                try {
                    trainset_no = Integer.parseInt(line.getOptionValue("n"));
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("trainset_no needs to be an integer.");
                }
            }
            if (line.hasOption("o")) {
                outdir = line.getOptionValue("o");
            } else {
                throw new Exception("output directory must be specified");
            }
            if (line.hasOption("v")) {
                version = line.getOptionValue("v");
            }
            if (line.hasOption("m")) {
                modification = line.getOptionValue("m");
            }
            
        } catch (Exception e) {
            System.out.println("Command Error: " + e.getMessage());
            new HelpFormatter().printHelp(120, "train", "", options, "", true);
            return;
        }
        
        ClassifierTraineeMaker maker = new ClassifierTraineeMaker(taxFile, seqFile, trainset_no, version, modification, outdir);
    }
}
