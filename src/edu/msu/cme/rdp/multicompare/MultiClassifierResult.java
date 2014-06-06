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

package edu.msu.cme.rdp.multicompare;

import edu.msu.cme.rdp.multicompare.taxon.MCTaxon;
import edu.msu.cme.rdp.taxatree.ConcretRoot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author wangqion
 */
public class MultiClassifierResult {
    private ConcretRoot<MCTaxon> root;
    private List<MCSample> samples = new ArrayList<MCSample>();
    private List<String> badSequences;
    private Map<String, Integer> seqCountMap;
    private HashMap<String, MCSample> sampleMap = new HashMap<String, MCSample>(); // for easy search

    public MultiClassifierResult(ConcretRoot root){
        this.root = root;
    }
    
    public MultiClassifierResult(ConcretRoot root, List<MCSample> samples, List<String> badSequences, Map<String, Integer> seqCountMap) {
        this.root = root;
        this.samples = samples;
        this.badSequences = badSequences;
        this.seqCountMap = seqCountMap;
    }

    public ConcretRoot getRoot() {
        return root;
    }

    public List<MCSample> getSamples() {
        return samples;
    }

    public List<String> getBadSequences() {
        return badSequences;
    }

    public Map<String, Integer> getSeqCountMap() {
        return seqCountMap;
    }
    
     
    public MCSample getSample(String sampleName){
        return sampleMap.get(sampleName);
    }
    
    public void addSample(MCSample sample){
        if ( !sampleMap.containsKey(sample.getSampleName())) {
            sampleMap.put(sample.getSampleName(), sample);
            samples.add(sample);
        }
    }
    
    public void addSampleList(List<MCSample> samples){
        for ( MCSample sample: samples){
            addSample(sample);
        }
    }
}
