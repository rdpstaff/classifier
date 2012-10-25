/*
 * Created on June 24, 2002, 2:11 PM
 * LeaveOneOutTesterMain.java is the main program for leave-one-out testing.
 */
/**
 *
 * @author  wangqion
 * @version 
 */
package edu.msu.cme.rdp.classifier.train.validation.leaveoneout;

/**
 * This is the Main class to do leave-one-out testing
 */
import java.io.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import edu.msu.cme.rdp.classifier.train.GoodWordIterator;
import edu.msu.cme.rdp.classifier.train.LineageSequence;
import edu.msu.cme.rdp.classifier.train.LineageSequenceParser;
import edu.msu.cme.rdp.classifier.train.validation.NBClassifier;
import edu.msu.cme.rdp.classifier.train.validation.TreeFactory;

public class LeaveOneOutTesterMain {

    private static final Options options = new Options();
    // long options
    public static final String QUERYFILE_LONG_OPT = "queryFile";
    public static final String OUTFILE_LONG_OPT = "outputFile";
    public static final String TRAIN_SEQFILE_LONG_OPT = "trainSeqFile";
    public static final String TRAIN_TAXONFILE_LONG_OPT = "trainTaxonFile";
    public static final String LENGTH_LONG_OPT = "length";
    public static final String MIN_BOOTSTRAP_WORDS_LONG_OPT = "minWords";
    //short options
    public static final String QUERYFILE_SHORT_OPT = "q";
    public static final String OUTFILE_SHORT_OPT = "o";
    public static final String TRAIN_SEQFILE_SHORT_OPT = "s";
    public static final String TRAIN_TAXONFILE_SHORT_OPT = "t";
    public static final String LENGTH_SHORT_OPT = "l";
    public static final String MIN_BOOTSTRAP_WORDS_SHORT_OPT = "w";
    
    
    // description of the options
    public static final String TRAIN_SEQFILE_DESC = "training files in fasta format labelled with the lineage information. "
            + "\nThe header of this fasta file starts with '>', followed by the sequence name, white space(s) and a list taxon names seperated by ';' with highest rank taxon first"
            + "\nex: Root;Bacteria;Proteobacteria;Gammaproteobacteria;Enterobacteriales;Enterobacteriaceae;Enterobacter";
    public static final String TRAIN_TAXONFILE_DESC = "contains the hierarchical taxonomy information, taxon name and rank together is unique."
            + " \nThe format looks like the following: taxid*taxon name*parent taxid*depth*rank"
            + " Note taxid, the parent taxid and depth should be in integer format. depth indicates the depth from the root taxon.";
    public static final String LENGTH_DESC = "the default is to test the entire query sequence. "
            + "if specifiy a length, a region of the query sequence with the specified length will be random choosen for testing";
    public static final String QUERYFILE_DESC = "query file contains sequences same format as the training sequence file";
    public static final String OUTFILE_DESC = "stat of leave-one-out testing including correctness rate at each rank, misclassified rate for each taxon ";
    public static final String MIN_WORDS_DESC = "minimum number of words for each run of bootstrap, minium is " + NBClassifier.MIN_BOOTSTRSP_WORDS ;
    
    static {
        options.addOption(new Option(TRAIN_SEQFILE_SHORT_OPT, TRAIN_SEQFILE_LONG_OPT, true, TRAIN_SEQFILE_DESC));
        options.addOption(new Option(TRAIN_TAXONFILE_SHORT_OPT, TRAIN_TAXONFILE_LONG_OPT, true, TRAIN_TAXONFILE_DESC));
        options.addOption(new Option(QUERYFILE_SHORT_OPT, QUERYFILE_LONG_OPT, true, QUERYFILE_DESC));
        options.addOption(new Option(OUTFILE_SHORT_OPT, OUTFILE_LONG_OPT, true, OUTFILE_DESC));
        options.addOption(new Option(LENGTH_SHORT_OPT, LENGTH_LONG_OPT, true, LENGTH_DESC));
        options.addOption(new Option(MIN_BOOTSTRAP_WORDS_SHORT_OPT, MIN_BOOTSTRAP_WORDS_LONG_OPT, true, MIN_WORDS_DESC));
    }
    TreeFactory factory = null;
    BufferedWriter outWriter = null;

    /** Creates a new Classification*/
    public LeaveOneOutTesterMain(String taxFile, String trainseqFile, String testFile, String outFile, int numGoodBases, int min_bootstrap_words)
            throws FileNotFoundException, IOException {
        boolean useSeed = true;  // use seed for random word selection
        factory = new TreeFactory(new FileReader(taxFile));
        // create a tree
        createTree(factory, trainseqFile);

        outWriter = new BufferedWriter(new FileWriter(outFile));
        LineageSequenceParser parser = new LineageSequenceParser(new File(testFile));
        LeaveOneOutTester tester = new LeaveOneOutTester(outWriter, numGoodBases);

        outWriter.write("taxon file: " + taxFile + "\n" + "train sequence file: " + trainseqFile + "\n");
        outWriter.write("word size: " + GoodWordIterator.WORDSIZE + "\n");
        outWriter.write("minimum number of words for bootstrap: " + min_bootstrap_words + "\n");

        if (numGoodBases > 0) {    // do partial   

            outWriter.write("query sequence file: " + testFile + "\n"
                    + "classify partial sequence, number of good bases=" + numGoodBases + "\n");
        } else {
            outWriter.write("query sequence file: " + testFile + "\n"
                    + "classify full-length sequence \n");

        }
        outWriter.write("test rank: " + factory.getLowestRank());

        tester.classify(factory, parser, useSeed, min_bootstrap_words);

    }

    /** reads from the stream, parses the sequences and creates the tree */
    private void createTree(TreeFactory factory, String input) throws IOException {
        LineageSequenceParser parser = new LineageSequenceParser(new File(input));

        while (parser.hasNext()) {
            factory.addSequence((LineageSequence) parser.next());
        }

        //after all the training set is being parsed, calculate the prior probability for all the words.
        factory.calculateWordPrior();
        // create the word occurrence for all the nodes, this is necessary if test on different level
        // besides genus but requires lot of memory for large taxonomy model
        // factory.getRoot().createWordOccurrenceFromSubclasses();
        //factory.displayTreePhylo(factory.getRoot(), "1", 0);
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {

        String queryFile = null;
        String outputFile = null;
        String trainSeqFile = null;
        String trainTaxonFile = null;
        int length = 0;
        int min_bootstrap_words = NBClassifier.MIN_BOOTSTRSP_WORDS;

        try {
            CommandLine line = new PosixParser().parse(options, args);

            if (line.hasOption(QUERYFILE_SHORT_OPT)) {
                queryFile = line.getOptionValue(QUERYFILE_SHORT_OPT);
            } else {
                throw new Exception("query file must be specified");
            }
            if (line.hasOption(OUTFILE_SHORT_OPT)) {
                outputFile = line.getOptionValue(OUTFILE_SHORT_OPT);
            } else {
                throw new Exception("output file must be specified");
            }

            if (line.hasOption(TRAIN_SEQFILE_SHORT_OPT)) {
                trainSeqFile = line.getOptionValue(TRAIN_SEQFILE_SHORT_OPT);
            } else {
                throw new Exception("training sequence file must be specified");
            }
            if (line.hasOption(TRAIN_TAXONFILE_SHORT_OPT)) {
                trainTaxonFile = line.getOptionValue(TRAIN_TAXONFILE_SHORT_OPT);
            } else {
                throw new Exception("training taxon file must be specified");
            }

            if (line.hasOption(LENGTH_SHORT_OPT)) {
                length = Integer.parseInt(line.getOptionValue(LENGTH_SHORT_OPT));
                if (length <= 0) {
                    throw new IllegalArgumentException(length + " must be a positive number ");
                }
            }
            if (line.hasOption(MIN_BOOTSTRAP_WORDS_SHORT_OPT)) {
                min_bootstrap_words = Integer.parseInt(line.getOptionValue(MIN_BOOTSTRAP_WORDS_SHORT_OPT));
                if (min_bootstrap_words < NBClassifier.MIN_BOOTSTRSP_WORDS) {
                    throw new IllegalArgumentException(MIN_BOOTSTRAP_WORDS_LONG_OPT + " must be at least " + NBClassifier.MIN_BOOTSTRSP_WORDS);
                }                
            }
        } catch (Exception e) {
            System.out.println("Command Error: " + e.getMessage());
            new HelpFormatter().printHelp(120, "LeaveOneOutTesterMain", "", options, "", true);
            return;
        }


        LeaveOneOutTesterMain aClassifier = new LeaveOneOutTesterMain(trainTaxonFile, trainSeqFile, queryFile, outputFile, length, min_bootstrap_words);

    }
}
