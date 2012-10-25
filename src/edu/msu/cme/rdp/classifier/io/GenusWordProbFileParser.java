/*
 * GenusWordProbFileParser.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 *
 * Created on September 19, 2003, 11:26 AM
 */
package edu.msu.cme.rdp.classifier.io;

import edu.msu.cme.rdp.classifier.GenusWordConditionalProb;
import edu.msu.cme.rdp.classifier.TrainingDataException;
import edu.msu.cme.rdp.classifier.utils.HierarchyVersion;
import java.io.*;
import java.util.StringTokenizer;
import java.util.List;

/**
 * A parser to parse a reader containing a list of the indices of genus nodes and 
 * the conditional probabilities that genus nodes contains the words.
 * @author  wangqion
 */
public class GenusWordProbFileParser {

    /** Reads a file and saves GenusWordConditionalProb objects in a list.
     * Returns the version information for validation purpose.
     * The input file format: integer follows by a tab and float each line.
     * The first value indicates the index of a genus node, the second value
     * indicates the conditional probability that genus node contains the word.
     */
    public static HierarchyVersion createGenusWordProbList(Reader r, List<GenusWordConditionalProb> aList, HierarchyVersion version) throws IOException, TrainingDataException {
        BufferedReader reader = new BufferedReader(r);
        String line = reader.readLine();

        if (line != null) {
            HierarchyVersion thisVersion = new HierarchyVersion(line);
            int trainsetNo = thisVersion.getTrainsetNo();

            if (thisVersion.getVersion() == null) {
                throw new TrainingDataException("Error: There is no version information "
                        + "in the probabilityList file");
            }
            if (version == null) {
                version = thisVersion;
            } else if (!version.getVersion().equals(thisVersion.getVersion()) || version.getTrainsetNo() != thisVersion.getTrainsetNo()) {
                throw new TrainingDataException("Error: The version information in the probabilityList file is different from the version of the other training files.");
            }
        }

        while ((line = reader.readLine()) != null) {

            StringTokenizer st = new StringTokenizer(line, "\t");
            if (st.countTokens() != 2) {
                throw new TrainingDataException("\nError: " + line + " does not have exact two numbers");
            }
            try {
                int genusIndex = Integer.parseInt(st.nextToken());
                float prob = Float.parseFloat(st.nextToken());
                GenusWordConditionalProb genusProb = new GenusWordConditionalProb(genusIndex, prob);
                aList.add(genusProb);
            } catch (NumberFormatException e) {
                reader.close();
                throw new TrainingDataException("\nError: "
                        + "The value for genusIndex or word conditional probability is not a number at line : " + line);
            }
        }
        reader.close();
        return version;
    }
}
