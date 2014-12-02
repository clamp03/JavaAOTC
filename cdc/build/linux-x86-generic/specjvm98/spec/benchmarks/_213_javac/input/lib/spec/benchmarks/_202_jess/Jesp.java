// -*- java -*-
//////////////////////////////////////////////////////////////////////
// jesp.java
// Parser functions for Java Expert System
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
  Java Expert System Parser

  A more elegant parser could certainly be written; the CLIPS
  language is simple enough, however, that this code suffices for now.

@author E.J. Friedman-Hill (C)1996
*/

public class Jesp {
  /**
    Stream where input comes from
   */
  private StreamTokenizer st;
  public Rete engine;

  /**
    Constructor
    */

  public Jesp(InputStream is, Rete e) {
    InitTokenizer(is);
    this.engine = e;
  }  
  /**
    Set up the tokenizer on stream is
    */

  public boolean InitTokenizer(InputStream is) {
    st = new StreamTokenizer(is);
    st.commentChar(';');
    st.wordChars('*','*');
    st.wordChars('=','=');
    st.wordChars('+','+');
    st.wordChars('/','/');
    st.wordChars('<','<');
    st.wordChars('>','>');
    st.wordChars('_','_');
    return true;
  }

  /**
    Advance the tokenizer
    */

  public int advance() throws ReteException {
    try {
      int rv = st.nextToken();
      // spec.harness.Context.out.println(st.toString());
      return rv;
    } catch (Throwable t) {
      throw new ReteException("Jesp::advance",
                              "File error",
                              t.toString());
    }
  }


  /**
    Parses an input file. Returns true on success.
    */

  public Value parse() throws ReteException {
    Value val = Funcall.NIL();
    while (advance() != st.TT_EOF) {
      val = ParseSexp();
    } 
    return val;
  }

  public boolean EOF()  {
    return (st.ttype == st.TT_EOF);
  }

  /**
    Parses a sexp. The opening paren should be current on entry.
    */

  private Value ParseSexp() throws ReteException {
    if (st.ttype != '(')
    if (advance() != st.TT_WORD)
      throw new ReteException("Jesp::ParseSexp", "Expected '(' at line " + st.lineno(),
                              "" + st.ttype);

    /* first symbol is always a word */
    if (advance() != st.TT_WORD)
      throw new ReteException("Jesp::ParseSexp", "Expected an atom at line "+st.lineno(),
                              "" + st.ttype);
    
    if (st.sval.equals("defrule"))
      return ParseDefrule();

    else if (st.sval.equals("deftemplate"))
      return ParseDeftemplate();

    else if (st.sval.equals("deffacts"))
      return ParseDeffacts();

    else if (st.sval.equals("deffunction"))
      return ParseDeffunction();

    else if (st.sval.equals("defglobal"))
      return ParseDefglobal();

    /* if not a valid funcall, this will throw an exception */
    else {
      Funcall f = ParseFuncall();
      engine.global_context.Push();
      Value v = f.Execute(engine.global_context);
      engine.global_context.Pop();
      return v;
    }
  }

  /**
    Parse a defglobal sexp.
    The word 'defglobal' should be the current token on entry
    */

  private Value ParseDefglobal() throws ReteException {
    Defglobal dg = new Defglobal();
    advance();

    // now we're looking for global definitions. These look like
    // ?*<name>* = <value>.

    while (st.ttype == '?') {
      advance();
      // name of variable expected
      if (st.ttype != st.TT_WORD)
        throw new ReteException("Jesp::ParseDefglobal",
                                "Expected an atom for variable at line " + st.lineno(),
                                "" + st.ttype);
      String g = st.sval;
      
      //skip the equals sign
      advance();
      advance();

      switch (st.ttype) {
      case st.TT_WORD:
        dg.AddGlobal(g, new Value(st.sval, RU.ATOM)); break;
      case st.TT_NUMBER:
        dg.AddGlobal(g, new Value(st.nval, RU.FLOAT)); break;
      case '"':
        dg.AddGlobal(g, new Value(st.sval, RU.STRING)); break;
      case '?':
        advance();
        dg.AddGlobal(g, new Value(st.sval, RU.VARIABLE)); break;
      case '(':
        advance();
        Funcall fc = ParseFuncall();
        dg.AddGlobal(g, new Value(fc.vvec, RU.FUNCALL)); break;
      default:
        throw new ReteException("Jesp::ParseDefglobal",
                                "Bad value at line " + st.lineno(),
                                "\"" + (char) st.ttype + "\"");
      }
      advance();
    }

    // Leave final paren as current token

    if (st.ttype != ')')
      throw new ReteException("Jesp::ParseDefglobal","Syntax error at line "
                              + st.lineno(), "");

    engine.AddDefglobal(dg);
    return Funcall.TRUE();

  }

  /**
    Parse a deffacts sexp.
    The word 'deffacts' should be the current token on entry
    */

  private Value ParseDeffacts() throws ReteException {
    Deffacts df;
    String name;
    advance();

    // name of deffacts required
    if (st.ttype != st.TT_WORD)
      throw new ReteException("Jesp::ParseDeffacts", "Expected an atom at line "
                              + st.lineno(), "" + st.ttype);

    name = st.sval;
    df = new Deffacts(st.sval);
    advance();

    // Deffacts docstrings are not required,
    // just encouraged...
    if (st.ttype == '"') {
      df.docstring = st.sval;
      advance();
    } 

    // now we're looking for facts.

    while (st.ttype == '(') {
      Fact f = ParseFact();
      df.AddFact(f);
    }

    // Leave final paren as current token

    if (st.ttype != ')')
      throw new ReteException("Jesp::ParseDeffacts","Syntax error at line "
                              + st.lineno(), name);

    engine.AddDeffacts(df);
    return Funcall.TRUE();

  }

  /**
    Parse a defrule sexp.
    The word 'defrule' should be the current token on entry
    A defrule looks something like this:
    <PRE>
    (defrule name
      "docstring...."
      (pattern 1)
      ?foo <- (pattern 2)
      (pattern 3)
     =>
      (action 1)
      (action ?foo))
    </PRE>
    */

  private Value ParseDefrule() throws ReteException {
    Defrule dr;
    String name;

    if (!st.sval.equals("defrule"))
      throw new ReteException("Jesp::ParseDefrule", "Expected 'defrule' at line "
                              + st.lineno(), "" + st.ttype);

    advance();

    // name of defrule required
    if (st.ttype != st.TT_WORD) 
      throw new ReteException("Jesp::ParseDefrule", "Expected an atom at line "
                              + st.lineno(), "" + st.ttype);
    name = st.sval;
    dr = new Defrule(st.sval, engine);
    advance();

    // Defrule docstrings are not required,
    // just encouraged...
    if (st.ttype == '"') {
      dr.docstring = st.sval;
      advance();
    } 

    int factIndex = 0;

    // check for salience declaration
    if (st.ttype == '(') {
      Pattern p = ParsePattern();
      if (p.classname() == RU.putAtom("declare")) {
        // just assume that it's salience.
        // Too ugly to do otherwise here right now.
        // this needs work.
        Test1 t = p.tests[0][0];
        dr.salience = (int) t.slot_value.NumericValue();
        
      } else {
        if (factIndex == 0  && p.negated())
          dr.AddPattern(new Pattern("initial-fact",
                                    RU.ORDERED_FACT, engine));
        ++factIndex;
        dr.AddPattern(p);
      }
    } 
    
    // now we're looking for just patterns

    while (st.ttype == '(' || st.ttype == '?') {
      switch (st.ttype) {
      case '(': {
        // pattern not bound to a var
        // looks like
        // (pattern 1 2 3)
        Pattern p = ParsePattern();
        if (factIndex == 0  && p.negated())
          dr.AddPattern(new Pattern("initial-fact", RU.ORDERED_FACT, engine));

        dr.AddPattern(p);
        ++factIndex;
        break;
      }
      case '?': {
        // pattern bound to a variable
        // These look like this:
        // ?name <- (pattern 1 2 3)
        advance();
        dr.AddBinding(RU.putAtom(st.sval), factIndex, RU.PATTERN);
        advance();
        advance();
        Pattern p = ParsePattern();
        if (p.negated())
          throw new ReteException("Jesp::ParseDefrule",
                                  "not and test CEs cannot be bound to vars at line "
                                  + st.lineno(),
                                  name + ":" + factIndex);
        dr.AddPattern(p);
        ++factIndex;
        break;
       }
      } 
    }

    if (factIndex == 0) {
      // No patterns for this rule; we will fire on "initial-fact".
      Pattern p = new Pattern("initial-fact", RU.ORDERED_FACT, engine);
      dr.AddPattern(p);
    }

    if (st.ttype != st.TT_WORD || !st.sval.equals("=>"))
      throw new ReteException("Jesp::ParseDefrule","expected '=>' in rule at line "
                              + st.lineno(), name);
    advance(); // throw out '=>'
    
    while (st.ttype == '(') {
      advance();
      Funcall f = ParseFuncall();
      dr.AddAction(f);
      advance();
    }
    // leave on closing ')'

    engine.AddDefrule(dr);
    return Funcall.TRUE();
  }
  /**
    Parse a Deffunction object. Deffunctions look like this:
    (deffunction name "doc-comment" (<arg1><arg2...)
    (action)
    (action)
    ... )
   */

  
  private Value ParseDeffunction() throws ReteException {
    Deffunction df;
    String name;

    if (!st.sval.equals("deffunction"))
      throw new ReteException("Jesp::ParseDeffunction", "Expected 'deffunction' at line "
                              + st.lineno(),
                              "" + st.ttype);

    advance();

    // name of deffunction required
    if (st.ttype != st.TT_WORD) 
      throw new ReteException("Jesp::ParseDeffunction", "Expected an atom at line "
                              + st.lineno(),"" + st.ttype);
    name = st.sval;
    df = new Deffunction(st.sval, engine);
    advance();

    // Deffunction docstrings are not required,
    // just encouraged...
    if (st.ttype == '"') {
      df.docstring = st.sval;
      advance();
    } 

    // Skip opening paren.
    advance();
    while (st.ttype != ')')
      if (st.ttype == '?') {
        advance();
        df.AddArgument(st.sval);
        advance();
      } else 
        throw new ReteException("Jesp::ParseDeffunction",
                                "Bad argument to deffunction at line " + st.lineno(),
                                name);
    advance();
    
    while (st.ttype == '(') {
      advance();
      Funcall f = ParseFuncall();
      df.AddAction(f);
      advance();
    }
    // leave on closing ')'

    engine.AddDeffunction(df);
    return Funcall.TRUE();
  }

  

  /**
    Parse a Fact object
    The opening paren should be current on entry

    <PRE>
    A fact is very simple: either an ordered fact:
    (atom field1 2 "field3")
    or an unordered fact:
    (atom (slotname value) (slotname value2))
    </PRE>
    */

  public Fact ParseFact() throws ReteException {
    String name, slot;
    Fact f;

    if (st.ttype != '(')
      throw new ReteException("Jesp::ParseFact",
                              "Syntax error at line " + st.lineno(),
                              "\"" + (char) st.ttype + "\"");
    
    // class name
    advance();
    if (st.ttype != st.TT_WORD)
      throw new ReteException("Jesp::ParseFact",
                              "Bad class name at line " + st.lineno(),
                              "\"" + (char) st.ttype + "\"");
    name = st.sval;
    advance();

    // What we do next depends on whether we're parsing
    // an ordered or unordered fact. We can determine this very easily: If there
    // is an existing unordered deftemplate, this is an unordered fact.
    // Otherwise, it is ordered!

    // get a deftemplate if one already exists.

    ValueVector deft = engine.FindDeftemplate(name);
    int ordered;
    if (deft == null)
      ordered = RU.ORDERED_FACT;
    else
      ordered = deft.get(RU.DESC).DescriptorValue();

    if (ordered == RU.UNORDERED_FACT) {
      f = new Fact(name, RU.UNORDERED_FACT, engine);
      while (st.ttype == '(') {
        advance();
        if (st.ttype != st.TT_WORD)
          throw new ReteException("Jesp::ParseFact",
                                  "Bad slot name at line " + st.lineno(),
                                  "\"" + (char) st.ttype + "\"");
        slot = st.sval;
        advance();
        switch (st.ttype) {
        case st.TT_WORD:
          f.AddValue(slot, st.sval, RU.ATOM); break;
        case st.TT_NUMBER:
          f.AddValue(slot, st.nval, RU.FLOAT); break;
        case '"':
          f.AddValue(slot, st.sval, RU.STRING); break;
        case '?':
          advance();
          f.AddValue(slot, st.sval, RU.VARIABLE); break;
        case '(':
          advance();
          Funcall fc = ParseFuncall();
          f.AddValue(slot, fc, RU.FUNCALL); break;
        default:
          throw new ReteException("Jesp::ParseFact",
                                  "Bad slot value at line " + st.lineno(),
                                  "\"" + (char) st.ttype + "\"");
        }
        advance();
        if (st.ttype == ')')
          advance();
        else
          throw new ReteException("Jesp::ParseFact",
                                  "Expected ')' at line " + st.lineno(),
                                  "\"" + (char) st.ttype + "\"");
      }
    } else {
      f = new Fact(name, RU.ORDERED_FACT, engine);
      while (st.ttype != ')') {
        switch (st.ttype) {
        case st.TT_WORD:
          f.AddValue(st.sval, RU.ATOM); break;
        case st.TT_NUMBER:
          f.AddValue(st.nval, RU.FLOAT); break;
        case '"':
          f.AddValue(st.sval, RU.STRING); break;
        case '?':
          advance();
          f.AddValue(st.sval, RU.VARIABLE); break;
        case '(':
          advance();
          Funcall fc = ParseFuncall();
          f.AddValue(fc, RU.FUNCALL); break;
        default:
          throw new ReteException("Jesp::ParseFact",
                                  "Bad slot value at line " + st.lineno(),
                                  "\"" + (char) st.ttype + "\"");
        }
        advance();
      }
    }
    if (st.ttype != ')')
      throw new ReteException("Jesp::ParseFact",
                              "Expected ')' at line " + st.lineno(),
                              "\"" + (char) st.ttype + "\"");


    // step off the closing paren. This is different than how the
    // def* constructs are parsed.
    advance();
    return f;
    
  }

  
  /**
    Parse a Pattern object in a Rule LHS context
    The opening paren should be current on entry

    <PRE>
    A Pattern is more complex than an ordinary fact:
    It can have '~' before any value, which means to test for NOT that value.
    </PRE>
    */

  private Pattern ParsePattern() throws ReteException {
    String name, slot;
    Pattern p;

    if (st.ttype != '(')
      throw new ReteException("Jesp::ParsePattern",
                              "Syntax error at line " + st.lineno(),
                              "\"" + (char) st.ttype + "\"");
    
    // class name
    advance();
    if (st.ttype != st.TT_WORD)
      throw new ReteException("Jesp::ParsePattern",
                              "Bad class name at line " + st.lineno(),
                              "\"" + (char) st.ttype + "\"");
    name = st.sval;

    if (name.equals("not")) {
      // this is a negated pattern; strip off the (not ) and parse
      // the actual pattern.
      advance();
      p = ParsePattern();
      p.setNegated();
      advance();
      return p;
    } 
    advance();

    // What we do next depends on whether we're parsing
    // an ordered or unordered fact. We can determine this very easily: If there
    // is a deftemplate, this is an unordered fact. Otherwise, it is ordered!

    // get a deftemplate if one already exists.
    ValueVector deft = engine.FindDeftemplate(name);
    int ordered;
    if (deft == null)
      ordered = RU.ORDERED_FACT;
    else
      ordered = deft.get(RU.DESC).DescriptorValue();

    if (ordered == RU.UNORDERED_FACT) {
      p = new Pattern(name, RU.UNORDERED_FACT, engine);
      while (st.ttype == '(') {
        advance();
        if (st.ttype != st.TT_WORD)
          throw new ReteException("Jesp::ParsePattern",
                                  "Bad slot name at line " + st.lineno(),
                                  "\"" + (char) st.ttype + "\"");
        slot = st.sval;
        advance();
        
        while (st.ttype != ')') {
          // if this is a '~'  pattern, keep track
          boolean not_slot = false;
          if (st.ttype == '~') {
            not_slot = true;
            advance();
          }
          
          switch (st.ttype) {
          case st.TT_WORD:
            p.AddTest(slot, new Value(st.sval, RU.ATOM), not_slot); break;
          case st.TT_NUMBER:
            p.AddTest(slot, new Value(st.nval, RU.FLOAT), not_slot); break;
          case '"':
            p.AddTest(slot, new Value(st.sval, RU.STRING), not_slot); break;
          case '?':
            advance();
            p.AddTest(slot, new Value(st.sval, RU.VARIABLE), not_slot); break;
          case ':':
            advance();
            advance();
            Funcall f = ParseFuncall();
            p.AddTest(slot, new Value(f.vvec, RU.FUNCALL), not_slot); break;

          default:
            throw new ReteException("Jesp::ParsePattern",
                                    "Bad slot value at line " + st.lineno(),
                                    "\"" + (char) st.ttype + "\"");
          }
          advance();
          if (st.ttype == '&')
            advance();
        }
        // skip this slot's closing paren
        advance();
        // are we at the end of this pattern?
        if (st.ttype == ')')
          // then get out of here.
          break;
        else if (st.ttype == '(')
          continue;
        else
          throw new ReteException("Jesp::ParsePattern",
                                  "Expected ')' or '(' at line " + st.lineno(),
                                  "\"" + (char) st.ttype + "\"");
      }
    } else {
      p = new Pattern(name, RU.ORDERED_FACT, engine);
      while (st.ttype != ')') {

        // if this is a '~' pattern, keep track

        do {
          if (st.ttype == '&')
            advance();
          boolean not_slot = false;
          if (st.ttype == '~') {
            not_slot = true;
            advance();
          }
          
          switch (st.ttype) {
          case st.TT_WORD:
            p.AddTest(new Value(st.sval, RU.ATOM), not_slot); break;
          case st.TT_NUMBER:
            p.AddTest(new Value(st.nval, RU.FLOAT), not_slot); break;
          case '"':
            p.AddTest(new Value(st.sval, RU.STRING), not_slot); break;
          case '?':
            advance();
            p.AddTest(new Value(st.sval, RU.VARIABLE), not_slot); break;
          case ':':
            advance();
            advance();
            Funcall f = ParseFuncall();
            p.AddTest(new Value(f.vvec, RU.FUNCALL), not_slot); break;
          default:
            throw new ReteException("Jesp::ParsePattern",
                                    "Bad slot value at line " + st.lineno(),
                                    "\"" + (char) st.ttype + "\"");
          }
          advance();
        } while (st.ttype == '&');
        p.advance();
      }
    }
    if (st.ttype != ')')
      throw new ReteException("Jesp::ParsePattern",
                              "Expected ') at line " + st.lineno(),
                              "\"" + (char) st.ttype + "\"");


    // step off the closing paren. This is different than how the
    // def* constructs are parsed.
    advance();

    return p;
    
  }


  /**
    Parse a simple Funcall object
    The functor should be current on entry

    A funcall is a general nested list; the first field (the functor)
    is always an atom.

    <PRE>
    (functor field1 (nested funcall) (double (nested funcall)))
    </PRE>

    */

  private Funcall ParseFuncall() throws ReteException {
    String name, slot;
    Funcall f;
    
    // functor
    switch (st.ttype) {
    case st.TT_WORD:
      name = st.sval;
      break;
    case '-': case '/':
      // special functors
      name = String.valueOf((char) st.ttype);
      break;
    default:
      throw new ReteException("Jesp::ParseFuncall",
                              "Bad Functor at line " + st.lineno(),
                              "\"" + (char) st.ttype + "\"");;
    }
    advance();
    
    f = new Funcall(name, engine);
    while (st.ttype != ')') {
      switch (st.ttype) {
      case st.TT_WORD:
        f.AddArgument(st.sval, RU.ATOM); break;
      case st.TT_NUMBER:
        f.AddArgument(st.nval, RU.FLOAT); break;
      case '"':
        f.AddArgument(st.sval, RU.STRING); break;
      case '?':
        advance();
        f.AddArgument(st.sval, RU.VARIABLE); break;
      case '(':
        if (name.equals("assert")) {
          Fact fact = ParseFact();
          f.AddArgument(fact);
          // put the closing paren back.
          st.pushBack();
          break;
        } else if (name.equals("modify")) {
          ValueVector pair = ParseValuePair();
          f.AddArgument(pair, RU.LIST);
          break;
        } else {
          //make functor current
          advance();
          Funcall funcall = ParseFuncall();
          f.AddArgument(funcall);
          break;
        }

      default:
        throw new ReteException("Jesp::ParseFuncall",
                                "Bad Argument at line " + st.lineno(),
                                "\"" + (char) st.ttype + "\"");

      }
      advance();
    }
    if (st.ttype != ')')
        throw new ReteException("Jesp::ParseFuncall",
                                "Expected ')' at line " + st.lineno(),
                                "\"" + (char) st.ttype + "\"");

    // leave the closing paren current

    return f;
  }

  public ValueVector ParseValuePair() throws ReteException {
    ValueVector pair = new ValueVector(2);
    advance();
    pair.add(new Value(st.sval, RU.ATOM));
    advance();
    switch (st.ttype) {
    case st.TT_WORD:
      pair.add(new Value(st.sval, RU.ATOM)); break;
    case st.TT_NUMBER:
      pair.add(new Value(st.nval, RU.FLOAT)); break;
    case '"':
      pair.add(new Value(st.sval, RU.STRING)); break;
    case '?':
      advance();
      pair.add(new Value(st.sval, RU.VARIABLE)); break;
    case '(':
      advance();
      Funcall fc = ParseFuncall();
      pair.add(new Value(fc.vvec, RU.FUNCALL)); break;
    default:
      throw new ReteException("Jesp::ParseValuePair",
                              "Bad argument at line " + st.lineno(),
                              "");
    }
    advance();
    return pair;
  }
  
  /**
    Parse a deftemplate sexp.
    The word 'deftemplate' should be the current token on entry
    */

  private Value ParseDeftemplate() throws ReteException {
    Deftemplate dt;
    String name;
    Value default_value = null;

    if (!st.sval.equals("deftemplate"))
      throw new ReteException("Jesp::deftemplate",
                              "Expected 'deftemplate' at line " + st.lineno(),
                              "" + st.ttype);

    advance();

    // name of deftemplate: required
    if (st.ttype == st.TT_WORD) {
      name = st.sval;
      dt = new Deftemplate(st.sval, RU.UNORDERED_FACT);
      advance();
    } else
      throw new ReteException("Jesp::deftemplate",
                              "Expected an atom at line " + st.lineno(),
                              "" + st.ttype);


    // Deftemplate docstrings are not required,
    // just encouraged...
    if (st.ttype == '"') {
      dt.docstring = st.sval;
      advance();
    } 

    // Now we're looking for slot descriptors.
    while (st.ttype == '(') {
      advance();
      if (st.ttype == st.TT_WORD)
        if (st.sval.equals("slot")) {
          advance();
          if (st.ttype == st.TT_WORD) {
            default_value = new Value(RU.NONE, RU.NONE);
            name = st.sval;
            advance();
            // parse slot descriptor fields
            while (st.ttype == '(') {
              advance();
              if (st.ttype != st.TT_WORD)
                throw new ReteException("Jesp::ParseDeftemplate",
                                        "Syntax error at slot qualifier at line "
                                        + st.lineno(),
                                        String.valueOf( (char) st.ttype));
              if (st.sval.equals("default")) {
                advance();
                switch (st.ttype) {
                case st.TT_WORD:
                  default_value = new Value(RU.putAtom(st.sval), RU.ATOM); break;
                case st.TT_NUMBER:
                  default_value = new Value(st.nval, RU.FLOAT); break;
                case '"':
                  default_value = new Value(st.sval, RU.STRING); break;
                default:
                  throw new ReteException("Jesp::ParseDeftemplate",
                                          "Syntax error at slot qualifier at line "
                                          + st.lineno(),
                                          String.valueOf( (char) st.ttype));
                  
                }
                advance();
                advance();
              } else {
                throw new ReteException("Jesp::ParseDeftemplate",
                                        "Unimplemented slot qualifier at line "
                                        + st.lineno(),
                                        "\"" + st.sval + "\"");

              }
            }
            if (st.ttype == ')')
              advance();
            else
              throw new ReteException("Jesp::ParseDeftemplate",
                                      "Expected ')' at line " + st.lineno(),
                                      "\"" + st.ttype + "\"");
            
          } else
            throw new ReteException("Jesp::ParseDeftemplate",
                                    "Bad slot name at line " + st.lineno(),
                                    "\"" + st.ttype + "\"");
        } else
          throw new ReteException("Jesp::ParseDeftemplate",
                                  "Expected 'slot' at line " + st.lineno(),
                                  "\"" + st.sval + "\"");
          

      dt.AddSlot(name, default_value);
      
    }

    // Leave final paren as current token

    if (st.ttype != ')')
      throw new ReteException("Jesp::ParseDeftemplate",
                              "Expected ')' at line " + st.lineno(),
                              "\"" + st.ttype + "\"");


    engine.AddDeftemplate(dt);
    return Funcall.TRUE();
  }

}




