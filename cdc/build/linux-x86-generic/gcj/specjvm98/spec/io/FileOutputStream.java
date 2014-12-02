/*
 * @(#)FileOutputStream.java	1.11 06/17/98
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

package spec.io;
import spec.harness.Context;

/**
 * Dummy output stream that is only used as a place holder and for a little
 * benchmark validity checking
 */ 
public class FileOutputStream extends java.io.OutputStream {

    /**
     * Development debug flag
     */
    final static boolean debug = false;
    
    /**
     * Count of bytes written
     */
    static int byteCount = -99999999;
    
    /**
     * Checksum
     */
    static int checksum = 0;

    /** 
     * Standard constructor. It creates the file if it is not existing, but it
	 * truncates the existing data if the file already exists
     */
    public FileOutputStream(String name) throws java.io.IOException{
    }
  
    /** 
     * Standard constructor. It opens the existing file but doesn't truncate
	 * existing data
	 * @see java.io.FileOutPutStream
     */
    public FileOutputStream(String name, boolean append) throws java.io.IOException {
    }

    /** 
     * Standard constructor
     */
    public FileOutputStream(spec.io.File file) throws java.io.IOException {
    }
 
    /** 
     * Writes specified byte into output stream.
	 * @param b - byte to be written
     */
    public void write(int b) throws java.io.IOException {   
	 byteCount++;
	 checksum += specialTrace("W", b ); 
    }

    /** 
     * Write b.length bytes into output stream
	 * @param b - byte array to be written
     */
    public void write(byte b[]) throws java.io.IOException {
	int len = b.length;    
	byteCount += len;
	if (len > 0) {
   	    checksum += specialTrace("X", b[0] );; // Just checksum the first character
	}	
	if( debug ) {
            System.out.println("Inside spec.io.FileOutStream.write() -- throwing away output");
	}
    }

/** 
 Writes len bytes from the specified byte array starting at offset off to this 
 output stream. The write method of FilterOutputStream calls the write method 
 of one argument on each byte to output. 

 Note that this method does not call the write method of its underlying input 
 stream with the same arguments. Subclasses of FilterOutputStream should 
 provide a more efficient implementation of this method
*/
    public void write(byte b[], int off, int len) throws java.io.IOException {
	byteCount += len;

	if (len > 0) {
   	    checksum += specialTrace("Z",b[off]); // Just checksum the first character
	}	
	if( debug ) {    
            System.out.println("Inside spec.io.FileOutStream.write() -- throwing away output");
	}
    }
    
    /** 
     * Chears the byte counter
     */    
    public static void clearCount() {
	byteCount = 0;
	checksum  = 0;
    }

    /** 
     * Prints the byte counter in validity check mode '1'
	 * @param check - Indicates whether the validation is enabled or not
     */
    public static void printCount(boolean check) {
        spec.io.PrintStream s = (spec.io.PrintStream)Context.out;
        if( check ) {
  	    s.println('1',"File output byte count = "+byteCount+" checksum = "+checksum);
        } else {  	
  	    s.println('1',"File output byte count = "+byteCount);
  	}
    }
    

    public static int specialTrace(String rtn, int ch) {
//	((spec.io.PrintStream)Context.out).println('1',rtn+ch);
	return ch;
    }    
}
  
		      
