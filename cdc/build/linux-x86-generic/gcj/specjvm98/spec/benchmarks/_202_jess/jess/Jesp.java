// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Jesp.java
// Parser functions for Java Expert System
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Laboratories
// $Id: Jesp.java,v 1.8 1997/12/02 01:14:32 ejfried Exp $
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.io.*;

/**
  Java Expert System Parser

  A top-down parser for the CLIPS language.

@author E.J. Friedman-Hill (C)1996
*/

public class Jesp {
  /**
    Stream where input comes from
   */
  private JessTokenStream jts;
  private Rete engine;
  /**
    Constructor
    */

  public Jesp(InputStream is, Rete e) {
    jts = new JessTokenStream(is);
    engine = e;
  }  


  /**
    Parses an input file. Returns true on success.
    Argument is true if a prompt should be printed, false
    for no prompt.
    */

  public Value parse(boolean prompt) throws ReteException {
    Value val = Funcall.TRUE();

    if (prompt)
      {
        engine.display().stdout().print("Jess> ");
        engine.display().stdout().flush();
      }
    while (!jts.eof() && val != null)
      {
        val = ParseSexp();
        if (prompt)
          {
            engine.display().stderr().println(val);
            engine.display().stdout().print("Jess> ");
            engine.display().stdout().flush();
          }
      }

    return val;
  }

  /**
    Parses an input file containing only facts, asserts each one.
    */

  Value LoadFacts() throws ReteException {
    Value val = Funcall.TRUE();

    while (!jts.eof())
      {
        Fact f = ParseFact();
        if (f == null)
          break;
        engine.Assert(f.FactData());
      }

    return val;
  }

  /** **********************************************************************
   * ParseSexp
   *
   * Syntax:
   *  ( -Something- )
   *
   ********************************************************************** */

  private Value ParseSexp() throws ReteException {
    String head = jts.head();

    if (head == null)
      return Funcall.NIL();

    else if (head.equals("defrule"))
      return ParseDefrule();

    else if (head.equals("deffacts"))
      return ParseDeffacts();

    else if (head.equals("deftemplate"))
      return ParseDeftemplate();

    else if (head.equals("deffunction"))
      return ParseDeffunction();

    else if (head.equals("defglobal"))
      return ParseDefglobal();

    else {
      Funcall fc = ParseFuncall();
      engine.global_context().Push();
      ValueVector vv = engine.global_context().ExpandAction(fc);
      Value v = Funcall.Execute(vv, engine.global_context());
      engine.global_context().Pop();
      return v;
    }

  }

  /** **********************************************************************
   * ParseDefglobal
   *
   * Syntax:
   *   (defglobal ?x = 3 ?y = 4 ... )
   *
   *********************************************************************** */

  private Value ParseDefglobal() throws ReteException {
    Defglobal dg = new Defglobal();

    /* ****************************************
       '(defglobal'
       **************************************** */

    if (  (jts.nextToken().ttype != '(') ||
        ! (jts.nextToken().sval.equals("defglobal")) )
      parse_error("ParseDefglobal", "Expected (defglobal...");


    /* ****************************************
       varname = value sets
       **************************************** */

    JessToken name, value;
    while ((name = jts.nextToken()).ttype != ')') {

      if (name.ttype != RU.VARIABLE)
        parse_error("ParseDefglobal", "Expected a variable name");
      
      if (jts.nextToken().ttype != '=')
        parse_error("ParseDefglobal", "Expected =");

      value = jts.nextToken();

      switch (value.ttype) {

      case RU.ATOM: case RU.STRING: case RU.VARIABLE:
        dg.AddGlobal(name.sval, new Value(value.sval, value.ttype)); break;

      case RU.FLOAT:
        dg.AddGlobal(name.sval, new Value(value.nval, value.ttype)); break;

      case '(': {
        jts.push_back(value);
        Funcall fc = ParseFuncall();
        engine.global_context().Push();
        ValueVector vv = engine.global_context().ExpandAction(fc);
        Value v = Funcall.Execute(vv, engine.global_context());
        engine.global_context().Pop();
        dg.AddGlobal(name.sval, v); break;
      }
      default:
        parse_error("ParseDefglobal", "Bad value");

      }
    }

    engine.AddDefglobal(dg);
    return Funcall.TRUE();

  }

  /** **********************************************************************
   * ParseFuncall
   *
   * Syntax:
   *   (functor field2 (nested funcall) (double (nested funcall)))
   *
   ********************************************************************** */

  private Funcall ParseFuncall() throws ReteException {
    JessToken tok;
    String name = null;
    Funcall fc;
    
    if (jts.nextToken().ttype != '(')
      parse_error("ParseFuncall", "Expected '('");

    /* ****************************************
       functor
       **************************************** */
    tok = jts.nextToken();
    switch (tok.ttype) {

    case RU.ATOM:
      name = tok.sval;
      break;

    case '-': case '/': case '=':
      // special functors
      name = "" + (char) tok.ttype;
      break;

    default:
      parse_error("ParseFuncall", "Bad functor");
    }
    fc = new Funcall(name, engine);

    /* ****************************************
       arguments
       **************************************** */
    tok = jts.nextToken();
    while (tok.ttype != ')') {

      switch (tok.ttype) {

      // simple arguments
      case RU.ATOM: case RU.STRING: case RU.VARIABLE: case RU.MULTIVARIABLE:
        fc.AddArgument(tok.sval, tok.ttype); break;

      case RU.FLOAT:
        fc.AddArgument(tok.nval, tok.ttype); break;

      // nested funcalls
      case '(':
        jts.push_back(tok);
        if (name.equals("assert")) {
          Fact fact = ParseFact();
          fc.AddArgument(fact);
          break;
          
        } else if (name.equals("modify")) {
          ValueVector pair = ParseValuePair();
          fc.AddArgument(pair, RU.LIST);
          break;

        } else {
          Funcall fc2 = ParseFuncall();
          fc.AddArgument(fc2);
          break;
        }

      default:
        parse_error("ParseFuncall", "Bad Argument");

      } // switch tok.ttype
      tok = jts.nextToken();
    } // while tok.ttype != ')'

    return fc;
  }

  /** **********************************************************************
   * ParseValuePair
   * These are used in (modify) funcalls
   *
   * Syntax:
   *   (ATOM VALUE)
   *
   ********************************************************************** */

  private ValueVector ParseValuePair() throws ReteException {
    ValueVector pair = new ValueVector(2);
    JessToken tok = null;

    /* ****************************************
       '(atom'
       **************************************** */

    if (jts.nextToken().ttype != '(' ||
        (tok = jts.nextToken()).ttype != RU.ATOM) {
      parse_error("ParseValuePair", "Expected '( <atom>'");
    }

    pair.add(new Value(tok.sval, RU.ATOM));

    /* ****************************************
       value
       **************************************** */

    switch ((tok = jts.nextToken()).ttype) {

    case RU.ATOM: case RU.STRING: case RU.VARIABLE: case RU.MULTIVARIABLE:
      pair.add(new Value(tok.sval, tok.ttype)); break;

    case RU.FLOAT:
      pair.add(new Value(tok.nval, tok.ttype)); break;

    case '(':
      jts.push_back(tok);
      Funcall fc = ParseFuncall();
      pair.add(new Value(fc, RU.FUNCALL)); break;

    default:
      parse_error("ParseValuePair", "Bad argument");
    }
    if (jts.nextToken().ttype != ')')
      parse_error("ParseValuePair", "Expected ')'");

    return pair;
  }
  

  /** **********************************************************************
   * ParseDeffacts
   *
   * Syntax:
   *  (deffacts <name> ["comment"] (fact) [(fact)...])
   *
   * ********************************************************************** */

  private Value ParseDeffacts() throws ReteException {
    Deffacts df = null;
    JessToken tok = null;

    /* ****************************************
       '(deffacts'
       **************************************** */

    if (jts.nextToken().ttype != '(' ||
        (tok = jts.nextToken()).ttype != RU.ATOM ||
        !tok.sval.equals("deffacts")) {
      parse_error("ParseDeffacts", "Expected '( deffacts'");
    }

    /* ****************************************
       deffacts name
       **************************************** */

    if ((tok = jts.nextToken()).ttype != RU.ATOM)
      parse_error("ParseDeffacts", "Expected deffacts name");
    df = new Deffacts(tok.sval);
  
    tok = jts.nextToken();

    /* ****************************************
       optional comment
       **************************************** */

    if (tok.ttype == RU.STRING) {
      df.docstring = tok.sval;
      tok = jts.nextToken();
    } 

    /* ****************************************
       list of facts
       **************************************** */

    while (tok.ttype == '(') {
      jts.push_back(tok);
      Fact f = ParseFact();
      df.AddFact(f);
      tok = jts.nextToken();
    }

    /* ****************************************
       closing paren
       **************************************** */

    if (tok.ttype != ')')
      parse_error("ParseDeffacts", "Expected ')'");

    engine.AddDeffacts(df);
    return Funcall.TRUE();

  }

  /** **********************************************************************
   * ParseFact
   * 
   * This is called from the parse routine for Deffacts and from the
   * Funcall parser for 'assert'; because of this latter, it can have
   * variables that need expanding.
   *
   * Syntax:
   *   ordered facts: (atom field1 2 "field3")
   *   unordered facts: (atom (slotname value) (slotname value2))
   *
   *********************************************************************** */

  Fact ParseFact() throws ReteException {
    String name, slot;
    int slot_type;
    Fact f;
    JessToken tok = null;

    /* ****************************************
       '( atom'
       **************************************** */

    if (jts.nextToken().ttype != '(' ||
        (tok = jts.nextToken()).ttype != RU.ATOM)
      parse_error("ParseFact", "Expected '( <atom>'");
    
    name = tok.sval;
  
    /* ****************************************
       slot data
       What we do next depends on whether we're parsing
       an ordered or unordered fact. We can determine this very easily:
       If there is an existing unordered deftemplate, this is an
       unordered fact. Otherwise, it is ordered!
       **************************************** */

    // get a deftemplate if one already exists.

    ValueVector deft = engine.FindDeftemplate(name);
    int ordered;
    if (deft == null)
      ordered = RU.ORDERED_FACT;
    else
      ordered = deft.get(RU.DESC).DescriptorValue();

    if (ordered == RU.UNORDERED_FACT) {

      /* ****************************************
         SLOT DATA FOR UNORDERED FACT
         **************************************** */
      f = new Fact(name, RU.UNORDERED_FACT, engine);
      tok = jts.nextToken();

      while (tok.ttype != ')') {

        // Opening parenthesis
        if (tok.ttype != '(')
          parse_error("ParseFact", "Expected '('");

        // Slot name
        if  ((tok = jts.nextToken()).ttype != RU.ATOM)
          parse_error("ParseFact", "Bad slot name");
        slot = tok.sval;
        
        // Is this a slot or a multislot?
        slot_type = Deftemplate.SlotType(deft, slot);

        switch (slot_type) {

        // Data in normal slot
        case RU.SLOT:
          switch ((tok = jts.nextToken()).ttype) {
            
          case RU.ATOM: case RU.STRING:
          case RU.VARIABLE: case RU.MULTIVARIABLE:
            f.AddValue(slot, tok.sval, tok.ttype); break;
            
          case RU.FLOAT:
            f.AddValue(slot, tok.nval, tok.ttype); break;
            
          case '(': {
            jts.push_back(tok);
            Funcall fc = ParseFuncall();
            f.AddValue(slot, fc, RU.FUNCALL); break;
          }
          default:
            parse_error("ParseFact", "Bad slot value");
          }
          
          if  ((tok = jts.nextToken()).ttype != ')')
            parse_error("ParseFact", "Expected ')'");
          break;

        case RU.MULTISLOT:
        // Data in multislot
        // Code is very similar, but bits of data are added to a multifield
          ValueVector slot_vv = new ValueVector();
          tok = jts.nextToken();
          while (tok.ttype != ')') {
            switch (tok.ttype) {
              
            case RU.ATOM: case RU.STRING:
            case RU.VARIABLE: case RU.MULTIVARIABLE:
              slot_vv.add(new Value(tok.sval, tok.ttype)); break;
              
            case RU.FLOAT:
              slot_vv.add(new Value(tok.nval, tok.ttype)); break;
              
            case '(': {
              jts.push_back(tok);
              Funcall fc = ParseFuncall();
              slot_vv.add(new Value(fc, RU.FUNCALL)); break;
            }
            default:
              parse_error("ParseFact", "Bad slot value");
            }
            
            tok = jts.nextToken();
            
          }
          f.AddValue(slot, new Value(slot_vv, RU.LIST));          
          break;

        default:
          parse_error("ParseFact", "No such slot in deftemplate");
        }          

        // hopefully advance to next ')'
        tok = jts.nextToken();
        
      }
    } else {

      /* ****************************************
         PARSE SLOT DATA FOR ORDERED FACT
         **************************************** */

      f = new Fact(name, RU.ORDERED_FACT, engine);
      tok = jts.nextToken();

      while (tok.ttype != ')') {

        switch (tok.ttype) {

        case RU.ATOM: case RU.STRING:
        case RU.VARIABLE: case RU.MULTIVARIABLE:
            f.AddValue(tok.sval, tok.ttype); break;

        case RU.FLOAT:
          f.AddValue(tok.nval, tok.ttype); break;

        case '(': {
          jts.push_back(tok);
          Funcall fc = ParseFuncall();
          f.AddValue(fc, RU.FUNCALL); break;
        }
        default:
          parse_error("ParseFact","Bad slot value");
        }
        tok = jts.nextToken();
      }
    }
    if (tok.ttype != ')')
      parse_error("ParseFact", "Expected ')'");

    return f;
    
  }

  /** **********************************************************************
   * ParseDeftemplate
   * 
   *
   * Syntax:
   *   (deftemplate (slot foo (default <value>)) (multislot bar))
   *
   *********************************************************************** */

  private Value ParseDeftemplate() throws ReteException {
    Deftemplate dt;
    String name;
    int slot_type = RU.SLOT;
    Value default_value = null;
    JessToken tok;

    /* ****************************************
       '(deftemplate'
       **************************************** */

    if (  (jts.nextToken().ttype != '(') ||
        ! (jts.nextToken().sval.equals("deftemplate")) )
      parse_error("ParseDeftemplate", "Expected (deftemplate...");

    /* ****************************************
       deftemplate name, optional comment
       **************************************** */

    if ((tok = jts.nextToken()).ttype != RU.ATOM)
      parse_error("ParseDeftemplate", "Expected deftemplate name");

    name = tok.sval;
    dt = new Deftemplate(tok.sval, RU.UNORDERED_FACT);

    if ((tok = jts.nextToken()).ttype == RU.STRING) {
      dt.docstring = tok.sval;
      tok = jts.nextToken();
    }

    /* ****************************************
       individual slot descriptions
       **************************************** */
    
    // ( <slot type>

    while (tok.ttype == '(') { // slot 
      if ((tok = jts.nextToken()).ttype != RU.ATOM ||
          !(tok.sval.equals("slot") || tok.sval.equals("multislot")))
        parse_error("ParseDeftemplate", "Bad slot type");

      //if (tok.sval.equals("multislot"))
      //parse_error("ParseDeftemplate", "No multislot support yet!");
        
      slot_type = tok.sval.equals("slot") ? RU.SLOT : RU.MULTISLOT;
      
      // <slot name>
      if ((tok = jts.nextToken()).ttype != RU.ATOM)
        parse_error("ParseDeftemplate", "Bad slot name");
      name = tok.sval;      
      
      // optional slot qualifiers
      
      default_value = new Value(RU.NONE, RU.NONE);
      
      tok = jts.nextToken();
      while (tok.ttype == '(') { // slot qualifier
        if ((tok = jts.nextToken()).ttype != RU.ATOM)
          parse_error("ParseDeftemplate", "Slot qualifier must be atom");
        
        // default value qualifier
        
      if (tok.sval.equalsIgnoreCase("default")) {
        tok = jts.nextToken();
        switch (tok.ttype) {

        case RU.ATOM: case RU.STRING:
          default_value = new Value(tok.sval, tok.ttype); break;

        case RU.FLOAT:
          default_value = new Value(tok.nval, tok.ttype); break;

        default:
          parse_error("ParseDeftemplate", "Illegal default slot value");
        }
      } else if (tok.sval.equalsIgnoreCase("type")) {
        // type is allowed, but ignored
        tok = jts.nextToken();
      } else
        parse_error("ParseDeftemplate", "Unimplemented slot qualifier");
      
      if ((tok = jts.nextToken()).ttype != ')')
        parse_error("ParseDeftemplate", "Expected ')'");
      
      tok = jts.nextToken();
      }
      if (tok.ttype != ')')
        parse_error("ParseDeftemplate", "Expected ')'");
      
      if (slot_type == RU.SLOT)
        dt.AddSlot(name, default_value);
      else
        dt.AddMultiSlot(name, new Value(new ValueVector(), RU.LIST));
      
      tok = jts.nextToken();
    }
    if (tok.ttype != ')')
      parse_error("ParseDeftemplate", "Expected ')'");

    engine.AddDeftemplate(dt);
    return Funcall.TRUE();
  }
  

  /** **********************************************************************
   * ParseDefrule
   * Wrapper around DoParseDefrule
   * We're going to split defrules into multiple rules is we see an (or) CE
   *********************************************************************** */
  private Value ParseDefrule() throws ReteException {
    Value v;
    v = DoParseDefrule();
    return v;
  }

  /** **********************************************************************
   * DoParseDefrule
   * 
   *
   * Syntax:
   * (defrule name
   *  [ "docstring...." ]
   *  [ (declare (salience 1)) ]
   *   (pattern 1)
   *   ?foo <- (pattern 2)
   *   (pattern 3)
   *  =>
   *   (action 1)
   *   (action ?foo)
   *   )
   *
   *********************************************************************** */

  private Value DoParseDefrule() throws ReteException {
    Defrule dr;
    JessToken tok, tok2, tok3, tok4, tok5;

    /* ****************************************
       '(defrule'
       **************************************** */

    if (  (jts.nextToken().ttype != '(') ||
        ! (jts.nextToken().sval.equals("defrule")) )
      parse_error("ParseDefrule", "Expected (defrule...");


    /* ****************************************
       defrule name, optional comment
       **************************************** */

    if ((tok = jts.nextToken()).ttype != RU.ATOM)
      parse_error("ParseDefrule", "Expected defrule name");
    dr = new Defrule(tok.sval, engine);

    if ((tok = jts.nextToken()).ttype == RU.STRING) {
      dr.docstring = tok.sval;
      tok = jts.nextToken();
    }

    int factIndex = 0;

    // check for salience declaration
    if (tok.ttype == '(')
      if ((tok2 = jts.nextToken()).ttype == RU.ATOM &&
          tok2.sval.equals("declare")) {

        if ((tok2 = jts.nextToken()).ttype != '(' ||
            (tok2 = jts.nextToken()).ttype != RU.ATOM ||
            !tok2.sval.equals("salience")) {
          parse_error("ParseDefrule", "Expected (salience ...");
        }
        if ((tok2 = jts.nextToken()).ttype != RU.FLOAT)
          parse_error("ParseDefrule", "Expected <integer>");
            
        dr.salience = (int) tok2.nval;
        if (jts.nextToken().ttype != ')' ||
            jts.nextToken().ttype != ')')
          parse_error("ParseDefrule", "Expected '))('");

        tok = jts.nextToken();

      } else { // head wasn't 'declare'
        jts.push_back(tok2);
      }

    // now we're looking for just patterns

    while (tok.ttype == '(' || tok.ttype == RU.VARIABLE) {
      switch (tok.ttype) {

      case '(': {
        // pattern not bound to a var
        jts.push_back(tok);
        Pattern p = ParsePattern();
        if (factIndex == 0 && p.negated())
          if (p.hasVariables)
            parse_error("ParseDefrule", "First pattern in rule is a not" +
                        " CE containing variables");
          else
            {
              dr.AddPattern(new Pattern("initial-fact",
                                        RU.ORDERED_FACT, engine));
              ++factIndex;
            }
        dr.AddPattern(p);
        ++factIndex;
        break;
      }

      case RU.VARIABLE: {
        // pattern bound to a variable
        // These look like this:
        // ?name <- (pattern 1 2 3)

        dr.AddBinding(RU.putAtom(tok.sval), factIndex, RU.PATTERN, -1);

        if ((tok = jts.nextToken()).ttype != RU.ATOM || !tok.sval.equals("<-"))
          parse_error("ParseDefrule", "Expected '<-'");

        Pattern p = ParsePattern();
        if (p.negated())
          parse_error("ParseDefrule",
                      "'not' and 'test' CE's cannot be bound to variables");

        dr.AddPattern(p);
        ++factIndex;
        break;
       }
      } 
      tok = jts.nextToken();
    }

    if (factIndex == 0) {
      // No patterns for this rule; we will fire on "initial-fact".
      Pattern p = new Pattern("initial-fact", RU.ORDERED_FACT, engine);
      dr.AddPattern(p);
    }

    if (tok.ttype != RU.ATOM || !tok.sval.equals("=>"))
          parse_error("ParseDefrule", "Expected '=>'");
    
    tok = jts.nextToken();

    while (tok.ttype == '(') {
      jts.push_back(tok);
      Funcall f = ParseFuncall();
      dr.AddAction(f);
      tok = jts.nextToken();
    }

    if (tok.ttype != ')')
      parse_error("ParseDefrule", "Expected ')'");

    engine.AddDefrule(dr);
    return Funcall.TRUE();
  }

  
  /** **********************************************************************
   * ParsePattern
   * 
   * Parse a Pattern object in a Rule LHS context
   *
   * Syntax:
   * Like that of a fact, except that values can have complex forms like
   * 
   * ~value       (test for not a value)
   * ?X&~red      (store the value in X; fail match if not red)
   * ?X&:(> ?X 3) (store the value in X; fail match if not greater than 3)
   *
   *********************************************************************** */

  private Pattern ParsePattern() throws ReteException {
    String name, slot;
    Pattern p;
    JessToken tok = null;

    /* ****************************************
      ' ( <atom> '
      **************************************** */

    if (  (jts.nextToken().ttype != '(') ||
        ! ((tok = jts.nextToken()).ttype == RU.ATOM))
      parse_error("ParsePattern", "Expected '( <atom>'");

    name = tok.sval;

    /* ****************************************
      Special handling for NOT CEs
      **************************************** */

    if (name.equals("not")) {
      // this is a negated pattern; strip off the (not ) and 
      // recursively parse the actual pattern.

      p = ParsePattern();
      p.setNegated();
      if (jts.nextToken().ttype != ')')
        parse_error("ParsePattern", "Expected ')'");
      return p;
    } 

    /* ****************************************
       What we do next depends on whether we're parsing
       an ordered or unordered fact. We can determine this very
       easily: If there is a deftemplate, this is an unordered
       fact. Otherwise, it is ordered!
       **************************************** */

    ValueVector deft = engine.FindDeftemplate(name);

    int ordered = (deft == null) ?
      RU.ORDERED_FACT : deft.get(RU.DESC).DescriptorValue();

    if (ordered == RU.UNORDERED_FACT) {

    /* ****************************************
       Actual Pattern slot data for unordered facts
       **************************************** */
      p = new Pattern(name, RU.UNORDERED_FACT, engine);
      tok = jts.nextToken();
      while (tok.ttype == '(') {
        if ((tok = jts.nextToken()).ttype != RU.ATOM)
          parse_error("ParsePattern", "Bad slot name");

        slot = tok.sval;
        boolean multislot = (Deftemplate.SlotType(deft, slot) == RU.MULTISLOT);

        tok = jts.nextToken();
        int subidx = (multislot ? 0 : -1);
        while (tok.ttype != ')') {

          // if this is a '~'  pattern, keep track
          boolean not_slot = false;
          if (tok.ttype == '~') {
            not_slot = true;
            tok = jts.nextToken();
          }
          
          switch (tok.ttype) {
          case RU.VARIABLE: case RU.MULTIVARIABLE:
            p.hasVariables = true;
            // FALL THROUGH
          case RU.ATOM: case RU.STRING:
              p.AddTest(slot, new Value(tok.sval, tok.ttype), subidx, not_slot);
          break;

          case RU.FLOAT:
            p.AddTest(slot, new Value(tok.nval, tok.ttype), subidx, not_slot);
          break;

          case ':':
            Funcall f = ParseFuncall();
            p.AddTest(slot, new Value(f, RU.FUNCALL), subidx, not_slot); break;

          default:
            parse_error("ParsePattern", "Bad slot value");
          }
          
          tok = jts.nextToken();
          
          if (tok.ttype == '&')
            tok = jts.nextToken();
          else
            if (!multislot && tok.ttype != ')')
              parse_error("ParsePattern", slot + " is not a multislot");
            else
              ++subidx;
        }

        if (multislot)
          p.SetMultislotLength(slot, subidx);

        tok = jts.nextToken();
          
      }
      return p;

    } else {

    /* ****************************************
       Actual Pattern slot data for ordered facts
       **************************************** */

      p = new Pattern(name, RU.ORDERED_FACT, engine);

      // We definitely accept a superset of correct syntax here.
      // multifields and single fields can be mixed, and things like
      // ?a&?b&?c are parsed.


      tok = jts.nextToken();
      while (tok.ttype != ')') {

        do { // loop while the next token is a '&'
          boolean not_slot = false;
          
          if (tok.ttype == '&') {
            tok = jts.nextToken();
          }
          
          if (tok.ttype == '~') {
            not_slot = true;
            tok = jts.nextToken();
          }
          
          switch (tok.ttype) {

          case RU.VARIABLE: case RU.MULTIVARIABLE:
            p.hasVariables = true;
            // FALL THROUGH
          case RU.ATOM: case RU.STRING:
            p.AddTest(new Value(tok.sval, tok.ttype), not_slot); break;

          case RU.FLOAT:
            p.AddTest(new Value(tok.nval, tok.ttype), not_slot); break;

          case ':':
            Funcall f = ParseFuncall();
            p.AddTest(new Value(f, RU.FUNCALL), not_slot); break;

          default:
            parse_error("ParsePattern", "Badslot value");
          }
          tok = jts.nextToken();
        } while (tok.ttype == '&');
        p.advance();
      }
      return p;
    }
    
  }

  /** **********************************************************************
   * ParseDeffunction
   * 
   * Syntax:
   *   (deffunction name "doc-comment" (<arg1><arg2...)
   *   (action)
   *    value
   *   (action))
   *
   *********************************************************************** */
  
  private Value ParseDeffunction() throws ReteException {
    Deffunction df;
    JessToken tok;

    /* ****************************************
       '(deffunction'
       **************************************** */

    if (  (jts.nextToken().ttype != '(') ||
        ! (jts.nextToken().sval.equals("deffunction")) )
      parse_error("ParseDeffunction", "Expected (deffunction...");


    /* ****************************************
       defrule name
       **************************************** */

    if ((tok = jts.nextToken()).ttype != RU.ATOM)
      parse_error("ParseDeffunction", "Expected deffunction name");
    df = new Deffunction(tok.sval, engine);
    
    /* ****************************************
       Argument list
       **************************************** */

    if ((tok = jts.nextToken()).ttype != '(') 
      parse_error("ParseDeffunction", "Expected '('");
    
    while ((tok = jts.nextToken()).ttype == RU.VARIABLE ||
           tok.ttype == RU.MULTIVARIABLE)
      df.AddArgument(tok.sval);

    if (tok.ttype != ')') 
      parse_error("ParseDeffunction", "Expected ')'");


    /* ****************************************
       optional comment
       **************************************** */

    if ((tok = jts.nextToken()).ttype == RU.STRING) {
      df.docstring = tok.sval;
      tok = jts.nextToken();
    }

    /* ****************************************
       function calls and values
       **************************************** */

    while (tok.ttype != ')') {
      if (tok.ttype == '(') {
        jts.push_back(tok);
        Funcall f = ParseFuncall();
        df.AddAction(f);
      } else {
        switch (tok.ttype) {
          
        case RU.ATOM: case RU.STRING:
        case RU.VARIABLE: case RU.MULTIVARIABLE:
          df.AddValue(new Value(tok.sval, tok.ttype)); break;
          
        case RU.FLOAT:
          df.AddValue(new Value(tok.nval, tok.ttype)); break;
          
        default:
          parse_error("ParseDeffunction", "Unexpected character");
        }
      }
      tok = jts.nextToken();
    }

    engine.AddDeffunction(df);
    return Funcall.TRUE();
  }


  /**
    Make error reporting a little more compact.
    */

  private void parse_error(String routine, String msg) throws ReteException {
    jts.clear();
    throw new ReteException("Jesp::" + routine,
                            msg  + " at line " + jts.lineno(),
                            jts.toString());
    

  }

}




