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
package edu.msu.cme.rdp.taxatree;

import edu.msu.cme.rdp.multicompare.MCSample;
import edu.msu.cme.rdp.unifrac.UnifracTaxon;
import edu.msu.cme.rdp.unifrac.UnifracTreeBuilder.UnifracSample;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author fishjord
 */
public class UnifracTree extends ConcretRoot<UnifracTaxon> {

    public static class UnifracResult {

        private List<MCSample> samples;
        private float[][] unifracMatrix;

        public UnifracResult(List<MCSample> samples, float[][] unifracMatrix) {
            this.samples = samples;
            this.unifracMatrix = unifracMatrix;
        }

        public List<MCSample> getSamples() {
            return samples;
        }

        public float[][] getUnifracMatrix() {
            return unifracMatrix;
        }
    }
    private Set<Integer> leaves = new LinkedHashSet();

    public UnifracTree() {
        super(new UnifracTaxon(0, "Root", "no rank", 0));
        leaves.add(0);
    }

    public UnifracResult computeUnifrac() {
        Set<MCSample> samplesSet = new HashSet();

        for (TaxonHolder<UnifracTaxon> t : taxonMap.values()) {
            if (leaves.contains(t.getTaxon().getTaxid())) {
                for (MCSample sample : t.getTaxon().getSamples()) {
                    samplesSet.add(sample);
                }
            }
        }
        List<MCSample> samples = new ArrayList(new HashSet(samplesSet));

        float[][] unifracMatrix = new float[samples.size()][samples.size()];

        for (int sample1 = 0; sample1 < samples.size(); sample1++) {
            unifracMatrix[sample1][sample1] = 0;
            for (int sample2 = sample1 + 1; sample2 < samples.size(); sample2++) {
                unifracMatrix[sample1][sample2] = unifracMatrix[sample2][sample1] =
                        computeUnifracMetric(samples.get(sample1), samples.get(sample2));
            }
        }

        return new UnifracResult(samples, unifracMatrix);
    }

    public UnifracResult computeUnifracSig(int permutations, boolean weighted) {
        UnifracResult real;
        if(weighted)
            real = this.computeWeightedUnifrac();
        else
            real = this.computeUnifrac();

        /**
         * First we need to create an array that contains every sample (with duplicates)
         * so we can shuffle them around (using an ArrayList instead of List to get at the
         * clone method)
         */
        Map<MCSample, List<UnifracSample>> allSamplesMap = new HashMap();
        Map<Integer, List<UnifracSample>> originalSampleMap = new HashMap();
        Map<MCSample, Double> totalsMap = new HashMap();

        for (TaxonHolder<UnifracTaxon> t : taxonMap.values()) {
            
            if (!originalSampleMap.containsKey(t.getTaxon().getTaxid())) {
                originalSampleMap.put(t.getTaxon().getTaxid(), new ArrayList());
            }


            if (leaves.contains(t.getTaxon().getTaxid())) {
                for (MCSample sample : t.getTaxon().getSamples()) {
                    UnifracSample unifracSample = new UnifracSample();
                    unifracSample.count = t.getTaxon().getCount(sample);
                    unifracSample.sample = sample;

                    if (!allSamplesMap.containsKey(sample)) {
                        allSamplesMap.put(sample, new ArrayList());
                    }

                    if(!totalsMap.containsKey(sample))
                        totalsMap.put(sample, 0.0);

                    allSamplesMap.get(sample).add(unifracSample);
                    originalSampleMap.get(t.getTaxon().getTaxid()).add(unifracSample);

                    totalsMap.put(sample, totalsMap.get(sample) + t.getTaxon().getCount(sample));
                }
            } else {
                t.getTaxon().resetSamples();
            }
        }

        /**
         * Unique list so that we can get the indicies for direct access
         * to the unifracMatrix
         */
        List<MCSample> samples = new ArrayList(allSamplesMap.keySet());

        float[][] unifracMatrix = new float[samples.size()][samples.size()];

        for (int perm = 0; perm < permutations; perm++) {

            for (int sample1 = 0; sample1 < samples.size(); sample1++) {
                unifracMatrix[sample1][sample1] = 0;
                for (int sample2 = sample1 + 1; sample2 < samples.size(); sample2++) {
                    List<UnifracSample> samplePool = new ArrayList();
                    samplePool.addAll(allSamplesMap.get(samples.get(sample1)));
                    samplePool.addAll(allSamplesMap.get(samples.get(sample2)));
                    Collections.shuffle(samplePool);

                    for (int taxid : leaves) {
                        UnifracTaxon t = taxonMap.get(taxid).getTaxon();
                        if (t.containsSample(samples.get(sample1)) || t.containsSample(samples.get(sample2))) {
                            t.resetSamples(samplePool);
                        }
                    }

                    this.refreshInnerTaxa();
                    float val;
                    if(weighted)
                        val = this.computeUnifracMetricWeighted(samples.get(sample1), samples.get(sample2), totalsMap);
                    else
                        val = this.computeUnifracMetric(samples.get(sample1), samples.get(sample2));

                    if (val > real.getUnifracMatrix()[sample1][sample2]) {
                        unifracMatrix[sample1][sample2]++;
                        unifracMatrix[sample2][sample1]++;
                    }

                    for (int taxid : leaves) {
                        taxonMap.get(taxid).getTaxon().resetSamples(new ArrayList(originalSampleMap.get(taxid)));
                    }

                    this.refreshInnerTaxa();
                }
            }
        }

        for (float[] row : unifracMatrix) {
            for (int index = 0; index < row.length; index++) {
                row[index] /= permutations;
            }
        }

        return new UnifracResult(samples, unifracMatrix);
    }

    private float computeUnifracMetric(MCSample sample1, MCSample sample2) {
        float unique = 0;
        float combined = 0;
        Set<Integer> touched = new HashSet();

        for (Integer taxid : leaves) {
            TaxonHolder<UnifracTaxon> leaf = this.getChild(taxid);
            TaxonHolder<UnifracTaxon> parent = leaf;

            while (parent.getParent() != null) {
                UnifracTaxon taxon = parent.getTaxon();
                if (!touched.contains(parent.getTaxon().getTaxid())) {
                    touched.add(taxon.getTaxid());
                    if (parent.getTaxon().containsSample(sample1) && taxon.containsSample(sample2)) {
                        combined += taxon.getBl();
                    } else if (taxon.containsSample(sample1) || taxon.containsSample(sample2)) {
                        unique += taxon.getBl();
                    }
                }
                parent = parent.getParent();
            }
        }

        return ((float) unique) / (unique + combined);
    }

    public UnifracResult computeWeightedUnifrac() {
        Set<MCSample> samplesSet = new HashSet();
        Map<MCSample, Double> totalsMap = new HashMap();

        for (int i : leaves) {
            UnifracTaxon leaf = this.getChildTaxon(i);
            for (MCSample sample : leaf.getSamples()) {
                if (!totalsMap.containsKey(sample)) {
                    totalsMap.put(sample, 0.0);
                }
                samplesSet.add(sample);
                totalsMap.put(sample, totalsMap.get(sample) + leaf.getCount(sample));
            }
        }
        List<MCSample> samples = new ArrayList(new HashSet(samplesSet));

        float[][] unifracMatrix = new float[samples.size()][samples.size()];

        for (int sample1 = 0; sample1 < samples.size(); sample1++) {
            unifracMatrix[sample1][sample1] = 0;
            for (int sample2 = sample1 + 1; sample2 < samples.size(); sample2++) {
                unifracMatrix[sample1][sample2] = unifracMatrix[sample2][sample1] =
                        computeUnifracMetricWeighted(samples.get(sample1), samples.get(sample2), totalsMap);
            }
        }

        return new UnifracResult(samples, unifracMatrix);
    }

    private float computeUnifracMetricWeighted(MCSample sample1, MCSample sample2, Map<MCSample, Double> totalsMap) {

        float ret = 0;
        for (TaxonHolder<UnifracTaxon> taxonHolder : taxonMap.values()) {
            UnifracTaxon taxon = taxonHolder.getTaxon();
            ret += taxon.getBl() * Math.abs(( taxon.getCount(sample1)) / totalsMap.get(sample1) - ( taxon.getCount(sample2)) / totalsMap.get(sample2));
        }

        return ret;
    }

    public void printLeaves() {
        for (Integer taxid : leaves) {
            UnifracTaxon leaf = this.getChildTaxon(taxid);
            System.out.println(leaf.getTaxid() + "\t" + leaf.getName() + "\t" + leaf.getRank() + "\t" + leaf.getSamples());
        }
    }

    @Override
    public void addChild(UnifracTaxon child, int parentId) {
        if (leaves.contains(parentId)) {
            leaves.remove(parentId);
        }

        leaves.add(child.getTaxid());

        super.addChild(child, parentId);
    }

    /*public void createFiles(String newickFile, String envFile) throws IOException {
    PrintWriter newickWriter = new PrintWriter(newickFile);
    PrintWriter envWriter = new PrintWriter(envFile);

    newickWriter.print("(");
    for (int childIndex = 0; childIndex < root.getChildren().size(); childIndex++) {
    createFiles(root.getChildren().get(childIndex), newickWriter, envWriter);
    if (childIndex != root.getChildren().size() - 1) {
    newickWriter.print(", ");
    }
    }
    newickWriter.print(") " + root.getTaxon().getName() + ";");
    newickWriter.close();
    envWriter.close();
    }

    private void createFiles(UnifracTaxon taxon, PrintWriter newickWriter, PrintWriter envWriter) {*/
    /* if (leaves.contains(taxon.getTaxid())) {
    int sampleIndex = 0;
    for (String sample : taxon.getSamples()) {
    String seqid = SeqIdGen.nextSeq();

    envWriter.println(seqid + "\t" + sample);
    newickWriter.print(seqid + ":" );

    if (++sampleIndex != taxon.getSamples().size()) {
    newickWriter.print(", ");
    }
    }
    } else {*//*
    if(leaves.contains(taxon.getTaxid())) {
    envWriter.println(taxon.getName() + "\t" + new ArrayList(taxon.getSamples()).get(0));
    } else {
    newickWriter.print("(");
    }
    for (int childIndex = 0; childIndex < taxon.getChildren().size(); childIndex++) {
    createFiles(taxon.getChildren().get(childIndex), newickWriter, envWriter);
    if (childIndex != taxon.getChildren().size() - 1) {
    newickWriter.print(", ");
    }
    }
    //}
    if(!leaves.contains(taxon.getTaxid()))
    newickWriter.print(") ");
    newickWriter.print(taxon.getName() + ":" + taxon.getBl());
    }*/

    public void refreshInnerTaxa() {
        for (TaxonHolder<UnifracTaxon> t : taxonMap.values()) {
            if (!leaves.contains(t.getTaxon().getTaxid())) {
                t.getTaxon().resetSamples();
            }
        }

        for (Integer taxid : leaves) {
            TaxonHolder<UnifracTaxon> leaf = taxonMap.get(taxid);
            TaxonHolder<UnifracTaxon> parent = leaf.getParent();

            while (parent != null) {
                for (MCSample sample : leaf.getTaxon().getSamples()) {
                    parent.getTaxon().addSampleCount(sample, leaf.getTaxon().getCount(sample));
                }
                parent = parent.getParent();
            }
        }
    }

    public void addTaxon(int parent, int taxid, String name, MCSample sample, float bl) {
        TaxonHolder<UnifracTaxon> parentHolder = taxonMap.get(parent);
        if (parentHolder == null) {
            throw new IllegalArgumentException("Couldn't find parent taxon id=" + parent);
        }
        UnifracTaxon parentTaxon = parentHolder.getTaxon();

        TaxonHolder<UnifracTaxon> holder = taxonMap.get(taxid);
        if (holder == null) {
            holder = new TaxonHolder(new UnifracTaxon(taxid, name, "", bl), parentHolder);
            UnifracTaxon t = holder.getTaxon();

            if (sample != null) {
                //xxxxx
                t.incCount(sample, 1);
            }
            parentHolder.addChild(holder);
            if (leaves.contains(parentTaxon.getTaxid())) {
                leaves.remove(parentTaxon.getTaxid());
            }
            leaves.add(t.getTaxid());
            taxonMap.put(t.getTaxid(), holder);
        }

    }
}
