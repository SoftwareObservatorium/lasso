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
package de.uni_mannheim.swt.lasso.runner.permutator.strategy.producer;

import de.uni_mannheim.swt.lasso.runner.permutator.Candidate;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Scans classpath for static field instances of required type t.
 *
 * @author Marcus Kessel
 */
public class StaticFieldInstance implements ProducerStrategy {

    private static final org.slf4j.Logger LOG = LoggerFactory
            .getLogger(StaticFieldInstance.class);

    private String[] packageNames;

    public StaticFieldInstance(String[] packageNames) {
        this.packageNames = packageNames;
    }

    @Override
    public List<Candidate> match(Class<?> t, Class<?>[] paramTypes) throws Throwable {
        List<Field> matches = scan(t);

        if(CollectionUtils.isEmpty(matches)) {
            return null;
        }

        List<Candidate> candidates = new ArrayList<>(matches.size());
        for(Field field : matches) {
            // register as member
            Candidate candidate = new Candidate(field, new int[]{0});
            candidate.setProducerStrategy(this);

            candidates.add(candidate);
        }

        return candidates;
    }

    @Override
    public Object createInstance(Candidate candidate, Object[] inputs) throws Throwable {
        if(candidate.getMethod() instanceof Field) {
            try {
                Field field = (Field) candidate.getMethod();
                // make accessible
                field.setAccessible(true);

                // static field has no instance
                Object value = field.get(null);

                return value;
            } catch (Throwable e) {
                // signal error
                return null;
            }
        } else {
            throw new IllegalArgumentException("Candidate member must be Field");
        }
    }

    /**
     * TODO Scan entire classpath
     *
     * @param cut
     * @return
     */
    public List<Field> scan(Class<?> cut) {
        List<Field> fields = new LinkedList<>();
        for(Field field : FieldUtils.getAllFieldsList(cut)) {
            // static + public + returns CUT
            if(java.lang.reflect.Modifier.isStatic(field.getModifiers())
                    && java.lang.reflect.Modifier.isPublic(field.getModifiers())
                    && field.getType().equals(cut)) {
                fields.add(field);
            }
        }

        return fields;
    }

//    private List<Field> scan(Class<?> t) {
//        String[] packageNames = ProducerUtils.getPackageNames(this.packageNames, t);
//
//        try (ScanResult scanResult =
//                     new ClassGraph()
//                             .enableStaticFinalFieldConstantInitializerValues()
//                             .whitelistPackages(packageNames)
//                             .scan()) {
//            //
//            //System.out.println(scanResult.toJSON(2));
//
//            List<Field> matches = new LinkedList<>();
//
//            // TODO what about interfaces?
//            ClassInfoList classes = scanResult.getAllClasses();
//            for(ClassInfo clazz : classes) {
//                // class references only for now and must be static
//                FieldInfoList fields = clazz.getFieldInfo()
//                        .filter(f -> f.getTypeDescriptor() instanceof ClassRefTypeSignature && f.isStatic());
//                for(FieldInfo field : fields) {
//                    ClassRefTypeSignature fieldDesc = (ClassRefTypeSignature) field.getTypeDescriptor();
//                    //System.out.println("field --> " + fieldDesc.getFullyQualifiedClassName());
//                    if(StringUtils.equals(t.getName(), fieldDesc.getFullyQualifiedClassName())) {
//                        //System.out.println("Found field in --> " + clazz.getName());
//
//                        try {
//                            Class<?> ownerClass = clazz.loadClass();
//                            Field fieldMember = ownerClass.getDeclaredField(field.getName());
//                            matches.add(fieldMember);
//                        } catch (Throwable e) {
//                            //
//                        }
//
//                    }
//                }
//            }
//
//            return matches;
//        } catch(Throwable e) {
//
//        }
//        return null;
//    }
}
