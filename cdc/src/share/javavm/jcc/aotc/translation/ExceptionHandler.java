package aotc.translation;

import components.*;
import aotc.share.*;
import aotc.ir.*;
import aotc.cfg.*;
import aotc.optimizer.*;
import aotc.profile.*;
import aotc.*;
import consts.*;
import java.util.*;
import opcodeconsts.*;
import vm.*;
import util.*;

// for each exception check
// find the variable's definition (recursively)
// check if it's in the same loop!!

public class ExceptionHandler {

    private AOTCPrint out;
    private String name;
    private ClassInfo classinfo;
    private MethodInfo methodinfo;
    private ExceptionEntry exceptionTable[];
    private TreeSet label;
    private CFG cfg;
    private ExceptionHandler callerHandler = null;
    // Private use for method inlining
    private int currentPC;
    public static StackVariable s0_ref = new StackVariable(TypeInfo.TYPE_REFERENCE, 999);
    
    private boolean[] hasInvoke;
    public Stack trace = new Stack();
    private static Hashtable useSetjmpInfo = new Hashtable();
    public boolean callerSetjmp = false;
    private int depth = 0;
    private String syncObj = null;
    private String syncCB = null;
    
    public static final int EXCEPTION_NOT_GEN = 0;
    public static final int EXCEPTION_GEN_S0_REF = 1;
    public static final int EXCEPTION_NULL_POINTER = 2;
    public static final int EXCEPTION_INSTANTIATION = 3;
    public static final int EXCEPTION_OUT_OF_MEMORY = 4;
    public static final int EXCEPTION_CLASS_CAST = 5;
    public static final int EXCEPTION_NEGATIVE_ARRAY_SIZE = 6;
    public static final int EXCEPTION_ARRAY_INDEX_OUT_OF_BOUNDS = 7;
    public static final int EXCEPTION_ARRAY_STORE = 8;
    public static final int EXCEPTION_ARITHMETIC = 9;
    public static final int EXCEPTION_INCOMPATIBLE_CLASS_CHANGE = 10;
    public static final int EXCEPTION_HANDLER = 11;
    
    public static final String exceptionTypeName[] = { 
	"EXCEPTION_NOT_GEN", "EXCEPTION_GEN_S0_REF", "EXCEPTION_NULL_POINTER", 
	"EXCEPTION_INSTANTIATION", "EXCEPTION_OUT_OF_MEMORY", "EXCEPTION_CLASS_CAST",
	"EXCEPTION_NEGATIVE_ARRAY_SIZE", "EXCEPTION_ARRAY_INDEX_OUT_OF_BOUNDS", "EXCEPTION_ARRAY_STORE",
	"EXCEPTION_ARITHMETIC", "EXCEPTION_INCOMPATIBLE_CLASS_CHANGE" };
    
    
    public static final String exceptionClassName[] = {
	"", "", "java/lang/NullPointerException", 
	"java/lang/InstantiationException", "java/lang/OutOfMemoryError",
	"java/lang/ClassCastException", "java/lang/NegativeArraySizeException", 
	"java/lang/ArrayIndexOutOfBoundsException", "java/lang/ArrayStoreException",
	"java/lang/ArithmeticException", "java/lang/IncompatibleClassChangeError",
	"" };
    
    public ExceptionHandler(CFG cfg, String name, AOTCPrint out, ExceptionEntry exceptionTable[]) {
	this.classinfo = cfg.getClassInfo();
	this.methodinfo = cfg.getMethodInfo();
	this.cfg = cfg;
	this.name = name;
	this.out = out;
	this.exceptionTable = exceptionTable;
	label = new TreeSet();
    
    hasInvoke = determineTrySetjmp(cfg);
    depth = MethodInliner.inline_depth;
    }
    public boolean useSetjmp() {
        if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.CALL_CHECK_HANDLING || callerSetjmp) return false;

        checkSetjmp(cfg, depth);
        String classname = classinfo.className;
        String methodname = methodinfo.name.string;
        String signature = methodinfo.type.string;

        String key = classname + " " + methodname + " " + signature + " " + depth;
        ExceptionSetjmpInfo exceptionInfo = (ExceptionSetjmpInfo)useSetjmpInfo.get(key);
        if(exceptionInfo != null) {
            return exceptionInfo.useSetjmp;
        }
        return false;
    }
    public int checkSetjmp(CFG cfg, int depth) {
        MethodInfo methodinfo = cfg.getMethodInfo();
        ClassInfo classinfo = cfg.getClassInfo();
        String classname = classinfo.className;
        String methodname = methodinfo.name.string;
        String signature = methodinfo.type.string;

        String key = classname + " " + methodname + " " + signature + " " + depth;

        ExceptionSetjmpInfo exceptionInfo = (ExceptionSetjmpInfo)useSetjmpInfo.get(key);
        if(exceptionInfo != null) {
            return exceptionInfo.setjmpCount;
        }

        if(trace.search(methodinfo) == -1) {
            trace.push(methodinfo);
        }else {
            return 0;
        }
        int setjmpCount = 0;
        int calleeSetjmpCount = 0;
        CallCountTable ct = null;
        if(AOTCWriter.USE_CALL_COUNT_INFO) {
            ct = CallCountAnalyzer.getCallCountTable();
            if(ct != null && ct.callCount.containsKey(classname)) {
                Hashtable methods = (Hashtable)ct.callCount.get(classname);
                if(methodname != null && methods.containsKey(methodname + signature)) {
                    setjmpCount = ((Integer)methods.get(methodname + signature)).intValue();
                }
            }
        }
        boolean[] hasInvokeTryBlock = determineTrySetjmp(cfg);
        for(int i = 0 ; i < hasInvokeTryBlock.length ; i++) {
            if(hasInvokeTryBlock[i]) {
                useSetjmpInfo.put(key, new ExceptionSetjmpInfo(true, setjmpCount));
                trace.pop();
                return setjmpCount;
            }
        }

        Iterator it = cfg.iterator();
        while(it.hasNext()) {
            IR ir = (IR)it.next();
            short opcode = ir.getShortOpcode();
            if(isInvokeBytecode(opcode)) {
                boolean isVirtual = false;
                switch(opcode) {
                    case OpcodeConst.opc_invokevirtual_quick_w: case OpcodeConst.opc_invokevirtual_quick:
                    case OpcodeConst.opc_ainvokevirtual_quick:  case OpcodeConst.opc_dinvokevirtual_quick:
                    case OpcodeConst.opc_vinvokevirtual_quick:  case OpcodeConst.opc_invokevirtualobject_quick:
                    case OpcodeConst.opc_invokeinterface_quick:
                        isVirtual = true;
                }
                if (ct != null) {
                    if(!AOTCWriter.USE_CALL_COUNT_INFO) {
                        useSetjmpInfo.put(key, new ExceptionSetjmpInfo(false, setjmpCount));
                        trace.pop();
                        return 0;
                    }
                    Vector calleeProfileList = ct.findCalleeList(classname, (methodname + signature),
                            Integer.toString(ir.getOriginalBpc()));
                    if(isVirtual&&AOTCWriter.INLINE_VIRTUALCALL) {
                        for(int i = 0 ; i < calleeProfileList.size() ; i++) {
                            CalleeInfo calleeInfo = (CalleeInfo)calleeProfileList.elementAt(i);
                            MethodInfo calleeMethod = AOTCInvoke.getMethodInfoFromProfile(calleeInfo);
                            ClassInfo calleeClass = AOTCInvoke.getClassInfoFromProfile(calleeInfo);
                            if(calleeClass != null && calleeMethod.code != null
                                    && MethodInliner.inlineTest(calleeMethod, true, calleeInfo.getCallCount())) {
                                calleeSetjmpCount += checkSetjmp(new CFG(calleeMethod, calleeMethod.parent), depth + 1);
                            }
                        }
                    }else if(!isVirtual && AOTCWriter.INLINE_STATICCALL) {
                        MethodConstant mc = (MethodConstant)classinfo.getConstantPool().getConstants()[ir.getBytecode().getOperand(0)];
                        MethodInfo calleeMethod = mc.find();
                        ClassInfo calleeClass = calleeMethod.parent;
                        int callCount = 0;
                        if(calleeProfileList.size() == 1 && AOTCWriter.USE_CALL_COUNT_INFO) {
                            callCount = ((CalleeInfo)calleeProfileList.elementAt(0)).getCallCount();
                        }
                        if(calleeClass != null && calleeMethod.code != null
                                && MethodInliner.inlineTest(calleeMethod, false, callCount)) {
                            calleeSetjmpCount += checkSetjmp(new CFG(calleeMethod, calleeMethod.parent), depth + 1);
                        }
                    }
                }
            }
        }
        trace.pop();
        if(setjmpCount != 0 && calleeSetjmpCount >= setjmpCount) {
            useSetjmpInfo.put(key, new ExceptionSetjmpInfo(true, setjmpCount));
            return setjmpCount;
        }
        useSetjmpInfo.put(key, new ExceptionSetjmpInfo(false, calleeSetjmpCount));
        return calleeSetjmpCount;
    }
    public boolean[] determineTrySetjmp(CFG cfg) {
        ExceptionEntry[] exceptionTable = cfg.getMethodInfo().exceptionTable;
        boolean[] hasInvokeTryBlock = new boolean[exceptionTable.length];
        if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.CALL_CHECK_HANDLING /* && !AOTCWriter.EXCEPTION_INFO */) {
            for(int i = 0 ; i < exceptionTable.length ; i++) {
                hasInvokeTryBlock[i] = false;
            }
            return hasInvokeTryBlock;
        }

        int endBpc = -1;
        int startBpc = -1;
        for(int i = 0 ; i < exceptionTable.length ; i++) {
            hasInvokeTryBlock[i] = false;
            if(startBpc == exceptionTable[i].startPC && endBpc == exceptionTable[i].endPC) continue;
            startBpc = exceptionTable[i].startPC;
            endBpc = exceptionTable[i].endPC;
            Iterator it = cfg.iterator();
            while(it.hasNext()) {
                IR ir = (IR)it.next();
                if(ir.getOriginalBpc() < exceptionTable[i].startPC || ir.getOriginalBpc() >= exceptionTable[i].endPC) continue;
                short opcode = ir.getShortOpcode();
                if(isInvokeBytecode(opcode)) {
                    hasInvokeTryBlock[i] = true;
                }
            }
        }   
        return hasInvokeTryBlock;
    }


    public void setCallerInfo(ExceptionHandler callerHandler, int pc) {
        this.callerSetjmp = (callerHandler.useSetjmp() || callerHandler.callerSetjmp);
        this.callerHandler = callerHandler;
        currentPC = pc;
    }
    public void setSyncObj(String syncObj, String syncCB) {                   
        this.syncObj = syncObj;                                                   
        this.syncCB = syncCB;                                                     
    }
    public void checkNullPointerException(IR ir, JavaVariable var) {
	if( ir.hasAttribute(IRAttribute.NULLCHECKELIMINATED) ) return ;
    /*
	if(AOTCWriter.PROFILE_LOOPPEELING) {
	    for(int i = 0 ; i < ir.getLoopHeaderNum() ; i++) {
		int loopHeaderBpc = ir.getLoopHeader(i);
		IR loopHeaderIR = cfg.getIRAtBpc(loopHeaderBpc);
		if(loopHeaderIR.isLoopPeeling(loopHeaderBpc) && LoopInvariantFinder.checkTailDomination(loopHeaderBpc, ir, cfg)) {
		    if(LoopInvariantFinder.isLoopInvariant(loopHeaderBpc, ir, var, cfg)) {
			//out.Printf("fprintf(stderr, \"EXCEPTION\\t" + cfg.getMethodInfo().getNativeName(true) + "\\t" + cfg.getMethodInfo().type.string + 
			//	"\\t" + loopHeaderBpc + "\\t" + loopHeaderIR.getLoopSize() + "\\n\");");
			out.Printf("increaseEliminatedExceptionCounter(\"" + classinfo.className + "\", \"" + methodinfo.name + methodinfo.type.string + "\",\""
				+ loopHeaderBpc + "\", \"" + loopHeaderIR.getLoopSize() + "\");");
			return;
		    }
		}
	    }
	}
    */
	out.Print("if(!" + var + ")");
	out.Print("{");
    /* if(AOTCWriter.EXCEPTION_INFO) out.Print("exc_gen++;"); */
	gotoHandler(ir.getOriginalBpc(), EXCEPTION_NULL_POINTER);
	out.Print("}");
    }
    
    public boolean checkInstantiationException(IR ir, int index) {
	//ClassConstant cc = (ClassConstant)classinfo.constants[index];
	ClassConstant cc = (ClassConstant)classinfo.getConstantPool().getConstants()[index];
	ClassInfo ci = cc.find();
	if(ci == null) {
	    new aotc.share.Assert(false, "Error : classinfo is null.");
	}
	if((ci.access & Const.ACC_ABSTRACT) != 0 || (ci.access & Const.ACC_INTERFACE) != 0) {
        /* if(AOTCWriter.EXCEPTION_INFO) out.Print("exc_gen++;"); */
	    gotoHandler(ir.getOriginalBpc(), EXCEPTION_INSTANTIATION);
	    return true;
	}
	return false;
    }
    
    public void checkOutOfMemoryError(IR ir, JavaVariable var) {
	out.Print("if(unlikely(" + var + " == NULL))");
	out.Print("{");
    /* if(AOTCWriter.EXCEPTION_INFO) out.Print("exc_gen++;");*/
	gotoHandler(ir.getOriginalBpc(), EXCEPTION_OUT_OF_MEMORY);
	out.Print("}");
    }
    
    public void checkClassCastException(IR ir, JavaVariable cb, ClassInfo ci) {
	if(ir.hasAttribute(IRAttribute.CLASSCASTCHECKELIMINATED)) {
	    return;
	}
    /*
	if(AOTCWriter.PROFILE_LOOPPEELING) {
	    for(int i = 0 ; i < ir.getLoopHeaderNum() ; i++) {
		int loopHeaderBpc = ir.getLoopHeader(i);
		IR loopHeaderIR = cfg.getIRAtBpc(loopHeaderBpc);
		if(loopHeaderIR.isLoopPeeling(loopHeaderBpc) && LoopInvariantFinder.checkTailDomination(loopHeaderBpc, ir, cfg)) {
		    if(LoopInvariantFinder.isLoopInvariant(loopHeaderBpc, ir, cb, cfg)) {
			//out.Printf("fprintf(stderr, \"EXCEPTION\\t" + cfg.getMethodInfo().getNativeName(true) + "\\t" + cfg.getMethodInfo().type.string + 
			//	"\\t" + loopHeaderBpc + "\\t" + loopHeaderIR.getLoopSize() + "\\n\");");
			out.Printf("increaseEliminatedExceptionCounter(\"" + classinfo.className + "\", \"" + methodinfo.name + methodinfo.type.string + "\",\""
				+ loopHeaderBpc + "\", \"" + loopHeaderIR.getLoopSize() + "\");");
			return;
		    }
		}
	    }
	}
    */
        CVMClass vmClass = (CVMClass) ci.vmClass;
        String castClass = vmClass.getNativeName();
	
        out.Print("CVMBool result;");

        String ciNativeName = vmClass.getNativeName()+ "_Classblock";
        out.Print("extern const CVMClassBlock "+ciNativeName+";");
        out.Print("CVMClassBlock* castCB  = (CVMClassBlock*)(&"+ciNativeName+");");

        out.Print("if (" + cb + " == 0) {");
        out.Print("result = CVM_TRUE;");
        out.Print("} else {");

        //System.out.println("checkcast to " + castClass);
        out.Print("CVMClassBlock* objCB = (CVMClassBlock*)CVMobjectGetClass(" + cb + ");");
        if (castClass.equals("java_lang_Object")) {
            //System.out.println("------------------------------");
            //System.out.println("checkcast to java_lang_Object");
            //System.out.println("------------------------------");
            out.Print("result = CVM_TRUE;");
        } else if (castClass.equals("java_lang_Cloneable") || castClass.equals("java_io_Serializable")) {
            //System.out.println("------------------------------------------------");
            //System.out.println("checkcast to java_lang_Clanable or java_io_Seri~");
            //System.out.println("------------------------------------------------");
            out.Print("AOTCImplements(ee, objCB, castCB, result);");
            out.Print("if (result == CVM_FALSE) {");
            out.Print("// All array class implements Clonable and Serializable");
            out.Print("result = CVMisArrayClass(objCB);");
            out.Print("}");
        } else if (vmClass.isInterface()) {
            //System.out.println("----------------------");
            //System.out.println("checkcast to Interface");
            //System.out.println("----------------------");
            out.Print("AOTCImplements(ee, objCB, castCB, result);");
        } else if (vmClass.isArrayClass()) {
            //System.out.println("checkcast to ArrayClass");
            out.Print("AOTCIsArrayOf(ee, objCB, castCB, result);");
        } else { // general class
            //System.out.println("checkcast to NormalClass");
            out.Print("AOTCIsSubclassOf(ee, objCB, castCB, result);");
        }

        out.Print("}");
        out.Print("if (result == CVM_FALSE)");
	out.Print("{");
    /* if(AOTCWriter.EXCEPTION_INFO) out.Print("exc_gen++;"); */
	gotoHandler(ir.getOriginalBpc(), EXCEPTION_CLASS_CAST);
        out.Print("}");
    }
    
    public void checkNegativeArraySizeException(IR ir, JavaVariable length) {
	if( ir.hasAttribute(IRAttribute.ARRAYNEGATIVESIZECHECKELIMINATED) ) return ;
    /*
	if(AOTCWriter.PROFILE_LOOPPEELING) {
	    for(int i = 0 ; i < ir.getLoopHeaderNum() ; i++) {
		int loopHeaderBpc = ir.getLoopHeader(i);
		IR loopHeaderIR = cfg.getIRAtBpc(loopHeaderBpc);
		if(loopHeaderIR.isLoopPeeling(loopHeaderBpc) && LoopInvariantFinder.checkTailDomination(loopHeaderBpc, ir, cfg)) {
		    if(LoopInvariantFinder.isLoopInvariant(loopHeaderBpc, ir, length, cfg)) {
			//out.Printf("fprintf(stderr, \"EXCEPTION\\t" + cfg.getMethodInfo().getNativeName(true) + "\\t" + cfg.getMethodInfo().type.string + 
			//	"\\t" + loopHeaderBpc + "\\t" + loopHeaderIR.getLoopSize() + "\\n\");");
			out.Printf("increaseEliminatedExceptionCounter(\"" + classinfo.className + "\", \"" + methodinfo.name + methodinfo.type.string + "\",\""
				+ loopHeaderBpc + "\", \"" + loopHeaderIR.getLoopSize() + "\");");
			return;
		    }
		}
	    }
	}
    */

	out.Print("if(" + length + " < 0)");
	out.Print("{");
    /* if(AOTCWriter.EXCEPTION_INFO) out.Print("exc_gen++;"); */
	gotoHandler(ir.getOriginalBpc(), EXCEPTION_NEGATIVE_ARRAY_SIZE);
	out.Print("}");
    }
    
    public void checkArrayIndexOutOfBoundsException(IR ir, JavaVariable index, JavaVariable array, String typeName) {
	/*
	out.Printf("fprintf(stderr, \"EXCEPTION\\t" + cfg.getMethodInfo().getNativeName(true) + "\\t" + cfg.getMethodInfo().type.string + 
		"\\t" + ir.getBpc() + "\\n\");");
	*/
	if(ir.hasAttribute(IRAttribute.ARRAYBOUNDCHECKELIMINATED)) return ;
    /*
	if(AOTCWriter.PROFILE_LOOPPEELING) {
	    for(int i = 0 ; i < ir.getLoopHeaderNum() ; i++) {
		int loopHeaderBpc = ir.getLoopHeader(i);
		IR loopHeaderIR = cfg.getIRAtBpc(loopHeaderBpc);
		if(loopHeaderIR.isLoopPeeling(loopHeaderBpc) && LoopInvariantFinder.checkTailDomination(loopHeaderBpc, ir, cfg)) {
		    if(LoopInvariantFinder.isLoopInvariant(loopHeaderBpc, ir, index, cfg) && LoopInvariantFinder.isLoopInvariant(loopHeaderBpc, ir, array, cfg)) {
			//out.Printf("fprintf(stderr, \"EXCEPTION\\t" + cfg.getMethodInfo().getNativeName(true) + "\\t" + cfg.getMethodInfo().type.string + 
			//	"\\t" + loopHeaderBpc + "\\t" + loopHeaderIR.getLoopSize() + "\\n\");");
			out.Printf("increaseEliminatedExceptionCounter(\"" + classinfo.className + "\", \"" + methodinfo.name + methodinfo.type.string + "\",\""
				+ loopHeaderBpc + "\", \"" + loopHeaderIR.getLoopSize() + "\");");
			return;
		    }
		}
	    }
	}
    */
	if(!ArrayBoundCheckEliminator.use) {
	    out.Print("{");
	    out.Print("CVMUint32 len;");
	    out.Print("len = CVMD_arrayGetLength((" + typeName + ")" + array + ");");
	    out.Print("if((CVMUint32)" + index + " >= len)" );
	    out.Print("{");
        /* if(AOTCWriter.EXCEPTION_INFO) out.Print("exc_gen++;"); */
	    gotoHandler(ir.getOriginalBpc(), EXCEPTION_ARRAY_INDEX_OUT_OF_BOUNDS);
	    out.Print("}");
	    out.Print("}");
	    return;
	}
	
	JavaVariable upperBound = ir.getUpperBound();
	JavaVariable lowBound = ir.getLowBound();
	int upperOffset = ir.getUpperOffset() + 1;
	int lowOffset = ir.getLowOffset();
	if(upperBound == null && lowBound == null) return;
	out.Print("{");
	if(upperBound == null) {
	    String offset = (lowOffset == 0 ? "" : (" + " + lowOffset));
	    if(lowBound.getType() == TypeInfo.TYPE_REFERENCE) {
		out.Print("CVMUint32 len;");
		out.Print("len = CVMD_arrayGetLength((" + typeName + ")" + lowBound + ");");
		out.Print("if(" + index + " < len" + offset + ")");
	    }else {
		out.Print("if(" + index + " < " + lowBound + offset + ")");
	    }
	}else if(lowBound == null) {
	    String offset = (upperOffset == 0 ? "" : (" + " + upperOffset));
	    if(upperBound.getType() == TypeInfo.TYPE_REFERENCE) {
		out.Print("CVMUint32 len;");
		out.Print("len = CVMD_arrayGetLength((" + typeName + ")" + upperBound + ");");
		out.Print("if(" + index + " >= len" + offset + ")");
	    }else {
		out.Print("if(" + index + " >= " + upperBound + offset + ")");
	    }
	}else {
	    if(lowBound.equals(ArrayBoundCheckEliminator.zero) && lowOffset == 0) {
		String offset = (upperOffset == 0 ? "" : (" + " + upperOffset));
		if(upperBound.getType() == TypeInfo.TYPE_REFERENCE) {
		    out.Print("CVMUint32 len;");
		    out.Print("len = CVMD_arrayGetLength((" + typeName + ")" + upperBound + ");");
		    out.Print("if((CVMUint32)" + index + " >= len" + offset + ")");
		}else {
		    out.Print("if((CVMUint32)" + index + " >= " + upperBound + offset + ")");
		}
	    }else {
		String upperCheck, lowCheck;
		String offset1 = (upperOffset == 0 ? "" : (" + " + upperOffset));
		if(upperBound.getType() == TypeInfo.TYPE_REFERENCE) {
		    out.Print("CVMUint32 len;");
		    out.Print("len = CVMD_arrayGetLength((" + typeName + ")" + upperBound + ");");
		    upperCheck = index + " >= len" + offset1;
		}else {
		    upperCheck = index + " >= " + upperBound  + offset1;
		}
		String offset2 = (lowOffset == 0 ? "" : (" + " + lowOffset));
		if(lowBound.getType() == TypeInfo.TYPE_REFERENCE) {
		    out.Print("CVMUint32 len;");
		    out.Print("len = CVMD_arrayGetLength((" + typeName + ")" + lowBound + ");");
		    lowCheck = index + " < len " + offset2;
		    out.Print("if(" + index + " < len" + offset2 + ")");
		}else {
		    lowCheck = index + " < " + lowBound  + offset2;
		}
		out.Print("if(" + upperCheck + " || " + lowCheck + ")");
	    }
	}
	out.Print("{");
    /* if(AOTCWriter.EXCEPTION_INFO) out.Print("exc_gen++;"); */
	gotoHandler(ir.getOriginalBpc(), EXCEPTION_ARRAY_INDEX_OUT_OF_BOUNDS);
	out.Print("}");
	out.Print("}");

	/*
	   String length = ir.getArrayLength();

	   out.Print("{");
	   if(length.equals("ref")) {
	   out.Print("CVMUint32 len;");
	   out.Print("len = CVMD_arrayGetLength((" + typeName + ")" + array + ");");
	   out.Print("if((CVMUint32)" + index + " >= len)" );
	   }else {
	   out.Print("if((CVMUint32)" + index + " >= " + length + ")" );
	   }
	   out.Print("{");
	   gotoHandler(ir.getOriginalBpc(), EXCEPTION_ARRAY_INDEX_OUT_OF_BOUNDS);
	   out.Print("}");
	   out.Print("}");
	 */
    }

    public void checkArrayStoreException(IR ir, int arrayType, JavaVariable array, JavaVariable operand) {
	if( arrayType == TypeInfo.TYPE_REFERENCE ) {
	    if(ir.hasAttribute(IRAttribute.ARRAYSTORECHECKELIMINATED)) {
		return;
	    }
        /*
	    if(AOTCWriter.PROFILE_LOOPPEELING) {
		for(int i = 0 ; i < ir.getLoopHeaderNum() ; i++) {
		    int loopHeaderBpc = ir.getLoopHeader(i);
		    IR loopHeaderIR = cfg.getIRAtBpc(loopHeaderBpc);
		    if(loopHeaderIR.isLoopPeeling(loopHeaderBpc) && LoopInvariantFinder.checkTailDomination(loopHeaderBpc, ir, cfg)) {
			if(LoopInvariantFinder.isLoopInvariant(loopHeaderBpc, ir, array, cfg) && LoopInvariantFinder.isLoopInvariant(loopHeaderBpc, ir, operand, cfg)) {
			    //out.Printf("fprintf(stderr, \"EXCEPTION\\t" + cfg.getMethodInfo().getNativeName(true) + "\\t" + cfg.getMethodInfo().type.string + 
			    //	    "\\t" + loopHeaderBpc + "\\t" + loopHeaderIR.getLoopSize() + "\\n\");");
			    out.Printf("increaseEliminatedExceptionCounter(\"" + classinfo.className + "\", \"" + methodinfo.name + methodinfo.type.string + "\",\""
				    + loopHeaderBpc + "\", \"" + loopHeaderIR.getLoopSize() + "\");");
			    return;
			}
		    }
		}
	    }
        */
	    out.Print("if(" + operand + ") {");
	    out.Print("CVMClassBlock* elemType = CVMarrayElementCb((CVMClassBlock*)CVMobjectGetClass(" + array + "));");
	    out.Print("CVMClassBlock* rhsType = (CVMClassBlock*)CVMobjectGetClass(" + operand + ");");

	    out.Print("if(!CVMisAssignable(ee, rhsType, elemType))");
	    out.Print("{");
        /* if(AOTCWriter.EXCEPTION_INFO) out.Print("exc_gen++;"); */
	    gotoHandler(ir.getOriginalBpc(), EXCEPTION_ARRAY_STORE);
	    out.Print("}");
	    out.Print("}");
	}
    }
    
    public void checkArithmeticException(IR ir, JavaVariable divisor) {
	if(ir.hasAttribute(IRAttribute.ARITHMETICCHECKELIMINATED)) return;
    /*
	if(AOTCWriter.PROFILE_LOOPPEELING) {
	    for(int i = 0 ; i < ir.getLoopHeaderNum() ; i++) {
		int loopHeaderBpc = ir.getLoopHeader(i);
		IR loopHeaderIR = cfg.getIRAtBpc(loopHeaderBpc);
		if(loopHeaderIR.isLoopPeeling(loopHeaderBpc) && LoopInvariantFinder.checkTailDomination(loopHeaderBpc, ir, cfg)) {
		    if(LoopInvariantFinder.isLoopInvariant(loopHeaderBpc, ir, divisor, cfg)) {
			//out.Printf("fprintf(stderr, \"EXCEPTION\\t" + cfg.getMethodInfo().getNativeName(true) + "\\t" + cfg.getMethodInfo().type.string + 
			//	"\\t" + loopHeaderBpc + "\\t" + loopHeaderIR.getLoopSize() + "\\n\");");
			out.Printf("increaseEliminatedExceptionCounter(\"" + classinfo.className + "\", \"" + methodinfo.name + methodinfo.type.string + "\",\""
				+ loopHeaderBpc + "\", \"" + loopHeaderIR.getLoopSize() + "\");");
			return;
		    }
		}
	    }
	}
    */
	out.Print("if(" + divisor + " == 0)");
	out.Print("{");
    /* if(AOTCWriter.EXCEPTION_INFO) out.Print("exc_gen++;"); */
	gotoHandler(ir.getOriginalBpc(), EXCEPTION_ARITHMETIC);
	out.Print("}");
    }
    
    public void checkIncompatibleClassChangeError(IR ir, String index) {
	out.Print("if(" + index + " < 0)");
	out.Print("{");
    /* if(AOTCWriter.EXCEPTION_INFO) out.Print("exc_gen++;"); */
	gotoHandler(ir.getOriginalBpc(), EXCEPTION_INCOMPATIBLE_CLASS_CHANGE);
	out.Print("}");
    }
    
    public void checkExceptionInvoke(IR ir) {
	int handlerPC = findHandler(ir.getOriginalBpc(), EXCEPTION_NOT_GEN);
	out.Print("{");
	out.Print("CVMObject* exc;");
	out.Print("AOTCExceptionOccurred(ee, exc);");

	out.Print("if(unlikely(exc != NULL))");
	out.Print("{");
	gotoHandler(ir.getOriginalBpc(), EXCEPTION_NOT_GEN);
	out.Print("}");
	out.Print("}");
    }

    public void doAthrow(IR ir, JavaVariable exception) {
	int handlerPC = findHandler(ir.getOriginalBpc(), EXCEPTION_NOT_GEN);
	out.Print("{");
	checkNullPointerException(ir, exception);
    /* if(AOTCWriter.EXCEPTION_INFO) out.Print("exc_gen++;"); */
	if ( handlerPC != -1 ) {
	    CCodeGenerator.exception_s0_ref++;
	    //if( !exception.toString().equals("s0_ref") ) {
        if( !exception.toString().equals(s0_ref) ) {
        out.Print(s0_ref + "_ICell->ref_DONT_ACCESS_DIRECTLY = " + exception + ";" );
		//out.Print("s0_ref = " + exception + ";" );
		//out.Print("s0_ref_ICell->ref_DONT_ACCESS_DIRECTLY = " + exception + ";" );
	    }
	    out.Print("exception_type = EXCEPTION_GEN_S0_REF;");
	    doPopSetjmp(ir, cfg.getIRAtBpc(handlerPC)); // set/longjmp
	    doGoto(handlerPC);
	} else {
        out.Print("{");
	    out.Print("CVMObject __attribute__ ((unused)) *exc = " + exception + ";");
	    out.Print("CVMgcUnsafeThrowLocalException(ee, "+exception+");");
	    doPopSetjmp(ir, null); // set/longjmp
	    doExceptionExit(EXCEPTION_NOT_GEN);
        out.Print("}");
	}
	out.Print("}");
    }
    
    public void findCatchBlock(IR ir) {
	int bpc = ir.getOriginalBpc();
	for( int i = 0 ; i < exceptionTable.length ; i++ ) {
	    if( exceptionTable[i].handlerPC == bpc ) {
		int nextHandlerPC = -1;
		// find if next handler is exist
		for( int j = i + 1 ; j < exceptionTable.length ; j++ ) {
		    if( exceptionTable[j].startPC <= exceptionTable[i].startPC
			    && exceptionTable[j].endPC >= exceptionTable[i].endPC ) {
			nextHandlerPC = exceptionTable[j].handlerPC;
			break;
		    }
		}
		catchException( ir, exceptionTable[i], nextHandlerPC );
		break;
	    }
	}
    }
    
    private void catchException(IR ir, ExceptionEntry exceptionTable, int nextHandlerPC) {
	int bpc = ir.getOriginalBpc();
	out.Print("{");
	if( exceptionTable.catchType != null ) {
	    String catchName = exceptionTable.catchType.name.string.replace('/','_');
	    catchName = catchName.replace('$', '_');
	    out.Print("extern const CVMClassBlock " + catchName +"_Classblock;");
	    out.Print("jboolean catch;");
	    //out.Print("catch = CVMisAssignable(ee, (CVMClassBlock*)CVMobjectGetClass(s0_ref), (CVMClassBlock*)(&"+catchName + "_Classblock));");
        out.Print("catch = CVMisAssignable(ee, (CVMClassBlock*)CVMobjectGetClass(" + s0_ref + "_ICell->ref_DONT_ACCESS_DIRECTLY), (CVMClassBlock*)(&"+catchName + "_Classblock));");
	    if( nextHandlerPC != -1 ) {
		out.Print("if(!catch)");
		doPopSetjmp(ir, cfg.getIRAtBpc(nextHandlerPC));
		doGoto(nextHandlerPC);
	    } else {
		out.Print("if(!catch) {");
		doPopSetjmp(ir, null);
		doExceptionExit(EXCEPTION_HANDLER);
		out.Print("}");
	    }
	}
	out.Print("CVMclearLocalException(ee);");
	//out.Print("CVMclearRemoteException(ee);");
	out.Print("}");
	if( label.contains(new Integer(( -bpc )))) {
	    out.Print(getGotoLabel(bpc) + "_CATCH:");
	}
	int index = MethodInliner.inline_depth*JavaVariable.INLINE_START;
	//if (index > 0 && ir.getShortOpcode()!=OpcodeConst.opc_pop) {
	if (ir.getShortOpcode()!=OpcodeConst.opc_pop) {
	    //out.Print("s" + index + "_ref = s0_ref;");
        out.Print("s" + index + "_ref = " + s0_ref + "_ICell->ref_DONT_ACCESS_DIRECTLY;");
	}
    }

    private void gotoHandler(int bpc, int type) {
	int handlerPC = findHandler(bpc, type);
	IR ir = cfg.getIRAtBpc(bpc);
	out.Print("{");
	if(handlerPC == -1) {
	    doPopSetjmp(ir, null); // set/longjmp
	    doExceptionExit(type);
	} else {
	    if( type == EXCEPTION_NOT_GEN ) {
		//out.Print("s0_ref = exc;" );
		//out.Print("s0_ref_ICell->ref_DONT_ACCESS_DIRECTLY = s0_ref;" );
	    CCodeGenerator.exception_s0_ref++;
        out.Print(s0_ref + "_ICell->ref_DONT_ACCESS_DIRECTLY = exc;" );
		out.Print("exception_type = EXCEPTION_NOT_GEN;");
	    } else if(type <= EXCEPTION_INCOMPATIBLE_CLASS_CHANGE){	
	    CCodeGenerator.exception_s0_ref++;
		String exceptionClassName = this.exceptionClassName[type];
		exceptionClassName = exceptionClassName.replace('/','_') + "_Classblock";
		out.Print("extern const CVMClassBlock " + exceptionClassName + ";");
		out.Print("CVMObject *exception = CVMgcAllocNewInstance(ee, (CVMClassBlock*)(&" + exceptionClassName + "));");
		//out.Print("s0_ref = exception;");
		//out.Print("s0_ref_ICell->ref_DONT_ACCESS_DIRECTLY = s0_ref;");
        out.Print(s0_ref + "_ICell->ref_DONT_ACCESS_DIRECTLY = exception;");
		out.Print("exception_type = EXCEPTION_GEN_S0_REF;");
	    }
	    if( handlerPC < -1 ) {
		doPopSetjmp(ir, cfg.getIRAtBpc(-handlerPC)); // set/longjmp
		doGotoCatch(-handlerPC);
	    }else {
		doPopSetjmp(ir, cfg.getIRAtBpc(handlerPC)); // set/longjmp
		doGoto(handlerPC);
	    }
	}
	out.Print("}");
    }

    public int findHandler(int bpc, int type) {
	String exceptionClassName = this.exceptionClassName[type];
	exceptionClassName = exceptionClassName.replace('/','.');
	Class exceptionClass = null, catchClass = null;
	try {
	    exceptionClass = Class.forName(exceptionClassName);
	} catch(ClassNotFoundException e) {
	    type = -1;
	} catch(Exception e) {
	    new aotc.share.Assert(false, "Error at findHandler in ExceptionHandler!");
	}
	for( int i = 0 ; i < exceptionTable.length ; i++) {
	    if( exceptionTable[i].startPC <= bpc
		&& exceptionTable[i].endPC > bpc ) {
		if( type < 0 ) return exceptionTable[i].handlerPC;
		try {
		    catchClass = Class.forName(exceptionTable[i].catchType.name.string.replace('/','.'));
		} catch(Exception e) {
		    return exceptionTable[i].handlerPC;
		}
		if( catchClass.isAssignableFrom(exceptionClass) ) {
		    int handlerPC = ( -1 * exceptionTable[i].handlerPC);
		    label.add(new Integer(handlerPC));
		    return handlerPC;
		}
	    }
	}
	return -1;
    }
    private void doGotoCatch(int bpc) {
	out.Print("goto " + getGotoLabel(bpc) + "_CATCH;");
    }
    private void doGoto(int bpc) {
	out.Print("goto " + getGotoLabel(bpc) + ";");
    }
    private String getGotoLabel(int bpc) {
	return "LL_" + name + "_" + bpc;
    }

    private void doExceptionExit(int type) {
	if(callerHandler==null) {
	    CCodeGenerator.exception_exit++;
	    if(type != EXCEPTION_HANDLER) out.Print("exception_type = " + exceptionTypeName[type] + ";");
	    out.Print("goto EXCEPTION_EXIT;");
	} else {
        if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING && useSetjmp()) {
            out.Printf("{");
            /*if(AOTCWriter.DEBUG_INFO)*/ // out.Printf("fprintf(stderr, \"Pop Setjmp Exception Exit" + cfg.getMethodInfo().getNativeName(true) + "   : %d\\n\", (--setjmpCount));");
            out.Printf("lock_temp = ee->lockList;");
            out.Printf("ee->handlerList = ee->handlerList->next;");
            out.Printf("ee->lockList = ee->lockList->next;");
            out.Printf("free(temp);");
            out.Printf("free(lock_temp);");
            out.Printf("}");
        }else if((cfg.getMethodInfo().access & ClassFileConst.ACC_SYNCHRONIZED)!=0){
            out.Printf("{");
            if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.CALL_CHECK_HANDLING) {
                String cb = ((CVMClass)(classinfo.vmClass)).getNativeName() + "_Classblock";
                out.Print("extern const CVMClassBlock " + cb + ";");
                String sync = (cb + ".javaInstanceX");

                out.Print("if(!CVMfastTryUnlock(ee, " + sync + "->ref_DONT_ACCESS_DIRECTLY)) {");
                out.Print("CVMobjectUnlock(ee, " + sync + ");");
                out.Print("}");
            }else {
                new aotc.share.Assert(syncObj != null , "synchronization error");
                if(syncCB != null) {
                    out.Print("extern const CVMClassBlock " + syncCB + ";");
                }
                out.Print("if(!CVMfastTryUnlock(ee, " + syncObj + "->ref_DONT_ACCESS_DIRECTLY)) {");
                out.Print("CVMobjectUnlock(ee, " + syncObj + ");");
                out.Print("}");
                
                if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING) {
                    out.Printf("lock_temp = ee->lockList;");
                }else {
                    out.Printf("AOTCLockList* lock_temp = ee->lockList;");
                }
                out.Printf("ee->lockList = ee->lockList->next;");
                out.Printf("free(lock_temp);");
            }

            out.Printf("}");
        }
	    callerHandler.gotoHandler(currentPC,type);
	}
    }
    public void doPushSetjmp() {
        /* if(AOTCWriter.EXCEPTION_INFO && useSetjmp()) out.Print("prol_setjmp++;"); */
        if(AOTCWriter.EXCEPTION_IMPLEMENTATION != AOTCWriter.SETJMP_PROLOG_HANDLING) return;
        if(useSetjmp()) {
            /*if(AOTCWriter.DEBUG_INFO)*/ //out.Print("fprintf(stderr, \"Push Setjmp " + cfg.getMethodInfo().getNativeName(true) + "  : %d\\n\", (++setjmpCount));");
            out.Print("{");
            out.Print("temp->next = ee->handlerList;");
            out.Print("ee->handlerList = temp;");
            String sync = "cls_ICell";
            if((methodinfo.access & ClassFileConst.ACC_SYNCHRONIZED)==0) {
                if(MethodInliner.inline_depth != 0) {
                    String cb = ((CVMClass)(classinfo.vmClass)).getNativeName() + "_Classblock";
                    out.Print("extern const CVMClassBlock " + cb + ";");
                    sync = (cb + ".javaInstanceX");
                }
                out.Print("lock_temp->next = ee->lockList;");
                out.Print("lock_temp->lock_object = " + sync + ";");
                out.Print("ee->lockList = lock_temp;");
            }

            out.Print("if(setjmp(temp->handle_block)) {");

            out.Print("CVMFrame* jniFrame = (&ee->interpreterStack)->currentFrame;");
            //if(AOTCWriter.DEBUG_INFO) out.Print("fprintf(stderr, \"LongJmp  End" + cfg.getMethodInfo().getNativeName(true) + "\\n\");");
            /*if(AOTCWriter.DEBUG_INFO)*/ // out.Print("fprintf(stderr, \"LongJmp  End" + cfg.getMethodInfo().getNativeName(true) + "\\n\");");
    

            out.Print("while(ee->lockList->lock_object != " + sync + ") {");
            out.Print("if(!CVMfastTryUnlock(ee, ee->lockList->lock_object->ref_DONT_ACCESS_DIRECTLY)) {");
            out.Print("CVMobjectUnlock(ee, ee->lockList->lock_object);");
            out.Print("}");
            out.Print("lock_temp = ee->lockList;");
            out.Print("ee->lockList = ee->lockList->next;");
            out.Print("free(lock_temp);");
            out.Print("}");

            out.Print("while(currentFrame != jniFrame) {");
            /* if(AOTCWriter.DEBUG_INFO) out.Print("fprintf(stderr, \"Pop Frame " + cfg.getMethodInfo().getNativeName(true) + "\\n\");"); */
            out.Print("CVMpopFrame(&ee->interpreterStack, jniFrame);");
            out.Print("jniFrame = (&ee->interpreterStack)->currentFrame;");
            out.Print("}");
            out.Print("currentFrame->topOfStack = restoreTOS;");
            out.Print("CVMgetJNIFrame(currentFrame)->freeList = restoreFreeList;");
            /* if(AOTCWriter.EXCEPTION_INFO) out.Print("exc_cp_read++;");*/
            out.Print("if(exception_cp->label != NULL) {");
            /* if(AOTCWriter.DEBUG_INFO) */ //out.Print("fprintf(stderr, \"Pop Setjmp goto" + cfg.getMethodInfo().getNativeName(true) + " nocount : %d\\n\", (setjmpCount));");
            out.Print("AOTCExceptionOccurred(ee, " + s0_ref + "_ICell->ref_DONT_ACCESS_DIRECTLY);");
            out.Print("exception_type = EXCEPTION_NOT_GEN;");
            /* if(AOTCWriter.DEBUG_INFO) out.Print("fprintf(stderr, \"GOTO~~~~\\n\");"); */
            /* if(AOTCWriter.EXCEPTION_INFO) out.Print("exc_cp_read++;"); */
            out.Print("goto *(exception_cp->label);");
            out.Print("} else {");
            /* if(AOTCWriter.DEBUG_INFO)*/ //out.Print("fprintf(stderr, \"Pop Setjmp in set" + cfg.getMethodInfo().getNativeName(true) + "  : %d\\n\", (--setjmpCount));");

            out.Print("ee->handlerList = ee->handlerList->next;");
            out.Print("free(temp);");
            if((methodinfo.access & ClassFileConst.ACC_SYNCHRONIZED)==0) {
                out.Print("lock_temp = ee->lockList;");
                out.Print("ee->lockList = ee->lockList->next;");
                out.Print("free(lock_temp);");
            }
            /* if(AOTCWriter.DEBUG_INFO) */ //out.Print("fprintf(stderr, \"LongJmp In  Start" + cfg.getMethodInfo().getNativeName(true) + "\\n\");");
            out.Print("longjmp(ee->handlerList->handle_block, 1);");
            out.Print("}");

            out.Print("}");
            out.Print("}");
        }
    }
    public void doPushSetjmp(IR ir, IR target) {
        if(AOTCWriter.EXCEPTION_IMPLEMENTATION != AOTCWriter.SETJMP_TRY_HANDLING) return;
        int bpc = ir.getOriginalBpc();
        int targetBpc = target.getOriginalBpc();
        int handlerPC = 0, i;
        for(i = exceptionTable.length - 1 ; i >= 0 ; i--) {
            int startBpc = exceptionTable[i].startPC;
            int endBpc = exceptionTable[i].endPC;
            if(!(targetBpc > startBpc && targetBpc < endBpc && (bpc < startBpc || bpc >= endBpc)) 
                    || !hasInvoke[i]
                    || exceptionTable[i].catchType == null) {
                continue;
            }

            // if(AOTCWriter.DEBUG_INFO) out.Print("fprintf(stderr, \"Push Setjmp " + cfg.getMethodInfo().getNativeName(true) + "  " + ir.getBpc() + " : %d\\n\", ++setjmpCount);");
            out.Print("{");
            out.Print("AOTCHandlerList* temp = (AOTCHandlerList*)malloc(sizeof(AOTCHandlerList));");
            out.Print("AOTCLockList* lock_temp = (AOTCLockList*)malloc(sizeof(AOTCLockList));");
            String sync = "cls_ICell";
            if(MethodInliner.inline_depth != 0) {
                String cb = ((CVMClass)(classinfo.vmClass)).getNativeName() + "_Classblock";
                out.Print("extern const CVMClassBlock " + cb + ";");
                sync = (cb + ".javaInstanceX");
            }
            out.Print("lock_temp->next = ee->lockList;");
            out.Print("lock_temp->lock_object = " + sync + ";");
            out.Print("ee->lockList = lock_temp;");
            out.Print("temp->next = ee->handlerList;");
            out.Print("ee->handlerList = temp;");
            //out.print("push_try++;");
            out.Print("if(setjmp(temp->handle_block)) {");
            // if(AOTCWriter.DEBUG_INFO) out.Print("fprintf(stderr, \"LongJmp  End" + cfg.getMethodInfo().getNativeName(true) + "\\n\");");
            out.Print("CVMFrame* jniFrame = ee->interpreterStack.currentFrame;");

            out.Print("while(ee->lockList->lock_object != " + sync + ") {");
            out.Print("if(!CVMfastTryUnlock(ee, ee->lockList->lock_object->ref_DONT_ACCESS_DIRECTLY)) {");
            out.Print("CVMobjectUnlock(ee, ee->lockList->lock_object);");
            out.Print("}");
            out.Print("lock_temp = ee->lockList;");
            out.Print("ee->lockList = ee->lockList->next;");
            out.Print("free(lock_temp);");

            out.Print("}");
            out.Print("while(currentFrame != jniFrame) {");
            // if(AOTCWriter.DEBUG_INFO) out.Print("fprintf(stderr, \"Pop Frame " + cfg.getMethodInfo().getNativeName(true) + "\\n\");");
            out.Print("CVMpopFrame(&ee->interpreterStack, jniFrame);");
            out.Print("jniFrame = (&ee->interpreterStack)->currentFrame;");
            out.Print("}");
            out.Print("currentFrame->topOfStack = restoreTOS;");
            out.Print("CVMgetJNIFrame(currentFrame)->freeList = restoreFreeList;");

            // if(AOTCWriter.DEBUG_INFO) out.Print("fprintf(stderr, \"Pop Setjmp in try start " + cfg.getMethodInfo().getNativeName(true) + "  " + ir.getBpc() + " : %d\\n\", --setjmpCount);");
            out.Print("lock_temp = ee->lockList;");
            out.Print("ee->lockList = ee->lockList->next;");
            out.Print("free(lock_temp);");
            out.Print("temp = ee->handlerList;");
            out.Print("ee->handlerList = ee->handlerList->next;");
            out.Print("free(temp);");

            //out.Print("AOTCExceptionOccurred(ee, s0_ref);");
            out.Print("AOTCExceptionOccurred(ee, " + s0_ref + "_ICell->ref_DONT_ACCESS_DIRECTLY);");
            out.Print("exception_type = EXCEPTION_NOT_GEN;");
            //out.Print("push_try--;");
            //out.print("CVMassert(push_try >= 0);");
            out.Print("goto " + getGotoLabel(exceptionTable[i].handlerPC)+";");

            out.Print("}");
            out.Print("}");
        }
    }

    public void doPushSetjmp(IR ir) {
        /*
        if(AOTCWriter.EXCEPTION_INFO) {
            int bpc = ir.getOriginalBpc();
            int handlerPC = 0, i;
            for(i = exceptionTable.length - 1 ; i >= 0 ; i--) {
                if(bpc != exceptionTable[i].startPC
                        || exceptionTable[i].catchType == null) continue;
                if(hasInvoke[i]) {
                    out.Print("try_setjmp++;");
                }
            }
        }
        */
        if(AOTCWriter.EXCEPTION_IMPLEMENTATION != AOTCWriter.SETJMP_TRY_HANDLING) return;
        int bpc = ir.getBpc();
        int handlerPC = 0, i;
        for(i = exceptionTable.length - 1 ; i >= 0 ; i--) {
            if(bpc != exceptionTable[i].startPC
                    || exceptionTable[i].catchType == null) continue;
            if(hasInvoke[i]) {
                // if(AOTCWriter.DEBUG_INFO) out.Print("fprintf(stderr, \"Push Setjmp " + cfg.getMethodInfo().getNativeName(true) + "  " + ir.getBpc() + " : %d\\n\", ++setjmpCount);");
                out.Print("{");
                out.Print("AOTCHandlerList* temp = (AOTCHandlerList*)malloc(sizeof(AOTCHandlerList));");
                out.Print("AOTCLockList* lock_temp = (AOTCLockList*)malloc(sizeof(AOTCLockList));");
                String sync = "cls_ICell";
                if(MethodInliner.inline_depth != 0) {
                    String cb = ((CVMClass)(classinfo.vmClass)).getNativeName() + "_Classblock";
                    out.Print("extern const CVMClassBlock " + cb + ";");
                    sync = (cb + ".javaInstanceX");
                }
                out.Print("lock_temp->next = ee->lockList;");
                out.Print("lock_temp->lock_object = " + sync + ";");
                out.Print("ee->lockList = lock_temp;");
                out.Print("temp->next = ee->handlerList;");
                out.Print("ee->handlerList = temp;");
		//out.print("push_try++;");
                out.Print("if(setjmp(temp->handle_block)) {");
                // if(AOTCWriter.DEBUG_INFO) out.Print("fprintf(stderr, \"LongJmp  End" + cfg.getMethodInfo().getNativeName(true) + "\\n\");");
                out.Print("CVMFrame* jniFrame = ee->interpreterStack.currentFrame;");

                out.Print("while(ee->lockList->lock_object != " + sync + ") {");
                out.Print("if(!CVMfastTryUnlock(ee, ee->lockList->lock_object->ref_DONT_ACCESS_DIRECTLY)) {");
                out.Print("CVMobjectUnlock(ee, ee->lockList->lock_object);");
                out.Print("}");
                out.Print("lock_temp = ee->lockList;");
                out.Print("ee->lockList = ee->lockList->next;");
                out.Print("free(lock_temp);");

                out.Print("}");
                out.Print("while(currentFrame != jniFrame) {");
                // if(AOTCWriter.DEBUG_INFO) out.Print("fprintf(stderr, \"Pop Frame " + cfg.getMethodInfo().getNativeName(true) + "\\n\");");
                out.Print("CVMpopFrame(&ee->interpreterStack, jniFrame);");
                out.Print("jniFrame = (&ee->interpreterStack)->currentFrame;");
                out.Print("}");
                out.Print("currentFrame->topOfStack = restoreTOS;");
                out.Print("CVMgetJNIFrame(currentFrame)->freeList = restoreFreeList;");

                // if(AOTCWriter.DEBUG_INFO) out.Print("fprintf(stderr, \"Pop Setjmp in try start " + cfg.getMethodInfo().getNativeName(true) + "  " + ir.getBpc() + " : %d\\n\", --setjmpCount);");
                out.Print("lock_temp = ee->lockList;");
                out.Print("ee->lockList = ee->lockList->next;");
                out.Print("free(lock_temp);");
                out.Print("temp = ee->handlerList;");
                out.Print("ee->handlerList = ee->handlerList->next;");
                out.Print("free(temp);");

                //out.Print("AOTCExceptionOccurred(ee, s0_ref);");
		out.Print("AOTCExceptionOccurred(ee, " + s0_ref + "_ICell->ref_DONT_ACCESS_DIRECTLY);");
                out.Print("exception_type = EXCEPTION_NOT_GEN;");
                //out.Print("push_try--;");
                //out.print("CVMassert(push_try >= 0);");
                out.Print("goto " + getGotoLabel(exceptionTable[i].handlerPC)+";");

                out.Print("}");
                out.Print("}");
            }
            if(cfg.getNeedLabelList().contains(new Integer(ir.getBpc()))) {
                out.Print(getGotoLabel(ir.getBpc()) + "_" + i +":");
            }
        }
    }
    public void doPopSetjmp(IR ir, IR succ) {
        if(AOTCWriter.EXCEPTION_IMPLEMENTATION != AOTCWriter.SETJMP_TRY_HANDLING) return;
        int popCount = 0;
        int bpc = ir.getOriginalBpc();
        for(int i = 0 ; i < exceptionTable.length ; i++) {
            if(exceptionTable[i].catchType == null || !hasInvoke[i]) continue;
            int startBpc = exceptionTable[i].startPC;
            int endBpc = exceptionTable[i].endPC;
            if(succ == null) {
                if( bpc >= startBpc && bpc < endBpc) {
			out.Print("// " + bpc + "  " + startBpc + "  " + endBpc);
                    popCount++;
                }
            }else {
                int succBpc = succ.getOriginalBpc();
                if((succBpc < startBpc || succBpc >= endBpc) && bpc >= startBpc && bpc < endBpc) {
		out.Print("// " + bpc + "  " + startBpc + "  " + endBpc + " " + succBpc);
                    popCount++;
                }
            }
        }
        if(popCount > exceptionTable.length) {
            new aotc.share.Assert(false, "pop number is greater than exception table size");
        }
        if(popCount == 0) return;
        out.Print("{");
        out.Print("AOTCHandlerList* temp;");
        out.Print("AOTCLockList* lock_temp;");
        for(int i = 0 ; i < popCount ; i++) {
            // if(AOTCWriter.DEBUG_INFO) out.Print("fprintf(stderr, \"Pop Setjmp " + cfg.getMethodInfo().getNativeName(true) + "  " + ir.getOriginalBpc() + " : %d\\n\", --setjmpCount);");
		//out.print("push_try--;");
		//out.print("CVMassert(push_try >= 0);");
            out.Print("temp = ee->handlerList;");
            out.Print("lock_temp = ee->lockList;");
            out.Print("ee->lockList = ee->lockList->next;");
            out.Print("free(lock_temp);");
            out.Print("ee->handlerList = ee->handlerList->next;");
            out.Print("free(temp);");
        }
        out.Print("}");
    }

    public void setExceptionCP(int bpc) {
        /*
        if(AOTCWriter.EXCEPTION_INFO && (useSetjmp() || callerSetjmp)) {
            IR ir = cfg.getIRAtBpc(bpc);
            bpc = ir.getOriginalBpc();
            short opcode = ir.getShortOpcode();
            if(isInvokeBytecode(opcode)) {
                for(int i = 0 ; i < exceptionTable.length ; i++) {
                    int startBpc = exceptionTable[i].startPC;
                    int endBpc = exceptionTable[i].endPC;
                    if(bpc >= startBpc && bpc < endBpc) {
                        out.Print("exc_cp_write++;");
                    }
                    if(callerHandler != null) {
                        callerHandler.setExceptionCP(currentPC);
                    }else {
                        out.Print("exc_cp_write++;");
                    }
                }
            }
        }
        */

        if(AOTCWriter.EXCEPTION_IMPLEMENTATION != AOTCWriter.SETJMP_PROLOG_HANDLING || (!useSetjmp()&& !callerSetjmp)) return;
        IR ir = cfg.getIRAtBpc(bpc);
        bpc = ir.getOriginalBpc();
        short opcode = ir.getShortOpcode();
        if(isInvokeBytecode(opcode)) {

            for(int i = 0 ; i < exceptionTable.length ; i++) {
                int startBpc = exceptionTable[i].startPC;
                int endBpc = exceptionTable[i].endPC;
                if(bpc >= startBpc && bpc < endBpc) {
                    out.Print("exception_cp->label = &&" + getGotoLabel(exceptionTable[i].handlerPC)+";");
                    return;
                }
            }
            if(callerHandler != null) {
                callerHandler.setExceptionCP(currentPC);
            }else {
                out.Print("exception_cp->label = NULL;");
            }
        }
    }
    public boolean isInvokeBytecode(short opcode) {
        switch(opcode) {
            case OpcodeConst.opc_invokestatic_checkinit_quick:
            case OpcodeConst.opc_invokestatic_quick:
            case OpcodeConst.opc_invokevirtual_quick_w:
            case OpcodeConst.opc_invokenonvirtual_quick:
            case OpcodeConst.opc_invokevirtual_quick:
            case OpcodeConst.opc_ainvokevirtual_quick:
            case OpcodeConst.opc_dinvokevirtual_quick:
            case OpcodeConst.opc_vinvokevirtual_quick:
            case OpcodeConst.opc_invokevirtualobject_quick:
            case OpcodeConst.opc_invokeinterface_quick:
                return true;
        }
        return false;
    }
}

class ExceptionSetjmpInfo {
    public boolean useSetjmp = false;
    public int setjmpCount = 0;
    ExceptionSetjmpInfo(boolean useSetjmp, int setjmpCount) {
        this.useSetjmp = useSetjmp;
        this.setjmpCount = setjmpCount;
    }
}

