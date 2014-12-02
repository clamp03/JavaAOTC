 // -*- java -*-
//////////////////////////////////////////////////////////////////////
// ReteCompiler.java
// Generates a pattern network
//
// See the paper
// "Rete: A Fast Algorithm for the Many Pattern/ Many Object Pattern
// Match Problem", Charles L.Forgy, Artificial Intelligence 19(1982), 17-37.
//
// The implementation used here does not follow this paper; the present
// implementation models the Rete net more literally as a set of networked Node
// objects with interconnections.
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
// $Id: ReteCompiler.java,v 1.9 1997/12/02 01:14:39 ejfried Exp $
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.util.*;

/**
 Generates the actual Rete pattern network
@author E.J. Friedman-Hill (C)1997
*/

class ReteCompiler {

  final private int VARIABLE_TYPES = RU.VARIABLE | RU.MULTIVARIABLE;
  private Rete rete;

  /**
    The roots of the pattern network
   */

  Vector roots = new Vector();
  public final Vector roots() { return roots; }

  /**
    Have any rules been added?
    */

  private boolean dirty = false;

  /**
    Constructor
    */

  public ReteCompiler(Rete r) {
    rete = r;
  }

  /**
    Evaluate a funcall, replacing all variable references with the int[]
    mini-bindings used by the nodes for predicate funcall evaluation.
    Bind each variable to the first occurrence in rule
    */

  private Value Eval(int[][] table, Value v)
    throws ReteException {
    ValueVector vv = (ValueVector) v.FuncallValue().clone();
    for (int i=0; i<vv.size(); i++) {
      if ((vv.get(i).type() &  VARIABLE_TYPES) != 0) {
        int[] binding = new int[3];
        int name = vv.get(i).VariableValue();
        for (int j=0; j<table[0].length; j++)
          if (table[0][j] == name) {
              binding[0] = table[1][j];
              binding[1] = table[2][j];
              binding[2] = table[3][j];
              break;
          }
        vv.set(new Value(binding, RU.INTARRAY), i);

      } else if (vv.get(i).type() ==  RU.FUNCALL) {
        // nested funcalls
        vv.set(Eval(table, vv.get(i)), i);
      }
    }
    return new Value(vv, RU.FUNCALL);
  }

  /**
    Call this on a funcall value AFTER Eval has modified it.
    it returns true iff this funcall contains binding to patterns other
    than the one named by index.
    */
  private boolean CheckForMultiPattern(Value v, int index)
    throws ReteException {

    ValueVector vv = v.FuncallValue();
    for (int i=0; i<vv.size(); i++) {
      if (vv.get(i).type() ==  RU.INTARRAY &&
          vv.get(i).IntArrayValue()[0] != index) {
        return true;
      }
        
      else if (vv.get(i).type() ==  RU.FUNCALL &&
               CheckForMultiPattern(vv.get(i), index)) {
        return true;
      }
    }
    return false;

  }

  void freeze() {
    if (dirty) {
      for (int i=0; i< roots.size(); i++) {
        ((Successor) roots.elementAt(i)).node.freeze();
      }
    }
    dirty = false;
  }

  /**
    Add a rule's patterns to the network
   */

  public void AddRule(Defrule r) throws ReteException {
    
    if (rete.watch_compilations())
      rete.display().stdout().print(RU.getAtom(r.name) + ": ");

    // Tell the rule to fix up its binding table;
    // it must be complete at this time.

    r.freeze();

    // First construct a set of three parallel int[]s, each of which
    // is n_variables long, which contain: a variable name (atom),
    // the number of the first pattern in this rule containing that
    // variable, and the index within that pattern where the variable
    // first appears. If the first occurrence of a variable is negated,
    // throw an exception - this is a syntax error.
    // this table is used to replace variable names in function calls and
    // also in pass three to produce two-input nodes.

    int[][] table = makeVarTable(r);


    // 'terminals' will be where we hold onto the final links in the
    // chain of nodes built during the first pass for each pattern
    Successor[] terminals = new Successor[r.patts.size()];
    
    /* *********
       FIRST PASS
       ********* */

    // In the first pass, we just create some of the one-input
    // nodes for each pattern.
    // These one-input nodes compare a certain slot in a fact (token) with a
    // fixed, typed, piece of data. Each pattern gets one special
    // one-input node, the TECT node, which checks the class type
    // and class name.


    // iterate over each of the rule's patterns
    for (int i=0; i<r.patts.size(); i++) {

      // get this pattern

      Pattern p = (Pattern) r.patts.elementAt(i);

      ////////////////////////////////////////////////////////////
      // Every pattern must have a definite class name
      // Therefore, the first node in a chain always
      // checks the class name and the orderedness of a token
      ////////////////////////////////////////////////////////////

      int length = RU.FIRST_SLOT + p.tests.length;
      Successor last = CreateSuccessor(roots, Node1.TECT,
                                        p.classname(), p.ordered(),
                                        0);

      // First we have to find all the multifields, because these change the
      // 'shape' of the facts as they go through the net. 

      for (int j=0; j<p.tests.length; j++) {

        // any tests on this slot?
        if (p.tests[j] == null)
          continue;

        int test_idx = RU.FIRST_SLOT + j;
        
        for (int k=0; k<p.tests[j].length; k++) {
          Test1 test = p.tests[j][k];

          if (test.slot_value.type() == RU.MULTIVARIABLE) {
            if (test.sub_idx == -1) {

            // Split this fact into many possible multifield facts
            last = CreateSuccessor(last.node.succ,
                                    Node1.TMF, test_idx, 0, 0);
            } else {
              // Split this multislot into many multislot facts
              last = CreateSuccessor(last.node.succ,
                                     Node1.MTMF, test_idx, test.sub_idx, 0);
            }
          } 
          
        }

      }


      ////////////////////////////////////////////////////////////
      // Good time to check size, now that it's deinitely set.
      ////////////////////////////////////////////////////////////

      last = CreateSuccessor(last.node.succ, Node1.TELN,
                             length, 0, 0);
      
      // Test multislot sizes too...
      for (int j=0; j<p.tests.length; j++) {
        
        // any tests on this slot?
        if (p.tests[j] == null)
          continue;
        
        // if multislot, size is determinate now (splitting done above)
        if (p.slotlengths != null && p.slotlengths[j] != 0) 
          last = CreateSuccessor(last.node.succ, Node1.MTELN,
                                 RU.FIRST_SLOT + j, p.slotlengths[j], 0);

      }

      ////////////////////////////////////////////////////////////
      // Simplest basic tests are done here
      ////////////////////////////////////////////////////////////

      for (int j=0; j<p.tests.length; j++) {

        // any tests on this slot?
        if (p.tests[j] == null)
          continue;

        int test_idx = RU.FIRST_SLOT + j;
        
        for (int k=0; k<p.tests[j].length; k++) {
          Test1 test = p.tests[j][k];
          int slot_test = (test.sub_idx == -1 ? Node1.TEQ : Node1.MTEQ);
          if (test.test == Test1.NEQ)
            slot_test = (test.sub_idx == -1 ? Node1.TNEQ : Node1.MTNEQ);

          if (test.slot_value.type() == RU.VARIABLE) {

             // don't make nodes for variables during this pass

          } else if (test.slot_value.type() == RU.MULTIVARIABLE) {

            // Just did these!

          } else if (test.slot_value.type() == RU.FUNCALL) {

            // expand the variable references to index, slot, subslot  triples

            Value v = Eval(table, test.slot_value);
            // if only this fact is mentioned, generate a test
            if (!CheckForMultiPattern(v, i)) {
              last = CreateSuccessor(last.node.succ,
                                      slot_test, v,
                                      test_idx, test.sub_idx);
            }


          } else // not a FUNCALL
            last = CreateSuccessor(last.node.succ,
                                    slot_test, test.slot_value,
                                    test_idx, test.sub_idx);

        }
      }
      terminals[i] = last;
    }

      /* *********
         SECOND PASS
         ********* */

    // In this pass, we are looking for variables which must be
    // instantiated the same way twice in one fact. IE, the pattern look like
    // (foo ?X foo ?X), and we're looking for facts like (foo bar foo bar).
    // NOT versions are handled as well.

    // iterate over each of the rule's patterns
    for (int i=0; i<r.patts.size(); i++) {

      // get this pattern
      Pattern p = (Pattern) r.patts.elementAt(i);

      // workspace to track variables that have been done.
      int[] done_vars = new int[256];
      int nvars = 0;

      // find a variable slot, if there is one. If one is found,
      // look at the rest of
      // the fact for another one with the same name.
      // If one is found, create the
      // appropriate node and put it in place.

      // NOTs make things a bit more complex.
      // There are a few cases for a varname
      // appearing twice in a pattern:

      // ?X ?X        ->        generate a TEV1 node.
      // ?X ~?X       ->        generate a TNEV1 node.
      // ~?X ?X       ->        generate a TNEV1 node.
      // ~?X ~?X      ->        (DO NOTHING!)


      // look for a slot in the pattern containing a variable

      for (int j= 0; j < p.tests.length; j++) {
        // any tests for this slot?
        if (p.tests[j] == null)
          continue;
      k_loop:
        for (int k= 0; k < p.tests[j].length; k++) {
          if ((p.tests[j][k].slot_value.type() & VARIABLE_TYPES) != 0 ) {
            
            // see if we've done this one before.
            for (int m=0; m<nvars; m++) {
              if (p.tests[j][k].slot_value.VariableValue() == done_vars[m])
                continue k_loop;
            }
            
            // no, we haven't. Find each other occurrence.
            
            for (int n=j + 1; n < p.tests.length; n++) {
              if (p.tests[n] == null)
                continue;
              for (int o= 0; o < p.tests[n].length; o++) {
                if ((p.tests[n][o].slot_value.type() & VARIABLE_TYPES) != 0 &&
                    p.tests[n][o].slot_value.VariableValue() ==
                    p.tests[j][k].slot_value.VariableValue()) {
                  // we've identified another slot with the same variable.
                  // Do what's described in the table above.
                  int slot1 = j + RU.FIRST_SLOT;
                  int slot2 = n + RU.FIRST_SLOT;
                  if (p.tests[j][k].test == Test1.EQ) {
                    if (p.tests[n][o].test == Test1.EQ)
                      terminals[i] = CreateSuccessor(terminals[i].node.succ,
                                                     Node1.TEV1, slot1,
                                                     slot2,
                                                     p.tests[j][k].sub_idx,
                                                     p.tests[n][o].sub_idx
                                                     );
                    else
                      terminals[i] = CreateSuccessor(terminals[i].node.succ,
                                                     Node1.TNEV1, slot1,
                                                     slot2,
                                                     p.tests[j][k].sub_idx,
                                                     p.tests[n][o].sub_idx);
                    
                  } else {
                    if (p.tests[n][o].test == Test1.EQ)
                      terminals[i] = CreateSuccessor(terminals[i].node.succ,
                                                     Node1.TNEV1, slot1,
                                                     slot2,
                                                     p.tests[j][k].sub_idx,
                                                     p.tests[n][o].sub_idx);
                    else
                      ;
                  }
                }
              }
            }
            done_vars[nvars++] = p.tests[j][k].slot_value.VariableValue();
          }
        }
      }
      
    } // end of second pass
    
    /* *********
       THIRD PASS
       ********* */

    // Now we start making some two-input nodes. These nodes check that
    // a variable with the same name in two patterns is instantiated the
    // same way in each of two facts; or not, in the case of negated
    // variables. An important thing to remember: the first instance of a
    // variable can never be negated. We'll check that here and throw
    // an exception if it's violated.  We can compare every other instance
    // of the variable in this rule against this first one - this simplifies
    // things a lot! We'll use simplified logic which will lead to a few
    // redundant, but correct tests.

    // Two-input nodes can contain many tests, so they are rather more
    // complex than the one-input nodes. To share them, what we'll do is build
    // a new node, then compare this new one to all possible shared ones.
    // If we can share, we just throw the new one out. The inefficiency is
    // gained back in spades at runtime, both in memory and speed. Note that
    // NodeNot2 type nodes cannot be shared.

    /*
       The number of two-input nodes that we create is *determinate*: it is
       always one less than the number of patterns. For example, w/ 4 patterns,
       numbered 0,1,2,3, and the following varTable:
       (Assuming RU.FIRST_SLOT == 2 and RU.SLOT_SIZE = 2)

       <PRE>

       X  Y  N
       0  1  2
       2  4  4

       </PRE>
       generated from the following rule LHS:

       <PRE>
       (foo ?X ?X)
       (bar ?X ?Y)
       (Goal (Type Simplify) (Object ?N))
       (Expression (Name ?N) (Arg1 0) (Op +) (Arg2 ~?X))
       </PRE>

       Would result in the following nodes being generated
       (Assuming SLOT_DATA == 0, SLOT_TYPE == 1, SLOT_SIZE == 2):

       <PRE>
       0     1
        \   /
     ___L___R____
     |          |            2
     | 0,2 = 2? |           /
     |          |          /
     ------------ \0,1    /
                  _L______R__                3
                 |          |               /
                 | NO TEST  |              /
                 |          |             /
                 ------------ \0,1,2     /
                                 L_______R__
                                 | 0,2 != 8?|
                                 | 2,4 = 2? |
                                 |          |
                                  ------------
                                       |0,1,2,3
                                       |
                                    (ACTIVATE)

       <PRE>

       Where the notation 2,4 = 8? means that this node tests tbat index 4 of
       fact 2 in the left token is equal to index 8 in the right
       token's single fact. L and R indicate Left and Right inputs.
       */

    // for each pattern, starting with the second one

    for (int i=1; i < (r.patts.size()); i++) {

      // get this pattern
      Pattern p = (Pattern) r.patts.elementAt(i);

      // construct an appropriate 2 input node...
      Node2 n2;
      if (p.negated())
        n2 = new NodeNot2(rete);
      else
        n2 = new Node2(rete);
      // now tell the node what tests to perform

      // for each field in this pattern
      for (int j=0; j< p.tests.length; j++) {

        // any tests for this slot?
        if (p.tests[j] == null)
          continue;

        // for every test on this slot..
        for (int k=0; k< p.tests[j].length; k++) {

          // if this test is against a variable...
          if ((p.tests[j][k].slot_value.type() & VARIABLE_TYPES) != 0) {
            
            // find this variable in the table
            int n = 0;
            while (table[0][n] != p.tests[j][k].slot_value.VariableValue()) n++;
            
            // table[1][n] can't be greater than i
            if (table[1][n] > i)
              compiler_error("AddRule", "Corrupted VarTable: table[1][n] > i");

            // if this is the first appearance, no test.
            else if (table[1][n] == i)
              continue;

            if (p.tests[j][k].test == Test1.EQ)
              n2.addTest(Test2.EQ,
                         table[1][n],
                         table[2][n],
                         table[3][n],
                         RU.FIRST_SLOT + j,
                         p.tests[j][k].sub_idx);
            else
              n2.addTest(Test2.NEQ,
                         table[1][n],
                         table[2][n],
                         table[3][n],
                         RU.FIRST_SLOT + j,
                         p.tests[j][k].sub_idx);

            
            // if this test is a function call
          } else if (p.tests[j][k].slot_value.type() == RU.FUNCALL) {

            // expand the variable references to index, slot pairs
            // we do this again even though we did it in pass one
            // we don't want to destroy the patterns themselves
            // Tell Eval to bind variables to first occurrence in Rule.
            Value v = Eval(table, p.tests[j][k].slot_value);

            // if other facts besides this one are mentioned, generate a test

            if (CheckForMultiPattern(v, i)) {
              if (p.tests[j][k].test == Test1.EQ)
                n2.addTest(Test1.EQ, RU.FIRST_SLOT + j,
                           p.tests[j][k].sub_idx, v);
              else
                n2.addTest(Test1.NEQ, RU.FIRST_SLOT + j,
                           p.tests[j][k].sub_idx, v);
            }
          }
        }
      }
      
      // search through the successors of this pattern and the next one.
      // Do they have any in common, and if so, are they equivalent to the
      // one we just built? If so, we don't need to add the new one!
      
 
      boolean new_node = true;
      Successor s1 = null, s2 = null;
      if (!p.negated()) {
      j_loop:
        for (int j=0; j<terminals[i-1].node.succ.size(); j++) {
          Node jnode = ((Successor)terminals[i-1].node.succ.elementAt(j)).node;
          if (!(jnode instanceof Node2) || (jnode instanceof NodeNot2))
            continue;
          for (int k=0; k<terminals[i].node.succ.size(); k++) {
            if (jnode==((Successor)terminals[i].node.succ.elementAt(k)).node) {
              s1 = (Successor) terminals[i-1].node.succ.elementAt(j);
              s2 = (Successor) terminals[i].node.succ.elementAt(k);
              if (s1.callType == Node.LEFT &&
                  s2.callType == Node.RIGHT &&
                  ((Node2)jnode).equals(n2)) {
                new_node = false;
                break j_loop;
              }
            }
          }
        }
      }
      
      if (new_node) {
        // attach it to the tails of the node chains from
        // this pattern and the next
        
        s1 = new Successor(n2, Node.LEFT);
        terminals[i-1].node.succ.addElement(s1);
        
        s2 = new Successor(n2, Node.RIGHT);
        terminals[i].node.succ.addElement(s2);
       
        // OK, it's done; speed it up!
        n2.complete();
        if (rete.watch_compilations())
          rete.display().stdout().print("+2"); 
      } else 
        if (rete.watch_compilations())
          rete.display().stdout().print("=2");
      
      // Advance the tails
      terminals[i-1] = s1;
      terminals[i] = s2;
      
    }
    
    /* ************
       FOURTH PASS
       ************ */

    // All that's left to do is to create the terminal node.
    // This is very easy.

    NodeTerm nt = new NodeTerm(r, rete);
    r.activator = nt;
    Successor s = new Successor(nt, Node.ACTIVATE);
    terminals[r.patts.size() - 1].node.succ.addElement(s);

    if (rete.watch_compilations())
      rete.display().stdout().println("+t");

    // we need freezing.
    dirty = true;

    //Tell the engine to update this rule if the agenda isn't empty
    for (int i=0; i<roots.size(); i++)
      rete.UpdateNode(((Successor) roots.elementAt(i)).node);

  }

  /**

    Construct a table (patterns x names) listing the first index at which
    each var appears in each pattern; -1 marks entries in
    which the var does not appear in the pattern.

   */

  private int[][] makeVarTable(Defrule r) throws ReteException {

    // First we need a list of all the unique variable names
    // in this rule. We take the brute force approach. Note: we assume
    // that no more than 256 variable references can occur on the LHS of a rule.

    int[] names =new int[256];
    int num_names = 0;

    // new the 2-D table
    int[][] scr_table = new int[4][256];

    // iterate over each of the rule's patterns
    for (int patt=0; patt<r.patts.size(); patt++) {

      // get this pattern
      Pattern p = (Pattern) r.patts.elementAt(patt);
      // for each slot in this pattern
      for (int slot= 0; slot < p.tests.length; slot++) {
        // are there any tests for this slot?
        if (p.tests[slot] == null)
          continue;
        // for each test in this slot
        for (int test= 0; test < p.tests[slot].length; test++) {
          // if this test is against a variable,
          if ((p.tests[slot][test].slot_value.type() & VARIABLE_TYPES) != 0) {

            // store it!

            // If we haven't seen this one before
            // checkfor illegal initial negation

            if (-1 ==
                findIntInArray(scr_table[0], num_names,
                               p.tests[slot][test].slot_value.VariableValue()))
              if (p.tests[slot][test].test == Test1.NEQ)
                compiler_error("makeVarTable","Variable " +
                               RU.getAtom(p.tests[slot][test]
                                          .slot_value.VariableValue()) +
                               " is used before definition, Rule " +
                               RU.getAtom(r.name));

            if (p.tests[slot][test].test != Test1.NEQ) {
              scr_table[0][num_names] =
                p.tests[slot][test].slot_value.VariableValue();
              scr_table[1][num_names] = patt;
              scr_table[2][num_names] = RU.FIRST_SLOT + slot;
              scr_table[3][num_names++] = p.tests[slot][test].sub_idx;
            }
          }
        }
      }
    }
    // new a 2-D table just the right size
    int[][] table = new int[4][num_names];
    
    for (int i=0; i<4; i++)
      System.arraycopy(scr_table[i], 0, table[i], 0, num_names);
    
    /*
      // debug print the vartable
      for (int i=0; i<num_names; i++) {
      spec.harness.Context.out.print(RU.getAtom(table[0][i]) + " ");
      for (int j=1; j<4; j++) 
      spec.harness.Context.out.print(table[j][i] + " ");
      spec.harness.Context.out.println();
      }
      */
    
    return table;

  }
  

  /**
    Returns index if i exists in array[size], else -1
    */

  private int findIntInArray(int[] array, int size, int i) {
    int rv = -1;
    for (int j=0; j< size; j++)
      if (array[j] == i) {
        rv = j;
        break;
      }
    return rv;
  }



  /**
    Return an old or new one-input node of a given description
    within the given Vector of nodes.
    */

  Successor CreateSuccessor(Vector amongst, int cmd,
                                   int R1, int R2, int R3)
       throws ReteException
  {
    return CreateSuccessor(amongst, cmd, R1, R2, R3, -1);
  }

  Successor CreateSuccessor(Vector amongst, int cmd,
                                   int R1, int R2, int R3, int R4) 
       throws ReteException
  {
    for (int i=0; i< amongst.size(); i++) {
      Successor test = (Successor) amongst.elementAt(i);
      if (test.node instanceof Node1) {
        Node1 node = (Node1) test.node;
        if ((node.command == cmd) &&
            (node.R1 == R1) &&
            (node.R2 == R2) &&
            (node.R3 == R3) &&
            (node.R4 == R4)) {
          if (rete.watch_compilations())
            rete.display().stdout().print("=1");
          return test;
        }
      }
    }
    Node1 n = Node1.create(cmd, R1, R2, R3, R4, rete);
    Successor rv = new Successor(n, Node.SINGLE);
    amongst.addElement(rv);
    if (rete.watch_compilations())
      rete.display().stdout().print("+1");
    return rv;
  }


  private void CleanupBindings(Value v) throws ReteException {
    ValueVector vv = v.FuncallValue();
    for (int i=0; i<vv.size(); i++) {
      if (vv.get(i).type() ==  RU.INTARRAY)
        vv.get(i).IntArrayValue()[0] = 0;
      else if (vv.get(i).type() ==  RU.FUNCALL)
        CleanupBindings(vv.get(i));
    }
  }


  Successor CreateSuccessor(Vector amongst, int cmd,
                                   Value value, int R3)
    throws ReteException {
      return CreateSuccessor(amongst, cmd, value, R3, -1);
  }
  Successor CreateSuccessor(Vector amongst, int cmd,
                                   Value value, int R3, int R4)
    throws ReteException {
    
    // Note that the 'value' object will contain bindings that refer to the real
    // pattern index; but tests will actually happen for single-pattern tokens
    // only. Therefore, some cleaning up is in order.

    if (value.type() == RU.FUNCALL)
      CleanupBindings(value);
    else if (value.type() == RU.INTARRAY)
      value.IntArrayValue()[0] = 0;


    for (int i=0; i< amongst.size(); i++) {
      Successor test = (Successor) amongst.elementAt(i);
      if (test.node instanceof Node1) {
        Node1 node = (Node1) test.node;
          if ((node.command == cmd) && value.equals(node.value)
              && node.R3 == R3 && node.R4 == R4) {
            if (rete.watch_compilations())
              rete.display().stdout().print("=1");
            return test;
          }
        }
    }
    Node1 n = Node1.create(cmd, value, R3, R4, rete);
    Successor rv = new Successor(n, Node.SINGLE);
    amongst.addElement(rv);
    if (rete.watch_compilations())
      rete.display().stdout().print("+1");

    return rv;
  }

  private void compiler_error(String routine, String message)
    throws ReteException {
    throw new ReteException("ReteCompiler::" + routine, message, "");
  }

}
