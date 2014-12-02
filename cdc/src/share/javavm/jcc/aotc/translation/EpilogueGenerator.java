package aotc.translation;

import java.util.*;
import aotc.share.*;
import aotc.ir.*;
import aotc.cfg.*;
//import components.*;
import util.*;

// Call Arg - By Clamp ******
import consts.*;
import components.*;
import vm.*;
import aotc.*;
// ******************************

public class EpilogueGenerator {
    private CFG cfg;
    private AOTCPrint out;
    private boolean localFrameIsPushed = true;
    private boolean CNILocalFrameIsPushed = true;
    private ExceptionHandler exceptionHandler;

    /**
     * The method return type.
     */
    private int retType;
    
    public EpilogueGenerator(CFG cfg, AOTCPrint out, ExceptionHandler exceptionHandler) {
        this.cfg = cfg;
        this.out = out;
        this.exceptionHandler = exceptionHandler;
        localFrameIsPushed = cfg.getCFGInfo().getLocalFrameIsPushed();
        CNILocalFrameIsPushed = cfg.getCFGInfo().getCNILocalFrameIsPushed();

	retType = cfg.getReturnType();
    }

    public void writeEpilogue() {
	out.Printf("");
	out.Printf("CVMassert(0);");
        writeExitForExceptionCode();
        writeNormalExitCode();
    }

    public void writeExitForExceptionCode() {
        if(CCodeGenerator.exception_exit > 0) {
            out.Printf("EXCEPTION_EXIT:");
	//out.Printf("CVMassert(push_try == 0);");
            //if(AOTCWriter.EXCEPTION_INFO) out.Printf("exc_exit++;");
            // exception generate
            if(CCodeGenerator.exception_s0_ref > 0 ) {
                out.Printf("generateException(ee, exception_type, " + ExceptionHandler.s0_ref + "_ICell->ref_DONT_ACCESS_DIRECTLY);");
            }else {
                out.Printf("generateException(ee, exception_type, NULL);");
            }
            if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING || AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
                if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING && exceptionHandler.useSetjmp()) {

                    /* if(AOTCWriter.DEBUG_INFO) */ //out.Printf("fprintf(stderr, \"Pop Setjmp Exception Exit" + cfg.getMethodInfo().getNativeName(true) + "   : %d\\n\", (--setjmpCount));");

                    out.Printf("lock_temp = ee->lockList;");
                    out.Printf("ee->handlerList = ee->handlerList->next;");
                    out.Printf("ee->lockList = ee->lockList->next;");
                    out.Printf("free(temp);");
                    out.Printf("free(lock_temp);");
                    //out.Printf("}");
                }else if((cfg.getMethodInfo().access & ClassFileConst.ACC_SYNCHRONIZED)!=0) {
                    if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
                        out.Printf("AOTCLockList* lock_temp = ee->lockList;");
                    }else {
                        out.Printf("lock_temp = ee->lockList;");
                    }
                    out.Printf("ee->lockList = ee->lockList->next;");
                    out.Printf("free(lock_temp);");
                }

                /* if(AOTCWriter.DEBUG_INFO) */ // out.Printf("fprintf(stderr, \"LongJmp  Start" + cfg.getMethodInfo().getNativeName(true) + "\\n\");");
                out.Printf("longjmp(ee->handlerList->handle_block, 1);");
                return;
            }

            // Call Arg - BY Clamp *****
            if( AOTCWriter.METHOD_ARGUMENT_PASSING != AOTCWriter.CNI_TYPE && cfg.getNeedExitCode() ) {
                
                if(AOTCWriter.METHOD_ARGUMENT_PASSING != AOTCWriter.CNI_TYPE) {
                    if(retType == TypeInfo.TYPE_VOID) {
                        return;
                    }else if(retType == TypeInfo.TYPE_REFERENCE) {
                        out.Printf("retObj = 0;");
                    } else if(retType <= TypeInfo.TYPE_INT) {
                        out.Printf("s0_int = 0;");
                    } else {
                        out.Printf("s0_" + TypeInfo.suffix[retType] + " = 0;");
                    }
                }
                
            } else {
                if((cfg.getMethodInfo().access & ClassFileConst.ACC_SYNCHRONIZED)!=0) {
                    if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.JNI_TYPE 
                            || AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.WRAPPER_TYPE 
                            || (cfg.getMethodInfo().access & Const.ACC_STATIC) == 0) {
                        out.Printf("if(!CVMfastTryUnlock(ee, cls_ICell->ref_DONT_ACCESS_DIRECTLY)) {");
                        out.Printf("CVMobjectUnlock(ee, cls_ICell);");
                        out.Printf("}");
                    }else {
                        out.Printf("{");
                        String cb = ((CVMClass)(cfg.getClassInfo().vmClass)).getNativeName() + "_Classblock";
                        String objName = (cb + ".javaInstanceX");
                        out.Printf("extern const CVMClassBlock " + cb + ";");
                        out.Printf("if(!CVMfastTryUnlock(ee, " + objName + "->ref_DONT_ACCESS_DIRECTLY)) {");
                        out.Printf("CVMobjectUnlock(ee, " + objName + ");");
                        out.Printf("}");
                        out.Printf("}");
                    }
                }
                if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.JNI_TYPE 
                        || AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.WRAPPER_TYPE) {
                    if(localFrameIsPushed) out.Printf("AOTCPopLocalFrame();");
                }else if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.CNI_TYPE) {
                    if(CNILocalFrameIsPushed) out.Printf("AOTCPopLocalFrame();");
                }else {
                    if(localFrameIsPushed && CNILocalFrameIsPushed) out.Printf("AOTCPopLocalFrame();");
                    else if(CNILocalFrameIsPushed) {
                        out.Printf("if(arguments != NULL) {");
                        out.Printf("AOTCPopLocalFrame();");
                        out.Printf("}");
                    }
                    //out.Printf("*ret =  CNI_EXCEPTION;");
                }
                if(AOTCWriter.METHOD_ARGUMENT_PASSING != AOTCWriter.CNI_TYPE) {
                    if(retType == TypeInfo.TYPE_VOID) {
                        out.Printf("return;");
                    } else {
                        out.Printf("return 0;");
                    }
                }else {
                    out.Printf("return CNI_EXCEPTION;");
                }
            }
            // *****************************

        }
    }
    
    public void writeNormalExitCode() {
	if(cfg.getNeedExitCode() == true) {
	    out.Printf("");
	    out.Printf("EXIT:");
	//out.Printf("CVMassert(push_try == 0);");
        //out.Printf("fprintf(stderr, \"METHOD FINISH" + cfg.getClassInfo().className + "  "+ cfg.getMethodInfo().name + " " + cfg.getMethodInfo().type + "\\n\");");
        //if(AOTCWriter.DEBUG_INFO) out.Printf("fprintf(stderr, \"Normal Exit ClassName  : " + cfg.getClassInfo().className + " , MethodName : " + cfg.getMethodInfo().name + " , Type : " + cfg.getMethodInfo().type.string + "\\n\");");
        if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING && exceptionHandler.useSetjmp()) {
            out.Printf("ee->handlerList = ee->handlerList->next;");
            out.Printf("free(temp);");

            /* if(AOTCWriter.DEBUG_INFO) */ // out.Printf("fprintf(stderr, \"Pop Setjmp Normal Exit" + cfg.getMethodInfo().getNativeName(true) + "   : %d\\n\", (--setjmpCount));");
            if((cfg.getMethodInfo().access & ClassFileConst.ACC_SYNCHRONIZED)==0) {
                out.Printf("lock_temp = ee->lockList;");
                out.Printf("ee->lockList = ee->lockList->next;");
                out.Printf("free(lock_temp);");
            }
        }



	    if((cfg.getMethodInfo().access & ClassFileConst.ACC_SYNCHRONIZED)!=0) {
            // Call Arg - BY Clamp *****
            // if(AOTCWriter.METHOD_ARGUMENT_PASSING != AOTCWriter.CNI_TYPE || (cfg.getMethodInfo().access & Const.ACC_STATIC) == 0) {
            if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.JNI_TYPE 
                    || AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.WRAPPER_TYPE
                    || (cfg.getMethodInfo().access & Const.ACC_STATIC) == 0) {
                if(cfg.getUseCls())
                    out.Printf("if(!CVMfastTryUnlock(ee, cls)) {");
                else
                    out.Printf("if(!CVMfastTryUnlock(ee, cls_ICell->ref_DONT_ACCESS_DIRECTLY)) {");
                out.Printf("CVMobjectUnlock(ee, cls_ICell);");
                out.Printf("}");
            }else {
                out.Printf("{");
                String cb = ((CVMClass)(cfg.getClassInfo().vmClass)).getNativeName() + "_Classblock";
                String objName = (cb + ".javaInstanceX");
                out.Printf("extern const CVMClassBlock " + cb + ";");
                out.Printf("if(!CVMfastTryUnlock(ee, " + objName + "->ref_DONT_ACCESS_DIRECTLY)) {");
                out.Printf("CVMobjectUnlock(ee, " + objName + ");");
                out.Printf("}");
                out.Printf("}");
            }
            if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_PROLOG_HANDLING || AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
                out.Printf("{");
                if(AOTCWriter.EXCEPTION_IMPLEMENTATION == AOTCWriter.SETJMP_TRY_HANDLING) {
                    out.Printf("AOTCLockList* lock_temp = ee->lockList;");
                }else {
                    out.Printf("lock_temp = ee->lockList;");
                }
                out.Printf("ee->lockList = ee->lockList->next;");
                out.Printf("free(lock_temp);");
                out.Printf("}");
            }
            // **************************
	    }
        
        if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.JNI_TYPE 
                || AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.WRAPPER_TYPE) {
            if(localFrameIsPushed) out.Printf("AOTCPopLocalFrame();");
        }

        // Call Arg - BY Clamp
        if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.MIX_TYPE) {
            if(localFrameIsPushed && CNILocalFrameIsPushed) out.Printf("AOTCPopLocalFrame();");
            out.Printf("if(arguments != NULL) {");
            if(!localFrameIsPushed && CNILocalFrameIsPushed) {
                out.Printf("AOTCPopLocalFrame();");
            }
            if(retType == TypeInfo.TYPE_VOID) {
                out.Printf("*ret = CNI_VOID;");
            } else if(retType == TypeInfo.TYPE_REFERENCE) {
                out.Printf("arguments[0].j.r.ref_DONT_ACCESS_DIRECTLY = retObj;");
                out.Printf("*ret =  CNI_SINGLE;");
            } else if(retType <= TypeInfo.TYPE_INT) {
                out.Printf("arguments[0].j.i = s0_int;");
                out.Printf("*ret =  CNI_SINGLE;");
            } else if(retType == TypeInfo.TYPE_FLOAT) {
                out.Printf("arguments[0].j.f = s0_float;");
                out.Printf("*ret =  CNI_SINGLE;");
            } else if(retType == TypeInfo.TYPE_LONG) {
                out.Printf("arguments[0].j.i = s0_long & 0xffffffff;");
                out.Printf("arguments[1].j.i = (s0_long & 0xffffffff00000000) >> 32;");
                out.Printf("*ret =  CNI_DOUBLE;");
            } else if(retType >= TypeInfo.TYPE_DOUBLE) {
                out.Printf("CNIDouble2jvm(arguments, s0_double);");
                out.Printf("*ret =  CNI_DOUBLE;");
            } else {
                out.Printf("arguments[0].j.i = s0_"+TypeInfo.suffix[retType]+";");
                out.Printf("*ret =  CNI_SINGLE;");
            }
            out.Printf("}");
        }
        if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.CNI_TYPE) {
            if(CNILocalFrameIsPushed) out.Printf("AOTCPopLocalFrame();");
            if(retType == TypeInfo.TYPE_VOID) {
                out.Printf("return CNI_VOID;");
            } else if(retType == TypeInfo.TYPE_REFERENCE) {
                out.Printf("arguments[0].j.r.ref_DONT_ACCESS_DIRECTLY = retObj;");
                out.Printf("return CNI_SINGLE;");
            } else if(retType <= TypeInfo.TYPE_INT) {
                out.Printf("arguments[0].j.i = s0_int;");
                out.Printf("return CNI_SINGLE;");
            } else if(retType == TypeInfo.TYPE_FLOAT) {
                out.Printf("arguments[0].j.f = s0_float;");
                out.Printf("return CNI_SINGLE;");
            } else if(retType == TypeInfo.TYPE_LONG) {
                out.Printf("arguments[0].j.i = s0_long & 0xffffffff;");
                out.Printf("arguments[1].j.i = (s0_long & 0xffffffff00000000) >> 32;");
                out.Printf("return CNI_DOUBLE;");
            } else if(retType >= TypeInfo.TYPE_DOUBLE) {
                out.Printf("CNIDouble2jvm(arguments, s0_double);");
                out.Printf("return CNI_DOUBLE;");
            } else {
                out.Printf("arguments[0].j.i = s0_"+TypeInfo.suffix[retType]+";");
                out.Printf("return CNI_SINGLE;");
            }
        }else {
            if(retType == TypeInfo.TYPE_VOID) {
                out.Printf("return;");
            } else if(retType == TypeInfo.TYPE_REFERENCE) {
                //out.Printf("if(!s0_ref) return NULL;");
                out.Printf("return (jobject)retObj;");
            } else if(retType <= TypeInfo.TYPE_INT) {
                out.Printf("return s0_int;");
            } else {
                out.Printf("return s0_" + TypeInfo.suffix[retType] + ";");
            }
        }
        // ********************

        

    }else {
        //out.Printf("fprintf(stderr, \"METHOD FINISH" + cfg.getClassInfo().className + "  "+ cfg.getMethodInfo().name + " " + cfg.getMethodInfo().type + "\\n\");");
    }
	}
}
