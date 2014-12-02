package aotc;

import java.util.*;
import java.io.*;
import components.*;
import util.*;
import opcodeconsts.*;
import aotc.cfg.*;
import aotc.share.*;
import aotc.translation.*;
import aotc.optimizer.*;
import aotc.ir.*;
import vm.*;

/**
 * MethodTranslator translates the given method.
 */
public class MethodTranslator {

    /**
     * The control flow graph.
     */
    private CFG cfg;
    /**
     * The function name.
     */
    private String name;
    /**
     * The class information.
     */
    private ClassInfo classInfo;
    /**
     * The method information.
     */
    private MethodInfo method;
    /**
     * The output stream.
     */
    private AOTCPrint out;
    /**
     * The CNI class identifier.
     * See class "ClassnameFilterList" for details.
     */
    public static ClassnameFilterList nativeTypes;

    /**
     * The exit label generation flag.
     */
    //private boolean exitLabel = false;

    /**
     * Class constructor.
     *
     * @param classInfo	    the class information
     * @param methodInfo    the method information
     * @param name	    the method name
     * @param out	    the output stream
     * @param nativeTypes   the CNI class identifier
     */
    public MethodTranslator(ClassInfo classInfo, MethodInfo method, String name,
			    AOTCPrint out, ClassnameFilterList nativeTypes) {
	this.classInfo = classInfo;
	this.method = method;
	this.name = name;
	this.out = out;
	this.nativeTypes = nativeTypes;
	this.cfg = new CFG(method, classInfo);
    }

    /**
     * Translates the given method.
     */
    public void translate() throws IOException {

        String tempfile = "temp";
        //CCodeGenerator.gc_point = 0;

        CCodeGenerator.exception_exit = 0;
        CCodeGenerator.exception_s0_ref = 0;
        Iterator it;

        cfg_translate(tempfile);

        // Writes out whether method is static
        if((method.access & ClassFileConst.ACC_STATIC)!=0) {
            out.Printf("// static function");
        }

        ExceptionHandler exceptionHandler = new ExceptionHandler(cfg, null, out, method.exceptionTable);

        // Writes out method name and args
        out.Printf(MethodDeclarationInfo.methodDefinition(name, method));
        out.Printf("{");

        // Put method prologue
        PrologueGenerator prologueGenerator = new PrologueGenerator(cfg, out, exceptionHandler, null);
        prologueGenerator.writePrologue();	

        // Writes out method body.
        BufferedReader in = new BufferedReader(new FileReader(tempfile));
        String buf;
        for(buf=in.readLine(); buf!=null; buf=in.readLine()) {
            out.Printf(buf);
        }
        in.close();

        EpilogueGenerator epilogueGenerator = new EpilogueGenerator(cfg, out, exceptionHandler);
        epilogueGenerator.writeEpilogue();

        out.Printf("}");
        out.Printf("");
        out.Printf("");
        
        generateWrapperFunction();
        //generateWrapperAssembly();

    }
    private void generateWrapperFunction() {
        if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.WRAPPER_TYPE) {
            String methodDef = "inline int ParseArgAndCall_" + name + "(CVMExecEnv* ee, CVMStackVal32 *arguments)";
            //String methodDef = "inline CVMStackVal32* ParseArgAndCall_" + name + "(CVMExecEnv* ee, CVMStackVal32 *arguments)";
            String type = method.type.string;
            out.Printf(methodDef);
            out.Printf("{");
            {
                boolean isVirtual=false;
                int i = 0;
                String args= "(ee, ";
                if((method.access & ClassFileConst.ACC_STATIC) == 0) {
                    i++;
                    args+= "&arguments[0].j.r";
                }else {
                    String cb = ((CVMClass)classInfo.vmClass).getNativeName();
                    out.Printf("extern const CVMClassBlock " + cb + "_Classblock;");
                    args += (cb + "_Classblock.javaInstanceX");
                }
                int index = type.indexOf('(')+1;
                char c;
                String JVMTo64 = "";
                while(( c= type.charAt(index)) != ')') {
                    int t = TypeInfo.toAOTCType(c);
                    args+= " , ";
                    if(t == TypeInfo.TYPE_REFERENCE) {
                        args += "&" + "arguments[" + i + "].j.r";
                    }else if(t == TypeInfo.TYPE_FLOAT) {
                        args += "arguments[" + i + "].j.f";
                    }else if(t == TypeInfo.TYPE_DOUBLE) {
                        out.Printf("CVMJavaDouble double"+i+";");
                        JVMTo64 += ("CNIjvm2Double(arguments+"+i+",double"+i+");\n");
                        args+= ("double"+i);
                        i++;
                    }else if(t == TypeInfo.TYPE_LONG) {
                        out.Printf("CVMJavaLong long"+i+";");
                        JVMTo64 += ("CNIjvm2Long(arguments+"+i+",long"+i+");\n");
                        args+= ("long"+i);
                        i++;
                    }else {
                        args += "arguments[" + i + "].j.i";
                    }
                    if(c == 'L') {
                        index = type.indexOf(';',index);
                    }
                    if(c == '[') {
                        index++;
                        while(type.charAt(index) == '[') {
                            index++;
                        }
                        if(type.charAt(index) == 'L')
                            index=type.indexOf(';',index);
                    }
                    index++;
                    i++;
                }
                out.Printf(JVMTo64);
                args += ");";
                int retType = TypeInfo.toAOTCType(type.charAt(type.indexOf(')')+1));
                String call = (name + args);
                out.Printf("{");
                if(retType == TypeInfo.TYPE_REFERENCE) {
                    out.Printf("jobject ret = " + call);
                    out.Printf("arguments[0].j.r.ref_DONT_ACCESS_DIRECTLY = (CVMObject*)ret;");
                    //out.Printf("return arguments + 1;");
                    out.Printf("return 1;");
                }else if(retType == TypeInfo.TYPE_DOUBLE) {
                    out.Printf("CVMJavaDouble ret = " + call);
                    out.Printf("CNIDouble2jvm(arguments, ret);");
                    //out.Printf("return arguments + 2;");
                    out.Printf("return 2;");
                }else if(retType == TypeInfo.TYPE_FLOAT) {
                    out.Printf("CVMJavaFloat ret = " + call);
                    out.Printf("arguments[0].j.f = ret;");
                    //out.Printf("return arguments + 1;");
                    out.Printf("return 1;");
                }else if(retType == TypeInfo.TYPE_LONG) {
                    out.Printf("CVMJavaLong ret = " + call);
                    out.Printf("CNILong2jvm(arguments, ret);");
                    //out.Printf("arguments[0].j.i = ret & 0xffffffff;");
                    //out.Printf("arguments[1].j.i = ret & 0xffffffff00000000;");
                    //out.Printf("return arguments + 2;");
                    out.Printf("return 2;");
                }else if(retType == TypeInfo.TYPE_VOID) {
                    out.Printf(call);
                    //out.Printf("return arguments;");
                    out.Printf("return 0;");
                }else {
                    out.Printf("arguments[0].j.i = " +call);
                    //out.Printf("return arguments + 1;");
                    out.Printf("return 1;");
                }
                out.Printf("}");
            }
            out.Printf("}");

        }
    }
    private String saveArgument(int type, int arg, int reg) {
        if(type == TypeInfo.TYPE_REFERENCE) {
            if(reg < 4) {
                return "addiu a" + reg + ", a1, " + (arg *4);
            }else {
                return "addiu v0, a1, " + (arg *4) + "\nsw v0, " + ((arg + 1) * 4) + "(sp)";
            }
        }else if(type == TypeInfo.TYPE_LONG || type == TypeInfo.TYPE_DOUBLE) {
            //out.Printf(("type is " + type));
            String result;
            if(reg < 4) {
                result = "lw a" + reg + ", " + (arg * 4) + "(a1)" + "\n";
            }else {
                result = "lw t0," + (arg * 4) + "(a1)\nsw t0, " + ((arg + 1) * 4) + "(sp)" + "\n";
            }
            reg ++;
            arg ++;
            if(reg < 4) {
                return result + "lw a" + reg + ", " + (arg * 4) + "(a1)";
            }else {
                return result + "lw t0," + (arg * 4) + "(a1)\nsw t0, " + ((arg + 1) * 4) + "(sp)";
            }
        }else { // int, byte, short, float
            //out.Printf(("type is " + type));
            if(reg < 4) {
                return "lw a" + reg + ", " + (arg * 4) + "(a1)";
            }else {
                return "lw t0," + (arg * 4) + "(a1)\nsw t0, " + ((arg + 1) * 4) + "(sp)";
            }
        }
    }
    private int nextIndex(String type, int index) {
        char c = type.charAt(index);
        if(c == 'L') {
            index = type.indexOf(';',index);
        }
        if(c == '[') {
            index++;
            while(type.charAt(index) == '[') {
                index++;
            }
            if(type.charAt(index) == 'L')
                index=type.indexOf(';',index);
        }
        return ++index;
    }
    private void generateWrapperAssembly() {
        if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.WRAPPER_TYPE) {
            String type = method.type.string;
            int retType = TypeInfo.toAOTCType(type.charAt(type.indexOf(')') + 1));
            //AOTCWriter.sOut.Printf("/*");
            AOTCWriter.sOut.Printf("ENTRY( ParseArgAndCall_" + name + " )");
            int argSize = 1; // ee
            // native stack pointer
            {
                if((method.access & ClassFileConst.ACC_STATIC) != 0) {
                    argSize++;
                }
                int index = type.indexOf('(') + 1;
                char c;
                while(( c = type.charAt(index)) != ')') {
                    int t = TypeInfo.toAOTCType(c);
                    if(t == TypeInfo.TYPE_DOUBLE || t == TypeInfo.TYPE_LONG) argSize++;;
                    if( c == 'L') index = type.indexOf(';', index);
                    if( c == '[') {
                        index++;
                        while(type.charAt(index) == '[') {
                            index++;
                        }
                        if(type.charAt(index) == 'L')
                            index=type.indexOf(';',index);
                    }
                    index++;
                    argSize++;
                }
                AOTCWriter.sOut.Printf("addiu sp, sp, -" + ((argSize + 2) * 4));
                AOTCWriter.sOut.Printf("sw ra, " + (argSize * 4) + "(sp)");
                AOTCWriter.sOut.Printf("sw a1, " + ((argSize + 1) * 4) + "(sp)");

            }
            // arguments 
            String delaySlot = "nop";
            {
                int arg = 0;
                int reg = 1; // ee
                int index = type.indexOf('(') + 1;
                char c;
                // first arg
                if((method.access & ClassFileConst.ACC_STATIC) != 0) {
                    // static
                    String cb = ((CVMClass)classInfo.vmClass).getNativeName();
                    AOTCWriter.sOut.Printf("LA(v0, " + cb + "_Classblock)");
                    delaySlot = "lw a1, 48(v0)";
                    //arg++;
                    reg++;
                }else {
                    // this object
                    arg++;
                    reg++;
                }

                AOTCWriter.sOut.Printf("LA(jp, " + name + ")");
                while(( c = type.charAt(index)) != ')') {
                    int t = TypeInfo.toAOTCType(c);
                    AOTCWriter.sOut.Printf(saveArgument(t, arg, reg));
                    index = nextIndex(type, index);
                    if(t == TypeInfo.TYPE_DOUBLE || t == TypeInfo.TYPE_LONG) { reg++; arg++; }
                    arg++;
                    reg++;
                }
            }
            // call
            {
                if(!delaySlot.equals("nop")) AOTCWriter.sOut.Printf(delaySlot);
                AOTCWriter.sOut.Printf("jalr jp");
            }
            // return
            {
                AOTCWriter.sOut.Printf("lw ra, " + (argSize * 4) + "(sp)");
                if(retType == TypeInfo.TYPE_DOUBLE) {
                    AOTCWriter.sOut.Printf("lw t0, " + ((argSize + 1) * 4) + "(sp)");
                    //AOTCWriter.sOut.Printf("mfc1 v0, $f0");
                    //AOTCWriter.sOut.Printf("mfc1 v1, $f1");
                    //AOTCWriter.sOut.Printf("sw v0, 0(t0)");
                    //AOTCWriter.sOut.Printf("sw v1, 4(t0)");
                    AOTCWriter.sOut.Printf("swc1 $f0, 0(t0)");
                    AOTCWriter.sOut.Printf("swc1 $f1, 4(t0)");
                    AOTCWriter.sOut.Printf("addiu v0, t0, 8");
                }else if(retType == TypeInfo.TYPE_FLOAT) {
                    AOTCWriter.sOut.Printf("lw t0, " + ((argSize + 1) * 4) + "(sp)");
                    AOTCWriter.sOut.Printf("swc1 $f0, 0(t0)");
                    AOTCWriter.sOut.Printf("addiu v0, t0, 4");
                }else if(retType == TypeInfo.TYPE_LONG) {
                    AOTCWriter.sOut.Printf("lw t0, " + ((argSize + 1) * 4) + "(sp)");
                    AOTCWriter.sOut.Printf("sw v0, 0(t0)");
                    AOTCWriter.sOut.Printf("sw v1, 4(t0)");
                    AOTCWriter.sOut.Printf("addiu v0, t0, 8");
                }else if(retType == TypeInfo.TYPE_VOID) {
                    AOTCWriter.sOut.Printf("lw v0, " + ((argSize + 1) * 4) + "(sp)");
                }else {
                    AOTCWriter.sOut.Printf("lw t0, " + ((argSize + 1) * 4) + "(sp)");
                    AOTCWriter.sOut.Printf("sw v0, 0(t0)");
                    AOTCWriter.sOut.Printf("addiu v0, t0, 4");
                }
                AOTCWriter.sOut.Printf("addiu sp, sp, " + ((argSize + 2)) * 4);
                AOTCWriter.sOut.Printf("jr ra");
            }
            AOTCWriter.sOut.Printf("SET_SIZE(ParseArgAndCall_" + name + " )");
            //AOTCWriter.sOut.Printf("*/");
        }
    }

    /**
     * Tranlates CFG.
     *
     * @param filename	the output file name
     */
    private void cfg_translate(String filename) {
	AOTCPrint file = null;
	try {
	    file = new AOTCPrint(new FileOutputStream(filename));
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	}

        GCCheckEliminator gcCheckEliminator = new GCCheckEliminator(cfg);
        gcCheckEliminator.eliminate();

	CCodeGenerator codegenerator = new CCodeGenerator(cfg, name, file,
						method.exceptionTable);		

        Iterator it = cfg.iterator();
	// Translate each byte code.
	while(it.hasNext()) {
	    IR ir = (IR)it.next();
	    codegenerator.translate(ir);
	    //int kind = ir.getReturnType();
	    //if(kind==MethodDeclarationInfo.BYTECODE_RETURN) CCodeGenerator.exitLabel = true;
	    file.Print("");
	}
    }

    public CFG getCFG() {
	return cfg;
    }
}

