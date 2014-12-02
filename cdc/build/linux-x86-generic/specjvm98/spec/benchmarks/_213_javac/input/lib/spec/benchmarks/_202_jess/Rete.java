// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Rete.java
// Somewhat less object-oriented Rete machine
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// TEST ONLY - NOT FOR DISTRIBUTION
//////////////////////////////////////////////////////////////////////
package spec.benchmarks._202_jess;
// import java.io.*;
// import java.util.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream; //**NS**
import java.io.StringBufferInputStream; //**NS**

import java.lang.System;
import java.lang.Integer;
import java.lang.Character;

import java.io.StreamTokenizer; //**NS**
import java.util.Enumeration;
import java.util.Stack;
import java.util.Hashtable;
import java.util.Vector;
/**
 A machine to perform the Rete pattern matching algorithm.
 This could be much more object-oriented, but we're going for speed.
 Remember, Java is not C++; there are no inline functions.

@author E.J. Friedman-Hill (C)1996
*/

public class Rete {

  /**
    Context for executing global functions
    */

  public GlobalContext global_context;

  /**
    Where to draw myself
   */

  public ReteDisplay display;

  /**
    Where Standard Input comes from
    */

  public StreamTokenizer st;

  /**
    Whether to log stuff
   */


  public boolean watch_rules = false;
  public boolean watch_facts = false;
  public boolean watch_compilations = false;


  /**
    Successively incremented ID for asserted facts.
   */
  
  private int next_fact_id = 0;

  /**
    Deftemplates are unique to each interpreter.
   */

  protected Hashtable deftemplates = new Hashtable(100);

  /**
    Deffacts are unique to each interpreter.
   */

  protected Vector deffacts = new Vector();

  /**
    Defglobals are unique to each interpreter.
   */

  protected Vector defglobals = new Vector();

  /**
    Deffunctions are unique to each interpreter.
   */

  protected Hashtable deffunctions = new Hashtable(100);

  /**
    Userfunctions are unique to each interpreter.
   */

  protected Hashtable userfunctions = new Hashtable(100);

  /**

  /**
    The fact-list is unique to each interpreter.
   */

  protected Vector facts = new Vector();

  /**
    The rule base is unique to each interpreter.
   */

  protected Vector rules = new Vector();

  /**
    The agenda is unique to each interpreter.
   */

  protected Vector activations = new Vector();

  /**
    Each interpreter has its own compilerobject
   */

  protected ReteCompiler rc = new ReteCompiler(this);

  /**
    Constructor
    */

  public Rete(ReteDisplay display)  {
    this.display = display;
    global_context = new GlobalContext(this);
    st = new StreamTokenizer(display.stdin());
    try  {
      Deftemplate dt = new Deftemplate("declare", RU.UNORDERED_FACT);
      dt.AddSlot("salience", new Value(0, RU.INTEGER));
      AddDeftemplate(dt);
    } catch (ReteException re) {
      // this can't happen in a brand new engine!
    }
  }

  /**
    Advance the tokenizer
    */

  public int advance() throws ReteException {
    try {
      return st.nextToken();
    } catch (Throwable t) {
      throw new ReteException("Rete::advance",
                              "File error",
                              t.toString());
    }
  }

  /**
    Reset the interpreter. Remove all facts, flush the network,
    remove all activations.
    */

  public void Reset() throws ReteException {
    int size = facts.size();

    // remove all existing facts
    // this is not all that efficient, but it's less error-prone
    // than some of the alternatives.

    Object[] o = new Object[size];
    facts.copyInto(o);
    for (int i=0; i<size; i++) {
      Retract((ValueVector) o[i]);
    }
    

    // Reset all the defglobals

    size = defglobals.size();
    for (int i=0; i<size; i++) {
      Defglobal dg = (Defglobal) defglobals.elementAt(i);
      int dgsize = dg.bindings.size();
      for (int j=0; j<dgsize; j++) {
        Binding b = (Binding) dg.bindings.elementAt(j);
        global_context.AddGlobalBinding(b.name, b.val);
      }
    }

    next_fact_id = 0;

    // assert the initial-fact
    Fact init = new Fact("initial-fact", RU.ORDERED_FACT, this);
    Assert(init.FactData());

    // assert all the deffacts
    size = deffacts.size();
    for (int i=0; i<size; i++) {
      Deffacts df = (Deffacts) deffacts.elementAt(i);
      int dfsize = df.facts.size();
      for (int j=0; j<dfsize; j++) {
        ValueVector f = (ValueVector) df.facts.elementAt(j);
        Assert(f);
      }
    }
  }

  /**
    Assert a fact, as a ValueVector
    */

  public int Assert(ValueVector f) throws ReteException {

    // find any old copy
    ValueVector of;
    of = findFact(f);

    if (of != null)
      return -1;

    // insert the new fact
    f.set(new Value(next_fact_id++, RU.FACT_ID), RU.ID);

    if (f.get(RU.DESC).DescriptorValue() == RU.UNORDERED_FACT) {
      // deal with any default values
      ValueVector deft = null;

      for (int i=RU.FIRST_SLOT; i< f.size(); i++) {
        if (f.get(i).type() == RU.NONE) {
          if (deft == null)
            deft = FindDeftemplate(f.get(RU.CLASS).AtomValue());
          int j = (i - RU.FIRST_SLOT)* RU.DT_SLOT_SIZE + RU.FIRST_SLOT + RU.DT_DFLT_DATA;
          Value dtd = deft.get(j);
          if (dtd.type() == RU.NONE)
            f.set(new Value(RU.putAtom("NIL"), RU.ATOM), i);
          else 
            f.set(dtd, i);
        }
      }          
    }
    
    display.AssertFact(f);

    if (watch_facts)
      display.stdout().println(" ==> " 
                               + " f-" +f.get(RU.ID).FactIDValue() + " " +
                               new Fact(f,this).toString());

    facts.addElement(f);
    Token t = new Token(RU.ADD, f);
    ProcessToken(t);
    return f.get(RU.ID).FactIDValue();
  }

  /**
    Retract a fact, as a ValueVector
    */

  public void Retract(ValueVector f) throws ReteException {
    ValueVector found;
    if ((found = findFact(f)) != null) {
      if (watch_facts)
        display.stdout().println(" <== " 
                                 + " f-" + f.get(RU.ID).FactIDValue() + " " +
                                 new Fact(f,this).toString());
      display.RetractFact(found);
      
      facts.removeElement(found);
      Token t = new Token(RU.REMOVE, f);
      ProcessToken(t);
    }
  }
  
  
  /**
    Retract a fact by ID, used by rule RHSs.
    */

  public void Retract(int id) throws ReteException {
    ValueVector found = findFactByID(id);
    if (found != null) {
      if (watch_facts)
        display.stdout().println(" <== " 
                                 + " f-" + found.get(RU.ID).FactIDValue() + " " +
                                 new Fact(found,this).toString());
      
      display.RetractFact(found);

      facts.removeElement(found);
      Token t = new Token(RU.REMOVE, found);
      ProcessToken(t);
    }
  }


  /**
    This 'find' is used by the retract that rules use.
    */

  public ValueVector findFactByID(int id) throws ReteException {
    int size = facts.size();
    for (int i=0; i<size; i++) {
      ValueVector tf = (ValueVector) facts.elementAt(i);
      if (tf.get(RU.ID).FactIDValue() == id)
        return tf;
    }
    return null;
  }

  /**
    Does a given fact (as a ValueVector) exist? (We're looking for identical
    data, but the ID can differ)
    */

  private ValueVector findFact(ValueVector f) throws ReteException {
    int id = f.get(RU.ID).FactIDValue();
    int size = facts.size();
    int fsize = f.size();
  outer_loop:
    for (int i=0; i < size; i++) {
      ValueVector tf = (ValueVector) facts.elementAt(i);
      if (f.size() != tf.size())
        continue;
      if (!f.get(RU.CLASS).equals(tf.get(RU.CLASS)))
        continue;
      for (int j=RU.FIRST_SLOT; j < fsize; j++) {
        if (!f.get(j).equals(tf.get(j)))
          continue outer_loop;
      }
      return tf;
    }
    return null;
  }

  /**
    What the (rules) command calls
    */

  public void ShowRules() {
    int size = rules.size();
    for (int i=0; i<size; i++) {
      Defrule dr = (Defrule) rules.elementAt(i);
      display.stdout().println(dr.toString());
    }
    display.stdout().println("For a total of " + size + " rules.");
  }
  
  /** Return the pretty print forms of all facts, as a big string */


  public String PPFacts(int name) {
    int size = facts.size();
    String s = "";
    Fact fact = null;
    for (int i=0; i<size; i++) {
      ValueVector f = (ValueVector) facts.elementAt(i);
      try {
        if (f.get(RU.CLASS).AtomValue() != name) continue;
        fact = new Fact(f, this);
      } catch (ReteException re) {
        continue;
      }
      s += fact.ppfact() + "\n";
    }
    return s;
  }

  public String PPFacts() {
    int size = facts.size();
    String s = "";
    Fact fact = null;
    for (int i=0; i<size; i++) {
      ValueVector f = (ValueVector) facts.elementAt(i);
      try {
        fact = new Fact(f, this);
      } catch (ReteException re) {
        continue;
      }
      s += fact.ppfact() + "\n";
    }
    return s;
  }


  /**
    What the (facts) command calls
    */

  public void ShowFacts() throws ReteException {
    int size = facts.size();
    for (int i=0; i<size; i++) {
      ValueVector f = (ValueVector) facts.elementAt(i);
      Fact fact = new Fact(f, this);
      display.stdout().println(fact.toString());
    }
    display.stdout().println("For a total of " + size + " facts.");
  }
  
  /**
    Process a Token which represents a fact being added or removed.
    Eventually we should set this up so that the token gets dispatched
    to only one root of the pattern net - right now it presented to all of
    them!
    
    */

  private int ProcessToken(Token t) throws ReteException {
    Vector v = rc.roots;
    // make sure the network is optimized
    rc.freeze();
    // put a sort code into this token
    // by convention, the sort code is either the data or the head (if no data)
    // of the last fact in a token. The private method ProcessToken is always called
    // with tokens containing ONE fact.

    ValueVector f = (ValueVector) t.facts[0];
    if (f.size() == RU.FIRST_SLOT)
      t.sortcode = f.get(RU.CLASS).AtomValue();
    else
      t.sortcode = f.get(RU.FIRST_SLOT).SortCode();
    
    int size = v.size();
    for (int i=0; i < size; i++)
      if (((Successor) v.elementAt(i)).node.CallNode(t, Node.SINGLE))
        return 1;
    return 0;
  }


  /**
    Find a deftemplate object with a certain name
    */

  public ValueVector FindDeftemplate(String name) {
    return (ValueVector) deftemplates.get(name);
  }

  public ValueVector FindDeftemplate(int code) {
    String name = RU.getAtom(code);
    return FindDeftemplate(name);
  }

  /**
    Creates a new deftemplate in this object. 
    Ensure that every deftemplate has a unique class name
    */

  public ValueVector AddDeftemplate(Deftemplate dt) throws ReteException {
    ValueVector dtia = dt.DeftemplateData();
    String name = dtia.get(RU.CLASS).StringValue();
    if (deftemplates.get(name) == null) {
      deftemplates.put(name, dtia);
      display.AddDeftemplate(dt);
    }
    return dtia;
  }


  /**
    Creates a new deffacts in this object
    Like adddeftemplate, won't let an old one be destroyed.
    */

  public Deffacts AddDeffacts(Deffacts df) throws ReteException {
    for (int i=0; i<deffacts.size(); i++) {
      if (df.name == ((Deffacts) deffacts.elementAt(i)).name)
        throw new ReteException("AddDeffacts",
                                "Cannot redefine deffacts:",
                                RU.getAtom(df.name));
    }

    display.AddDeffacts(df);

    deffacts.addElement(df);
    return df;

  }

  /**
    Creates a new Defglobal in this object
    */

  public Defglobal AddDefglobal(Defglobal dg) throws ReteException {
    defglobals.addElement(dg);
    return dg;

  }

  /**
    Creates a new deffunction in this object
    Will happily destroy an old one.
    */

  public Deffunction AddDeffunction(Deffunction df) throws ReteException {
    String name = RU.getAtom(df.name);
    deffunctions.put(name, df);
    return df;
  }

  /**
    Find a deffunction, if there is one.
   */

  public Deffunction FindDeffunction(String name) {
    return (Deffunction) deffunctions.get(name);
  }

  public Deffunction FindDeffunction(int code) {
    String name = RU.getAtom(code);
    return FindDeffunction(name);
  }

  /**
    Creates a new userfunction in this object
    Will happily destroy an old one.
    */

  public Userfunction AddUserfunction(Userfunction uf) {
    String name = RU.getAtom(uf.name());
    userfunctions.put(name, uf);
    return uf;
  }

  /**
    Find a userfunction, if there is one.
   */

  public Userfunction FindUserfunction(String name) {
    return (Userfunction) userfunctions.get(name);
  }

  public Userfunction FindUserfunction(int code) {
    String name = RU.getAtom(code);
    return FindUserfunction(name);
  }


  /**
    Creates a new defrule in this object
    Like adddeftemplate, won't let an old one be destroyed.
    */

  public Defrule AddDefrule(Defrule dr) throws ReteException {
    for (int i=0; i<rules.size(); i++) {
      if (dr.name == ((Defrule) rules.elementAt(i)).name)
        throw new ReteException("AddDefrule",
                                "Cannot redefine defrule:",
                                RU.getAtom(dr.name));
    }

    display.AddDefrule(dr);

    rules.addElement(dr);

    rc.AddRule(dr);
    return dr;

  }

  /**
    Info about a rule to fire.
   */

  public void AddActivation(Activation a) {
    display.ActivateRule(a.rule);
    activations.addElement(a);
  }

  /**
    An activation has been cancelled; forget it
    */

  public void standDown(Activation a) {
    display.DeactivateRule(a.rule);
    activations.removeElement(a);
  }


  /**
    Run the actual engine.
    */

  public int Run() throws ReteException {
    int n = 0;

    while (activations.size() > 0) {
      int bs = 0, rs, ms = -10001;
      for (int i=0; i< activations.size(); i++) {
        rs = ((Activation) activations.elementAt(i)).rule.salience;
        if (rs >= ms) {
          ms = rs;
          bs = i;
        }
      }
      Activation a = (Activation) activations.elementAt(bs);
      a.Fire();
      ++n;
    }
    return n;
  }

  /**
    Stuff to let Java code call functions inside of us.
   */

  private Jesp StringReader;

  public Value ExecuteCommand(String cmd) throws ReteException {
    StringBufferInputStream sbis;
    try {
      sbis = new StringBufferInputStream(cmd);
    } catch (Throwable t) {
      throw new ReteException("Rete::ExecuteCommand", t.toString(), cmd);
    }
      
    if (StringReader == null)
      StringReader = new Jesp(sbis, this);
    else
      StringReader.InitTokenizer(sbis);

    return StringReader.parse();
  }


}

