// -*- java -*-
//////////////////////////////////////////////////////////////////////
// 
// JessTokenStream.java
//  A packet of info  about a token in the input stream.
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// $Id: JessToken.java,v 1.3 1997/05/16 19:27:55 ejfried Exp $
///////////////////////////////////////////////////////////////////////**

package spec.benchmarks._202_jess.jess;

import java.io.*;

final class JessToken {
  String sval;
  double nval;
  int lineno;
  int ttype;

  static final JessToken create(StreamTokenizer st) {

    try {
      st.nextToken();
    } catch (IOException ioe) {
      return null;
    }

    JessToken tok = new JessToken();

    switch (st.ttype) {
    case -3: tok.ttype   = RU.ATOM;    break; // case TT_WORD
    case -2: tok.ttype = RU.FLOAT;   break;   // case TT_NUMBER
    case '"': tok.ttype          = RU.STRING;  break;
    default:  tok.ttype          = st.ttype;   break;
    }

    tok.sval = st.sval;
    tok.nval = st.nval;
    tok.lineno = st.lineno();

    // Change the ttype for a few special types
    if (tok.ttype == RU.ATOM)
      if (tok.sval.charAt(0) == '?') {
        tok.ttype = RU.VARIABLE;
        if (tok.sval.length() > 1)
          tok.sval = tok.sval.substring(1);
        else
          tok.sval = RU.getAtom(RU.gensym("__var"));
          

      } else if (tok.sval.charAt(0) == '$' && tok.sval.charAt(1) == '?') {
        tok.ttype = RU.MULTIVARIABLE;
        if (tok.sval.length() > 2)
          tok.sval = tok.sval.substring(2);
        else
          tok.sval = RU.getAtom(RU.gensym("__mvar"));

      } else if (tok.sval.equals("=")) {
        tok.ttype = '=';
      }

    
    return tok;
  }

  public String toString() {
    if (ttype == RU.VARIABLE)
      return "?" + sval;
    else if (ttype == RU.MULTIVARIABLE)
      return "$?" + sval;
    else if (ttype == RU.STRING)
      return "\"" + sval + "\"";
    else if (sval != null)
      return sval;
    else if (ttype == RU.FLOAT)
      return "" + nval;
    else return "" +  (char) ttype;
  }

}
  

