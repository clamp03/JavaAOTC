/*
 * @(#)TuningNotes.java	1.3 06/17/98
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
import java.io.*;
import java.net.*;

/** 
 This class provices a way by which user can enter some observations made 
 during the execution of the benchmark. These observations will be mad a part
 of the mail sent at the end of the test.
 */
public class TuningNotes extends Frame{

///////////////////////////////////////
//class variable field declarations
///////////////////////////////////////

///////////////////////////////////////
//instance variable field declarations
///////////////////////////////////////
private String notes;
private TextArea text = new TextArea (24,80);

///////////////////////////////////////
//constructor declarations
///////////////////////////////////////
/** 
 Constructor for the Tuning notes object. Creates the windows for editing the
 tuning notes.
 */
public TuningNotes (String path){
    setTitle ("Tuning Notes");
    setBackground (Color.white);
    setLayout (new BorderLayout());
    notes = readFile (spec.harness.Context.getSpecBasePath() + path);
    text.setFont (new Font ("Courier", Font.PLAIN, 12));
    add ("Center",text);
    Panel bp = new Panel();
    bp.setLayout (new FlowLayout());
    bp.add (new Button ("OK"));
    bp.add (new Button ("Cancel"));
    add ("South",bp);
    pack();
}

///////////////////////////////////////
//class method declarations
///////////////////////////////////////

///////////////////////////////////////
//instance method declarations
///////////////////////////////////////
/**
 Callback routinue to handle the Button presses of the user
 */
public boolean action(Event evt, Object arg){
    if (evt.target instanceof Button){
        if (arg.equals("OK")){
            notes = text.getText();
	}
	hide();
        return true;
    }
    return super.action(evt,arg);
}

/**
 Reads the already existing tuning notes from the file given
 */
private String readFile (String path){
    if (! path.startsWith ("http://"))
	path = "file://" + path;
    StringBuffer buf = new StringBuffer();
    try{
    	URL url = new URL(path);
	DataInputStream in = new DataInputStream(
	    new BufferedInputStream (url.openStream()));
	String line;
	while ((line = in.readLine()) != null){
	    buf.append (line + "\n");
	}
    }catch (Exception e){
	spec.harness.Context.out.println( "Error reading tuning notes: " + e );
	return "could not read tuning notes";
    }
    return buf.toString();
}

/**
 Shows the dialog box. Set the Text area string before displaying the dialog
 */

public void show(){
    text.setText (notes);
    super.show();
}

public String toSingleLine(){
    StringBuffer sb = new StringBuffer();
    char[] nb = new char [notes.length()];
    notes.getChars (0, nb.length, nb, 0);
    for (int i=0; i < nb.length; i++)
	if (nb[i] == '\n')
	    sb.append ("\\n");
	else
	    sb.append (nb[i]);
    return sb.toString();
}

}//end class

