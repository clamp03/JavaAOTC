// -*- java -*-
//////////////////////////////////////////////////////////////////////
// ReteException.java
// A special type of exception for the expert system
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////



package spec.benchmarks._202_jess.jess;

/**
 Detailed exception with lots of info
@author E.J. Friedman-Hill (C)1996
*/

public class ReteException extends Throwable {

  String routine, text1, text2;
  /**
    Constructor
@param Routine the routine that threw this exception
@param Text1 an informational message
@param Text2 usually some data    
    */

  public ReteException(String routine, String text1, String text2) {
    this.routine = routine;
    this.text1 = text1;
    this.text2 = text2;
  }

  
  /**
    How things get printed
    */

  public String toString() {
    String s = "Rete Exception in routine " + routine + ".\n";
    s += "  Message: " + text1 + " " + text2 + ".";
    return s;
  }


}
