/*
 * @(#)Logo.java	1.5 07/10/98
 * Walter Bays
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */
 
/**
 This class displays a SPEC logo.
 */ 
package spec.harness;

//import java.applet.*;
import java.awt.*;
import java.net.*;

public class Logo extends Canvas{

private final static int width =  57;
private final static int height = 72;
private final static int border =  2;

private Image image;

/**
 Create a SPEC logo.
 Size of image is hard coded - a bad idea, but expedient
 @param base installation directory for SPEC software
 @filename name of SPEC logo image file
 @applet caller applet
 */
public Logo (String base, String filename, Color color){
    setBackground (color);
    readImage (base + "images/" + filename);
    try{
        setSize (width + 2*border, height + 2*border);
    }catch (NoSuchMethodError e){
	// run a bit further on API 1.0.2
        resize (width + 2*border, height + 2*border);
    }
}

public void paint (Graphics g){
    if (image != null)
	g.drawImage (image, border, border, this);
}

private void readImage (String path){
    try{
	image = Toolkit.getDefaultToolkit().getImage (new URL(path));
    }catch (MalformedURLException e){
        System.out.println ("Error: " + e);
    }
}

}//end class
