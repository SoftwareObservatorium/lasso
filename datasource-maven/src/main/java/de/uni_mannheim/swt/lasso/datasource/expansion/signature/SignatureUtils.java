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
package de.uni_mannheim.swt.lasso.datasource.expansion.signature;

import de.uni_mannheim.swt.lasso.index.match.SignatureMatch;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.iterators.PermutationIterator;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class SignatureUtils {

    public static List<Signature> createPermutations(Signature signature) {
        PermutationIterator<String> permutationIterator = new PermutationIterator<>(signature.getInputTypes());

        List<Signature> signatures = new LinkedList<>();
        permutationIterator.forEachRemaining(l -> {
            Signature sig = new Signature();
            sig.setName(signature.getName());
            sig.setInputTypes(l);
            sig.setReturnType(signature.getReturnType());
            sig.setTypeDescriptor(signature.getTypeDescriptor());

            signatures.add(sig);
        });

        return signatures;
    }

    public static Clazz create(CodeUnit mavenImplementation) {
        Clazz clazz = new Clazz();
        clazz.setName(mavenImplementation.getName());

        List<String> methodSignatures = mavenImplementation.getMethodSignatureParamsOrderedKeywordsFq();
        if(CollectionUtils.isEmpty(methodSignatures)) {
            return clazz;
        }

        List<String> methodNames = mavenImplementation.getMethodNames();

        List<String> bytecodeNames = mavenImplementation.getMethodBytecodeNames();

        for(int i = 0; i < methodSignatures.size(); i++) {
            String methodSignature = methodSignatures.get(i);

            SignatureMatch signatureMatch = new SignatureMatch(methodSignature);

            Signature signature = new Signature();
            // important!: use original method name (not lower case)
            signature.setName(methodNames.get(i));
            signature.setReturnType(signatureMatch.getReturnType());
            signature.setInputTypes(signatureMatch.getParameterTypes());

            // extract descriptor
            if(CollectionUtils.isNotEmpty(bytecodeNames) && i < bytecodeNames.size()) {
                String bytecodeName = bytecodeNames.get(i);
                int index = StringUtils.indexOf(bytecodeName, "(");
                if(index > -1) {
                    signature.setTypeDescriptor(bytecodeName.substring(index));
                }
            }

            boolean init = false;
            if(StringUtils.equals(signature.getName(), "<init>")) {
                // set to clazz name for constructor
                signature.setName(clazz.getName());
                init = true;
            } else if(StringUtils.equals(signature.getName(), "<clinit>")) {
                // FIXME handle static inits?
                continue;
            }

            if(init) {
                clazz.getConstructors().add(signature);
            } else {
                clazz.getMethods().add(signature);
            }
        }

        return clazz;
    }
}
