/*
 * BrowserBean.java
 *
 * Created on November 12, 2003, 2:47 PM
 */

package edu.msu.cme.rdp.classifier.comparison;

import java.util.Iterator;
import java.util.List;
import java.util.Date;

/**
 *
 * @author  wangqion
 */
public class ComparisonBrowserBean {
    
    private static final int DEFAULT_DEPTH = 0;
    private static final int DISPLAY_LIMIT = 25;
    private static final int MIN_DEPTH = 2;
    private static final int MAX_DEPTH = 10;
    private static final float MIN_CONF = 0.0f;
    private static final float MAX_CONF = 1.0f;
    private static final int MAX_SEQUENCE = 40000;
    private static final float DEFAULT_CONF = 0.8f;
    private int depthValue = DEFAULT_DEPTH;
    private String depth;
    private float confidenceValue = DEFAULT_CONF ;
    private String confidence;
    private List errorList;
    private ComparisonBrowserNode currentRoot;
    private ComparisonBrowserNode root;
    private String hierarchyVer;
    private String classifierVer;
    
    private Date submitDate;
    private String s1Filename;
    private String s2Filename;
    
    public static int getSeqCountLimit() {
        return MAX_SEQUENCE;
    }
    
    public static float getDefaultConfidence(){
        return DEFAULT_CONF;
    }
    
    public Date getSubmitDate() {
        return this.submitDate;
    }
    
    public void setSubmitDate(Date date) {
        this.submitDate = date;
    }
           
    public String getHierarchyVersion() {
        return this.hierarchyVer;
    }
    
    public String getClassifierVersion() {
        return this.classifierVer;
    }
    
    public String getDepth() {
        if ( this.depth == null ) {
            this.depth = String.valueOf(this.depthValue);
        }
        return this.depth;
    }
    
    public void setDepth(String depthStr) {
        if ( depthStr == null || depthStr.trim().equals("") ) {
            return;
        }
        int depValue;
        try {
            depValue = Integer.parseInt(depthStr);
        } catch(NumberFormatException ex) {
            throw new IllegalArgumentException("Expects display level between 2 and 10");
        }
        if ( depValue != this.DEFAULT_DEPTH && (depValue < this.MIN_DEPTH || depValue > this.MAX_DEPTH) ) {
            throw new IllegalArgumentException("Expects display level between 2 and 10");
        }
        if ( this.depthValue != depValue ) {
            this.depthValue = depValue;
            this.depth = String.valueOf(this.depthValue);
        }
    }
    
    public float getFloatConfidence() {
        if ( this.confidence == null ) {
            return this.confidenceValue;
        }
        return Float.parseFloat(this.confidence);
    }
    
    public int getS1Total(){
        return ((AbstractNode)root).getS1Count();
    }
    
    public int getS2Total(){
        return ((AbstractNode)root).getS2Count();
    }
    
    void setS1Filename(String s){
        s1Filename = s;
    }
    
    void setS2Filename(String s){
        s2Filename = s;
    }
    
    public String getS1Filename(){
        return s1Filename;
    }
    
     public String getS2Filename(){
        return s2Filename;
    }
    
    public String getConfidence() {
        if ( this.confidence == null ) {
            this.confidence = String.valueOf(this.confidenceValue);
        }
        return this.confidence;
    }
    
    public void setConfidence(String confidenceStr) {
        if ( confidenceStr == null || confidenceStr.trim().equals("") || root == null) {
            return;
        }
        float confValue;
        try {
            confValue = Float.parseFloat(confidenceStr);
        } catch(NumberFormatException ex) {
            throw new IllegalArgumentException("Expects cut off value between 0 and 1");
        }
        if (confValue < MIN_CONF || confValue >  MAX_CONF ) {
            throw new IllegalArgumentException("Expects cut off value between 0 and 1");
        }
        if ( this.confidenceValue != confValue ) {
            this.confidenceValue = confValue;
            this.confidence = String.valueOf(confValue);
            SigCalculator cal = new SigCalculator( ((Taxon)root).getS1Count(), ((Taxon)root).getS2Count(), confidenceValue);
            root.changeConfidence(cal);
        }
    }
    
    public int getSeqErrorCount() {
        if ( errorList != null ) {
            return this.errorList.size();
        }
        return 0;
    }
    
    public ComparisonBrowserNode getCurrentBrowserRoot() {
        return this.currentRoot;
    }
    
    public void setCurrentRoot(String tax) {
        if ( tax == null || tax.trim().equals("") ) {
            //throw new IllegalArgumentException("Expects cut off value between 0 and 1");
            return;
        }
        int taxid;
        try {
            taxid = Integer.parseInt( tax );
        } catch(NumberFormatException ex) {
            throw new IllegalArgumentException("Expects taxon id in integer format");
        }
        ComparisonBrowserNode bNode = root.findNode(taxid);
        if ( bNode == null ) {
            throw new IllegalArgumentException("Taxon node with id " + tax + " cannot be found");
        }
        currentRoot = bNode;
    }
    
    public void setRoot(ComparisonBrowserNode root) {
        this.root = root;
        this.currentRoot = root;
    }
    
    public Iterator getLineageList() {
        if ( currentRoot == null ) {
            return null;
        }
        // This extra 3 lines of coding is to make the JSTL 'empty' operator work
        // when displaying lineage history.
        Iterator iter = currentRoot.getLineageIterator();
        if (! iter.hasNext()) return null;
        return iter;
    /*
    return currentRoot.getLineageIterator(); */
    }
    
    public Iterator getTabularTaxonList() {       
        ComparisonResultTreeSet resultSet = new ComparisonResultTreeSet();
        
        Iterator taxonIt = root.getTaxonIterator(MAX_DEPTH);
        while (taxonIt.hasNext()){
            resultSet.add(taxonIt.next());
        }
        return resultSet.iterator();
    }
    
    public Iterator getChildTaxonList() {        
        Iterator taxonIt = currentRoot.getTaxonIterator(this.MIN_DEPTH);
        if ( taxonIt.hasNext()){
            taxonIt.next();
        }
        return taxonIt;
    }
    
    
    public Iterator getHierarchyTaxonList() {
        if ( currentRoot == null ) {
            return null;
        }
        Iterator taxonIt = null;
        
        int returnDepth = MIN_DEPTH;
        
        if ( this.depthValue != DEFAULT_DEPTH ) {
        		returnDepth = this.depthValue;
        }else {
            int dp = this.MIN_DEPTH;
            int prevSize = 0;
            
            while( dp < this.MAX_DEPTH && prevSize < 30 ) {
                taxonIt = currentRoot.getTaxonIterator(dp);
                int size = 0;
                
                while ( taxonIt.hasNext() ) {
                    taxonIt.next();
                    size++;
                    if (size >= DISPLAY_LIMIT) {
                        break;
                    }
                }
                
                if ( size == prevSize || size >= DISPLAY_LIMIT ) {    
             	   returnDepth = dp;
	                 break;
	             } 
	             dp++;
	             returnDepth = dp;                
	             prevSize = size;
            }
        }
        return currentRoot.getTaxonIterator(returnDepth);
    }
    
    public Iterator getDetailList() {
        if ( currentRoot == null ) {
            return null;
        }
        return currentRoot.getDetailIterator(this.confidenceValue);
    /*
    Iterator iter = currentRoot.getDetailIterator(this.confidenceValue);
    if (! iter.hasNext()) return null;
    return iter; */
    }
    
    public int getDetailListSize() {
        Iterator it = this.getDetailList();
        int size = 0;
        while ( it.hasNext() ) {
            size++;
        }
        return size;
    }
    
    public void setSeqErrorList(List errorList) {
        this.errorList = errorList;
    }
    
    public Iterator getSeqErrorList() {
        if ( this.errorList == null ) {
            return null;
        }
        return this.errorList.iterator();
    }
    
    public ComparisonBrowserBean(String hver, String cver) {
        hierarchyVer = hver;
        classifierVer = cver;
    }
}
