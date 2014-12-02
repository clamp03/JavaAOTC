// -*- java -*-
//////////////////////////////////////////////////////////////////////
// MiscFunctions.java
// Example user-defined functions for the Jess Expert System Shell
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;
import java.io.*;
///import java.awt.*;

/**
  Some small sample user-defined miscellaneous functions
  
  To use one of these functions from Jess, simply register the
  package class in your Java mainline:

  engine.AddUserpackage(new MiscFunctions());

@author E.J. Friedman-Hill (C)1997
*/


public class MiscFunctions implements Userpackage {

  public void Add(Rete engine) {
    engine.AddUserfunction(new batch());
    engine.AddUserfunction(new _system());
    engine.AddUserfunction(new loadpkg());
    engine.AddUserfunction(new loadfn());
    engine.AddUserfunction(new time());
    // setgen added by Win Carus (9.19.97)
    engine.AddUserfunction(new setgen());
  }
}

class batch implements Userfunction {

  int _name = RU.putAtom("batch");
  public int name() { return _name; }
  
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    String filename = vv.get(1).StringValue();
    Value v = Funcall.FALSE();
    try
      {
        FileInputStream fis = new FileInputStream(filename);
        if (fis != null)
          {
            Jesp j = new Jesp(fis, context.engine());
            do
              {
                v = j.parse(false);
              }
            while (fis.available() > 0);
          }
        fis.close();
      }
    catch (IOException ex)
      {
        throw new ReteException("batch", "I/O Exception on file", "");
      }
    return v;
  }
}
class _system implements Userfunction {

  int _name = RU.putAtom("system");
  public int name() { return _name; }
  
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    String command = "";
    try
      {
        String[] words = new String[vv.size() - 1];
        for (int i=1; i<vv.size(); i++)
          words[i-1] = vv.get(i).toString();
        Runtime.getRuntime().exec(words);
        return Funcall.TRUE();
      }
    catch (IOException ioe)
      {
        throw new ReteException("system", "I/O Exception", command);
      }
    catch (SecurityException se)
      {
        throw new ReteException("system", "Security Exception", command);
      }
  }
}

class loadpkg implements Userfunction {

  int _name = RU.putAtom("load-package");
  public int name() { return _name; }
  
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    String clazz = vv.get(1).StringValue();
    try
      {
        Userpackage up = (Userpackage) Class.forName(clazz).newInstance();
        context.engine().AddUserpackage(up);
      }
    catch (ClassNotFoundException cnfe)
      {
        throw new ReteException("load-package", "Class not found", clazz);
      }
    catch (IllegalAccessException iae)
      {
        throw new ReteException("load-package", "Class is not accessible",
                                clazz);
      }
    catch (InstantiationException ie)
      {
        throw new ReteException("load-package", "Class cannot be instantiated",
                                clazz);
      }
    catch (ClassCastException cnfe)
      {
        throw new ReteException("load-package",
                                "Class must inherit from UserPackage", clazz);
      }
    return Funcall.TRUE();
  }
}

class loadfn implements Userfunction {

  int _name = RU.putAtom("load-function");
  public int name() { return _name; }
  
  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    String clazz = vv.get(1).StringValue();
    try
      {
        Userfunction uf = (Userfunction) Class.forName(clazz).newInstance();
        context.engine().AddUserfunction(uf);
      }
    catch (ClassNotFoundException cnfe)
      {
        throw new ReteException("load-function", "Class not found", clazz);
      }
    catch (IllegalAccessException iae)
      {
        throw new ReteException("load-function", "Class is not accessible",
                                clazz);
      }
    catch (InstantiationException ie)
      {
        throw new ReteException("load-function",
                                "Class cannot be instantiated",
                                clazz);
      }
    catch (ClassCastException cnfe)
      {
        throw new ReteException("load-function",
                                "Class must inherit from UserFunction", clazz);
      }
      
    return Funcall.TRUE();
  }
}

class time implements Userfunction
{
    int _name = RU.putAtom( "time" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( System.currentTimeMillis()/1000, RU.FLOAT );
    }
}

class setgen implements Userfunction
{
    int _name = RU.putAtom( "setgen" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        RU.gensym_idx = vv.get( 1 ).IntValue( );
        return Funcall.TRUE( );
    }
}

