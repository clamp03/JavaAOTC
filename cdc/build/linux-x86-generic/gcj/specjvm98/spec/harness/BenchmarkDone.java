/*
 * @(#)BenchmarkDone.java	1.11 06/17/98
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

package spec.harness;

/**
 * This interface defines the functions to be called when the 
 * benchmark finishes running. SpecJava class impements this 
 * interface
 */

public interface BenchmarkDone {    
/**
 Constructor. The benchmark constructs the BenchmarkDone object with it's name
 and the results of running like, the runtime, IO times etc.
 @param className Name of the class
 @param results Run data. 
 */
    void benchmarkDone( String className, java.util.Properties results );
/**
 The results are formated and printed on to the console for the user.
 @param str String to be printed
 */	
    void benchmarkPrint( String str );    
}

