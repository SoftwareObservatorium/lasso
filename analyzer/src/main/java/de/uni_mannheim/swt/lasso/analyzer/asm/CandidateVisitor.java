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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import de.uni_mannheim.swt.lasso.analyzer.model.ClassType;
import de.uni_mannheim.swt.lasso.analyzer.model.CompilationUnit;
import de.uni_mannheim.swt.lasso.analyzer.model.Method;
import de.uni_mannheim.swt.lasso.analyzer.model.Parameter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * ASM Class visitor for current candidate.
 */
public class CandidateVisitor extends ClassVisitor {

    private int access;

    private CompilationUnit unit;

    // private Map<Method, MethodNode> mToNodes = new HashMap<>();
    //
    // private Map<Method, MethodBodyDependencyVisitor> mToDeps = new
    // HashMap<>();

    public CandidateVisitor(ClassVisitor cv) {
        super(DependencyVisitor.ASM_VERSION, cv);
    }

    public static String toFqName(String name) {
        return StringUtils.replaceEach(name, new String[] { "/", "$" }, new String[] { ".", "." });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.ClassVisitor#visit(int, int, java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String[])
     */
    @Override
    public void visit(final int version, final int access, final String name, final String signature,
            final String superName, final String[] interfaces) {
        this.access = access;

        CompilationUnit unit = new CompilationUnit();
        // unit.setName(toFqName(name));
        unit.setByteCodeName(name);
        unit.setByteCodeVersion(version);

        if ((access & Opcodes.ACC_INTERFACE) != 0) {
            unit.setType(ClassType.INTERFACE);
        } else if ((access & Opcodes.ACC_ENUM) != 0) {
            unit.setType(ClassType.ENUM);
        } else if ((access & Opcodes.ACC_ANNOTATION) != 0) {
            unit.setType(ClassType.ANNOTATION);
        } else {
            unit.setType(ClassType.CLASS);
        }

        List<String> javaKeywords = new LinkedList<>();
        appendAccess(javaKeywords, access);

        unit.setJavaKeywords(javaKeywords);

        this.unit = unit;

        // System.out.println("Class " + name);

        super.visit(version, access, name, signature, superName, interfaces);
    }

    // /*
    // * (non-Javadoc)
    // *
    // * @see org.objectweb.asm.ClassVisitor#visitInnerClass(java.lang.String,
    // * java.lang.String, java.lang.String, int)
    // */
    // @Override
    // public void visitInnerClass(String name, String outerName, String
    // innerName, int access) {
    // this.access = access;
    //
    // CompilationUnit parentUnit = stack.peek();
    //
    // // TODO set owning class?
    //
    // CompilationUnit unit = new CompilationUnit();
    // unit.setInnerClass(true);
    //
    // unit.setName(name);
    // unit.setByteCodeVersion(parentUnit.getByteCodeVersion());
    //
    // units.put(name, unit);
    //
    // stack.push(unit);
    //
    // System.out.println("Inner " + name);
    //
    // super.visitInnerClass(name, outerName, innerName, access);
    // }

    // /* (non-Javadoc)
    // * @see org.objectweb.asm.ClassVisitor#visitOuterClass(java.lang.String,
    // java.lang.String, java.lang.String)
    // */
    // @Override
    // public void visitOuterClass(String owner, String name, String desc) {
    // //
    // System.out.println("Outer Class " + name);
    //
    // super.visitOuterClass(owner, name, desc);
    // }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.asm.ClassVisitor#visitEnd()
     */
    @Override
    public void visitEnd() {
        // CompilationUnit unit = stack.pop();
        //
        // System.out.println("Class popped " + unit.getName());

        // // cyclomatic complexity
        // mToNodes.forEach((method, node) -> {
        // try {
        // Graph graph = new Graph(unit.getByteCodeName(), node, false);
        //
        // int cc = graph.getCyclomaticComplexity();
        //
        // method.getMeasures().put("static_cc", new Integer(cc).doubleValue());
        // } catch (AnalyzerException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // });

        super.visitEnd();
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
            final String[] exceptions) {
        List<String> javaKeywords = new LinkedList<>();

        //
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            javaKeywords.add("deprecated");
        }

        appendAccess(javaKeywords, access & ~Opcodes.ACC_VOLATILE);
        if ((access & Opcodes.ACC_NATIVE) != 0) {
            javaKeywords.add("native");
        }
        if ((access & Opcodes.ACC_VARARGS) != 0) {
            javaKeywords.add("varargs");
        }
        if ((access & Opcodes.ACC_BRIDGE) != 0) {
            javaKeywords.add("bridge");
        }
        if ((this.access & Opcodes.ACC_INTERFACE) != 0 && (access & Opcodes.ACC_ABSTRACT) == 0
                && (access & Opcodes.ACC_STATIC) == 0) {
            javaKeywords.add("default");
        }

        Method method = new Method();
        method.setName(name);
        method.setJavaKeywords(javaKeywords);

        Parameter returnParameter = new Parameter();
        // return
        Type rv = Type.getReturnType(desc);
        returnParameter.setType(toFqName(rv.getClassName()));
        returnParameter.setName(null);
        if (rv.getSort() == Type.ARRAY) {
            returnParameter.setArrayDim(rv.getDimensions());
        }
        method.setReturnParameter(returnParameter);

        // input
        Type[] inputParams = Type.getArgumentTypes(desc);
        if (ArrayUtils.isNotEmpty(inputParams)) {
            method.setParameters(Arrays.stream(inputParams).map(type -> {
                Parameter input = new Parameter();
                input.setType(toFqName(type.getClassName()));
                input.setName(null);
                if (type.getSort() == Type.ARRAY) {
                    input.setArrayDim(type.getDimensions());
                }

                return input;
            }).collect(Collectors.toList()));
        }

        // measures

        // // collect metrics
        // MethodNode node = new MethodNode(DependencyVisitor.ASM_VERSION,
        // access, name, desc, signature, exceptions);
        //
        method.setByteCodeName(unit.getByteCodeName() + "." + name + desc);
        //
        unit.addMethod(method);
        //
        // mToNodes.put(method, node);

        // // collect deps
        // MethodBodyDependencyVisitor mBody = new
        // MethodBodyDependencyVisitor(node);
        // mToDeps.put(method, mBody);
        //
        // return mBody.visitMethod(access, name, desc, signature, exceptions);

        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    /**
     * Appends a string representation of the given access modifiers to
     * {@link #buf buf}.
     *
     * @param access
     *            some access modifiers.
     */
    private void appendAccess(List<String> modifiers, final int access) {
        if ((access & Opcodes.ACC_PUBLIC) != 0) {
            modifiers.add("public");
        }
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            modifiers.add("private");
        }
        if ((access & Opcodes.ACC_PROTECTED) != 0) {
            modifiers.add("protected");
        }
        if ((access & Opcodes.ACC_FINAL) != 0) {
            modifiers.add("final");
        }
        if ((access & Opcodes.ACC_STATIC) != 0) {
            modifiers.add("static");
        }
        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
            modifiers.add("synchronized");
        }
        if ((access & Opcodes.ACC_VOLATILE) != 0) {
            modifiers.add("volatile");
        }
        if ((access & Opcodes.ACC_TRANSIENT) != 0) {
            modifiers.add("transient");
        }
        if ((access & Opcodes.ACC_ABSTRACT) != 0) {
            modifiers.add("abstract");
        }
        if ((access & Opcodes.ACC_STRICT) != 0) {
            modifiers.add("strictfp");
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            modifiers.add("synthetic");
        }
        if ((access & Opcodes.ACC_MANDATED) != 0) {
            modifiers.add("mandated");
        }
        if ((access & Opcodes.ACC_ENUM) != 0) {
            modifiers.add("enum");
        }
    }

    /**
     * @return the unit
     */
    public CompilationUnit getUnit() {
        return unit;
    }
}
