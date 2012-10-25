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

import edu.msu.cme.rdp.classifier.train.LineageSequence;
import edu.msu.cme.rdp.classifier.train.LineageSequenceParser;
import edu.msu.cme.rdp.classifier.train.validation.HierarchyTree;
import edu.msu.cme.rdp.classifier.train.validation.Taxonomy;
import edu.msu.cme.rdp.classifier.train.validation.TreeFactory;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author wangqion
 */
public class RdmSelectTaxon {

    /**
     * random select (without replacement) a fraction of sequences from the input file
     * @param infile
     * @param fraction
     * @return 
     * @throws IOException
     */
    public static Set<String> randomSelectSeq(File infile, float fraction) throws IOException{
        Set<String> selectedSeqIDs = new HashSet<String>();
        ArrayList<String> allIDs = new ArrayList<String>();

        LineageSequenceParser parser = new LineageSequenceParser(infile);

        while ( parser.hasNext() ){
            LineageSequence pSeq = parser.next();
            allIDs.add(pSeq.getSeqName());
        }
        parser.close();

        int testCount = (int) (((float) allIDs.size()) * fraction);
        while (selectedSeqIDs.size() < testCount){
            int rdmIndex = (int) (Math.floor(Math.random()* (double)allIDs.size()));

            selectedSeqIDs.add(allIDs.get(rdmIndex));
            allIDs.remove(rdmIndex);
        }

        return selectedSeqIDs;

    }
    
    /**
     * Random select taxa at given rank, and return all the sequence IDs assigned to the selected taxa.
     * @param tax_file
     * @param source_file
     * @param fraction
     * @param rank
     * @return
     * @throws IOException
     */
    public static Set<String> randomSelectTaxon(File tax_file, File source_file, float fraction, String rank) throws IOException{
        TreeFactory factory = new TreeFactory(new FileReader(tax_file));
        LineageSequenceParser parser = new LineageSequenceParser(source_file);
        HashMap<String, HashSet> genusTrainSeqMap = new HashMap<String, HashSet>(); // keep the seqID for each genus

        while ( parser.hasNext() ){
            LineageSequence pSeq = parser.next();
            HierarchyTree genusNode = factory.addSequence( pSeq);
            HashSet<String> genusSeqSet = genusTrainSeqMap.get(genusNode.getName());
            if ( genusSeqSet == null){
                genusSeqSet = new HashSet<String>();
                genusTrainSeqMap.put(genusNode.getName(), genusSeqSet);
            }
            genusSeqSet.add(pSeq.getSeqName());
        }
        parser.close();


        // random select nodes at the give rank level
        ArrayList<HierarchyTree> nodeList = new ArrayList<HierarchyTree>();
        factory.getRoot().getNodeList(rank, nodeList);

        //xxx print the size of the nodes
        for ( HierarchyTree node: nodeList){
           // System.err.println(node.getName() + "\t" + node.getNumOfLeaves());
        }

        Set<HierarchyTree> selectedNodes = new HashSet<HierarchyTree>();

        int testCount = (int) (((float) nodeList.size()) * fraction);
        while (selectedNodes.size() < testCount){
            int rdmIndex = (int) (Math.floor(Math.random()* (double)nodeList.size()));
            selectedNodes.add(nodeList.get(rdmIndex));
            System.err.println("selected " + nodeList.get( rdmIndex).getName() + "\t" + nodeList.get(rdmIndex).getNumOfLeaves());
            nodeList.remove(rdmIndex);
        }

        // select the seqIDs
        Set<String> selectedSeqIDs = new HashSet<String>();

        for (HierarchyTree node : selectedNodes){
            ArrayList<HierarchyTree> tmp = new ArrayList<HierarchyTree>();
            node.getNodeList(Taxonomy.GENUS, tmp);
            for ( HierarchyTree genusNode: tmp){
                selectedSeqIDs.addAll(genusTrainSeqMap.get(genusNode.getName()));
            }
        }
        return selectedSeqIDs;

    }

     public static void main(String[] args) throws IOException {
        String Usage = "tax_file, source_file, fraction, rank";
        File tax_file = new File(args[0]);
        File source_file = new File(args[1]);

        RdmSelectTaxon.randomSelectTaxon(tax_file, source_file, Float.parseFloat(args[2]), args[3]);

    }
}
