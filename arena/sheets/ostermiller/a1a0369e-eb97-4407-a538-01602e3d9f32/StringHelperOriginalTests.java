package com.Ostermiller.util;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StringHelperOriginalTests {

    @BeforeClass
    public static void setupAll() {
    }

    @AfterClass
    public static void teardownAll() {
    }

    @Before
    public void setup() {
    }

    @After
    public void teardown() {
    }

    @Test
    public void bc1_a1a0369eeb974407a53801602e3d9f32() throws Throwable {
        com.Ostermiller.util.StringHelper stringHelper0 = new com.Ostermiller.util.StringHelper();
        java.lang.String str4 = com.Ostermiller.util.StringHelper.replace("1%2%3%4%5!", "%", "-");
        org.junit.Assert.assertEquals("'" + str4 + "' != '" + "1-2-3-4-5!" + "'", str4, "1-2-3-4-5!");
    }

    @Test
    public void bc2_a1a0369eeb974407a53801602e3d9f32() throws Throwable {
        com.Ostermiller.util.StringHelper stringHelper0 = new com.Ostermiller.util.StringHelper();
        java.lang.String str4 = com.Ostermiller.util.StringHelper.replace("1%2%3%4%5!", "%", "");
        org.junit.Assert.assertEquals("'" + str4 + "' != '" + "12345!" + "'", str4, "12345!");
    }

    @Test
    public void bc3_a1a0369eeb974407a53801602e3d9f32() throws Throwable {
        com.Ostermiller.util.StringHelper stringHelper0 = new com.Ostermiller.util.StringHelper();
        java.lang.String str3 = null;
        java.lang.String str4 = com.Ostermiller.util.StringHelper.replace("1%2%3%4%5!", "%", str3);
        org.junit.Assert.assertEquals("'" + str4 + "' != '" + "12345!" + "'", str4, "12345!");
    }

    @Test
    public void bc4_a1a0369eeb974407a53801602e3d9f32() throws Throwable {
        com.Ostermiller.util.StringHelper stringHelper0 = new com.Ostermiller.util.StringHelper();
        java.lang.String str4 = com.Ostermiller.util.StringHelper.replace("1%2%3%4%5!", "", "-");
        org.junit.Assert.assertEquals("'" + str4 + "' != '" + "1%2%3%4%5!" + "'", str4, "1%2%3%4%5!");
    }

    @Test
    public void bc5_a1a0369eeb974407a53801602e3d9f32() throws Throwable {
        com.Ostermiller.util.StringHelper stringHelper0 = new com.Ostermiller.util.StringHelper();
        java.lang.String str2 = null;
        java.lang.String str4 = com.Ostermiller.util.StringHelper.replace("1%2%3%4%5!", str2, "-");
        org.junit.Assert.assertEquals("'" + str4 + "' != '" + "1%2%3%4%5!" + "'", str4, "1%2%3%4%5!");
    }

    @Test
    public void bc6_a1a0369eeb974407a53801602e3d9f32() throws Throwable {
        com.Ostermiller.util.StringHelper stringHelper0 = new com.Ostermiller.util.StringHelper();
        java.lang.String str4 = com.Ostermiller.util.StringHelper.replace("1%2%3%4%5!", "\247", "-");
        org.junit.Assert.assertEquals("'" + str4 + "' != '" + "1%2%3%4%5!" + "'", str4, "1%2%3%4%5!");
    }

    @Test
    public void bc7_a1a0369eeb974407a53801602e3d9f32() throws Throwable {
        com.Ostermiller.util.StringHelper stringHelper0 = new com.Ostermiller.util.StringHelper();
        java.lang.String str4 = com.Ostermiller.util.StringHelper.replace("", "%", "-");
        org.junit.Assert.assertEquals("'" + str4 + "' != '" + "" + "'", str4, "");
    }

    @Test
    public void bc8_a1a0369eeb974407a53801602e3d9f32() throws Throwable {
        com.Ostermiller.util.StringHelper stringHelper0 = new com.Ostermiller.util.StringHelper();
        java.lang.String str1 = null;
        // The following exception was thrown during execution in test generation
        try {
            java.lang.String str4 = com.Ostermiller.util.StringHelper.replace(str1, "%", "-");
            org.junit.Assert.fail("Expected exception of type java.lang.NullPointerException; message: null");
        } catch (java.lang.NullPointerException e) {
            // Expected exception.
        }
    }
}
