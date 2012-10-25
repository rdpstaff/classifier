/*
 * UncNode.java
 *
 * Created on December 3, 2003, 4:00 PM
 */

package edu.msu.cme.rdp.classifier.comparison;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author  wangqion
 */
public class UncNode extends AbstractNode{
    
    /** Creates a new instance of UncNode */
    UncNode(AbstractNode p)  {
        setParent(p);
    }
    
    
    AbstractNode getFirstChild(){
        return null;
    }
    
    public String getName(){
        return "unclassified " + this.getParent().getName();
    }
    
    public String getRank(){
        return "";
    }
    
    public int getTaxid(){
        return 0 - this.getParent().getTaxid();
    }
    
    public ComparisonBrowserNode findNode( int id ) {
        if ( this.getTaxid() == id ){
            return this;
        }
        
        ComparisonBrowserNode node = this.getNextSibling();
        if (node != null){
            node = node.findNode(id);
        }
        
        return node;
    }
    
    
    public void changeConfidence(SigCalculator cal){
        // the unclassified node is the last one in the children list
        // so simply sum the counts from all the other children
        resetCount();
        AbstractNode sibling = this.getParent().getFirstChild();
        if ( sibling == null){
            return;
        }
        
        int parentS1Count = this.getParent().getS1Count();
        int parentS2Count = this.getParent().getS2Count();
        int childS1Count = 0;
        int childS2Count = 0;
        while (sibling != null){           
            childS1Count += sibling.getS1Count();
            childS2Count += sibling.getS2Count();
            sibling = sibling.getNextSibling();
        }
        
        this.incS1Count(parentS1Count - childS1Count);
        this.incS2Count(parentS2Count - childS2Count);
        this.setSignificance( cal.calculateSig(this.getS1Count(), this.getS2Count()));
        
    }
    
    public Iterator getDetailIterator(float conf){
        DetailIterator it = new DetailIterator(conf);
        return it;
    }
    
    /** An Iterator of the sequences assigned to the node
     */
   
    class DetailIterator implements Iterator {
        
        private Object onDeck;
        private Iterator s1DetailIterator;
        private Iterator s2DetailIterator;
        
        public DetailIterator(float conf){
            Score s1Score = ((TaxonTree)(UncNode.this.getParent())).getFirstS1Score();
            Score s2Score = ((TaxonTree)(UncNode.this.getParent())).getFirstS2Score();
            s1DetailIterator = new SampleDetailIterator(s1Score, conf);
            s2DetailIterator = new SampleDetailIterator(s2Score, conf);
            setNextElement(s1DetailIterator, s2DetailIterator);
        }
        
        public boolean hasNext(){
            return (onDeck != null);
        }
        
        public Object next(){
            Object tmp;
            if (onDeck != null){
                tmp = onDeck;
                setNextElement(s1DetailIterator, s2DetailIterator);
            } else {
                throw new NoSuchElementException();
            }
            return tmp;
        }
        
        
        private void setNextElement(Iterator s1, Iterator s2){
            if ( s1.hasNext()){
                onDeck  = s1.next();
            }else {
                if ( s2.hasNext()){
                    onDeck = s2.next();
                }else {
                    onDeck = null;
                }
            }
            
        }
        
        public void remove() throws UnsupportedOperationException {
        }
    }
    
    
    
    class SampleDetailIterator implements Iterator {
        
        private Object onDeck;
        private Score curScore;
        private float confCutoff;
        
        public SampleDetailIterator(Score s, float conf){
            Score score =  s;
            confCutoff = conf;
            setNextElement(score);
        }
        
        public boolean hasNext(){
            return (onDeck != null);
        }
        
        public Object next(){
            Object tmp;
            if (onDeck != null){
                tmp = onDeck;
                Score score = curScore.getNextSeqScore();
                setNextElement(score);
            } else {
                throw new NoSuchElementException();
            }
            return tmp;
        }
        
        private void setNextElement(Score score){
            while ( score != null){
                if ( score.getScore() >= confCutoff ){
                    Score nextAssignScore = score.getNextAssignScore();
                    if (nextAssignScore != null && nextAssignScore.getScore() < confCutoff){
                        break;
                    }
                }
                score = score.getNextSeqScore();
            }
            if ( score != null){
                onDeck = score.getSeqInfo();
                curScore = score;
            }else{
                onDeck = null;
            }
            
        }
        
        public void remove() throws UnsupportedOperationException {
        }
        
        
    }
    
}
