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

import edu.msu.cme.rdp.classifier.train.validation.StatusCount;
import edu.msu.cme.rdp.classifier.train.validation.ValidationClassificationResult;
import edu.msu.cme.rdp.classifier.train.validation.ValidClassificationResultFacade;
import edu.msu.cme.rdp.classifier.train.validation.DecisionMaker;
import edu.msu.cme.rdp.classifier.train.GoodWordIterator;
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
import java.util.List;
import java.util.Set;

/**
 *
 * @author wangqion
 */
  public class CrossValidate {

    
    /**
     * The method randomly selects a fraction of sequences from the source file as test set, 
     * used the remaining sequences from the source file as training set.
     * @param tax_file
     * @param source_file
     * @param selectedTestSeqIDs
     * @throws IOException
     */
    public ArrayList<HashMap> runTest(File tax_file, File source_file,
            String rdmSelectedRank, float fraction, Integer partialLength, boolean useSeed, int min_bootstrap_words) throws IOException{
        Set<String> selectedTestSeqIDs = null;
        if ( rdmSelectedRank == null){
            selectedTestSeqIDs = RdmSelectTaxon.randomSelectSeq(source_file, fraction);
        }else {
            selectedTestSeqIDs = RdmSelectTaxon.randomSelectTaxon(tax_file, source_file, fraction, rdmSelectedRank);
        }
        
        TreeFactory factory = setup(tax_file, source_file, selectedTestSeqIDs );
        DecisionMaker dm = new DecisionMaker(factory);
         // get all the genus node list
        HashMap<String, HierarchyTree> genusNodeMap = new HashMap<String, HierarchyTree>();
        factory.getRoot().getNodeMap(Taxonomy.GENUS, genusNodeMap);
        if (genusNodeMap.isEmpty()) {
          throw new IllegalArgumentException("\nThere is no node in GENUS level!");
        }
        
        HashMap<String,HashSet> rankNodeMap = new HashMap<String,HashSet>();
        for (String rank: factory.getRankSet()){
            ArrayList<HierarchyTree> nodeList = new ArrayList<HierarchyTree>();
            factory.getRoot().getNodeList(rank, nodeList);
            HashSet<String> nodeNameSet = getnodeNameSet(nodeList);
            rankNodeMap.put(rank, nodeNameSet);
        }

        ArrayList<HashMap> statusCountList = new ArrayList<HashMap>();
        // initialize a list of statusCount, one for each bootstrap from 0 to 100
        for ( int b = 0; b <= 100; b++){
             HashMap<String, StatusCount> statusCountMap = new HashMap<String, StatusCount>();
             statusCountList.add(statusCountMap);
             for (String rank: factory.getRankSet()){
                 statusCountMap.put(rank, new StatusCount());
             }
         }

        int i = 0;
        LineageSequenceParser parser = new LineageSequenceParser(source_file);
        while ( parser.hasNext()){
          LineageSequence pSeq = parser.next();
          if ( !selectedTestSeqIDs.contains(pSeq.getSeqName()) || pSeq.getSeqString().length() == 0){
              continue;
          }
          GoodWordIterator wordIterator = null ;
          if ( partialLength != null ){
                wordIterator = pSeq.getPartialSeqIteratorbyGoodBases(partialLength.intValue());  // test partial sequences with good words only

          }else {
                wordIterator = new GoodWordIterator(pSeq.getSeqString()); // full sequence
          }

          if (wordIterator == null || wordIterator.getNumofWords() == 0){
            //System.err.println(pSeq.getSeqName() + " unable to find good sequence");
            continue;
          }
        
          List result = dm.getBestClasspath( wordIterator, genusNodeMap, useSeed, min_bootstrap_words);

          //xxx
          ValidClassificationResultFacade resultFacade = new ValidClassificationResultFacade(pSeq, result);

          compareClassificationResult(factory, resultFacade, rankNodeMap, statusCountList);

          //System.out.print(i +" ");
          i++;
        }
        parser.close();

        calErrorRate(statusCountList);
        return statusCountList;

    }

    /**
     * use the sequences not in the test set as training set
     * @param tax_file
     * @param source_file
     * @param selectedTestSeqIDs
     * @return
     * @throws IOException
     */
    private TreeFactory setup(File tax_file, File source_file, Set<String> selectedTestSeqIDs) throws IOException {

        TreeFactory factory = new TreeFactory(new FileReader(tax_file));
        LineageSequenceParser parser = new LineageSequenceParser(source_file);

        while ( parser.hasNext() ){
            LineageSequence pSeq = parser.next();
            if ( !selectedTestSeqIDs.contains(pSeq.getSeqName())){
              factory.addSequence( pSeq);
              //System.err.println(">Training " + pSeq.getName()+ "\t" + pSeq.getLineage());
            }
        }
        parser.close();

        //after all the training set is being parsed, calculate the prior probability for all the words.
        factory.calculateWordPrior();
        return factory;
      }
    
    private HashSet<String> getnodeNameSet(ArrayList<HierarchyTree> genusNodeList){
        HashSet<String> nodeNameSet = new HashSet<String>();
        for (HierarchyTree t: genusNodeList ){
            nodeNameSet.add(t.getName());
        }
        return nodeNameSet;
    }

    /** If we only care about if the genus in the training set or not, there are four status
     * TP: bootstrap above cutoff, labeled taxon in training set
     * FN: bootstrap below cutoff, labeled taxon in training set
     * FP: bootstrap above cutoff, labeled taxon NOT in training set
     * TN: bootstrap below cutoff, labeled taxon NOT in training set
     * 
     **/
     private void compareClassificationResult( TreeFactory factory, ValidClassificationResultFacade resultFacade,
       HashMap<String,HashSet> rankNodeMap, ArrayList<HashMap> statusCountList ) throws IOException{
         // determine assignment status
        // find all the taxa for the ancestors
        HashMap<String, Taxonomy> labeledTaxonMap = new HashMap<String, Taxonomy>();
        labeledTaxonMap.put(factory.getRoot().getTaxonomy().getHierLevel(), factory.getRoot().getTaxonomy());
        int pid = factory.getRoot().getTaxonomy().getTaxID();
        for ( int i = 1 ; i < resultFacade.getAncestors().size(); i++){
            Taxonomy tax = factory.getTaxonomy(resultFacade.getSeqName(), (String) resultFacade.getAncestors().get(i), pid, i);
            labeledTaxonMap.put(tax.getHierLevel(), tax);
            pid = tax.getTaxID();
        }


        List<ValidationClassificationResult> hitList = resultFacade.getRankAssignment();
        for ( ValidationClassificationResult curRankResult: hitList){
            String curRank = curRankResult.getBestClass().getTaxonomy().getHierLevel();
            // find the corresponding ancestor at the current rank
            Taxonomy matchingRankTaxon = labeledTaxonMap.get(curRank);
            if ( matchingRankTaxon == null) {  // no match rank found
                // System.err.println("no matching rank labeled taxon found for " + resultFacade.getSeqName() + " at " + curRank );
                continue;
            }
            HashSet<String> nodeNameSet = rankNodeMap.get(curRank);
            if ( nodeNameSet != null){
                   
                int bootstrap = (int)(curRankResult.getNumOfVotes()*100);
                //System.err.println( "rank: " + curRank + "\t" + matchingRankTaxon.getName() + "\t" + nodeNameSet.contains( matchingRankTaxon.getName()) + "\t" + bootstrap +"\t");
                if ( nodeNameSet.contains( matchingRankTaxon.getName())){  // TP or FN
                    for( int b = 0; b <= bootstrap; b++){
                        ((StatusCount)statusCountList.get(b).get(curRank)).incNumTP(1);
                    }
                    for( int b = bootstrap+1; b < statusCountList.size(); b++){
                        ((StatusCount)statusCountList.get(b).get(curRank)).incNumFN(1);
                    }

                }else {// TN or FP
                    for( int b = 0; b <= bootstrap; b++){
                        ((StatusCount)statusCountList.get(b).get(curRank)).incNumFP(1);
                    }
                    for( int b = bootstrap+1; b < statusCountList.size(); b++){
                        ((StatusCount)statusCountList.get(b).get(curRank)).incNumTN(1);
                    }
                 }

            }
        }

        
     }

     /**
      * sensitivity = #TP / (#TP + #FN)
      * specificity = #TN / (#TN + #FP)
      * @param statusCountList
      */
     public void calErrorRate(ArrayList<HashMap> statusCountList){
        System.err.println("\nbootstrap\t1-Specificity\tSensitivity");
        System.err.print("bootstrap");
        HashMap<String, StatusCount> statusCountMap = statusCountList.get(0);
        for ( String rank: statusCountMap.keySet()){
            if ( rank.startsWith("sub")) continue;
            System.err.print("\t" + rank + "1-Specificity" +"\t" + rank );
        }
        System.err.println();

        for( int b = 0; b < statusCountList.size(); b++){
            System.err.print(b );
            statusCountMap = statusCountList.get(b);
            for ( String rank: statusCountMap.keySet()){
                if ( rank.startsWith("sub")) continue;
                StatusCount st = statusCountMap.get(rank);
                double se = st.calSensitivity();
                double sp = st.calSpecificity();
                System.err.print("\t" + (1-sp) + "\t" + se  );
            }
            System.err.println();
        }

     }

}
