// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Test2.java
// A tiny class to hold a test for a 2-input node to do
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////



package spec.benchmarks._202_jess.jess;

/**
 A tiny class to hold an individual test for a 2-input node to perform
@author E.J. Friedman-Hill (C)1996
*/

class Test2 {

  final static int EQ  = 0;
  final static int NEQ = 1;

  /**
    What test to do (Test2.EQ, Test2.NEQ, etc)
   */

  int test;

  /**
    Which fact within a token (0,1,2...)
   */

  int token_idx;

  /**
    Which field (absolute index of slot start) from the left memory
   */

  int left_idx;

  /**
    Which subfield from the left memory
   */

  int left_sub_idx;


  /**
    Which field (absolute index of slot start) from the right memory
   */

  int right_idx;

  /**
    Which subfield from the right memory
   */

  int right_sub_idx;

  /**
    Constructors
    */

  Test2(int test, int token_idx, int left_idx, 
               int left_sub_idx, int right_idx, int right_sub_idx) {
    this.test = test;
    this.token_idx = token_idx;
    this.right_idx = right_idx;
    this.right_sub_idx = right_sub_idx;
    this.left_idx = left_idx;
    this.left_sub_idx = left_sub_idx;
  }

  Test2(int test, int token_idx, int left_idx, int right_idx) {
    this(test, token_idx, left_idx, -1, right_idx, -1);
  }

  boolean equals(Test2 t) {
    return  (test == t.test &&
             token_idx == t.token_idx &&
             right_idx == t.right_idx &&
             left_idx == t.left_idx &&
             right_sub_idx == t.right_sub_idx &&
             left_sub_idx == t.left_sub_idx);
  }

  public String toString() {
    return "[Test2: " + (test == EQ ? "EQ" : "NEQ")
      + ";token_idx=" + token_idx + ";left_idx=" + left_idx + "," +
      left_sub_idx + ";right_idx=" + right_idx + "," + right_sub_idx + "]";
  }


}


