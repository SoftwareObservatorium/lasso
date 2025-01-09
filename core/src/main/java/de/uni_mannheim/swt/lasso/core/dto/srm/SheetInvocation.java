package de.uni_mannheim.swt.lasso.core.dto.srm;

/**
 *
 * @author Marcus Kessel
 */
public class SheetInvocation {

    private String name;
    private String invocation;

    public SheetInvocation(String name, String invocation) {
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
