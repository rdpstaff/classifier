/*
 * Taxonomy.java
 *
 * Created on August 21, 2002, 5:14 PM
 */

package edu.msu.cme.rdp.classifier.train.validation;

/**
 *
 * @author  wangqion
 */
public class Taxonomy {
    public static final String GENUS = "GENUS";
    
  int taxID;    // the taxonomy id
  int parentID; // the parent taxonomy id
  int depth;    // the depth from the root.
  String hierLevel; // the hierarchy level
  String name;
  String lineage;
  
  /** Creates a new instance of Taxonomy */
   public Taxonomy(int id, String name, int pid, int depth, String level) {
    taxID = id;
    this.name = name;
    parentID = pid;
    hierLevel = level;
    this.depth = depth;  
  }
   
    
  public int getTaxID(){
    return taxID;
  }
  
  public int getParentID(){
    return parentID;
  }
  
  public void setParentID(int p){
    parentID = p;
  }
  
  
  public int getDepth(){
    return depth;
  }
  
  public void setDepth(int d){
    depth = d;
  }
  
  public String getHierLevel(){
    return hierLevel;
  }
  
  public String getName(){
    return name; 
  }
  
  public String getLineage(){
    return lineage;
  }
  
  public void setLineage(String l){
    lineage = l;    
  }
}
