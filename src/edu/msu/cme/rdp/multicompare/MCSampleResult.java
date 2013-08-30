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

import edu.msu.cme.rdp.classifier.rrnaclassifier.ClassificationParser;
import edu.msu.cme.rdp.classifier.utils.ClassifierFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *
 * @author wangqion
 */
public class MCSampleResult extends MCSample {

    private File sample_resultFile;

    public MCSampleResult(String classicationResultFile) throws IOException {
        super(classicationResultFile);
        sample_resultFile = new File(classicationResultFile);
    }
   
    public MCSampleResult(String classicationResultFile, File dupCountFile) throws IOException {
        super(classicationResultFile, dupCountFile);
        sample_resultFile = new File(classicationResultFile);
    }
    

    public ClassificationParser getClassificationParser(ClassifierFactory classifierFactory) throws FileNotFoundException{
        ClassificationParser parser = new ClassificationParser(sample_resultFile, classifierFactory);
        return parser;
    }

}
