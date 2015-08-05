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
import edu.msu.cme.rdp.classifier.cli.CmdOptions;
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
    public static final String COMPARE_OUTFILE_LONG_OPT = "compare_outputFile";

    //short options
    public static final String QUERYFILE1_SHORT_OPT = "q1";
    public static final String QUERYFILE2_SHORT_OPT = "q2";
    public static final String COMPARE_OUTFILE_SHORT_OPT = "o";

    // description of the options
    public static final String QUERYFILE_DESC = "query file contains sequences in one of the following formats: Fasta, Genbank and EMBL.";
    public static final String COMPARE_OUTFILE_DESC = "output file name for the comparsion results.";
    
    private static Options options = new Options();
    private ClassifierFactory factory;
    private float confidenceCutoff = CmdOptions.DEFAULT_CONF;  // default
    private static final String SAMPLE1 = "sample1";
    private static final String SAMPLE2 = "sample2";

    static {
        options.addOption(new Option(QUERYFILE1_SHORT_OPT, QUERYFILE1_LONG_OPT, true, QUERYFILE_DESC));
        options.addOption(new Option(QUERYFILE2_SHORT_OPT, QUERYFILE2_LONG_OPT, true, QUERYFILE_DESC));
        options.addOption(new Option(CmdOptions.OUTFILE_SHORT_OPT, CmdOptions.OUTFILE_LONG_OPT, true, CmdOptions.OUTFILE_DESC));
        options.addOption(new Option(COMPARE_OUTFILE_SHORT_OPT, COMPARE_OUTFILE_LONG_OPT, true, COMPARE_OUTFILE_DESC));
        options.addOption(new Option(CmdOptions.TRAINPROPFILE_SHORT_OPT, CmdOptions.TRAINPROPFILE_LONG_OPT, true, CmdOptions.TRAINPROPFILE_DESC));
        options.addOption(new Option(CmdOptions.FORMAT_SHORT_OPT, CmdOptions.FORMAT_LONG_OPT, true, CmdOptions.FORMAT_DESC));
        options.addOption(new Option(CmdOptions.BOOTSTRAP_SHORT_OPT, CmdOptions.BOOTSTRAP_LONG_OPT, true, CmdOptions.BOOTSTRAP_DESC));
        options.addOption(new Option(CmdOptions.GENE_SHORT_OPT, CmdOptions.GENE_LONG_OPT, true, CmdOptions.GENE_DESC));
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
                    System.err.println(e.getMessage());
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
                    System.err.println(e.getMessage());
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
        ClassificationResultFormatter.FORMAT format = CmdOptions.DEFAULT_FORMAT;
        float conf_cutoff = CmdOptions.DEFAULT_CONF;
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

            if (line.hasOption(CmdOptions.OUTFILE_SHORT_OPT)) {
                class_outputFile = line.getOptionValue(CmdOptions.OUTFILE_SHORT_OPT);
            } else {
                throw new Exception("outputFile for classification results must be specified");
            }

            if (line.hasOption(COMPARE_OUTFILE_SHORT_OPT)) {
                compare_outputFile = line.getOptionValue(COMPARE_OUTFILE_SHORT_OPT);
            } else {
                throw new Exception("outputFile for comparsion results must be specified");
            }

            if (line.hasOption(CmdOptions.TRAINPROPFILE_SHORT_OPT)) {
                if (gene != null) {
                    throw new IllegalArgumentException("Already specified the gene from the default location. Can not specify train_propfile");
                } else {
                    propFile = line.getOptionValue(CmdOptions.TRAINPROPFILE_SHORT_OPT);
                }
            }
            if (line.hasOption(CmdOptions.BOOTSTRAP_SHORT_OPT)) {
                conf_cutoff = Float.parseFloat(line.getOptionValue(CmdOptions.BOOTSTRAP_SHORT_OPT));
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
        } catch (Exception e) {
            System.err.println("Command Error: " + e.getMessage());
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
