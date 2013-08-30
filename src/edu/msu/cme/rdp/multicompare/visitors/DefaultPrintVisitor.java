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

package edu.msu.cme.rdp.multicompare.visitors;

import edu.msu.cme.rdp.multicompare.MCSample;
import edu.msu.cme.rdp.multicompare.taxon.MCTaxon;
import edu.msu.cme.rdp.taxatree.VisitInfo;
import edu.msu.cme.rdp.taxatree.interfaces.TreeVisitor;
import java.io.PrintStream;
import java.util.List;

/**
 *
 * @author fishjord
 */
public class DefaultPrintVisitor implements TreeVisitor<MCTaxon> {

    private PrintStream out;
    private List<MCSample> samples;
    private boolean ommitEmpty;

    public DefaultPrintVisitor(PrintStream out, List<MCSample> samples) {
        this(out, samples, true);
    }

    public DefaultPrintVisitor(PrintStream out, List<MCSample> samples, boolean ommitEmpty) {
        this.out = out;
        this.samples = samples;
        printHeader();
    }

    private void printHeader() {
        String ret = "taxid\tlineage\tname\trank";
        for(MCSample sample : samples)
            ret += "\t" + sample.getSampleName();

        out.println(ret);
    }

    public boolean visitNode(VisitInfo<MCTaxon> info) {
        StringBuffer sampleBuf = new StringBuffer();
        MCTaxon taxon = info.getTaxon();

        int seqCount = 0;
        for(MCSample sample : samples) {
            sampleBuf.append("\t" + taxon.getCount(sample));
            seqCount += taxon.getCount(sample);
        }

        if(seqCount > 0 || !ommitEmpty)
            out.println(taxon.getTaxid() +"\t" + taxon.getLineage() + "\t" + taxon.getName() + "\t" + taxon.getRank() + sampleBuf);

        return true;
    }
}
