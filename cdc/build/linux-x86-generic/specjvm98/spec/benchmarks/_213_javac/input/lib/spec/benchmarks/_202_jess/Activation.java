// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Activation.java
// A single activation of a rule.
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// TEST ONLY - NOT FOR DISTRIBUTION
//////////////////////////////////////////////////////////////////////

/**
 An activation of a rule. Contains enough info to bind
 a rule's variables.

@author E.J. Friedman-Hill (C)1996
*/
package spec.benchmarks._202_jess;

public class Activation {

  /**
    Token is the token that got us fired.
   */
  
  public Token token;

  /**
    Rule is the rule we will fire.
   */

  public Defrule rule;

  /**
    Nt is the NodeTerm that created us; we need to notify it when
    we die.
   */

  private NodeTerm nt;


  /**
    Constructor
    */

  public Activation(Token token, Defrule rule, NodeTerm nt) {
    this.token = token;
    this.rule = rule;
    this.nt = nt;
  }

  /**
    Fire my rule
    */

  public boolean Fire() throws ReteException {
    rule.Fire(token.facts);
      
    nt.standDown(this);
    return true;
  }


}

