/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.arena.classloader.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilePermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;

/**
 * {@link SecurityManager} for CUT executions.
 *
 * @author Marcus Kessel
 */
// FIXME does not work as intended
public class SandboxSecurityManager extends SecurityManager {

    private static final Logger LOG = LoggerFactory
            .getLogger(SandboxSecurityManager.class);

    private final String basePath;

    public SandboxSecurityManager(String basePath) {
        this.basePath = basePath;
    }

    // FIXME do not allow foreign code to call System.exit(). only we are allowed to do it.
    // permission.getName().startsWith("exitVM")

    /**
     * FIXME implement a fair set of permissions.
     *
     * @param perm
     */
    @Override
    public void checkPermission(Permission perm) {
        // FIXME java.lang.StackOverflowError
        //	at de.uni_mannheim.swt.lasso.arena.sandbox.SandboxSecurityManager.checkPermission(SandboxSecurityManager.java:36)
        //	at java.lang.ClassLoader.checkClassLoaderPermission(ClassLoader.java:1528)

//        for (Class<?> clazz : getClassContext()) {
//
//            // handle CUT permissions here
//            if(clazz.getClassLoader() instanceof Container) {
//
//                LOG.debug("Access to '{}' from loader '{}' of class '{}'", perm, clazz.getClassLoader().getClass(), clazz.getName());
//
//                if(perm instanceof FilePermission) {
//                    PermissionCollection perms = cutPermissions();
//
//                    if(perms.implies(perm)) {
//                        LOG.debug("File access identified");
//                    } else {
//                        //         super.checkPermission(perm);
//                    }
//                }
//            }
//        }
    }

    /**
     * FIXME implement a fair set of permissions.
     *
     * @param perm
     * @param context
     */
    @Override
    public void checkPermission(Permission perm, Object context) {
//        for (Class<?> clazz : getClassContext()) {
//
//            // handle CUT permissions here
//            if(clazz.getClassLoader() instanceof Container) {
//
//                LOG.debug("Access context '{}' to '{}' from loader '{}' of class '{}'", context, perm, clazz.getClassLoader().getClass(), clazz.getName());
//
//                if(perm instanceof FilePermission) {
//                    PermissionCollection perms = cutPermissions();
//
//                    if(perms.implies(perm)) {
//                        LOG.debug("File access identified");
//                    } else {
//                        //         super.checkPermission(perm);
//                    }
//                }
//            }
//        }
//
//        // from super
//        if (context instanceof AccessControlContext) {
//            //((AccessControlContext)context).checkPermission(perm);
//        } else {
//            //throw new SecurityException();
//        }
    }

    private PermissionCollection cutPermissions() {
        Permissions permissions = new Permissions();
        FilePermission filePermission = new FilePermission(basePath, "read,readlink");
        permissions.add(filePermission);

        LOG.debug("Called {}", basePath);

        return permissions;
    }
}
