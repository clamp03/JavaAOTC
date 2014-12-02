// -*- java -*-
//////////////////////////////////////////////////////////////////////
// NodeTerm.java
//   Terminal nodes of the pattern network
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.util.*;

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

  Defrule rule;
  /**
    Are we being installed or are we old?
    */

  private boolean installing;

  /**
    Has our rule been deleted?
    */
  private boolean active = true;

  public final boolean active() { return active; }
 
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
    installing = true;
  }
  
  /**
    An activation has been cancelled; forget it
    */

  public void standDown(Activation a) throws ReteException {
    activations.removeElement(a);
    engine.standDown(a);
  }

  /**
    An activation has been fired; forget it
    */

  public void ruleFired(Activation a) {
    activations.removeElement(a);
    engine.ruleFired(a);
  }

  /**
    Add an activation to this rule
    */
     
  private void doAddCall(Token token) throws ReteException {
    Activation a = new Activation(token, rule, this);
    activations.addElement(a);
    engine.AddActivation(a);
    installing = false;
  }


  /**
    All we need to do is create or destroy the appropriate Activation
    object, which contains enough info to fire a rule.
    */
  
  public boolean CallNode(Token token, int callType) throws ReteException {
    if (!active)
      return true;

    // debugPrint(token, callType);
    switch (token.tag) {
      
    case RU.UPDATE:
      {
        if (installing)
          doAddCall(token);
        break;
      }

    case RU.ADD:
      {
        doAddCall(token);
        break;
      }

    case RU.REMOVE:
      {
        int size = activations.size();
        for (int i=0; i < size; i++) {
          Activation a = (Activation) activations.elementAt(i);
          if (token.data_equals(a.token)) {
            standDown(a);
            return true;
          }
        }
        break;
      }
    }
    return true;
  }

  void deactivate() throws ReteException
  {
    for (int i=0; i< activations.size(); i++)
      standDown((Activation) activations.elementAt(i));
    active=false;
  }

  
  /**
    callNode can call this to show debug info
    */

  private void debugPrint(Token token, int callType) {
    spec.harness.Context.out.println("TEST " + toString() +";tag=" + token.tag + "callType="+callType);
  }

  /**
    Describe myself
    */
  
  public String toString() {
    String s ="[NodeTerm rule=";

    s += RU.getAtom(rule.name);

    s += ";active=" + active + "]";
    return s;
  }


}





