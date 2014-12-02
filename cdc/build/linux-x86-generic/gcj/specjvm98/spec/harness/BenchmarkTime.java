/*
 * @(#)BenchmarkTime.java	1.4 06/17/98
 * Walter Bays
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

package spec.harness;

/**
 This class is used for storing the benchmark run time information. The harness
 collects the benchmark run data like Runtime, IOTime etc. The time is calculated
 by taking the time samples at the starting and ending of the benchamrk. The 
 difference between the two values gives the time spent in benchmark in milli
 seconds. This class becomes handy in storing the time and converting the
 time to seconds  
 */
public class BenchmarkTime{

///////////////////////////////////////
//class variable field declarations
///////////////////////////////////////

///////////////////////////////////////
//instance variable field declarations
///////////////////////////////////////
/**
 Time in milli seconds
 */
private long		millis;

/**
 Flag indicating whether the time is a valid time or not
 */
public boolean		valid = true;

///////////////////////////////////////
//constructor declarations
///////////////////////////////////////
public BenchmarkTime (long millis){
    if (millis <= 0)
	millis = 1;
    this.millis = millis;
}

///////////////////////////////////////
//class method declarations
///////////////////////////////////////

///////////////////////////////////////
//instance method declarations
///////////////////////////////////////
/**
 Returns the time in milliseconds
 */
public long millis(){
    return millis;
}

/**
 Returns the time in seconds
 */
public double secs(){
    return millis / 1000.0;
}

/**
 Returns the time in seconds in formated way
  Eg.  28.6 secs
	   30.6 secs : **NOT VALID**
 */
public String toLongString(){
    return secs() + " secs" + ((valid)?"":" **NOT VALID**");
}

/**
 Converts the time in seconds into string without any special formatting
 */
public String toString(){
    return Double.toString (secs());
}

}//end class
