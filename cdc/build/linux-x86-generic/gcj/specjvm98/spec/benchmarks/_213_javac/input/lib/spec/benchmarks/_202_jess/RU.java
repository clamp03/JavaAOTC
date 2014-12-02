// -*- java -*-
//////////////////////////////////////////////////////////////////////
// RU.java
// Utilities for expert system
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
 Contains static utility functions for expert system.
@author E.J. Friedman-Hill (C)1996
*/

public class RU {

  /**
    Absolute indexes within the fact and deftemplate arrays
   */

  final public static int CLASS      = 0;
  final public static int DESC       = 1;
  final public static int ID         = 2;
  final public static int FIRST_SLOT = 3;

  /**
    Types of CEs
   */

  final public static int NOT_CE     = 1;

  /**
    Relative indexes within a deftemplate's slots
   */

  final public static int DT_SLOT_NAME      = 0;
  final public static int DT_DFLT_DATA      = 1;
  final public static int DT_SLOT_SIZE      = 2;

  /**
    The types of data
    */

  final public static int NONE             =    0;
  final public static int ATOM             =    1;
  final public static int STRING           =    2;
  final public static int INTEGER          =    4;
  final public static int VARIABLE         =    8;
  final public static int FACT_ID          =   16;
  final public static int FLOAT            =   32;
  final public static int FUNCALL          =   64;
  final public static int ORDERED_FACT     =  128;
  final public static int UNORDERED_FACT   =  256;
  final public static int LIST             =  512;
  final public static int DESCRIPTOR       = 1024;
  final public static int EXTERNAL_ADDRESS = 2048;
  final public static int INTARRAY         = 4096;

  /**
    The two actions for tokens
   */

  final static int ADD       = 0;
  final static int REMOVE    = 1;

  /**
    Constants specifying that a variable is bound to a fact-index
    or is created during rule execution
   */

  final static int PATTERN = -1;
  final static int LOCAL   = -2;
  final static int GLOBAL  = -3;
  
  /**
  facts bigger than this can cause problems
    */
  final static int MAXFIELDS = 32;

  /**
    The atom Hashtable is shared by all interpreters; this makes good sense.
   */

  protected static Hashtable atoms = new Hashtable(100);

  /**
    A number used in quickly generating unique symbols.
    */
  
  private static int gensym_idx = 0;


  /**
    putAtom stores an atom in the hash table, avoiding collisions
    **NS** - recode to remove compiler error message
   */

  public static int putAtom(String atom) {
    
    int hci = atom.hashCode();
    Integer hc = new Integer(hci);
    String n = null;

    while ( true ) {
    
      n = (String) atoms.get(hc);
      
      if (n != null && n.equals(atom))
        break;
	
      if (n == null) {
        atoms.put(hc,atom);
        break;
      }
      
      hc = new Integer(++hci);
    } 
    
    return hci;    
  }

  /**
    grabs an atom from the integer hash code.
    */

  public static String getAtom(int hc) {
    return (String) atoms.get(new Integer(hc));
  }

  public static int gensym(String prefix) {
    String sym = prefix + gensym_idx;
    while (getAtom(sym.hashCode()) != null) {
      ++gensym_idx;
      sym = prefix + gensym_idx;
    }

    return putAtom(sym);
  }


}

