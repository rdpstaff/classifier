/*
 * SigCalculater.java
 *
 * Created on February 8, 2005, 4:28 PM
 */

package edu.msu.cme.rdp.classifier.comparison;

/**
 *
 * @author  wangqion
 */
public class SigCalculator {
    
    private static final int FREQ_LIMIT = 5;
    private int s1Total;
    private int s2Total;
    private int combinedTotal;
    private double faction;
    private double ratio_1;
    private double ratio_2;
    private float confidence; // the classification confidence
    private Ztable ztable;
    
    
    /** Creates a new instance of SigCalculater */
    public SigCalculator(int total1, int total2, float conf) {
        if ( ztable == null){
            ztable = new Ztable();
        }
        s1Total = total1;
        s2Total = total2;
        combinedTotal = s1Total + s2Total;
        faction = 1.0/(double)s1Total + 1.0/(double)s2Total;
        confidence = conf;
        ratio_1 = (double)total1/(double)total2;
        ratio_2 = (double)total2/(double)total1;
        
    }
    
    public int getS1Total(){
        return s1Total;
    }
    
    public int getS2Total(){
        return s2Total;
    }
    
    public float getConfidence(){
        return confidence;
    }
    
    
    public double calculateSig(int s1, int s2){
        if(s1 <= FREQ_LIMIT || s2 <= FREQ_LIMIT || (s1Total-s1) <= FREQ_LIMIT || (s2Total-s2) <= FREQ_LIMIT ){
            return this.smallProportionTest(s1, s2);            
        }
        return this.largeProportionTest(s1, s2);
        
    }
    
    /** This test works good for small propotion <5%
     * Calculates the probability for y occurrences in sample2 when x
     * occurrences is observed in sample1.
     * Given sample1 size N1 and sample2 size N2, the probability equation is:
     * p(y|x) = (N2/N1)^y * (x+y)! /x! /y! /(1+N2/N1)^(x+y+1)
     * to avoid data overflow, we modify the equation:
     * p(y|x) = exp( log(p(y|x) )
     * log(p(y|x) = y*log(N2/N1) - (x+y+1)*log(1+N2/N1)
     *             + log( (x+y)!) - log(x!) -log(y!)
     * Because we assume the sample distribution follows the Poisson distribution
     * To calculate the cumulative distribution:
     * we sum p(y|x) for y in [0, y_min] if y/N2 <= x/N1
     *    or p(x|y) for x in [0, x_min] if x/N1 <= y/N2
     * we need to multiply the p by 2 to give the probability on both end.
     */
    
    double smallProportionTest(int s1, int s2){
        
        double ratio = 0;
        int upperLimit = 0;
        int constantValue = 0;
        
        double percent_s1 = (double)s1/ (double)s1Total;
        double percent_s2 = (double)s2/ (double)s2Total;
        
        // sum the prob on the left tail
        if ( percent_s1 >= percent_s2) {
            constantValue = s1;
            upperLimit = s2;
            ratio = ratio_2;
        }else {
            constantValue = s2;
            upperLimit = s1;
            ratio = ratio_1;
        }
        
        double log_ratio = Math.log(ratio);
        double log_ratio_add1 = Math.log(ratio + 1.0);
        
        double sum_prob = 0;
        int i = 0;
        double prev_factorial = 0;
        double p2 = i* log_ratio - (constantValue + i +1)* log_ratio_add1;
        sum_prob += Math.exp(prev_factorial + p2);
        
        for ( i = 1; i <= upperLimit; i++){
            p2 = i* log_ratio - (constantValue + i +1)* log_ratio_add1;
            prev_factorial += Math.log((double)(constantValue+i)) - Math.log((double)i);
            sum_prob += Math.exp(prev_factorial + p2);
        }
        
        return sum_prob *2;
        
    }
    
    
    
  /* Calculates the log( (x+y)!) - log(x!) -log(y!)
   * After simplify the formula, we get sum( log(x+y-i) - log(y-i) ) where
   * the range for i is [0, y-1]
   * Note: this function also takes care of the case when x or y equals to 0.
   */
    double calFactorial(int x, int y){
        double result = 0;
        int total = x + y;
        int min = Math.min(x,y);
        
        for (int i = 0; i < min; i++){
            result += Math.log((double)(total -i) /(double)(min-i));
        }
        return result;
    }
    
    /* This is the two proportion test
     * requirement: s1, s2, s1Total -s1, and s2Total -s2 > 5
     *  p1 = s1/s1Total, p2 = s2/s2Total, p = (s1+s2)/(s1Total + s2Total)
     *  z = ( pl - p2)/(sqrt( p*(1-p)*(1/s1Total + 1/s2Total) )
     */
    double largeProportionTest(int s1, int s2){
        double p1 = (double)s1 / (double)s1Total;
        double p2 = (double)s2 / (double)s2Total;
        double p = (double)(s1+s2)/ (double)combinedTotal;
       
        if ( p == 0f || p == 1.0f){
            return 1.0;
        }        
        double z = ( p1 -p2)/Math.sqrt(p * ( 1.0 -p) * faction);
        return ztable.getPvalue(z);
    }
    
}
