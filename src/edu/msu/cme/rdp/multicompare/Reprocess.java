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

import edu.msu.cme.rdp.classifier.cli.CmdOptions;
import edu.msu.cme.rdp.classifier.io.ClassificationResultFormatter;
import edu.msu.cme.rdp.classifier.utils.ClassifierFactory;
import edu.msu.cme.rdp.multicompare.visitors.DefaultPrintVisitor;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.*;
import org.apache.commons.io.output.NullWriter;


/**
 *
 * @author fishjord
 */
public class Reprocess {


    private static Options options = new Options();
   
    public static HashSet<String> readTaxonFilterFile(String file) throws IOException{
        HashSet<String> ret = new HashSet<String>();

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;

        while((line = reader.readLine()) != null) {
            if(line.trim().equals("")) continue;
            ret.add(line.trim());
            System.err.println(line.trim());
        }
        reader.close();
        return ret;
    }
    
    
     static {
        options.addOption(new Option(CmdOptions.HIER_OUTFILE_SHORT_OPT, CmdOptions.HIER_OUTFILE_LONG_OPT, true, CmdOptions.HIER_OUTFILE_DESC));
        options.addOption(new Option(CmdOptions.OUTFILE_SHORT_OPT, CmdOptions.OUTFILE_LONG_OPT, true, CmdOptions.OUTFILE_DESC));
        options.addOption(new Option(CmdOptions.RANK_SHORT_OPT, CmdOptions.RANK_LONG_OPT, true, "the rank to apply the confidence cutoff." +
                " Only assignment reuslts with confidence above the cutoff at the specified rank will be included in the assignment detail outfile. Default is the lowest rank"));
        options.addOption(new Option(CmdOptions.TAXON_SHORT_OPT, CmdOptions.TAXON_LONG_OPT, true, "the taxon name filter containing one taxon per line.\n" +
                "Only assignment results matching the taxon name will be included in assignment detail outfile."));
        options.addOption(new Option(CmdOptions.FORMAT_SHORT_OPT, CmdOptions.FORMAT_LONG_OPT, true, CmdOptions.FORMAT_DESC));
        options.addOption(new Option(CmdOptions.BOOTSTRAP_SHORT_OPT, CmdOptions.BOOTSTRAP_LONG_OPT, true, CmdOptions.BOOTSTRAP_DESC));
        options.addOption(new Option(CmdOptions.TRAINPROPFILE_SHORT_OPT, CmdOptions.TRAINPROPFILE_LONG_OPT, true, CmdOptions.TRAINPROPFILE_DESC));
        options.addOption(new Option(CmdOptions.GENE_SHORT_OPT, CmdOptions.GENE_LONG_OPT, true, CmdOptions.GENE_DESC));
     }
     
    /**
     * This class reprocesses the classification results (allrank output) and print out hierarchy output file, based on the confidence cutoff;
     * and print out only the detail classification results with assignment at certain rank with confidence above the cutoff or/and matching a given taxon.
     * @param args
     * @throws Exception 
     */
    public static void main(String [] args) throws Exception {
        
        PrintWriter assign_out = new PrintWriter(new NullWriter());
        float conf = 0.8f;
        PrintStream heir_out = null;
        String hier_out_filename = null;
        ClassificationResultFormatter.FORMAT format = ClassificationResultFormatter.FORMAT.allRank;
        String rank = null;
        String taxonFilterFile = null;
        String train_propfile = null;
        String gene = null;
        List<MCSample> samples = new ArrayList();
        
        try {
            CommandLine line = new PosixParser().parse(options, args);
            if (line.hasOption(CmdOptions.HIER_OUTFILE_SHORT_OPT) ) {
                hier_out_filename = line.getOptionValue(CmdOptions.HIER_OUTFILE_SHORT_OPT);
                heir_out = new PrintStream(hier_out_filename);                
            } else {
                throw new Exception("It make sense to provide output filename for " + CmdOptions.HIER_OUTFILE_LONG_OPT);
            }
             if (line.hasOption(CmdOptions.OUTFILE_SHORT_OPT) ) {
                assign_out = new PrintWriter(line.getOptionValue(CmdOptions.OUTFILE_SHORT_OPT));                
            }
                       
            if (line.hasOption(CmdOptions.RANK_SHORT_OPT) ) {
                rank = line.getOptionValue(CmdOptions.RANK_SHORT_OPT);                
            } 
            if (line.hasOption(CmdOptions.TAXON_SHORT_OPT) ) {
                taxonFilterFile = line.getOptionValue(CmdOptions.TAXON_SHORT_OPT);                
            }
            
            if (line.hasOption(CmdOptions.BOOTSTRAP_SHORT_OPT) ) {
                conf = Float.parseFloat(line.getOptionValue(CmdOptions.BOOTSTRAP_SHORT_OPT));
                 if (conf < 0 || conf > 1) {
                    throw new IllegalArgumentException("Confidence must be in the range [0,1]");
                }
            } 
            if (line.hasOption(CmdOptions.FORMAT_SHORT_OPT) ) {
                String f = line.getOptionValue(CmdOptions.FORMAT_SHORT_OPT);
                if (f.equalsIgnoreCase("allrank")) {
                    format = ClassificationResultFormatter.FORMAT.allRank;
                } else if (f.equalsIgnoreCase("fixrank")) {
                    format = ClassificationResultFormatter.FORMAT.fixRank;
                } else if (f.equalsIgnoreCase("db")) {
                    format = ClassificationResultFormatter.FORMAT.dbformat;
                } else if (f.equalsIgnoreCase("filterbyconf")) {
                    format = ClassificationResultFormatter.FORMAT.filterbyconf;
                } else {
                    throw new IllegalArgumentException("Not valid output format, only allrank, fixrank, filterbyconf and db allowed");
                }
            }
            if (line.hasOption(CmdOptions.TRAINPROPFILE_SHORT_OPT)) {
                if (gene != null) {
                    throw new IllegalArgumentException("Already specified the gene from the default location. Can not specify train_propfile");
                } else {
                    train_propfile = line.getOptionValue(CmdOptions.TRAINPROPFILE_SHORT_OPT);
                }
            }
            if (line.hasOption(CmdOptions.GENE_SHORT_OPT)) {
                if (train_propfile != null) {
                    throw new IllegalArgumentException("Already specified train_propfile. Can not specify gene any more");
                }
                gene = line.getOptionValue(CmdOptions.GENE_SHORT_OPT).toLowerCase();

                if (!gene.equals(ClassifierFactory.RRNA_16S_GENE) && !gene.equals(ClassifierFactory.FUNGALLSU_GENE)) {
                    throw new IllegalArgumentException(gene + " is NOT valid, only allows " + ClassifierFactory.RRNA_16S_GENE
                     + ", " + ClassifierFactory.FUNGALLSU_GENE + ", " + ClassifierFactory.FUNGALITS_warcup_GENE + " and " + ClassifierFactory.FUNGALITS_unite_GENE);
                }
            }
            args = line.getArgs();
            if ( args.length < 1){
                throw new Exception("Incorrect number of command line arguments");
            }
            
            for ( String arg: args){
                String[] inFileNames = arg.split(",");
                String inputFile = inFileNames[0];
                File idmappingFile = null;
                
                if (inFileNames.length == 2) {
                    idmappingFile = new File(inFileNames[1]);
                    if (!idmappingFile.exists()) {
                        System.err.println("Failed to find input file \"" + inFileNames[1] + "\"");
                        return;
                    }
                }

                MCSample nextSample = new MCSampleResult(inputFile, idmappingFile);
                samples.add(nextSample);
              
            }
        } catch (Exception e) {
            System.out.println("Command Error: " + e.getMessage());
            new HelpFormatter().printHelp(120, "Reprocess [options] <Classification_allrank_result>[,idmappingfile] ...", "", options, "");
            return;
        }
        
        if (train_propfile == null && gene == null) {
            gene = ClassifierFactory.RRNA_16S_GENE;
        }
        
        HashSet<String> taxonFilter = null;
        if ( taxonFilterFile != null){
            taxonFilter = readTaxonFilterFile(taxonFilterFile);
        }
        
        MultiClassifier multiClassifier = new MultiClassifier(train_propfile, gene);
        DefaultPrintVisitor printVisitor = new DefaultPrintVisitor(heir_out, samples);
        MultiClassifierResult result = multiClassifier.multiClassificationParser(samples, conf, assign_out, format, rank, taxonFilter);

        result.getRoot().topDownVisit(printVisitor);

        assign_out.close();
        heir_out.close();
        if ( multiClassifier.hasCopyNumber()){
            // print copy number corrected counts
            File cn_corrected_s =  new File (new File(hier_out_filename).getParentFile(), "cncorrected_" + hier_out_filename);
            PrintStream cn_corrected_hier_out = new PrintStream(cn_corrected_s);
            printVisitor = new DefaultPrintVisitor(cn_corrected_hier_out, samples, true);
            result.getRoot().topDownVisit(printVisitor);
            cn_corrected_hier_out.close();
        }
        
    }
}
