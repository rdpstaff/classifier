/*
 * ParsedRawSequence.java
 * 
 * Copyright 2006 Michigan State University Board of Trustees
 * 
 * Created on June 25, 2002, 10:28 AM
 */
package edu.msu.cme.rdp.classifier.train;

import edu.msu.cme.rdp.readseq.readers.Sequence;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/**
 * A ParsedRawSequence holds the data for a raw sequence.
 * @author  wangqion
 * @version
 */
public class LineageSequence extends Sequence {

    private List<String> ancestors;  // the highest ranked ancestor first if it's the complete lineage, or hold the taxid of the immediate parent taxon
    private static final String marker = "#";

    /** Creates new ParsedRawSequence. */
    public LineageSequence(String n, List<String> al, String seq) {
        super(n, "", seq);
        ancestors = al;
        if(ancestors.isEmpty()) {
            throw new IllegalArgumentException("No lineage data for sequence " + n);
        }
    }

    /**
     * Returns the list of the ancestor taxa, with the highest ranked taxon first.
     */
    public List<String> getAncestors() {
        return ancestors;
    }
  /* partial sequences with good words only
   */
  public GoodWordIterator getPartialSeqIteratorbyGoodBases(int num_good_bases) throws IOException{
    GoodWordIterator wordIterator = null;
    String sequence = super.getSeqString();
    int size = sequence.length();
    if ( size < num_good_bases) {
        return wordIterator;
    }

    double d = Math.random();
    int loc = (int)Math.round((double)(size * d ) );
    String newSeq = sequence.substring(loc,size) + marker + sequence.substring(0,loc);

    int numGoodBases = 0;

    StringReader in = new StringReader(newSeq);

    int offset = 0;

    int c;
    while ( (c = in.read()) != -1 ){
      if ( numGoodBases == num_good_bases){
        break;
      }
      int charIndex = GoodWordIterator.getCharIndex(c);

      if ( charIndex != -1){
        numGoodBases ++;
      }else {
        numGoodBases = 0;
      }

      offset ++;
    }

    in.close();
    if (numGoodBases == num_good_bases){
      String partialSeq = newSeq.substring(offset - num_good_bases, offset);
      //System.err.println(">" + this.getName() + " " + this.getLineage() + "\n" + partialSeq + " NUM_GOOD_BASES=" + numGoodBases);
      wordIterator = new GoodWordIterator(partialSeq);
       if ( wordIterator.getNumofWords() == 0){
            wordIterator = null;
        }
    }

    return wordIterator;

  }
}
