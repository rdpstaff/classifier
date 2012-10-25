/*
 * ProbIndexFileParser.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 *
 * Created on September 19, 2003, 10:41 AM
 */
package edu.msu.cme.rdp.classifier.io;

import edu.msu.cme.rdp.classifier.TrainingDataException;
import edu.msu.cme.rdp.classifier.utils.HierarchyVersion;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.StringTokenizer;

/**
 * A parser to parser a reader contains a list of indices of words,
 * and start indices of the conditional probabilities.
 * @author  wangqion
 */
public class ProbIndexFileParser {

    /** Reads a file and saves the start index of conditional probability of the genera containing the word to an array.
     * Returns the version information for validation purpose.
     * The input file format: integer follows by a tab and integer each line
     * The first number indicates the index of a word, the second number
     * indicates the start index of conditional probability of the genera containing that word
     * in the list genus_wordConditionalProbList.
     */
    public static HierarchyVersion createProbIndexArr(Reader r, int[] arr, HierarchyVersion version) throws IOException, TrainingDataException {
        BufferedReader reader = new BufferedReader(r);
        String line = reader.readLine();

        if (line != null) {
            HierarchyVersion thisVersion = new HierarchyVersion(line);
            int trainsetNo = thisVersion.getTrainsetNo();

            if (thisVersion.getVersion() == null) {
                throw new TrainingDataException("Error: There is no version information "
                        + "in the probabilityIndex file");
            }
            if (version == null) {
                version = thisVersion;
            } else if (!version.getVersion().equals(thisVersion.getVersion()) || version.getTrainsetNo() != thisVersion.getTrainsetNo()) {
                throw new TrainingDataException("Error: The version information in the probabilityIndex file is different from the version of the other training files.");
            }
        }

        while ((line = reader.readLine()) != null) {

            StringTokenizer st = new StringTokenizer(line, "\t");
            if (st.countTokens() != 2) {
                throw new TrainingDataException("\nError: " + line + " does not have exact two numbers");
            }
            try {
                int wordIndex = Integer.parseInt(st.nextToken());
                int start = Integer.parseInt(st.nextToken());
                arr[wordIndex] = start;
            } catch (NumberFormatException e) {
                reader.close();
                throw new TrainingDataException("\nError: "
                        + "The value for wordIndex or start position is not a number at line : " + line);
            }
        }
        reader.close();
        return version;
    }
}
