/*
 * Created on Feb 20, 2006
 *
 */
/**
 * This is a simple command line class to do classification.
 */
package edu.msu.cme.rdp.classifier;

import edu.msu.cme.rdp.classifier.io.ClassificationResultFormatter;
import edu.msu.cme.rdp.classifier.utils.ClassifierFactory;
import edu.msu.cme.rdp.classifier.utils.ClassifierSequence;
import edu.msu.cme.rdp.readseq.readers.SequenceReader;
import edu.msu.cme.rdp.readseq.readers.SeqReader;
import edu.msu.cme.rdp.readseq.readers.Sequence;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

/**
 * This is the command line class to do the classification.
 * @author wangqion
 */
public class ClassifierCmd {

    private static final Options options = new Options();
    // long options
    public static final String QUERYFILE_LONG_OPT = "queryFile";
    public static final String OUTFILE_LONG_OPT = "outputFile";
    public static final String TRAINPROPFILE_LONG_OPT = "train_propfile";
    public static final String FORMAT_LONG_OPT = "format";
    public static final String GENE_LONG_OPT = "gene";   // specify the gene if not provide train_propfile
    public static final String MIN_BOOTSTRAP_WORDS_LONG_OPT = "minWords";
    
    //short options
    public static final String QUERYFILE_SHORT_OPT = "q";
    public static final String OUTFILE_SHORT_OPT = "o";
    public static final String TRAINPROPFILE_SHORT_OPT = "t";
    public static final String FORMAT_SHORT_OPT = "f";
    public static final String GENE_SHORT_OPT = "g";
    public static final String MIN_BOOTSTRAP_WORDS_SHORT_OPT = "w";
    
    // description of the options
    public static final String QUERYFILE_DESC = "query file contains sequences in one of the following formats: Fasta, Genbank and EMBL";
    public static final String OUTFILE_DESC = "output file name for classification assignment";
    public static final String TRAINPROPFILE_DESC = "specify a property file contains the mapping of the training files"
            + " if it's located outside of data/classifier/."
            + "\nNote: the training files and the property file should be in the same directory."
            + "\nThe default property file is set to data/classifier/" + ClassifierFactory.RRNA_16S_GENE + "/rRNAClassifier.properties.";
    public static final String FORMAT_DESC = "all tab delimited output format: [allrank|fixrank|db]. Default is allrank. "
            + "\n allrank: outputs the results for all ranks applied for each sequence: seqname, orientation, taxon name, rank, conf, ..."
            + "\n fixrank: only outputs the results for fixed ranks in order: domain, phylum, class, order, family, genus"
            + "\n db: outputs the seqname, trainset_no, tax_id, conf. This is good for storing in a database";
    public static final String GENE_DESC = ClassifierFactory.RRNA_16S_GENE + "|" + ClassifierFactory.FUNGALLSU_GENE
            + ", the default training model for 16S rRNA or Fungal LSU genes. This option will be overwritten by --train_propfile option";
    public static final String MIN_WORDS_DESC = "minimum number of words for each bootstrap trial, Default is 1/8 of the words. Minimum is " + Classifier.MIN_BOOTSTRSP_WORDS ;

    private ClassificationResultFormatter.FORMAT defaultFormat = ClassificationResultFormatter.FORMAT.allRank;
     
    static {
        options.addOption(new Option(QUERYFILE_SHORT_OPT, QUERYFILE_LONG_OPT, true, QUERYFILE_DESC));
        options.addOption(new Option(OUTFILE_SHORT_OPT, OUTFILE_LONG_OPT, true, OUTFILE_DESC));
        options.addOption(new Option(TRAINPROPFILE_SHORT_OPT, TRAINPROPFILE_LONG_OPT, true, TRAINPROPFILE_DESC));
        options.addOption(new Option(FORMAT_SHORT_OPT, FORMAT_LONG_OPT, true, FORMAT_DESC));
        options.addOption(new Option(GENE_SHORT_OPT, GENE_LONG_OPT, true, GENE_DESC));
        options.addOption(new Option(MIN_BOOTSTRAP_WORDS_SHORT_OPT, MIN_BOOTSTRAP_WORDS_LONG_OPT, true, MIN_WORDS_DESC));
    }
   

    /** It classifies query sequences from the input file.
     * If the property file of the mapping of the training files is not null, the default property file will be override.
     * The classification results will be writen to the output file.
     */
    public void doClassify(String inputFile, String outFile, String propfile, ClassificationResultFormatter.FORMAT format, String gene, int min_bootstrap_words) throws IOException, TrainingDataException {
        if (propfile != null) {
            ClassifierFactory.setDataProp(propfile, false);
        }
        if (format == null) {
            format = defaultFormat;
        }
        ClassifierFactory factory = ClassifierFactory.getFactory(gene);
        Classifier aClassifier = factory.createClassifier();
        SeqReader parser = new SequenceReader(new File(inputFile));
        BufferedWriter wt = new BufferedWriter(new FileWriter(outFile));
        Sequence pSeq = null;

        try {
            while ((pSeq = parser.readNextSequence()) != null) {
                try {
                    ClassificationResult result = aClassifier.classify(new ClassifierSequence(pSeq), min_bootstrap_words);
                    wt.write(ClassificationResultFormatter.getOutput(result, format));

                } catch (ShortSequenceException e) {
                    System.out.println(e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            wt.close();
        }

    }

    /**
     * Prints the license information to std err.
     */
    public static void printLicense() {
        String license = "Copyright 2006-2011 Michigan State University Board of Trustees.\n\n"
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

    /**
     * This is the main method to do classification.
     * <p>Usage: java ClassifierCmd queryFile outputFile [property file].
     * <br>
     * queryFile can be one of the following formats: Fasta, Genbank and EMBL. 
     * <br>
     * outputFile will be used to save the classification output.
     * <br>
     * property file contains the mapping of the training files.
     * <br>
     * Note: the training files and the property file should be in the same directory.
     * The default property file is set to data/classifier/16srrna/rRNAClassifier.properties.
     */
    public static void main(String[] args) throws Exception {

        String queryFile = null;
        String outputFile = null;
        String propFile = null;
        String gene = null;
        ClassificationResultFormatter.FORMAT format = null;
        int min_bootstrap_words = Classifier.MIN_BOOTSTRSP_WORDS;

        try {
            CommandLine line = new PosixParser().parse(options, args);

            if (line.hasOption(QUERYFILE_SHORT_OPT)) {
                queryFile = line.getOptionValue(QUERYFILE_SHORT_OPT);
            } else {
                throw new Exception("queryFile must be specified");
            }
            if (line.hasOption(OUTFILE_SHORT_OPT)) {
                outputFile = line.getOptionValue(OUTFILE_SHORT_OPT);
            } else {
                throw new Exception("outputFile must be specified");
            }

            if (line.hasOption(TRAINPROPFILE_SHORT_OPT)) {
                if (gene != null) {
                    throw new IllegalArgumentException("Already specified the gene from the default location. Can not specify train_propfile");
                } else {
                    propFile = line.getOptionValue(TRAINPROPFILE_SHORT_OPT);
                }
            }
            if (line.hasOption(FORMAT_SHORT_OPT)) {
                String f = line.getOptionValue(FORMAT_SHORT_OPT);
                if (f.equalsIgnoreCase("allrank")) {
                    format = ClassificationResultFormatter.FORMAT.allRank;
                } else if (f.equalsIgnoreCase("fixrank")) {
                    format = ClassificationResultFormatter.FORMAT.fixRank;
                } else if (f.equalsIgnoreCase("db")) {
                    format = ClassificationResultFormatter.FORMAT.dbformat;
                } else {
                    throw new IllegalArgumentException("Not valid output format, only allrank, fixrank and db allowed");
                }
            }
            if (line.hasOption(GENE_SHORT_OPT)) {
                if (propFile != null) {
                    throw new IllegalArgumentException("Already specified train_propfile. Can not specify gene any more");
                }
                gene = line.getOptionValue(GENE_SHORT_OPT).toLowerCase();

                if (!gene.equals(ClassifierFactory.RRNA_16S_GENE) && !gene.equals(ClassifierFactory.FUNGALLSU_GENE)) {
                    throw new IllegalArgumentException(gene + " is NOT valid, only allows " + ClassifierFactory.RRNA_16S_GENE + " and " + ClassifierFactory.FUNGALLSU_GENE);
                }
            }
            if (line.hasOption(MIN_BOOTSTRAP_WORDS_SHORT_OPT)) {
                min_bootstrap_words = Integer.parseInt(line.getOptionValue(MIN_BOOTSTRAP_WORDS_SHORT_OPT));
                if (min_bootstrap_words < Classifier.MIN_BOOTSTRSP_WORDS) {
                    throw new IllegalArgumentException(MIN_BOOTSTRAP_WORDS_LONG_OPT + " must be at least " + Classifier.MIN_BOOTSTRSP_WORDS);
                }                
            }
        } catch (Exception e) {
            System.out.println("Command Error: " + e.getMessage());
            new HelpFormatter().printHelp(120, "ClassifierCmd", "", options, "", true);
            return;
        }

        if (propFile == null && gene == null) {
            gene = ClassifierFactory.RRNA_16S_GENE;
        }
        ClassifierCmd classifierCmd = new ClassifierCmd();

        printLicense();
        classifierCmd.doClassify(queryFile, outputFile, propFile, format, gene, min_bootstrap_words);

    }
}
