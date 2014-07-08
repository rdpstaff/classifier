/*
 * Copyright (C) 2013 wangqion
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package edu.msu.cme.rdp.classifier.cli;

import edu.msu.cme.rdp.classifier.Classifier;
import edu.msu.cme.rdp.classifier.io.ClassificationResultFormatter;
import edu.msu.cme.rdp.classifier.utils.ClassifierFactory;

/**
 *
 * @author wangqion
 */
public class CmdOptions {
    
        
    public static ClassificationResultFormatter.FORMAT DEFAULT_FORMAT = ClassificationResultFormatter.FORMAT.allRank;
    public static float DEFAULT_CONF = 0.8f;
    public static String DEFAULT_GENE = ClassifierFactory.RRNA_16S_GENE;
    
    // long options
    public static final String QUERYFILE_LONG_OPT = "queryFile";
    public static final String OUTFILE_LONG_OPT = "outputFile";
    public static final String TRAINPROPFILE_LONG_OPT = "train_propfile";
    public static final String FORMAT_LONG_OPT = "format";
    public static final String GENE_LONG_OPT = "gene";   // specify the gene if not provide train_propfile
    public static final String MIN_BOOTSTRAP_WORDS_LONG_OPT = "minWords";   
    public static final String HIER_OUTFILE_LONG_OPT = "hier_outfile";     
    public static final String BOOTSTRAP_LONG_OPT = "conf";
    public static final String RANK_LONG_OPT = "rank";
    public static final String TAXON_LONG_OPT = "taxon";
    public static final String BOOTSTRAP_OUTFILE_LONG_OPT = "bootstrap_outfile";
    public static final String SHORTSEQ_OUTFILE_LONG_OPT = "shortseq_outfile";
    public static final String BIOMFILE_LONG_OPT = "biomFile";
    public static final String SAMPLE_LONG_OPT = "biomFile";
    public static final String METADATA_LONG_OPT = "metadata";
    
    //short options
    public static final String QUERYFILE_SHORT_OPT = "q";
    public static final String OUTFILE_SHORT_OPT = "o";
    public static final String TRAINPROPFILE_SHORT_OPT = "t";
    public static final String FORMAT_SHORT_OPT = "f";
    public static final String GENE_SHORT_OPT = "g";
    public static final String MIN_BOOTSTRAP_WORDS_SHORT_OPT = "w";
    public static final String HIER_OUTFILE_SHORT_OPT = "h";
    public static final String BOOTSTRAP_SHORT_OPT = "c"; 
    public static final String RANK_SHORT_OPT = "r";
    public static final String TAXON_SHORT_OPT = "n";
    public static final String BOOTSTRAP_OUTFILE_SHORT_OPT = "b";
    public static final String SHORTSEQ_OUTFILE_SHORT_OPT = "s";
    public static final String BIOMFILE_SHORT_OPT = "m";
    public static final String METADATA_SHORT_OPT = "d";
    
    // description of the options
    public static final String QUERYFILE_DESC = "legacy option, no longer needed ";
    public static final String OUTFILE_DESC = "tab-delimited text output file for classification assignment.";
    public static final String TRAINPROPFILE_DESC = "property file containing the mapping of the training files if not using the default."
            + " Note: the training files and the property file should be in the same directory.";
    public static final String FORMAT_DESC = "tab-delimited output format: [allrank|fixrank|biom|filterbyconf|db]. Default is " + DEFAULT_FORMAT + "."
            + "\n allrank: outputs the results for all ranks applied for each sequence: seqname, orientation, taxon name, rank, conf, ..."
            + "\n fixrank: only outputs the results for fixed ranks in order: domain, phylum, class, order, family, genus"
            + "\n biom: outputs rich dense biom format if OTU or metadata provided" 
            + "\n filterbyconf: only outputs the results for major ranks as in fixrank, results below the confidence cutoff were bin to a higher rank unclassified_node"
            + "\n db: outputs the seqname, trainset_no, tax_id, conf.";
    public static final String GENE_DESC = ClassifierFactory.RRNA_16S_GENE + ", " + ClassifierFactory.FUNGALLSU_GENE 
            +  ", " + ClassifierFactory.FUNGALITS_warcup_GENE + ", " + ClassifierFactory.FUNGALITS_unite_GENE
            + ". Default is " + DEFAULT_GENE +  ". This option can be overwritten by -t option";
    public static final String MIN_WORDS_DESC = "minimum number of words for each bootstrap trial. Default(maximum) is 1/8 of the words of each sequence. Minimum is " + Classifier.MIN_BOOTSTRSP_WORDS ;
    public static final String HIER_OUTFILE_DESC = "tab-delimited output file containing the assignment count for each taxon in the hierarchical format. Default is null.";
    public static final String BOOTSTRAP_DESC = "assignment confidence cutoff used to determine the assignment count for each taxon. Range [0-1], Default is " + DEFAULT_CONF + ".";
  
    public static final String BOOTSTRAP_OUTFILE_DESC = "the output file containing the number of matching assignments out of 100 bootstraps for major ranks. Default is null";
    public static final String SHORTSEQ_OUTFILE_DESC = "the output file containing the sequence names that are too short to be classified";
    public static final String BIOMFILE_DESC = "the input clluster biom file. The classification result will replace the taxonomy of the corresponding cluster id.";
    public static final String METADATA_DESC = "the tab delimited metadata file for the samples, with first row containing attribute name and first column containing the sample name";
}
