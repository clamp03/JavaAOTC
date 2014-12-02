// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Node1.java
// Single-input nodes of the pattern network
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.util.*;

/**
 Single-input nodes of the pattern network
@author E.J. Friedman-Hill (C)1996
*/

public abstract class Node1 extends Node 
{
  
  final static int TEQ = 1;
  final static int TECT = 2;
  final static int TEV1 = 3;
  final static int TNEV1 = 4;
  final static int TNEQ = 5;
  final static int TELN = 6;
  final static int TMF = 7;
  final static int MTEQ = 20;
  final static int MTNEQ = 21;
  final static int MTELN = 22;
  final static int MTMF = 23;

  int R1, R2, R3, R4;
  Value value;

  /**
    Constructor
    */

  Node1(int command, int R1, int R2, int R3, int R4, Value value, Rete engine)
  {
    super(engine);
    this.command = command;
    this.R1 = R1;
    this.R2 = R2;
    this.R3 = R3;
    this.R4 = R4;
    this.value = value;
  }

  /**
    Do the business of this node.
    The input token of a Node1 should only be single-fact tokens.
    In this version, both ADD and REMOVE Tokens are handled the same
    way; I think this is correct. We don't test the token to see if
    there's a fact there, for speed's sake; if we throw an exception,
    well, there's a bug!
    */
  
  abstract boolean CallNode(Token token, int callType) throws ReteException;

  /**
    callNode can call this to print debug info
    */

  private void debugPrint(Token token, int callType, ValueVector fact,
                          boolean result) throws ReteException
  {
    
    spec.harness.Context.out.print("TEST " + toString() + ";ct=" + callType);
    spec.harness.Context.out.println(";id=" + fact.get(RU.ID).FactIDValue() + ";tag=" +
                       token.tag + ";" +  result);
  }
  
  
  public String toString()
  {
    String s ="[Node1 command=";
    
    switch (command)
      {
      case TEQ:
        s += "TEQ;data=" + value;
        s += ";type=" + R2 + ";idx=" + R3; break;
        
      case MTEQ:
        s += "MTEQ;data=" + value;
        s += ";type=" + R2 + ";idx=" + R3 + ";subidx=" + R4; break;
        
      case TMF:
        s += "TMF"; break;
        
      case MTMF:
        s += "MTMF"; break;
        
      case TNEQ:
        s += "TNEQ;data=" + value;
        s += ";type=" + R2 + ";idx=" + R3; break;
        
      case MTNEQ:
        s += "MTNEQ;data=" + value;
        s += ";type=" + R2 + ";idx=" + R3 + ";subidx=" + R4; break;
        
      case TECT:
        s += "TECT;class=" + RU.getAtom(R1) + ";ordr=" + R2;  break;
        
      case TELN:
        s += "TELN;length=" + R1;  break;
        
      case MTELN:
        s += "MTELN;idx=" + R1 + ";length=" + R2;  break;
        
      case TEV1:
        s += "TEV1;idx1=" + R1 + ";idx2=" + R2; break;
        
      case TNEV1:
        s += "TNEV1;idx1=" + R1 + ";idx2=" + R2; break;
      }
    
    s += "]";
    return s;
  }
  
  /*
   * Node1 Factory method
   */

  static final Node1 create(int cmd, int R1, int R2, int R3,
                            int R4, Value value, Rete rete)
       throws ReteException
  {
    switch(cmd)
      {
      case TMF:
        return new Node1TMF(R1, R2, R3, R4, value, rete);
      case MTMF:
        return new Node1MTMF(R1, R2, R3, R4, value, rete);
      case TEQ:
        return new Node1TEQ(R1, R2, R3, R4, value, rete);
      case MTEQ:
        return new Node1MTEQ(R1, R2, R3, R4, value, rete);
      case MTNEQ:
        return new Node1MTNEQ(R1, R2, R3, R4, value, rete);
      case TEV1:
        return new Node1TEV1(R1, R2, R3, R4, value, rete);
      case TNEV1:
        return new Node1TNEV1(R1, R2, R3, R4, value, rete);
      case TNEQ:
        return new Node1TNEQ(R1, R2, R3, R4, value, rete);
      case TELN:
        return new Node1TELN(R1, R2, R3, R4, value, rete);
      case MTELN:
        return new Node1MTELN(R1, R2, R3, R4, value, rete);
      case TECT:
        return new Node1TECT(R1, R2, R3, R4, value, rete);
      default:
        throw new ReteException("Node1::create",
                                "invalid command code:",
                                String.valueOf(cmd));
      }
  }

  static final Node1 create(int cmd, int R1, int R2, int R3,
                            int R4, Rete rete)
       throws ReteException
  {
    return create(cmd, R1, R2, R3, R4, null, rete);
  }

  static final Node1 create(int cmd, Value value, int R3, int R4, Rete rete)
       throws ReteException
  {
    return create(cmd, 0, 0, R3, R4, value, rete);
  }


}

/**
 * Test that two slots in the same fact DO NOT have the same type and value.
 * Fails if either type or value differ.
 * Absolute index of 1st slot start in R1, second in R2.
 */

class Node1TNEV1 extends Node1
{
  
  Node1TNEV1(int R1, int R2, int R3, int R4, Value value, Rete engine) 
  {
    super(TNEV1, R1, R2, R3, R4, value, engine);
  }
  
  boolean CallNode(Token token, int callType) throws ReteException
  {

    boolean result = false;
    ValueVector fact = token.facts[0];

    Value v1, v2;
    ValueVector slot;
    // make sure both facts are big enough
    if (fact.size() >= R1 && fact.size() >= R2)
      {
        if (R3 != -1)  // i.e., first variable is in a multislot
          v1 = fact.get(R1).ListValue().get(R3);
        else 
          v1 = fact.get(R1);
        
        if (R4 != -1)  // i.e., first variable is in a multislot
          v2 = fact.get(R2).ListValue().get(R4);
        else 
          v2 = fact.get(R2);
        
        result = ! (v1.equals(v2));
      }

    if (result)
      // Pass this token on to all successors.
      for (int i=0; i< _succ.length; i++)
        {
          Successor s = _succ[i];
          s.node.CallNode(token, s.callType);
        }      
    //debugPrint(token, callType, fact, result);
    return result;
  }

}

/**
 * Test that a slot value and type are NOT that same as some type and value;
 * test passes if either type or value differs. value in R1, type in R2, 
 * absolute index of slot start in R3
 */

class Node1TNEQ extends Node1 {
  
  Node1TNEQ(int R1, int R2, int R3, int R4, Value value, Rete engine) 
  {
    super(TNEQ, R1, R2, R3, R4, value, engine);
  }
  
  boolean CallNode(Token token, int callType) throws ReteException
  {

    boolean result = false;
    ValueVector fact = token.facts[0];

    if (value.type() == RU.FUNCALL)
      {
        if (fact.size() >= R3 &&
            ! Funcall.Execute(Eval(value, token),
                              engine.global_context()).equals(Funcall.TRUE()))
          {
            result = true;
          }
      } else if (fact.size() >= R3 && ! fact.get(R3).equals(value))
        {
          result = true;
        }
    
    if (result)
      // Pass this token on to all successors.
      for (int i=0; i< _succ.length; i++) 
        {
          Successor s = _succ[i];
          s.node.CallNode(token, s.callType);
        }      
    //debugPrint(token, callType, fact, result);
    return result;
  }

}

/**
 * Not a test at all. Split a fact into lots of possible alternate facts,
 * each representing a possible multifield mapping. R1 = absolute index
 */

class Node1TMF extends Node1 
{

  Node1TMF(int R1, int R2, int R3, int R4, Value value, Rete engine)
  {
    super(TMF, R1, R2, R3, R4, value, engine);
  }

  boolean CallNode(Token old_token, int callType) throws ReteException 
  {

    int i,j;
    ValueVector fact = old_token.facts[0];
    int fact_size = fact.size();
    int new_fact_size, multi_size;

    for (i=0; i < (fact_size - R1 + 1); i++) 
      {
        new_fact_size = R1 + 1 + i;
        multi_size = fact_size - new_fact_size + 1;

        ValueVector new_fact = new ValueVector(new_fact_size);
        // Start of new and old are the same
        for (j=0; j < R1; j++)
          new_fact.add( fact.get(j));

        // Middle of old moved into special multifield in new
        ValueVector multi
          = new ValueVector(multi_size);
        new_fact.add( new Value(multi, RU.LIST));

        for (j=R1; j < multi_size + R1; j++)
          multi.add( fact.get(j));

        // End of new and old are the same
        for (j=R1 + multi_size; j < new_fact_size - 1 + multi_size; j++)
          new_fact.add( fact.get(j));

        // Handy for debugging
        // System.out.println(new_fact);

        // Now propagate to our successors
        Token token = new Token(old_token.tag, new_fact);

        // Pass this token on to all successors.
        for (j=0; j< _succ.length; j++) 
          {
            Successor s = _succ[j];
            s.node.CallNode(token, s.callType);
          }      
      }

    return true;
  }
  
}

/**
 * Test that two slots in the same fact have the same type and value.
 * Absolute index of 1st slot start in R1, second in R2.
 */

class Node1TEV1 extends Node1
{
  Node1TEV1(int R1, int R2, int R3, int R4, Value value, Rete engine) 
  {
    super(TEV1, R1, R2, R3, R4, value, engine);
  }
  
  boolean CallNode(Token token, int callType) throws ReteException
  {

    boolean result = false;
    ValueVector fact = token.facts[0];

    Value v1, v2;
    ValueVector slot;
    // make sure both facts are big enough
    if (fact.size() >= R1 && fact.size() >= R2)
      {
        if (R3 != -1)  // i.e., first variable is in a multislot
          v1 = fact.get(R1).ListValue().get(R3);
        else 
          v1 = fact.get(R1);
        
        if (R4 != -1)  // i.e., first variable is in a multislot
          v2 = fact.get(R2).ListValue().get(R4);
        else 
          v2 = fact.get(R2);
        
        result = (v1.equals(v2));
      }

    if (result)
      // Pass this token on to all successors.
      for (int i=0; i< _succ.length; i++)
        {
          Successor s = _succ[i];
          s.node.CallNode(token, s.callType);
        }      
    //debugPrint(token, callType, fact, result);
    return result;
  }

}

/**
 * Test slot value and type. value in 'value;', type in R2, 
 * absolute index of slot start in R3; R1 is unused
 */

class Node1TEQ extends Node1 
{
  
  Node1TEQ(int R1, int R2, int R3, int R4, Value value, Rete engine) 
  {
    super(TEQ, R1, R2, R3, R4, value, engine);
  }
  
  boolean CallNode(Token token, int callType) throws ReteException
  {

    boolean result = false;
    ValueVector fact = token.facts[0];

    if (value.type() == RU.FUNCALL) 
      {
        if (fact.size() >= R3 &&
            Funcall.Execute(Eval(value, token),
                            engine.global_context()).equals(Funcall.TRUE())) 
          {
            result = true;
          }
      } else if (fact.size() >= R3 && fact.get(R3).equals(value)) 
        {
          result = true;
        }
    
    if (result)
      // Pass this token on to all successors.
      for (int i=0; i< _succ.length; i++) 
        {
          Successor s = _succ[i];
          s.node.CallNode(token, s.callType);
        }      
    //debugPrint(token, callType, fact, result);
    return result;
  }

}

/**
 * Test just the fact length; length in R1
 */

class Node1TELN extends Node1 
{
  
  Node1TELN(int R1, int R2, int R3, int R4, Value value, Rete engine)
  {
    super(TELN, R1, R2, R3, R4, value, engine);
  }

  boolean CallNode(Token token, int callType) throws ReteException
  {

    ValueVector fact = token.facts[0];
    boolean result = (fact.size() == R1);

    if (result)
      // Pass this token on to all successors.
      for (int i=0; i< _succ.length; i++) 
        {
          Successor s = _succ[i];
          s.node.CallNode(token, s.callType);
        }      
    //debugPrint(token, callType, fact, result);
    return result;
  }


}

/**
  Test class type; class in R1, orderedness in R2, fact length in R3. 
  Length test is >=.
  */

class Node1TECT extends Node1 
{
  
  Node1TECT(int R1, int R2, int R3, int R4, Value value, Rete engine)
  {
    super(TECT, R1, R2, R3, R4, value, engine);
  }

  boolean CallNode(Token token, int callType) throws ReteException
  {

    ValueVector fact = token.facts[0];
    boolean result = 
      (fact.get(RU.CLASS).AtomValue() == R1
       && fact.get(RU.DESC).DescriptorValue() == R2);
       
    if (result)
      // Pass this token on to all successors.
      for (int i=0; i< _succ.length; i++) 
        {
          Successor s = _succ[i];
          s.node.CallNode(token, s.callType);
        }      
    //debugPrint(token, callType, fact, result);
    return result;
  }


}

/**
 * Test multislot value and type for inequality. value in R1, type in R2, 
 * absolute index of slot in R3, absolute index of subslot in R4
 */

class Node1MTNEQ extends Node1 
{

  Node1MTNEQ(int R1, int R2, int R3, int R4, Value value, Rete engine) 
  {
    super(MTNEQ, R1, R2, R3, R4, value, engine);
  }
  
  boolean CallNode(Token token, int callType) throws ReteException
  {

    boolean result = false;
    ValueVector fact = token.facts[0];

    if (fact.size() >= R3) 
      {
        Value s;
        if ((s = fact.get(R3)).type() == RU.LIST) 
          {
            ValueVector vv = s.ListValue();
            if (vv.size() >= R4) 
              {
                Value subslot = vv.get(R4);
                if (value.type() == RU.FUNCALL) 
                  {
                    if (! Funcall.Execute(Eval(value,token), engine.global_context())
                        .equals(Funcall.TRUE()))
                      result = true;
                  }
                else
                  if ( ! subslot.equals(value))
                    result = true;
              }
          }
      }
    
    if (result)
      // Pass this token on to all successors.
      for (int i=0; i< _succ.length; i++) 
        {
          Successor s = _succ[i];
          s.node.CallNode(token, s.callType);
        }      
    //debugPrint(token, callType, fact, result);
    return result;
  }

}

/**
 * Not a test at all. Split a fact into lots of possible alternate facts,
 * each representing a possible multifield mapping within a multislot.
 * R1 = absolute index, R2 = subindex.
 */

class Node1MTMF extends Node1 
{

  Node1MTMF(int R1, int R2, int R3, int R4, Value value, Rete engine)
  {
    super(MTMF, R1, R2, R3, R4, value, engine);
  }
  
  boolean CallNode(Token old_token, int callType) throws ReteException
  {

    ValueVector fact = old_token.facts[0];
    
    int i,j;
    int slot_idx = R1;
    int sub_idx = R2;
    if (fact.get(slot_idx).equals(Funcall.NIL()))
      return false;
    
    ValueVector mslot = fact.get(slot_idx).ListValue();
    int slot_size = mslot.size();
    int new_slot_size, multi_size;
    
    for (i=0; i < (slot_size - sub_idx + 1); i++) 
      {
        new_slot_size = sub_idx + 1 + i;
        multi_size = slot_size - new_slot_size + 1;
      
        ValueVector new_slot = new ValueVector(new_slot_size);
        // Start of new and old are the same
        for (j=0; j < sub_idx; j++)
          new_slot.add( mslot.get(j));
      
        // Middle of old moved into special multifield in new
        ValueVector multi = new ValueVector(multi_size);
        new_slot.add( new Value(multi, RU.LIST));
      
        for (j=sub_idx; j < multi_size + sub_idx; j++) 
          multi.add(mslot.get(j));
      
        // End of new and old are the same
        for (j=sub_idx + multi_size; j < new_slot_size - 1 + multi_size; j++)
          new_slot.add( mslot.get(j));
      
        ValueVector new_fact = (ValueVector) fact.clone();
        new_fact.set(new Value(new_slot, RU.LIST), slot_idx);
      
        // Handy for debugging
        // System.out.println(new_fact);
      
        // Pass this token on to all successors.
        Token token = new Token(old_token.tag, new_fact);
        for (j=0; j< _succ.length; j++) 
          {
            Successor s = _succ[j];
            s.node.CallNode(token, s.callType);
          }      
      }
    return true;
  }
  
}

/**
 * Test multislot value and type. value in R1, type in R2, 
 * absolute index of slot in R3, absolute index of subslot in R4
 */

class Node1MTEQ extends Node1 
{

  Node1MTEQ(int R1, int R2, int R3, int R4, Value value, Rete engine) 
  {
    super(MTEQ, R1, R2, R3, R4, value, engine);
  }
  
  boolean CallNode(Token token, int callType) throws ReteException
  {

    boolean result = false;
    ValueVector fact = token.facts[0];

    if (fact.size() >= R3) 
      {
        Value s;
        if ((s = fact.get(R3)).type() == RU.LIST) 
          {
            ValueVector vv = s.ListValue();
            if (vv.size() >= R4) 
              {
                Value subslot = vv.get(R4);
                if (value.type() == RU.FUNCALL) 
                  {
                    if (Funcall.Execute(Eval(value, token), engine.global_context())
                        .equals(Funcall.TRUE()))
                      result = true;
                  }
                else
                  if ( subslot.equals(value))
                    result = true;
              }
          }
      }
    
    if (result)
      // Pass this token on to all successors.
      for (int i=0; i< _succ.length; i++) 
        {
          Successor s = _succ[i];
          s.node.CallNode(token, s.callType);
        }      
    //debugPrint(token, callType, fact, result);
    return result;
  }

}

/**
 * Test multislot length. Absolute index of slot in R1, length in R2.
 */

class Node1MTELN extends Node1 
{
  
  Node1MTELN(int R1, int R2, int R3, int R4, Value value, Rete engine)
  {
    super(MTELN, R1, R2, R3, R4, value, engine);
  }
  
  boolean CallNode(Token token, int callType) throws ReteException
  {

    ValueVector fact = token.facts[0];
    boolean result = false;

    if (fact.size() >= R1) 
      {
        Value s;
        if ((s = fact.get(R1)).type() == RU.LIST) 
          {
            ValueVector vv = s.ListValue();
            if (vv.size() == R2)
              result = true;
          }
      }

    if (result)
      // Pass this token on to all successors.
      for (int i=0; i< _succ.length; i++) 
        {
          Successor s = _succ[i];
          s.node.CallNode(token, s.callType);
        }      
    //debugPrint(token, callType, fact, result);
    return result;
  }
}



