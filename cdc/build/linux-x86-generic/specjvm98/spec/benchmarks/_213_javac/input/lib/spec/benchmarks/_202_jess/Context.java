// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Context.java
// An execution context in the Jess expert system shell
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
 An execution context for Funcalls. To be used as a base class for Defrule,
 Deffunction, etc.
@author E.J. Friedman-Hill (C)1996
*/

public class Context {
  public Vector actions;
  public Vector bindings;
  public Rete engine;
  public Stack states;

  protected boolean _return = false;
  protected Value _retval;

  public Context(Rete engine) {
    actions = new Vector();
    bindings = new Vector();
    this.engine = engine;
    states = new Stack();
  }
  
  public boolean Returning() {
    return _return;
  }

  public Value SetReturnValue(Value val) {
    _return = true;
    _retval = val;
    return val;
  }

  public Value GetReturnValue() {
    return _retval;
  }

  public void ClearReturnValue() {
    _return = false;
    _retval = null;
  }

  public void Push() {
    ContextState cs = new ContextState();
    cs._return = _return;
    cs._retval = _retval;
    cs.bindings = new Vector();
    for (int i=0; i<bindings.size(); i++) 
      cs.bindings.addElement(((Binding) bindings.elementAt(i)).Clone());
    states.push(cs);
  }
  
  public void Pop() {
    try {
      ContextState cs = (ContextState) states.pop();
      _return = cs._return;
      _retval = cs._retval;
      bindings = cs.bindings;
    } catch (java.util.EmptyStackException ese) {
      // tough break.
    }
  }

  /**
    Find a variable table entry
    */
  
  protected Binding findBinding(int name) {
    for (int i=0; i< bindings.size(); i++) {
      Binding b = (Binding) bindings.elementAt(i);
      if (b.name == name)
        return b;
    }
    return engine.global_context.findGlobalBinding(name);
  }
  
  /**
    Add a Funcall to this context
    */

  final public void AddAction(Funcall funcall) {
    AddAction(funcall.vvec);
  }

  final public void AddAction(ValueVector funcall) {
    actions.addElement(funcall);
  }


  /**
    Make note of a variable binding during parsing,
    so it can be used at runtime; the fact and slot indexes are for the use
    of the subclasses. Defrules use factIndex and slotIndex to indicate where in
    a fact to get data from; Deffunctions just use factIndex to indicate which
    element of the argument vector to pull their information from.
    */

  final public Binding AddBinding(int name, int factIndex, int slotIndex) {
    Binding b = new Binding(name, factIndex, slotIndex);
    bindings.addElement(b);
    return b;
  }

  /**
    Set a (possibly new!) variable to some type and value
    */

  final public Binding SetVariable(int name, Value value) {
    Binding b = findBinding(name);
    if (b == null) 
     b = AddBinding(name, RU.LOCAL, RU.LOCAL);
    b.val = value;

    return b;
  }

    /**
    Recursive function which expands an action ValueVector by doing
    variable substitutions.
@returns A copy of the action, expanded.
   */

  public ValueVector ExpandAction(ValueVector original) throws ReteException {

    // Make a local copy 
    ValueVector action = (ValueVector) original.clone();
    String functor = action.get(0).StringValue();            
    // do variable replacements, using current values;
    // note that these can change during a rule execution!
    for (int j=1; j<action.size(); j++) {
      
      // -*- sigh -*-
      // For very few forms, a variable is treated as an lvalue and
      // therefore must not be substituted here. For now, only
      // bind is treated this way: Just leave the first arg alone.
      // Note that the actual bind is still executed in Funcall.
      
      Value current = action.get(j);      
      if (functor.equals("bind") && j == 1) {
        Binding b = findBinding(current.VariableValue());
        if (b == null) 
          // This had better be an lvalue...
          b = SetVariable(current.VariableValue(), null);
        continue;
      } else if ((functor.equals("if") && j > 1) || functor.equals("while")
                 || functor.equals("and") || functor.equals("or"))
        break;

      switch (current.type()) {
      case RU.VARIABLE:
        {
          Binding b = findBinding(current.VariableValue());
          if (b == null) {
            // This had better be an lvalue...
            b = SetVariable(current.VariableValue(), null);
          } else {
            // plug in current values. 
            action.set(b.val, j);
          }
          break;
        } 
      case RU.FUNCALL:
        {
          ValueVector subaction = current.FuncallValue();
          action.set(new Value(ExpandAction(subaction), RU.FUNCALL), j);
          break;
        }
      case RU.ORDERED_FACT:
      case RU.UNORDERED_FACT:
        {
          ValueVector fact = current.FactValue();
          action.set(new Value(ExpandFact(fact), current.type()), j);
          break;
        }
      case RU.LIST:
        {
          ValueVector list = current.ListValue();
          action.set(new Value(ExpandList(list), current.type()), j);
          break;
        }
      }
    }
    return action;
  }
  
  /**
    Do varsubs in facts
    */
  
  public ValueVector ExpandFact(ValueVector original) throws ReteException {
    ValueVector fact = (ValueVector) original.clone();

    for (int i=RU.FIRST_SLOT; i<fact.size(); i++) {
      switch (fact.get(i).type()) {
      case RU.VARIABLE:
        {
          Binding b = findBinding(fact.get(i).VariableValue());
          fact.set(b.val, i);
          break;
        }
      case RU.FUNCALL:
        {
          fact.set(new Value(ExpandAction(fact.get(i).FuncallValue()), RU.FUNCALL), i);
        }
      }
    }
    return fact;
  }

  /**
    Do varsubs in lists
    */

  public ValueVector ExpandList(ValueVector original) throws ReteException {

    ValueVector list = (ValueVector) original.clone();

    for (int i=0; i<list.size(); i++) {
      switch (list.get(i).type()) {
      case RU.VARIABLE:
        {
          Binding b = findBinding(list.get(i).VariableValue());
          list.set(b.val, i);
          break;
        }
      case RU.FUNCALL:
        {
          list.set(new Value(ExpandAction(list.get(i).FuncallValue()), RU.FUNCALL), i);
          break;
        }
      }
    }
    
    return list;
  }
  

}

