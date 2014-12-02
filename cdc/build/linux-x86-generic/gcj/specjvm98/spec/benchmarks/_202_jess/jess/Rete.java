// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Rete.java
// Rete machine: execute the built Network
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.io.*;
import java.util.*;

/**
 A machine to perform the Rete pattern matching algorithm.

@author E.J. Friedman-Hill (C)1996
*/

public class Rete {

  /**
    Context for executing global functions
    */

  private GlobalContext global_context;
  public GlobalContext global_context() { return global_context; }

  /**
    Where to draw myself
   */

  private ReteDisplay display;
  public ReteDisplay display() { return display; }

  /**
    Where Standard Input comes from
    */

  private StreamTokenizer st;

  /**
    Whether to log stuff
   */


  private boolean watch_rules = false;
  public void watch_rules(boolean val) { watch_rules = val; }
  public boolean watch_rules() { return watch_rules; }
  private boolean watch_facts = false;
  public void watch_facts(boolean val) { watch_facts = val; }
  public boolean watch_facts() { return watch_facts; }
  private boolean watch_compilations = false;
  public void watch_compilations(boolean val) { watch_compilations = val; }
  public boolean watch_compilations() { return watch_compilations; }
  private boolean watch_activations = false;
  public void watch_activations(boolean val) { watch_activations = val; }
  public boolean watch_activations() { return watch_activations; }


  /**
    Successively incremented ID for asserted facts.
   */
  
  private int next_fact_id = 0;
  public int next_fact_id() { return next_fact_id++;}

  /**
    Successively incremented ID for new rules.
   */

  private int next_rule_id = 0;
  public int next_rule_id() { return next_rule_id++;}

  /**
    Deftemplates are unique to each interpreter.
   */

  private Hashtable deftemplates = new Hashtable(100);

  /**
    Deffacts are unique to each interpreter.
   */

  private Vector deffacts = new Vector();

  /**
    Defglobals are unique to each interpreter.
   */

  private Vector defglobals = new Vector();

  /**
    Deffunctions are unique to each interpreter.
   */

  private Hashtable deffunctions = new Hashtable(100);

  /**
    Userfunctions are unique to each interpreter.
   */

  private Hashtable userfunctions = new Hashtable(100);

  /**

  /**
    The fact-list is unique to each interpreter.
   */

  private Vector facts = new Vector();

  /**
    The rule base is unique to each interpreter.
   */

  private Vector rules = new Vector();

  /**
    The agenda is unique to each interpreter.
   */

  private Vector activations = new Vector();

  /**
    Each interpreter has its own compiler object
   */

  private ReteCompiler rc = new ReteCompiler(this);

  public final ReteCompiler compiler() { return rc; }
  /**
    Flag for (halt) function
    */

  private boolean halt;


  /**
    Constructor
    */

  public Rete(ReteDisplay display)  {
    this.display = display;
    global_context = new GlobalContext(this);

    Reader reader = new BufferedReader(new InputStreamReader(display.stdin()));
    st = new StreamTokenizer(reader);
  }

  /*
    Reinitialize engine
    Thanks to Karl Mueller for idea
    */

  void clear() throws ReteException
  {
    watch_rules(false);
    watch_facts(false);
    watch_compilations(false);
    watch_activations(false);
    halt = false;
    next_fact_id = next_rule_id = 0;
    deftemplates.clear();
    deffacts.removeAllElements();
    defglobals.removeAllElements();
    deffunctions.clear();

    // Don't want to do this, I don't think
    // userfunctions.clear();

    facts.removeAllElements();
    rc = new ReteCompiler(this);
    if (rules.size() != 0)
        display.AddDefrule((Defrule) rules.elementAt(0));
    rules.removeAllElements();
    activations.removeAllElements();
    System.gc();
  }


  /**
    Advance the tokenizer
    */

  StreamTokenizer advance() throws ReteException {
    try {
      st.nextToken();
      return st;
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
    Assert a fact, as a String
    */

  public int AssertString(String s) throws ReteException {
    StringBufferInputStream sbis;
    try
      {
        sbis = new StringBufferInputStream(s);
        Fact f = new Jesp(sbis, this).ParseFact();
        return Assert(f.FactData());
      }
    catch (Throwable t)
      {
        throw new ReteException("Rete::AssertString", t.toString(), s);
      }
  }

  /**
    Assert a fact, as a ValueVector
    */

  int Assert(ValueVector f) throws ReteException {

    // First, expand any multifields if ordered fact
    if ( f.get(RU.DESC).DescriptorValue() == RU.ORDERED_FACT)
      for (int i=0; i<f.size(); i++)
        if (f.get(i).type() == RU.LIST) {
          ValueVector nf = new ValueVector(f.size());
          for (int j=0; j<f.size(); j++) {
            Value v = f.get(j);
            if (v.type() != RU.LIST)
              nf.add(v);
            else {
              ValueVector vv = v.ListValue();
              for (int k=0; k < vv.size(); k++) {
                nf.add(vv.get(k));
              }
            }
          }
          f = nf;
          break;
        }
    
    // find any old copy
    ValueVector of = findFact(f);

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
            f.set(Funcall.NIL(), i);
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

  void Retract(ValueVector f) throws ReteException {
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

  void Retract(int id) throws ReteException {
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

  ValueVector findFactByID(int id) throws ReteException {
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
      if (fsize != tf.size())
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
    The token gets dispatched to only one root of the pattern net.    
    */

  private int ProcessTokenOneNode(Token t, Node n) throws ReteException {

    // put a sort code into this token
    // by convention, the sort code is either the data or the head (if no data)
    // of the last fact in a token. The method ProcessToken should always be called
    // with tokens containing ONE fact.

    ValueVector f = (ValueVector) t.facts[0];
    if (f.size() == RU.FIRST_SLOT)
      t.sortcode = f.get(RU.CLASS).AtomValue();
    else
      t.sortcode = f.get(RU.FIRST_SLOT).SortCode();
    
    if (n.CallNode(t, Node.SINGLE))
      return 1;
    return 0;
  }

  /**
    Process a Token which represents a fact being added or removed.
    Eventually we should set this up so that the token gets dispatched
    to only one root of the pattern net - right now it presented to all of
    them!
    
    */

  private int ProcessToken(Token t) throws ReteException {
    Vector v = rc.roots();

    // make sure the network is optimized
    rc.freeze();

    int size = v.size();
    for (int i=0; i < size; i++)
      {
        Node1 n = (Node1) ((Successor) v.elementAt(i)).node;
        if (ProcessTokenOneNode(t, n) == 1)
          return 1;
      }
    return 0;
  }

  /**
    Present all the facts on the agenda to a single Node.
    */

  void UpdateNode(Node n) throws ReteException {
    for (int i=0; i < facts.size(); i++) {
      Token t = new Token(RU.UPDATE, (ValueVector) facts.elementAt(i));
      ProcessToken(t);
    }
  }

  /**
    Find a deftemplate object with a certain name
    */

  ValueVector FindDeftemplate(String name) {
    return (ValueVector) deftemplates.get(name);
  }

  ValueVector FindDeftemplate(int code) {
    String name = RU.getAtom(code);
    return FindDeftemplate(name);
  }

  /**
    Creates a new deftemplate in this object. 
    Ensure that every deftemplate has a unique class name
    */

  ValueVector AddDeftemplate(Deftemplate dt) throws ReteException {
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

  Deffacts AddDeffacts(Deffacts df) throws ReteException {
    for (int i=0; i<deffacts.size(); i++) {
      if (df.name == ((Deffacts) deffacts.elementAt(i)).name)
        throw new ReteException("AddDeffacts",
                                "Cannot redefine deffacts:",
                                RU.getAtom(df.name));
    }

    deffacts.addElement(df);
    display.AddDeffacts(df);
    return df;

  }

  /**
    Creates a new Defglobal in this object
    */

  Defglobal AddDefglobal(Defglobal dg) throws ReteException {
    defglobals.addElement(dg);
    return dg;

  }

  /**
    Creates a new deffunction in this object
    Will happily destroy an old one.
    */

  Deffunction AddDeffunction(Deffunction df) throws ReteException {
    String name = RU.getAtom(df.name());
    deffunctions.put(name, df);
    return df;
  }

  /**
    Find a deffunction, if there is one.
   */

  Deffunction FindDeffunction(String name) {
    return (Deffunction) deffunctions.get(name);
  }

  Deffunction FindDeffunction(int code) {
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
    A package simply calls AddUserfunction lots of times.
    */

  public Userpackage AddUserpackage(Userpackage up) {
    up.Add(this);
    return up;
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

  Defrule AddDefrule(Defrule dr) throws ReteException {
    for (int i=0; i<rules.size(); i++) {
      Defrule odr = (Defrule) rules.elementAt(i);
      if (dr.name == odr.name)
        {
          odr.deactivate();
          rules.removeElement(odr);
          break;
        }
    }

    rules.addElement(dr);

    rc.AddRule(dr);
    display.AddDefrule(dr);
    return dr;

  }

  Value Undefrule(int name) throws ReteException {
    for (int i=0; i<rules.size(); i++) {
      Defrule odr = (Defrule) rules.elementAt(i);
      if (name == odr.name)
        {
          odr.deactivate();
          rules.removeElement(odr);
          display.AddDefrule(odr);
          return Funcall.TRUE();
        }
    }
    return Funcall.FALSE();
  }

  /**
    Info about a rule to fire.
   */

  void AddActivation(Activation a) throws ReteException {
    display.ActivateRule(a.rule);
    activations.addElement(a);
    if (watch_activations)
      display.stdout().println("==> Activation: " 
                               + RU.getAtom(a.rule.name) + " : "
                               + factList(a.token));
  }

  /**
    An activation has been cancelled; forget it
    */

  void standDown(Activation a) throws ReteException {
    ruleFired(a);
    if (watch_activations)
      display.stdout().println("<== Activation: " 
                               + RU.getAtom(a.rule.name) + " : "
                               + factList(a.token));
  }

  /**
    An activation has been fired; forget it
    */

  void ruleFired(Activation a) {
    display.DeactivateRule(a.rule);
    activations.removeElement(a);
  }

  /**
    Return a string describing a list of facts
    */
  
  String factList(Token t) throws ReteException {
    String s = "";
    boolean first = true;
    for (int i=0; i<t.size; i++) {
      if (!first)
        s += ", ";
      int id = t.facts[i].get(RU.ID).FactIDValue();
      if (id != -1)
        s += "f-" + id;
      first = false;
    }
    return s;
  }

  /**
    Run the actual engine.
    */

  public int Run() throws ReteException {
    int n = 0;

    halt = false;
    while (activations.size() > 0 && !halt) {
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


  public Value ExecuteCommand(String cmd) throws ReteException {
    StringBufferInputStream sbis;
    try {
      sbis = new StringBufferInputStream(cmd);
    } catch (Throwable t) {
      throw new ReteException("Rete::ExecuteCommand", t.toString(), cmd);
    }
      
    return new Jesp(sbis, this).parse(false);
  }

  /**
    Jane, stop this crazy thing!
    */

  void Halt() {
    halt = true;
  }


}
