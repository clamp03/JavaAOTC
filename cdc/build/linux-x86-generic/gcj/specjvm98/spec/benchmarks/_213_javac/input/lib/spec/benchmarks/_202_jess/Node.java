// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Node.java
// Parent class of all nodes of the pattern network
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
 Ancestor of all nodes of the pattern network
@author E.J. Friedman-Hill (C)1996
*/

public abstract class Node  {
  
  final public static int LEFT     = 0;
  final public static int RIGHT    = 1;
  final public static int SINGLE   = 2;
  final public static int ACTIVATE = 3;


  /**
    What kind of node is this?
   */
  
  public int command;

  /**
    Succ is the list of this node's successors
   */

  public Vector succ;
  protected Successor[] _succ;
  
  protected Rete engine;

  /**
    Constructor
   */

  public Node(Rete engine) {
    succ = new Vector();
    this.engine = engine;
  }

  protected ValueVector Eval(Value v, Token t) throws ReteException {
    ValueVector vv = (ValueVector) v.FuncallValue().clone();

    for (int i=0; i<vv.size(); i++) {
      if (vv.get(i).type() ==  RU.INTARRAY) {
        int[] binding = vv.get(i).IntArrayValue();
        vv.set(((ValueVector)t.facts[binding[0]]).get(binding[1]),
               i);
      } else if (vv.get(i).type() ==  RU.FUNCALL)
        vv.set(new Value(Eval(vv.get(i), t), RU.FUNCALL), i);
    }
    return vv;
  }

  /**
    Move the successors into an array
    */
  public void freeze() {
    _succ = new Successor[succ.size()];
    for (int i=0; i< succ.size(); i++) {
      _succ[i] = (Successor) succ.elementAt(i);
      _succ[i].node.freeze();
    }
    
  }

  /**
    Do the business of this node.
   */

  public abstract boolean CallNode(Token token, int callType) throws ReteException;


}





