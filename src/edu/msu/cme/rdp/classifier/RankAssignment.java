/*
 * RankAssignment.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 * 
 * Created on June 25, 2002, 11:35 AM
 */

package edu.msu.cme.rdp.classifier;

/**
 * A RankAssignment holds the classification result of a sequence at one rank level.
 * @author  wangqion
 * @version
 */
public class RankAssignment {
  
  //the HierarchyTree which the query sequence is assigned to
  private HierarchyTree bestClass;
  private float confidence;  // percent of voters which agrees with this assignment
  
  /** Creates new RankAssignment. */
  public RankAssignment(HierarchyTree t, float c) {
    bestClass = t;
    confidence = c;
  }
  
  /** Returns the estimate of confidence of the assignment.
   * The number of times a taxon was selected out of 
   * the total number of bootstrap trials was used as an estimate of confidence
   * in the assignment to that treenode.
   */
  public float getConfidence(){
    return confidence;
  }
  
  /** Returns the HierarchyTree which the query sequence is assigned to.
   */
  public HierarchyTree getBestClass(){
    return bestClass;
  }
  
  /** Returns the name of the assigned HierarchyTree.
   */
  public String  getName(){
    if (bestClass != null){
      return bestClass.getName();
    }else{
      throw new IllegalStateException("Error: Can not get the name " +
      "of the taxonomy node. The bestClass is null");
    }
  }
  
  /** Returns the rank of the assigned HiearchyTree.
   */
  public String getRank(){
    if (bestClass != null){
      return bestClass.getRank();
    }else{
      throw new IllegalStateException("Error: Can not get the rank " +
      "of the taxonomy node. The bestClass is null");
    }
  }
  
  /** Returns the taxid of the assigned HierachyTree
   */
  public int getTaxid(){
    if (bestClass != null){
      return bestClass.getTaxid();
    }else{
      throw new IllegalStateException("Error: Can not get the taxid "
      +" of the taxonomy node. The bestClass is null");
    }
  }
  
  /** Set the confidence of the assignment.
   */
  void setConfidence(float f){
    confidence = f;
  }
  
  
}
