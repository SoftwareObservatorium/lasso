package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.dto;

import org.apache.commons.collections.CollectionUtils;

import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class SheetDto {

    private String signature;
    private String body;
    private String interfaceSpecification;

    // invocations of the sheet in case it is parameterized
    private List<String> invocations;

    public SheetDto(String signature, String body, String interfaceSpecification) {
        this.signature = signature;
        this.body = body;
        this.interfaceSpecification = interfaceSpecification;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getInterfaceSpecification() {
        return interfaceSpecification;
    }

    public void setInterfaceSpecification(String interfaceSpecification) {
        this.interfaceSpecification = interfaceSpecification;
    }

    public List<String> getInvocations() {
        return invocations;
    }

    public void setInvocations(List<String> invocations) {
        this.invocations = invocations;
    }
}
