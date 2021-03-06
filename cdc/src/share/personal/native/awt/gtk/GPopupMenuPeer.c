/*
 * @(#)GPopupMenuPeer.c	1.12 06/10/10
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
 *
 */

#include <gtk/gtk.h>

#include "jni.h"
#include "sun_awt_gtk_GPopupMenuPeer.h"
#include "GPopupMenuPeer.h"

JNIEXPORT void JNICALL
Java_sun_awt_gtk_GPopupMenuPeer_create (JNIEnv *env, jobject this)
{
	GPopupMenuPeerData *data = (GPopupMenuPeerData *)calloc (1, sizeof (GPopupMenuPeerData));
	
	if (data == NULL)
	{
		(*env)->ThrowNew (env, GCachedIDs.OutOfMemoryErrorClass, NULL);
		return;
	}
	
	awt_gtk_threadsEnter();
	awt_gtk_GMenuPeerData_init(env, this, (GMenuPeerData *)data);
	awt_gtk_threadsLeave();
}

static gint menuX, menuY;

static void
menuPositionFunc (GtkMenu *menu, gint *x, gint *y, gpointer userData)
{
	*x = menuX;
	*y = menuY;
}

JNIEXPORT void JNICALL
Java_sun_awt_gtk_GPopupMenuPeer_show (JNIEnv *env, jobject this, jint x, jint y)
{
	GPopupMenuPeerData *data = (GPopupMenuPeerData *)(*env)->GetIntField (env, this, GCachedIDs.GMenuItemPeer_dataFID);

	menuX = (gint)x;
	menuY = (gint)y;
	
	awt_gtk_threadsEnter();
	gtk_menu_popup (GTK_MENU(((GMenuPeerData *)data)->menuWidget), NULL, NULL, menuPositionFunc, NULL, 0, 0);
	awt_gtk_threadsLeave();
}

