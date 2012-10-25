/*
 * Score.java
 *
 * Created on November 10, 2003, 5:24 PM
 */

package edu.msu.cme.rdp.classifier.comparison;

/**
 *
 * @author  wangqion
 */
public class Score {
  
  private float score;
  private Taxon node;
  private Score nextSeqScore; // the score of the next sequence at current rank
  private Score nextAssignScore; // the score of the same sequence at the next lower rank
  private SeqInfo seqInfo;
  
  /** Creates a new instance of Score */
  public Score( float s, SeqInfo seq, Taxon n) {
    score = s;
    seqInfo = seq;
    seqInfo.addScore(this);
    node = n;
  }
  
  Taxon getTaxon(){
    return node;
  }
  
  SeqInfo getSeqInfo(){
    return seqInfo;
  }
  
  Score getNextSeqScore(){
    return nextSeqScore;
  }
  
  Score getNextAssignScore(){
    return nextAssignScore;
  }
  
  void setNextSeqScore(Score s){
    nextSeqScore = s;
  }
  
  void setNextAssignScore(Score s){
    if (nextAssignScore == null){
      nextAssignScore = s;
      return;
    }    
    nextAssignScore.setNextAssignScore(s);
  }
  
  
  public float getScore(){
    return score;
  }
  
  public String getName(){
    return node.getName();
  }
  
  public String getRank(){
    return node.getRank();
  }
  
  public int getTaxid(){
    return node.getTaxid();
  }
  
  
}
