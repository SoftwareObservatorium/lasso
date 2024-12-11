package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.dto;

/**
 *
 * @author Marcus Kessel
 */
public class SheetInvocationDto {

    private String name;
    private String invocation;

    public SheetInvocationDto(String name, String invocation) {
        this.name = name;
        this.invocation = invocation;
    }

    public String getInvocation() {
        return invocation;
    }

    public String getName() {
        return name;
    }
}
