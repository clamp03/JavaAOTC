#include "javavm/include/string_impl.h"

#define EXCEPTION_NOT_GEN 0
#define EXCEPTION_GEN_S0_REF 1
#define EXCEPTION_NULL_POINTER 2
#define EXCEPTION_INSTANTIATION 3
#define EXCEPTION_OUT_OF_MEMORY 4
#define EXCEPTION_CLASS_CAST 5
#define EXCEPTION_NEGATIVE_ARRAY_SIZE 6
#define EXCEPTION_ARRAY_INDEX_OUT_OF_BOUNDS 7
#define EXCEPTION_ARRAY_STORE 8
#define EXCEPTION_ARITHMETIC 9
#define EXCEPTION_INCOMPATIBLE_CLASS_CHANGE 10

#define CNIjvm2Double(location, value) {   \
    union { \
        CVMJavaLong l;\
        CVMJavaDouble d;    \
        CVMUint32 v[2];   \
    }res;   \
    res.v[0] = (location)[0].j.raw;   \
    res.v[1] = (location)[1].j.raw;   \
    value = res.d;  \
}
#define CNIDouble2jvm(location, value) {    \
    union { \
        CVMJavaLong l;\
        CVMJavaDouble d;    \
        CVMUint32 v[2]; \
    }res;   \
    res.d = value;  \
    (location)[0].j.raw = res.v[0]; \
    (location)[1].j.raw = res.v[1]; \
}
#define CNIjvm2Long(location, value) {   \
    union { \
        CVMJavaLong d;    \
        CVMUint32 v[2];   \
    }res;   \
    res.v[0] = (location)[0].j.raw;   \
    res.v[1] = (location)[1].j.raw;   \
    value = res.d;  \
}
#define CNILong2jvm(location, value) {    \
    union { \
        CVMJavaLong d;    \
        CVMUint32 v[2]; \
    }res;   \
    res.d = value;  \
    (location)[0].j.raw = res.v[0]; \
    (location)[1].j.raw = res.v[1]; \
}

/*
#define AOTCExceptionOccurred(ee_, result_)	\
    if(CVMlocalExceptionOccurred(ee_)) {	\
	result_ = CVMlocalExceptionICell(ee_);	\
    }else if(CVMremoteExceptionOccurred(ee_)) {	\
	result_ = CVMremoteExceptionICell(ee_); \
    }else result_ = NULL;
*/
/*
#define AOTCExceptionOccurred(ee_, result_)	\
    if(CVMlocalExceptionOccurred(ee_)) {	\
	result_ = CVMlocalExceptionICell(ee_);	\
    }else result_ = NULL;
*/
#define AOTCExceptionOccurred(ee_, result_)	\
    result_ = ee_->localExceptionICell->ref_DONT_ACCESS_DIRECTLY;

extern unsigned int global_gc_and_exception_count;
extern unsigned int setjmpCount;
//extern unsigned int aotc_call_count;
//extern unsigned int to_aotc_call_count;

void AOTCclassInit(CVMExecEnv* ee, CVMClassBlock* cb);
//CVMBool AOTCisAssignable(CVMExecEnv* ee, CVMClassBlock* srcCb, CVMClassBlock* dstCb);
//CVMBool AOTCisSubclassOf(CVMExecEnv* ee, CVMClassBlock* subclasscb, CVMClassBlock* cb);
//jobject AOTCPushLocalFrame(CVMExecEnv *ee, int number);
//void AOTCPopLocalFrame(CVMExecEnv *ee, int number);
#define AOTCPopLocalFrame()	\
	    /*CVMStack* jniLocalsStack = &ee->interpreterStack;*/    \
	    /*CVMFrame* currentFrame = jniLocalsStack->currentFrame;*/  \
	    /*if(topOfStack != (CVMStackVal32*)locals) {		*/	    \
	    if(pushed) {			    \
		CVMpopFrame(&ee->interpreterStack, currentFrame);	    \
	    } else {						    \
	    /*currentFrame->topOfStack = tos;*/			    \
	    currentFrame->topOfStack = topOfStack;		    \
	    }							    \
	    /* CVMgetJNIFrame(currentFrame)->freeList = freeList;			   */ \

#define AOTCPushLocalFrame(number_)                                             \
{                                                                               \
    /*int i;   */                                                                   \
    CVMStackVal32* slot;                                                        \
                                                                                \
    CVMStack* jniLocalsStack = &ee->interpreterStack;                      \
    currentFrame   = jniLocalsStack->currentFrame;               \
    topOfStack = currentFrame->topOfStack;                       \
    /* freeList = CVMgetJNIFrame(currentFrame)->freeList;		*/		 \
                                                                                \
    if (unlikely(CVMcheckCapacity(jniLocalsStack, topOfStack, (number_)+CVMJNIFrameCapacity) == 0)) {    \
        currentFrame = AOTCExpandFrame(ee, (number_));                          \
        pushed = CVM_TRUE;  \
    }                                                                           \
    slot = currentFrame->topOfStack;                                        \
                                                                                \
    /* Makes local reference space. */                                          \
    currentFrame->topOfStack = slot + (number_);                                   \
    /* for(i=0; i < (number_); i++) {   */                                                \
	/* (slot+i)->ref.ref_DONT_ACCESS_DIRECTLY = NULL;*/                         \
    /* }                                              */                             \
    locals = &slot->ref;                                                        \
}

//jobject AOTCPushLocalFrameSync(CVMExecEnv *ee, jobject lock_ref, int number);
/*#define AOTCPushLocalFrameSync(cls, number, ref)    \
	ref = AOTCPushLocalFrame(ee, number);	    \
	if(!CVMfastTryLock(ee, cls)) {		    \
	    CVMobjectLock(ee, cls_ICell);	    \
	}*/
//void AOTCPopLocalFrameSync(CVMExecEnv *ee, jobject lock_ref, int number);
/*#define AOTCPopLocalFrameSync(cls, number)	    \
	if (!CVMfastTryUnlock(ee, cls)) {   \
	    CVMobjectUnlock(ee, cls_ICell); \
	}				    \
	AOTCPopLocalFrame(number);*/
	    
//jobject AOTCCreateLocalRef(CVMExecEnv *ee);
//jthrowable JNICALL AOTCExceptionOccurred(CVMExecEnv *ee);


/*#define AOTCIsInstanceOf(ee_, obj_, dstCb_, result_)                \
{                                                                   \
    CVMassert(CVMD_isgcUnsafe((ee_)));                              \
    CVMClassBlock* srcCb_;                                          \
    if ((obj_) == 0) {                                              \
        (result_) = CVM_TRUE;                                       \
    } else {                                                        \
        srcCb_ = (CVMClassBlock*)CVMobjectGetClassWord((obj_));     \
        if (srcCb_ == (dstCb_)) {                                   \
            (result_) = CVM_TRUE;                                   \
        } else {                                                    \
            (result_) = AOTCisAssignable((ee_), srcCb_, (dstCb_));  \
        }                                                           \
    }                                                               \
}*/                                                                


// assumes that obj is not null
#define AOTCImplements(ee_, srcCb_, dstCb_, result_)                  \
{                                                                   \
    CVMassert(CVMD_isgcUnsafe((ee_)));                              \
    (result_) = CVMimplementsInterface(ee, (srcCb_), (dstCb_));     \
}                                                                   

// assumes that obj_ is not null
#define AOTCIsArrayOf(ee_, srcCb_, dstCb_, result_)                   \
{                                                                   \
    CVMassert(CVMD_isgcUnsafe((ee_)));                              \
    if (!CVMisArrayClass((srcCb_))) {                               \
        (result_) = CVM_FALSE;                                      \
    } else {                                                        \
        /* Both srcCb and dstCb are arrays. Check array assignability. */ \
        /* derived from CVMarrayIsAssignable(ee, srcCb, dstCb);        */ \
        int              srcArrayDepth     = CVMarrayDepth((srcCb_));      \
        CVMBasicType     srcArrayBaseType  = CVMarrayBaseType((srcCb_));   \
        CVMClassBlock*   srcArrayBaseClass = CVMarrayBaseCb((srcCb_));     \
                                                                           \
        int              dstArrayDepth     = CVMarrayDepth((dstCb_));      \
        CVMBasicType     dstArrayBaseType  = CVMarrayBaseType((dstCb_));   \
        CVMClassBlock*   dstArrayBaseClass = CVMarrayBaseCb((dstCb_));     \
                                                                                \
        if (srcArrayDepth > dstArrayDepth) {                                    \
            /* The source array is 'deeper' than the destination array.    */   \
            /* The only way they could be assignable is if the destination */   \
            /* array is an array of Object, Cloneable or Serializable.     */   \
            (result_) = (dstArrayBaseClass == CVMsystemClass(java_lang_Object) ||    \
                    dstArrayBaseClass == CVMsystemClass(java_lang_Cloneable) || \
                    dstArrayBaseClass == CVMsystemClass(java_io_Serializable)); \
        } else if (srcArrayDepth == dstArrayDepth) {                            \
            /* Equal depths.                                                 */ \
            /* If the base types of both arrays are not of the same category */ \
            /* they are definitely not assignable                            */ \
            if (dstArrayBaseType != srcArrayBaseType) {                         \
                (result_) = CVM_FALSE;                                          \
            } else {                                                            \
            /* Now the classes are assignable iff:                           */ \
            /* both base types are the same non-object basic type            */ \
            /*    or                                                         */ \
            /* both base types are object types and the source type          */ \
            /* is a subclass of the destination type.                        */ \
                (result_) = ((srcArrayBaseType != CVM_T_CLASS) ||               \
                    (CVMisSubclassOf(ee, srcArrayBaseClass, dstArrayBaseClass))); \
            }                                                                   \
        } else {                                                                \
            /* The destination array is 'deeper' than the source array, */      \
            /* so they are not assignable.                              */      \
            (result_) = CVM_FALSE;                                              \
        }                                                                       \
    }                                                                           \
} 

// assumes that obj_ is not null
#define AOTCIsSubclassOf(ee_, srcCb_, dstCb_, result_)              \
{                                                                   \
    CVMassert(CVMD_isgcUnsafe((ee_)));                              \
    if ((srcCb_) == (dstCb_)) {                                     \
        (result_) = CVM_TRUE;                                       \
    } else {                                                        \
	/* cb is a class type */                                    \
	CVMClassBlock* subclasscb = CVMcbSuperclass((srcCb_));      \
        (result_) = CVM_FALSE;                                      \
	while (subclasscb != NULL) {                                \
            if (subclasscb == (dstCb_)) {                           \
                (result_) = CVM_TRUE;                               \
                break;                                              \
	    }                                                       \
            subclasscb = CVMcbSuperclass(subclasscb);               \
        }                                                           \
    }                                                               \
}                                           

void generateException(CVMExecEnv *ee, int type, CVMObject *exception);

typedef union AOTCreturn {
    jint     i;
    jlong    j;
    jfloat   f;
    jdouble  d;
    CVMObject *l;
} AOTCreturn;

/*
typedef struct AOTCMethodArray {
        CVMClassBlock const *cb;
	    CVMMethodBlock mb[1];
}AOTCMethodArray;
*/


CVMFrame* AOTCExpandFrame(CVMExecEnv* ee, int size);

void InvokeInterpreterV(CVMExecEnv *ee, jobject obj, ...);
jint InvokeInterpreterI(CVMExecEnv *ee, jobject obj, ...);
jlong InvokeInterpreterJ(CVMExecEnv *ee, jobject obj, ...);
jfloat InvokeInterpreterF(CVMExecEnv *ee, jobject obj, ...);
jdouble InvokeInterpreterD(CVMExecEnv *ee, jobject obj, ...);
CVMObject* InvokeInterpreterL(CVMExecEnv *ee, jobject obj, ...);

void AOTCInvoke(CVMExecEnv *ee, jobject obj, jmethodID methodID, CVMUint16 retType,
		AOTCreturn *retValue, CVMBool isStatic, CVMBool isVirtual, ...);

CVMJavaChar AOTC_java_lang_String_charAt(CVMExecEnv* ee, CVMObjectICell* thisICell,
                                         CVMJavaInt index);
void AOTC_java_lang_String_getChars(CVMExecEnv* ee, jobject cls, CVMJavaInt srcBegin,
                                    CVMJavaInt srcEnd, jobject dstobj,
                                    CVMJavaInt dstBegin);
CVMJavaBoolean AOTC_java_lang_String_equals(CVMExecEnv* ee, jobject cls, jobject obj);
CVMJavaInt AOTC_java_lang_String_compareTo(CVMExecEnv* ee, jobject cls, jobject obj);
CVMJavaInt AOTC_java_lang_String_hashCode(CVMExecEnv* ee, CVMObjectICell* thisICell);
CVMJavaInt AOTC_java_lang_String_indexOf__I(CVMExecEnv* ee, CVMObjectICell* thisICell,
                                            CVMJavaInt ch);
CVMJavaInt AOTC_java_lang_String_indexOf__II(CVMExecEnv* ee, CVMObjectICell* thisICell,
                                             CVMJavaInt ch, CVMJavaInt fromIndex);
CVMJavaInt AOTC_java_lang_String_indexOf__Ljava_lang_String_2(CVMExecEnv* ee,
                                                              CVMObjectICell* thisICell,
                                                              CVMObjectICell* strICell);
CVMJavaInt AOTC_java_lang_String_indexOf__Ljava_lang_String_2I(CVMExecEnv* ee,
                                                               CVMObjectICell* thisICell,
                                                               CVMObjectICell* strICell,
                                                               CVMJavaInt fromIndex);
CVMObject* AOTC_java_lang_String_intern(CVMExecEnv* ee, CVMObjectICell* thisICell);


CVMObject* AOTC_java_lang_StringBuffer_append___3C(CVMExecEnv* ee,
                                                        CVMObjectICell* thisICell,
                                                        CVMObjectICell* strICell);
CVMObject* AOTC_java_lang_StringBuffer_append___3CII(CVMExecEnv* ee,
                                                          CVMObjectICell* thisICell,
                                                          CVMObjectICell* strICell,
                                                          CVMJavaInt offset,
                                                          CVMJavaInt len);
void AOTC_java_lang_StringBuffer_expandCapacity(CVMExecEnv* ee,
                                                CVMObjectICell* thisICell,
                                                CVMJavaInt minimumCapacity);


void AOTC_java_util_Vector_ensureCapacityHelper(CVMExecEnv* ee,
                                                CVMObjectICell* thisICell,
                                                CVMJavaInt minCapacity);
void AOTC_java_util_Vector_addElement(CVMExecEnv* ee, CVMObjectICell* thisICell,
                                      CVMObjectICell* objICell);
CVMJavaInt
AOTCsun_io_ByteToCharISO8859_1_convert(CVMExecEnv* ee, CVMObjectICell* converterObjectICell, CVMObjectICell* input,
                                       CVMJavaInt inOff, CVMJavaInt inEnd, CVMObjectICell* output, CVMJavaInt outOff, CVMJavaInt outEnd);
CVMJavaInt
AOTC_sun_io_CharToByteISO8859_1_convert(CVMExecEnv* ee, CVMObjectICell *conObjICell,
                                        CVMObjectICell *input, CVMJavaInt inOff, CVMJavaInt inEnd,
                                        CVMObjectICell *output, CVMJavaInt outOff, CVMJavaInt outEnd);

#ifdef SERVER_AOTC_SETJMP_EXCEPTION_HANDLING
typedef struct exception_label {
    volatile void* label;
}exception_label;
#endif
#if 0
#ifdef AOTC_PROFILE_CALL_COUNT
struct _list;
typedef struct _node {
    char* item;
    int count;
    struct _node* next;
    struct _list* child;
} node;

typedef struct _list {
    struct _node* head;
} list;

typedef struct _tree {
    struct _list* root;
} tree;

extern tree t;

void printTree(void);

void increaseVirtualCallCounter(char* class, char* method, char* bpc, 
                       CVMClassBlock *calleeCB, CVMMethodBlock *calleeMB);
void increaseStaticCallCounter(char* class, char* method, char* bpc, 
                       char *calleeCB, CVMMethodBlock *calleeMB);
#elif defined(AOTC_PROFILE_LOOPPEELING)
struct _list;
typedef struct _node {
    char *item;
    unsigned int loopCount;
    unsigned int eliminatedException;
    unsigned int loopIntoCount;
    struct _node* next;
    struct _list* child;
} node;

typedef struct _list {
    struct _node* head;
} list;

typedef struct _tree {
    struct _list* root;
} tree;

extern tree t;

void printTree(void);
void increaseLoopCounter(char* class, char* method, char* loopHeaderBpc, char* loopSize);
void increaseLoopIntoCounter(char* class, char* method, char* loopHeaderBpc, char* loopSize);
void increaseEliminatedExceptionCounter(char* class, char*method, char* loopHeaderBpc, char* loopSize);

#endif
#endif
