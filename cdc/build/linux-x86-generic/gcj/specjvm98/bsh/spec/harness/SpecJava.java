/*
 * @(#)SpecJava.java	2.1 08/10/98
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

package spec.harness;

import java.awt.*;
import java.applet.*;
import java.net.*;
import java.io.*;
import java.util.*;

///////////////////////////////////////////////////////////////////////
////////////////////////BEGIN SPECJAVA CLASS///////////////////////////
/////////////////////////////////////////////////////////////////////// 
/** 
 * SpecJava class defines the harness environment within which various
 * benchmarks are executed.  It displays results, generates reports, and
 * creates an interface to choose benchmarks for execution. This class 
 * implements the BenchmarkDone interface, which has a method benchmarkdone().
 * The Benchmarks call this method once they finish the execution. 
 * 
 * @see BenchmarkDone
 * @see SpecJava#BenchmarkDone
 */
public class SpecJava implements BenchmarkDone {

    /** 
     *	A variable to indicate the version of the SpecJava
     */
    static String version = "V99 Harness: 9999-99-99";
    
	private String compliantGroup;

    Applet applet;
    boolean running = false;    
	
    /**
     * Flag indicating, whether the benchmarks are being run in automode or not.
     * @see JavaSpec#autoRunButton_Clicked	
     */	
    public boolean autoRun = false;
    
	/** 
	 * Flag indicating the mode of SpecJVMClient98
	 */
	boolean testMode = false;
    
	int percentTimes100 = 200;

    /** 
     *      Default constructor for the SpecJava class
     */
    public SpecJava() {
    }

    ///////////////////////// EVENT METHODS /////////////////////////
    /////////////////////////////////////////////////////////////////

    // Generate the report frame

    /**
     * Brings up the Report frame. This method is called, when the user 
     * clicks on the Report button
	 * @see spec.harness.ReportFrame
     */
    void reportButton_Clicked(Event event) {
	if (!interactive || props.isSetup()){
	    new ReportFrame( 
                         runResults, 
			 props,
			 props.get ("spec.testx.mailHost",
                             (standalone) ? "localhost"
				          : applet.getCodeBase().getHost()),
			 interactive
                	   ).show();
	}else{
	    message ("You must first press the 'Setup' button\n");
	}
    }


    static String fileColon = "file:///";

     /**
     * Gets the codebase of the applet. The current directory is returned 
	 * if applet is running in the local mode, otherwise the Applet code 
	 * base is returned.
	 * @return CodeBase
     */
    String getCbase() {
        if (!standalone) {
            return applet.getCodeBase().toString();
        } else {
            return fileColon + (new File("")).getAbsolutePath().replace('\\','/');
        }
    }

    // Generate Setup Frame 
    /**
     * Brings up the Setup frame. This method is called, when the user 
     * clicks on the Setup button. User can edit the properties of the JVM, 
	 * the mail address to which the results have to be forwarded.
     */
    void setupButton_Clicked(Event event) {
	props.setup();
    }

    /**
     * Starts running the benchmarks. The benchmarks are run one at a time. 
	 * The option is also there to start the benchmarks on individual thread. 
	 * This function is called when the user clicks the start button.
     * @see SpecJava#handleStart#append(int)
     */
    void startButton_Clicked( Event event ) {
        selectedBenchmarks = benchlist.getSelectedItems();           		        
        if (selectedBenchmarks.length == 0) {
            spec.harness.Context.out.println("No Benchmarks Selected");
        } else {
            end = false;
            totalaverage = 0;
            running_selection = 0;

            message ("--------------------------------\n");
	    Context.setSpeed( Integer.parseInt( speedlist.getSelectedItem() ) );
	    Context.setCachedInputFlag (
		((String)props.get("spec.initial.cache")).
		    equalsIgnoreCase ("true"));
	    running = true;
	    terminateAutorun = false;
            handleStart(running_selection);
        }
    }


    /** 
     * Stops the running of the benchmark. This function is called when the 
	 * user clicks on the stop button. This generates the ThreadDeathException
	 * which is shown in the Run result display window
     */
    void stopButton_Clicked(Event event) {
 	terminateAutorun = true;
   	RunProgram.stop();
	running = false;
    	showStatus ("Ready");
    }


    /**
     * Deselects all the selected benchmarks in the list box.
     */
    void deselectAll() { 
	for( int i = 0 ; i < benchlist.countItems() ; i++ ) {
	    benchlist.deselect(i);
	}
    }

    /**
     * Selects all the benchmarks in the list box.
     */
    void selectAll() { 
	deselectAll(); // Seems to solve some bug
	for( int i = 0 ; i < benchlist.countItems() ; i++ ) {
	    benchlist.select(i);
	}
    }
  
    /**
     * Selects all the benchmarks in the list box. This function is called 
	 * when the user presses the SelectAll button. The same button is used
	 * for both select all and unselect functions. The text of the button 
	 * is changed depending on the previous state of the button.
     */
    void selectAllButton_Clicked(Event event) {    
	if ( (selectAllButton.getLabel()).equals("Select All") ){
	    selectAllButton.setLabel("Unselect All");
	    selectAll();
        } else {
	    selectAllButton.setLabel("Select All");
	    deselectAll();
        }
    }

    // Help function -wnb
/** Puts the SpecJVMClient98 into help mode. User can get the context sensitive
 * help for SpecJVMClient98. By pressing the help button, SpecJVMClient98 
 * enters into 'Help mode'. User can now click on any component to get context
 * sensitive help. The text of the 'HELP' button is turned into '>>help<<' when
 * SpecJVMClient98 is in help mode
 */
   
	void helpButton_Clicked(Event event) {    
	helpActive = true;
	helpButton.setLabel(">>help<<");
    }

/**
 * The console can be toggled On/Off by pressing the Console button. This 
 * method is called when the user presses the 'Console On/Off', button. The
 * text of the button toggles depending on the console Window visibility.
 */    
	void viewButton_Clicked(Event event) {    
	if ( (viewButton.getLabel()).equals("Console On") ){
	    viewButton.setLabel("Console Off");
	    Context.startOutputWindow();
        } else {
	    viewButton.setLabel("Console On");
	    Context.stopOutputWindow();
        }
    }


/**
 * Starts the autorun mode of SpecJVMClient98. This function is called when
 * user presses the 'Autorun' button
 */ 
	
    /** 
     * Starts the autorun functionality.
     * The benchmarks can be run individually, or in a batch. The user is provided
     * with the option of selecting the benchmarks he/she wants to run. Yet another
     * option called AutoMode is provided. This mode ensures that the selected 
     * benchmark(s) is run multiple times till the time for completion is within
     * 5 % limit of the previous run. 
     *	
     * <pre>
     *			| 	 *
     *			|	    
     *			|
     *			|	     *	
     *	Time	|
     *			|			 *
     *			|				 *	 *	 *	 *	*
     *			|
     *			|_____________________________________
     *				 1   2   3   4   5
     *				    	 Run
     *	
     *
     * </pre>
     */
    void autoRunButton_Clicked(Event event) { 
	if (anythingRun){
	    message  (
	        "Warning. You have previously run some benchmark(s)\n" +
	        "so this run will not comply with the SPEC run rules\n");
	    props.put ("spec.results.restarted","true");
	}
	autoRun = true;	
	terminateAutorun = false;
	startButton_Clicked( event );		
    }


    // User clicked start button
/** 
 * This function is called when the user pushes the start Button
 */    
    void handleStart(int choice) {
    	valid = true;
	showStatus ("Running");
	anythingRun = true;
        RunProgram.run (
	    selectedBenchmarks[choice],
	    autoRun,
	    props.getProperties(),
	    this
	    );
    }

    public void start() {
//**NS**        if (runner != null)
//**NS**            runner.resume();
    }


    public void stop() {
//**NS**        if (runner != null)
//**NS**            runner.suspend();
    }

/** 
 * This functin is called when the user pushes the Help Button
 */
 
    private boolean handleHelpEvent (Event event) {
        if (event.id == Event.LIST_SELECT){
            int n = ((Integer)(event.arg)).intValue();
            if (n < benchlist.countItems()){
                showHelp ((String) props.get("spec.help.benchmarks") + "/" +
		    benchlist.getItem(n) + ".html");
                benchlist.deselect(n);      // undo action not desired for help
                return true;
            }
        }else if (event.id == Event.LIST_DESELECT){
            int n = ((Integer)(event.arg)).intValue();
            if (n < benchlist.countItems()){
                showHelp((String) props.get("spec.help.benchmarks") + "/" +
		    benchlist.getItem(n) + ".html");
                benchlist.select(n);        // undo action not desired for help
                return true;
            }
        }else if (event.id == Event.ACTION_EVENT){
            if (event.target instanceof Button){
                Button b = (Button) event.target;
                showHelp((String) props.get ("spec.help.Button." + 
		    b.getLabel().replace(' ','_')));
                return true;
            }
        }
        return false;
    }

    // Help function -wnb
/**
 * SpecJVMClient98 has help function. User can get the context sensitive
 * help, by clicking the help button and clicking on the component on which
 * help is wanted. The help information is displayed in the browser if the 
 * benchmark is run in a browser, or it will be displaye on the stdout, if the
 * benchmark is run using the appletviewer
 */
    private void showHelp (String s){
	try{
	    URL u = new URL(getCbase() + s);
	    if (showHelpInBrowser)
	        applet.getAppletContext().showDocument (u, "help");
	    else{
		DataInputStream in = new DataInputStream(
		    new BufferedInputStream(u.openStream()));
		System.out.println("--- HELP: " + s + " ---");
		String line;
		while ((line = in.readLine()) != null)
		    if (! line.startsWith("<"))
		        System.out.println(line);
	    }
	}catch (Exception e){
	    System.out.println("showHelp: " + e);
	}
    }

    // HANDLEEVENT
/**
 * This is the callback function for the buttons in the applet
 */	
    public boolean handleEvent(Event event) {

	if (helpActive && handleHelpEvent(event)){
	    helpActive = false;
	    helpButton.setLabel("Help");
	    return true;
	}

        if (event.target == grouplist && event.id == Event.ACTION_EVENT){
            setBenchmarkList( (String)event.arg );
            return true;
        }

        if (event.target == helpButton && event.id == Event.ACTION_EVENT) {
            helpButton_Clicked(event);
            return true;
        }

        if (event.target == startButton && event.id == Event.ACTION_EVENT) {
            startButton_Clicked(event);
            return true;
        }

        if (event.target == stopButton && event.id == Event.ACTION_EVENT) {
            stopButton_Clicked(event);
            return true;
        }
	if (event.target == selectAllButton && event.id == Event.ACTION_EVENT) {
            selectAllButton_Clicked(event);
            return true;
        }
	
	if (event.target == autoRunButton && event.id == Event.ACTION_EVENT) {
             autoRunButton_Clicked(event);
            return true;
        }	

        if (event.target == setupButton && event.id == Event.ACTION_EVENT) {
            setupButton_Clicked(event);
            return true;
        }

        if (event.target == reportButton && event.id == Event.ACTION_EVENT) {
            reportButton_Clicked(event);
            return true;
        }

        if (event.target == viewButton && event.id == Event.ACTION_EVENT) {
            viewButton_Clicked(event);
            return true;
        }


        if (event.target == quitButton && event.id == Event.ACTION_EVENT) {
            System.exit(0);
            return true;
        }

/* For initial dialog */

        if (event.target == initialButton && event.id == Event.ACTION_EVENT) {
	    showHelpInBrowser = helpWhere.getCurrent().getLabel().equals("browser");
	    if (showHelpInBrowser && standalone){
	        System.out.println ("Cannot display help in browser when running " +
	            "as a standalone application.\n" +
	            "Will display on system console instead.");
	        showHelpInBrowser = false;
	    }
    	    testMode = runType.getCurrent().getLabel().equals("Test mode");
            storeProperties();
            return true;
	}

/*
	if (event.target == aboutButton && event.id == Event.ACTION_EVENT){
	    About about = new About (getCbase(),
	        applet.getAppletContext());
	    try{
		Thread thread = new Thread (about);
		about.setRunner (thread);
	        thread.start();
	    }catch (Exception e){
		System.out.println ("Error handling About button: " + e);
	    }
	    return true;
	}
*/

        if (event.target == initialHelpButton && event.id == Event.ACTION_EVENT) {
	    showHelpInBrowser = helpWhere.getCurrent().getLabel().equals("browser");
	    System.out.println(
		"\nIf you are running in a web browser, e.g. Netscape Navigator,\n" +
		"Internet Explorer, or HotJava, you may display help information\n" +
		"in a new browser window for convenience in reading and navigating\n"+
		"the topics, by checking the appropriate button in the initial\n" +
		"window. Otherwise help information will be displayed in\n" +
		"your Java console like this message.\n");
	    showHelp((String) props.get ("spec.help.initial"));
            return true;
	}

        if (event.target == checkTypeCompliant && event.id == Event.ACTION_EVENT) {
            grouplist.select(compliantGroup);
            setBenchmarkList(compliantGroup);
            grouplist.disable();
            return true;
	}
        if (event.target == checkTypeTest && event.id == Event.ACTION_EVENT) {
            grouplist.enable();
            return true;
	}

        if (event.target == cancelButton && event.id == Event.ACTION_EVENT) {
            System.exit(0);
            return true;
        }

        return false;
    }

    /**
     * Loads the benchmark groups. The benchmarks have been divided into groups.
     * The user can select the group of his interest and run it.
     */
    void loadBenchmarkGroups() {
	for( int i = 1 ;; i++ ) {  	
	    String s = props.get("spec.benchmarkGroup"+i+".00");
	    if( s == null )
	        return;	    
	    Vector v = new Vector();
	    v.addElement( s );	    
		    	    
	    for( int j = 1 ;; j++ ) {			    
		s = props.get("spec.benchmarkGroup"+i+"."+((j < 10) ? "0" : "") + j);
		if( s == null )
		    break;  
		v.addElement( s );
	    }	    
	    benchmarkGroups.addElement( v );
	}	
    }

/**
 * Loads the benchmark set depending on the user's selection of the benchmark
 * group
 */
     void setBenchmarkList( String s ) {
	benchlist.clear();
	for( int i = 0 ; i < benchmarkGroups.size() ; i++ ) {
	    Vector x = (Vector)benchmarkGroups.elementAt( i );
	    if( s.equals( x.elementAt( 0 ) ) ) {
		for( int j = 1 ; j < x.size() ; j++ ) {
		    benchlist.addItem( (String)x.elementAt( j ) );     
		}
	    }
	}
    }
    
    // INIT Method
/** 
 * init method of the applet. Loads the benchmark groups. Get the property
 * files. The mode or running the SpecJVMClient.
 */	
    public void init( Applet applet, boolean standalone ) {
        this.applet      = applet;
        this.standalone  = standalone;	
	Context.setSpecBasePath( getCbase() );
	props = new SpecProps( getCbase(), bgcolor );
	compliantGroup = props.get ("spec.initial.compliant.group","All");
	String percentString = (String)props.get( "spec.initial.percentTimes100" );
	percentTimes100 = Context.getValue( percentString, 200 );
        if( props.get("spec.initial.console").equals("true") ) {
            Context.setupConsoleWindow();
        }
	loadBenchmarkGroups();		
	version = props.get("spec.report.version")+
	    " (" +props.get("spec.report.versionDate") + ")";
	// Command line or applet parameter settings show up in the
	// Context object. These settings override properties settings
	// in the user file if you choose batch (non-interactive) mode.
	// Otherwise run interactive unless the property exists and
	// is false
	String si = (String) props.get ("spec.initial.interactive");
	if (Context.isBatch() || (si != null && si.equalsIgnoreCase ("false")))
	    interactive = false;
	if (applet != null)
	    initialDialog();
        if (!interactive){
	    String size = props.get ("spec.initial.default.size","100");
            String group = props.get ("spec.initial.default.group",
		compliantGroup);
	    testMode = ! (group.equals(compliantGroup) && size.equals("100"));
            storeProperties();
            grouplist.select (group);
            setBenchmarkList (group);
	    speedlist.select (size);
	    selectAll();
    	    autoRunButton_Clicked((Event) null);
	}
    }

/** Initialization is divided into 2 parts. Part1 display some dialogs and
 * part2 which shows the benchmark lists available and the buttons to select
 * them and run them.
 */	
    public void initPart2() {
    
	applet.hide();
	applet.removeAll();

        applet.setBackground(bgcolor);
	Font helveticaBold14 = new Font("helvetica", Font.BOLD,14);

	/*
	The main division of the window is left/right.  On the left
	side is a scrolling list of available benchmarks to run. This
	list extends the full height of the window. In test mode the
	user may select any or all of the benchmarks. In SPEC-compliant
	measured run mode all benchmarks are selected and the user may
	not modify the selection.  We hope this list can be tall enough
	to contain all the benchmarks finally included by SPEC so that
	the user need not scroll the list to see any benchmark.
	*/

	applet.setLayout (new BorderLayout());
	Panel leftPanel = new Panel();
	leftPanel.setLayout (new BorderLayout());
	Label leftPanelLabel = new Label ("Benchmarks:");
	leftPanelLabel.setFont (helveticaBold14);
	leftPanel.setFont (new Font("helvetica", 0,14));
	Panel logoAndLabel = new Panel();
	logoAndLabel.setLayout (new BorderLayout());
	logoAndLabel.add ("Center", specLogo);
	logoAndLabel.add ("South", leftPanelLabel);
	leftPanel.add ("North",logoAndLabel);
	leftPanel.add ("Center",benchlist);
	applet.add ("West",leftPanel);
	
	/*
	Save some properties documenting the test conditions for
	this run
	*/
	
	props.put ("spec.results.commandLine",
	    String.valueOf (Context.getCommandLineMode()));
	//props.put ("spec.results.standalone", String.valueOf(standalone));
	props.put ("spec.initial.cbase", getCbase());
	props.put ("spec.results.testMode", String.valueOf(testMode));
	
	if (!testMode){
	    selectAll();
	    benchlist.disable();
	}

	/*
	The right of the window is split top/bottom At the top is a
	text display area.  used for informational display messages
	from the benchmark program to the benchmark user.
	*/

	Panel rightPanel = new Panel();
	rightPanel.setLayout (new FlowLayout());
	//Label rightPanelLabel = new Label("Status:");
	//rightPanelLabel.setFont (helveticaBold14);
	initStatus (helveticaBold14);
	rightPanel.add (statusPanel);
        textarea = new TextArea (
	    props.get("spec.report.suiteName") + 
	    " " + SpecJava.version + "\n" +
	    "Press the HELP button, then any button or benchmark\n" +
	    "to display help ", 10, 45);
	if (showHelpInBrowser)
	    message  ("in a browser window\n");
	else
	    message  ("in your Java console\n");
	if (!testMode){
	    message  ("Set to perform SPEC compliant run\n" +
		"Some controls are disabled. To use these or read help\n" +
		"on these items, run in test mode\n");
	}
	if (!interactive){
	    message  ("Non-interactive mode selected\n" +
		"Will autorun the default benchmark group and send results\n");
	    if (standalone)
	        message  ("and then exit\n");
	}
        textarea.setEditable (false);
	rightPanel.add (textarea);

	/*
	At the bottom of the right portion is an array of buttons and
	choice menu(s) arranged in three columns. These are used to
	control benchmark operation. In test mode all buttons are
	active whereas in SPEC-compliant mode some are inactive
	*/

	/*
	 Contains the button panel plus a label with
	 contact information for SPEC
	 */
	Panel outerButtonPanel = new Panel();
	outerButtonPanel.setLayout (new BorderLayout());
	Panel buttonPanel = new Panel();
	buttonPanel.setLayout (new GridLayout(0,3,5,3));

	// row 1
	Label buttonLabel = new Label ("Controls:");
	buttonLabel.setFont (helveticaBold14);
	buttonPanel.add (buttonLabel);
        speedlist = new Choice();	
	speedlist.addItem( "1" );
	speedlist.addItem( "10" );
	speedlist.addItem( "100" );
//	speedlist.addItem( "1000" );	NOT NOW, BUT MAYBE LATER
//	speedlist.select( "100" );			
	speedlist.select (props.get ("spec.initial.default.size","100"));			
	buttonPanel.add (speedlist);
        viewButton      = new Button("Console On");
	buttonPanel.add(viewButton);
	if(! props.get("spec.initial.console").equalsIgnoreCase("true") )
	    viewButton.disable();

	// row 2
        stopButton      = new Button("Stop");
	buttonPanel.add(stopButton);
        startButton	= new Button("Start");
	buttonPanel.add(startButton);
	autoRunButton   = new Button("Auto Run");	
	buttonPanel.add(autoRunButton);

	// row 3
        setupButton     = new Button("Setup");
	buttonPanel.add(setupButton);
        reportButton    = new Button("Report");
	buttonPanel.add(reportButton);
	helpButton	= new Button("Help");
	buttonPanel.add(helpButton);

	// row 4
	selectAllButton = new Button("Select All");
	buttonPanel.add(selectAllButton);
	quitButton  = new Button("Quit");
	buttonPanel.add(quitButton);
	if (! standalone)
	    quitButton.disable();
	String initialStatus = "Ready";
	if (!testMode){
	    speedlist.select( "100" );			
	    speedlist.disable();
	    startButton.disable();
	    selectAllButton.disable();
	    if (props.get ("spec.results.commandLine","").
		    equalsIgnoreCase ("true") 
		||
	        ! props.get ("spec.initial.cbase","").
	            startsWith ("http:/"))
	    {
		message  ("Error: SPEC compliant reportable results\n" +
		    "must be run from a web server. See the run rules.\n");
		initialStatus = "Error";
	    }
	}
	outerButtonPanel.add ("Center", buttonPanel);
	outerButtonPanel.add ("South", new Label (
	    "www.spec.org   +1 (703) 331-0180  info@spec.org"));
	rightPanel.add (outerButtonPanel);
	applet.add ("Center",rightPanel);
	try{
	    textarea.setCaretPosition (textarea.getText().length());
        }catch (NoSuchMethodError e){
	    // let API 1.0.2 run a bit further, albeit without
	    // the text windows scrolling conveniently
	    // You'll have to manually click to make them scroll automatically
	}
	showStatus (initialStatus);

    applet.validate();
    applet.show();
    }



/**
 * Converts a package name to a relative directory path
 */
    String classToPath(String clsname) {
        return getCbase() + (new String(clsname.toCharArray(),0,clsname.lastIndexOf(".")+1)).replace('.','/');
    }

/**
 *	converts filenames to URL's. 
 */
	public String qualifyFileName(String unqualified) {
        return getCbase() + unqualified;
    }

   
/** 
 * This method is called by program runner after finishing the benchmark.
 * This is a method of BenchmarkDone interface. 
 * @see spec.harness.BenchmarkDone
 * @see spec.harness.ProgramRunner
 */
 public void benchmarkPrint( String s ) {
     if (s != null && s.indexOf ("NOT VALID") >= 0)
     	valid = false;
    message ( s );
 }

/**
 * This method is called to append text to the status message area.
 * For some backward compatibility with the 1.0.2 API we'll first
 * attempt the recommended 1.1 method, and if that fails, fall back
 * on the deprecated 1.0.2 method
 */
private void message (String s){
    try{
	textarea.append (s);
    }catch (NoSuchMethodError e){
	textarea.appendText (s);
    }
}

/** 
 * This method is called by the programmer runner class after finishing the
 * benchmark. The results are mailed to the specified address if 
 * SpecJVMClient98 is running in the non-interactive mode. In the Interactive
 * mode of operation. The next benchmark from the selection list is started.
 * "Ver XX Finished" will be displayed if all the benchmarks in the selection
 * list were finished
 */
 public void benchmarkDone( String className, Properties results ) {
 
	if( results != null ) {
   	    runResults.put( className, results );	
	}
 
	if( selectedBenchmarks.length > 0 ) {
            if( running_selection < selectedBenchmarks.length - 1 ) {
                running_selection += 1;
                RunProgram.done();
                if (terminateAutorun)
                    message ("Run sequence terminated\n");
                else
                    handleStart(running_selection);
                return;
            }
	}
	
        message ("SPEC JVM Client98 Finished\n" );
        
	if (valid)
	    showStatus ("Ready");
	else
	    showStatus ("Error");
	RunProgram.done();
	running = autoRun = false;	    
	if (!interactive){
	    String s = props.get ("spec.initial.resultDelay");
	    double min = 0;
	    if (s != null){
	        try{
	            min = (Double.valueOf(s)).doubleValue();
	        }catch (NumberFormatException e){} //remains zero
	    }
	    if (min > 0){
	        message ("Finished run. " +
	            "Waiting " + min + " minutes before sending results\n");
	        try{
	            Thread.sleep ((long)(60 * min * 1000));
	        }catch (InterruptedException e){
	            message ("Results delay was interrupted\n");
	        }
	    }
	    reportButton_Clicked ((Event)null);
	    if (standalone)
	        System.exit (0);
	}
    }

  /**
  Perform all the visual cues necessary to inform the user of what's going on
  @param status Ready, Running, Error, Done
  */
  private void showStatus (String status){
    if (!standalone)
      applet.showStatus ("SPEC JVM Client98: " + status);
    statusPanel.removeAll();
    statusPanel.add (new Label ("Status:"));
    statusPanel.setFont (statusLabelFont);
    if (status.equals ("Ready")){
	statusPanel.add (statusReady);
	enableActionButtons();
    }else if (status.equals ("Running")){
	statusPanel.add (statusRunning);
	autoRunButton.disable();
        benchlist.disable();
        ////grouplist.disable();
        startButton.disable();
    }else if (status.equals ("Error")){
	statusPanel.add (statusError);
	enableActionButtons();
    }
    statusPanel.validate();
  }

  private void enableActionButtons(){
	autoRunButton.enable();
	if (testMode){
            benchlist.enable();
            ////grouplist.enable();
            startButton.enable();
        }
  }
  
    /**
  Initializations for status display
  */
  private void initStatus (Font font){
    statusPanel = new Panel();
    statusLabelFont = font;
    statusPanel.setLayout (new GridLayout(1,0));
    statusReady = initStatus (	"Ready", Color.black, Color.green);
    statusRunning = initStatus ("Running", Color.black, Color.yellow);
    statusError = initStatus (	"Error", Color.white, Color.red);
  }
  
  private Panel initStatus (String text, Color fg, Color bg){
    Panel p = new Panel();
    p.setLayout (new FlowLayout());
    p.setForeground (fg);
    p.setBackground (bg);
    p.add (new Label (text));
    return p;
  }
  
    ///////////////////////////////////////////////////////////////////////
    ///////////////////       DECLARATIONS    /////////////////////////////
    // Class Member Declarations
/** 
 *	Various Panels used.
 */
	private Panel panel1,panel2,panel3;
    
	Button helpButton;    // Help function -wnb
    
	boolean helpActive = false;
    
	boolean interactive = true;
    
	boolean showHelpInBrowser = false;
    
	Button viewButton;    
    
	Button startButton;
    
	Button setupButton;
    
	Button stopButton;
    
	Button reportButton;
    
	Button quitButton;
    
	Button selectAllButton;
    
	Button autoRunButton;
    
	//Button aboutButton;		// About in initial dialogue
    
	Button initialButton;	// OK in initial dialogue
    
	Button initialHelpButton;	// Help in initial dialogue
    
	Button cancelButton;	// Cancel in initial dialogue
    
	Choice grouplist;
    
	Choice speedlist;
    
	Choice cachelist;    
    
	List benchlist = new List(13,true);	
    
	TextField textfield;
    
	TextArea textarea;
    
	GridBagLayout gridbag = new GridBagLayout();
    
	//About creditsWindow;
	
	Logo specLogo;

	Color bgcolor;

	boolean	    anythingRun = false;// if any benchmark has started
	boolean     terminateAutorun = false;
        boolean	    valid;		// all current runs valid?
        Panel	    statusPanel;	// display status indicator
        Panel	    statusReady;	// status indicators: Ready
        Panel	    statusRunning;	//		Running
        Panel	    statusError;	//		Error
        Font	    statusLabelFont;	// font for status display

    int running_selection = 0;
//    float averagetime = 0.0f;       //unused
    float totalaverage = 0.0f;

    boolean end = false;

    ProgramRunner runner; // this runs the chosen program in a separate thread.

/*
    ConfigProp config;		// read user configuration file & present editing dialog
    static Properties props;	// all we know
*/
    SpecProps props;

    Vector   benchmarkGroups    = new Vector();
    String[] selectedBenchmarks = new String[0];

    public  static boolean standalone = false;

    protected int NUM_ITER = 0;


    /** This is a Hashtable where there is an entry for each benchmark. The associated data
    is another Hashtable with all the keys and values that should be placed in the output
    email. It is done this way so that it is easy to remove the results of a previous benchmark
    run */

    Hashtable runResults  = new Hashtable();



/************************************** Initial dialog *************************************/



    TextField[] fields;
    String[] propNames = {
	        "spec.initial.clientFile",
	        "spec.initial.serverFile",
	        "spec.initial.noteFile"
                };    
    String[] propNameLabels = {
	        "Client configuration:",
	        "Server configuration:",
	        "Tuning notes:",
                };    
    CheckboxGroup helpWhere;	// Help function -wnb
    CheckboxGroup runType;
    Checkbox checkTypeCompliant;
    Checkbox checkTypeTest;
    
    /**
    Create the initial window that appears
    The user must enter URL's of property files describing the client
    and server systems, and make other choices regarding benchmark
    operation. Sketch of screen showing use of panels p1..p9:

   /------------------\
   |WARNINGS - NOTICES| p1
   \------------------/

   /---------------------------------------------\
   |Property files describing your configuration:|
   |___________________________________________  | p2
   |___________________________________________  |
   \---------------------------------------------/
   /----------------------------------------------------------\
   |Type of run:                                              |
   | /----------------------------------------------------\   |
   | |* SPEC compliant  /---------------------------\     |   |
   | |     p10          |* test mode  p11           |     |   |
   | |                  |/---------------------\    |     |   |
   | |                  ||Benchmark set: [[[]]]| p3 |     |   |
   | |                  |\---------------------/    | p5  |p6 | p7
   | |                  \---------------------------/     |   |
   | \----------------------------------------------------/   |
   \----------------------------------------------------------/
   /----------------------------------------------------------\
   | * SPEC compliant run  * Test run  Benchmark set: [[[]]]  |p7
   \----------------------------------------------------------/
   no more p10,p11,p3,p5,p6
   /--------------------------------------------\
   | /-------------------------------------\    |
   | |Show help in:  * console   * browser | p8 |
   | \-------------------------------------/    |
   |            /----------------------\        | p12
   |            | [OK] [Help] [Cancel] | p9     |
   |            \----------------------/        |
   \--------------------------------------------/

    */
    void initialDialog() {
	Panel p1 = new Panel();
	Panel p2 = new Panel();
	Panel p4 = new Panel();
	Panel p7 = new Panel();
	Panel p8 = new Panel();
	Panel p9 = new Panel();
	Panel p12 = new Panel();
	Font helveticaBoldItalic = new Font("helvetica",
	    Font.BOLD+Font.ITALIC, 12);
	applet.setLayout(new FlowLayout());
	try{
	    bgcolor = new Color (Integer.parseInt (
	        props.get ("spec.initial.bgcolor","ffffff"), 16));
	}catch (NumberFormatException e){
	    bgcolor = Color.white;
	}
	applet.setBackground (bgcolor);

	Graphics g = applet.getGraphics();
	g.draw3DRect(30,10,20,50,false);

	// Warnings - notices - from "props/spec"
	Panel p13 = new Panel();
	p13.setLayout (new BorderLayout());
	specLogo = new Logo (getCbase(), props.get ("spec.initial.logo", "spec-sm.gif"), bgcolor);
	p13.add ("West", specLogo);
	p1.setLayout(new GridLayout(0,1));
	for (int i=1; ; i++){
	    Label lbl;
	    String msg = (String) props.get("spec.initial.message" + i);
	    if (msg == null)
		break;
	    lbl = new Label(msg ,Label.CENTER);
	    lbl.setFont (helveticaBoldItalic);
	    p1.add (lbl);
	}
	p13.add ("Center", p1);
	applet.add (p13);

	// property files describing client and server configurations
	p2.setLayout (new GridLayout (0,1));
	fields = new TextField[propNames.length];
	for (int i=0; i<propNames.length; i++){
	    String value = props.get( propNames[i] );
            if (value == null)
		value = "";
	    else
		value = getCbase() + value;
	    fields[i] = new TextField(value,45);
	    p2.add (new Label(propNameLabels[i]));
            p2.add(fields[i]);
	}
	applet.add (p2);

	/*
	Now working from inside out
	Choose whether this is a compliant SPEC run or just a test
	*/

	// Select benchmark set
	p7.add (new Label("Benchmarks:"));
        grouplist = new Choice();
	setBenchmarkList( (String)((Vector)benchmarkGroups.elementAt( 0 )).elementAt( 0 ) );
        for( int i = 0 ; i < benchmarkGroups.size(); i++) {
	    grouplist.addItem( (String)((Vector)benchmarkGroups.elementAt( i )).elementAt( 0 ) );
        }	
        String group = props.get ("spec.initial.default.group",compliantGroup);
        grouplist.select (group);
        setBenchmarkList (group);
	p7.add (grouplist);

	// Block of controls that go with test mode
	runType = new CheckboxGroup();
	checkTypeTest = new Checkbox("Test mode",runType,true);
	p7.add (checkTypeTest);

	// SPEC compliant or test mode
	p7.setLayout (new GridLayout (1,2));
	checkTypeCompliant = new Checkbox("SPEC compliant",runType,false);
	p7.add (checkTypeCompliant);

	applet.add (p7);

	// Choose where you want help displayed
	p12.setLayout (new GridLayout (2,1));
	p8.add( new Label("Show help in:"));
	helpWhere = new CheckboxGroup();
	p8.add(new Checkbox("console", helpWhere, false));
	p8.add(new Checkbox("browser", helpWhere, true));
	p12.add(p8);

	//Action buttons
	initialButton = new Button("OK");
	initialHelpButton = new Button("Help");
	cancelButton =  new Button("Cancel");
	p9.add(initialButton);
	p9.add(initialHelpButton);
	/*
	if (! standalone){
	    aboutButton = new Button("About");
	    p9.add(aboutButton);
	}
	*/
	if (standalone)
	    p9.add(cancelButton);
	p12.add(p9);

	// help and button panel
	applet.add(p12);
    
    }

/** 
 * Stores back the editied properties.
 */	
    private void storeProperties(){
	for (int i=0; i<propNames.length; i++){
	    props.put(propNames[i],fields[i].getText());
        }
	initPart2();
    }






    ///////////////////////   END OF CLASS   //////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    ///////////////////   BEGIN DEBUG METHODS /////////////////////////////

    ////////////////////   END DEBUG METHODS   ////////////////////////////
    ///////////////////////////////////////////////////////////////////////

}

