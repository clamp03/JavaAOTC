// -*- java -*-
//////////////////////////////////////////////////////////////////////
// 
// JessTokenStream.java
// A smart lexer for the Jess subset of CLIPS
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.io.*;
import java.util.*;

class JessTokenStream {
  private Stack m_stack;
  private StreamTokenizer m_stream;
  private int m_lineno = 1;
  private StringBuffer m_string = new StringBuffer();

  /**
    Construct a JessTokenStream.
    Tell the tokenizer how to separate Jess tokens
    */

  JessTokenStream(InputStream is) {
    m_stream = PrepareStream(is);
    m_stack = new Stack();
  }

  StreamTokenizer PrepareStream(InputStream is) {
    Reader reader = new BufferedReader(new InputStreamReader(is));
    StreamTokenizer stream = new StreamTokenizer(reader);
    
    stream.commentChar(';');
    stream.wordChars('$','$');
    stream.wordChars('*','*');
    stream.wordChars('=','=');
    stream.wordChars('+','+');
    stream.wordChars('/','/');
    stream.wordChars('<','<');
    stream.wordChars('>','>');
    stream.wordChars('_','_');
    stream.wordChars('?','?');
    // Pound sign added at the request of Karl.R.Mueller@gsfc.nasa.gov
    stream.wordChars('#','#');    
    return stream;
  }

  /**
    Return the current line number, corresponding to the most recently
    popped or pushed token.
    */
  
  int lineno() {
    return m_lineno;
  }

  /**
    Are there any more tokens in this sexp?
    */
  
  boolean moreTokens() {
    return m_stack.empty();
  }

  /**
    Are there any more tokens in this sexp?
    */
  
  boolean eof() {
    if (m_stack.empty())
      if (!PrepareSexp()) {
        return true;
      }
    return false;
  }

  /**
    Load a full sexp into the stack.
    */

  boolean PrepareSexp() {
    int level = 1;
    m_string.setLength(0);
    Stack temp_stack = new Stack();
    JessToken tok = JessToken.create(m_stream);
    temp_stack.push(tok);
    if (tok.ttype != '(') 
      return false;

    while (level > 0) {
      tok = JessToken.create(m_stream);
      temp_stack.push(tok);
      if (tok.ttype == m_stream.TT_EOF)
        return false; 
      else if (tok.ttype == ')')
        --level;
      else if (tok.ttype == '(')
        ++level;
    }

    while (!temp_stack.empty()) {
      m_stack.push(temp_stack.pop());
    }

    return true;
  }

  /*
    Reload the last sexp onto the stack
   */

  boolean RedoSexp() {
    StreamTokenizer sbis
      = PrepareStream(new StringBufferInputStream(m_string.toString()));
    int level = 1;
    m_string.setLength(0);
    Stack temp_stack = new Stack();
    JessToken tok = JessToken.create(sbis);
    temp_stack.push(tok);
    if (tok.ttype != '(') 
      return false;

    while (level > 0) {
      tok = JessToken.create(sbis);
      temp_stack.push(tok);
      if (tok.ttype == sbis.TT_EOF)
        return false; 
      else if (tok.ttype == ')')
        --level;
      else if (tok.ttype == '(')
        ++level;
    }

    while (!temp_stack.empty()) {
      m_stack.push(temp_stack.pop());
    }

    return true;

  }

  /**
    Return the next token in the stream, or NULL if empty.
    */

  JessToken nextToken() {
    if (m_stack.empty())
      if (!PrepareSexp()) {
        return null;
      }
    JessToken tok = (JessToken) m_stack.pop();
    m_string.append(tok.toString() + " ");
    m_lineno = tok.lineno;
    return tok;
  }

  /**
    Infinite pushback
    */

  void push_back(JessToken tok) {
    m_lineno = tok.lineno;
    m_stack.push(tok);
    m_string.setLength(m_string.length() - (tok.toString().length() + 1));
  }

  /**
    Return the 'car' of a sexp as a String, or null.
    */

  String head() {
    if (m_stack.empty())
      if (!PrepareSexp())
        return null;

    JessToken top = (JessToken) m_stack.pop();
    JessToken tok = (JessToken) m_stack.peek();

    m_stack.push(top);

    if (tok.ttype != RU.ATOM)
      {
        if (tok.ttype == '-')
          return "-";
        else if (tok.ttype == '=')
          return "=";
        else
          {
            clear();
            return null;
          }
      }
    else
      return tok.sval;       
  }

  void clear() {
    m_stack = new Stack();
    m_string.setLength(0);
  }
  // Print the sexp on the stack
  public String toString() {
    return m_string.toString();
  }
  
  public static void main(String[] argv) {
    JessTokenStream jts = new JessTokenStream(System.in);
    JessToken jt;
    jts.PrepareSexp();
    do {
      jt = jts.nextToken();
      spec.harness.Context.out.println(jt);
    }
    while (jt.ttype != ')');
    spec.harness.Context.out.println("----------------------");
    jts.RedoSexp();
    while ((jt = jts.nextToken()) != null)
      spec.harness.Context.out.println(jt);
  }    
}












