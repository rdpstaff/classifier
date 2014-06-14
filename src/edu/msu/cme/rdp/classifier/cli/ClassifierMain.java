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

import edu.msu.cme.rdp.alignment.errorcheck.RmPartialSeqs;
import edu.msu.cme.rdp.classifier.comparison.ComparisonCmd;
import edu.msu.cme.rdp.classifier.train.ClassifierTraineeMaker;
import edu.msu.cme.rdp.classifier.train.validation.crossvalidate.CrossValidateMain;
import edu.msu.cme.rdp.classifier.train.validation.leaveoneout.LeaveOneOutTesterMain;
import edu.msu.cme.rdp.classifier.train.validation.movingwindow.MainMovingWindow;
import edu.msu.cme.rdp.classifier.train.validation.distance.TaxaSimilarityMain;
import edu.msu.cme.rdp.multicompare.Main;
import edu.msu.cme.rdp.multicompare.MergeTaxonCount;
import edu.msu.cme.rdp.multicompare.Reprocess;
import edu.msu.cme.rdp.readseq.utils.ResampleSeqFile;
import edu.msu.cme.rdp.readseq.utils.RmDupSeqs;
import java.util.Arrays;

/**
 *
 * @author wangqion
 */
public class ClassifierMain {
    public static void main(String [] args) throws Exception {
        String usage = "USAGE: ClassifierMain <subcommand> <subcommand args ...>" +
                "\ndefault command is classify" +
                "\n\tclassify      - classify one or multiple samples" +
                "\n\tcrossvalidate - cross validate accuracy testing" + 
                "\n\tlibcompare    - compare two samples" +
                "\n\tloot          - leave one (sequence or taxon) out accuracy testing" +
                "\n\tmerge-detail  - merge classification detail result files to create a taxon assignment counts file" +
                "\n\tmerge-count   - merge multiple taxon assignment count files to into one count file" +
                "\n\trandom-sample - random select a subset or subregion of sequences" +
                "\n\trm-dupseq     - remove identical or any sequence contained by another sequence" +
                "\n\trm-partialseq - remove partial sequences" +
                "\n\ttaxa-sim      - calculate and plot the similarities within taxa" +
                "\n\ttrain         - retrain classifier" ;
                //"\n\tsegment       - accuracy testing with short segments of the training sequences";
        if(args.length == 0 ) {
            System.err.println(usage);
            return;
        }

        String cmd = args[0];
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);

        if(cmd.equals("classify")) {
            Main.main(newArgs);
        } else if(cmd.equals("libcompare")) {
            ComparisonCmd.main(newArgs);
        } else if(cmd.equals("merge-detail")) {
            Reprocess.main(newArgs);
        } else if(cmd.equals("merge-count")) {
            MergeTaxonCount.main(newArgs);
        } else if(cmd.equals("train")) {
            ClassifierTraineeMaker.main(newArgs);
        } else if(cmd.equals("loot")) {
            LeaveOneOutTesterMain.main(newArgs);
        } else if(cmd.equals("crossvalidate")) {
            CrossValidateMain.main(newArgs);
        } else if(cmd.equals("taxa-sim")) {
            TaxaSimilarityMain.main(newArgs);
        } else if(cmd.equals("random-sample")) {
            ResampleSeqFile.main(newArgs);
        } else if(cmd.equals("rm-dupseq")) {
            RmDupSeqs.main(newArgs);
        } else if(cmd.equals("rm-partialseq")) {
            RmPartialSeqs.main(newArgs);
        } else if(cmd.equals("segment")) {
            MainMovingWindow.main(newArgs);
        } else if (cmd.startsWith("-") ){ // we need to keep the classify as the default command
            Main.main(args);
        } else {
            System.err.println("ERROR: " + "wrong subcommand");
            System.err.println(usage);
            return;
        }
    }
    
}
