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
import java.io.StringReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * A Sequence containing the sequence information.
 *
 * @author wangqion
 * @version
 */
public class ClassifierSequence extends Sequence {

    /**
     * The number of rna bases (ATGC). Initially set to 4.
     */
    public static final int RNA_BASES = 4;
    /**
     * The size of a word. Initially set to 8.
     */
    public static final int WORDSIZE = 8;
    /**
     * The mask for converting a string to integer
     */
    public static final int MASK = (1 << (WORDSIZE * 2)) - 1;
    private boolean reverse = false;
    private Integer goodWordCount = null; // the number of words with only valid bases
    private static final int MAX_ASCII = 128;
    private final static int[] charIntegerLookup = new int[MAX_ASCII];
    private final static int[] intComplementLookup = new int[RNA_BASES];

    static {
        // initialize the integer complement look up table
        intComplementLookup[0] = 1;
        intComplementLookup[1] = 0;
        intComplementLookup[2] = 3;
        intComplementLookup[3] = 2;

        // initializes the char to integer mapping table
        for (int i = 0; i < MAX_ASCII; i++) {
            charIntegerLookup[i] = -1;
        }
        charIntegerLookup['A'] = 0;
        charIntegerLookup['U'] = 1;
        charIntegerLookup['T'] = 1;
        charIntegerLookup['G'] = 2;
        charIntegerLookup['C'] = 3;

        charIntegerLookup['a'] = 0;
        charIntegerLookup['u'] = 1;
        charIntegerLookup['t'] = 1;
        charIntegerLookup['g'] = 2;
        charIntegerLookup['c'] = 3;
    }

    /**
     * Creates new ParsedSequence.
     */
    public ClassifierSequence(Sequence seq) {
        this(seq.getSeqName(), seq.getDesc(), seq.getSeqString());
    }

    public ClassifierSequence(String seqName, String desc, String seqString) {
        super(seqName, desc, SeqUtils.getUnalignedSeqString(seqString));
    }

    /**
     * Sets the sequence string.
     */
    protected void setSeqString(String s) {
        seqString = s;
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
    public ClassifierSequence getReversedSeq() {
        ClassifierSequence retval = new ClassifierSequence(seqName, desc, IUBUtilities.reverseComplement(seqString));
        retval.reverse = true;
        return retval;
    }

    /**
     * Returns the reverse complement of the word in an integer array format.
     */
    public static int[] getReversedWord(int[] word) {
        int length = word.length;
        int[] reverseWord = new int[length];
        for (int w = 0; w < length; w++) {
            reverseWord[length - 1 - w] = intComplementLookup[ word[w]];
        }
        return reverseWord;
    }

    /**
     * Returns an integer representation of a single word.
     */
    public static int getWordIndex(int[] word) {
        int wordIndex = 0;
        for (int w = 0; w < word.length; w++) {
            wordIndex <<= 2;
            wordIndex = wordIndex & (MASK);
            wordIndex = wordIndex | word[w];
        }
        return wordIndex;
    }

    /**
     * Fetches every overlapping word from the sequence string, changes each
     * word to integer format and saves in an array.
     */
    public int[] createWordIndexArr() {
        int[] wordIndexArr = new int[this.seqString.length()];
        createWordIndexArr(wordIndexArr);
        return wordIndexArr;
    }

    /**
     * Fetches every overlapping word from the sequence string, changes each
     * word to integer format and saves in an array.
     */
    public void createWordIndexArr(int[] wordIndexArr) {
        if (wordIndexArr.length < this.seqString.length()) {
            throw new IllegalArgumentException("wordIndexArr buffer doesn't have enough room in it");
        }
        Arrays.fill(wordIndexArr, -1);

        int wordCount = 0;  // number of good words in a query sequence
        int count = 0;
        int wordIndex = 0;
        int charIndex;

        for (char c  : seqString.toCharArray()) {
            if (c > 0 && c < 128) {
                charIndex = charIntegerLookup[c];
            } else {
                charIndex = -1;
            }

            if (charIndex == -1) {
                wordIndex = 0;
                count = 0;
            } else {
                count++;
                wordIndex <<= 2;
                wordIndex = wordIndex & (MASK);
                wordIndex = wordIndex | charIndex;

                if (count == WORDSIZE) {
                    wordIndexArr[wordCount] = wordIndex;
                    wordCount++;
                    count--;
                }
            }
        }
        this.goodWordCount = wordCount;
    }

    /**
     * Returns the number of words with valid bases.
     */
    public int getGoodWordCount() {
        if (goodWordCount == null) {
            this.createWordIndexArr();
        }
        return goodWordCount;
    }
}
