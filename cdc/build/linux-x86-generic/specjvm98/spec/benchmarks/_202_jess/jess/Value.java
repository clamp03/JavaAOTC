// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Value.java
// A typed value like an atom, a number, etc.
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////


package spec.benchmarks._202_jess.jess;

/**
 A class to represent a Jess typed value
 Does some 'type conversions'
@author E.J. Friedman-Hill (C)1996
*/

public final class Value {
  private final int STRING_TYPES = RU.ATOM | RU.STRING |
                                   RU.VARIABLE | RU.MULTIVARIABLE |
                                   RU.SLOT | RU.MULTISLOT;
  private final int NUM_TYPES = RU.INTEGER | RU.FLOAT;
  private final int FACT_TYPES = RU.FACT_ID | NUM_TYPES;

  private int             _type;
  private int            intval;
  private double       floatval;
  private Object      Objectval;

  public Value(int value, int type) throws ReteException {
    _type = type;
    switch (_type) {
    case RU.ATOM:
    case RU.SLOT:
    case RU.MULTISLOT:
    case RU.NONE:
    case RU.STRING:
    case RU.INTEGER:
    case RU.FACT_ID:
    case RU.MULTIVARIABLE:
    case RU.VARIABLE:
    case RU.DESCRIPTOR:
      intval = value; break;

    default:
      throw new ReteException("Value::Value",
                              "Not an integral type",
                              "type = " + _type);
    }
  }
  
  public Value(Value v) {
    _type = v._type;
    intval = v.intval;
    floatval = v.floatval;
    Objectval = v.Objectval;
  }

  public Value(String s, int type) throws ReteException {
    if ((type & STRING_TYPES) == 0)
      throw new ReteException("Value::Value",
                              "not a string type",
                              "type = " + _type);

    _type = type;
    intval = RU.putAtom(s);
  }

  public Value(ValueVector f, int type) throws ReteException {
    if (type != RU.FUNCALL && type != RU.ORDERED_FACT &&
        type != RU.UNORDERED_FACT && type != RU.LIST)
      throw new ReteException("Value::Value",
                              "not a vector type",
                              "type = " + _type);

    _type = type;
    Objectval = f;
  }

  public Value(double d, int type) throws ReteException {
    if (type != RU.FLOAT && type != RU.FACT_ID)
      throw new ReteException("Value::Value",
                              "not a float type:",
                              "type = " + _type);
    _type = type;
    intval = (int) (floatval = d);
  }

  public Value(Object o, int type) throws ReteException {
    if (type != RU.EXTERNAL_ADDRESS)
      throw new ReteException("Value::Value",
                              "Not an External Address type",
                              "type = " + _type);

    _type = type;
    Objectval = o;
  }

  public Value(int[] a, int type) throws ReteException {
    if (type != RU.INTARRAY)
      throw new ReteException("Value::Value",
                              "Not an int[] type",
                              "type = " + _type);
    _type = type;
    Objectval = a;
  }

  public int[] IntArrayValue() throws ReteException {
    if (_type == RU.INTARRAY)
      return (int[]) Objectval;
    throw new ReteException("Value::IntArrayValue",
                            "Not an int[]: " + toString(), 
                            "type = " + _type);

  }

  public Object ExternalAddressValue() throws ReteException {
    if (_type == RU.EXTERNAL_ADDRESS)
      return Objectval;
    throw new ReteException("Value::ExternalAddressValue",
                            "Not an external address: " + toString(),
                            "type = " + _type);

  }


  public ValueVector FuncallValue() throws ReteException {
    if (_type == RU.FUNCALL)
      return (ValueVector) Objectval;
    throw new ReteException("Value::FuncallValue",
                            "Not a Funcall: " + toString(),
                            "type = " + _type);
  }

  public ValueVector FactValue() throws ReteException {
    if (_type == RU.ORDERED_FACT || _type == RU.UNORDERED_FACT)
      return (ValueVector) Objectval;
    throw new ReteException("Value::FactValue",
                            "Not a Fact: " + toString(),
                            "type = " + _type);


  }

  public ValueVector ListValue() throws ReteException {
    if (_type == RU.LIST)
      return (ValueVector) Objectval;
    throw new ReteException("Value::ListValue",
                            "Not a List: " + toString(), 
                            "type = " + _type);


  }

  public double NumericValue() throws ReteException {
    if (_type == RU.FLOAT)
      return floatval;
    else if (_type == RU.INTEGER)
      return (double) intval;

    throw new ReteException("Value::NumericValue",
                            "Not a number: " + toString(),
                            "type = " + _type);
      
  }


  public int DescriptorValue() throws ReteException {
    if (_type == RU.DESCRIPTOR)
      return intval;

    throw new ReteException("Value::DescriptorValue",
                            "Not a descriptor: " + toString(), 
                            "type = " + _type);
  }

  public int IntValue() throws ReteException {
    return (int) NumericValue();
  }

  public double FloatValue() throws ReteException {
    return (double) NumericValue();
  }

  public String StringValue() throws ReteException {
    if ((_type & STRING_TYPES) != 0)
      return RU.getAtom(intval);
    throw new ReteException("Value::StringValue",
                            "Not a string: " + toString(),
                            "type = " + _type);
  }

  public int AtomValue() throws ReteException {
    if ((_type & STRING_TYPES) != 0)
      return intval;
    throw new ReteException("Value::AtomValue",
                            "Not an atom: " + toString(),
                            "type = " + _type);
  }

  public int VariableValue() throws ReteException {
    if (_type == RU.VARIABLE || _type == RU.MULTIVARIABLE)
      return intval;
    throw new ReteException("Value::VariableValue",
                            "Not a Variable: " + toString(),
                            "type = " + _type);
  }

  public int FactIDValue() throws ReteException {
    if ((_type & FACT_TYPES) != 0)
      return intval;
    throw new ReteException("Value::FactIDValue",
                            "Not a Fact-ID: " + toString(),
                            "type = " + _type);
  }


  public String toString() {
    switch (_type) {
    case RU.INTEGER:
      return "" + intval;
    case RU.FLOAT:
      return "" + floatval;
    case RU.STRING:
    case RU.ATOM:
    case RU.SLOT:
    case RU.MULTISLOT:
      return RU.getAtom(intval);
    case RU.FACT_ID:
      return "<Fact-" + intval + ">";
    case RU.VARIABLE:
      return "?" + RU.getAtom(intval);
    case RU.MULTIVARIABLE:
      return "$?" + RU.getAtom(intval);
    case RU.DESCRIPTOR:
      return (intval == RU.ORDERED_FACT) ? "ordered" : "unordered";
    case RU.FUNCALL:
    case RU.LIST:
    case RU.UNORDERED_FACT:
    case RU.ORDERED_FACT:
      return Objectval.toString();
    case RU.INTARRAY: {
      int[] binding = (int[]) Objectval;
      return "?" + binding[0] + "," + binding[1] + "," + binding[2];
    }
    case RU.NONE:
      return "(NONE)";
    default:
      return "?";
    }
  }


  public int SortCode() {
    switch (_type) {
    case RU.INTEGER:
    case RU.STRING:
    case RU.ATOM:
    case RU.SLOT:
    case RU.MULTISLOT:
    case RU.FACT_ID:
    case RU.VARIABLE:
    case RU.MULTIVARIABLE:
    case RU.DESCRIPTOR:
      return intval;
    case RU.FLOAT:
      return (int) floatval;
    case RU.FUNCALL:
    case RU.UNORDERED_FACT:
    case RU.ORDERED_FACT:
      return ((ValueVector)Objectval).get(0).intval;
    case RU.LIST:
      return ((ValueVector)Objectval).get(0).SortCode();
    default:
      return 0;
    }
  }



  public int type() {
    return _type;
  }

  final int ITYPES = RU.STRING | RU.ATOM | RU.SLOT | RU.MULTISLOT |
  RU.FACT_ID | RU.VARIABLE | RU.MULTIVARIABLE | RU.DESCRIPTOR | RU.INTEGER;

  final int VTYPES = RU.FUNCALL | RU.ORDERED_FACT | RU.UNORDERED_FACT |
  RU.LIST;

  final public boolean equals(Value v) {

    if (this == v)
     return true;

    int t = _type;
    if (v._type != t)
      return false;

    if ((t & ITYPES) != 0)
      return (intval == v.intval);

    if (t == RU.FLOAT)
      return (floatval == v.floatval);

    return Objectval.equals(v.Objectval);

  }
  
}


