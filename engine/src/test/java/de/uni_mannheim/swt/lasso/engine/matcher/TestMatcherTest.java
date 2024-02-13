/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.engine.matcher;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Marcus Kessel
 */
public class TestMatcherTest {

    @Test
    public void testMatch() {
        TestMatcher matcher = new TestMatcher();

        assertTrue(matcher.match("*", "SomeTest.java"));
        assertFalse(matcher.match("test", "SomeTest.java"));

        assertTrue(matcher.match("**/*_0_Test*", "JSGFRuleAlternatives_0_Test.java"));

        assertTrue(matcher.match("/**/*_0_Test*", "/some/complex/path/Attr_HuaweiInputAverageRate_0_Test.java"));

        // match any
        assertTrue(matcher.match("*", "blabla"));

        assertTrue(matcher.match("*", "/volumes/samsung_ssd_1/lasso-arena/lasso-work/7627863e-01e3-4137-82f9-1d844aaad654/random/copy_621b31e8-5b9a-4ce1-80c4-48734a495938/06e28898-22d4-431e-85e4-b3eb6621157b/evosuite-tests/net/jradius/dictionary/vsa_huawei/Attr_HuaweiInputAverageRate_0_Test.java"));
        assertTrue(matcher.match("/**/*_0_Test*", "/volumes/samsung_ssd_1/lasso-arena/lasso-work/7627863e-01e3-4137-82f9-1d844aaad654/random/copy_621b31e8-5b9a-4ce1-80c4-48734a495938/06e28898-22d4-431e-85e4-b3eb6621157b/evosuite-tests/net/jradius/dictionary/vsa_huawei/Attr_HuaweiInputAverageRate_0_Test.java"));
    }

    @Test
    public void testAnyMatch() {
        TestMatcher matcher = new TestMatcher();

        assertTrue(matcher.match("/**/*_0_Test*,/**/*AmplifiedTests*", "/some/complex/path/Attr_HuaweiInputAverageRate_0_Test.java"));
        assertTrue(matcher.match("/**/*_0_Test*,/**/*AmplifiedTests*", "/some/complex/path/MyAmplifiedTests.java"));
        assertFalse(matcher.match("/**/*_0_Test*,/**/*AmplifiedTests*", "/some/complex/path/OtherTests.java"));
    }

    @Disabled
    @Test
    public void testPath() {
        TestMatcher matcher = new TestMatcher();

        List<File> files = matcher.findMatches("/**/src/test/java/**/*", new File("/home/marcus/lasso-work/54727d12-56a5-41e4-bcc6-bf0a2adfed2e/ca07ef4f-5576-4e8b-8328-6e7bd648ff03/arena_1b53f95a-f212-4e1e-a957-73f45f789b1e"));

        files.forEach(System.out::println);
    }
}
