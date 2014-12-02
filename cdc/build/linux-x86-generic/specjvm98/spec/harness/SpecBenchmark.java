/*
 * @(#)SpecBenchmark.java	1.5 06/17/98
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

package spec.harness;

/**
 * This is the interface which has to be implemented by any benchmark
 * which is a part of Spec benchmark list. This has a abstract method
 * harnessMain. The benchmark implements this method so that it can be 
 * launched by calling this method.
 */
public interface SpecBenchmark { 
/**
 Starts the benchmark. Each benchmark implements this method. Given a class name, the ProgramRunner
 class dynamically creates the instance of the benchmark class and calls this
 method.
 @see spec.harness.ProgramRunner
 */   
    public long harnessMain( String[] args );
}

