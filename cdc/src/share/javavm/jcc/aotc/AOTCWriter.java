package aotc;

import java.io.*;
import components.*;
import util.*;
import aotc.translation.*;
import aotc.share.*;

/**
 * AOTCWriter is the top class of AOTC.
 */
public class AOTCWriter {

    private static AOTCPrint out;
    public static final boolean debug = false; // if this is on, aotc won't generate code (just build with previous aotced c files)

    /////////////////////////////////////////////
    // Options that control whether to make profile information
    //public static final boolean PROFILE_CALL_COUNT = false;
    //public static final boolean PROFILE_LOOPPEELING = false;
    public static final boolean MOVE_CI_CHECK_TO_LOOP_PREHEADER = false; // move class initialization checks to loop preheader --> always turn this off

    // Options that control optimizations
    public static final boolean REMOVE_REDUNDANT_CI_CHECK = true; // class initialization check
    public static final boolean REMOVE_REDUNDANT_NULL_CHECK = true; // remove redundant null check
    public static final boolean REMOVE_REDUNDANT_BOUND_CHECK = true; // remove redundant array bound check
    public static final boolean REMOVE_REDUNDANT_REFERENCE_COPY = true; // remove redundant reference copy
    public static final boolean REMOVE_REDUNDANT_FRAME = true; // remove redundant reference copy
    public static final boolean INLINE_STATICCALL = true; // inline static & virtual methods
    public static final boolean INLINE_VIRTUALCALL = true; // inline virtual methods
    public static final boolean USE_GNU_C_EXTENSION = true;  //__builtin_expect(x,c);
    public static final boolean USE_CALL_COUNT_INFO = true; // use call count information (aotc profile)
    public static final boolean DO_LOOPPEELING = false;

    public static int JNI_TYPE = 0;
    public static int CNI_TYPE = 1;
    public static int MIX_TYPE = 2;
    public static int WRAPPER_TYPE = 3;
    public static int METHOD_ARGUMENT_PASSING = 3;

    public static int CALL_CHECK_HANDLING = 0;
    public static int SETJMP_PROLOG_HANDLING = 1;
    public static int SETJMP_TRY_HANDLING = 2;
    public static int EXCEPTION_IMPLEMENTATION = 0;

    /**
     * Translates the method.
     *
     * @param cls	the class information
     * @param info	the method information
     * @param name	the method name
     * @param nativeTypes the CNI class identifier
     */
    public static void translate(ClassInfo cls, MethodInfo info, String name,
				 ClassnameFilterList nativeTypes) {
	if(debug) return;
	MethodTranslator translator = new MethodTranslator(cls, info, name, out, nativeTypes);
	try {
	    translator.translate();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    /**
     * Opens the output file.
     *
     * @param fileName	the output file name
     */
    public static void open(String fileName) {
	if(debug) return;
	//if(debug) fileName = "temp_debug";
	try {
	    out = new AOTCPrint(new FileOutputStream(fileName));
	} catch ( java.io.IOException e ){
	    e.printStackTrace();
	}
	writeHeader(out);
    }
    /**
     * Closes the output file.
     */
    public static void close(){
	if(debug) return;
	out.close();
    }

    private static void writeHeader(AOTCPrint out){
	for( int i = 0 ; i < header.length ; i++ ) {
	    out.Printf(header[i]);
	}   /*
        if (PROFILE_CALL_COUNT) {
            out.Printf("#define AOTC_PROFILE_CALL_COUNT");
        }
	if (PROFILE_LOOPPEELING) {
	    out.Printf("#define AOTC_PROFILE_LOOPPEELING");
	}
    */
        if (USE_GNU_C_EXTENSION) {
            out.Printf("// used for branch prediction");
            out.Printf("#if __GNUC__ == 2 && __GNUC_MINOR__ < 96");
            out.Printf("//#define __builtin_expect(x, expected_value) (x)");
            out.Printf("#error Why dont you upgrade your GCC version");
            out.Printf("#endif");
            out.Printf("");
            out.Printf("#define likely(x)   __builtin_expect((x),1)");
            out.Printf("#define unlikely(x) __builtin_expect((x),0)");
        } else {
            out.Printf("#define likely(x)     (x)");
            out.Printf("#define unlikely(x)   (x)");
        }
        out.Printf("");
    }

    /**
     * Writes "C" codes which aotc functions uses.
     *
     * @param filename	the output file name
     * @param infile	the input file name
     */
    public static void writeAOTCHeader(String filename, String infile) {
	AOTCPrint mOut;
	String headerDef = "_" + infile.replace('.', '_').toUpperCase() + "_";
	infile = "../../src/share/javavm/jcc/aotc/" + infile;
	try {
	    mOut = new AOTCPrint(new FileOutputStream(filename));
	    BufferedReader in = new BufferedReader(new FileReader(infile));
            
            mOut.Print("#ifndef " + headerDef);
	    mOut.Print("#define " + headerDef);
	    writeHeader(mOut);
            
            
	    String buf;
	    for(buf=in.readLine(); buf!=null; buf=in.readLine()) {
		mOut.Print(buf);
	    }

            mOut.Print("");
            mOut.Print("");
	    mOut.Print("#endif // " + headerDef);
	    mOut.close();
	} catch ( java.io.IOException e ){
	    e.printStackTrace();
	}
    }
    public static void writeAOTCFunctions(String filename, String infile) {
	AOTCPrint mOut;
	String headerDef = "_" + infile.replace('.', '_').toUpperCase() + "_";
	infile = "../../src/share/javavm/jcc/aotc/" + infile;
	try {
	    mOut = new AOTCPrint(new FileOutputStream(filename));
	    BufferedReader in = new BufferedReader(new FileReader(infile));
	    writeHeader(mOut);
	    String buf;
	    for(buf=in.readLine(); buf!=null; buf=in.readLine()) {
		mOut.Print(buf);
	    }
            // print interpreter stub functions
            mOut.Print("");
            
	    mOut.close();
	} catch ( java.io.IOException e ){
	    e.printStackTrace();
	}
    } 
    public static AOTCPrint sOut = null;
    public static void openAssemblyWriter(String filename) {
        try {
            if(sOut == null) {
                sOut = new AOTCPrint(new FileOutputStream(filename));
                sOut.Print("#include \"javavm/include/asmmacros_cpu.h\"");
                sOut.Print("#include \"javavm/include/jit/jitasmmacros_cpu.h\"");
                sOut.Print("#include \"javavm/include/jit/jitasmconstants.h\"");
                sOut.Print("#include \"javavm/include/porting/jit/jit.h\"");
                sOut.Print("#include \"javavm/include/sync.h\"");

            }
        }catch( java.io.IOException e) {
            e.printStackTrace();
        }
    }
    public static void openAssemblyWriter() {
        if(sOut != null) {
            sOut.close();
            sOut = null;
        }
    }
    private static final String header[] = {
	"",
	"// This is the generated file of AOTC",
	"// Do not modify at your own hand!!",
	"",
	"#include \"javavm/export/jvm.h\"",
	"#include \"javavm/export/jni.h\"",
	"#include \"native/common/jni_util.h\"",
	"#include \"javavm/include/interpreter.h\"",
	"#include \"javavm/include/gc_common.h\"",
	"#include \"javavm/include/porting/threads.h\"",
	"#include \"javavm/include/porting/time.h\"",
	"#include \"javavm/include/porting/int.h\"",
	"#include \"javavm/include/porting/float.h\"",
	"#include \"javavm/include/porting/doubleword.h\"",
	"#include \"javavm/include/clib.h\"",
	"#include \"javavm/include/indirectmem.h\"",
	"#include \"javavm/include/globalroots.h\"",
	"#include \"javavm/include/localroots.h\"",
	"#include \"javavm/include/preloader.h\"",
	"#include \"javavm/include/common_exceptions.h\"",
	"#include \"generated/offsets/java_lang_Class.h\"",
	"#include \"generated/offsets/java_lang_Thread.h\"",
	"#include \"generated/offsets/java_lang_Throwable.h\"",
	"#include \"generated/javavm/include/build_defs.h\"",
	"#include \"generated/javavm/runtime/romjavaAotcFunctions.h\"",
	"#include <setjmp.h>"
    };
}
