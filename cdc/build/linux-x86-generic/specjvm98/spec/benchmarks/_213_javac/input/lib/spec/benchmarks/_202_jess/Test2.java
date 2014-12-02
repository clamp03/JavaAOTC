// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Test2.java
// A tiny class to hold a test for a 2-input node to do
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// TEST ONLY - NOT FOR DISTRIBUTION
//////////////////////////////////////////////////////////////////////
package spec.benchmarks._202_jess;



/**
 A tiny class to hold an individual test for a 2-input node to perform
@author E.J. Friedman-Hill (C)1996
*/

public class Test2 {

  final public static int EQ  = 0;
  final public static int NEQ = 1;

  /**
    What test to do (Test2.EQ, Test2.NEQ, etc)
   */

  public int test;

  /**
    Which fact within a token (0,1,2...)
   */

  public int token_idx;

  /**
    Which field (absolute index of slot start) from the left memory
   */

  public int left_idx;

  /**
    Which field (absolute index of slot start) from the right memory
   */

  public int right_idx;

  /**
    Constructor
    */

  public Test2(int test, int token_idx, int left_idx, int right_idx) {
    this.test = test;
    this.token_idx = token_idx;
    this.right_idx = right_idx;
    this.left_idx = left_idx;
  }

  public boolean equals(Test2 t) {
    return  (test == t.test &&
             token_idx == t.token_idx &&
             right_idx == t.right_idx &&
             left_idx == t.left_idx);
  }

  public String toString() {
    return "[Test2: " + test + " " + token_idx + " " + left_idx + " " +
      right_idx + "]";
  }


}


