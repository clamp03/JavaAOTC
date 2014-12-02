// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Test1.java
// A tiny class to hold a test for a 1-input node to do
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////



package spec.benchmarks._202_jess.jess;

/**
 A tiny class to hold an individual test for a 1-input node to perform
@author E.J. Friedman-Hill (C)1996
*/

class Test1 {

  final static int EQ  = 0;
  final static int NEQ = 1;

  /**
    What test to do (Test1.EQ, Test1.NEQ, etc)
   */

  int test;

  /**
    Which slot within a fact (0,1,2...)
   */

  int slot_idx;

  /**
    Which subslot within a multislot (0,1,2...)
   */

  int sub_idx;

  /**
    The datum to test against
    */

  Value slot_value;

  /**
    Constructor
    */
  Test1(int test, int slot_idx, Value slot_value) {
    this(test, slot_idx, -1, slot_value);
  }
  Test1(int test, int slot_idx, int sub_idx, Value slot_value) {
    this.test = test;
    this.slot_idx = slot_idx;
    this.sub_idx = sub_idx;
    this.slot_value = slot_value;
  }

  public String toString() { return "[Test: test=" + (test==1 ? "NEQ" : "EQ") +
                               ";slot_idx=" + slot_idx + 
                               ";sub_idx=" + sub_idx +
                               ";slot_value=" + slot_value +"]"; } 


}


