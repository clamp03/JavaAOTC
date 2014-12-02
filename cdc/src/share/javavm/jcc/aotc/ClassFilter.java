package aotc;

import aotc.profile.*;
import java.util.*;
import java.io.*;
import components.*;
public class ClassFilter {

    /**
     * Determines whether the method will be aotced or not.
     *
     * @param classname	    the class name
     * @param methodname    the method name
     */
    private static Vector methodFilterList = null;
    // for method filter (>=)
    public static final int CALL_COUNT = 1;
    public static final int LOOP_COUNT = 100;
    public static final double RUNTIME_PERCENTAGE = 0.1;
    public static int AOTC_LIST = 0;

    public static boolean inList(String classname, String methodname, String signature) {
        boolean AOTC_ALL_CLASSES = false;
        boolean AOTC_TEST = false;
        boolean AOTC_APP = false;
	boolean AOTC_CALL_COUNT = false;
	boolean AOTC_RUN_TIME = false;
	boolean AOTC_LOOP_COUNT =false;
	
    switch(AOTC_LIST) {
    case 0:
        return false;
	case 1:
	    AOTC_ALL_CLASSES = true;
	    break;
	case 2:
	    AOTC_TEST = true;
	    break;
	case 3:
	    AOTC_APP = true;
	    break;
	case 4:
	    AOTC_CALL_COUNT = true;
	    break;
	case 5:
	    AOTC_RUN_TIME = true;
	    break;
	case 6:
	    AOTC_LOOP_COUNT = true;
	    break;
	case 7:
	    AOTC_RUN_TIME = true;
	    AOTC_CALL_COUNT = true;
	    break;
	case 8:
	    AOTC_LOOP_COUNT = true;
	    AOTC_CALL_COUNT = true;
	    break;
	}
	// Exclude
	if(classname.equals("spec/io/TableOfExistingFiles") ||
	   classname.equals("sun/io/CharacterEncoding") || 
	   (classname.equals("com/motorola/bench/parallel/ParallelBench") && methodname != null && methodname.equals("setup")) ||
	   (classname.equals("com/motorola/bench/parallel/data02") && methodname != null && methodname.equals("<clinit>")) ||
       classname.equals("spec/benchmarks/_200_check/LoopBounds") ||
	   classname.equals("spec/benchmarks/_200_check/LoopBounds2") ||
       (classname.equals("java/lang/Class") && methodname != null && methodname.equals("forName")) ||
       (classname.equals("sun/security/util/Resources") && methodname != null && methodname.equals("<clinit>")) ||
       classname.equals("java/io/UnixFileSystem") ||
	   classname.equals("java/util/TimeZoneData")) {    // jdh
	    return false;
	}
    // Exclude Branch out of range (at gcc compiling)
    if((classname.equals("org/bouncycastle/crypto/engines/DESEngine") && methodname != null && methodname.equals("<clinit>")) ||
	   (classname.equals("com/motorola/bench/parallel/data01") && methodname != null && methodname.equals("<clinit>"))  ||
	   (classname.equals("org/bouncycastle/crypto/engines/BlowfishEngine") && methodname != null && methodname.equals("<clinit>")) ||
	   (classname.equals("java/math/BigInteger") && methodname != null && methodname.equals("<clinit>")) ||
	   (classname.equals("sun/tools/java/RuntimeConstants") && methodname != null && methodname.equals("<clinit>")) ||
       (classname.equals("spec/benchmarks/_228_jack/NfaState") && methodname != null && methodname.equals("GenerateCode")) || //GCC 3.x debug mode
       (classname.equals("spec/benchmarks/_213_javac/SourceClass") && methodname != null && methodname.equals("compileClass")) ||  //GCC 3.x debug mode
       (classname.equals("spec/benchmarks/_228_jack/Jack_the_Parser_GeneratorTokenManager") && methodname != null && methodname.equals("MoveStringLiteralDfa"))  //GCC 3.x debug mode
            ) {
        return false;
    }
    if((classname.equals("java/lang/Thread") && (methodname != null && (methodname.equals("<init>") && signature.equals("(Ljava/lang/ThreadGroup;Ljava/lang/String;IJ)V")))) ||
            (classname.equals("sun/io/CharToByteConverter") && methodname != null && methodname.equals("convertAny"))||
            (classname.equals("spec/benchmarks/_213_javac/FieldDefinition") && methodname != null && methodname.equals("<init>"))) {
        return false;
    }

    if(classname.equals("spec/benchmarks/_222_mpegaudio/p") ||
        false) {
        return false;
    }
    /*
    if(!classname.startsWith("spec/benchmarks/_213_")) return false;
    
    if((classname.startsWith("spec/benchmarks/_213_javac/A") || 
        classname.startsWith("spec/benchmarks/_213_javac/B") || 
        classname.startsWith("spec/benchmarks/_213_javac/C") ||
        classname.startsWith("spec/benchmarks/_213_javac/D") ||
        classname.startsWith("spec/benchmarks/_213_javac/E")
        )
    )return false;
    if((classname.startsWith("spec/benchmarks/_213_javac/F") || 
        classname.startsWith("spec/benchmarks/_213_javac/W") ||
        classname.startsWith("spec/benchmarks/_213_javac/J") ||
        classname.startsWith("spec/benchmarks/_213_javac/R")
        )
    )return false;
    */
    /*
     if(!(classname.endsWith("Expression") || 
        classname.endsWith("Statement") 
        )
    )return false;
    
    
    if(!(classname.startsWith("spec/benchmarks/_213_javac/T") || 
        classname.startsWith("spec/benchmarks/_213_javac/P")  ||

        classname.startsWith("spec/benchmarks/_213_javac/M")  ||
        classname.startsWith("spec/benchmarks/_213_javac/N")
        )
    )return false;
    */
	/*
	CallCountTable callerCountTable = CallCountAnalyzer.getCallCountTable();
	if(callerCountTable.containsForName(classname, methodname, signature)) {
	    return false;
	}
	*/
	
        // AOTC ALL SYSTEM & APPLICATION CLASSES
	if(AOTC_ALL_CLASSES) {
	    if(classname.equals("java/security/AccessController")) {
		if(methodname==null) return true;
		else return !(methodname.startsWith("doPrivileged"));
	    } else {
		//return classFilter(classname);
		return true;
	    }
	}
        
        // AOTC TEST CLASS
        if (AOTC_TEST) {
	    if(
		//classname.startsWith("spec/benchmarks/_200") ||
		//classname.startsWith("java/lang/Float") ||
		//(classname.equals("org/kxml/parser/XmlParser") && (methodname == null || methodname.equals("parseComment"))) ||
		//(classname.equals("org/bouncycastle/crypto/engines/TwofishEngine") && (methodname == null || methodname.equals("<clinit>"))) ||
		//classname.startsWith("spec/benchmarks/_228") ||
		//(classname.equals("java/util/Vector") && (methodname == null || methodname.equals("addElement"))) ||
		classname.equals("Test") ||
		//(classname.equals("Test") && (methodname == null || methodname.startsWith("callee")))||
		//(classname.equals("Test") && (methodname == null || methodname.startsWith("facto"))) || 
		//(classname.equals("java/io/PrintStream")) || 
		false
	    ) {
		return true;
	    }
        }
        
        // AOTC Application only
        if (AOTC_APP) {
            if(!classname.startsWith("java/") && !classname.startsWith("sun/")) {
		return true;
	    }
	}
	
	
	if(AOTC_CALL_COUNT) {
	    if(classname.equals("java/security/AccessController")) {
		if(methodname==null) return true;
		else return !(methodname.startsWith("doPrivileged"));
	    } else {
		if(MethodFilter.determineAOTC_CallCount(classname, methodname, signature)) {
		    //writeToFile("aotc_call_count" , classname , methodname, signature);
		    return true;
		}
	    }
	}
	if(AOTC_RUN_TIME) {
	    if(classname.equals("java/security/AccessController")) {
		if(methodname==null) return true;
		else return !(methodname.startsWith("doPrivileged"));
	    } else {
		if(MethodFilter.determineAOTC_Runtime(classname, methodname, signature)) {
		    //writeToFile("aotc_run_time" , classname , methodname, signature);
		    return true;
		}
	    }
	}
	if(AOTC_LOOP_COUNT) {
	    if(classname.equals("java/security/AccessController")) {
		if(methodname==null) return true;
		else return !(methodname.startsWith("doPrivileged"));
	    } else {
		if( MethodFilter.determineAOTC_LoopCount(classname, methodname, signature)) {
		    //writeToFile("aotc_loop_count" , classname , methodname, signature);
		    return true;
		}
	    }
	}
        return false;
    }
    public static boolean start = true;
    public static boolean write = true;
    private static void writeToFile(String fileName, String classname, String methodname, String signature) {
	if(!write) return;
	try {
	    String write = classname + "\t" + methodname + "\t" + signature + "\n";
	    FileOutputStream fo = new FileOutputStream(fileName, !start);
	    fo.write(write.getBytes());
	    fo.close();
	}catch(IOException e) {
	    System.err.println("FileOutput Error : " + fileName);
	}
	start = false;

    }
    /*
    private static boolean methodFilter(String classname, String methodname) {
	
	if(methodFilterList == null) {
	    String buf;
	    BufferedReader in = null;
	    methodFilterList = new Vector();
	    try {
		in = new BufferedReader(new FileReader("methodfilter"));
		for(buf = in.readLine() ; buf != null ; buf = in.readLine()) {
		    if(!methodFilterList.contains(buf)) {
			methodFilterList.add(buf);
		    }
		}
	    }catch(IOException e) {
		System.err.println("can't find classfilter file");
		methodFilterList = null;
		return false;
	    }
	}
	String key;
	if(methodname == null) {
	    key = "Java/" + classname;
	}else {
	    key = "Java/" + classname + "/" + methodname;
	}
	key = key.replaceAll("_", "_1");
	key = key.replace('/', '_');
	for(int i = 0 ; i < methodFilterList.size(); i++) {
	    String method = methodFilterList.elementAt(i).toString();
	    if(method.startsWith(key)) {
		return true;
	    }
	}
	return false;
    }
    */
    /*
    private static boolean methodFilter(String methodname, String name) {
	if(methodname==null)
	    return true;
	return methodname.equals(name);
    }
    */
    private static boolean classFilter(String classname) {
	// general rules
	if(!classname.startsWith("java/") && !classname.startsWith("sun/")) {
	    return true;
	}
	if(classname.regionMatches(classname.length()-9, "Exception", 0, 9)) {
	    return false;
	}
	if(classname.regionMatches(classname.length()-5, "Error", 0, 5)) {
	    return false;
	}

	// first include test, next exclude test
	String cname = null;
	String include[] = null;
	String exclude[] = null;
	String exclude_star[] = null;
	if(classname.startsWith("java/io/")) {
	    cname = classname.substring(8);
	    exclude = java_io;
	    exclude_star = java_io_star;
	} else if(classname.startsWith("java/lang/")) {
	    cname = classname.substring(10);
	    exclude = java_lang;
	    exclude_star = java_lang_star;
	} else if(classname.startsWith("java/lang/ref/")) {
	    cname = classname.substring(14);
	    exclude = java_lang_ref;
	    exclude_star = java_lang_ref_star;
	} else if(classname.startsWith("java/lang/reflect")) {
	    cname = classname.substring(17);
	    include = java_lang_reflect_include;
	} else if(classname.startsWith("java/math/")) {
	    return false;
	} else if(classname.startsWith("java/net/")) {
	    cname = classname.substring(9);
	    include = java_net_include;
	} else if(classname.startsWith("java/security/")) {
	    cname = classname.substring(14);
	    include = java_security_iclude;
	} else if(classname.startsWith("java/text/")) {
	    return false;
	} else if(classname.startsWith("java/util/")) {
	    cname = classname.substring(10);
	    include = java_util_include;
	    exclude = java_util;
	    exclude_star = java_util_star;
	} else if(classname.startsWith("sun/io/")) {
	    return true;
	} else if(classname.startsWith("sun/misc/")) {
	    cname = classname.substring(9);
	    include = sun_misc_include;
	} else if(classname.startsWith("sun/net/")) {
	    return false;
	} else if(classname.startsWith("sun/security/")) {
	    cname = classname.substring(13);
	    include = sun_security_include;
	} else if(classname.startsWith("sun/tools/")) {
	    return false;
	}

	boolean ret = true;
	if(exclude==null && exclude_star==null) {
	    ret = false;
	}
	if(include!=null) {
	    for(int i=0; i<include.length; i++) {
		if(cname.equals(include[i])) {
		    return true;
		}
	    }
	}
	if(exclude!=null) {
	    for(int i=0; i<exclude.length; i++) {
		if(cname.equals(exclude[i])) {
		    return false;
		}
	    }
	}
	if(exclude_star!=null) {
	    for(int i=0; i<exclude_star.length; i++) {
		if(cname.startsWith(exclude_star[i])) {
		    return false;
		}
	    }
	}
	return ret;
    }

    private static final String java_io[] = {"Bits", "DataInput", "DataOutput", "Externalizable",
					    "FileFilter", "FileReader", "FileWriter", "FilenameFilter",
					    "ObjectStreamConstants", "ObjectStreamField", "PrintWriter",
					    "PushbackInputStream", "RandomAccessFile"};
    private static final String java_io_star[] = {"FilePermission", "ObjectInput", "ObjectOutput",
						 "ObjectStreamClass$", "Piped", "Serializable"};
    private static final String java_lang[] = {"Cloneable", "Comparable", "FDBigInt", "Number",
					      "Process", "Runnable", "RuntimePermission", "System$1",
					      "Terminator", "ThreadDeath"};
    private static final String java_lang_star[] = {"Character$", "ClassLoader$", "Package",
						   "SecurityManager", "Shutdown$", "String$"};
    private static final String java_lang_ref[] = {"FinalReference", "Finalizer$3", "PhantomReference",
						  "WeakReference"};
    private static final String java_lang_ref_star[] = {"Reference$"};
    private static final String java_lang_reflect_include[] = {"AccessibleObject", "Constructor"};
    //String java_math[] = {"*"};
    private static final String java_net_include[] = {"URL", "URLClassLoader", "URLStreamHandler"};
    private static final String java_security_iclude[] = {"AccessControlContext", "AccessController",
							 "BasicPermission", "Permission",
							 "SecureClassLoader"};
    //String java_text[] = {"*"};
    private static final String java_util_include[] = {"Collections$EmptyMap", "HashMap$Entry",
						      "Hashtable$Entry", "Hashtable$Enumerator",
                                                      "Hashtable$EmptyEnumerator"};
    private static final String java_util[] = {"AbstractCollection", "AbstractSequentialList",
					      "AbstractSet", "BitSet", "Calendar", "Collection",
					      "Comparator", "Date", "Dictionary", "Enumeration",
					      "GregorianCalendar", "Iterator", "Observer", "Set",
					      "SimpleTimeZone", "StringTokenizer"};
    private static final String java_util_star[] = {"AbstractList$", "AbstractMap$", "Arrays",
						   "Collections$", "HashMap$", "Hashtable$",
						   "LinkedList", "List", "Map", "Property",
						   "ResourceBundle", "Sorted", "SubList", "TimeZone",
						   "WeakHashMap", "jar/", "zip/"};
    //String sun_io_include[] = {"*"};
    private static final String sun_misc_include[] = {"CVM", "Launcher", "Launcher$1", "Launcher$3",
						     "Launcher$AppClassLoader", "Launcher$Factory",
						     "SoftCache", "ThreadRegistry", "URLClassPath",
						     "Version"};
    //String sun_net[] = {"*"};
    private static final String sun_security_include[] = {"action/GetPropertyAction", "util/Debug"};
    //String sun_tools[] = {"*"};
}
