/*
 * MainMovingWindow.java
 *
 * Created on November 15, 2006, 11:28 AM
 */

package edu.msu.cme.rdp.classifier.train.validation.movingwindow;

import edu.msu.cme.rdp.classifier.train.LineageSequence;
import edu.msu.cme.rdp.classifier.train.LineageSequenceParser;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import edu.msu.cme.rdp.classifier.train.validation.TreeFactory;


/**
 *
 * @author  wangqion
 */
public class MainMovingWindow {
    TreeFactory factory = null;
    BufferedWriter outWriter = null;
    ArrayList seqList = new ArrayList();
    ArrayList windowFrames = null;
    private static int beginIndex = 0;    
    
     
    /** Creates a new instance of MainMovingWindow */
    public MainMovingWindow(String taxFile, String inFile, String testFile , String outFile, String ecoliFile, int begin, int end, int  min_bootstrap_words) throws IOException{       
        windowFrames = this.getWindowFrame(ecoliFile);
               
        beginIndex = begin;
        
        factory = new TreeFactory(new FileReader(taxFile));            
         // create a tree
        createTree(factory, new File(inFile));                            
        outWriter = new BufferedWriter(new FileWriter(outFile));
        outWriter.write("taxon file: " + taxFile + "\n" + "train sequence file: " + inFile +"\n");        
        
        LineageSequenceParser parser = new LineageSequenceParser(new File(testFile) ); 
        while ( parser.hasNext()){
            seqList.add(parser.next());
        }
        
        outWriter.write("query sequence file: " + testFile + "\n" + "classify moving window of size " + FindWindowFrame.window_size + ", step " + FindWindowFrame.step +"\n");
               
        Iterator windowIt = windowFrames.iterator();
        int windowIndex = 1; 
        try {
        while ( windowIt.hasNext()){
            Window w = (Window) windowIt.next();
            if ( windowIndex>= begin && windowIndex <= end){
                WindowTester tester = new WindowTester(outWriter);                
                System.err.print("**** windowIndex: " + windowIndex  + " model position range: " + w.getStart() + " " + w.getStop() +"\n" );
                tester.classify(factory, seqList, w, windowIndex, min_bootstrap_words);  
            }
            windowIndex ++;
        }
        }catch(RuntimeException e){
            throw e;
        }finally {
            outWriter.close();
        }
    }
    
    public static int getBeginIndex(){
        return beginIndex;
    }
    
    /** reads from the stream, parses the sequence and creates the tree */
    private void createTree(TreeFactory factory, File input) throws IOException{
      long startTime = System.currentTimeMillis(); 
      LineageSequenceParser parser = new LineageSequenceParser(input);     
      
      while ( parser.hasNext() ){
        factory.addSequence( (LineageSequence)parser.next()); 
        
      }   
      //after all the training set is being parsed, calculate the prior probability for all the words.
      factory.calculateWordPrior();
      // create the word occurrence for all the nodes
      factory.getRoot().createWordOccurrenceFromSubclasses();
      
    }
    
    public ArrayList getWindowFrame(String ecoliFile)throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(new File(ecoliFile)));
        String seqString = "";
        String line;
        while ( (line = reader.readLine()) != null){
            seqString += line;
        }
        reader.close();
        
        FindWindowFrame finder = new FindWindowFrame();
        return finder.find(seqString);
        
    }
    
    public static void main(String[] args) throws IOException{
    
        if (args.length < 8) {
            System.out.println("Usage: java MainClassification <tax_file> <source_file> <test_file> <out_file> <ecoli_file> begin end min_bootstrap_words");
            System.exit(-1);
        }
        
        MainMovingWindow main = new MainMovingWindow( args[0], args[1], args[2],args[3], args[4], Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]) );
        
    }
}
