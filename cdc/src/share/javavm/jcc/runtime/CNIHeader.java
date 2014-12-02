/*
 * @(#)CNIHeader.java	1.12 06/10/10
 *
 * Copyright  1990-2006 Sun Microsystems, Inc. All Rights Reserved.  
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER  
 *   
 * This program is free software; you can redistribute it and/or  
 * modify it under the terms of the GNU General Public License version  
 * 2 only, as published by the Free Software Foundation.   
 *   
 * This program is distributed in the hope that it will be useful, but  
 * WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU  
 * General Public License version 2 for more details (a copy is  
 * included at /legal/license.txt).   
 *   
 * You should have received a copy of the GNU General Public License  
 * version 2 along with this work; if not, write to the Free Software  
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  
 * 02110-1301 USA   
 *   
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa  
 * Clara, CA 95054 or visit www.sun.com if you need additional  
 * information or have any questions. 
 *
 */

package runtime;
import consts.Const;
import jcc.Util;
import util.*;
import components.*;
import vm.*;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
// AOTC - BY Clamp **********************************
import aotc.*;
// **************************************************

public class CNIHeader extends HeaderDump{

    public CNIHeader( ){
	super( '_' );
    }
    // AOTC - BY Clamp **************************************************************
    class ParamTypeGenerator extends util.SignatureIterator {
        String  returnType;
        Vector  paramTypes = new Vector();

        ParamTypeGenerator( String s ){ super( s ); }

        private void put( String typestring ){
            if ( isReturnType ) returnType = typestring;
            else paramTypes.addElement( typestring );
        }

        public void do_boolean(){ put("jboolean"); }
        public void do_byte(){    put( "jbyte"); }
        public void do_short(){   put( "jshort" ); }
        public void do_char(){    put( "jchar" ); }
        public void do_int(){     put( "jint" ); }
        public void do_long(){    put( "jlong" ); }
        public void do_float(){   put( "jfloat" ); }
        public void do_double(){  put( "jdouble" ); }
        public void do_void(){    put( "void" ); }

        public void do_array( int depth, int start, int end ){
            if ( depth > 1 ){
                put("jobjectArray");
            }else switch (sig.charAt(start)){
                case Const.SIGC_BYTE:   put( "jbyteArray" );
                                        break;
                case Const.SIGC_CHAR:   put( "jcharArray" );
                                        break;
                case Const.SIGC_SHORT:  put( "jshortArray" );
                                        break;
                case Const.SIGC_BOOLEAN:put( "jbooleanArray" );
                                        break;
                case Const.SIGC_INT:    put( "jintArray" );
                                        break;
                case Const.SIGC_LONG:   put( "jlongArray" );
                                        break;
                case Const.SIGC_FLOAT:  put( "jfloatArray" );
                                        break;
                case Const.SIGC_DOUBLE: put( "jdoubleArray" );
                                        break;
                case Const.SIGC_CLASS:
                case Const.SIGC_ARRAY:  put( "jobjectArray" );
                                        break;
                default:        put( "void *" );
                                break;
            }
        }

        public void do_object( int start, int end ){
            String name = className(start,end);
            if ( name.equals("java/lang/String") ){
                put( "jstring");
            }else if ( name.equals("java/lang/Class") ){
                put( "jclass");
            }else if ( JNIHeader.isThrowable( name ) ){
                put( "jthrowable");
            } else {
                put( "jobject");
            }
        }

    }
    // ******************************************************************************

    private boolean
    generateNatives( MethodInfo methods[], boolean cplusplusguard ){
	if ( methods == null ) return false;
	boolean anyMethods = false;
	for ( int i =0; i < methods.length; i++ ){
	    MethodInfo m = methods[i];
	    if ( (m.access&Const.ACC_NATIVE) == 0 ) continue;
	    if ( cplusplusguard && ! anyMethods ){
		o.println("#ifdef __cplusplus\nextern \"C\"{\n#endif");
		anyMethods = true;
	    }
	    String methodsig = m.type.string;
	    o.println("/*");
	    o.println(" * Class:	"+className);
	    o.println(" * Method:	"+m.name.string);
	    o.println(" * Signature:	"+methodsig);
	    o.println(" */");

	    //
	    // decide whether to use long name or short.
	    // we use the long name only if we find another
	    // native method with the same short name.
	    //
	    String nameParams = null;
        // AOTC - BY Clamp **************************************************************
        int count = 0;
        for(int j = 0; j < methods.length; j ++ ) {
            MethodInfo other = methods[j];
            if((other.access&Const.ACC_NATIVE) != 0 && (other.access&Const.ACC_AOTC) == 0) {
                if(other.name.equals(m.name)) count++;
            }
        }
        if(count != 1 || (m.access&Const.ACC_AOTC) != 0) {
            // ******************************************************************************

	    for ( int j =0; j < methods.length; j++ ){
		MethodInfo other = methods[j];
		if ( (other.access&Const.ACC_NATIVE) == 0 ) continue;
		if ( other == m ) continue; // don't compare with self!
		if ( other.name.string.equals( m.name.string ) ){
		    nameParams = methodsig;
		    break;
		}
	    }
        // AOTC - BY Clamp **************************************************************
        }
        // ******************************************************************************

	    // print return type and name.
        // AOTC - BY Clamp **************************************************************
        if((m.access & Const.ACC_AOTC)!=0) {
            // Call Arg - BY Clamp *****
            if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.CNI_TYPE) {
                o.print("JNIEXPORT int Java_");//ikhyun 061226
                o.print(Util.convertToJNIName(className, m.name.string, nameParams).substring(5));
                o.print("(CVMExecEnv*, CVMStackVal32*);\n");
            }else {
                ParamTypeGenerator ptg;
                try {
                    ptg = new ParamTypeGenerator( methodsig );
                    ptg.iterate();
                } catch ( Exception e ){
                    e.printStackTrace();
                    return false;
                }
                o.print("JNIEXPORT ");
                o.print( ptg.returnType);
                o.print(" JNICALL ");
                o.println( "CNI" + Util.convertToJNIName( className, m.name.string, nameParams ).substring(5) );
                // print parameter list
                if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.JNI_TYPE
                    || AOTCWriter. METHOD_ARGUMENT_PASSING == AOTCWriter.WRAPPER_TYPE) {
                    o.print("  (CVMExecEnv *,");
                }else{
                    o.print("  (CVMExecEnv *,CVMStackVal32*,int*,");
                }
                Enumeration tlist = ptg.paramTypes.elements();
                // implicit first parameter of "this" or class pointer
                if ( m.isStaticMember() ) o.print(" jclass");
                else o.print(" jobject");

                while (tlist.hasMoreElements()){
                    o.print(", ");
                    o.print( tlist.nextElement() );
                }
                o.println(");\n");
                if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.WRAPPER_TYPE) {
                    o.print("extern int ");
                    o.println( "ParseArgAndCall_CNI" + Util.convertToJNIName( className, m.name.string, nameParams ).substring(5) );
                    o.print("  (CVMExecEnv*, CVMStackVal32*);\n");
                }
            }
            // ****************************
        } else {
            // ******************************************************************************

	    o.print("CNINativeMethod ");
	    String name =
		Util.convertToJNIName( className, m.name.string, nameParams );
	    name = "CNI" + name.substring(5);
	    o.println( name + ";");
        // AOTC - BY Clamp **************************************************************
        }
        // ******************************************************************************

	}
	if ( cplusplusguard && anyMethods ){
	    o.println("#ifdef __cplusplus\n}\n#endif");
	    return true;
	}
	return false;
    }

    private void prolog(){
	o.println("/* DO NOT EDIT THIS FILE - it is machine generated */\n" +
	    "#include \"javavm/include/cni.h\"");
    // AOTC - BY Clamp **************************************************************
    o.println("#include \"jni.h\"");
    // ******************************************************************************
	o.println("/* Header for class "+className+" */\n");
	String includeName = "_CVM_CNI"+strsub(className,CDelim);
	o.println("#ifndef "+includeName);
	o.println("#define "+includeName);
    }
    private void epilog(){
	o.println("#endif");
    }
    // LG DTV - BY Clamp (JNI Upate) *****
    synchronized public boolean
    dumpHeader( ClassInfo c, PrintStream outfile, boolean jniUpdate ){
    // ***********************************
    // LG DTV - BY Clamp (JNI Upate, Commented) *****
    // synchronized public boolean
    // dumpHeader( ClassInfo c, PrintStream outfile ){
    // ***********************************
	boolean didWork;
	o = outfile;
	className = c.className;
	prolog();
	didWork = generateConsts( Util.convertToClassName( c.className ), c.fields );
	didWork |= generateNatives( c.methods, true );
	epilog();
	return didWork;
    }
    // LG DTV - BY Clamp (JNI Upate) *****
    synchronized public boolean
    dumpExternals( ClassInfo c, PrintStream outfile, boolean jniUpdate ){
    // ***********************************
    // LG DTV - BY Clamp (JNI Upate, Commented) *****
    // synchronized public boolean
    // dumpExternals( ClassInfo c, PrintStream outfile ){
    // ***********************************
	boolean didWork;
	o = outfile;
	className = c.className;
	didWork = generateNatives( c.methods, false );
System.err.println("CNIHeader " + className + " didWork " + didWork);
	return didWork;
    }
}
