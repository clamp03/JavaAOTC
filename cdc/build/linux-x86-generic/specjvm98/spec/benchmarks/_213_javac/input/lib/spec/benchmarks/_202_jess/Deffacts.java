// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Deffacts.java
//  Class used to represent deffacts.
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// TEST ONLY - NOT FOR DISTRIBUTION
//////////////////////////////////////////////////////////////////////
package spec.benchmarks._202_jess;


import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream; //**NS**

import java.lang.System;
import java.lang.Integer;
import java.lang.Character;

import java.util.BitSet; //**NS**
import java.util.Enumeration;
import java.util.Stack;
import java.util.Hashtable;
import java.util.Vector;
/**
 Class used to represent deffacts.

 A Deffacts has no ValueVector  representation; it is always a
 class object.

@author E.J. Friedman-Hill (C)1996
*/

public class Deffacts {
  
  public int name;
  public Vector facts;
  public String docstring = null;
  /**
    Constructor.
    */

  public Deffacts(String name) {
    this.name = RU.putAtom(name);
    facts = new Vector();
  }
  
  /**
    Add a fact to this deffacts
    */

  public void AddFact(Fact fact) throws ReteException {
    AddFact(fact.FactData());
  }

  public void AddFact(ValueVector fact) {
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

