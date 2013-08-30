/*
 * Copyright (C) 2012 Michigan State University <rdpstaff at msu.edu>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package edu.msu.cme.rdp.taxatree.utils;

import edu.msu.cme.rdp.classifier.ClassificationResult;
import edu.msu.cme.rdp.classifier.RankAssignment;
import edu.msu.cme.rdp.taxatree.ConcretRoot;
import edu.msu.cme.rdp.taxatree.Taxon;
import edu.msu.cme.rdp.taxatree.TaxonHolder;
import java.util.List;

/**
 *
 * @author fishjord
 */
public class ClassifierTreeBuilder<E extends Taxon> {

    public static interface ClassifierTaxonFactory<E extends Taxon> {

        public E buildTaxon(int taxid, String name, String rank, float conf, boolean unclassified);
    }
    private ClassifierTaxonFactory<E> taxonFactory;
    private ConcretRoot<E> root;
    private float conf;
    private boolean incSeqNodes = true;

    public ClassifierTreeBuilder(E rootTaxon, List<ClassificationResult> classifications, float conf, ClassifierTaxonFactory<E> taxonFactory) {
        this(new ConcretRoot<E>(rootTaxon), classifications, conf, taxonFactory);
    }

    public ClassifierTreeBuilder(ConcretRoot<E> root, List<ClassificationResult> classifications, float conf, ClassifierTaxonFactory<E> taxonFactory) {
        this(root, conf, taxonFactory);

        this.appendClassifications(classifications);
    }

    public ClassifierTreeBuilder(E rootTaxon, float conf, ClassifierTaxonFactory<E> taxonFactory) {
        this(new ConcretRoot<E>(rootTaxon), conf, taxonFactory);
    }

    public ClassifierTreeBuilder(ConcretRoot<E> root, float conf, ClassifierTaxonFactory<E> taxonFactory) {
        this.root = root;
        this.taxonFactory = taxonFactory;
        this.conf = conf;
    }

    public void appendClassifications(List<ClassificationResult> classifications) {
        for (ClassificationResult result : classifications) {
            processAssignments(root.getRootTaxid(), result);
        }
    }

    public void appendClassification(ClassificationResult classification) {
        processAssignments(root.getRootTaxid(), classification);
    }

    public void setIncSeqNodes(boolean b) {
        this.incSeqNodes = b;
    }

    public void setTaxonFactory(ClassifierTaxonFactory<E> taxonFactory) {
        this.taxonFactory = taxonFactory;
    }

    private void processAssignments(int rootId, ClassificationResult result) {
        int parentId = rootId;
        for (RankAssignment assign : (List<RankAssignment>) result.getAssignments()) {
            if (!createOrAddTaxon(parentId, assign)) {
                break;
            }
            parentId = assign.getTaxid();
        }

        if (incSeqNodes) {
            createOrAddTaxon(parentId, result.getSequence().getSeqName().hashCode(), result.getSequence().getSeqName(), "sequence", 1);
        }
    }

    private boolean createOrAddTaxon(int parentId, RankAssignment assign) {
        return createOrAddTaxon(parentId, assign.getTaxid(), assign.getName(), assign.getRank(), assign.getConfidence());
    }

    private boolean createOrAddTaxon(int parentId, int assignTaxid, String assignName, String assignRank, float assignConf) {
        TaxonHolder<E> parentHolder = root.getChild(parentId);
        if (parentHolder == null) {
            throw new IllegalStateException("Cannot find parent (taxid=" + parentId + ") when trying to add child " + assignTaxid + " " + assignName + " " + assignRank);
        }

        int taxid = assignTaxid;
        if (assignConf < conf) {
            taxid = Taxon.getUnclassifiedId(taxid);
        }

        E taxon = root.getChildTaxon(taxid);

        if (taxon == null) {
            taxon = taxonFactory.buildTaxon(assignTaxid, assignName, assignRank, assignConf, assignConf < conf);
            root.addChild(taxon, parentId);
        }

        return assignConf > conf;
    }

    public ConcretRoot<E> getRoot() {
        return root;
    }
}
