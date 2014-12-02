// -*- java -*-
//////////////////////////////////////////////////////////////////////
// ContextState.java
// An execution context state
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.util.*;

/**
 An execution context state
@author E.J. Friedman-Hill (C)1996
*/

class ContextState {
  public Vector bindings;
  public boolean _return;
  public Value _retval;
}
