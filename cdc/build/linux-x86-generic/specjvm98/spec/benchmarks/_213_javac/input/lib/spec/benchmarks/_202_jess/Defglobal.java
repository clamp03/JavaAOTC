// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Defglobal.java
//  Class used to represent Defglobals.
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
 Class used to represent Defglobals.

 A Defglobal has no ValueVector  representation; it is always a
 class object.

@author E.J. Friedman-Hill (C)1996
*/

public class Defglobal {
  
  public Vector bindings;

  /**
    Constructor.
    */

  public Defglobal() {
    bindings = new Vector();
  }
  
  /**
    Add a variable to this Defglobal
    */


  public void AddGlobal(String name, Value val) {
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

