/*
 * @(#)Main.java	1.7 06/17/98
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 *
 * Complain about Java errors that ought to be caught as
 * exceptions by the JVM. *NOT* an exhaustive conformance
 * test. This is just intended to catch some common errors
 * and omissions for the convenience of the benchmarker in
 * avoiding some run rule mistakes. This needs to be
 * expanded to test more.
 *
 * The timing of this "benchmark" is ignored in metric
 * calculation. It is here only in order to pass or
 * fail output verification.
 */

package spec.benchmarks._200_check;
import spec.harness.*;

public class Main implements SpecBenchmark {

///////////////////////////////////////
//class variable field declarations
///////////////////////////////////////

///////////////////////////////////////
//instance variable field declarations
///////////////////////////////////////

///////////////////////////////////////
//constructor declarations
///////////////////////////////////////

///////////////////////////////////////
//class method declarations
///////////////////////////////////////

public static long runBenchmark(String[] args){
    boolean caughtIndex = false;
    boolean gotToFinally = false;
    try{
        int[] a = new int[10];
        for (int i=0; i<=10; i++)
            a[i] = i;
        spec.harness.Context.out.println("Error: array bounds not checked");
    }catch (ArrayIndexOutOfBoundsException e){
        caughtIndex = true;
    }finally{
        gotToFinally = true;
    }
    if (!caughtIndex)
        spec.harness.Context.out.println("1st bounds test error:\tindex exception not received");
    if (!gotToFinally)
        spec.harness.Context.out.println("1st bounds test error:\tfinally clause not executed");
    if (caughtIndex && gotToFinally)
        spec.harness.Context.out.println("1st bounds test:\tOK");
    checkSubclassing();
    LoopBounds mule = new LoopBounds();
    mule.run();
    if (mule.gotError)
        spec.harness.Context.out.println("2nd bounds test:\tfailed");
    else
        spec.harness.Context.out.println("2nd bounds test:\tOK");
    PepTest horse = new PepTest();
    horse.instanceMain();
    if (horse.gotError)
        spec.harness.Context.out.println("PepTest failed");
    return 0;
}

public static void main( String[] args ) {           
    runBenchmark( args );
}

private static void checkSubclassing(){
    Super sup = new Super (3);
    Sub   sub = new Sub   (3);
    spec.harness.Context.out.println (sup.getName() + ": " + sup.toString());
    spec.harness.Context.out.println (sub.getName() + ": " + sub.toString());
    spec.harness.Context.out.println ("Super: prot=" + sup.getProtected() +
        ", priv=" + sup.getPrivate());
    spec.harness.Context.out.println ("Sub:  prot=" + sub.getProtected() +
        ", priv=" + sub.getPrivate());
}


///////////////////////////////////////
//instance method declarations
///////////////////////////////////////

public long harnessMain( String[] args ) {
    return runBenchmark( args );
}

}//end class

