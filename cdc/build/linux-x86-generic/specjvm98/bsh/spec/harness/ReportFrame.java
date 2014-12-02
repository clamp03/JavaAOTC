/*
 * @(#)ReportFrame.java	1.13 07/09/98
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
import java.util.*;


/**
 * This class represents the Frame window which displays the run results.
 * User has options to display the results in the full or brief format.
 * The results (full) can be sent to the specified email id entered in
 * the setup window by pressing the send button in this window
 */
public class ReportFrame extends Frame {
    
    private String getin( DataInputStream in ) {
    
        StringBuffer sb = new StringBuffer();
        
        try {             
            while( in.available() > 0 )
                sb.append( (char)in.read() );            
            } catch( Exception e ) {}          
        
        
        if( sb.length() > 0 ) {            
            spec.harness.Context.out.print( "<<<<" );
            spec.harness.Context.out.print( sb.toString() );
            spec.harness.Context.out.flush();
        }
        
        return sb.toString();
    }

/**
 Check for data with a DataInputStream given. This call times out after the 
 specified time
 */
    private void inwait( DataInputStream in, int time ) {
        
        for( int i = 0 ; i < time ; i+= 100 ) {
            try {
                if( in.available() > 0 )
                    return;
                Thread.sleep( 100 );
                } catch( Exception e ) { return; }                    
        }    
    }
        

    private boolean outin( PrintStream out, DataInputStream in, boolean response, String msg ) {    
    
        spec.harness.Context.out.println( "===============================================" );      
    
        getin( in );
    
        out.println( msg );
        spec.harness.Context.out.println( ">>>>" + msg );
        spec.harness.Context.out.flush();
        
        if( response )
            inwait( in, 3000 );
        
        getin( in );
                
        return false;
    }

/**
 * This method is called  when the user presses the send button. This opens a
 * socket to the port number 25 of the specified SMTP server and sends the run
 * results in the e-mail format to the address specified in the setup window.
 * Hides the report window after sending the e-mail
 */  
    void sendClicked(Event event) {
    
	String from = props.get("spec.testx.emailReturn", "SPEC-JVM-Client-Benchmark");

//spec.harness.Context.out.println("Mailing to: " + actfield.getText() );
       
        Socket s = null;
        PrintStream out = null;
        DataInputStream in = null;
       
        try {
            s   = new Socket( serverName, 25 );
            out = new PrintStream( s.getOutputStream() );
            in  = new DataInputStream( s.getInputStream() );
            
            if( outin( out, in, true,  "HELO "        + serverName ) )           return;
            if( outin( out, in, true,  "MAIL FROM: " + from ) )                  return;
            if( outin( out, in, true,  "RCPT TO: " + actfield.getText() ) )      return;
            if( outin( out, in, true,  "DATA") )                                 return;
            if( outin( out, in, false, "Subject: SPEC JVM Client98 Results" ) )           return;
            if( outin( out, in, false, "To: " + actfield.getText() ) )           return;
            if( outin( out, in, false, "From: " + from) )			 return;           
//	    if( outin( out, in, false, props.toString() ) )			 return;
	    if( outin( out, in, false, results ) )		      	         return;	    
            if( outin( out, in, true,  ".") )                                    return;
            if( outin( out, in, true,  "QUIT") )                                 return;                     

        } catch(Exception e){
            System.err.println("Message Send error: " + e.toString());
        } finally {
            try{ out.close(); } catch( Exception x ) {}
            try{ in.close();  } catch( Exception x ) {}
            try{ s.close();   } catch( Exception x ) {}
            hide();
        }
    }

/** 
 * Hides the report Window 
 */
    void cancelClicked(Event event) {
        hide();
    }

/** 
 * Shows the Window. SpecJava can be run in the non-interactive mode. In the
 * non-interactive mode, the report is automatically sent after finishing the
 * benchmark suite
 */
 
    public synchronized void show() {
        move(50, 50);
        super.show();
	if (!interactive)
    	    sendClicked ((Event) null );
    }

/** 
 * This method is called when the operator presses on any of the Send, Full
 * buttons or closes this window.
 */
	public boolean handleEvent(Event event) {
        if (event.id == Event.WINDOW_DESTROY) {
            hide();         // hide the Frame
            return true;
        }

        if (event.target == fullBrief && event.id == Event.ACTION_EVENT) {
            fullBriefClicked(event);
            return true;
        }

        if (event.target == calculate && event.id == Event.ACTION_EVENT) {
            calculateClicked(event);
            return true;
        }

        if (event.target == send && event.id == Event.ACTION_EVENT) {
            sendClicked(event);
            return true;
        }
	
        if (event.target == cancel && event.id == Event.ACTION_EVENT) {
            cancelClicked(event);
            return true;
        }

        return super.handleEvent(event);
    }

/** 
 * This method is called when the user clickes on the Full(Brief) button. The
 * benchmark run results can be viewed in the full or brief format.
 */
 
    void fullBriefClicked(Event event) {    
	resetResults( (fullBrief.getLabel()).equals("Full") );
    }

/** 
 * This method is called when the user clickes on the calculate button. A
 * quick calculation of ratios of the benchmark run results is viewed.
 */
 
    void calculateClicked(Event event) {    
	textarea.setText ((new QuickCalc (props, runResults)).toString());
    }

/**
 * The results are displayed in the text area depending on the user selection
 * of full report or brief report. In brief report, user can see only the 
 * runtimes of the benchmarks. In the 'Full' mode, user can see the IO times
 * the validity of the run, Spec properties.
 */
    void resetResults( boolean full ) {
    
	StringBuffer brief = new StringBuffer();
    
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	
	for( Enumeration e = runResults.elements() ; e.hasMoreElements() ; ) {
	    Properties pp = (Properties)e.nextElement();
	    pp.save( baos, null );
	    for( Enumeration f = pp.keys() ; f.hasMoreElements() ; ) {
  		String key = (String)f.nextElement();
		if( key.endsWith( ".time" ) ) {
		    brief.append( key );
		    brief.append( "=" );
		    brief.append( pp.get( key ) );
		    brief.append( "\n" );
		}
	    }
	}
          
	results = QuickSort.sort( props.toString() + baos.toString(), false );    
	
	textarea.setText(   (full) ? results : QuickSort.sort( brief.toString() ) );
	fullBrief.setLabel( (full) ? "Brief" : "Full" );
	
    }


/**
 * Constructor for the ReportFrame.
 * @param runResults Run results of the benchmark
 * @param props Spec properties. Information about the JVM vendor. Client 
 * machine properties.
 * @srvName SMTP server used for sending the report in e-mail format
 * @interactive Flag indicating whether SpecJVMClient98 is run in interactive
 * mode or not
 */
    public ReportFrame( Hashtable runResults, SpecProps props,
	String srvName, boolean interactive ) {
    
	this.props      = props;
	this.runResults = runResults;
        serverName = srvName;
	this.interactive = interactive;

        addNotify();

        panel1 = new Panel();
        panel1.setLayout(gridbag);
        constraints.constrain(panel1, new Label( SpecJava.version ), 0, 0, 1, 1,
            GridBagConstraints.NONE,GridBagConstraints.CENTER, 0.3, 0.0, 0, 0, 0, 0);

        panel2 = new Panel();
        panel2.setLayout(gridbag);
        textarea = new TextArea(20, 40);
        textarea.setEditable(false);
        textarea.setFont(new Font("Helvetica", Font.BOLD, 12));
        textarea.setBackground(Color.lightGray);

        constraints.constrain(panel2, textarea, 0, 1, 3, 3, GridBagConstraints.HORIZONTAL,
            GridBagConstraints.NORTH, 1.0, 0.0, 0, 0, 0, 0);

	fullBrief = new Button( "????" );
	calculate = new Button ("Calculate");

        actlabel = new Label("Mail to ");
/*** switching from various hash tables to SpecProps   
*/
	String emailTo = props.get("spec.testx.emailTo","root@localhost");
        actfield = new TextField(emailTo);

        panel3 = new Panel();
        panel3.setLayout(gridbag);
/*** NS ***        
        constraints.constrain(panel3, new Label("Report Format  "), 0, 0, 1, 1, GridBagConstraints.NONE,
            GridBagConstraints.EAST, 0.3, 0.0, 0, 0, 0, 0);
        constraints.constrain(panel3, new Label("Submit  "), 2, 0, 1, 1, GridBagConstraints.NONE,
            GridBagConstraints.EAST, 0.3, 0.0, 0, 0, 0, 0);
        constraints.constrain(panel3, choice2, 1, 0, 1, 1, GridBagConstraints.NONE,
            GridBagConstraints.WEST, 0.3, 0.0, 0, 0, 0, 0);
        constraints.constrain(panel3, choice3, 3, 0, 1, 1, GridBagConstraints.NONE,
            GridBagConstraints.WEST, 0.3, 0.0, 0, 0, 0, 0);
*** NS ***/  
        constraints.constrain(panel3, fullBrief, 2, 1, 1, 1, GridBagConstraints.NONE,
            GridBagConstraints.EAST, 0.3, 0.0, 0, 0, 0, 0);          
        constraints.constrain(panel3, calculate, 1, 1, 1, 1, GridBagConstraints.NONE,
            GridBagConstraints.EAST, 0.3, 0.0, 0, 0, 0, 0);          
        constraints.constrain(panel3, actlabel, 3, 1, 1, 1, GridBagConstraints.NONE,
            GridBagConstraints.EAST, 0.3, 0.0, 0, 0, 0, 0);
        constraints.constrain(panel3, actfield, 4, 1, 1, 1, GridBagConstraints.NONE,
            GridBagConstraints.WEST, 0.3, 0.0, 0, 0, 0, 0);

        panel4 = new Panel();
        panel4.setLayout(gridbag);
        constraints.constrain(panel4, send, 0, 1, 1, 1, GridBagConstraints.BOTH,
            GridBagConstraints.CENTER, 0.3, 0.0, 0, 0, 0, 0);
        constraints.constrain(panel4, cancel, 1, 1, 1, 1, GridBagConstraints.BOTH,
            GridBagConstraints.CENTER, 0.3, 0.0, 0, 0, 0, 0);

	resetResults( false );
	
        setTitle("SPEC JVM Client98 Report");
        resize(insets().left + insets().right + 520,insets().top + insets().bottom + 480);

        // Finally, use a GridBagLayout to arrange the panels themselves
        this.setLayout(gridbag);
        this.setBackground(c6);
        constraints.constrain(this, panel1, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL,
            GridBagConstraints.NORTH, 1.0, 0.0, 5, 0, 0, 0);
        constraints.constrain(this, panel2, 0, 1, 1, 1, GridBagConstraints.BOTH,
            GridBagConstraints.NORTH, 1.0, 1.0, 10, 10, 5, 10);
        constraints.constrain(this, panel3, 0, 2, 1, 1, GridBagConstraints.HORIZONTAL,
            GridBagConstraints.NORTH, 1.0, 0.0, 5, 0, 0, 0);
        constraints.constrain(this, panel4, 0, 3, 1, 1, GridBagConstraints.BOTH,
            GridBagConstraints.CENTER, 1.0, 1.0, 10, 10, 5, 10);

    }

/**
 Panels for placing the UI components
 */
	private Panel panel1,panel2,panel3,panel4;

/**
 * text area used to display the run results. The run results can be 
 * viewed in two ways. 
 */
    TextArea textarea;
    
/**
 * Mail-to Label
 */
     Label actlabel;
    
/**
 * Text field in which Reciever mailing id can be entered. This can be set in
 * in the setup window also.
 */
	TextField actfield;

/** 
 * Full/Brief button
 */
	Button fullBrief;

/**
 * Calculate button
 */
      Button calculate;
      	
//    Choice choice2;
//    Choice choice3;
    
/**
 * Flag indicating whether SpecJVMClient98 is running in interactive mode or not
 */
 	boolean interactive;
    
/**
 * Benchmark run results
 */
 	Hashtable runResults;

/**	
 Send button 
 */
    
	Button send   = new Button("Send");

/**
 Cancel button - Disposes the report dialog
 */	    
	Button cancel = new Button("Cancel");;

/**
 Background of the Report frame window    
 */
	Color c6 = new Color(0xffffe6);  // light yellow

/**
 Layout manager associated with this window
 @see java.awt.GridBagLayout
 */
    GridBagLayout gridbag = new GridBagLayout();
/**
 Gridbag layout constraints
 @see java.awt.GridBagLayout
 @see spec.harness.Constraints
 @see java.awt.GridBagConstraints
 */	
    Constraints constraints = new Constraints();

/**
 Spec properties
 */		
    SpecProps props;
/**
 Formated results. The run results, Spec properties are concatinated together
 to form a single string. This string is displayed in the TextArea. 
 */	
    String results = "";

/**
 Mail server name
 */		
    String serverName;
}
