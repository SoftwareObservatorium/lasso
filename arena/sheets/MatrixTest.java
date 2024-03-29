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
    public void test0_12() throws Throwable {
        double[][] doubleArray0 = new double[4][5];
        double[] doubleArray1 = new double[4];
        doubleArray0[0] = doubleArray1;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, 0.0, 0.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_12() throws Throwable {
        double[][] doubleArray0 = new double[4][5];
        double[] doubleArray1 = new double[4];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[10];
        doubleArray2[0] = 1.0;
        doubleArray0[2] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_8() throws Throwable {
        double[][] doubleArray0 = new double[3][2];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertNull(doubleArray2);
    }

    @Test(timeout = 4000)
    public void test1_8() throws Throwable {
        double[][] doubleArray0 = new double[3][2];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[4];
        doubleArray2[0] = 1307.9703582751076;
        doubleArray0[1] = doubleArray2;
        doubleArray0[2] = doubleArray1;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { Double.NaN, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_2() throws Throwable {
        double[][] doubleArray0 = new double[1][7];
        double[] doubleArray1 = new double[1];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertNull(doubleArray2);
    }

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

    @Test(timeout = 4000)
    public void test0_9() throws Throwable {
        double[][] doubleArray0 = new double[4][3];
        double[] doubleArray1 = new double[4];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertNull(doubleArray2);
    }

    @Test(timeout = 4000)
    public void test1_9() throws Throwable {
        double[][] doubleArray0 = new double[4][3];
        double[] doubleArray1 = new double[4];
        doubleArray0[0] = doubleArray1;
        doubleArray0[1] = doubleArray1;
        double[] doubleArray2 = new double[7];
        doubleArray2[0] = 825.6251551530846;
        doubleArray0[2] = doubleArray2;
        doubleArray0[3] = doubleArray1;
        double[] doubleArray3 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { Double.NaN, Double.NaN, Double.NaN, Double.NaN }, doubleArray1, 0.01);
        assertNotNull(doubleArray3);
    }

    @Test(timeout = 4000)
    public void test0_24() throws Throwable {
        double[][] doubleArray0 = new double[7][9];
        double[] doubleArray1 = new double[7];
        doubleArray0[0] = doubleArray1;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_24() throws Throwable {
        double[][] doubleArray0 = new double[7][9];
        double[] doubleArray1 = new double[7];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[7];
        doubleArray2[0] = 1.0;
        doubleArray0[1] = doubleArray2;
        double[] doubleArray3 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN }, doubleArray1, 0.01);
        assertNotNull(doubleArray3);
    }

    @Test(timeout = 4000)
    public void test0_21() throws Throwable {
        double[][] doubleArray0 = new double[4][4];
        double[] doubleArray1 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray0[1]);
        assertNull(doubleArray1);
    }

    @Test(timeout = 4000)
    public void test1_21() throws Throwable {
        double[][] doubleArray0 = new double[2][3];
        double[] doubleArray1 = new double[2];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[2];
        doubleArray2[0] = 1419.1388281;
        doubleArray0[1] = doubleArray2;
        double[] doubleArray3 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN }, doubleArray1, 0.01);
        assertNotNull(doubleArray3);
    }

    @Test(timeout = 4000)
    public void test0_22() throws Throwable {
        double[][] doubleArray0 = new double[3][7];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, 0.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_22() throws Throwable {
        double[][] doubleArray0 = new double[3][7];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[4];
        doubleArray2[0] = (-287.123781302);
        doubleArray0[1] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_1() throws Throwable {
        double[][] doubleArray0 = new double[2][1];
        double[] doubleArray1 = new double[2];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertNull(doubleArray2);
    }

    @Test(timeout = 4000)
    public void test1_1() throws Throwable {
        double[][] doubleArray0 = new double[2][1];
        double[] doubleArray1 = new double[2];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[6];
        doubleArray2[0] = 784.6019203861166;
        doubleArray0[1] = doubleArray2;
        double[] doubleArray3 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertNotNull(doubleArray3);
        assertArrayEquals(new double[] { 0.0, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_18() throws Throwable {
        double[][] doubleArray0 = new double[2][6];
        double[] doubleArray1 = new double[2];
        doubleArray0[0] = doubleArray1;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_18() throws Throwable {
        double[][] doubleArray0 = new double[2][6];
        double[] doubleArray1 = new double[2];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[8];
        doubleArray2[0] = (-201.959736563);
        doubleArray0[1] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_16() throws Throwable {
        double[][] doubleArray0 = new double[2][6];
        double[] doubleArray1 = new double[2];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[4];
        doubleArray2[0] = 1338.193989;
        doubleArray0[1] = doubleArray2;
        double[] doubleArray3 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN }, doubleArray1, 0.01);
        assertNotNull(doubleArray3);
    }

    @Test(timeout = 4000)
    public void test1_16() throws Throwable {
        double[][] doubleArray0 = new double[8][8];
        double[] doubleArray1 = new double[14];
        doubleArray1[0] = (-1227.34);
        doubleArray0[2] = doubleArray1;
        double[] doubleArray2 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray0[1]);
        assertNull(doubleArray2);
    }

    @Test(timeout = 4000)
    public void test0_25() throws Throwable {
        double[][] doubleArray0 = new double[4][7];
        double[] doubleArray1 = new double[4];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[6];
        doubleArray2[0] = 1368.0;
        doubleArray0[3] = doubleArray2;
        double[] doubleArray3 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertNotNull(doubleArray3);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_25() throws Throwable {
        double[][] doubleArray0 = new double[4][7];
        double[] doubleArray1 = new double[4];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[4];
        doubleArray2[0] = 1969.06669;
        doubleArray0[1] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray2);
        assertArrayEquals(new double[] { 0.0, 0.0, 0.0, 0.0 }, doubleArray2, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_15() throws Throwable {
        double[] doubleArray0 = new double[8];
        doubleArray0[0] = (-3000.3718);
        double[][] doubleArray1 = new double[8][6];
        doubleArray1[0] = doubleArray0;
        doubleArray1[1] = doubleArray0;
        doubleArray1[2] = doubleArray0;
        doubleArray1[3] = doubleArray0;
        doubleArray1[4] = doubleArray0;
        doubleArray1[5] = doubleArray0;
        doubleArray1[6] = doubleArray0;
        doubleArray1[7] = doubleArray0;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray1, doubleArray0);
        assertArrayEquals(new double[] { Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN }, doubleArray0, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_15() throws Throwable {
        double[][] doubleArray0 = new double[2][5];
        double[] doubleArray1 = new double[2];
        doubleArray1[0] = 473.0;
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertNull(doubleArray2);
        assertArrayEquals(new double[] { 473.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_4() throws Throwable {
        double[][] doubleArray0 = new double[3][7];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, 0.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_4() throws Throwable {
        double[][] doubleArray0 = new double[3][7];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[6];
        doubleArray2[0] = 1516.6251508308887;
        doubleArray0[1] = doubleArray2;
        double[] doubleArray3 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN }, doubleArray1, 0.01);
        assertNotNull(doubleArray3);
    }

    @Test(timeout = 4000)
    public void test0_0() throws Throwable {
        double[][] doubleArray0 = new double[2][9];
        double[] doubleArray1 = new double[2];
        doubleArray0[0] = doubleArray1;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_0() throws Throwable {
        double[][] doubleArray0 = new double[2][9];
        double[] doubleArray1 = new double[2];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[4];
        doubleArray2[0] = (-1261.1090909804718);
        doubleArray0[1] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_10() throws Throwable {
        double[][] doubleArray0 = new double[2][7];
        double[] doubleArray1 = new double[2];
        doubleArray0[0] = doubleArray1;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_10() throws Throwable {
        double[][] doubleArray0 = new double[2][7];
        double[] doubleArray1 = new double[2];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[9];
        doubleArray2[0] = 13.644593977;
        doubleArray0[1] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_26() throws Throwable {
        double[][] doubleArray0 = new double[4][9];
        double[] doubleArray1 = new double[4];
        doubleArray0[0] = doubleArray1;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, 0.0, 0.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_26() throws Throwable {
        double[][] doubleArray0 = new double[4][9];
        double[] doubleArray1 = new double[4];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[5];
        doubleArray2[0] = 1.0;
        doubleArray0[3] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test2_26() throws Throwable {
        double[][] doubleArray0 = new double[4][9];
        double[] doubleArray1 = new double[4];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[7];
        doubleArray2[0] = 1586.731550717;
        doubleArray0[1] = doubleArray2;
        double[] doubleArray3 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertNotNull(doubleArray3);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_13() throws Throwable {
        double[][] doubleArray0 = new double[5][1];
        double[] doubleArray1 = new double[5];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertNull(doubleArray2);
    }

    @Test(timeout = 4000)
    public void test1_13() throws Throwable {
        double[][] doubleArray0 = new double[5][1];
        double[] doubleArray1 = new double[5];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[8];
        doubleArray2[0] = (-2554.9274687369);
        doubleArray0[1] = doubleArray2;
        doubleArray0[2] = doubleArray1;
        doubleArray0[3] = doubleArray1;
        doubleArray0[4] = doubleArray1;
        double[] doubleArray3 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertNotNull(doubleArray3);
        assertArrayEquals(new double[] { Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test2_13() throws Throwable {
        double[][] doubleArray0 = new double[2][9];
        double[] doubleArray1 = new double[2];
        doubleArray1[0] = 1.0;
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertNull(doubleArray2);
        assertArrayEquals(new double[] { 1.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_19() throws Throwable {
        double[][] doubleArray0 = new double[3][8];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, 0.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_19() throws Throwable {
        double[][] doubleArray0 = new double[3][8];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[9];
        doubleArray2[0] = 2018.7;
        doubleArray0[1] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_17() throws Throwable {
        double[][] doubleArray0 = new double[2][5];
        double[] doubleArray1 = new double[2];
        doubleArray0[0] = doubleArray1;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_17() throws Throwable {
        double[][] doubleArray0 = new double[2][5];
        double[] doubleArray1 = new double[2];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[8];
        doubleArray2[0] = 4112.301182030585;
        doubleArray0[1] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_23() throws Throwable {
        double[][] doubleArray0 = new double[3][9];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray0[0]);
        assertNull(doubleArray2);
    }

    @Test(timeout = 4000)
    public void test1_23() throws Throwable {
        double[][] doubleArray0 = new double[3][9];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[5];
        doubleArray2[0] = (-560.809516844938);
        doubleArray0[2] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test2_23() throws Throwable {
        double[][] doubleArray0 = new double[3][9];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[3];
        doubleArray2[0] = 599.626961;
        doubleArray0[1] = doubleArray2;
        double[] doubleArray3 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN }, doubleArray1, 0.01);
        assertNotNull(doubleArray3);
    }

    @Test(timeout = 4000)
    public void test0_27() throws Throwable {
        double[][] doubleArray0 = new double[4][9];
        double[] doubleArray1 = new double[4];
        doubleArray0[0] = doubleArray1;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, 0.0, 0.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_27() throws Throwable {
        double[][] doubleArray0 = new double[4][9];
        double[] doubleArray1 = new double[4];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[6];
        doubleArray2[0] = (-1988.250621286109);
        doubleArray0[2] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_6() throws Throwable {
        double[][] doubleArray0 = new double[3][1];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertNull(doubleArray2);
    }

    @Test(timeout = 4000)
    public void test1_6() throws Throwable {
        double[][] doubleArray0 = new double[3][1];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[7];
        doubleArray2[0] = (-2452.72432);
        doubleArray0[1] = doubleArray2;
        doubleArray0[2] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_7() throws Throwable {
        double[][] doubleArray0 = new double[4][0];
        double[] doubleArray1 = new double[4];
        doubleArray0[0] = doubleArray1;
        doubleArray0[1] = doubleArray1;
        doubleArray0[2] = doubleArray1;
        double[] doubleArray2 = new double[8];
        doubleArray2[0] = 1.0;
        doubleArray0[3] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { Double.NaN, Double.NaN, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_7() throws Throwable {
        double[][] doubleArray0 = new double[4][4];
        double[] doubleArray1 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray0[0]);
        assertNull(doubleArray1);
    }

    @Test(timeout = 4000)
    public void test0_14() throws Throwable {
        double[][] doubleArray0 = new double[3][8];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, 0.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_14() throws Throwable {
        double[][] doubleArray0 = new double[3][8];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[4];
        doubleArray2[0] = 1228.25;
        doubleArray0[1] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_11() throws Throwable {
        double[][] doubleArray0 = new double[4][1];
        double[] doubleArray1 = new double[4];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertNull(doubleArray2);
    }

    @Test(timeout = 4000)
    public void test1_11() throws Throwable {
        double[][] doubleArray0 = new double[4][1];
        double[] doubleArray1 = new double[4];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[15];
        doubleArray2[0] = (-2.203848634911945);
        doubleArray0[1] = doubleArray2;
        doubleArray0[2] = doubleArray2;
        doubleArray0[3] = doubleArray1;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { Double.NaN, Double.NaN, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_28() throws Throwable {
        double[][] doubleArray0 = new double[2][2];
        double[] doubleArray1 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray0[0]);
        assertNull(doubleArray1);
    }

    @Test(timeout = 4000)
    public void test1_28() throws Throwable {
        double[][] doubleArray0 = new double[2][2];
        double[] doubleArray1 = new double[2];
        doubleArray1[0] = 1.0;
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[2];
        doubleArray2[0] = (-1108.5152);
        doubleArray0[1] = doubleArray2;
        double[] doubleArray3 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray2);
        assertArrayEquals(new double[] { -0.0, 1.0 }, doubleArray3, 0.01);
        assertNotNull(doubleArray3);
        assertArrayEquals(new double[] { 0.0, 1.0 }, doubleArray2, 0.01);
    }

    @Test(timeout = 4000)
    public void test2_28() throws Throwable {
        double[][] doubleArray0 = new double[3][9];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[46];
        doubleArray2[0] = (-3111.950780320551);
        doubleArray0[1] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_3() throws Throwable {
        double[][] doubleArray0 = new double[2][4];
        double[] doubleArray1 = new double[2];
        doubleArray0[0] = doubleArray1;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_3() throws Throwable {
        double[][] doubleArray0 = new double[2][4];
        double[] doubleArray1 = new double[2];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[4];
        doubleArray2[0] = 2481.391999374178;
        doubleArray0[1] = doubleArray2;
        double[] doubleArray3 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN }, doubleArray1, 0.01);
        assertNotNull(doubleArray3);
    }

    @Test(timeout = 4000)
    public void test2_3() throws Throwable {
        double[][] doubleArray0 = new double[4][7];
        double[] doubleArray1 = new double[4];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[7];
        doubleArray2[0] = (-58.347);
        doubleArray0[2] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_29() throws Throwable {
        double[][] doubleArray0 = new double[2][7];
        double[] doubleArray1 = new double[2];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[2];
        doubleArray2[0] = 1100.5278289884752;
        doubleArray0[1] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_29() throws Throwable {
        double[][] doubleArray0 = new double[4][6];
        double[] doubleArray1 = new double[4];
        doubleArray1[0] = (-1.0);
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[6];
        doubleArray2[1] = 1166.108;
        doubleArray0[2] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { (-1.0), 0.0, 0.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_20() throws Throwable {
        double[][] doubleArray0 = new double[3][9];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, 0.0, 0.0 }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test1_20() throws Throwable {
        double[][] doubleArray0 = new double[3][9];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = new double[3];
        doubleArray2[0] = (-3315.8295489);
        doubleArray0[1] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }

    @Test(timeout = 4000)
    public void test0_5() throws Throwable {
        double[][] doubleArray0 = new double[3][1];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        double[] doubleArray2 = com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertNull(doubleArray2);
    }

    @Test(timeout = 4000)
    public void test1_5() throws Throwable {
        double[][] doubleArray0 = new double[3][1];
        double[] doubleArray1 = new double[3];
        doubleArray0[0] = doubleArray1;
        doubleArray0[1] = doubleArray1;
        double[] doubleArray2 = new double[12];
        doubleArray2[0] = 13.587970171674613;
        doubleArray0[2] = doubleArray2;
        com.vividsolutions.jts.math.Matrix.solve(doubleArray0, doubleArray1);
        assertArrayEquals(new double[] { Double.NaN, Double.NaN, Double.NaN }, doubleArray1, 0.01);
    }
}
