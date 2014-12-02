// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Fact.java
// Convenience class used to parse and print Facts
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

import java.util.BitSet; //**NS**
import java.util.Enumeration;
import java.util.Stack;
import java.util.Hashtable;
import java.util.Vector;
/**
 Convenience class used to parse and print facts.
 A Fact is represented as a Value Vector where the first entry is the
 head, the second a descriptor with type RU.ORDERED_ -
 or RU.UNORDERED_ - FACT. The third entry has type RU.FACT_ID and is
 the integer fact id.

@author E.J. Friedman-Hill (C)1996
*/

public class Fact {

  /**
    The actual fact array
   */

  private ValueVector fact;

  /**
    The deftemplate corresponding to this fact
   */

  ValueVector deft;
  int name;
  int ordered;

  /**
    Constructor.
    */

  public Fact(String name, int ordered, Rete engine) throws ReteException {
    this.name = RU.putAtom(name);
    this.ordered = ordered;
    deft = FindDeftemplate(name, ordered, engine);
    fact = CreateNewFact(deft);
  }

  /**
    Constructor
    starts from the ValueVector form of a Fact
    */

  public Fact(ValueVector f, Rete engine) throws ReteException {
    name = f.get(RU.CLASS).AtomValue();
    ordered = f.get(RU.DESC).DescriptorValue();
    deft = FindDeftemplate(RU.getAtom(name), ordered, engine);
    fact = (ValueVector) f.clone();
  }

  private ValueVector CreateNewFact(ValueVector deft) {
    ValueVector fact = new ValueVector();
    int size = (deft.size() -RU.FIRST_SLOT)/RU.DT_SLOT_SIZE + RU.FIRST_SLOT;
    fact.set_length(size);

    fact.set(deft.get(RU.CLASS), RU.CLASS);
    fact.set(deft.get(RU.DESC), RU.DESC);
    fact.set(deft.get(RU.ID), RU.ID);
    int j = RU.FIRST_SLOT + RU.DT_DFLT_DATA;
    for (int i=RU.FIRST_SLOT; i< size; i++) {
      fact.set(deft.get(j), i);
      j += RU.DT_SLOT_SIZE;
    }
    return fact;
  }


  /**
    find the deftemplate, if there is one, or create implied dt.
    */
  
  private ValueVector FindDeftemplate(String name, int ordered, Rete engine) throws ReteException {
    
    ValueVector deft;
    
    deft = engine.FindDeftemplate(name);
    if (deft != null) {
      if (deft.get(RU.DESC).DescriptorValue() != ordered) {
        // looks like there are semantic errors in input.
        throw new ReteException("Fact::FindDeftemplate",
                                "Attempt to duplicate implied deftemplate:",
                                name);
      }
    } else  {
      // this is OK. Create an implied deftemplate if this is an ordered fact.
      if (ordered == RU.UNORDERED_FACT) {
          throw new ReteException("Fact::FindDeftemplate",
                                  "Can't create implied unordered deftempl:",
                                  name);
      }
      deft = engine.AddDeftemplate(new Deftemplate(name,ordered));
    }
    return deft;
  }
  
  
  /**
    Add a value to this fact
    */
  
  private int FindSlot(String slotname) throws ReteException {
    return FindSlot(RU.putAtom(slotname));
  }

  private int FindSlot(int slotname) throws ReteException {

    if (ordered == RU.ORDERED_FACT)
      throw new ReteException("Fact::FindSlot",
                              "Attempt to find named slot in ordered fact",
                              "");

    // try to find this slotname in the deftemplate
    int i;
    for (i=RU.FIRST_SLOT; i < deft.size(); i+=RU.DT_SLOT_SIZE)
      if (deft.get(i + RU.DT_SLOT_NAME).AtomValue() == slotname)
        break;
    if (i >= deft.size())
      throw new ReteException("Fact::AddValue",
                              "Attempt to add field with invalid slotname",
                              RU.getAtom(slotname));
    return (i - RU.FIRST_SLOT) / RU.DT_SLOT_SIZE + RU.FIRST_SLOT;
  }

  
  public void AddValue(String slotname, String value, int type) throws ReteException {
    int i = FindSlot(slotname);
    fact.set(new Value(value, type),i);
  }
  
  public void AddValue(String slotname, int value, int type) throws ReteException {
    int i = FindSlot(slotname);
    fact.set(new Value(value, type),i);
  }

  public void AddValue(String slotname, double value, int type) throws ReteException {
    int i = FindSlot(slotname);
    fact.set(new Value(value, type),i);
  }

  public void AddValue(String slotname, Funcall value, int type) throws ReteException {
    int i = FindSlot(slotname);
    fact.set(new Value(value.vvec, type),i);
  }

  public void AddValue(String slotname, Value value) throws ReteException {
    switch (value.type()) {
    case RU.ATOM:
      AddValue(slotname, value.AtomValue(), RU.ATOM); break;
    case RU.STRING:
      AddValue(slotname, value.AtomValue(), RU.STRING); break;
    case RU.INTEGER:
      AddValue(slotname, value.IntValue(), RU.INTEGER); break;
    case RU.FACT_ID:
      AddValue(slotname, value.FactIDValue(), RU.FACT_ID); break;
    case RU.FLOAT:
      AddValue(slotname, value.FloatValue(), RU.FLOAT); break;
    default:
      throw new ReteException("Fact::AddValue",
                              "Bad field type",
                              "" + value.type());
    }
  }


  public void AddValue(String value, int type) throws ReteException {
    AddValue(RU.putAtom(value), type);
  }

  

  public void AddValue(int value, int type) throws ReteException {
    if (ordered == RU.UNORDERED_FACT)
      throw new ReteException("Fact::AddValue",
                              "Attempt to add ordered field to unordered fact",
                              "");

    fact.add(new Value(value, type));
  }

  public void AddValue(double value, int type) throws ReteException {
    if (ordered == RU.UNORDERED_FACT)
      throw new ReteException("Fact::AddValue",
                              "Attempt to add ordered field to unordered fact",
                              "");

    fact.add(new Value(value, type));
  }

  public void AddValue(Funcall value, int type) throws ReteException {
    if (ordered == RU.UNORDERED_FACT)
      throw new ReteException("Fact::AddValue",
                              "Attempt to add ordered field to unordered fact",
                              "");

    fact.add(new Value(value.vvec, type));
  }


  /**
    Returns the actual fact
    */

  public ValueVector FactData() {
    return fact;
  }

  /**
    Describe myself
    */

  private String valString(int i)  throws ReteException {
    switch (fact.get(i).type()) {
    case RU.ATOM:
      return fact.get(i).StringValue();
    case RU.STRING:
      return "\"" + fact.get(i).StringValue() + "\"";
    case RU.INTEGER:
      return String.valueOf(fact.get(i).IntValue());
    case RU.FLOAT:
      return String.valueOf(fact.get(i).FloatValue());
    case RU.VARIABLE:
      return fact.get(i).StringValue();
    case RU.FUNCALL:
      return fact.get(i).FuncallValue().get(0).StringValue();
    default:
      return String.valueOf(fact.get(i).toString());
    }
  }


  public String toString() {
    try {
      String s = "[Fact: " + RU.getAtom(name) + " ";
      s += (ordered == RU.ORDERED_FACT)
        ? "(ordered)" : "(unordered)";
      
      if (ordered == RU.ORDERED_FACT) {
        // print each slot
        for (int i=RU.FIRST_SLOT; i< fact.size(); i++) {
          s += " " + valString(i);
        }
        
      } else { // UNORDERED_FACT
        
        int nslots = (deft.size() - RU.FIRST_SLOT) / RU.DT_SLOT_SIZE;
        
        for (int i=0; i< nslots; i++) {
          s  += " "
            + deft.get(RU.FIRST_SLOT+(i*RU.DT_SLOT_SIZE)+RU.DT_SLOT_NAME).StringValue()
                         + "=" + valString(RU.FIRST_SLOT + i) + ";";
        }
      }
      s += "]";
      return s;
    } catch (ReteException re) {
      throw new RuntimeException(re.toString());
    }
  }

  public String ppfact() {
    try {
      String s = "(" + RU.getAtom(name);

      if (ordered == RU.ORDERED_FACT) {
        // print each slot
        for (int i=RU.FIRST_SLOT; i< fact.size(); i++) {
          s += " " + valString(i);
        }
        
      } else { // UNORDERED_FACT
        
        int nslots = (deft.size() - RU.FIRST_SLOT) / RU.DT_SLOT_SIZE;
        
        for (int i=0; i< nslots; i++) {
          s  += " ("
            + deft.get(RU.FIRST_SLOT+(i*RU.DT_SLOT_SIZE)+RU.DT_SLOT_NAME).StringValue()
                         + " " + valString(RU.FIRST_SLOT + i) + ")";
        }
      }
      s += ")";
      return s;
    } catch (ReteException re) {
      throw new RuntimeException(re.toString());
    }
  }


}

