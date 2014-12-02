package aotc.optimizer;

import aotc.cfg.*;
import java.lang.Comparable;
import aotc.share.*;
import aotc.ir.*;
import java.util.*;
import opcodeconsts.*;

/**
 * Express field/method/class block load expression.
 *
 * This is used for {@link Preloader} to store load expression
 * 
 * There are 5 kinds of load expression. 
 *
 * 1) invokestatic_checkinit_quick
 *    invokestatic_quick
 * 
 *    CVMMethodBlock *mb = CVMcpGetMb(cp, 3);
 *    CVMClassBlock *scb = CVMmbClassBlock(mb);
 *
 * 2) invokenonvirtual_quick (this used for only <init>)
 *    
 *    CVMMethodBlock *mb = CVMcpGetMb(cp, 9);
 * 
 * 3) invokevirtual_quick_w
 *    invokevirtual_quick
 *    ainvokevirtual_quick
 *    dinvokevirtual_quick
 *    vinvokevirtual_quick
 *    
 *    CVMClassBlock *clz = CVMobjectGetClass(object_ref);
 *    CVMMethodBlock *mb = CVMcbMethodTableSlot(clz, 29);
 *    
 * 4) aputstatic_quick:
 *    putstatic_quick:
 *    putstatic2_quick:
 *    aputstatic_checkinit_quick:
 *    putstatic_checkinit_quick:
 *    putstatic2_checkinit_quick:
 *    agetstatic_quick:
 *    getstatic_quick:
 *    getstatic2_quick:
 *    agetstatic_checkinit_quick:
 *    getstatic_checkinit_quick:
 *    getstatic2_checkinit_quick:
 *  
 *    CVMFieldBlock *fb = CVMcpGetFb(cp, 28);
 *    if(CVMcbInitializationNeeded(CVMfbClassBlock(fb), ee)) {
 *      AOTCclassInit(ee, CVMfbClassBlock(fb));
 *      if(local_gc_check_val != gc_check_val) {
 *          restore_ref_point = &&LL_Java_spec_benchmarks__1200_1check_Main_runBenchmark_119_GC;
 *          goto GC_RESTORE;
 *      }
 *    }
 *
 * 5) instanceof_quick:
 *    checkcast_quick:
 *
 *    CVMClassBlock *castCb = CVMcpGetCb(cp, 5);
 *
 * All of these expressions are used to preload some expression beform loop-entrance.
 *
 * @see Preloader
 */
public class LoadExpression /*implements Comparable */{
    public final static int INIT_CB_FOR_INVOKE_STATIC = 1;
    //public final static int LOAD_FOR_INVOKE_NONVIRTUAL = 2;
    public final static int LOAD_MB_FOR_INVOKE_VIRTUAL = 3;
    //public final static int LOAD_ARRAY_LENGTH = 4;
    public final static int LOAD_FIELD_BLOCK = 5;
    public final static int INIT_CB_FOR_PUTSTATIC_GETSTATIC = 6;
    public final static int INIT_CB_FOR_NEW = 7;

    private CFG cfg;
    private int bpc; // user of this scalar expression's bpc   
    private int expType; // the type of expression
    private int CPIndex;                // index in costant pool
    private JavaVariable objectRef;    // object reference for invoke virtual and array length and get/put static
    private String classBlock;
    
    /**
     * Constructor
     *
     * @param expType expression type one of followings
     *                LOAD_FOR_INVOKE_STATIC
     *                LOAD_FOR_INVOKE_NONVIRTUAL
     *                LOAD_FOR_INVOKE_VIRTUAL
     *                LOAD_ARRAY_LENGTH
     *                LOAD_FIELD_BLOCK
     *                LOAD_CLASSBLOCK
     * @param objectRef target object of invoke virtual
     *                  not used for other instructions.
     * @param CPndex constant pool index of load expression
     * @param bpc bpc of instruction which use preloaded information
     */
    public LoadExpression(int expType, JavaVariable objectRef, int CPIndex, int bpc, String classBlock) {
        this.expType = expType;
        this.objectRef = objectRef;
        this.CPIndex = CPIndex;
        this.bpc = bpc;
        this.classBlock = classBlock;
    }
    
    /**
     * Returns the expression type
     */
    public int getExpType() {
        return expType;
    }

    /**
     * Returns the constant pool index 
     */
    public int getCPIndex() {
        return CPIndex;
    }

    /**
     * Returns the object reference 
     */
    public JavaVariable getObjectRef() {
        return objectRef;
    }

    /**
     * Returns the bpc of instruction which use the preloaded information 
     */
    public int getBpc() {
        return bpc;
    }

    public String getCB() {
        return classBlock;
    }

    /**
     * Returns if the expression can occur GC.
     */
     public boolean canInvokeGC() {
        if (expType == INIT_CB_FOR_INVOKE_STATIC 
                || expType == INIT_CB_FOR_PUTSTATIC_GETSTATIC
                || expType == INIT_CB_FOR_NEW) {
            return true;
        } else {
            return false;
        }
     }
}
