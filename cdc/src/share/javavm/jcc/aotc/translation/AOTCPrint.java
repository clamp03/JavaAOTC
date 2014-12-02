package aotc.translation;

import java.io.*;

/**
 * AOTCPrint writes "C" code with correct indent.
 */
public class AOTCPrint extends PrintStream {

    private final int indent = 4;
    private int level = 0;

    public AOTCPrint(FileOutputStream stream) {
	super(stream);
    }

    /**
     * Writes without indent.
     *
     * @param str   the string which print
     */
    public void Print(String str) {
	println(str);
    }
    /**
     * Writes with indent.
     *
     * @param str   the string which print
     */
    public void Printf(String str) {
	if(str.length()==0) {
	    println("");
	    return;
	}
	if(str.charAt(0)=='}') {
	    level--;
	}
	if(str.charAt(0)!='#') {
	    for(int i=0; i<level*indent; i++) {
		print(" ");
	    }
	}
	println(str);
	if(str.charAt(str.length()-1)=='{') {
	    level++;
	}
    }
}
