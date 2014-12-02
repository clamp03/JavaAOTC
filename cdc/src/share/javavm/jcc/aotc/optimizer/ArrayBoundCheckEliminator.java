package aotc.optimizer;

import java.util.*;
import aotc.cfg.*;
import aotc.ir.*;
import aotc.share.*;
import aotc.translation.*;
import opcodeconsts.*;

public class ArrayBoundCheckEliminator {
    private CFG cfg;
    private int codeSize;
    private ExceptionHandler exceptionHandler;

    private Hashtable C_GEN[];
    private Hashtable C_GEN_BACKWARD[];
    private Hashtable C_IN[];
    private Hashtable C_OUT[];
    private Hashtable IN[];
    private Hashtable OUT[];
    private Hashtable C_RESULT[];

    public static JavaVariable inf = new Constant(-1, "inf");
    public static JavaVariable mInf = new Constant(-1, "-mInf");
    public static JavaVariable zero = new Constant(4, "0");
    public static JavaVariable one = new Constant(4, "1");
    public static JavaVariable mOne = new Constant(4, "-1");

    public static boolean use = false;
    
    private final int INCREMENT = 1;
    private final int DECREMENT = 2;
    private final int CHANGED = 3;

    public ArrayBoundCheckEliminator(CFG cfg) {
	use = true;
	this.cfg = cfg;
	codeSize = cfg.getCodeSize();
	exceptionHandler = new ExceptionHandler(cfg, null, null, cfg.getMethodInfo().exceptionTable);
    }
    public void doElimination() {
	if(!preForwardAnalysis()) {
	    return;
	}
	backwardAnalysis();
	forwardAnalysis();
	Iterator it = cfg.iterator();
	while(it.hasNext()) {
	    IR ir = (IR)it.next();
	    int bpc = ir.getBpc();
	    
	    if(ir.hasAttribute(IRAttribute.ELIMINATED)) {
		continue;
	    }
	    switch(ir.getShortOpcode()) {
		// array load
		case OpcodeConst.opc_iaload:    case OpcodeConst.opc_laload:
		case OpcodeConst.opc_faload:    case OpcodeConst.opc_daload:
		case OpcodeConst.opc_aaload:    case OpcodeConst.opc_baload:
		case OpcodeConst.opc_caload:    case OpcodeConst.opc_saload:
		// array store
		case OpcodeConst.opc_iastore:   case OpcodeConst.opc_lastore:
		case OpcodeConst.opc_fastore:   case OpcodeConst.opc_dastore:
		case OpcodeConst.opc_aastore:   case OpcodeConst.opc_bastore:
		case OpcodeConst.opc_castore:   case OpcodeConst.opc_sastore:
		    {
			JavaVariable index = ir.getOperand(1);
			IndexBound bound = (IndexBound)C_RESULT[bpc].get(index.toString());
			JavaVariable upperCheck = bound.getUB(0);
			JavaVariable lowCheck = bound.getLB(0);
			
			IndexBound upperBound = findValue(bound.getUB(0), IN[bpc]);
			IndexBound lowBound = findValue(bound.getLB(0), IN[bpc]);
			
			int upperOffset = bound.getUOffset(0);
			int lowOffset = bound.getLOffset(0);
			
			if(upperBound.getValue(0) != null) {
			    upperCheck = upperBound.getValue(0);
			    upperOffset += upperBound.getUOffset(0);
			}
			if(lowBound.getValue(0) != null) {
			    lowCheck = lowBound.getValue(0);
			    lowOffset += lowBound.getLOffset(0);
			}

			IndexBound indexBound = (IndexBound)IN[bpc].get(index.toString());
			if(indexBound != null) {
			    int upper = findMinValue(bound.getUB(0), indexBound.getUB(0), bound.getUOffset(0), indexBound.getUOffset(0),IN[bpc]);
			    int low = findMaxValue(bound.getLB(0), indexBound.getLB(0), bound.getLOffset(0), indexBound.getLOffset(0),IN[bpc]);
			    /*
			    if((upper == 2 || upper == 0) && (low == 2 || low == 0)) {
				cfg.getIRAtBpc(bpc).setAttribute(IRAttribute.ARRAYBOUNDCHECKELIMINATED);
				cfg.getIRAtBpc(bpc).addDebugInfoString("ArrayBoundCheck Eliminated" );
			    }else {
				IndexBound value = findValue(bound.getUB(0), IN[bpc]);
				if(value.getValue(0) != null && value.getValue(0).getType() != TypeInfo.TYPE_REFERENCE) {
				    cfg.getIRAtBpc(bpc).setArrayLength(value);
				    cfg.getIRAtBpc(bpc).addDebugInfoString("ArrayLength : length " + value.getUB(0)+" , Offset " + value.getUOffset(0));
				}
			    }
			     */
			    if((upper == 2 || upper == 0) && (low == 2 || low == 0)) {
				cfg.getIRAtBpc(bpc).setAttribute(IRAttribute.ARRAYBOUNDCHECKELIMINATED);
				cfg.getIRAtBpc(bpc).addDebugInfoString("ArrayBoundCheck Eliminated" );
			    }else if(upper == 2 || upper == 0) {
				cfg.getIRAtBpc(bpc).setLowBound(bound.getLB(0), bound.getLOffset(0));
			    }else if(low == 2 || low == 0) {
				cfg.getIRAtBpc(bpc).setUpperBound(bound.getUB(0), bound.getUOffset(0));
			    }else {
				cfg.getIRAtBpc(bpc).setUpperBound(bound.getUB(0), bound.getUOffset(0));
				cfg.getIRAtBpc(bpc).setLowBound(bound.getLB(0), bound.getLOffset(0));
			    }
			}
		    }
		    break;
		// array length
		case OpcodeConst.opc_arraylength:
		    {
			IndexBound indexBound = (IndexBound)IN[bpc].get(ir.getOperand(0).toString());
			if(indexBound == null || indexBound.getValue(0) == null) {
			    cfg.getIRAtBpc(bpc).setArrayLength(new IndexBound(null, null, 0, 0));
			    break;
			}
			IndexBound bound = findValue(indexBound.getUB(0), IN[bpc]);
			if(bound.getValue(0) != null && bound.getValue(0).getType() != TypeInfo.TYPE_REFERENCE) {
			    cfg.getIRAtBpc(bpc).setArrayLength(bound);
			    cfg.getIRAtBpc(bpc).addDebugInfoString("ArrayLength : length " + bound.getUB(0)+" , Offset " + bound.getUOffset(0));
			}else {
			    cfg.getIRAtBpc(bpc).setArrayLength(new IndexBound(null, null, 0, 0));
			}
			    
		    }
		    break;
		// newarray
		case OpcodeConst.opc_newarray:
		    {
			IndexBound indexBound = (IndexBound)IN[bpc].get(ir.getOperand(0).toString());
			if(indexBound != null) {
			    int low = findMaxValue(zero, indexBound.getUB(0), 0, indexBound.getUOffset(0), IN[bpc]);
			    if(low == 2 || low == 0) {
				cfg.getIRAtBpc(bpc).setAttribute(IRAttribute.ARRAYNEGATIVESIZECHECKELIMINATED);
				cfg.getIRAtBpc(bpc).addDebugInfoString("ArrayNegativeSizeCheck Eliminated");
			    }
			}
		    }
		    break;
		// anewarray
		case OpcodeConst.opc_anewarray_quick:
		    {
			JavaVariable count = ir.getOperand(1);
			IndexBound indexBound = (IndexBound)IN[bpc].get(count.toString());
			if(indexBound != null) {
			    int low = findMaxValue(zero, indexBound.getUB(0), 0, indexBound.getUOffset(0), IN[bpc]);
			    if(low == 2 || low == 0) {
				cfg.getIRAtBpc(bpc).setAttribute(IRAttribute.ARRAYNEGATIVESIZECHECKELIMINATED);
				cfg.getIRAtBpc(bpc).addDebugInfoString("ArrayNegativeSizeCheck Eliminated");
			    }
			}
		    }
		    break;
		// multianewarray
		/*
		case OpcodeConst.opc_multianewarray_quick:
		    break;
		*/
	    }
	}
    }
    public boolean preForwardAnalysis() {
	TreeSet changeNodes = new TreeSet();
	Iterator it;
	IR ir;
	boolean needAnalysis = false;
	it = cfg.iterator();
	while(it.hasNext()) {
	    ir = (IR)it.next();
	    changeNodes.add(ir);
	    switch(ir.getShortOpcode()) {
		case OpcodeConst.opc_iaload:    case OpcodeConst.opc_laload:
		case OpcodeConst.opc_faload:    case OpcodeConst.opc_daload:
		case OpcodeConst.opc_aaload:    case OpcodeConst.opc_baload:
		case OpcodeConst.opc_caload:    case OpcodeConst.opc_saload:
		case OpcodeConst.opc_iastore:   case OpcodeConst.opc_lastore:
		case OpcodeConst.opc_fastore:   case OpcodeConst.opc_dastore:
		case OpcodeConst.opc_aastore:   case OpcodeConst.opc_bastore:
		case OpcodeConst.opc_castore:   case OpcodeConst.opc_sastore:
		case OpcodeConst.opc_arraylength:
		case OpcodeConst.opc_newarray:
		case OpcodeConst.opc_anewarray_quick:
		    needAnalysis = true;
	    }
	}
	if(!needAnalysis) return needAnalysis;
	
	int bpc;
	Hashtable oldIn;
	IN = new Hashtable[codeSize];
	OUT = new Hashtable[codeSize];
	C_GEN = new Hashtable[codeSize]; 
	while(changeNodes.size() > 0) {
	    ir = (IR)changeNodes.first();
	    bpc = ir.getBpc();
	    changeNodes.remove(ir);
	    Hashtable oldOUT;
	    if(OUT[bpc] != null) {
		oldOUT = new Hashtable(OUT[bpc]);
	    }else {
		oldOUT = null;
	    }
	    solveInSet_preforward(ir, IN, OUT);
	    
	    OUT[bpc] = new Hashtable(IN[bpc]);
	    C_GEN[bpc] = new Hashtable();
	    if(ir.hasAttribute(IRAttribute.ELIMINATED)) {
		if(checkChange(oldOUT, OUT[bpc])) {
		    for(int i = 0 ; i < ir.getSuccNum() ; i++) {
			changeNodes.add(ir.getSucc(i));
		    }
		}
		continue;
	    }
	    
	    switch(ir.getShortOpcode()) {
		case OpcodeConst.opc_aconst_null:
		    removeBound(OUT[bpc], ir.getTarget(0));
		    break;
		case OpcodeConst.opc_iconst_m1: case OpcodeConst.opc_iconst_0:
		case OpcodeConst.opc_iconst_1:  case OpcodeConst.opc_iconst_2:
		case OpcodeConst.opc_iconst_3:  case OpcodeConst.opc_iconst_4:
		case OpcodeConst.opc_iconst_5:  case OpcodeConst.opc_lconst_0:
		case OpcodeConst.opc_lconst_1:  case OpcodeConst.opc_fconst_0:
		case OpcodeConst.opc_fconst_1:  case OpcodeConst.opc_fconst_2:
		case OpcodeConst.opc_dconst_0:  case OpcodeConst.opc_dconst_1:
		case OpcodeConst.opc_bipush:    case OpcodeConst.opc_sipush:
		    insertBound(OUT[bpc], ir.getTarget(0), ir.getOperand(0));
		    break;
		case OpcodeConst.opc_iload:     case OpcodeConst.opc_iload_0:
		case OpcodeConst.opc_iload_1:   case OpcodeConst.opc_iload_2:
		case OpcodeConst.opc_iload_3:   case OpcodeConst.opc_lload:
		case OpcodeConst.opc_lload_0:   case OpcodeConst.opc_lload_1:
		case OpcodeConst.opc_lload_2:   case OpcodeConst.opc_lload_3:
		case OpcodeConst.opc_fload:     case OpcodeConst.opc_fload_0:
		case OpcodeConst.opc_fload_1:   case OpcodeConst.opc_fload_2:
		case OpcodeConst.opc_fload_3:   case OpcodeConst.opc_dload:
		case OpcodeConst.opc_dload_0:   case OpcodeConst.opc_dload_1:
		case OpcodeConst.opc_dload_2:   case OpcodeConst.opc_dload_3:
		case OpcodeConst.opc_aload:     case OpcodeConst.opc_aload_0:
		case OpcodeConst.opc_aload_1:   case OpcodeConst.opc_aload_2:
		case OpcodeConst.opc_aload_3:   case OpcodeConst.opc_istore:
		case OpcodeConst.opc_istore_0:  case OpcodeConst.opc_istore_1:
		case OpcodeConst.opc_istore_2:  case OpcodeConst.opc_istore_3:
		case OpcodeConst.opc_lstore:    case OpcodeConst.opc_lstore_0:
		case OpcodeConst.opc_lstore_1:  case OpcodeConst.opc_lstore_2:
		case OpcodeConst.opc_lstore_3:  case OpcodeConst.opc_fstore:
		case OpcodeConst.opc_fstore_0:  case OpcodeConst.opc_fstore_1:
		case OpcodeConst.opc_fstore_2:  case OpcodeConst.opc_fstore_3:
		case OpcodeConst.opc_dstore:    case OpcodeConst.opc_dstore_0:
		case OpcodeConst.opc_dstore_1:  case OpcodeConst.opc_dstore_2:
		case OpcodeConst.opc_dstore_3:  case OpcodeConst.opc_astore:
		case OpcodeConst.opc_astore_0:  case OpcodeConst.opc_astore_1:
		case OpcodeConst.opc_astore_2:  case OpcodeConst.opc_astore_3:
		case OpcodeConst.opc_dup:
		    copyBound(OUT[bpc], ir.getTarget(0), ir.getOperand(0));
		    break;
		case OpcodeConst.opc_aaload:
		case OpcodeConst.opc_iaload:
		case OpcodeConst.opc_laload:
		case OpcodeConst.opc_faload:
		case OpcodeConst.opc_daload:
		case OpcodeConst.opc_baload:
		case OpcodeConst.opc_caload:
		case OpcodeConst.opc_saload:
		    insertCheckBound(OUT[bpc], ir.getOperand(1), ir.getOperand(0), zero, -1, 0);
		    removeBound(OUT[bpc], ir.getTarget(0));
		    insertBound(C_GEN[bpc],ir.getOperand(1), ir.getOperand(0), zero, -1, 0);
		    break;
		case OpcodeConst.opc_aastore:
		case OpcodeConst.opc_iastore:
		case OpcodeConst.opc_lastore:
		case OpcodeConst.opc_fastore:
		case OpcodeConst.opc_dastore:
		case OpcodeConst.opc_bastore:
		case OpcodeConst.opc_castore:
		case OpcodeConst.opc_sastore:
		    insertCheckBound(OUT[bpc],ir.getOperand(1), ir.getOperand(0), zero, -1, 0);
		    insertBound(C_GEN[bpc],ir.getOperand(1), ir.getOperand(0), zero, -1, 0);
		    break;
		case OpcodeConst.opc_dup_x1:    case OpcodeConst.opc_dup_x2:
		case OpcodeConst.opc_dup2:      case OpcodeConst.opc_dup2_x1:
		case OpcodeConst.opc_dup2_x2:
		    new aotc.share.Assert(false, "DUP");
		    break;
		case OpcodeConst.opc_swap:
		    IndexBound ib1 = (IndexBound)IN[bpc].get(ir.getTarget(0).toString());
		    IndexBound ib2 = (IndexBound)IN[bpc].get(ir.getTarget(1).toString());
		    
		    OUT[bpc].put(ir.getTarget(0).toString(), ib2);
		    OUT[bpc].put(ir.getTarget(1).toString(), ib1);
		    break;
		case OpcodeConst.opc_frem: case OpcodeConst.opc_drem:
		case OpcodeConst.opc_irem: case OpcodeConst.opc_lrem:
		    arithRem(OUT[bpc], ir.getTarget(0), ir.getOperand(0), ir.getOperand(1));
		    break;
		case OpcodeConst.opc_iadd: case OpcodeConst.opc_ladd:
		case OpcodeConst.opc_fadd: case OpcodeConst.opc_dadd:
		    arithAdd(OUT[bpc], ir.getTarget(0), ir.getOperand(0), ir.getOperand(1));
		    break;
		case OpcodeConst.opc_isub: case OpcodeConst.opc_lsub:
		case OpcodeConst.opc_fsub: case OpcodeConst.opc_dsub:
		    arithSub(OUT[bpc], ir.getTarget(0), ir.getOperand(0), ir.getOperand(1));
		    break;
		case OpcodeConst.opc_imul: case OpcodeConst.opc_lmul:
		case OpcodeConst.opc_fmul: case OpcodeConst.opc_dmul:
		    arithMul(OUT[bpc], ir.getTarget(0), ir.getOperand(0), ir.getOperand(1));
		    break;
		case OpcodeConst.opc_idiv: case OpcodeConst.opc_ldiv:
		case OpcodeConst.opc_fdiv: case OpcodeConst.opc_ddiv:
		    arithDiv(OUT[bpc], ir.getTarget(0), ir.getOperand(0), ir.getOperand(1));
		    break;
		case OpcodeConst.opc_ishl: case OpcodeConst.opc_lshl:
		    arithShl(OUT[bpc], ir.getTarget(0), ir.getOperand(0), ir.getOperand(1));
		    break;
		case OpcodeConst.opc_ishr: case OpcodeConst.opc_lshr:
		    arithShr(OUT[bpc], ir.getTarget(0), ir.getOperand(0), ir.getOperand(1));
		    break;
		case OpcodeConst.opc_iushr: case OpcodeConst.opc_lushr:
		    arithUShr(OUT[bpc], ir.getTarget(0), ir.getOperand(0), ir.getOperand(1));
		    break;
		case OpcodeConst.opc_iand: case OpcodeConst.opc_land:
		    arithAnd(OUT[bpc], ir.getTarget(0), ir.getOperand(0), ir.getOperand(1));
		    break;
		case OpcodeConst.opc_ior: case OpcodeConst.opc_lor:
		    arithOr(OUT[bpc], ir.getTarget(0), ir.getOperand(0), ir.getOperand(1));
		    break;
		case OpcodeConst.opc_ixor: case OpcodeConst.opc_lxor:
		    arithXor(OUT[bpc], ir.getTarget(0), ir.getOperand(0), ir.getOperand(1));
		    break;
		case OpcodeConst.opc_iinc:
		    arithIinc(OUT[bpc], ir.getTarget(0), ir.getOperand(0), ir.getOperand(1));
		    break;
		case OpcodeConst.opc_ineg:  case OpcodeConst.opc_lneg:
		case OpcodeConst.opc_fneg:  case OpcodeConst.opc_dneg:
		    arithNeg(OUT[bpc], ir.getTarget(0), ir.getOperand(0));
		    break;
		case OpcodeConst.opc_arraylength:
		    insertBound(OUT[bpc], ir.getTarget(0), ir.getOperand(0));
		    break;
		case OpcodeConst.opc_aldc_ind_quick:
		case OpcodeConst.opc_aldc_ind_w_quick:
		case OpcodeConst.opc_aldc_quick:
		case OpcodeConst.opc_aldc_w_quick:
		    removeBound(OUT[bpc], ir.getTarget(0));
		    break;
		case OpcodeConst.opc_ldc_quick:
		case OpcodeConst.opc_ldc_w_quick:
		    insertBound(OUT[bpc], ir.getTarget(0), ir.getOperand(0));
		    break;
		case OpcodeConst.opc_ldc2_w_quick:
		    removeBound(OUT[bpc], ir.getTarget(0));
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
		    if(ir.getNumOfTargets() != 0)
			removeBound(OUT[bpc], ir.getTarget(0));
		    for(int i = 0 ; i < ir.getNumOfOperands() ; i++) {
			if(ir.getOperand(i).getType() == TypeInfo.TYPE_REFERENCE) {
			    removeBound(OUT[bpc], ir.getOperand(i));
			}
		    }
		    break;
		case OpcodeConst.opc_instanceof_quick:
		    removeBound(OUT[bpc], ir.getTarget(0));
		    break;
		case OpcodeConst.opc_agetstatic_quick:
		case OpcodeConst.opc_getstatic_quick:
		case OpcodeConst.opc_agetstatic_checkinit_quick:
		case OpcodeConst.opc_getstatic_checkinit_quick:
		case OpcodeConst.opc_getstatic2_quick:
		case OpcodeConst.opc_getstatic2_checkinit_quick:
		    removeBound(OUT[bpc], ir.getTarget(0));
		    break;
		case OpcodeConst.opc_aputstatic_quick:
		case OpcodeConst.opc_putstatic_quick:
		case OpcodeConst.opc_aputstatic_checkinit_quick:
		case OpcodeConst.opc_putstatic_checkinit_quick:
		case OpcodeConst.opc_putstatic2_quick:
		case OpcodeConst.opc_putstatic2_checkinit_quick:
		    if(ir.getOperand(0).getType() == TypeInfo.TYPE_REFERENCE) {
			removeBound(OUT[bpc], ir.getOperand(0));
		    }
		    break;
		case OpcodeConst.opc_getfield_quick:
		case OpcodeConst.opc_getfield2_quick:
		case OpcodeConst.opc_agetfield_quick:
		case OpcodeConst.opc_getfield_quick_w:
		    removeBound(OUT[bpc], ir.getTarget(0));
		    break;
		case OpcodeConst.opc_putfield_quick:
		case OpcodeConst.opc_putfield2_quick:
		case OpcodeConst.opc_aputfield_quick:
		case OpcodeConst.opc_putfield_quick_w:
		    if(ir.getOperand(1).getType() == TypeInfo.TYPE_REFERENCE) {
			removeBound(OUT[bpc], ir.getOperand(1));
		    }
		    break;
		case OpcodeConst.opc_new_quick:
		case OpcodeConst.opc_new_checkinit_quick:
		    removeBound(OUT[bpc], ir.getTarget(0));
		    break;
		case OpcodeConst.opc_newarray:
		    insertBound(OUT[bpc], ir.getTarget(0), ir.getOperand(0));
		    break;
		case OpcodeConst.opc_anewarray_quick:
		    insertBound(OUT[bpc], ir.getTarget(0), ir.getOperand(1));
		    break;
		case OpcodeConst.opc_multianewarray_quick:
		    removeBound(OUT[bpc], ir.getTarget(0));
		    break;

	    }
	    if(checkChange(oldOUT, OUT[bpc])) {
		for(int i = 0 ; i < ir.getSuccNum() ; i++) {
		    changeNodes.add(ir.getSucc(i));
		}
	    }
	}
	return true;
    }
    public void backwardAnalysis() {
	TreeSet changeNodes = new TreeSet();
	Iterator temp;
	temp = cfg.iterator();
	while(temp.hasNext())
	    changeNodes.add((IR)temp.next());
	
	int bpc;
	IR ir;
	
	C_IN = new Hashtable[codeSize];
	C_OUT = new Hashtable[codeSize];
	C_GEN_BACKWARD = new Hashtable[codeSize];
	while(changeNodes.size() > 0) {
	    ir = (IR)changeNodes.last();
	    bpc = ir.getBpc();

	    changeNodes.remove(ir);
	
	    Hashtable oldIN = C_IN[bpc];
	    solveOutSet(ir, C_IN, C_OUT);
	    
	    C_GEN_BACKWARD[bpc] = new Hashtable();
	    C_IN[bpc] = new Hashtable();
	    if(ir.hasAttribute(IRAttribute.ELIMINATED)) {
		C_IN[bpc] = new Hashtable(C_OUT[bpc]);
		if(checkChange(oldIN, C_IN[bpc])) {
		    for(int i = 0 ; i < ir.getPredNum() ; i++) {
			changeNodes.add(ir.getPred(i));
		    }
		}
		continue;
	    }
	    switch(ir.getShortOpcode()) {
		case OpcodeConst.opc_iaload:	case OpcodeConst.opc_laload:
		case OpcodeConst.opc_faload:	case OpcodeConst.opc_daload:
		case OpcodeConst.opc_aaload:	case OpcodeConst.opc_baload:
		case OpcodeConst.opc_caload:	case OpcodeConst.opc_saload:
		    if( ir.hasAttribute(IRAttribute.NULLCHECKELIMINATED) ) break;

		case OpcodeConst.opc_istore_0:  case OpcodeConst.opc_istore_1:
		case OpcodeConst.opc_istore_2:  case OpcodeConst.opc_istore_3:
		case OpcodeConst.opc_lstore:    case OpcodeConst.opc_lstore_0:
		case OpcodeConst.opc_lstore_1:  case OpcodeConst.opc_lstore_2:
		case OpcodeConst.opc_lstore_3:  case OpcodeConst.opc_fstore:
		case OpcodeConst.opc_fstore_0:  case OpcodeConst.opc_fstore_1:
		case OpcodeConst.opc_fstore_2:  case OpcodeConst.opc_fstore_3:
		case OpcodeConst.opc_dstore:    case OpcodeConst.opc_dstore_0:
		case OpcodeConst.opc_dstore_1:  case OpcodeConst.opc_dstore_2:
		case OpcodeConst.opc_dstore_3:  case OpcodeConst.opc_astore:
		case OpcodeConst.opc_astore_0:  case OpcodeConst.opc_astore_1:
		case OpcodeConst.opc_astore_2:  case OpcodeConst.opc_astore_3:

		case OpcodeConst.opc_iastore:	case OpcodeConst.opc_lastore:
		case OpcodeConst.opc_fastore:	case OpcodeConst.opc_dastore:
		case OpcodeConst.opc_aastore:	case OpcodeConst.opc_bastore:
		case OpcodeConst.opc_castore:	case OpcodeConst.opc_sastore:
		
		case OpcodeConst.opc_ireturn:	case OpcodeConst.opc_lreturn:
		case OpcodeConst.opc_freturn:	case OpcodeConst.opc_dreturn:
		case OpcodeConst.opc_areturn:	case OpcodeConst.opc_return:

		case OpcodeConst.opc_newarray:
		case OpcodeConst.opc_new_quick:
		case OpcodeConst.opc_new_checkinit_quick:
		case OpcodeConst.opc_anewarray_quick:
		case OpcodeConst.opc_multianewarray_quick:
		
		case OpcodeConst.opc_athrow:
		case OpcodeConst.opc_monitorenter:
		case OpcodeConst.opc_monitorexit:
		
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

		case OpcodeConst.opc_agetstatic_quick:
		case OpcodeConst.opc_getstatic_quick:
		case OpcodeConst.opc_agetstatic_checkinit_quick:
		case OpcodeConst.opc_getstatic_checkinit_quick:

		case OpcodeConst.opc_getstatic2_quick:
		case OpcodeConst.opc_getstatic2_checkinit_quick:

		case OpcodeConst.opc_aputstatic_quick:
		case OpcodeConst.opc_putstatic_quick:
		case OpcodeConst.opc_aputstatic_checkinit_quick:
		case OpcodeConst.opc_putstatic_checkinit_quick:

		case OpcodeConst.opc_putstatic2_quick:
		case OpcodeConst.opc_putstatic2_checkinit_quick:

		case OpcodeConst.opc_getfield_quick:
		case OpcodeConst.opc_getfield2_quick:
		case OpcodeConst.opc_agetfield_quick:
		case OpcodeConst.opc_getfield_quick_w:

		case OpcodeConst.opc_putfield_quick:
		case OpcodeConst.opc_putfield2_quick:
		case OpcodeConst.opc_aputfield_quick:
		case OpcodeConst.opc_putfield_quick_w:

		case OpcodeConst.opc_idiv: case OpcodeConst.opc_ldiv:
		case OpcodeConst.opc_irem: case OpcodeConst.opc_lrem:
		case OpcodeConst.opc_checkcast_quick:
		    C_OUT[bpc] = new Hashtable();
		    break;
	    }
	    Enumeration it;
	    it = C_OUT[bpc].keys();
	    while(it.hasMoreElements()) {
		String key = it.nextElement().toString();
		if(OUT[bpc].get(key) == null) {
		    insertBound(C_IN[bpc], key, ((IndexBound)C_OUT[bpc].get(key)).getUB(0), ((IndexBound)C_OUT[bpc].get(key)).getLB(0),
			((IndexBound)C_OUT[bpc].get(key)).getUOffset(0), ((IndexBound)C_OUT[bpc].get(key)).getLOffset(0));
		    continue;
		}
		int affect = ((IndexBound)OUT[bpc].get(key)).getAttribute();
		if(affect == CHANGED) {
		    removeBound(C_IN[bpc], key);
		}else if(affect == INCREMENT) {
		    insertBound(C_IN[bpc], key, ((IndexBound)C_OUT[bpc].get(key)).getUB(0), null, ((IndexBound)C_OUT[bpc].get(key)).getUOffset(0), 0);
		}else if(affect == DECREMENT) {
		    insertBound(C_IN[bpc], key, null, ((IndexBound)C_OUT[bpc].get(key)).getLB(0), 0, ((IndexBound)C_OUT[bpc].get(key)).getLOffset(0));
		}else {
		    insertBound(C_IN[bpc], key, ((IndexBound)C_OUT[bpc].get(key)).getUB(0), ((IndexBound)C_OUT[bpc].get(key)).getLB(0),
			((IndexBound)C_OUT[bpc].get(key)).getUOffset(0), ((IndexBound)C_OUT[bpc].get(key)).getLOffset(0));
		}
	    }
	    
	    if(!C_GEN[bpc].isEmpty() ) {
		Enumeration enu = C_GEN[bpc].keys();
		if(enu.hasMoreElements()) {
		    String key = enu.nextElement().toString();
		    IndexBound gen = (IndexBound)C_GEN[bpc].get(key);
		    if(C_IN[bpc].get(key) != null) {
			IndexBound backward = (IndexBound)C_IN[bpc].get(key);
			int select, uOffset, lOffset;
			JavaVariable uBound, lBound;
			select = findMinValue(gen.getUB(0), backward.getUB(0), gen.getUOffset(0), backward.getUOffset(0), OUT[bpc]);
			if(select <= 1) {
			    uBound = gen.getUB(0);
			    uOffset = gen.getUOffset(0);
			}else {
			    uBound = backward.getUB(0);
			    uOffset = backward.getUOffset(0);
			}
			select = findMaxValue(gen.getLB(0), backward.getLB(0), gen.getLOffset(0), backward.getLOffset(0), OUT[bpc]);
			if(select <= 1 ) {
			    lBound = gen.getLB(0);
			    lOffset = gen.getLOffset(0);
			}else {
			    lBound = backward.getLB(0);
			    lOffset = backward.getLOffset(0);
			}
			if( ir.hasAttribute(IRAttribute.NULLCHECKELIMINATED)) {
			    C_IN[bpc].put(key, new IndexBound(uBound, lBound, uOffset, lOffset));
			}
			C_GEN_BACKWARD[bpc].put(key, new IndexBound(uBound, lBound, uOffset, lOffset));
		    }else {
			if(ir.hasAttribute(IRAttribute.NULLCHECKELIMINATED)) {
			    C_IN[bpc].put(key, new IndexBound(gen));
			}
			C_GEN_BACKWARD[bpc].put(key, new IndexBound(gen));
		    }
		}
	    }
	    if(checkChange(oldIN, C_IN[bpc])) {
		for(int i = 0 ; i < ir.getPredNum() ; i++) {
		    changeNodes.add(ir.getPred(i));
		}
	    }
	}
    }
    
    public void forwardAnalysis() {
	TreeSet changeNodes = new TreeSet();
	Iterator temp;
	temp = cfg.iterator();
	while(temp.hasNext())
	    changeNodes.add((IR)temp.next());

	int bpc;
	IR ir;

	C_IN = new Hashtable[codeSize];
	C_OUT = new Hashtable[codeSize];
	C_RESULT = new Hashtable[codeSize];

	while(changeNodes.size() > 0) {
	    ir = (IR)changeNodes.first();
	    bpc = ir.getBpc();

	    changeNodes.remove(ir);
	    
	    Hashtable oldOUT = C_OUT[bpc];

	    solveInSet(ir, C_IN, C_OUT);
	    C_OUT[bpc] = new Hashtable(C_IN[bpc]);
	    
	    if( ir.hasAttribute(IRAttribute.ELIMINATED)) {
		if(checkChange(oldOUT, C_OUT[bpc])) {
		    for(int i = 0 ; i < ir.getSuccNum() ; i++) {
			changeNodes.add(ir.getSucc(i));
		    }
		}
		continue;
	    }
	    Enumeration it;
	    it = C_IN[bpc].keys();
	    while(it.hasMoreElements()) {
		String key = it.nextElement().toString();
		int affect = ((IndexBound)OUT[bpc].get(key)).getAttribute();
		if(affect == CHANGED) {
		    insertBound(C_OUT[bpc], key, inf, mInf, 0, 0);
		}else if(affect == INCREMENT) {
		    insertBound(C_OUT[bpc], key, inf, ((IndexBound)C_IN[bpc].get(key)).getLB(0), 0, ((IndexBound)C_IN[bpc].get(key)).getLOffset(0));
		}else if(affect == DECREMENT) {
		    insertBound(C_OUT[bpc], key, ((IndexBound)C_IN[bpc].get(key)).getUB(0), mInf, ((IndexBound)C_IN[bpc].get(key)).getUOffset(0), 0);
		}else {
		    insertBound(C_OUT[bpc], key, ((IndexBound)C_IN[bpc].get(key)).getUB(0), ((IndexBound)C_IN[bpc].get(key)).getLB(0),
			((IndexBound)C_IN[bpc].get(key)).getUOffset(0), ((IndexBound)C_IN[bpc].get(key)).getLOffset(0) );
		}
	    }
	    if(!C_GEN_BACKWARD[bpc].isEmpty()) {
		Enumeration keys = C_GEN_BACKWARD[bpc].keys();
		C_RESULT[bpc] = new Hashtable();
		if(keys.hasMoreElements()) {
		    String key = keys.nextElement().toString();
		    IndexBound gen = (IndexBound)C_GEN_BACKWARD[bpc].get(key);
		    if(C_OUT[bpc].get(key) != null) {
			IndexBound forward = (IndexBound)C_OUT[bpc].get(key);
			int select, uOffset, lOffset;
			JavaVariable uBound, lBound;
			boolean uCheck = true;
			boolean lCheck = true;
			select = findMinValue(gen.getUB(0), forward.getUB(0), gen.getUOffset(0), forward.getUOffset(0), IN[bpc]);
			if(select <= 1) {
			    uBound = gen.getUB(0);
			    uOffset = gen.getUOffset(0);
			}else {
			    uBound = forward.getUB(0);
			    uOffset = forward.getUOffset(0);
			    uCheck = false;
			}
			select = findMaxValue(gen.getLB(0), forward.getLB(0), gen.getLOffset(0), forward.getLOffset(0), IN[bpc]);
			if(select <= 1) {
			    lBound = gen.getLB(0);
			    lOffset = gen.getLOffset(0);
			}else {
			    lBound = forward.getLB(0);
			    lOffset = forward.getLOffset(0);
			    lCheck = false;
			}
			C_IN[bpc].put(key, new IndexBound(uBound, lBound, uOffset, lOffset));

			if(uCheck) {
			    uBound = gen.getUB(0);
			    uOffset = gen.getUOffset(0);
			}else {
			    uBound = inf;
			    uOffset = 0;
			}
			if(lCheck) {
			    lBound = gen.getLB(0);
			    lOffset = gen.getLOffset(0);
			}else {
			    lBound = mInf;
			    lOffset = 0;
			}
			C_RESULT[bpc].put(key, new IndexBound(uBound, lBound, uOffset, lOffset));
		    }else {
			C_IN[bpc].put(key, new IndexBound(gen));
			C_RESULT[bpc].put(key, new IndexBound(gen));
		    }
		}
	    }

	    if(checkChange(oldOUT, C_OUT[bpc])) {
		for(int i = 0 ; i < ir.getSuccNum() ; i++) {
		    changeNodes.add(ir.getSucc(i));
		}
	    }
	}
    }
    private int findMaxValue(JavaVariable value1, JavaVariable value2, int offset1, int offset2, Hashtable set) {
	if(offset1 == offset2 && ((inf.equals(value1) && inf.equals(value2)) || (mInf.equals(value1) && mInf.equals(value2)))) return 0;
	if(inf.equals(value1) || mInf.equals(value2)) return 1;
	if(inf.equals(value2) || mInf.equals(value1)) return 2;

	if(value1 == null || value2 == null) return -1;
	if(value1.equals(value2)) {
	    if(offset1 > offset2) {
		return 1;
	    }else if(offset1 < offset2) {
		return 2;
	    }
	    return 0;
	}
	IndexBound bound1 = findValue(value1, set);
	IndexBound bound2 = findValue(value2, set);
	if(bound1 == null || bound2 == null) {
	    return -1;
	}
	if( bound1.getUB(0) != null && bound1.getLB(0) != null &&
	    bound1.toString().equals(bound2.toString())) {
	    return 0;
	}
	JavaVariable bound1LB = bound1.getLB(0);
	JavaVariable bound1UB = bound1.getUB(0);
	JavaVariable bound2LB = bound2.getLB(0);
	JavaVariable bound2UB = bound2.getUB(0);
	
	if(inf.equals(bound1LB) || mInf.equals(bound2UB)) return 1;
	if(inf.equals(bound2LB) || mInf.equals(bound1UB)) return 2;
	
	Integer iValue1 = new Integer(0);
	Integer iValue2 = new Integer(0);
	boolean isInteger = true;
	try {
	    iValue1 = new Integer(bound1LB.toString());
	}catch(Exception e) {
	    isInteger = false;
	}
	try {
	    iValue2 = new Integer(bound2UB.toString());
	}catch(Exception e) {
	    isInteger = false;
	}
	if( isInteger && (iValue1.intValue() + bound1.getLOffset(0) + offset1) >= (iValue2.intValue() + bound2.getUOffset(0) + offset2)) return 1;

	try {
	    iValue1 = new Integer(bound1UB.toString());
	}catch(Exception e) {
	    return -1;
	}
	try {
	    iValue2 = new Integer(bound2LB.toString());
	}catch(Exception e) {
	    return -1;
	}
	if((iValue1.intValue() + bound1.getUOffset(0) + offset1) <= (iValue2.intValue() + bound2.getLOffset(0) + offset2)) return 2;
	return -1;
    }
    private int findMinValue(JavaVariable value1, JavaVariable value2, int offset1, int offset2, Hashtable set ) {
	return findMaxValue(value2, value1, offset2, offset1, set);
    }
    private IndexBound findValue(JavaVariable value, Hashtable set) {
	if(set == null) return null;
	JavaVariable upperBound, lowBound;
	int upperOffset = 0;
	int lowOffset = 0;
	TreeSet passed = new TreeSet();
	
	upperBound = value;
	passed.add(value);
	while(upperBound != null) {
	    Integer iValue;
	    try {
		iValue = new Integer(upperBound.toString());
		break;
	    }catch(Exception e) {}
	    IndexBound bound = (IndexBound)set.get(upperBound.toString());
	    if(bound == null || bound.getUB(0) == null || passed.contains(bound.getUB(0))) break;
	    upperBound = bound.getUB(0);
	    passed.add(upperBound);
	    upperOffset += bound.getUOffset(0);
	}
	
	passed = new TreeSet();
	passed.add(value);
	
	lowBound = value;
	while(lowBound != null) {
	    Integer iValue;
	    try {
		iValue = new Integer(lowBound.toString());
		break;
	    }catch(Exception e) {}

	    IndexBound bound = (IndexBound)set.get(lowBound.toString());
	    if(bound == null || bound.getLB(0) == null || passed.contains(bound.getLB(0))) break;
	    lowBound = bound.getLB(0);
	    passed.add(lowBound);
	    lowOffset += bound.getLOffset(0);
	}
	return new IndexBound(upperBound, lowBound, upperOffset, lowOffset);
    }
	    
    private void arithDiv(Hashtable set, JavaVariable target, JavaVariable operand1, JavaVariable operand2) {
	if(hasNoBound(operand1, set) || hasNoBound(operand2, set)) {
	    removeBound(set, target);
	    return;
	}

	if(target.equals(operand1)) {
	    IndexBound ib = (IndexBound)set.get(operand2.toString());
	    IndexBound targetBound = new IndexBound((IndexBound)set.get(target.toString()));
	    removeBound(set, target);	
	    if(ib.isGreaterThanOne(set, 0)) {
		if(targetBound.isNonNegative(set, 0)){
		    targetBound.setLB(0,zero,0);
		    targetBound.setAttribute(DECREMENT);
		    set.put(target.toString(), targetBound);
		}else if(targetBound.isNonPositive(set, 0)) {
		    targetBound.setUB(0,zero,0);
		    targetBound.setAttribute(INCREMENT);
		    set.put(target.toString(), targetBound);
		}else {
		    removeBound(set, target);
		}
	    }else if(ib.isLessThanOne(set, 0)&& ib.isPositive(set, 0)) {
		if(targetBound.isPositive(set, 0)) {
		    targetBound.setUB(0,inf,0);
		    targetBound.setAttribute(INCREMENT);
		    set.put(target.toString(), targetBound);
		}else {
		    removeBound(set, target);
		}
	    }
	}else {
	    removeBound(set, target);
	}
    }
    
    private void arithAdd(Hashtable set, JavaVariable target, JavaVariable operand1, JavaVariable operand2) {
	if(hasNoBound(operand1, set) || hasNoBound(operand2, set)) {
	    removeBound(set, target);
	    return;
	}
	
	JavaVariable operand = null;
	if(target.equals(operand1)) {
	    operand = operand2;
	}else if(target.equals(operand2)) {
	    operand = operand1;
	}
	
	if(operand == null) {
	    removeBound(set, target);
	}else {
	    IndexBound ib = (IndexBound)set.get(operand.toString());
	    IndexBound targetBound = new IndexBound((IndexBound)set.get(target.toString()));
	    
	    removeBound(set, target);
	    if(ib.isPositive(set, 0)) {
		targetBound.setUB(0,inf,0);
		targetBound.setAttribute(INCREMENT);
		set.put(target.toString(), targetBound);
	    }else if(ib.isNegative(set, 0)) {
		targetBound.setLB(0, mInf,0);
		targetBound.setAttribute(DECREMENT);
		set.put(target.toString(), targetBound);
	    }if(ib.isZero(set, 0)) {
		set.put(target.toString(), targetBound);
	    }	
	}
    }
    
    private void arithSub(Hashtable set, JavaVariable target, JavaVariable operand1, JavaVariable operand2) {
	if(hasNoBound(operand1, set) || hasNoBound(operand2, set)) {
	    removeBound(set, target);
	    return;
	}
	
	if(target.equals(operand1)) {
	    IndexBound ib = (IndexBound)set.get(operand2.toString());
	    IndexBound targetBound = new IndexBound((IndexBound)set.get(target.toString()));

	    removeBound(set, target);
	    if(ib.isNegative(set, 0)) {
		targetBound.setUB(0,inf,0);
		targetBound.setAttribute(INCREMENT);
		set.put(target.toString(), targetBound);
	    }else if(ib.isPositive(set, 0)) {
		targetBound.setLB(0,mInf,0);
		targetBound.setAttribute(DECREMENT);
		set.put(target.toString(), targetBound);
	    }if(ib.isZero(set, 0)) {
		set.put(target.toString(), targetBound);
	    }
	}else {
	    removeBound(set, target);
	}
    }

    private void arithRem(Hashtable set, JavaVariable target, JavaVariable operand1, JavaVariable operand2) {
	if(hasNoBound(operand1, set) || hasNoBound(operand2, set)) {
	    removeBound(set, target);
	    return;
	}
	
	if(target.equals(operand1)) {
	    IndexBound ib = (IndexBound)set.get(operand2.toString());
	    
	    removeBound(set, target);
	    if(ib.isPositive(set, 0)) {
		IndexBound bound = new IndexBound(ib.getUB(0), zero, -1 , 0);
		bound.setAttribute(CHANGED);
		set.put(target.toString(), bound);
	    }else if(ib.isNegative(set, 0)) {
		JavaVariable temp = new Constant(ib.getUB(0).getType(), ib.getUB(0).toString().substring(1));
		IndexBound bound = new IndexBound(temp, zero , -1, 0);
		bound.setAttribute(CHANGED);
		set.put(target.toString(), bound);
	    }
	}else {
	    removeBound(set, target);
	}
    }

    private void arithMul(Hashtable set, JavaVariable target, JavaVariable operand1, JavaVariable operand2) {
	if(hasNoBound(operand1, set) || hasNoBound(operand2, set)) {
	    removeBound(set, target);
	    return;
	}

	IndexBound ib1 = (IndexBound)set.get(operand1.toString());
	IndexBound ib2 = (IndexBound)set.get(operand2.toString());

	removeBound(set, target);
	IndexBound bound;
	if((ib1.isNonNegative(set, 0) && ib2.isNonNegative(set, 0)) || (ib1.isNonPositive(set, 0) && ib2.isNonPositive(set, 0))) {
	    bound = new IndexBound(inf, zero, 0, 0);
	    bound.setAttribute(INCREMENT);
	    set.put(target.toString(), bound);
	}else if((ib1.isNonNegative(set, 0) && ib2.isNonPositive(set, 0)) || (ib1.isNonPositive(set, 0) && ib2.isNonNegative(set, 0))) {
	    bound = new IndexBound(zero, mInf, 0, 0);
	    bound.setAttribute(DECREMENT);
	    set.put(target.toString(), bound);
	}else {
	    removeBound(set, target);
	}
    }
    private void arithIinc(Hashtable set, JavaVariable target, JavaVariable operand1, JavaVariable operand2) {
	if(hasNoBound(target, set)) return;

	IndexBound bound = new IndexBound((IndexBound)set.get(target.toString()));
	
	removeBound(set, target);
	if(operand2.toString().charAt(0) == '-') {
	    bound.setLB(0,mInf,0);
	    bound.setAttribute(DECREMENT);
	}else {
	    bound.setUB(0,inf,0);
	    bound.setAttribute(INCREMENT);
	}
	set.put(target.toString(), bound);
    }
    private void arithShl(Hashtable set, JavaVariable target, JavaVariable operand1, JavaVariable operand2) {
	removeBound(set, target);
    }
    private void arithShr(Hashtable set, JavaVariable target, JavaVariable operand1, JavaVariable operand2) {
	removeBound(set, target);
    }
    private void arithUShr(Hashtable set, JavaVariable target, JavaVariable operand1, JavaVariable operand2) {
	removeBound(set, target);
    }
    private void arithAnd(Hashtable set, JavaVariable target, JavaVariable operand1, JavaVariable operand2) {
	removeBound(set, target);
    }
    private void arithOr(Hashtable set, JavaVariable target, JavaVariable operand1, JavaVariable operand2) {
	removeBound(set, target);
    }
    private void arithXor(Hashtable set, JavaVariable target, JavaVariable operand1, JavaVariable operand2) {
	removeBound(set, target);
    }
    private void arithNeg(Hashtable set, JavaVariable target, JavaVariable operand) {
	removeBound(set, target);
    }
    private void insertBound(Hashtable set, JavaVariable target, JavaVariable value) {
	insertBound(set, target, value, value, 0, 0);
    }
    
    private void insertBound(Hashtable set, JavaVariable target, JavaVariable ub, JavaVariable lb, int uoffset, int loffset) {
	insertBound(set, target.toString(), ub, lb, uoffset, loffset);
    }
    private void insertBound(Hashtable set, String target, JavaVariable ub, JavaVariable lb, int uoffset, int loffset) {
	removeBound(set, target);
	IndexBound bound = new IndexBound(ub, lb, uoffset, loffset);
	set.put(target, bound);
    }
    private void insertCheckBound(Hashtable set, JavaVariable target, JavaVariable ub, JavaVariable lb, int uoffset, int loffset) {
	TreeSet passed = new TreeSet();
	JavaVariable upperBound = null;
	JavaVariable lowBound = null;
	JavaVariable origin = target;
	int upperOffset = uoffset;
	int lowOffset = loffset;
	IndexBound bound;
	
	IndexBound upper = findValue(ub, set);
	IndexBound low = findValue(lb, set);
	if(upper != null) {
	    ub = upper.getUB(0);
	    upperOffset += upper.getUOffset(0);
	}
	if(low != null) {
	    lb = low.getLB(0);
	    lowOffset += low.getLOffset(0);
	}

	int i = 0;
	passed.add(origin);
	while(true) {

	    bound = (IndexBound)set.get(target.toString());

	    if(bound == null) break;
	    upperBound = bound.getUB(0);
	    if(upperBound == null || passed.contains(upperBound)  /*|| upperBound.endsWith("ref")*/
		    || upperBound.equals(inf) || upperBound.equals(mInf)) {
		break;
	    }
	    try {
		Integer iValue = new Integer(upperBound.toString());
		break;
	    }catch(Exception e) {}
	    passed.add(upperBound);
	    target = upperBound;
	    upperOffset -= bound.getUOffset(0);
	}
	if(bound == null) {
	    upperBound = ub;
	    lowBound = mInf;
	    lowOffset = 0;
	}else {
	    int select = findMinValue(upperBound, ub, bound.getUOffset(0), upperOffset, set);
	    lowBound = bound.getLB(0);
	    lowOffset = bound.getLOffset(0);
	    if(select != 1) {
		upperBound = ub;
	    }else {
		upperOffset = bound.getUOffset(0);
	    }
	}
	if(target.equals(upperBound)) {
	    upperBound = inf;
	    upperOffset = 0;
	}
	if(target.equals(lowBound)) {
	    lowBound = mInf;
	    lowOffset = 0;
	}
	set.put(target.toString(), new IndexBound(upperBound, lowBound, upperOffset, lowOffset));

	lowOffset = 0;
	passed = new TreeSet();
	passed.add(origin);
	target = origin;
	while(true) {
	    bound = (IndexBound)set.get(target.toString());

	    if(bound == null) break;
	    lowBound = bound.getLB(0);
	    if(lowBound == null || passed.contains(lowBound) /*|| lowBound.endsWith("ref")*/
		    || lowBound.equals(inf) || lowBound.equals(mInf)) {
		break;
	    }
	    try {
		Integer iValue = new Integer(lowBound.toString());
		break;
	    }catch(Exception e) {}
	    passed.add(lowBound);
	    target = lowBound;
	    lowOffset -= bound.getLOffset(0);
	}
	if(bound == null) {
	    lowBound = lb;
	    upperBound = inf;
	    upperOffset = 0;
	}else {
	    int select = findMaxValue(lowBound, lb, bound.getLOffset(0), lowOffset, set);
	    upperBound = bound.getUB(0);
	    upperOffset = bound.getUOffset(0);
	    if(select != 1) {
		lowBound = lb;
	    }else {
		lowOffset = bound.getUOffset(0);
	    }
	}
	if(target.equals(upperBound)) {
	    upperBound = inf;
	    upperOffset = 0;
	}
	if(target.equals(lowBound)) {
	    lowBound = mInf;
	    lowOffset = 0;
	}
	set.put(target.toString(), new IndexBound(upperBound, lowBound, upperOffset, lowOffset));
    }
    
    private void removeBound(Hashtable set, JavaVariable target) {
	removeBound(set, target.toString());
    }
    private void removeBound(Hashtable set, String target) {
	IndexBound bound = (IndexBound)set.get(target.toString());
	Enumeration keys = set.keys();
	int i = 0;
	while(keys.hasMoreElements()) {
	    
	    String key = keys.nextElement().toString();
	    IndexBound element = new IndexBound((IndexBound)set.get(key.toString()));
	    if(element == null) continue;
	    if(bound == null) {
		element.switchLB(target, mInf);
		element.switchUB(target, inf);
	    }else {
		if(bound.isNonNegative(set, 0)) {
		    element.switchLB(target, bound.getLB(0));
		}else if(target.endsWith("ref")) {
		    element.switchLB(target, zero);
		}else {
		    element.switchLB(target, bound.getLB(0));
		}
		element.switchUB(target, bound.getUB(0));
	    }
	    set.put(key, element);
	}
	set.remove(target);
    }
    private void copyBound(Hashtable set, JavaVariable target, JavaVariable operand) {
	removeBound(set, target);
	IndexBound to = new IndexBound(operand, operand, 0, 0);
	set.put(target.toString(), to);
    }
    public boolean hasNoBound(JavaVariable target, Hashtable set) {
	IndexBound targetBound = (IndexBound)set.get(target.toString());
	if(targetBound == null) return true;
	for(int i = 0 ; i < targetBound.sizeOfUB() ; i++) {
	    if(targetBound.getUB(i) != null && !targetBound.getUB(i).equals(inf)) return false;
	}
	for(int i = 0 ; i < targetBound.sizeOfUB() ; i++) {
	    if(targetBound.getLB(i) != null && !targetBound.getLB(i).equals(mInf)) return false;
	}
	return true;
    }
    private void solveInSet_preforward(IR ir, Hashtable[] IN, Hashtable[] OUT) {
	int bpc = ir.getBpc();
	IN[bpc] = new Hashtable();
	if(ir.getPredNum() > 0) {
	    Hashtable pred;
	    if(OUT[ir.getPred(0).getBpc()] == null) {
		pred = new Hashtable();
	    }else {
		pred = new Hashtable(OUT[ir.getPred(0).getBpc()]);
	    }
	    IN[bpc] = new Hashtable(pred);
	}
	for(int i = 1 ; i < ir.getPredNum() ; i++) {
	    Hashtable pred;
	    if(OUT[ir.getPred(i).getBpc()] == null) {
		pred = new Hashtable();
	    }else {
		pred = new Hashtable(OUT[ir.getPred(i).getBpc()]);
	    }
	    Enumeration keys = IN[bpc].keys();
	    while(keys.hasMoreElements()) {
		String key = keys.nextElement().toString();
		if(pred.get(key) == null) {
		    IN[bpc].remove(key);
		}else {
		    IndexBound predBound = (IndexBound)pred.get(key);
		    IndexBound inBound = (IndexBound)IN[ir.getBpc()].get(key);
		    int select, uOffset, lOffset;
		    IndexBound result = new IndexBound();
		    int size = predBound.sizeOfUB() > inBound.sizeOfUB() ? predBound.sizeOfUB() : inBound.sizeOfUB();
		    for(int j = 0 ; j < size ; j++) {
			JavaVariable uBound, lBound;
			select = findMaxValue(predBound.getUB(j), inBound.getUB(j), predBound.getUOffset(j), inBound.getUOffset(j), IN[bpc]);
			if(select == 1 || select == 0 ) {
			    uBound = predBound.getUB(j);
			    uOffset = predBound.getUOffset(j);
			}else if(select == 2) {
			    uBound = inBound.getUB(j);
			    uOffset = inBound.getUOffset(j);
			}else {
			    uBound = inf;
			    uOffset = 0;
			}	
			select = findMinValue(predBound.getLB(j), inBound.getLB(j), predBound.getLOffset(j), inBound.getLOffset(j), IN[bpc]);
			if(select == 1 || select == 0) {
			    lBound = predBound.getLB(j);
			    lOffset = predBound.getLOffset(j);
			}else if(select == 2){
			    lBound = inBound.getLB(j);
			    lOffset = inBound.getLOffset(j);
			}else {
			    lBound = mInf;
			    lOffset = 0;
			}
			result.setBound(j, uBound, lBound, uOffset, lOffset);
		    }
		    IN[bpc].put(key, result);
		}
	    }
	}
	if(ir.getPredNum() == 1) {
	    IR pred = ir.getPred(0);
	    Hashtable set = IN[pred.getBpc()];
	    switch(pred.getShortOpcode()) {
		case OpcodeConst.opc_ifeq:
		    if(pred.getBranchTarget().getBpc() == bpc)
			insertCheckBound(IN[bpc], pred.getOperand(0), zero, zero, 0,0);
		    break;
		case OpcodeConst.opc_ifne:
		    if(pred.getBranchTarget().getBpc() != bpc)
			insertCheckBound(IN[bpc], pred.getOperand(0), zero, zero, 0,0);
		    break;
		case OpcodeConst.opc_iflt: 
		    {
			IndexBound bound = (IndexBound)IN[bpc].get(pred.getOperand(0).toString());
			if(bound == null) bound = new IndexBound(inf , mInf , 0, 0);
			if(pred.getBranchTarget().getBpc() == bpc) {
			    int select, offset;
			    JavaVariable ub;
			    select = findMinValue(mOne, bound.getUB(0),0,bound.getUOffset(0),set);
			    if(select <= 1) {
				ub = mOne;
				offset = 0;
			    }else {
				ub = bound.getUB(0);
				offset = bound.getUOffset(0);
			    }
			    insertCheckBound(IN[bpc], pred.getOperand(0), ub, bound.getLB(0), offset,bound.getLOffset(0));
			}else {
			    JavaVariable lb;
			    int select, offset;
			    select = findMaxValue(zero, bound.getLB(0), 0, bound.getLOffset(0),set);
			    if(select <= 1) {
				lb = zero;
				offset = 0;
			    }else {
				lb = bound.getLB(0);
				offset = bound.getLOffset(0);
			    }
			    insertCheckBound(IN[bpc], pred.getOperand(0), bound.getUB(0), lb, bound.getUOffset(0), offset);
			}
		    }
		    break;
		case OpcodeConst.opc_ifge:
		    {
			IndexBound bound = (IndexBound)IN[bpc].get(pred.getOperand(0).toString());
			
			if(bound == null) bound = new IndexBound(inf , mInf , 0, 0);
			
			if(pred.getBranchTarget().getBpc() == bpc) {
			    JavaVariable lb;
			    int select, offset;
			    select = findMaxValue(zero, bound.getLB(0), 0, bound.getLOffset(0),set);
			    if(select <= 1) {
				lb = zero;
				offset = 0;
			    }else {
				lb = bound.getLB(0);
				offset = bound.getLOffset(0);
			    }
			    insertCheckBound(IN[bpc], pred.getOperand(0), bound.getUB(0), lb, bound.getUOffset(0), offset);
			}else {
			    int select, offset;
			    JavaVariable ub;
			    select = findMinValue(mOne, bound.getUB(0),0,bound.getUOffset(0),set);
			    if(select <= 1) {
				ub = mOne;
				offset = 0;
			    }else {
				ub = bound.getUB(0);
				offset = bound.getUOffset(0);
			    }
			    insertCheckBound(IN[bpc], pred.getOperand(0), ub, bound.getLB(0), offset,bound.getLOffset(0));

			}
		    }
		    break;
		case OpcodeConst.opc_ifgt:
		    {
			IndexBound bound = (IndexBound)IN[bpc].get(pred.getOperand(0).toString());
			if(bound == null) bound = new IndexBound(inf , mInf , 0, 0);
			if(pred.getBranchTarget().getBpc() == bpc) {
			    JavaVariable lb;
			    int select, offset;
			    select = findMaxValue(one, bound.getLB(0), 0, bound.getLOffset(0),set);
			    if(select <= 1) {
				lb = one;
				offset = 0;
			    }else {
				lb = bound.getLB(0);
				offset = bound.getLOffset(0);
			    }
			    insertCheckBound(IN[bpc], pred.getOperand(0), bound.getUB(0), lb, bound.getUOffset(0), offset);

			}else {
			    int select, offset;
			    JavaVariable ub;
			    select = findMinValue(zero, bound.getUB(0),0,bound.getUOffset(0),set);
			    if(select <= 1) {
				ub = zero;
				offset = 0;
			    }else {
				ub = bound.getUB(0);
				offset = bound.getUOffset(0);
			    }
			    insertCheckBound(IN[bpc], pred.getOperand(0), ub, bound.getLB(0), offset,bound.getLOffset(0));
			}
		    }
		    break;
		case OpcodeConst.opc_ifle:
		    {
			IndexBound bound = (IndexBound)IN[bpc].get(pred.getOperand(0).toString());
			if(bound == null) bound = new IndexBound(inf , mInf , 0, 0);
			if(pred.getBranchTarget().getBpc() == bpc) {
			    int select, offset;
			    JavaVariable ub;
			    select = findMinValue(zero, bound.getUB(0),0,bound.getUOffset(0),set);
			    if(select <= 1) {
				ub = zero;
				offset = 0;
			    }else {
				ub = bound.getUB(0);
				offset = bound.getUOffset(0);
			    }
			    insertCheckBound(IN[bpc], pred.getOperand(0), ub, bound.getLB(0), offset,bound.getLOffset(0));

			}else {
			    JavaVariable lb;
			    int select, offset;
			    select = findMaxValue(one, bound.getLB(0), 0, bound.getLOffset(0),set);
			    if(select <= 1) {
				lb = one;
				offset = 0;
			    }else {
				lb = bound.getLB(0);
				offset = bound.getLOffset(0);
			    }
			    insertCheckBound(IN[bpc], pred.getOperand(0), bound.getUB(0), lb, bound.getUOffset(0), offset);

			}
		    }
		    break;
		case OpcodeConst.opc_if_icmpeq:
		    {
			if(pred.getBranchTarget().getBpc() == bpc) {
			    int select, uOffset1, lOffset1,uOffset2, lOffset2;
			    JavaVariable ub1, lb1, ub2, lb2;
			    
			    if(IN[pred.getBpc()] == null) break;
			    
			    IndexBound bound = (IndexBound)IN[pred.getBpc()].get(pred.getOperand(1).toString());
			    IndexBound target = (IndexBound)IN[pred.getBpc()].get(pred.getOperand(0).toString());
			    if(bound == null || target == null) break;
			    select =  findMinValue(bound.getUB(0), target.getUB(0), bound.getUOffset(0), target.getUOffset(0),set);
			    if(select == 1 || select == 0) {
				ub1 = bound.getUB(0);
				ub2 = bound.getUB(0);
				uOffset1 = bound.getUOffset(0);
				uOffset2 = bound.getUOffset(0);
			    }else if(select == 2){
				ub1 = target.getUB(0);
				ub2 = target.getUB(0);
				uOffset1 = target.getUOffset(0);
				uOffset2 = target.getUOffset(0);
			    }else {
				ub1 = bound.getUB(0);
				ub2 = target.getUB(0);
				uOffset1 = bound.getUOffset(0);
				uOffset2 = target.getUOffset(0);
			    }
			    select = findMaxValue(bound.getLB(0), target.getLB(0), bound.getLOffset(0), target.getLOffset(0),set);
			    if(select == 1 || select == 0) {
				lb1 = bound.getLB(0);
				lb2 = bound.getLB(0);
				lOffset1 = bound.getLOffset(0);
				lOffset2 = bound.getLOffset(0);
			    }else if(select == 2){
				lb1 = target.getLB(0);
				lb2 = target.getLB(0);
				lOffset1 = target.getLOffset(0);
				lOffset2 = target.getLOffset(0);
			    }else {
				lb1 = bound.getLB(0);
				lb2 = target.getLB(0);
				lOffset1 = bound.getLOffset(0);
				lOffset2 = target.getLOffset(0);
			    }
			    insertCheckBound(IN[bpc], pred.getOperand(0), ub1, lb1, uOffset1, lOffset1);
			    insertCheckBound(IN[bpc], pred.getOperand(1), ub2, lb2, uOffset2, lOffset2);
			}
		    }
		    break;
		case OpcodeConst.opc_if_icmpne:
		    {
			if(pred.getBranchTarget().getBpc() != bpc) {
			    int select, uOffset1, lOffset1,uOffset2, lOffset2;
			    JavaVariable ub1, lb1, ub2, lb2;
			    if(IN[pred.getBpc()] == null) break;
			    IndexBound bound = (IndexBound)IN[pred.getBpc()].get(pred.getOperand(1).toString());
			    IndexBound target = (IndexBound)IN[pred.getBpc()].get(pred.getOperand(0).toString());
			    if(bound == null || target == null) break;
			    select =  findMinValue(bound.getUB(0), target.getUB(0), bound.getUOffset(0), target.getUOffset(0),set);
			    if(select == 1 || select == 0) {
				ub1 = bound.getUB(0);
				ub2 = bound.getUB(0);
				uOffset1 = bound.getUOffset(0);
				uOffset2 = bound.getUOffset(0);
			    }else if(select == 2){
				ub1 = target.getUB(0);
				ub2 = target.getUB(0);
				uOffset1 = target.getUOffset(0);
				uOffset2 = target.getUOffset(0);
			    }else {
				ub1 = bound.getUB(0);
				ub2 = target.getUB(0);
				uOffset1 = bound.getUOffset(0);
				uOffset2 = target.getUOffset(0);
			    }
			    select = findMaxValue(bound.getLB(0), target.getLB(0), bound.getLOffset(0), target.getLOffset(0),set);
			    if(select == 1 || select == 0) {
				lb1 = bound.getLB(0);
				lb2 = bound.getLB(0);
				lOffset1 = bound.getLOffset(0);
				lOffset2 = bound.getLOffset(0);
			    }else if(select == 2){
				lb1 = target.getLB(0);
				lb2 = target.getLB(0);
				lOffset1 = target.getLOffset(0);
				lOffset2 = target.getLOffset(0);
			    }else {
				lb1 = bound.getLB(0);
				lb2 = target.getLB(0);
				lOffset1 = bound.getLOffset(0);
				lOffset2 = target.getLOffset(0);
			    }
			    insertCheckBound(IN[bpc], pred.getOperand(0), ub1, lb1, uOffset1, lOffset1);
			    insertCheckBound(IN[bpc], pred.getOperand(1), ub2, lb2, uOffset2, lOffset2);
			}
		    }
		    break;
		case OpcodeConst.opc_if_icmplt:
		    {
			if(IN[pred.getBpc()] == null) break;
			JavaVariable big, small;
			int select, uOffset, lOffset;
			if(pred.getBranchTarget().getBpc() == bpc) {
			    big = pred.getOperand(1);
			    small = pred.getOperand(0);
			    lOffset = 1;
			    uOffset = -1;
			}else {
			    big = pred.getOperand(0);
			    small = pred.getOperand(1);
			    lOffset = 0;
			    uOffset = 0;
			}

			JavaVariable ub, lb;
			IndexBound bigBound = (IndexBound)IN[pred.getBpc()].get(big.toString());
			IndexBound smallBound = (IndexBound)IN[pred.getBpc()].get(small.toString());
			if(bigBound == null || smallBound == null) break;
			select =  findMinValue(bigBound.getUB(0), smallBound.getUB(0), bigBound.getUOffset(0) + uOffset, smallBound.getUOffset(0),set);
			if(select <= 1) {
			    ub = bigBound.getUB(0);
			    uOffset += bigBound.getUOffset(0);
			}else {
			    ub = smallBound.getUB(0);
			    uOffset = smallBound.getUOffset(0);
			}
			insertCheckBound(IN[bpc], small, ub, smallBound.getLB(0), uOffset, smallBound.getLOffset(0));

			select = findMaxValue(bigBound.getLB(0), smallBound.getLB(0), bigBound.getLOffset(0), smallBound.getLOffset(0) + lOffset,set);

			if(select <= 1) {
			    lb = bigBound.getUB(0);
			    lOffset = bigBound.getUOffset(0);
			}else { 
			    lb = smallBound.getUB(0);
			    lOffset += smallBound.getUOffset(0);
			}
			insertCheckBound(IN[bpc], big, bigBound.getUB(0), lb, bigBound.getUOffset(0), lOffset);
		    }
		    break;
		case OpcodeConst.opc_if_icmpge:
		    {
			if(IN[pred.getBpc()] == null) break;
			JavaVariable big, small;
			int select, uOffset, lOffset;
			if(pred.getBranchTarget().getBpc() == bpc) {
			    small = pred.getOperand(1);
			    big = pred.getOperand(0);
			    uOffset = 0;
			    lOffset = 0;
			}else {
			    small = pred.getOperand(0);
			    big = pred.getOperand(1);
			    lOffset = 1;
			    uOffset = -1;
			}

			JavaVariable ub, lb;
			IndexBound bigBound = (IndexBound)IN[pred.getBpc()].get(big.toString());
			IndexBound smallBound = (IndexBound)IN[pred.getBpc()].get(small.toString());
			if(bigBound == null || smallBound == null) break;
			select =  findMinValue(bigBound.getUB(0), smallBound.getUB(0), bigBound.getUOffset(0) + uOffset, smallBound.getUOffset(0),set);
			if(select <= 1) {
			    ub = bigBound.getUB(0);
			    uOffset += bigBound.getUOffset(0);
			}else {
			    ub = smallBound.getUB(0);
			    uOffset = smallBound.getUOffset(0);
			}
			insertCheckBound(IN[bpc], small, ub, smallBound.getLB(0), uOffset, smallBound.getLOffset(0));

			select = findMaxValue(bigBound.getLB(0), smallBound.getLB(0), bigBound.getLOffset(0), smallBound.getLOffset(0) + lOffset,set);

			if(select <= 1) {
			    lb = bigBound.getUB(0);
			    lOffset = bigBound.getUOffset(0);
			}else { 
			    lb = smallBound.getUB(0);
			    lOffset += smallBound.getUOffset(0);
			}
			insertCheckBound(IN[bpc], big, bigBound.getUB(0), lb, bigBound.getUOffset(0), lOffset);
		    }
		    break;

		case OpcodeConst.opc_if_icmpgt:
		    {
			if(IN[pred.getBpc()] == null) break;
			JavaVariable big, small;
			int select, uOffset, lOffset;
			if(pred.getBranchTarget().getBpc() == bpc) {
			    small = pred.getOperand(1);
			    big = pred.getOperand(0);
			    lOffset = 1;
			    uOffset = -1;
			}else {
			    small = pred.getOperand(0);
			    big = pred.getOperand(1);
			    uOffset = 0;
			    lOffset = 0;
			}

			JavaVariable ub, lb;
			IndexBound bigBound = (IndexBound)IN[pred.getBpc()].get(big.toString());
			IndexBound smallBound = (IndexBound)IN[pred.getBpc()].get(small.toString());
			if(bigBound == null || smallBound == null) break;
			select =  findMinValue(bigBound.getUB(0), smallBound.getUB(0), bigBound.getUOffset(0)+uOffset, smallBound.getUOffset(0),set);
			if(select <= 1) {
			    ub = bigBound.getUB(0);
			    uOffset += bigBound.getUOffset(0);
			}else {
			    ub = smallBound.getUB(0);
			    uOffset = smallBound.getUOffset(0);
			}
			insertCheckBound(IN[bpc], small, ub, smallBound.getLB(0), uOffset, smallBound.getLOffset(0));

			select = findMaxValue(bigBound.getLB(0), smallBound.getLB(0), bigBound.getLOffset(0), smallBound.getLOffset(0)+lOffset,set);

			if(select <= 1) {
			    lb = bigBound.getUB(0);
			    lOffset = bigBound.getUOffset(0);
			}else { 
			    lb = smallBound.getUB(0);
			    lOffset += smallBound.getUOffset(0);
			}
			insertCheckBound(IN[bpc], big, bigBound.getUB(0), lb, bigBound.getUOffset(0), lOffset);

		    }
		    break;
		case OpcodeConst.opc_if_icmple:
		    {
			if(IN[pred.getBpc()] == null) break;
			JavaVariable big, small;
			int select, uOffset, lOffset;
			if(pred.getBranchTarget().getBpc() == bpc) {
			    big = pred.getOperand(1);
			    small = pred.getOperand(0);
			    uOffset = 0;
			    lOffset = 0;
			}else {
			    big = pred.getOperand(0);
			    small= pred.getOperand(1);
			    lOffset = 1;
			    uOffset = -1;
			}

			JavaVariable ub, lb;
			IndexBound bigBound = (IndexBound)IN[pred.getBpc()].get(big.toString());
			IndexBound smallBound = (IndexBound)IN[pred.getBpc()].get(small.toString());
			if(bigBound == null || smallBound == null) break;
			select =  findMinValue(bigBound.getUB(0), smallBound.getUB(0), bigBound.getUOffset(0)+uOffset, smallBound.getUOffset(0),set);
			if(select <= 1) {
			    ub = bigBound.getUB(0);
			    uOffset += bigBound.getUOffset(0);
			}else {
			    ub = smallBound.getUB(0);
			    uOffset = smallBound.getUOffset(0);
			}
			insertCheckBound(IN[bpc], small, ub, smallBound.getLB(0), uOffset, smallBound.getLOffset(0));

			select = findMaxValue(bigBound.getLB(0), smallBound.getLB(0), bigBound.getLOffset(0), smallBound.getLOffset(0)+lOffset,set);

			if(select <= 1) {
			    lb = bigBound.getUB(0);
			    lOffset = bigBound.getUOffset(0);
			}else { 
			    lb = smallBound.getUB(0);
			    lOffset += smallBound.getUOffset(0);
			}
			insertCheckBound(IN[bpc], big, bigBound.getUB(0), lb, bigBound.getUOffset(0), lOffset);
		    }

		    break;
	    }
	}
    }
    private void solveOutSet(IR ir, Hashtable[] IN, Hashtable[] OUT) {
	int bpc = ir.getBpc();
	OUT[bpc] = new Hashtable();
	int handlerPC = exceptionHandler.findHandler(ir.getOriginalBpc(), 0);
	for(int i = 0 ; i < ir.getSuccNum() ; i++) {
	    if(handlerPC == exceptionHandler.findHandler(ir.getSucc(i).getBpc(), 0)) {
		Hashtable succ;
		if(IN[ir.getSucc(i).getBpc()] == null) {
		    succ = new Hashtable();
		}else {
		    succ = new Hashtable(IN[ir.getSucc(i).getBpc()]);
		}
		Enumeration succKeys = succ.keys();
		while(succKeys.hasMoreElements()) {
		    String succKey = succKeys.nextElement().toString();
		    if(OUT[bpc].get(succKey) == null) {
			OUT[bpc].put(succKey, new IndexBound((IndexBound)succ.get(succKey)));
		    }else {
			int select, uOffset, lOffset;
			JavaVariable ub, lb;
			IndexBound succBound = (IndexBound)succ.get(succKey);
			IndexBound outBound = (IndexBound)OUT[bpc].get(succKey);
			select =  findMaxValue(succBound.getUB(0), outBound.getUB(0), succBound.getUOffset(0), outBound.getUOffset(0),OUT[bpc] );
			if(select == 1 || select == 0) {
			    ub = succBound.getUB(0);
			    uOffset = succBound.getUOffset(0);
			}else if(select == 2){
			    ub = outBound.getUB(0);
			    uOffset = outBound.getUOffset(0);
			}else {
			    ub = null;
			    uOffset = 0;
			}
			select = findMinValue(succBound.getLB(0), outBound.getLB(0), succBound.getLOffset(0), outBound.getLOffset(0), OUT[bpc]);
			if(select == 1 || select == 0) {
			    lb = succBound.getLB(0);
			    lOffset = succBound.getLOffset(0);
			}else if(select == 2){
			    lb = outBound.getLB(0);
			    lOffset = outBound.getLOffset(0);
			}else {
			    lb = null;
			    lOffset = 0;
			}
			OUT[bpc].put(succKey, new IndexBound(ub, lb, uOffset, lOffset));
		    }
		}
	    }
	}
    }
    private void solveInSet(IR ir, Hashtable[] IN, Hashtable[] OUT) {
	int bpc = ir.getBpc();
	IN[bpc] = new Hashtable();
	if(ir.getPredNum() > 0) {
	    Hashtable pred;
	    if(OUT[ir.getPred(0).getBpc()] == null) {
		pred = new Hashtable();
	    }else {
		pred = new Hashtable(OUT[ir.getPred(0).getBpc()]);
	    }
	    IN[bpc] = new Hashtable(pred);
	}
	for(int i = 1 ; i < ir.getPredNum() ; i++) {
	    Hashtable pred;
	    if(OUT[ir.getPred(i).getBpc()] == null) {
		pred = new Hashtable();
	    }else {
		pred = new Hashtable(OUT[ir.getPred(i).getBpc()]);
	    }
	    Enumeration keys = IN[bpc].keys();
	    while(keys.hasMoreElements()) {
		String key = keys.nextElement().toString();
		if(pred.get(key) == null) {
		    IN[bpc].remove(key);
		}else {
		    int select, uOffset, lOffset;
		    JavaVariable ub, lb;
		    IndexBound predBound = (IndexBound)pred.get(key);
		    IndexBound inBound = (IndexBound)IN[ir.getBpc()].get(key);
		    select =  findMaxValue(predBound.getUB(0), inBound.getUB(0), predBound.getUOffset(0), inBound.getUOffset(0), IN[bpc]);
		    if(select == 1 || select == 0) {
			ub = predBound.getUB(0);
			uOffset = predBound.getUOffset(0);
		    }else if(select == 2){
			ub = inBound.getUB(0);
			uOffset = inBound.getUOffset(0);
		    }else {
			ub = null;
			uOffset = 0;
		    }
		    select = findMinValue(predBound.getLB(0), inBound.getLB(0), predBound.getLOffset(0), inBound.getLOffset(0), IN[bpc]);
		    if(select == 1 || select == 0) {
			lb = predBound.getLB(0);
			lOffset = predBound.getLOffset(0);
		    }else if(select == 2){
			lb = inBound.getLB(0);
			lOffset = inBound.getLOffset(0);
		    }else {
			lb = null;
			lOffset = 0;
		    }
		    IN[bpc].put(key, new IndexBound(ub, lb, uOffset, lOffset));
		}
	    }
	}   
    }
    private boolean checkChange(Hashtable set1, Hashtable set2) {
	if(set1 == null && set2 == null) return false;
	if(set1 == null ) { 
	    return true; 
	}
	if(set1.size() != set2.size()) { 
	    return true;
	}
	Enumeration keys = set1.keys();
	while(keys.hasMoreElements()) {
	    String key = keys.nextElement().toString();
	    if(set2.get(key) == null) { 
		return true;
	    }else {
		String bound1 = ((IndexBound)set1.get(key)).toString();
		String bound2 = ((IndexBound)set2.get(key)).toString();
		if(!bound1.equals(bound2)) { 
		    return true;
		}
	    }
	}
	return false;
    }
    private void printDebugInfo(Hashtable hash) {
	if(hash == null) return;
	Enumeration enu = hash.keys();
	while(enu.hasMoreElements()) {
	    String key = enu.nextElement().toString();
	    IndexBound bound = (IndexBound)hash.get(key);
	    System.err.println("BoundDebug : " +  key + "  :  "  + bound.toString());
	}
    }
}
