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
package de.uni_mannheim.swt.lasso.arena.sequence;

import de.uni_mannheim.swt.lasso.arena.DefaultArena;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ReflectionConstructorSignature;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ReflectionMethodSignature;

import org.junit.jupiter.api.Test;
import randoop.ExecutionVisitor;

import randoop.sequence.ExecutableSequence;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Glitches found when using randoop. For debugging purposes.
 *
 * @author Marcus Kessel
 */
public class RandoopBugsTest {

    /**
     * Issue with exported code string (ambigious assertEquals())
     *
     * <pre>
     *      java.lang.Integer int1 = new java.lang.Integer(0);
     *      int int2 = int1.intValue();
     *      org.junit.Assert.assertEquals("'" + int1 + "' != '" + 0+ "'", int1, 0);
     * </pre>
     *
     * @throws NoSuchMethodException
     */
    @Test
    public void test_PrimitiveWrapper() throws NoSuchMethodException, IllegalAccessException {
        SequenceSpecification ss = new SequenceSpecification();
        ss.setName("customtest");

        ConstructorCallStatement constructorCallStatement = new ConstructorCallStatement(
                new ReflectionConstructorSignature(Integer.class.getConstructor(int.class)));
        ss.addStatement(constructorCallStatement, ss.getNextPosition());

        ValueStatement valueStatement = new ValueStatement(int.class, 0);
        constructorCallStatement.addInput(valueStatement);

        MethodCallStatement methodCallStatement = new MethodCallStatement(
                new ReflectionMethodSignature(Integer.class.getMethod("intValue")));
        ss.addStatement(methodCallStatement, ss.getNextPosition());

        // first input always subject
        methodCallStatement.addInput(constructorCallStatement); // subject

        SequenceExecutionRecord sequenceExecutionRecord = ss.instantiate(null,null);

        System.out.println(sequenceExecutionRecord.getSequence());

        ExecutableSequence executableSequence = new DefaultArena().execute(sequenceExecutionRecord.getSequence(), new ExecutionVisitor() {
            @Override
            public void visitBeforeStatement(ExecutableSequence executableSequence,int i) {
            }

            @Override
            public void visitAfterStatement(ExecutableSequence executableSequence, int i) {
            }

            @Override
            public void initialize(ExecutableSequence executableSequence) {
            }

            @Override
            public void visitAfterSequence(ExecutableSequence executableSequence) {
            }
        });

        System.out.println(executableSequence.toCodeString());

//        for(Check check : executableSequence.getChecks().checks()) {
//            System.out.println(String.format("%s => '%s'", check.getClass().getName(), check.toCodeStringPostStatement()));
//
//            if(check instanceof ObjectCheck) {
//                ObjectCheck objectCheck = (ObjectCheck) check;
//                ObjectContract contract = (ObjectContract) FieldUtils.readDeclaredField(objectCheck, "contract", true);
//                System.out.println("contract " + contract.getClass().getName() + " => " + contract.toCodeString());
//                for(Type type : contract.getInputTypes()) {
//                    System.out.println("type => " + type);
//                }
//
//                if(contract instanceof PrimValue) {
//                    PrimValue primValue = (PrimValue) contract;
//
//                    PrimValue.EqualityMode equalityMode = (PrimValue.EqualityMode) FieldUtils.readDeclaredField(primValue, "equalityMode", true);
//
//                    if(equalityMode == PrimValue.EqualityMode.EQUALSMETHOD) {
//                        Variable[] vars = (Variable[]) FieldUtils.readDeclaredField(objectCheck, "vars", true);
//                        for(Variable var : vars) {
//                            System.out.println("var => " + var.getType());
//
//                            if(var.getType().isBoxedPrimitive()) {
//                                System.out.println("Found bug");
//
//                                // replace contract with our own one
//                                //FieldUtils.writeField();
//                            }
//                        }
//                    }
//                }
//            }
//        }

        assertTrue(executableSequence.toCodeString().contains("org.junit.Assert.assertEquals(\"'\" + int1 + \"' != '\" + 0+ \"'\", (java.lang.Object) int1, 0);"));
    }

    @Test
    public void test_string_getbytes() throws NoSuchMethodException {
        SequenceSpecification ss = new SequenceSpecification();
        ss.setName("customtest");

        ValueStatement valueStatement = new ValueStatement(String.class, "hi");
        ss.addStatement(valueStatement, ss.getNextPosition());

        MethodCallStatement methodCallStatement = new MethodCallStatement(
                new ReflectionMethodSignature(String.class.getMethod("getBytes")));
        ss.addStatement(methodCallStatement, ss.getNextPosition());

        // first input always subject
        methodCallStatement.addInput(valueStatement); // subject

        SequenceExecutionRecord sequenceExecutionRecord = ss.instantiate(null,null);

        System.out.println(sequenceExecutionRecord.getSequence());
    }
}
