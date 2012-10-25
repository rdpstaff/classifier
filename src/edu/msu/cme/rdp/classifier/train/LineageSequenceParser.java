/*
 * RawSequenceParser.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 * 
 * Created on June 24, 2002, 2:13 PM
 */
package edu.msu.cme.rdp.classifier.train;

import edu.msu.cme.rdp.readseq.readers.Sequence;
import edu.msu.cme.rdp.readseq.readers.SequenceReader;
import java.util.*;
import java.io.*;

/**
 * A parser to parse a reader containing the raw sequences.
 * @author  wangqion
 * @version
 */
public class LineageSequenceParser {

    private SequenceReader seqReader;
    public static final String delimiter = ";";
    private LineageSequence onDeck;
    private LineageSequence curSeq = null;

    /** Creates new RawSequenceParser to parse the input fasta file. */
    public LineageSequenceParser(File inFile) throws IOException {
        seqReader = new SequenceReader(inFile);
    }
    
    /** Creates new RawSequenceParser to parse the input fasta file. */
    public LineageSequenceParser(InputStream is) throws IOException {
        seqReader = new SequenceReader(is);
    }

    /**
     * Closes the reader.
     */
    public void close() throws IOException {
        seqReader.close();
    }

    /** Returns true if there is a parsed sequence available.
     */
    public boolean hasNext() throws IOException {
        if (onDeck != null) {
            return true;
        }
        if ((onDeck = getNextElement()) != null) {
            return true;
        }
        return false;
    }

    /** Returns the next parsed sequence. */
    public LineageSequence next() throws NoSuchElementException, IOException {
        LineageSequence tmp;
        if (onDeck != null) {
            tmp = onDeck;
            onDeck = null;
        } else {
            tmp = getNextElement();
        }
        if (tmp == null) {
            throw new NoSuchElementException();
        }

        return tmp;
    }

    /** Reads from the input stream and returns a parsed sequence.
     * Header format: seqID followed by a tab, followed by a list of ancestor nodes
     * Reads one line for the header and decompose the header into
     * a list of ancestors. Then reads the following lines for the sequence string
     * and modifies the sequence string.
     */
    private LineageSequence getNextElement() throws IOException {
        LineageSequence nextSeq = null;

        String seqstring = "";
        boolean endoffile = true;
        boolean origin = false;

        Sequence seq = seqReader.readNextSequence();
        if (seq == null) {
            return null;
        }

        curSeq = new LineageSequence(seq.getSeqName(), decomposeHeader(seq.getDesc()), modifySequence(seq.getSeqString()));

        LineageSequence retval = curSeq;
        curSeq = nextSeq;

        return retval;
    }

    /** Takes two different formats:
     * the old format is a string of sequence header( ancestors seperated by delimiter, such as ";" in our case).
     * new format: the taxid of the immediate parent taxon
     * It returns an array of ancestors with root ancestor first.
     */
    private List<String> decomposeHeader(String s) {
        List<String> al = new ArrayList();

        String[] values = s.split(delimiter);

        for (int i = 0; i < values.length; i++) {
            al.add(values[i].trim());
        }
        return al;
    }

    /** Modifies the sequence. Removes - and ~. It returns a string.
     */
    private String modifySequence(String s) {
        try {
            StringReader in = new StringReader(s);
            StringWriter out = new StringWriter();
            int c;
            while ((c = in.read()) != -1) {
                if (c == '-' || c == '~') {
                    continue;
                }
                out.write(c);
            }
            in.close();
            out.close();
            return out.toString();
        } catch (IOException e) {
            System.out.println("In StringReader or StringWriter exception : " + e.getMessage());
        }
        return null;
    }
}
