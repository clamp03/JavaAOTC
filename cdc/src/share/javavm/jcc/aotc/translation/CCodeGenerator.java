package aotc.translation;

import java.util.*;
import components.*;
import opcodeconsts.*;
import consts.*;
import util.*;

import jcc.*;
import vm.*;

import aotc.cfg.*;
import aotc.ir.*;
import aotc.share.*;
import aotc.optimizer.*;
import aotc.*;

/**
 * CCodeGenerator is the class for writing "C" code output from IR representation.
 * It is constructed for each method.
 * And method "translate" is called for each IR.
 */
public class CCodeGenerator {

    /**
     * The number of gc retore points.
     */
    //public static int gc_point;
    public static int exception_exit;
    public static int exception_s0_ref;
    //public static boolean exitLabel;

    private CFG cfg;

    /**
     * The function name for goto label.
     */
    private String name;
    //private ExceptionEntry exceptionTable[];
    /**
     * The class information.
     */
    private ClassInfo classinfo;
    /**
     * The method information.
     */
    private MethodInfo methodinfo;
    /**
     * The output stream.
     */
    private AOTCPrint out;
    private TreeSet needLabelList;
    private String MB, SCB, CLZ, TMB, FB, CASTCB;

    //private int handlerPC;
    /**
     * The bytecode bpc.
     */
    private int bpc;
    //private boolean nullcheck;
    /**
     * The preload flag for special bytecode.
     */
    private boolean preload;

    /**
     * Current IR
     */
    private IR currentIR;

    /**
     * Live Variable (liveOUT) of currentIR
     */
    private TreeSet liveVariables;

    /**
     * Exception Handle
     */
    private ExceptionHandler exceptionHandler;
    
    /**
     * Class constructor.
     *
     * @param classinfo	    the class information
     * @param methodinfo    the method information
     * @param name	    the method name
     * @param out	    the output stream
     //* @param exceptionTable the exception information table
     * @param needLabelList ??
     */
    public CCodeGenerator(CFG cfg, String name, AOTCPrint out,
			  ExceptionEntry exceptionTable[]) {
        //this.exitLabel = false;
        this.cfg = cfg;
	this.classinfo = cfg.getClassInfo();
	this.methodinfo = cfg.getMethodInfo();
	this.name = name;
	this.out = out;
	//this.exceptionTable = exceptionTable;
        this.needLabelList = cfg.getNeedLabelList();

	exceptionHandler = new ExceptionHandler(cfg, name, out, exceptionTable);
    }

    public ExceptionHandler getExceptionHandler() {
	return exceptionHandler;
    }

    /**
     * Writes "C" code output from IR representation.
     * It writes "C" code output to output stream "out".
     * See additional details for the output example.
     *
     * @param ir    the IR representation
     */
    public void translate(IR ir) {
	//if(AOTCWriter.PROFILE_LOOPPEELING && ir.hasAttribute(IRAttribute.LOOP_HEADER)) {
	//    out.Printf("fprintf(stderr, \"LOOP\\t" + cfg.getMethodInfo().getNativeName(true) + "\\t" + cfg.getMethodInfo().type.string + 
	//	    "\\t" + ir.getBpc() + "\\t" + ir.getLoopSize() + "\\n\");");
	//}
	String debug = "/* Loop Header : ";
	for(int i = 0 ; i < ir.getLoopHeaderNum() ; i++) {
	    debug += "  " + ir.getLoopHeader(i);
	}
	debug += "  */";
	out.Print(debug);
	/*
	if(ir.hasAttribute(IRAttribute.LOOP_PREHEADER) && !ir.getSucc(0).hasAttribute(IRAttribute.LOOP_HEADER)) {
	    out.Printf("fprintf(stderr, \"LoopPeeling : " + cfg.getMethodInfo().getNativeName(true) + " " + ir.getSucc(0).getBpc() + "\\n\");");
	}
	*/

	// Gets the needed information for IR.
        this.currentIR = ir;
        Bytecode bytecode = ir.getBytecode();
	bpc = ir.getBpc();
	short shortOpcode = ir.getShortOpcode();
	preload = ir.hasAttribute(IRAttribute.PRELOADED);
	//nullcheck = !ir.hasAttribute(IRAttribute.NULLCHECKELIMINATED);

	// ??
        MB = "mbAt" + bpc;
        SCB = "scbAt" + bpc;
        CLZ = "clzAt" + bpc;
        TMB = "tmbAt" + bpc;
        FB = "fbAt" + bpc;
        //CASTCB = "castCbAt" + bpc;
        
        //if (MethodInliner.inline_depth == 0) { 
	liveVariables = ir.getLiveVariables();
        //} else { // inlined : inherit live variables of the call site
        //    this.liveVariables = MethodInliner.liveVariables;
        //}
	
	// Prints IR(bytecode) info.
        out.Print("/* " + ir + " */");
       
	// Print goto label if needs
        // (target of branch instruction)
        if (needLabelList.contains(new Integer(bpc))) {
	    out.Print(getGotoLabel(bpc) + ":");
        }
	//For Loop Peeling Profile
    /*
	if(AOTCWriter.PROFILE_LOOPPEELING) {
	    for(int i = 0 ; i < ir.getSuccNum() ; i++) {
		IR succ = ir.getSucc(i);
		if(succ.hasAttribute(IRAttribute.LOOP_HEADER) && !ir.isInLoop(succ.getBpc())) {
		    out.Print("prof_" + MethodInliner.inline_depth + "_" + succ.getBpc() + " = CVM_FALSE;");
		}
	    }
	    if(ir.hasAttribute(IRAttribute.LOOP_HEADER)) {
		out.Print("increaseLoopCounter(\"" + classinfo.className + "\", \"" + methodinfo.name + methodinfo.type.string + "\",\""
			+ ir.getBpc() + "\", \"" + ir.getLoopSize() + "\");");
		out.Print("if(!prof_" + MethodInliner.inline_depth + "_" + ir.getBpc() + ") {");
		out.Print("increaseLoopIntoCounter(\"" + classinfo.className + "\", \"" + methodinfo.name + methodinfo.type.string + "\",\""
			+ ir.getBpc() + "\", \"" + ir.getLoopSize() + "\");");
		out.Print("prof_" + MethodInliner.inline_depth + "_" + ir.getBpc() + " = CVM_TRUE;");
		out.Print("}");
	    }
	}
    */
    if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING /* || AOTCWriter.EXCEPTION_INFO */) {
        exceptionHandler.setExceptionCP(ir.getBpc());
    }

	// Do preload action
        Iterator it = ir.getPreloadExpressions().iterator();
        //int preloadIndex = 0;
        while (it.hasNext()) {
            LoadExpression exp = (LoadExpression) it.next();
            switch(exp.getExpType()) {
                case LoadExpression.INIT_CB_FOR_INVOKE_STATIC:
                    // TODO evaluate below lines only when class != this
                    out.Print("{");
                    out.Print("//CheckInit_type1 for " + exp.getBpc() + " at loop-preheader");
        	    out.Print("extern const CVMClassBlock " + exp.getCB() + ";");
                    doCheckInit(exp.getCB(), true, null);
                    out.Print("}");
                    break;
                case LoadExpression.LOAD_MB_FOR_INVOKE_VIRTUAL:
                    out.Print("{");
                    out.Print("//Load_MB for " + exp.getBpc() + " at loop-preheader");
	            out.Print("if(" + exp.getObjectRef() + ") {");
                    out.Print("clzAt" + exp.getBpc() + " = (CVMClassBlock*)CVMobjectGetClass(" + exp.getObjectRef() + ");");
                    out.Print("mbAt" + exp.getBpc() + " = CVMcbMethodTableSlot(clzAt" + exp.getBpc()  + ", " + exp.getCPIndex() + ");");
                    out.Print("}");
                    out.Print("}");
                    break;
                case LoadExpression.INIT_CB_FOR_PUTSTATIC_GETSTATIC:
                    ClassInfo ci = ((FieldConstant)classinfo.getConstantPool().getConstants()[exp.getCPIndex()]).find().parent;
                    if (!ci.equals(this.classinfo)) {
                        String cb = exp.getCB();
                        out.Print("{");
                        out.Print("//CheckInit_type2 for " + exp.getBpc() + " at loop-preheader");
                        out.Print("extern const CVMClassBlock " + cb + ";");
                        doCheckInit(cb, true, null);
                        out.Print("}");
                    }
                    break;
                case LoadExpression.INIT_CB_FOR_NEW:
                    {
                        String cb = exp.getCB();
                        out.Print("{");
                        out.Print("//CheckInit_type3 for " + exp.getBpc() + " at loop-preheader");
                        out.Print("extern const CVMClassBlock "+ cb +";");
	                doCheckInit(cb, true, null); // no closing-braket
                        out.Print("}");
                    }
                    break;
            }
        }

	exceptionHandler.findCatchBlock(currentIR);
	exceptionHandler.doPushSetjmp(ir);
	// Does nothing because this IR is eliminated by our optimization.
        //if(classinfo.className.toString().equals("spec/benchmarks/_213_javac/NewInstanceExpression") && methodinfo.name.toString().equals("checkValue")) {
            //System.out.println("Success");
            //out.Printf("CVMassert(l5_ref->hdr.clas->methodTablePtrX != NULL);");
        //}
        if (ir.hasAttribute(IRAttribute.ELIMINATED)) {
	    IR fallthru = ir.getFallthruSucc();
	    if(fallthru != null) {
		exceptionHandler.doPopSetjmp(ir, fallthru);
	    }else if(ir.getSuccNum() == 0) {
		exceptionHandler.doPopSetjmp(ir, null);
	    }
            return;
        }
	//out.Print("fprintf(stderr, \"" + MethodInliner.inline_depth + " DEBUGGING " + bpc + "\\n\");");
	// Below switch statement is the core of bytecode-to-C translation
	switch(shortOpcode)
	{
	    case OpcodeConst.opc_nop:
	    case OpcodeConst.opc_pop:
	    case OpcodeConst.opc_pop2:
		break;

	    case OpcodeConst.opc_aconst_null:
	    case OpcodeConst.opc_iconst_m1:
	    case OpcodeConst.opc_iconst_0:
	    case OpcodeConst.opc_iconst_1:
	    case OpcodeConst.opc_iconst_2:
	    case OpcodeConst.opc_iconst_3:
	    case OpcodeConst.opc_iconst_4:
	    case OpcodeConst.opc_iconst_5:
	    case OpcodeConst.opc_lconst_0:
	    case OpcodeConst.opc_lconst_1:
	    case OpcodeConst.opc_fconst_0:
	    case OpcodeConst.opc_fconst_1: 
	    case OpcodeConst.opc_fconst_2:
	    case OpcodeConst.opc_dconst_0:
	    case OpcodeConst.opc_dconst_1:
	    case OpcodeConst.opc_bipush:
	    case OpcodeConst.opc_sipush:
	    case OpcodeConst.opc_iload:
	    case OpcodeConst.opc_iload_0:
	    case OpcodeConst.opc_iload_1:
	    case OpcodeConst.opc_iload_2:
	    case OpcodeConst.opc_iload_3:
	    case OpcodeConst.opc_lload:
	    case OpcodeConst.opc_lload_0:
	    case OpcodeConst.opc_lload_1:
	    case OpcodeConst.opc_lload_2:
	    case OpcodeConst.opc_lload_3:
	    case OpcodeConst.opc_fload:
	    case OpcodeConst.opc_fload_0:
	    case OpcodeConst.opc_fload_1:
	    case OpcodeConst.opc_fload_2:
	    case OpcodeConst.opc_fload_3:
	    case OpcodeConst.opc_dload:
	    case OpcodeConst.opc_dload_0:
	    case OpcodeConst.opc_dload_1:
	    case OpcodeConst.opc_dload_2:
	    case OpcodeConst.opc_dload_3:
	    case OpcodeConst.opc_aload:
	    case OpcodeConst.opc_aload_0:
	    case OpcodeConst.opc_aload_1:
	    case OpcodeConst.opc_aload_2:
	    case OpcodeConst.opc_aload_3:
	    case OpcodeConst.opc_istore:
	    case OpcodeConst.opc_istore_0:
	    case OpcodeConst.opc_istore_1:
	    case OpcodeConst.opc_istore_2:
	    case OpcodeConst.opc_istore_3:
	    case OpcodeConst.opc_lstore:
	    case OpcodeConst.opc_lstore_0:
	    case OpcodeConst.opc_lstore_1:
	    case OpcodeConst.opc_lstore_2:
	    case OpcodeConst.opc_lstore_3:
	    case OpcodeConst.opc_fstore:
	    case OpcodeConst.opc_fstore_0:
	    case OpcodeConst.opc_fstore_1:
	    case OpcodeConst.opc_fstore_2:
	    case OpcodeConst.opc_fstore_3:
	    case OpcodeConst.opc_dstore:
	    case OpcodeConst.opc_dstore_0:
	    case OpcodeConst.opc_dstore_1:
	    case OpcodeConst.opc_dstore_2:
	    case OpcodeConst.opc_dstore_3:
	    case OpcodeConst.opc_astore:
	    case OpcodeConst.opc_astore_0:
	    case OpcodeConst.opc_astore_1:
	    case OpcodeConst.opc_astore_2:
	    case OpcodeConst.opc_astore_3:
	    case OpcodeConst.opc_dup:
		doCopy(ir.getTarget(0), ir.getOperand(0));
		break;

	    case OpcodeConst.opc_iaload:
	    case OpcodeConst.opc_laload:
	    case OpcodeConst.opc_faload:
	    case OpcodeConst.opc_daload:
	    case OpcodeConst.opc_aaload:
	    case OpcodeConst.opc_baload:
	    case OpcodeConst.opc_caload:
	    case OpcodeConst.opc_saload:
		{
		    int arrayType = -1;
		    switch(shortOpcode) {
			case OpcodeConst.opc_iaload:
			    arrayType = TypeInfo.TYPE_INT;
			    break;
			case OpcodeConst.opc_laload:
			    arrayType = TypeInfo.TYPE_LONG;
			    break;
			case OpcodeConst.opc_faload:
			    arrayType = TypeInfo.TYPE_FLOAT;
			    break;
			case OpcodeConst.opc_daload:
			    arrayType = TypeInfo.TYPE_DOUBLE;
			    break;
			case OpcodeConst.opc_aaload:
			    arrayType = TypeInfo.TYPE_REFERENCE;
			    break;
			case OpcodeConst.opc_baload:
			    arrayType = TypeInfo.TYPE_BYTE;
			    break;
			case OpcodeConst.opc_caload:
			    arrayType = TypeInfo.TYPE_CHAR;
			    break;
			case OpcodeConst.opc_saload:
			    arrayType = TypeInfo.TYPE_SHORT;
			    break;
		    }

		    // target, arrayref, index, array type
		    doArrayLoad(ir.getTarget(0), ir.getOperand(0), ir.getOperand(1), arrayType);
		}
		break;

	    case OpcodeConst.opc_iastore:
	    case OpcodeConst.opc_lastore:
	    case OpcodeConst.opc_fastore:
	    case OpcodeConst.opc_dastore:
	    case OpcodeConst.opc_aastore:
	    case OpcodeConst.opc_bastore:
	    case OpcodeConst.opc_castore:
	    case OpcodeConst.opc_sastore:
		{
		    int arrayType = -1;
		    switch(shortOpcode) {
			case OpcodeConst.opc_iastore:
			    arrayType = TypeInfo.TYPE_INT;
			    break;
			case OpcodeConst.opc_lastore:
			    arrayType = TypeInfo.TYPE_LONG;
			    break;
			case OpcodeConst.opc_fastore:
			    arrayType = TypeInfo.TYPE_FLOAT;
			    break;
			case OpcodeConst.opc_dastore:
			    arrayType = TypeInfo.TYPE_DOUBLE;
			    break;
			case OpcodeConst.opc_aastore:
			    arrayType = TypeInfo.TYPE_REFERENCE;
			    break;
			case OpcodeConst.opc_bastore:
			    arrayType = TypeInfo.TYPE_BYTE;
			    break;
			case OpcodeConst.opc_castore:
			    arrayType = TypeInfo.TYPE_CHAR;
			    break;
			case OpcodeConst.opc_sastore:
			    arrayType = TypeInfo.TYPE_SHORT;
			    break;
		    }

		    // arrayref, index, value, array type
		    doArrayStore(ir.getOperand(0), ir.getOperand(1), ir.getOperand(2), arrayType);
		}
		break;

	    case OpcodeConst.opc_dup_x1:
		doDupX1(ir.getOperand(1).getType(), ir.getOperand(0), ir.getOperand(1),
			ir.getTarget(0), ir.getTarget(1), ir.getTarget(2));
		break;

	    case OpcodeConst.opc_dup_x2:
		if(ir.getNumOfOperands()==3) {
		    doDupX2(ir.getOperand(1).getType(), ir.getOperand(2).getType(),
			    ir.getOperand(0), ir.getOperand(1), ir.getOperand(2),
			    ir.getTarget(0), ir.getTarget(1), ir.getTarget(2), ir.getTarget(3));
		} else if(ir.getNumOfOperands()==2) {
		    doDupX1(ir.getOperand(1).getType(), ir.getOperand(0), ir.getOperand(1),
			    ir.getTarget(0), ir.getTarget(1), ir.getTarget(2));
		} else {
		    new aotc.share.Assert(false, "dup_x2 operands number error!!");
		}
		break;

	    case OpcodeConst.opc_dup2:
		if(ir.getNumOfOperands()==2) {
		    doCopy(ir.getTarget(2), ir.getOperand(1));
		    doCopy(ir.getTarget(3), ir.getOperand(0));
		} else if(ir.getNumOfOperands()==1) {
		    doCopy(ir.getTarget(1), ir.getOperand(0));
		} else {
		    new aotc.share.Assert(false, "dup2 operands number error!!");
		}
		break;

	    case OpcodeConst.opc_dup2_x1:
		if(ir.getNumOfOperands()==3) {
		    doDup2X1(ir.getOperand(2).getType(), ir.getOperand(0),
			    ir.getOperand(1), ir.getOperand(2),
			    ir.getTarget(0), ir.getTarget(1), ir.getTarget(2),
			    ir.getTarget(3), ir.getTarget(4));
		} else if(ir.getNumOfOperands()==2) {
		    doDupX1(ir.getOperand(1).getType(), ir.getOperand(0), ir.getOperand(1),
			    ir.getTarget(0), ir.getTarget(1), ir.getTarget(2));
		} else {
		    new aotc.share.Assert(false, "dup2_x1 operands number error!!");
		}
		break;

	    case OpcodeConst.opc_dup2_x2:
		if(ir.getNumOfTargets()==6) {
		    doDup2X2(ir.getOperand(2).getType(), ir.getOperand(3).getType(),
			    ir.getOperand(0), ir.getOperand(1), ir.getOperand(2),
			    ir.getOperand(3),
			    ir.getTarget(0), ir.getTarget(1), ir.getTarget(2),
			    ir.getTarget(3), ir.getTarget(4), ir.getTarget(5));
		} else if(ir.getNumOfTargets()==4) {
		    doDupX2(ir.getOperand(1).getType(), ir.getOperand(2).getType(),
			    ir.getOperand(0), ir.getOperand(1), ir.getOperand(2),
			    ir.getTarget(0), ir.getTarget(1), ir.getTarget(2), ir.getTarget(3));
		} else if(ir.getNumOfTargets()==5) {
		    doDup2X1(ir.getOperand(2).getType(), ir.getOperand(0),
			    ir.getOperand(1), ir.getOperand(2),
			    ir.getTarget(0), ir.getTarget(1), ir.getTarget(2),
			    ir.getTarget(3), ir.getTarget(4));
		} else if(ir.getNumOfTargets()==3) {
		    doDupX1(ir.getOperand(1).getType(), ir.getOperand(0), ir.getOperand(1),
			    ir.getTarget(0), ir.getTarget(1), ir.getTarget(2));
		} else {
		    new aotc.share.Assert(false, "dup2_x2 operands number error!!");
		}
		break;

	    case OpcodeConst.opc_swap:
		{
		    TemporaryVariable tt = new TemporaryVariable(ir.getOperand(1).getType(), 0);
		    out.Print("{");
		    if(tt.getType() == TypeInfo.TYPE_REFERENCE) {
			out.Printf("CVMObject* " + tt + ";");
		    } else {
			out.Printf(TypeInfo.typeName[tt.getType()] + " " + tt + ";");
		    }
		    out.Print(tt + " = " + ir.getOperand(1) + ";");
		    doCopy(ir.getTarget(0), ir.getOperand(0));
		    doCopy(ir.getTarget(1), tt);
		    out.Print("}");
		}
		break;

	    case OpcodeConst.opc_frem:
		out.Print(ir.getTarget(0) + " = CVMfloatRem("
			+ ir.getOperand(0) + ", " + ir.getOperand(1) + ");");
		break;
	    case OpcodeConst.opc_drem:
		out.Print(ir.getTarget(0) + " = CVMdoubleRem("
			+ ir.getOperand(0) + ", " + ir.getOperand(1) + ");");
		break;
		
	    case OpcodeConst.opc_iadd: case OpcodeConst.opc_ladd:
	    case OpcodeConst.opc_fadd: case OpcodeConst.opc_dadd:
	    case OpcodeConst.opc_isub: case OpcodeConst.opc_lsub:
	    case OpcodeConst.opc_fsub: case OpcodeConst.opc_dsub:
	    case OpcodeConst.opc_imul: case OpcodeConst.opc_lmul:
	    case OpcodeConst.opc_fmul: case OpcodeConst.opc_dmul:
	    case OpcodeConst.opc_idiv: case OpcodeConst.opc_ldiv:
	    case OpcodeConst.opc_fdiv: case OpcodeConst.opc_ddiv:
	    case OpcodeConst.opc_irem: case OpcodeConst.opc_lrem:
	    case OpcodeConst.opc_ishl: case OpcodeConst.opc_lshl:
	    case OpcodeConst.opc_ishr: case OpcodeConst.opc_lshr:
	    case OpcodeConst.opc_iushr: case OpcodeConst.opc_lushr:
	    case OpcodeConst.opc_iand: case OpcodeConst.opc_land:
	    case OpcodeConst.opc_ior: case OpcodeConst.opc_lor:
	    case OpcodeConst.opc_ixor: case OpcodeConst.opc_lxor:
	    case OpcodeConst.opc_iinc:
		// Integer divide operator can throw a exception.
		/* if (shortOpcode == OpcodeConst.opc_idiv
			|| shortOpcode == OpcodeConst.opc_ldiv
			|| shortOpcode == OpcodeConst.opc_irem
			|| shortOpcode == OpcodeConst.opc_lrem ) {
        */
		if ( shortOpcode == OpcodeConst.opc_ldiv
			|| shortOpcode == OpcodeConst.opc_lrem ) {
		    /*
		    out.Print("if(" + ir.getOperand(1) + " == 0)");
		    if( handlerPC == -1) {
			out.Print("{");
			out.Print("exception_type = EXCEPTION_ARITHMETIC;");
			exception_exit++;
			out.Print("goto EXCEPTION_EXIT;");
			out.Print("}");
		    }else {
			doThrowException("java/lang/ArithmeticException");
		    }
		    */
		    exceptionHandler.checkArithmeticException(currentIR, ir.getOperand(1));

		}
		if(shortOpcode == OpcodeConst.opc_iushr) {
		    out.Print(ir.getTarget(0) + " = (CVMInt32)((CVMUint32)" + ir.getOperand(0)
			    + " >> " + ir.getOperand(1) + ");");
		} else if(shortOpcode == OpcodeConst.opc_lushr) {
		    out.Print(ir.getTarget(0) + " = (CVMInt64)((unsigned long long)" + ir.getOperand(0)
			    + " >> " + ir.getOperand(1) + ");");
        }else if (shortOpcode == OpcodeConst.opc_idiv || shortOpcode == OpcodeConst.opc_irem) {
            out.Print("if(" + ir.getOperand(1) + " > 0 ) {");
		    doBinaryOperation(ir.getTarget(0), ir.getOperand(0), ir.getOperand(1), ir.getOperator());
            out.Print("} else {");
		    exceptionHandler.checkArithmeticException(currentIR, ir.getOperand(1));
            out.Print(ir.getTarget(0) + " = intBinaryOp2(" + ir.getOperand(0) + ", " + ir.getOperand(1) + ", " + ir.getOperator() + ");");
            out.Print("}");
		} else {
		    doBinaryOperation(ir.getTarget(0), ir.getOperand(0), ir.getOperand(1), ir.getOperator());
		}
		break;

	    case OpcodeConst.opc_ineg:
	    case OpcodeConst.opc_lneg:
	    case OpcodeConst.opc_fneg:
	    case OpcodeConst.opc_dneg:
		doUnaryOperation(ir.getTarget(0), ir.getOperand(0), ir.getOperator());
		break;

	    case OpcodeConst.opc_i2l: case OpcodeConst.opc_i2f: case OpcodeConst.opc_i2d:
	    case OpcodeConst.opc_l2i: case OpcodeConst.opc_l2f: case OpcodeConst.opc_l2d:
	    case OpcodeConst.opc_f2i: case OpcodeConst.opc_f2l: case OpcodeConst.opc_f2d:
	    case OpcodeConst.opc_d2i: case OpcodeConst.opc_d2l: case OpcodeConst.opc_d2f:
	    case OpcodeConst.opc_i2b: case OpcodeConst.opc_i2c: case OpcodeConst.opc_i2s:
		{
		    int toType = -1;
		    if (shortOpcode == OpcodeConst.opc_l2i || shortOpcode == OpcodeConst.opc_f2i
			    || shortOpcode == OpcodeConst.opc_d2i) {
			toType = TypeInfo.TYPE_INT;
		    } else if (shortOpcode == OpcodeConst.opc_i2l || shortOpcode == OpcodeConst.opc_f2l
			    || shortOpcode == OpcodeConst.opc_d2l) {
			toType = TypeInfo.TYPE_LONG;
		    } else if (shortOpcode == OpcodeConst.opc_i2f || shortOpcode == OpcodeConst.opc_l2f
			    || shortOpcode == OpcodeConst.opc_d2f) {
			toType = TypeInfo.TYPE_FLOAT;
		    } else if (shortOpcode == OpcodeConst.opc_i2d || shortOpcode == OpcodeConst.opc_l2d
			    || shortOpcode == OpcodeConst.opc_f2d) {
			toType = TypeInfo.TYPE_DOUBLE;
		    } else if (shortOpcode == OpcodeConst.opc_i2b) {
			toType = TypeInfo.TYPE_BYTE;
		    } else if (shortOpcode == OpcodeConst.opc_i2c) {
			toType = TypeInfo.TYPE_CHAR;
		    } else if (shortOpcode == OpcodeConst.opc_i2s) {
			toType = TypeInfo.TYPE_SHORT;
		    }
		    doCast(ir.getTarget(0), ir.getOperand(0), TypeInfo.typeName[toType]);
		}
		break;			

	    case OpcodeConst.opc_lcmp: 
		out.Print("if(" + ir.getOperand(0) + " == " + ir.getOperand(1) + ") {");
		out.Print(ir.getTarget(0) + " = 0;");
		out.Print("} else if(" + ir.getOperand(0) + " < " + ir.getOperand(1) + ") {");
		out.Print(ir.getTarget(0) + " = -1;");
		out.Print("} else {");
		out.Print(ir.getTarget(0) + " = 1;");
		out.Print("}");
		break;

	    case OpcodeConst.opc_fcmpl: 
	    case OpcodeConst.opc_dcmpl: 
		{
		    out.Print("if(" + ir.getOperand(0) + " == " + ir.getOperand(1) + ") {");
		    out.Print(ir.getTarget(0) + " = 0;");
		    out.Print("} else if(" + ir.getOperand(0) + " > " + ir.getOperand(1) + ") {");
		    out.Print(ir.getTarget(0) + " = 1;");
		    out.Print("} else {");
                    out.Print(ir.getTarget(0) + " = -1;");
		    out.Print("}");
		}
		break;
            case OpcodeConst.opc_fcmpg: 
            case OpcodeConst.opc_dcmpg:
		{
		    out.Print("if(" + ir.getOperand(0) + " == " + ir.getOperand(1) + ") {");
		    out.Print(ir.getTarget(0) + " = 0;");
		    out.Print("} else if(" + ir.getOperand(0) + " < " + ir.getOperand(1) + ") {");
		    out.Print(ir.getTarget(0) + " = -1;");
		    out.Print("} else {");
		    out.Print(ir.getTarget(0) + " = 1;");
		    out.Print("}");
		}
		break;
	    case OpcodeConst.opc_ifeq: case OpcodeConst.opc_ifne:
	    case OpcodeConst.opc_iflt: case OpcodeConst.opc_ifge:
	    case OpcodeConst.opc_ifgt: case OpcodeConst.opc_ifle:
	    case OpcodeConst.opc_if_icmpeq: case OpcodeConst.opc_if_icmpne:
	    case OpcodeConst.opc_if_icmplt: case OpcodeConst.opc_if_icmpge:
	    case OpcodeConst.opc_if_icmpgt: case OpcodeConst.opc_if_icmple:
	    case OpcodeConst.opc_if_acmpeq: case OpcodeConst.opc_if_acmpne:
	    case OpcodeConst.opc_ifnull:    case OpcodeConst.opc_ifnonnull:
	    case OpcodeConst.opc_nonnull_quick:
		out.Print("if (" + ir.getOperand(0) + ir.getOperator() + ir.getOperand(1) + ") {");
		//if(bpc > ir.getBranchTarget().getBpc()) {
		if(ir.hasAttribute(IRAttribute.LOOP_TAIL)
		    && ir.getBranchTarget().hasAttribute(IRAttribute.LOOP_HEADER)
                    && (!ir.hasAttribute(IRAttribute.REDUNDANT_GC_CHECK))) {
		    out.Print("CVMD_gcSafeCheckPoint(ee, {}, {");
		    doGCRestore(methodinfo, bpc, liveVariables, out, true);
		    out.Print("} );");
		    //doGCLabel(bpc);
		}
		//exceptionHandler.doPopSetjmp(ir, ir.getBranchTarget());
		doGoto(ir.getBranchTarget().getBpc());
		out.Print("}");
		break;

	    case OpcodeConst.opc_goto: case OpcodeConst.opc_goto_w:
		//if(bpc > ir.getBranchTarget().getBpc()) {
		if(ir.hasAttribute(IRAttribute.LOOP_TAIL)
		    && ir.getBranchTarget().hasAttribute(IRAttribute.LOOP_HEADER) 
                    && (!ir.hasAttribute(IRAttribute.REDUNDANT_GC_CHECK))) {
		    out.Print("CVMD_gcSafeCheckPoint(ee, {}, {");
		    doGCRestore(methodinfo, bpc, liveVariables, out, true);
		    out.Print("} );");
		    //doGCLabel(bpc);
		}
		//exceptionHandler.doPopSetjmp(ir, ir.getBranchTarget());
		doGoto(ir.getBranchTarget().getBpc());
		break;

	    case OpcodeConst.opc_jsr:
		//exceptionHandler.doPopSetjmp(ir, ir.getBranchTarget());
		doJsr(ir.getTarget(0), bpc+3, ir.getBranchTarget().getBpc());
		break;
	    case OpcodeConst.opc_jsr_w:
		//exceptionHandler.doPopSetjmp(ir, ir.getBranchTarget());
		doJsr(ir.getTarget(0), bpc+5, ir.getBranchTarget().getBpc());
		break;
	    case OpcodeConst.opc_ret:
		exceptionHandler.doPopSetjmp(ir, null);
		doRet(ir.getOperand(0));
		break;

	    case OpcodeConst.opc_tableswitch:
		doTableswitch(ir.getOperand(0), bytecode, bpc);
		break;

	    case OpcodeConst.opc_lookupswitch:
		doLookupswitch(ir.getOperand(0), bytecode, bpc);			
		break;

	    case OpcodeConst.opc_ireturn:
	    case OpcodeConst.opc_lreturn:
	    case OpcodeConst.opc_freturn:
	    case OpcodeConst.opc_dreturn:
	    case OpcodeConst.opc_areturn:
	    case OpcodeConst.opc_return:
		exceptionHandler.doPopSetjmp(ir, null);
		if(MethodInliner.inline_depth==0) {
		    if (shortOpcode == OpcodeConst.opc_areturn) {
			out.Print("retObj = " + ir.getOperand(0) + ";");
		    }
		    out.Print("goto EXIT;");
		} else {
		    out.Print("goto EXIT_" + name + ";");
		}
		break;

	    case OpcodeConst.opc_newarray:
		doNewArray(ir.getTarget(0), bytecode.getOperand(0), ir.getOperand(0));
		break;

	    case OpcodeConst.opc_arraylength:
		doArrayLength(ir.getTarget(0), ir.getOperand(0), ir.getArrayLength());
		break;

	    case OpcodeConst.opc_athrow:
		exceptionHandler.doAthrow(currentIR, ir.getOperand(0));
		break;

	    case OpcodeConst.opc_monitorenter:
		doMonitor(ir.getOperand(0), "Lock");
		break;

	    case OpcodeConst.opc_monitorexit:
		doMonitor(ir.getOperand(0), "Unlock");
		break;

//	    case OpcodeConst.opc_aldc_ind_quick:
//	    case OpcodeConst.opc_aldc_ind_w_quick:
//		doAldc_ind(ir.getTarget(0), bytecode.getOperand(0));
//		break;

	    case OpcodeConst.opc_aldc_quick:
	    case OpcodeConst.opc_aldc_w_quick:
		doAldc(ir.getTarget(0), bytecode.getOperand(0));
		break;

	    case OpcodeConst.opc_ldc_quick:
	    case OpcodeConst.opc_ldc_w_quick:
		doLdc(ir.getTarget(0), ir.getOperand(0));
		break;

	    case OpcodeConst.opc_ldc2_w_quick:
		doLdc2(ir.getTarget(0), ir.getOperand(0), ir.getOperand(1));
		break;

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
		{ // null check here
		    AOTCInvoke invoke = new AOTCInvoke(ir, classinfo, methodinfo, name, out,
						       exceptionHandler, cfg.getCFGInfo(), liveVariables);
		    invoke.doInvoke();   
		}
		break;

	    case OpcodeConst.opc_checkcast_quick:
		doCheckcast(ir.getOperand(0), bytecode.getOperand(0));
		break;

	    case OpcodeConst.opc_instanceof_quick:
		doInstanceof(ir.getOperand(0), bytecode.getOperand(0), ir.getTarget(0));
		break;

	    case OpcodeConst.opc_agetstatic_quick:
	    case OpcodeConst.opc_getstatic_quick:
		doGetstatic(ir.getTarget(0), bytecode.getOperand(0), false);
		break;
	    case OpcodeConst.opc_agetstatic_checkinit_quick:
	    case OpcodeConst.opc_getstatic_checkinit_quick:
		doGetstatic(ir.getTarget(0), bytecode.getOperand(0), true);
		break;
	    case OpcodeConst.opc_getstatic2_quick:
		doGetstatic2(ir.getTarget(0), bytecode.getOperand(0), false);
		break;
	    case OpcodeConst.opc_getstatic2_checkinit_quick:
		doGetstatic2(ir.getTarget(0), bytecode.getOperand(0), true);
		break;

	    case OpcodeConst.opc_aputstatic_quick:
	    case OpcodeConst.opc_putstatic_quick:
		doPutstatic(ir.getOperand(0), bytecode.getOperand(0), false);
		break;
	    case OpcodeConst.opc_aputstatic_checkinit_quick:
	    case OpcodeConst.opc_putstatic_checkinit_quick:
		doPutstatic(ir.getOperand(0), bytecode.getOperand(0), true);
		break;
	    case OpcodeConst.opc_putstatic2_quick:
		doPutstatic2(ir.getOperand(0), bytecode.getOperand(0), false);
		break;
	    case OpcodeConst.opc_putstatic2_checkinit_quick:
		doPutstatic2(ir.getOperand(0), bytecode.getOperand(0), true);
		break;

	    case OpcodeConst.opc_getfield_quick:
	    case OpcodeConst.opc_getfield2_quick:
		doGetfield(ir.getTarget(0), ir.getOperand(0), bytecode.getOperand(0));
		break;
	    case OpcodeConst.opc_agetfield_quick:
		doAGetfield(ir.getTarget(0), ir.getOperand(0), bytecode.getOperand(0));
		break;
	    case OpcodeConst.opc_getfield_quick_w:
		doGetfieldw(ir.getTarget(0), ir.getOperand(0), bytecode.getOperand(0));
		break;

	    case OpcodeConst.opc_putfield_quick:
	    case OpcodeConst.opc_putfield2_quick:
		doPutfield(ir.getOperand(0), ir.getOperand(1), bytecode.getOperand(0));
		break;
	    case OpcodeConst.opc_aputfield_quick:
		doAPutfield(ir.getOperand(0), ir.getOperand(1), bytecode.getOperand(0));
		break;
	    case OpcodeConst.opc_putfield_quick_w:
		doPutfieldw(ir.getOperand(0), ir.getOperand(1), bytecode.getOperand(0));
		break;

	    case OpcodeConst.opc_new_quick:
		doNew(ir.getTarget(0), bytecode.getOperand(0), false);
		break;

	    case OpcodeConst.opc_new_checkinit_quick:
		doNew(ir.getTarget(0), bytecode.getOperand(0), true);
		break;

	    case OpcodeConst.opc_anewarray_quick:
		doAnewarray(ir.getTarget(0), bytecode.getOperand(0), ir.getOperand(1));
		break;
	    case OpcodeConst.opc_multianewarray_quick:
		doMultianewarray(ir.getTarget(0), bytecode.getOperand(0), bytecode.getOperand(1), ir.getOperandList());
		break;

	    default:
		new aotc.share.Assert(false, "What is it?? : " + bytecode.getString());
	}
	IR fallthru = ir.getFallthruSucc();
	if(fallthru != null) {
		exceptionHandler.doPopSetjmp(ir, fallthru);
	}else if(ir.getSuccNum() == 0) {
		exceptionHandler.doPopSetjmp(ir, null);
	}
    }


    private String getGotoLabel(int bpc) {
	return "LL_" + name + "_" + bpc;
    }
    
    private void doGoto(int bpc) {
        if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
            int currentBpc = currentIR.getOriginalBpc();
            int tryBpc = ((IR)cfg.getIRAtBpc(bpc)).getOriginalBpc();
            ExceptionEntry[] exceptionTable = methodinfo.exceptionTable;
            if(currentIR.getBranchTarget() != null) {
                exceptionHandler.doPopSetjmp(currentIR, currentIR.getBranchTarget());
                exceptionHandler.doPushSetjmp(currentIR, currentIR.getBranchTarget());
            }
            for(int i = 0 ; i < exceptionTable.length ; i++) {
                if(exceptionTable[i].catchType == null) continue;
                int startBpc = exceptionTable[i].startPC;
                int endBpc = exceptionTable[i].endPC;
                if(tryBpc == startBpc && currentBpc >= startBpc && currentBpc < endBpc) {
                    out.Print("goto " + getGotoLabel(bpc) + "_" + i + ";");
		    return;
                }
            }
        }
	out.Print("goto " + getGotoLabel(bpc) + ";");
    }

    private void doCopy(JavaVariable target, JavaVariable operand) {
	// Target and operand are same.
	if(target.compareTo(operand)==0) return;
	
	if(target.getType()==TypeInfo.TYPE_REFERENCE) {
	    out.Print(target + " = " + operand + ";");

            //out.Print(target + "_ICell->ref_DONT_ACCESS_DIRECTLY = " + operand + ";"); <- deleted
            // FOR_ICELL_ELIMINATION
            if (currentIR.hasAttribute(IRAttribute.NEED_ICELL) /*|| target instanceof LocalVariable*/) {
                if (operand.toString() == "NULL") {  // aconstnull
                    out.Print(target + "_ICell->ref_DONT_ACCESS_DIRECTLY = " + operand + ";");
                } else {
                    out.Print(target + "_ICell->ref_DONT_ACCESS_DIRECTLY = " + target + ";");
                }
            }
	} else {
	    out.Print(target + " = " + operand + ";");
	}
    }

    private final String arrayTypeName[] = {
	"Boolean", "Byte", "Char", "Short",
	"Int", "Long", "Float", "Double", "Ref"
    };

    private void doArrayLoad(JavaVariable target, JavaVariable array, JavaVariable index, int arrayType) {

	out.Print("{");

	// type definition
	String typeName = "CVMArrayOf" + arrayTypeName[arrayType] + "*";
	//out.Print("CVMUint32 len;");

	exceptionHandler.checkNullPointerException(currentIR, array);
	exceptionHandler.checkArrayIndexOutOfBoundsException(currentIR, index, array, typeName);
	
	// load
	out.Print("");
	if(target.getType()==TypeInfo.TYPE_REFERENCE) {
	    out.Print("{");
	    out.Print("CVMObject* ref;");
	    out.Print("CVMD_arrayReadRef((" + typeName + ")" + array + ", " + index + ", ref);");
	    //out.Print(target + " = ref;");
	    //out.Print(target + "_ICell->ref_DONT_ACCESS_DIRECTLY = ref;");
            // FOR_ICELL_ELIMINATION
            doCopy(target, new Constant(TypeInfo.TYPE_REFERENCE, "ref"));
	    out.Print("}");
	} else {
	    out.Print("CVMD_arrayRead" + arrayTypeName[arrayType] + "((" + typeName + ")" + array + ", " + index + ", " + target + ");");
	}
	out.Print("}");
    }

    private void doArrayStore(JavaVariable array, JavaVariable index, JavaVariable operand, int arrayType) {

	out.Print("{");

	// type definition
	String typeName = "CVMArrayOf" + arrayTypeName[arrayType] + "*";
	//out.Print("CVMUint32 len;");

	exceptionHandler.checkNullPointerException(currentIR, array);
	exceptionHandler.checkArrayIndexOutOfBoundsException(currentIR, index, array, typeName);
	exceptionHandler.checkArrayStoreException(currentIR, arrayType, array, operand);

	// store
	out.Print("");
	if(arrayType==TypeInfo.TYPE_REFERENCE) {
	    out.Print("CVMD_arrayWriteRef((" + typeName + ")" + array + ", " + index + ", " + operand + ");");
	} else {
	    out.Print("CVMD_arrayWrite" + arrayTypeName[arrayType] + "((" + typeName + ")" + array + ", " + index + ", " + operand + ");");
	}
	out.Print("}");
    }

    private void doBinaryOperation(
	    JavaVariable target,
	    JavaVariable operand1,
	    JavaVariable operand2,
	    String operator) {

	out.Print(target + " = " + operand1 + operator + operand2 + ";");
    }
    private void doUnaryOperation(
	    JavaVariable target,
	    JavaVariable operand,
	    String operator) {

	out.Print(target + " = " + operator + operand + ";");
    }

    private void doCast(JavaVariable target, JavaVariable operand, String type) {
	out.Print(target + " = (" + type + ")" + operand + ";");
    }

    private void doJsr(JavaVariable buf, int nextbpc, int targetBpc) {
        // FOR_ICELL_ELIMINATION
        doCopy(buf, new Constant(TypeInfo.TYPE_REFERENCE, "(CVMObject*)(&&" + getGotoLabel(nextbpc) + ")"));
	doGoto(targetBpc);
    }
    private void doRet(JavaVariable buf) {
	out.Print("goto *(void*)" + buf + ";");
    }

    private void doTableswitch(JavaVariable variable, Bytecode bytecode, int bpc) {
	// Operand(0) is offset from current Bpc.
	int defaultBpc = bytecode.getOperand(0) + bpc;
	int low = bytecode.getOperand(1); // low
	int high = bytecode.getOperand(2); // high
	out.Print("switch (" + variable + ") {");
	for (int i = 0; i < high - low + 1; i++) {
	    // Make each case statement.
	    out.Print("case " + Integer.toString(low + i) + ":");
	    doGoto(bytecode.getOperand(i + 3) + bpc);
	    out.Print("break;");
	}
	// Make default statement. 
	out.Print("default:");
	doGoto(defaultBpc);
	out.Print("}");
    }

    private void doLookupswitch(JavaVariable variable, Bytecode bytecode, int bpc) {
	// Operand(0) is offset from current Bpc.
	int defaultBpc = bytecode.getOperand(0) + bpc;
	out.Print("switch (" + variable + ") {");
	for (int i = 0; i < bytecode.getOperand(1) * 2; i += 2) {
	    // Make each case statement.
	    out.Print("case " + Integer.toString(bytecode.getOperand(i + 2)) + ":");
	    doGoto(bytecode.getOperand(i + 3) + bpc);
	    out.Print("break;");
	}
	// Make default statement. 
	out.Print("default:");
	doGoto(defaultBpc);
	out.Print("}");
    }

    private void doNewArray(JavaVariable target, int arrayType, JavaVariable length) {

        String arrayClassBlock;
        String arrayTypeSize;
	// get array class block informatiopn
        switch (arrayType) {
            case 2:
                arrayClassBlock = "CVMsystemArrayClassOf(Object)"; /* A generic objects array */
                arrayTypeSize = "4";
                break;
            case 4:
                arrayClassBlock = "CVMsystemArrayClassOf(Boolean)"; /* CVM_T_BOOLEAN */
                arrayTypeSize = "1";
                break;
            case 5:
                arrayClassBlock = "CVMsystemArrayClassOf(Char)";   /* CVM_T_CHAR */
                arrayTypeSize = "2";
                break;
            case 6:
                arrayClassBlock = "CVMsystemArrayClassOf(Float)";  /* CVM_T_FLOAT */
                arrayTypeSize = "4";
                break;
            case 7:
                arrayClassBlock = "CVMsystemArrayClassOf(Double)"; /* CVM_T_DOUBLE */
                arrayTypeSize = "8";
                break;
            case 8:
                arrayClassBlock = "CVMsystemArrayClassOf(Byte)";   /* CVM_T_BYTE */
                arrayTypeSize = "1";
                break;
            case 9:
                arrayClassBlock = "CVMsystemArrayClassOf(Short)";  /* CVM_T_SHORT */
                arrayTypeSize = "2";
                break;
            case 10:
                arrayClassBlock = "CVMsystemArrayClassOf(Int)";    /* CVM_T_INT */
                arrayTypeSize = "4";
                break;
            case 11:
                arrayClassBlock = "CVMsystemArrayClassOf(Long)";   /* CVM_T_LONG */
                arrayTypeSize = "8";
                break;
            default:
                arrayClassBlock = new String();
                arrayTypeSize = null;
                new aotc.share.Assert(false, "Can't determine array type");
        }

	out.Print("{");
	out.Print("CVMClassBlock *arrCb;");
        out.Print("CVMJavaInt arrayObjectSize;");
	exceptionHandler.checkNegativeArraySizeException(currentIR, length);
	out.Print("");
        out.Print("arrCb = (CVMClassBlock*) " + arrayClassBlock + ";");
        out.Print("arrayObjectSize = CVMalignWordUp(CVMoffsetof(CVMArrayOfAnyType, elems) + " 
                        + arrayTypeSize + " * " + length + ");");
	out.Print("CVMassert(arrCb != 0);");
        out.Print("CVMassert(CVMbasicTypeSizes[" + arrayType +"] != 0);");
	out.Print("");
        // derived from CVMgcAllocNewArray
        //doCopy (target, new Constant(TypeInfo.TYPE_REFERENCE,
        //                        "(CVMObject*)CVMgcAllocNewArray(ee, " + arrayType + ", arrCb, " + length + ")"));
        doCopy (target, new Constant(TypeInfo.TYPE_REFERENCE,
                      "(CVMObject*)CVMgcAllocNewArrayWithInstanceSize(ee, arrayObjectSize, arrCb, " + length + ");"));
	out.Print("");
        
        // If the reference copied into ICELL, nothing wrong with GCRestore
        // If not, may the reference is overwritted with a valid (previos) Value.
        // So We need to backup & restore valid ref pointer.
        //if (currentIR.hasAttribute(IRAttribute.NEED_ICELL)) {	
	//    doGCRestore(bpc);
	    //doGCLabel(bpc);
        //} else {
            doGCRestorePreserve(methodinfo, bpc, target, liveVariables, out, true);
            //doGCLabelPreserve(bpc, target);
        //}

	exceptionHandler.checkOutOfMemoryError(currentIR, target);

	out.Print("}");
    }

    private void doArrayLength(JavaVariable target, JavaVariable array, String length) {
	exceptionHandler.checkNullPointerException(currentIR, array);

	// get length
	if(length.equals("ref") ) {
	    out.Print(target + " = CVMD_arrayGetLength((CVMArrayOfAnyType*)" + array + ");");
	}else {
	    //out.Print("printf(\"Array Length is " + length + "   " + offset +" : " + currentIR.getBpc() + " \\n\");");
	    out.Print(target + " = " + length +";");
	}
    }

    private void doMonitor(JavaVariable obj, String str) {

	exceptionHandler.checkNullPointerException(currentIR, obj);
	
	out.Print("if (!CVMfastTry" + str + "(ee, " + obj + ")) {");
	out.Print("CVMobject" + str + "(ee, " + obj + "_ICell);");
	doGCRestore(methodinfo, bpc, liveVariables, out, true);
	out.Print("}");
    }
    
    private void doAldc(JavaVariable target, int index) {
	//out.Print(target + " = CVMcpGetStringICell(cp," + index + ").r.ref_DONT_ACCESS_DIRECTLY;");
	//out.Print(target + "_ICell->ref_DONT_ACCESS_DIRECTLY = " + target + ";");
        // FOR_ICELL_ELIMINATION
        //doCopy (target, new Constant(TypeInfo.TYPE_REFERENCE, "CVMcpGetVal32(cp," + index + ").r.ref_DONT_ACCESS_DIRECTLY"));
        StringConstant sc = (StringConstant) classinfo.getConstantPool().getConstants()[index]; 
        //System.out.println(name + "doAldc: " + sc.toString());
        String stringRef = "(CVMObject*)&(CVM_ROMStrings[" + sc.unicodeIndex + "])";
        //System.out.println("StringConstant:" + sc + "ref: " + stringRef);
        out.Print("{");
        //out.Print("extern struct java_lang_String CVM_ROMStrings[];");
        out.Print("extern struct java_lang_String * const CVM_ROMStrings;");
        doCopy (target, new Constant(TypeInfo.TYPE_REFERENCE, stringRef));
        out.Print("}");
        //out.Print("CVMconsolePrintf(\"CLAS %C\\n\", " + target + "->hdr.clas);");
        //out.Print("CVMconsolePrintf(\"%O\\n\", " + target + ");");
        //out.Print("fprintf(stderr, \"DONE\\n\");");
    }
    private void doLdc(JavaVariable target, JavaVariable value) {
	if (target.getType() == TypeInfo.TYPE_INT) {
	    doCopy(target, value);
	} else {
	    out.Print("{");
	    out.Print("CVMJavaVal32 tempVal32;");
	    out.Print("tempVal32.i = " + value + ";");
	    out.Print(target + " = tempVal32.f;");
	    out.Print("}");
	}
    }
    private void doLdc2(JavaVariable target, JavaVariable value1, JavaVariable value2) {
	out.Print("{");
	out.Print("CVMJavaVal64 tempVal64;");
	out.Print("tempVal64.v[0] = " + value1 + ";");
	out.Print("tempVal64.v[1] = " + value2 + ";");
	if (target.getType() == TypeInfo.TYPE_LONG) {
	    out.Print(target + " = tempVal64.l;");
	} else {
	    out.Print(target + " = tempVal64.d;");
	}
	out.Print("}");
    }

    private void doCheckcast(JavaVariable obj, int index) {
	out.Print("{");
        //out.Print("CVMClassBlock* " + CASTCB + " = CVMcpGetCb(cp, " + index + ");");
        ClassInfo ci = ((ClassConstant)classinfo.getConstantPool().getConstants()[index]).find();
	exceptionHandler.checkClassCastException(currentIR, obj, ci);
	out.Print("}");
    }
    private void doInstanceof(JavaVariable obj, int index, JavaVariable ret) {
	out.Print("{");
        //if (!preload) {
        new aotc.share.Assert (preload == false, "can't be preloaded");
	    ClassInfo ci = ((ClassConstant)classinfo.getConstantPool().getConstants()[index]).find();
            String ciNativeName = ((CVMClass)ci.vmClass).getNativeName()+ "_Classblock";
            out.Print("extern const CVMClassBlock "+ciNativeName+";");
	    out.Print("CVMClassBlock* castCB = (CVMClassBlock*)(&"+ciNativeName+");");
            //out.Print("CVMClassBlock* " + CASTCB + " = CVMcpGetCb(cp, " + index + ");");
        //}
        CVMClass vmClass = (CVMClass) ci.vmClass;
        String castClass = vmClass.getNativeName();

        out.Print("if (" + obj + " == NULL) {");
        out.Print(ret + " = 0;");
        out.Print("} else {");
	//out.Print("AOTCIsInstanceOf(ee, " + obj + ", castCB, " + ret + ");");
        //System.out.println("checkcast to " + castClass);
        out.Print("CVMClassBlock* objCB = (CVMClassBlock*)CVMobjectGetClass(" + obj + ");");
        if (castClass.equals("java_lang_Object")) {
            //System.out.println("------------------------------");
            //System.out.println("checkcast to java_lang_Object");
            //System.out.println("------------------------------");
            out.Print("result = CVM_TRUE;");
        } else if (castClass.equals("java_lang_Cloneable") || castClass.equals("java_io_Serializable")) {
            //System.out.println("------------------------------------------------");
            //System.out.println("checkcast to java_lang_Clanable or java_io_Seri~");
            //System.out.println("------------------------------------------------");
            out.Print("AOTCImplements(ee, objCB, castCB, " + ret + ");");
            out.Print("if (" + ret + " == CVM_FALSE) {");
            out.Print("// All array class implements Clonable and Serializable");
            out.Print(ret + " = CVMisArrayClass(objCB);");
            out.Print("}");
        } else if (vmClass.isInterface()) {
            //System.out.println("----------------------");
            //System.out.println("checkcast to Interface");
            //System.out.println("----------------------");
            out.Print("AOTCImplements(ee, objCB, castCB, " + ret + ");");

        } else if (vmClass.isArrayClass()) {
            //System.out.println("checkcast to ArrayClass");
            out.Print("AOTCIsArrayOf(ee, objCB, castCB, " + ret + ");");
            
        } else { // general class
            //System.out.println("checkcast to NormalClass");
            out.Print("AOTCIsSubclassOf(ee, objCB, castCB, " + ret + ");");
        }
	out.Print("}");
	out.Print("}");
    }

    private void doGetstatic(JavaVariable target, int index, boolean checkinit) {
	out.Print("{");
	out.Print("CVMJavaVal32 tempVal32;");

        ClassInfo ci = ((FieldConstant)classinfo.getConstantPool().getConstants()[index]).find().parent;
        String ciNativeName = ((CVMClass)(ci.vmClass)).getNativeName()+"_Classblock";
        out.Print("extern const CVMClassBlock "+ciNativeName+";");
        if (checkinit && !preload && !currentIR.hasAttribute(IRAttribute.REDUNDANT_CLASSINIT)) {
            if(!ci.equals(this.classinfo)) {
	        doCheckInit(ciNativeName, true, target);
            }
        }
        int offset = +((FieldConstant)classinfo.getConstantPool().getConstants()[index]).find().instanceOffset;
        out.Print("#ifdef CVM_LVM");
        out.Print("tempVal32 = ((CVMcbLVMClassIndex(&"+ciNativeName+") > 0)?CVMLVMeeStatics(ee)["+offset+"] : CVMcbStatics(&"+ciNativeName+")["+offset+"]);");
        out.Print("#else");
	out.Print("tempVal32 = CVMcbStatics(&"+ciNativeName+")["+offset+"];");
        out.Print("#endif");
	//out.Print("tempVal32 = CVMfbStaticField(ee, " + FB + ");");
	String suffix;
	if (target.getType() == TypeInfo.TYPE_FLOAT) {
	    suffix = ".f";
	} else {
	    suffix = ".i";
	}
	if(target.getType() == TypeInfo.TYPE_REFERENCE) {
	    //out.Print(target + " = tempVal32.r.ref_DONT_ACCESS_DIRECTLY;");
	    //out.Print(target + "_ICell->ref_DONT_ACCESS_DIRECTLY = " + target + ";");
            // FOR_ICELL_ELIMINATION
            doCopy(target, new Constant(TypeInfo.TYPE_REFERENCE, "tempVal32.r.ref_DONT_ACCESS_DIRECTLY"));
	} else {
	    out.Print(target + " = tempVal32" + suffix + ";");
	}
	out.Print("}");
    }

    private void doGetstatic2(JavaVariable target, int index, boolean checkinit) {
	out.Print("{");
	out.Print("CVMJavaVal64 tempVal64;");

        ClassInfo ci = ((FieldConstant)classinfo.getConstantPool().getConstants()[index]).find().parent;
        String ciNativeName = ((CVMClass)(ci.vmClass)).getNativeName()+"_Classblock";
        out.Print("extern const CVMClassBlock "+ciNativeName+";");
        if (checkinit && !preload && !currentIR.hasAttribute(IRAttribute.REDUNDANT_CLASSINIT)) {
	    //out.Print("CVMFieldBlock *" + FB + " = CVMcpGetFb(cp, " + index + ");");
            if(!ci.equals(this.classinfo)) {
	        doCheckInit(ciNativeName, true, target);
            }
        }
        int offset = +((FieldConstant)classinfo.getConstantPool().getConstants()[index]).find().instanceOffset;
	//out.Print("tempVal64.v[0] = CVMfbStaticField(ee, " + FB + ").raw;");
	//out.Print("tempVal64.v[1] = (*(&CVMfbStaticField(ee, " + FB + ")+1)).raw;");
        out.Print("#ifdef CVM_LVM");
        out.Print("tempVal64.v[0] = ((CVMcbLVMClassIndex(&"+ciNativeName+") > 0)?CVMLVMeeStatics(ee)["+offset+"] : CVMcbStatics(&"+ciNativeName+")["+offset+"]).raw;");
        out.Print("tempVal64.v[1] = ((CVMcbLVMClassIndex(&"+ciNativeName+") > 0)?CVMLVMeeStatics(ee)["+(offset+1)+"] : CVMcbStatics(&"+ciNativeName+")["+(offset+1)+"]).raw;");
        out.Print("#else");
	out.Print("tempVal64.v[0] = CVMcbStatics(&"+ciNativeName+")["+offset+"].raw;");
	out.Print("tempVal64.v[1] = CVMcbStatics(&"+ciNativeName+")["+(offset+1)+"].raw;");
        out.Print("#endif");


	String suffix = null;
	if (target.getType() == TypeInfo.TYPE_LONG) {
	    suffix = ".l";
	} else if (target.getType() == TypeInfo.TYPE_DOUBLE) {
	    suffix = ".d";
	}
	out.Print(target + " = tempVal64" + suffix + ";");
	out.Print("}");
    }

    private void doPutstatic(JavaVariable target, int index, boolean checkinit) {
	out.Print("{");
	out.Print("CVMJavaVal32 tempVal32;");

        ClassInfo ci = ((FieldConstant)classinfo.getConstantPool().getConstants()[index]).find().parent;
        String ciNativeName = ((CVMClass)(ci.vmClass)).getNativeName()+"_Classblock";
        out.Print("extern const CVMClassBlock "+ciNativeName+";");
        if (checkinit && !preload && !currentIR.hasAttribute(IRAttribute.REDUNDANT_CLASSINIT)) {
	    //out.Print("CVMFieldBlock *" + FB + " = CVMcpGetFb(cp, " + index + ");");
            if(!ci.equals(this.classinfo)) {
	        doCheckInit(ciNativeName, true, null);
            }
        }
	String suffix;
	if (target.getType() == TypeInfo.TYPE_FLOAT) {
	    suffix = ".f";
	} else {
	    suffix = ".i";
	}
	if(target.getType() == TypeInfo.TYPE_REFERENCE) {
	    out.Print("tempVal32.raw = (CVMUint32)(" + target + ");");
	} else {
	    out.Print("tempVal32" + suffix + " = " + target + ";");
	}

        int offset = +((FieldConstant)classinfo.getConstantPool().getConstants()[index]).find().instanceOffset;
        out.Print("#ifdef CVM_LVM");
        out.Print("((CVMcbLVMClassIndex(&"+ciNativeName+") > 0)?CVMLVMeeStatics(ee)["+offset+"] : CVMcbStatics(&"+ciNativeName+")["+offset+"]) = tempVal32;");
        out.Print("#else");
        out.Print("CVMcbStatics(&"+ciNativeName+")["+offset+"] = tempVal32;");
        out.Print("#endif");

	//out.Print("CVMfbStaticField(ee, " + FB + ") = tempVal32;");
	out.Print("}");
    }

    private void doPutstatic2(JavaVariable target, int index, boolean checkinit) {
	out.Print("{");
	out.Print("CVMJavaVal64 tempVal64;");

        ClassInfo ci = ((FieldConstant)classinfo.getConstantPool().getConstants()[index]).find().parent;
        String ciNativeName = ((CVMClass)(ci.vmClass)).getNativeName()+"_Classblock";
        out.Print("extern const CVMClassBlock "+ciNativeName+";");
        if (checkinit && !preload && !currentIR.hasAttribute(IRAttribute.REDUNDANT_CLASSINIT)) {
	    //out.Print("CVMFieldBlock *" + FB + " = CVMcpGetFb(cp, " + index + ");");
            if(!ci.equals(this.classinfo)) {
	        doCheckInit(ciNativeName, true, null);
            }
        }
	String suffix = null;
	if (target.getType() == TypeInfo.TYPE_LONG) {
	    suffix = ".l";
	} else if (target.getType() == TypeInfo.TYPE_DOUBLE) {
	    suffix = ".d";
	}
	out.Print("tempVal64" + suffix + " = " + target.toString() + ";");

        int offset = +((FieldConstant)classinfo.getConstantPool().getConstants()[index]).find().instanceOffset;
        out.Print("#ifdef CVM_LVM");
        out.Print("((CVMcbLVMClassIndex(&"+ciNativeName+") > 0)?CVMLVMeeStatics(ee)["+offset+"] : CVMcbStatics(&"+ciNativeName+")["+offset+"]).raw = tempVal64.v[0];");
        out.Print("((CVMcbLVMClassIndex(&"+ciNativeName+") > 0)?CVMLVMeeStatics(ee)["+(offset+1)+"] : CVMcbStatics(&"+ciNativeName+")["+(offset+1)+"]).raw = tempVal64.v[1];");
        out.Print("#else");
        out.Print("CVMcbStatics(&"+ciNativeName+")["+offset+"].raw = tempVal64.v[0];");
        out.Print("CVMcbStatics(&"+ciNativeName+")["+(offset+1)+"].raw = tempVal64.v[1];");
        out.Print("#endif");

	//out.Print("CVMfbStaticField(ee, " + FB + ").raw = tempVal64.v[0];");
	//out.Print("(*(&CVMfbStaticField(ee, " + FB + ")+1)).raw = tempVal64.v[1];");
	out.Print("}");
    }
    private void doGetfield(JavaVariable target, JavaVariable addr, int index) {
	
	exceptionHandler.checkNullPointerException(currentIR, addr);
	
	String suffix = TypeInfo.toAOTCTypeSuffix(target.getType());
	out.Print("CVMD_fieldRead" + suffix + "(" + addr + ", " + index + ", " + target + ");");
    }
    private void doAGetfield(JavaVariable target, JavaVariable addr, int index) {
    
	exceptionHandler.checkNullPointerException(currentIR, addr);
	
	out.Print("CVMD_fieldReadRef(" + addr + ", " + index + ", " + target + ");");
        // FOR_ICELL_ELIMINATION
	if (currentIR.hasAttribute(IRAttribute.NEED_ICELL)) {
            out.Print(target + "_ICell->ref_DONT_ACCESS_DIRECTLY = " + target + ";");
        }
    }
    private void doGetfieldw(JavaVariable target, JavaVariable addr, int index) {

	exceptionHandler.checkNullPointerException(currentIR, addr);
	
	String suffix = TypeInfo.toAOTCTypeSuffix(target.getType());

	//out.Print("CVMD_fieldRead" + suffix + "(" + addr + ", " + ((FieldConstant)classinfo.getConstantPool().getConstants()[index]).find().instanceOffset + ", " + target + ");");
    int offset = ((FieldConstant)classinfo.getConstantPool().getConstants()[index]).find().instanceOffset + CVMConst.ObjHeaderWords;
	out.Print("CVMD_fieldRead" + suffix + "(" + addr + ", " + offset + ", " + target + ");");
	if(target.getType()==TypeInfo.TYPE_REFERENCE) {
            if (currentIR.hasAttribute(IRAttribute.NEED_ICELL)) {
        	out.Print(target + "_ICell->ref_DONT_ACCESS_DIRECTLY = " + target + ";");
            }
	}
    }

    private void doPutfield(JavaVariable addr, JavaVariable value, int index) {
	exceptionHandler.checkNullPointerException(currentIR, addr);

	String suffix = TypeInfo.toAOTCTypeSuffix(value.getType());
	out.Print("CVMD_fieldWrite" + suffix + "(" + addr + ", " + index + ", " + value + ");");
    }
    private void doAPutfield(JavaVariable addr, JavaVariable value, int index) {
	exceptionHandler.checkNullPointerException(currentIR, addr);
	out.Print("CVMD_fieldWriteRef(" + addr + ", " + index + ", " + value + ");");
    }
    private void doPutfieldw(JavaVariable addr, JavaVariable value, int index) {
	exceptionHandler.checkNullPointerException(currentIR, addr);

	String suffix = TypeInfo.toAOTCTypeSuffix(value.getType());
	//out.Print("CVMD_fieldWrite" + suffix + "(" + addr + ", " + ((FieldConstant)classinfo.getConstantPool().getConstants()[index]).find().instanceOffset + ", " + value + ");");
    int offset = ((FieldConstant)classinfo.getConstantPool().getConstants()[index]).find().instanceOffset + CVMConst.ObjHeaderWords;
	out.Print("CVMD_fieldWrite" + suffix + "(" + addr + ", " + offset +  ", " + value + ");");
    }

    private void doNew(JavaVariable addr, int index, boolean needCheckInit)  {
	boolean check = exceptionHandler.checkInstantiationException(currentIR, index);

	if( !check ) {
            String ciNativeName = (((CVMClass)((ClassConstant)classinfo.getConstantPool().getConstants()[index]).find().vmClass).getNativeName())+"_Classblock";

	    out.Print("{");
	    //out.Print("CVMClassBlock* newCb = CVMcpGetCb(cp, " + index + ");");
            out.Print("extern const CVMClassBlock "+ciNativeName+";");
            out.Print("CVMClassBlock* newCb = (CVMClassBlock*)(&" + ciNativeName + ");");
            if (needCheckInit && currentIR.hasAttribute(IRAttribute.PRELOADED) == false
                    && currentIR.hasAttribute(IRAttribute.REDUNDANT_CLASSINIT) == false) {
	        doCheckInit(ciNativeName, false, null);
            }
	    
            //out.Print(addr + " = CVMgcAllocNewInstance(ee, newCb);");
            // FOR_ICELL_ELIMINATION
            //if (currentIR.hasAttribute(IRAttribute.NEED_ICELL)) {
    	    //    out.Print(addr + "_ICell->ref_DONT_ACCESS_DIRECTLY = " + addr + ";");
            //}
            doCopy(addr, new Constant(TypeInfo.TYPE_REFERENCE, "CVMgcAllocNewInstance(ee, newCb)"));
	    
            // If the reference copied into ICELL, nothing wrong with GCRestore
            // If not, may the reference is overwritted with a valid (previos) Value.
            // So We need to backup & restore valid ref pointer.
            //if (currentIR.hasAttribute(IRAttribute.NEED_ICELL)) {
            //    doGCRestore(bpc);
                //doGCLabel(bpc);
            //} else {
            doGCRestorePreserve(methodinfo, bpc, addr, liveVariables, out, true);
                //doGCLabelPreserve(bpc, addr);
            //}
	    /*    
	    out.Print("if(!" + addr + ")");
	    if(handlerPC == -1) {
		out.Print("{");
		out.Print("exception_type = EXCEPTION_OUT_OF_MEMORY;");
		exception_exit++;
		out.Print("goto EXCEPTION_EXIT;");
		out.Print("}");
	    }else {
		doThrowException("java/lang/OutOfMemoryError");
	    }
	    */
	    exceptionHandler.checkOutOfMemoryError(currentIR, addr);

	    out.Print("}");
	}
    }

    private void doAnewarray(JavaVariable ret, int index, JavaVariable count) {
	/*
	out.Print("if(" + count + " < 0)");
	if(handlerPC == -1) {
	    out.Print("{");
	    out.Print("exception_type = EXCEPTION_NEGATIVE_ARRAY_SIZE;");
	    exception_exit++;
	    out.Print("goto EXCEPTION_EXIT;");
	    out.Print("}");
	}else {
	    doThrowException("java/lang/NegativeArraySizeException");
	}
	*/
	//if(check != null)
	exceptionHandler.checkNegativeArraySizeException(currentIR, count);
	
	out.Print("{");
	out.Print("CVMClassBlock* arrCb;");
        out.Print("CVMJavaInt arrayObjectSize;");
	out.Print("");
	//out.Print("CVMClassBlock* elemCb = CVMcpGetCb(cp, " + index + ");");
        ClassInfo ci = ((ClassConstant)classinfo.getConstantPool().getConstants()[index]).find();
        String ciNativeName = ((CVMClass)ci.vmClass).getNativeName()+ "_Classblock";
        out.Print("extern const CVMClassBlock "+ciNativeName+";");
	out.Print("CVMClassBlock* elemCb = (CVMClassBlock*)(&"+ciNativeName+");");
	out.Print("CVMD_gcSafeExec(ee, { arrCb = CVMclassGetArrayOf(ee, elemCb); } );");
	//out.Print(ret + " = (CVMObject*)CVMgcAllocNewArray(ee, CVM_T_CLASS, arrCb, " + count + ");");
	//out.Print(ret + "_ICell->ref_DONT_ACCESS_DIRECTLY = " + ret + ";");
        // FOR_ICELL_ELIMINATION
        //doCopy (ret, new Constant(TypeInfo.TYPE_REFERENCE, "(CVMObject*)CVMgcAllocNewArray(ee, CVM_T_CLASS, arrCb, " + count + ")"));
        // inlining above call
        out.Print("arrayObjectSize = CVMalignWordUp(CVMoffsetof(CVMArrayOfAnyType, elems) + 4 * " + count + ");");
	out.Print("");
        doCopy(ret, new Constant(TypeInfo.TYPE_REFERENCE, "(CVMObject*)CVMgcAllocNewArrayWithInstanceSize(ee, arrayObjectSize, arrCb, " + count + ");"));
	out.Print("");

        // If the reference copied into ICELL, nothing wrong with GCRestore
        // If not, may the reference is overwritted with a valid (previos) Value.
        // So We need to backup & restore valid ref pointer.
        //if (currentIR.hasAttribute(IRAttribute.NEED_ICELL)) {
	//    doGCRestore(bpc);
	    //doGCLabel(bpc);
        //} else {
            doGCRestorePreserve(methodinfo, bpc, ret, liveVariables, out, true);
            //doGCLabelPreserve(bpc, ret);
        //}
	/*
	out.Print("if(!" + ret + ")");
	if(handlerPC == -1) {
	    out.Print("{");
	    out.Print("exception_type = EXCEPTION_OUT_OF_MEMORY;");
	    exception_exit++;
	    out.Print("goto EXCEPTION_EXIT;");
	    out.Print("}");
	}else {
	    doThrowException("java/lang/OutOfMemoryError");
	}
	*/
	exceptionHandler.checkOutOfMemoryError(currentIR, ret);
	
	out.Print("}");
    }
    private void doMultianewarray(JavaVariable ret, int index, int dimension, Vector operands) {
	out.Print("{");
	out.Print("int count[" + dimension + "];");
	for(int i=0; i<dimension; i++) {
	    JavaVariable num = (JavaVariable)operands.elementAt(i+2);
	    out.Print("count[" + i + "] = " + num + ";");
	    /*
	    out.Print("if(" + num + " < 0)");
	    if(handlerPC == -1) {
		out.Print("{");
		out.Print("exception_type = EXCEPTION_NEGATIVE_ARRAY_SIZE;");
		exception_exit++;
		out.Print("goto EXCEPTION_EXIT;");
		out.Print("}");
	    }else {
		doThrowException("java/lang/NegativeArraySizeException");
	    }
	    */
	    //if( check != null) 
	    exceptionHandler.checkNegativeArraySizeException(currentIR, num);
	}
	out.Print("{");
	//out.Print("CVMClassBlock* arrCb = CVMcpGetCb(cp, " + index + ");");
        ClassInfo ci = ((ClassConstant)classinfo.getConstantPool().getConstants()[index]).find();
        String ciNativeName = ((CVMClass)ci.vmClass).getNativeName()+ "_Classblock";
        out.Print("extern const CVMClassBlock "+ciNativeName+";");
	out.Print("CVMClassBlock* arrCb = (CVMClassBlock*)(&"+ciNativeName+");");
	
	out.Print("CVMD_gcSafeExec(ee, { CVMmultiArrayAlloc(ee, " + dimension + ", count, arrCb, " + ret + "_ICell); } );");
        
        // If the reference copied into ICELL, nothing wrong with GCRestore
        // If not, may the reference is overwritted with a valid (previos) Value.
        // So We need to backup & restore valid ref pointer.
	//if (currentIR.hasAttribute(IRAttribute.NEED_ICELL)) {
            out.Print(ret + " = " + ret + "_ICell->ref_DONT_ACCESS_DIRECTLY;");
	//    doGCRestore(bpc);
	    //doGCLabel(bpc);
        //} else {
        //    new aotc.share.Assert(false, "MULTIANEWARRAY ALWAYS NEEDS ICELL");
            doGCRestorePreserve(methodinfo, bpc, ret, liveVariables, out, true);
            //doGCLabelPreserve(bpc, ret);
        //}
	/*
	out.Print("if(!" + ret + ")");
	if(handlerPC == -1) {
	    out.Print("{");
	    out.Print("exception_type = EXCEPTION_OUT_OF_MEMORY;");
	    exception_exit++;
	    out.Print("goto EXCEPTION_EXIT;");
	    out.Print("}");
	}else {
	    doThrowException("java/lang/OutOfMemoryError");
	}
	*/
	exceptionHandler.checkOutOfMemoryError(currentIR, ret);

	out.Print("}");
	out.Print("}");
    }

    private void doCheckInit(String cb, boolean gc, JavaVariable preserved) {
        for (int i = 0 ; i < Preloader.PRELOADED_AT_VM_INITIALIZATION.length; i++) {
            if (cb.equals(Preloader.PRELOADED_AT_VM_INITIALIZATION[i])) {
                return;
            }
        }
	out.Print("if(CVMcbInitializationNeeded(&" + cb + ", ee)) {");
	out.Print("AOTCclassInit(ee, (CVMClassBlock*)(&" + cb + "));");
	if(gc) {
	    if(preserved==null)
		doGCRestore(methodinfo, bpc, liveVariables, out, true);
	    else
		doGCRestorePreserve(methodinfo, bpc, preserved, liveVariables, out, true);
	}
	out.Print("}");
    }

    public static void doGCRestore(MethodInfo methodinfo, int bpc, TreeSet liveVariables, AOTCPrint out, boolean checkIfOccurred) {
	doGCRestorePreserve(methodinfo, bpc, null, liveVariables, out, checkIfOccurred);
    }
    
    public static void doGCRestorePreserve(MethodInfo methodinfo, int bpc, JavaVariable preserved, TreeSet liveVariables, AOTCPrint out, boolean checkIfOccurred) {
	TreeSet restore = getRefsToRestore(methodinfo, preserved, liveVariables);

	if (restore.size()>0) {
            if (checkIfOccurred) {
                out.Print("if(unlikely(local_gc_and_exception_count != global_gc_and_exception_count)) {");
                out.Printf("local_gc_and_exception_count = global_gc_and_exception_count;");
            }

	    Iterator it = restore.iterator();
	    JavaVariable liveRef;
	    while (it.hasNext()) {
		liveRef = (JavaVariable)it.next();
		out.Printf(liveRef + " = " +  liveRef + "_ICell->ref_DONT_ACCESS_DIRECTLY;");
	    }
            if (checkIfOccurred) {
	        out.Print("}");
            }
	} else {
	    out.Print("// No GC restore needed!! (no live reference variable)");
	}
    }

    public static TreeSet getRefsToRestore(MethodInfo methodinfo, JavaVariable preserved, TreeSet liveVariables) {
	TreeSet restore = (TreeSet)liveVariables.clone();
	restore.remove(new Constant(TypeInfo.TYPE_REFERENCE, "NULL"));
	if (preserved!=null) {
	    restore.remove(preserved);
	}
        if ((methodinfo.access & Const.ACC_SYNCHRONIZED) != 0 && (methodinfo.access & ClassFileConst.ACC_STATIC) ==0 && MethodInliner.inline_depth==0)
        {
            restore.add(new Constant(TypeInfo.TYPE_REFERENCE, "cls"));
        }
        return restore;
    }
    
    private void doDupX1(int type, JavaVariable i1, JavaVariable i2,
			JavaVariable o1, JavaVariable o2, JavaVariable o3) {
	TemporaryVariable tt = new TemporaryVariable(type, 0);
	out.Print("{");
	if(type == TypeInfo.TYPE_REFERENCE) {
	    out.Printf("CVMObject* " + tt + ";");
	} else {
	    out.Printf(TypeInfo.typeName[type] + " " + tt + ";");
	}
	out.Print(tt + " = " + i2 + ";");
	doCopy(o1, i1);
	doCopy(o2, tt);
	doCopy(o3, o1);
	out.Print("}");
    }
    private void doDupX2(int type2, int type3, JavaVariable i1, JavaVariable i2, JavaVariable i3,
			JavaVariable o1, JavaVariable o2, JavaVariable o3, JavaVariable o4) {
	TemporaryVariable tt1 = new TemporaryVariable(type2, 0);
	TemporaryVariable tt2 = new TemporaryVariable(type3, 1);
	out.Print("{");
	if(type2 == TypeInfo.TYPE_REFERENCE) {
	    out.Printf("CVMObject* " + tt1 + ";");
	} else {
	    out.Printf(TypeInfo.typeName[type2] + " " + tt1 + ";");
	}
	if(type3 == TypeInfo.TYPE_REFERENCE) {
	    out.Printf("CVMObject* " + tt2 + ";");
	} else {
	    out.Printf(TypeInfo.typeName[type3] + " " + tt2 + ";");
	}
	out.Print(tt1 + " = " + i2 + ";");
	out.Print(tt2 + " = " + i3 + ";");
	doCopy(o1, i1);
	doCopy(o2, tt2);
	doCopy(o3, tt1);
	doCopy(o4, o1);
	out.Print("}");
    }
    private void doDup2X1(int type, JavaVariable i1, JavaVariable i2, JavaVariable i3,
			JavaVariable o1, JavaVariable o2, JavaVariable o3, JavaVariable o4, JavaVariable o5) {
	TemporaryVariable tt = new TemporaryVariable(type, 0);
	out.Print("{");
	if(type == TypeInfo.TYPE_REFERENCE) {
	    out.Printf("CVMObject* " + tt + ";");
	} else {
	    out.Printf(TypeInfo.typeName[type] + " " + tt + ";");
	}
	out.Print(tt + " = " + i3 + ";");
	doCopy(o1, i2);
	doCopy(o2, i1);
	doCopy(o3, tt);
	doCopy(o4, o1);
	doCopy(o5, o2);
	out.Print("}");
    }
    private void doDup2X2(int type3, int type4, JavaVariable i1, JavaVariable i2, JavaVariable i3,
	    JavaVariable i4, JavaVariable o1, JavaVariable o2, JavaVariable o3,
	    JavaVariable o4, JavaVariable o5, JavaVariable o6) {
	TemporaryVariable tt1 = new TemporaryVariable(type3, 0);
	TemporaryVariable tt2 = new TemporaryVariable(type4, 1);
	out.Print("{");
	if(type3 == TypeInfo.TYPE_REFERENCE) {
	    out.Printf("CVMObject* " + tt1 + ";");
	} else {
	    out.Printf(TypeInfo.typeName[type3] + " " + tt1 + ";");
	}
	if(type4 == TypeInfo.TYPE_REFERENCE) {
	    out.Printf("CVMObject* " + tt2 + ";");
	} else {
	    out.Printf(TypeInfo.typeName[type4] + " " + tt2 + ";");
	}
	out.Print(tt1 + " = " + i3 + ";");
	out.Print(tt2 + " = " + i4 + ";");
	doCopy(o1, i2);
	doCopy(o2, i1);
	doCopy(o3, tt2);
	doCopy(o4, tt1);
	doCopy(o5, o1);
	doCopy(o6, o2);
	out.Print("}");
    }
}

