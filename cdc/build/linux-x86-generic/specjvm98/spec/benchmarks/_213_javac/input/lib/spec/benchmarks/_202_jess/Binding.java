// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Binding.java
// Another tiny container class
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// TEST ONLY - NOT FOR DISTRIBUTION
//////////////////////////////////////////////////////////////////////
package spec.benchmarks._202_jess;



/**
 another struct-like class for holding variable binding info
@author E.J. Friedman-Hill (C)1996
*/

public class Binding {
  public int name;
  public int factIndex;
  public int slotIndex;

  public Value val;

  public Binding(int name, int factIndex, int slotIndex) {
    this.name = name;
    this.factIndex = factIndex;
    this.slotIndex = slotIndex;
    val = null;
  }
  public Binding(int name, Value val) {
    this.name = name;
    this.factIndex = RU.LOCAL;
    this.slotIndex = RU.LOCAL;
    this.val = val;
  }

  public Object Clone() {
    Binding b = new Binding(name, factIndex, slotIndex);
    b.val = val;
    return b;
  }

}
