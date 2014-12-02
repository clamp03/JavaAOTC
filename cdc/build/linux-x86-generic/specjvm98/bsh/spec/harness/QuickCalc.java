/*
 * @(#)QuickCalc.java	1.2 07/13/98
 * Calculate SPEC ratios to give quick look at a run without having
 * to do the reporting page generation
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

package spec.harness;

import java.text.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

public class QuickCalc{

///////////////////////////////////////
//class variable field declarations
///////////////////////////////////////
private static String[] benchmarks = {
    "_227_mtrt",
    "_202_jess",
    "_201_compress",
    "_209_db",
    "_222_mpegaudio",
    "_228_jack",
    "_213_javac",
};

private static DecimalFormat[] formats = {
    new DecimalFormat ("#"),
    new DecimalFormat ("##.0"),
    new DecimalFormat ("#.00"),
    new DecimalFormat ("#.000"),
    new DecimalFormat ("#.0000"),
};

private final static int BENCH_WIDTH = 18;	// print in # columns
private final static int digits = 3;
private final static String pad = "                             ";

///////////////////////////////////////
//instance variable field declarations
///////////////////////////////////////
private double[]	best;
private String		calcError;
private double		gmBest = 1;
private double		gmWorst = 1;
private int		nRun = 0;
private double[]	reftimes;
private Hashtable	results;
private boolean[]	valids;
private double[]	worst;

///////////////////////////////////////
//constructor declarations
///////////////////////////////////////
public QuickCalc (SpecProps props, Hashtable rawResults){
    results = new Hashtable();
    init (props, rawResults);
}

///////////////////////////////////////
//class method declarations
///////////////////////////////////////
public static String format (double x){
    int decimals = digits - (int) (Math.log(x) / Math.log(10) +3) + 2;
    if (decimals < 0)
        decimals = 0;
    else if (decimals >= formats.length)
        decimals = formats.length - 1;
    if (x > 0)
        return formats[decimals].format (x);
    else
        return "n/a";
}

///////////////////////////////////////
//instance method declarations
///////////////////////////////////////
/*
 * Format results for a particular benchmark from our hash table
 * into a string buffer
 */
private void formatResult (int b, StringBuffer buf){
    buf.append (benchmarks[b] + pad.substring (0,BENCH_WIDTH -
        benchmarks[b].length()) + "\t");
    if (worst[b] == 0)
        buf.append ("(size 100 not run)");
    else{
        double bestRatio = reftimes[b] / best[b];
        double worstRatio = reftimes[b] / worst[b];
	buf.append (format (worstRatio));
	buf.append ("\t");
	buf.append (format (bestRatio));
	gmBest *= bestRatio;
	gmWorst *= worstRatio;
    }
    if (!valids[b])
        buf.append (" **invalid run");
    buf.append ("\n");
}

/*
 * Get all run results for a particular benchmark
 * from our private hash table
 */
private void getRuns (int b) throws NumberFormatException {
    String prefix = "spec.results." + benchmarks[b] + ".run";
    valids[b] = true;
    for (int j=0; ; j++){
	String speed = (String) results.get (prefix + j + ".speed");
	String time = (String) results.get (prefix + j + ".time");
	String valid = (String) results.get (prefix + j + ".valid");
	String error = (String) results.get (prefix + j + "validity.error01");
	if (speed == null || time == null || valid == null)
	    break;
	if (! speed.equals ("100"))
	    break;
	if (valid.equals ("false") || error != null)
	    valids[b] = false;
	if (worst[b] == 0)	// first occurence of this benchmark
	    nRun++;
	double t = Double.valueOf (time).doubleValue();
	if (t < best[b])
	    best[b] = t;
	if (t > worst[b])
	    worst[b] = t;
    }
}

/*
 * Copy potentially useful values from the complicated properties and
 * results hash table used by the harness into a simple hash table
 */
private void init (SpecProps props, Hashtable rawResults){
    reftimes =	new double [benchmarks.length];
    valids =	new boolean [benchmarks.length];
    best =	new double [benchmarks.length];
    worst =	new double [benchmarks.length];
    try{
	for (Enumeration e = rawResults.elements() ; e.hasMoreElements() ; ){
	    Properties pp = (Properties)e.nextElement();
	    for( Enumeration f = pp.keys() ; f.hasMoreElements() ; ) {
  		String key = (String)f.nextElement();
		if (key.endsWith (".time")
		 || key.endsWith (".speed")
		 || key.endsWith (".valid")
		 || key.endsWith (".error01")){
		    results.put (key, pp.get (key));
		}
	    }
	}
        for (int i=0; i < benchmarks.length; i++){
	    String ref = props.get ("spec.report." + benchmarks[i] + ".ref","");
	    reftimes[i] = Double.valueOf (ref).doubleValue();
	    best[i] = Double.MAX_VALUE;
	    worst[i] = 0;
	    getRuns (i);
        }
    }catch (NumberFormatException e){
	calcError = "Error: cannot calculate ratios for these results";
    }
}

public String toString(){
    if (calcError != null)
        return calcError;
    if (nRun <= 0)
	return "No size 100 runs have been completed";
    StringBuffer buf = new StringBuffer ("SPEC ratios thus far, " +
        "slowest / fastest\n");
    for (int i=0; i < benchmarks.length; i++)
	formatResult (i, buf);
    gmBest = Math.pow (gmBest, (1.0D / nRun));
    gmWorst = Math.pow (gmWorst, (1.0D / nRun));
    buf.append ("Geometric Mean    " + "\t" );
    buf.append (format (gmWorst) + "\t");
    buf.append (format (gmBest) + "\n\n");
    buf.append(
        "These ratios are provided only for your convenience in viewing results\n" + 
        "prior to mailing. This is NOT a substitute for the SPEC reporting page.\n" +
        "See the disclosure requirements in the run rules.");
    return buf.toString();
}

}//end class

