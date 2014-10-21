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

package edu.msu.cme.rdp.multicompare.taxon;


import edu.msu.cme.rdp.multicompare.MCSample;
import edu.msu.cme.rdp.taxatree.Taxon;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author fishjord
 */
public class MCTaxon extends Taxon {
  
    protected Map<MCSample, double[]> sampleCountMap = new LinkedHashMap(); // the first count is the original count, the second count are copy number adjusted
    private Set<String> sequences = new HashSet();
    private String lineage;
    public static final int Count_Array_size = 2;
    
    public MCTaxon(int id, String name, String rank) {
        this(id, name, rank, false);
    }

    public MCTaxon(int id, String name, String rank, boolean unclassified) {
        super(id, name, rank, unclassified);
    }

    public MCTaxon(Taxon t) {
        super(t);
    }

    public void addSequence(String seqid) {
        sequences.add(seqid);
    }

    public Set<String> getSequences() {
        return Collections.unmodifiableSet(sequences);
    }

    public void setLineage(String lineage) {
        this.lineage = lineage;
    }

    public String getLineage() {
        return lineage;
    }

    public Set<MCSample> getSamples() {
        return sampleCountMap.keySet();
    }

    public double getCount(MCSample s) {
        if(sampleCountMap.containsKey(s))
            return sampleCountMap.get(s)[0];
        else
            return 0.0;
    }
    
    public double getCopyCorrectedCount(MCSample s) {
        if(sampleCountMap.containsKey(s))
            return sampleCountMap.get(s)[1];
        else
            return 0.0;
    }
        

    public void incCount(MCSample s, double c ) {
        incCount(s, c, 1);
    }
    
    /*
    * Need to correct the count by copy number
    */
    public void incCount(MCSample s, double c, double copyNumber) {
        double[] i = sampleCountMap.get(s);
        if(i == null){
            i = new double[Count_Array_size];
            sampleCountMap.put(s, i );
        }

        i[0] = i[0] + c ;
        i[1] = i[1] + c/copyNumber;
        
    }
}
