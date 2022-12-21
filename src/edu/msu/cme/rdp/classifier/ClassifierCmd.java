/*
 * Created on Feb 20, 2006
 *
 */
/**
 * This is a simple command line class to do classification.
 */
package edu.msu.cme.rdp.classifier;

import edu.msu.cme.rdp.classifier.cli.CmdOptions;
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
 * This is the legacy command line class to do the classification. See edu.msu.cme.rdp.multicompare.main
 * @author wangqion
 */
public class ClassifierCmd {

    private static final Options options = new Options();
    
     
    static {
        options.addOption(new Option(CmdOptions.QUERYFILE_SHORT_OPT, CmdOptions.QUERYFILE_LONG_OPT, false, CmdOptions.QUERYFILE_DESC)); // keep this for compatibility with old interface
        options.addOption(new Option(CmdOptions.OUTFILE_SHORT_OPT, CmdOptions.OUTFILE_LONG_OPT, true, CmdOptions.OUTFILE_DESC));
        options.addOption(new Option(CmdOptions.TRAINPROPFILE_SHORT_OPT, CmdOptions.TRAINPROPFILE_LONG_OPT, true, CmdOptions.TRAINPROPFILE_DESC));
        options.addOption(new Option(CmdOptions.FORMAT_SHORT_OPT, CmdOptions.FORMAT_LONG_OPT, true, CmdOptions.FORMAT_DESC));
        options.addOption(new Option(CmdOptions.GENE_SHORT_OPT, CmdOptions.GENE_LONG_OPT, true, CmdOptions.GENE_DESC));
        options.addOption(new Option(CmdOptions.MIN_BOOTSTRAP_WORDS_SHORT_OPT, CmdOptions.MIN_BOOTSTRAP_WORDS_LONG_OPT, true, CmdOptions.MIN_WORDS_DESC));
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
            format = CmdOptions.DEFAULT_FORMAT;
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
                    System.err.println(e.getMessage());
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
        ClassificationResultFormatter.FORMAT format = CmdOptions.DEFAULT_FORMAT;
        int min_bootstrap_words = Classifier.MIN_BOOTSTRSP_WORDS;

        try {
            CommandLine line = new PosixParser().parse(options, args);
            
            if (line.hasOption(CmdOptions.OUTFILE_SHORT_OPT)) {
                outputFile = line.getOptionValue(CmdOptions.OUTFILE_SHORT_OPT);
            } else {
                throw new Exception("outputFile must be specified");
            }

            if (line.hasOption(CmdOptions.TRAINPROPFILE_SHORT_OPT)) {
                if (gene != null) {
                    throw new IllegalArgumentException("Already specified the gene from the default location. Can not specify train_propfile");
                } else {
                    propFile = line.getOptionValue(CmdOptions.TRAINPROPFILE_SHORT_OPT);
                }
            }
            if (line.hasOption(CmdOptions.FORMAT_SHORT_OPT)) {
                String f = line.getOptionValue(CmdOptions.FORMAT_SHORT_OPT);
                if (f.equalsIgnoreCase("allrank")) {
                    format = ClassificationResultFormatter.FORMAT.allRank;
                } else if (f.equalsIgnoreCase("fixrank")) {
                    format = ClassificationResultFormatter.FORMAT.fixRank;
                } else if (f.equalsIgnoreCase("filterbyconf")) {
                    format = ClassificationResultFormatter.FORMAT.filterbyconf;
                } else if (f.equalsIgnoreCase("db")) {
                    format = ClassificationResultFormatter.FORMAT.dbformat;
                }else {
                    throw new IllegalArgumentException("Not valid output format, only allrank, fixrank, filterbyconf and db allowed");
                }
            }
            if (line.hasOption(CmdOptions.GENE_SHORT_OPT)) {
                if (propFile != null) {
                    throw new IllegalArgumentException("Already specified train_propfile. Can not specify gene any more");
                }
                gene = line.getOptionValue(CmdOptions.GENE_SHORT_OPT).toLowerCase();

                if (!gene.equals(ClassifierFactory.RRNA_16S_GENE) && !gene.equals(ClassifierFactory.FUNGALLSU_GENE)) {
                    throw new IllegalArgumentException(gene + " is NOT valid, only allows " + ClassifierFactory.RRNA_16S_GENE + " and " + ClassifierFactory.FUNGALLSU_GENE);
                }
            }
            if (line.hasOption(CmdOptions.MIN_BOOTSTRAP_WORDS_SHORT_OPT)) {
                min_bootstrap_words = Integer.parseInt(line.getOptionValue(CmdOptions.MIN_BOOTSTRAP_WORDS_SHORT_OPT));
                if (min_bootstrap_words < Classifier.MIN_BOOTSTRSP_WORDS) {
                    throw new IllegalArgumentException(CmdOptions.MIN_BOOTSTRAP_WORDS_LONG_OPT + " must be at least " + Classifier.MIN_BOOTSTRSP_WORDS);
                }                
            }
            
            args = line.getArgs();
            if ( args.length != 1){
                throw new Exception("Expect one query file");
            }
            queryFile = args[0];
            
        } catch (Exception e) {
            System.err.println("Command Error: " + e.getMessage());
            new HelpFormatter().printHelp(120, "ClassifierCmd [options] <samplefile>\nNote this is the legacy command for one sample classification ", "", options, "");

            return;
        }

        if (propFile == null && gene == null) {
            gene = CmdOptions.DEFAULT_GENE;
        }
        ClassifierCmd classifierCmd = new ClassifierCmd();

        printLicense();
        classifierCmd.doClassify(queryFile, outputFile, propFile, format, gene, min_bootstrap_words);

    }
}
