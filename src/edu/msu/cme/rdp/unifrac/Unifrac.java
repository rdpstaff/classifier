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
package edu.msu.cme.rdp.unifrac;

import edu.msu.cme.rdp.multicompare.MCSample;
import edu.msu.cme.rdp.multicompare.taxon.MCTaxon;
import edu.msu.cme.rdp.taxatree.UnifracTree;
import edu.msu.cme.rdp.taxatree.UnifracTree.UnifracResult;
import edu.msu.cme.rdp.taxatree.utils.NewickTreeBuilder;
import edu.msu.cme.rdp.taxatree.utils.NewickTreeBuilder.NewickTaxonFactory;
import edu.msu.cme.rdp.unifrac.UnifracTreeBuilder.UnifracSample;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;

/**
 *
 * @author fishjord
 */
public class Unifrac {

    private static final DecimalFormat format = new DecimalFormat("0.0000");
    private static final Options options = new Options();

    static {
        options.addOption("t", "tree", true, "Newick tree file to preform unifrac calculations on, must specify a sample mapping file");
        options.addOption("s", "sample-mapping", true, "Sample mapping file specifying what sample sequences are in");
        options.addOption("f", "sequence-file", true, "Comma seperated list of sequence files");
        options.addOption("u", "unweighted", false, "Perform unweighted unifrac calculation");
        options.addOption("w", "weighted", false, "Perform weighted unifrac calculation");
        options.addOption("S", "significance", false, "Compute significance with unifrac metric");
        options.addOption("o", "outfile", true, "Write results to file instead of stdout");
    }

    private static void printUsage() {
        new HelpFormatter().printHelp("Unifrac", options, true);
    }

    private static UnifracTree parseNewickTree(String treeFile, Map<String, UnifracSample> sampleMap) throws IOException {
        UnifracTree unifracTree = new UnifracTree();

        NewickTreeBuilder builder = new NewickTreeBuilder(unifracTree, new FileInputStream(treeFile),
                new NewickTaxonFactory<MCTaxon>() {

                    public MCTaxon buildTaxon(int taxid, String name, float distance) {
                        return new UnifracTaxon(taxid, name, "", distance);
                    }
                });


        for (String seqName : sampleMap.keySet()) {
            UnifracSample sample = sampleMap.get(seqName);
            Integer taxid = builder.getTaxidByName(seqName);
            if (taxid == null) {
                System.err.println("Couldn't find " + seqName + " in the tree");
                continue;
            }

            unifracTree.getChildTaxon(taxid).addSampleCount(sample.sample, sample.count);
        }

        unifracTree.refreshInnerTaxa();

        return unifracTree;
    }

    public static void printResults(PrintStream out, UnifracResult unifracResult, String label) {

        List<MCSample> labels = new ArrayList(unifracResult.getSamples());
        Collections.sort(labels, new Comparator<MCSample>() {

            public int compare(MCSample o1, MCSample o2) {
                return o1.getSampleName().compareTo(o2.getSampleName());
            }
        });

        out.println(label);
        for (int i = 0; i < labels.size(); i++) {
            for (int j = i + 1; j < labels.size(); j++) {
                int row = unifracResult.getSamples().indexOf(labels.get(i));
                int col = unifracResult.getSamples().indexOf(labels.get(j));
                out.println(labels.get(i) + "-" + labels.get(j) + "\t" + format.format(unifracResult.getUnifracMatrix()[row][col]) + "  ");
            }
        }
    }

    private static Map<String, UnifracSample> readSampleMap(String sampleFile) throws IOException {
        Map<String, UnifracSample> ret = new HashMap();
        Map<String, MCSample> sampleMap = new HashMap();

        int lineno = 1;
        Scanner s = new Scanner(new File(sampleFile)).useDelimiter("\n");
        while (s.hasNext()) {
            String line = s.next().trim();
            if (line.equals("")) {
                continue;
            }

            String[] tokens = line.split("\\s+");
            if (tokens.length < 2) {
                throw new IOException("Failed to parse sample mapping file (lineno=" + lineno + ")");
            }

            String sampleName = tokens[1];
            String seqName = tokens[0];
            int sampleCount = 1;

            try {
                sampleCount = Integer.parseInt(tokens[2]);
            } catch (Exception e) {
            }

            if(!sampleMap.containsKey(sampleName))
                sampleMap.put(sampleName, new MCSample(sampleName));

            UnifracSample unifracSample = new UnifracSample();
            unifracSample.sample = sampleMap.get(sampleName);
            unifracSample.count = sampleCount;

            ret.put(seqName, unifracSample);

            lineno++;
        }
        s.close();

        return ret;
    }

    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new PosixParser();
        CommandLine line = null;
        try {
             line = parser.parse(options, args);
        } catch(UnrecognizedOptionException e) {
            System.err.println(e.getMessage());
            printUsage();
            return;
        }
        UnifracTree unifracTree = null;
        PrintStream out = System.out;

        if (line.hasOption("tree") && line.hasOption("sequence-files")) {
            printUsage();
        } else if (!(line.hasOption("weighted") || line.hasOption("unweighted") || line.hasOption("significance"))) {
            System.err.println("Must specify at least one calculation option");
            printUsage();
        } else if (line.hasOption("sample-mapping")) {
            Map<String, UnifracSample> sampleMap = readSampleMap(line.getOptionValue("sample-mapping"));

            if (line.hasOption("tree")) {
                unifracTree = parseNewickTree(line.getOptionValue("tree"), sampleMap);
            }
        } else {
            if (!line.hasOption("sample-mapping")) {
                System.err.println("A sample mapping file must be provided");
            }
            printUsage();
        }

        if (line.hasOption("outfile")) {
            out = new PrintStream(line.getOptionValue("outfile"));
        }

        if (unifracTree != null) {
            if (line.hasOption("unweighted")) {
                printResults(out, unifracTree.computeUnifrac(), "Unweighted Unifrac");
                if (line.hasOption("significance")) {
                    printResults(out, unifracTree.computeUnifracSig(1000, false), "Unweighted Unifrac Significance");
                }
            }

            if (line.hasOption("weighted")) {
                printResults(out, unifracTree.computeWeightedUnifrac(), "Weighted Unifrac");
                if (line.hasOption("significance")) {
                    printResults(out, unifracTree.computeUnifracSig(1000, true), "Weighted Unifrac Significance");
                }
            }
        }
    }
}
