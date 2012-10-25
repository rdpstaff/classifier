/*
 * AbstractNode.java
 *
 * Created on December 3, 2003, 12:29 PM
 */

package edu.msu.cme.rdp.classifier.comparison;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.text.DecimalFormat;

/**
 *
 * @author  wangqion
 */
public abstract class AbstractNode implements Taxon, ComparisonBrowserNode {
    
    private static DecimalFormat df = new DecimalFormat("0.##E0");
    private int indent;
    private AbstractNode parent;
    private AbstractNode nextSibling;
    private int s1Count = 0;
    private int s2Count = 0;
    private double significance = 0;
    
    /** Creates a new instance of AbstractNode */
    public AbstractNode() {
    }
    
    public Iterator getTaxonIterator(int depth){
        TaxonIterator it = new TaxonIterator( depth);
        return it;
    }
    
    public Iterator getLineageIterator(){
        LineageIterator it = new LineageIterator();
        return it;
    }
    
    public abstract Iterator getDetailIterator(float conf);
    public abstract void changeConfidence(SigCalculator cal);
    abstract AbstractNode getFirstChild();
    public abstract ComparisonBrowserNode findNode(int id);
    public abstract String getName();
    public abstract String getRank();
    public abstract int getTaxid();
    
    /** the parent of the root is null
     */
    AbstractNode getParent(){
        return parent;
    }
    
    AbstractNode getNextSibling(){
        return nextSibling;
    }
    
    public int getS1Count(){
        return s1Count;
    }
    
    public int getS2Count(){
        return s2Count;
    }
    
    public double getDoubleSignificance(){
        return significance;
    }
    
    public String getSignificance(){       
        if ( this instanceof UncNode){
            return "NA";
        }
        return (df.format(significance)).toString();
    }
    
    public int getIndent(){
        return indent;
    }
    
    void incS1Count(int c){
        s1Count += c;
    }
    
    void incS2Count(int c){
        s2Count += c;
    }
    
    void resetCount(){
        s1Count = 0;
        s2Count = 0;
    }
    
    void setParent(AbstractNode n){
        parent = n;
    }
    
    void setIndent(int d){
        indent = d;
    }
    
    void setSignificance(double s){
        significance = s;
    }
    
    void addSibling(AbstractNode n){
        if (nextSibling == null){
            nextSibling = n;
            return ;
        }
        nextSibling.addSibling(n);
    }
    
    /** An Iterator of the BrowserNode. Returns the next node.
     */
    class TaxonIterator implements Iterator  {
        
        private int depth;
        private AbstractNode root;
        private AbstractNode onDeck;
        private final int ROOT_INDENT = 0;
        
        public TaxonIterator( int depth){
            this.depth = depth;
            root = AbstractNode.this ;
            root.setIndent(ROOT_INDENT);
            onDeck = root;
        }
        
        public Object next(){
            Object tmp;
            if (onDeck != null){
                tmp = onDeck;
                
                onDeck = getNextElement();
                
                while ( onDeck != null ){
                    if ( onDeck.s1Count > 0 || onDeck.s2Count > 0){
                        break;
                    }
                    onDeck = getNextElement();
                }
            } else {
                throw new NoSuchElementException();
            }
            
            return tmp;
        }
        
        public boolean hasNext(){
            return (onDeck != null);
        }
        
        private AbstractNode getNextElement(){
            AbstractNode nextNode = null;
            if ( onDeck.indent < (depth-1) && onDeck.getFirstChild() != null){
                
                nextNode = onDeck.getFirstChild();
            }else if ( onDeck.getTaxid() != root.getTaxid()){
                nextNode = onDeck.nextSibling;
                if ( nextNode == null ){
                    AbstractNode parent = onDeck.parent;
                    while ( parent!= null && parent.getTaxid() != root.getTaxid()){
                        nextNode = parent.nextSibling;
                        if (nextNode != null){
                            break;
                        }
                        parent = parent.parent;
                    }
                }
            }
            if (nextNode != null){
                nextNode.setIndent(nextNode.parent.indent + 1);
            }
            return nextNode;
        }
        
        public void remove() throws UnsupportedOperationException {
        }
        
        
    }
    
    
    /** An Iterator of the ancestor nodes of the current node(not included)
     * returns the most recent ancestor last.
     */
    class LineageIterator implements Iterator {
        public LineageIterator(){
            AbstractNode node = AbstractNode.this.parent;
            while( node != null){
                lineage.add(node);
                node = node.parent;
            }
            if ( !lineage.isEmpty()){
                index = lineage.size() -1;
                onDeck = lineage.get(index--);
            }
        }
        
        public boolean hasNext(){
            return (onDeck != null);
        }
        
        public Object next(){
            Object tmp;
            if (onDeck != null){
                tmp = onDeck;
                if (index >= 0){
                    onDeck = lineage.get(index);
                    index --;
                }else{
                    onDeck = null;
                }
            } else {
                throw new NoSuchElementException();
            }
            return tmp;
        }
        
        public void remove() throws UnsupportedOperationException {
        }
        
        private ArrayList lineage = new ArrayList();
        private Object onDeck;
        private int index;
    }
    
    
}
