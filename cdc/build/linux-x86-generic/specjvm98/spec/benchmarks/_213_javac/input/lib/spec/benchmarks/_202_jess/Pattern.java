// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Pattern.java
// Convenience class used to parse and print Patterns
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
 Convenience class used to parse and print patterns.
 A Pattern consists mainly of a two-dimensional array of Test1 structures.

 Each Test1 contains information about a specific characteristic of a slot.

@author E.J. Friedman-Hill (C)1996
*/

public class Pattern {

  /**
    The deftemplate corresponding to this pattern
   */

  private ValueVector deft;

  /**
    The number of slots in THIS pattern
    */

  private int nvalues;

  /**
    The Slot tests for this pattern
   */

  public Test1[][] tests;

  /**
    Am I in a (not () ) ?
   */
  
  private boolean _negated = false;

  /**
    Class of fact matched by this pattern
    */

  private int _class;

  /**
    ordered or unordered
   */

  private int _ordered = 0;

  /**
    Constructor.
    */

  public Pattern(String name, int ordered, Rete engine) throws ReteException {
    _class = RU.putAtom(name);
    _ordered = ordered;
    nvalues = 0;
    deft = FindDeftemplate(name, engine);

    if (_ordered == RU.ORDERED_FACT)
      tests = new Test1[RU.MAXFIELDS][];
    else {
      int size = (deft.size() - RU.FIRST_SLOT) / RU.DT_SLOT_SIZE;
      tests = new Test1[size][];
    }
  }


  /**
    find the deftemplate, if there is one, or create implied dt.
    */

  protected ValueVector FindDeftemplate(String name, Rete engine) throws ReteException {

    deft = engine.FindDeftemplate(name);
    if (deft != null) {
      if (_ordered != deft.get(RU.DESC).DescriptorValue()) {
        // looks like there are semantic errors in input.
        ReteException re =
          new ReteException("Pattern::FindDeftemplate",
                            "Attempt to duplicate implied deftemplate:",
                            name);
        throw new RuntimeException(re.toString());
      }
    } else  {
      // this is OK. Create an implied deftemplate if this is an ordered pattern.
      if (_ordered == RU.UNORDERED_FACT) {
        ReteException re =
          new ReteException("Pattern::FindDeftemplate",
                            "Attempt to create implied unordered deftemplate:",
                            name);
        throw new RuntimeException(re.toString());
      }
      try {
        deft =
          engine.AddDeftemplate(new Deftemplate(name,_ordered));
      } catch (ReteException re) {
        // this can't happen
        throw new RuntimeException(re.toString());
      }
    }
    return deft;
 }


  /**
    Add a value to this pattern
    */

  public void AddTest(String slotname,Value value, boolean negated) throws ReteException {
    AddTest(RU.putAtom(slotname), value, negated);
  }

  public void AddTest(int slotname, Value value,  boolean negated) throws ReteException {
    if (_ordered == RU.ORDERED_FACT)
      throw new ReteException("Pattern::AddValue",
                              "Attempt to add slot integer to ordered pattern",
                              "");

    // try to find this slotname in the deftemplate
    int i;
    for (i=RU.FIRST_SLOT; i< deft.size(); i+=RU.DT_SLOT_SIZE)
      if (deft.get(i).AtomValue() == slotname)
        break;
    if (i >= deft.size())
      throw new ReteException("Pattern::AddValue",
                              "Attempt to add field with invalid slotname",
                              String.valueOf(slotname));

    int idx = i - RU.FIRST_SLOT;
    idx /= RU.DT_SLOT_SIZE;

    if (tests[idx] == null)
      tests[idx] = new Test1[RU.MAXFIELDS];

    int j=0;
    while (tests[idx][j] != null)
      ++j;
    
    if (negated)
      tests[idx][j] = new Test1(Test1.NEQ, i, value);
    else
      tests[idx][j] = new Test1(Test1.EQ, i, value);
    
  }


  public void AddTest(Value value, boolean negated) throws ReteException {
    if (_ordered == RU.UNORDERED_FACT)
      throw new ReteException("Pattern::AddValue",
                              "Attempt to add ordered field to unordered pattern",
                              "");

    int i = RU.FIRST_SLOT + nvalues* RU.DT_SLOT_SIZE;

    if (nvalues < RU.MAXFIELDS) {
      
      if (tests[nvalues] == null)
        tests[nvalues] = new Test1[RU.MAXFIELDS];
      
      int j=0;
      while (tests[nvalues][j] != null)
        ++j;

      if (negated)
        tests[nvalues][j] = new Test1(Test1.NEQ, i, value);
      else
        tests[nvalues][j] = new Test1(Test1.EQ, i, value);

    } else
      throw new ReteException("Pattern::AddValue",
                              "MaxFields exceeded",
                              "");
  }

  /**
    point at the next slot.
    */
  
  public void advance() {
    ++nvalues;
  }

  /**
    Make this pattern a (not()) CE pattern.
    */

  public void setNegated() {
    _negated = true;
  }

  public boolean negated() {
    return _negated;
  }

  public int classname() {
    return _class;
  }

  public int ordered() {
    return _ordered;
  }

  /**
    Shrink all our subarrays to the needed size.
   */

  public void compact() {
    if (_ordered == RU.ORDERED_FACT) {
      // if this was an unordered fact the 'backbone'
      // would already be the right size; since this is ordered,
      // we allocated it too big.

      Test1[][] nt = new Test1[nvalues][];
      for (int i=0; i< nvalues; i++)
        nt[i] = tests[i];
      tests = nt;
    }
    
    int size = tests.length;
    for (int i=0; i < size; i++) {
      // now create new subarrays of just the right size.
      if (tests[i] == null)
        continue;
      int n = 0;
      while (tests[i][n] != null) {
        n++;
      }
      if (n != 0) {
        Test1[] nt = new Test1[n];
        for (int j=0; j<n; j++)
          nt[j] = tests[i][j];
        tests[i] = nt;
      } else {
        tests[i] = null;
      }
    }
  }

  /**
    Describe myself
    */

  public String toString() {
    String s = "[Pattern: " + RU.getAtom(_class) + " ";
    s += (_ordered == RU.ORDERED_FACT) ? "(ordered)" : "(unordered)";

    s += "]";
    return s;
  }
  


}

