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
package edu.msu.cme.rdp.classifier.train.validation.distance;

import edu.msu.cme.rdp.alignment.AlignmentMode;
import edu.msu.cme.rdp.alignment.pairwise.PairwiseAligner;
import edu.msu.cme.rdp.alignment.pairwise.PairwiseAlignment;
import edu.msu.cme.rdp.alignment.pairwise.ScoringMatrix;
import edu.msu.cme.rdp.alignment.pairwise.rna.DistanceModel;
import edu.msu.cme.rdp.alignment.pairwise.rna.IdentityDistanceModel;
import edu.msu.cme.rdp.alignment.pairwise.rna.OverlapCheckFailedException;
import edu.msu.cme.rdp.classifier.train.LineageSequence;
import edu.msu.cme.rdp.classifier.train.LineageSequenceParser;
import edu.msu.cme.rdp.classifier.train.validation.HierarchyTree;
import edu.msu.cme.rdp.classifier.train.validation.Taxonomy;
import edu.msu.cme.rdp.classifier.train.validation.TreeFactory;
import edu.msu.cme.rdp.readseq.stat.StdevCal;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

/**
 *
 * @author wangqion
 */
public class PairwiseSeqDistance {
    private static final char gapChar = '-';    
    private ScoringMatrix scoringMatrix = ScoringMatrix.getDefaultNuclMatrix();
    private AlignmentMode mode = AlignmentMode.overlap;
    private boolean show_alignment = false;
    private static final Options options = new Options();
    private static DistanceModel dist = new IdentityDistanceModel();
    private HashMap<Taxonomy, ArrayList<Double>> distanceMap = new HashMap<Taxonomy, ArrayList<Double>>();
    private ArrayList<LineageSequence> seqList = new ArrayList<LineageSequence>();
    TreeFactory factory = null;
        
    static {          
        options.addOption("a", "alignment-mode", true, "Alignment mode: overlap, glocal, local or global. default = overlap");
        options.addOption("w", "show_alignment", false, "if true, output the detailed alignment to stdout. default = false");
    }
    
    
    public PairwiseSeqDistance(String trainseqFile, String taxFile, AlignmentMode mode, boolean show_alignment ) throws IOException, OverlapCheckFailedException{
        this.mode = mode;
        this.show_alignment = show_alignment;
        factory = new TreeFactory(new FileReader(taxFile));
        LineageSequenceParser parser = new LineageSequenceParser(new File(trainseqFile));
        
        while (parser.hasNext()) {
            LineageSequence tmp = (LineageSequence) parser.next();
            seqList.add(tmp);
            factory.addSequence((LineageSequence) parser.next());
        }
        parser.close();
        
        calDist();
    }
   
    private void calDist() throws OverlapCheckFailedException{
        HashMap<String, HierarchyTree> nodeMap = new HashMap<String, HierarchyTree>();
        HierarchyTree root = factory.getRoot();
        root.getNodeMap(factory.getLowestRank(), nodeMap);
        
        for ( int i= 0; i < seqList.size(); i++){
            LineageSequence seqx = seqList.get(i);
            HierarchyTree treex = nodeMap.get((String) seqx.getAncestors().get(seqx.getAncestors().size() - 1));
            for ( int j = i+1; j < seqList.size(); j++){
                LineageSequence seqy = seqList.get(j);
                HierarchyTree treey = nodeMap.get((String) seqy.getAncestors().get(seqy.getAncestors().size() - 1));
                
                Taxonomy lowestCommonAnc = findLowestCommonAncestor(treex, treey);               
                PairwiseAlignment result = PairwiseAligner.align(seqx.getSeqString().replaceAll("U", "T"), seqy.getSeqString().replaceAll("U", "T"), scoringMatrix, mode);
                double distance = dist.getDistance(result.getAlignedSeqj().getBytes(), result.getAlignedSeqi().getBytes(), 0);

                if ( show_alignment){
                    System.out.println(">\t" + seqx.getSeqName() + "\t" + seqy.getSeqName() + "\t" + String.format("%.3f", distance) + "\t" + lowestCommonAnc.getHierLevel());
                    System.out.println(result.getAlignedSeqi() + "\n");
                    System.out.println(result.getAlignedSeqj() + "\n");
                }
        
                ArrayList<Double> distList = distanceMap.get(lowestCommonAnc);
                if ( distList == null){
                    distList = new ArrayList<Double>();
                    distList.add(distance);
                    distanceMap.put(lowestCommonAnc, distList);
                }else {
                    distList.add(distance);
                }
            }
        }        
    }

    public void printSummary( PrintStream outStream ){
        HashMap<String, ArrayList<Double>> rankDistanceMap = new HashMap<String, ArrayList<Double>>();
        outStream.println("\nrank\ttaxonname\ttotalcount\tmean_distance\tstdev");

        for ( Taxonomy taxon: distanceMap.keySet()){
            StdevCal.Std result = StdevCal.calStd(distanceMap.get(taxon));
            outStream.println(taxon.getHierLevel() + "\t" + taxon.getName() + "\t" + result.getTotalCount() + "\t" + String.format("%.3f",result.getMean()) + "\t" + String.format("%.3f",result.getStdev()));
            ArrayList<Double> distList = rankDistanceMap.get(taxon.getHierLevel());
            if ( distList == null){
                distList = new ArrayList<Double>();
                distList.addAll(distanceMap.get(taxon));
                rankDistanceMap.put(taxon.getHierLevel(), distList);
            }else {
                distList.addAll(distanceMap.get(taxon));
            }
        }        
        
        outStream.println("\nrank\ttotalcount\tmean_distance\tstdev");
        for (String rank: rankDistanceMap.keySet()){
            StdevCal.Std result = StdevCal.calStd(rankDistanceMap.get(rank));
            outStream.println(rank + "\t" + result.getTotalCount() + "\t" + String.format("%.3f", result.getMean()) + "\t" + String.format("%.3f", result.getStdev()));

        }
        outStream.close();
    }
        
    private static Taxonomy findLowestCommonAncestor(HierarchyTree treex, HierarchyTree treey){
        ArrayList<HierarchyTree> ancestorx = new ArrayList<HierarchyTree>();
        ArrayList<HierarchyTree> ancestory = new ArrayList<HierarchyTree>();
        HierarchyTree parent = treex.getParent();
        ancestorx.add(treex);
        while(parent != null) {
            ancestorx.add(parent);
            parent = parent.getParent();
        }
        ancestory.add(treey);
        parent = treey.getParent();
        while(parent != null) {
            ancestory.add(parent);
            parent = parent.getParent();
        }
        
        Taxonomy lowestCommonAnc = ancestorx.get(ancestorx.size() -1).getTaxonomy();
        for ( int i = 2; i <= ancestorx.size() ; i++){
            if (  (ancestory.size() -i ) >= 0 ){
                if ( ancestorx.get( ancestorx.size() -i).getTaxonomy().equals(ancestory.get( ancestory.size() -i).getTaxonomy()) ){
                    lowestCommonAnc = ancestorx.get( ancestorx.size() -i).getTaxonomy();
                }
            }else {
               break; 
            }
        }
        return lowestCommonAnc;
    }
    
    
     /**
     * This program does the pairwise alignment between each pair of sequences, 
     * reports a summary of the average distances and the stdev at each rank.
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
       
        String trainseqFile = null;
        String taxFile = null;
        PrintStream outStream = null;
        AlignmentMode mode = AlignmentMode.overlap;
        boolean show_alignment = false;
       
         try {
            CommandLine line = new PosixParser().parse(options, args);

            if (line.hasOption("show_alignment")) {
                show_alignment = true;
            }
            if (line.hasOption("alignment-mode")) {
                String m = line.getOptionValue("alignment-mode").toLowerCase();
                mode = AlignmentMode.valueOf(m);
              
            }
            
            if ( args.length != 3){
                throw new Exception("wrong arguments");
            }
            args = line.getArgs();            
            trainseqFile = args[0];
            taxFile = args[1];
            outStream = new PrintStream(new File(args[2]));
         }catch (Exception e) {
            System.err.println("Command Error: " + e.getMessage());
            new HelpFormatter().printHelp(80, " [options] trainseqFile taxonFile outFile", "", options, "");
            return;
        }
                        
        PairwiseSeqDistance theObj = new PairwiseSeqDistance(trainseqFile,taxFile, mode, show_alignment);
        
        theObj.printSummary(outStream);
   }
}
