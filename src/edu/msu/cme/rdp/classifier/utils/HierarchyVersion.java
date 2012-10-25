/*
 * HierarchyVersion.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 * 
 * Created on November 13, 2003, 11:26 AM
 */

package edu.msu.cme.rdp.classifier.utils;
import edu.msu.cme.rdp.classifier.TrainingDataException;
import java.util.regex.*;

/**
 * A HierarchyVersion holds the training set number and version of the taxonomy.
 * @author  wangqion
 */
public class HierarchyVersion {
  
  private final static String regex="<trainsetNo>(.*)</trainsetNo><version>(.*)</version>";
  private Matcher matcher;
  private static Pattern versionPattern;
  private String version ;
  private int trainsetNo;
  
  static {
  	 try{
        versionPattern = Pattern.compile(regex);
      } catch(PatternSyntaxException pse){
        System.out.println("There is a problem with the regular expression! " + regex);
      }
  }
  
  /** Creates a new instance of HierarchyVersion, */
  public HierarchyVersion(String doc) throws TrainingDataException{
  	if ( doc != null){
	  	matcher = versionPattern.matcher(doc);
	  	try {
		    if (matcher.find()){
		    		trainsetNo = Integer.parseInt(matcher.group(1).trim());
				version = matcher.group(2);
		    }
	  	} catch (NumberFormatException ex){
	  		throw new TrainingDataException("Error: The trainsetNo should be integer in the header:" + doc);
	  	}
  	}
  }
  
  /** Returns the training set number of the taxonomy hierarchy from the header of the training file.
   */
  public int getTrainsetNo(){    
    return trainsetNo;
  }
  
  /** Returns the version of the taxonomy hierarchy from the header of the training file.
   */
  public String getVersion(){    
    return version;
  }
  
  
  public static void main(String[] args) throws TrainingDataException{
    String doc = "<trainsetNo>1</trainsetNo><version>Bergey's Manual of Systematic Bacteriology v_3_mod_1</version><file>bergeyTree</file>";
    HierarchyVersion version = new HierarchyVersion(doc);
    String v = version.getVersion(); 
    int trainsetNo = version.getTrainsetNo();
    System.out.println("version =" + v +"*" +  " trainset no = " + trainsetNo + "*");
    
  }
  
}
