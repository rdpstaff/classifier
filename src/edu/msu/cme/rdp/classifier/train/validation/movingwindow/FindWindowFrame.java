/*
 * FindWindowFrame.java
 *
 * Created on November 15, 2006, 10:53 AM
 */
package edu.msu.cme.rdp.classifier.train.validation.movingwindow;

import java.util.ArrayList;
import java.io.StringReader;
import java.io.IOException;

/**
 *
 * @author  wangqion
 */
public class FindWindowFrame {

    public static final int window_size = 200;
    public static final int step = 25;

    /**
     * Given the aligned E.coli seqstring, find the corresponding model position
     * for each window. The model position and the ecoli position starts with 1
     */
    public ArrayList find(String seqString) throws IOException {
        ArrayList modelPosList = new ArrayList();
        StringReader reader = new StringReader(seqString);
        int c;
        int ecoliPos = 0;
        int modelPos = 0;
        int index = 0;
        int[] ecoli_model = new int[seqString.length()];

        while ((c = reader.read()) != -1) {
            char ch = (char) c;

            if (ch < 'a' || ch == '-') {
                modelPos++;
            }
            if (ch != '-') {
                ecoliPos++;

                // for every ecoli base, count the model position,
                // if lower case, use the next model position.           
                if (ch < 'a') {
                    ecoli_model[index++] = modelPos;
                } else {
                    ecoli_model[index++] = modelPos + 1;
                }
            }
        }

        reader.close();

        for (int i = 0; i <= index / step; i++) {
            //System.err.println("i=" + (i+1) + " " + ecoli_model[i]);
            if (ecoli_model[i] == 0) {
                break;
            }
            int start = i * step;
            int stop = start + window_size - 1;
            if (stop >= index || ecoli_model[stop] == 0) {
                stop = index - 1;
            }
            Window w = new Window(ecoli_model[start], ecoli_model[stop]);
            modelPosList.add(w);
            if (stop == index - 1) {
                break;
            }
        }
        return modelPosList;
    }
}
