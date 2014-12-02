// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Binding.java
// Another tiny container class
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////


package spec.benchmarks._202_jess.jess;


/**
 another struct-like class for holding variable binding info
@author E.J. Friedman-Hill (C)1996
*/

class Binding implements Cloneable {
  int name;
  int factIndex;
  int slotIndex;
  int subIndex;

  Value val;

  public Binding(int name, int factIndex, int slotIndex, int subIndex) {
    this.name = name;
    this.factIndex = factIndex;
    this.slotIndex = slotIndex;
    this.subIndex = subIndex;
    val = null;
  }
  public Binding(int name, Value val) {
    this.name = name;
    this.factIndex = RU.LOCAL;
    this.slotIndex = RU.LOCAL;
    this.subIndex = -1;
    this.val = val;
  }

  public Object clone() {
    Binding b = new Binding(name, factIndex, slotIndex, subIndex);
    b.val = val;
    return b;
  }
  public String toString()
  {
    String s = "[Binding: " + RU.getAtom(name);
    s += ";factIndex=" + factIndex;
    s += ";slotIndex=" + slotIndex;
    s += ";subIndex=" + subIndex;
    s += ";val=" + val;
    s += "]";
    return s;
  }

}
