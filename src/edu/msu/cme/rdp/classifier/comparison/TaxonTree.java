/*
 * TaxonTree.java
 *
 * Created on November 7, 2003, 5:22 PM
 */

package edu.msu.cme.rdp.classifier.comparison;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author  wangqion
 */
public class TaxonTree extends AbstractNode{
    private int taxid;
    private String name;
    private String rank;
    
    private AbstractNode firstChild;    
    
    private Score firstS1Score;
    private Score lastS1Score;
    private Score firstS2Score;
    private Score lastS2Score;
    
    /** Creates a new instance of TaxonTree */
    public TaxonTree(int id, String name, String rank, AbstractNode p) {
        taxid = id;
        this.name = name;
        this.rank = rank;
        setParent(p);
    }
    
    
    public Iterator getDetailIterator(float conf){
        DetailIterator it = new DetailIterator(conf);
        return it;
    }
    
    public String getName(){
        return name;
    }
    
    public String getRank(){
        return rank;
    }
    
    public int getTaxid(){
        return taxid;
    }
    
    
    AbstractNode getFirstChild(){
        return firstChild;
    }
    
    Score getFirstS1Score(){
        return firstS1Score;
    }
    Score getFirstS2Score(){
        return firstS2Score;
    }
    
    private Score getLastS1Score(){
        return lastS1Score;
    }
    private Score getLastS2Score(){
        return lastS2Score;
    }
    
    // the last node is the unclassified node
    // each time add one child node to the begining of the children nodes
    protected void addChild(AbstractNode n){
        if (firstChild == null){
            UncNode unc_child = new UncNode(this);
            firstChild = unc_child;
        }
        
        AbstractNode temp = firstChild;
        firstChild = n;       
        firstChild.addSibling(temp);             
    }
    
    private void setFirstS1Score(Score s){
        firstS1Score = s;
        lastS1Score = s;
    }
    
    private void setFirstS2Score(Score s){
        firstS2Score = s;
        lastS2Score = s;
    }
    
    protected void addS1Score(Score s){
        incS1Count(1);
        if (lastS1Score == null){
            this.setFirstS1Score(s);
            return;
        }
        lastS1Score.setNextSeqScore(s);
        lastS1Score  = s;
    }
    
    protected void addS2Score(Score s){
        incS2Count(1);
        if (lastS2Score == null){
            this.setFirstS2Score(s);
            return;
        }
        lastS2Score.setNextSeqScore(s);
        lastS2Score  = s;
    }
    
    /** Given a assignment, first search the asigned node among the children,
     * if the child does not exist, create a new child node.
     */
    TaxonTree getChildbyTaxid(int taxid, String name, String rank){
        AbstractNode child = firstChild;
        boolean found = false;
        
        while(child != null){
            if ( child.getTaxid() == taxid){
                found = true;
                break;
            }
            child = child.getNextSibling();
        }
        
        if ( !found){
            child = new TaxonTree(taxid, name, rank, this);
            this.addChild(child);
        }
        
        return (TaxonTree)child;
    }
    
    /** calculate the count of sequences assigned to the node, only count
     *  the sequence with score for that node greater or equal to confidence
     *  cutoff value
     */
    public void changeConfidence(SigCalculator cal){
        
        resetCount();
        Score s1Score = firstS1Score;
        while (s1Score != null){
            if (s1Score.getScore() >= cal.getConfidence()){
                incS1Count(1);
            }
            s1Score = s1Score.getNextSeqScore();
        }
        
        Score s2Score = firstS2Score;
        while (s2Score != null){
            if (s2Score.getScore() >= cal.getConfidence()){
                incS2Count(1);
            }
            s2Score = s2Score.getNextSeqScore();
        }
        this.setSignificance( cal.calculateSig(this.getS1Count(), this.getS2Count()));
        
        if (firstChild != null){
            firstChild.changeConfidence(cal);
        }
        
        if ( this.getNextSibling() != null){
            this.getNextSibling().changeConfidence(cal);
        }
    }
    
    /** returns a node with the given taxid if found. returns null if not found.
     */
    public ComparisonBrowserNode findNode( int id ) {
        if ( this.taxid == id ){
            return this;
        }
        
        ComparisonBrowserNode node = firstChild;
        if ( node != null ){
            node =  node.findNode(id);
            if (node != null){
                return node;
            }
        }
        
        node = this.getNextSibling();
        if (node != null){
            node = node.findNode(id);
        }
        
        return node;
    }
    
    
    
    /** An Iterator of the sequences assigned to the node
     */
    class DetailIterator implements Iterator {
        
        private Object onDeck;
        private Iterator s1DetailIterator;
        private Iterator s2DetailIterator;
        
        public DetailIterator(float conf){
            s1DetailIterator = new SampleDetailIterator(TaxonTree.this.firstS1Score, conf);
            s2DetailIterator = new SampleDetailIterator(TaxonTree.this.firstS2Score, conf);
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
                if ( score.getScore() >= confCutoff){
                    break;
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
