// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Deftemplate.java
// Convenience class used to parse and print deftemplates
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.util.*;

/**
 Convenience class used to parse and print deftmplates.
 A deftemplate is represented as a ValueVector

An ordered deftemplate has only the first few fields, no slots

@author E.J. Friedman-Hill (C)1996
*/

public class Deftemplate {
  
  private ValueVector deft;
  int name;
  int ordered;
  String docstring = null;

  public final int getName() { return name; }
  public final String getDocstring() { return docstring; }

  /**
    Constructor.
    */

  Deftemplate(String name, int ordered) throws ReteException {
    deft = new ValueVector();
    this.name = RU.putAtom(name);
    this.ordered = ordered;
    deft.set_length(3);
    deft.set(new Value(this.name, RU.ATOM), RU.CLASS);
    deft.set(new Value(ordered, RU.DESCRIPTOR), RU.DESC);
    deft.set(new Value(0, RU.FACT_ID), RU.ID);
  }
  
  /**
    Constructor
    starts from the ValueVector form of a Deftemplate
    */

  Deftemplate(ValueVector dt) throws ReteException {
    deft = (ValueVector) dt.clone();
    name = dt.get(RU.CLASS).AtomValue();
    ordered = dt.get(RU.DESC).DescriptorValue();
  }


  /**
    Create a new slot in this deftemplate
    */

  void AddSlot(String name, Value value) throws ReteException {
    if (ordered != RU.UNORDERED_FACT)
      throw new ReteException("AddSlot",
                              "Ordered deftemplates cannot have slots:",
                              name);
    deft.add(new Value(name, RU.SLOT));
    deft.add(value);

  }

  /**
    Create a new multislot in this deftemplate
    */

  void AddMultiSlot(String name, Value value) throws ReteException {
    if (ordered != RU.UNORDERED_FACT)
      throw new ReteException("AddSlot",
                              "Ordered deftemplates cannot have slots:",
                              name);
    deft.add(new Value(name, RU.MULTISLOT));
    deft.add(value);
  }
  
  /**
    Generates the ValueVector form of this Deftemplate
    */

  ValueVector DeftemplateData() {
    return deft;
  }

  /**
    Report whether a given slot is multi or not in a ValueVector dt
    */

  static int SlotType(ValueVector vvec, String slotname)
       throws ReteException {

    if (vvec.get(RU.DESC).DescriptorValue() != RU.UNORDERED_FACT)
      return RU.NONE;

    int slotint = RU.putAtom(slotname);

    for (int i = RU.FIRST_SLOT; i < vvec.size(); i += RU.DT_SLOT_SIZE)
      if (vvec.get(i + RU.DT_SLOT_NAME).AtomValue() == slotint)
        return vvec.get(i + RU.DT_SLOT_NAME).type();
    

    return RU.NONE;
  }


  
  /**
    Describe myself
    */

  public String toString() {
    try {
      String s = "[Deftemplate: " + RU.getAtom(name) + " ";
      s += (ordered == RU.ORDERED_FACT)
        ? "(ordered)" : "(unordered)";
      if (docstring != null)
        s += " \"" + docstring + "\" ";
      if (ordered == RU.UNORDERED_FACT) {
        s +=  "slots:";
        for (int i=RU.FIRST_SLOT; i<deft.size(); i+=RU.DT_SLOT_SIZE)
          s += " " + deft.get(i + RU.DT_SLOT_NAME).StringValue() +
            " default: " + deft.get(i + RU.DT_DFLT_DATA).toString() + ";";
      }
      s += "]";
      return s; 
    } catch (ReteException re) {
      throw new RuntimeException(re.toString());
    }

  }
}

