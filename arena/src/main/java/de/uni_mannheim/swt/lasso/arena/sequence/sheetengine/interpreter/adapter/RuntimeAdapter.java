package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME do we need this?
 *
 * @author Marcus Kessel
 */
public class RuntimeAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeAdapter.class);

    private AdaptedImplementation adaptedImplementation;

    public Object adapt(String methodName, String... args) {
        LOG.debug("method called '{}' with args '{}'", methodName, args);

        return null;
    }

    public AdaptedImplementation getAdaptedImplementation() {
        return adaptedImplementation;
    }

    public void setAdaptedImplementation(AdaptedImplementation adaptedImplementation) {
        this.adaptedImplementation = adaptedImplementation;
    }
}
