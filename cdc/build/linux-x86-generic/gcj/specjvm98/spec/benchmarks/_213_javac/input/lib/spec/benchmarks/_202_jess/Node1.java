// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Node1.java
// Single-input nodes of the pattern network
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
 Single-input nodes of the pattern network
@author E.J. Friedman-Hill (C)1996
*/

public class Node1 extends Node {
  

  /**
    Test slot value and type. value in R1, type in R2, 
    absolute index of slot start in R3
   */

  final static public int TEQ = 1;

  /**
    Test class type; class in R1, orderedness in R2, fact length in R3. 
   */

  final static public int TECT = 2;

  /**
    Test that two slots in the same fact have the same type and value.
    Absolute index of 1st slot start in R1, second in R2.
   */

  final static public int TEV1 = 3;

  /**
    Test that two slots in the same fact DO NOT have the same type and value.
    Fails if either type or value differ.
    Absolute index of 1st slot start in R1, second in R2.
   */

  final static public int TNEV1 = 4;

  /**
    Test that a slot value and type are NOT that same as some type and value;
    test passes if either type or value differs. value in R1, type in R2, 
    absolute index of slot start in R3
   */

  final static public int TNEQ = 5;

  public int R1, R2, R3;
  public Value value;

  /**
    Constructor
    */

  public Node1(int command, int R1, int R2, int R3, Rete engine) {
    super(engine);
    this.command = command;
    this.R1 = R1;
    this.R2 = R2;
    this.R3 = R3;
  }

  public Node1(int command, Value value, int R3, Rete engine) {
    super(engine);
    this.command = command;
    this.value = value;
    this.R3 = R3;
  }

  /**
    Do the business of this node.
    The input token of a Node1 should only be single-fact tokens.
    In this version, both ADD and REMOVE Tokens are handled the same
    way; I think this is correct. We don't test the token to see if
    there's a fact there, for speed's sake; if we throw an exception,
    well, there's a bug!
    */
  
  public boolean CallNode(Token token, int callType) throws ReteException {
    
    boolean result = false;
    ValueVector fact = token.facts[0];

    switch (command) {

    case TECT: // test class type
      if (fact.size() == R3 && fact.get(RU.CLASS).AtomValue() == R1
          && fact.get(RU.DESC).DescriptorValue() == R2)
        result = true;
      break;

    case TEQ: // test a slot for a value
      if (value.type() == RU.FUNCALL) {
        if (fact.size() >= R3 &&
            Funcall.Execute(Eval(value, token),
                            engine.global_context).equals(Funcall.TRUE()))
          result = true;
      } else
        if (fact.size() >= R3 && fact.get(R3).equals(value))
          result = true;
      break;
      
    case TNEQ: // test a slot for NOT a value
      if (value.type() == RU.FUNCALL) {
        if (fact.size() >= R3 &&
            ! Funcall.Execute(Eval(value, token),
                            engine.global_context).equals(Funcall.TRUE()))
          result = true;
      } else
        if (fact.size() >= R3 && !fact.get(R3).equals(value))
          result = true;
      break;
        
    case TEV1: // check for two identical variable slots
      if (fact.size() >= R1 && fact.size() >= R2 &&
          fact.get(R1).equals(fact.get(R2)))
        result = true;
      break;

    case TNEV1: // check for two identical variable slots
      if (fact.size() >= R1 && fact.size() >= R2 &&
          !fact.get(R1).equals(fact.get(R2)))
        result = true;
      break;
      
    default: // invalid command
      throw new ReteException("Node1::CallNode",
                              "invalid command code:",
                              String.valueOf(command));
    }

    if (result)
      // Pass this token on to all successors.
      for (int i=0; i< _succ.length; i++) {
        Successor s = _succ[i];
        s.node.CallNode(token, s.callType);
      }      
    //debugPrint(token, callType, fact, result);
    return result;
  }
  
  /**
    callNode can call this to print debug info
    */

  private void debugPrint(Token token, int callType, ValueVector fact, boolean result) throws ReteException {
    
  }


  public String toString() {
    String s ="[Node1 command=";


    switch (command) {
    case TEQ:
      s += "TEQ;data=";
      switch (R2) {
      case RU.ATOM:
      case RU.STRING:
        s += RU.getAtom(R1); break;
      default:
        s += String.valueOf(R1); break;
      }
      s += ";type=" + R2 + ";idx=" + R3; break;
    case TNEQ:
      s += "TNEQ;data=";
      switch (R2) {
      case RU.ATOM:
      case RU.STRING:
        s += RU.getAtom(R1); break;
      default:
        s += String.valueOf(R1); break;
      }
      s += ";type=" + R2 + ";idx=" + R3; break;

    case TECT:
      s += "TECT;class=" + RU.getAtom(R1) + ";ordr=" + R2;  break;
    case TEV1:
      s += "TEV1;idx1=" + R1 + ";idx2=" + R2; break;
    case TNEV1:
      s += "TNEV1;idx1=" + R1 + ";idx2=" + R2; break;
    }

    s += "]";
    return s;
  }


}





