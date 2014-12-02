// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Userfunction.java
// Interface for user-defined functions
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////



package spec.benchmarks._202_jess.jess;

/**
  A User defined function interface. I suggest that this be used to
 implement multiple functions per subclass by using the 1st argument
 as a selector.

@author E.J. Friedman-Hill (C)1996
*/

public interface Userfunction {

  public int name();
  
  public Value Call(ValueVector vv, Context context) throws ReteException;

}
