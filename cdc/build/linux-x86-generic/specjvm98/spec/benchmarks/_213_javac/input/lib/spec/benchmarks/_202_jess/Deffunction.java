// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Deffunction.java
//  Class used to represent Deffunctions.
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// TEST ONLY - NOT FOR DISTRIBUTION
//////////////////////////////////////////////////////////////////////
package spec.benchmarks._202_jess;


import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream; //**NS**

import java.lang.System;
import java.lang.Integer;
import java.lang.Character;

import java.util.BitSet; //**NS**
import java.util.Enumeration;
import java.util.Stack;
import java.util.Hashtable;
import java.util.Vector;
/**
 Class used to represent Deffunctions.

 A Deffunction has no ValueVector representation; it is always a
 class object.

@author E.J. Friedman-Hill (C)1996
*/

public class Deffunction extends Context {
  
  public int name;
  public String docstring = null;
  private int nargs = 0;

  /**
    Constructor.
    */

  public Deffunction(String name, Rete engine) {
    super(engine);
    this.name = RU.putAtom(name);
  }

  /**
    Add a formal argument to this deffunction.
   */

  public void AddArgument(String name) {
    AddBinding(RU.putAtom(name), nargs + 1, 0);
    ++nargs;
  }

  /**
    Execute this deffunction. For each action (ValueVector form of a Funcall),
    do two things:
    1) Call ExpandAction to do variable substitution and
       subexpression expansion
    2) call Funcall.Execute on it.


    Vars is the ValueVector form of the funcall that we're executing in.
   */

  public Value Call(ValueVector call) throws ReteException {
    
    Value result = null;

    if (call.size() < (nargs + 1))
      throw new ReteException("Deffunction::Fire",
                              "Too few arguments to Deffunction",
                              RU.getAtom(name));

    Push();
    ClearReturnValue();

    // set up the variable table
    int size = bindings.size();
    for (int i=0; i<size; i++) {
      Binding b = (Binding) bindings.elementAt(i);
      if (b.slotIndex == RU.LOCAL)
        // no default binding for locals
        continue;
      // all others variables come from arguments
      b.val = call.get(b.factIndex);
    }

    // OK, now run the function. For every action...
    size = actions.size();
    for (int i=0; i<size; i++) {

      // do all substitutions on this action
      ValueVector original = (ValueVector) actions.elementAt(i);
      ValueVector action = ExpandAction(original);
      result = Funcall.Execute(action, this);
      if (_return) {
        result = _retval;
        ClearReturnValue();
        break;
      }
    }
    Pop();
    return result;
  }
  

  private void debugPrint(Vector facts) throws ReteException {
    engine.display.stdout().print("FIRE " + toString());
    for (int i=0; i<facts.size(); i++) {
      ValueVector f = (ValueVector) facts.elementAt(i);
      engine.display.stdout().print(" f-" + f.get(RU.ID).FactIDValue());
      if (i< facts.size() -1)
        engine.display.stdout().print(",");
    }
    engine.display.stdout().println();
  }
  
  /**
    Describe myself
    */

  public String toString() {
    String s = "[Deffunction: " + RU.getAtom(name) + " ";
    if (docstring != null)
      s += "\"" + docstring + "\"; ";
    s += "]";
    return s; 
  }

}

