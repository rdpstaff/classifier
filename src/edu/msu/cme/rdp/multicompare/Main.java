/*
 * Copyright (C) 2012 Michigan State University <rdpstaff at msu.edu>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.msu.cme.rdp.multicompare;

import edu.msu.cme.rdp.classifier.Classifier;
import edu.msu.cme.rdp.classifier.ClassifierCmd;
import edu.msu.cme.rdp.classifier.cli.CmdOptions;
import edu.msu.cme.rdp.classifier.io.ClassificationResultFormatter;
import edu.msu.cme.rdp.classifier.utils.ClassifierFactory;
import edu.msu.cme.rdp.multicompare.MultiClassifier.MultiClassifierResult;
import edu.msu.cme.rdp.multicompare.taxon.MCTaxon;
import edu.msu.cme.rdp.multicompare.visitors.DefaultPrintVisitor;
import edu.msu.cme.rdp.taxatree.ConcretRoot;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.output.NullWriter;

/**
 *
 * @author fishjord
 */
public class Main {

    private static final Options options = new Options();
      
    static {
        options.addOption(new Option(CmdOptions.QUERYFILE_SHORT_OPT, CmdOptions.QUERYFILE_LONG_OPT, false, CmdOptions.QUERYFILE_DESC));
        options.addOption(new Option(CmdOptions.OUTFILE_SHORT_OPT, CmdOptions.OUTFILE_LONG_OPT, true, CmdOptions.OUTFILE_DESC));
        options.addOption(new Option(CmdOptions.TRAINPROPFILE_SHORT_OPT, CmdOptions.TRAINPROPFILE_LONG_OPT, true, CmdOptions.TRAINPROPFILE_DESC));
        options.addOption(new Option(CmdOptions.FORMAT_SHORT_OPT, CmdOptions.FORMAT_LONG_OPT, true, CmdOptions.FORMAT_DESC));
        options.addOption(new Option(CmdOptions.GENE_SHORT_OPT, CmdOptions.GENE_LONG_OPT, true, CmdOptions.GENE_DESC));
        options.addOption(new Option(CmdOptions.MIN_BOOTSTRAP_WORDS_SHORT_OPT, CmdOptions.MIN_BOOTSTRAP_WORDS_LONG_OPT, true, CmdOptions.MIN_WORDS_DESC));
        options.addOption(new Option(CmdOptions.HIER_OUTFILE_SHORT_OPT, CmdOptions.HIER_OUTFILE_LONG_OPT, true, CmdOptions.HIER_OUTFILE_DESC));
        options.addOption(new Option(CmdOptions.BOOTSTRAP_SHORT_OPT, CmdOptions.BOOTSTRAP_LONG_OPT, true, CmdOptions.BOOTSTRAP_DESC));
        options.addOption(new Option(CmdOptions.BOOTSTRAP_OUTFILE_SHORT_OPT, CmdOptions.BOOTSTRAP_OUTFILE_LONG_OPT, true, CmdOptions.BOOTSTRAP_OUTFILE_DESC));
        options.addOption(new Option(CmdOptions.SHORTSEQ_OUTFILE_SHORT_OPT, CmdOptions.SHORTSEQ_OUTFILE_LONG_OPT, true, CmdOptions.SHORTSEQ_OUTFILE_DESC));
    }
   

    public static void printResults(ConcretRoot<MCTaxon> root, List<MCSample> samples, PrintStream heirOut, PrintStream bootstrapOut) throws IOException {
        DefaultPrintVisitor printVisitor = new DefaultPrintVisitor(heirOut, samples);
        root.topDownVisit(printVisitor);
        for (MCSample sample : samples) {
            MCSamplePrintUtil.printBootstrapCountTable(bootstrapOut, sample);
        }
    }

    public static Map<String, String> readSampleMapping(String file) throws IOException{
        Map<String, String> ret = new HashMap();

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;

        while((line = reader.readLine()) != null) {
            if(line.trim().equals("")) continue;

            String seqid = line.split("\t")[0].trim();
            String sample = line.split("\t")[1].trim();

            ret.put(seqid, sample);
        }

        reader.close();

        return ret;
    }

    public static Map<String, Integer> readReplicateMapping(String file) throws IOException {
        Map<String, Integer> ret = new HashMap();

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;

        while((line = reader.readLine()) != null) {
            if(line.trim().equals("")) continue;

            String seqid = line.split("\t")[0].trim();
            int replicates = Integer.valueOf(line.split("\t")[1].trim());

            ret.put(seqid, replicates);
        }

        reader.close();

        return ret;
    }

    public static void main(String[] args) throws Exception {
        PrintStream hier_out = null;
        PrintWriter assign_out = new PrintWriter(new NullWriter());
        PrintStream bootstrap_out = null;
        String propFile = null;
        PrintWriter shortseq_out = null;
        List<MCSample> samples = new ArrayList();
        ClassificationResultFormatter.FORMAT format = ClassificationResultFormatter.FORMAT.allRank;
        float conf = CmdOptions.DEFAULT_CONF;
        String gene = null;
        int min_bootstrap_words = Classifier.MIN_BOOTSTRSP_WORDS;
        
        try {
            CommandLine line = new PosixParser().parse(options, args);

            if (line.hasOption(CmdOptions.OUTFILE_SHORT_OPT)) {
                assign_out = new PrintWriter(line.getOptionValue(CmdOptions.OUTFILE_SHORT_OPT));
            } else {
                throw new IllegalArgumentException("Require the output file for classification assignment" );
            } 
            if (line.hasOption(CmdOptions.HIER_OUTFILE_SHORT_OPT)) {
                hier_out = new PrintStream(line.getOptionValue(CmdOptions.HIER_OUTFILE_SHORT_OPT));
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
            if (line.hasOption(CmdOptions.BOOTSTRAP_SHORT_OPT)) {
                String confString = line.getOptionValue(CmdOptions.BOOTSTRAP_SHORT_OPT);
                try {
                    conf = Float.valueOf(confString);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Confidence must be a decimal number");
                }

                if (conf < 0 || conf > 1) {
                    throw new IllegalArgumentException("Confidence must be in the range [0,1]");
                }
            }
            if (line.hasOption(CmdOptions.SHORTSEQ_OUTFILE_SHORT_OPT)) {
                shortseq_out = new PrintWriter(line.getOptionValue(CmdOptions.SHORTSEQ_OUTFILE_SHORT_OPT));
            }
            if (line.hasOption(CmdOptions.BOOTSTRAP_OUTFILE_SHORT_OPT)) {
                bootstrap_out = new PrintStream(line.getOptionValue(CmdOptions.BOOTSTRAP_OUTFILE_SHORT_OPT));
            }
             
            args = line.getArgs();
            for ( String arg: args){
                String[] inFileNames = arg.split(",");
                File inputFile = new File(inFileNames[0]);
                File idmappingFile = null;
                if (!inputFile.exists()) {
                    System.err.println("Failed to find input file \"" + inFileNames[0] + "\"");
                    return;
                }
                if (inFileNames.length == 2) {
                    idmappingFile = new File(inFileNames[1]);
                    if (!idmappingFile.exists()) {
                        System.err.println("Failed to find input file \"" + inFileNames[1] + "\"");
                        return;
                    }
                }

                MCSample nextSample = new MCSample(inputFile, idmappingFile);
                samples.add(nextSample);
            }
            if ( propFile == null && gene == null){
                gene = CmdOptions.DEFAULT_GENE;
            }
            if (samples.size() < 1) {
                throw new IllegalArgumentException("Require at least one sample files");
            }
        }catch (Exception e) {
            System.out.println("Command Error: " + e.getMessage());
            new HelpFormatter().printHelp(80, " [options] <samplefile>[,idmappingfile] ...", "", options, "");
            return;
        }
                 
        MultiClassifier multiClassifier = new MultiClassifier(propFile, gene);
        MultiClassifierResult result = multiClassifier.multiCompare(samples, conf, assign_out, format, min_bootstrap_words);
        assign_out.close();
        if ( hier_out != null){
            DefaultPrintVisitor printVisitor = new DefaultPrintVisitor(hier_out, samples);
            result.getRoot().topDownVisit(printVisitor);
            hier_out.close();
        }
        
        if ( bootstrap_out != null){
            for (MCSample sample : samples) {
                MCSamplePrintUtil.printBootstrapCountTable(bootstrap_out, sample);
            }
            bootstrap_out.close();
        }

        if ( shortseq_out != null){
            for (String id: result.getBadSequences()){
                shortseq_out.write(id +"\n");
            } 
            shortseq_out.close();   
        }
            
    }
    
}
