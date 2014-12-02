// -*- java -*-
//////////////////////////////////////////////////////////////////////
// NullDisplay.java
// A Null JES GUI
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// TEST ONLY - NOT FOR DISTRIBUTION
//////////////////////////////////////////////////////////////////////
package spec.benchmarks._202_jess;


/**
 A Null JES GUI
@author E.J. Friedman-Hill (C)1996
*/

public class NullDisplay implements ReteDisplay {

  public void AssertFact(ValueVector fact) {}
  public void RetractFact(ValueVector fact) {}

  public void AddDeffacts(Deffacts df) {}
  public void AddDeftemplate(Deftemplate dt) {}

  public void AddDefrule(Defrule rule) {}
  public void ActivateRule(Defrule rule) {}
  public void DeactivateRule(Defrule rule) {}
  public void FireRule(Defrule rule) {}

  public java.io.PrintStream stdout() {return System.out;}
  public java.io.InputStream stdin() {return System.in;}
  public java.io.PrintStream stderr() {return System.err;}
}
