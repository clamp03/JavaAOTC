/*
 * @(#)About.java	1.8 06/29/98
 * Walter Bays
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */
 
/**
 This class displays the information about SpecJVMClient benchmark and the 
 pictures of the contributors. This is just a help window.
 */ 
package spec.harness;

import java.applet.*;
import java.awt.*;
import java.net.*;
import java.util.Random;

/**
 This class is used to display the information about the people who contributed
 for developing SpecJVMClient98
 */
public class About extends Frame implements Runnable{

///////////////////////////////////////
//class variable field declarations
///////////////////////////////////////
// todo: put this in a file

/**
 how many subfields per pseudo-entry in the pics array
 */
private final static int NFIELDS = 4;

/**
 String array consisting of the names of the contributors, the location they
 work and the name of their picture files
 */
private static String[] pics = {

    "thanks.gif",
    "ACKNOWLEDGEMENT",
    null,
    "www.spec.org",

    null,
    "Alex Jacoby",
    null,
    "www.sunlabs.com",

    "AnneTroop.jpg",
    "Anne Troop",
    "Hewlett-Packard",
    "www.hp.com",

    null,
    "Arthur van Hoff",
    "Sun Microsystems",
    "java.sun.com",

    "BodoParady.jpg",
    "Bodo Parady",
    "Sun Microsystems",
    "www.sun.com",

    null,
    "Charles Rosendahl",
    null,
    "www.learningnetwork.com",

    "DavidConnelly.gif",
    "David Connelly",
    "Sun Microsystems",
    "java.sun.com/people/dac/index.html",

    null,
    "Donald W. McCauley",
    "IBM",
    "www.ibm.com",

    null,
    "Elliot Joel Berk",
    "Princeton University",
    "www.cs.princeton.edu/~ejberk/",

    "foss.gif",
    "Ernest Friedman-Hill",
    "Sandia National Laboratories",
    "herzberg.ca.sandia.gov",

    "iis_11b.gif",
    "Fraunhofer IIS Multimedia Software Team",
    "Fraunhofer Institut fuer Integrierte Schaltungen",
    "www.iis.fhg.de/audio",

    null,
    "Hans Boehm",
    "Silicon Graphics",
    "www.sgi.com",

    null,
    "James A. Woods",
    null,
    null,

    null,
    "Jeff Chan",
    "Sun Microsystems",
    "www.sun.com",

    null,
    "Jim McKie",
    "formerly with Lucent Technology",
    "www.cs.nyu.edu/~rubin/alums.html",

    null,
    "Joe Orost",
    "AT&T",
    "www.research.att.com/~orost/",

    null,
    "John Maloney",
    null,
    null,

    null,
    "Jonathan Gibbons",
    "Sun Microsystems",
    "www.sun.com",

    null,
    "Joseph Coha",
    "Hewlett-Packard",
    "www.hp.com",

    "KaivalyaDixit.jpg",
    "Kaivalya Dixit",
    "IBM",
    "www.ibm.com",

    null,
    "Ken Turkowski",
    null,
    null,

    "KumarThiagarajan.jpg",
    "Kumar Thiagarajan",
    "Sun Microsystems",
    "www.sun.com",

    null,
    "L Peter Deutsch",
    "Aladdin Enterprises",
    "www.cs.wisc.edu/~ghost/",

    "MarioWolczko.jpg",
    "Mario Wolczko",
    "Sun Microsystems",
    "www.sunlabs.com/people/mario/",

    "MartinRichards.gif",
    "Martin Richards",
    "Cambridge University",
    "www.cl.cam.ac.uk/users/mr/",

    "MickJordan.gif",
    "Mick Jordan",
    "Sun Microsystems",
    "self.smli.com",

    "s2-gorge1.jpg",
    "Nik Shaylor",
    "Sun Microsystems",
    "www.sun.com",

    "OleAgesen.gif",
    "Ole Agesen",
    "Sun Microsystems",
    "www.sun.com",

    "paula_at_aspen.jpg",
    "Paula Smith",
    "Digital",
    "www.digital.com",

    "pbk.gif",
    "Peter Kessler",
    "Sun Microsystems",
    "www.sun.com",

    null,
    "Radhakrishnan Manga",
    "Sun Microsystems",
    "www.sun.com",

    null,
    "Rajeev Gandhi",
    "IBM",
    "www.ibm.com",

    "rrh3b.jpg",
    "Randy Heisch",
    "IBM",
    "www.ibm.com",

    null,
    "Salina Chu",
    "IBM",
    "www.ibm.com",

    null,
    "Spencer W. Thomas",
    null,
    null,

    null,
    "Steve Davies",
    null,
    null,

    "WalterBays.jpg",
    "Walter Bays",
    "Sun Microsystems",
    "www.sun.com",

};

///////////////////////////////////////
//instance variable field declarations
///////////////////////////////////////
/**
 String array storing the affiliation information of each constributor 
 */
private String[]	affiliations;

/**
 Stores the applet context.
 */
private AppletContext	context;

/**
 Picture to show for people whose picture we don't have
 */
private Image		defaultImage;

/**
 Font used for painting any text on the display window
 */
private Font		font = new Font("Helvetica", Font.BOLD, 14);

/**
 String array storing the home pages for each contributor
 */
private String[]	homepages;

/**
 Image array for storing the pictures of contributors
 */
private Image[]		images;

/**
 Delay between showing of two images. Background displays the images w for 'ms' milliseconds and displays the next one.
 */
private long		ms;

/**
 Names of the contributors
 */
private String[]	names;

/**
 OK button
 */
private Button		ok = new Button ("OK");

/**
 Just a counter indicating which image has to be loaded next
 */
private int		rot = 0;

/**
 Thread which displays the images continously
 */
private Thread		runner;

/**
 Randomizes the images indexes and shows them in random order
 */
private int[]		scramble;

///////////////////////////////////////
//constructor declarations
///////////////////////////////////////
/**
 Constructor. Calls the constructor with size and sleep time information.
 @param base Location from which the images can be read
 @param context AppletContext.
 */
public About (String base, AppletContext context){
    this (base, context, 385, 360, 4000);
}

/**
 Constructor which also takes the size, sleep time.
 @param base Location from which the images can be read
 @param contdxt AppletContext
 @param width Width of the window
 @param height height of the window
 @param ms Sleep time in milliseconds
 */
public About (String base, AppletContext context, 
    int width, int height, long ms)
{
    this.context = context;
    this.runner = runner;
    this.ms = ms;
    readImages (base);
    scramble = permute (images.length);
    setTitle ("SPEC thanks...");
    setBackground (Color.white);
    setLayout (new BorderLayout());
    Panel p = new Panel();
    p.setLayout (new FlowLayout());
    p.add (ok);
    add ("South", p);
    resize (width, height);
    show();
}

///////////////////////////////////////
//class method declarations
///////////////////////////////////////
/**
 Generates a random order to indexes to display the images
 However, always map item 0 to 0 so it will display first
 */
private static int[] permute (int n){
    int[] a = new int[n];
    a[0] = 0;
    Random rand = new Random();
    for (int i=n-1; i > 0; i--){
	int j = rand.nextInt();
	if (j < 0)
	    j = -j;
	j = j % (n-1) + 1;
	while (a[j] != 0)
	    j = (j + 1) % (n-1) + 1;
	a[j] = i;
    }
    return a;
}

///////////////////////////////////////
//instance method declarations
///////////////////////////////////////
/**
 Call back function called when the user presses 'OK' button
 */
public boolean action (Event evt, Object arg){
    if (arg.equals("OK")){
	this.dispose();
	runner.stop();
	return true;
    }
    return false;
}

/**
 Paints the window
 @param Handle to the graphics context
 */
public void paint (Graphics g){
    Insets in = insets();
    FontMetrics fm = g.getFontMetrics(font);
    int ascent = fm.getAscent();
    int leading = fm.getLeading();
    int x = in.left + 2;
    int y = in.top + ascent + leading;
    g.setColor (Color.black);
    g.drawString (names[scramble[rot]], x, y);
    y += ascent + leading;
    if (affiliations[scramble[rot]] != null)
        g.drawString (affiliations[scramble[rot]], x, y);
    y += ascent + leading;
    if (homepages[scramble[rot]] != null)
        g.drawString (homepages[scramble[rot]], x, y);
    y += leading + 2;
    if (images[scramble[rot]] != null)
        g.drawImage (images[scramble[rot]], x, y, this);
    else
        g.drawImage (defaultImage, x, y, this);
}

/**
 Reads the images from the specified location.
 @param base Location from which the images are read
 */
private void readImages (String base){
    try{
        images = new Image[pics.length/NFIELDS];
        names = new String[pics.length/NFIELDS];
        affiliations = new String[pics.length/NFIELDS];
        homepages = new String[pics.length/NFIELDS];
        int j = 0;
        for (int i=0; i < pics.length; i += NFIELDS){
	    if (pics[i] != null){
                String path = base + "images/credits/" + pics[i];
                URL url = new URL(path);
	        images[j] = context.getImage (url);
	    }
	    names[j] = pics[i+1];
	    affiliations[j] = pics[i+2];
	    homepages[j] = pics[i+3];
	    j++;
        }
	defaultImage = context.getImage (
	    new URL (base + "images/credits/default.jpg"));
    }catch (MalformedURLException e){
        System.out.println ("Error: " + e);
    }
}

/**
  Run method for the thread.
 */
public void run(){
    for(;;){
        try{
	    Thread.sleep (ms);
        }catch (InterruptedException e){}
        rot = (rot+1) % images.length;
	repaint();
    }
}

/**
 Sets the runner parameter
 @param runner Thread object, which displays the images
 */
public void setRunner (Thread runner){
    this.runner = runner;
}

}//end class

