/*
 * PixelCalculatorBean.java
 *
 * Created on March 11, 2005, 12:01 PM
 */

package edu.msu.cme.rdp.classifier.comparison;

import java.util.Iterator;

/**
 *
 * @author  wangqion
 */
public class PixelCalculatorBean {
      
    private int s1Total = 0;
    private int s2Total = 0;
    private Taxon taxon = null;  
    private float maxPercent = 0f;
    private String rank = "";  
    private int numOfChildren = 0;
    private double sig99 = 0.01;
    
    public void setS1Total(int total){
        s1Total = total;
    }
    
    public void setS2Total(int total){
        s2Total = total;
    }
    
    
    public void setTaxonList(Iterator it){
        float percent = 0f;
        numOfChildren = 0;
        while(it.hasNext()){
            Taxon taxon = (Taxon)it.next();
            numOfChildren ++;
            if ( !taxon.getRank().equals("")){
                rank = taxon.getRank();
            }
            if( s1Total > 0){
                percent = ((float)taxon.getS1Count()) / ((float)s1Total)  * 100f ;
               
                if( percent > maxPercent) {
                    maxPercent = percent;
                }
            }
            if( s2Total > 0){
                percent = ((float)taxon.getS2Count())  / ((float)s2Total)  * 100f ;
                if( percent > maxPercent) {
                    maxPercent = percent;
                }
            }
        }        
    }
    
    public int getNumOfChildren(){
        return numOfChildren;
    }
    
    public String getRank(){
        return rank ;
    }
    
    public void setTaxon(Taxon t){
        taxon = t;
    }
    
      
    // we need the format like xx.x  
    public String getS1PercentStr(){        
        float percent = 0f;
        if ( s1Total > 0){
            percent = Math.round( ((float)taxon.getS1Count()) / ((float)s1Total)  * 1000f ) /10f;
        }
        return Float.toString(percent) ;
    }
    
    public String getS2PercentStr(){
        float percent = 0f;
        if ( s2Total > 0){
            percent = Math.round( ((float)taxon.getS2Count()) / ((float)s2Total)  * 1000f ) / 10f;
        }
        return Float.toString(percent);
    }
    
    public String getS1GifPercent(){
        int percent = 0;
        if ( s1Total > 0){            
            percent= Math.round( ((float)taxon.getS1Count()) / ((float)s1Total) / maxPercent * 10000f);           
        }
        return Integer.toString(percent) + "%";
    }
    
    public String getS2GifPercent(){
        int percent = 0;
        if ( s2Total > 0){            
            percent = Math.round( ((float)taxon.getS2Count()) / ((float)s2Total) / maxPercent * 10000f);         
        }
        return Integer.toString(percent) + "%";
    }
    
    public boolean isSignificant(){
        if ( taxon instanceof UncNode) {
            return false;
        }
        
        if ( ((AbstractNode)taxon).getDoubleSignificance() <= sig99){
            return true;
        }
        return false;
    }
    
}
