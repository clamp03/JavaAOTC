package aotc.optimizer;

import java.util.*;
import java.io.*;
import components.*;
import util.*;
import opcodeconsts.*;
import consts.*;
import vm.*;
import aotc.*;
import aotc.cfg.*;
import aotc.ir.*;
import aotc.share.*;
import aotc.translation.*;
import aotc.profile.*;

/**
 * MethodInliner is the class for method inline.
 */
public class MethodInliner {
    
    /**
     * The class information.
     */
    private ClassInfo classinfo;
    /**
     * The method information.
     */
    private MethodInfo methodinfo;
    /**
     * The control flow graph.
     */
    private CFG cfg = null;
    /**
     * The output stream.
     */
    private AOTCPrint out;
    /**
     * The return of method.
     */
    private JavaVariable ret;
    /**
     * The arguments of method.
     */
    private Vector operands;
    /**
     * The bpc.
     */
    private int CurrentBpc;
    /**
     * The function name.
     */
    private String name;

    /**
     * Inline depth.
     * MwthodInliner class does recursive inline.
     */
    public static int inline_depth = 0;
    private ExceptionHandler exceptionHandler;
    private boolean needICell;
    private static Stack trace = new Stack();
    private IR inlinedIR;

    /**
     * Does method inline.
     * Checks whether inline is possible and does method inline.
     *
     * @param classinfo	    the class information
     * @param methodinfo    the method information
     * @param out	    the output stream
     * @param ret	    the method return variable
     * @param operands	    the method arguments
     * @param CurrentBpc    the current bpc
     * @param name	    the method name
     */
    public boolean doInline(ClassInfo classinfo, MethodInfo methodinfo,
			    boolean isVirtual, IR ir, JavaVariable ret,
			    AOTCPrint out, String name, int count,
			    ExceptionHandler exceptionHandler) {
	// Stores informations
	this.classinfo = classinfo;
	this.methodinfo = methodinfo;
	this.out = out;
	this.ret = ret;
	this.operands = ir.getOperandList();
	this.CurrentBpc = ir.getBpc();
	this.name = name;
        needICell = ir.hasAttribute(IRAttribute.NEED_ICELL);
	this.exceptionHandler = exceptionHandler;
    this.inlinedIR = ir;

	// Checks inline possibility.
	if(!inlineTest(methodinfo, isVirtual, count)) {
	    return false;
	}

	if(trace.search(methodinfo) == -1) {
	    inline_depth++;
	    trace.push(methodinfo);
	    // Does inline.
	    doTranslation();
	    MethodInfo info = (MethodInfo)trace.pop();
	    new aotc.share.Assert(info==methodinfo, "bsh2, -----------------------");
	    inline_depth--;
	    return true;
	} else {
	    System.err.println("===recursive call===============================================================");
	    return false;
	}
    }

    /**
     * Tests if inline is possible.
     *
     * @param ir	    the current calling IR
     */
    public static boolean inlineTest(MethodInfo me, boolean isVirtual, int count) {
        if(me.parent.className.equals("spec/benchmarks/_205_raytrace/Scene")) {
            String methodname = me.name.string;
            if(methodname != null && methodname.equals("LoadScene")) return false;
        }
	// filters out virtual calls
	//if(isVirtual) return false;
	if(me.code==null)
	    return false;
    
    //if(!ClassFilter.inList(me.parent.className, me.name.string, me.type.string)) return false;
    if(me.parent.className.equals("java/security/AccessController")) {
        String methodname = me.name.string;
        if(methodname != null && methodname.startsWith("doPrivileged")) {
            return false;
        }
    }
    if(me.parent.className.equals("Test")) return false;

	int codelen = me.code.length;
	if(codelen<5) return true;
	//if(codelen<30) return true;
	if(count>0) {
	    // profile heuristics
	    int ratio;
	    if(isVirtual) ratio=3;
	    else ratio=1;
        
	    if(count>100000) {
		if(codelen<60/ratio) return true;
	    } else if(count>10000) {
		if(codelen<55/ratio) return true;
	    } else if(count>1000) {
		if(codelen<50/ratio) return true;
	    } else if(count>100) {
		if(codelen<40/ratio) return true;
	    }
        /*
        
        if(count>100000) {
            if(codelen<100/ratio) return true;
	    } else if(count>10000) {
            if(codelen<80/ratio) return true;
	    } else if(count>1000) {
            if(codelen<60/ratio) return true;
	    } else if(count>100) {
            if(codelen<40/ratio) return true;
	    }
        */
        
	    return false;
	}
	
	// static max code length : 40
        if (AOTCWriter.USE_CALL_COUNT_INFO) {
            return (codelen < 4);
        } else {
    	    return (codelen < 30);
        }
        //return false;
    }
    private static boolean isNonvirtual(IR ir) {
	short opcode = ir.getShortOpcode();
	return (opcode==OpcodeConst.opc_invokestatic_checkinit_quick) ||
	       (opcode==OpcodeConst.opc_invokestatic_quick) ||
	       (opcode==OpcodeConst.opc_invokenonvirtual_quick);
    }

    private void changeArgUseDef(IR ir, int offset, boolean isUse) {
	int index, size = 0;
	int number;

	if(isUse) {
	    number = ir.getOperand(offset).getIndex();
	} else {
	    number = ir.getTarget(offset).getIndex();
	}

	if(number==-1) {
	    index = 0;
	} else {
	    for(index = 0; index < operands.size(); index++) {
		if(size==number) break;
		size += ((JavaVariable)operands.elementAt(index)).size();
	    }
	}

	JavaVariable op = (JavaVariable)operands.elementAt(index);
	if(isUse) {
	    ir.changeOperand(offset, op);
	} else {
	    ir.changeTarget(offset, op);
	}
    }

    /**
     * Removes arguments copy.
     */
    private void variableAdjust() {
	
	// Changes the index of local & stack variables
	// arguments variable adjust
        Iterator it = cfg.iterator();
	while(it.hasNext()) {
            IR ir = (IR) it.next();
	    for(int i=0; i<ir.getNumOfTargets(); i++) {
		JavaVariable v = ir.getTarget(i);
		if((v instanceof LocalVariable && v.getIndex() < methodinfo.argsSize)||
			v.toString().equals("cls") ) {
		    changeArgUseDef(ir, i, false);
		} else {
		    v.setInline();
		}
	    }
	    for(int i=0; i<ir.getNumOfOperands(); i++) {
		JavaVariable v = ir.getOperand(i);
		if((v instanceof LocalVariable && v.getIndex() < methodinfo.argsSize)||
			v.toString().equals("cls") ) {
		    changeArgUseDef(ir, i, true);
		} else {
		    v.setInline();
		}
	    }
	}
    }
    
    /**
     * Writes actual inlined body.
     */
    private void doTranslation() {

	cfg = new CFG(methodinfo, classinfo);
        GCCheckEliminator gcCheckEliminator = new GCCheckEliminator(cfg);
        gcCheckEliminator.eliminate();

	// variables adjust for inline
	variableAdjust();
	cfg.recomputeInfo();

        Iterator it = cfg.iterator();
        IR ir;

	out.Print("{");
	    /*
        // Put method prologue
        PrologueGenerator prologueGenerator = new PrologueGenerator(cfg, out, codegenerator.getExceptionHandler());
        prologueGenerator.writePrologue();
        */

	//String cb = null;
	//String sync = null;
    /*
	if((methodinfo.access & ClassFileConst.ACC_SYNCHRONIZED)!=0) {
	    cb = ((CVMClass)(classinfo.vmClass)).getNativeName() + "_Classblock";
	    out.Print("{");
	    out.Print("extern const CVMClassBlock " + cb + ";");
	    sync = (cb + ".javaInstanceX");

	    out.Print("if(!CVMfastTryLock(ee, " + sync + "->ref_DONT_ACCESS_DIRECTLY)) {");
	    out.Print("CVMobjectLock(ee, " + sync + ");");
	    out.Print("}");
	    out.Print("}");
	}
    */

	
    // Put method prologue
	String label_name = CurrentBpc + "_" + name;
	CCodeGenerator codegenerator = new CCodeGenerator(cfg, label_name,
							out, methodinfo.exceptionTable);

    PrologueGenerator prologueGenerator = new PrologueGenerator(cfg, out, codegenerator.getExceptionHandler(), inlinedIR);
    prologueGenerator.writePrologue();
	
    boolean useSetjmp = codegenerator.getExceptionHandler().useSetjmp();

    String cb = prologueGenerator.inlinedCB;
    String sync = prologueGenerator.inlinedSYNC;
    // Writes out body
	codegenerator.getExceptionHandler().setCallerInfo(exceptionHandler, CurrentBpc);
	codegenerator.getExceptionHandler().setSyncObj(sync, cb);

    boolean exit_label = false;

        it = cfg.iterator();
	while(it.hasNext()) {
	    ir = (IR)it.next();
	    codegenerator.translate(ir);
	    out.Print("");

	    if(isReturn(ir.getBytecode().getOpcode())) {
		exit_label = true;
	    }
	}

	//if(ret==null)
	    //out.Print("}");
	
	if(exit_label) {
	    out.Print("EXIT_" + label_name + ":;");
	}
    
    //if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING && codegenerator.getExceptionHandler().useSetjmp()) {
    
    if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING && useSetjmp) {
        // if(AOTCWriter.DEBUG_INFO) out.Printf("fprintf(stderr, \"Pop Setjmp Inline Exit" + cfg.getMethodInfo().getNativeName(true) + "   : %d\\n\", (--setjmpCount));");
        out.Printf("ee->handlerList = ee->handlerList->next;");
        out.Printf("free(temp);");
        if(sync == null) {
            out.Print("{");
            //out.Print("AOTCLockList* lock_temp = ee->lockList;");
            out.Printf("lock_temp = ee->lockList;");
            out.Printf("ee->lockList = ee->lockList->next;");
            out.Printf("free(lock_temp);");
            out.Print("}");
        }
    }

    if(sync!= null && (AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING || AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING)) {
        out.Print("{");
        if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
            out.Print("AOTCLockList* lock_temp = ee->lockList;");
        }else if (useSetjmp) {
            out.Print("lock_temp = ee->lockList;");
        }
        out.Print("ee->lockList = ee->lockList->next;");
        out.Print("free(lock_temp);");
        out.Print("}");
    }

	if(ret!=null) {
	    String retName = "s" + inline_depth*JavaVariable.INLINE_START + "_" + TypeInfo.suffix[ret.getType()];
	    out.Print(ret + " = " + retName + ";");
	    if(ret.getType()==TypeInfo.TYPE_REFERENCE && needICell) {
		out.Print(ret + "_ICell->ref_DONT_ACCESS_DIRECTLY = " + retName + ";");
	    }
	    out.Print("}");
	}
	
	if(sync!=null) {
	    out.Print("{");
	    if(cb != null) out.Print("extern const CVMClassBlock " + cb + ";");
	    out.Print("if(!CVMfastTryUnlock(ee, " + sync + "->ref_DONT_ACCESS_DIRECTLY)) {");
	    out.Print("CVMobjectUnlock(ee, " + sync + ");");
	    out.Print("}");
	    out.Print("}");
	}
	if(ret==null)
	    out.Print("}");
    }

    private boolean isReturn(byte opcode) {
	if(opcode==(byte)OpcodeConst.opc_ireturn
	    || opcode==(byte)OpcodeConst.opc_lreturn
	    || opcode==(byte)OpcodeConst.opc_freturn
	    || opcode==(byte)OpcodeConst.opc_dreturn
	    || opcode==(byte)OpcodeConst.opc_areturn
	    || opcode==(byte)OpcodeConst.opc_return)
	    return true;
	return false;
    }
    private boolean isLocalLoad(byte opcode) {
	if(opcode==(byte)OpcodeConst.opc_iload
	    || opcode==(byte)OpcodeConst.opc_iload_0
	    || opcode==(byte)OpcodeConst.opc_iload_1
	    || opcode==(byte)OpcodeConst.opc_iload_2
	    || opcode==(byte)OpcodeConst.opc_iload_3
	    || opcode==(byte)OpcodeConst.opc_lload
	    || opcode==(byte)OpcodeConst.opc_lload_0
	    || opcode==(byte)OpcodeConst.opc_lload_1
	    || opcode==(byte)OpcodeConst.opc_lload_2
	    || opcode==(byte)OpcodeConst.opc_lload_3
	    || opcode==(byte)OpcodeConst.opc_fload
	    || opcode==(byte)OpcodeConst.opc_fload_0
	    || opcode==(byte)OpcodeConst.opc_fload_1
	    || opcode==(byte)OpcodeConst.opc_fload_2
	    || opcode==(byte)OpcodeConst.opc_fload_3
	    || opcode==(byte)OpcodeConst.opc_dload
	    || opcode==(byte)OpcodeConst.opc_dload_0
	    || opcode==(byte)OpcodeConst.opc_dload_1
	    || opcode==(byte)OpcodeConst.opc_dload_2
	    || opcode==(byte)OpcodeConst.opc_dload_3
	    || opcode==(byte)OpcodeConst.opc_aload
	    || opcode==(byte)OpcodeConst.opc_aload_0
	    || opcode==(byte)OpcodeConst.opc_aload_1
	    || opcode==(byte)OpcodeConst.opc_aload_2
	    || opcode==(byte)OpcodeConst.opc_aload_3)
	    return true;
	return false;
    }
}
