// -*- java -*-
//////////////////////////////////////////////////////////////////////
// PredFunctions.java
// Example user-defined functions for the Jess Expert System Shell
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

/**
  Some small sample user-defined predicate functions
  
  To use one of these functions from Jess, simply register the
  package class in your Java mainline:

  engine.AddUserpackage(new PredFunctions());

@author E.J. Friedman-Hill (C)1997
*/


public class PredFunctions implements Userpackage {

  public void Add(Rete engine) {
    engine.AddUserfunction(new evenp());
    engine.AddUserfunction(new oddp());
    engine.AddUserfunction(new floatp());
    engine.AddUserfunction(new integerp());
    engine.AddUserfunction(new lexemep());
    engine.AddUserfunction(new multifieldp());
    engine.AddUserfunction(new numberp());
    engine.AddUserfunction(new stringp());
    engine.AddUserfunction(new symbolp());
  }
}



class evenp implements Userfunction {

  int _name = RU.putAtom("evenp");
  public int name() { return _name; }
  
  public Value Call(ValueVector vv, Context context) throws ReteException {

      boolean b = ((((int) vv.get(1).NumericValue()) % 2) == 0);
      return b ? Funcall.TRUE() : Funcall.FALSE();
  }
}

class oddp implements Userfunction {

  int _name = RU.putAtom("oddp");
  public int name() { return _name; }
  
  public Value Call(ValueVector vv, Context context) throws ReteException {

      boolean b = ((((int) vv.get(1).NumericValue()) % 2) == 0);
      return b ? Funcall.FALSE() : Funcall.TRUE();
  }
}

// For the moment, the definition of an integer is "rounded version
// equal to original". This is subject to change.

class floatp implements Userfunction {

  int _name = RU.putAtom("floatp");
  public int name() { return _name; }
  
  public Value Call(ValueVector vv, Context context) throws ReteException {

      double rnd = (int) vv.get(1).NumericValue();
      double val = vv.get(1).NumericValue();

      return rnd == val ? Funcall.FALSE() : Funcall.TRUE();
  }
}

class integerp implements Userfunction {

  int _name = RU.putAtom("integerp");
  public int name() { return _name; }
  
  public Value Call(ValueVector vv, Context context) throws ReteException {

      double rnd = (int) vv.get(1).NumericValue();
      double val = vv.get(1).NumericValue();

      return rnd == val ? Funcall.TRUE() : Funcall.FALSE();
  }
}

class lexemep implements Userfunction {

  int _name = RU.putAtom("lexemep");
  public int name() { return _name; }
  
  public Value Call(ValueVector vv, Context context) throws ReteException {

      if (vv.get(1).type() == RU.ATOM ||
          vv.get(1).type() == RU.STRING)
        return Funcall.TRUE();
      else
        return Funcall.FALSE();
  }
}

class multifieldp implements Userfunction {

  int _name = RU.putAtom("multifieldp");
  public int name() { return _name; }
  
  public Value Call(ValueVector vv, Context context) throws ReteException {

      if (vv.get(1).type() == RU.LIST)
        return Funcall.TRUE();
      else
        return Funcall.FALSE();
  }
}

class numberp implements Userfunction {

  int _name = RU.putAtom("numberp");
  public int name() { return _name; }
  
  public Value Call(ValueVector vv, Context context) throws ReteException {
      if (vv.get(1).type() == RU.INTEGER ||
          vv.get(1).type() == RU.FLOAT)
        return Funcall.TRUE();
      else
        return Funcall.FALSE();
  }
}

class stringp implements Userfunction {

  int _name = RU.putAtom("stringp");
  public int name() { return _name; }
  
  public Value Call(ValueVector vv, Context context) throws ReteException {
      if (vv.get(1).type() == RU.STRING)
        return Funcall.TRUE();
      else
        return Funcall.FALSE();
  }
}

class symbolp implements Userfunction {

  int _name = RU.putAtom("symbolp");
  public int name() { return _name; }
  
  public Value Call(ValueVector vv, Context context) throws ReteException {
      if (vv.get(1).type() == RU.ATOM)
        return Funcall.TRUE();
      else
        return Funcall.FALSE();
  }
}





