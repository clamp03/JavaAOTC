// -*- java -*-
//////////////////////////////////////////////////////////////////////
// NullDisplay.java
// A Null JESS GUI
// It extends observable so that it can send messages to the
// network viewer tool
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// $Id: NullDisplay.java,v 1.4 1997/12/02 01:14:22 ejfried Exp $
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess;

import spec.benchmarks._202_jess.jess.*;
import java.util.*;

/**
 A Null JESS GUI
@author E.J. Friedman-Hill (C)1996
*/

public class NullDisplay extends Observable implements ReteDisplay {

  public void AssertFact(ValueVector fact) {}
  public void RetractFact(ValueVector fact) {}

  public void AddDeffacts(Deffacts df) {}
  public void AddDeftemplate(Deftemplate dt) {}

  public void AddDefrule(Defrule rule)
  {
    // The network has chenged. Notify anyone who cares..
    setChanged();
    notifyObservers();
  }
  public void ActivateRule(Defrule rule) {}
  public void DeactivateRule(Defrule rule) {}
  public void FireRule(Defrule rule) {System.gc();}

  public java.io.PrintStream stdout() {return spec.harness.Context.out;}
  public java.io.InputStream stdin() {return System.in;}
  public java.io.PrintStream stderr() {return System.err;}
  ///public java.applet.Applet applet() { return null; }
}
