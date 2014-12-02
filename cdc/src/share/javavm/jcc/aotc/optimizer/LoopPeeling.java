package aotc.optimizer;

import java.util.*;
import aotc.cfg.*;
import aotc.ir.*;
import opcodeconsts.*;
import aotc.translation.*;
import aotc.share.*;
import aotc.profile.*;
import components.*;

public class LoopPeeling {
    private CFG cfg;
    private int codeSize;

    public LoopPeeling(CFG cfg) {
	this.cfg = cfg;
	codeSize = cfg.getCodeSize();
    }
    public boolean determineLoopPeeling(int headerBpc) {
	ClassInfo classinfo = cfg.getClassInfo();
	MethodInfo methodinfo = cfg.getMethodInfo();
	LoopInfo loopInfo = LoopCountAnalyzer.findProfileInfo(classinfo.className, methodinfo.name.toString() + methodinfo.type.string, headerBpc);
	if(loopInfo == null) return false;
	return loopInfo.determineLoopPeeling();
    }
    public boolean doLoopPeeling() {
	if(!aotc.AOTCWriter.DO_LOOPPEELING) return false;
	boolean loopPeeling = false;
	if(!LoopCountAnalyzer.loadProfileInfo()) return false;

	Vector loopHeaderList = findLoopHeader();
	for(int i = loopHeaderList.size() - 1 ; i >= 0 ; i--) {
	    TreeSet loopHeaders = new TreeSet((TreeSet)loopHeaderList.elementAt(i));
	    Iterator it = loopHeaders.iterator();
	    while(it.hasNext()) {
		int loopHeaderBpc = ((Integer)it.next()).intValue();
		if(determineLoopPeeling(loopHeaderBpc)) {
		    loopPeeling |= doLoopPeeling(loopHeaderBpc, i);
		}
	    }
	}
	return loopPeeling;
    }
    private boolean doLoopPeeling(int loopHeaderBpc, int depth) {
	Iterator it = cfg.iterator();
	while(it.hasNext()) {
	    IR ir = (IR)it.next();
	    short opcode = ir.getShortOpcode();
	    switch(opcode) {
		case OpcodeConst.opc_lookupswitch:
		case OpcodeConst.opc_tableswitch:
		case OpcodeConst.opc_jsr:
		case OpcodeConst.opc_jsr_w:
		    return false;
	    }
	}

	IR loopHeaderIR = cfg.getIRAtBpc(loopHeaderBpc);
	
	IR newLoopHeaderIR = makeNewIR(loopHeaderIR, loopHeaderBpc);
	Hashtable loopBody = new Hashtable();
	//Hashtable newLoopBody = new Hashtable();
	TreeSet temp = new TreeSet();
	loopBody.put(loopHeaderIR, newLoopHeaderIR);
	temp.add(loopHeaderIR);
	// set succ to pred of loop header
	for(int i = 0 ; i < loopHeaderIR.getPredNum() ; i++) {
	    IR pred = loopHeaderIR.getPred(i);
	    if(!pred.isInLoop(loopHeaderBpc)) {
		loopHeaderIR.removePred(pred);
		i--;
		if(pred.hasAttribute(IRAttribute.LOOP_PREHEADER)) {
		    IR gotoIR;
		    int operands[] = new int[1];
		    int newBpc = cfg.allocateNewBpc();
		    Bytecode gotoBytecode = new Bytecode(newBpc, (byte)OpcodeConst.opc_goto, operands);
		    gotoIR = new IR(gotoBytecode);
		    //set loop heaer
		    for(int j = 0 ; j < pred.getLoopHeaderNum() ; j++) {
			gotoIR.setLoopHeader(pred.getLoopHeader(j));
		    }
		    gotoIR.addPred(pred);
		    gotoIR.addSucc(newLoopHeaderIR);
		    
		    pred.changeSucc(loopHeaderIR, gotoIR);
		    newLoopHeaderIR.changePred(pred, gotoIR);
		    cfg.addNewIR(gotoIR, pred, cfg.ADD_AFTER);
		}else {
		    pred.changeSucc(loopHeaderIR, newLoopHeaderIR);
		}
	    }else {
		newLoopHeaderIR.removePred(pred);
	    }
	}
	// find ir in loop
	while(temp.size() > 0) {
	    IR ir = (IR)temp.first();
	    temp.remove(ir);
	    for( int i = 0 ; i < ir.getSuccNum() ; i++) {
		IR succ = ir.getSucc(i);
		if(succ.isInLoop(loopHeaderBpc)) {
		    if(!loopBody.containsKey(succ)) {
			loopBody.put(succ, makeNewIR(succ, loopHeaderBpc));
			temp.add(succ);
		    }
		}
	    }
	}
	
	    
	// set loopHeader
	Enumeration keys = loopBody.keys();
	while(keys.hasMoreElements()){
	    IR ir = (IR)keys.nextElement();
	    IR newIR = (IR)loopBody.get(ir);
	    for(int i = 0 ; i < ir.getLoopHeaderNum() ; i++) {
		IR loopHeaderOfIR = cfg.getIRAtBpc(ir.getLoopHeader(i));
		if(loopHeaderOfIR.isInLoop(loopHeaderBpc)) {
		    IR newLoopHeaderOfIR = (IR)loopBody.get(loopHeaderOfIR);
		    newIR.removeLoopHeader(ir.getLoopHeader(i));
		    newIR.setLoopHeader(newLoopHeaderOfIR.getBpc());
		}
	    }
	}
	// change loop peeling info
	keys = loopBody.keys();
	while(keys.hasMoreElements()){
	    IR ir = (IR)keys.nextElement();
	    IR newIR = (IR)loopBody.get(ir);
	    for(int i = 0 ; i < ir.getNumLoopPeeling() ; i++) {
		IR loopPeeling = cfg.getIRAtBpc(ir.getLoopPeeling(i));
		IR newLoopPeeling = (IR)loopBody.get(loopPeeling);
		if(newLoopPeeling != null) {
		    newIR.changeLoopPeeling(loopPeeling.getBpc(), newLoopPeeling.getBpc());
		}
	    }
	}
	// set defs and uses
	/*
	keys = loopBody.keys();
	System.out.println("Start:");
	while(keys.hasMoreElements()) {
	    IR ir = (IR)keys.nextElement();
	    IR newIR = (IR)loopBody.get(ir);
	    for(int i = 0 ; i < ir.getNumOfOperands() ; i++) {
		for(int j = 0 ; j < ir.getNumOfDefs(i); j++) {
		    IR def = cfg.getIRAtBpc(ir.getDefOfOperand(i, j));
		    IR newDefs = (IR)loopBody.get(def);
		    if(newDefs != null) {
			newIR.changeDefOfOperand(i, def.getBpc(), newDefs.getBpc());
		    }
		}
	    }
	    for(int i = 0 ; i < ir.getNumOfTargets() ; i++) {
		for(int j = 0 ; j < ir.getNumOfUses(i) ; j++) {
		    IR use = cfg.getIRAtBpc(ir.getUseOfTarget(i, j));
		    IR newUse = (IR)loopBody.get(use);
		    if(newUse != null) {
			newIR.changeUseOfTarget(i, use.getBpc(), newUse.getBpc());
		    }
		}
	    }
	}
	*/
	    	
	// set succ and pred of ir in loop.
	keys = loopBody.keys();
	while(keys.hasMoreElements()) {
	    IR ir = (IR)keys.nextElement();
	    IR newIR = (IR)loopBody.get(ir);
	    for(int i = 0 ; i < ir.getSuccNum() ; i++) {
		IR succ = ir.getSucc(i);
		if(succ.isInLoop(loopHeaderBpc)) {
		    IR newSucc = (IR)loopBody.get(succ);
		    newIR.changeSucc(succ, newSucc);
		    newSucc.changePred(ir, newIR);
		}else {
		    succ.addPred(newIR);
		}
	    }
	}
	
	// add new ir to cfg
	boolean inLoop = false;
	IR loopEndIR = null;
	IR loopOutIR = null;
	IR loopStartIR = null;
	for(int i = 0 ; i < cfg.getNumOfIR() ; i++) {
	    IR ir = cfg.getIRAtIndex(i);
	    IR newIR = (IR)loopBody.get(ir);
	    if(inLoop && loopBody.size() == 0) {
		inLoop = false;
		loopOutIR = ir;
		break;
	    }
	    if(newIR == null && inLoop) {
		if(!ir.hasAttribute(IRAttribute.LOOP_PREHEADER)) {
		    if(loopEndIR != null && (loopEndIR.isSucc(ir))) {
			IR gotoIR;
			int operands[] = new int[1];
			int newBpc = cfg.allocateNewBpc();
			Bytecode gotoBytecode = new Bytecode(newBpc, (byte)OpcodeConst.opc_goto, operands);
			gotoIR = new IR(gotoBytecode);
			for(int j = 0 ; j < ir.getLoopHeaderNum() ; j++) {
			    gotoIR.setLoopHeader(ir.getLoopHeader(j));
			}
			gotoIR.addPred(loopEndIR);
			gotoIR.addSucc(ir);
			ir.changePred(loopEndIR,gotoIR);
			loopEndIR.changeSucc(ir,gotoIR);
			cfg.addNewIR(gotoIR, loopStartIR, cfg.ADD_IN_FRONT_OF);
			i++;
		    }
		    inLoop = false;
		}
	    }
	    if(newIR != null) {
		if(loopStartIR == null) {
		    loopStartIR = ir;
		}
		inLoop = true;
		loopEndIR = newIR;
		newIR.addDebugInfoString("Loop Peeling (Depth : " + depth + ")");
		cfg.addNewIR(newIR, loopStartIR, cfg.ADD_IN_FRONT_OF);
		loopBody.remove(ir);
		i++;
	    }
	}
	
	cfg.getIRAtBpc(loopHeaderBpc).addLoopPeeling(loopHeaderBpc);
	
	// add goto statement to end of loop peeling
	if(loopEndIR != null && (loopEndIR.getShortOpcode() == OpcodeConst.opc_goto || loopEndIR.getShortOpcode() == OpcodeConst.opc_goto_w)) {
	    if(loopEndIR.getBranchTarget().getBpc() == loopStartIR.getBpc()) loopEndIR.setAttribute(IRAttribute.ELIMINATED);
	    return true;
	}
	if(loopOutIR == null) {
	    int returnType = cfg.getReturnType();
	    byte opcode = -1;
	    if(cfg.getReturnType() == TypeInfo.TYPE_VOID) {
		opcode = (byte)OpcodeConst.opc_return;
	    }else {
		new aotc.share.Assert(false, "LOOP PEELING : Cannot add exit code to end of loop peeling ");
	    }
	    
	    Bytecode bytecode = new Bytecode(cfg.allocateNewBpc(), opcode, new int[0]);

	    IR newIR = new IR(bytecode);
	    newIR.addPred(loopEndIR);
	    loopEndIR.addSucc(newIR);
	    
	    for(int i = 0 ; i < loopEndIR.getLoopHeaderNum() ; i++) {
		newIR.setLoopHeader(loopEndIR.getLoopHeader(i));
	    }
	    newIR.addDebugInfoString("Loop Peeling (Depth : " + depth + ")");
	    cfg.addNewIR(newIR, loopStartIR, cfg.ADD_IN_FRONT_OF);
	    cfg.setNeedExitCode(true);
	}else {
	    Bytecode bytecode = new Bytecode(cfg.allocateNewBpc(), (byte)OpcodeConst.opc_goto, new int[0]);
	    IR newIR = new IR(bytecode);
	    
	    newIR.addSucc(loopOutIR);
	    loopOutIR.addPred(newIR);
	    newIR.addPred(loopEndIR);
	    loopEndIR.addSucc(newIR);
	    
	    for(int i = 0 ; i < loopEndIR.getLoopHeaderNum() ; i++) {
		newIR.setLoopHeader(loopEndIR.getLoopHeader(i));
	    }
	    newIR.addDebugInfoString("Loop Peeling (Depth : " + depth + ")");
	    cfg.addNewIR(newIR, loopStartIR, cfg.ADD_IN_FRONT_OF); 
	}
	return true;
    }
    private IR makeNewIR(IR ir, int loopHeaderBpc) {
	IR newIR = new IR(ir);
	newIR.removeLoopHeader(loopHeaderBpc);
	newIR.setBpc(cfg.allocateNewBpc());
	newIR.setOriginalBpc(ir.getOriginalBpc());
	newIR.getBytecode().setBpc(newIR.getBpc());
	newIR.getBytecode().setIR(newIR);
	if(ir.getBpc() == loopHeaderBpc) {
	    newIR.removeAttribute(IRAttribute.LOOP_HEADER);
	}
	if(newIR.getLoopHeaderNum() == 0) {
	    newIR.removeAttribute(IRAttribute.LOOP_BODY);
	}
	if(newIR.hasAttribute(IRAttribute.LOOP_TAIL) &&
		newIR.isPred(cfg.getIRAtBpc(loopHeaderBpc))) {
	    newIR.removeAttribute(IRAttribute.LOOP_TAIL);
	}
	newIR.addDebugInfoString("Original Bpc : " + newIR.getOriginalBpc());
	return newIR;
    }
    private Vector findLoopHeader() {
	Vector loopHeaderList = new Vector();
	Iterator it = cfg.iterator();
	while(it.hasNext()) {
	    IR ir = (IR)it.next();
	    if(ir.hasAttribute(IRAttribute.LOOP_HEADER)) {
		int loopDepth = ir.getLoopHeaderNum();
		for(int i = loopHeaderList.size(); i <= loopDepth; i++) {
		    loopHeaderList.add(new TreeSet());
		}
		TreeSet headers = (TreeSet)loopHeaderList.elementAt(loopDepth);
		headers.add(new Integer(ir.getBpc()));
		loopHeaderList.setElementAt(headers, loopDepth);
	    }
	}
	return loopHeaderList;
    }
}
