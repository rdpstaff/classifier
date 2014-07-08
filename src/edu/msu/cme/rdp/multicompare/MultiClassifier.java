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
package edu.msu.cme.rdp.multicompare;

import edu.msu.cme.rdp.classifier.*;
import edu.msu.cme.rdp.classifier.rrnaclassifier.ClassificationParser;
import edu.msu.cme.rdp.classifier.io.ClassificationResultFormatter;
import edu.msu.cme.rdp.classifier.utils.ClassifierFactory;
import edu.msu.cme.rdp.classifier.utils.ClassifierSequence;
import edu.msu.cme.rdp.multicompare.taxon.MCTaxon;
import edu.msu.cme.rdp.readseq.readers.Sequence;
import edu.msu.cme.rdp.taxatree.ConcretRoot;
import edu.msu.cme.rdp.taxatree.Taxon;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.output.NullWriter;

/**
 *
 * @author fishjord
 */
public class MultiClassifier {

    private static ClassifierFactory classifierFactory;
    private static final float DEFAULT_CONF = 0.8f;
    private static final PrintWriter DEFAULT_ASSIGN_WRITER = new PrintWriter(new NullWriter());
    private static final ClassificationResultFormatter.FORMAT DEFAULT_FORMAT = ClassificationResultFormatter.FORMAT.allRank;
    private File biomFile = null;
    private HashMap<String, HashMap<String, String>> metadataMap = null;
    private String[] ranks = ClassificationResultFormatter.RANKS; // default ranks;

    public MultiClassifier(String propfile, String gene){

        if (propfile != null) {
            ClassifierFactory.setDataProp(propfile, false);
        }
        try {
            classifierFactory = ClassifierFactory.getFactory(gene);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        if ( gene != null && (gene.equalsIgnoreCase(ClassifierFactory.FUNGALITS_warcup_GENE) || gene.equalsIgnoreCase(ClassifierFactory.FUNGALITS_unite_GENE)) ){
            ranks = ClassificationResultFormatter.RANKS_WITHSPECIES;
        }
    }

    public MultiClassifier(String propfile, String gene, File biomFile, File metadataFile) throws IOException{
        this(propfile, gene);
        this.biomFile = biomFile;
        if ( metadataFile != null){
            metadataMap = readMetaData(metadataFile);
        }        
    }
    
    public MultiClassifierResult multiCompare(List<MCSample> samples) throws IOException {
        return multiCompare(samples, DEFAULT_CONF, DEFAULT_ASSIGN_WRITER, DEFAULT_FORMAT, Classifier.MIN_BOOTSTRSP_WORDS);
    }
    
    public MultiClassifierResult multiCompare(List<MCSample> samples, int min_bootstrap_words) throws IOException {
        return multiCompare(samples, DEFAULT_CONF, DEFAULT_ASSIGN_WRITER, DEFAULT_FORMAT, min_bootstrap_words);
    }

    public MultiClassifierResult multiCompare(List<MCSample> samples, float conf, int min_bootstrap_words) throws IOException {
        return multiCompare(samples, conf, DEFAULT_ASSIGN_WRITER, DEFAULT_FORMAT, min_bootstrap_words);
    }

    public MultiClassifierResult multiCompare(List<MCSample> samples, PrintWriter assign_out, int min_bootstrap_words) throws IOException {
        return multiCompare(samples, DEFAULT_CONF, assign_out, DEFAULT_FORMAT, min_bootstrap_words);
    }

    /**
     * Input files are sequence files
     */
    public MultiClassifierResult multiCompare(List<MCSample> samples, float confidence, PrintWriter assign_out,
            ClassificationResultFormatter.FORMAT format, int min_bootstrap_words) throws IOException {
        HierarchyTree sampleTreeRoot  = classifierFactory.getRoot();
        ConcretRoot<MCTaxon> root = new ConcretRoot<MCTaxon>(new MCTaxon(sampleTreeRoot.getTaxid(), sampleTreeRoot.getName(), sampleTreeRoot.getRank()) );

        Classifier classifier = classifierFactory.createClassifier();
        List<String> badSequences = new ArrayList();
        Map<String, Integer> seqCountMap = new HashMap();
        Map<String, String> seqClassificationMap = new HashMap(); // holds the classification results to replace the biom metadata
        if ( format.equals(ClassificationResultFormatter.FORMAT.filterbyconf) ){
            for (int i = 0; i <= ranks.length -1; i++) {
                assign_out.print("\t" + ranks[i]);
            }
            assign_out.println();
        }
        for (MCSample sample : samples) {
            Sequence seq;

            while ((seq = sample.getNextSeq()) != null) {
                try {
                    ClassificationResult result = classifier.classify(new ClassifierSequence(seq), min_bootstrap_words);
                    if ( !format.equals(ClassificationResultFormatter.FORMAT.biom)){
                        printClassificationResult(result, assign_out, format, confidence);
                    }else {
                        seqClassificationMap.put(result.getSequence().getSeqName(), ClassificationResultFormatter.getOutput(result, format, confidence, ranks));
                    }
                    processClassificationResult(result, sample, root, confidence, seqCountMap);
                    sample.addRankCount(result);

                } catch (ShortSequenceException e) {
                    badSequences.add(seq.getSeqName());
                }

            }
        }

        if ( format.equals(ClassificationResultFormatter.FORMAT.biom)){
            printBiom(assign_out, seqClassificationMap);
        }
        return new MultiClassifierResult(root, samples, badSequences, seqCountMap);
    }
    
    
    private void printBiom(PrintWriter assign_out, Map<String, String> seqClassificationMap) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(biomFile)));
        String line = null;
        boolean replaceTaxonomy = false;
        boolean replaceSampleMetadata = false;
        boolean continueColumns = false;
        while ( (line=reader.readLine())!= null){
            if ( line.startsWith("\"rows")){
                replaceTaxonomy = true;
                assign_out.println(line);
                continue;
            }
            if (line.startsWith("\"columns\"")){                
                replaceTaxonomy = false;            
                if ( metadataMap != null){                
                    replaceSampleMetadata = true;
                }
                assign_out.println(line);
                continue;
            }
            if (line.trim().startsWith("],")){
                if ( continueColumns){ // finish printing the previous sample
                    assign_out.println("");
                }
                replaceTaxonomy = false; 
                replaceSampleMetadata = false;                
            }
            if (replaceTaxonomy) { // replace the taxonomy metadata    
               
                String[] values = line.split("\"");
                String cluster_id = values[3];
                String cluster_classification = seqClassificationMap.get(cluster_id);
                if ( cluster_classification == null ){
                    throw new IllegalArgumentException("Can not find the cluster_id " + cluster_id + " in the classification result of the input sequences");
                }
                String[] classification = cluster_classification.split("\\t");
                assign_out.print("\t {\"id\" : \"" + cluster_id +"\", \"metadata\" : {\"taxonomy\":[");

                assign_out.print( "\"" + classification[1] + "\"");

                if ( line.endsWith(",")){
                    assign_out.println( "]}},");
                }else {
                    assign_out.println( "]}}");
                }
                               
            }else if (replaceSampleMetadata){  // replace the sample metadata
                if ( line.trim().contains("{\"id\"")){
                    if ( continueColumns){ // finish printing the previous sample
                        assign_out.println(",");
                    }
                    String[] values = line.split("\"");
                    HashMap<String, String> sampleMap = metadataMap.get( values[3]);
                    if ( sampleMap== null){
                        throw new IllegalArgumentException("Sample " +  values[3] + " does not have metadata in the metadata file.");
                    }
                    assign_out.println( values[0] + "\"" + values[1] + "\"" + values[2]+ "\"" + values[3] 
                            + "\"" + values[4] + "\"" + values[5] +"\" : {");
                    Object[] tempList = sampleMap.keySet().toArray();
                    for ( int i = 0; i < tempList.length; i++){
                        if ( i < tempList.length -1){
                            assign_out.println("\t\t\"" + tempList[i] + "\":\"" + sampleMap.get(tempList[i]) + "\",");
                        }else {
                            assign_out.print("\t\t\"" + tempList[i] + "\":\"" + sampleMap.get(tempList[i]) + "\"}}");
                        }
                    }
                    continueColumns = true;
                }
            }else {
                assign_out.println(line);
            }
        }
        
        reader.close();
    }
    
    private HashMap<String, HashMap<String, String>> readMetaData(File metadataFile) throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader (metadataFile));
        String line = reader.readLine();        
        String[] header = line.split("\\t");
        HashMap<String, HashMap<String, String>> metadataMap = new HashMap<String, HashMap<String, String>>();
                
        while ( (line= reader.readLine())!= null){
            String[] vals = line.split("\\t");
            HashMap<String, String> sampleMap = new HashMap<String, String>();
            metadataMap.put(vals[0].trim(), sampleMap);
            for ( int i = 1; i < header.length; i++){
                sampleMap.put(header[i], vals[i]);
            }
        }
        
        reader.close();
        return metadataMap;
    }

    /**
     * Input files are the classification results
     * printRank indicates which rank to filter by the confidence for the detail assignment output
     * taxonFilter indicates which taxon to match for the detail assignment output
     */
    public MultiClassifierResult multiClassificationParser(List<MCSample> samples, float confidence, PrintWriter assign_out,
            ClassificationResultFormatter.FORMAT format, String printRank, HashSet<String> taxonFilter) throws IOException {
        HierarchyTree sampleTreeRoot  = classifierFactory.getRoot();
        ConcretRoot<MCTaxon> root = new ConcretRoot<MCTaxon>(new MCTaxon(sampleTreeRoot.getTaxid(), sampleTreeRoot.getName(), sampleTreeRoot.getRank()) );
        List<String> badSequences = new ArrayList();
        Map<String, Integer> seqCountMap = new HashMap();

        for (MCSample sample : samples) {
            ClassificationParser parser = ((MCSampleResult) sample).getClassificationParser(classifierFactory);
            ClassificationResult result;

            while ((result = parser.next()) != null) {               
                processClassificationResult(result, sample, root, confidence, seqCountMap);
                List<RankAssignment> assignList = result.getAssignments();
                if ( printRank == null){
                    printRank = assignList.get(assignList.size() -1).getRank();
                }
                boolean match = false;
                if ( taxonFilter == null){
                    match = true;
                }else {
                    for ( RankAssignment assign: assignList){
                        if (taxonFilter.contains(assign.getBestClass().getName()) ){
                            match = true;
                            break;
                        }   
                    }
                }
                if ( match){
                    for ( RankAssignment assign: assignList){
                        if ( assign.getRank().equalsIgnoreCase(printRank) && assign.getConfidence() >= confidence ){
                            printClassificationResult(result, assign_out, format, confidence);
                            break;
                        }
                    } 
                }
            }
            parser.close();
        }
        return new MultiClassifierResult(root, samples, badSequences, seqCountMap);
    }

    private MCTaxon findOrCreateTaxon(ConcretRoot<MCTaxon> root, RankAssignment assignment, int parentId, boolean unclassified, Map<String, Integer> seqCountMap, String lineage) {
        int taxid = assignment.getTaxid();
        if (unclassified) {
            taxid = Taxon.getUnclassifiedId(taxid);
        }

        MCTaxon ret = root.getChildTaxon(taxid);
        if (ret == null) {

            ret = new MCTaxon(assignment.getTaxid(), assignment.getName(), assignment.getRank(), unclassified);
            root.addChild(ret, parentId);

            Integer val = seqCountMap.get(ret.getRank());
            if (val == null) {
                val = 0;
            }
            seqCountMap.put(ret.getRank(), val + 1);
            ret.setLineage(lineage.toString() + ret.getName() + ";" + ret.getRank() + ";");
        }

        return ret;
    }

    private void printClassificationResult(ClassificationResult result, PrintWriter assign_out, ClassificationResultFormatter.FORMAT format, float confidence) throws IOException {
        String assignmentStr = ClassificationResultFormatter.getOutput(result, format, confidence, ranks);
        assign_out.print(assignmentStr);
    }

    private void processClassificationResult(ClassificationResult result, MCSample sample, ConcretRoot<MCTaxon> root, float conf, Map<String, Integer> seqCountMap) {
        RankAssignment lastAssignment = null;
        RankAssignment twoAgo = null;
        StringBuffer lineage = new StringBuffer();
        
        MCTaxon taxon = null;
        for (RankAssignment assignment : (List<RankAssignment>) result.getAssignments()) {

            int parentId = root.getRootTaxid();
            if (lastAssignment != null) {
                parentId = lastAssignment.getTaxid();
            }
            boolean stop = false;
            if (assignment.getConfidence() < conf) {

                parentId = root.getRootTaxid();
                if (twoAgo != null) {
                    parentId = twoAgo.getTaxid();
                }

                taxon = findOrCreateTaxon(root, lastAssignment, parentId, true, seqCountMap, lineage.toString());
                stop = true;
            } else {
                taxon = findOrCreateTaxon(root, assignment, parentId, false, seqCountMap, lineage.toString());

            }

            int count = sample.getDupCount(result.getSequence().getSeqName());
            taxon.incCount(sample, count);
            twoAgo = lastAssignment;
            lastAssignment = assignment;

            if (stop) {
                break;
            }
            lineage.append(assignment.getName()).append(";").append(assignment.getRank()).append(";");

        }
    }
}
