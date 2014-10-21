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

import edu.msu.cme.rdp.classifier.ClassificationResult;
import edu.msu.cme.rdp.multicompare.MCSample;
import edu.msu.cme.rdp.taxatree.UnifracTree;
import edu.msu.cme.rdp.taxatree.VisitInfo;
import edu.msu.cme.rdp.taxatree.interfaces.TreeVisitor;
import edu.msu.cme.rdp.taxatree.utils.ClassifierTreeBuilder;
import edu.msu.cme.rdp.taxatree.utils.ClassifierTreeBuilder.ClassifierTaxonFactory;
import java.util.List;
import java.util.Map;

/**
 *
 * @author fishjord
 */
public class UnifracTreeBuilder {

    public static class UnifracSample {
        public MCSample sample;
        public double count;
    }

    private ClassifierTreeBuilder builder;

    public UnifracTreeBuilder(List<ClassificationResult> classifications, final Map<String, UnifracSample> sampleMap) {
        this(classifications, sampleMap, 0.0f);
    }

    public UnifracTreeBuilder(List<ClassificationResult> classifications, final Map<String, UnifracSample> sampleMap, float cutoff) {
        builder = new ClassifierTreeBuilder(new UnifracTree(), classifications, cutoff, new ClassifierTaxonFactory<UnifracTaxon>() {
            public UnifracTaxon buildTaxon(int taxid, String name, String rank, float conf, boolean unclassified) {
                float dist = 1f;
                if(rank.equals("sequence"))
                    dist = 0.0f;
                return new UnifracTaxon(taxid, name, rank, dist, unclassified);
            }
        });

        builder.getRoot().topDownVisit(new TreeVisitor<UnifracTaxon>(){

            public boolean visitNode(VisitInfo<UnifracTaxon> visitInfo) {
                UnifracTaxon taxon = visitInfo.getTaxon();
                
                if(sampleMap.containsKey(taxon.getName()))
                    taxon.setSampleCount(sampleMap.get(taxon.getName()).sample, sampleMap.get(taxon.getName()).count);
                return true;
            }

        });

        ((UnifracTree)builder.getRoot()).refreshInnerTaxa();
    }

    public UnifracTree getUnifracTree() {
        return (UnifracTree)builder.getRoot();
    }
}
