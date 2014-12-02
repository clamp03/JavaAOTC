/*
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

package spec.benchmarks._201_compress;
import spec.harness.*;

public class Main implements SpecBenchmark {


    static long runBenchmark( String[] args ) {

     int speed = spec.harness.Context.getSpeed();

        if( args.length == 0 ) {

            if( speed == 100 ) {
               args = new String[7];
               args [0] = "5";   //  number of files
               args [1] = "5";   //  loop count 
               args [2] = "input/213x.tar";
               args [3] = "input/209.tar";
               args [4] = "input/239.tar";
               args [5] = "input/211.tar";
               args [6] = "input/202.tar";
                              }	
            if( speed == 10  ) {
               args = new String[5];
               args [0] = "3";
               args [1] = "1";
               args [2] = "input/228.tar";
               args [3] = "input/205.tar";
               args [4] = "input/misc.tar";
				
                              }	
            if( speed == 1   ) {
               args = new String[4];
               args [0] = "1";
               args [1] = "1";
               args [2] = "input/208.tar";
               args [3] = "input/210.tar";
                              }	
                                 }

    	return new Harness().inst_main( args );
    }


    public static void main( String[] args ) {  	 
        runBenchmark( args );
    }

    
    public long harnessMain( String[] args ) {
        return runBenchmark( args );
    }

  
}
