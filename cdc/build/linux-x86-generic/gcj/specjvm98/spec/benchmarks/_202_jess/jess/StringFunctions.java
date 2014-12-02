// -*- java -*-
//////////////////////////////////////////////////////////////////////
// StringFunctions.java
// Example user-defined functions for the Jess Expert System Shell
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

/**
  Some small sample user-defined string functions

  To use one of these functions from Jess, simply register the
  package class in your Java mainline:

  engine.AddUserpackage(new StringFunctions());

@author E.J. Friedman-Hill (C)1997
*/

public class StringFunctions implements Userpackage
{

    public void Add( Rete engine )
    {
        engine.AddUserfunction( new lowcase( ) );
        engine.AddUserfunction( new strcat( ) );
        engine.AddUserfunction( new strcompare( ) );
        engine.AddUserfunction( new strindex( ) );
        engine.AddUserfunction( new strlength( ) );
        engine.AddUserfunction( new substring( ) );
        engine.AddUserfunction( new upcase( ) );
     }
}


class strcat implements Userfunction
{
    int _name = RU.putAtom( "str-cat" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {

      StringBuffer buf = new StringBuffer( "" );
      
      for ( int i = 1; i < vv.size( ); i++ )
        buf.append( vv.get( i ).toString());
      
      return new Value( buf.toString( ), RU.STRING );
      
    }
}

class upcase implements Userfunction
{
    int _name = RU.putAtom( "upcase" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( vv.get( 1 ).StringValue( ).toUpperCase( ), RU.STRING );
    }
}

class lowcase implements Userfunction
{
    int _name = RU.putAtom( "lowcase" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( vv.get( 1 ).StringValue( ).toLowerCase( ), RU.STRING );
    }
}

class strcompare implements Userfunction
{
    int _name = RU.putAtom( "str-compare" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( vv.get( 1 ).StringValue( ).compareTo( vv.get( 2 ).StringValue( ) ), RU.INTEGER );
    }
}

class strindex implements Userfunction
{
    int _name = RU.putAtom( "str-index" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        int rv = vv.get( 2 ).StringValue( ).indexOf( vv.get( 1 ).StringValue( ) );
        return rv == -1 ? Funcall.FALSE( ) : new Value( rv + 1, RU.INTEGER );
    }
}

class strlength implements Userfunction
{
    int _name = RU.putAtom( "str-length" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( vv.get( 1 ).StringValue( ).length( ), RU.INTEGER );
    }
}

class substring implements Userfunction
{
    int _name = RU.putAtom( "sub-string" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        int begin = (int) vv.get( 1 ).NumericValue( ) -1;
        int end = (int) vv.get( 2 ).NumericValue( );
        String s = vv.get( 3 ).StringValue( );
        if (begin < 0 || begin > s.length() - 1 ||
            end > s.length() || end <= 0)
          throw new ReteException("sub-string",
                                  "Indices must be between 1 and " +
                                  s.length(), "");
        return new Value(s.substring(begin, end), RU.STRING); 
    }
}
