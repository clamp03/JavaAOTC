/*
 * @(#)Harness.java	1.14 06/26/98
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * Modified by Kaivalya M. Dixit & Don McCauley (IBM) to read input files
 * This source code is provided as is, without any express or implied warranty.
 */

package spec.benchmarks._201_compress;
import spec.harness.*;

import java.io.*;

public final class Harness
{

    final static int COMPRESS = 0;
    final static int UNCOMPRESS = 1;

    private byte orig_text_buffer[];
    private byte comp_text_buffer[];

    private int fill_text_buffer(String infile) {
      int act = 0;
      int num_bytes = 0;
       
      try {
		
        spec.io.FileInputStream sif = new spec.io.FileInputStream(infile);
        num_bytes = (int)sif.getContentLength();
        
        // Only allocate size of input file rather than MAX - kmd
        // If compressed file is larger than input file this allocation 
	// will fail and out of bound exception will occur 
	// In real lie, compress will no do any compression as no
	// space is saved.-- kaivalya
	
	orig_text_buffer = new byte[num_bytes];
	comp_text_buffer = new byte[num_bytes];  

	int bytes_read;
	 while ( (bytes_read = sif.read(orig_text_buffer, act , (num_bytes - act))) > 0){
	   act = act +  bytes_read;
	 }

	 sif.close();    // release resources 
        
	 if ( act != num_bytes )
            {
            spec.harness.Context.out.println("ERROR reading test input file");
            }
      }
      catch (IOException e)
	  {
	 spec.harness.Context.out.println("ERROR opening/accessing input file: "+infile);
         };

      return act;
      }


    public Harness() {
	/*
	orig_text_buffer = new byte[BUFFERSIZE];
	comp_text_buffer = new byte[BUFFERSIZE];
	new_text_buffer = new byte[BUFFERSIZE];
	*/
    }

    public boolean run_compress(String[] args) {
	int count = 0;
	int i, oper;
	int comp_count, new_count;
	int fn = Integer.parseInt(args [0] );     // get number of files
	int loopct = Integer.parseInt(args [1] ); // get loop count

    spec.harness.Context.out.println( "Loop count = " + loopct );

    for (int cntr=0; cntr < loopct; cntr++ )  // iterate over
     for (int j=0; j < fn ; j++)            { // number of files
     count = fill_text_buffer( args [j+2] );  // give file names to read
     oper=COMPRESS;
     spec.harness.Context.out.println( count);  // write input file size
     
     // uncompress in the original text buffer.
     comp_count=Compress.spec_select_action(orig_text_buffer, count,
     				   oper, comp_text_buffer);
     spec.harness.Context.out.println( comp_count); // write compressed file size
	
     oper=UNCOMPRESS;
     new_count=Compress.spec_select_action(comp_text_buffer, comp_count,
     				  oper, orig_text_buffer); 
     // if uncompressed files size is not same as the original ERROR

     if ( new_count != count ) {
          spec.harness.Context.out.println ("Error : Number of Bytes should have been  " + count + " instead of " + new_count);
	    }

         // Release resources tor prevent  resource leak
         orig_text_buffer = null;
	 comp_text_buffer = null;
	
    }
	return true;
    }
		

    public long inst_main( String[] argv ) { 

        long startTime = System.currentTimeMillis();
    
        if (!run_compress(argv))
            return 0;
        
        return System.currentTimeMillis() - startTime;
    }

}

