// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Node2.java
// Two-input nodes of the pattern network
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
// $Id: Node2.java,v 1.5 1997/12/02 01:14:36 ejfried Exp $
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.util.*;

/**
  Two-input nodes of the pattern network
  Test that a slot from the left input and one
  from the right input have the same
  value and type.
    
@author E.J. Friedman-Hill (C)1997
*/

public class Node2 extends Node {
  

  /**
    The left and right token memories
    They are Vectors of Tokens
   */

  protected TokenVector left, right;
  
  /**
    The tests this node performs
   */

  protected Vector _tests;
  protected Object[] tests;

  /**
    Constructor
    */

  Node2(Rete engine) {
    super(engine);
    left = new TokenVector();
    right = new TokenVector();
    _tests = new Vector();
  }

  void complete() {
    // freeze the tests vector
    tests = new Object[_tests.size()];
    for (int i=0; i< _tests.size(); i++)
     tests[i] = _tests.elementAt(i); 
  }



  void addTest(int test, int token_idx, int left_idx, int right_idx) {
    _tests.addElement(new Test2(test, token_idx, left_idx, -1, right_idx, -1));
  }

  void addTest(int test, int token_idx, int left_idx, int left_sub_idx,
                      int right_idx, int right_sub_idx) {
    Test2 t;
    _tests.addElement(t = new Test2(test, token_idx, left_idx, 
                                left_sub_idx, right_idx, right_sub_idx));

  }

  void addTest(int test, int slot_idx, Value v) {
    _tests.addElement(new Test1(test, slot_idx, -1, v));
  }

  void addTest(int test, int slot_idx, int slot_sidx, Value v) {
    _tests.addElement(new Test1(test, slot_idx, slot_sidx, v));
  }

  /**
    For our purposes, two Node2's are equal if every test in
    one has an equivalent test in the other, and if the test vectors are
    the same size. The subclass NodeNot2 should never be shared, so we'll
    report unequal always. This routine is used during network compilation,
    not at runtime.
    */

  boolean equals(Node2 n) {
    if (this instanceof NodeNot2 ||
        n instanceof NodeNot2 ||
        n._tests.size() != _tests.size())
      return false;
  outer_loop:
    for (int i=0; i<_tests.size(); i++) {
      // Test1 nodes hold function call tests. They're too
      // complicated to try to share.
      if (_tests.elementAt(i) instanceof Test1)
        return false;
      Test2 t1 = (Test2) _tests.elementAt(i);
      for (int j=0; j<_tests.size(); j++) {
        if (t1.equals(n._tests.elementAt(j)))
          continue outer_loop;
      }
      return false;
    }
    return true;
  }

  /**
    Do the business of this node.
    The 2-input nodes, on receiving a token, have to do several things,
    and their actions change based on whether it's an ADD or REMOVE,
    and whether it's the right or left input!
    <PRE>

    *********** For ADDs, left input:
    1) Look for this token in the left memory. If it's there, do nothing;
    If it's not, add it to the left memory.

    2) Perform all this node's tests on this token and each of the right-
    memory tokens. For any right token for which they succeed:

    3) a) append the right token to a copy of this token. b) do a
    CallNode on each of the successors using this new token.

    *********** For ADDs, right input:

    1) Look for this token in the right memory. If it's there, do nothing;
    If it's not, add it to the right memory.

    2) Perform all this node's tests on this token and each of the left-
    memory tokens. For any left token for which they succeed:

    3) a) append this  token to a copy of the left token. b) do a
    CallNode on each of the successors using this new token.

    *********** For REMOVEs, left input:
    
    1) Look for this token in the left memory. If it's there, remove it;
    else do nothing.

    2) Perform all this node's tests on this token and each of the right-
    memory tokens. For any right token for which they succeed:
    
    3) a) append the right token to a copy of this token. b) do a
    CallNode on each of the successors using this new token.

    *********** For REMOVEs, right input:
    
    1) Look for this token in the right memory. If it's there, remove it;
    else do nothing.

    2) Perform all this node's tests on this token and each of the left-
    memory tokens. For any left token for which they succeed:
    
    3) a) append this token to a copy of the left token. b) do a
    CallNode on each of the successors using this new token.

    </PRE>
    */
  
  boolean CallNode(Token token, int callType) throws ReteException {
    // the four cases listed above are implemented in order.

    //debugPrint(token, callType);
    Token tmp;
    switch (callType) {

    case Node.LEFT:
      switch (token.tag) {

      case RU.ADD:
      case RU.UPDATE:
        if ((tmp = findInMemory(left, token)) == null)
          left.addElement(token);

        runTestsVaryRight(token);
        break;

      case RU.REMOVE:
        if ((tmp = findInMemory(left, token)) != null)
          left.removeElement(tmp);

        runTestsVaryRight(token);
        break;

      default:
        throw new ReteException("Node2::CallNode",
                                "Bad tag in token",
                                String.valueOf(token.tag));
      } // switch token.tag
      break; // case Node.LEFT;


    case Node.RIGHT:
      switch (token.tag) {

      case RU.UPDATE:
      case RU.ADD:
        if ((tmp = findInMemory(right, token)) == null)
          right.addElement(token);
        

        runTestsVaryLeft(token);
        break;

      case RU.REMOVE:
        if ((tmp = findInMemory(right, token)) != null)
          right.removeElement(tmp);

        runTestsVaryLeft(token);
        break;

      default:
        throw new ReteException("Node2::CallNode",
                                "Bad tag in token",
                                String.valueOf(token.tag));
      } // switch token.tag
      break; // case Node.RIGHT

    default:
      throw new ReteException("Node2::CallNode",
                              "Bad callType",
                              String.valueOf(callType));

    } // switch callType

    // the runTestsVary* routines pass messages on to the successors.

    return true;
  }

  /**
    Node2.callNode can call this to produce debug info.
   */

  protected void debugPrint(Token token, int callType) throws ReteException {
    spec.harness.Context.out.println("TEST " + toString() + ";calltype=" + callType
                       + ";tag=" + token.tag + ";class=" +
                       ((ValueVector) token.facts[0]).get(RU.CLASS).StringValue());
  }

  /**
    Run all the tests on a given (left) token and every token in the
    right memory. For the true ones, assemble a composite token and
    pass it along to the successors.
    */

  protected void runTestsVaryRight(Token lt) throws ReteException {
    int size = right.size();
    for (int i=0; i<size; i++) {
      Token rt = right.elementAt(i);
      Token nt = appendToken(lt,rt);
      if (runTests(lt, rt, nt)) {
        int nsucc = _succ.length;
        for (int j=0; j<nsucc; j++) {
          Successor s = _succ[j];
          s.node.CallNode(nt, s.callType);
        }
      }
    }
  }

  /**
    Run all the tests on a given (right) token and every token in the
    left memory. For the true ones, assemble a composite token and
    pass it along to the successors.
    */

  protected void runTestsVaryLeft(Token rt) throws ReteException {
    int size = left.size();
    for (int i=0; i<size; i++) {
      Token lt = left.elementAt(i);
      Token nt = appendToken(lt,rt);
      if (runTests(lt, rt, nt)) {
        // the new token has the *left* token's tag at birth...
        nt.tag = rt.tag;
        int nsucc = _succ.length;
        for (int j=0; j<nsucc; j++) {
          Successor s = _succ[j];
          s.node.CallNode(nt, s.callType);
        }
      }
    }
  }


  /**
    Run all this node's tests on two tokens.  This routine assumes
    that the right token has only one fact in it, which is true at this
    time!
    */

  boolean runTests(Token lt, Token rt, Token token) throws ReteException {
    int ntests = tests.length;

    ValueVector rf = rt.facts[0];

    for (int i=0; i<ntests; i++) {

      if (tests[i] instanceof Test2) {
        Test2 t = (Test2) tests[i];
        ValueVector lf = lt.facts[t.token_idx];

        if (lf == null)
          continue;

        int lidx = t.left_idx;
        int ridx = t.right_idx;
        int lsubidx = t.left_sub_idx;
        int rsubidx = t.right_sub_idx;

        switch (t.test) {

        case Test2.NEQ:
        case Test2.EQ: {
          Value v1, v2;
          ValueVector slot;
      
          if (lsubidx != -1)  // i.e., first variable is in a multislot
            v1 = lf.get(lidx).ListValue().get(lsubidx);
          else 
            v1 = lf.get(lidx);

          if (rsubidx != -1)  // i.e., first variable is in a multislot
            v2 = rf.get(ridx).ListValue().get(rsubidx);
          else 
            v2 = rf.get(ridx);
          
          boolean retval = v1.equals(v2);
          if (t.test == Test2.EQ && !retval)
            return false;
          else if (t.test == Test2.NEQ && retval)
            return false;

        }
        break;

        default:
          throw new ReteException("Node2::runTests",
                                  "Test2 type not supported",
                                  String.valueOf(t.test));
        }
      } else { // Test1
        // this is a function call! if it evals to Funcall.TRUE(),
        // the test passed; FALSE(), it failed.
        Test1 t = (Test1) tests[i];
        Value value = t.slot_value;
        switch (t.test) {
        case Test1.EQ:
          if (!Funcall.Execute(Eval(value, token),
                          engine.global_context()).equals(Funcall.TRUE()))
            return false;
          break;
        case Test2.NEQ:
          if (Funcall.Execute(Eval(value, token),
                              engine.global_context()).equals(Funcall.TRUE()))
            return false;
          break;
        default:
          throw new ReteException("Node2::runTests",
                                  "Test2 type not supported",
                                  String.valueOf(t.test));
        }
      }
    }
    return true;
  }




  /**
    Create a new token containing the data from rt appended
    to the data from lt. THis routine handles multiple data in the
    right token. NOTE: Only a shallow copy is made;
    the fact data is not duplicated.
    */

  Token appendToken(Token lt, Token rt) {
    Token nt = new Token(lt);
    nt.AddFact(rt.facts[0]);
    nt.sortcode = rt.sortcode;
    return nt;
  }



  /**
    The system was designed so that right-input tokens have only one fact 
    in them. Furthermore, they'll all have the same class type! As
    an optimization, then, we could skip the size check and class comparison.
    For now, I'm leaving it in, using a general comparison function in the
    Token class. In addition, we're assuming a token can only appear once;
    this is also by design, and should always be true, unless we implement
    fact duplication, which CLIPS has as an option. The user can always
    simulate this by using serial #s for facts.

@returns null if not found, the found token otherwise.
    */

  final protected Token findInMemory(TokenVector v, Token t) {
    Token tmp;
    int size = v.size();
    for (int i=0; i < size; i++) {
      tmp = v.elementAt(i);
      if (t.sortcode != tmp.sortcode)
        continue;
      if (t.data_equals(tmp))
        return tmp;
    }
    return null;
  }

  /**
    Describe myself
    */
  
  public String toString() {
    String s ="[Node2 ntests=" + String.valueOf(_tests.size()) + " ";
    for (int i=0; i<_tests.size(); i++)
      s += _tests.elementAt(i).toString() + " ";
    s += "]";
    return s;
  }


}



