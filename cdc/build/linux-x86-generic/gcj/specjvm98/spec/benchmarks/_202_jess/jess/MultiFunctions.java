// -*- java -*-
//////////////////////////////////////////////////////////////////////
// MultiFunctions.java
// Example user-defined functions for the Jess Expert System Shell
// Many functions contributed by Win Carus (Win_Carus@inso.com)
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

import java.io.*;
import java.util.Hashtable;

/**
  Some small sample user-defined multifield functions

  To use one of these functions from Jess, simply register the
  package class in your Java mainline:

  engine.AddUserpackage(new MultiFunctions());

@author E.J. Friedman-Hill (C)1997
*/

public class MultiFunctions implements Userpackage
{
  public void Add(Rete engine)
  {
    engine.AddUserfunction(new createmf());
    engine.AddUserfunction(new deletemf());
    // explode$
    // engine.AddUserfunction(new explodemf());
    engine.AddUserfunction(new firstmf());
    // implode$ added by Win Carus (9.17.97)
    engine.AddUserfunction(new implodemf());
    // insert$ added by Win Carus (9.17.97)
    engine.AddUserfunction(new insertmf());
    engine.AddUserfunction(new lengthmf());
    engine.AddUserfunction(new membermf());
    engine.AddUserfunction(new nthmf());
    // replace$ added by Win Carus (9.17.97)
    engine.AddUserfunction(new replacemf());
    engine.AddUserfunction(new restmf());
    // subseq$ added by Win Carus (9.17.97)
    engine.AddUserfunction(new subseqmf());
    // subsetp added by Win Carus (9.17.97); revised (10.2.97)
    engine.AddUserfunction(new subsetp());
    // union added by Win Carus (10.2.97)
    engine.AddUserfunction(new union());
    // intersection added by Win Carus (10.2.97)
    engine.AddUserfunction(new intersection());
    // complement added by Win Carus (10.2.97)
    engine.AddUserfunction(new complement());
  }
}

class createmf implements Userfunction
{
  int _name = RU.putAtom( "create$" );
  public int name() { return _name; }

  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    ValueVector mf = new ValueVector();

    for ( int i = 1; i < vv.size( ); i++ )
      {
        switch ( vv.get( i ).type( ) )
          {
          case RU.LIST:
            ValueVector list = vv.get( i ).ListValue( );
            for ( int k = 0; k < list.size( ); k++ )
              {
                mf.add( list.get( k ) );
              }
            break;
          default:
            mf.add( vv.get(i ) );
            break;
          }
      }
    return new Value( mf, RU.LIST );
  }
}

class deletemf implements Userfunction
{
  int _name = RU.putAtom( "delete$" );
  public int name( ) { return _name; }

  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    ValueVector newmf = new ValueVector( );

    ValueVector mf = vv.get( 1 ).ListValue( );
    int begin = (int) vv.get( 2 ).NumericValue( );
    int end = (int) vv.get( 3 ).NumericValue( );

    if (end < begin || begin < 1 || end > mf.size())
        throw new ReteException( "delete$",
                                 "invalid range",
                                 "(" + begin + "," + end + ")");      
    for ( int i = 0; i < mf.size( ); i++ )
      {
        if ( i >= (begin-1) && i <= (end-1) )
          {
            continue;
          }
        newmf.add(mf.get(i));
      }

    return new Value( newmf, RU.LIST );
  }
}

class firstmf implements Userfunction
{
  int _name = RU.putAtom( "first$" );
  public int name( ) { return _name; }

  public Value Call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector mf = vv.get( 1 ).ListValue( );
    ValueVector newmf = new ValueVector(1);
    newmf.add(mf.get( 0 ));
    return new Value(newmf, RU.LIST);
  }
}

class implodemf implements Userfunction
{
  int _name = RU.putAtom( "implode$" );
  public int name( ) { return _name; }

  public Value Call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector mf = vv.get( 1 ).ListValue( );

    StringBuffer buf = new StringBuffer( "" );

    for ( int i = 0; i < mf.size( ); i++ )
      {
        buf.append( mf.get( i ).StringValue( ) + " ");
      }

    String result = buf.toString( );
    int len = result.length( );

    if ( len == 0 )
      return new Value( result, RU.STRING );
    else
      return new Value( result.substring( 0, len - 1 ), RU.STRING );
  }
}

class insertmf implements Userfunction
{
  int _name = RU.putAtom( "insert$" );
  public int name( ) { return _name; }

  public Value Call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector mf = vv.get( 1 ).ListValue( );
    int idx = (int) vv.get( 2 ).NumericValue( );
    ValueVector insertedmf = vv.get( 3 ).ListValue( );

    if ( idx < 1 || idx > mf.size( ) + 1 )
      {
        throw new ReteException( "insert$", "index must be >= 1 and <= " + (mf.size( ) + 1), ": " + idx );
      }

    ValueVector newmf = new ValueVector( );
    // adjust for zero indexing
    --idx;
    for ( int i = 0; i < idx; i++ )
      {
        newmf.add( mf.get( i ) );
      }

    for ( int j = 0; j < insertedmf.size( ); j++ )
      {
        newmf.add( insertedmf.get( j ) );
      }

    for ( int i = idx; i < mf.size( ); i++ )
      {
        newmf.add( mf.get( i ) );
      }

    return new Value( newmf, RU.LIST );
  }
}

class nthmf implements Userfunction
{
  int _name = RU.putAtom( "nth$" );
  public int name( ) { return _name; }

  public Value Call( ValueVector vv, Context context ) throws ReteException
  {
    int idx = (int) vv.get( 1 ).NumericValue( );

    if ( idx < 1 )
      {
        throw new ReteException( "nth$", "index must be > 0", "" + idx );
      }
    ValueVector mf = vv.get( 2 ).ListValue( );

    if ( idx > mf.size( ) )
      {
        throw new ReteException( "nth$", "index out of bounds", "" + idx );
      }

    return mf.get( idx - 1 );
  }
}

class lengthmf implements Userfunction
{
  int _name = RU.putAtom( "length$" );
  public int name() { return _name; }

  public Value Call(ValueVector vv, Context context) throws ReteException
  {
    ValueVector mf = vv.get( 1 ).ListValue( );
    return new Value( mf.size( ), RU.INTEGER );
  }
}

class replacemf implements Userfunction
{
  int _name = RU.putAtom( "replace$" );
  public int name( ) { return _name; }

  public Value Call( ValueVector vv, Context context ) throws ReteException
  {

    ValueVector mf = vv.get( 1 ).ListValue( );
    int startIdx = (int) vv.get( 2 ).NumericValue( );
    int endIdx = (int) vv.get( 3 ).NumericValue( );
    ValueVector insertedmf = vv.get( 4 ).ListValue( );

    if ( startIdx < 1 || startIdx > mf.size( ) + 1 ||
         endIdx < 1 || endIdx > mf.size( ) + 1 || startIdx > endIdx )
      {
        throw new ReteException( "replace$", "index must be >= 1 and <= " +
                                 (mf.size( ) + 1),
                                 ": " + startIdx + " " + endIdx);
      }

    ValueVector newmf = new ValueVector( );

    // adjust for 0-based
    --startIdx;
    --endIdx;

    for ( int i = 0; i <= startIdx - 1; i++ )
      {
        newmf.add( mf.get( i ) );
      }
    
    for ( int j = 0; j < insertedmf.size( ); j++ )
      {
        newmf.add( insertedmf.get( j ) );
      }

    for ( int i = endIdx + 1; i < mf.size( ); i++ )
      {
        newmf.add( mf.get( i ) );
      }

    return new Value( newmf, RU.LIST );
  }
}

class restmf implements Userfunction
{
  int _name = RU.putAtom( "rest$" );
  public int name( ) { return _name; }

  public Value Call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector newmf = new ValueVector( );

    ValueVector mf = vv.get( 1 ).ListValue( );

    for ( int i = 1; i < mf.size( ); i++ )
      {
        newmf.add( mf.get( i ) );
      }

    return new Value( newmf, RU.LIST );
  }
}

class membermf implements Userfunction
{
  int _name = RU.putAtom( "member$" );
  public int name( ) { return _name; }

  public Value Call( ValueVector vv, Context context ) throws ReteException
  {
    Value target = vv.get( 1 );
    ValueVector list = vv.get( 2 ).ListValue( );

    for ( int i = 0; i < list.size( ); i++ )
      {
        if ( target.equals( list.get( i ) ) )
          {
            return new Value( i + 1, RU.INTEGER );
          }
      }
    return Funcall.FALSE( );
  }
}

class subseqmf implements Userfunction
{
  int _name = RU.putAtom( "subseq$" );
  public int name( ) { return _name; }

  public Value Call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector mf = vv.get( 1 ).ListValue( );
    int startIdx = (int) vv.get( 2 ).NumericValue( );
    int endIdx = (int) vv.get( 3 ).NumericValue( );

    // Note: multifield indices are 1-based, not 0-based.

    if ( startIdx < 1 )
      startIdx = 1;

    if ( endIdx > mf.size( ) )
      endIdx = mf.size( );

    ValueVector newmf = new ValueVector( );

    if ( startIdx <= mf.size( ) &&
         endIdx <= mf.size( ) &&
         startIdx <= endIdx )
      {
        if ( startIdx == endIdx )
          {
            newmf.add( mf.get( startIdx - 1 ) );
          }
        else
          {
            for ( int i = startIdx; i <= endIdx; ++i )
              {
                newmf.add( mf.get( i - 1 ) );
              }
          }
      }
    return new Value( newmf, RU.LIST );
  }
}

class subsetp implements Userfunction
{
  int _name = RU.putAtom( "subsetp" );
  public int name( ) { return _name; }

  public Value Call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector firstmf = vv.get( 1 ).ListValue( );
    ValueVector secondmf = vv.get( 2 ).ListValue( );

    // if ( the first multifield is empty ) then return TRUE
    if ( firstmf.size( ) == 0 )
      {
        return Funcall.TRUE( );
      }

    // if ( the second multifield is empty and the first is not )
    // then return FALSE.
    if ( secondmf.size( ) == 0 )
      {
        return Funcall.FALSE( );
      }

    // if ( one member in the first multifield is not in the second multifield )
    // then return FALSE else return TRUE
        
    Hashtable ht = new Hashtable( );
    Integer value = new Integer( 0 );

    for ( int i = 0; i < secondmf.size( ); ++i )
      {
        String key = secondmf.get( i ).toString( ) + secondmf.get( i ).type( );
        if ( !ht.containsKey( key ) )
          {
            ht.put( key, value );
          }
      }
        
    for ( int i = 0; i < firstmf.size( ); ++i )
      {
        String key = firstmf.get( i ).toString( ) + firstmf.get( i ).type( );

        if ( !ht.containsKey( key ) )
          {
            return Funcall.FALSE( );
          }
      }
    return Funcall.TRUE( );
  }
}
    
class union implements Userfunction
{
  int _name = RU.putAtom( "union$" );
  public int name( ) { return _name; }

  public Value Call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector firstmf = vv.get( 1 ).ListValue( );
    ValueVector secondmf = vv.get( 2 ).ListValue( );
        
    ValueVector newmf = new ValueVector( );
        
    Hashtable ht = new Hashtable( );
    Integer value = new Integer( 0 );

    for ( int i = 0; i < firstmf.size( ); ++i )
      {
        newmf.add( firstmf.get( i ) );
        // key concatenates the String representation of the element
        // and its type; this would be better handled by overloading
        // the hashCode( ) method for all RU data types
        String key = firstmf.get( i ).toString( ) + firstmf.get( i ).type( );            
        ht.put( key, value );
      }
            
    for ( int i = 0; i < secondmf.size( ); ++i )
      {
        String key = secondmf.get( i ).toString( ) + secondmf.get( i ).type( );            
        if ( !ht.containsKey( key ) )
          {
            newmf.add( secondmf.get( i ) );
          }
      }
    return new Value( newmf, RU.LIST );
  }
}

class intersection implements Userfunction
{
  int _name = RU.putAtom( "intersection$" );
  public int name( ) { return _name; }

  public Value Call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector firstmf = vv.get( 1 ).ListValue( );
    ValueVector secondmf = vv.get( 2 ).ListValue( );
        
    ValueVector newmf = new ValueVector( );
        
    Hashtable ht = new Hashtable( );
    Integer value = new Integer( 0 );

    for ( int i = 0; i < firstmf.size( ); ++i )
      {
        String key = firstmf.get( i ).toString( ) + firstmf.get( i ).type( );            
        ht.put( key, value );
      }
            
    for ( int i = 0; i < secondmf.size( ); ++i )
      {
        String key = secondmf.get( i ).toString( ) + secondmf.get( i ).type( );            
        if ( ht.containsKey( key ) )
          {
            newmf.add( secondmf.get( i ) );
          }
      }
    return new Value( newmf, RU.LIST );
  }
}

class complement implements Userfunction
{
  int _name = RU.putAtom( "complement$" );
  public int name( ) { return _name; }

  public Value Call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector firstmf = vv.get( 1 ).ListValue( );
    ValueVector secondmf = vv.get( 2 ).ListValue( );
        
    ValueVector newmf = new ValueVector( );
        
    Hashtable ht = new Hashtable( );
    Integer value = new Integer( 0 );

    for ( int i = 0; i < firstmf.size( ); ++i )
      {
        String key = firstmf.get( i ).toString( ) + firstmf.get( i ).type( );            
        ht.put( key, value );
      }
            
    for ( int i = 0; i < secondmf.size( ); ++i )
      {
        String key = secondmf.get( i ).toString( ) + secondmf.get( i ).type( );            
        if ( !ht.containsKey( key ) )
          {
            newmf.add( secondmf.get( i ) );
          }
      }
         
    return new Value( newmf, RU.LIST );
  }
}

