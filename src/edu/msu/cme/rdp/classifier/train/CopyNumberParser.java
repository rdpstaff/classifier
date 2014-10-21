/*
 * Copyright (C) 2014 wangqion
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package edu.msu.cme.rdp.classifier.train;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 *
 * @author wangqion
 */
public class CopyNumberParser {
    public class TaxonCopyNumber{
        String name;
        String rank;
        float meanCopyNumber;
        
        public TaxonCopyNumber(String n, String r, float c){
            name = n;
            rank = r;
            meanCopyNumber = c;
        }
        
        public String getName(){
            return name;
        }
        public String getRank(){
            return rank;
        }
        public float getCopyNumber(){
            return meanCopyNumber;
        }
        
    }
    
    public HashMap<String, TaxonCopyNumber> parse(String cnFile) throws IOException{
        HashMap<String, TaxonCopyNumber> cnMap = new HashMap<String, TaxonCopyNumber>();
        BufferedReader reader = new BufferedReader(new FileReader(cnFile));
        // get the header, find the column indices for the rank, name and mean
        String line = reader.readLine();
        String[] header = line.split("\t");
        int rankcol = -1;
        int namecol = -1;
        int meancol = -1;
        for ( int i = 0; i < header.length; i++){
            if ( header[i].equalsIgnoreCase("name")){
                namecol = i;
            }else if ( header[i].equalsIgnoreCase("rank")){
                rankcol = i;
            }else if ( header[i].equalsIgnoreCase("mean")){
                meancol = i;
            }
        }
        if ( rankcol< 0 || namecol < 0 || meancol < 0){
            throw new IllegalArgumentException("The header line of the copynumber file " + cnFile + " does not have rank, name or mean headers");
        }
                
        while ((line = reader.readLine()) != null) {
            if (line.length() == 0) { // skip the empty line
                continue;
            }
            String[] val = line.split("\t");
            String key = (val[namecol] + val[rankcol]).toLowerCase();
            if ( cnMap.get(key) != null){
                throw new IllegalArgumentException("Duplicate taxon " + val[namecol] + " " + val[rankcol] +  " found in the copynumber file " + cnFile );
            }
            TaxonCopyNumber cn = new TaxonCopyNumber(val[namecol], val[rankcol], Float.parseFloat(val[meancol]));
            cnMap.put(key, cn);
        }
        
        return cnMap;
    }
    
}
