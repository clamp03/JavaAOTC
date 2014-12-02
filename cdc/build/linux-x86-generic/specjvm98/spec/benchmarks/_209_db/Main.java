/*
 * @(#)Main.java	1.17 06/17/98
 *
 * Main.java   Version 2.0 03/24/98 rrh, kaivalya, salina
 * Randy Heisch       IBM Corp. - Austin, TX
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1996,1997,1998 IBM Corporation, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies.
 *
 * IBM MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

package spec.benchmarks._209_db;
import spec.harness.*;

import java.io.*;
import java.util.*;


public class Main implements SpecBenchmark
{
	private boolean standalone = true;

static void help()
   {
   spec.harness.Context.out.println("a - add record");
   spec.harness.Context.out.println("b - show beginning record");
   spec.harness.Context.out.println("d - delete record");
   spec.harness.Context.out.println("e - show end record");
   spec.harness.Context.out.println("f - find record");
   spec.harness.Context.out.println("m - modify record");
   spec.harness.Context.out.println("n - next entry");
   spec.harness.Context.out.println("p - previous record");
   spec.harness.Context.out.println("q - quit");
   spec.harness.Context.out.println("w - write database");
   spec.harness.Context.out.println("s - sort");
   spec.harness.Context.out.println(". - current record number");
   spec.harness.Context.out.println("x - Total records");
   spec.harness.Context.out.println("num - goto record number 'num'");
   }




    static long runBenchmark( String[] args ) {
    
     int speed =  spec.harness.Context.getSpeed();   
    
//      if( args.length == 0 ) {
        if( speed == 100 ) {
	    args = new String[2];
  	    args[0] = "input/db6";
  	    args[1] = "input/scr6";
	}
        if( speed == 10 ) {
	    args = new String[2];
  	    args[0] = "input/db2";
  	    args[1] = "input/scr2";
	}
        if( speed == 1  ) {
	    args = new String[2];
  	    args[0] = "input/db4";
  	    args[1] = "input/scr4";
	}
	
  	Database.printRecords = false;	//**NS**
	return new Main().inst_main( args );
    }


    public static void main( String[] args ) {  	 
        runBenchmark( args );
    }

    
    public long harnessMain( String[] args ) {
        return runBenchmark( args );
    }

  public long inst_main(String[] argv) {

    long starttime = System.currentTimeMillis();
    try {
	int iter = 1; //spec.harness.Context.getSpeed();
	spec.harness.Context.out.println( "db " + iter + " iterations " );
	
	for( int i = 0 ; i < iter ; i++ ) { //**NS**
            run(argv);
	}
    } catch (IOException e) {
        spec.harness.Context.out.println("Error in run() method");
    }
    return System.currentTimeMillis() - starttime;
  }


  public void run(String[] arg)  throws IOException
   {
   int i;
   String s;
   Database db;
   DataInputStream dis = null;
   dis = new DataInputStream( new spec.io.FileInputStream( arg[1]) );
   
   boolean OK = true;
   boolean changed = false;
   int rec;
   char cmd, last = ' ';


   db = new Database(arg[0]);


   while ( OK )
      {
      // Reduce output
      //spec.harness.Context.out.print(": "); spec.harness.Context.out.flush();

      s = dis.readLine();

      if ( s.length() == 0 )
	 cmd = last;
      else
	 {
	 cmd = s.charAt(0);

         if ( (cmd <= '9') && (cmd >= '0') )
	    {
	    rec = Integer.parseInt(s);
	    db.gotoRec(rec);
            }
         }

      switch(cmd)
         {
         case 'a':
            db.add(db.getEntry(dis));
            //spec.harness.Context.out.println("Number of records: "+db.numRecords());
            // Really reduce output
            //spec.harness.Context.out.println(db.numRecords()+" ");
            changed = true;
         break;

         case 'h':  help(); break;

         case 'd':
	    //spec.harness.Context.out.print("Delete record "+(db.currentRec()+1)+" y/n? ");
	    //spec.harness.Context.out.print("d "+(db.currentRec()+1)+" y/n? ");
	    //spec.harness.Context.out.print(db.currentRec()+1+" ");
	    //spec.harness.Context.out.flush();
            s = dis.readLine();
	    if ( s.length() > 0 )
	       {
	       if ( s.charAt(0) == 'y' )
		  {
	          db.remove();
                  //spec.harness.Context.out.println("Number of records: "+db.numRecords());
                  //spec.harness.Context.out.println(db.numRecords());
                  changed = true;
		  }
               }

            cmd = ' ';
         break;

         case 'b':  db.list(); break;

         case 'x':  db.status(); break;

	 case 'e':  db.end(); break;

         case 'm':  
	    db.modify(dis); 
	    changed = true;
            cmd = ' ';
         break;

         case 'n':  db.next(); break;

         case 'p':  db.previous(); break;

         case 's':  db.sort(dis); break;

	 case 'f':  db.find(dis); break;

         case 'w':
            db.write_db();
            changed = false;
            cmd = ' ';
         break;

         case '.':  db.printRec(); break;

         case 'q': OK = false; break;
         }

      last = cmd;
      }


   if ( changed )
      {
      spec.harness.Context.out.print("Save database (y or n)? "); spec.harness.Context.out.flush();
      s = dis.readLine();

      if ( (s.charAt(0) != 'n') && standalone )
         db.write_db();
      }
            
   dis.close();
   }


}
