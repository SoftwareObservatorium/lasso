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
package de.uni_mannheim.swt.lasso.lsl;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.util.DelegatingScript;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

/**
 * @author Marcus Kessel
 */
public class LSLRunner {

    ClassLoader cl = LSLRunner.class.getClassLoader();

    /**
     * @param scriptSource
     * @param logger
     * @see <a href="http://docs.groovy-lang.org/latest/html/api/groovy/util/DelegatingScript.html">Groovy DelegatingScript</a>
     */
    public LSLDelegatingScript runScript(String scriptSource, LSLLogger logger) {
        GroovyShell sh = createShell(cl, new Binding());
        DelegatingScript script = (DelegatingScript) sh.parse(scriptSource);

        // trick is that everything is delegated to this instance
        LSLDelegatingScript delegateScript = new LSLDelegatingScript();
        delegateScript.init(logger);
        script.setDelegate(delegateScript);

        // run script
        script.run();

        return delegateScript;
    }

    public Script parseScript(String scriptSource) {
        GroovyShell sh = createShell(cl, new Binding());

        Script script = sh.parse(scriptSource);

        return script;
    }

    private GroovyShell createShell(ClassLoader classLoader, Binding binding) {
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setScriptBaseClass(DelegatingScript.class.getName());

        // TODO enable import customizers
        //ImportCustomizer importCustomizer = loadImportCustomizer();
        //compilerConfiguration.addCompilationCustomizers(importCustomizer);

        GroovyShell shell = new GroovyShell(classLoader, binding, cc);

        return shell;
    }

    /**
     * Add custom imports + aliases
     *
     * @return
     *
     * @see <a href="http://docs.groovy-lang.org/docs/latest/html/documentation/core-domain-specific-languages.html#_import_customizer">Documentation</a>
     */
    private ImportCustomizer loadImportCustomizer() {
        // FIXME adjust default imports

        ImportCustomizer icz = new ImportCustomizer();
//        // "normal" import
//        icz.addImports("java.util.concurrent.atomic.AtomicInteger", "java.util.concurrent.ConcurrentHashMap");
//        // "aliases" import
//        icz.addImport("CHM", "java.util.concurrent.ConcurrentHashMap");
//        // "static" import
//        icz.addStaticImport("java.lang.Math", "PI"); // import static java.lang.Math.PI
//        // "aliased static" import
//        icz.addStaticImport("pi", "java.lang.Math", "PI"); // import static java.lang.Math.PI as pi
//        // "star" import
//        icz.addStarImports("java.util.concurrent"); // import java.util.concurrent.*
//        // "static star" import
//        icz.addStaticStars("java.lang.Math"); // import static java.lang.Math.*

        return icz;
    }
}
