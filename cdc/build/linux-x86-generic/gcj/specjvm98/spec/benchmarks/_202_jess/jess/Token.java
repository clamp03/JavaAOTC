// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Token.java
// Tiny class to hold Rete Tokens
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
// $Id: Token.java,v 1.3 1997/05/11 20:22:38 ejfried Exp $
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.util.Vector;

/**
 Tiny class to hold a tag and a set of facts
@author E.J. Friedman-Hill (C)1997
*/

class Token {

  int tag;
  int negcnt;

  /**
    sortcode is used by the engine to hash tokens
     and prevent long liner memory searches
   */

  int sortcode;

  ValueVector[] facts;
  int size = 0;
  
  /**
    Constructor
    tag should be RU.ADD or RU.REMOVE
    */

  Token(int tag, ValueVector firstFact) {
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

  Token(Token t) {
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

  void AddFact(ValueVector fact) {
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
    */

  final public boolean data_equals(Token t) {
    ValueVector[] v = t.facts;

    for (int i=0; i< size; i++) {
      if (!(facts[i].equals(v[i])))
        return false;
    }
    return true;
  }

  public String toString() {
    String rv = "[Token: size=" + size;
    rv += ";tag=" + (tag == RU.ADD ? "ADD" : (tag == RU.UPDATE ? "UPDATE" : "REMOVE"));
    rv += ";negcnt=" + negcnt;
    rv += ";facts=";
    for (int i=0; i<size; i++)
      rv += facts[i] + ";";
    rv += "]";
    return rv;

  }

}
