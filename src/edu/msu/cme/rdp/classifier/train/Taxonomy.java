/**
 * Taxonomy.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 * Created on August 21, 2002, 5:14 PM
 */

package edu.msu.cme.rdp.classifier.train;

/**
 * A Taxonomy holds the raw taxon information.
 * @author  wangqion
 */
public class Taxonomy {
  int taxID;    // the taxonomy id
  String taxName; // the taxon name
  int parentID; // the parent taxonomy id
  int depth;    // the depth from the root.
  String hierLevel; // the hierarchy rank level
  
  /** Creates a new instance of Taxonomy */
  public Taxonomy(int id, String name, int pid, int d, String level) {
    taxID = id;
    taxName = name;
    parentID = pid;
    depth = d;
    hierLevel = level;
  }
  
  /**
   * Returns the taxID.
   */
  public int getTaxID(){
  	return taxID;
  }
  
  /**
   * Returns the taxon name.
   */
  public String getTaxName(){
  	return taxName;
  }
  
  
  /**
   * Returns the taxID of the parent taxon.
   */
  public int getParentID(){
  	return parentID;
  }
  
  /**
   * Returns the depth of the current taxon from root taxon.
   */
  public int getDepth(){
  	return depth;
  }
  
  /**
   * Returns the hierarchy rank level.
   */
  public String  getHierLevel(){
  	return hierLevel;
  }
  
  
  public void setDepth(int d){
	  depth =d ;
  }
  
  public void changeRank(String s){
	  hierLevel = s;
  }
}
