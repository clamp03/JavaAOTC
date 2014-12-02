// -*- java -*-
//////////////////////////////////////////////////////////////////////
// TokenVector.java
// A Vector that holds Tokens only.
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// TEST ONLY - NOT FOR DISTRIBUTION
//////////////////////////////////////////////////////////////////////
package spec.benchmarks._202_jess;

/**
 A mini version of Vector that only holds Tokens.
@author E.J. Friedman-Hill (C)1996
*/

public class TokenVector {

  private Token[] v;
  private int ptr = 0;

  public TokenVector() {
    this(20);
  }

  public TokenVector(int size) {
    v = new Token[size];
  }

  public int size() {
    return ptr;
  }

  public Token elementAt(int i) {
    // let Java throw the exception
    return v[i];
  }

  public void addElement(Token val) {
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
  public void removeElement(Token val) {
    for (int i=0; i< ptr; i++)
      if (v[i].equals(val)) {
        v[i] = null; // delete the token (in case i == ptr-1!)
        v[i] = v[ptr-1];
        --ptr;
        return;
      }
  }

}

