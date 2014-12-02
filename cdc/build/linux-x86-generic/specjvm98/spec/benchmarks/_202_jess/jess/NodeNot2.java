// -*- java -*-
//////////////////////////////////////////////////////////////////////
// NodeNot2.java
// Specialized two-input nodes for negated patterns
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

/**
 Specialized two-input node for negated patterns

 NOTE: CLIPS behaves in a surprising way which I'm following here.
 Given this defrule:
 <PRE>
 (defrule test-1
  (bar)       
  (not (foo))
  =>
  (printout t "not foo"))
 
 CLIPS behaves this way:
 
 (watch activations)
 (assert (bar))
 ==> Activation 0 test-1
 (assert (foo))
 <== Activation 0 test-1
 (retract (foo))
 ==> Activation 0 test-1
 
 This is not surprising yet. Here's the funky part
 
 (run)
 "not foo"
 (assert (foo))
 (retract (foo))
 ==> Activation 0 test-1

 The rule fires,  and all that's required to fire it again is for the
 "not-ness" to be removed and replaced; the (bar) fact does not need to
 be replaced. This obviously falls out of the implementation; it makes things
 easy!

</PRE>

@author E.J. Friedman-Hill (C)1996
*/

public class NodeNot2 extends Node2 {

  private static Token nullToken;
  static
  {
    try
      {
        ValueVector nullFact = new ValueVector();
        nullFact.set(new Value("$0$", RU.ATOM), RU.CLASS);
        nullFact.set(new Value(RU.ORDERED_FACT, RU.DESCRIPTOR), RU.DESC);
        nullFact.set(new Value(-1, RU.FACT_ID), RU.ID);
        nullToken = new Token(RU.ADD, nullFact);
      }
    catch (ReteException rex) {}
  }

  NodeNot2(Rete engine) {
    super(engine);
  }

  /**
    Run all the tests on a given (left) token and every token in the
    right memory. Every time a right token *passes* the tests, increment 
    the left token's negation count; at the end, if the
    left token has a zero count, pass it through.
    */

  protected void runTestsVaryRight(Token lt) throws ReteException {
    int size = right.size();
    for (int i=0; i<size; i++) {
      Token rt = right.elementAt(i);
      Token nt = appendToken(lt,rt);
      if (runTests(lt, rt, nt)) 
        lt.negcnt++;
    }

    if (lt.negcnt == 0) {
      Token nt = appendToken(lt, nullToken);
      int nsucc = _succ.length;
      for (int j=0; j<nsucc; j++) {
        Successor s = _succ[j];
        s.node.CallNode(nt, s.callType);
      }
    }
  }

  /**
    Run all the tests on a given (right) token and every token in the
    left memory. For the true ones, increment (or decrement) the appropriate
    negation counts. Any left token which transitions to zero gets passed
    along.
    */
  protected void runTestsVaryLeft(Token rt) throws ReteException {
    int size = left.size();
    for (int i=0; i<size; i++) {
      Token lt = left.elementAt(i);
      Token nt = appendToken(lt,rt);
      if (runTests(lt, rt, nt)) {
        if (rt.tag == RU.ADD || rt.tag == RU.UPDATE) {
          // retract any activation due to the left token
          Token nt2 = appendToken(lt, nullToken);
          nt2.tag = RU.REMOVE;
          int nsucc = _succ.length;
          for (int j=0; j<nsucc; j++) {
            Successor s = _succ[j];
            s.node.CallNode(nt2, s.callType);
          }
        lt.negcnt++;
        } else if (--lt.negcnt == 0) {
          // pass along the revitalized left token
          Token nt2 = appendToken(lt, nullToken); 
          int nsucc = _succ.length;
          for (int j=0; j<nsucc; j++) {
            Successor s = _succ[j];
            s.node.CallNode(nt2, s.callType);
          }
        } else if (lt.negcnt < 0)
          throw new ReteException("NodeNot2::RunTestsVaryLeft",
                                  "Corrupted Negcnt (< 0)",
                                  "");
        
      }
    }
  }
  /**
    Describe myself
    */
  
  public String toString() {
    String s ="[NodeNot2 ntests=" + _tests.size();
    for (int i=0; i<_tests.size(); i++)
      s += _tests.elementAt(i) + " ";
    s += "]";
    return s;
  }
}

