// -*- java -*-
//////////////////////////////////////////////////////////////////////
// GlobalContext.java
//  Class used to represent the Global Context of a Rete Engine
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.util.*;

/**
 Class used to represent the Global Execution Context of a Rete engine.

@author E.J. Friedman-Hill (C)1996
*/

class GlobalContext extends Context {
  

  Vector global_bindings;

  /**
    Constructor.
    */

  public GlobalContext(Rete engine) {
    super(engine);
    global_bindings = new Vector();
  }

  /**
    Find a variable table entry
    */
  
  protected Binding findGlobalBinding(int name) {
    for (int i=0; i< global_bindings.size(); i++) {
      Binding b = (Binding) global_bindings.elementAt(i);
      if (b.name == name)
        return b;
    }
    return null;
  }

  /**
    Make note of a global variable during Parsing.
    */

  final public Binding AddGlobalBinding(int name, Value value) {
    Binding b;
    if ((b = findGlobalBinding(name)) != null) {
      b.val = value;
      return b;
    }
    b = new Binding(name, value);
    global_bindings.addElement(b);
    return b;
  }

  
  /**
    Describe myself
    */

  public String toString() {
    String s = "[GlobalContext: ";
    s += "]";
    return s; 
  }

}

