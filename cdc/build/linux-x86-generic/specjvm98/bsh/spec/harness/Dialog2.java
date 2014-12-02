/*
 * @(#)Dialog2.java	1.11 06/17/98
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
 * Dialog2 is the class which is used to create the Setup parameter
 * window. This windows consists of the editable user parameters
 * like the mailing address of the report. The Hardware details,
 * the JVM details, test specific comments etc. The number of pages
 * in this window is determined by the Window's resultion, The Next and 
 * previous pages can be accessed using 'Next' and 'Previous' buttons.
 */
public class Dialog2 extends Frame{

///////////////////////////////////////
//class variable field declarations
///////////////////////////////////////

/**
 * helvetica12 is the variable used to hold the Helvetica font object
 * created using the size 12 and PLAIM property
 */
Font helvetica12 = new Font ("helvetica",Font.PLAIN,12);

///////////////////////////////////////
//instance variable field declarations
///////////////////////////////////////

/**
 * title variable which holds the title for the dialog
 */
String title;

/**
 * titleProps holds the title Strings
 */
Properties titleProps;

/**
 * titleProps holds the title Strings
 */
Properties valueProps;

/** propNames is an array of Strings which holds the names of the
 * current set of properties 
 */ 
 String[] propNames;

/**
 * fields is an Array of Text fields. These are the edit boxes where
 * user can type in the new value for a particular parameter.
 */
TextField[] fields;

/**
 * colWidth represents the Maximum column widhth
 */
int colWidth;

/**
 * nrow represents the number of rows in the page
 */
int nrow;

/**
 * cards is an array of the Dialog2Card objects. Which represent the 
 * different pages in the Dialog2.
 */
Dialog2Card[] cards;

/**
 * cardLayout is the layoutmanager for the Dialog2 object
 * @see java.awt.LayoutManager
 * @see java.awt.CardLayout
 */
CardLayout cardLayout;

/** 
 * cardPanel is the container for holding the Dialog2Cards
 */
Panel cardPanel;

/**
 Flag indicating whether the user pressed OK button. This flag is set in the
 action method if the user presses the OK button.
 */
public boolean statusOK = false;
/**
 * TuningNotes is a dialog in which the user can enter the JVM specific, tuning
 * notes, which will be updated in the properties.
 * @see spec.harness.TuningNotes
 */
private TuningNotes tuning;

///////////////////////////////////////
//constructor declarations
///////////////////////////////////////
/**
 Constructor.
 @param ttl Title of the dialog box
 @param t title Properties structure
 @param v value Properties structure
 @param pn Property strings
 @param cw Column width
 @param maxRows Maximum number of rows in each page
 @param backgroundColor Background color of the dialog
 */
 
Dialog2(String ttl, Properties t, Properties v, String[] pn, 
    int cw, int maxRows, Color backgroundColor){
    title = ttl;
    titleProps = t;
    valueProps = v;
    propNames = pn;
    colWidth = cw;
    setFont (helvetica12);
    int screenSize = Toolkit.getDefaultToolkit().getScreenSize().height;
    int height = getFontMetrics(helvetica12).getHeight();
    int nrow = (int)((screenSize - insets().top - insets().bottom)
	* 0.4 / height) - 1;
    if (nrow > maxRows)		// allow property to override
	nrow = maxRows;		// in case calculation is wrong
    setBackground (backgroundColor);
    setLayout(new BorderLayout());   
    setTitle(title);
    fields = new TextField[propNames.length];
    cardPanel = new Panel();
    cardLayout = new CardLayout();
    cardPanel.setLayout(cardLayout);
    int nCards = (pn.length + nrow - 1)/ nrow;
    cards = new Dialog2Card[nCards];
    int iCard, iFrom;
    for (iCard=0, iFrom=0; iCard < nCards; iCard++){
        int iTo = Math.min(iFrom + nrow, propNames.length);
        cards[iCard] = new Dialog2Card(t, v, propNames, fields,
        iFrom, iTo, cw);
        cardPanel.add("Page " + iCard + " of " + nCards,cards[iCard]);
        iFrom += nrow;
    }
    cards[nCards-1].addInner (new Label("Tuning Notes"));
    Panel p = new Panel();
    p.add (new Button("edit"));
    cards[nCards-1].addInner (p);
    String noteFile = (String) valueProps.get ("spec.initial.noteFile");
    if (noteFile == null)
	noteFile = "props/notes";
    tuning = new TuningNotes (noteFile);
    add("Center",cardPanel);
    Panel buttonPanel = new Panel();
    buttonPanel.setLayout(new FlowLayout());
    buttonPanel.add(new Button("Previous"));
    buttonPanel.add(new Button("Next"));
    buttonPanel.add(new Button("OK"));
    buttonPanel.add(new Button("Cancel"));
    add("South",buttonPanel);
    pack();
    show();
}


///////////////////////////////////////
//class method declarations
///////////////////////////////////////

///////////////////////////////////////
//instance method declarations
///////////////////////////////////////

/**
 * action routine which is called when the user performs any action
 * like clicking on the button available in the button panel
 */
public boolean action(Event evt, Object arg){
    if (evt.target instanceof Button){
        if (arg.equals("OK")){
	    valueProps.put ("spec.test.notes", tuning.toSingleLine());
            storeProperties();
	    statusOK = true;
            hide();
        }else if (arg.equals("Cancel")){
	    resetValues();
            hide();
        }else if (arg.equals("Previous")){
            cardLayout.previous(cardPanel);
        }else if (arg.equals("Next")){
            cardLayout.next(cardPanel);
        }else if (arg.equals("edit")){
            tuning.show();
        }
        return true;
    }
    return super.action(evt,arg);
}

/**
 * Resets the setup parameters. The user can modify the setup 
 * parameters by editing the the text fields corresponding to them.
 * If the user decides to go back with all the changes he/she had done,
 * he/she can reset the values to the values available in the 
 * porperties file.
 */
public void resetValues(){
    int iCard, iFrom;
    for (iCard=0, iFrom=0; iCard < cards.length; iCard++){
        int iTo = Math.min(iFrom + nrow, propNames.length);
        cards[iCard].resetValues();
        iFrom += nrow;
    }
}

/* Stores the changed properties back to the properties structure
 */
private void storeProperties(){
    for (int i=0; i<propNames.length; i++){
        valueProps.put(propNames[i],fields[i].getText());
    }
}

}

