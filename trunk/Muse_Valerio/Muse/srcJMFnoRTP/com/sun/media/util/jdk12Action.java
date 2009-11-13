/*
 * @(#)jdk12Action.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.Permission;

public class jdk12Action {
     public static Constructor getCheckPermissionAction() throws NoSuchMethodException {
	 return CheckPermissionAction2.class.getConstructor(new Class[] {
	     Permission.class});
     }

}


class CheckPermissionAction2 implements java.security.PrivilegedAction {

    private Permission permission;
    public CheckPermissionAction2(Permission p) {
	permission = p;
    }

    public Object run() {
	AccessController.checkPermission(permission);
	return null; // nothing to return
    }
}
