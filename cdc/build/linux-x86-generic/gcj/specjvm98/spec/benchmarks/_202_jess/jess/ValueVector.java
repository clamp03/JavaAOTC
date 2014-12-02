// -*- java -*-
//////////////////////////////////////////////////////////////////////
// ValueVector.java
// A Vector that holds Values only.
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
// $Id: ValueVector.java,v 1.4 1997/07/04 19:55:25 ejfried Exp $
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

/**
 A mini version of Vector that only holds Values.
@author E.J. Friedman-Hill (C)1996
*/

public class ValueVector implements Cloneable {

  protected Value[] v;
  protected int ptr = 0;

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

  public final Value get(int i) {
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


  public final void set(Value val, int i) {
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


  public boolean equals(Object o) {
    if (this == o)
      return true;
    ValueVector vv = (ValueVector) o;
    if (ptr != vv.ptr)
      return false;
    for (int i=0; i< ptr; i++)
      if (!v[i].equals(vv.v[i]))
        return false;
    return true;
  }

  public String toString() {
    String s = "(";
    for (int i=0; i < ptr; i++) {
      if (i > 0)
        s += " ";
      s += v[i];
    }
    s += ")";
    return s;
  }


}

