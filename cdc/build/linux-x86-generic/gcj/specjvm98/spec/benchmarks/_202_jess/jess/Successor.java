// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Successor.java
// Successor is a holder class for a Rete node and how to call it
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////



package spec.benchmarks._202_jess.jess;

/**
 A Successor is used to tell Nodes how to interact
@author E.J. Friedman-Hill (C)1996
*/

public class Successor {


  public Node node;
  public int callType;

  /**
    Create a successor. callType should be either Node.LEFT, Node.RIGHT,
    Node.SINGLE, or Node.ACTIVATE.
    */

  public Successor(Node node, int callType) {
    this.node = node;
    this.callType = callType;
  }

}
