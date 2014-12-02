// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Deffacts.java
//  Class used to represent deffacts.
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.util.*;

/**
 Class used to represent deffacts.

 A Deffacts has no ValueVector  representation; it is always a
 class object.

@author E.J. Friedman-Hill (C)1996
*/

public class Deffacts {
  
  int name;
  Vector facts;
  String docstring = null;

  public final int getName() { return name; }
  public final String getDocstring() { return docstring; }

  /**
    Constructor.
    */

  Deffacts(String name) {
    this.name = RU.putAtom(name);
    facts = new Vector();
  }
  
  /**
    Add a fact to this deffacts
    */

  void AddFact(Fact fact) throws ReteException {
    AddFact(fact.FactData());
  }

  void AddFact(ValueVector fact) {
    facts.addElement(fact);
  }

  
  /**
    Describe myself
    */

  public String toString() {
    String s = "[Deffacts: " + RU.getAtom(name) + " ";
    if (docstring != null)
      s += "\"" + docstring + "\"; ";
    s += facts.size() + " facts";
    s += "]";
    return s; 
  }

}

