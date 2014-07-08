/*
 * Copyright (C) 2014 wangqion
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
import edu.msu.cme.rdp.classifier.train.validation.TreeFactory;
import edu.msu.cme.rdp.readseq.utils.kmermatch.KmerMatchCore;
import edu.msu.cme.rdp.readseq.utils.kmermatch.NuclSeqMatch;
import edu.msu.cme.rdp.readseq.utils.orientation.GoodWordIterator;
import java.awt.BasicStroke;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author wangqion
 */
public class TaxaSimilarityMain {
    
    public static String[] RANKS = { "norank", "domain", "phylum", "class", "order", "family", "genus"};
    private ArrayList<Short> withinLowestRankSabSet = new ArrayList<Short>();
    private ArrayList<Short> diffLowestRankSabSet = new ArrayList<Short>();
    private List<String> ranks = new ArrayList<String>();
    private DecimalFormat format = new DecimalFormat("#.###");
    private HashMap<String, long[]> sabCoutMap = new HashMap<String, long[]>();  // key = rank, value, count of the sab scores
    private final int BINSIZE = 101;
    private ScoringMatrix scoringMatrix = ScoringMatrix.getDefaultNuclMatrix();
    private AlignmentMode mode = AlignmentMode.overlap_trim;
    private static DistanceModel dist = new IdentityDistanceModel(true);

    
    public TaxaSimilarityMain( List<String> selectedRanks){
        for ( String r: selectedRanks){
            this.ranks.add(r.toLowerCase());
        }
        for ( String rank: ranks){
            sabCoutMap.put(rank.toLowerCase(), new long[BINSIZE]);
        }
        
    }
    
    public static List<String> readRanks(String rankFile) throws IOException {
        List<String> ranks = new ArrayList();
        BufferedReader reader = new BufferedReader(new FileReader(new File(rankFile)));
        String line = null;
        while ( (line=reader.readLine()) != null){
            ranks.add(line.trim());
        }
        return ranks;
    }
    
    public HashMap<String,HierarchyTree> getAncestorNodes(HierarchyTree root, String seqName, List<String> ancestors){
        HashMap<String,HierarchyTree> ancestorNodes = new HashMap<String,HierarchyTree>();
        if ( !ancestors.get(0).equals(root.getName())){
            throw new IllegalArgumentException("Sequence " + seqName + " does not have the same root taxon" + root.getName());
        }
        ancestorNodes.put(root.getTaxonomy().getHierLevel(), root);
        HierarchyTree curParent = root;
        for (int i = 1; i < ancestors.size(); i++){
            
            HierarchyTree  node = curParent.getSubclassbyName(ancestors.get(i));
            if ( node == null){
                throw new IllegalArgumentException("Sequence " + seqName + " cannot find ancestor node: " + ancestors.get(i));
            }
            ancestorNodes.put(node.getTaxonomy().getHierLevel().toLowerCase(), node);
            curParent = node;
        }        
        return ancestorNodes;
    }
    
    
    
    public void calSabSimilarity(String taxonFile, String trainSeqFile, String testSeqFile) throws IOException{        
        TreeFactory factory = new TreeFactory(new FileReader(taxonFile));
        factory.buildTree();
        // get the lineage of the trainSeqFile  
        LineageSequenceParser trainParser = new LineageSequenceParser(new File(trainSeqFile));
        HashMap<String, List<String>> lineageMap = new HashMap<String, List<String>>();
        while (trainParser.hasNext()) {
            LineageSequence seq = (LineageSequence) trainParser.next();
            lineageMap.put(seq.getSeqName(), seq.getAncestors());
            
         }
        trainParser.close();
        NuclSeqMatch sabCal = new NuclSeqMatch(trainSeqFile);
        LineageSequenceParser parser = new LineageSequenceParser(new File(testSeqFile));

        int count = 0;
        while (parser.hasNext()) {
            LineageSequence seq = (LineageSequence) parser.next();
            HashMap<String,HierarchyTree> queryAncestorNodes = getAncestorNodes(factory.getRoot(), seq.getSeqName(), seq.getAncestors());
           TreeSet<KmerMatchCore.BestMatch> matchResults = sabCal.findAllMatches(seq);
            
            short withinLowestRankSab = -1;
            short diffLowestRankSab = -1;  
            String bestDiffLowestRankMatch = null;
            for (KmerMatchCore.BestMatch match: matchResults){
                if ( match.getBestMatch().getSeqName().equals(seq.getSeqName())) continue;
                short sab = (short)(Math.round(100*match.getSab()));
                HashMap<String,HierarchyTree> matchAncestorNodes = getAncestorNodes(factory.getRoot(), match.getBestMatch().getSeqName(), lineageMap.get(match.getBestMatch().getSeqName()));
                boolean withinTaxon = false; 
                for (int i = ranks.size() -1; i >=0; i--){                    
                    HierarchyTree queryTaxon = queryAncestorNodes.get( ranks.get(i));
                    HierarchyTree matchTaxon = matchAncestorNodes.get( ranks.get(i));
                    if ( queryTaxon != null && matchTaxon != null){
                        if ( queryTaxon.getName().equals(matchTaxon.getName())){
                            if ( !withinTaxon){  // if the query and match are not in the same child taxon, add sab to the current taxon
                                (sabCoutMap.get(ranks.get(i)))[sab]++; 
                            }
                            withinTaxon = true;                            
                        }else {
                            withinTaxon = false;
                        }
                    }
                    
                }  
                
                // find within or different lowest level rank sab score, be either species or genus or any rank
                HierarchyTree speciesQueryTaxon = queryAncestorNodes.get( ranks.get(ranks.size()-1));    
                HierarchyTree speciesMatchTaxon = matchAncestorNodes.get( ranks.get(ranks.size()-1)); 
                
                if ( speciesQueryTaxon != null && speciesMatchTaxon != null && speciesQueryTaxon.getName().equals(speciesMatchTaxon.getName())){
                    withinLowestRankSab = sab >= withinLowestRankSab ? sab: withinLowestRankSab;
                }else {
                    
                    if ( sab >= diffLowestRankSab ){
                        bestDiffLowestRankMatch = match.getBestMatch().getSeqName();
                        diffLowestRankSab = sab;
                    }
                }
            }
            if ( withinLowestRankSab > 0){
                withinLowestRankSabSet.add(withinLowestRankSab);
            }
            if ( diffLowestRankSab > 0 ){
                diffLowestRankSabSet.add(diffLowestRankSab);
            }
            //System.out.println(seq.getSeqName() + "\t" + withinLowestRankSab + "\t" + diffLowestRankSab );
            count++;
            if ( count % 100 == 0){
                System.out.println(count);
            }
        }
        parser.close();
    
    }
    
    public void calPairwiseSimilaritye(String taxonFile, String trainSeqFile, String testSeqFile) throws IOException, OverlapCheckFailedException{        
        TreeFactory factory = new TreeFactory(new FileReader(taxonFile));
        factory.buildTree();
        // get the lineage of the trainSeqFile  
        LineageSequenceParser trainParser = new LineageSequenceParser(new File(trainSeqFile));
        ArrayList<LineageSequence> trainSeqList = new ArrayList<LineageSequence>();
        while (trainParser.hasNext()) {
            LineageSequence seq = (LineageSequence) trainParser.next();
            trainSeqList.add(seq);
         }
        trainParser.close();
        LineageSequenceParser parser = new LineageSequenceParser(new File(testSeqFile));

        while (parser.hasNext()) {
            LineageSequence seq = (LineageSequence) parser.next();
            HashMap<String,HierarchyTree> queryAncestorNodes = getAncestorNodes(factory.getRoot(), seq.getSeqName(), seq.getAncestors());
            
            for (LineageSequence trainSeq: trainSeqList){
                if ( trainSeq.getSeqName().equals(seq.getSeqName())) continue;
                
                HashMap<String,HierarchyTree> matchAncestorNodes = getAncestorNodes(factory.getRoot(), trainSeq.getSeqName(), trainSeq.getAncestors());
                boolean withinTaxon = false;
                String lowestCommonRank = null;
                for (int i = ranks.size() -1; i >=0; i--){                    
                    HierarchyTree queryTaxon = queryAncestorNodes.get( ranks.get(i));
                    HierarchyTree matchTaxon = matchAncestorNodes.get( ranks.get(i));
                    if ( queryTaxon != null && matchTaxon != null){
                        if ( queryTaxon.getName().equals(matchTaxon.getName())){
                            if ( !withinTaxon){  // if the query and match are not in the same child taxon, add sab to the current taxon
                                lowestCommonRank = ranks.get(i);
                                //(sabCoutMap.get(ranks.get(i)))[sab]++; 
                            }
                            withinTaxon = true;                            
                        }else {
                            withinTaxon = false;
                        }
                    }
                }  
                
                if ( lowestCommonRank == null){  // not the rank we care
                    continue;
                }

                // we need to use overlap_trim mode and calculate distance as metric to count insertions, deletions and mismatches.
                PairwiseAlignment result = PairwiseAligner.align(seq.getSeqString().replaceAll("U", "T"), trainSeq.getSeqString().replaceAll("U", "T"), scoringMatrix, mode);
                short sab = (short) (100 - 100*dist.getDistance(result.getAlignedSeqj().getBytes(), result.getAlignedSeqi().getBytes(), 0));
                sabCoutMap.get(lowestCommonRank)[sab]++; 
                                
            }           
        }
        parser.close();
    
    }
    
    public void createPlot(String plotTitle, File outdir) throws IOException{
        XYSeriesCollection dataset = new XYSeriesCollection();
        DefaultBoxAndWhiskerCategoryDataset scatterDataset = new DefaultBoxAndWhiskerCategoryDataset();

        PrintStream boxchart_dataStream = new PrintStream(new File(outdir, plotTitle + ".boxchart.txt"));
        
        boxchart_dataStream.println("#\tkmer" + "\trank" + "\t" + "max" + "\t" + "avg" + "\t" + "min" + 
                "\t" + "Q1" + "\t" + "median" + "\t" + "Q3" + "\t" + "98Pct" + "\t" + "2Pct" + "\t" + "comparisons" + "\t" + "sum");
        for ( int i = 0; i < ranks.size(); i++){
            long[] countArray = sabCoutMap.get(ranks.get(i));
            if ( countArray == null) continue;
            
            double sum = 0.0;
            int max = 0;
            int min = 100;
            double mean = 0;
            int Q1 = -1;
            int median = -1;
            int Q3 = -1;
            int pct_98 =-1;
            int pct_2 = -1;
            long comparisons = 0;
            int minOutlier = 0;  // we don't care about the outliers
            int maxOutlier = 0;  //
            
            XYSeries series = new XYSeries(ranks.get(i));
            
            for ( int c = 0; c< countArray.length; c++){
                if ( countArray[c] == 0 ) continue;
                comparisons += countArray[c];
                sum += countArray[c] * c;
                if ( c < min){
                    min = c;
                }
                if ( c > max){
                    max = c;
                }
            }
            
            // create series
            double cum = 0;
            for ( int c = 0; c< countArray.length; c++){
                if ( countArray[c] == 0 ) continue;
                cum += countArray[c];
                int pct = (int) Math.floor(100*cum/comparisons);
                series.add(c, pct);
                
                if ( pct_2 == -1 && pct >=5){
                    pct_2 = c;
                }
                if ( Q3 == -1 && pct >=25){
                    Q3 = c;
                } 
                if ( median == -1 && pct >=50){
                    median = c;
                } 
                if ( Q1 == -1 && pct >=75){
                    Q1 = c;
                }
                if ( pct_98 == -1 && pct >=98){
                    pct_98 = c;
                }
            }
            if ( !series.isEmpty()) {
                dataset.addSeries(series);
                
                BoxAndWhiskerItem item = new BoxAndWhiskerItem(sum/comparisons, median, Q1, Q3, pct_2, pct_98,  minOutlier,  maxOutlier, new ArrayList());
                scatterDataset.add(item, ranks.get(i), "");
                
                boxchart_dataStream.println("#\t" + GoodWordIterator.getWordsize() + "\t" + ranks.get(i) + "\t" + max + "\t" + format.format(sum/comparisons) + "\t" + min + 
                "\t" + Q1 + "\t" + median + "\t" + Q3 + "\t" + pct_98 + "\t" + pct_2 + "\t" + comparisons + "\t" + sum);
            }
        }  
        boxchart_dataStream.close();       
        Font lableFont = new Font("Helvetica", Font.BOLD, 28);
        
        JFreeChart chart = ChartFactory.createXYLineChart(plotTitle, "Similarity%", "Percent Comparisions",  dataset,  PlotOrientation.VERTICAL, true, true, false  );
        ((XYPlot) chart.getPlot()).getRenderer().setStroke( new BasicStroke( 2.0f ));
        chart.getLegend().setItemFont(new Font("Helvetica", Font.BOLD, 24));
        chart.getTitle().setFont(lableFont);
        ((XYPlot) chart.getPlot()).getDomainAxis().setLabelFont(lableFont);
        ((XYPlot) chart.getPlot()).getDomainAxis().setTickLabelFont(lableFont);
        ValueAxis rangeAxis = ((XYPlot) chart.getPlot()).getRangeAxis();
        rangeAxis.setRange(0,100);
        rangeAxis.setTickLabelFont(lableFont);
        rangeAxis.setLabelFont(lableFont);
        ((NumberAxis)rangeAxis).setTickUnit(new NumberTickUnit(5));
        ChartUtilities.writeScaledChartAsPNG(new PrintStream(new File(outdir, plotTitle + ".linechart.png")), chart, 800, 1000, 3, 3);

        BoxPlotUtils.createBoxplot(scatterDataset, new PrintStream(new File(outdir, plotTitle + ".boxchart.png")), plotTitle, "Rank", "Similarity%", lableFont);

    }
    
  
    
        
    /**
     * This calculates the average similarity (Sab score or pairwise alignment) between taxa at given ranks and plot the box and whisker plot and accumulation curve. 
     * The distances associate to a given rank contains the distances between different child taxa. It does not include the distances within the same child taxa.
     * For example, if a query and it's closest match are from the same genus, the distance value is added to that genus.
     * If there are from different genera but the same family, the distance value is added to that family, etc.
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException, OverlapCheckFailedException{
        String usage = "Usage: taxonfile trainset.fasta query.fasta outdir kmersize rankFile sab|pw \n" + 
                "  This program calculates the average similarity (Sab score, or pairwise alignment) within taxa\n" + 
                "  and plot the box and whisker plot and accumulation curve plot. \n" + 
                "  rankFile: a file contains a list of ranks to be calculated and plotted. One rank per line, no particular order required. \n" + 
                "  Note pw is extremely slower, recommended only for lower ranks such as species, genus and family. ";
        
        
        if ( args.length != 7 ){
            System.err.println(usage);
            System.exit(1);
        }
        List<String> ranks = readRanks(args[5]);       
        File outdir = new File(args[3]);
        if ( !outdir.isDirectory()){
            System.err.println("outdir must be a directory");
            System.exit(1);
        }
        int kmer = Integer.parseInt(args[4]);
        GoodWordIterator.setWordSize(kmer);        
        TaxaSimilarityMain theObj = new TaxaSimilarityMain(ranks);

        String plotTitle = new File(args[2]).getName();
        int index = plotTitle.indexOf(".");
        if ( index != -1){
            plotTitle = plotTitle.substring(0, index);          
        }
        if ( args[6].equalsIgnoreCase("sab")){
            theObj.calSabSimilarity(args[0], args[1], args[2]);
        }else {
            theObj.calPairwiseSimilaritye(args[0], args[1], args[2]);
        }
        
        theObj.createPlot(plotTitle, outdir);
        
    }
    
}
