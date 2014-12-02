/*
 * @(#)Main.java	1.1 04/02/01
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1996,1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

package spec.benchmarks._205_raytrace;
import spec.harness.*;


public class Main implements SpecBenchmark {


    static long runBenchmark( String[] args ) {
    
        if( args.length == 0 ) {
   	    args = new String[3];	    
	    args[0] = "" + (200*spec.harness.Context.getSpeed())/100;
	    args[1] = "200";		    
	    args[2] = "input/time-test.model";
	}
	
	return new RayTracer().inst_main( args );
    }


    public static void main( String[] args ) {  	 
        runBenchmark( args );
    }

    
    public long harnessMain( String[] args ) {
        return runBenchmark( args );
    }

  
}
