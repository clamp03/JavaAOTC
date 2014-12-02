/*
 * @(#)Dialog2Card.java	1.10 06/17/98
 * Walter Bays
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

package spec.harness;

import java.awt.*;
import java.util.Properties;

/**
 * The Dialog2Card is derived from the panel class and holds the visual 
 * components using which the operator can edit the setup parameters. 
 * Dialog2 window is added with the different panels of Dialog2Card type. 
 * The operator can move between the different card using the previous
 * and next button.
 */

public class Dialog2Card extends Panel{

///////////////////////////////////////
//class variable field declarations
///////////////////////////////////////

///////////////////////////////////////
//instance variable field declarations
///////////////////////////////////////
/**
 * The title properties read from the file
 */
Properties titleProps;

/**
 * The title property values read from the property file
 */
Properties valueProps;

/** 
 * Array of Property name strings
 */
String[] propNames;

/** Array of the text fields. The properties are displayed in these test field
 *  objects. 
 */
TextField[] fields;

/**
 * indexFrom reresents the index to the property in the main property 
 * array with which this property page is starting.
 */
int indexFrom;

/**
 * indexTo represents the index to the property in the main property
 * array with which this property page is ending
 */
int indexTo;

/**
 Column width
 */
int colWidth;

/** 
 Inner panel. This the actual panel which holds all the gui components.
 */
Panel innerPanel;

///////////////////////////////////////
//constructor declarations
///////////////////////////////////////
/**
 * The constructor for the Dialog2Card
 * @param t Title properties
 * @param v Property values
 * @param pn Property Names String array
 * @param textFields Textfield array
 * @param iFrom Start index in the main property array
 * @param iTo End index in the main property array
 * @param cw Column width
 */
Dialog2Card(Properties t, Properties v, String[] pn, 
    TextField[] textFields, int iFrom, int iTo, int cw){
    titleProps = t;
    valueProps = v;
    propNames = pn;
    fields = textFields;
    indexFrom = iFrom;
    indexTo = iTo;
    colWidth = cw;
    innerPanel = new Panel();
    innerPanel.setLayout(new GridLayout(0,2));
    for (int i=iFrom; i<iTo; i++){
	String label = titleProps.getProperty(propNames[i]);
	if (label == null) label = "label " + i + " missing";
	innerPanel.add(new Label(label));
	fields[i] = new TextField("uninitialized",colWidth);
	innerPanel.add(fields[i]);
    }
    add (innerPanel);
    resetValues();
}

///////////////////////////////////////
//class method declarations
///////////////////////////////////////
/**
 Adds the components to the inner panel
 */
public void addInner (Component c){
    innerPanel.add (c);
}

/**
 * Resets the values for the properties found in this page.
 */
public void resetValues(){
    for (int i=indexFrom; i<indexTo; i++){
	String value = valueProps.getProperty(propNames[i]);
	if (value == null) value = "";
	if (fields[i] == null)
	    spec.harness.Context.out.println("Error. Text field null: " + i);
	else
	    fields[i].setText(value);
    }
}

///////////////////////////////////////
//instance method declarations
///////////////////////////////////////

}
