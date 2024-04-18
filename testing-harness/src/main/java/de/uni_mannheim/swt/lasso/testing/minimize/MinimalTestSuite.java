package de.uni_mannheim.swt.lasso.testing.minimize;

import java.util.BitSet;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class MinimalTestSuite {

    private List<TestCase> suite;
    private BitSet coveredElements;
    private int totalElements;

    public List<TestCase> getSuite() {
        return suite;
    }

    public void setSuite(List<TestCase> suite) {
        this.suite = suite;
    }

    public BitSet getCoveredElements() {
        return coveredElements;
    }

    public void setCoveredElements(BitSet coveredElements) {
        this.coveredElements = coveredElements;
    }

    public int getTotalUncovered() {
        return totalElements - getTotalCovered();
    }

    public int getTotalCovered() {
        return coveredElements.cardinality();
    }

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }
}
