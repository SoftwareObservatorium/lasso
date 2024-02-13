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
package de.uni_mannheim.swt.lasso.analyzer.asm;

/***
 * ASM examples: examples showing how ASM can be used
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.*;

import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

/**
 * DependencyVisitor
 *
 * @author Eugene Kuleshov
 * @author Marcus Kessel (modifications etc.)
 */
public class DependencyVisitor extends ClassVisitor {

    public static final int ASM_VERSION = Opcodes.ASM8;

    Set<String> packages = new HashSet<String>();

    Map<String, Map<String, Integer>> groups = new HashMap<String, Map<String, Integer>>();

    Map<String, Integer> current;

    public Map<String, Map<String, Integer>> getGlobals() {
        return groups;
    }

    public Set<String> getPackages() {
        return packages;
    }

    private String superClass;
    private Set<String> interfaces;

    public boolean isGeneric() {
        return generic;
    }

    private boolean generic = false;

    public DependencyVisitor() {
        super(ASM_VERSION);
    }

    // ClassVisitor

    @Override
    public void visit(
            final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {
        String p = getGroupKey(name);
        current = groups.get(p);
        if (current == null) {
            current = new HashMap<String, Integer>();
            groups.put(p, current);
        }

        if (signature == null) {
            if (superName != null) {
                addInternalName(superName);

                // add superClass
                addInternalSuperClassName(superName);
            }

            addInternalNames(interfaces);

            // add interfaces
            if (ArrayUtils.isNotEmpty(interfaces)) {
                Arrays.stream(interfaces).forEach(i -> addInternalInterfaceName(i));
            }
        } else {
            addSignature(signature);

            this.generic = true;
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(
            final String desc,
            final boolean visible) {
        addDesc(desc);
        return new AnnotationDependencyVisitor();
    }

    @Override
    public FieldVisitor visitField(
            final int access,
            final String name,
            final String desc,
            final String signature,
            final Object value) {
        if (signature == null) {
            addDesc(desc);
        } else {
            addTypeSignature(signature);
        }
        if (value instanceof Type) {
            addType((Type) value);
        }
        return new FieldDependencyVisitor();
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions) {
        if (signature == null) {
            addMethodDesc(desc);
        } else {
            addSignature(signature);
        }
        addInternalNames(exceptions);

        //

        return new MethodDependencyVisitor();
    }

    class AnnotationDependencyVisitor extends AnnotationVisitor {

        public AnnotationDependencyVisitor() {
            super(ASM_VERSION);
        }

        @Override
        public void visit(final String name, final Object value) {
            if (value instanceof Type) {
                addType((Type) value);
            }
        }

        @Override
        public void visitEnum(
                final String name,
                final String desc,
                final String value) {
            addDesc(desc);
        }

        @Override
        public AnnotationVisitor visitAnnotation(
                final String name,
                final String desc) {
            addDesc(desc);
            return this;
        }

        @Override
        public AnnotationVisitor visitArray(final String name) {
            return this;
        }
    }

    class FieldDependencyVisitor extends FieldVisitor {

        public FieldDependencyVisitor() {
            super(ASM_VERSION);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            addDesc(desc);
            return new AnnotationDependencyVisitor();
        }
    }

    class MethodDependencyVisitor extends MethodVisitor {

        public MethodDependencyVisitor() {
            super(ASM_VERSION);
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return new AnnotationDependencyVisitor();
        }

        @Override
        public AnnotationVisitor visitAnnotation(
                final String desc,
                final boolean visible) {
            addDesc(desc);
            return new AnnotationDependencyVisitor();
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(
                final int parameter,
                final String desc,
                final boolean visible) {
            addDesc(desc);
            return new AnnotationDependencyVisitor();
        }

        @Override
        public void visitTypeInsn(final int opcode, final String type) {
            addType(Type.getObjectType(type));
        }

        @Override
        public void visitFieldInsn(
                final int opcode,
                final String owner,
                final String name,
                final String desc) {
            addInternalName(owner);
            addDesc(desc);
        }

        @Override
        public void visitMethodInsn(
                final int opcode,
                final String owner,
                final String name,
                final String desc) {
            addInternalName(owner);
            addMethodDesc(desc);
        }

        @Override
        public void visitInvokeDynamicInsn(
                String name,
                String desc,
                Handle bsm,
                Object... bsmArgs) {
            addMethodDesc(desc);
            addConstant(bsm);
            for (int i = 0; i < bsmArgs.length; i++) {
                addConstant(bsmArgs[i]);
            }
        }

        @Override
        public void visitLdcInsn(final Object cst) {
            addConstant(cst);
        }

        @Override
        public void visitMultiANewArrayInsn(final String desc, final int dims) {
            addDesc(desc);
        }

        @Override
        public void visitLocalVariable(
                final String name,
                final String desc,
                final String signature,
                final Label start,
                final Label end,
                final int index) {
            addTypeSignature(signature);
        }

        @Override
        public void visitTryCatchBlock(
                final Label start,
                final Label end,
                final Label handler,
                final String type) {
            if (type != null) {
                addInternalName(type);
            }
        }
    }

    class SignatureDependencyVisitor extends SignatureVisitor {

        String signatureClassName;

        boolean isSuperClass = false;
        boolean isInterface = false;

        public SignatureDependencyVisitor() {
            super(ASM_VERSION);
        }

        @Override
        public SignatureVisitor visitSuperclass() {
            isSuperClass = true;
            isInterface = false;

            return super.visitSuperclass();
        }

        @Override
        public SignatureVisitor visitInterface() {
            isSuperClass = false;
            isInterface = true;

            return super.visitInterface();
        }

        @Override
        public void visitClassType(final String name) {
            signatureClassName = name;
            addInternalName(name);

            if (isSuperClass) {
                addInternalSuperClassName(name);
            }

            if (isInterface) {
                addInternalInterfaceName(name);
            }
        }

        @Override
        public void visitInnerClassType(final String name) {
            signatureClassName = signatureClassName + "$" + name;
            addInternalName(signatureClassName);
        }
    }

    // ---------------------------------------------

    private String getGroupKey(String name) {
//        int n = name.lastIndexOf('/');
//        if (n > -1) {
//            name = name.substring(0, n);
//        }
//        packages.add(name);
//        return name;

        packages.add(name);
        return name;
    }

    private void addName(final String name) {
        if (name == null) {
            return;
        }
        String p = getGroupKey(name);
        if (current.containsKey(p)) {
            current.put(p, current.get(p) + 1);
        } else {
            current.put(p, 1);
        }
    }

    void addInternalName(final String name) {
        addType(Type.getObjectType(name));
    }

    void addInternalSuperClassName(final String name) {
        this.superClass = Type.getObjectType(name).getInternalName();
    }

    void addInternalInterfaceName(final String name) {
        if (name == null) {
            return;
        }

        if (interfaces == null) {
            interfaces = new HashSet<>();
        }

        interfaces.add(Type.getObjectType(name).getInternalName());
    }

    private void addInternalNames(final String[] names) {
        for (int i = 0; names != null && i < names.length; i++) {
            addInternalName(names[i]);
        }
    }

    void addDesc(final String desc) {
        addType(Type.getType(desc));
    }

    void addMethodDesc(final String desc) {
        addType(Type.getReturnType(desc));
        Type[] types = Type.getArgumentTypes(desc);
        for (int i = 0; i < types.length; i++) {
            addType(types[i]);
        }
    }

    void addType(final Type t) {
        switch (t.getSort()) {
            case Type.ARRAY:
                addType(t.getElementType());
                break;
            case Type.OBJECT:
                addName(t.getInternalName());
                break;
            case Type.METHOD:
                addMethodDesc(t.getDescriptor());
                break;
        }
    }

    private void addSignature(final String signature) {
        if (signature != null) {
            new SignatureReader(signature).accept(new SignatureDependencyVisitor());
        }
    }

    void addTypeSignature(final String signature) {
        if (signature != null) {
            new SignatureReader(signature).acceptType(new SignatureDependencyVisitor());
        }
    }

    void addConstant(final Object cst) {
        if (cst instanceof Type) {
            addType((Type) cst);
        } else if (cst instanceof Handle) {
            Handle h = (Handle) cst;
            addInternalName(h.getOwner());
            addMethodDesc(h.getDesc());
        }
    }

    public String getSuperClass() {
        return superClass;
    }

    public Set<String> getInterfaces() {
        return interfaces;
    }
}

