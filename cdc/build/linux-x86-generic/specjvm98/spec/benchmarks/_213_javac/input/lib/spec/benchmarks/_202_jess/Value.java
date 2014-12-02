// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Value.java
// A typed value like an atom, a number, etc.
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// TEST ONLY - NOT FOR DISTRIBUTION
//////////////////////////////////////////////////////////////////////
package spec.benchmarks._202_jess;

/**
 A tiny class to represent a CLIPS typed value
@author E.J. Friedman-Hill (C)1996
*/

public class Value {
  private int             _type;
  private int            intval;
  private double       floatval;
  private Object      Objectval;

  public Value(int value, int type) throws ReteException {
    _type = type;
    switch (_type) {
    case RU.ATOM:
    case RU.NONE:
    case RU.STRING:
    case RU.INTEGER:
    case RU.FACT_ID:
    case RU.VARIABLE:
    case RU.DESCRIPTOR:
      intval = value; break;

    default:
      throw new ReteException("Value::Value",
                              "Not an integral type",
                              "type = " + _type);
    }
  }
  
  public Value(String s, int type) throws ReteException {
    if (type != RU.ATOM && type != RU.STRING && type != RU.VARIABLE)
      throw new ReteException("Value::Value",
                              "type = " + _type,
                              "not a string type");
    _type = type;
    intval = RU.putAtom(s);
  }

  public Value(ValueVector f, int type) throws ReteException {
    if (type != RU.FUNCALL && type != RU.ORDERED_FACT &&
        type != RU.UNORDERED_FACT && type != RU.LIST)
      throw new ReteException("Value::Value",
                              "type = " + _type,
                              "not a vector type");
    _type = type;
    Objectval = f;
  }

  public Value(double d, int type) throws ReteException {
    if (type != RU.FLOAT)
      throw new ReteException("Value::Value",
                              "type = " + _type,
                              "not a floating type");
    _type = type;
    floatval = d;
  }

  public Value(Object o, int type) throws ReteException {
    if (type != RU.EXTERNAL_ADDRESS)
      throw new ReteException("Value::Value",
                              "Not an External Address",
                              "type = " + _type);

    _type = type;
    Objectval = o;
  }

  public Value(int[] a, int type) throws ReteException {
    if (type != RU.INTARRAY)
      throw new ReteException("Value::Value",
                              "Not an int[]",
                              "type = " + _type);
    _type = type;
    Objectval = a;
  }

  public int[] IntArrayValue() throws ReteException {
    if (_type == RU.INTARRAY)
      return (int[]) Objectval;
    throw new ReteException("Value::IntArrayValue",
                            "Not an int[]",
                            "type = " + _type);

  }

  public Object ExternalAddressValue() throws ReteException {
    if (_type == RU.EXTERNAL_ADDRESS)
      return Objectval;
    throw new ReteException("Value::ExternalAddressValue",
                            "Not an external address",
                            "type = " + _type);

  }


  public ValueVector FuncallValue() throws ReteException {
    if (_type == RU.FUNCALL)
      return (ValueVector) Objectval;
    throw new ReteException("Value::FuncallValue",
                            "Not a Funcall",
                            "type = " + _type);
  }

  public ValueVector FactValue() throws ReteException {
    if (_type == RU.ORDERED_FACT || _type == RU.UNORDERED_FACT)
      return (ValueVector) Objectval;
    throw new ReteException("Value::FactValue",
                            "Not a Fact",
                            "type = " + _type);


  }

  public ValueVector ListValue() throws ReteException {
    if (_type == RU.LIST)
      return (ValueVector) Objectval;
    throw new ReteException("Value::ListValue",
                            "Not a List",
                            "type = " + _type);


  }

  public double NumericValue() throws ReteException {
    if (_type == RU.FLOAT)
      return floatval;
    else if (_type == RU.INTEGER)
      return (double) intval;

    throw new ReteException("Value::NumericValue",
                            "Not a number",
                            "type = " + _type);
      
  }


  public int DescriptorValue() throws ReteException {
    if (_type == RU.DESCRIPTOR)
      return intval;

    throw new ReteException("Value::DescriptorValue",
                            "Not a descriptor",
                            "type = " + _type);
  }

  public int IntValue() throws ReteException {
    if (_type == RU.INTEGER)
      return intval;

    throw new ReteException("Value::IntValue",
                            "Not an integer",
                            "type = " + _type);
  }

  public double FloatValue() throws ReteException {
    if (_type == RU.FLOAT)
      return floatval;

    throw new ReteException("Value::FloatValue",
                            "Not a float",
                            "type = " + _type);
  }

  public String StringValue() throws ReteException {
    if (_type == RU.ATOM || _type == RU.STRING || _type == RU.VARIABLE)
      return RU.getAtom(intval);
    throw new ReteException("Value::StringValue",
                            "Not a string",
                            "type = " + _type);
  }

  public int AtomValue() throws ReteException {
    if (_type == RU.ATOM || _type == RU.STRING || _type == RU.VARIABLE)
      return intval;
    throw new ReteException("Value::AtomValue",
                            "Not an atom",
                            "type = " + _type);
  }

  public int VariableValue() throws ReteException {
    if (_type == RU.VARIABLE )
      return intval;
    throw new ReteException("Value::VariableValue",
                            "Not a Variable!",
                            "type = " + _type);
  }

  public int FactIDValue() throws ReteException {
    if (_type == RU.FACT_ID )
      return intval;
    throw new ReteException("Value::FactIDValue",
                            "Not a Fact-ID!",
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
      return RU.getAtom(intval);
    case RU.FACT_ID:
      return "<Fact-" + intval + ">";
    case RU.VARIABLE:
      return "?" + RU.getAtom(intval);
    case RU.DESCRIPTOR:
      return (intval == RU.ORDERED_FACT) ? "ordered" : "unordered";
    case RU.FUNCALL:
      return "FUNCALL";
    case RU.LIST:
      return "LIST";
    case RU.UNORDERED_FACT:
      return "UNORDERED FACT";
    case RU.ORDERED_FACT:
      return "ORDERED FACT";
    case RU.INTARRAY: {
      int[] binding = (int[]) Objectval;
      return "?" + binding[0] + "," + binding[1];
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
    case RU.FACT_ID:
    case RU.VARIABLE:
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


  final public boolean equals(Value v) {
    if (v._type != _type)
      return false;
    switch (_type) {
    case RU.INTEGER:
    case RU.STRING:
    case RU.ATOM:
    case RU.FACT_ID:
    case RU.VARIABLE:
    case RU.DESCRIPTOR:
      return (intval == v.intval);
    case RU.FLOAT:
      return (floatval == v.floatval);
    case RU.FUNCALL:
    case RU.UNORDERED_FACT:
    case RU.ORDERED_FACT:
    case RU.LIST:
      return ((ValueVector)Objectval).equals((ValueVector)v.Objectval);
    }
    return false;
  }
  
}


