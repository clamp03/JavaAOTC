// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Test1.java
// A tiny class to hold a test for a 1-input node to do
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// TEST ONLY - NOT FOR DISTRIBUTION
//////////////////////////////////////////////////////////////////////
package spec.benchmarks._202_jess;



/**
 A tiny class to hold an individual test for a 1-input node to perform
@author E.J. Friedman-Hill (C)1996
*/

public class Test1 {

  final public static int EQ  = 0;
  final public static int NEQ = 1;

  /**
    What test to do (Test1.EQ, Test1.NEQ, etc)
   */

  public int test;

  /**
    Which slot within a fact (0,1,2...)
   */

  public int slot_idx;

  /**
    The datum to test against
    */

  public Value slot_value;

  /**
    Constructor
    */

  public Test1(int test, int slot_idx, Value slot_value) {
    this.test = test;
    this.slot_idx = slot_idx;
    this.slot_value = slot_value;
  }

}


