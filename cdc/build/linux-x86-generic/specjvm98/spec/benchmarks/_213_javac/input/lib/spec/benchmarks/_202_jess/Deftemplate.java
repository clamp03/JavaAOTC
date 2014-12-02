// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Deftemplate.java
// Convenience class used to parse and print deftemplates
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
 Convenience class used to parse and print deftmplates.
 A deftemplate is represented as a ValueVector

An ordered deftemplate has only the first few fields, no slots

@author E.J. Friedman-Hill (C)1996
*/

public class Deftemplate {
  
  private ValueVector deft;
  public int name;
  public int ordered;
  public String docstring = null;
  /**
    Constructor.
    */

  public Deftemplate(String name, int ordered) throws ReteException {
    deft = new ValueVector();
    this.name = RU.putAtom(name);
    this.ordered = ordered;
    deft.set_length(3);
    deft.set(new Value(this.name, RU.ATOM), RU.CLASS);
    deft.set(new Value(ordered, RU.DESCRIPTOR), RU.DESC);
    deft.set(new Value(0, RU.FACT_ID), RU.ID);
  }
  
  /**
    Constructor
    starts from the ValueVector form of a Deftemplate
    */

  public Deftemplate(ValueVector dt) throws ReteException {
    deft = (ValueVector) dt.clone();
    name = dt.get(RU.CLASS).AtomValue();
    ordered = dt.get(RU.DESC).DescriptorValue();
  }


  /**
    Create a new slot in this deftemplate
    */

  public void AddSlot(String name, Value value) throws ReteException {
    if (ordered != RU.UNORDERED_FACT)
      throw new ReteException("AddSlot",
                              "Ordered deftemplates cannot have slots:",
                              name);
    deft.add(new Value(name, RU.ATOM));
    deft.add(value);

  }
  
  /**
    Generates the integer array form of this Deftemplate
    */

  public ValueVector DeftemplateData() {
    return deft;
  }
  
  /**
    Describe myself
    */

  public String toString() {
    try {
      String s = "[Deftemplate: " + RU.getAtom(name) + " ";
      s += (ordered == RU.ORDERED_FACT)
        ? "(ordered)" : "(unordered)";
      if (docstring != null)
        s += " \"" + docstring + "\" ";
      if (ordered == RU.UNORDERED_FACT) {
        s +=  "slots:";
        for (int i=RU.FIRST_SLOT; i<deft.size(); i+=RU.DT_SLOT_SIZE)
          s += " " + deft.get(i + RU.DT_SLOT_NAME).StringValue() +
            " default: " + deft.get(i + RU.DT_DFLT_DATA).toString() + ";";
      }
      s += "]";
      return s; 
    } catch (ReteException re) {
      throw new RuntimeException(re.toString());
    }

  }
}

