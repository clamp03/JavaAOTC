package aotc.optimizer;

import aotc.cfg.*;
import java.lang.Comparable;
import aotc.share.*;
import aotc.ir.*;
import java.util.*;
import opcodeconsts.*;
import components.*;
import vm.*;

/**
 * Preload loop invariant field/method/class block at the loop preheader.
 * 
 * Finds loop invariant field/method/class block inside loop, and move them
 * out of loop (preheader of the loop).
 *
 * This requires loop information and preheader (these information is generated at {@link LoopFinder})
 *
 * @see LoadExpression
 *
 */

public class Preloader {
    private CFG cfg;
    /**
     * Constructor
     * @param cfg   {@link CFG} to be applied preloading
     */
    public Preloader(CFG cfg) {
        this.cfg = cfg;
        //System.out.println(cfg.getMethodInfo().name);
        doPreload();
    }

    public static final String[] PRELOADED_AT_VM_INITIALIZATION = { "java_lang_Character_classblock",
                                                                    "java_lang_Float_Classblock",
                                                                    "java_lang_FloatingDecimal_Classblock",
                                                                    "java_lang_Integer_Classblock",
                                                                    "java_lang_String_Classblock",
                                                                    "java_lang_System_Classblock",
                                                                    "java_lang_Class_Classblock",
                                                                    "java_lang_Thread_Classblock",
                                                                    "sun_misc_ThreadRegistry_Classblock" };

    /**
     * Moves loop invariant field/method/class block out of loop.
     * 
     * First, finds load expressions (expressed as {@link LoadExpression}) which are loop invariant.
     * And try to move them out of loop (preheader of each loop).
     * 
     * It can move 6 types of load expressions
     *
     * 1) load method block for invoke_static
     * 2) load method block for invoke_nonvirtual
     * 3) load method block for invoke_virtual
     * 4) load field block for get_static and put_static
     * 5) load class block for instance_of and check_cast
     *
     * Load expression that can be preloaded are stored in loop preheader by calling
     * IR.addPreloadExpression and they are loaded at {@link CCodeGenerator} by calling
     * IR.getPreloadEpxression.
     *
     * Among them, 1), 2), 4), 5) are all loop invariant regardness of the context.
     * Because they read blocks by refering only constant value. (So they can move 
     * beyond nested loop.)
     * 
     * Note that 3) is loop invarint when the def of target object is outside of the loop.
     *
     */

    public void doPreload() { // this use the ud-du chain
        TreeSet workList = new TreeSet();
        IR  code, pred, succ;
        int bpc;
        IR ir;

        LoadExpression newExpression = null; 
        IR loopHeader;
        IR preheader;

        //ClassInfo classinfo = cfg.getClassInfo();
        //MethodConstant mc;
        int cpIndex;
        String cb;

	Iterator it = cfg.iterator();
	while (it.hasNext()) {
	    ir = (IR) it.next();
        //System.out.println(ir);
            switch (ir.getShortOpcode()) { // Find and move loop invariant load expression
                case OpcodeConst.opc_invokestatic_checkinit_quick:
                    if (ir.hasAttribute(IRAttribute.REDUNDANT_CLASSINIT)) {
                        break;
                    }
                //case OpcodeConst.opc_invokestatic_quick:
                    if (ir.hasAttribute(IRAttribute.LOOP_BODY) 
                        || ir.hasAttribute(IRAttribute.LOOP_HEADER)) { /* If invoke is inside a loop */
	                
                        // prepare information
                        cpIndex = ir.getBytecode().getOperand(0);
                        cb = ir.getClassBlockToInit();
                        new aotc.share.Assert (cb != null, "CB can't be null");
                        
                        // allocate new expression
                        newExpression = new LoadExpression(LoadExpression.INIT_CB_FOR_INVOKE_STATIC, null,
                                                                cpIndex, ir.getBpc(), cb);

                        /* Finds loop header of current loop. */
                        if (ir.hasAttribute(IRAttribute.LOOP_BODY)) {
                            loopHeader = cfg.getIRAtBpc(ir.getLoopHeader());
                        } else {
                            loopHeader = ir;
                        }
                        //System.out.println("loopHeader = " + loopHeader);

                        /* Find the header of outer loop to execute preload considering nested loop. */
                        while (loopHeader.hasAttribute(IRAttribute.LOOP_BODY)) {
                            loopHeader = cfg.getIRAtBpc(loopHeader.getLoopHeader());
                        }

                        /* Add preload expression */
                        loopHeader.getLoopPreheader().addPreloadExpression(newExpression);
                        ir.setAttribute(IRAttribute.PRELOADED);
                        ir.addDebugInfoString("CheckInit done at " + loopHeader.getLoopPreheader().getBpc());
                        //System.out.println("preload at " + loopHeader.getBpc());
                    }
                    break;
                
/*                case OpcodeConst.opc_invokevirtual_quick_w:
                case OpcodeConst.opc_invokevirtual_quick:
                case OpcodeConst.opc_vinvokevirtual_quick:
                case OpcodeConst.opc_ainvokevirtual_quick:
                case OpcodeConst.opc_dinvokevirtual_quick:
                
                    if (ir.hasAttribute(IRAttribute.LOOP_BODY) 
                            || ir.hasAttribute(IRAttribute.LOOP_HEADER)) { // If invoke is inside a loop

                        // Check whether the def of target object is unique.
                        if (ir.getNumOfDefs(0) == 1) {
                            // Finds loop header of current loop.
                            if (ir.hasAttribute(IRAttribute.LOOP_BODY)) {
                                loopHeader = cfg.getIRAtBpc(ir.getLoopHeader());
                            } else {
                                loopHeader = ir;
                            }

                            // In addition, the def should be located at outside of loop. (loop invariant condition)
                            if (!cfg.getIRAtBpc(ir.getDefOfOperand(0, 0)).hasAttribute(IRAttribute.LOOP_BODY) 
                                    && !cfg.getIRAtBpc(ir.getDefOfOperand(0, 0)).hasAttribute(IRAttribute.LOOP_HEADER)) { // def outside loop
                                
                                // Add preload expression
                                newExpression = new LoadExpression(LoadExpression.LOAD_MB_FOR_INVOKE_VIRTUAL, 
                                                                        ir.getOperand(0), 
                                                                        ir.getBytecode().getOperand(0), ir.getBpc(), null);
                                loopHeader.getLoopPreheader().addPreloadExpression(newExpression);
                                ir.setAttribute(IRAttribute.PRELOADED);
                                ir.addDebugInfoString("MB preloaded at " + loopHeader.getLoopPreheader().getBpc());
                            }
                        }
                    }
                    break;
*/
                //case OpcodeConst.opc_aputstatic_quick:
                //case OpcodeConst.opc_putstatic_quick:
                //case OpcodeConst.opc_putstatic2_quick:
                case OpcodeConst.opc_aputstatic_checkinit_quick:
                case OpcodeConst.opc_putstatic_checkinit_quick:
                case OpcodeConst.opc_putstatic2_checkinit_quick:
                //case OpcodeConst.opc_agetstatic_quick:
                //case OpcodeConst.opc_getstatic_quick:
                //case OpcodeConst.opc_getstatic2_quick:
                case OpcodeConst.opc_agetstatic_checkinit_quick:
                case OpcodeConst.opc_getstatic_checkinit_quick:
                case OpcodeConst.opc_getstatic2_checkinit_quick:
                    if (ir.hasAttribute(IRAttribute.REDUNDANT_CLASSINIT)) {
                        break;
                    }
                    if (ir.hasAttribute(IRAttribute.LOOP_BODY) 
                            || ir.hasAttribute(IRAttribute.LOOP_HEADER)) { /* If get/putstatic is inside a loop */
                        
                        // prepare information
                        cpIndex = ir.getBytecode().getOperand(0);
                        cb = ir.getClassBlockToInit();
                        new aotc.share.Assert (cb != null, "CB can't be null");
                        
                        // allocate new expression
                        newExpression = new LoadExpression(LoadExpression.INIT_CB_FOR_PUTSTATIC_GETSTATIC, null,
                                                                cpIndex, ir.getBpc(), cb);
                        /* Finds loop header of current loop. */
                        if (ir.hasAttribute(IRAttribute.LOOP_BODY)) {
                            loopHeader = cfg.getIRAtBpc(ir.getLoopHeader());
                        } else {
                            loopHeader = ir;
                        }

                        /* Find the header of outer loop to execute preload considering nested loop. */
                        while (loopHeader.hasAttribute(IRAttribute.LOOP_BODY)) {
                            loopHeader = cfg.getIRAtBpc(loopHeader.getLoopHeader());
                        }
                        /* Add preload expression */
                        loopHeader.getLoopPreheader().addPreloadExpression(newExpression);
                        ir.setAttribute(IRAttribute.PRELOADED);
                        ir.removeAttribute(IRAttribute.POTENTIAL_GC_POINT);
                        ir.addDebugInfoString("CheckInit done at " + loopHeader.getLoopPreheader().getBpc());
                        //System.out.println("preload at " + loopHeader.getBpc());
                    }
                    break;
                case OpcodeConst.opc_new_checkinit_quick:
                    if (ir.hasAttribute(IRAttribute.REDUNDANT_CLASSINIT)) {
                        break;
                    }
                    if (ir.hasAttribute(IRAttribute.LOOP_BODY) 
                        || ir.hasAttribute(IRAttribute.LOOP_HEADER)) { /* If invoke is inside a loop */
                        
                        // prepare information
                        cpIndex = ir.getBytecode().getOperand(0);
                        cb = ir.getClassBlockToInit();
                        new aotc.share.Assert (cb != null, "CB can't be null");

                        // allocate new expression
                        newExpression = new LoadExpression(LoadExpression.INIT_CB_FOR_NEW, null,
                                                                cpIndex, ir.getBpc(), cb);

                        /* Finds loop header of current loop. */
                        if (ir.hasAttribute(IRAttribute.LOOP_BODY)) {
                            loopHeader = cfg.getIRAtBpc(ir.getLoopHeader());
                        } else {
                            loopHeader = ir;
                        }
                        //System.out.println("loopHeader = " + loopHeader);

                        /* Find the header of outer loop to execute preload considering nested loop. */
                        while (loopHeader.hasAttribute(IRAttribute.LOOP_BODY)) {
                            loopHeader = cfg.getIRAtBpc(loopHeader.getLoopHeader());
                        }

                        /* Add preload expression */
                        loopHeader.getLoopPreheader().addPreloadExpression(newExpression);
                        ir.setAttribute(IRAttribute.PRELOADED);
                        ir.addDebugInfoString("CheckInit done at " + loopHeader.getLoopPreheader().getBpc());
                        //System.out.println("preload at " + loopHeader.getBpc());
                    }
                    break;
                default:
                    
            }
            
        }
    }
    
}

