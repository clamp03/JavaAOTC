// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Defrule.java
//  Class used to represent Defrules.
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.util.*;

/**
 Class used to represent Defrules.

 A Defrule has no ValueVector representation; it is always a
 class object.

@author E.J. Friedman-Hill (C)1996
*/

public class Defrule extends Context {
  
  int name;
  Vector patts;
  String docstring = null;
  private int _id;
  int salience = 0;
  private boolean _frozen = false;
  private ValueVector[] _actions;
  NodeTerm activator;

  public final int getName() { return name; }
  public final String getDocstring() { return docstring; }

  /**
    Constructor.
    */

  Defrule(String name, Rete engine) {
    super(engine);
    this.name = RU.putAtom(name);
    this._id = engine.next_rule_id();
    patts = new Vector();
  }

  /**
    The id is read-only
  */

  public int id() { return _id; }

  void deactivate() throws ReteException
  {
    activator.deactivate();
  }
  
  /**
    Add a pattern to this Defrule
    */

  void AddPattern(Pattern pattern) throws ReteException {
    patts.addElement(pattern);
    pattern.compact();

    // Look for variables in the fact, and create bindings for new ones
    for (int i=0; i< pattern.tests.length; i++) {
      if (pattern.tests[i] == null)
        continue;
      for (int j=0; j< pattern.tests[i].length; j++) {
        if (pattern.tests[i][j].slot_value.type() == RU.VARIABLE ||
            pattern.tests[i][j].slot_value.type() == RU.MULTIVARIABLE) {
          if (findBinding(pattern.tests[i][j].slot_value.VariableValue())
              == null)
            if (pattern.tests[i][j].test == Test1.EQ) {
              AddBinding(pattern.tests[i][j].slot_value.VariableValue(),
                         patts.size()-1,
                         RU.FIRST_SLOT + i,
                         pattern.tests[i][j].sub_idx);
            } else
              throw new ReteException("Defrule::AddPattern",
                                      "First reference to variable negated",
                                      pattern.tests[i][j].slot_value.toString());
        }
      }
    }
  }

  /**
    Tell this rule to set the actions up for faster execution
        */

  void freeze() {
    if (_frozen) return;

    _frozen = true;

    _actions = new ValueVector[actions.size()];
    for (int i=0; i<_actions.length; i++)
      _actions[i] = (ValueVector) actions.elementAt(i);

  }
  

  ValueVector[] Ready(ValueVector[] fact_input) throws ReteException {

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
          if (b.subIndex == -1)
            b.val = fact.get(b.slotIndex);
          else {
            ValueVector vv = fact.get(b.slotIndex).ListValue();
            b.val = vv.get(b.subIndex);
          }
            
        }
      } catch (Throwable t) {
        // bad binding. These can come from unused bindings in not CE's.
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

  void Fire(ValueVector[] fact_input) throws ReteException {
    if (engine.watch_rules())
      debugPrint(fact_input);
    
    Push();
    ClearReturnValue();

    // Pull needed values out of facts into bindings table
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
  

  void debugPrint(ValueVector[] facts) throws ReteException {
    java.io.PrintStream ps = engine.display().stdout();
    ps.print("FIRE " + toString());
    for (int i=0; i<facts.length && facts[i] != null; i++) {
      ValueVector f = facts[i];
      if (f.get(RU.ID).FactIDValue() != -1)
        ps.print(" f-" + f.get(RU.ID).FactIDValue());
      if (i< facts.length -1)
        ps.print(",");
    }
    ps.println();
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

