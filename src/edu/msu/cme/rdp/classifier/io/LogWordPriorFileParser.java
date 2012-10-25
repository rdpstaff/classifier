/*
 * LogWordPriorFileParser.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 * 
 * Created on September 18, 2003, 5:38 PM
 */
package edu.msu.cme.rdp.classifier.io;

import edu.msu.cme.rdp.classifier.TrainingDataException;
import edu.msu.cme.rdp.classifier.utils.HierarchyVersion;
import java.io.*;
import java.util.StringTokenizer;

/**
 * A parser to parse a read containing a list of the indices of words
 *  and the log value of word prior probabilities.
 * @author  wangqion
 */
public class LogWordPriorFileParser {

    /** Reads a file and saves the log word priors in an array.
     * Returns the version information for validation purpose.
     * Input file format: integer follows by a tab and float each line.
     * The first value indicates the index of a word, the second value
     * indicates the log value of word prior probability.
     */
    public static HierarchyVersion createLogWordPriorArr(Reader r, float[] arr, HierarchyVersion version) throws IOException, TrainingDataException {
        BufferedReader reader = new BufferedReader(r);
        String line = reader.readLine();
        if (line != null) {

            HierarchyVersion thisVersion = new HierarchyVersion(line);
            int trainsetNo = thisVersion.getTrainsetNo();

            if (thisVersion.getVersion() == null) {
                throw new TrainingDataException("Error: There is no version information "
                        + "in the wordPrior file");
            }
            if (version == null) {
                version = thisVersion;
            } else if (!version.getVersion().equals(thisVersion.getVersion()) || version.getTrainsetNo() != thisVersion.getTrainsetNo()) {
                throw new TrainingDataException("Error: The version information in the wordPrior file is different from the version of the other training files.");
            }
        }


        while ((line = reader.readLine()) != null) {

            StringTokenizer st = new StringTokenizer(line, "\t");
            if (st.countTokens() != 2) {
                throw new TrainingDataException("\nError: " + line + " does not have exact two numbers");
            }
            try {
                int wordIndex = Integer.parseInt(st.nextToken());
                float logWordPrior = Float.parseFloat(st.nextToken());
                arr[wordIndex] = logWordPrior;
            } catch (NumberFormatException e) {
                reader.close();
                throw new TrainingDataException("\nError: "
                        + "The value for wordIndex or word prior is not a number at line : " + line);
            }
        }
        reader.close();
        return version;
    }
}
