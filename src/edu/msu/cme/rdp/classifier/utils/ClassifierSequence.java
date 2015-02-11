/*
 * ParsedSequence.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 *
 * Created on November 7, 2003, 5:46 PM
 */
package edu.msu.cme.rdp.classifier.utils;

import edu.msu.cme.rdp.readseq.readers.Sequence;
import edu.msu.cme.rdp.readseq.utils.IUBUtilities;
import edu.msu.cme.rdp.readseq.utils.SeqUtils;
import edu.msu.cme.rdp.readseq.utils.orientation.GoodWordIterator;
import java.io.IOException;

/**
 * A Sequence containing the sequence information.
 *
 * @author wangqion
 * @version
 */
public class ClassifierSequence extends Sequence {
    private boolean reverse = false;
    private Integer goodWordCount = null; // the number of words with only valid bases
    private int [] wordIndexArr = null; 
    /**
     * Creates new ParsedSequence.
     */
    public ClassifierSequence(Sequence seq) throws IOException{
        this(seq.getSeqName(), seq.getDesc(), seq.getSeqString());
    }

    public ClassifierSequence(String seqName, String desc, String seqString) throws IOException {
        super(seqName, desc, SeqUtils.getUnalignedSeqString(seqString));
        /**
        * Fetches every overlapping word from the sequence string, changes each
        * word to integer format and saves in an array.
        */
        GoodWordIterator iterator = new GoodWordIterator(this.getSeqString());
        this.wordIndexArr = iterator.getWordArr();        
        this.goodWordCount = wordIndexArr.length;
    }

    /**
     * Sets the sequence string.
     */
    protected void setSeqString(String s) {
        seqString = s;
    }

    public int[] getWordIndexArr(){
        return this.wordIndexArr;
    }
    /**
     * Returns true if the sequence string is a minus strand.
     */
    public boolean isReverse() {
        return reverse;
    }

    /**
     * Returns a Sequence object whose sequence string is the reverse complement
     * of the current rRNA sequence string.
     */
    public ClassifierSequence getReversedSeq() throws IOException {
        ClassifierSequence retval = new ClassifierSequence(seqName, desc, IUBUtilities.reverseComplement(seqString));
        retval.reverse = true;
        return retval;
    }
    
    /**
     * Returns the number of words with valid bases.
     */
    public int getGoodWordCount() {
        return goodWordCount;
    }
}
