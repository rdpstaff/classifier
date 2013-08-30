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

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 *
 * @author wangqion
 */
public class MCSamplePrintUtil {

    private static ArrayList<String> orderedRankList = new ArrayList<String>();

    static {
        orderedRankList.add("domain");
        orderedRankList.add("phylum");
        orderedRankList.add("class");
        orderedRankList.add("order");
        orderedRankList.add("family");
        orderedRankList.add("genus");
    }

    /* Print the header of the bootstrap range
     */
    private static String[] generateBinRange() {
        String[] binRange = new String[11];  //String representation 0-0, 1-10, 11-20, ..., >90, but0 should never occur
        int index = 0;
        int begin = 0;

        int countArrIndex = MCSample.getBootstrapIndex(0.0);
        for (int i = 0; i <= 100; i++) {
            int newCountArrIndex = MCSample.getBootstrapIndex((double) i / 100.0);

            if (newCountArrIndex > countArrIndex) {
                binRange[index++] = new String((i - 1) + "-" + begin);
                countArrIndex = newCountArrIndex;
                begin = i;
            }
        }
        binRange[index++] = new String(">" + (begin - 1));
        return binRange;
    }

    /* Output the bootstrap count of the defined bin range for each rank
     * ranks are output in the order of a defined rank order
     */
    public static void printBootstrapCountTable(PrintStream out, MCSample sample) throws IOException {
        out.println("sample: " + sample.getSampleName());
        out.println("\t" + "Number of matching assignments out of 100 bootstraps");
        String[] binRange = generateBinRange();
        // print the headers, higher bootstrap bin range first
        out.print("Rank");
        for (int i = binRange.length - 1; i > 0; i--) {
            out.print("\t" + binRange[i]);
        }
        out.println();

        for (String rank : orderedRankList) {
            int[] bootstrapCountArr = (int[]) sample.getBootstrapCountTable().get(rank);
            if (bootstrapCountArr == null) {
                continue; // some rank may not exists
            }
            out.print(rank);

            for (int i = bootstrapCountArr.length - 1; i > 0; i--) {
                out.print("\t" + bootstrapCountArr[i]);
            }
            out.println();
        }

    }
}
