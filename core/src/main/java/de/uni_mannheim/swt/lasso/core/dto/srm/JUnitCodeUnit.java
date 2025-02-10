package de.uni_mannheim.swt.lasso.core.dto.srm;

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;

/**
 *
 * @author Marcus Kessel
 */
public class JUnitCodeUnit extends Sheet {

    private final CodeUnit codeUnit;

    private String testPrefix = "";

    public JUnitCodeUnit(CodeUnit codeUnit, String interfaceSpecification) {
        super(codeUnit.getName() + "()", codeUnit.getContent(), interfaceSpecification);
        this.codeUnit = codeUnit;
    }

    public CodeUnit getCodeUnit() {
        return codeUnit;
    }

    public String getTestPrefix() {
        return testPrefix;
    }

    public void setTestPrefix(String testPrefix) {
        this.testPrefix = testPrefix;
    }
}
