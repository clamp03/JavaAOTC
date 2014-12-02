/*
 * @(#)QuickSort.java	1.5 06/17/98
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

package spec.harness;

import java.util.*;

/**
 * This class implements the quick sort algorithm. Given the array of
 * strings, this class provides the functions to sort them
 */
public class QuickSort
{
/**
 String array to be sorted
 */
String[] userArray;

/**
 Starts the sorting of strings. 
 @param userArray Array of String objects to be sorted
 */
public void startSort( String[] userArray )
    {
    this.userArray = userArray;
    
    if( !isAlreadySorted() )
        quicksort( 0, userArray.length-1 );
    }

/**
 A recursive routine which sorts the data between two indices given
 @param p Start index
 @param r Stop index
 */
private void quicksort( int p, int r )
    {
    if( p < r )
        {
        int q = partition( p, r );
        
        if( q == r )
            q--;
        
        quicksort( p, q );
        quicksort( q+1, r );
        } 
    }

/**
 Sorts the strings between two indices given
 @param lo Lower index
 @param hi Higher Index
 */
private int partition( int lo, int hi )
    {
    String pivot = userArray[lo];
    
    while (true)
        {
        while( compare( userArray[hi], pivot ) >= 0 && lo < hi )
            hi--;
            
        while( compare( userArray[lo], pivot ) <  0 && lo < hi )
            lo++;
            
        if( lo < hi )
            {
            String      T = userArray[lo];
            userArray[lo] = userArray[hi];
            userArray[hi] = T;
            }
        else
            return hi;            
        } 
    } 

/**
 Checks whether the array is already sorted.
 */
private boolean isAlreadySorted()
    {
    for( int i=1 ; i < userArray.length ; i++ )
        {
        if( compare( userArray[i], userArray[i-1] ) < 0 )
            return false;
        }
        
    return true;    
    }
  
/**
 Compares two strings lexicographically. The comparison is based on the 
 Unicode value of each character in the strings. 
 @param a First string 
 @param b Second string
 @return the value 0 if the argument string is equal to this string; a value 
 less than 0 if this string is lexicographically less than the string argument;
 and a value greater than 0 if this string is lexicographically greater than 
 the string argument. 
 */
public int compare( String a, String b )  
    {
    return a.compareTo( b );
    }

/**
 Sorts the string array passed
 @param args Array of strings to be sorted
 */
public static String[] sort( String[] args )
    {
    QuickSort q = new QuickSort();
    q.startSort( args );
    return q.userArray;
    }


/**
 Tokenizes the given string by looking for '\n' and sorts the resulting string
 array. Constructs back the string object with the sorted entries and returns
 the resultant string object.
 @param text Text to be sorted
 @param comments Flag indicating whether the resulting string should have
 line feeds or not
 */
public static String sort( String text, boolean comments )
    {
    Vector v = new Vector();
    
    for( StringTokenizer st = new StringTokenizer( text, "\n", false ) ; st.hasMoreElements() ; )    
        v.addElement( st.nextElement() );

    String[] sa = new String[v.size()];
    v.copyInto( sa );
    sa = sort( sa );

    StringBuffer sb = new StringBuffer();
    for( int i = 0 ; i < sa.length ; i++ )
        {
	if( comments == true || sa[i].charAt( 0 ) != '#' )
	    {
            sb.append( sa[i] );
	    sb.append( "\n" );        
	    }
        }

    return sb.toString();
    }
    
/**
 Sorts the sub strings (which are seperated by "\n") in a string
 */
public static String sort( String text )
    {    
    return sort( text, true );
    }        
    
}


