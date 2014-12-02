// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Jess.java
// An applet and a command line driver for testing the jess package
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess;
import spec.harness.*;
import spec.benchmarks._202_jess.jess.*;

///import java.awt.*;
import java.io.*;
///import java.applet.*;
import java.net.*;

/**
 A driver class for testing Rete
@author E.J. Friedman-Hill (C)1997
*/

public class Jess //extends Applet implements Runnable
{
  
  final private static int ROWS = 10;
  final private static int COLS = 60;    

  ///LostDisplay ld;
  String filename;
  Rete rete;
  Jesp jesp;
  PrintStream out;
  ///TextArea ta = null;
  ///Button bananas, grapes;
  String goal = "bananas";
  boolean first_time = true;
  boolean running = false;

/*	//use in SPEC harness instead of as standalone applet, wnb 2/98
  public void init()
  {
    setLayout(new BorderLayout());
    
    ta = new TextArea(ROWS, COLS);
    ta.setEditable(false);
    add("South", ta);
    out = new PrintStream(new TextAreaOutputStream(ta), true);

    Panel p = new Panel();
    p.setLayout(new BorderLayout());
    add("Center", p);
    Panel pp = new Panel();
    pp.setLayout(new FlowLayout());
    p.add("East",pp);
    pp.add(bananas=new Button("Find Bananas"));
    pp.add(grapes=new Button("Find Grapes"));

    
    ld = new LostDisplay(out, System.in, this);
    p.add("Center", ld);

    rete = new Rete(ld);
    filename = getParameter("INPUT");
    if (filename == null)
      filename = "examples/mab.clp";

    repaint();
  }

  public void start()
  {
    new Thread(this).start();
  }

  public void run()
  {
    if (running)
      return;
    running = true;
    InputStream fis;
    try
      {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
      }
    catch (Throwable t) {}// damn Netscape

    if (first_time) {
      // read in the given problem

      try
        {
          fis = new URL(getDocumentBase(), filename).openStream();
        }
      catch (Throwable ex)
        {
          showStatus("File not found or cannot open file");
          running = false;
          return;
        }
      
      if (fis != null)
        {
          jesp = new Jesp(fis, rete);
          try
            {
              jesp.parse(false);
              first_time = false;
              
            }
          catch (ReteException re)
            {
              ld.stderr().println(re);
            }
          
          try
            {
              fis.close();
            }
          catch (Throwable ex)
            {
              // so?
            }
        }
    }
    
    try
      {
        rete.ExecuteCommand("(reset)");
        rete.ExecuteCommand("(assert (goal-is-to (action eat) (argument-1 "
+ goal + ")))");
        rete.ExecuteCommand("(run)");
      }
    catch (Throwable ex)
      {
        showStatus("Error while running");
        running = false;
        return;
      }
    running = false;
  }

  public void stop()
  {
  }

  public void update(Graphics g)
  {
    paint(g);
  }
  
  public void paint(Graphics g)
  {
    ld.paint(g);
  }
  
  public boolean action(Event e, Object o) 
  {
    if (e.target == bananas)
      {
        goal = "bananas";
        start();
        return true;
      }
    else if (e.target == grapes)
      {
        goal = "grapes";
        start();
        return true;
      }
    return false;
  }
*/

  public long inst_main( String[] argv ) { 
    long startTime = System.currentTimeMillis();
    if (!run_jess(argv))
       return 0;
    return System.currentTimeMillis() - startTime;
  }
  
  public boolean run_jess(String[] argv) 
  {
    NullDisplay nd = new NullDisplay();
    Rete rete = new Rete(nd);

    // Here's how you add a package of user functions
    rete.AddUserpackage(new StringFunctions());
    rete.AddUserpackage(new PredFunctions());
    rete.AddUserpackage(new MultiFunctions());
    rete.AddUserpackage(new MiscFunctions());
    rete.AddUserpackage(new MathFunctions());
    
    spec.harness.Context.out.println("\nJess, the Java Expert System Shell");
    spec.harness.Context.out.println("Copyright (C) 1997 E.J. Friedman Hill"
                       + " and the Sandia Corporation");
    try
      {
        rete.ExecuteCommand("(printout t (jess-version-string) crlf crlf)");
      } 
    catch(ReteException re) {}

    InputStream fis = System.in;
    String name = argv.length == 0 ? null : argv[0];
    try
      {
        if (name != null)
          fis = new spec.io.FileInputStream(name);
      }
    catch (Throwable ex) 
      {
        nd.stderr().println("File not found or cannot open file");
        System.exit(0);
      }
    if (fis != null) 
      {
        Jesp j = new Jesp(fis, rete);
        do 
          {
            try
              {
                // Argument is 'true' for prompting, false otherwise
                j.parse(fis == System.in);
              }
            catch (ReteException re) 
              {
                re.printStackTrace(nd.stderr());
              }
            catch (ArrayIndexOutOfBoundsException ai) 
              {
                nd.stderr().println("Wrong number of arguments in funcall:");
                ai.printStackTrace(nd.stderr());
              }
          }
        // Loop if we're using the command line
        while (fis == System.in);
      }
      return true;
  }
}
