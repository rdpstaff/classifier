package edu.msu.cme.rdp.classifier.train.validation;

class AddLogs {
  
  // an interval of 0.01 gives a maximum error ~ 3E-6
  static final double interval       = 0.01;
  static final int    xlateFactor    = (int) Math.round( 1.0 / interval );
  
  // -13 gives value of ~ 2.3E-6, acceptable error
  static final int    stopVal        = -13;
  static final int    maxIndex       = 0 - xlateFactor * stopVal; 
  static final int    xlateTableSize = 1 - stopVal * xlateFactor;
  static final double xlateTable[]   = new double[ xlateTableSize ];
  
  static {
    for ( int i = 0; i < xlateTableSize; ++i ) {
      xlateTable[ i ] =
          Math.log( 1 + Math.exp( 0.0 - ( i * interval ) ) );
    }
  }

  
  /** calculate log( exp(p1) + exp(p2) ) */
  static double add( double p1, double p2 ) {
    
    double pMax       = Math.max( p1, p2 );
    double pMin       = Math.min( p1, p2 );
    double diff       = pMax - pMin;
    double scaledDiff = diff * xlateFactor;
    double index      = Math.floor( scaledDiff );
    double delta      = scaledDiff - index;
    double retVal;
    
    if ( (int) index < maxIndex ) {
      retVal = pMax + ( xlateTable[ (int) index ] * ( 1.0  - delta ) )
                    + ( xlateTable[ (int) index + 1 ] * delta );
    } else {
      retVal = pMax;
    }
    return retVal;
  }
  
}
