/*
 * @(#)ConsoleWindow.java	1.1 04/02/01
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */
 
package spec.harness;
///import java.awt.*;

/**
 * This windows is used to display the trace from the benchmark
 * in a seperate window, instead of displaying it in the stdout.
 * This class extends the Frame and has a TextArea where the benchmark
 * trace is shown. The Windows can be enabled and disabled using the 
 * Console on/off button available in the Applet.
 * @see java.awt.TextArea
 * @see java.awt.Frame 
 */

public
class ConsoleWindow /*extends Frame*/ {
    /**
     * The TextField componant
     */
    TextArea text = new TextArea(25,80);

    /**
     * First show() call flag. The window is created only once. Subsequent 
	 * console on/off operations just hides or shows this window. 
     */
    boolean firstTime = true;

    /**
     * Constructor
     */   
    public ConsoleWindow(){   
        setTitle("SPEC JVM Client98 Console");
	///setLayout(new GridLayout(1,1,2,2));
	///add(text);
    }
        
	
    /**
     * Shows the window. This method overrides the show of the parent Frame
	 * class. If the is window is displayed for the firsttime, the size is made
	 * to activate the window.
     */
    public void show() {
	///if (firstTime) {
	/*    firstTime = false;
	  super.show();
            hide();
            resize(insets().left + insets().right  + 400,
		   insets().top  + insets().bottom + 360);	
	}
	super.show();*/
    }
	    
    /**
     * Append some data to the text area.
     */
    public void append(String data) {
	///if (Context.getCommandLineMode()) {
	    System.out.print(data);
	///} else {
	///    if (firstTime == false) {
	///	text.appendText(data);
	///    }
	///}
    }      
    
    
    /**
     * Handle event. The Window_Destroy message is handled in this method. 
	 * The Frame is not created and disposed every time the user reqests for
	 * console on and console off, it is just shown or hiden. When the user
	 * kills the console window, it is just hiden.
	 * @see spec.harness.Context
     */
///
/*
    public boolean handleEvent(Event evt) {
        switch( evt.id ) {
        case Event.WINDOW_DESTROY:
	    Context.stopOutputWindow();
            return true;
        default:
            return super.handleEvent(evt);
        }
    }
*/
}//end class

