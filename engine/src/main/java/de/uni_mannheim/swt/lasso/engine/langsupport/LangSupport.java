package de.uni_mannheim.swt.lasso.engine.langsupport;

import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.System;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 *
 * @author Marcus Kessel
 */
public class LangSupport {

    public static boolean isJava(ActionConfiguration actionConfiguration) {
        Optional<System> system = actionConfiguration.getAbstraction().getImplementations().stream().findAny();

        return system.isPresent() && system.get().getCode().isJava();
    }

    public static boolean isJava(String lang) {
        return StringUtils.equalsIgnoreCase(lang, CodeUnit.JAVA);
    }

    public static boolean isPython(ActionConfiguration actionConfiguration) {
        Optional<System> system = actionConfiguration.getAbstraction().getImplementations().stream().findAny();

        return system.isPresent() && system.get().getCode().isPython();
    }

    public static boolean isPython(String lang) {
        return StringUtils.equalsIgnoreCase(lang, CodeUnit.PYTHON);
    }
}
