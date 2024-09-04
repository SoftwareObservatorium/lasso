package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util;

import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.search.LQLMethodSignature;
import de.uni_mannheim.swt.lasso.core.model.Interface;
import de.uni_mannheim.swt.lasso.core.model.MethodSignature;
import de.uni_mannheim.swt.lasso.lql.parser.LQL;
import de.uni_mannheim.swt.lasso.lql.parser.LQLParseResult;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LQL parsing facilities.
 *
 * @author Marcus Kessel
 */
public class LQLUtils {

    public static Map<String, InterfaceSpecification> lqlToMap(String lql) throws IOException {
        List<InterfaceSpecification> interfaceSpecifications = lqlToList(lql);

        return interfaceSpecifications.stream()
                .collect(Collectors.toMap(InterfaceSpecification::getClassName, v -> v));
    }

    public static List<InterfaceSpecification> lqlToList(String lql) throws IOException {
        // LQL
        LQLParseResult lqlParseResult = parseLQL(lql);

        List<InterfaceSpecification> parseResults = new LinkedList<>();
        List<de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature> mSignatures = new LinkedList<>();
        List<de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature> cSignatures = new LinkedList<>();

        Interface system = lqlParseResult.getInterfaceSpecification();
        String className = system.getName();

        // construct ordered signatures for searching
        List<MethodSignature> methods = system.getMethods();
        if (methods != null && methods.size() > 0) {
            for (int i = 0; i < methods.size(); i++) {
                MethodSignature method = methods.get(i);
                if (!method.isConstructor()) {
                    mSignatures.add(new de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature("public", method.getName(),
                            method.getInputs(), method.getOutputs().get(0)));
                } else {
                    cSignatures.add(new de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature("public", "<init>",
                            method.getInputs(), "void"));
                }
            }
        }

        if (CollectionUtils.isEmpty(cSignatures)) {
            // add default one
            cSignatures.add(new de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature("public", "<init>",
                    Collections.emptyList(), "void"));
        }

        InterfaceSpecification parseResult = new InterfaceSpecification();
        parseResult.setClassName(className);
        parseResult.setConstructors(cSignatures.stream()
                .map(c -> {
                    LQLMethodSignature lm = new LQLMethodSignature(parseResult, c);
                    //lm.setParent(parseResult);
                    return lm;
                })
                .collect(Collectors.toList()));
        parseResult.setMethods(mSignatures.stream()
                .map(c -> {
                    LQLMethodSignature lm = new LQLMethodSignature(parseResult, c);
                    //lm.setParent(parseResult);
                    return lm;
                })
                .collect(Collectors.toList()));

        parseResults.add(parseResult);
        //}

        return parseResults;
    }

    public static LQLParseResult parseLQL(String lql) throws IOException {
        //

        LQLParseResult parseResult = LQL.parse(lql);
        if (parseResult != null) {
            return parseResult;
        }

        throw new IOException("unsupported LQL query " + lql);
    }
}
