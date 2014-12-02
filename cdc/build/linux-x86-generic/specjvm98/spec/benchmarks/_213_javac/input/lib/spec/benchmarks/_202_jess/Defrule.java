// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Defrule.java
//  Class used to represent Defrules.
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
 Class used to represent Defrules.

 A Defrule has no ValueVector representation; it is always a
 class object.

@author E.J. Friedman-Hill (C)1996
*/

public class Defrule extends Context {
  
  private static int next_rule_id = 0;
  public int name;
  public Vector patts;
  public String docstring = null;
  private int _id;
  private boolean have_negated_patterns = false;
  public int salience = 0;
  private boolean _frozen = false;
  private ValueVector[] _actions;
  /**
    Constructor.
    */

  public Defrule(String name, Rete engine) {
    super(engine);
    this.name = RU.putAtom(name);
    this._id = next_rule_id++;
    patts = new Vector();
  }

  /**
    The id is read-only!
  */

  public int id() { return _id; }
  
  /**
    Add a pattern to this Defrule
    */

  public void AddPattern(Pattern pattern) throws ReteException {
    patts.addElement(pattern);
    if (pattern.negated())
      have_negated_patterns = true;
    
    pattern.compact();

    // Look for variables in the fact, and create bindings for new ones
    for (int i=0; i< pattern.tests.length; i++) {
      if (pattern.tests[i] == null)
        continue;
      for (int j=0; j< pattern.tests[i].length; j++) {
        if (pattern.tests[i][j].slot_value.type() == RU.VARIABLE) {
          if (findBinding(pattern.tests[i][j].slot_value.VariableValue()) == null)
            if (pattern.tests[i][j].test == Test1.EQ) {
              AddBinding(pattern.tests[i][j].slot_value.VariableValue(), patts.size()-1,
                         RU.FIRST_SLOT + i);
            } else
              throw new ReteException("Defrule::AddPattern",
                                      "First reference to variable negated",
                                      pattern.tests[i][j].slot_value.toString());
        }
      }
    }
  }

  public Pattern LastPattern() {
    return (Pattern) patts.lastElement();
  }


  /**
    Tell this rule to modify its binding table to account for negated patterns, and
    Set the actions up for faster execution
        */

  public void freeze() {
    if (_frozen) return;
    _frozen = true;

    // make a table of indexes which will then be used to modify the bindings.
    int[] indexes = new int[patts.size()];
    for (int i=0; i< indexes.length; i++)
      indexes[i] = i;

    for (int i=0; i< indexes.length; i++)
      if (((Pattern) patts.elementAt(i)).negated())
        for (int j=i; j< indexes.length; j++)
          -- indexes[j];

    int size = bindings.size();
    for (int i=0; i<size; i++) {
      Binding b = (Binding) bindings.elementAt(i);
      if (b.slotIndex == RU.LOCAL)
        // no default binding for locals
        continue;
      // all others variables need info from a fact
      b.factIndex = indexes[b.factIndex];
    }

    // now speed up the actions too
    _actions = new ValueVector[actions.size()];
    for (int i=0; i<_actions.length; i++)
      _actions[i] = (ValueVector) actions.elementAt(i);

  }
  

  public ValueVector[] Ready(ValueVector[] fact_input) throws ReteException {

    ValueVector[] facts = fact_input;
    ValueVector fact;
    // set up the variable table
    int size = bindings.size();
    for (int i=0; i<size; i++) {
      Binding b = (Binding) bindings.elementAt(i);
      if (b.slotIndex == RU.LOCAL)
        // no default binding for locals
        continue;

      // all others variables need info from a fact
      // if this is a not CE, skip it;
      fact = facts[b.factIndex];
      try {
        if (b.slotIndex == RU.PATTERN) {
          b.val = fact.get(RU.ID);
        } else {
          b.val = fact.get(b.slotIndex);
        }
      } catch (Throwable t) {
        // bad binding. These can come from bogus bindings in not CE's.
      }
      
    }
    return facts;
  }
  

  /**
    Do the RHS of this rule.For each action (ValueVector form of a Funcall),
    do two things:
    1) Call ExpandAction to do variable substitution and
       subexpression expansion
    2) call Funcall.Execute on it.

    Fact_input is the Vector of ValueVector facts we were fired with.
   */

  public void Fire(ValueVector[] fact_input) throws ReteException {
    if (engine.watch_rules)
      debugPrint(fact_input);
    
    Push();
    ClearReturnValue();

    // we may need to adjust the fact_input vector to reflect the real
    // layout of patterns; some of our patterns may be 'not's, so there can be
    // fewer facts than patterns.

    ValueVector[] facts = Ready(fact_input);

    // OK, now run the rule. For every action...
    int size = _actions.length;
    for (int i=0; i<size; i++) {

      // do all substitutions on this action
      ValueVector original = (ValueVector) _actions[i];
      ValueVector action = ExpandAction(original);
      Funcall.Execute(action, this);
      if (_return) {
        ClearReturnValue();
        Pop();
        return;
      }
    }
    Pop();
  }
  

  private void debugPrint(ValueVector[] facts) throws ReteException {
    engine.display.stdout().print("FIRE " + toString());
    for (int i=0; i<facts.length && facts[i] != null; i++) {
      ValueVector f = facts[i];
      engine.display.stdout().print(" f-" + f.get(RU.ID).FactIDValue());
      if (i< facts.length -1)
        engine.display.stdout().print(",");
    }
    engine.display.stdout().println();
  }
  
  /**
    Describe myself
    */

  public String toString() {
    String s = "[Defrule: " + RU.getAtom(name) + " ";
    if (docstring != null)
      s += "\"" + docstring + "\"; ";
    s += patts.size() + " patterns; salience: " + salience;
    s += "]";
    return s; 
  }

}

