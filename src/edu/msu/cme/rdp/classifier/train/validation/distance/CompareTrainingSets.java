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

import edu.msu.cme.rdp.classifier.train.LineageSequence;
import edu.msu.cme.rdp.classifier.train.LineageSequenceParser;
import edu.msu.cme.rdp.classifier.train.validation.HierarchyTree;
import edu.msu.cme.rdp.classifier.train.validation.TreeFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author wangqion
 */
public class CompareTrainingSets {
    private ArrayList<HierarchyTreeExtend> trainsets = new ArrayList<HierarchyTreeExtend>();
    private ArrayList<String> ranks = new ArrayList<String>();  // an  order list of ranks
    
    public class HierarchyTreeExtend {
        private HierarchyTree root;
        private String trainsetName;
        private HashMap<String, String> seqMap ; // seqID, desc
        
        public HierarchyTreeExtend(HierarchyTree root, String name){
            this.root = root;
            trainsetName = name;
        }
        
        public HierarchyTree getRoot(){
            return root;
        }
        
        public String getTrainsetName(){
            return trainsetName;
        }
    }
    
    public CompareTrainingSets(String rankFile, String[] files) throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(new File(rankFile)));
        String line;
        while ( (line = reader.readLine()) != null){
            ranks.add(line.trim());
        }
        reader.close();
        
        for ( int i = 0; i < files.length; i+=2 ){
            HierarchyTreeExtend trainset = parseOneTraining(files[i], files[i+1], i/2, "", "");
            
            trainsets.add(trainset);
        }
        
    }
    
    private HierarchyTreeExtend parseOneTraining(String taxFile, String seqFile, int trainset_no, String version, String modification) throws IOException{
        File temp = new File(taxFile);
        int index = temp.getName().indexOf(".");
        String trainsetName = temp.getName();
        if ( index != -1){
            trainsetName = trainsetName.substring(0, index);
        }
        // need to use ISO encoding for UNITE
        FileReader tax  = new FileReader(new File(taxFile));
        TreeFactory factory = new TreeFactory(tax);
               
        LineageSequenceParser parser = new LineageSequenceParser(new File(seqFile));
        LineageSequence seq;
        HashMap<String, String> seqMap = new HashMap<String, String>(); // seqID, desc
        while ( parser.hasNext()){
            seq = parser.next();
            factory.addSequence(seq, false); // donot check the kmers
            
            if ( seq.getSeqName().contains("|S00") ){ // rdpID
                String[] values = seq.getSeqName().split("\\|");
                seqMap.put(values[0], seq.getDesc());   
            }else if (seq.getSeqName().contains("|SH") ){  // if it's seq from UNITE, we need to do something with the seqID             
                String[] values = seq.getSeqName().split("\\|");
                seqMap.put(values[1], seq.getDesc()); 
            }else {
                seqMap.put(seq.getSeqName(), seq.getDesc()); 
            }
            
        }
        parser.close();
        HierarchyTreeExtend retVal = new HierarchyTreeExtend(factory.getRoot(), trainsetName);
        retVal.seqMap = seqMap;
        return retVal;
        
    }
   
    
    public void compare(String summaryOutFile, String detailOutFile) throws IOException{
        PrintStream outStream = new PrintStream(summaryOutFile);
        PrintStream detailOutStream = new PrintStream(detailOutFile);
        outStream.println("## data for Taxonomic Composition");
        outStream.print("Rank"  );
        for ( HierarchyTreeExtend factory: this.trainsets ){
            outStream.print("\t" + factory.trainsetName);
        }
        outStream.println();
        for ( int i = 0; i < ranks.size(); i++){
            outStream.print(ranks.get(i));
            for ( int t = 0; t < this.trainsets.size(); t++){                   
                HierarchyTree root = trainsets.get(t).getRoot();
                HashMap<String, HierarchyTree> nodeMap = new HashMap<String, HierarchyTree>();
                root.getNodeMap(ranks.get(i), nodeMap);
                outStream.print("\t" + nodeMap.size());
            }
            outStream.println();
        }
        outStream.print("All Seqs");
        for ( HierarchyTreeExtend factory: this.trainsets ){
            HierarchyTree root = factory.getRoot();
            outStream.print("\t" + root.getTotalSeqs());
        }
        outStream.println();
        
        // data for Venn Diagram if less than 3 sets       
        if ( this.trainsets.size() == 2 || this.trainsets.size() == 3){
            outStream.println("\n## data for Venn Diagram");
            for ( int i = 0; i < ranks.size(); i++){
                outStream.println("\n## Rank " + ranks.get(i));
                ArrayList<Set<String>> taxaList = new ArrayList<Set<String>>();
                for ( int t = 0; t < this.trainsets.size(); t++){                   
                    HierarchyTree root = trainsets.get(t).getRoot();
                    HashMap<String, HierarchyTree> nodeMap = new HashMap<String, HierarchyTree>();
                    root.getNodeMap(ranks.get(i), nodeMap);
                    taxaList.add(nodeMap.keySet());
                }
                
                Set<String> tempSet = new HashSet<String>();
                tempSet.addAll(taxaList.get(0));
                for ( int t = 1; t < taxaList.size(); t++){
                    tempSet.retainAll(taxaList.get(t));
                }
                outStream.println("Shared by all:\t" + tempSet.size());
                
                for ( int t = 0; t < taxaList.size(); t++){
                    tempSet.clear();
                    tempSet.addAll(taxaList.get(t));                    
                    for ( int k = 1; k < taxaList.size(); k++){
                        tempSet.removeAll(taxaList.get( (k+t)%(taxaList.size())));                        
                    }
                    outStream.println("Unique to " + trainsets.get(t).getTrainsetName()+ ":\t" + tempSet.size());
                    print(detailOutStream, ranks.get(i) + " unique to " + trainsets.get(t).getTrainsetName(), tempSet);
                }
                
                for ( int t = 0; t < taxaList.size(); t++){
                    tempSet.clear();
                    tempSet.addAll(taxaList.get(t));                    
                    tempSet.retainAll(taxaList.get( (t+1)%(taxaList.size()) ));
                    
                    if ( taxaList.size() == 3){
                        tempSet.removeAll(taxaList.get( (t+2)%(taxaList.size()) ));                        
                    }
                    outStream.println("Shared only by " + trainsets.get(t).getTrainsetName()+ " and " + trainsets.get((t+1)%(taxaList.size())).getTrainsetName()+ ":\t"+ tempSet.size());
                }
                
            }
            
            // shared seqs
            outStream.println("\n## Shared Sequences (by seqID)" );
            Set<String> tempSet = new HashSet<String>();
            tempSet.addAll(trainsets.get(0).seqMap.keySet());
            for ( int t = 1; t < trainsets.size(); t++){
                tempSet.retainAll(trainsets.get(t).seqMap.keySet());
            }
            outStream.println("Shared seqs by all:\t" + tempSet.size());

            for ( int t = 0; t < trainsets.size(); t++){
                tempSet.clear();
                tempSet.addAll(trainsets.get(t).seqMap.keySet());
                for ( int k = 1; k < trainsets.size(); k++){
                    tempSet.removeAll(trainsets.get( (t+k)%(trainsets.size())).seqMap.keySet());
                }
                outStream.println("Unique to " + trainsets.get(t).getTrainsetName()+ ":\t" + tempSet.size());
                printSeqs(detailOutStream, "Unique seqs to " + trainsets.get(t).getTrainsetName(), tempSet,trainsets.get(t));
            }

            for ( int t = 0; t < trainsets.size(); t++){
                tempSet.clear();
                tempSet.addAll(trainsets.get(t).seqMap.keySet());
                tempSet.retainAll(trainsets.get( (t+1)%(trainsets.size()) ).seqMap.keySet());
                if ( trainsets.size() == 3){
                    tempSet.removeAll(trainsets.get( (t+2)%(trainsets.size()) ).seqMap.keySet());
                }
                outStream.println("Shared only by " + trainsets.get(t).getTrainsetName()+ " and " + trainsets.get((t+1)%(trainsets.size())).getTrainsetName()+ ":\t"+ tempSet.size());
            }
        }
        
        outStream.close();
        detailOutStream.close();
    }
    
    private void print(PrintStream out, String message, Set<String> tempSet){
        out.println("##" + message);
        for ( String s: tempSet){
            out.println(s);
        }
        out.println();
    }
    
    private void printSeqs(PrintStream out, String message, Set<String> tempSet, HierarchyTreeExtend trainset){
        out.println("##" + message);
        for ( String s: tempSet){
            out.println(s + "\t" + trainset.seqMap.get(s));
        }
        out.println();
    }
    
    public static void main(String[] args) throws Exception{
        String usage = "Usage: rank.txt summary_out.txt detail_out.txt set1_taxon.txt set1_seq.fasta set2_taxon.txt set2_seq.fasta ...\n" + 
                "rank.txt contains an ordered list of ranks to be compared, from the highest rank down to lowest rank. one per line\n" +
                "Each input training set requires a taxonomy file and a sequences file with lineage information as the description\n" +
                "  This program compares multiple training sets and generates the taxonomic composition data at each rank.\n" +
                "  For two or three sets, it produces an summary output data suitable to generate Venn diagrams\n" +
                "  and a detailed output contained the detailed list of taxa or sequences unique to each training set";
        if (args.length < 5 || args.length %2 != 1){
            System.err.println(usage);
            System.exit(1);
        }
        
        CompareTrainingSets theObj = new CompareTrainingSets(args[0], Arrays.copyOfRange(args, 3, args.length));
        theObj.compare(args[1], args[2]);
    }
    
}
