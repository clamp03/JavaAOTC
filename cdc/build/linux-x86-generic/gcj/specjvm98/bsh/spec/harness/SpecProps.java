/*
 * @(#)SpecProps.java	2.1 08/10/98
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

package spec.harness;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import spec.harness.OrderedProperties;

/**
 * This class is used to maintain the SpecJVMClient properties. User can
 * edit these properties. These properties are part of the results sent in 
 * the e-mail format. These properties are used in processing the results. 
 */
public class SpecProps extends Frame {

///////////////////////////////////////
//class variable field declarations
///////////////////////////////////////
private final static Color setupColorDefault = Color.white;

///////////////////////////////////////
//instance variable field declarations
///////////////////////////////////////

private Dialog2		    dialog2;
private boolean		    initialFilesRead = false;
private int				maxRows = 99;
private Properties	    props;
private String		    specDir;
private String		    specFile;
private Properties	    specProps;
private OrderedProperties   title;
private String		    titleFile;
private String		    userFile;
private Color		    setupColor;

///////////////////////////////////////
//constructor declarations
///////////////////////////////////////

/**
Creates a new SPEC properties object, initialized with properties read from
a file or URL "props/spec". The titles of those properties are given in "props/title".
Those files are specified relative to a base directory which is typically the
benchmark installation directory. The user is given an opportunity to override
the default location of those files through a dialog box.
@param baseDir base directory / URL prefix
*/
public SpecProps (String baseDir){
    this (baseDir, setupColorDefault);
}

/**
Creates a new SPEC properties object as above
@param baseDir base directory / URL prefix
@param setupColor background color of the setup screen
*/
public SpecProps (String baseDir, Color setupColor){
    this (  baseDir,
	    "props/spec",
    	    ((Context.getUserPropFile() == null) ?
    		baseDir + "props/user" :
    		Context.getUserPropFile()
	    ),
	    "props/title",
	    setupColor);
}

/**
Creates a new SPEC properties object, initialized with properties and titles
read from files as above.
@param baseDir base directory / URL prefix
@param specFileName Default name of file with inital properties specified by SPEC. Path name is relative to baseDir
@param userFileName full URL of file with inital properties specified by user
@param titleFileName Default name of file with property titles. Path name is relative to baseDir
*/
public SpecProps (String baseDir, String specFileName, String userFileName,
    String titleFileName, Color setupColor)
{
    specDir = baseDir;
    specFile = specFileName;
    userFile = userFileName;
    titleFile = titleFileName;
    this.setupColor = setupColor;
    Properties userProps = getProperties(userFile);
    specProps = merge(specDir + specFile, userProps);
    title = getOrderedProperties(specDir + titleFile);   
    props = (Properties) specProps.clone();
    Locale locale = Locale.getDefault();
    if (getBoolean ("spec.initial.specDates"))
	locale = new Locale(
	    get ("spec.initial.language","en"),
	    get ("spec.initial.country","US"));
    props.put ("spec.test.date",
	DateFormat.getDateInstance (DateFormat.LONG, locale).format (new Date()));
    props.put ("spec.other.testTime",
	DateFormat.getDateTimeInstance().format (new Date()));
    try{
	String s = props.getProperty("spec.initial.maxRows");
	if (s != null)
	    maxRows = Integer.parseInt (s);
    }catch (NumberFormatException e){
	// retain default value as initialized
    }
}

///////////////////////////////////////
//class method declarations
///////////////////////////////////////

/**
 * return an array of strings representing keys for user editable properties
 */
private String[] getEditable(){
    Vector names = new Vector(50);
    for (int i=0; i<title.size(); i++){
	String key = (String) title.keyAt(i);
	if (key.startsWith("spec.client.") || key.startsWith("spec.server.") 
	 || key.startsWith("spec.test"))
            names.addElement(key);
    }
    String[] editable = new String[names.size()];
    for (int i=0; i<editable.length; i++)
        editable[i] = (String) names.elementAt(i);
    return editable;
}

/**
 * Loads the properties from the file given by the user in the first screen.
 * @return Properties class
 * @see Properties
 * @see SpecProps#loadPropertyFile
 */
private static Properties getProperties (String urlString){
    Properties p = new TestProperties();
//spec.harness.Context.out.println( "Loading " +  urlString );   
    loadPropertyFile(p, urlString);
    return p;
}

/**
 * Loads the OrderedProperties from the file given by the user in the first screen.
 * @return OrderedProperties class
 * @see spec.harness.OrderedProperties
 * @see spec.harness.SpecProps#loadPropertyFile
 */
private static OrderedProperties getOrderedProperties (String urlString){
    OrderedProperties p = new OrderedProperties();
//spec.harness.Context.out.println( "Loading " +  urlString );     
    loadPropertyFile(p, urlString);
    return p;
}

/**
 * Loads the propertis from the file given the URL
 * @param urlString URL of the file to be used for loading he properties
 * @param p Properties 
 */
private static void loadPropertyFile(Properties p, String urlString){

    if( urlString == null ) {
	spec.harness.Context.out.println( "Null urlString" );	
	return;
    }
	

    try{
    	URL url = new URL(urlString);
	boolean ok = true;

	int ind = urlString.indexOf("http:" , 0);
//spec.harness.Context.out.println( urlString + "=" + ind);	
	if( ind == 0) {
	    ok = spec.io.FileInputStream.IsURLOk( url );
	}
	
	if( ok ) {	
	    InputStream str = url.openStream();
	    if( str == null ) {
		spec.harness.Context.out.println( "InputStream in loadPropertyFile is null" );
	    } else {   	    
        	p.load( new BufferedInputStream( str ) );
	    }
	} else {
	    spec.harness.Context.out.println( "Could not open URL " + urlString );
	}
	
	
    }catch (Exception e){
        spec.harness.Context.out.println("Error loading property file " + urlString + " : " + e);
//	e.printStackTrace( spec.harness.Context.out );
    }
}

/**
 * Given two Properties objects, forms a single Properties object with 
 * hash values from both the objects.
 */
private static Properties merge(Properties primary, Properties secondary){
    Properties p = (Properties) secondary.clone();
    for (Enumeration e = primary.propertyNames(); e.hasMoreElements();){
        String name = (String) e.nextElement();
        String value = primary.getProperty(name,"");
        p.put(name,value);
    }
    return p;
}

/** 
 * Creates a Properties object from the file name given and merges it with
 * existing Properties object.
 * @return Properties merged properties having the hashvlaues from both the
 * Properties objects
 */
private static Properties merge(String primary, Properties secondary){
    return merge (getProperties(primary), secondary);
}

/*
private static Properties merge(Properties primary, String secondary){
    return merge (primary, getProperties(secondary));
}

private static Properties merge(String primary, String secondary){
    return merge (getProperties(primary), getProperties(secondary));
}
*/

///////////////////////////////////////
//instance method declarations
///////////////////////////////////////

/**
 Get the property value associated with the key.
 @param key key value
 @return property value (may be null if property does not exist)
 Note that both key and value are Strings, not general Objects
 @see java.util.Hashtable#get
 */
public String get (String key){
    return (String) props.get(key);
}

/**
 Get the property value associated with the key,
 or if there is none then return the supplied default value
 @param key key value
 @param def default value
 @return property value or def if property does not exist
 */
public String get (String key, String def){
    String s = (String) props.get(key);
    if (s == null)
	return def;
    else
	return s;
}

/**
 Get the boolean property value associated with the key.
 @param key key value
 @return property value (false if property does not exist)
 Note that key is a Strings, not general Objects
 */
public boolean getBoolean (String key){
    String tf = (String) props.get (key);
    return tf != null && tf.equalsIgnoreCase ("true");
}

/**
 Get the java.util.Properties (all of them)
 @return properties
 */
public Properties getProperties (){
    return props;
}

/**
 Get the title property value associated with the key.
 @param key key value
 @return property value (may be null if property does not exist)
 Note that both key and value are Strings, not general Objects
 @see java.util.Hashtable#get
 */
public String getTitle (String key){
    return (String) title.get(key);
}


/**
 Checks whether the user did the setup atleast once. The report can't be
 generated before doing the setup atleast once. User can alter the properties
 of the JVM client, Hardware, e-mail address to send the report during the 
 Setup process.
 */
public boolean isSetup(){
    if (dialog2 == null)
	return false;
    else
	return dialog2.statusOK;
}

/**
 Lists all the properties to the output stream provided
 * @param out Outputput stream.
 */
 
public void list (PrintStream out){
    props.list (out);
}

public boolean parametersChosen(){
    return true; 
}

/** 
 Adds the Key and Hash to the Properties object
 @param key Key
 @param value value of the key
 */
public void put (String key, String value){
//spec.harness.Context.out.println( "SpecProps PUT >>>" + key + "=" + value + "<<<" );
    props.put (key, value);
}

/** 
 * Shows the Setup dialog. User can edit the properties. 
 */
public void setup(){
    if (! initialFilesRead){  
	props = merge(get("spec.initial.clientFile"),props);
	props = merge(get("spec.initial.serverFile"),props);
	props = merge(specProps,props);
	initialFilesRead = true;
    }
    if (dialog2 == null)
	dialog2 = new Dialog2 ("edit reporting fields",
	    title, props, getEditable(), 30, maxRows, setupColor);
    dialog2.show();
}

/**
 Overriden toString() method
 */
public String toString(){
    StringBuffer buf = new StringBuffer();
    for (Enumeration e = props.keys(); e.hasMoreElements();){
        String name = (String) e.nextElement();
        String value = (String) props.get(name);
	if (value == null) value = "";
	char[] chars = value.toCharArray();
        buf.append (name + "=");
	for (int i=0; i < chars.length; i++)
	    if (chars[i] == '\n')
		buf.append ("\\n");
	    else
		buf.append (chars[i]);
	buf.append ('\n');
//spec.harness.Context.out.println( "SpecProps >>>" + name + "=" + value + "<<<" );
    }
    return buf.toString();
}

// End class SpecProps
}


class TestProperties extends Properties {
    public Object put( Object key, Object value ) {
	String k = (String)key;
	if( !k.startsWith( "spec." ) ) {
//	     spec.harness.Context.out.println( "TestProperties: Funny entry Ignored >>>" + key + "=" + value + "<<<" );  
	     return null;	     
	} else {	
//	    spec.harness.Context.out.println( "TestProperties PUT >>>" + key + "=" + value + "<<<" );    
	    return super.put( key, value );
	}
    }
}

