#ifndef _INCLUDED_AOTC_PROFILE_H
#define _INCLUDED_AOTC_PROFILE_H
#include "javavm/include/defs.h"
#include "javavm/include/objects.h"
#include "javavm/include/directmem.h"
#include "javavm/include/indirectmem.h"
#include "generated/javavm/include/opcodes.h"
#include "javavm/include/interpreter.h"
#include "javavm/include/classes.h"
#include "javavm/include/stacks.h"
#include "javavm/include/globalroots.h"
#include "javavm/include/gc_common.h"
#include "javavm/include/globals.h"
#include "javavm/include/preloader.h"
#include "javavm/include/utils.h"
#include "javavm/include/common_exceptions.h"
#include "javavm/include/clib.h"

#include "javavm/include/porting/doubleword.h"
#include "javavm/include/porting/float.h"
#include "javavm/include/porting/int.h"
#include "javavm/include/porting/jni.h"

#include <pthread.h>
#ifdef AOTC_PROFILE_CALL_COUNT

//struct _list;

typedef struct _node {
	void* item;
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
void increaseCallCounter(CVMMethodBlock* callerMB, CVMUint32 bpc, CVMMethodBlock *calleeMB);
/*
void increaseJavaCallCounter();
void increaseAOTCCallCounter();
void increaseAOTCINLINECallCounter();
void increaseCNICallCounter();
void increaseJNICallCounter();
*/
void writeProfileInfo();
#elif AOTC_USE_PROFILE

typedef struct _CallInfo {
    //char* className;
    //char* methodName;
    //char* signature;
    CVMMethodBlock* mb;
    int callCount;
    int totalCallCount;
} CallInfo;

typedef struct _HashtableEntry {
    void* key;
    void* value;
    struct _HashtableEntry* next;
}HashtableEntry;

typedef struct _Hashtable {
    HashtableEntry** entries;
    int count;
    int capacity;
}Hashtable;

unsigned int hashCode(Hashtable* hash, void* key);
Hashtable* makeHashtable();
Hashtable* makeHashtableWithCapacity(int capacity);
void* getHashtable(Hashtable* hash, void* key);
Hashtable* putHashtable(Hashtable* hash, void* key, void* value);
Hashtable* rehash(Hashtable* oldHash);
//int clearHashtable(Hashtable* hash);
CVMUint32 getAOTCallCount(CVMMethodBlock* callerMB, CVMMethodBlock* calleeMB, CVMUint32 bpc);
CVMMethodBlock* getAOTMethod(CVMMethodBlock* callerMB, CVMUint32 bpc);
CVMUint8 loadAOTFile(CVMExecEnv* ee);
CVMUint8 unloadAOTFile();
#endif
#endif

