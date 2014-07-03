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
package edu.msu.cme.rdp.classifier.io;

import edu.msu.cme.rdp.classifier.ClassificationResult;
import edu.msu.cme.rdp.classifier.RankAssignment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author wangqion
 */
public class ClassificationResultFormatter {
    // list of major rankd

    public static String[] RANKS = { "domain", "phylum", "class", "order", "family", "genus"};
    public static String[] RANKS_WITHSPECIES = { "domain", "phylum", "class", "order", "family", "genus", "species"};
    public static final List<ClassificationResultFormatter.FORMAT> fileFormats 
            = new ArrayList(Arrays.asList(FORMAT.allRank,FORMAT.dbformat,FORMAT.fixRank,FORMAT.filterbyconf,FORMAT.biom));

    public enum FORMAT {

        allRank, fixRank, dbformat, filterbyconf, biom;
    }

    public static String getOutput(ClassificationResult result, FORMAT format){
        return getOutput(result, format, 0f, RANKS);
    }
    
    public static String getOutput(ClassificationResult result, FORMAT format, float conf, String[] ranks) {
        switch (format) {
            case allRank:
                return getAllRankOutput(result);
            case fixRank:
                return getFixRankOutput(ranks, result);
            case dbformat:
                return getDBOutput(result, conf);
            case filterbyconf:
                return getFilterByConfOutput(ranks, result, conf);
            case biom:
                return getBiomOutput(ranks, result, conf, ';');
            default:
                getAllRankOutput(result);
        }
        return null;
    }

    public static String getAllRankOutput(ClassificationResult result) {
        StringBuilder assignmentStr = new StringBuilder(result.getSequence().getSeqName() + "\t");
        if (result.isReverse()) {
            assignmentStr.append("-");
        }
        for (RankAssignment assignment : (List<RankAssignment>) result.getAssignments()) {
            assignmentStr.append("\t").append(assignment.getName()).append("\t").append(assignment.getRank()).append("\t").append(assignment.getConfidence());
        }
        assignmentStr.append("\n");
        return assignmentStr.toString();
    }

    public static String getAllRankOutput(ClassificationResult result, double conf) {
        StringBuilder assignmentStr = new StringBuilder(result.getSequence().getSeqName() + "\t");
        if (result.isReverse()) {
            assignmentStr.append("-");
        }
        for (RankAssignment assignment : (List<RankAssignment>) result.getAssignments()) {

            if (assignment.getConfidence() >= conf) {
                assignmentStr.append("\t").append(assignment.getName()).append("\t").append(assignment.getRank()).append("\t").append(assignment.getConfidence());
            }

        }
        assignmentStr.append("\n");
        return assignmentStr.toString();
    }

    public static String getFixRankOutput(ClassificationResult result) {
        return getFixRankOutput(RANKS, result);
    }

    public static String getFixRankOutput(String[] ranks, ClassificationResult result) {
        StringBuilder assignmentStr = new StringBuilder();

        HashMap<String, RankAssignment> rankMap = new HashMap<String, RankAssignment>();
        for (RankAssignment assignment : (List<RankAssignment>) result.getAssignments()) {
            rankMap.put(assignment.getRank().toLowerCase(), assignment);
        }
        
        // if the score is missing for the rank, report the conf and name from the lower rank
        RankAssignment prevAssign = null;
        for (int i = ranks.length -1; i>=0; i--) {
            RankAssignment assign = rankMap.get(ranks[i]);
            if (assign != null) {
                assignmentStr.insert(0, "\t" + assign.getName() +"\t" + assign.getRank() + "\t" + assign.getConfidence());
                prevAssign = assign;
            } else {
                assignmentStr.insert(0, "\t" + prevAssign.getName() +"\t" + ranks[i] + "\t" + prevAssign.getConfidence());
            }
            
        }
        if (result.isReverse()) {
            assignmentStr.insert(0,"-");
        } else {
            assignmentStr.insert(0, "");
        }
        assignmentStr.insert(0, result.getSequence().getSeqName() + "\t");
        assignmentStr.append("\n");
        
        return assignmentStr.toString();

    }
    
    public static String getFilterByConfOutput(ClassificationResult result, float conf) {
        return getFilterByConfOutput(RANKS, result, conf);
    }
    
    public static String getFilterByConfOutput(String[] ranks, ClassificationResult result, float conf) {
        StringBuilder assignmentStr = new StringBuilder();

        HashMap<String, RankAssignment> rankMap = new HashMap<String, RankAssignment>();
        for (RankAssignment assignment : (List<RankAssignment>) result.getAssignments()) {
            rankMap.put(assignment.getRank().toLowerCase(), assignment);
        }
        // if the score is missing for the rank, report the conf and name from the lower rank if above the conf
        // if the lower rank is below the conf, output unclassified node name and the conf from the one above the conf
        RankAssignment prevAssign = result.getAssignments().get(0);
        assignmentStr.append(result.getSequence().getSeqName());
        for (int i = 0; i <= ranks.length -1; i++) {
            RankAssignment assign = rankMap.get(ranks[i]);
            if (assign != null) {
                if ( assign.getConfidence() <= conf){
                    assignmentStr.append("\t" + "unclassified_" + prevAssign.getName() );                   
                }else {
                    assignmentStr.append("\t" + assign.getName() );
                    prevAssign = assign;
                }
                
            } else {
                if ( prevAssign != null && prevAssign.getConfidence() >= conf){
                    assignmentStr.append("\t" + "unclassified_" + prevAssign.getName()  );
                }
            }
            
        } 
        assignmentStr.append("\n");
        return assignmentStr.toString();

    }

    /**
    * Output the classification result suitable to load into biom format. 
    * Concatenate the rank and the taxon name, remove quotes in the taxon name
    */
    public static String getBiomOutput(String[] ranks, ClassificationResult result, float conf, char delimiter) {
        StringBuilder assignmentStr = new StringBuilder();

        HashMap<String, RankAssignment> rankMap = new HashMap<String, RankAssignment>();
        for (RankAssignment assignment : (List<RankAssignment>) result.getAssignments()) {
            rankMap.put(assignment.getRank().toLowerCase(), assignment);
        }
        // if the score is missing for the rank, report the conf and name from the lower rank if above the conf
        // if the lower rank is below the conf, output unclassified node name and the conf from the one above the conf
        // remove the quotes in the name
        RankAssignment prevAssign = result.getAssignments().get(0);
        assignmentStr.append(result.getSequence().getSeqName() + "\t");
        for (int i = 0; i <= ranks.length -1; i++) {
            RankAssignment assign = rankMap.get(ranks[i]);
            String rank = RANKS[i].substring(0,1).toLowerCase();
            if (assign != null) {
                if ( assign.getConfidence() <= conf){
                    assignmentStr.append(rank + "__" + "unclassified_" + prevAssign.getName().replaceAll("\"", "") );                   
                }else {
                    assignmentStr.append( rank + "__"+ assign.getName().replaceAll("\"", "") );
                    prevAssign = assign;
                }
                
            } else {
                if ( prevAssign != null && prevAssign.getConfidence() >= conf){
                    assignmentStr.append( rank + "__" + "unclassified_" + prevAssign.getName().replaceAll("\"", "") );
                }
            }
            
            if ( i < ranks.length -1){
                assignmentStr.append(delimiter);
            }
        } 
        return assignmentStr.toString();

    }

    public static String getDBOutput(ClassificationResult result, float conf) {
        StringBuilder assignmentStr = new StringBuilder();
	boolean set = false;
	List assignments = result.getAssignments();
        for (int i = assignments.size() - 1; i >= 0; i--) {
        	int markAssigned = 0;
		RankAssignment assign = (RankAssignment) assignments.get(i);
		if (!set && assign.getConfidence() >= conf) {
			markAssigned = 1;
			set = true;
		}
 		assignmentStr.append(result.getSequence().getSeqName()).append("\t").append(result.getTrainsetNo()).append("\t").append(assign.getTaxid()).append("\t").append(assign.getConfidence()).append("\t").append(markAssigned).append("\n");
	}

        return assignmentStr.toString();
    }
}
