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
public class EstimateSPrintVisitor implements TreeVisitor<MCTaxon> {

    private PrintStream out;
    private List<MCSample> samples;
    private String rank;
    private int nodeCount = 0;

    public EstimateSPrintVisitor(PrintStream out, List<MCSample> samples, String rank, int nodeCount) {
        this.out = out;
        this.samples = samples;
        this.rank = rank;
        this.nodeCount = nodeCount;
        printHeader();
    }

    private void printHeader() {
        out.println("EstimateS File prepared by RDP MultiCompare Library Comparision Tool");
        out.println(nodeCount + "\t" + samples.size());
    }

    public boolean visitNode(VisitInfo<MCTaxon> info) {
        MCTaxon taxon = info.getTaxon();
        if(!taxon.getRank().equals(rank))
            return true;

        StringBuffer sampleBuf = new StringBuffer();

        int seqCount = 0;
        for(MCSample sample : samples) {
            sampleBuf.append(taxon.getCount(sample) + "\t");
            seqCount += taxon.getCount(sample);
        }

        if(seqCount > 0)
            out.println(sampleBuf);

        return true;
    }
}
