// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Funcall.java
// A class for parsing, assembling, and interpreting function calls
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
// $Id: Funcall.java,v 1.11 1997/12/02 01:14:32 ejfried Exp $
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.io.*;
import java.net.*;
import java.applet.*;
import java.util.*;

/**
 A class for parsing, assembling, and interpreting function calls
@author E.J. Friedman-Hill (C)1996

*/

public class Funcall extends ValueVector
{
  
  static Value _true = null;
  static Value _false = null;
  static Value _nil = null;
  
  static Hashtable intrinsics = new Hashtable(50);

  static
  {
    try
      {
        _true = new Value(RU.putAtom("TRUE"),RU.ATOM);
        Funcall._false = new Value(RU.putAtom("FALSE"),RU.ATOM);
        _nil = new Value(RU.putAtom("nil"),RU.ATOM);
      }
    catch (ReteException re)
      {
        spec.harness.Context.out.println("*** FATAL ***: Can't instantiate constants");
        System.exit(0);
      }

    // Load in all the intrinsic functions

    String [] intlist = {"_return", "_assert","_retract","_printout",
                         "_read","_readline","_gensym_star","_while",
                         "_if","_bind","_modify","_and","_or","_not",
                         "_eq","_equals","_not_equals","_gt","_lt",
                         "_gt_or_eq","_lt_or_eq","_neq","_mod","_plus",
                         "_times","_minus","_divide","_sym_cat","_reset",
                         "_run","_facts","_rules","_halt","_exit","_clear",
                         "_watch","_unwatch","_jess_version_string",
                         "_jess_version_number","_load_facts","_save_facts",
                         "_assert_string","_undefrule"};
    
    try
      {
        for (int i=0; i< intlist.length; i++)
          {
            Userfunction uf = (Userfunction)
              Class.forName("spec.benchmarks._202_jess.jess." +
	        intlist[i]).newInstance();
            Funcall.intrinsics.put(RU.getAtom(uf.name()), uf);
          }
      }
    catch (Throwable t)
      {
        spec.harness.Context.out.println("*** FATAL ***: Missing intrinsic function class");
        System.exit(0);
      }
  }
  
  /**
    Engine is the Rete engine we're parsed into
    */

  Rete engine;

  Funcall(String name, Rete engine) throws ReteException
  {
    this.engine = engine;
    add(new Value(RU.putAtom(name), RU.ATOM));
  }

  private Funcall(int size)
  {
    super(size);
  }
  
  public Object clone()
  {
    Funcall vv = new Funcall(ptr);
    vv.ptr = ptr;
    System.arraycopy(v, 0, vv.v, 0, ptr);
    vv.engine = engine;
    return vv;
  }

  public final static Value TRUE()
  {
    return _true;
  }
  
  public final static Value FALSE()
  {
    return _false;
  }
  
  public final static Value NIL()
  {
    return _nil;
  }
  
  
  /**
    Add an argument to this funcall
    */

  void AddArgument(String value, int type) throws ReteException
  {
    AddArgument(RU.putAtom(value), type);
  }

  void AddArgument(int value, int type) throws ReteException
  {
    add(new Value(value, type));
  }

  void AddArgument(double value, int type) throws ReteException 
  {
    add(new Value(value, type));
  }

  void AddArgument(Funcall value) throws ReteException
  {
    add(new Value(value, RU.FUNCALL));
  }

  void AddArgument(Fact value) throws ReteException
  {
    ValueVector vv = value.FactData();
    add(new Value(vv, vv.get(RU.DESC).DescriptorValue()));
  }
  
  void AddArgument(ValueVector value, int type) throws ReteException
  {
    add(new Value(value, type));
  }
  
  /**
    Execute a Funcall, recursively expanding its fields in the context of a
    Defrule. 

    @returns A Value structure containing the return value, or null
    if expand_root false.
    */

  static Value Execute(ValueVector vv, Context context)
       throws ReteException
  {
    int size = vv.size();
    String functor = vv.get(0).StringValue();    
    
    if (!(functor.equals("if")) && !(functor.equals("while"))
        && !(functor.equals("and")) && !(functor.equals("or")))
      {
        // for each argument
        for (int i=1; i<size; i++)
          {
            int type = vv.get(i).type();
            if (type == RU.FUNCALL)
              {
                
                // dig out the subexpression
                ValueVector subexp = vv.get(i).FuncallValue();
                
                // expand subexpression, keeping return value
                Value r = Execute(subexp, context);
                vv.set(r,i);
                
              }
            else if (type == RU.UNORDERED_FACT ||
                     type == RU.ORDERED_FACT)
              {
                
                ValueVector fact = vv.get(i).FactValue();
                for (int j=RU.FIRST_SLOT; j<fact.size(); j++)
                  {
                    if (fact.get(j).type() == RU.FUNCALL)
                      {
                        ValueVector subexp = fact.get(j).FuncallValue();
                        Value r = Execute(subexp, context);
                        fact.set(r,j);
                      }
                  }
              }
            else if (type == RU.LIST)
              {
                
                ValueVector list = vv.get(i).ListValue();
                for (int j=1; j<list.size(); j++)
                  {
                    if (list.get(j).type() == RU.FUNCALL)
                      {
                        ValueVector subexp = list.get(j).FuncallValue();
                        Value r = Execute(subexp, context);
                        list.set(r,j);
                      }
                  }
                vv.set(new Value(Context.FlattenList(list), RU.LIST), i);
              }
          }
      }
    // we need to expand ourselves too
    return SimpleExecute(vv, context);
  }
  
  /**
    Execute this Funcall
    */
  
  Value Execute(Context context) throws ReteException
  {
    return Execute(this, context);
  }
  
  /**
    Execute a funcall in a particular context
    We let the Value class do all our argument type checking, leading
    to remarkably small code!
    */
  
  static Value SimpleExecute(ValueVector vv, Context context)
       throws ReteException
  {
    
    Rete engine = context.engine;
    String functor = vv.get(0).StringValue();
    
    Deffunction df = engine.FindDeffunction(functor);
    if (df != null) 
      return df.Call(vv, context);
    
    Userfunction uf = engine.FindUserfunction(functor);
    if (uf != null) 
      return uf.Call(vv, context);

    Userfunction intf = (Userfunction) intrinsics.get(functor);
    if (intf != null) 
      return intf.Call(vv, context);

    else
      throw new ReteException("Funcall::SimpleExecute",
                              "Unimplemented function",
                              functor);
  }
}    

// *** return  *******************************************
class _return implements Userfunction
{
  public int name() { return RU.putAtom("return"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    return context.SetReturnValue(vv.get(1));
  }
}

// *** assert  *******************************************
class _assert implements Userfunction
{
  public int name() { return RU.putAtom("assert"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    int result = -1;
    for (int i=1; i< vv.size(); i++)
      {
        ValueVector fact = vv.get(i).FactValue();
        result = context.engine().Assert(fact);
      }
    if (result != -1)
      return new Value(result, RU.FACT_ID);
    else
      return Funcall._false;
  }
}
  
// *** retract *******************************************
        
class _retract implements Userfunction
{
  public int name() { return RU.putAtom("retract"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=1; i< vv.size(); i++) 
      {
        context.engine().Retract(vv.get(i).FactIDValue());
      }
    return Funcall._true;
  }
}
        
// *** printout *******************************************
      
 
class _printout implements Userfunction
{
  public int name() { return RU.putAtom("printout"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    if (!vv.get(1).StringValue().equals("t"))
      throw new ReteException("Funcall::SimpleExecute",
                              "printout: bad router",
                              vv.get(1).StringValue());
    
    PrintStream ps = context.engine().display().stdout();
    
    for (int i = 2; i < vv.size(); i++) 
      {
        switch(vv.get(i).type()) 
          {
          case RU.ATOM:
            if (vv.get(i).StringValue().equals("crlf"))
              ps.println();
            else
              ps.print(vv.get(i).StringValue());
            break;
          case RU.INTEGER:
            ps.print(vv.get(i).IntValue()); break;
          case RU.FLOAT:
            {
              Float f = new Float(vv.get(i).FloatValue()); 
              if (f.intValue() == f.floatValue())
                ps.print(f.intValue());
              else
                ps.print(f.floatValue());
              break;
            }
          case RU.FACT_ID:
            ps.print("<Fact-" + vv.get(i).FactIDValue() + ">");
            break;
          case RU.STRING:
            ps.print(vv.get(i).StringValue());
            break;
          case RU.LIST:
            ps.print(vv.get(i).toString());
            break;
          default:
            throw new ReteException("Funcall::SimpleExecute",
                                    "printout: bad data type",
                                    "type =" +vv.get(i).type());
            
          }
      }
    ps.flush();
    return Funcall._nil;
  }
}  
// *** read *******************************************
class _read implements Userfunction
{
  public int name() { return RU.putAtom("read"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    if (vv.size() > 1)
      if (!vv.get(1).StringValue().equals("t"))
        throw new ReteException("Funcall::SimpleExecute",
                                "read: bad router",
                                vv.get(1).StringValue());
        
    StreamTokenizer st = context.engine().advance();
    switch (st.ttype) 
      {
      case -3: // st.TT_WORD. 1.3 compiler requires constant expr here.
        return new Value(RU.putAtom(st.sval), RU.ATOM);
      case -2: // st.TT_NUMBER. 1.3 compiler requires constant expr here.
        return new Value(st.nval, RU.FLOAT);
      case '"':
        return new Value(RU.putAtom(st.sval), RU.STRING);
      default:
        return new Value(RU.putAtom("" + st.ttype), RU.ATOM);
      }
  }
}       
// *** readline  *******************************************
 
class _readline implements Userfunction
{
  public int name() { return RU.putAtom("readline"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    if (vv.size() > 1)
      if (!vv.get(1).StringValue().equals("t"))
        throw new ReteException("Funcall::SimpleExecute",
                                "readline: bad router",
                                vv.get(1).StringValue());
        
    try 
      {
        DataInputStream dis
          = new DataInputStream(context.engine().display().stdin());
        return new Value(dis.readLine(), RU.STRING);
      } 
    catch (Throwable t) 
      {
        return new Value("", RU.STRING);
      }
  }
}

// *** gensym*  *******************************************

class _gensym_star implements Userfunction
{
  public int name() { return RU.putAtom("gensym*"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    return new Value(RU.gensym("gen"), RU.STRING);
  }
}
// *** while *******************************************

class _while implements Userfunction
{
  public int name() { return RU.putAtom("while"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    // This accepts a superset of the correct syntax...
    Value result = Funcall._nil;
  outer_loop:
    while (! Funcall.Execute(context.ExpandAction(vv.get(1).FuncallValue()),
                             context).equals(Funcall._false)) 
      {
        for (int i=2; i< vv.size(); i++) 
          {
            Value current = vv.get(i);
            if (current.type() == RU.FUNCALL) 
              {
                ValueVector copy = context.ExpandAction(current.FuncallValue());
                result = Funcall.Execute(copy, context);
                if (context.Returning()) 
                  {
                    result = context.GetReturnValue();
                    break outer_loop;
                  }
              } 
            else if (current.type() == RU.ATOM && current.StringValue().equals("do"))
              // just go on to the next action
              continue;
          }
      }
    return result; 
  }
}
// *** if *******************************************

class _if implements Userfunction
{
  public int name() { return RU.putAtom("if"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    // This accepts a superset of the correct syntax...
    // check condition
    Value result = Funcall.Execute(vv.get(1).FuncallValue(), context);
    if (!(result.equals(Funcall._false))) 
      {
        // do 'then' part
        result = Funcall._false;
        for (int i=3; i< vv.size(); i++) 
          {
            if (vv.get(i).type() == RU.FUNCALL) 
              {
                ValueVector copy = context.ExpandAction(vv.get(i).FuncallValue());
                result = Funcall.Execute(copy, context);
                if (context.Returning()) 
                  {
                    result = context.GetReturnValue();
                    break;
                  }
              } else
                break;
                
          }
        return result; 
      }
    else
      {
        // first find the 'else'
        result = Funcall._false;
        boolean seen_else = false;
        for (int i=3; i< vv.size(); i++)
          {
            if (vv.get(i).type() == RU.FUNCALL && seen_else) 
              {
                ValueVector copy = context.ExpandAction(vv.get(i).FuncallValue());
                result = Funcall.Execute(copy, context);
                if (context.Returning()) 
                  {
                    result = context.GetReturnValue();
                    break;
                  }
              } 
            else if (vv.get(i).type() == RU.ATOM &&
                     vv.get(i).StringValue().equals("else"))
              seen_else = true;
          }
        return result;
      }
  }
}
        
// *** bind *******************************************
        
class _bind implements Userfunction
{
  public int name() { return RU.putAtom("bind"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    context.SetVariable(vv.get(1).VariableValue(),
                        vv.get(2));
    return vv.get(2);
  }
}
// *** modify  *******************************************
        
class _modify implements Userfunction
{
  public int name() { return RU.putAtom("modify"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    ValueVector fact;
    if ((fact
         = context.engine.findFactByID(vv.get(1).FactIDValue())) == null)
      throw new ReteException("Funcall::SimpleExecute",
                              "modify: no such fact",
                              "");
        
    // we have the ValueVector fact, make a Fact out of it
        
    Fact f = new Fact(fact, context.engine());
        
    // now change the values. For each argument...
    for (int i= 2; i < vv.size(); i++) 
      {
            
        // fetch the slot, value subexp, stored as a List
        ValueVector svp = vv.get(i).ListValue();
            
        int slot_desc_idx = (f.FindSlot(svp.get(0).AtomValue()) - RU.FIRST_SLOT)
          * RU.DT_SLOT_SIZE  + RU.FIRST_SLOT;
            
        if (f.deft.get(slot_desc_idx).type() == RU.MULTISLOT
            && svp.get(1).type() != RU.LIST) 
          {
            // wrap the new value in a list before adding
            ValueVector mf = new ValueVector();
            for (int j=1; j < svp.size(); j++)
              mf.add(svp.get(j));
            f.AddValue(svp.get(0).StringValue(), new Value(mf, RU.LIST));
          }
        else 
          {
            // add the new value directly
            f.AddValue(svp.get(0).StringValue(), svp.get(1));
          }
      }
        
    // get rid of the old one.
    context.engine().Retract(fact.get(RU.ID).FactIDValue());
        
    // get the new fact data
    fact = f.FactData();
        
    // and assert the new fact
    return new Value(context.engine().Assert(fact), RU.FACT_ID);
  }
}

// *** and *******************************************

class _and implements Userfunction
{
  public int name() { return RU.putAtom("and"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=1; i<vv.size(); i ++) 
      {
        if (Funcall.Execute(context.ExpandAction(vv.get(i).FuncallValue()),
                            context).equals(Funcall._false))
          return Funcall._false;
      }
    return Funcall._true;
        
  }
}
// *** or *******************************************
class _or implements Userfunction
{
  public int name() { return RU.putAtom("or"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=1; i<vv.size(); i ++) 
      {
        if (Funcall.Execute(context.ExpandAction(vv.get(i).FuncallValue()),
                            context).equals(Funcall._true))
          return Funcall._true;
      }
    return Funcall._false;
  }
}
        
// *** or *******************************************

class _not implements Userfunction
{
  public int name() { return RU.putAtom("not"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    if (vv.get(1).equals(Funcall._false))
      return Funcall._true;
    else
      return Funcall._false;
  }
}
// *** eq *******************************************
 
class _eq implements Userfunction
{
  public int name() { return RU.putAtom("eq"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=2; i<vv.size(); i ++) 
      {
        if (!vv.get(i).equals(vv.get(1)))
          return Funcall._false;
      }
    return Funcall._true;
  }
}
// *** = *******************************************


class _equals implements Userfunction
{
  public int name() { return RU.putAtom("="); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=2; i<vv.size(); i ++) 
      {
        if (!(vv.get(i).NumericValue() == vv.get(1).NumericValue()))
          return Funcall._false;
      }
    return Funcall._true;
  }
}
        
// *** <> *******************************************

class _not_equals implements Userfunction
{
  public int name() { return RU.putAtom("<>"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=2; i<vv.size(); i ++) 
      {
        if (vv.get(i).NumericValue() == vv.get(1).NumericValue())
          return Funcall._false;
      }
    return Funcall._true;
  }
}
        
// *** > *******************************************

class _gt implements Userfunction
{
  public int name() { return RU.putAtom(">"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=1; i<vv.size()-1; i ++) 
      {
        double value1 = vv.get(i).NumericValue();
        double value2 = vv.get(i+1).NumericValue();
            
        if (!(value1 > value2))
          return Funcall._false;
      }
    return Funcall._true;
  }
}
        
// *** < *******************************************

class _lt implements Userfunction
{
  public int name() { return RU.putAtom("<"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=1; i<vv.size()-1; i ++) 
      {
        double value1 = vv.get(i).NumericValue();
        double value2 = vv.get(i+1).NumericValue();
            
        if (!(value1 < value2))
          return Funcall._false;
      }
    return Funcall._true;
  }
}
        
// *** >= *******************************************

class _gt_or_eq implements Userfunction
{
  public int name() { return RU.putAtom(">="); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=1; i<vv.size()-1; i ++) 
      {
        double value1 = vv.get(i).NumericValue();
        double value2 = vv.get(i+1).NumericValue();
            
        if (!(value1 >= value2))
          return Funcall._false;
      }
    return Funcall._true;
  }
}
// *** <= *******************************************
class _lt_or_eq implements Userfunction
{
  public int name() { return RU.putAtom("<="); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=1; i<vv.size()-1; i ++) 
      {
        double value1 = vv.get(i).NumericValue();
        double value2 = vv.get(i+1).NumericValue();
          
        if (!(value1 <= value2))
          return Funcall._false;
      }
    return Funcall._true;
  }
}
// *** neq *******************************************

class _neq implements Userfunction
{
  public int name() { return RU.putAtom("neq"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=2; i<vv.size(); i ++) 
      {
        if (!vv.get(i).equals(vv.get(1)))
          return Funcall._true;
      }
    return Funcall._false;
  }
}
// *** mod *******************************************

class _mod implements Userfunction
{
  public int name() { return RU.putAtom("mod"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    int d1 = (int) vv.get(1).NumericValue();
    int d2 = (int) vv.get(2).NumericValue();
        
    // create a new Value to hold the result.
    return new Value(d1 % d2, RU.INTEGER);
        

  }
}
// *** + *******************************************
class _plus implements Userfunction
{
  public int name() { return RU.putAtom("+"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    double sum = 0;
    for (int i=1; i<vv.size(); i++) 
      {
        sum += vv.get(i).NumericValue();
      }
      
    return new Value(sum, RU.FLOAT);
      
  } 
}
// *** * *******************************************

class _times implements Userfunction
{
  public int name() { return RU.putAtom("*"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    double product = 1;
    for (int i=1; i<vv.size(); i++) 
      {
        product *= vv.get(i).NumericValue();
      }
                                  
    return new Value(product, RU.FLOAT);

  } 
}
// *** - *******************************************
class _minus implements Userfunction
{
  public int name() { return RU.putAtom("-"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    double diff = vv.get(1).NumericValue();

    for (int i=2; i<vv.size(); i++) 
      {
        diff -= vv.get(i).NumericValue();
      }

    return new Value(diff, RU.FLOAT);

  } 
}
// *** / *******************************************
class _divide implements Userfunction
{
  public int name() { return RU.putAtom("/"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    double quotient = vv.get(1).NumericValue();
      
    for (int i=2; i<vv.size(); i++) 
      {
        quotient /= vv.get(i).FloatValue();
      }
    return new Value(quotient, RU.FLOAT);


  } 
}

// *** sym-cat *******************************************
class _sym_cat implements Userfunction
{
  public int name() { return RU.putAtom("sym-cat"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    StringBuffer buf = new StringBuffer( "" );
    
    for ( int i = 1; i < vv.size( ); i++ )
      buf.append( vv.get( i ).toString());
    
    return new Value( buf.toString( ), RU.ATOM );
  }
}
// *** reset *******************************************

class _reset implements Userfunction
{
  public int name() { return RU.putAtom("reset"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    context.engine().Reset();
    return Funcall._true  ;
      
  }
}
// *** run *******************************************

class _run implements Userfunction
{
  public int name() { return RU.putAtom("run"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    context.engine().Run();
    return Funcall._true;
  }
}
// *** facts *******************************************

class _facts implements Userfunction
{
  public int name() { return RU.putAtom("facts"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    context.engine().ShowFacts();
    return Funcall._true;
      
  } 
  // *** rules *******************************************

}
class _rules implements Userfunction
{
  public int name() { return RU.putAtom("rules"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    context.engine().ShowRules();
    return Funcall._true;

      
  } 
}
// *** halt *******************************************
class _halt implements Userfunction
{
  public int name() { return RU.putAtom("halt"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    context.engine().Halt();
    return Funcall._true;
  }
}
// *** exit *******************************************
      

class _exit implements Userfunction
{
  public int name() { return RU.putAtom("exit"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    System.exit(0);
    return Funcall._true;
  }
}
// *** halt *******************************************
      
class _clear implements Userfunction
{
  public int name() { return RU.putAtom("clear"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    context.engine().clear();
    return Funcall._true;
  }
}
// *** watch *******************************************
class _watch implements Userfunction
{
  public int name() { return RU.putAtom("watch"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    String what = vv.get(1).StringValue();
    if (what.equals("rules"))
      context.engine().watch_rules(true);
    else if (what.equals("facts"))
      context.engine().watch_facts(true);
    else if (what.equals("compilations"))
      context.engine().watch_compilations(true);
    else if (what.equals("activations"))
      context.engine().watch_activations(true);
    else if (what.equals("all")) 
      {
        context.engine().watch_facts(true);
        context.engine().watch_rules(true);
        context.engine().watch_compilations(true);
        context.engine().watch_activations(true);
      }
    else
      throw new ReteException("Funcall::Execute", "watch: can't watch" ,
                              what);

    return Funcall._true;
  }
}
// *** unwatch *******************************************

class _unwatch implements Userfunction
{
  public int name() { return RU.putAtom("unwatch"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    String what = vv.get(1).StringValue();
    if (what.equals("rules"))
      context.engine().watch_rules(false);
    else if (what.equals("facts"))
      context.engine().watch_facts(false);
    else if (what.equals("compilations"))
      context.engine().watch_compilations(false);
    else if (what.equals("all")) 
      {
        context.engine().watch_facts(false);
        context.engine().watch_rules(false);
        context.engine().watch_compilations(false);
        context.engine().watch_activations(false);
      }
    else
      throw new ReteException("Funcall::Execute", "unwatch: can't unwatch" ,
                              what);
    return Funcall._true;
  }
}
// *** jess versions  *******************************************

 
class _jess_version_string implements Userfunction
{
  public int name() { return RU.putAtom("jess-version-string"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    return new Value("Jess Version 3.2 11/8/97", RU.STRING);
  } 
}
class _jess_version_number implements Userfunction
{
  public int name() { return RU.putAtom("jess-version-number"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    return new Value(3.2, RU.FLOAT);
  }
}

// *** load-facts ***********************************************

class _load_facts implements Userfunction
{
  public int name() { return RU.putAtom("load-facts"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    String s = "ERROR";
    InputStream f;
    ///if (context.engine().display().applet() == null) 
      {
        try 
          {
            f = new FileInputStream(vv.get(1).StringValue());
          }
        catch (Throwable t) 
          {
            throw new ReteException("Funcall::SimpleExecute", "load-facts", t.toString());
          }
        
      }
    ///
    /*else 
      {
        try 
          {
            URL url = new URL(context.engine().display().applet().getDocumentBase(),
                              vv.get(1).StringValue());          
            f = url.openStream();
          } 
        catch (Throwable t) 
          {
            throw new ReteException("Funcall::SimpleExecute", "load-facts", t.toString());
          }
      }*/
      
    // OK, we have a stream. Now the tricky part!

    Jesp jesp = new Jesp(f, context.engine());

    return jesp.LoadFacts();
  }
}

// *** save-facts ***********************************************


class _save_facts implements Userfunction
{
  public int name() { return RU.putAtom("save-facts"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    String s = "";
    PrintStream f;
    ///if (context.engine().display().applet() == null) 
      {
        try 
          {
            f = new PrintStream(new FileOutputStream(vv.get(1).StringValue()));
          } 
        catch (Throwable t) 
          {
            throw new ReteException("Funcall::SimpleExecute", "save-facts", t.toString());
          }
        
      }
    ///
    /*else 
      {
        try 
          {
            URL url = new URL(context.engine().display().applet().getDocumentBase(),
                              vv.get(1).StringValue());          
            URLConnection urlc  = url.openConnection();
            urlc.setDoOutput(true);
            f = new PrintStream(urlc.getOutputStream());
          }
        catch (Throwable t) 
          {
            throw new ReteException("Funcall::SimpleExecute", "load-facts", t.toString());
          }
      }*/
      
    // OK, we have a stream. Now the tricky part!
    if (vv.size() > 2) 
      {
        for (int i=2; i< vv.size(); i++) 
          {
            s += context.engine().PPFacts(vv.get(i).AtomValue());
          
          }
      }
    else 
      s = context.engine().PPFacts();

    f.println(s);
    f.close();
    return Funcall._true;
      
  } 
}

class _assert_string implements Userfunction
{
  public int name() { return RU.putAtom("assert-string"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    String fact = vv.get(1).StringValue();
    return new Value(context.engine().AssertString(fact), RU.FACT_ID);      

  }
}
class _undefrule implements Userfunction
{
  public int name() { return RU.putAtom("undefrule"); }
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    int rulename = vv.get(1).AtomValue();
    return context.engine().Undefrule(rulename);      

  } 

}    






