/*
 * SeqInfo.java
 *
 * Created on November 10, 2003, 5:25 PM
 */

package edu.msu.cme.rdp.classifier.comparison;

import java.util.Iterator;
import java.util.NoSuchElementException;
/**
 *
 * @author  wangqion
 */
public class SeqInfo {
  private String name;
  private String title;
  private Score firstScore;  // the score of the assignment for the root rank
  private boolean reverse = false;
  
  /** Creates a new instance of SeqInfo */
  public SeqInfo(String n, String t) {
    name = n;
    title = t;
  }
  
  public String getName(){
    return name;
  }
  
  public String getTitle(){
    return title;
  }
  
  public boolean isReverse(){
    return reverse;
  }
  
  void setReverse(boolean b){
    reverse = b;
  }
  
  Score getFirstScore(){
    return firstScore;
  }
  
  
  
  void addScore(Score s){
    if (firstScore == null){
      firstScore = s;
      return;
    }
    firstScore.setNextAssignScore(s);
  }
  
  public Iterator getScoreList(){
    ScoreIterator it = new ScoreIterator();
    return it;
  }
  
  /** An Iterator of scores of assignments sequence
   */
  class ScoreIterator implements Iterator{
    public ScoreIterator(){
      onDeck = firstScore;
    }
    
    public boolean hasNext(){
      return (onDeck != null);
    }
    
    public Object next(){
      Object tmp;
      if (onDeck != null){
        tmp = onDeck;
        onDeck = ((Score)onDeck).getNextAssignScore();
      } else {
        throw new NoSuchElementException();
      }
      return tmp;
    }
    
    public void remove() throws UnsupportedOperationException {
    }
    
    private Object onDeck;
  }
  
  
}
