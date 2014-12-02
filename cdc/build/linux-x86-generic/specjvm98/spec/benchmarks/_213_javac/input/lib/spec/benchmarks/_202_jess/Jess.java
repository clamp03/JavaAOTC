// -*- java -*-
//////////////////////////////////////////////////////////////////////
// Jess.java
// A driver for testing Rete
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// TEST ONLY - NOT FOR DISTRIBUTION
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess;
// import spec.harness.*;


/**
 A driver class for testing Rete
@author E.J. Friedman-Hill (C)1996
*/

public class Jess /*extends Applet implements Runnable*/ {
  
  final private static int ROWS = 10;
  final private static int COLS = 60;    

  String filename;
  Rete rete;
  Jesp jesp;
  String goal = "bananas";
  boolean first_time = true;
  boolean running = false;


    public long inst_main( String[] argv ) { 

        long startTime = System.currentTimeMillis();
    
        if (!run_jess(argv))
            return 0;
        
        return System.currentTimeMillis() - startTime;
    }


  public boolean run_jess(String[] argv) {
    NullDisplay nd = new NullDisplay();
    Rete rete = new Rete(nd);

    java.io.InputStream fis = System.in;
    String name = argv.length == 0 ? "zebra.clp" : argv[0];
    try {
		
	fis =  new java.io.FileInputStream(name);
		
    } catch (Throwable ex) {
      nd.stderr().println("File not found or cannot open file");
	  return false;
    }
    if (fis != null) {
      Jesp j = new Jesp(fis, rete);
      try {
        j.parse();
      } catch (ReteException re) {
        re.printStackTrace(nd.stderr());
      }
    }
	return true;
  }
}
