// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Funcall.java
// A class for parsing, assembling, and interpreting function calls
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

import java.io.StreamTokenizer; //**NS**
import java.util.Enumeration;
import java.util.Stack;
import java.util.Hashtable;
import java.util.Vector;

/**
 A class for parsing, assembling, and interpreting function calls
@author E.J. Friedman-Hill (C)1996

*/

public class Funcall {

  /**
    vvec is the vector representing this Funcall
   */

  public ValueVector vvec;

  private static Value _true = null;
  private static Value _false = null;
  private static Value _nil = null;

  static {
    try {
      _true = new Value(RU.putAtom("TRUE"),RU.ATOM);
      _false = new Value(RU.putAtom("FALSE"),RU.ATOM);
      _nil = new Value(RU.putAtom("nil"),RU.ATOM);
    } catch (ReteException re) {
      // Things are really messed up, but what can I do?
    }
    
  }
  
  /**
    Engine is the Rete engine we're parsed into
   */

  Rete engine;

  public Funcall(String name, Rete engine) throws ReteException {
    this.engine = engine;

    vvec = new ValueVector();
    vvec.add(new Value(RU.putAtom(name), RU.ATOM));
  }
  

  public final static Value TRUE() {
    return _true;
  }

  public final static Value FALSE() {
    return _false;
  }

  public final static Value NIL() {
    return _nil;
  }


  /**
    Add an argument to this funcall
    */

  public void AddArgument(String value, int type) throws ReteException {
    AddArgument(RU.putAtom(value), type);
  }

  public void AddArgument(int value, int type) throws ReteException {
    vvec.add(new Value(value, type));
  }

  public void AddArgument(double value, int type) throws ReteException {
    vvec.add(new Value(value, type));
  }

  public void AddArgument(Funcall value) throws ReteException {
    vvec.add(new Value(value.vvec, RU.FUNCALL));
  }

  public void AddArgument(Fact value) throws ReteException {
    ValueVector vv = value.FactData();
    vvec.add(new Value(vv, vv.get(RU.DESC).DescriptorValue()));
  }

  public void AddArgument(ValueVector value, int type) throws ReteException {
    vvec.add(new Value(value, type));
  }

  /**
    Execute a Funcall, recursively expanding its fields in the context of a
    Defrule. 

    @returns A Value structure containing the return value, or null
    if expand_root false.
   */

  public static Value Execute(ValueVector vv, Context context) throws ReteException {
    int size = vv.size();

    String functor = vv.get(0).StringValue();    

    if (!functor.equals("if") && !functor.equals("while")
        && !functor.equals("and") && !functor.equals("or")) {
      // for each argument
      for (int i=1; i<size; i++) {
        int type = vv.get(i).type();
        if (type == RU.FUNCALL) {
          
          // dig out the subexpression
          ValueVector subexp = vv.get(i).FuncallValue();
          
          // expand subexpression, keeping return value
          Value r = Execute(subexp, context);
          vv.set(r,i);
          
        } else if (type == RU.UNORDERED_FACT ||
                   type == RU.ORDERED_FACT) {
          
          ValueVector fact = vv.get(i).FactValue();
          for (int j=RU.FIRST_SLOT; j<fact.size(); j++) {
            if (fact.get(j).type() == RU.FUNCALL) {
              ValueVector subexp = fact.get(j).FuncallValue();
              Value r = Execute(subexp, context);
              fact.set(r,j);
            }
          }
        } else if (type == RU.LIST) {
          
          ValueVector list = vv.get(i).ListValue();
          for (int j=1; j<list.size(); j++) {
            if (list.get(j).type() == RU.FUNCALL) {
              ValueVector subexp = list.get(j).FuncallValue();
              Value r = Execute(subexp, context);
              list.set(r,j);
            }
          }
        }
      }
    }
    // we need to expand ourselves too
    return SimpleExecute(vv, context);
  }

  /**
    Execute this Funcall
   */
  
  public Value Execute(Context context) throws ReteException {
    return Execute(this.vvec, context);
  }

  /**
    Execute a funcall in a particular context
    We let the Value class do all our argument type checking, leading
    to remarkably small code!
   */
  
  private static Value SimpleExecute(ValueVector vv, Context context) throws ReteException {
    
    Rete engine = context.engine;
    String functor = vv.get(0).StringValue();

    Deffunction df = engine.FindDeffunction(functor);
    if (df != null) 
      return df.Call(vv);

    Userfunction uf = engine.FindUserfunction(functor);
    if (uf != null) 
      return uf.Call(vv, context);

      // *** return  *******************************************
    if (functor.equals("return")) {
      return context.SetReturnValue(vv.get(1));

      // *** assert  *******************************************
      
    } else if (functor.equals("assert")) {
      int result = -1;
      for (int i=1; i< vv.size(); i++) {
        ValueVector fact = vv.get(i).FactValue();
        result = engine.Assert(fact);
      }
      if (result != -1)
        return new Value(result, RU.FACT_ID);
      else
        return _false;
      
      // *** retract *******************************************
      
    } else if (functor.equals("retract")) {
      for (int i=1; i< vv.size(); i++) {
        engine.Retract(vv.get(i).FactIDValue());
      }
      return _true;
      
      // *** printout *******************************************
      
    } else if (functor.equals("printout")) {
      if (!vv.get(1).StringValue().equals("t"))
        throw new ReteException("Funcall::SimpleExecute",
                                "printout: bad router",
                                vv.get(1).StringValue());

      java.io.PrintStream ps = engine.display.stdout();

      for (int i = 2; i < vv.size(); i++) {
        switch(vv.get(i).type()) {
        case RU.ATOM:
          if (vv.get(i).StringValue().equals("crlf"))
            ps.println();
          else
            ps.print(vv.get(i).StringValue());
          break;
        case RU.INTEGER:
          ps.print(vv.get(i).IntValue()); break;
        case RU.FLOAT:
          ps.print(vv.get(i).FloatValue()); break;
        case RU.FACT_ID:
          ps.print("<Fact-" + vv.get(i).FactIDValue() + ">");
          break;
        case RU.STRING:
          ps.print(vv.get(i).StringValue());
          break;
        default:
        throw new ReteException("Funcall::SimpleExecute",
                                "printout: bad data type",
                                "type =" +vv.get(i).type());
          
        }
      }
      ps.flush();
      return _nil;

    // *** read *******************************************
    } else if (functor.equals("read")) {
      if (vv.size() > 1)
        if (!vv.get(1).StringValue().equals("t"))
          throw new ReteException("Funcall::SimpleExecute",
                                  "read: bad router",
                                  vv.get(1).StringValue());

      engine.advance();
      switch (engine.st.ttype) {
      case engine.st.TT_WORD:
        return new Value(RU.putAtom(engine.st.sval), RU.ATOM);
      case engine.st.TT_NUMBER:
        return new Value(engine.st.nval, RU.FLOAT);
      case '"':
        return new Value(RU.putAtom(engine.st.sval), RU.STRING);
      default:
        return new Value(RU.putAtom("" + engine.st.ttype), RU.ATOM);
      }

    // *** readline  *******************************************
    } else if (functor.equals("readline")) {
      if (vv.size() > 1)
        if (!vv.get(1).StringValue().equals("t"))
          throw new ReteException("Funcall::SimpleExecute",
                                  "readline: bad router",
                                  vv.get(1).StringValue());

// spec.harness.Context.out.println( "****WARNING "+ new Exception() );	 
//      try {     
//        DataInputStream dis = new DataInputStream(engine.display.stdin());
//        return new Value(dis.readLine(), RU.STRING);
//      } catch (Throwable t) {
         return new Value("", RU.STRING);
//      }
        // *** gensym*  *******************************************
    } else if (functor.equals("gensym*")) {
      return new Value(RU.gensym("gen"), RU.STRING);

    // *** while *******************************************
    } else if (functor.equals("while")) {
      // This accepts a superset of the correct syntax...
      Value result = _nil;
    outer_loop:
      while (Execute(context.ExpandAction(vv.get(1).FuncallValue()),
                     context).equals(_true)) {
        for (int i=2; i< vv.size(); i++) {
          Value current = vv.get(i);
          if (current.type() == RU.FUNCALL) {
            ValueVector copy = context.ExpandAction(current.FuncallValue());
            result = Execute(copy, context);
            if (context.Returning()) {
              result = context.GetReturnValue();
              break outer_loop;
            }
          } else if (current.type() == RU.ATOM && current.StringValue().equals("do"))
            // just go on to the next action
            continue;
        }
      }
      return result; 
      
    // *** if *******************************************
    } else if (functor.equals("if")) {
      // This accepts a superset of the correct syntax...
      // check condition
      Value result = Execute(vv.get(1).FuncallValue(), context);
      if (result.equals(_true)) {
        // do 'then' part
        result = _false;
        for (int i=3; i< vv.size(); i++) {
          if (vv.get(i).type() == RU.FUNCALL) {
            ValueVector copy = context.ExpandAction(vv.get(i).FuncallValue());
            result = Execute(copy, context);
            if (context.Returning()) {
              result = context.GetReturnValue();
              break;
            }
          } else
            break;
            
        }
        return result; 
      } else {
        // first find the 'else'
        result = _false;
        boolean seen_else = false;
        for (int i=3; i< vv.size(); i++) {
          if (vv.get(i).type() == RU.FUNCALL && seen_else) {
            ValueVector copy = context.ExpandAction(vv.get(i).FuncallValue());
            result = Execute(copy, context);
            if (context.Returning()) {
              result = context.GetReturnValue();
              break;
            }
          } else if (vv.get(i).type() == RU.ATOM &&
                     vv.get(i).StringValue().equals("else"))
            seen_else = true;
        }
        return result;
      }
      
      // *** bind *******************************************
    
    } else if (functor.equals("bind")) {
      context.SetVariable(vv.get(1).VariableValue(),
                       vv.get(2));
      return vv.get(2);
      // *** modify  *******************************************
      
    } else if (functor.equals("modify")) {
      ValueVector fact;
      if ((fact = engine.findFactByID(vv.get(1).FactIDValue())) == null)
        throw new ReteException("Funcall::SimpleExecute",
                                "modify: syntax error",
                                "");
      
      // we have the ValueVector fact, make a Fact out of it

      Fact f = new Fact(fact, engine);


      // now change the values. For each argument...
      for (int i= 2; i < vv.size(); i++) {

        // fetch the slot, value subexp, stored as a List
        ValueVector svp = vv.get(i).ListValue();

        // add the new value
        f.AddValue(svp.get(0).StringValue(), svp.get(1));

      }

      // get rid of the old one.
      engine.Retract(fact.get(RU.ID).FactIDValue());

      // get the new fact data
      fact = f.FactData();

      // and assert the new fact
      return new Value(engine.Assert(fact), RU.FACT_ID);
      
      // *** and *******************************************
    } else if (functor.equals("and")) {
      for (int i=1; i<vv.size(); i ++) {
        if (Execute(context.ExpandAction(vv.get(i).FuncallValue()),
                    context).equals(_false))
          return _false;
      }
      return _true;
      
      // *** or *******************************************
    } else if (functor.equals("or")) {
      for (int i=1; i<vv.size(); i ++) {
        if (Execute(context.ExpandAction(vv.get(i).FuncallValue()),
                    context).equals(_true))
          return _true;
      }
      return _false;

      // *** or *******************************************
    } else if (functor.equals("not")) {
      if (vv.get(1).equals(_false))
        return _true;
      else
        return _false;

      // *** eq *******************************************
    } else if (functor.equals("eq")) {
      for (int i=2; i<vv.size(); i ++) {
        if (!vv.get(i).equals(vv.get(1)))
          return _false;
      }
      return _true;

      // *** = *******************************************
    } else if (functor.equals("=")) {
      for (int i=2; i<vv.size(); i ++) {
        if (!(vv.get(i).NumericValue() == vv.get(1).NumericValue()))
          return _false;
      }
      return _true;

      // *** <> *******************************************
    } else if (functor.equals("<>")) {
      for (int i=2; i<vv.size(); i ++) {
        if (vv.get(i).NumericValue() == vv.get(1).NumericValue())
          return _false;
      }
      return _true;

      // *** > *******************************************
    } else if (functor.equals(">")) {
      for (int i=1; i<vv.size()-1; i ++) {
        double value1 = vv.get(i).NumericValue();
        double value2 = vv.get(i+1).NumericValue();
              
        if (!(value1 > value2))
          return _false;
      }
      return _true;

      // *** < *******************************************
    } else if (functor.equals("<")) {
      for (int i=1; i<vv.size()-1; i ++) {
        double value1 = vv.get(i).NumericValue();
        double value2 = vv.get(i+1).NumericValue();
              
        if (!(value1 < value2))
          return _false;
      }
      return _true;

      // *** >= *******************************************
    } else if (functor.equals(">=")) {
      for (int i=1; i<vv.size()-1; i ++) {
        double value1 = vv.get(i).NumericValue();
        double value2 = vv.get(i+1).NumericValue();
              
        if (!(value1 >= value2))
          return _false;
      }
      return _true;

      // *** <= *******************************************
    } else if (functor.equals("<=")) {
      for (int i=1; i<vv.size()-1; i ++) {
        double value1 = vv.get(i).NumericValue();
        double value2 = vv.get(i+1).NumericValue();
              
        if (!(value1 <= value2))
          return _false;
      }
      return _true;

    // *** neq *******************************************
    } else if (functor.equals("neq")) {
      for (int i=2; i<vv.size(); i ++) {
        if (!vv.get(i).equals(vv.get(1)))
          return _true;
      }
      return _false;

    // *** mod *******************************************
    } else if (functor.equals("mod")) {
      int d1 = (int) vv.get(1).NumericValue();
      int d2 = (int) vv.get(2).NumericValue();
      
      // create a new Value to hold the result.
      return new Value(d1 % d2, RU.INTEGER);
      
    // *** + *******************************************
    } else if (functor.equals("+")) {
      double sum = 0;
      for (int i=1; i<vv.size(); i++) {
        sum += vv.get(i).NumericValue();
      }
      
      return new Value(sum, RU.FLOAT);
      
    // *** * *******************************************
    } else if (functor.equals("*")) {
      double product = 1;
      for (int i=1; i<vv.size(); i++) {
        product *= vv.get(i).NumericValue();
      }
                                  
      return new Value(product, RU.FLOAT);

    // *** - *******************************************
    } else if (functor.equals("-")) {
      double diff = vv.get(1).NumericValue();

      for (int i=2; i<vv.size(); i++) {
        diff -= vv.get(i).NumericValue();
      }
                                  
      return new Value(diff, RU.FLOAT);

      // *** / *******************************************
    } else if (functor.equals("/")) {
      double quotient = vv.get(1).NumericValue();
      
      for (int i=2; i<vv.size(); i++) {
        quotient /= vv.get(i).FloatValue();
      }
      return new Value(quotient, RU.FLOAT);

    // *** sym-cat *******************************************

    } else if (functor.equals("sym-cat")) {
      String s = vv.get(1).StringValue();
      for (int i=2; i < vv.size(); i++)
        s += vv.get(i).StringValue();
        
      return new Value(s, RU.ATOM);

    // *** reset *******************************************

    } else if (functor.equals("reset")) {
      engine.Reset();
      return _true  ;
      // *** run *******************************************
      
    } else if (functor.equals("run")) {
      engine.Run();
      return _true;
      
      // *** facts *******************************************
      
    } else if (functor.equals("facts")) {
      engine.ShowFacts();
      return _true;

      // *** rules *******************************************
      
    } else if (functor.equals("rules")) {
      engine.ShowRules();
      return _true;

    // *** watch *******************************************

    } else if (functor.equals("watch")) {
      String what = vv.get(1).StringValue();
      if (what.equals("rules"))
        engine.watch_rules = true;
      else if (what.equals("facts"))
        engine.watch_facts = true;
      else if (what.equals("compilations"))
        engine.watch_compilations = true;
      else if (what.equals("all"))
        engine.watch_facts = engine.watch_rules =
          engine.watch_compilations = true;
      else
        throw new ReteException("Funcall::Execute", "watch: can't watch" ,
                                what);

      return _true;
      // *** unwatch *******************************************

    } else if (functor.equals("unwatch")) {
      String what = vv.get(1).StringValue();
      if (what.equals("rules"))
        engine.watch_rules = false;
      else if (what.equals("facts"))
        engine.watch_facts = false;
      else if (what.equals("compilations"))
        engine.watch_compilations = false;
      else if (what.equals("all"))
        engine.watch_facts = engine.watch_rules =
          engine.watch_compilations = false;
      else
        throw new ReteException("Funcall::Execute", "unwatch: can't unwatch" ,
                                what);
      return _true;
      
      // *** jess versions  *******************************************

    } else if (functor.equals("jess-version-string")) {
      return new Value("Jess Version 2.21 5/20/96", RU.STRING);
    } else if (functor.equals("jess-version-number")) {
      return new Value(2.21, RU.FLOAT);

      // *** load-facts ***********************************************

    } else if (functor.equals("load-facts")) {
      String s = "ERROR";

// spec.harness.Context.out.println( "****WARNING "+ new Exception() );	

      return _true;

      // *** save-facts ***********************************************

    } else if (functor.equals("save-facts")) {
      String s = "";
//      PrintStream f;
// spec.harness.Context.out.println( "****WARNING "+ new Exception() );	     
      
      // OK, we have a stream. Now the tricky part!
      if (vv.size() > 2) {
        for (int i=2; i< vv.size(); i++) {
          s += engine.PPFacts(vv.get(i).AtomValue());
          
        }
      } else 
        s = engine.PPFacts();
// spec.harness.Context.out.println( "****WARNING "+ new Exception() );	
//      f.println(s);
//      f.close();
      return _true;
      
    } else
      throw new ReteException("Funcall::SimpleExecute",
                              "Unimplemented function",
                              functor);
    }    

  
  public String toString() {
    String s = "[Funcall: ";
    try {
      s += vvec.get(0).StringValue();
    } catch (ReteException re) { s += "MALFORMED"; }
    s += "]";
    return s;
  }

}



