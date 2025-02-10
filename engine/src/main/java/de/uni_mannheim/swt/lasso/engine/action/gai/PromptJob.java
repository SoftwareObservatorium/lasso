package de.uni_mannheim.swt.lasso.engine.action.gai;

import de.uni_mannheim.swt.lasso.gai.openai.Prompt;

/**
 *
 * @author Marcus Kessel
 */
public class PromptJob {

    private Prompt prompt;
    private int sampleId;

    public PromptJob(Prompt prompt, int sampleId) {
        this.prompt = prompt;
        this.sampleId = sampleId;
    }

    public Prompt getPrompt() {
        return prompt;
    }

    public int getSampleId() {
        return sampleId;
    }
}
