package aotc.optimizer;

import java.util.*;
import aotc.cfg.*;
import aotc.ir.*;
import aotc.translation.*;
import opcodeconsts.*;
import aotc.share.*;
import aotc.*;

public class LoopInvariantFinder {
    private CFG cfg;
    private int codeSize;
    public LoopInvariantFinder(CFG cfg) {
	this.cfg = cfg;
	codeSize = cfg.getCodeSize();
    }
    public void find() {
	TreeSet changeNodes = new TreeSet();
	Iterator it;
	it = cfg.iterator();
	
	while(it.hasNext()) {
	    changeNodes.add(it.next());
	}

	while(changeNodes.size() > 0) {
	    IR ir = (IR)changeNodes.first();
	    changeNodes.remove(ir);
	    
	    if( ir.hasAttribute(IRAttribute.LOOP_HEADER)) {
		
		int loopHeaderBpc = ir.getBpc();
		
		BitSet[] IN = new BitSet[codeSize];
		BitSet[] OUT = new BitSet[codeSize];

		TreeSet nodesInLoop = new TreeSet();
		TreeSet newNodes = new TreeSet();
		nodesInLoop.add(ir);
		newNodes.add(ir);
		
		while(newNodes.size() > 0) {
		    ir = (IR)newNodes.first();
		    newNodes.remove(ir);
		    for(int i = 0 ; i < ir.getSuccNum() ; i++) {
			if(ir.getSucc(i).isInLoop(loopHeaderBpc)) {
			    if(nodesInLoop.add(ir.getSucc(i))) {
				newNodes.add(ir.getSucc(i));
			    }
			}
		    }
		}
		cfg.getIRAtBpc(loopHeaderBpc).setLoopSize(nodesInLoop.size());
		
		TreeSet nodes = new TreeSet(nodesInLoop);
		while(nodes.size() > 0) {
		    
		    ir = (IR)nodes.first();
		    nodes.remove(ir);
		    int bpc = ir.getBpc();
		    BitSet oldOUT = null;
		    if(OUT[bpc] != null) {
			oldOUT = (BitSet)OUT[bpc].clone();
		    }
		    IN[bpc] = new BitSet(codeSize);
		    for( int i = 0 ; i < ir.getPredNum() ; i++ ) {
			if(ir.getPred(i).isInLoop(loopHeaderBpc) || ir.getPred(i).getBpc() == loopHeaderBpc) {
			    if(OUT[ir.getPred(i).getBpc()] != null) {
				IN[bpc].or(OUT[ir.getPred(i).getBpc()]);
			    }
			}
		    }
		    OUT[bpc] = (BitSet)IN[bpc].clone();
		    
		    if(ir.hasAttribute(IRAttribute.ELIMINATED)) {
			if(!OUT[bpc].equals(oldOUT)) {
			    for(int i = 0 ; i < ir.getSuccNum() ; i++) {
				if(ir.getSucc(i).isInLoop(loopHeaderBpc)) {
				    nodes.add(ir.getSucc(i));
				}
			    }
			}
			continue;
		    }

		    switch(ir.getShortOpcode()) {
			/*
			case OpcodeConst.opc_aconst_null:
			case OpcodeConst.opc_iconst_m1: case OpcodeConst.opc_iconst_0:
			case OpcodeConst.opc_iconst_1:  case OpcodeConst.opc_iconst_2:
			case OpcodeConst.opc_iconst_3:  case OpcodeConst.opc_iconst_4:
			case OpcodeConst.opc_iconst_5:  case OpcodeConst.opc_lconst_0:
			case OpcodeConst.opc_lconst_1:  case OpcodeConst.opc_fconst_0:
			case OpcodeConst.opc_fconst_1:  case OpcodeConst.opc_fconst_2:
			case OpcodeConst.opc_dconst_0:  case OpcodeConst.opc_dconst_1:
			case OpcodeConst.opc_bipush:    case OpcodeConst.opc_sipush:
			
			case OpcodeConst.opc_aldc_ind_quick:
			case OpcodeConst.opc_aldc_ind_w_quick:
			case OpcodeConst.opc_aldc_quick:
			case OpcodeConst.opc_aldc_w_quick:
			case OpcodeConst.opc_ldc_quick:
			case OpcodeConst.opc_ldc_w_quick:
			case OpcodeConst.opc_ldc2_w_quick:
			    break;
			*/
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
			{
			    if(!isLoopInvariant(ir, loopHeaderBpc, 0, IN, OUT)) {
				OUT[bpc].set(bpc);
			    }
			    break;
			}
			case OpcodeConst.opc_iaload:
			case OpcodeConst.opc_laload:
			case OpcodeConst.opc_faload:
			case OpcodeConst.opc_daload:
			case OpcodeConst.opc_aaload:
			case OpcodeConst.opc_baload:
			case OpcodeConst.opc_caload:
			case OpcodeConst.opc_saload:
			    OUT[bpc].set(bpc);
			    break;
			case OpcodeConst.opc_iastore:
			case OpcodeConst.opc_lastore:
			case OpcodeConst.opc_fastore:
			case OpcodeConst.opc_dastore:
			case OpcodeConst.opc_aastore:
			case OpcodeConst.opc_bastore:
			case OpcodeConst.opc_castore:
			case OpcodeConst.opc_sastore:
			    break;
			case OpcodeConst.opc_dup_x1:    case OpcodeConst.opc_dup_x2:
			case OpcodeConst.opc_dup2:      case OpcodeConst.opc_dup2_x1:
			case OpcodeConst.opc_dup2_x2:
			    new aotc.share.Assert(false, "DUP");
			    break;
			case OpcodeConst.opc_swap:
			    OUT[bpc].set(bpc);
			    break;
			case OpcodeConst.opc_frem:
			case OpcodeConst.opc_drem:
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
			
			case OpcodeConst.opc_lcmp:
			case OpcodeConst.opc_fcmpl:
			case OpcodeConst.opc_dcmpl:
			case OpcodeConst.opc_fcmpg:
			case OpcodeConst.opc_dcmpg:
			
			case OpcodeConst.opc_if_icmpeq: case OpcodeConst.opc_if_icmpne:
			case OpcodeConst.opc_if_icmplt: case OpcodeConst.opc_if_icmpge:
			case OpcodeConst.opc_if_icmpgt: case OpcodeConst.opc_if_icmple:
			case OpcodeConst.opc_if_acmpeq: case OpcodeConst.opc_if_acmpne:

			case OpcodeConst.opc_instanceof_quick:
			{
			    if(!isLoopInvariant(ir, loopHeaderBpc,0, IN, OUT) || !isLoopInvariant(ir,loopHeaderBpc, 1, IN, OUT)) {
				OUT[bpc].set(bpc);
			    }
			    break;
			}
			case OpcodeConst.opc_iinc:
			case OpcodeConst.opc_ineg:  case OpcodeConst.opc_lneg:
			case OpcodeConst.opc_fneg:  case OpcodeConst.opc_dneg:
			    OUT[bpc].set(bpc);
			    break;
			
			case OpcodeConst.opc_ireturn:
			case OpcodeConst.opc_lreturn:
			case OpcodeConst.opc_freturn:
			case OpcodeConst.opc_dreturn:
			case OpcodeConst.opc_areturn:
			case OpcodeConst.opc_return:
			    if(ir.getNumOfOperands() == 0 ) break;
			case OpcodeConst.opc_i2l: case OpcodeConst.opc_i2f: case OpcodeConst.opc_i2d:
			case OpcodeConst.opc_l2i: case OpcodeConst.opc_l2f: case OpcodeConst.opc_l2d:
			case OpcodeConst.opc_f2i: case OpcodeConst.opc_f2l: case OpcodeConst.opc_f2d:
			case OpcodeConst.opc_d2i: case OpcodeConst.opc_d2l: case OpcodeConst.opc_d2f:
			case OpcodeConst.opc_i2b: case OpcodeConst.opc_i2c: case OpcodeConst.opc_i2s:
			
			case OpcodeConst.opc_tableswitch:
			case OpcodeConst.opc_lookupswitch:
			
			case OpcodeConst.opc_ifeq: case OpcodeConst.opc_ifne:
			case OpcodeConst.opc_iflt: case OpcodeConst.opc_ifge:
			case OpcodeConst.opc_ifgt: case OpcodeConst.opc_ifle:
			case OpcodeConst.opc_ifnull:    case OpcodeConst.opc_ifnonnull:
			case OpcodeConst.opc_nonnull_quick:
			
			case OpcodeConst.opc_arraylength:
			case OpcodeConst.opc_ret:

			{
			    if(!isLoopInvariant(ir,loopHeaderBpc, 0, IN, OUT)) {
				OUT[bpc].set(bpc);
			    }
			    break;
			}
			/*
			case OpcodeConst.opc_goto:  case OpcodeConst.opc_goto_w:
			case OpcodeConst.opc_jsr:   case OpcodeConst.opc_jsr_w:
			    break;
			*/
			case OpcodeConst.opc_newarray:
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
			    OUT[bpc].set(bpc);
			    break;
			/*
			case OpcodeConst.opc_checkcast_quick:
			    break;
			 */
			case OpcodeConst.opc_agetstatic_quick:
			case OpcodeConst.opc_getstatic_quick:
			case OpcodeConst.opc_agetstatic_checkinit_quick:
			case OpcodeConst.opc_getstatic_checkinit_quick:
			case OpcodeConst.opc_getstatic2_quick:
			case OpcodeConst.opc_getstatic2_checkinit_quick:
			    OUT[bpc].set(bpc);
			    break;
			case OpcodeConst.opc_aputstatic_quick:
			case OpcodeConst.opc_putstatic_quick:
			case OpcodeConst.opc_aputstatic_checkinit_quick:
			case OpcodeConst.opc_putstatic_checkinit_quick:
			case OpcodeConst.opc_putstatic2_quick:
			case OpcodeConst.opc_putstatic2_checkinit_quick:
			    OUT[bpc].set(bpc);
			    break;
			case OpcodeConst.opc_getfield_quick:
			case OpcodeConst.opc_getfield2_quick:
			case OpcodeConst.opc_agetfield_quick:
			case OpcodeConst.opc_getfield_quick_w:
			    OUT[bpc].set(bpc);
			    break;
			case OpcodeConst.opc_putfield_quick:
			case OpcodeConst.opc_putfield2_quick:
			case OpcodeConst.opc_aputfield_quick:
			case OpcodeConst.opc_putfield_quick_w:
			    OUT[bpc].set(bpc);
			    break;
			case OpcodeConst.opc_new_quick:
			case OpcodeConst.opc_new_checkinit_quick:
			case OpcodeConst.opc_anewarray_quick:
			case OpcodeConst.opc_multianewarray_quick:
			    OUT[bpc].set(bpc);
			    break;
		    }

		    if(!OUT[bpc].equals(oldOUT)) {
			for(int i = 0 ; i < ir.getSuccNum() ; i++) {
			    if(ir.getSucc(i).isInLoop(loopHeaderBpc) || ir.getSucc(i).getBpc() == loopHeaderBpc	) {
				nodes.add(ir.getSucc(i));
			    }
			}
		    }
		}
		it = nodesInLoop.iterator();
		BitSet result = new BitSet(codeSize);
		while(it.hasNext()) {
		    int bpc = ((IR)it.next()).getBpc();
		    if(OUT[bpc] != null) {
			result.or(OUT[bpc]);
		    }
		}
		cfg.getIRAtBpc(loopHeaderBpc).setInvariantCheckSet(result);
	    }
	}
    }
    public void doElimination() {
	//if(AOTCWriter.PROFILE_LOOPPEELING) return;
	Iterator it;
	it = cfg.iterator();
	while(it.hasNext()) {
	    IR ir = (IR)it.next();
	    int bpc = ir.getBpc();
	    for(int i = 0 ; i < ir.getLoopHeaderNum() ; i++) {
		int loopHeaderBpc = ir.getLoopHeader(i);
		IR loopHeaderIR = cfg.getIRAtBpc(loopHeaderBpc);
		if(loopHeaderIR.isLoopPeeling(loopHeaderBpc) && checkTailDomination(loopHeaderBpc, ir, cfg)) {
		    switch(ir.getShortOpcode()) {
			case OpcodeConst.opc_idiv: case OpcodeConst.opc_ldiv:
			case OpcodeConst.opc_irem: case OpcodeConst.opc_lrem:
			    if(isLoopInvariant(loopHeaderBpc, ir, ir.getOperand(1), cfg)) {
				cfg.getIRAtBpc(bpc).setAttribute(IRAttribute.ARITHMETICCHECKELIMINATED);
				cfg.getIRAtBpc(bpc).addDebugInfoString("Arithmetic Check Eliminated By Loop Peeling");
			    }
			    break;
			case OpcodeConst.opc_iaload:
			case OpcodeConst.opc_laload:
			case OpcodeConst.opc_faload:
			case OpcodeConst.opc_daload:
			case OpcodeConst.opc_aaload:
			case OpcodeConst.opc_baload:
			case OpcodeConst.opc_caload:
			case OpcodeConst.opc_saload:
			    if(isLoopInvariant(loopHeaderBpc, ir, ir.getOperand(0), cfg)) {
				cfg.getIRAtBpc(bpc).setAttribute(IRAttribute.NULLCHECKELIMINATED);
				cfg.getIRAtBpc(bpc).addDebugInfoString("Null Check Eliminated By Loop Peeling");
				if(isLoopInvariant(loopHeaderBpc, ir, ir.getOperand(1), cfg)) {
				    cfg.getIRAtBpc(bpc).setAttribute(IRAttribute.ARRAYBOUNDCHECKELIMINATED);
				    cfg.getIRAtBpc(bpc).addDebugInfoString("ArrayBound Check Eliminated By Loop Peeling");
				}
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
			    if(isLoopInvariant(loopHeaderBpc, ir, ir.getOperand(0), cfg)) {
				cfg.getIRAtBpc(bpc).setAttribute(IRAttribute.NULLCHECKELIMINATED);
				cfg.getIRAtBpc(bpc).addDebugInfoString("Null Check Eliminated By Loop Peeling");
				if(isLoopInvariant(loopHeaderBpc, ir, ir.getOperand(2), cfg)) {
				    cfg.getIRAtBpc(bpc).setAttribute(IRAttribute.ARRAYSTORECHECKELIMINATED);
				    cfg.getIRAtBpc(bpc).addDebugInfoString("ArrayStore Check Eliminated By Loop Peeling");
				}
				if(isLoopInvariant(loopHeaderBpc, ir, ir.getOperand(1), cfg)) {
				    cfg.getIRAtBpc(bpc).setAttribute(IRAttribute.ARRAYBOUNDCHECKELIMINATED);
				    cfg.getIRAtBpc(bpc).addDebugInfoString("ArrayBound Check Eliminated By Loop Peeling");
				}
			    }
			    break;
			case OpcodeConst.opc_checkcast:
			    if(isLoopInvariant(loopHeaderBpc, ir, ir.getOperand(0), cfg)) {
				cfg.getIRAtBpc(bpc).addDebugInfoString("ClassCast Check Eliminated By Loop Peeling");
				cfg.getIRAtBpc(bpc).setAttribute(IRAttribute.CLASSCASTCHECKELIMINATED);
			    }
			    break;
			case OpcodeConst.opc_newarray:
			    if(isLoopInvariant(loopHeaderBpc, ir, ir.getOperand(1), cfg)) {
				cfg.getIRAtBpc(bpc).addDebugInfoString("ArrayNegativeSize Check Eliminated By Loop Peeling");
				cfg.getIRAtBpc(bpc).setAttribute(IRAttribute.ARRAYNEGATIVESIZECHECKELIMINATED);
			    }
			    break;
			case OpcodeConst.opc_anewarray_quick:
			    if(isLoopInvariant(loopHeaderBpc, ir, ir.getOperand(0), cfg)) {
				cfg.getIRAtBpc(bpc).addDebugInfoString("ArrayNegativeSize Check Eliminated By Loop Peeling");
				cfg.getIRAtBpc(bpc).setAttribute(IRAttribute.ARRAYNEGATIVESIZECHECKELIMINATED);
			    }
			    break;
			case OpcodeConst.opc_getfield_quick:
			case OpcodeConst.opc_getfield2_quick:
			case OpcodeConst.opc_agetfield_quick:
			case OpcodeConst.opc_getfield_quick_w:
			case OpcodeConst.opc_putfield_quick:
			case OpcodeConst.opc_putfield2_quick:
			case OpcodeConst.opc_aputfield_quick:
			case OpcodeConst.opc_putfield_quick_w:
			case OpcodeConst.opc_monitorenter:
			case OpcodeConst.opc_monitorexit:
			case OpcodeConst.opc_arraylength:
			case OpcodeConst.opc_invokevirtual_quick_w:
			case OpcodeConst.opc_invokenonvirtual_quick:
			case OpcodeConst.opc_invokevirtual_quick:
			case OpcodeConst.opc_ainvokevirtual_quick:
			case OpcodeConst.opc_dinvokevirtual_quick:
			case OpcodeConst.opc_vinvokevirtual_quick:
			case OpcodeConst.opc_invokevirtualobject_quick:
			case OpcodeConst.opc_invokeinterface_quick:
			    if(isLoopInvariant(loopHeaderBpc, ir, ir.getOperand(0), cfg)) {
				cfg.getIRAtBpc(bpc).addDebugInfoString("Null Check Eliminated By Loop Peeling");
				cfg.getIRAtBpc(bpc).setAttribute(IRAttribute.NULLCHECKELIMINATED);
			    }
			    break;
		    }
		}
	    }
	}
    }
    public boolean isLoopInvariant(IR ir, int loopHeaderBpc, int index, BitSet[] IN, BitSet[] OUT) {
	int defCount = 0;
	boolean oneVariantVar = false;
	int bpc = ir.getBpc();
	for(int i = 0 ; i < ir.getNumOfDefs(index) ; i++) {
	    int defBpc = ir.getDefOfOperand(index, i);
	    IR defIR = cfg.getIRAtBpc(defBpc);
	    if(defIR.isInLoop(loopHeaderBpc)) {
		defCount++;
		if(!IN[bpc].get(defBpc)) {
		    oneVariantVar = true;
		}
	    }
	}
	if(oneVariantVar) defCount--;
	if(defCount != 0) {
	    return false;
	}else {
	    return true;
	}
    }
	

    public static boolean isLoopInvariant(int headerBpc, IR ir, JavaVariable index, CFG cfg) {
	if(headerBpc == -1) return false;
	
	IR headerIR = cfg.getIRAtBpc(headerBpc);
	BitSet headerSet = headerIR.getInvariantCheckSet();
	if(headerSet == null) return false;
	int defCount = 0;
	boolean oneVariantVar = false;
	for(int i = 0 ; i < ir.getNumOfDefs(index) ; i++) {
	    int defBpc = ir.getDefOfOperand(index, i);
	    IR defIR = cfg.getIRAtBpc(defBpc);
	    boolean inLoop = defIR.isInLoop(headerBpc) || defIR.getBpc() == headerBpc;
	    if(inLoop){
		defCount++;
		if(!headerSet.get(defBpc)) {
		    oneVariantVar = true;
		}
	    }
	}
	if(oneVariantVar) defCount--;
	
	if(defCount == 0) return true;
	return false;
    }
    public static boolean checkTailDomination(int headerBpc, IR ir, CFG cfg) {
	// check Dominant
	if(headerBpc == -1) return false;
	ExceptionHandler handler = new ExceptionHandler(cfg, null, null, cfg.getMethodInfo().exceptionTable);
	int handlerPc = handler.findHandler(ir.getOriginalBpc(), 0);
	if(handlerPc != -1) {
	    IR handlerIR = cfg.getIRAtBpc(handlerPc);
	    if(handlerIR.isInLoop(headerBpc));
	    return false;
	}
	boolean IN[] = new boolean[cfg.getCodeSize()];
	boolean OUT[] = new boolean[cfg.getCodeSize()];
	IR headerIR = cfg.getIRAtBpc(headerBpc);
	TreeSet newNodes = new TreeSet();
	TreeSet nodesInLoop = new TreeSet();
	
	nodesInLoop.add(ir);
	newNodes.add(ir);
	//initialize
	for(int i = 0 ; i < cfg.getCodeSize() ; i++) {
	    OUT[i] = false;
	}
	while(newNodes.size() > 0) {
	    IR temp = (IR)newNodes.first();
	    newNodes.remove(temp);
	    for(int i = 0 ; i < temp.getSuccNum() ; i++) {
		if(temp.getSucc(i).isInLoop(headerBpc) || temp.getSucc(i).getBpc() == headerBpc) {
		    if(nodesInLoop.add(temp.getSucc(i))) {
			newNodes.add(temp.getSucc(i));
		    }
		}
	    }
	}

	//check Domination
	while(nodesInLoop.size() > 0) {
	    IR traverse = (IR)nodesInLoop.first();
	    nodesInLoop.remove(traverse);
	    int bpc = traverse.getBpc();
	    boolean oldOUT = OUT[bpc];
	    
	    IN[bpc] = true;
	    for(int i = 0 ; i < traverse.getPredNum() ; i++) {
		IR pred = traverse.getPred(i);
		int predBpc = pred.getBpc();
		if(pred.getBpc() == headerBpc || (pred.isInLoop(headerBpc)
		    && !(traverse.hasAttribute(IRAttribute.LOOP_HEADER) && traverse.isInLoop(headerBpc) 
		    && pred.isInLoop(bpc) && pred.hasAttribute(IRAttribute.LOOP_TAIL))))   {
		    IN[bpc] = IN[bpc] & OUT[predBpc];
		}
	    }
	    OUT[bpc] = IN[bpc];
	    if(ir.getBpc() == bpc) {
		OUT[bpc] = true;
	    }
	    if(oldOUT != OUT[bpc]) {
		for(int i = 0 ; i < traverse.getSuccNum() ; i++) {
		    IR succ = traverse.getSucc(i);
		    if(succ.isInLoop(headerBpc) || succ.getBpc() == headerBpc){
			nodesInLoop.add(succ);
		    }
		}
	    }
	}
	return IN[ir.getBpc()];
    }
}
