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

package edu.msu.cme.rdp.classifier.train.validation.crossvalidate;

import edu.msu.cme.rdp.classifier.train.validation.NBClassifier;
import java.io.File;
import java.io.IOException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 *
 * @author wangqion
 */
public class CrossValidateMain {

    private static Options options = new Options();
    static {
        options.addOption("t", "tax_file", true, "taxonomy file");
        options.addOption("s", "source_file", true, "the source of the training fasta file");
        options.addOption("o", "out_file", true, "the output file");
        options.addOption("p", "partial_length", true, "length of the region to be tested. If not specified, full length will be used");
        options.addOption("f", "fraction", true, "fraction of the complete set as test set, default is 0.1");
        options.addOption("r", "rdmRank", true, "if specified, random select a fraction of taxa at given rank, " +
        		"and return all the sequence IDs assigned to the selected taxa as test set. If rank is not specified, " +
        		"a fraction of sequences will be selected from the source file to use as test set");
        options.addOption("w", "minWords", true, "minimum number of words for each run of bootstrap, minium is " + NBClassifier.MIN_BOOTSTRSP_WORDS );


    }

    /**
     * This is the main method for cross validation test. 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException{
        String tax_file = null;
        String source_file = null;
        String out_file = null;
        Integer partialLength = null;  // default is full length
        float fraction = 0.1f;
        String rdmSelectedRank = null;
        int min_bootstrap_words = NBClassifier.MIN_BOOTSTRSP_WORDS;
        try {
            CommandLine line = new PosixParser().parse(options, args);
            if (line.hasOption("tax_file") ) {
            	tax_file = line.getOptionValue("tax_file");
            } else {
                throw new ParseException("Taxonomy file must be specified");
            }
            if (line.hasOption("source_file") ) {
            	source_file = line.getOptionValue("source_file");
            } else {
                throw new ParseException("Source training fasta file must be specified");
            }
            if (line.hasOption("out_file") ) {
            	out_file = line.getOptionValue("out_file");
            } else {
                throw new ParseException("Output file must be specified");
            }

            if (line.hasOption("partial_length") ) {
;            	partialLength = new Integer(line.getOptionValue("partial_length"));
            }
            if (line.hasOption("fraction") ) {
            	fraction = Float.parseFloat(line.getOptionValue("fraction"));
            }
            if (line.hasOption("rdmRank") ) {
                rdmSelectedRank = line.getOptionValue("rdmRank");
            }
            if (line.hasOption("minWords")) {
                min_bootstrap_words = Integer.parseInt(line.getOptionValue("minWords"));
                if (min_bootstrap_words < NBClassifier.MIN_BOOTSTRSP_WORDS) {
                    throw new IllegalArgumentException(min_bootstrap_words + " must be at least " + NBClassifier.MIN_BOOTSTRSP_WORDS);
                }                
            }
            
        } catch (ParseException ex) {
            new HelpFormatter().printHelp(120, "CrossValidateMain", "", options, "", true);
            return;
        }
        
        boolean useSeed = true;  // use seed for random number generator

        CrossValidate theObj = new CrossValidate();
        theObj.runTest(new File(tax_file), new File(source_file), rdmSelectedRank, fraction, partialLength, useSeed, min_bootstrap_words);


    }
}
