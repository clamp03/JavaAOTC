// -*- java -*-
//////////////////////////////////////////////////////////////////////
// TokenVector.java
// A Vector that holds Tokens only.
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// $Id: TokenVector.java,v 1.2 1997/05/11 20:22:39 ejfried Exp $
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

/**
 A mini version of Vector that only holds Tokens.
@author E.J. Friedman-Hill (C)1996
*/

class TokenVector {

  private Token[] v;
  private int ptr = 0;

  TokenVector() {
    this(10);
  }

  TokenVector(int size) {
    v = new Token[size];
  }

  final int size() {
    return ptr;
  }

  final Token elementAt(int i) {
    // let Java throw the exception
    return v[i];
  }

  void addElement(Token val) {
    if (ptr >= v.length) {
      Token[] nv = new Token[v.length + 10];
      System.arraycopy(v, 0, nv, 0, v.length);
      v = nv;
    }
    v[ptr++] = val;
  }

  // This is *very* fast, but does not preserve the order
  // of the tokens in memory. This is OK; it slightly changes the
  // (undefined) order of activations on the agenda.
  void removeElement(Token val) {
    for (int i=0; i< ptr; i++)
      if (v[i].equals(val)) {
        v[i] = null; // delete the token (in case i == ptr-1!)
        v[i] = v[ptr-1];
        --ptr;
        return;
      }
  }

}

