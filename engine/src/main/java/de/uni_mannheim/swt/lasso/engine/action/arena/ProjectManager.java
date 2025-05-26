package de.uni_mannheim.swt.lasso.engine.action.arena;

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.datasource.maven.build.Candidate;

import java.util.Map;

/**
 *
 * @author Marcus Kessel
 */
public interface ProjectManager {

    public interface ExecutableFilter {

        boolean accept(System executable);
    }

    public interface ProjectSettingsHandler {

        void onFillTemplate(CodeUnit implementation, Candidate candidate, Map<String, Object> valueMap);
    }
}
