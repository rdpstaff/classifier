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
  
    protected Map<MCSample, Integer> sampleCountMap = new LinkedHashMap();
    private Set<String> sequences = new HashSet();
    private String lineage;

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

    public int getCount(MCSample s) {
        if(sampleCountMap.containsKey(s))
            return sampleCountMap.get(s);
        else
            return 0;
    }

    public void incCount(MCSample s, int c ) {
        Integer i = sampleCountMap.get(s);
        if(i == null)
            i = 0;

        sampleCountMap.put(s, i + c);
    }
}
