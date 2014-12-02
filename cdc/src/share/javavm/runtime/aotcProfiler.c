#include "javavm/include/aotcProfiler.h"
#ifdef AOTC_PROFILE_CALL_COUNT

list l = { NULL };
tree t = { &l };

pthread_mutex_t mut = PTHREAD_MUTEX_INITIALIZER;

CVMMethodBlock* cacheMethodBlock = NULL;
node* cacheMethodNode = NULL;
CVMClassBlock* cacheClassBlock = NULL;
node* cacheClassNode = NULL;
int increaseCount = 0;
int newNodeCount = 0;
/*
unsigned int JAVACall = 0;
unsigned int AOTCCall = 0;
unsigned int AOTCINCall = 0;
unsigned int CNICall = 0;
unsigned int JNICall = 0;
*/

node* addNode(list* list, void* item)
{
    node* newNode = (node*)malloc(sizeof(node));
    newNode->item = item;
    newNode->next = list->head;
    newNode->count = 0;
    list->head = newNode;
    newNode->child = NULL;
    return newNode;
}
node* findList(list* current, void* item) {
    node* n;
    for(n=current->head; n; n=n->next) {
        if(n->item == item) return n;
    }
    addNode(current, item);
    newNodeCount++;
    if((newNodeCount % 10000) == 0) fprintf(stderr, "newNodeCount: %d\n", newNodeCount);
    return current->head;
}

node* findChild(node* current, void* item) {
    if(current->child==NULL) {
        current->child = (list*)malloc(sizeof(list));
        current->child->head = NULL;
    }
    return findList(current->child, item);
}

node* nextNode(list* l, node* n)
{
    if(n) return n->next;
    return l->head;
}

/*
void increaseJavaCallCounter() {
    pthread_mutex_lock(&mut);
    JAVACall++;
    pthread_mutex_unlock(&mut);
}
void increaseAOTCCallCounter() {
    pthread_mutex_lock(&mut);
    AOTCCall++;
    pthread_mutex_unlock(&mut);
}
void increaseAOTCINLINECallCounter() {
    pthread_mutex_lock(&mut);
    AOTCINCall++;
    pthread_mutex_unlock(&mut);
}

void increaseCNICallCounter() {
    pthread_mutex_lock(&mut);
    CNICall++;
    pthread_mutex_unlock(&mut);
}
void increaseJNICallCounter() {
    pthread_mutex_lock(&mut);
    JNICall++;
    pthread_mutex_unlock(&mut);
}
*/
void increaseCallCounter(CVMMethodBlock* callerMB, CVMUint32 bpc, CVMMethodBlock *calleeMB)
{
    CVMClassBlock* mbClass = CVMmbClassBlock(callerMB);
	node* bp;
    node* ce;
    if(!CVMcbIsInROM(mbClass) || !CVMcbIsInROM(CVMmbClassBlock(calleeMB))) {
        return;
    }
    pthread_mutex_lock(&mut);
    //if(/*!CVMcbIsInROM(mbClass) ||*/ !CVMcbIsInROM(CVMmbClassBlock(calleeMB))) return;
    increaseCount++;
    if((increaseCount % 10000) == 0) fprintf(stderr, "increaseCount: %d\n", increaseCount);
    if(cacheClassBlock != mbClass) {
        // Class of Caller
        cacheClassBlock = mbClass;
        cacheClassNode = findList(t.root, (void*)mbClass);
        // Method of Caller
        cacheMethodBlock = callerMB;
        cacheMethodNode = findChild(cacheClassNode, (void*)callerMB);
    }else if(cacheMethodBlock != callerMB) {
        cacheMethodBlock = callerMB;
        cacheMethodNode = findChild(cacheClassNode, (void*)callerMB);
    }
    // BPC in Caller
    bp = findChild(cacheMethodNode, (void*)bpc);
    // Callee
    ce = findChild(bp, calleeMB);
	ce->count++;
    pthread_mutex_unlock(&mut);
}

void _printTree(FILE* fp, list* l, int depth)
{
    int i;
    node* n = NULL;
    char tmp1[512];
    char tmp2[512];
    char tmp3[512];
    CVMMethodTypeID tid;
    while((n=nextNode(l, n))!=NULL) {
        for(i=0; i<depth; i++) fprintf(fp, " ");
        switch(depth) {
            case 0: 
            {
                CVMClassBlock* callerClass = (CVMClassBlock*)n->item;
                //fprintf(stderr, "CASE 1\n");
                CVMtypeidClassNameToCString(CVMcbClassName(callerClass), tmp1, 512);
                fprintf(fp, "%s\n", tmp1);
                break;
            }
            case 1:
            {
                CVMMethodBlock* callerMethod = (CVMMethodBlock*)n->item;
                //fprintf(stderr, "CASE 2\n");
                tid = CVMmbNameAndTypeID(callerMethod);
                CVMtypeidMethodNameToCString(tid, tmp1, 512);
                CVMtypeidMethodTypeToCString(tid, tmp2, 512);
                fprintf(fp, "%s %s %d\n", tmp1, tmp2, CVMmbMethodIndex(callerMethod));
                break;
            }
            case 2:
            {
                int bpc = (int)n->item;
                fprintf(fp, "%d\n", bpc);
                break;
            }
            case 3:
            {
                CVMMethodBlock* calleeMethod = (CVMMethodBlock*)n->item;
                //fprintf(stderr, "CASE 3\n");
                CVMtypeidClassNameToCString(CVMcbClassName(CVMmbClassBlock(calleeMethod)), tmp1, 512);
                tid = CVMmbNameAndTypeID(calleeMethod);
                CVMtypeidMethodNameToCString(tid, tmp2, 512);
                CVMtypeidMethodTypeToCString(tid, tmp3, 512);
                fprintf(fp, "%s %s %s %d %d\n", tmp1, tmp2, tmp3, CVMmbMethodIndex(calleeMethod), n->count);
            }
        }
        if(n->child) _printTree(fp, n->child, depth+1);
    }
}

void printTree(void)
{
    //int i = 0;
    //int count = 0;
    //char* alphabet = "abcdefghijklmnopqrstuvwxyz";
    char aotcprof[10] = "aotcprof";
    /*
    {
        FILE* fpc = fopen("aotccount", "r");
        fscanf(fpc, "%d", &count);
        fclose(fpc);
        fpc = fopen("aotccount", "w");
        fprintf(fpc, "%d", (count+1));
        fclose(fpc);
        aotcprof[8] = alphabet[count];
    }
    */
    
    //FILE* fp = fopen("aotcprof", "w");
    FILE* fp = fopen(aotcprof, "w");
    fprintf(stderr, "writing.................\n");
    fprintf(fp, "#start\n");
    _printTree(fp, t.root, 0);
    fprintf(fp, "#end\n");
    fclose(fp);
}
//#endif
void writeProfileInfo() {
    pthread_mutex_lock(&mut);
    printTree();
    /*
    fprintf(stderr, "JAVA CALL COUNT: %u\n", JAVACall);
    fprintf(stderr, "AOTC CALL COUNT: %u\n", AOTCCall);
    fprintf(stderr, "AOTC INLINE CALL COUNT: %u\n", AOTCINCall);
    fprintf(stderr, "CNI CALL COUNT: %u\n", CNICall);
    fprintf(stderr, "JNI CALL COUNT: %u\n", JNICall);
    */
    pthread_mutex_unlock(&mut);
}

#elif AOTC_USE_PROFILE

#define TOKEN_TYPE_START 1
#define TOKEN_TYPE_CLASS 2
#define TOKEN_TYPE_METHOD 3
#define TOKEN_TYPE_BPC 4
#define TOKEN_TYPE_CALLEE 5
#define TOKEN_TYPE_END 6


unsigned int hashtableCode(Hashtable* hash, void* key) {
    return (((unsigned int)key) / (sizeof(void*))) % hash->capacity;
}
Hashtable* makeHashtable() {
    return makeHashtableWithCapacity(11);
}
Hashtable* makeHashtableWithCapacity(int capacity) {
    int i;
    Hashtable* hash = (Hashtable*)malloc(sizeof(Hashtable));
    hash->capacity = capacity;
    hash->count = 0;
    hash->entries = (HashtableEntry**)malloc(sizeof(HashtableEntry*) * capacity);
    for(i = 0 ; i < capacity ; i++) {
        hash->entries[i] = NULL;
    }
    return hash;
}
Hashtable* rootHash;
int containsHashtable(Hashtable* hash, void* key) {
    unsigned int hashCode = hashtableCode(hash, key);
    HashtableEntry* hashEntry;
    hashEntry = hash->entries[hashCode];
    while(hashEntry != NULL) {

        if(hashEntry->key == key) {
            return 1;
        }
        hashEntry = hashEntry->next;
    }
    return 0;

}
void* getHashtable(Hashtable* hash, void* key) {
    unsigned int hashCode = hashtableCode(hash, key);
    HashtableEntry* hashEntry = hash->entries[hashCode];
    while(hashEntry != NULL) {
        if(hashEntry->key == key) return hashEntry->value;
        hashEntry = hashEntry->next;
    }
    return NULL;
}

Hashtable* putHashtable(Hashtable* hash, void* key, void* value) {
    unsigned int hashCode;
    HashtableEntry* hashEntry;
    if(containsHashtable(hash, key)) return hash;
    if(hash->capacity < (0.75 * (hash->count+1))) {
        hash = rehash(hash);
    }
    hashCode = hashtableCode(hash, key);
    hashEntry = (HashtableEntry*)malloc(sizeof(HashtableEntry));
    hashEntry->key = key;
    hashEntry->value = value;
    hashEntry->next = hash->entries[hashCode];
    hash->entries[hashCode] = hashEntry;
    (hash->count)++;
    return hash;
}
Hashtable* rehash(Hashtable* oldHash) {
    int i;
    int oldCapacity = oldHash->capacity;
    int newCapacity = oldCapacity * 2;
    HashtableEntry** oldEntries = oldHash->entries;
    HashtableEntry** newEntries = (HashtableEntry**)malloc(sizeof(HashtableEntry*) * newCapacity);
    for( i = 0 ; i < newCapacity ; i++) {
        newEntries[i] = NULL;
    }
    oldHash->entries = newEntries;
    oldHash->capacity = newCapacity;
    for( i = 0 ; i < oldCapacity ; i++) {
        HashtableEntry* hashEntry = oldEntries[i];
        while(hashEntry != NULL) {
            unsigned int hashCode = hashtableCode(oldHash, hashEntry->key);
            HashtableEntry* nextEntry = hashEntry->next;
            hashEntry->next = newEntries[hashCode];
            newEntries[hashCode] = hashEntry;
            hashEntry = nextEntry;
        }
    }
    free(oldEntries);
    return oldHash;
}

int getTokenType(char* line)
{
	if (line[0] == ' ') {
		if (line[1] == ' ') {
			if (line[2] == ' ') {
				return TOKEN_TYPE_CALLEE;
			}
			return TOKEN_TYPE_BPC;
		}
        return TOKEN_TYPE_METHOD;
	} else {
        if(line[0] == '#') {
            if(line[1] == 's') {
                return TOKEN_TYPE_START;
            }else {
                return TOKEN_TYPE_END;
            }
        }else {
			return TOKEN_TYPE_CLASS;
        }
	}
}
CVMMethodBlock* loadMethodBlock(CVMExecEnv* ee, char* className, char* methodName, char* methodSignature, int methodIndex) {
    CVMClassBlock* cb = CVMclassLookupByNameFromClassLoader(ee, className, JNI_FALSE, NULL, NULL, JNI_FALSE);
    int i;
    CVMMethodBlock* mb = CVMcbMethodSlot(cb, methodIndex);
    CVMMethodTypeID tid;
    char tmp1[512];
    char tmp2[512];
    {
        tid = CVMmbNameAndTypeID(mb);
        CVMtypeidMethodNameToCString(tid, tmp1, 512);
        CVMtypeidMethodTypeToCString(tid, tmp2, 512);
        if(strcmp(tmp1, methodName) == 0 && strcmp(tmp2, methodSignature) == 0) return mb;
    }
    for(i = 0 ; i < CVMcbMethodCount(cb) ; i++) {
        mb = CVMcbMethodSlot(cb, i);
        tid = CVMmbNameAndTypeID(mb);
        CVMtypeidMethodNameToCString(tid, tmp1, 512);
        CVMtypeidMethodTypeToCString(tid, tmp2, 512);
        if(strcmp(tmp1, methodName) == 0 && strcmp(tmp2, methodSignature) == 0) return mb;
    }
    CVMconsolePrintf("return NULL\n");
    return NULL;
}
CallInfo* callee;
Hashtable* calleeHash;
char currentClassName[1024];
int currentBpc;

void parseAOTFile(CVMExecEnv* ee, Hashtable* callerHash, char* line) {
    int tokenType = getTokenType(line);
    switch(tokenType) {
        case TOKEN_TYPE_CLASS:
            sscanf(line, "%s", currentClassName);
            break;
        case TOKEN_TYPE_METHOD:
        {
            char tmp1[1024], tmp2[1024];
            int tmp3;
            CVMMethodBlock* caller;
            sscanf(line, "%s %s %d", tmp1, tmp2, &tmp3);
            caller = loadMethodBlock(ee, currentClassName, tmp1, tmp2, tmp3);
            calleeHash = makeHashtable();
            callerHash = putHashtable(callerHash, (void*)caller, (void*)calleeHash);
            break;
        }
        case TOKEN_TYPE_BPC:
        {
            sscanf(line, "%d", &currentBpc);
            callee = (CallInfo*)malloc(sizeof(CallInfo));
            callee->totalCallCount = 0;
            callee->callCount = 0;
            calleeHash = putHashtable(calleeHash, (void*)currentBpc, (void*)callee);
            break;
        }
        case TOKEN_TYPE_CALLEE:
        {
            char tmp1[1024], tmp2[1024], tmp3[1024];
            int tmp4, tmp5;
            sscanf(line, "%s %s %s %d %d", tmp1, tmp2, tmp3, &tmp4, &tmp5);
            callee->totalCallCount += tmp5;
            if(callee->callCount < tmp5) {
                callee->mb = loadMethodBlock(ee, tmp1, tmp2, tmp3, tmp4);
                callee->callCount = tmp5;
            }
            break;
        }
        default:
            break;
    }
}
CVMUint8 loadAOTFile(CVMExecEnv* ee) {
    char line[1024];
    FILE* fp = fopen("aotcprof", "r");
    if(fp == NULL) {
        CVMconsolePrintf("No aotcprof!!\n");
        return 0;
    }
    rootHash = makeHashtable();
    while(fgets(line, 1024, fp)) {
        parseAOTFile(ee, rootHash, line);
    }
    fclose(fp);
    return 1;
}
CVMUint8 unloadAOTFile() {
    int i;
    if(rootHash == NULL) return 0;
    for(i = 0 ; i < rootHash->capacity ; i++) {
        HashtableEntry* hashEntry1 = rootHash->entries[i];
        while(hashEntry1 != NULL) {
            HashtableEntry* freeEntry1 = hashEntry1;
            Hashtable* hash = (Hashtable*)freeEntry1->value;
            {
                int j;
                for(j = 0 ; j < hash->capacity ; j++) {
                    HashtableEntry* hashEntry2 = hash->entries[j];
                    while(hashEntry2 != NULL) {
                        HashtableEntry* freeEntry2 = hashEntry2;
                        CallInfo* calleeInfo = (CallInfo*)freeEntry2->value;
                        
                        hashEntry2 = hashEntry2->next;
                        free(calleeInfo);
                        free(freeEntry2);
                    }
                }
            }
            hashEntry1 = hashEntry1->next;
            free(hash->entries);
            free(hash);
            free(freeEntry1);
        }
    }
    free(rootHash->entries);
    free(rootHash);
    return 1;
}

CVMUint32 getAOTCallCount(CVMMethodBlock* callerMB, CVMMethodBlock* calleeMB,  CVMUint32 bpc) {
    Hashtable* hash;
    CallInfo* info;
    if(rootHash == NULL || callerMB == NULL || calleeMB == NULL) return 0;
    hash = (Hashtable*)getHashtable(rootHash, (void*)callerMB);
    if(hash == NULL) return 0;

    info = (CallInfo*)getHashtable(hash, (void*)bpc);
    if(info == NULL) return 0;
    
    if(info->mb == calleeMB) return info->callCount;
    return 0;
}

CVMMethodBlock* getAOTMethod(CVMMethodBlock* callerMB, CVMUint32 bpc) {
    Hashtable* hash;
    CallInfo* info;
    if(rootHash == NULL || callerMB == NULL) return NULL;
    hash = (Hashtable*)getHashtable(rootHash, (void*)callerMB);
    if(hash == NULL) return NULL;

    info = (CallInfo*)getHashtable(hash, (void*)bpc);
    if(info == NULL || info->mb) return NULL;
    if(info->totalCallCount <= (info->callCount * 2)) {
        return info->mb;
    }
    return NULL;
}
#endif
