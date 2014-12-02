// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Token.java
// Tiny class to hold Rete Tokens
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// TEST ONLY - NOT FOR DISTRIBUTION
//////////////////////////////////////////////////////////////////////
package spec.benchmarks._202_jess;


import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream; //**NS**

import java.lang.System;
import java.lang.Integer;
import java.lang.Character;

import java.util.BitSet; //**NS**
import java.util.Enumeration;
import java.util.Stack;
import java.util.Hashtable;
import java.util.Vector;
/**
 Tiny class to hold a tag and a set of facts
@author E.J. Friedman-Hill (C)1996
*/

public class Token {

  public int tag;
  public int negcnt;

  /**
    sortcode is used by the engine to hash tokens
     and prevent long liner memory searches
   */

  public int sortcode;

  public ValueVector[] facts;
  public int size = 0;
  
  /**
    Constructor
    tag should be RU.ADD or RU.REMOVE
    */

  public Token(int tag, ValueVector firstFact) {
    facts = new ValueVector[5];
    facts[size++] = firstFact;
    this.tag = tag;
    negcnt = 0;
    sortcode = 0;
  }


  /**
    Create a new Token containing the same data as an old one
    NOTE: This does a shallow copy - the ValueVector facts are NOT duplicated.
    */

  public Token(Token t) {
    // we'll probably get added to right away!
    facts = new ValueVector[t.size+3];
    this.tag = t.tag;
    this.negcnt = 0;
    this.size = t.size;
    System.arraycopy(t.facts, 0, facts, 0, size);
    this.sortcode = t.sortcode;
  }



  /**
    Add a fact to this token
    */

  public void AddFact(ValueVector fact) {
    if (size >= facts.length) {
      ValueVector[] vv = new ValueVector[size + 3];
      System.arraycopy(facts, 0, vv, 0, size);
      facts = vv;
    }

    facts[size++] = fact;
  }


  
  /**
    Compare the data in this token to another token.
    The tokens are assumed to be of the same size (same number of facts).
    Because of the way we've designed this system, because two identical
    facts can never exist at the same time, we don't have to compare the
    contents of the facts, just their addresses!!!
    */

  final public boolean data_equals(Token t) {
    ValueVector[] v = t.facts;

    for (int i=0; i< size; i++) {
      if (facts[i] != v[i])
        return false;
    }
    return true;
  }
}









