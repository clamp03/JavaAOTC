/*
 * @(#)UnexpectedException.java	1.6 06/10/10
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

package java.rmi;

/**
 * An <code>UnexpectedException</code> is thrown if the client of a
 * remote method call receives, as a result of the call, a checked
 * exception that is not among the checked exception types declared in the
 * <code>throws</code> clause of the method in the remote interface.
 * 
 * @version 1.9, 02/02/00
 * @author  Roger Riggs
 * @since   JDK1.1
 */
public class UnexpectedException extends RemoteException {
    /* indicate compatibility with JDK 1.1.x version of class */
    private static final long serialVersionUID = 1800467484195073863L;
    /**
     * Constructs an <code>UnexpectedException</code> with the specified
     * detail message.
     *
     * @param s the detail message
     * @since JDK1.1
     */
    public UnexpectedException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>UnexpectedException</code> with the specified
     * detail message and nested exception.
     *
     * @param s the detail message
     * @param ex the nested exception
     * @since JDK1.1
     */
    public UnexpectedException(String s, Exception ex) {
        super(s, ex);
    }
}
