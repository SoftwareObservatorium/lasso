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
package de.uni_mannheim.swt.lasso.engine;

import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;

import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.corpus.Datasource;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Misc. utilities.
 *
 * @author Marcus Kessel
 */
public final class LassoUtils {

    private static final Logger LOG = LoggerFactory
            .getLogger(LassoUtils.class);

//    /**
//     * Attempts to determine reference implementation from classic TDS and its supplied test class source
//     * by looking at the first TestFilter defined.
//     *
//     * @param context
//     * @param actionConfiguration
//     * @return
//     * @throws IOException
//     */
//    public static CodeUnit generateReferenceImplementation(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
//        ExecutionPlan executionPlan = context.getExecutionPlan();
//        ActionNode testFilter = executionPlan.findFirstAction(ExecutionPlan.typeFilter(TestFilter.class));
//
//        // fetch ALL previous implementations
//        Executables candidateExecutables = context.getLassoOperations()
//                .getExecutables(context.getExecutionId(), actionConfiguration.getAbstraction().getName(), testFilter.getName());
//
//        Executable refExec = candidateExecutables.getExecutables().get(0);
////        CompilationUnit refAdapter = refExec.getCandidate().getTestAdapter().getAdapter();
////        CompilationUnit testUnit = refExec.getCandidate().getTestAdapter().getTests().get(0);
//
//        ActionNode select = executionPlan.findFirstAction(ExecutionPlan.typeFilter(Select.class));
//
//        // XXX BUG #327 (correct action name set?)
//        QueryReport queryReport = context.getReportOperations().getFirst(context.getExecutionId(), ReportKey.of(select.getName(), actionConfiguration.getAbstraction().getName(), QueryReport.UNDEFINED, QueryReport.UNDEFINED, -1), QueryReport.class);
//
//        CandidateQuery candidateQuery = TestAdaptationManager.createJavaParserQueryBuilder(null).build(queryReport.getQuery(), null, new SearchOptions());
//
//        CodeUnit refImpl = new CodeUnit();
//        refImpl.setImplementationUnit(CodeUnit.CodeUnitType.METHOD);
//        refImpl.setName(candidateQuery.getTestSubjectName());
//        refImpl.setPackagename(TestAdaptationManager.ADAPTER_TEST_PKG); // this is ok
//
//        List<String> methods = candidateQuery.getMethods().stream().map(s -> StringUtils.substringBefore(s, "(")).collect(Collectors.toList());
//
//        String classFq = StringUtils.substringBefore(refExec.getCode().getBytecodeName(), ".");
//
//        refImpl.setMethodBytecodeNames(Arrays.asList(String.format("%s.%s(%s", classFq, methods.get(0), StringUtils.substringAfter(refExec.getCode().getBytecodeName(), "("))));
//
//        refImpl.setBytecodeName(refImpl.getMethodBytecodeNames().get(0));
//
//        refImpl.setMethodNames(methods);
//
//        return refImpl;
//    }

    /**
     * Resolve data source
     *
     * <pre>
     *     1. try provided one
     *     2. try the default one in script
     *     3. try the first one defined in the executable corpus configuration.
     * </pre>
     *
     * @param context
     * @param dataSourceGiven
     * @return
     */
    public static String resolveDataSource(LSLExecutionContext context, String dataSourceGiven) {
        if(StringUtils.isNotBlank(dataSourceGiven)) {
            return dataSourceGiven;
        }

        // default data source defined in script
        if(CollectionUtils.isNotEmpty(context.getLassoContext().getDataSources())) {
            return context.getLassoContext().getDataSources().get(0);
        }

        Datasource ds = context.getConfiguration().getExecutableCorpus().getDatasources().get(0);

        //
        return ds.getId();
    }

    public static Optional<Datasource> getDataSource(LSLExecutionContext context, String dataSourceId) {
        return context.getConfiguration().getExecutableCorpus().getDatasources().stream()
                .filter(ds -> StringUtils.equalsIgnoreCase(ds.getId(), dataSourceId)).findFirst();
    }

    /**
     * Find and remove duplicates (alternatively, throwing exception).
     *
     * @param abstraction
     * @param throwException
     */
    @Deprecated
    public static void findDuplicates(Abstraction abstraction, boolean throwException) {
        if(abstraction == null || CollectionUtils.isEmpty(abstraction.getImplementations())) {
            return;
        }

        Set<String> ids = new HashSet<>();
        Iterator<System> it = abstraction.getImplementations().iterator();
        while(it.hasNext()) {
            System impl = it.next();
            if(ids.contains(impl.getId())) {
                if(throwException) {
                    throw new IllegalStateException(String.format("Found duplicate implementation with id '%s'", impl.getId()));
                } else {
                    if(LOG.isWarnEnabled()) {
                        LOG.warn("Found duplicate implementation with id '{}'", impl.getId());
                    }
                }

                it.remove();
            } else {
                ids.add(impl.getId());
            }
        }
    }

    /**
     * <pre>
     *     8-4-4-4-12 --> 844412
     * </pre>
     *
     * @param uuid
     * @return
     */
    public static String compactUUID(String uuid) {
        return StringUtils.replace(uuid, "-", "");
    }

    /**
     * <pre>
     *     844412 --> 8-4-4-4-12
     * </pre>
     *
     * @param uuid
     * @return
     */
    public static String decompactUUID(String uuid) {
        return String.format("%s-%s-%s-%s-%s",
                StringUtils.substring(uuid, 0, 8),
                StringUtils.substring(uuid, 8, 12),
                StringUtils.substring(uuid, 12, 16),
                StringUtils.substring(uuid, 16, 20),
                StringUtils.substring(uuid, 20, uuid.length())
                );
    }

    @Deprecated
    public static File resolveJarFromMavenRepository(Workspace workspace, CodeUnit implementation) {
        //
        File repository = workspace.getMavenRepository();
        String[] parts = StringUtils.split(implementation.toUri(), ':');

        String relJarPath = String.format("%s/%s/%s/%s-%s.jar",
                StringUtils.replace(parts[0], ".", "/"),
                parts[1],//StringUtils.replace(parts[1], ".", "/"),
                parts[2],
                parts[1],//StringUtils.replace(parts[1], ".", "/"),
                parts[2]
        );

        return new File(repository, relJarPath);
    }

    public static String getReferenceImplementationFromAlternatives(Abstraction altAbstraction) {
        // find impl == abstraction
        return altAbstraction.getName();
    }

    /**
     * Check if {@link Abstraction#getName()} is valid {@link System#getId()}.
     *
     * @param abstraction
     * @return
     */
    public static boolean isValidReferenceImplementation(Abstraction abstraction) {
        try {
            UUID.fromString(abstraction.getName());
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static String getDataSource(Abstraction abstraction) {
        if(abstraction == null || CollectionUtils.isEmpty(abstraction.getImplementations())) {
            throw new IllegalArgumentException(String.format("Cannot determine datasource from abstraction '%s'", abstraction));
        }

        //
        return abstraction.getImplementations().get(0).getCode().getDataSource();
    }

    public static String bc2JavaSignature(String bytecodeSignature) {
        String className = StringUtils.replace(StringUtils.substringBeforeLast(bytecodeSignature, "."), "/", ".");
        String methodName = StringUtils.substringBetween(bytecodeSignature, ".", "(");
        String methodDescriptor = "(" + StringUtils.substringAfter(bytecodeSignature, "(");
        Type method = Type.getMethodType(methodDescriptor);

        StringBuilder sb = new StringBuilder();
        sb.append(className);
        sb.append("::");
        sb.append(methodName);
        sb.append("(");

        Class[] inputTypes = new Class[0];
        try {
            inputTypes = Arrays.stream(method.getArgumentTypes()).map(p -> {
                try {
                    return ClassUtils.getClass(p.getClassName());
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(e);
                }
            }).toArray(Class[]::new);

            sb.append(Arrays.stream(inputTypes).map(Class::getCanonicalName).collect(Collectors.joining(",")));
        } catch (Throwable e) {
            e.printStackTrace();
        }

        sb.append("):");

        try {
            Class clazz = ClassUtils.getClass(method.getReturnType().getClassName());

            sb.append(clazz.getCanonicalName());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }

        return sb.toString();
    }

    /**
     * Simple way to shorten fully qualified byte code names.
     *
     * @param desc
     * @return
     */
    public static String shortenByteCodeNames(String desc) {
        // "(I[BLcom/github/megatronking/stringfog/lib/Base64$1;)V"

        Pattern pattern = Pattern.compile("L([.,[^;]]*);");

        List<String> toReplace = new LinkedList<>();
        Matcher matcher = pattern.matcher(desc);
        while(matcher.find()) {
            toReplace.add(desc.substring(matcher.start(), matcher.end()));
        }

        String mod = desc;
        for(String to : toReplace) {
            String replacement = "L" + StringUtils.substringAfterLast(to, "/");
            mod = StringUtils.replace(mod, to, replacement);
        }

        return mod;
    }
}
