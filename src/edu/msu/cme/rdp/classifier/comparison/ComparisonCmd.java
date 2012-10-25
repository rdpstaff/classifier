/*
 * ComparisonCmd.java
 *
 * This is a command line comparison class
 * Created on November 7, 2003, 5:14 PM
 */
package edu.msu.cme.rdp.classifier.comparison;

/**
 *
 * @author  wangqion
 */
import edu.msu.cme.rdp.classifier.ClassificationResult;
import edu.msu.cme.rdp.classifier.Classifier;
import edu.msu.cme.rdp.classifier.RankAssignment;
import edu.msu.cme.rdp.classifier.ShortSequenceException;
import edu.msu.cme.rdp.classifier.TrainingDataException;
import edu.msu.cme.rdp.classifier.io.ClassificationResultFormatter;
import edu.msu.cme.rdp.classifier.utils.ClassifierFactory;
import edu.msu.cme.rdp.readseq.readers.Sequence;
import edu.msu.cme.rdp.readseq.readers.SequenceReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

public class ComparisonCmd {

    // long options
    public static final String QUERYFILE1_LONG_OPT = "queryFile1";
    public static final String QUERYFILE2_LONG_OPT = "queryFile2";
    public static final String CLASS_OUTFILE_LONG_OPT = "class_outputFile";
    public static final String COMPARE_OUTFILE_LONG_OPT = "compare_outputFile";
    public static final String TRAINPROPFILE_LONG_OPT = "train_propfile";
    public static final String FORMAT_LONG_OPT = "format";
    public static final String CONF_LONG_OPT = "conf";
    public static final String GENE_LONG_OPT = "gene";   // specify the gene if not provide train_propfile
    //short options
    public static final String QUERYFILE1_SHORT_OPT = "q1";
    public static final String QUERYFILE2_SHORT_OPT = "q2";
    public static final String CLASS_OUTFILE_SHORT_OPT = "r";
    public static final String COMPARE_OUTFILE_SHORT_OPT = "o";
    public static final String TRAINPROPFILE_SHORT_OPT = "t";
    public static final String FORMAT_SHORT_OPT = "f";
    public static final String CONF_SHORT_OPT = "n";
    public static final String GENE_SHORT_OPT = "g";
    // description of the options
    public static final String QUERYFILE_DESC = "query file contains sequences in one of the following formats: Fasta, Genbank and EMBL.";
    public static final String CLASS_OUTFILE_DESC = "output file name for classification assignments.";
    public static final String COMPARE_OUTFILE_DESC = "output file name for the comparsion results.";
    public static final String TRAINPROPFILE_DESC = "specify a property file contains the mapping of the training files"
            + " if it's located outside of data/classifier/."
            + "\nNote: the training files and the property file should be in the same directory."
            + "\nThe default property file is set to data/classifier/" + ClassifierFactory.RRNA_16S_GENE + "/rRNAClassifier.properties.";
    public static final String FORMAT_DESC = "tab delimited output format: [ allrank | fixrank | db ]. Default is allrank. "
            + "\n allrank: outputs the results for all ranks applied for each sequence: seqname, orientation, taxon name, rank, conf, ..."
            + "\n fixrank: only outputs the results for fixed ranks in order: no rank, domain, phylum, class, order, family, genus."
            + "\n db: outputs the seqname, trainset_no, tax_id, conf. Good for storing in a database.";
    public static final String CONF_DESC = "specifies the assignment confidence cutoff used to determine the assignment count in the hierarchical format. Range [0-1], Default is 0.8.";
    public static final String GENE_DESC = ClassifierFactory.RRNA_16S_GENE + "|" + ClassifierFactory.FUNGALLSU_GENE
            + ", the default training model for 16S rRNA or Fungal LSU genes. This option will be overwritten by --train_propfile option";
    public static final float DEFAULT_CON_CUTOFF = 0.8f;  // default
    public static final ClassificationResultFormatter.FORMAT DEFAULT_FORMAT = ClassificationResultFormatter.FORMAT.allRank;
    private static Options options = new Options();
    private ClassifierFactory factory;
    private float confidenceCutoff = 0.8f;  // default
    private static final String SAMPLE1 = "sample1";
    private static final String SAMPLE2 = "sample2";

    static {
        options.addOption(new Option(QUERYFILE1_SHORT_OPT, QUERYFILE1_LONG_OPT, true, QUERYFILE_DESC));
        options.addOption(new Option(QUERYFILE2_SHORT_OPT, QUERYFILE2_LONG_OPT, true, QUERYFILE_DESC));
        options.addOption(new Option(CLASS_OUTFILE_SHORT_OPT, CLASS_OUTFILE_LONG_OPT, true, CLASS_OUTFILE_DESC));
        options.addOption(new Option(COMPARE_OUTFILE_SHORT_OPT, COMPARE_OUTFILE_LONG_OPT, true, COMPARE_OUTFILE_DESC));
        options.addOption(new Option(TRAINPROPFILE_SHORT_OPT, TRAINPROPFILE_LONG_OPT, true, TRAINPROPFILE_DESC));
        options.addOption(new Option(FORMAT_SHORT_OPT, FORMAT_LONG_OPT, true, FORMAT_DESC));
        options.addOption(new Option(CONF_SHORT_OPT, CONF_LONG_OPT, true, CONF_DESC));
        options.addOption(new Option(GENE_SHORT_OPT, GENE_LONG_OPT, true, GENE_DESC));
    }

    /** Creates a new Manager*/
    public ComparisonCmd(String propfile, String gene) throws IOException, TrainingDataException {
        if (propfile != null) {
            ClassifierFactory.setDataProp(propfile, false);
        }
        factory = ClassifierFactory.getFactory(gene);
    }

    /** given the parser and root, calls the tester to do the classification
     */
    public void doClassify(String s1, String s2, String detailFile, String sigFile, ClassificationResultFormatter.FORMAT format) throws IOException {

        FileInputStream inStream1 = new FileInputStream(s1);
        FileInputStream inStream2 = new FileInputStream(s2);
        BufferedWriter wt = new BufferedWriter(new FileWriter(detailFile));


        Classifier aClassifier = factory.createClassifier();
        TaxonTree root = null;
        SequenceReader parser = null;
        Sequence pSeq = null;
        int count = 0;

        try {
            parser = new SequenceReader(inStream1);
            while((pSeq = parser.readNextSequence()) != null) {
                try {
                    ClassificationResult result = aClassifier.classify(pSeq);
                    root = reconstructTree(result, root, SAMPLE1);
                    wt.write(ClassificationResultFormatter.getOutput(result, format));

                } catch (ShortSequenceException e) {
                    System.out.println(e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            parser = new SequenceReader(inStream2);
            while((pSeq = parser.readNextSequence()) != null) {
                try {
                    ClassificationResult result = aClassifier.classify(pSeq);
                    root = reconstructTree(result, root, SAMPLE2);
                    wt.write(ClassificationResultFormatter.getOutput(result, format));

                } catch (ShortSequenceException e) {
                    System.out.println(e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            parser.close();
            wt.close();
        }

        if (root != null) {
            // change the assignment count for the taxa based on the confidence cutoff value
            SigCalculator cal = new SigCalculator(root.getS1Count(), root.getS2Count(), this.confidenceCutoff);
            root.changeConfidence(cal);
            BufferedWriter sigWt = new BufferedWriter(new FileWriter(sigFile));
            sigWt.write("sample1:\t" + s1 + "\n");
            sigWt.write("sample2:\t" + s2 + "\n");
            sigWt.write("confidence:\t" + this.confidenceCutoff + "\n");
            printSignificance(root, sigWt);
            sigWt.close();
        }

    }

    /** reconstruct the hierarchical tree based on the ClassificationResult
     * we assume that the first assignment is always the root and the only root
     **/
    private TaxonTree reconstructTree(ClassificationResult result, TaxonTree root, String sample) {
        List assignments = result.getAssignments();

        Iterator assignIt = assignments.iterator();
        int nodeCount = 0;
        SeqInfo seqInfo = new SeqInfo(result.getSequence().getSeqName(), result.getSequence().getDesc());
        seqInfo.setReverse(result.isReverse());

        TaxonTree curTree = null;
        while (assignIt.hasNext()) {
            RankAssignment assign = (RankAssignment) assignIt.next();

            if (nodeCount == 0) {
                if (root == null) {     // the parent of the root is null
                    root = new TaxonTree(assign.getTaxid(), assign.getName(), assign.getRank(), null);
                } else {
                    if (root.getTaxid() != assign.getTaxid()) {
                        // this should never occur
                        throw new IllegalStateException("Error: the root " + assign.getTaxid()
                                + " of assignment for " + result.getSequence().getSeqName()
                                + " is different from the other sequences " + root.getTaxid());
                    }
                }
                curTree = root;

            } else {
                curTree = curTree.getChildbyTaxid(assign.getTaxid(), assign.getName(), assign.getRank());
            }
            Score score = new Score(assign.getConfidence(), seqInfo, curTree);
            if (sample.equals(SAMPLE1)) {
                curTree.addS1Score(score);
            } else {
                curTree.addS2Score(score);
            }
            nodeCount++;
        }
        return root;
    }

    private void printSignificance(TaxonTree root, BufferedWriter wt) throws IOException {
        ComparisonResultTreeSet resultSet = new ComparisonResultTreeSet();

        Iterator taxonIt = root.getTaxonIterator(10);
        while (taxonIt.hasNext()) {
            resultSet.add(taxonIt.next());
        }

        Iterator it = resultSet.iterator();

        wt.write("Significance\tRank\tName\tSample1\tSample2\n");
        while (it.hasNext()) {
            AbstractNode node = (AbstractNode) it.next();
            wt.write(node.getSignificance() + "\t" + node.getRank() + "\t" + node.getName() + "\t" + node.getS1Count() + "\t" + node.getS2Count() + "\n");
        }

    }

    private void setConfidenceCutoff(float conf) {
        if (conf < 0 || conf > 1) {
            throw new IllegalArgumentException("The confidence cutoff value should be between 0 - 1.0");
        }
        this.confidenceCutoff = conf;
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

    public static void main(String[] args) throws Exception {

        String queryFile1 = null;
        String queryFile2 = null;
        String class_outputFile = null;
        String compare_outputFile = null;
        String propFile = null;
        ClassificationResultFormatter.FORMAT format = ComparisonCmd.DEFAULT_FORMAT;
        float conf_cutoff = ComparisonCmd.DEFAULT_CON_CUTOFF;
        String gene = null;

        try {
            CommandLine line = new PosixParser().parse(options, args);

            if (line.hasOption(QUERYFILE1_SHORT_OPT)) {
                queryFile1 = line.getOptionValue(QUERYFILE1_SHORT_OPT);
            } else {
                throw new Exception("queryFile1 must be specified");
            }
            if (line.hasOption(QUERYFILE2_SHORT_OPT)) {
                queryFile2 = line.getOptionValue(QUERYFILE2_SHORT_OPT);
            } else {
                throw new Exception("queryFile2 must be specified");
            }

            if (line.hasOption(CLASS_OUTFILE_SHORT_OPT)) {
                class_outputFile = line.getOptionValue(CLASS_OUTFILE_SHORT_OPT);
            } else {
                throw new Exception("outputFile for classification results must be specified");
            }

            if (line.hasOption(COMPARE_OUTFILE_SHORT_OPT)) {
                compare_outputFile = line.getOptionValue(COMPARE_OUTFILE_SHORT_OPT);
            } else {
                throw new Exception("outputFile for comparsion results must be specified");
            }

            if (line.hasOption(TRAINPROPFILE_SHORT_OPT)) {
                if (gene != null) {
                    throw new IllegalArgumentException("Already specified the gene from the default location. Can not specify train_propfile");
                } else {
                    propFile = line.getOptionValue(TRAINPROPFILE_SHORT_OPT);
                }
            }
            if (line.hasOption(CONF_SHORT_OPT)) {
                conf_cutoff = Float.parseFloat(line.getOptionValue(CONF_SHORT_OPT));
            }
            if (line.hasOption(FORMAT_SHORT_OPT)) {
                String f = line.getOptionValue(FORMAT_SHORT_OPT);
                if (f.equals("allrank")) {
                    format = ClassificationResultFormatter.FORMAT.allRank;
                } else if (f.equals("fixrank")) {
                    format = ClassificationResultFormatter.FORMAT.fixRank;
                } else if (f.equals("db")) {
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
        } catch (Exception e) {
            System.out.println("Command Error: " + e.getMessage());
            new HelpFormatter().printHelp(120, "ComparisonCmd", "", options, "", true);
            return;
        }

        if (propFile == null && gene == null) {
            gene = ClassifierFactory.RRNA_16S_GENE;
        }

        ComparisonCmd cmd = new ComparisonCmd(propFile, gene);
        cmd.setConfidenceCutoff(conf_cutoff);
        printLicense();


        cmd.doClassify(queryFile1, queryFile2, class_outputFile, compare_outputFile, format);

    }
}