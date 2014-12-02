#include "javavm/include/asmmacros_cpu.h"
#include "javavm/include/jit/jitasmmacros_cpu.h"
#include "javavm/include/jit/jitasmconstants.h"
#include "javavm/include/porting/jit/jit.h"
#include "javavm/include/sync.h"
ENTRY( ParseArgAndCall_Java_Test_factorial__F )
addiu sp, sp, -20
sw ra, 12(sp)
sw a1, 16(sp)
LA(v0, Test_Classblock)
LA(jp, Java_Test_factorial__F)
lw a2, 0(a1)
lw a1, 48(v0)
jalr jp
lw ra, 12(sp)
lw t0, 16(sp)
swc1 $f0, 0(t0)
addiu v0, t0, 4
addiu sp, sp, 20
jr ra
SET_SIZE(ParseArgAndCall_Java_Test_factorial__F )
ENTRY( ParseArgAndCall_Java_Test_factorial__D )
addiu sp, sp, -24
sw ra, 16(sp)
sw a1, 20(sp)
LA(v0, Test_Classblock)
LA(jp, Java_Test_factorial__D)
lw a2, 0(a1)
lw a3, 4(a1)
lw a1, 48(v0)
jalr jp
lw ra, 16(sp)
lw t0, 20(sp)
swc1 $f0, 0(t0)
swc1 $f1, 4(t0)
addiu sp, sp, 24
jr ra
SET_SIZE(ParseArgAndCall_Java_Test_factorial__D )
ENTRY( ParseArgAndCall_Java_Test_factorial__J )
addiu sp, sp, -24
sw ra, 16(sp)
sw a1, 20(sp)
LA(v0, Test_Classblock)
LA(jp, Java_Test_factorial__J)
lw a2, 0(a1)
lw a3, 4(a1)
lw a1, 48(v0)
jalr jp
lw ra, 16(sp)
lw t0, 20(sp)
sw v0, 0(t0)
sw v1, 4(t0)
addiu v0, t0, 8
addiu sp, sp, 24
jr ra
SET_SIZE(ParseArgAndCall_Java_Test_factorial__J )
ENTRY( ParseArgAndCall_Java_Test_factorial__Ljava_lang_Object_2 )
addiu sp, sp, -20
sw ra, 12(sp)
sw a1, 16(sp)
LA(v0, Test_Classblock)
LA(jp, Java_Test_factorial__Ljava_lang_Object_2)
addiu a2, a1, 0
lw a1, 48(v0)
jalr jp
lw ra, 12(sp)
lw t0, 16(sp)
sw v0, 0(t0)
addiu v0, t0, 4
addiu sp, sp, 20
jr ra
SET_SIZE(ParseArgAndCall_Java_Test_factorial__Ljava_lang_Object_2 )
