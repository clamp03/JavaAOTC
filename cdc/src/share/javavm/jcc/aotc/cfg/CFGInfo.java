package aotc.cfg;

import java.util.*;
import components.*;
import opcodeconsts.*;
import aotc.*;
import aotc.ir.*;
import aotc.cfg.*;
import aotc.share.*;
import aotc.optimizer.*;
import aotc.translation.*;
import aotc.profile.*;

public class CFGInfo {
    private boolean prologueGCCheckPoint = false;
    private int loopGCCheckPoint = 0; /* not correct! just check true or false */
    private int gcSafe = 0; /* not correct! just check true or false */
    private int exceptionCheck = 0; /* not correct! just check true or false */
    private int ICells = 0;
    private int calleeICells = 0;
    private boolean constantPool = false; /* AOTCInvoke class will set this value!! */
    private boolean containsAOTCHandler = false;

    private static HashMap cache = new HashMap();
    private static Stack trace = new Stack();
    private static Stack invalidate = null;

    private boolean localFrameIsPushed = true;        
    private boolean CNILocalFrameIsPushed = true;


    private CFG cfg;

    public CFGInfo() {
        prologueGCCheckPoint = true;
        loopGCCheckPoint = 1;
        gcSafe = 1;
        exceptionCheck = 1;
        ICells = 0;
        calleeICells = 0;
        constantPool = false;
    }

    public CFGInfo(CFG cfg) {
        trace.push(cfg.getMethodInfo());
        this.cfg = cfg;
        countStaticInfo();
        countIRInfo();
        this.cfg = null;
        MethodInfo info = (MethodInfo)trace.pop();
        if(trace.size()==0 && invalidate!=null) {
            for(int i=1; i<invalidate.size(); i++) {
                cache.put(invalidate.pop(), null);
            }
            invalidate = null;
        }
        new aotc.share.Assert(info==cfg.getMethodInfo(), "bsh, -----------------------");
        cache.put(cfg.getMethodInfo(), this);
    }

    public static CFGInfo getCFGInfo(MethodInfo methodinfo) {
        if(trace.search(methodinfo) != -1) {
            invalidate = (Stack)trace.clone();
            return null;
        }
        CFGInfo ret = (CFGInfo)cache.get(methodinfo);
        if(ret==null) {
            if(methodinfo.code != null) {
                new CFG(methodinfo, methodinfo.parent);
                return (CFGInfo)cache.get(methodinfo);
            } else {
                return new CFGInfo();
                //return null;
            }
        }
        return ret;
    }

    private void countStaticInfo() {
        MethodInfo methodinfo = cfg.getMethodInfo();
        if(methodinfo.code.length>50) {
            prologueGCCheckPoint = true;
        } else {
            prologueGCCheckPoint = false;
        }
        ICells = cfg.getChangedArgs().size() + cfg.getNumOfLocalICells();
        if(methodinfo.exceptionTable.length>0)
            containsAOTCHandler = true;
    }
    private void countIRInfo() {
        Iterator it = cfg.iterator();
        IR ir;
        short opcode;
        while(it.hasNext()) {
            ir = (IR)it.next();
            if(ir.hasAttribute(IRAttribute.ELIMINATED)) continue;
            opcode = ir.getShortOpcode();

            if((opcodeAttribute[opcode]&ATTR_GC_CHECK)!=0) {
                if(ir.hasAttribute(IRAttribute.LOOP_TAIL) &&
                        ir.getBranchTarget().hasAttribute(IRAttribute.LOOP_HEADER)) {
                    loopGCCheckPoint++;
                }
            }
            if((opcodeAttribute[opcode]&ATTR_GC_SAFE)!=0) {
                gcSafe++;
            }
            if((opcodeAttribute[opcode]&ATTR_NULL_CHECK)!=0) {
                if(!ir.hasAttribute(IRAttribute.NULLCHECKELIMINATED)) {
                    exceptionCheck++;
                }
            }
            if((opcodeAttribute[opcode]&ATTR_EXCEPTION_CHECK)!=0) {
                exceptionCheck++;
            }

            // virtual calls
            if((OpcodeConst.opc_invokevirtual_quick<=opcode &&
                        opcode<=OpcodeConst.opc_invokevirtualobject_quick) ||
                    opcode==OpcodeConst.opc_invokeinterface_quick) {
                gcSafe++;
                exceptionCheck++;

                if (AOTCWriter.USE_CALL_COUNT_INFO) {
                    CallCountTable ct = CallCountAnalyzer.getCallCountTable();
                    if(ct!=null) {
                        Vector calleeProfileList = ct.findCalleeList(cfg.getClassInfo().className,
                                (cfg.getMethodInfo().name.string + cfg.getMethodInfo().type.string),
                                Integer.toString(ir.getOriginalBpc()));
                        for(int i=0; i<calleeProfileList.size(); i++) {
                            CalleeInfo ci = (CalleeInfo)calleeProfileList.elementAt(i);
                            int callCount = ci.getCallCount();
                            MethodInfo me = AOTCInvoke.getMethodInfoFromProfile(ci);
                            if(me==null) continue;
                            CFGInfo calleeInfo = getCFGInfo(me);

                            if(calleeInfo!=null && MethodInliner.inlineTest(me, true, callCount)) {
                                containsAOTCHandler |= calleeInfo.getContainsAOTCHandler();
                                calleeICells = Math.max(calleeICells, calleeInfo.getICells()+calleeInfo.getCalleeICells());
                            }
                        }
                    }
                }
            }

            // non-virtual calls
            if(opcode==OpcodeConst.opc_invokestatic_checkinit_quick ||
                    opcode==OpcodeConst.opc_invokestatic_quick ||
                    opcode==OpcodeConst.opc_invokenonvirtual_quick) {
                MethodInfo me = ir.getCalleeMethodInfo();
                ClassInfo cl = ir.getCalleeClassInfo();
                CFGInfo calleeInfo = getCFGInfo(me);
                int type = AOTCInvoke.checkStaticCalltype(me, cl);
                if(type==AOTCInvoke.CALLTYPE_CNI || type==AOTCInvoke.CALLTYPE_JAVA)
                    setConstantPool();
                if(me != null && !ClassFilter.inList(me.parent.className, me.name.string, me.type.string)) {
                    gcSafe++;
                    exceptionCheck++;
                }

                if(calleeInfo==null) {
                    gcSafe++;
                    exceptionCheck++;
                } else {
                    loopGCCheckPoint += calleeInfo.getGCCheckPoints();
                    gcSafe += calleeInfo.getGcSafe();
                    exceptionCheck += calleeInfo.getExceptionCheck();
                    int callCount = -1;
                    if (AOTCWriter.USE_CALL_COUNT_INFO) {
                        CallCountTable ct = CallCountAnalyzer.getCallCountTable();
                        if(ct!=null) {
                            Vector calleeProfileList = ct.findCalleeList(cfg.getClassInfo().className,
                                    (cfg.getMethodInfo().name.string + cfg.getMethodInfo().type.string),
                                    Integer.toString(ir.getOriginalBpc()));
                            if(calleeProfileList.size()==0)
                                callCount = 0;
                            else
                                callCount = ((CalleeInfo)calleeProfileList.elementAt(0)).getCallCount();
                        }
                    }
                    if(MethodInliner.inlineTest(ir.getCalleeMethodInfo(), false, callCount)) {
                        containsAOTCHandler |= calleeInfo.getContainsAOTCHandler();
                        calleeICells = Math.max(calleeICells, calleeInfo.getICells()+calleeInfo.getCalleeICells());
                        //MethodInfo mi = ir.getCalleeMethodInfo();
                    }
                }
            }
        }
    }

    public boolean getPrologueGCCheckPoint() {
        return prologueGCCheckPoint;
        //return false;
        //return true ;
    }
    public int getGCCheckPoints() {
        int ret = loopGCCheckPoint;
        if(getPrologueGCCheckPoint()) ret++;
        return ret;
    }
    public int getGcSafe() {
        return gcSafe;
    }
    public int getExceptionCheck() {
        return exceptionCheck;
    }
    public int getICells() {
        return ICells;
    }
    public int getCalleeICells() {
        return calleeICells;
    }
    public boolean getConstantPool() {
        return constantPool;
    }
    public void setConstantPool() {
        constantPool = true;
    }
    public boolean getContainsAOTCHandler() {
        return containsAOTCHandler;
    }
    public boolean getLocalFrameIsPushed() {
        return localFrameIsPushed;
    }
    public void setLocalFrameIsPushed(boolean b) {
        localFrameIsPushed = b;
    }
    public boolean getCNILocalFrameIsPushed() {
        return CNILocalFrameIsPushed;
    }
    public void setCNILocalFrameIsPushed(boolean b) {
        CNILocalFrameIsPushed = b;
    }


    public static final int ATTR_GC_CHECK   = 1;
    public static final int ATTR_GC_SAFE    = 2;
    public static final int ATTR_NULL_CHECK = 4;
    public static final int ATTR_EXCEPTION_CHECK = 8; /* not ATTR_NULL_CHECK */
    public static final int opcodeAttribute[] = {
        0,  /*nop*/
        0,  /*aconst_null*/
        0,  /*iconst_m1*/
        0,  /*iconst_0*/
        0,  /*iconst_1*/
        0,  /*iconst_2*/
        0,  /*iconst_3*/
        0,  /*iconst_4*/
        0,  /*iconst_5*/
        0,  /*lconst_0*/
        0,  /*lconst_1*/
        0,  /*fconst_0*/
        0,  /*fconst_1*/
        0,  /*fconst_2*/
        0,  /*dconst_0*/
        0,  /*dconst_1*/
        0,  /*bipush*/
        0,  /*sipush*/
        -1, /*ldc*/
        -1, /*ldc_w*/
        -1, /*ldc2_w*/
        0,  /*iload*/
        0,  /*lload*/
        0,  /*fload*/
        0,  /*dload*/
        0,  /*aload*/
        0,  /*iload_0*/
        0,  /*iload_1*/
        0,  /*iload_2*/
        0,  /*iload_3*/
        0,  /*lload_0*/
        0,  /*lload_1*/
        0,  /*lload_2*/
        0,  /*lload_3*/
        0,  /*fload_0*/
        0,  /*fload_1*/
        0,  /*fload_2*/
        0,  /*fload_3*/
        0,  /*dload_0*/
        0,  /*dload_1*/
        0,  /*dload_2*/
        0,  /*dload_3*/
        0,  /*aload_0*/
        0,  /*aload_1*/
        0,  /*aload_2*/
        0,  /*aload_3*/
        8,  /*iaload*/
        8,  /*laload*/
        8,  /*faload*/
        8,  /*daload*/
        8,  /*aaload*/
        8,  /*baload*/
        8,  /*caload*/
        8,  /*saload*/
        0,  /*istore*/
        0,  /*lstore*/
        0,  /*fstore*/
        0,  /*dstore*/
        0,  /*astore*/
        0,  /*istore_0*/
        0,  /*istore_1*/
        0,  /*istore_2*/
        0,  /*istore_3*/
        0,  /*lstore_0*/
        0,  /*lstore_1*/
        0,  /*lstore_2*/
        0,  /*lstore_3*/
        0,  /*fstore_0*/
        0,  /*fstore_1*/
        0,  /*fstore_2*/
        0,  /*fstore_3*/
        0,  /*dstore_0*/
        0,  /*dstore_1*/
        0,  /*dstore_2*/
        0,  /*dstore_3*/
        0,  /*astore_0*/
        0,  /*astore_1*/
        0,  /*astore_2*/
        0,  /*astore_3*/
        8,  /*iastore*/
        8,  /*lastore*/
        8,  /*fastore*/
        8,  /*dastore*/
        8,  /*aastore*/
        8,  /*bastore*/
        8,  /*castore*/
        8,  /*sastore*/
        0,  /*pop*/
        0,  /*pop2*/
        0,  /*dup*/
        0,  /*dup_x1*/
        0,  /*dup_x2*/
        0,  /*dup2*/
        0,  /*dup2_x1*/
        0,  /*dup2_x2*/
        0,  /*swap*/
        0,  /*iadd*/
        0,  /*ladd*/
        0,  /*fadd*/
        0,  /*dadd*/
        0,  /*isub*/
        0,  /*lsub*/
        0,  /*fsub*/
        0,  /*dsub*/
        0,  /*imul*/
        0,  /*lmul*/
        0,  /*fmul*/
        0,  /*dmul*/
        8,  /*idiv*/
        8,  /*ldiv*/
        0,  /*fdiv*/
        0,  /*ddiv*/
        8,  /*irem*/
        8,  /*lrem*/
        0,  /*frem*/
        0,  /*drem*/
        0,  /*ineg*/
        0,  /*lneg*/
        0,  /*fneg*/
        0,  /*dneg*/
        0,  /*ishl*/
        0,  /*lshl*/
        0,  /*ishr*/
        0,  /*lshr*/
        0,  /*iushr*/
        0,  /*lushr*/
        0,  /*iand*/
        0,  /*land*/
        0,  /*ior*/
        0,  /*lor*/
        0,  /*ixor*/
        0,  /*lxor*/
        0,  /*iinc*/
        0,  /*i2l*/
        0,  /*i2f*/
        0,  /*i2d*/
        0,  /*l2i*/
        0,  /*l2f*/
        0,  /*l2d*/
        0,  /*f2i*/
        0,  /*f2l*/
        0,  /*f2d*/
        0,  /*d2i*/
        0,  /*d2l*/
        0,  /*d2f*/
        0,  /*i2b*/
        0,  /*i2c*/
        0,  /*i2s*/
        0,  /*lcmp*/
        0,  /*fcmpl*/
        0,  /*fcmpg*/
        0,  /*dcmpl*/
        0,  /*dcmpg*/
        1,  /*ifeq*/
        1,  /*ifne*/
        1,  /*iflt*/
        1,  /*ifge*/
        1,  /*ifgt*/
        1,  /*ifle*/
        1,  /*if_icmpeq*/
        1,  /*if_icmpne*/
        1,  /*if_icmplt*/
        1,  /*if_icmpge*/
        1,  /*if_icmpgt*/
        1,  /*if_icmple*/
        1,  /*if_acmpeq*/
        1,  /*if_acmpne*/
        1,  /*goto*/
        0,  /*jsr*/
        0,  /*ret*/
        0,  /*tableswitch*/
        0,  /*lookupswitch*/
        0,  /*ireturn*/
        0,  /*lreturn*/
        0,  /*freturn*/
        0,  /*dreturn*/
        0,  /*areturn*/
        0,  /*return*/
        -1, /*getstatic*/
        -1, /*putstatic*/
        -1, /*getfield*/
        -1, /*putfield*/
        -1, /*invokevirtual*/
        -1, /*invokespecial*/
        -1, /*invokestatic*/
        -1, /*invokeinterface*/
        -1, /*xxxunusedxxx*/
        -1, /*new*/
        10, /*newarray*/
        -1, /*anewarray*/
        4,  /*arraylength*/
        8,  /*athrow*/
        -1, /*checkcast*/
        -1, /*instanceof*/
        6,  /*monitorenter*/
        6,  /*monitorexit*/
        -1, /*wide*/
        -1, /*multianewarray*/
        1,  /*ifnull*/
        1,  /*ifnonnull*/
        1,  /*goto_w*/
        0,  /*jsr_w*/
        -1, /*breakpoint*/
        -1, /*aldc_ind_quick*/
        -1, /*aldc_ind_w_quick*/
        0,  /*aldc_quick*/
        0,  /*ldc_quick*/
        0,  /*aldc_w_quick*/
        0,  /*ldc_w_quick*/
        0,  /*ldc2_w_quick*/
        0,  /*invokestatic_quick*/
        0,  /*invokestatic_checkinit_quick*/
        4,  /*invokevirtual_quick*/
        4,  /*ainvokevirtual_quick*/
        4,  /*dinvokevirtual_quick*/
        4,  /*vinvokevirtual_quick*/
        4,  /*invokevirtual_quick_w*/
        4,  /*invokevirtualobject_quick*/
        4,  /*invokenonvirtual_quick*/
        -1, /*invokesuper_quick*/
        -1, /*invokeignored_quick*/
        4,  /*invokeinterface_quick*/
        8,  /*checkcast_quick*/
        0,  /*instanceof_quick*/
        1,  /*nonnull_quick*/
        -1, /*exittransition*/
        0,  /*agetstatic_quick*/
        0,  /*getstatic_quick*/
        0,  /*getstatic2_quick*/
        0,  /*aputstatic_quick*/
        0,  /*putstatic_quick*/
        0,  /*putstatic2_quick*/
        2,  /*agetstatic_checkinit_quick*/
        2,  /*getstatic_checkinit_quick*/
        2,  /*getstatic2_checkinit_quick*/
        2,  /*aputstatic_checkinit_quick*/
        2,  /*putstatic_checkinit_quick*/
        2,  /*putstatic2_checkinit_quick*/
        4,  /*getfield_quick*/
        4,  /*putfield_quick*/
        4,  /*getfield2_quick*/
        4,  /*putfield2_quick*/
        4,  /*agetfield_quick*/
        4,  /*aputfield_quick*/
        4,  /*getfield_quick_w*/
        4,  /*putfield_quick_w*/
        10, /*new_checkinit_quick*/
        10, /*new_quick*/
        10, /*anewarray_quick*/
        10, /*multianewarray_quick*/
        -1, /*??250*/
        -1, /*??251*/
        -1, /*??252*/
        -1, /*??253*/
        -1, /*software*/
        -1  /*hardware*/
    };
}
