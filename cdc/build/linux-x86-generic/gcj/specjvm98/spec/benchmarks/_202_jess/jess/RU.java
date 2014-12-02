// -*- java -*-
//////////////////////////////////////////////////////////////////////
// RU.java
// Utilities for Java expert system
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.util.*;

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

  final public static int NONE             =     0;
  final public static int ATOM             =     1;
  final public static int STRING           =     2;
  final public static int INTEGER          =     4;
  final public static int VARIABLE         =     8;
  final public static int FACT_ID          =    16;
  final public static int FLOAT            =    32;
  final public static int FUNCALL          =    64;
  final public static int ORDERED_FACT     =   128;
  final public static int UNORDERED_FACT   =   256;
  final public static int LIST             =   512;
  final public static int DESCRIPTOR       =  1024;
  final public static int EXTERNAL_ADDRESS =  2048;
  final public static int INTARRAY         =  4096;
  final public static int MULTIVARIABLE    =  8192;
  final public static int SLOT             = 16384;
  final public static int MULTISLOT        = 32768;

  /**
    The three actions for tokens
   */

  final static int ADD       = 0;
  final static int REMOVE    = 1;
  final static int UPDATE    = 2;

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
  
  static int gensym_idx = 0;

  /**
    putAtom stores an atom in the hash table, avoiding collisions
   */

  public static synchronized int putAtom(String atom) {
    
    int hci = atom.hashCode();
    Integer hc = new Integer(hci);
    String n = null;
    boolean flag = true;
    while (flag == true) {
      n = (String) atoms.get(hc);
      if (n != null && n.equals(atom))
        return hci;
      if (n == null) {
        atoms.put(hc,atom);
        return hci;
      }
      hc = new Integer(++hci);
    } 
    // NOT REACHED
    return 0;
  }

  /**
    grabs an atom from the integer hash code.
    */

  public static String getAtom(int hc) {
    return (String) atoms.get(new Integer(hc));
  }

  public static synchronized int gensym(String prefix) {
    String sym = prefix + gensym_idx;
    while (getAtom(sym.hashCode()) != null) {
      ++gensym_idx;
      sym = prefix + gensym_idx;
    }

    return putAtom(sym);
  }


}

