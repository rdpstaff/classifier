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

package edu.msu.cme.rdp.classifier.rrnaclassifier;

import edu.msu.cme.rdp.classifier.HierarchyTree;
import edu.msu.cme.rdp.classifier.ClassificationResult;
import edu.msu.cme.rdp.classifier.RankAssignment;
import edu.msu.cme.rdp.classifier.utils.ClassifierFactory;
import edu.msu.cme.rdp.classifier.utils.ClassifierSequence;
import edu.msu.cme.rdp.classifier.utils.HierarchyVersion;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fishjord
 */
public class ClassificationParser {

    private BufferedReader reader;
    private static HierarchyTree root;   // the root is the root of trained Classifier, should match the root of the classification results
    private static HierarchyVersion versionNo;

    public ClassificationParser(String fileName, ClassifierFactory classifierFactory) throws FileNotFoundException {
        this(new FileInputStream(fileName), classifierFactory);
    }

    public ClassificationParser(File f, ClassifierFactory classifierFactory) throws FileNotFoundException {
        this(new FileInputStream(f), classifierFactory);

    }

    public ClassificationParser(InputStream is, ClassifierFactory classifierFactory) {
        root = classifierFactory.getRoot();
        versionNo = classifierFactory.getHierarchyTrainsetNo();
        this.reader = new BufferedReader(new InputStreamReader(is));
    }

    public ClassificationParser(Reader reader) {
        if(reader instanceof BufferedReader)
            this.reader = (BufferedReader) reader;
        else
            this.reader = new BufferedReader(reader);
    }

    public void close() throws IOException {
        reader.close();
    }

    
    public HierarchyTree getRoot(){
        return root;
    }
    
    public ClassificationResult next() throws IOException {
        String line = reader.readLine();
        if(line == null)
            return null;

        String delimiter = "\t";  // delimiter can be either tab or ;
        if ( line.contains(";")) {
            delimiter= ";";
        }
        
        String [] tokens = line.split(delimiter);
        List<RankAssignment> assignments = new ArrayList();
        ClassifierSequence seq = new ClassifierSequence(tokens[0], "", "");
        
        boolean seqreversed = (tokens[1].equalsIgnoreCase("-"));

        if ( !root.getName().equalsIgnoreCase(tokens[2].trim()) || !root.getRank().equalsIgnoreCase(tokens[3].trim())) {
            throw new IllegalArgumentException("Root of \"" + line + "\" does not match the Root node in the original Classifier training data: " + root.getName() + "\t" + root.getRank());
        }
        
        HierarchyTree currRoot = root;

        assignments.add(new RankAssignment(currRoot, Float.valueOf(tokens[4].trim())));

        for(int index = 5;index < tokens.length;index += 3) {
            String nextName = tokens[index].trim();
            boolean found = false;
            for(HierarchyTree tree : (List<HierarchyTree>)currRoot.getSubclasses()) {
                if(tree.getName().equals(nextName)) {
                    currRoot = tree;
                    assignments.add(new RankAssignment(currRoot, Float.valueOf(tokens[index + 2].trim())));
                    found = true;
                    break;
                }
            }
            if ( !found){
                throw new IllegalArgumentException("taxon Node " + nextName + " in line \"" + line + "\" is not found in the original Classifier training data.");
            }
        }
        return new ClassificationResult(seq, seqreversed, assignments, versionNo);
    }

}
