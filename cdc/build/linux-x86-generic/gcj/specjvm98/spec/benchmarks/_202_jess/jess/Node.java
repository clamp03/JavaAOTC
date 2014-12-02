// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Node.java
// Parent class of all nodes of the pattern network
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.util.*;

/**
 Ancestor of all nodes of the pattern network
@author E.J. Friedman-Hill (C)1996
*/

public abstract class Node  {
  
  final static int LEFT     = 0;
  final static int RIGHT    = 1;
  final static int SINGLE   = 2;
  final static int ACTIVATE = 3;


  /**
    What kind of node is this?
   */
  
  int command;

  /**
    Succ is the list of this node's successors
   */

  Vector succ;
  public final Vector succ() { return succ; }
  protected Successor[] _succ;
  
  protected Rete engine;

  /**
    Constructor
   */

  Node(Rete engine) {
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
  void freeze() {
    _succ = new Successor[succ.size()];
    for (int i=0; i< succ.size(); i++) {
      _succ[i] = (Successor) succ.elementAt(i);
      _succ[i].node.freeze();
    }
    
  }

  /**
    Do the business of this node.
   */

  abstract boolean CallNode(Token token, int callType) throws ReteException;


}





