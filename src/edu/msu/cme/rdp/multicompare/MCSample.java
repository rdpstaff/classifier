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

import edu.msu.cme.rdp.classifier.ClassificationResult;
import edu.msu.cme.rdp.classifier.RankAssignment;
import edu.msu.cme.rdp.classifier.utils.ClassifierSequence;
import edu.msu.cme.rdp.readseq.readers.SeqReader;
import edu.msu.cme.rdp.readseq.readers.SequenceReader;
import edu.msu.cme.rdp.readseq.readers.Sequence;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author fishjord
 */
public class MCSample {

    private String sampleName;
    private SeqReader parser = null;
    private List<ClassifierSequence> sampleSeqs = new ArrayList();
    private Map<String, int[]> bootstrapCountTable = new HashMap<String, int[]>();
    private Map<String, Integer> dupCountTable = new HashMap<String, Integer>();
    private int seqIndex = 0;

    public MCSample(SeqReader reader, String name) {
        this.sampleName = name;
        this.parser = reader;
    }

    public MCSample(File f) throws IOException {
        this(f, f.getName());
    }

    public MCSample(String sampleName) {
        this.sampleName = sampleName;
    }

    public MCSample(String sampleName, File dupCountFile) throws IOException {
        this.sampleName = sampleName;
        setDupCountFile(dupCountFile);
    }

    public MCSample(List<ClassifierSequence> sampleSeqs, String sampleName) {
        this.sampleSeqs = sampleSeqs;
        this.sampleName = sampleName;
    }

    public MCSample(List<ClassifierSequence> sampleSeqs, String sampleName, File dupCountFile) throws IOException {
        this.sampleSeqs = sampleSeqs;
        this.sampleName = sampleName;
        setDupCountFile(dupCountFile);
    }

    public MCSample(File in, String sampleName) throws IOException {
        parser = new SequenceReader(in);
        this.sampleName = sampleName;
    }

    public MCSample(File sampleFile, File dupCountFile) throws IOException {
        this(sampleFile);
        setDupCountFile(dupCountFile);
    }

    public String getSampleName() {
        return sampleName;
    }

    public ClassifierSequence getNextSeq() throws IOException {
        if (parser != null) {
            Sequence seq = parser.readNextSequence();
            if(seq == null) {
                return null;
            } else {
                return new ClassifierSequence(seq);
            }
        } else {
            if (seqIndex < sampleSeqs.size()) {
                return sampleSeqs.get(seqIndex++);
            } else {
                return null;
            }
        }
    }

    private void setDupCountFile(File dupCountFile) throws IOException {
        if (dupCountFile == null) {
            return;
        }
        BufferedReader reader = new BufferedReader(new FileReader(dupCountFile));
        String line = "";
        while ((line = reader.readLine()) != null) {
            String[] keys = line.split("\\s+");
            String[] dupSeqList = keys[1].split(",");
            dupCountTable.put(dupSeqList[0], new Integer(dupSeqList.length));
        }
        reader.close();
    }

    public int getDupCount(String seqName) {
        Integer dup = dupCountTable.get(seqName);
        // returns 1 if seqName not found in the table
        return (dup == null) ? 1 : dup.intValue();
    }

    public void addRankCount(ClassificationResult result) {
        int count = getDupCount(result.getSequence().getSeqName());

        for (RankAssignment assignment : (List<RankAssignment>) result.getAssignments()) {
            int[] bootstrapCountArr = bootstrapCountTable.get(assignment.getRank());
            if (bootstrapCountArr == null) {
                bootstrapCountArr = new int[11];
                bootstrapCountTable.put(assignment.getRank(), bootstrapCountArr);
            }

            int bootstrapIndex = getBootstrapIndex(assignment.getConfidence());
            bootstrapCountArr[bootstrapIndex] += count;
        }

    }

    public static int getBootstrapIndex(double d) {
        return (int) Math.ceil(d * 10.0);
    }

    public Map<String, int[]> getBootstrapCountTable() {
        return bootstrapCountTable;
    }

    @Override
    public String toString() {
        return sampleName;
    }
}
