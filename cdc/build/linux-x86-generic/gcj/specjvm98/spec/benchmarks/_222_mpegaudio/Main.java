/*
 * @(#)Main.java	1.7 06/17/98
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

package spec.benchmarks._222_mpegaudio;
import spec.harness.*;


public class Main implements SpecBenchmark {

    static long runBenchmark( String[] args ) {
    
	int speed = spec.harness.Context.getSpeed();    
	int iter  = 1;
	String name = "track16.mp3";	
    
	if( speed > 9 ) {
	    name = "spot3.mp3";
	    iter = 2*(speed/10);
	}

	if( speed > 99 ) {
	    name = "track2.mp3";	
	    iter = speed/100;
	}    
	    
        if( args.length == 0 ) {
	    args = new String[1];
	    args[0] = "input/"+name;
	}
	
	spec.harness.Context.out.println( "MPEG Audio - Decoding " + args[0] + " " + iter + " time" + (iter > 1 ? "s" : "") );
	long startTime = System.currentTimeMillis();
	
	for( int i = 0 ; i < iter ; i++ ) {
	    BenchDec.main( args );
	}
	
	return System.currentTimeMillis() - startTime; 
    }


    public static void main( String[] args ) {  	 
        runBenchmark( args );
    }

    
    public long harnessMain( String[] args ) {
        return runBenchmark( args );
    }



static int[] foo()
   {
   int fred[] = { 1, 2, 3 };
   return fred;
   }

}
