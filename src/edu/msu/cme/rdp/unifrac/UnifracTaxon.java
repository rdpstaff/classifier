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

package edu.msu.cme.rdp.unifrac;

import edu.msu.cme.rdp.multicompare.MCSample;
import edu.msu.cme.rdp.multicompare.taxon.MCTaxon;
import edu.msu.cme.rdp.unifrac.UnifracTreeBuilder.UnifracSample;
import java.util.List;

/**
 *
 * @author fishjord
 */
public class UnifracTaxon extends MCTaxon {
    private float bl;

    public UnifracTaxon(int taxid, String name, String rank, float bl) {
        this(taxid, name, rank, bl, false);
    }

    public UnifracTaxon(int taxid, String name, String rank, float bl, boolean unclassified) {
        super(taxid, name, rank, unclassified);
        this.bl = bl;
    }

    public UnifracTaxon(MCTaxon taxon, float bl) {
        this(taxon.getTaxid(), taxon.getName(), taxon.getRank(), bl);

        for(MCSample sample : taxon.getSamples()) {
            setSampleCount(sample, taxon.getCount(sample));
        }
    }

    public float getBl() {
        return bl;
    }

    public boolean containsSample(MCSample sample) {
        return sampleCountMap.containsKey(sample) && sampleCountMap.get(sample)[0] > 0;
    }

    public void resetSamples() {
        sampleCountMap.clear();
    }

    public void setSampleCount(MCSample sample, double count) {
        double[] d = new double[Count_Array_size];
        d[0] = count;
        sampleCountMap.put(sample, d);
    }

    public void addSampleCount(MCSample sample, double count) {
        double[] d = sampleCountMap.get(sample);
        if( d == null){
            d = new double[Count_Array_size];
            sampleCountMap.put(sample, d);
        }
        d[0] += count;
        
    }

    public void resetSamples(List<UnifracSample> samplePool) {
        int sampleCount = sampleCountMap.size();

        this.resetSamples();

        for(int index = 0;index < sampleCount;index++) {
            UnifracSample sample = samplePool.remove(0);
            this.addSampleCount(sample.sample, sample.count);
        }
    }
}
