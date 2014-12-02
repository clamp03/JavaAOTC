// -*- java -*-
//////////////////////////////////////////////////////////////////////
// ValueVector.java
// A Vector that holds Values only.
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// TEST ONLY - NOT FOR DISTRIBUTION
//////////////////////////////////////////////////////////////////////
package spec.benchmarks._202_jess;
/**
 A mini version of Vector that only holds Values.
@author E.J. Friedman-Hill (C)1996
*/

public class ValueVector {

  private Value[] v;
  private int ptr = 0;

  public ValueVector() {
    this(10);
  }

  public ValueVector(int size) {
    v = new Value[size];
  }

  public int size() {
    return ptr;
  }

  public Object clone() {
    ValueVector vv = new ValueVector(ptr);
    vv.ptr = ptr;
    System.arraycopy(v, 0, vv.v, 0, ptr);
    return vv;
  }

  public Value get(int i) {
    // let Java throw the exception
    return v[i];
  }

  public void set_length(int i) {
    if (i > v.length) {
      Value[] nv = new Value[i];
      System.arraycopy(v, 0, nv, 0, v.length);
      v = nv;
    }
    ptr = i;
  }


  public void set(Value val, int i) {
    // let Java throw the exception
    v[i] = val;
  }

  public void add(Value val) {
    if (ptr >= v.length) {
      Value[] nv = new Value[v.length + 10];
      System.arraycopy(v, 0, nv, 0, v.length);
      v = nv;
    }
    v[ptr++] = val;
  }


  public boolean equals(ValueVector vv) {
    if (ptr != vv.ptr)
      return false;
    for (int i=0; i< ptr; i++)
      if (!v[i].equals(vv.v[i]))
        return false;
    return true;
  }

}

