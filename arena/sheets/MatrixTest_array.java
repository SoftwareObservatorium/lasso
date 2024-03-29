package com.vividsolutions.jts.math;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;

/**
 * Modified and generated by XXXX on Fri Mar 05 19:01:00 UTC 2021
 */
public class MatrixTest {

    @Test(timeout = 4000)
    public void test1_2() throws Throwable {
        double[][] doubleArray0 = new double[5][5];
        double[] doubleArray1 = new double[5];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[6];
        doubleArray2[0] = 2426.846650691062;
        doubleArray0[2] = doubleArray2;
        double[] doubleArray3 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertNotNull(doubleArray3);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }
}
