// -*- java -*-
//////////////////////////////////////////////////////////////////////
// NodeTerm.java
//   Terminal nodes of the pattern network
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// TEST ONLY - NOT FOR DISTRIBUTION
//////////////////////////////////////////////////////////////////////
package spec.benchmarks._202_jess;
// import java.util.*;

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
  Terminal nodes of the pattern network
  Package up the info needed to fire a rule and create an Activation
  object containing this info.
    
@author E.J. Friedman-Hill (C)1996
*/

public class NodeTerm extends Node {
  
  /**
    The rule we activate
   */

  private Defrule rule;
  
  /**
    Activations we've created. We need to keep an eye on them so that
    we can retract them if need be.
   */

  private Vector activations = new Vector();

  /**
    Constructor
    */

  public NodeTerm(Defrule rule, Rete engine) {
    super(engine);
    this.rule = rule;
  }
  
  /**
    An activation has either fired or been cancelled; forget it
    */

  public void standDown(Activation a) {
    activations.removeElement(a);
    engine.standDown(a);
  }


  /**
    All we need to do is create or destroy the appropriate Activation
    object, which contains enough info to fire a rule.
    */
  
  public boolean CallNode(Token token, int callType) {
    //debugPrint(token, callType);
    if (token.tag == RU.ADD) {
      Activation a = new Activation(token, rule, this);
      activations.addElement(a);
      engine.AddActivation(a);
    } else { // tag == RU.REMOVE
      int size = activations.size();
      for (int i=0; i < size; i++) {
        Activation a = (Activation) activations.elementAt(i);
        if (token.data_equals(a.token)) {
          standDown(a);
          return true;
        }
      }
    }
    return true;
  }

  
  /**
    callNode can call this to show debug info
    */

  private void debugPrint(Token token, int callType) {
  }

  /**
    Describe myself
    */
  
  public String toString() {
    String s ="[NodeTerm rule=";

    s += RU.getAtom(rule.name);

    s += "]";
    return s;
  }


}



