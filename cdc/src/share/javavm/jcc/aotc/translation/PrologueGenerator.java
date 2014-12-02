package aotc.translation;

import java.util.*;
import aotc.*;
import aotc.share.*;
import aotc.ir.*;
import aotc.optimizer.*;
import aotc.cfg.*;
import components.*;
import jcc.*;
import vm.*;
import util.*;

import consts.*;

public class PrologueGenerator {

    private TreeSet usedVariables;
    private TreeSet usedICells;
    private TreeSet changedArgs;
    private int returnType;
    private int argumentSize;
    private AOTCPrint out;
    private CFG cfg;

    private int numOfLocalICells;
    private int numOfCalleeICells;
    private int numOfICells;

    // Call Arg - BY Clamp *****
    private int numOfCNIICells;
    private int numOfCNILocalICells;
    // *************************

    private int changedArg_index = -1;
    public String inlinedCB = null;                                           
    public String inlinedSYNC = null;                                         
    public IR inlinedIR = null;        

    private ExceptionHandler exceptionHandler;

    public PrologueGenerator(CFG cfg, AOTCPrint out, ExceptionHandler exceptionHandler, IR inlinedIR) {
        this.cfg = cfg;
        this.out = out;
        this.exceptionHandler = exceptionHandler;
        this.inlinedIR = inlinedIR;
        usedVariables = cfg.getUsedVariables(true);
        usedICells = cfg.getUsedICells();
        returnType = cfg.getReturnType();
        argumentSize = cfg.getMethodInfo().argsSize;
        changedArgs = cfg.getChangedArgs();

        numOfLocalICells = cfg.getCFGInfo().getICells();
        numOfCalleeICells = cfg.getCFGInfo().getCalleeICells();
		numOfICells = numOfLocalICells + numOfCalleeICells;

        // Call Arg - BY Clamp ************************
        numOfCNIICells = numOfICells;
        numOfCNILocalICells = numOfLocalICells;

        if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.CNI_TYPE 
                || AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.MIX_TYPE) {
            MethodInfo methodInfo = cfg.getMethodInfo();
            if(MethodInliner.inline_depth==0) {
                if((methodInfo.access & ClassFileConst.ACC_STATIC) == 0) {
                    numOfCNIICells++;
                    numOfCNILocalICells++;
                }
                Iterator it = usedICells.iterator();
                while (it.hasNext()) {
                    JavaVariable usedICell = (JavaVariable) it.next();
                    if(((usedICell instanceof LocalVariable) && usedICell.getIndex() < argumentSize)) {
                        numOfCNIICells++;
                        numOfCNILocalICells++;
                    }
                }
            }
        }
        // ********************************************** 

        if(MethodInliner.inline_depth==0 && cfg.getCFGInfo().getContainsAOTCHandler()) {
            if(!usedICells.contains(ExceptionHandler.s0_ref)) {
                usedICells.add(ExceptionHandler.s0_ref);
                numOfLocalICells++;
                numOfICells++;
                numOfCNIICells++;
                numOfCNILocalICells++;
            }
        } 

        cfg.getCFGInfo().setLocalFrameIsPushed(false);
        cfg.getCFGInfo().setCNILocalFrameIsPushed(false);
        if(numOfICells > 0) {
            cfg.getCFGInfo().setLocalFrameIsPushed(true);
        }
        if(numOfCNIICells > 0) {
            cfg.getCFGInfo().setCNILocalFrameIsPushed(true);
        }


    }

    public void writePrologue() {
	//out.Printf("int push_try = 0;");
        // Don't change the order of following codes
        declareEnvironmentalVariables();
        declareLocalVariables();
        declareICellVariables();
        declarePreloadedBlocks();
        /*
        if(AOTCWriter.PROFILE_LOOPPEELING) {
            declareProfileVariables();
        }
        */
        // Call Arg - BY Clamp
        declarePushFrameVariables(numOfICells, numOfCNIICells);
        declareSetjmpVariables();
        out.Printf("CVMassert(CVMD_isgcUnsafe(ee));");
        //out.Printf("aotc_call_count++;");
        
        MethodInfo methodInfo = cfg.getMethodInfo();
        if(AOTCWriter.METHOD_ARGUMENT_PASSING != AOTCWriter.CNI_TYPE) {
            if(CFGInfo.getCFGInfo(methodInfo).getPrologueGCCheckPoint()) {
                out.Printf("CVMD_gcSafeCheckPoint(ee, {}, {});");
            }
        }

        if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.MIX_TYPE && MethodInliner.inline_depth == 0) {
            out.Printf("if(arguments == NULL) {");
        }
        
        if(AOTCWriter.METHOD_ARGUMENT_PASSING != AOTCWriter.CNI_TYPE) {
            if((methodInfo.access & ClassFileConst.ACC_SYNCHRONIZED) != 0) {
                if(MethodInliner.inline_depth == 0) {
                    out.Printf("if(!CVMfastTryLock(ee, cls_ICell->ref_DONT_ACCESS_DIRECTLY)) {");
                    out.Printf("CVMobjectLock(ee, cls_ICell);");
                    out.Printf("}");
                    if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING || AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
                        out.Printf("{");
                        if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
                            out.Printf("AOTCLockList* lock_temp = (AOTCLockList*)malloc(sizeof(AOTCLockList));");
                        }
                        out.Printf("lock_temp->next = ee->lockList;");
                        out.Printf("lock_temp->lock_object = cls_ICell;");
                        out.Printf("ee->lockList = lock_temp;");
                        out.Printf("}");
                    }

                }else {
                    out.Print("{");
                    if((methodInfo.access& ClassFileConst.ACC_STATIC) != 0) {

                        String cb = ((CVMClass)(cfg.getClassInfo().vmClass)).getNativeName() + "_Classblock";
                        out.Print("extern const CVMClassBlock " + cb + ";");
                        String sync = (cb + ".javaInstanceX");

                        out.Print("if(!CVMfastTryLock(ee, " + sync + "->ref_DONT_ACCESS_DIRECTLY)) {");
                        out.Print("CVMobjectLock(ee, " + sync + ");");
                        out.Print("}");
                        inlinedCB = cb;
                        inlinedSYNC = sync;
                    }else {
                        JavaVariable inlinedCls = inlinedIR.getOperand(0);
                        out.Printf("if(!CVMfastTryLock(ee," + inlinedCls + "_ICell->ref_DONT_ACCESS_DIRECTLY)) {");
                        out.Printf("CVMobjectLock(ee," + inlinedCls + "_ICell);");
                        out.Printf("}");
                        inlinedSYNC = inlinedCls.toString() + "_ICell";
                    }
                    /*
                    String cb = ((CVMClass)(cfg.getClassInfo().vmClass)).getNativeName() + "_Classblock";
                    out.Print("{");
                    out.Print("extern const CVMClassBlock " + cb + ";");
                    String sync = (cb + ".javaInstanceX");

                    out.Print("if(!CVMfastTryLock(ee, " + sync + "->ref_DONT_ACCESS_DIRECTLY)) {");
                    out.Print("CVMobjectLock(ee, " + sync + ");");
                    out.Print("}");
                    */
                    if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING || AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
                        //out.Printf("{");
                        if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
                            out.Printf("AOTCLockList* lock_temp = (AOTCLockList*)malloc(sizeof(AOTCLockList));");
                        }
                        out.Printf("lock_temp->next = ee->lockList;");
                        out.Printf("lock_temp->lock_object = " + inlinedSYNC+ ";");
                        out.Printf("ee->lockList = lock_temp;");
                        //out.Printf("}");
                    }
                    out.Print("}");

                }
            }
            pushLocalFrame(numOfICells);
            allocateLocalICells();
            readReferenceArguments();
            //if(AOTCWriter.DEBUG_INFO) out.Printf("fprintf(stderr, \"Start ClassName  : " + cfg.getClassInfo().className + " , MethodName : " + cfg.getMethodInfo().name + " , Type : " + cfg.getMethodInfo().type.string + "inline : " + MethodInliner.inline_depth + "\\n\");");
            //out.Printf("fprintf(stderr, \"Start ClassName  : " + cfg.getClassInfo().className + " , MethodName : " + cfg.getMethodInfo().name + " , Type : " + cfg.getMethodInfo().type.string + "inline : " + MethodInliner.inline_depth + "\\n\");");
            //if((AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING /*|| AOTCWriter.EXCEPTION_INFO*/) && MethodInliner.inline_depth == 0) {
            if((AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING /*|| AOTCWriter.EXCEPTION_INFO*/) /* && MethodInliner.inline_depth == 0 */) {
                exceptionHandler.doPushSetjmp();
            }

        }
        if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.MIX_TYPE && MethodInliner.inline_depth == 0) {
            out.Printf("}else{");
        }
        if((AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.MIX_TYPE) && (methodInfo.access & ClassFileConst.ACC_SYNCHRONIZED) != 0) {
            if(MethodInliner.inline_depth == 0) {
                if((methodInfo.access & Const.ACC_STATIC) == 0) {
                    out.Printf("if(!CVMfastTryLock(ee, arguments[0].j.r.ref_DONT_ACCESS_DIRECTLY)) {");
                    out.Printf("CVMobjectLock(ee, &arguments[0].j.r);");
                    out.Printf("}");
                    if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING || AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
                        out.Printf("{");
                        if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
                            out.Printf("AOTCLockList* lock_temp = (AOTCLockList*)malloc(sizeof(AOTCLockList));");
                        }
                        out.Printf("lock_temp->next = ee->lockList;");
                        out.Printf("lock_temp->lock_object = &arguments[0].j.r;");
                        out.Printf("ee->lockList = lock_temp;");
                        out.Printf("}");
                    }
                }else {
                    out.Printf("{");
                    String cb = ((CVMClass)(cfg.getClassInfo().vmClass)).getNativeName() + "_Classblock";
                    String objName = (cb + ".javaInstanceX");
                    out.Printf("extern const CVMClassBlock " + cb + ";");
                    out.Printf("if(!CVMfastTryLock(ee, " + objName + "->ref_DONT_ACCESS_DIRECTLY)) {");
                    out.Printf("CVMobjectLock(ee, " + objName + ");");
                    out.Printf("}");
                    if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING || AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
                        out.Printf("{");
                        if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
                            out.Printf("AOTCLockList* lock_temp = (AOTCLockList*)malloc(sizeof(AOTCLockList));");
                        }
                        out.Printf("lock_temp->next = ee->lockList;");
                        out.Printf("lock_temp->lock_object = " + objName+ ";");
                        out.Printf("ee->lockList = lock_temp;");
                        out.Printf("}");
                    }
                    out.Printf("}");
                }
            }else {
                out.Print("{");
                if((methodInfo.access & Const.ACC_STATIC) == 0) {
                    JavaVariable inlinedCls = inlinedIR.getOperand(0);
                    out.Printf("if(!CVMfastTryLock(ee," + inlinedCls + "_ICell->ref_DONT_ACCESS_DIRECTLY)) {");
                    out.Printf("CVMobjectLock(ee," + inlinedCls + "_ICell);");
                    out.Printf("}");
                    inlinedSYNC = inlinedCls.toString() + "_ICell";
                }else {
                    String cb = ((CVMClass)(cfg.getClassInfo().vmClass)).getNativeName() + "_Classblock";
                    out.Print("extern const CVMClassBlock " + cb + ";");
                    String sync = (cb + ".javaInstanceX");

                    out.Print("if(!CVMfastTryLock(ee, " + sync + "->ref_DONT_ACCESS_DIRECTLY)) {");
                    out.Print("CVMobjectLock(ee, " + sync + ");");
                    out.Print("}");
                    inlinedCB = cb;
                    inlinedSYNC = sync;
                }
                /*
                String cb = ((CVMClass)(cfg.getClassInfo().vmClass)).getNativeName() + "_Classblock";
                out.Print("{");
                out.Print("extern const CVMClassBlock " + cb + ";");
                String sync = (cb + ".javaInstanceX");

                out.Print("if(!CVMfastTryLock(ee, " + sync + "->ref_DONT_ACCESS_DIRECTLY)) {");
                out.Print("CVMobjectLock(ee, " + sync + ");");
                out.Print("}");
                */
                if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING || AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
                    out.Printf("{");
                    if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
                        out.Printf("AOTCLockList* lock_temp = (AOTCLockList*)malloc(sizeof(AOTCLockList));");
                    }
                    out.Printf("lock_temp->next = ee->lockList;");
                    out.Printf("lock_temp->lock_object = " + inlinedSYNC+ ";");
                    out.Printf("ee->lockList = lock_temp;");
                    out.Printf("}");
                }
                out.Print("}");
            }
        }
        if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.CNI_TYPE || (AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.MIX_TYPE && MethodInliner.inline_depth == 0)) {
            pushLocalFrame(numOfCNIICells);
            allocateLocalCNIICells();
            assignLocalVariables();
            readReferenceCNIArguments();
        }
        if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.MIX_TYPE && MethodInliner.inline_depth == 0) {
            out.Printf("}");
        }
        if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.CNI_TYPE) {
            if(CFGInfo.getCFGInfo(methodInfo).getPrologueGCCheckPoint() || (MethodInliner.inline_depth == 0 && (methodInfo.access & ClassFileConst.ACC_SYNCHRONIZED)!=0)) {  
                if(CFGInfo.getCFGInfo(methodInfo).getPrologueGCCheckPoint()) out.Printf("CVMD_gcSafeCheckPoint(ee, {}, {});");
                if(MethodInliner.inline_depth==0 && (methodInfo.access & ClassFileConst.ACC_SYNCHRONIZED) != 0) {
                    if((methodInfo.access & Const.ACC_STATIC) == 0) {
                        out.Printf("if(!CVMfastTryLock(ee, cls_ICell->ref_DONT_ACCESS_DIRECTLY)) {");
                        out.Printf("CVMobjectLock(ee, cls_ICell);");
                        out.Printf("}");
                    }else {
                        out.Printf("{");
                        String cb = ((CVMClass)(cfg.getClassInfo().vmClass)).getNativeName() + "_Classblock";
                        String objName = (cb + ".javaInstanceX");
                        out.Printf("extern const CVMClassBlock " + cb + ";");
                        out.Printf("if(!CVMfastTryLock(ee, " + objName + "->ref_DONT_ACCESS_DIRECTLY)) {");
                        out.Printf("CVMobjectLock(ee, " + objName + ");");
                        out.Printf("}");
                        out.Printf("}");

                    }
                }
                
                out.Printf("if(global_gc_and_exception_count != local_gc_and_exception_count) {");
                Iterator it = usedVariables.iterator();
                while(it.hasNext())
                {
                    JavaVariable usedVariable = (JavaVariable) it.next();
                    if(usedVariable.isCallerVariable()) continue;
                    if(usedVariable.getType() == TypeInfo.TYPE_REFERENCE) {
                        if(usedVariable instanceof LocalVariable && usedVariable.getIndex() < argumentSize) {
                            out.Printf(usedVariable + " = " +  usedVariable + "_ICell->ref_DONT_ACCESS_DIRECTLY;");
                        }
                        if(usedVariable instanceof Constant) {
                            out.Printf(usedVariable + " = " +  usedVariable + "_ICell->ref_DONT_ACCESS_DIRECTLY;");
                        }
                    }
                }
                out.Printf("}");
            }
        }
        //out.Printf("fprintf(stderr,\" METHOD START" + cfg.getClassInfo().className + "  "+ cfg.getMethodInfo().name + " " + cfg.getMethodInfo().type + "\\n\");");
    }
    public void declareSetjmpVariables() {
        if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING) {
            if(exceptionHandler.useSetjmp()) {
                out.Printf("volatile exception_label* exception_cp = (exception_label*)malloc(sizeof(exception_label));");
                out.Printf("AOTCHandlerList* temp = (AOTCHandlerList*)malloc(sizeof(AOTCHandlerList));");
                out.Printf("AOTCLockList* lock_temp = (AOTCLockList*)malloc(sizeof(AOTCLockList));");
            }else if((cfg.getMethodInfo().access & ClassFileConst.ACC_SYNCHRONIZED)!=0) {
                out.Printf("AOTCLockList* lock_temp = (AOTCLockList*)malloc(sizeof(AOTCLockList));");
            }
        }
    }

    public void declareEnvironmentalVariables() {
        // Writes out needed variables
        if(cfg.getCFGInfo().getConstantPool()) {
            String cbName = cfg.getClassInfo().className.replace('/', '_');
            cbName = cbName.replace('$', '_') + "_Classblock";
            out.Printf("extern const CVMClassBlock " + cbName + ";");
            out.Printf("register CVMConstantPool __attribute__ ((unused)) *cp = CVMcbConstantPool(&" + cbName + ");");
        }
        if(MethodInliner.inline_depth==0) {
            out.Printf("int __attribute__ ((unused)) exception_type;");
        }
        out.Printf("register __attribute__ ((unused)) unsigned int local_gc_and_exception_count = global_gc_and_exception_count;");
        if (MethodInliner.inline_depth==0 && returnType  == TypeInfo.TYPE_REFERENCE) {
            out.Printf("CVMObject *retObj;");
        }
        out.Printf("");
    }

    public void declareLocalVariables() {
        // Write out local variable declaration.
        Iterator it = usedVariables.iterator();
        while(it.hasNext())
        {
            JavaVariable usedVariable = (JavaVariable) it.next();
            if(usedVariable.isCallerVariable()) continue;
            // Call Arg - BY Clamp *****
            if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.CNI_TYPE) {
                if((usedVariable instanceof LocalVariable) && usedVariable.getIndex() < argumentSize) {
                    if(usedVariable.getType() == TypeInfo.TYPE_REFERENCE) {       
                        //out.Printf("register CVMObject* " + usedVariable + "; // = arguments["+ (usedVariable.getIndex())+"].j.r.ref_DONT_ACCESS_        DIRECTLY;");
                        out.Printf("CVMObject* " + usedVariable + "; // = arguments["+ (usedVariable.getIndex())+"].j.r.ref_DONT_ACCESS_        DIRECTLY;");
                    }else {
                        if(usedVariable.getType() == TypeInfo.TYPE_FLOAT) {   
                            //out.Printf("register " + TypeInfo.typeName[usedVariable.getType()] + " " + usedVariable + " = arguments[" + (usedVariable.getIndex()) + "].j.f;");                                                     
                            out.Printf("" + TypeInfo.typeName[usedVariable.getType()] + " " + usedVariable + " = arguments[" + (usedVariable.getIndex()) + "].j.f;");                                                     
                        }                                                     
                        else if(usedVariable.getType() == TypeInfo.TYPE_DOUBLE) {
                            //out.Printf("register " + TypeInfo.typeName[usedVariable.getType()]+ " " + usedVariable + ";");
                            out.Printf("" + TypeInfo.typeName[usedVariable.getType()]+ " " + usedVariable + ";");
                        }
                        else if(usedVariable.getType() == TypeInfo.TYPE_LONG) {                        
                            //out.Printf("register " + TypeInfo.typeName[usedVariable.getType()]+ " " + usedVariable + ";");
                            out.Printf("" + TypeInfo.typeName[usedVariable.getType()]+ " " + usedVariable + ";");
                        }
                        else                                                  
                            //out.Printf("register " + TypeInfo.typeName[usedVariable.getType()] + " " + usedVariable + " = arguments[" + (usedVariable.getIndex()) + "].j.i;");
                            out.Printf("" + TypeInfo.typeName[usedVariable.getType()] + " " + usedVariable + " = arguments[" + (usedVariable.getIndex()) + "].j.i;");
                    }
                    continue;
                }
            }
            // ****************************
            if(usedVariable.getType() == TypeInfo.TYPE_REFERENCE) {
                //out.Printf("register CVMObject* " + usedVariable + ";");
                out.Printf("CVMObject* " + usedVariable + ";");
            } else {
                if((usedVariable instanceof StackVariable) || usedVariable.getIndex() >= argumentSize) {
                    //out.Printf("register " + TypeInfo.typeName[usedVariable.getType()] + " " + usedVariable + ";");
                    out.Printf("" + TypeInfo.typeName[usedVariable.getType()] + " " + usedVariable + ";");
                }
            }
        }
    }

    public void declareICellVariables() {
        Iterator it = usedICells.iterator();
        while (it.hasNext()) {
            JavaVariable usedICell = (JavaVariable) it.next();
            if(usedICell.isCallerVariable()) continue;
            if((usedICell instanceof StackVariable) || usedICell.getIndex() >= argumentSize) {
                out.Printf("register jobject " + usedICell + "_ICell;");
            }else if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.CNI_TYPE) {
                out.Printf("register jobject " + usedICell + "_ICell;");
            }
        }
        out.Printf("");
    }
    public void declareProfileVariables() {
        IR ir;
        Iterator it = cfg.iterator();
        while(it.hasNext()) {
            ir = (IR)it.next();
            if(ir.hasAttribute(IRAttribute.LOOP_HEADER)) {
                out.Printf("CVMBool prof_" + MethodInliner.inline_depth + "_" + ir.getBpc() + " = CVM_FALSE;");
            }
        }
    }
    // Call Arg - BY Clamp *****
    public void declarePushFrameVariables(int icells, int cniICells) {
            if(MethodInliner.inline_depth != 0 ) return;
            if(((AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.JNI_TYPE || AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.WRAPPER_TYPE) && icells > 0) 
                    || ((AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.CNI_TYPE || AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.MIX_TYPE) && cniICells > 0)) {
                out.Printf("jobject locals;");
                //out.Printf("CVMStack* jniLocalsStack;");
                out.Printf("CVMFrame* currentFrame;");
                out.Printf("CVMStackVal32* topOfStack;");
                out.Printf("CVMBool pushed = CVM_FALSE;");
                //out.Printf("CVMStackVal32* freeList;");
                if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING || AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
                    out.Printf("CVMStackVal32* restoreFreeList;");
                    out.Printf("CVMStackVal32* restoreTOS;");
                }

            }
    }
    // **************************
    public void pushLocalFrame(int ICellNum) {
        MethodInfo methodInfo = cfg.getMethodInfo();

        // GC check point generation
        //if(CFGInfo.getCFGInfo(methodInfo).getPrologueGCCheckPoint()) {
        //    out.Printf("CVMD_gcSafeCheckPoint(ee, {}, {});");
        //}

        // Writes out push frame code
        if(MethodInliner.inline_depth==0 && ICellNum>0) {
            out.Printf("AOTCPushLocalFrame(" + ICellNum + ");");
            if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING || AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
                out.Printf("restoreFreeList = CVMgetJNIFrame(currentFrame)->freeList;");
                out.Printf("restoreTOS = topOfStack + " + numOfICells + ";");
            }
        }
    }

    public void allocateLocalICells() {
        // Creates needed spaces for references
        Iterator it;
        int i;
        if(numOfLocalICells>0) {
            i = numOfCalleeICells;
            it = usedICells.iterator();
            while(it.hasNext())
            {
                JavaVariable usedICell = (JavaVariable) it.next();
                if(usedICell.isCallerVariable()) continue;
                if(usedICell.getType() == TypeInfo.TYPE_REFERENCE) {
                    if(usedICell instanceof StackVariable ||
                            (usedICell instanceof LocalVariable && usedICell.getIndex() >= argumentSize)) {
                        out.Printf(usedICell + "_ICell = locals + " + i + ";");
                        out.Printf(usedICell + "_ICell->ref_DONT_ACCESS_DIRECTLY  = NULL;");
                        i++;
                    }
                }
            }
            changedArg_index = i;
        }
        out.Printf("");
    }

    public void readReferenceArguments() {
        // Reads arguments
        Iterator it = usedVariables.iterator();
        while(it.hasNext())
        {
            JavaVariable usedVariable = (JavaVariable) it.next();
            if(usedVariable.isCallerVariable()) continue;
            if(usedVariable.getType() == TypeInfo.TYPE_REFERENCE) {
                if(usedVariable instanceof LocalVariable && usedVariable.getIndex() < argumentSize) {
                    out.Printf(usedVariable + " = " +  usedVariable + "_ICell->ref_DONT_ACCESS_DIRECTLY;");
                    //if(usedVariable.getIndex() == 5) out.Printf("CVMassert(l5_ref->hdr.clas->methodTablePtrX != NULL);");
                }
                if(usedVariable instanceof Constant) {
                    out.Printf(usedVariable + " = " +  usedVariable + "_ICell->ref_DONT_ACCESS_DIRECTLY;");
                    cfg.setUseCls(true); // Note!! this will be used at epilogue generator
                }
            }
        }
        out.Printf("");

        // allocate ICells for reference arguments which are re-written inside this method
        it = changedArgs.iterator();
        while(it.hasNext()) {
            JavaVariable changedArg = (JavaVariable) it.next();
            if(changedArg.getType() == TypeInfo.TYPE_REFERENCE) {
                if(changedArg instanceof LocalVariable && changedArg.getIndex() < argumentSize) {
                    out.Printf(changedArg+"_ICell = locals + "+changedArg_index+";");
                    out.Printf(changedArg+"_ICell->ref_DONT_ACCESS_DIRECTLY = "+changedArg+";");
                    changedArg_index++;
                }
            }
        }
        for(int i = changedArg_index ; i < numOfICells && i >= 0; i++) {
            out.Printf("(locals + " + i + ")->ref_DONT_ACCESS_DIRECTLY = NULL;");
        }
        out.Printf("");

    }

    public void declarePreloadedBlocks() {
        Iterator it = cfg.iterator();
        while(it.hasNext()) {
            IR ir = (IR) it.next();
            Iterator it2 = ir.getPreloadExpressions().iterator();
            while (it2.hasNext()) { // variable declaration
                LoadExpression exp = (LoadExpression) it2.next();
                switch(exp.getExpType()) {
                    case LoadExpression.LOAD_MB_FOR_INVOKE_VIRTUAL:
                        out.Printf("CVMClassBlock *clzAt" + exp.getBpc() + ";");
                        out.Printf("CVMMethodBlock *mbAt" + exp.getBpc() + ";");
                        break;
                }
            }
        }
    }
    public void allocateLocalCNIICells() {
        Iterator it;
        MethodInfo methodInfo = cfg.getMethodInfo();
        int i;
        int localICells = numOfCNILocalICells;
        if((methodInfo.access & ClassFileConst.ACC_STATIC) == 0) {
            localICells++;
        }
        if(localICells>0) {
            i = numOfCalleeICells;
            it = usedICells.iterator();
            while(it.hasNext())
            {
                JavaVariable usedICell = (JavaVariable) it.next();
                if(usedICell.isCallerVariable()) continue;
                if(usedICell.getType() == TypeInfo.TYPE_REFERENCE) {
                    if(usedICell instanceof StackVariable ||
                            usedICell instanceof LocalVariable) {
                        out.Printf(usedICell + "_ICell = locals + " + i + ";");
                        i++;
                    }
                    else {
                        out.Printf(usedICell + "_ICell = locals + " + i + ";");
                        out.Printf("cls = arguments[0].j.r.ref_DONT_ACCESS_DIRECTLY;");
                        i++;
                    }
                }
            }
        }

        out.Printf("");
    }
    public void readReferenceCNIArguments() {
        MethodInfo methodInfo = cfg.getMethodInfo();
        Iterator it = usedVariables.iterator();
        while(it.hasNext())
        {
            JavaVariable usedVariable = (JavaVariable) it.next();
            if(usedVariable.isCallerVariable()) continue;
            if(usedVariable.getType() == TypeInfo.TYPE_REFERENCE) {
                if(usedVariable instanceof LocalVariable && usedVariable.getIndex() < argumentSize) {
                    out.Printf(usedVariable + "_ICell->ref_DONT_ACCESS_DIRECTLY = " + usedVariable + ";");
                }
                if(usedVariable instanceof Constant) {
                    out.Printf(usedVariable + "_ICell->ref_DONT_ACCESS_DIRECTLY = " + usedVariable + ";");

                    cfg.setUseCls(true); // Note!! this will be used at epilogue generator
                }
            }
        }
        out.Printf("");
    }
    public void assignLocalVariables(){
        Iterator it = usedVariables.iterator();
        while(it.hasNext())
        {
            JavaVariable usedVariable = (JavaVariable) it.next();
            if(usedVariable.isCallerVariable()) continue;
            if((usedVariable instanceof LocalVariable) && usedVariable.getIndex() < argumentSize) {
                int type = usedVariable.getType();
                if(type == TypeInfo.TYPE_REFERENCE) {
                    out.Printf(usedVariable + " = arguments["+ (usedVariable.getIndex())+"].j.r.ref_DONT_ACCESS_DIRECTLY;");
                }else {
                    if(type == TypeInfo.TYPE_FLOAT) {
                        out.Printf(usedVariable + " = arguments[" + (usedVariable.getIndex()) + "].j.f;");
                    }else if(type == TypeInfo.TYPE_DOUBLE) {
                        out.Printf("CNIjvm2Double(arguments+"+(usedVariable.getIndex())+", "+usedVariable+");");
                    }else if(type == TypeInfo.TYPE_LONG) {
                        out.Printf("CNIjvm2Long(arguments+"+(usedVariable.getIndex())+", "+usedVariable+");");
                    }else {
                        out.Printf(usedVariable + " = arguments[" + (usedVariable.getIndex()) + "].j.i;");
                    }

                }
            }
        }
    }
}
