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

    public enum FORMAT {

        allRank, fixRank, dbformat, filterbyconf;
    }

    public static String getOutput(ClassificationResult result, FORMAT format){
        return getOutput(result, format, 0f);
    }
    
    public static String getOutput(ClassificationResult result, FORMAT format, float conf) {
        switch (format) {
            case allRank:
                return getAllRankOutput(result);
            case fixRank:
                return getFixRankOutput(result);
            case dbformat:
                return getDBOutput(result, conf);
            case filterbyconf:
                return getFilterByConfOutput(result, conf);
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
        RankAssignment prevAssign = null;
        for (int i = ranks.length -1; i>=0; i--) {
            RankAssignment assign = rankMap.get(ranks[i]);
            if (assign != null) {
                if ( assign.getConfidence() >= conf){
                    if ( prevAssign != null && prevAssign.getConfidence() < conf){
                        assignmentStr.insert(0, "\t" +  "unclassified_" + assign.getName() +"\t" + ranks[i+1] + "\t" + assign.getConfidence());
                    }
                    assignmentStr.insert(0, "\t" + assign.getName() +"\t" + assign.getRank() + "\t" + assign.getConfidence());
                }
                prevAssign = assign;
            } else {
                if ( prevAssign != null && prevAssign.getConfidence() >= conf){
                    assignmentStr.insert(0, "\t" + prevAssign.getName() +"\t" + ranks[i] + "\t" + prevAssign.getConfidence());
                }
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
