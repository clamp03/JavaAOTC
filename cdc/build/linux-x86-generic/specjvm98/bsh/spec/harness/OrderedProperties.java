/*
 * @(#)OrderedProperties.java	1.5 06/17/98
 * Walter Bays
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

package spec.harness;

import java.io.*;
import java.util.Properties;
import java.util.Vector;

/**
 * Ordered properties is a class derived from properties. It simply 
 * extends the functionality of the property class. The property 
 * class puts the properties in a hashtable whose order is not 
 * known. The orderedproperties class maintains a vector
 * of the Hash of the entry put into the hashtable so that the hash
 * table can be retreved in the known order any number of times.
 */
public class OrderedProperties extends Properties{

///////////////////////////////////////
//class variable field declarations
///////////////////////////////////////

///////////////////////////////////////
//instance variable field declarations
///////////////////////////////////////

/**
 * sequence is the vector in which the hash value is put
 */
private Vector sequence = new Vector();

///////////////////////////////////////
//constructor declarations
///////////////////////////////////////

///////////////////////////////////////
//class method declarations
///////////////////////////////////////

///////////////////////////////////////
//instance method declarations
///////////////////////////////////////

/**
 * returns the element at the particular posion of the sequence vector
 */
public Object keyAt (int i){
    return sequence.elementAt(i);
}

/** The load method of the Properties class is overloaded to provide the
 * extra functionality of the sequencing. The key is added to the Vector,
 * every time an entry is put to HashTable
 */ 
public void load (InputStream in) throws IOException{
    DataInputStream din = new DataInputStream(in);
    String line;
    while ((line = din.readLine()) != null){
	if (line.startsWith("#")) continue;
	int i = line.indexOf('=');
	if (i < 0) continue;
	String key = line.substring(0,i);
	String value = line.substring(i+1);
	if (get(key) == null)	// don't duplicate keys in sequence
	    sequence.addElement(key);
	super.put (key,value);	// put initial/updated value in any case
    }
}

/**
 * returns the size of the sequence vector.
 */
public int size(){
    return sequence.size();
}

public Object valueAt (int i){
    return get(sequence.elementAt(i));
}



}

