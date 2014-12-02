// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Defglobal.java
//  Class used to represent Defglobals.
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.util.*;

/**
 Class used to represent Defglobals.

 A Defglobal has no ValueVector  representation; it is always a
 class object.

@author E.J. Friedman-Hill (C)1996
*/

public class Defglobal {
  
  Vector bindings;

  /**
    Constructor.
    */

  Defglobal() {
    bindings = new Vector();
  }
  
  /**
    Add a variable to this Defglobal
    */


  void AddGlobal(String name, Value val) {
    bindings.addElement(new Binding(RU.putAtom(name),val));
  }

  
  /**
    Describe myself
    */

  public String toString() {
    String s = "[Defglobal: ";
    s += bindings.size() + " variables";
    s += "]";
    return s; 
  }

}

