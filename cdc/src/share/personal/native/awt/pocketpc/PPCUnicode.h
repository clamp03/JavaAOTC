/*
 * 
 * Copyright  1990-2006 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version
 * 2 only, as published by the Free Software Foundation. 
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included at /legal/license.txt). 
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA 
 * 
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, CA 95054 or visit www.sun.com if you need additional
 * information or have any questions. 
 */

#ifndef _WINCE_UNICODE_H
#define _WINCE_UNICODE_H

#include <malloc.h>
#include "awt.h"

// Get a Unicode string copy of a Java String object (Java String aren't
// null-terminated).
extern LPWSTR J2WHelper(LPWSTR lpw, LPWSTR lpj, int nChars);
extern LPWSTR J2WHelper1(LPWSTR lpw, LPWSTR lpj, int offset, int nChars);
extern LPWSTR JNI_J2WHelper1(JNIEnv *env, LPWSTR lpw, jstring jstr);

#define TO_WSTRING(jstr) \
   ((jstr == NULL) ? NULL : \
     (JNI_J2WHelper1(env, (LPWSTR) new jchar[(env->GetStringLength(jstr)+1)*2], \
		     jstr) \
    ))

extern jstring JNI_W2JHelper1(JNIEnv *env, LPWSTR lpw);
#define TO_JSTRING(lpw) \
   ((lpw == NULL) ? NULL : \
     (JNI_W2JHelper1(env, lpw)))

// Translate an ANSI string into a Unicode one.
extern LPWSTR A2WHelper(LPWSTR lpw, LPCSTR lpa, int nChars);
#define A2W(lpa) \
    ((NULL == lpa) ? NULL : \
     (A2WHelper((LPWSTR) new jchar[(strlen(lpa)+1)*2], lpa, strlen(lpa)+1)))

// Translate a Unicode string into an ANSI one.
extern LPSTR W2AHelper(LPSTR lpa, LPCWSTR lpw, int nChars);
#define W2A(lpw) \
    ((NULL == (LPCWSTR)lpw) ? NULL : \
     (W2AHelper((LPSTR) new jchar[(wcslen(lpw)+1)*2], lpw, (wcslen(lpw)+1)*2)))


#endif // _WINCE_UNICODE_H
