/*
 * GoodWordIterator.java
 *
 * Created on June 26, 2002, 12:27 PM
 */
/**
 *
 * @author  wangqion
 */
package edu.msu.cme.rdp.classifier.train;

import java.io.IOException;
import java.io.StringReader;
import java.util.NoSuchElementException;
import java.util.Random;

public class GoodWordIterator {

    private int curIndex = 0;
    private int numOfWords = 0;
    private int[] wordIndexArr;
    public static final int WORDSIZE = 8;    // the size of a word
    public static final float PERCENT_SELECTION = 0.125f;
    static final int MASK = (1 << (WORDSIZE * 2)) - 1;
    static final int MAX_ASCII = 128;
    private static int[] charIntegerLookup = new int[MAX_ASCII];

    static {
        // initialize the char to integer mapping table
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

    /** Creates a new instance of GoodWordIterator */
    public GoodWordIterator(String seq) throws IOException {
        wordIndexArr = new int[seq.length()];
        createWordIndex(seq);
    }

    public GoodWordIterator(String seq, int wordsLimit) throws IOException {
        wordIndexArr = new int[seq.length()];
        createWordIndex(seq, wordsLimit);
    }

    /** Returns true if the iteration has more good element on deck.
     */
    public boolean hasNext() {
        if (curIndex < numOfWords) {
            return true;
        } else {
            return false;
        }
    }

    /** Returns the next good element in the iteration. */
    public int next() throws NoSuchElementException {
        int tmp;
        if (hasNext()) {
            tmp = curIndex;
            curIndex++;
            return wordIndexArr[tmp];
        } else {
            throw new NoSuchElementException();
        }
    }

    /** Fetchs every overlapping word, change to integer and save in an array
     */
    private void createWordIndex(String seq) throws IOException {
        StringReader in = new StringReader(seq);

        int count = 0;
        int wordIndex = 0;
        int charIndex = 0;
        int c;
        while ((c = in.read()) != -1) {

            charIndex = charIntegerLookup[c];

            if (charIndex == -1) {
                wordIndex = 0;
                count = 0;

            } else {
                count++;
                wordIndex <<= 2;
                wordIndex = wordIndex & (MASK);
                wordIndex = wordIndex | charIndex;

                if (count == WORDSIZE) {
                    wordIndexArr[numOfWords] = wordIndex;
                    numOfWords++;
                    count--;
                }
            }
        }
        in.close();
    }

    /** Fetchs a certain number of overlapping word, change to integer and save in an array
     * this is for partial sequence testing only
     */
    private void createWordIndex(String seq, int wordsLimit) throws IOException {
        StringReader in = new StringReader(seq);

        int count = 0;
        int wordIndex = 0;
        int charIndex = 0;
        int c;
        while ((c = in.read()) != -1) {
            if (numOfWords >= wordsLimit) {
                break;
            }
            charIndex = charIntegerLookup[c];

            if (charIndex == -1) {
                wordIndex = 0;
                count = 0;

            } else {
                count++;
                wordIndex <<= 2;
                wordIndex = wordIndex & (MASK);
                wordIndex = wordIndex | charIndex;

                if (count == WORDSIZE) {
                    wordIndexArr[numOfWords] = wordIndex;
                    numOfWords++;
                    count--;
                }
            }
        }
        in.close();
    }

    public int getWordsize() {
        return WORDSIZE;
    }

    public int getMask() {
        return MASK;
    }

    public int getNumofWords() {
        return numOfWords;
    }

    public void resetCurIndex() {
        curIndex = 0;
    }

    public static int getCharIndex(int c) {
        return charIntegerLookup[c];
    }
    
    public int[] getWordArr(){
        if (this.getNumofWords() == 0) {
            throw new IllegalStateException("Error: The sequence contains 0 valid words");
        }
        int[] wordList = new int[this.getNumofWords()];
        this.resetCurIndex();
        int num = 0;
        while (this.hasNext()) {
            wordList[num++] = this.next();
        }
        return wordList;
    }
    
    
    public static int[] getRdmWordArr(int[] wordList, int min_bootstrap_words , Random randomGenerator) {

        // choose at one-eighth of the words or the min_bootstrap_words
        int numOfSelection = Math.max((int) (wordList.length * PERCENT_SELECTION), min_bootstrap_words);
        int[] testWordList = new int[numOfSelection];

        for (int i = 0; i < numOfSelection; i++) {
            int randomIndex = randomGenerator.nextInt(wordList.length);
            testWordList[i] = wordList[randomIndex];
        }
        return testWordList;
    }
    
}
