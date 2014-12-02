// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Context.java
// An execution context in the Jess expert system shell
//
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// $Id: Context.java,v 1.7 1997/12/02 01:14:31 ejfried Exp $
//////////////////////////////////////////////////////////////////////


package spec.benchmarks._202_jess.jess;

import java.util.*;

/**
 An execution context for Funcalls. To be used as a base class for Defrule,
 Deffunction, etc.
@author E.J. Friedman-Hill (C)1996
*/

public class Context {
  Vector actions;
  Vector bindings;
  Rete engine;
  Stack states;

  protected boolean _return = false;
  protected Value _retval;

  Context(Rete engine) {
    actions = new Vector();
    bindings = new Vector();
    this.engine = engine;
    states = new Stack();
  }
  
  final boolean Returning() {
    return _return;
  }

  final Value SetReturnValue(Value val) {
    _return = true;
    _retval = val;
    return val;
  }

  final Value GetReturnValue() {
    return _retval;
  }

  final void ClearReturnValue() {
    _return = false;
    _retval = null;
  }

  public final Rete engine() { return engine; }

  void Push() {
    ContextState cs = new ContextState();
    cs._return = _return;
    cs._retval = _retval;
    cs.bindings = new Vector();
    for (int i=0; i<bindings.size(); i++) 
      cs.bindings.addElement(((Binding) bindings.elementAt(i)).clone());
    states.push(cs);
  }
  
  void Pop() {
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
  
  Binding findBinding(int name) {
    for (int i=0; i< bindings.size(); i++) {
      Binding b = (Binding) bindings.elementAt(i);
      if (b.name == name)
        return b;
    }
    return engine.global_context().findGlobalBinding(name);
  }
  
  /**
    Add a Funcall to this context
    */

  final void AddAction(ValueVector funcall) {
    actions.addElement(funcall);
  }


  /**
    Make note of a variable binding during parsing,
    so it can be used at runtime; the fact and slot indexes are for the use
    of the subclasses. Defrules use factIndex and slotIndex to indicate where in
    a fact to get data from; Deffunctions just use factIndex to indicate which
    element of the argument vector to pull their information from.
    */

  final Binding AddBinding(int name, int factIndex, int slotIndex,
                           int subIndex) {
    Binding b = new Binding(name, factIndex, slotIndex, subIndex);
    bindings.addElement(b);
    return b;
  }

  /**
    Set a (possibly new!) variable to some type and value
    */

  final Binding SetVariable(int name, Value value) {
    Binding b = findBinding(name);
    if (b == null) 
     b = AddBinding(name, RU.LOCAL, RU.LOCAL, -1);
    b.val = value;

    return b;
  }

    /**
    Recursive function which expands an action ValueVector by doing
    variable substitutions.
@returns A copy of the action, expanded.
   */

  ValueVector ExpandAction(ValueVector original)
    throws ReteException {

    boolean did_bind = false;

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
        did_bind = true;
        continue;
      } else if ((functor.equals("if") && j > 1) || functor.equals("while")
                 || functor.equals("and") || functor.equals("or"))
        break;

      action.set(ExpandValue(current), j);
    }
    return action;
  }
  
  public Value ExpandValue(Value current) throws ReteException {

      switch (current.type()) {
      case RU.VARIABLE:
      case RU.MULTIVARIABLE:
        {
          Binding b = findBinding(current.VariableValue());
          if (b == null) {
            // This had better be an lvalue...
            b = SetVariable(current.VariableValue(), null);
          } else {
            // plug in current values. 
            if (b.val != null)
              return b.val;
            else
              return Funcall.NIL();
          }
          break;
        } 
      case RU.FUNCALL:
        {
          ValueVector subaction = current.FuncallValue();
          return new Value(ExpandAction(subaction), RU.FUNCALL);
        }
      case RU.ORDERED_FACT:
      case RU.UNORDERED_FACT:
        {
          ValueVector fact = current.FactValue();
          return new Value(ExpandFact(fact), current.type());
        }
      case RU.LIST:
        {
          ValueVector list = current.ListValue();
          list = ExpandList(list);
          list = FlattenList(list);
          return new Value(list, current.type());
        }
      }
      return current;
  }


  /**
    Do varsubs in facts
    */
  
  public ValueVector ExpandFact(ValueVector original) throws ReteException {
    ValueVector fact = (ValueVector) original.clone();

    for (int i=RU.FIRST_SLOT; i<fact.size(); i++) {
      switch (fact.get(i).type()) {
      case RU.VARIABLE:
      case RU.MULTIVARIABLE:
        {
          Binding b = findBinding(fact.get(i).VariableValue());
          fact.set(b.val, i);
          break;
        }
      case RU.FUNCALL:
        {
          ValueVector fc = ExpandAction(fact.get(i).FuncallValue());
          Value v = Funcall.Execute(fc, this);
          fact.set(v, i);
          break;
        }
      case RU.LIST:
        {
          ValueVector list = fact.get(i).ListValue();
          list = ExpandList(list);
          list = FlattenList(list);
          fact.set(new Value(list, RU.LIST), i);
          break;
        }
      }
    }
    return fact;
  }

  /**
    Do varsubs in lists
    */

  ValueVector ExpandList(ValueVector original) throws ReteException {

    ValueVector list = (ValueVector) original.clone();

    for (int i=0; i<list.size(); i++) {
      switch (list.get(i).type()) {
      case RU.VARIABLE:
      case RU.MULTIVARIABLE:
        {
          Binding b = findBinding(list.get(i).VariableValue());
          list.set(b.val, i);
          break;
        }
      case RU.FUNCALL:
        {
          ValueVector fc = ExpandAction(list.get(i).FuncallValue());
          Value v = Funcall.Execute(fc, this);
          list.set(v, i);
          break;
        }
      }
    }
    
    return list;
  }
  
  // Replace a nested set of lists with a flat list.

  static final ValueVector FlattenList(ValueVector vv) throws ReteException {
    ValueVector flat = new ValueVector();
    DoFlattenList(flat, vv);
    return flat;
  }

  // Recursively smoosh nested lists into one flat list. 

  private static void DoFlattenList(ValueVector acc, ValueVector vv)
    throws ReteException {
    for (int i=0; i< vv.size(); i++) {
      Value current = vv.get(i);
      if (current.type() != RU.LIST) {        
        acc.add(current);
      }
      else
        DoFlattenList(acc, current.ListValue());
    }
  }
}
