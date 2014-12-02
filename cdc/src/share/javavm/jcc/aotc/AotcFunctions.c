#include "javavm/include/directmem.h"
#include "generated/offsets/java_lang_String.h"
#include "generated/offsets/java_lang_StringBuffer.h"
#include "generated/offsets/java_util_Vector.h"
#include "generated/offsets/java_util_AbstractList.h"
#include "generated/offsets/sun_io_ByteToCharConverter.h"
#include "generated/offsets/sun_io_CharToByteConverter.h"
#include "generated/offsets/sun_io_CharToByteISO8859_1.h"

void AOTCclassInit(CVMExecEnv* ee, CVMClassBlock* cb)
{
    CVMD_gcSafeExec(ee, {
	CVMclassInitNoCRecursion(ee, cb, NULL);
    });
}

//unsigned int gc_check_val = 0;
unsigned int global_gc_and_exception_count = 0;
//unsigned int aotc_call_count = 0;
//unsigned int to_aotc_call_count = 0;
unsigned int setjmpCount = 0;


/*jobject AOTCPushLocalFrame(CVMExecEnv *ee, int number)
{
    CVMStack*      jniLocalsStack = &ee->interpreterStack;
    CVMFrame*      currentFrame   = jniLocalsStack->currentFrame;
    CVMStackVal32* topOfStack = currentFrame->topOfStack;
    CVMFrame* cf = currentFrame; ///

    int i;
    CVMStackVal32* slot;

    //if (!CVMcheckCapacity(jniLocalsStack, topOfStack, number)) {
    if (!CVMcheckCapacity(jniLocalsStack, topOfStack, number+CVMJNIFrameCapacity)) {
	//slot = CVMexpandStack(ee, jniLocalsStack, number, CVM_FALSE, CVM_FALSE);
	slot = CVMexpandStack(ee, jniLocalsStack, number+CVMJNIFrameCapacity, CVM_FALSE, CVM_FALSE);
	if(slot == 0) {
	    CVMclearLocalException(ee);
	    CVMthrowOutOfMemoryError(ee, NULL);
	    return NULL;
	}
	//CVMsetFrameNeedsSpecialHandlingBit(currentFrame->prevX);
	CVMsetFrameNeedsSpecialHandlingBit(cf);
	currentFrame = (CVMFrame*)slot;
	CVMsetPrevSlow(currentFrame, cf);                                           
	currentFrame->type = CVM_FRAMETYPE_JNI;                                     
	currentFrame->mb = NULL;
	jniLocalsStack->currentFrame = currentFrame;                                
	currentFrame->topOfStack = (CVMStackVal32*)currentFrame + CVMJNIFrameCapacity;
	CVMjniFrameInit(CVMgetJNIFrame(currentFrame), CVM_TRUE);                    

	slot = currentFrame->topOfStack; 
    } else {
	slot = currentFrame->topOfStack;
    }

    // Makes local reference space.
    currentFrame->topOfStack = slot + number;
    for(i=0; i<number; i++) {
	(slot+i)->ref.ref_DONT_ACCESS_DIRECTLY = NULL;
    }
    return &slot->ref;
}*/
/*
CVMFrame* AOTCpopFrame(CVMExecEnv* ee) {
    CVMStack* jniLocalsStack = &ee->interpreterStack;
    CVMFrame* currentFrame = jniLocalsStack->currentFrame;
    CVMpopFrame(jniLocalsStack, currentFrame);
  
  return jniLocalsStack->currentFrame;
}
*/
CVMFrame* AOTCExpandFrame(CVMExecEnv* ee, int size) 
{
//    CVMconsolePrintf("---------- AOTCExpandFrame called. ----------\n");
    CVMStack*      jniLocalsStack = &ee->interpreterStack;
    CVMFrame*      oldFrame   = jniLocalsStack->currentFrame;
    //CVMStackVal32* topOfStack = currentFrame->topOfStack;
    //CVMFrame* cf = currentFrame; ///
    CVMFrame* newFrame;

    newFrame = (CVMFrame*) CVMexpandStack(ee, jniLocalsStack, size+CVMJNIFrameCapacity, CVM_FALSE, CVM_FALSE);
    if(newFrame == 0) {
        CVMclearLocalException(ee);
        CVMthrowOutOfMemoryError(ee, NULL);
        return NULL;
    }
    //CVMsetFrameNeedsSpecialHandlingBit(currentFrame->prevX);
    CVMsetFrameNeedsSpecialHandlingBit(oldFrame);
    newFrame = (CVMFrame*)newFrame;
    CVMsetPrevSlow(newFrame, oldFrame);                                           
    newFrame->type = CVM_FRAMETYPE_JNI;                                     
    newFrame->mb = NULL;
    jniLocalsStack->currentFrame = newFrame;                                
    newFrame->topOfStack = (CVMStackVal32*)newFrame + CVMJNIFrameCapacity;
    CVMjniFrameInit(CVMgetJNIFrame(newFrame), CVM_TRUE);                    
    return newFrame; 
}

/*
void AOTCPopLocalFrame(CVMExecEnv *ee, int number)
{
    CVMStack* jniLocalsStack = &ee->interpreterStack;
    CVMFrame* currentFrame = jniLocalsStack->currentFrame;
    currentFrame->topOfStack -= number;
}
*/
/*
jobject AOTCPushLocalFrameSync(CVMExecEnv *ee, jobject lock_ref, int number)
{
    jobject ret = AOTCPushLocalFrame(ee, number);
    if(!CVMfastTryLock(ee, CVMID_icellDirect(ee, lock_ref))) {
	CVMobjectLock(ee, lock_ref);
    }
    return ret;
}
*/
/*
void AOTCPopLocalFrameSync(CVMExecEnv *ee, jobject lock_ref, int number)
{
    if (!CVMfastTryUnlock(ee, CVMID_icellDirect(ee, lock_ref))) {
	CVMobjectUnlock(ee, lock_ref);
    }
    AOTCPopLocalFrame(ee, number);
}
*/
/*
jthrowable JNICALL AOTCExceptionOccurred(CVMExecEnv *ee)
{
    CVMThrowableICell* exceptionICell = NULL;

    if (CVMlocalExceptionOccurred(ee)) {
	exceptionICell = CVMlocalExceptionICell(ee);
    } else if (CVMremoteExceptionOccurred(ee)) {
	exceptionICell = CVMremoteExceptionICell(ee);
    }

    return exceptionICell;
}

*/
/* virtual method : isVirtual = true, isStatic = false
   static method  : isVirtual = false, isStatic = true
   nonvirtual method : isVirtual = false, isStatic  = false (init function call)
*/
/* for retType
CVM_TYPEID_OBJ
CVM_TYPEID_BOOLEAN
CVM_TYPEID_BYTE
CVM_TYPEID_CHAR
CVM_TYPEID_SHORT
CVM_TYPEID_INT
CVM_TYPEID_LONG
CVM_TYPEID_FLOAT
CVM_TYPEID_DOUBLE
CVM_TYPEID_VOID
*/

#define AOTC_SAFE_COUNT 32

#ifdef SERVER_AOTC_WRAPPER
void InvokeInterpreterV(CVMExecEnv *ee, jobject obj, ...) {
    CVMFrame* frame;
    CVMassert(CVMframeIsFreelist(CVMeeGetCurrentFrame(ee)));
    CVMMethodBlock* methodID = ee->callee;
    if (CVMlocalExceptionOccurred(ee)) {
        return;
    }
    
    frame = (CVMFrame*)CVMpushTransitionFrame(ee, methodID);
    if (frame == NULL) {
	CVMassert(CVMexceptionOccurred(ee));
	return;  /* exception already thrown */
    }
    CVMBool isStatic = CVMmbIs(methodID, STATIC); // TODO 
    CVMassert(!isStatic);
    //CVMBool isVirtual = !isStatic; // SLOW
    //CVMUint16 retType; // TODO
    
    /* Push the "this" argument if the method is not static. */
    if (!isStatic) {
	CVMID_icellAssignDirect(ee, &frame->topOfStack->j.r, obj);
	frame->topOfStack++;
    }

	CVMterseSigIterator terseSig;
    /* Push the all the other arguments. */
    {
	CVMStackVal32 *topOfStack = frame->topOfStack;
	int safeCount = AOTC_SAFE_COUNT;
	int typeSyllable;
	va_list args;
	va_start(args, obj);

	CVMtypeidGetTerseSignatureIterator(CVMmbNameAndTypeID(methodID), &terseSig);

	while((typeSyllable=CVM_TERSE_ITER_NEXT(terseSig))!=CVM_TYPEID_ENDFUNC) {
	    if(safeCount-- == 0) {
		CVMD_gcSafeCheckPoint(ee, { frame->topOfStack = topOfStack; }, {});
		safeCount = AOTC_SAFE_COUNT;
	    }

	    switch(typeSyllable) {
		case CVM_TYPEID_BOOLEAN:
		case CVM_TYPEID_SHORT:
		case CVM_TYPEID_BYTE:
		case CVM_TYPEID_CHAR:
		case CVM_TYPEID_INT:
		    (topOfStack++)->j.i = va_arg(args, jint);
		    continue;
		case CVM_TYPEID_FLOAT:
		    (topOfStack++)->j.f = va_arg(args, jdouble);
		    continue;
		case CVM_TYPEID_OBJ: {
		    jobject o = va_arg(args, jobject);
		    CVMID_icellAssignDirect(ee, &topOfStack->j.r, o);

		    topOfStack++;
		    continue;
		}
		case CVM_TYPEID_LONG: {
		    jlong l = va_arg(args, jlong);
		    CVMlong2Jvm(&topOfStack->j.raw, l);
		    topOfStack += 2;
		    continue;
		}
		case CVM_TYPEID_DOUBLE: {
		    jdouble d = va_arg(args, jdouble);
		    CVMdouble2Jvm(&topOfStack->j.raw, d);
		    topOfStack += 2;
		    continue;
		}
		default:
		    CVMassert(0);
		    return;
	    }
	}
	va_end(args);
	frame->topOfStack = topOfStack;
    }

    CVMassert(!CVMlocalExceptionOccurred(ee));

    /* Call the java method. */
    //CVMgcUnsafeExecuteJavaMethod(ee, methodID, isStatic, !isStatic);
    CVMgcUnsafeExecuteJavaMethod(ee, methodID, CVM_FALSE, CVM_TRUE);

    /* 
     * Copy the result. frame->topOfStack will point after the result.
     */
#if 0
    void* retValue;
    CVMTypeIDTypePart retType = CVM_TERSE_ITER_RETURNTYPE(terseSig);
    if (!CVMlocalExceptionOccurred(ee) /* && retValue */) {
	switch (retType) {
	    case CVM_TYPEID_VOID:
		break;
	    case CVM_TYPEID_BOOLEAN:
	    case CVM_TYPEID_BYTE:
	    case CVM_TYPEID_CHAR:
	    case CVM_TYPEID_SHORT:
	    case CVM_TYPEID_INT:
		retValue = (void*)frame->topOfStack[-1].j.i;
		break;
	    case CVM_TYPEID_LONG: {
		// retValue = (void*)CVMjvm2Long(&frame->topOfStack[-2].j.raw); // TODO
		break;
	    }
	    case CVM_TYPEID_FLOAT:
		// retValue = (void*)frame->topOfStack[-1].j.f; // TODO
		break;
	    case CVM_TYPEID_DOUBLE: {
		// retValue = (void*)CVMjvm2Double(&frame->topOfStack[-2].j.raw); // TODO
		break;
	    }
	    case CVM_TYPEID_OBJ:
		retValue = (void*)frame->topOfStack[-1].j.r.ref_DONT_ACCESS_DIRECTLY;
		break;
	}
    }
#endif
    /* Pop the transition frame. */
    CVMpopFrame(&ee->interpreterStack, ee->interpreterStack.currentFrame);
    return;
}

jint InvokeInterpreterI(CVMExecEnv *ee, jobject obj, ...) {
    CVMFrame* frame;
    CVMassert(CVMframeIsFreelist(CVMeeGetCurrentFrame(ee)));
    CVMMethodBlock* methodID = ee->callee;
    if (CVMlocalExceptionOccurred(ee)) {
        return 0;
    }
    
    frame = (CVMFrame*)CVMpushTransitionFrame(ee, methodID);
    if (frame == NULL) {
	CVMassert(CVMexceptionOccurred(ee));
	return 0;  /* exception already thrown */
    }
    CVMBool isStatic = CVMmbIs(methodID, STATIC); // TODO 
    CVMassert(!isStatic);
    //CVMBool isVirtual = !isStatic; // SLOW
    //CVMUint16 retType; // TODO
    
    /* Push the "this" argument if the method is not static. */
    if (!isStatic) {
	CVMID_icellAssignDirect(ee, &frame->topOfStack->j.r, obj);
	frame->topOfStack++;
    }

	CVMterseSigIterator terseSig;
    /* Push the all the other arguments. */
    {
	CVMStackVal32 *topOfStack = frame->topOfStack;
	int safeCount = AOTC_SAFE_COUNT;
	int typeSyllable;
	va_list args;
	va_start(args, obj);

	CVMtypeidGetTerseSignatureIterator(CVMmbNameAndTypeID(methodID), &terseSig);

	while((typeSyllable=CVM_TERSE_ITER_NEXT(terseSig))!=CVM_TYPEID_ENDFUNC) {
	    if(safeCount-- == 0) {
		CVMD_gcSafeCheckPoint(ee, { frame->topOfStack = topOfStack; }, {});
		safeCount = AOTC_SAFE_COUNT;
	    }

	    switch(typeSyllable) {
		case CVM_TYPEID_BOOLEAN:
		case CVM_TYPEID_SHORT:
		case CVM_TYPEID_BYTE:
		case CVM_TYPEID_CHAR:
		case CVM_TYPEID_INT:
		    (topOfStack++)->j.i = va_arg(args, jint);
		    continue;
		case CVM_TYPEID_FLOAT:
		    (topOfStack++)->j.f = va_arg(args, jdouble);
		    continue;
		case CVM_TYPEID_OBJ: {
		    jobject o = va_arg(args, jobject);
		    CVMID_icellAssignDirect(ee, &topOfStack->j.r, o);

		    topOfStack++;
		    continue;
		}
		case CVM_TYPEID_LONG: {
		    jlong l = va_arg(args, jlong);
		    CVMlong2Jvm(&topOfStack->j.raw, l);
		    topOfStack += 2;
		    continue;
		}
		case CVM_TYPEID_DOUBLE: {
		    jdouble d = va_arg(args, jdouble);
		    CVMdouble2Jvm(&topOfStack->j.raw, d);
		    topOfStack += 2;
		    continue;
		}
		default:
		    CVMassert(0);
		    return 0;
	    }
	}
	va_end(args);
	frame->topOfStack = topOfStack;
    }

    CVMassert(!CVMlocalExceptionOccurred(ee));

    /* Call the java method. */
    //CVMgcUnsafeExecuteJavaMethod(ee, methodID, isStatic, !isStatic);
    CVMgcUnsafeExecuteJavaMethod(ee, methodID, CVM_FALSE, CVM_TRUE);

    /* 
     * Copy the result. frame->topOfStack will point after the result.
     */
    jint retValue = frame->topOfStack[-1].j.i;
#if 0
    void* retValue;
    CVMTypeIDTypePart retType = CVM_TERSE_ITER_RETURNTYPE(terseSig);
    if (!CVMlocalExceptionOccurred(ee) /* && retValue */) {
	switch (retType) {
	    case CVM_TYPEID_VOID:
		break;
	    case CVM_TYPEID_BOOLEAN:
	    case CVM_TYPEID_BYTE:
	    case CVM_TYPEID_CHAR:
	    case CVM_TYPEID_SHORT:
	    case CVM_TYPEID_INT:
		retValue = (void*)frame->topOfStack[-1].j.i;
		break;
	    case CVM_TYPEID_LONG: {
		// retValue = (void*)CVMjvm2Long(&frame->topOfStack[-2].j.raw); // TODO
		break;
	    }
	    case CVM_TYPEID_FLOAT:
		// retValue = (void*)frame->topOfStack[-1].j.f; // TODO
		break;
	    case CVM_TYPEID_DOUBLE: {
		// retValue = (void*)CVMjvm2Double(&frame->topOfStack[-2].j.raw); // TODO
		break;
	    }
	    case CVM_TYPEID_OBJ:
		retValue = (void*)frame->topOfStack[-1].j.r.ref_DONT_ACCESS_DIRECTLY;
		break;
	}
    }
#endif
    /* Pop the transition frame. */
    CVMpopFrame(&ee->interpreterStack, ee->interpreterStack.currentFrame);
    return retValue;
}

jlong InvokeInterpreterJ(CVMExecEnv *ee, jobject obj, ...) {
    CVMFrame* frame;
    CVMassert(CVMframeIsFreelist(CVMeeGetCurrentFrame(ee)));
    CVMMethodBlock* methodID = ee->callee;
    if (CVMlocalExceptionOccurred(ee)) {
        return 0;
    }
    
    frame = (CVMFrame*)CVMpushTransitionFrame(ee, methodID);
    if (frame == NULL) {
	CVMassert(CVMexceptionOccurred(ee));
	return 0;  /* exception already thrown */
    }
    CVMBool isStatic = CVMmbIs(methodID, STATIC); // TODO 
    CVMassert(!isStatic);
    //CVMBool isVirtual = !isStatic; // SLOW
    //CVMUint16 retType; // TODO
    
    /* Push the "this" argument if the method is not static. */
    if (!isStatic) {
	CVMID_icellAssignDirect(ee, &frame->topOfStack->j.r, obj);
	frame->topOfStack++;
    }

	CVMterseSigIterator terseSig;
    /* Push the all the other arguments. */
    {
	CVMStackVal32 *topOfStack = frame->topOfStack;
	int safeCount = AOTC_SAFE_COUNT;
	int typeSyllable;
	va_list args;
	va_start(args, obj);

	CVMtypeidGetTerseSignatureIterator(CVMmbNameAndTypeID(methodID), &terseSig);

	while((typeSyllable=CVM_TERSE_ITER_NEXT(terseSig))!=CVM_TYPEID_ENDFUNC) {
	    if(safeCount-- == 0) {
		CVMD_gcSafeCheckPoint(ee, { frame->topOfStack = topOfStack; }, {});
		safeCount = AOTC_SAFE_COUNT;
	    }

	    switch(typeSyllable) {
		case CVM_TYPEID_BOOLEAN:
		case CVM_TYPEID_SHORT:
		case CVM_TYPEID_BYTE:
		case CVM_TYPEID_CHAR:
		case CVM_TYPEID_INT:
		    (topOfStack++)->j.i = va_arg(args, jint);
		    continue;
		case CVM_TYPEID_FLOAT:
		    (topOfStack++)->j.f = va_arg(args, jdouble);
		    continue;
		case CVM_TYPEID_OBJ: {
		    jobject o = va_arg(args, jobject);
		    CVMID_icellAssignDirect(ee, &topOfStack->j.r, o);

		    topOfStack++;
		    continue;
		}
		case CVM_TYPEID_LONG: {
		    jlong l = va_arg(args, jlong);
		    CVMlong2Jvm(&topOfStack->j.raw, l);
		    topOfStack += 2;
		    continue;
		}
		case CVM_TYPEID_DOUBLE: {
		    jdouble d = va_arg(args, jdouble);
		    CVMdouble2Jvm(&topOfStack->j.raw, d);
		    topOfStack += 2;
		    continue;
		}
		default:
		    CVMassert(0);
		    return 0;
	    }
	}
	va_end(args);
	frame->topOfStack = topOfStack;
    }

    CVMassert(!CVMlocalExceptionOccurred(ee));

    /* Call the java method. */
    //CVMgcUnsafeExecuteJavaMethod(ee, methodID, isStatic, !isStatic);
    CVMgcUnsafeExecuteJavaMethod(ee, methodID, CVM_FALSE, CVM_TRUE);

    /* 
     * Copy the result. frame->topOfStack will point after the result.
     */
    jlong retValue = CVMjvm2Long(&frame->topOfStack[-2].j.raw);
#if 0
    void* retValue;
    CVMTypeIDTypePart retType = CVM_TERSE_ITER_RETURNTYPE(terseSig);
    if (!CVMlocalExceptionOccurred(ee) /* && retValue */) {
	switch (retType) {
	    case CVM_TYPEID_VOID:
		break;
	    case CVM_TYPEID_BOOLEAN:
	    case CVM_TYPEID_BYTE:
	    case CVM_TYPEID_CHAR:
	    case CVM_TYPEID_SHORT:
	    case CVM_TYPEID_INT:
		retValue = (void*)frame->topOfStack[-1].j.i;
		break;
	    case CVM_TYPEID_LONG: {
		// retValue = (void*)CVMjvm2Long(&frame->topOfStack[-2].j.raw); // TODO
		break;
	    }
	    case CVM_TYPEID_FLOAT:
		// retValue = (void*)frame->topOfStack[-1].j.f; // TODO
		break;
	    case CVM_TYPEID_DOUBLE: {
		// retValue = (void*)CVMjvm2Double(&frame->topOfStack[-2].j.raw); // TODO
		break;
	    }
	    case CVM_TYPEID_OBJ:
		retValue = (void*)frame->topOfStack[-1].j.r.ref_DONT_ACCESS_DIRECTLY;
		break;
	}
    }
#endif
    /* Pop the transition frame. */
    CVMpopFrame(&ee->interpreterStack, ee->interpreterStack.currentFrame);
    return retValue;
}


jfloat InvokeInterpreterF(CVMExecEnv *ee, jobject obj, ...) {
    CVMFrame* frame;
    CVMassert(CVMframeIsFreelist(CVMeeGetCurrentFrame(ee)));
    CVMMethodBlock* methodID = ee->callee;
    if (CVMlocalExceptionOccurred(ee)) {
        return 0.0;
    }
    
    frame = (CVMFrame*)CVMpushTransitionFrame(ee, methodID);
    if (frame == NULL) {
	CVMassert(CVMexceptionOccurred(ee));
	return 0.0;  /* exception already thrown */
    }
    CVMBool isStatic = CVMmbIs(methodID, STATIC); // TODO 
    CVMassert(!isStatic);
    //CVMBool isVirtual = !isStatic; // SLOW
    //CVMUint16 retType; // TODO
    
    /* Push the "this" argument if the method is not static. */
    if (!isStatic) {
	CVMID_icellAssignDirect(ee, &frame->topOfStack->j.r, obj);
	frame->topOfStack++;
    }

	CVMterseSigIterator terseSig;
    /* Push the all the other arguments. */
    {
	CVMStackVal32 *topOfStack = frame->topOfStack;
	int safeCount = AOTC_SAFE_COUNT;
	int typeSyllable;
	va_list args;
	va_start(args, obj);

	CVMtypeidGetTerseSignatureIterator(CVMmbNameAndTypeID(methodID), &terseSig);

	while((typeSyllable=CVM_TERSE_ITER_NEXT(terseSig))!=CVM_TYPEID_ENDFUNC) {
	    if(safeCount-- == 0) {
		CVMD_gcSafeCheckPoint(ee, { frame->topOfStack = topOfStack; }, {});
		safeCount = AOTC_SAFE_COUNT;
	    }

	    switch(typeSyllable) {
		case CVM_TYPEID_BOOLEAN:
		case CVM_TYPEID_SHORT:
		case CVM_TYPEID_BYTE:
		case CVM_TYPEID_CHAR:
		case CVM_TYPEID_INT:
		    (topOfStack++)->j.i = va_arg(args, jint);
		    continue;
		case CVM_TYPEID_FLOAT:
		    (topOfStack++)->j.f = va_arg(args, jdouble);
		    continue;
		case CVM_TYPEID_OBJ: {
		    jobject o = va_arg(args, jobject);
		    CVMID_icellAssignDirect(ee, &topOfStack->j.r, o);

		    topOfStack++;
		    continue;
		}
		case CVM_TYPEID_LONG: {
		    jlong l = va_arg(args, jlong);
		    CVMlong2Jvm(&topOfStack->j.raw, l);
		    topOfStack += 2;
		    continue;
		}
		case CVM_TYPEID_DOUBLE: {
		    jdouble d = va_arg(args, jdouble);
		    CVMdouble2Jvm(&topOfStack->j.raw, d);
		    topOfStack += 2;
		    continue;
		}
		default:
		    CVMassert(0);
		    return 0.0;
	    }
	}
	va_end(args);
	frame->topOfStack = topOfStack;
    }

    CVMassert(!CVMlocalExceptionOccurred(ee));

    /* Call the java method. */
    //CVMgcUnsafeExecuteJavaMethod(ee, methodID, isStatic, !isStatic);
    CVMgcUnsafeExecuteJavaMethod(ee, methodID, CVM_FALSE, CVM_TRUE);

    /* 
     * Copy the result. frame->topOfStack will point after the result.
     */
    jfloat retValue = (jfloat)frame->topOfStack[-1].j.f;
#if 0
    void* retValue;
    CVMTypeIDTypePart retType = CVM_TERSE_ITER_RETURNTYPE(terseSig);
    if (!CVMlocalExceptionOccurred(ee) /* && retValue */) {
	switch (retType) {
	    case CVM_TYPEID_VOID:
		break;
	    case CVM_TYPEID_BOOLEAN:
	    case CVM_TYPEID_BYTE:
	    case CVM_TYPEID_CHAR:
	    case CVM_TYPEID_SHORT:
	    case CVM_TYPEID_INT:
		retValue = (void*)frame->topOfStack[-1].j.i;
		break;
	    case CVM_TYPEID_LONG: {
		// retValue = (void*)CVMjvm2Long(&frame->topOfStack[-2].j.raw); // TODO
		break;
	    }
	    case CVM_TYPEID_FLOAT:
		// retValue = (void*)frame->topOfStack[-1].j.f; // TODO
		break;
	    case CVM_TYPEID_DOUBLE: {
		// retValue = (void*)CVMjvm2Double(&frame->topOfStack[-2].j.raw); // TODO
		break;
	    }
	    case CVM_TYPEID_OBJ:
		retValue = (void*)frame->topOfStack[-1].j.r.ref_DONT_ACCESS_DIRECTLY;
		break;
	}
    }
#endif
    /* Pop the transition frame. */
    CVMpopFrame(&ee->interpreterStack, ee->interpreterStack.currentFrame);
    return retValue;
}

jdouble InvokeInterpreterD(CVMExecEnv *ee, jobject obj, ...) {
    CVMFrame* frame;
    CVMassert(CVMframeIsFreelist(CVMeeGetCurrentFrame(ee)));
    CVMMethodBlock* methodID = ee->callee;
    if (CVMlocalExceptionOccurred(ee)) {
        return 0.0;
    }
    
    frame = (CVMFrame*)CVMpushTransitionFrame(ee, methodID);
    if (frame == NULL) {
	CVMassert(CVMexceptionOccurred(ee));
	return 0.0;  /* exception already thrown */
    }
    CVMBool isStatic = CVMmbIs(methodID, STATIC); // TODO 
    CVMassert(!isStatic);
    //CVMBool isVirtual = !isStatic; // SLOW
    //CVMUint16 retType; // TODO
    
    /* Push the "this" argument if the method is not static. */
    if (!isStatic) {
	CVMID_icellAssignDirect(ee, &frame->topOfStack->j.r, obj);
	frame->topOfStack++;
    }

	CVMterseSigIterator terseSig;
    /* Push the all the other arguments. */
    {
	CVMStackVal32 *topOfStack = frame->topOfStack;
	int safeCount = AOTC_SAFE_COUNT;
	int typeSyllable;
	va_list args;
	va_start(args, obj);

	CVMtypeidGetTerseSignatureIterator(CVMmbNameAndTypeID(methodID), &terseSig);

	while((typeSyllable=CVM_TERSE_ITER_NEXT(terseSig))!=CVM_TYPEID_ENDFUNC) {
	    if(safeCount-- == 0) {
		CVMD_gcSafeCheckPoint(ee, { frame->topOfStack = topOfStack; }, {});
		safeCount = AOTC_SAFE_COUNT;
	    }

	    switch(typeSyllable) {
		case CVM_TYPEID_BOOLEAN:
		case CVM_TYPEID_SHORT:
		case CVM_TYPEID_BYTE:
		case CVM_TYPEID_CHAR:
		case CVM_TYPEID_INT:
		    (topOfStack++)->j.i = va_arg(args, jint);
		    continue;
		case CVM_TYPEID_FLOAT:
		    (topOfStack++)->j.f = va_arg(args, jdouble);
		    continue;
		case CVM_TYPEID_OBJ: {
		    jobject o = va_arg(args, jobject);
		    CVMID_icellAssignDirect(ee, &topOfStack->j.r, o);

		    topOfStack++;
		    continue;
		}
		case CVM_TYPEID_LONG: {
		    jlong l = va_arg(args, jlong);
		    CVMlong2Jvm(&topOfStack->j.raw, l);
		    topOfStack += 2;
		    continue;
		}
		case CVM_TYPEID_DOUBLE: {
		    jdouble d = va_arg(args, jdouble);
		    CVMdouble2Jvm(&topOfStack->j.raw, d);
		    topOfStack += 2;
		    continue;
		}
		default:
		    CVMassert(0);
		    return 0.0;
	    }
	}
	va_end(args);
	frame->topOfStack = topOfStack;
    }

    CVMassert(!CVMlocalExceptionOccurred(ee));

    /* Call the java method. */
    //CVMgcUnsafeExecuteJavaMethod(ee, methodID, isStatic, !isStatic);
    CVMgcUnsafeExecuteJavaMethod(ee, methodID, CVM_FALSE, CVM_TRUE);

    /* 
     * Copy the result. frame->topOfStack will point after the result.
     */
    jdouble retValue = CVMjvm2Double(&frame->topOfStack[-2].j.raw);
#if 0
    void* retValue;
    CVMTypeIDTypePart retType = CVM_TERSE_ITER_RETURNTYPE(terseSig);
    if (!CVMlocalExceptionOccurred(ee) /* && retValue */) {
	switch (retType) {
	    case CVM_TYPEID_VOID:
		break;
	    case CVM_TYPEID_BOOLEAN:
	    case CVM_TYPEID_BYTE:
	    case CVM_TYPEID_CHAR:
	    case CVM_TYPEID_SHORT:
	    case CVM_TYPEID_INT:
		retValue = (void*)frame->topOfStack[-1].j.i;
		break;
	    case CVM_TYPEID_LONG: {
		// retValue = (void*)CVMjvm2Long(&frame->topOfStack[-2].j.raw); // TODO
		break;
	    }
	    case CVM_TYPEID_FLOAT:
		// retValue = (void*)frame->topOfStack[-1].j.f; // TODO
		break;
	    case CVM_TYPEID_DOUBLE: {
		// retValue = (void*)CVMjvm2Double(&frame->topOfStack[-2].j.raw); // TODO
		break;
	    }
	    case CVM_TYPEID_OBJ:
		retValue = (void*)frame->topOfStack[-1].j.r.ref_DONT_ACCESS_DIRECTLY;
		break;
	}
    }
#endif
    /* Pop the transition frame. */
    CVMpopFrame(&ee->interpreterStack, ee->interpreterStack.currentFrame);
    return retValue;
}

CVMObject* InvokeInterpreterL(CVMExecEnv *ee, jobject obj, ...) {
    CVMFrame* frame;
    CVMassert(CVMframeIsFreelist(CVMeeGetCurrentFrame(ee)));
    CVMMethodBlock* methodID = ee->callee;
    if (CVMlocalExceptionOccurred(ee)) {
        return NULL;
    }
    
    frame = (CVMFrame*)CVMpushTransitionFrame(ee, methodID);
    if (frame == NULL) {
	CVMassert(CVMexceptionOccurred(ee));
	return NULL;  /* exception already thrown */
    }
    CVMBool isStatic = CVMmbIs(methodID, STATIC); // TODO 
    CVMassert(!isStatic);
    //CVMBool isVirtual = !isStatic; // SLOW
    //CVMUint16 retType; // TODO
    
    /* Push the "this" argument if the method is not static. */
    if (!isStatic) {
	CVMID_icellAssignDirect(ee, &frame->topOfStack->j.r, obj);
	frame->topOfStack++;
    }

	CVMterseSigIterator terseSig;
    /* Push the all the other arguments. */
    {
	CVMStackVal32 *topOfStack = frame->topOfStack;
	int safeCount = AOTC_SAFE_COUNT;
	int typeSyllable;
	va_list args;
	va_start(args, obj);

	CVMtypeidGetTerseSignatureIterator(CVMmbNameAndTypeID(methodID), &terseSig);

	while((typeSyllable=CVM_TERSE_ITER_NEXT(terseSig))!=CVM_TYPEID_ENDFUNC) {
	    if(safeCount-- == 0) {
		CVMD_gcSafeCheckPoint(ee, { frame->topOfStack = topOfStack; }, {});
		safeCount = AOTC_SAFE_COUNT;
	    }

	    switch(typeSyllable) {
		case CVM_TYPEID_BOOLEAN:
		case CVM_TYPEID_SHORT:
		case CVM_TYPEID_BYTE:
		case CVM_TYPEID_CHAR:
		case CVM_TYPEID_INT:
		    (topOfStack++)->j.i = va_arg(args, jint);
		    continue;
		case CVM_TYPEID_FLOAT:
		    (topOfStack++)->j.f = va_arg(args, jdouble);
		    continue;
		case CVM_TYPEID_OBJ: {
		    jobject o = va_arg(args, jobject);
		    CVMID_icellAssignDirect(ee, &topOfStack->j.r, o);

		    topOfStack++;
		    continue;
		}
		case CVM_TYPEID_LONG: {
		    jlong l = va_arg(args, jlong);
		    CVMlong2Jvm(&topOfStack->j.raw, l);
		    topOfStack += 2;
		    continue;
		}
		case CVM_TYPEID_DOUBLE: {
		    jdouble d = va_arg(args, jdouble);
		    CVMdouble2Jvm(&topOfStack->j.raw, d);
		    topOfStack += 2;
		    continue;
		}
		default:
		    CVMassert(0);
		    return NULL;
	    }
	}
	va_end(args);
	frame->topOfStack = topOfStack;
    }

    CVMassert(!CVMlocalExceptionOccurred(ee));

    /* Call the java method. */
    //CVMgcUnsafeExecuteJavaMethod(ee, methodID, isStatic, !isStatic);
    CVMgcUnsafeExecuteJavaMethod(ee, methodID, CVM_FALSE, CVM_TRUE);

    /* 
     * Copy the result. frame->topOfStack will point after the result.
     */
    CVMObject* retValue = frame->topOfStack[-1].j.r.ref_DONT_ACCESS_DIRECTLY;
#if 0
    void* retValue;
    CVMTypeIDTypePart retType = CVM_TERSE_ITER_RETURNTYPE(terseSig);
    if (!CVMlocalExceptionOccurred(ee) /* && retValue */) {
	switch (retType) {
	    case CVM_TYPEID_VOID:
		break;
	    case CVM_TYPEID_BOOLEAN:
	    case CVM_TYPEID_BYTE:
	    case CVM_TYPEID_CHAR:
	    case CVM_TYPEID_SHORT:
	    case CVM_TYPEID_INT:
		retValue = (void*)frame->topOfStack[-1].j.i;
		break;
	    case CVM_TYPEID_LONG: {
		// retValue = (void*)CVMjvm2Long(&frame->topOfStack[-2].j.raw); // TODO
		break;
	    }
	    case CVM_TYPEID_FLOAT:
		// retValue = (void*)frame->topOfStack[-1].j.f; // TODO
		break;
	    case CVM_TYPEID_DOUBLE: {
		// retValue = (void*)CVMjvm2Double(&frame->topOfStack[-2].j.raw); // TODO
		break;
	    }
	    case CVM_TYPEID_OBJ:
		retValue = (void*)frame->topOfStack[-1].j.r.ref_DONT_ACCESS_DIRECTLY;
		break;
	}
    }
#endif
    /* Pop the transition frame. */
    CVMpopFrame(&ee->interpreterStack, ee->interpreterStack.currentFrame);
    return retValue;
}
#endif

void AOTCInvoke(CVMExecEnv *ee, jobject obj, jmethodID methodID, CVMUint16 retType,
		AOTCreturn *retValue, CVMBool isStatic, CVMBool isVirtual, ...)
{   
    CVMFrame*     frame;

    CVMassert(CVMframeIsFreelist(CVMeeGetCurrentFrame(ee)));
    //CVMconsolePrintf("ClassName : %O, MethodName : %M\n", obj->ref_DONT_ACCESS_DIRECTLY, methodID);
    if (CVMlocalExceptionOccurred(ee)) {
	return;
    }

    frame = (CVMFrame*)CVMpushTransitionFrame(ee, methodID);
    if (frame == NULL) {
	CVMassert(CVMexceptionOccurred(ee));
	return;  /* exception already thrown */
    }

    CVMassert(!CVMlocalExceptionOccurred(ee));

    /* Push the "this" argument if the method is not static. */
    if (!isStatic) {
	CVMID_icellAssignDirect(ee, &frame->topOfStack->j.r, obj);
	frame->topOfStack++;
    }

    /* Push the all the other arguments. */
    {
	CVMterseSigIterator terseSig;
	CVMStackVal32 *topOfStack = frame->topOfStack;
	int safeCount = AOTC_SAFE_COUNT;
	int typeSyllable;
	va_list args;
	va_start(args, isVirtual);

	CVMtypeidGetTerseSignatureIterator(CVMmbNameAndTypeID(methodID), &terseSig);

	while((typeSyllable=CVM_TERSE_ITER_NEXT(terseSig))!=CVM_TYPEID_ENDFUNC) {
	    if(safeCount-- == 0) {
		CVMD_gcSafeCheckPoint(ee, { frame->topOfStack = topOfStack; }, {});
		safeCount = AOTC_SAFE_COUNT;
	    }

	    switch(typeSyllable) {
		case CVM_TYPEID_BOOLEAN:
		case CVM_TYPEID_SHORT:
		case CVM_TYPEID_BYTE:
		case CVM_TYPEID_CHAR:
		case CVM_TYPEID_INT:
		    (topOfStack++)->j.i = va_arg(args, jint);
		    continue;
		case CVM_TYPEID_FLOAT:
		    (topOfStack++)->j.f = va_arg(args, jdouble);
		    continue;
		case CVM_TYPEID_OBJ: {
		    jobject o = va_arg(args, jobject);
		    CVMID_icellAssignDirect(ee, &topOfStack->j.r, o);

		    topOfStack++;
		    continue;
		}
		case CVM_TYPEID_LONG: {
		    jlong l = va_arg(args, jlong);
		    CVMlong2Jvm(&topOfStack->j.raw, l);
		    topOfStack += 2;
		    continue;
		}
		case CVM_TYPEID_DOUBLE: {
		    jdouble d = va_arg(args, jdouble);
		    CVMdouble2Jvm(&topOfStack->j.raw, d);
		    topOfStack += 2;
		    continue;
		}
		default:
		    CVMassert(CVM_FALSE);
		    return;
	    }
	}
	va_end(args);
	frame->topOfStack = topOfStack;
    }

    CVMassert(!CVMlocalExceptionOccurred(ee));

    /* Call the java method. */
    CVMgcUnsafeExecuteJavaMethod(ee, methodID, isStatic, isVirtual);

    /* 
     * Copy the result. frame->topOfStack will point after the result.
     */
    if (!CVMlocalExceptionOccurred(ee) && retValue) {
	switch (retType) {
	    case CVM_TYPEID_VOID:
		break;
	    case CVM_TYPEID_BOOLEAN:
	    case CVM_TYPEID_BYTE:
	    case CVM_TYPEID_CHAR:
	    case CVM_TYPEID_SHORT:
	    case CVM_TYPEID_INT:
		retValue->i = frame->topOfStack[-1].j.i;
		break;
	    case CVM_TYPEID_LONG: {
		retValue->j = CVMjvm2Long(&frame->topOfStack[-2].j.raw);
		break;
	    }
	    case CVM_TYPEID_FLOAT:
		retValue->f = (jfloat)frame->topOfStack[-1].j.f;
		break;
	    case CVM_TYPEID_DOUBLE: {
		retValue->d = CVMjvm2Double(&frame->topOfStack[-2].j.raw);
		break;
	    }
	    case CVM_TYPEID_OBJ:
		retValue->l = frame->topOfStack[-1].j.r.ref_DONT_ACCESS_DIRECTLY;
		break;
	}
    }

    /* Pop the transition frame. */
    //CVMpopFrame(&ee->interpreterStack, ee->interpreterStack.currentFrame);
    CVMpopFrame(&ee->interpreterStack, ee->interpreterStack.currentFrame);
}


/*
 * java/lang/String AOTC-interface native methods
 */

#undef MIN
#define MIN(x,y) ((x) < (y) ? (x) : (y))

#undef FIELD_READ_COUNT
#define FIELD_READ_COUNT(obj, val) \
CVMD_fieldReadInt(obj,         \
	CVMoffsetOfjava_lang_String_count, val) 

#undef FIELD_READ_VALUE
#define FIELD_READ_VALUE(obj, val) \
CVMD_fieldReadRef(obj,         \
	CVMoffsetOfjava_lang_String_value, val)

#undef FIELD_READ_OFFSET
#define FIELD_READ_OFFSET(obj, val) \
CVMD_fieldReadInt(obj,          \
	CVMoffsetOfjava_lang_String_offset, val)

/*
 * The rate for periodically offering gcSafeCheckPoint
 * in while loop.
 */
#define GCSAFE_CHECKPOINT_RATE 100

/*
 * Class:       java/lang/String
 * Method:      charAt
 * Signature:   (I)C
 */
CVMJavaChar
AOTC_java_lang_String_charAt(CVMExecEnv* ee, CVMObjectICell* thisICell, CVMJavaInt index)
{
    CVMObject *thisStringObject;
    CVMJavaInt count;
    CVMJavaInt offset;
    CVMObject *valueObject;
    CVMArrayOfChar *value;
    CVMJavaChar c;
    
    /* CNI policy: offer a gc-safe checkpoint */
    CVMD_gcSafeCheckPoint(ee, {}, {});

    thisStringObject = CVMID_icellDirect(ee, thisICell);
    FIELD_READ_COUNT(thisStringObject, count);
    
    if ((index < 0) || (index >= count)) {
        CVMthrowStringIndexOutOfBoundsException(
            ee, "String index out of range: %d", index);
        return 0;
    }

    FIELD_READ_VALUE(thisStringObject, valueObject);
    FIELD_READ_OFFSET(thisStringObject, offset);
    value = (CVMArrayOfChar *)valueObject;
    CVMD_arrayReadChar(value, index+offset, c);
    return c;
}

/*
 * Class:       java/lang/String
 * Method:      getChars
 * Signature:   (II[CI)V
 */
void AOTC_java_lang_String_getChars(CVMExecEnv* ee, jobject cls, CVMJavaInt srcBegin, CVMJavaInt srcEnd,
				    jobject dstobj, CVMJavaInt dstBegin)
{
    CVMObject *thisStringObject;
    CVMArrayOfChar *dst; 

    CVMJavaInt count;
    CVMJavaInt offset;
    CVMJavaInt dstLen;
    CVMObject *valueObject;
    CVMArrayOfChar *value;

    CVMClassBlock *dstCb;

    /* CNI policy: offer a gc-safe checkpoint */
    CVMD_gcSafeCheckPoint(ee, {}, {});

    thisStringObject = CVMID_icellDirect(ee, cls);
    dst = (CVMArrayOfChar *)CVMID_icellDirect(ee, dstobj);

    /* Copy from the Java version of String.getChars*/
    if (srcBegin < 0) {
        CVMthrowStringIndexOutOfBoundsException(
            ee, "String index out of range: %d", srcBegin);
        return;
    } 

    FIELD_READ_COUNT(thisStringObject, count);
    if (srcEnd > count) {
        CVMthrowStringIndexOutOfBoundsException(
            ee, "String index out of range: %d", srcEnd);
        return;
    }

    if (srcBegin > srcEnd) {
        CVMthrowStringIndexOutOfBoundsException(
            ee, "String index out of range: %d", srcEnd - srcBegin);
        return;
    }

    /* Check NULL for dstObject */
    if (dst == NULL) {
        CVMthrowNullPointerException(ee, NULL);
        return;
    }

    /* Check whether dst is a char array */
    dstCb = (CVMClassBlock*)CVMobjectGetClass(dst);
    //dstCb = CVMobjectGetClass(dst);
    if (!CVMisArrayClass(dstCb) || CVMarrayBaseType(dstCb) != CVM_T_CHAR) {
        CVMthrowArrayStoreException(ee, NULL);
        return;
    }

    /* Array bounds checking for dst*/
    dstLen = CVMD_arrayGetLength(dst);
    if (dstBegin < 0 || dstBegin > dstLen || 
        dstBegin + srcEnd - srcBegin > dstLen) {
        CVMthrowArrayIndexOutOfBoundsException(ee, NULL);
        return; 
    }

    FIELD_READ_VALUE(thisStringObject, valueObject);
    FIELD_READ_OFFSET(thisStringObject, offset);
    value = (CVMArrayOfChar *)valueObject;
    CVMD_arrayCopyChar(value, offset + srcBegin, 
                       dst, dstBegin, srcEnd - srcBegin); 
    return; 
}
 

/*
 * Class:       java/lang/String
 * Method:      equals
 * Signature:   (Ljava/lang/Object;)Z
 */
CVMJavaBoolean
AOTC_java_lang_String_equals(CVMExecEnv* ee, jobject cls, jobject obj)
{
    CVMObject *thisStringObject = CVMID_icellDirect(ee, cls);
    CVMObject *anObject = CVMID_icellDirect(ee, obj);
    
    if (thisStringObject == anObject) {
	return CVM_TRUE;
    } else if (anObject == NULL) {
	return CVM_FALSE;
    }

    if ((CVMClassBlock*)CVMobjectGetClass(anObject) == CVMsystemClass(java_lang_String)) {
    //if (CVMobjectGetClass(anObject) == CVMsystemClass(java_lang_String)) {
        CVMJavaInt count1;
        CVMJavaInt count2;
        /* The argument anObject is a String. So compare. */
        FIELD_READ_COUNT(thisStringObject, count1);
        FIELD_READ_COUNT(anObject, count2);
        if (count1 == count2) {
            CVMObject *valueObject1;
            CVMObject *valueObject2;
            CVMArrayOfChar *v1;
            CVMArrayOfChar *v2;
            CVMJavaInt i;
            CVMJavaInt j;
            CVMJavaChar c1;
            CVMJavaChar c2;
            CVMJavaInt innerCount;

            FIELD_READ_VALUE(thisStringObject, valueObject1);
            FIELD_READ_VALUE(anObject, valueObject2);
            v1 = (CVMArrayOfChar *)valueObject1;
            v2 = (CVMArrayOfChar *)valueObject2; 

            FIELD_READ_OFFSET(thisStringObject, i); 
            FIELD_READ_OFFSET(anObject, j);

            while (count1 > 0) {
                innerCount = MIN(count1, GCSAFE_CHECKPOINT_RATE);
                count1 = count1 - innerCount;
                while (innerCount-- > 0) {
                    CVMD_arrayReadChar(v1, i, c1);
                    CVMD_arrayReadChar(v2, j, c2);
                    if (c1 != c2) {
                        return CVM_FALSE; 
                    }
                    i++;
                    j++;
                }

                /* CNI policy: offer a gc-safe checkpoint periodically */
                CVMD_gcSafeCheckPoint(ee, {}, {
                    thisStringObject = CVMID_icellDirect(ee, cls);
                    anObject = CVMID_icellDirect(ee, obj);
                    FIELD_READ_VALUE(thisStringObject, valueObject1); 
                    FIELD_READ_VALUE(anObject, valueObject2); 
                    v1 = (CVMArrayOfChar *)valueObject1;
                    v2 = (CVMArrayOfChar *)valueObject2;
                });
            }
            return CVM_TRUE;
        } 
    }
    
    return CVM_FALSE;
}

/*
 * Class:       java/lang/String
 * Method:      compareTo
 * Signature:   (Ljava/lang/String;)I
 */
CVMJavaInt
AOTC_java_lang_String_compareTo(CVMExecEnv* ee, jobject cls, jobject obj)
{
    CVMObject* thisStringObject = CVMID_icellDirect(ee, cls);
    CVMObject* anotherStringObject = CVMID_icellDirect(ee, obj);
    
    CVMJavaInt len1;
    CVMJavaInt len2;
    CVMJavaInt n;

    CVMObject *valueObject1;
    CVMObject *valueObject2;
    CVMArrayOfChar *v1;
    CVMArrayOfChar *v2;

    CVMJavaInt i;
    CVMJavaInt j;

    CVMJavaChar c1;
    CVMJavaChar c2;

    CVMJavaInt innerCount;

    /* 
     * If anObject is null, a NullPointerException
     * should be thrown. The interpreter already
     * did CHECK_NULL for this String.
     */
    if (anotherStringObject == NULL) {
        CVMthrowNullPointerException(ee, NULL);
        return 0;
    }

    /* Get count, value, and offset */
    FIELD_READ_COUNT(thisStringObject, len1);
    FIELD_READ_COUNT(anotherStringObject, len2);
    n = MIN(len1, len2);

    FIELD_READ_VALUE(thisStringObject, valueObject1);
    FIELD_READ_VALUE(anotherStringObject, valueObject2);
    v1 = (CVMArrayOfChar *)valueObject1;
    v2 = (CVMArrayOfChar *)valueObject2; 

    FIELD_READ_OFFSET(thisStringObject, i);
    FIELD_READ_OFFSET(anotherStringObject, j);

    /* Now compare */
    if (i == j) {
        /* This is an optimized case */
        while (n > 0) {
            innerCount = MIN(n, GCSAFE_CHECKPOINT_RATE);
            n = n - innerCount;
            while (innerCount-- > 0) {
                CVMD_arrayReadChar(v1, i, c1);
                CVMD_arrayReadChar(v2, i, c2);
                if (c1 != c2) {
		    return (CVMJavaInt)c1 - (CVMJavaInt)c2;
                }
                i++;
            }

            /* CNI policy: offer a gc-safe checkpoint periodically */
            CVMD_gcSafeCheckPoint(ee, {}, {
                thisStringObject = CVMID_icellDirect(ee, cls);
                anotherStringObject = CVMID_icellDirect(ee, obj);
                FIELD_READ_VALUE(thisStringObject, valueObject1);
                FIELD_READ_VALUE(anotherStringObject, valueObject2);
                v1 = (CVMArrayOfChar *)valueObject1;
                v2 = (CVMArrayOfChar *)valueObject2;
            });
        }
    } else {
        while (n > 0) {
            innerCount = MIN(n, GCSAFE_CHECKPOINT_RATE);
            n = n - innerCount;
            while (innerCount-- > 0) { 
                CVMD_arrayReadChar(v1, i, c1);
                CVMD_arrayReadChar(v2, j, c2);
                i++;
                j++;
                if (c1 != c2) {
		    return (CVMJavaInt)c1 - (CVMJavaInt)c2;
                }
            }

            /* CNI policy: offer a gc-safe checkpoint periodically */
            CVMD_gcSafeCheckPoint(ee, {}, {
                thisStringObject = CVMID_icellDirect(ee, cls);
                anotherStringObject = CVMID_icellDirect(ee, obj);
                FIELD_READ_VALUE(thisStringObject, valueObject1);
                FIELD_READ_VALUE(anotherStringObject, valueObject2);
                v1 = (CVMArrayOfChar *)valueObject1;
                v2 = (CVMArrayOfChar *)valueObject2;
            });
        }
    }
    return len1 - len2;
}

/*
 * Class:       java/lang/String
 * Method:      hashCode
 * Signature:   ()I
 */
// AOT - BY Clamp ************ 
CVMJavaInt
AOTC_java_lang_String_hashCode(CVMExecEnv* ee, CVMObjectICell* thisICell)
{
    CVMObject *thisObject = CVMID_icellDirect(ee, thisICell);
    CVMObject *valueObject;
    CVMArrayOfChar *value;
    CVMJavaInt offset;
    CVMJavaInt count;
    CVMJavaChar c;
    CVMJavaInt h = 0;
    FIELD_READ_VALUE(thisObject, valueObject);
    FIELD_READ_OFFSET(thisObject, offset);
    FIELD_READ_COUNT(thisObject, count);
    value = (CVMArrayOfChar *)valueObject;

    while (count-- > 0 ) {
        CVMD_arrayReadChar(value, offset, c);
        h = 31*h + (CVMJavaInt)c;
        offset ++;
    }

    return h; 
}

/*
 * Helper function for indexOf(I)I and indexOf(II)I.
 */
static CVMJavaInt
stringIndexOfHelper1(CVMExecEnv* ee,  CVMObjectICell *thisICell,
                     CVMJavaInt ch, CVMJavaInt fromIndex) 
{
    CVMObject *thisObj = CVMID_icellDirect(ee, thisICell);
    CVMObject *valueObj;
    CVMArrayOfChar *valueArr;
    CVMJavaInt offset;
    CVMJavaInt count;
    CVMJavaInt max;
    CVMJavaInt innerMax;
    CVMJavaInt i;
    CVMJavaChar c;

    FIELD_READ_COUNT(thisObj, count);
 
    /* The argument fromIndex is always >=0. Guaranteed by caller.*/ 
    if (fromIndex >= count) {
        return -1;
    }
   
    FIELD_READ_OFFSET(thisObj, offset);
    FIELD_READ_VALUE(thisObj, valueObj);
    valueArr = (CVMArrayOfChar *)valueObj;

    max = offset + count;
    i = offset + fromIndex;
    while (i < max) {
        innerMax = MIN(max, i+GCSAFE_CHECKPOINT_RATE);
        while (i < innerMax) {
            CVMD_arrayReadChar(valueArr, i, c);
            if (c == ch) {
                return i - offset;
            }
            i++;
        }
        CVMD_gcSafeCheckPoint(ee, {}, {
            thisObj = CVMID_icellDirect(ee, thisICell);
            FIELD_READ_VALUE(thisObj, valueObj);
            valueArr = (CVMArrayOfChar *)valueObj;
        });
    }
    /* Not found. Return -1. */
    return -1;
}

/*
 * Class:       java/lang/String
 * Method:      indexOf
 * Signature:   (I)I
 */
CVMJavaInt
AOTC_java_lang_String_indexOf__I(CVMExecEnv* ee, CVMObjectICell* thisICell, CVMJavaInt ch)
{
   return stringIndexOfHelper1(ee, thisICell, ch, 0);
}

/*
 * Class:       java/lang/String
 * Method:      indexOf
 * Signature:   (II)I
 */
CVMJavaInt
AOTC_java_lang_String_indexOf__II(CVMExecEnv* ee, CVMObjectICell* thisICell, CVMJavaInt ch, CVMJavaInt fromIndex)
{
    if (fromIndex < 0) {
	fromIndex = 0;
    }
 
    return stringIndexOfHelper1(ee, thisICell, ch, fromIndex);
}

/*
 * Helper function for indexOf(Ljava/lang/String;)I
 * and indexOf(Ljava/lang/String;I)I.
 */
static CVMJavaInt
stringIndexOfHelper2(CVMExecEnv* ee, CVMObjectICell *thisICell,
                     CVMObjectICell *strICell, CVMJavaInt fromIndex)
{
   CVMObject *thisObj = CVMID_icellDirect(ee, thisICell);
   CVMObject *strObj = CVMID_icellDirect(ee, strICell);
   CVMObject *vObj1;
   CVMObject *vObj2;
   CVMArrayOfChar *v1;
   CVMArrayOfChar *v2;
   CVMJavaInt offset;
   CVMJavaInt strOffset;
   CVMJavaInt count;
   CVMJavaInt strCount;
   CVMJavaInt max;
   CVMJavaChar first;
   CVMJavaChar c1;
   CVMJavaChar c2;
   CVMJavaInt i; /* Iterates v1 searching for the first char of v2 */
   CVMJavaInt j; /* Iterates v1 searching for the rest of v2 */
   CVMJavaInt k; /* Iterates v2 */
   CVMJavaInt end;

   CVMJavaInt innerMax, innerEnd; 

   /* The caller is responsible for NULL checking for strICell. */
   FIELD_READ_COUNT(thisObj, count);
   FIELD_READ_COUNT(strObj, strCount);

   if (fromIndex >= count) {
       if (count == 0 && fromIndex == 0 && strCount == 0) {
           /* 
            * Special case: An empty string at 
            * index 0 in an empty string. 
            */
           return 0;
       }
       return -1;
   }
   if (fromIndex < 0) {
       fromIndex = 0;
   }
   if (strCount == 0) {
       return fromIndex;
   }

   FIELD_READ_VALUE(thisObj, vObj1);
   FIELD_READ_VALUE(strObj, vObj2);
   FIELD_READ_OFFSET(thisObj, offset);
   FIELD_READ_OFFSET(strObj, strOffset);
   v1 = (CVMArrayOfChar *)vObj1;
   v2 = (CVMArrayOfChar *)vObj2;

   CVMD_arrayReadChar(v2, strOffset, first);
   max = offset + (count - strCount);
   i = offset + fromIndex;

   while (i <= max) {
       innerMax = MIN(max, i+GCSAFE_CHECKPOINT_RATE);
       while (i <= innerMax) {
           /* Search for first character. */
           CVMD_arrayReadChar(v1, i, c1);
           if (c1 == first) {
               /* Found first character, now look at the rest of v2 */
               j = i + 1;
               end = i + strCount;
               k = strOffset + 1;
               while(j < end) {
                   innerEnd = MIN(end, j+GCSAFE_CHECKPOINT_RATE);
                   while (j < innerEnd) {
                       CVMD_arrayReadChar(v1, j, c1);
                       CVMD_arrayReadChar(v2, k, c2);
                       if (c1 != c2) {
                           goto continueSearch;
                       }
                       j++;
                       k++;
                   }
                   CVMD_gcSafeCheckPoint(ee, {}, {
                       thisObj = CVMID_icellDirect(ee, thisICell);
                       strObj = CVMID_icellDirect(ee, strICell);
                       FIELD_READ_VALUE(thisObj, vObj1);
                       FIELD_READ_VALUE(strObj, vObj2);
                       v1 = (CVMArrayOfChar *)vObj1;
                       v2 = (CVMArrayOfChar *)vObj2;
                   });
               }
               /* Found */
               return i - offset;
           }
continueSearch:
           i++;
       }
       CVMD_gcSafeCheckPoint(ee, {}, {
           thisObj = CVMID_icellDirect(ee, thisICell);
           strObj = CVMID_icellDirect(ee, strICell);
           FIELD_READ_VALUE(thisObj, vObj1);
           FIELD_READ_VALUE(strObj, vObj2);
           v1 = (CVMArrayOfChar *)vObj1;
           v2 = (CVMArrayOfChar *)vObj2;
       });
   }
   /* If we are here, then we have failed to find v2 in v1.*/
   return -1;
}

/*
 * Class:       java/lang/String
 * Method:      indexOf
 * Signature:   (Ljava/lang/String;)I
 */
CVMJavaInt
AOTC_java_lang_String_indexOf__Ljava_lang_String_2(CVMExecEnv* ee, CVMObjectICell* thisICell, CVMObjectICell* strICell)
{
    /* NULL check for the argument string */ 
    if (CVMID_icellIsNull(strICell)) {
        CVMthrowNullPointerException(ee, NULL);
        return 0;
    }

    return stringIndexOfHelper2(ee, thisICell, strICell, 0);
}

/*
 * Class:       java/lang/String
 * Method:      indexOf
 * Signature:   (Ljava/lang/String;I)I
 */
CVMJavaInt
AOTC_java_lang_String_indexOf__Ljava_lang_String_2I(CVMExecEnv* ee, CVMObjectICell* thisICell, CVMObjectICell* strICell, CVMJavaInt fromIndex)
{
   /* NULL check for the argument string */
   if (CVMID_icellIsNull(strICell)) {
       CVMthrowNullPointerException(ee, NULL);
       return 0;
   }

   return stringIndexOfHelper2(ee, thisICell, strICell, fromIndex);
}

CVMObject*
AOTC_java_lang_String_intern(CVMExecEnv* ee, CVMObjectICell* thisICell)
{
    CVMObjectICell *thisString = thisICell;
    JNIEnv* env = CVMexecEnv2JniEnv(ee);
    CVMObjectICell *result = NULL;

    CVMD_gcSafeExec(ee, {
        if ((*env)->PushLocalFrame(env, 4) == 0) {
            result = (CVMObjectICell *)JVM_InternString(
                env, (jobject)thisString);
            (*env)->PopLocalFrame(env, NULL);
        }
    });
    return result->ref_DONT_ACCESS_DIRECTLY;
}


/*
 * java/lang/StringBuffer AOTC-interface native methods
 */

#define JAVA_LANG_INTEGER_MAX_VALUE (0x7fffffff)

#undef FIELD_READ_COUNT
#define FIELD_READ_COUNT(obj, val) \
    CVMD_fieldReadInt(obj,         \
        CVMoffsetOfjava_lang_StringBuffer_count, val)

#undef FIELD_READ_VALUE
#define FIELD_READ_VALUE(obj, val) \
    CVMD_fieldReadRef(obj,         \
        CVMoffsetOfjava_lang_StringBuffer_value, val)

#undef FIELD_READ_SHARED
#define FIELD_READ_SHARED(obj, val) \
    CVMD_fieldReadInt(obj,          \
        CVMoffsetOfjava_lang_StringBuffer_shared, val)

#undef FIELD_WRITE_VALUE
#define FIELD_WRITE_VALUE(obj, val)   \
    CVMD_fieldWriteRef(obj,                 \
        CVMoffsetOfjava_lang_StringBuffer_value, val);

#undef FIELD_WRITE_COUNT
#define FIELD_WRITE_COUNT(obj, val) \
    CVMD_fieldWriteInt(obj,         \
        CVMoffsetOfjava_lang_StringBuffer_count, val)

#undef FIELD_WRITE_SHARED
#define FIELD_WRITE_SHARED(obj, val)   \
    CVMD_fieldWriteInt(obj,                 \
        CVMoffsetOfjava_lang_StringBuffer_shared, val);

/*
 * helper routine for "expandCapacity(int minCapacity)" 
 */
static void
CVMexpandCapacityHelper(CVMExecEnv* ee, CVMObjectICell *thisICell,
                        CVMJavaInt minimumCapacity)
{
    CVMObject* thisObject = CVMID_icellDirect(ee, thisICell);
    
    CVMJavaInt count;
    CVMJavaBoolean shared;
    CVMObject* valueObject;

    CVMJavaInt valueLength;
    CVMJavaInt newCapacity;
    CVMArrayOfChar* value;
    CVMArrayOfChar* newValue;

    FIELD_READ_COUNT(thisObject, count);
    FIELD_READ_SHARED(thisObject, shared);
    FIELD_READ_VALUE(thisObject, valueObject);
    value = (CVMArrayOfChar*)valueObject;
    valueLength = CVMD_arrayGetLength(value);
    
    newCapacity = (valueLength + 1) * 2;
    if (newCapacity < 0) {
        newCapacity = JAVA_LANG_INTEGER_MAX_VALUE;
    }
    else if (minimumCapacity > newCapacity) {
        newCapacity = minimumCapacity;
    }

    /* Allocate an array with the new capacity */ 
    /* NOTE: CVMgcAllocNewArray may block and initiate GC. */
    newValue = (CVMArrayOfChar *)CVMgcAllocNewArray(
        ee, CVM_T_CHAR, 
        (CVMClassBlock*)CVMbasicTypeArrayClassblocks[CVM_T_CHAR], 
        newCapacity);
    
    if (newValue == NULL) {
        CVMthrowOutOfMemoryError(ee, NULL);
        return;
    }
    if ((count > valueLength) || (count < 0)) {
        CVMthrowArrayIndexOutOfBoundsException(ee, NULL);
        return; 
    }

    /* GC might have initiated so get thisObject and value once again */
    thisObject = CVMID_icellDirect(ee, thisICell);
    FIELD_READ_VALUE(thisObject, valueObject);
    value = (CVMArrayOfChar*)valueObject;
    CVMD_arrayCopyChar(value, 0, newValue, 0, count);

    FIELD_WRITE_VALUE(thisObject,(CVMObject*)newValue);
    FIELD_WRITE_SHARED(thisObject, CVM_FALSE); 
}

/*
 * helper routine for "append(...)" functions
 */
static CVMObjectICell*  
CVMappendHelper(CVMExecEnv* ee, CVMObjectICell *thisICell, 
                CVMArrayOfChar* str, CVMJavaInt offset, 
                CVMJavaInt len, CVMJavaInt strLen) 
{
    CVMObject* thisObject;
    CVMJavaInt newcount;
    CVMJavaInt valueLength;

    CVMJavaInt count;
    CVMObject* valueObject;
    CVMArrayOfChar* value;

    /* 
     * NOTE: This is a synchronized method. The interpreter 
     * loop does not take care of synchronization for CNI 
     * method. So we should do it here.
     */
    if (!CVMfastTryLock(ee, CVMID_icellDirect(ee, thisICell))) {
        /* May become GC-safe */
        if (!CVMobjectLock(ee, thisICell)) {
            CVMthrowOutOfMemoryError(ee, NULL);
            return NULL;
        }
    }

    thisObject = CVMID_icellDirect(ee, thisICell);
 
    FIELD_READ_COUNT(thisObject, count);
    FIELD_READ_VALUE(thisObject, valueObject);
    value = (CVMArrayOfChar* )valueObject;
    valueLength = CVMD_arrayGetLength(value);
    newcount = count + len;

    if (newcount > valueLength) {
        CVMexpandCapacityHelper(ee, thisICell, newcount);
        if (CVMexceptionOccurred(ee)) {
            return NULL;
        }
    }
    
    /* CVMexpandCapacityHelper might have changed thisObject & value */
    thisObject = CVMID_icellDirect(ee, thisICell);
    FIELD_READ_VALUE(thisObject, valueObject);
    value = (CVMArrayOfChar* )valueObject;
    valueLength = CVMD_arrayGetLength(value);

    if (((count + len) > valueLength) || ((offset + len) > strLen) ||
        (offset < 0) || (len < 0) || (count < 0) ) {
        if (!CVMfastTryUnlock(ee, thisObject)) {
            if (!CVMobjectUnlock(ee, thisICell)) {
                CVMthrowIllegalMonitorStateException(ee,
                                                     "current thread not owner");
                return NULL;
            }
        }
        CVMthrowArrayIndexOutOfBoundsException(ee, NULL);
        return NULL; 
    }
  
    CVMD_arrayCopyChar(str, offset, value, count, len);

    FIELD_WRITE_COUNT(thisObject, newcount);

    /* Unlock the object before return */
    if (!CVMfastTryUnlock(ee, thisObject)) {
        if (!CVMobjectUnlock(ee, thisICell)) {
            CVMthrowIllegalMonitorStateException(ee,
                                                 "current thread not owner");
            return NULL;
        }
    }
    return thisICell;
}

/*
 * Class:	java/lang/StringBuffer
 * Method:	append
 * Signature:	([C)Ljava/lang/StringBuffer;
 */
CVMObject*
AOTC_java_lang_StringBuffer_append___3C(CVMExecEnv* ee, CVMObjectICell* thisICell, CVMObjectICell* strICell)
{
    CVMObject* strObject;
    CVMArrayOfChar* str; 

    CVMJavaInt strLength;
    CVMObjectICell* result;
    
    /* CNI policy: offer a gc-safe checkpoint */
    CVMD_gcSafeCheckPoint(ee, {}, {});

    strObject = CVMID_icellDirect(ee, strICell);
    if (strObject == NULL) {
        CVMthrowNullPointerException(ee, NULL);
        return NULL;
    }
    str = (CVMArrayOfChar* )strObject;
    strLength = CVMD_arrayGetLength(str);
    result = CVMappendHelper(ee, thisICell, str, 0, strLength, strLength);
    return result->ref_DONT_ACCESS_DIRECTLY;
}

/*
 * Class:	java/lang/StringBuffer
 * Method:	append
 * Signature:	([CII)Ljava/lang/StringBuffer;
 */
CVMObject*
AOTC_java_lang_StringBuffer_append___3CII(CVMExecEnv* ee, CVMObjectICell* thisICell, CVMObjectICell* strICell, CVMJavaInt offset, CVMJavaInt len)
{
    CVMObject* strObject;
    CVMArrayOfChar* str;
    
    CVMJavaInt strLength;
    CVMObjectICell* result;
    
    /* CNI policy: offer a gc-safe checkpoint */
    CVMD_gcSafeCheckPoint(ee, {}, {});

    strObject = CVMID_icellDirect(ee, strICell);
    if (strObject == NULL) {
        CVMthrowNullPointerException(ee, NULL);
        return NULL;
    }
  
    str = (CVMArrayOfChar *)strObject;
    strLength = CVMD_arrayGetLength(str);
    
    result = CVMappendHelper(ee, thisICell, str, offset, len, strLength);
    return result->ref_DONT_ACCESS_DIRECTLY;
}

/*
 * Class:	java/lang/StringBuffer
 * Method:	expandCapacity
 * Signature:	(I)V
 */
void
AOTC_java_lang_StringBuffer_expandCapacity(CVMExecEnv* ee, CVMObjectICell* thisICell, CVMJavaInt minimumCapacity)
{
    /* CNI policy: offer a gc-safe checkpoint */
    CVMD_gcSafeCheckPoint(ee, {}, {});

    CVMexpandCapacityHelper(ee, thisICell, minimumCapacity);
}

/*
 * java/util/Vector AOTC-interface native methods
 */

#undef FIELD_READ_ELEMENTDATA
#define FIELD_READ_ELEMENTDATA(obj, val)         \
    CVMD_fieldReadRef(                           \
        obj,                                     \
        CVMoffsetOfjava_util_Vector_elementData, \
        val)

#undef FIELD_READ_ELEMENTCOUNT
#define FIELD_READ_ELEMENTCOUNT(obj, val)         \
    CVMD_fieldReadInt(                            \
        obj,                                      \
        CVMoffsetOfjava_util_Vector_elementCount, \
        val)

#undef FIELD_WRITE_ELEMENTDATA
#define FIELD_WRITE_ELEMENTDATA(obj, val)        \
    CVMD_fieldWriteRef(                          \
        obj,                                     \
        CVMoffsetOfjava_util_Vector_elementData, \
        val);

#undef FIELD_WRITE_ELEMENTCOUNT
#define FIELD_WRITE_ELEMENTCOUNT(obj, val)        \
    CVMD_fieldWriteInt(                           \
        obj,                                      \
        CVMoffsetOfjava_util_Vector_elementCount, \
        val);

/* 
 * Helper routine that implements the unsynchronized 
 * semantics of ensureCapacity.
 * 
 * Note: This routine may block and initiate GC.
 */
static void
CVMensureCapacityHelper(CVMExecEnv* ee, CVMObjectICell *thisICell,
                        CVMJavaInt minCapacity)
{
    CVMObject *thisObject = CVMID_icellDirect(ee, thisICell);
    CVMObject *elementDataObject;
    CVMJavaInt oldCapacity;

    FIELD_READ_ELEMENTDATA(thisObject, elementDataObject);

    /* NULL check. */
    if (elementDataObject == NULL) {
        CVMthrowNullPointerException(ee, NULL);
        return;
    }
    
    oldCapacity = CVMD_arrayGetLength((CVMArrayOfRef *)elementDataObject);

    if (minCapacity > oldCapacity) {
        CVMArrayOfRef *newData;
        CVMJavaInt newCapacity;
        CVMJavaInt capacityIncrement;
        CVMJavaInt elementCount;

        /* Calculate the new capacity */
        CVMD_fieldReadInt(thisObject,
                          CVMoffsetOfjava_util_Vector_capacityIncrement,
                          capacityIncrement);
        newCapacity = (capacityIncrement > 0) ?
            (oldCapacity + capacityIncrement) : (oldCapacity * 2);
        if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
        }

        /* Allocate an array with the new capacity */ 
        /* NOTE: CVMgcAllocNewArray may block and initiate GC. */
        newData = (CVMArrayOfRef *)CVMgcAllocNewArray(
            ee, CVM_T_CLASS, 
            (CVMClassBlock*)CVMbasicTypeArrayClassblocks[CVM_T_CLASS], 
            newCapacity);

        if (newData == NULL) {
            CVMthrowOutOfMemoryError(ee, NULL);
            return;
        }

        /* 
         * Copy the contents from the old array to 
         * the new array, then set elementData to be
         * the new one. 
         */
        thisObject = CVMID_icellDirect(ee, thisICell);
        FIELD_READ_ELEMENTDATA(thisObject, elementDataObject);
        /* 
         * Get the number of existing elements. Can't trust
         * the number though.
         */
        FIELD_READ_ELEMENTCOUNT(thisObject, elementCount);
        if (elementCount > oldCapacity) {
            CVMthrowArrayIndexOutOfBoundsException(ee, NULL);
            return; 
        }
        CVMD_arrayCopyRef((CVMArrayOfRef *)elementDataObject, 0, 
                          newData, 0, elementCount);
        FIELD_WRITE_ELEMENTDATA(thisObject, (CVMObject *)newData);
    }
}

/*
 * Class:       java/util/Vector
 * Method:      ensureCapacityHelper
 * Signature:   (I)V
 *
 * This implements the unsynchronized semantics of ensureCapacity.
 * Synchronized methods in Vector class can internally call this
 * method for ensuring capacity without incurring the cost of an
 * extra synchronization.
 */
void
AOTC_java_util_Vector_ensureCapacityHelper(CVMExecEnv* ee, CVMObjectICell* thisICell, CVMJavaInt minCapacity)
{
    /* CNI policy: offer a gc-safe checkpoint */
    CVMD_gcSafeCheckPoint(ee, {}, {});

    /* Call helper routine, which may throw exception */
    CVMensureCapacityHelper(ee, thisICell, minCapacity);
}

/*
 * Class:       java/util/Vector
 * Method:      addElement
 * Signature:   (Ljava/lang/Object;)V
 */
void
AOTC_java_util_Vector_addElement(CVMExecEnv* ee, CVMObjectICell* thisICell, CVMObjectICell* objICell)
{
    CVMObject *thisObject;
    CVMObject *obj;
    CVMObject *elementDataObject;
    CVMJavaInt modCount;
    CVMJavaInt elementCount;

    /* CNI policy: offer a gc-safe checkpoint */
    CVMD_gcSafeCheckPoint(ee, {}, {});

    /* Note: This is a synchronized method */
    if (!CVMfastTryLock(ee, CVMID_icellDirect(ee, thisICell))) {
         /* CVMobjectLock may become GC-safe */
         if (!CVMobjectLock(ee, thisICell)) {
             CVMthrowOutOfMemoryError(ee, NULL);
             return;
         }
    }

    thisObject = CVMID_icellDirect(ee, thisICell);

    /* Increment modCount */
    CVMD_fieldReadInt(thisObject,
                      CVMoffsetOfjava_util_AbstractList_modCount,
                      modCount);
    modCount ++;
    CVMD_fieldWriteInt(thisObject,
                       CVMoffsetOfjava_util_AbstractList_modCount,
                       modCount);

    /* Get elementCount */
    FIELD_READ_ELEMENTCOUNT(thisObject, elementCount);
    elementCount ++;

    /* 
     * Ensure capacity. May become GC-safe. 
     * May throw exception.
     */
    CVMensureCapacityHelper(ee, thisICell, elementCount);

    /* Check exception */
    if (CVMexceptionOccurred(ee)) {
        /* Unlock the object before return */
        if (!CVMfastTryUnlock(ee, CVMID_icellDirect(ee, thisICell))) {
            if (!CVMobjectUnlock(ee, thisICell)) {
                CVMthrowIllegalMonitorStateException(ee,
                    "current thread not owner");
            }
        }
        return;
    }
    
    /* Add the new element */
    thisObject = CVMID_icellDirect(ee, thisICell);
    obj = CVMID_icellDirect(ee, objICell);

    /* 
     * We don't need to do NULL check here since CVMensureCapacityHelper
     * already did it for us.
     */
    FIELD_READ_ELEMENTDATA(thisObject, elementDataObject);
    CVMD_arrayWriteRef((CVMArrayOfRef *)elementDataObject,
                       elementCount - 1,
                       obj);

    /* Don't forget write back elementCount and elementData*/
    FIELD_WRITE_ELEMENTCOUNT(thisObject, elementCount);
    FIELD_WRITE_ELEMENTDATA(thisObject, elementDataObject);

    /* Unlock the object before return */
    if (!CVMfastTryUnlock(ee, thisObject)) {
        if (!CVMobjectUnlock(ee, thisICell)) {
            CVMthrowIllegalMonitorStateException(ee,
                "current thread not owner");
            return;
        }
    }
 
    return;
}

/*
 * Class:       sun/io/ByteToCharISO8859_1
 * Method:      convert
 * Signature:   int convert(byte[] input, int inOff, int inEnd,
 *                          char[] output, int outOff, int outEnd)
 */

#undef MIN
#define MIN(x,y) ((x) < (y) ? (x) : (y))

#undef FIELD_WRITE_CHAROFF
#define FIELD_WRITE_CHAROFF(value)                     \
    CVMD_fieldWriteInt(                                \
        converterObject,                               \
        CVMoffsetOfsun_io_ByteToCharConverter_charOff, \
        value)

#undef FIELD_WRITE_BYTEOFF
#define FIELD_WRITE_BYTEOFF(value)                     \
    CVMD_fieldWriteInt(                                \
        converterObject,                               \
        CVMoffsetOfsun_io_ByteToCharConverter_byteOff, \
        value)

//CNIResultCode
CVMJavaInt
AOTCsun_io_ByteToCharISO8859_1_convert(CVMExecEnv* ee, CVMObjectICell* converterObjectICell, CVMObjectICell* input,
                                        CVMJavaInt inOff, CVMJavaInt inEnd, CVMObjectICell* output, CVMJavaInt outOff, CVMJavaInt outEnd)
{
    CVMObject* converterObject = CVMID_icellDirect(ee, converterObjectICell);
    //CVMObjectICell *input = &arguments[1].j.r;
    //CVMJavaInt inOff = arguments[2].j.i;
    //CVMJavaInt inEnd = arguments[3].j.i;
    //CVMObjectICell *output = &arguments[4].j.r;
    //CVMJavaInt outOff = arguments[5].j.i;
    //CVMJavaInt outEnd = arguments[6].j.i;

    CVMArrayOfByte *inArr = (CVMArrayOfByte *)CVMID_icellDirect(ee, input);
    CVMArrayOfChar *outArr = (CVMArrayOfChar *)CVMID_icellDirect(ee, output);
    CVMJavaByte inElem;
    CVMJavaChar outElem;

    CVMJavaInt inLen = inArr->length;
    CVMJavaInt outLen = outArr->length;
    CVMJavaInt inBound = MIN(inEnd, inLen) - inOff;
    CVMJavaInt outBound = MIN(outEnd, outLen) - outOff;
    CVMJavaInt bound = inOff + MIN(inBound, outBound);
    CVMJavaInt outStart = outOff; /* Record outOff */

    CVMJavaInt innerMax;

    CVMJavaInt returnValue;

    /* 
     * We need to check bounds in the following order so that
     * we can throw the right exception that is compatible
     * with the original Java version.
     */
    if (inOff >= inEnd) {
        FIELD_WRITE_CHAROFF(outOff);
        FIELD_WRITE_BYTEOFF(inOff);
        //arguments[0].j.i = 0;
        //return CNI_SINGLE;
        returnValue = 0;
        return returnValue;
    } else if (inOff < 0 || inOff >= inLen) {
        int tmp = inOff + (outEnd - outOff); 
        FIELD_WRITE_CHAROFF(outOff);
        FIELD_WRITE_BYTEOFF(inOff);
        if (tmp < inEnd && inOff >= tmp) {
            CVMthrowConversionBufferFullException(ee, NULL);
        } else {
            CVMthrowArrayIndexOutOfBoundsException(ee, NULL);
        }
        return CNI_EXCEPTION;
    } else if (outOff >= outEnd) {
        FIELD_WRITE_CHAROFF(outOff);
        FIELD_WRITE_BYTEOFF(inOff);
        CVMthrowConversionBufferFullException(ee, NULL);
        return CNI_EXCEPTION;
    } else if (outOff < 0 || outOff >= outLen) {
        FIELD_WRITE_CHAROFF(outOff);
        FIELD_WRITE_BYTEOFF(inOff);
        CVMthrowArrayIndexOutOfBoundsException(ee, NULL);
        return CNI_EXCEPTION;
    }

    /* Start conversion until we hit the bound */
    while (inOff < bound) {
        innerMax = MIN(inOff+GCSAFE_CHECKPOINT_RATE, bound);
        while (inOff < innerMax) {     
            CVMD_arrayReadByte(inArr, inOff, inElem);
            outElem = (CVMJavaChar)(0xff & inElem); 
            CVMD_arrayWriteChar(outArr, outOff, outElem);
            inOff++;
            outOff++;
        }                              

        /* CNI policy: offer a gc-safe checkpoint periodically */
        CVMD_gcSafeCheckPoint(ee, {}, {
            inArr = (CVMArrayOfByte *)CVMID_icellDirect(ee, input);
            outArr = (CVMArrayOfChar *)CVMID_icellDirect(ee, output);
            converterObject = CVMID_icellDirect(ee, converterObjectICell);
        });
    }

    FIELD_WRITE_CHAROFF(outOff);
    FIELD_WRITE_BYTEOFF(inOff);

    /* Check array bounds and throw appropriate exception. */
    if (inOff < inEnd) { /* we didn't copy everything */
        if (inOff >= inLen || outEnd > outLen) {
            CVMthrowArrayIndexOutOfBoundsException(ee, NULL);
            return CNI_EXCEPTION;
        } else {
            CVMthrowConversionBufferFullException(ee, NULL);
            return CNI_EXCEPTION;
        }
    }

    returnValue = outOff - outStart;

    return returnValue;
}

/*
 * Class:       sun/io/CharToByteISO8859_1
 * Method:      convert
 * Signature:   int convert(char[] input, int inOff, int inEnd,
 *                          byte[] output, int outOff, int outEnd)
 */
#undef MIN
#define MIN(x,y) ((x) < (y) ? (x) : (y))

#undef FIELD_WRITE_BADINPUTLENGTH
#define FIELD_WRITE_BADINPUTLENGTH(value)                     \
    CVMD_fieldWriteInt(                                       \
        converterObject,                                      \
        CVMoffsetOfsun_io_CharToByteConverter_badInputLength, \
        value)

#undef FIELD_WRITE_CHAROFF
#define FIELD_WRITE_CHAROFF(value)                     \
    CVMD_fieldWriteInt(                                \
        converterObject,                               \
        CVMoffsetOfsun_io_CharToByteConverter_charOff, \
        value)

#undef FIELD_WRITE_BYTEOFF
#define FIELD_WRITE_BYTEOFF(value)                     \
    CVMD_fieldWriteInt(                                \
        converterObject,                               \
        CVMoffsetOfsun_io_CharToByteConverter_byteOff, \
        value)

#undef FIELD_WRITE_HIGHHALFZONECODE
#define FIELD_WRITE_HIGHHALFZONECODE(value)                     \
    CVMD_fieldWriteInt(                                         \
        converterObject,                                        \
        CVMoffsetOfsun_io_CharToByteISO8859_1_highHalfZoneCode, \
        value);
CVMJavaInt
AOTC_sun_io_CharToByteISO8859_1_convert(CVMExecEnv* ee, CVMObjectICell *conObjICell
                                        , CVMObjectICell *input, CVMJavaInt inOff, CVMJavaInt inEnd
                                        , CVMObjectICell *output, CVMJavaInt outOff, CVMJavaInt outEnd) 
{
    CVMObject *converterObject = CVMID_icellDirect(ee, conObjICell);
    CVMArrayOfChar *inArr = (CVMArrayOfChar *)CVMID_icellDirect(ee, input);
    CVMArrayOfByte *outArr = (CVMArrayOfByte *)CVMID_icellDirect(ee, output);
    CVMJavaChar inElem;

    CVMBool subMode;
    CVMJavaChar highHalfZoneCode;
    CVMObject *subBytesObject;
    CVMArrayOfByte *subBytes;
    CVMJavaInt subBytesSize;

    CVMJavaInt inBound;
    CVMJavaInt outBound;
    CVMJavaInt inLen = (inArr)->length;
    CVMJavaInt outLen = (outArr)->length;
    CVMJavaInt inLimit = MIN(inEnd, inLen);
    CVMJavaInt outLimit = MIN(outEnd, outLen);
    CVMJavaInt bound;
    CVMJavaInt outStart = outOff;

    CVMJavaInt innerMax;

    CVMJavaInt returnValue;

    /* 
     * We need to check bounds in the following order so that
     * we can throw the right exception that is compatible
     * with the original Java version.
     */
    if (inOff >= inEnd) {
        FIELD_WRITE_CHAROFF(inOff);
        FIELD_WRITE_BYTEOFF(outOff);
        //arguments[0].j.i = 0;
        //return CNI_SINGLE;
        returnValue = 0;
        return returnValue;
    } else if (inOff < 0 || inOff >= inLen) {
        FIELD_WRITE_CHAROFF(inOff);
        FIELD_WRITE_BYTEOFF(outOff);
        CVMthrowArrayIndexOutOfBoundsException(ee, NULL);
        return CNI_EXCEPTION;
    } else if (outOff >= outEnd) {
        FIELD_WRITE_CHAROFF(inOff);
        FIELD_WRITE_BYTEOFF(outOff);
        CVMthrowConversionBufferFullException(ee, NULL);
        return CNI_EXCEPTION;
    } else if (outOff < 0 || outOff >= outLen) {
        FIELD_WRITE_CHAROFF(inOff);
        FIELD_WRITE_BYTEOFF(outOff);
        CVMthrowArrayIndexOutOfBoundsException(ee, NULL);
        return CNI_EXCEPTION;
    }

    CVMD_fieldReadInt(converterObject,
                      CVMoffsetOfsun_io_CharToByteConverter_subMode,
                      subMode);
    CVMD_fieldReadInt(converterObject,
                      CVMoffsetOfsun_io_CharToByteISO8859_1_highHalfZoneCode,
                      highHalfZoneCode); 
    CVMD_fieldReadRef(converterObject,
                      CVMoffsetOfsun_io_CharToByteConverter_subBytes,
                      subBytesObject);
    subBytes = (CVMArrayOfByte *)subBytesObject;
    subBytesSize = (subBytes)->length;

    /* 
     * Check if previous converted string ends 
     * with highHalfZoneCode. 
     */
    if (highHalfZoneCode != 0) {
        /* Reset highHalfZoneCode */ 
        FIELD_WRITE_HIGHHALFZONECODE(0);

        /* 
         * We already checked input array bounds. 
         */

        CVMD_arrayReadChar(inArr, inOff, inElem);
        if (inElem >= 0xdc00 && inElem <= 0xdfff) {
            /* This is a legal UTF16 sequence. */
            if (subMode) {
                if (outOff + subBytesSize > outLimit) {
                    goto finish;
                }
                /* Copy subBytes into the output array */ 
                CVMD_arrayCopyByte(subBytes, 0, outArr, outOff, subBytesSize); 
                inOff += 1;
                outOff += subBytesSize;
            } else {
                FIELD_WRITE_BADINPUTLENGTH(1);
                FIELD_WRITE_CHAROFF(inOff);
                FIELD_WRITE_BYTEOFF(outOff);
                CVMthrowUnknownCharacterException(ee, NULL);
                return CNI_EXCEPTION;
            }
        } else {
            /* This is illegal UTF16 sqeuence. */
            FIELD_WRITE_BADINPUTLENGTH(0);
            FIELD_WRITE_CHAROFF(inOff);
            FIELD_WRITE_BYTEOFF(outOff);
            CVMthrowMalformedInputException(
                ee, "Previous converted string ends with"
                " <High Half Zone Code> of UTF16, but this string does"
                " not begin with <Low Half Zone>"); 
            return CNI_EXCEPTION;
        }
    }

    /* Calculate bound */
    inBound = inLimit - inOff;
    outBound = outLimit - outOff;
    bound = inOff + MIN(inBound, outBound);

    /* Loop until we hit the bound */
    while (inOff < bound) {
        CVMJavaInt newInBound;
        CVMJavaInt newOutBound;

        innerMax = MIN(inOff+GCSAFE_CHECKPOINT_RATE, bound);
        while (inOff < innerMax) {

            /* Get the input character */
            CVMD_arrayReadChar(inArr, inOff, inElem);

            /* Is this character mappable? */
            if (inElem <= 0x00FF) {
                CVMD_arrayWriteByte(outArr, outOff, (CVMJavaByte)inElem);
                inOff ++;
                outOff ++;
            }
            /* Is this a high surrogate? */
            else if (inElem >= 0xD800 && inElem <= 0xDBFF) {

                /* Is this the last character in the input? */
                if (inOff + 1 == inEnd) {
                    FIELD_WRITE_HIGHHALFZONECODE(inElem);
                    inOff ++;
                    goto finish;
                }

                /* 
                 * Is there a low surrogate following? 
                 * Don't forget the array bounds check. 
                 */
                if (inOff + 1 >= inLen) {
                    FIELD_WRITE_CHAROFF(inOff);
                    FIELD_WRITE_BYTEOFF(outOff);
                    CVMthrowArrayIndexOutOfBoundsException(ee, NULL);
                    return CNI_EXCEPTION;
                }
                CVMD_arrayReadChar(inArr, inOff+1, inElem);
                if (inElem >= 0xDC00 && inElem <= 0xDFFF) {
                    /* 
                     * We have a valid surrogate pair. Too bad we don't map
                     * surrogates. Is subsititution enable?
                     */
                    if (subMode) {

                        /* Check bound */
                        if (outOff + subBytesSize > outLimit) {
                            goto finish;
                        }

                        CVMD_arrayCopyByte(subBytes, 0, outArr, 
                                           outOff, subBytesSize);
                        inOff += 2;
                        outOff += subBytesSize;

                        /* Recalculate bound */
                        if (subBytesSize != 2) {
                            newInBound = inLimit - inOff;
                            newOutBound = outLimit - outOff;
                            bound = inOff + MIN(newInBound, newOutBound);
                            innerMax = MIN(innerMax, bound);
                        }
                    } else {
                        FIELD_WRITE_BADINPUTLENGTH(2);
                        FIELD_WRITE_CHAROFF(inOff);
                        FIELD_WRITE_BYTEOFF(outOff);
                        CVMthrowUnknownCharacterException(ee, NULL);
                        return CNI_EXCEPTION;
                    }
                } else {
                    /* We have a malformed surrogate pair */
                    FIELD_WRITE_BADINPUTLENGTH(1);
                    FIELD_WRITE_CHAROFF(inOff);
                    FIELD_WRITE_BYTEOFF(outOff);
                    CVMthrowMalformedInputException(ee, NULL);
                    return CNI_EXCEPTION;
                } 
            }
            /* Is this an unaccompanied low surrogate? */
            else if (inElem >= 0xDC00 && inElem <= 0xDFFF) {
                FIELD_WRITE_BADINPUTLENGTH(1);
                FIELD_WRITE_CHAROFF(inOff);
                FIELD_WRITE_BYTEOFF(outOff);
                CVMthrowMalformedInputException(ee, NULL);
                return CNI_EXCEPTION;
            }
            /* Unmappable and not part of a surrogate */
            else {
                /* Is subsititution enabled? */
                if (subMode) {
                    /* Check bound */
                    if (outOff + subBytesSize > outLimit) {
                        goto finish;
                    }

                    CVMD_arrayCopyByte(subBytes, 0, outArr, 
                                       outOff, subBytesSize);
                    inOff += 1;
                    outOff += subBytesSize; 
                                                                                
                    /* Recalculate bound */
                    if (subBytesSize != 1) {
                        newInBound = inLimit - inOff;
                        newOutBound = outLimit - outOff;
                        bound = inOff + MIN(newInBound, newOutBound);
                        innerMax = MIN(innerMax, bound);
                    }
                } else {
                    FIELD_WRITE_BADINPUTLENGTH(1);
                    FIELD_WRITE_CHAROFF(inOff);
                    FIELD_WRITE_BYTEOFF(outOff);
                    CVMthrowUnknownCharacterException(ee, NULL);
                    return CNI_EXCEPTION;
                }
            }
        }

        /* CNI policy: offer a gc-safe checkpoint periodically */
        CVMD_gcSafeCheckPoint(ee, {}, {
            inArr = (CVMArrayOfChar *)CVMID_icellDirect(ee, input);
            outArr = (CVMArrayOfByte *)CVMID_icellDirect(ee, output);
            converterObject = CVMID_icellDirect(ee, conObjICell);
            CVMD_fieldReadRef(converterObject,
                              CVMoffsetOfsun_io_CharToByteConverter_subBytes,
                              subBytesObject);
            subBytes = (CVMArrayOfByte *)subBytesObject;
            subBytesSize = (subBytes)->length;
        });
    }
   
 finish: 
    /* Don't forget to write back inOff and outOff. */
    FIELD_WRITE_CHAROFF(inOff);
    FIELD_WRITE_BYTEOFF(outOff);

    /* Check array bounds and throw appropriate exception. */
    if (inOff < inEnd) { /* we didn't copy everything */
        if (inOff >= inLen || outEnd > outLen) {
            CVMthrowArrayIndexOutOfBoundsException(ee, NULL);
            return CNI_EXCEPTION;
        }

        while (inOff < inEnd) {
            CVMD_arrayReadChar(inArr, inOff, inElem);
            if (inElem <= 0x00FF) {
                CVMthrowConversionBufferFullException(ee, NULL);
                return CNI_EXCEPTION;
            } else if (inElem >= 0xD800 && inElem <= 0xDBFF) {
                if (inOff+1 == inEnd) {
                    inOff++;
                    FIELD_WRITE_HIGHHALFZONECODE(inElem);
                } else {
                    CVMD_arrayReadChar(inArr, inOff+1, inElem);
                    if (inElem >= 0xDC00 && inElem <= 0xDFFF) {
                        if (subMode) {
                            if (subBytesSize > 0) {
                                CVMthrowConversionBufferFullException(ee, NULL);
                                return CNI_EXCEPTION;
                            }
                            inOff += 2;
                        } else {
                            FIELD_WRITE_BADINPUTLENGTH(2);
                            CVMthrowUnknownCharacterException(ee, NULL);
                            return CNI_EXCEPTION;
                        }
                    } else {
                        FIELD_WRITE_BADINPUTLENGTH(1);
                        CVMthrowMalformedInputException(ee, NULL);
                        return CNI_EXCEPTION;
                   }
                }
            } else if (inElem >= 0xDC00 && inElem <= 0xDFFF) {
                FIELD_WRITE_BADINPUTLENGTH(1);
                CVMthrowMalformedInputException(ee, NULL);
                return CNI_EXCEPTION;
            } else {
                if (subMode) {
                    if (subBytesSize > 0) {
                        CVMthrowConversionBufferFullException(ee, NULL);
                        return CNI_EXCEPTION;
                    }
                    inOff++;
                } else {
                    FIELD_WRITE_BADINPUTLENGTH(1);
                    CVMthrowUnknownCharacterException(ee, NULL);
                    return CNI_EXCEPTION;
                }
            }
        }

        FIELD_WRITE_CHAROFF(inOff);
    }

    //arguments[0].j.i = outOff - outStart;
    //return CNI_SINGLE; 
    returnValue = outOff - outStart;
    return returnValue;
}
                             

void generateException(CVMExecEnv *ee, int type, CVMObject *exception)
{
    switch(type) {
    case EXCEPTION_NOT_GEN:
	return;
    case EXCEPTION_GEN_S0_REF:
	break;
    case EXCEPTION_NULL_POINTER:
	{
	    extern const CVMClassBlock java_lang_NullPointerException_Classblock;
	    exception =  CVMgcAllocNewInstance(ee, (CVMClassBlock*)&java_lang_NullPointerException_Classblock);
	    break;
	}
    case EXCEPTION_INSTANTIATION:
	{
	    extern const CVMClassBlock java_lang_InstantiationException_Classblock;
	    exception =  CVMgcAllocNewInstance(ee, (CVMClassBlock*)&java_lang_InstantiationException_Classblock);
	    break;
	}
    case EXCEPTION_OUT_OF_MEMORY:
	{
	    extern const CVMClassBlock java_lang_OutOfMemoryError_Classblock;
	    exception =  CVMgcAllocNewInstance(ee, (CVMClassBlock*)&java_lang_OutOfMemoryError_Classblock);
	    break;
	}
    case EXCEPTION_CLASS_CAST:
	{
	    extern const CVMClassBlock java_lang_ClassCastException_Classblock;
	    exception =  CVMgcAllocNewInstance(ee, (CVMClassBlock*)&java_lang_ClassCastException_Classblock);
	    break;
	}
    case EXCEPTION_NEGATIVE_ARRAY_SIZE:
	{
	    extern const CVMClassBlock java_lang_NegativeArraySizeException_Classblock;
	    exception =  CVMgcAllocNewInstance(ee, (CVMClassBlock*)&java_lang_NegativeArraySizeException_Classblock);
	    break;
	}
    case EXCEPTION_ARITHMETIC:
	{
	    extern const CVMClassBlock java_lang_ArithmeticException_Classblock;
	    exception =  CVMgcAllocNewInstance(ee, (CVMClassBlock*)&java_lang_ArithmeticException_Classblock);
	    break;
	}
    case EXCEPTION_ARRAY_INDEX_OUT_OF_BOUNDS:
	{
	    extern const CVMClassBlock java_lang_ArrayIndexOutOfBoundsException_Classblock;
	    exception =  CVMgcAllocNewInstance(ee, (CVMClassBlock*)&java_lang_ArrayIndexOutOfBoundsException_Classblock);
	    break;
	}
    case EXCEPTION_ARRAY_STORE:
	{
	    extern const CVMClassBlock java_lang_ArrayStoreException_Classblock;
	    exception =  CVMgcAllocNewInstance(ee, (CVMClassBlock*)&java_lang_ArrayStoreException_Classblock);
	    break;
	}
    case EXCEPTION_INCOMPATIBLE_CLASS_CHANGE:
	{
	    extern const CVMClassBlock java_lang_IncompatibleClassChangeError_Classblock;
	    exception =  CVMgcAllocNewInstance(ee, (CVMClassBlock*)&java_lang_IncompatibleClassChangeError_Classblock);
	    break;
	}
    default:
	CVMassert(0);
    }

    CVMgcUnsafeThrowLocalException(ee, exception);
}


/*
jobject AOTCCreateLocalRef(CVMExecEnv *ee)
{
    CVMStack* jniLocalsStack = &ee->interpreterStack;
    CVMFreelistFrame* currentFrame = (CVMFreelistFrame*)jniLocalsStack->currentFrame;
    CVMStackVal32* slot = currentFrame->frame.topOfStack;
    currentFrame->frame.topOfStack = slot + 1;
    CVMID_icellSetNull(&slot->ref);
    currentFrame->inUse++;
    return &slot->ref;
}
*/
#if 0
#ifdef AOTC_PROFILE_CALL_COUNT

#include <pthread.h>

list l = { NULL };
tree t = { &l };

pthread_mutex_t mut = PTHREAD_MUTEX_INITIALIZER;


node* addNode(list* list, char* name)
{
    node* newNode = (node*)malloc(sizeof(node));
    newNode->item = strdup(name);
    newNode->next = list->head;
    newNode->count = 0;
    list->head = newNode;
    newNode->child = NULL;
    return newNode;
}
node* findList(list* current, char *name) {
    node* n;
    for(n=current->head; n; n=n->next) {
        if(strcmp(n->item, name)==0) return n;
    }
    addNode(current, name);
    return current->head;
}

node* findChild(node* current, char* name) {
    if(current->child==NULL) {
        current->child = (list*)malloc(sizeof(list));
        current->child->head = NULL;
    }
    return findList(current->child, name);
}

node* nextNode(list* l, node* n)
{
    if(n) return n->next;
    return l->head;
}

char* getCallTypeString(CVMUint8 invokerIdx) {
    if (invokerIdx == CVM_INVOKE_AOTC_METHOD) {
        return "AOTC";
    } else if (invokerIdx == CVM_INVOKE_JNI_METHOD || invokerIdx == CVM_INVOKE_LAZY_JNI_METHOD) {
        return "JNI";
    } else if (invokerIdx == CVM_INVOKE_JAVA_METHOD) {
        return "JAVA";
    } else if (invokerIdx == CVM_INVOKE_JAVA_SYNC_METHOD) {
        return "JAVA_SYNC";
    } else if (invokerIdx == CVM_INVOKE_CNI_METHOD) {
        return "CVM";
    } else {
        fprintf(stderr, "invokerIdx = %d", invokerIdx);
        CVMassert(0);
    }
    return NULL;
}

void increaseVirtualCallCounter(char* class, char* method, char* bpc, 
                       CVMClassBlock *calleeCB, CVMMethodBlock *calleeMB)
{
    pthread_mutex_lock(&mut);
    char calleeInfo[1024];
    char tmp[512];
    CVMClassBlock* mbClass = CVMmbClassBlock(calleeMB);
    
    CVMMethodTypeID tid;
    calleeInfo[0]='\0';

    node* cl = findList(t.root, class);
    node* me = findChild(cl, method);
    node* bp = findChild(me, bpc);

    // get method name and type
    strcpy(calleeInfo, calleeCB->theNameX);
    CVMassert((calleeCB->theNameX) != NULL);
    strcat(calleeInfo, " ");
    tid = CVMmbNameAndTypeID(calleeMB);
    CVMtypeidMethodNameToCString(tid, tmp, 512);
    strcat(calleeInfo, tmp);
    strcat(calleeInfo, " ");
    CVMtypeidMethodTypeToCString(tid, tmp, 512);
    strcat(calleeInfo, tmp);
    strcat(calleeInfo, " ");
    strcat (calleeInfo, getCallTypeString(CVMmbInvokerIdx(calleeMB)));
    
    strcat(calleeInfo, " ");
    strcat(calleeInfo, mbClass->theNameX);

    node* ce = findChild(bp, calleeInfo);
    ce->count++;
    pthread_mutex_unlock(&mut);
}

void increaseStaticCallCounter(char* class, char* method, char* bpc, 
                       char *calleeCB, CVMMethodBlock *calleeMB)
{
    pthread_mutex_lock(&mut);
    char calleeInfo[1024];
    char tmp[512];
    CVMMethodTypeID tid;
    calleeInfo[0]='\0';

    node* cl = findList(t.root, class);
    node* me = findChild(cl, method);
    node* bp = findChild(me, bpc);

    // get method name and type
    CVMassert(calleeCB != NULL);
    strcpy(calleeInfo, calleeCB);
    strcat(calleeInfo, " ");
    CVMassert(calleeMB != NULL);
    tid = CVMmbNameAndTypeID(calleeMB);
    CVMtypeidMethodNameToCString(tid, tmp, 512);
    strcat(calleeInfo, tmp);
    strcat(calleeInfo, " ");
    CVMtypeidMethodTypeToCString(tid, tmp, 512);
    strcat(calleeInfo, tmp);
    strcat(calleeInfo, " ");
    strcat (calleeInfo, getCallTypeString(CVMmbInvokerIdx(calleeMB)));

    strcat(calleeInfo, " ");
    strcat(calleeInfo, calleeCB);
    
    node* ce = findChild(bp, calleeInfo);
    ce->count++;
    pthread_mutex_unlock(&mut);
}
void _printTree(FILE* fp, list* l, int depth)
{
    int i;
    node* n = NULL;
    while((n=nextNode(l, n))!=NULL) {
        for(i=0; i<depth; i++) fprintf(fp, " ");
        if (n->count > 0) {
            fprintf(fp, "%s %d\n",n->item,n->count);
        } else {
            fprintf(fp, "%s\n",n->item);
        }
        if(n->child) _printTree(fp, n->child, depth+1);
    }
}
void printTree(void)
{
    FILE* fp = fopen("aotcprof", "w");
    fprintf(fp, "#start\n");
    _printTree(fp, t.root, 0);
    fprintf(fp, "#end\n");
    fclose(fp);
}

void writeProfileInfo() {
    printTree();
}
#elif defined(AOTC_PROFILE_LOOPPEELING)

#include <pthread.h>

list l = { NULL };
tree t = { &l };

pthread_mutex_t mut = PTHREAD_MUTEX_INITIALIZER;

node* addNode(list* list, char* name)
{
    node* newNode = (node*)malloc(sizeof(node));
    newNode->item = strdup(name);
    newNode->next = list->head;
    newNode->loopCount = 0;
    newNode->eliminatedException = 0;
    newNode->loopIntoCount = 0;
    list->head = newNode;
    newNode->child = NULL;
    return newNode;
}
node* findList(list* current, char *name) {
    node* n;
    for(n=current->head; n; n=n->next) {
        if(strcmp(n->item, name)==0) return n;
    }
    addNode(current, name);
    return current->head;
}

node* findChild(node* current, char* name) {
    if(current->child==NULL) {
        current->child = (list*)malloc(sizeof(list));
        current->child->head = NULL;
    }
    return findList(current->child, name);
}

node* nextNode(list* l, node* n)
{
    if(n) return n->next;
    return l->head;
}
void increaseLoopCounter(char* class, char* method, char* loopHeaderBpc, char* loopSize) 
{
    pthread_mutex_lock(&mut);
    char loop[512];
    unsigned int temp;
    loop[0]='\0';
    
    node* cl = findList(t.root, class);
    node* me = findChild(cl, method);

    strcpy(loop, loopHeaderBpc);
    strcat(loop, " ");
    strcat(loop, loopSize);

    node* lp = findChild(me, loop);
    temp = lp->loopCount;
    lp->loopCount++;
    CVMassert(temp < lp->loopCount);

    pthread_mutex_unlock(&mut);
}

void increaseLoopIntoCounter(char* class, char* method, char* loopHeaderBpc, char* loopSize) 
{    
    pthread_mutex_lock(&mut);
    char loop[512];
    unsigned int temp;
    loop[0]='\0';
    
    node* cl = findList(t.root, class);
    node* me = findChild(cl, method);

    strcpy(loop, loopHeaderBpc);
    strcat(loop, " ");
    strcat(loop, loopSize);

    node* lp = findChild(me, loop);
    
    temp = lp->loopIntoCount;
    lp->loopIntoCount++;
    CVMassert(temp < lp->loopIntoCount);

    pthread_mutex_unlock(&mut);

}
void increaseEliminatedExceptionCounter(char* class, char*method, char* loopHeaderBpc, char* loopSize) 
{
    pthread_mutex_lock(&mut);
    char loop[512];
    unsigned int temp;
    loop[0]='\0';
    
    node* cl = findList(t.root, class);
    node* me = findChild(cl, method);

    strcpy(loop, loopHeaderBpc);
    strcat(loop, " ");
    strcat(loop, loopSize);

    node* lp = findChild(me, loop);
    
    temp = lp->eliminatedException;
    lp->eliminatedException++;
    CVMassert(temp < lp->eliminatedException);
    
    pthread_mutex_unlock(&mut);
}
void _printTree(FILE* fp, list* l, int depth)
{
    int i;
    node* n = NULL;
    while((n=nextNode(l, n))!=NULL) {
        for(i=0; i<depth; i++) fprintf(fp, " ");
        if (n->loopCount > 0) {
            fprintf(fp, "%s %u %u %u\n",n->item,n->loopCount,n->eliminatedException,n->loopIntoCount);
        } else {
            fprintf(fp, "%s\n",n->item);
        }
        if(n->child) _printTree(fp, n->child, depth+1);
    }
}

void printTree(void)
{
    FILE* fp = fopen("looppeelingprof", "w");
    fprintf(fp, "#start\n");
    _printTree(fp, t.root, 0);
    fprintf(fp, "#end\n");
    fclose(fp);
}

void writeProfileInfo() {
    printTree();
}
#else
void writeProfileInfo() {
    return;
}
#endif
#endif
