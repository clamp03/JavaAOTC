// -*- java -*-
//////////////////////////////////////////////////////////////////////
// MathFunctions.java
// Example user-defined functions for the Jess Expert System Shell
//
// (C) 1997 Win Carus (Win_Carus@inso.com)
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess.jess;

/**
  Some small sample user-defined math functions

  To use one of these functions from Jess, simply register the
  package class in your Java mainline:

  engine.AddUserpackage(new MathFunctions());

@author Win Carus (C)1997
*/

public class MathFunctions implements Userpackage
{
    public void Add( Rete engine )
    {
        // abs added by Win Carus (9.19.97)
        engine.AddUserfunction( new abs( ) );
        // div added by Win Carus (9.19.97)
        engine.AddUserfunction( new div( ) );
        // float added by Win Carus (9.19.97)
        engine.AddUserfunction( new _float( ) );
        // integer added by Win Carus (9.19.97)
        engine.AddUserfunction( new _integer( ) );
        // max added by Win Carus (9.19.97)
        engine.AddUserfunction( new max( ) );
        // min added by Win Carus (9.19.97)
        engine.AddUserfunction( new min( ) );
        // ** added by Win Carus (9.19.97)
        engine.AddUserfunction( new expt( ) );
        // exp added by Win Carus (9.19.97)
        engine.AddUserfunction( new exp( ) );
        // log added by Win Carus (9.19.97)
        engine.AddUserfunction( new log( ) );
        // log10 added by Win Carus (9.19.97)
        engine.AddUserfunction( new log10( ) );
        // pi added by Win Carus (9.19.97)
        engine.AddUserfunction( new pi( ) );
        // e added by Win Carus (9.19.97)
        // Note : this is NOT a part of standard CLIPS.
        engine.AddUserfunction( new e( ) );
        // round added by Win Carus (9.19.97)
        engine.AddUserfunction( new round( ) );
        // sqrt added by Win Carus (9.19.97)
        engine.AddUserfunction( new sqrt( ) );
        // random added by Win Carus (9.19.97)
        engine.AddUserfunction( new random( ) );
        // TBD : rad-deg
        // engine.AddUserfunction( new rad-deg( ) );
        // TBD : deg-rad
        // engine.AddUserfunction( new deg-rad( ) );
        // TBD : grad-deg
        // engine.AddUserfunction( new grad-deg( ) );
        // TBD : deg-grad
        // engine.AddUserfunction( new deg-grad( ) );
    }
}

class abs implements Userfunction
{
    int _name = RU.putAtom( "abs" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        int type = vv.get( 1 ).type( );
        switch ( type )
        {
            case RU.INTEGER:
                return new Value( Math.abs( vv.get( 1 ).IntValue( ) ), RU.INTEGER );
            case RU.FLOAT:
                return new Value( Math.abs( vv.get( 1 ).FloatValue( ) ), RU.FLOAT );
            default:
                throw new ReteException( "abs", "integer or float required", "" );
        }
    }
}

class div implements Userfunction
{
    int _name = RU.putAtom( "div" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        int first = (int)vv.get( 1 ).NumericValue( );
        int second = (int)vv.get( 2 ).NumericValue( );

        return new Value( ( first / second ), RU.INTEGER );
    }
}

class _float implements Userfunction
{
    int _name = RU.putAtom( "float" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( (float)vv.get( 1 ).NumericValue( ), RU.FLOAT );
    }
}

class _integer implements Userfunction
{
    int _name = RU.putAtom( "integer" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( (int)vv.get( 1 ).NumericValue( ), RU.INTEGER );
    }
}

class max implements Userfunction
{
    int _name = RU.putAtom( "max" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        if ( vv.get( 1 ).type( ) == RU.FLOAT || vv.get( 2 ).type( ) == RU.FLOAT )
        {
            return new Value( Math.max( (float)vv.get( 1 ).NumericValue( ), (float)vv.get( 2 ).NumericValue( ) ), RU.FLOAT );
        }
        else
        {
            return new Value( Math.max( vv.get( 1 ).NumericValue( ), vv.get( 2 ).NumericValue( ) ), RU.INTEGER );
        }
    }
}

class min implements Userfunction
{
    int _name = RU.putAtom( "min" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        if ( vv.get( 1 ).type( ) == RU.FLOAT || vv.get( 2 ).type( ) == RU.FLOAT )
        {
            return new Value( Math.min( (float)vv.get( 1 ).NumericValue( ), (float)vv.get( 2 ).NumericValue( ) ), RU.FLOAT );
        }
        else
        {
            return new Value( Math.min( vv.get( 1 ).NumericValue( ), vv.get( 2 ).NumericValue( ) ), RU.INTEGER );
        }
    }
}

class expt implements Userfunction
{
    int _name = RU.putAtom( "**" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( Math.pow( (float)vv.get( 1 ).NumericValue( ), (float)vv.get( 2 ).NumericValue( ) ), RU.FLOAT );
    }
}

class exp implements Userfunction
{
    int _name = RU.putAtom( "exp" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( Math.pow( Math.E, vv.get( 1 ).NumericValue( ) ), RU.FLOAT );
    }
}

class log implements Userfunction
{
    int _name = RU.putAtom( "log" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( (float)Math.log( (double)vv.get( 1 ).NumericValue( ) ), RU.FLOAT );
    }
}

class log10 implements Userfunction
{
    private static final double log10 = Math.log( 10.0F );

    int _name = RU.putAtom( "log10" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( (float) (Math.log( (double)vv.get( 1 ).NumericValue( ) ) / log10 ), RU.FLOAT );
    }
}

class pi implements Userfunction
{
    int _name = RU.putAtom( "pi" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( Math.PI, RU.FLOAT );
    }
}

class e implements Userfunction
{
    int _name = RU.putAtom( "e" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( Math.E, RU.FLOAT );
    }
}

class round implements Userfunction
{
    int _name = RU.putAtom( "round" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( Math.round( (float)vv.get( 1 ).NumericValue( ) ), RU.INTEGER );
    }
}

class sqrt implements Userfunction
{
    int _name = RU.putAtom( "sqrt" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( Math.sqrt( (float)vv.get( 1 ).NumericValue( ) ), RU.FLOAT );
    }
}

class random implements Userfunction
{
    int _name = RU.putAtom( "random" );
    public int name( ) { return _name; }

    public Value Call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( (int) (Math.random( ) * 65536), RU.INTEGER );
    }
}
