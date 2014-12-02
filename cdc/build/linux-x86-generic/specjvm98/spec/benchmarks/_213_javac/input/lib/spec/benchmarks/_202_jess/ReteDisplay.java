// -*- java -*-
//////////////////////////////////////////////////////////////////////
// ReteDisplay.java
// An interface to a JES GUI
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// TEST ONLY - NOT FOR DISTRIBUTION
//////////////////////////////////////////////////////////////////////
package spec.benchmarks._202_jess;

public interface ReteDisplay {

  public void AssertFact(ValueVector fact);
  public void RetractFact(ValueVector fact);

  public void AddDeffacts(Deffacts df);
  public void AddDeftemplate(Deftemplate dt);

  public void AddDefrule(Defrule rule);
  public void ActivateRule(Defrule rule);
  public void DeactivateRule(Defrule rule);
  public void FireRule(Defrule rule);

  public java.io.PrintStream stdout();
  public java.io.InputStream stdin();
  public java.io.PrintStream stderr();
}
