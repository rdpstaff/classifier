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

import edu.msu.cme.rdp.classifier.cli.CmdOptions;
import edu.msu.cme.rdp.classifier.train.validation.NBClassifier;
import edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain;
import static edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain.LENGTH_DESC;
import static edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain.LENGTH_LONG_OPT;
import static edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain.LENGTH_SHORT_OPT;
import static edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain.OUTFILE_DESC;
import static edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain.OUTFILE_LONG_OPT;
import static edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain.OUTFILE_SHORT_OPT;
import static edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain.QUERYFILE_DESC;
import static edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain.QUERYFILE_LONG_OPT;
import static edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain.QUERYFILE_SHORT_OPT;
import static edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain.TRAIN_SEQFILE_DESC;
import static edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain.TRAIN_SEQFILE_LONG_OPT;
import static edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain.TRAIN_SEQFILE_SHORT_OPT;
import static edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain.TRAIN_TAXONFILE_DESC;
import static edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain.TRAIN_TAXONFILE_LONG_OPT;
import static edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain.TRAIN_TAXONFILE_SHORT_OPT;
import java.io.File;
import java.io.IOException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
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
        options.addOption(new Option(TRAIN_SEQFILE_SHORT_OPT, TRAIN_SEQFILE_LONG_OPT, true, TRAIN_SEQFILE_DESC));
        options.addOption(new Option(TRAIN_TAXONFILE_SHORT_OPT, TRAIN_TAXONFILE_LONG_OPT, true, TRAIN_TAXONFILE_DESC));
        options.addOption(new Option(OUTFILE_SHORT_OPT, OUTFILE_LONG_OPT, true, OUTFILE_DESC));
        options.addOption(new Option(LENGTH_SHORT_OPT, LENGTH_LONG_OPT, true, LENGTH_DESC));
        options.addOption(new Option(CmdOptions.MIN_BOOTSTRAP_WORDS_SHORT_OPT, CmdOptions.MIN_BOOTSTRAP_WORDS_LONG_OPT, true, CmdOptions.MIN_WORDS_DESC));
        options.addOption("f", "fraction", true, "fraction of the complete set as test set, default is 0.1");
        options.addOption("r", "rdmRank", true, "if specified, random select a fraction of taxa at the given rank, " +
        		"and use all the sequence assigned to the selected taxa as test set. If rank is not specified, " +
        		"a fraction of sequences will be selected from the source file to use as test set");
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
            if (line.hasOption(TRAIN_TAXONFILE_SHORT_OPT) ) {
            	tax_file = line.getOptionValue(TRAIN_TAXONFILE_SHORT_OPT);
            } else {
                throw new ParseException("Taxonomy file must be specified");
            }
            if (line.hasOption(TRAIN_SEQFILE_SHORT_OPT) ) {
            	source_file = line.getOptionValue(TRAIN_SEQFILE_SHORT_OPT);
            } else {
                throw new ParseException("Source training fasta file must be specified");
            }
            if (line.hasOption(OUTFILE_SHORT_OPT) ) {
            	out_file = line.getOptionValue(OUTFILE_SHORT_OPT);
            } else {
                throw new ParseException("Output file must be specified");
            }

            if (line.hasOption(LENGTH_SHORT_OPT) ) {
;            	partialLength = new Integer(line.getOptionValue(LENGTH_SHORT_OPT));
            }
            if (line.hasOption("fraction") ) {
            	fraction = Float.parseFloat(line.getOptionValue("fraction"));
            }
            if (line.hasOption("rdmRank") ) {
                rdmSelectedRank = line.getOptionValue("rdmRank");
            }
            if (line.hasOption(CmdOptions.MIN_BOOTSTRAP_WORDS_SHORT_OPT)) {
                min_bootstrap_words = Integer.parseInt(line.getOptionValue(CmdOptions.MIN_BOOTSTRAP_WORDS_SHORT_OPT));
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
        theObj.runTest(new File(tax_file), new File(source_file), new File(out_file), rdmSelectedRank, fraction, partialLength, useSeed, min_bootstrap_words);


    }
}
