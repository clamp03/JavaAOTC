/*
 * @(#)SpecApplet.java	1.7 06/17/98
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */
import java.awt.*;
import java.applet.*;
import spec.harness.*;

/**
 This the the root class using which the SpecJVMClient98 can be launched. This
 class is placed in the html file with applet tag and the initialization params
 */
public class SpecApplet extends Applet {

/**
 SpecJVMClient object
 */
	SpecJava specJava;

/**
 Applet initialization method. This method apart from calling the default init
 method of Applet class, creates the instance of the SpecJVMClient object and
 initializes it
 */
    public void init() {
        super.init();
	String p = getParameter ("properties");
	Context.setUserPropFile (p);
        specJava = new SpecJava();
        specJava.init( this, false );
    }

/**
 This method is used when SpecJVM client is run as an application. This is only
 for debugging purpos
 @see spec.harness.SpecJava#init
 */
    public void applicationInit() {
        super.init();
        specJava = new SpecJava();
        specJava.init( this, true );
    }

/**
 Applet Start method
 @see spec.harness.SpecJava#start
 */
    public void start() {
        specJava.start();
    }

/**
 Applet stop method
 @see spec.harness.SpecJava#stop
 */
    public void stop() {
        specJava.stop();
    }

/**
 Callback function to handle the user operations
 */
    public boolean handleEvent(Event event) {
        if( specJava.handleEvent( event ) == true ) {
            return true;
        } else {
            return super.handleEvent( event );
        }
    }

}
