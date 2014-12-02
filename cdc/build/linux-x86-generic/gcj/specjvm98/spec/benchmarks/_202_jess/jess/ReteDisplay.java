// -*- java -*-
//////////////////////////////////////////////////////////////////////
// ReteDisplay.java
// An interface to a JESS GUI
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

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

  ///public java.applet.Applet applet();

}
