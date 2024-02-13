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
package de.uni_mannheim.swt.lasso.datasource.maven.lsl.experimental

class JSSSIAutoV2 {

    static Map<String, String> testClasses = [:]
    static {
            testClasses.put('564debc5-124a-439f-b9f2-6a247dfc1c20', '''
import org.junit.Test;
import com.microsoft.azure.storage.core.Base64;
import static org.junit.Assert.*;

public class Base64Test {
    
    @Test
    public void testDecode() {
        Base64 cut = new Base64();
        byte[] actual = cut.decode("dXNlcjpwYXNz");
        assertEquals("user:pass", new String(actual));
    }
    
    @Test
    public void testDecodePadding() {
        Base64 cut = new Base64();
        byte[] actual = cut.decode("SGVsbG8gV29ybGQ=");
        assertEquals("Hello World", new String(actual));
    }
}
''')
            testClasses.put('379e30b8-cb5f-45c4-93b9-d592450d2743', '''
import org.junit.Test;
import com.vividsolutions.jts.math.Matrix;
import static org.junit.Assert.*;

public class MatrixTest {
    
    @Test
    public void testSolve() {
        Matrix cut = new Matrix();
    
        double[][] a = new double[][]{
            { 0, 1,  1 },
            { 2, 4, -2 },
            { 0, 3, 15 }
        };
        double[] b = new double[]{ 4, 2, 36 };
        
        double[] actual = cut.solve(a, b);
        assertArrayEquals(new double[]{-1.0, 2.0, 2.0}, actual, 1e-9);
    }
}
''')
            testClasses.put('2ac3cdd5-8ea5-4a10-8ed2-a5a454d54570', '''
import java.lang.Exception;
import org.junit.Test;
import net.sf.jasperreports.engine.util.JRStringUtil;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.JRStringUtil\'
 * Method Signature = 'htmlEncode(java.lang.String):java.lang.String\'
 *
 * START Functionality Description
 *
 * \t 
 * \t 
 * END Functionality Description
 *
 * Doc ID: 2ac3cdd5-8ea5-4a10-8ed2-a5a454d54570
 * 
 */
public class JRStringUtilTest {

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.JRStringUtil\'
   * Method Signature = 'htmlEncode(java.lang.String):java.lang.String\'
   */
  //empty string
  @Test
  public void test_example0() throws Exception {
    JRStringUtil _cut = new JRStringUtil();
    String actual = _cut.htmlEncode(" ");
    assertEquals("&nbsp;", actual);
  }
  
  //'&\'
  @Test
  public void test_example1() throws Exception {
    JRStringUtil _cut = new JRStringUtil();
    String actual = _cut.htmlEncode("&");
    assertEquals("&amp;", actual);
  }
  
  //'>\'
  @Test
  public void test_example2() throws Exception {
    JRStringUtil _cut = new JRStringUtil();
    String actual = _cut.htmlEncode(">");
    assertEquals("&gt;", actual);
  }
  
  //'<\'
  @Test
  public void test_example3() throws Exception {
    JRStringUtil _cut = new JRStringUtil();
    String actual = _cut.htmlEncode("<");
    assertEquals("&lt;", actual);
  }
  
  //'\\\'
  @Test
  public void test_example4() throws Exception {
    JRStringUtil _cut = new JRStringUtil();
    String actual = _cut.htmlEncode("\\"");
    assertEquals("&quot;", actual);
  }
  
  //linebreak
  @Test
  public void test_example5() throws Exception {
    JRStringUtil _cut = new JRStringUtil();
    String actual = _cut.htmlEncode("\\n");
    assertEquals("<br/>", actual);
  }
  
  //special character within arbitrary text
  @Test
  public void test_example6() throws Exception {
    JRStringUtil _cut = new JRStringUtil();
    String actual = _cut.htmlEncode("foo \\n foo");
    assertEquals("foo <br/> foo", actual);
  }
  
  //string not containing any special character
  @Test
  public void test_example7() throws Exception {
    JRStringUtil _cut = new JRStringUtil();
    String actual = _cut.htmlEncode("foo");
    assertEquals("foo", actual);
  }
}
''')
            testClasses.put('0be199da-1339-4ffe-a151-0343e287c6eb', '''
import java.lang.Exception;
import org.junit.Test;
import org.weakref.jmx.internal.guava.primitives.UnsignedLongs;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.UnsignedLongs'
 * Method Signature = 'divide(long,long):long'
 *
 * START Functionality Description
 *
 *     Returns dividend / divisor, where the dividend and divisor are treated as unsigned 64-bit
 *     quantities.
 *    
 *     @param dividend the dividend (numerator)
 *     @param divisor the divisor (denominator)
 *     @throws ArithmeticException if divisor is 0
 *    
 * END Functionality Description
 *
 * Doc ID: 0be199da-1339-4ffe-a151-0343e287c6eb
 * 
 */
public class UnsignedLongsTest {
  // EXP: refactored into 1:1 mapping

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.UnsignedLongs\'
   * Method Signature = 'divide(long,long):long\'
   */
  @Test
  public void test_example1() throws Exception {
    // class under test
    UnsignedLongs _cut = new UnsignedLongs();
    
    //(A: a >b / B: a=b / C: a<b)
    //(1: a != 0 / 2: a = 0)
    //(X: b != 0 / Y: b= 0)
    
    //BASE CHOICE A1X
    
    long actual = _cut.divide(5L, 2L);
    
    assertEquals(2, actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.UnsignedLongs\'
   * Method Signature = 'divide(long,long):long\'
   */
  @Test
  public void test_example2() throws Exception {
    // class under test
    UnsignedLongs _cut = new UnsignedLongs();
    
    //(A: a >b / B: a=b / C: a<b)
    //(1: a != 0 / 2: a = 0)
    //(X: b != 0 / Y: b= 0)
    
    //B1X
    
    long actual2 = _cut.divide(5L, 5L);
    
    assertEquals(1, actual2);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.UnsignedLongs\'
   * Method Signature = 'divide(long,long):long\'
   */
  @Test
  public void test_example3() throws Exception {
    // class under test
    UnsignedLongs _cut = new UnsignedLongs();
    
    //(A: a >b / B: a=b / C: a<b)
    //(1: a != 0 / 2: a = 0)
    //(X: b != 0 / Y: b= 0)
    
    //C1X
    
    long actual3 = _cut.divide(5L, 6L);
    
    assertEquals(0, actual3);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.UnsignedLongs'
   * Method Signature = 'divide(long,long):long'
   */
  @Test
  public void test_example4() throws Exception {
    // class under test
    UnsignedLongs _cut = new UnsignedLongs();
    
    //(A: a >b / B: a=b / C: a<b)
    //(1: a != 0 / 2: a = 0)
    //(X: b != 0 / Y: b= 0)
    
    long actual4 = _cut.divide(0L, 2L);
    
    //A2X
    assertEquals(0, actual4);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.UnsignedLongs\'
   * Method Signature = 'divide(long,long):long\'
   */
  @Test
  public void test_example5() throws Exception {
    // class under test
    UnsignedLongs _cut = new UnsignedLongs();
    
    //(A: a >b / B: a=b / C: a<b)
    //(1: a != 0 / 2: a = 0)
    //(X: b != 0 / Y: b= 0)
       
    //A1Y
    
    try {
         long result = _cut.divide(5L, 0L);
         fail();
    } catch (Throwable e) {
    }
  }
}
''')
            testClasses.put('3f5330c5-1235-4cc7-9cde-8f8928935e61', '''

import java.lang.Exception;
import org.junit.Test;
import org.codehaus.plexus.util.PathTool;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.PathTool\'
 * Method Signature = 'getRelativeFilePath(java.lang.String,java.lang.String):java.lang.String\'
 *
 * START Functionality Description
 *
 *       This method can calculate the relative path between two pathes on a file system.
 *       <br/>
 *       <pre>
 *       PathTool.getRelativeFilePath( null, null )                                   = ""
 *       PathTool.getRelativeFilePath( null, "/usr/local/java/bin" )                  = ""
 *       PathTool.getRelativeFilePath( "/usr/local", null )                           = ""
 *       PathTool.getRelativeFilePath( "/usr/local", "/usr/local/java/bin" )          = "java/bin"
 *       PathTool.getRelativeFilePath( "/usr/local", "/usr/local/java/bin/" )         = "java/bin"
 *       PathTool.getRelativeFilePath( "/usr/local/java/bin", "/usr/local/" )         = "../.."
 *       PathTool.getRelativeFilePath( "/usr/local/", "/usr/local/java/bin/java.sh" ) = "java/bin/java.sh"
 *       PathTool.getRelativeFilePath( "/usr/local/java/bin/java.sh", "/usr/local/" ) = "../../.."
 *       PathTool.getRelativeFilePath( "/usr/local/", "/bin" )                        = "../../bin"
 *       PathTool.getRelativeFilePath( "/bin", "/usr/local/" )                        = "../usr/local"
 *       </pre>
 *       Note: On Windows based system, the <code>/</code> character should be replaced by <code>\\</code> character.
 *      
 *       @param oldPath
 *       @param newPath
 *       @return a relative file path from <code>oldPath</code>.
 *      
 * END Functionality Description
 *
 * Doc ID: 3f5330c5-1235-4cc7-9cde-8f8928935e61
 * 
 */
public class PathToolTest {
\t
\t// EXP: refactored into 1:1 mapping

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.PathTool\'
   * Method Signature = 'getRelativeFilePath(java.lang.String,java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example1() throws Exception {
    // class under test
\t  
\t  // A: a !=null B: a == null
\t  // 1: b !=null 2: b == null
\t  // X: path direction is straight forward Y: path direction is straight backward Z: path is both
\t  
\t  PathTool _cut = new PathTool();
\t    
\t    String[] expecteds = new String[]{\t"",
\t    \t\t\t\t\t\t"",
\t    \t\t\t\t\t\t"c/d",
\t    \t\t\t\t\t\t"../..",
\t    \t\t\t\t\t\t"../../../z"
\t    \t\t\t\t\t\t};
\t    
\t    //BASECHOICE A1X
\t    String actual = _cut.getRelativeFilePath("/a/b", "/a/b/c/d");
\t    
\t    assertEquals(expecteds[2], actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.PathTool\'
   * Method Signature = 'getRelativeFilePath(java.lang.String,java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example2() throws Exception {
    // class under test
\t  
\t  // A: a !=null B: a == null
\t  // 1: b !=null 2: b == null
\t  // X: path direction is straight forward Y: path direction is straight backward Z: path is both
\t  
\t  PathTool _cut = new PathTool();
\t    
\t    String[] expecteds = new String[]{\t"",
\t    \t\t\t\t\t\t"",
\t    \t\t\t\t\t\t"c/d",
\t    \t\t\t\t\t\t"../..",
\t    \t\t\t\t\t\t"../../../z"
\t    \t\t\t\t\t\t};
\t    
\t    //B1X
\t    String actual = _cut.getRelativeFilePath(null, "/a/b/c");
\t    
\t    assertEquals(expecteds[0], actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.PathTool\'
   * Method Signature = 'getRelativeFilePath(java.lang.String,java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example3() throws Exception {
    // class under test
\t  
\t  // A: a !=null B: a == null
\t  // 1: b !=null 2: b == null
\t  // X: path direction is straight forward Y: path direction is straight backward Z: path is both
\t  
\t  PathTool _cut = new PathTool();
\t    
\t    String[] expecteds = new String[]{\t"",
\t    \t\t\t\t\t\t"",
\t    \t\t\t\t\t\t"c/d",
\t    \t\t\t\t\t\t"../..",
\t    \t\t\t\t\t\t"../../../z"
\t    \t\t\t\t\t\t};
\t    
\t    //A2X
\t    String actual = _cut.getRelativeFilePath("/a/b", null);
\t    
\t    assertEquals(expecteds[1], actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.PathTool\'
   * Method Signature = 'getRelativeFilePath(java.lang.String,java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example4() throws Exception {
    // class under test
\t  
\t  // A: a !=null B: a == null
\t  // 1: b !=null 2: b == null
\t  // X: path direction is straight forward Y: path direction is straight backward Z: path is both
\t  
\t  PathTool _cut = new PathTool();
\t    
\t    String[] expecteds = new String[]{\t"",
\t    \t\t\t\t\t\t"",
\t    \t\t\t\t\t\t"c/d",
\t    \t\t\t\t\t\t"../..",
\t    \t\t\t\t\t\t"../../../z"
\t    \t\t\t\t\t\t};
\t    
\t    //A1Y
\t    String actual = _cut.getRelativeFilePath("/a/b/c/d", "/a/b");
\t    
\t    assertEquals(expecteds[3], actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.PathTool\'
   * Method Signature = 'getRelativeFilePath(java.lang.String,java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example5() throws Exception {
    // class under test
\t  
\t  // A: a !=null B: a == null
\t  // 1: b !=null 2: b == null
\t  // X: path direction is straight forward Y: path direction is straight backward Z: path is both
\t  
\t  PathTool _cut = new PathTool();
\t    
\t    String[] expecteds = new String[]{\t"",
\t    \t\t\t\t\t\t"",
\t    \t\t\t\t\t\t"c/d",
\t    \t\t\t\t\t\t"../..",
\t    \t\t\t\t\t\t"../../../z"
\t    \t\t\t\t\t\t};

\t    //A1Z
\t    String actual = _cut.getRelativeFilePath("a/b/c", "/z");
\t    
\t    assertEquals(expecteds[4], actual);
  }
}

''')
            testClasses.put('9cc424b6-be67-4b3e-a496-0bb003cb65d5', '''
import java.lang.Exception;
import org.junit.Test;
import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.StringUtils\'
 * Method Signature = 'abbreviate(java.lang.String,int,int):java.lang.String\'
 *
 * START Functionality Description
 *
 *       <p>Abbreviates a String using ellipses. This will turn
 *       "Now is the time for all good men" into "...is the time for..."</p>
 *      
 *       <p>Works like {@code abbreviate(String, int)}, but allows you to specify
 *       a "left edge" offset.  Note that this left edge is not necessarily going to
 *       be the leftmost character in the result, or the first character following the
 *       ellipses, but it will appear somewhere in the result.
 *      
 *       <p>In no case will it return a String of length greater than
 *       {@code maxWidth}.</p>
 *      
 *       <pre>
 *       StringUtils.abbreviate(null, , )                = null
 *       StringUtils.abbreviate("", 0, 4)                  = ""
 *       StringUtils.abbreviate("abcdefghijklmno", -1, 10) = "abcdefg..."
 *       StringUtils.abbreviate("abcdefghijklmno", 0, 10)  = "abcdefg..."
 *       StringUtils.abbreviate("abcdefghijklmno", 1, 10)  = "abcdefg..."
 *       StringUtils.abbreviate("abcdefghijklmno", 4, 10)  = "abcdefg..."
 *       StringUtils.abbreviate("abcdefghijklmno", 5, 10)  = "...fghi..."
 *       StringUtils.abbreviate("abcdefghijklmno", 6, 10)  = "...ghij..."
 *       StringUtils.abbreviate("abcdefghijklmno", 8, 10)  = "...ijklmno"
 *       StringUtils.abbreviate("abcdefghijklmno", 10, 10) = "...ijklmno"
 *       StringUtils.abbreviate("abcdefghijklmno", 12, 10) = "...ijklmno"
 *       StringUtils.abbreviate("abcdefghij", 0, 3)        = IllegalArgumentException
 *       StringUtils.abbreviate("abcdefghij", 5, 6)        = IllegalArgumentException
 *       </pre>
 *      
 *       @param str  the String to check, may be null
 *       @param offset  left edge of source String
 *       @param maxWidth  maximum length of result String, must be at least 4
 *       @return abbreviated String, {@code null} if null String input
 *       @throws IllegalArgumentException if the width is too small
 *       @since 2.0
 *      
 * END Functionality Description
 *
 * Doc ID: 9cc424b6-be67-4b3e-a496-0bb003cb65d5
 * 
 */
public class StringUtilsTest {

\t// EXP: refactored into 1:1 mapping
\t
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.StringUtils\'
   * Method Signature = 'abbreviate(java.lang.String,int,int):java.lang.String\'
   */
  @Test
  public void test_example1() throws Exception {
\t  
\t  //A: Ellipse in front of Text B: ellipse on both side C: ellipse After Text
\t  //1: Maxwith valid 2: Maxwith not valid
\t  //Y: text not null Z: text null
\t  
\t  
\t    StringUtils _cut = new StringUtils();
\t    String[] expecteds = new String[]{null, "abcdefg...","...fghi...","...ijklmno",null};
\t    
\t    //BASECHOICE A1Y
\t    String actual = _cut.abbreviate("abcdefghijklmno", 10, 10);

\t    assertEquals(expecteds[3], actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.StringUtils\'
   * Method Signature = 'abbreviate(java.lang.String,int,int):java.lang.String\'
   */
  @Test
  public void test_example2() throws Exception {
\t  
\t  //A: Ellipse in front of Text B: ellipse on both side C: ellipse After Text
\t  //1: Maxwith valid 2: Maxwith not valid
\t  //Y: text not null Z: text null
\t  
\t  
\t    StringUtils _cut = new StringUtils();
\t    String[] expecteds = new String[]{null, "abcdefg...","...fghi...","...ijklmno",null};

\t    //B1Y
\t    String actual = _cut.abbreviate("abcdefghijklmno", 5, 10);

\t    assertEquals(expecteds[2], actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.StringUtils\'
   * Method Signature = 'abbreviate(java.lang.String,int,int):java.lang.String\'
   */
  @Test
  public void test_example3() throws Exception {
\t  
\t  //A: Ellipse in front of Text B: ellipse on both side C: ellipse After Text
\t  //1: Maxwith valid 2: Maxwith not valid
\t  //Y: text not null Z: text null
\t  
\t  
\t    StringUtils _cut = new StringUtils();
\t    String[] expecteds = new String[]{null, "abcdefg...","...fghi...","...ijklmno",null};

\t    //C1Y
\t    String actual = _cut.abbreviate("abcdefghijklmno", 4, 10);

\t    assertEquals(expecteds[1], actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.StringUtils\'
   * Method Signature = 'abbreviate(java.lang.String,int,int):java.lang.String\'
   */
  @Test
  public void test_example4() throws Exception {
\t  
\t  //A: Ellipse in front of Text B: ellipse on both side C: ellipse After Text
\t  //1: Maxwith valid 2: Maxwith not valid
\t  //Y: text not null Z: text null
\t  
\t  
\t    StringUtils _cut = new StringUtils();
\t    String[] expecteds = new String[]{null, "abcdefg...","...fghi...","...ijklmno",null};
\t    
\t    //A2Y
\t    String actual = null;
\t    try {
\t    \tactual = _cut.abbreviate("abcdefghijklmno", 0, 3);
\t\t} catch (IllegalArgumentException e) {
\t\t\tactual = null;
\t\t}

\t    assertEquals(expecteds[4], actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.StringUtils\'
   * Method Signature = 'abbreviate(java.lang.String,int,int):java.lang.String\'
   */
  @Test
  public void test_example5() throws Exception {
\t  
\t  //A: Ellipse in front of Text B: ellipse on both side C: ellipse After Text
\t  //1: Maxwith valid 2: Maxwith not valid
\t  //Y: text not null Z: text null
\t  
\t  
\t    StringUtils _cut = new StringUtils();
\t    String[] expecteds = new String[]{null, "abcdefg...","...fghi...","...ijklmno",null};
\t    
\t    //A1Z
\t    String actual = _cut.abbreviate(null,1,10);

\t    assertEquals(expecteds[0], actual);
  }
}
''')
            testClasses.put('00ef8b03-5c7a-403b-925d-def90fbe8d85', '''
import org.junit.Test;
import java.lang.Exception;
import spark.resource.UriPath;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.UriPath\'
 * Method Signature = 'canonical(java.lang.String):java.lang.String\'
 *
 * START Functionality Description
 *
 *       Convert a path to a cananonical form.
 *       All instances of "." and ".." are factored out.  Null is returned
 *       if the path tries to .. above its root.
 *      
 *       @param path the path to convert
 *       @return path or null.
 *      
 * END Functionality Description
 *
 * Doc ID: 00ef8b03-5c7a-403b-925d-def90fbe8d85
 * 
 */
public class UriPathTest {
\t
\t// EXP: refactored into 1:1 mapping

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.UriPath\'
   * Method Signature = 'canonical(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example1() throws Exception {
    // class under test
    UriPath _cut = new UriPath();
    
    String[] expecteds = new String[]{"a/b/","a/b/","",null};
    
    //Contains . operator (A:Yes/ B: No)
    // (1: stepInwards > stepback /2: stepInwards = StepBack /3: StepInwards < stepBack)
    
    //BaseChoice B1:
    String actual = _cut.canonical("a/b/c/..");
    
    assertEquals(expecteds[0], actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.UriPath\'
   * Method Signature = 'canonical(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example2() throws Exception {
    // class under test
    UriPath _cut = new UriPath();
    
    String[] expecteds = new String[]{"a/b/","a/b/","",null};
    
    //Contains . operator (A:Yes/ B: No)
    // (1: stepInwards > stepback /2: stepInwards = StepBack /3: StepInwards < stepBack)
    
    //A1
    String actual = _cut.canonical("a/b/./c/..");
    
    assertEquals(expecteds[1], actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.UriPath\'
   * Method Signature = 'canonical(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example3() throws Exception {
    // class under test
    UriPath _cut = new UriPath();
    
    String[] expecteds = new String[]{"a/b/","a/b/","",null};
    
    //Contains . operator (A:Yes/ B: No)
    // (1: stepInwards > stepback /2: stepInwards = StepBack /3: StepInwards < stepBack)
    
    //B2
    String actual = _cut.canonical("a/b/c/../../..");
    
    assertEquals(expecteds[2], actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.UriPath\'
   * Method Signature = 'canonical(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example4() throws Exception {
    // class under test
    UriPath _cut = new UriPath();
    
    String[] expecteds = new String[]{"a/b/","a/b/","",null};
    
    //Contains . operator (A:Yes/ B: No)
    // (1: stepInwards > stepback /2: stepInwards = StepBack /3: StepInwards < stepBack)
    
    //B3
    String actual = _cut.canonical("a/b/c/../../../..");
    
    assertEquals(expecteds[3], actual);
  }
}
''')
            testClasses.put('3cf7db4f-407b-44e7-9398-abaedc4747d5', '''
import org.junit.Test;
import java.lang.Exception;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.GeometryUtilities' Method Signature
 * = 'getPolygonArea(int[],int[],int):double\'
 *
 * START Functionality Description
 *
 * Calculates the area of a polygon from its vertices.
 * 
 * @param x
 *            the array of x coordinates.
 * @param y
 *            the array of y coordinates.
 * @param N
 *            the number of sides of the polygon.
 * @return the area of the polygon.
 * 
 *         END Functionality Description
 *
 *         Doc ID: 3cf7db4f-407b-44e7-9398-abaedc4747d5 
 */
public class GeometryUtilitiesTest {

\t/**
\t * JUnit Test Case Example
\t *
\t *
\t * Class Under Test = 'com.XXX.adapter.GeometryUtilities' Method
\t * Signature = 'getPolygonArea(int[],int[],int):double\'
\t */
\t@Test
\tpublic void test_example() throws Exception {
\t\t// class under test
\t\tGeometryUtilities _cut = new GeometryUtilities();
\t\t// method under test
\t\t// double actual = _cut.getPolygonArea(int[],int[],int);

\t\t// A: has Area B: has no Area
\t\t// 1: is line 2: is triangle 3: is rectangle 4: is PolyEder

\t\t// BASECHOICE A2:
\t\tint[] x = new int[]{ 0, 0, 1 };
\t\tint[] y = new int[]{ 0, 1, 1 };
\t\t
\t\tdouble actual = _cut.getPolygonArea(x, y, 3);

\t\tassertEquals(0.5, actual, 0.1);

\t\t// Available assertion methods: org.junit.Assert.* and/or
\t\t// org.hamcrest.CoreMatchers.*
\t\t// e.g. assertEquals(expected, actual)
\t}

\t// B2
\t@Test
\tpublic void test_example1() throws Exception {
\t\tGeometryUtilities _cut = new GeometryUtilities();

\t\tint[] x = new int[]{ 0, 0, 0 };
\t\tint[] y = new int[]{ 0, 1, 0 };
\t\t
\t\tdouble actual = _cut.getPolygonArea(x, y, 3);

\t\tassertEquals(0, actual, 0.0);
\t}

\t// A1
\t@Test
\tpublic void test_example2() throws Exception {
\t\tGeometryUtilities _cut = new GeometryUtilities();

\t\tint[] x = new int[]{ 0, 0 };
\t\tint[] y = new int[]{ 0, 1 };
\t\t
\t\tdouble actual = _cut.getPolygonArea(x, y, 2);

\t\tassertEquals(0, actual, 0.0);
\t}

\t// A3
\t@Test
\tpublic void test_example3() throws Exception {
\t\tGeometryUtilities _cut = new GeometryUtilities();

\t\tint[] x = new int[]{0,0,1,1};
\t    int[] y = new int[]{0,1,1,0};
\t    
\t    double actual = _cut.getPolygonArea(x, y, 4);
\t    
\t    assertEquals(1.0,actual, 0.0);
\t}
\t
\t// A4
\t@Test
\t\tpublic void test_example4() throws Exception {
\t\t\tGeometryUtilities _cut = new GeometryUtilities();

\t\t\tint[] x = new int[]{0,0,1,2,1};
\t\t    int[] y = new int[]{0,1,1,0,0};
\t\t    
\t\t    double actual = _cut.getPolygonArea(x, y, 4);
\t\t    
\t\t    assertEquals(1.5, actual, 0.0);
\t\t}
}
''')
            testClasses.put('3f727645-a65d-4755-abdc-ceb470a734c3', '''
import org.junit.Test;
import java.lang.Exception;
import org.activiti.engine.impl.util.json.Cookie;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.Cookie\'
 * Method Signature = 'escape(java.lang.String):java.lang.String\'
 *
 * START Functionality Description
 * 
 *       Produce a copy of a string in which the characters '+', '%', '=', ';\'
 *       and control characters are replaced with "%hh". This is a gentle form
 *       of URL encoding, attempting to cause as little distortion to the
 *       string as possible. The characters '=' and ';' are meta characters in
 *       cookies. By convention, they are escaped using the URL-encoding. This is
 *       only a convention, not a standard. Often, cookies are expected to have
 *       encoded values. We encode '=' and ';' because we must. We encode '%' and
 *       '+' because they are meta characters in URL encoding.
 *       @param string The source string.
 *       @return       The escaped result.
 *      
 * END Functionality Description
 *
 * Doc ID: 3f727645-a65d-4755-abdc-ceb470a734c3
 * 
 */
public class CookieTest {

\t// EXP: refactored into 1:1 mapping
\t
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.Cookie\'
   * Method Signature = 'escape(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example1() throws Exception {
\t\t// class under test
\t\tCookie _cut = new Cookie();

\t\t// A: contains + B: contains no +
\t\t// 1: contains % 2: contains no 2
\t\t// I: contains = J: contains no =
\t\t// Y: contains ; Z: contains no ;

\t\t// Basechoice A1IY

\t\tString actual = _cut.escape("+%=;");
\t\t
\t\tassertEquals("%2b%25%3d%3b", actual);
\t}
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.Cookie\'
   * Method Signature = 'escape(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example2() throws Exception {
\t\t// class under test
\t\tCookie _cut = new Cookie();

\t\t// A: contains + B: contains no +
\t\t// 1: contains % 2: contains no 2
\t\t// I: contains = J: contains no =
\t\t// Y: contains ; Z: contains no ;

\t\t// B1IY
\t\tString actual1 = _cut.escape("%=;");
\t\t
\t\tassertEquals("%25%3d%3b", actual1);
\t}
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.Cookie\'
   * Method Signature = 'escape(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example3() throws Exception {
\t\t// class under test
\t\tCookie _cut = new Cookie();

\t\t// A: contains + B: contains no +
\t\t// 1: contains % 2: contains no 2
\t\t// I: contains = J: contains no =
\t\t// Y: contains ; Z: contains no ;

\t\t// A2IY
\t\tString actual2 = _cut.escape("+=;");
\t\tassertEquals("%2b%3d%3b", actual2);
\t}
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.Cookie\'
   * Method Signature = 'escape(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example4() throws Exception {
\t\t// class under test
\t\tCookie _cut = new Cookie();

\t\t// A: contains + B: contains no +
\t\t// 1: contains % 2: contains no 2
\t\t// I: contains = J: contains no =
\t\t// Y: contains ; Z: contains no ;

\t\t// A1JY
\t\tString actual3 = _cut.escape("+%;"); 
\t\tassertEquals("%2b%25%3b", actual3);
\t}
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.Cookie\'
   * Method Signature = 'escape(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example5() throws Exception {
\t\t// class under test
\t\tCookie _cut = new Cookie();

\t\t// A: contains + B: contains no +
\t\t// 1: contains % 2: contains no 2
\t\t// I: contains = J: contains no =
\t\t// Y: contains ; Z: contains no ;

\t\t// A1IZ
\t\tString actual4 = _cut.escape("+%=");
\t\tassertEquals("%2b%25%3d", actual4);
\t}
}
''')
            testClasses.put('74c4734f-52ed-4e89-86c0-844ab2043993', '''
import org.junit.Test;
import java.lang.Exception;
import org.hipparchus.util.ArithmeticUtils;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.ArithmeticUtils\'
 * Method Signature = 'lcm(long,long):long\'
 *
 * START Functionality Description
 *
 *       Returns the least common multiple of the absolute value of two numbers,
 *       using the formula {@code lcm(a,b) = (a / gcd(a,b))  b}.
 *       <p>
 *       Special cases:
 *       <ul>
 *       <li>The invocations {@code lcm(Long.MIN_VALUE, n)} and
 *       {@code lcm(n, Long.MIN_VALUE)}, where {@code abs(n)} is a
 *       power of 2, throw an {@code ArithmeticException}, because the result
 *       would be 2^63, which is too large for an int value.</li>
 *       <li>The result of {@code lcm(0L, x)} and {@code lcm(x, 0L)} is
 *       {@code 0L} for any {@code x}.
 *       </ul>
 *      
 *       @param a Number.
 *       @param b Number.
 *       @return the least common multiple, never negative.
 *       @throws MathRuntimeException if the result cannot be represented
 *       as a non-negative {@code long} value.
 *      
 * END Functionality Description
 *
 * Doc ID: 74c4734f-52ed-4e89-86c0-844ab2043993
 * 
 */
public class ArithmeticUtilsTest {

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.ArithmeticUtils\'
   * Method Signature = 'lcm(long,long):long\'
   */
  
  //Functionality-Based Approach
  //Partition 1 a: B1 a>1, B2 a=1, B3 a=0, B4 a<0
  //Partition 2 b: B1 b>1, B2 b=1, B3 b=0, B4 b<0
  // -24 and -12 are used to catch ArithmeticException / MathRuntimeException
  
  //Base-Choice P1-B1, P2-B1
  @Test
  public void bc1() throws Exception {
    
    ArithmeticUtils _cut = new ArithmeticUtils();
    long a = 6;
    long b = 3;
    long actual = _cut.lcm(a,b);

    assertEquals(6, actual);
  }
  
  //P1-B1, P2-B2
  @Test
  public void bc2() throws Exception {
    
    ArithmeticUtils _cut = new ArithmeticUtils();
    long a = 6;
    long b = 1;
    long actual = _cut.lcm(a,b);

    assertEquals(6, actual);
  }
  
  //P1-B1, P2-B3
  @Test
  public void bc3() throws Exception {
    
    ArithmeticUtils _cut = new ArithmeticUtils();
    long a = 6;
    long b = 0;
    long actual = _cut.lcm(a,b);

    assertEquals(0, actual);
  }
  
  //P1-B1, P2-B4
  @Test
  public void bc4() throws Exception {
    
    ArithmeticUtils _cut = new ArithmeticUtils();
    long a = 6;
    long b = -4;
    long actual = _cut.lcm(a,b);

    assertEquals(12, actual);
  }
  
  //P1-B2, P2-B1
  @Test
  public void bc5() throws Exception {
    
    ArithmeticUtils _cut = new ArithmeticUtils();
    long a = 1;
    long b = 3;
    long actual = _cut.lcm(a,b);

    assertEquals(3, actual);
  }
  
  //P1-B3, P2-B1
  @Test
  public void bc6() throws Exception {
    
    ArithmeticUtils _cut = new ArithmeticUtils();
    long a = 0;
    long b = 3;
    long actual = _cut.lcm(a,b);

    assertEquals(0, actual);
  }
  
  //P1-B4, P2-B1
  @Test
  public void bc7() throws Exception {
    
    ArithmeticUtils _cut = new ArithmeticUtils();
    long a = -4;
    long b = 3;
    long actual = _cut.lcm(a,b);

    assertEquals(12, actual);
  }
}
''')
            testClasses.put('044d48bc-7e59-45fd-87ee-260bc3260f0e', '''
import org.junit.Test;
import java.lang.Exception;
import org.apache.commons.lang.ArrayUtils;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.ArrayUtils\'
 * Method Signature = 'lastIndexOf(float[],float,int):int\'
 *
 * START Functionality Description
 * 
 *       <p>Finds the last index of the given value in the array starting at the given index.</p>
 *      
 *       <p>This method returns {@link #INDEX_NOT_FOUND} (<code>-1</code>) for a <code>null</code> input array.</p>
 *      
 *       <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} (<code>-1</code>). A startIndex larger than the
 *       array length will search from the end of the array.</p>
 *      
 *       @param array  the array to traverse for looking for the object, may be <code>null</code>
 *       @param valueToFind  the value to find
 *       @param startIndex  the start index to travers backwards from
 *       @return the last index of the value within the array,
 *        {@link #INDEX_NOT_FOUND} (<code>-1</code>) if not found or <code>null</code> array input
 *      
 * END Functionality Description
 *
 * Doc ID: 044d48bc-7e59-45fd-87ee-260bc3260f0e
 * 
 */
public class ArrayUtilsTest {

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.ArrayUtils\'
   * Method Signature = 'lastIndexOf(float[],float,int):int\'
   */
  
  //Functionality Based Approach
  //Partition 1 Array Parameter: Block 1 Array > 1 | Block 2 Array = 1 | Block 3 Array = {} | Block 4 Array = null
  //Partition 2 valueToFind Parameter: B1 valueToFind is element of Array | B2 valueToFind is no element of Array
  //Partition 3 startIndex Parameter: B1 startIndex = array.length() | B2 startIndex < array.length() | B3 startIndex > array.length()
  
  //Base Choice --- P1-B1, P2-B1, P3-B2
  @Test
  public void bc1() throws Exception {
\t  
\tArrayUtils _cut = new ArrayUtils();
    
\tfloat[] array = new float[]{1, 2, 3, 4, 5};
    float valueToFind = 4 ;
    int startIndex = 4;

    int actual = _cut.lastIndexOf(array, valueToFind, startIndex);
    
    assertEquals(3, actual);
  }
  
  //P1-B1, P2-B1, P3-B1
  @Test
  public void bc2() throws Exception {
\t  
\tArrayUtils _cut = new ArrayUtils();
    
\tfloat[] array = new float[]{1, 2, 3, 4, 5};
    float valueToFind = 4 ;
    int startIndex = 5;

    int actual = _cut.lastIndexOf(array, valueToFind, startIndex);
    
    assertEquals(3, actual);
  }
  
  //P1-B1, P2-B1, P3-B3
  @Test
  public void bc3() throws Exception {
\t  
\tArrayUtils _cut = new ArrayUtils();
    
\tfloat[] array = new float[]{1, 2, 3, 4, 5};
    float valueToFind = 4 ;
    int startIndex = 7;

    int actual = _cut.lastIndexOf(array, valueToFind, startIndex);
    
    assertEquals(3, actual);
  }
  
  //P1-B1, P2-B2, P3-B2
  @Test
  public void bc4() throws Exception {
\t  
\tArrayUtils _cut = new ArrayUtils();
    
\tfloat[] array = new float[]{1, 2, 3, 4, 5};
    float valueToFind = 7 ;
    int startIndex = 2;

    int actual = _cut.lastIndexOf(array, valueToFind, startIndex);
    
    assertEquals(-1, actual);
  }
  
  //P1-B2, P2-B1, P3-B2
  @Test
  public void bc5() throws Exception {
\t  
\tArrayUtils _cut = new ArrayUtils();
    
\tfloat[] array = new float[]{1};
    float valueToFind = 1 ;
    int startIndex = 0;

    int actual = _cut.lastIndexOf(array, valueToFind, startIndex);
    
    assertEquals(0, actual);
  }
  
  //P1-B3, P2-B1, P3-B2
  @Test
  public void bc6() throws Exception {
\t  
\tArrayUtils _cut = new ArrayUtils();
    
\tfloat[] array = new float[]{};
    float valueToFind = 4 ;
    int startIndex = 2;

    int actual = _cut.lastIndexOf(array, valueToFind, startIndex);
    
    assertEquals(-1, actual);
  }
  
  //P1-B4, P2-B1, P3-B2
  @Test
  public void bc7() throws Exception {
\t  
\tArrayUtils _cut = new ArrayUtils();
    
\tfloat[] array = null;
    float valueToFind = 4 ;
    int startIndex = 2;

    int actual = _cut.lastIndexOf(array, valueToFind, startIndex);
    
    assertEquals(-1, actual);
  }
}
''')
            testClasses.put('a1a0369e-eb97-4407-a538-01602e3d9f32', '''
import org.junit.Test;
import java.lang.Exception;
import com.Ostermiller.util.StringHelper;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.StringHelper\'
 * Method Signature = 'replace(java.lang.String,java.lang.String,java.lang.String):java.lang.String\'
 *
 * START Functionality Description
 *
 * \t  Replace occurrences of a substring.
 * \t 
 * \t  StringHelper.replace("1-2-3", "-", "|");<br>
 * \t  result: "1|2|3"<br>
 * \t  StringHelper.replace("-1--2-", "-", "|");<br>
 * \t  result: "|1||2|"<br>
 * \t  StringHelper.replace("123", "", "|");<br>
 * \t  result: "123"<br>
 * \t  StringHelper.replace("1-2---3----4", "--", "|");<br>
 * \t  result: "1-2|-3||4"<br>
 * \t  StringHelper.replace("1-2---3----4", "--", "---");<br>
 * \t  result: "1-2----3------4"<br>
 * \t 
 * \t  @param s String to be modified.
 * \t  @param find String to find.
 * \t  @param replace String to replace.
 * \t  @return a string with all the occurrences of the string to find replaced.
 * \t  @throws NullPointerException if s is null.
 * \t 
 * \t  @since ostermillerutils 1.00.00
 * \t 
 * END Functionality Description
 *
 * Doc ID: a1a0369e-eb97-4407-a538-01602e3d9f32
 * 
 */
public class StringHelperTest {

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.StringHelper\'
   * Method Signature = 'replace(java.lang.String,java.lang.String,java.lang.String):java.lang.String\'
   */
  
  //Functionality-Based Approach
  //Partition 1 string: B1 string.length >= 1, B2 string = "", B3 string = null
  //Partition 2 booleanFind: B1 find = true, B2 find = false
  //Partition 3 findLength: B1 find >= 1, B2 find = ""; B3 find = null
  //Partition 4 replace: B1 replace.length >= 1, B2 replace = "", B3 replace = null
  
  //Base Choice --- P1-B1, P2-B1, P3-B1, P4-B1
  @Test
  public void bc1() throws Exception {
   
    StringHelper _cut = new StringHelper();
    
    String string = "1%2%3%4%5!";
    String find = "%";
    String replace = "-";
    
    String actual = _cut.replace(string,find,replace);

    assertEquals("1-2-3-4-5!", actual);
  }
  
  //P1-B1, P2-B1, P3-B1, P4-B2
  @Test
  public void bc2() throws Exception {
   
    StringHelper _cut = new StringHelper();
    
    String string = "1%2%3%4%5!";
    String find = "%";
    String replace = "";
    
    String actual = _cut.replace(string,find,replace);

    assertEquals("12345!", actual);
  }
  
  //P1-B1, P2-B1, P3-B1, P4-B3
  @Test
  public void bc3() throws Exception {
   
    StringHelper _cut = new StringHelper();
    
    String string = "1%2%3%4%5!";
    String find = "%";
    String replace = null;
    
    String actual = _cut.replace(string,find,replace);

    assertEquals("12345!", actual);
  }
  
  //P1-B1, P2-B1, P3-B2, P4-B1
  @Test
  public void bc4() throws Exception {
   
    StringHelper _cut = new StringHelper();
    
    String string = "1%2%3%4%5!";
    String find = "";
    String replace = "-";
    
    String actual = _cut.replace(string,find,replace);

    assertEquals("1%2%3%4%5!", actual);
  }
  
  //P1-B1, P2-B1, P3-B3, P4-B1
  @Test
  public void bc5() throws Exception {
   
    StringHelper _cut = new StringHelper();
    
    String string = "1%2%3%4%5!";
    String find = null;
    String replace = "-";
    
    String actual = _cut.replace(string,find,replace);

    assertEquals("1%2%3%4%5!", actual);
  }
  
  //P1-B1, P2-B2, P3-B1, P4-B1
  @Test
  public void bc6() throws Exception {
   
    StringHelper _cut = new StringHelper();
    
    String string = "1%2%3%4%5!";
    String find = "§";
    String replace = "-";
    
    String actual = _cut.replace(string,find,replace);

    assertEquals("1%2%3%4%5!", actual);
  }
  
  //P1-B2, P2-B1, P3-B1, P4-B1
  @Test
  public void bc7() throws Exception {
   
    StringHelper _cut = new StringHelper();
    
    String string = "";
    String find = "%";
    String replace = "-";
    
    String actual = _cut.replace(string,find,replace);

    assertEquals("", actual);
  }
  
  //P1-B3, P2-B1, P3-B1, P4-B1
  @Test
  public void bc8() throws Exception {
   
    StringHelper _cut = new StringHelper();
    
    String string = null;
    String find = "%";
    String replace = "-";

    try {
         String actual = _cut.replace(string,find,replace);
         fail();
    } catch (Throwable e) {
    }
  }
}
''')
            testClasses.put('87eb9eb8-7c57-4f54-8ecb-f2be91e7a38f', '''
import org.junit.Test;
import java.lang.Exception;
import picard.util.MathUtil;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.MathUtil\'
 * Method Signature = 'median(double[]):double\'
 *
 * START Functionality Description
 *  Calculate the median of an array of doubles. Assumes that the input is sorted 
 * END Functionality Description
 *
 * Doc ID: 87eb9eb8-7c57-4f54-8ecb-f2be91e7a38f
 * 
 */
public class MathUtilTest {

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.MathUtil\'
   * Method Signature = 'median(double[]):double\'
   */
  
  //Functionality-Based Approach
  //Partition 1 even/uneven : B1 array.length() % 2 == 1, B2 array.length() % 2 == 0
  //Partition 2 array: B1 array >= 1, B2 array = {}, B3 = null
  
  //Base-Choice P1-B1, P2-B1
  @Test
  public void bc1() throws Exception {
    
    MathUtil _cut = new MathUtil();
    double[] array = new double[]{1.0, 2.0, 3.0};
    double actual = _cut.median(array);

    assertEquals(2.0, actual, 0.1d);
  }
  
  //P1-B1, P2-B2
  @Test
  public void bc2() throws Exception {
    
    MathUtil _cut = new MathUtil();
    double[] array = new double[]{};
    

    try {
         double actual = _cut.median(array);
         fail();
    } catch (Throwable e) {
    }
  }
  
  //P1-B1, P2-B3
  @Test
  public void bc3() throws Exception {
    
    MathUtil _cut = new MathUtil();
    double[] array = null;
    try {
         double actual = _cut.median(array);
         fail();
    } catch (Throwable e) {
    }
  }
  
  //P1-B2, P2-B1
  @Test
  public void bc4() throws Exception {
    
    MathUtil _cut = new MathUtil();
    double[] array = new double[]{1.0, 2.0};
    double actual = _cut.median(array);

    assertEquals(1.5, actual, 0.1d);
  }
}
''')
            testClasses.put('16eb796d-1533-4ff8-9be4-318d52a09a50', '''
import org.junit.Test;
import java.lang.Exception;
import org.apache.poi.util.StringUtil;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.StringUtil\'
 * Method Signature = 'format(java.lang.String,java.lang.Object[]):java.lang.String\'
 *
 * START Functionality Description
 * 
 * \t   Apply printf() like formatting to a string.
 * \t   Primarily used for logging.
 * \t  @param  message  the string with embedded formatting info
 * \t                  eg. "This is a test %2.2"
 * \t  @param  params   array of values to format into the string
 * \t  @return          The formatted string
 * \t 
 * END Functionality Description
 *
 * Doc ID: 16eb796d-1533-4ff8-9be4-318d52a09a50
 * 
 */
public class StringUtilTest {

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.StringUtil\'
   * Method Signature = 'format(java.lang.String,java.lang.Object[]):java.lang.String\'
   */
  
  //Functionality-Based Approach
  //Partition 1 message: B1 message.length > 1, B2 message.length = 1, B3 messsage = "",  B4 message = null
  //Partition 2 params: B1 params.length() > 1, B2 params.length = 1, B3 params = {}, B4 params = null
  
  //Base-Choice --- P1-B1, P2-B1
  @Test
  public void bc1() throws Exception {
    
    StringUtil _cut = new StringUtil();
    String message = "Yes we can! %, %";
    Object[] params = new Object[]{"Barack Obama", 2008};
    String actual = _cut.format(message,params);

    assertEquals("Yes we can! Barack Obama, "+2008, actual);
  }
  
  //P1-B1, P2-B2
  @Test
  public void bc2() throws Exception {
    
    StringUtil _cut = new StringUtil();
    String message = "Yes we can! %";
    Object [] params = new Object[]{"Barack Obama"};
    String actual = _cut.format(message,params);

    assertEquals("Yes we can! Barack Obama", actual);
  }
  
  //P1-B1, P2-B3
  @Test
  public void bc3() throws Exception {
    
    StringUtil _cut = new StringUtil();
    String message = "Yes we can! %, %";
    Object [] params = new Object[]{};
    String actual = _cut.format(message,params);

    assertNotEquals(message, actual);
  }
  
  //P1-B1, P2-B4
  @Test
  public void bc4() throws Exception {
    
    StringUtil _cut = new StringUtil();
    String message = "Yes we can! %, %";
    Object [] params = null;
    
    try {
         String actual = _cut.format(message,params);
         fail();
    } catch (Throwable e) {
    }
  }
  
  //P1-B2, P2-B1
  @Test
  public void bc5() throws Exception {
    
    StringUtil _cut = new StringUtil();
    String message = "I";
    Object [] params = new Object[]{"Barack Obama"};
    String actual = _cut.format(message,params);

    assertEquals("I", actual);
  }
  
  //P1-B3, P2-B1
  @Test
  public void bc6() throws Exception {
    
    StringUtil _cut = new StringUtil();
    String message = "";
    Object [] params = new Object[]{"Barack Obama"};
    String actual = _cut.format(message,params);

    assertEquals("", actual);
  }
  
  //P1-B4, P2-B1
  @Test
  public void bc7() throws Exception {
    
    StringUtil _cut = new StringUtil();
    String message = null;
    Object [] params = new Object[]{"Barack Obama", 2008};
    try {
         String actual = _cut.format(message,params);
         fail();
    } catch (Throwable e) {
    }
  }
}
''')
            testClasses.put('081fae15-faf8-4b0b-9026-c80966710051', '''
import org.junit.Test;
import java.lang.Exception;
import smile.sort.QuickSelect;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.QuickSelect\'
 * Method Signature = 'select(int[],int):int\'
 *
 * START Functionality Description
 *
 *       Given k in [0, n-1], returns an array value from arr such that k array
 *       values are less than or equal to the one returned. The input array will
 *       be rearranged to have this value in location arr[k], with all smaller
 *       elements moved to arr[0, k-1] (in arbitrary order) and all larger elements
 *       in arr[k+1, n-1] (also in arbitrary order).
 *      
 * END Functionality Description
 *
 * Doc ID: 081fae15-faf8-4b0b-9026-c80966710051
 * 
 */
public class QuickSelectTest {

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.QuickSelect\'
   * Method Signature = 'select(int[],int):int\'
   */
  
  //Functionality-Based Approach
  //Partition 1 array: B1 array > 1, B2 array = 1, B3 array = {}, B4 array = null
  //Partition 2 k: B1 k <= array.length(), B2 k > array.length(), B3 k=0
  
  //Base Choice --- P1-B1 , P2-B1
  @Test
  public void bc1() throws Exception {
    
    QuickSelect _cut = new QuickSelect();
    int[] array = new int[]{1,2,3,4,5,6,7,8,9,10};
    int k = 5;

    int actual = _cut.select(array, k);

    assertEquals(6, actual);
  }
  
  //P1-B1 , P2-B2
  @Test
  public void bc2() throws Exception {
    
    QuickSelect _cut = new QuickSelect();
    int[] array = new int[]{1,2,3,4,5,6,7,8,9,10};
    int k = 42;

    try {
         int actual = _cut.select(array, k);
         fail();
    } catch (Throwable e) {
    }
  }
  
  //P1-B1 , P2-B3
  @Test
  public void bc3() throws Exception {
    
    QuickSelect _cut = new QuickSelect();
    int[] array = new int[]{1,2,3,4,5,6,7,8,9,10};
    int k = 0;

    int actual = _cut.select(array, k);

    assertEquals(1, actual);
  }
  
  //P1-B2 , P2-B1
  @Test
  public void bc4() throws Exception {
    
    QuickSelect _cut = new QuickSelect();
    int[] array = new int[]{1};
    int k = 0;

    int actual = _cut.select(array, k);

    assertEquals(1, actual);
  }
  
  //P1-B3 , P2-B1
  @Test
  public void bc5() throws Exception {
    
    QuickSelect _cut = new QuickSelect();
    int[] array = new int[]{};
    int k = 5;

    try {
         int actual = _cut.select(array, k);
         fail();
    } catch (Throwable e) {
    }
  }
  
  //P1-B4 , P2-B1
  @Test
  public void bc6() throws Exception {
    
    QuickSelect _cut = new QuickSelect();
    int[] array = null;
    int k = 5;

    try {
         int actual = _cut.select(array, k);
         fail();
    } catch (Throwable e) {
    }
  }
}
''')
            testClasses.put('782c0cbb-bcc6-4a1d-b018-e6f9c7bc81bc', '''
import org.junit.Test;
import java.lang.Exception;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.QuotedPrintableCodec\'
 * Method Signature = 'decodeQuotedPrintable(byte[]):byte[]\'
 *
 * START Functionality Description
 * 
 *       Decodes an array quoted-printable characters into an array of original bytes. Escaped characters are converted
 *       back to their original representation.
 *       
 *       <p>
 *       This function implements a subset of quoted-printable encoding specification (rule #1 and rule #2) as defined in
 *       RFC 1521.
 *       </p>
 *       
 *       @param bytes
 *                        array of quoted-printable characters
 *       @return array of original bytes
 *       @throws DecoderException
 *                        Thrown if quoted-printable decoding is unsuccessful
 *      
 * END Functionality Description
 *
 * Doc ID: 782c0cbb-bcc6-4a1d-b018-e6f9c7bc81bc
 * 
 */
public class QuotedPrintableCodecTest {

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.QuotedPrintableCodec\'
   * Method Signature = 'decodeQuotedPrintable(byte[]):byte[]\'
   */
  //string contains escaped characters
\t@Test
\tpublic void test_example0() throws Exception {
\t\tQuotedPrintableCodec _cut = new QuotedPrintableCodec();
\t\tString quotedPrintable = "p=C3=A9dagogues";
\t\tbyte[] bytes = quotedPrintable.getBytes();
\t\tbyte[] actual = _cut.decodeQuotedPrintable(bytes);
\t\tassertArrayEquals("pédagogues".getBytes(), actual);
\t}
\t
\t//string does not contain any escaped characters
\t@Test
\tpublic void test_example1() throws Exception {
\t\tQuotedPrintableCodec _cut = new QuotedPrintableCodec();
\t\tString quotedPrintable = "Lehrer";
\t\tbyte[] bytes = quotedPrintable.getBytes();
\t\tbyte[] actual = _cut.decodeQuotedPrintable(bytes);
\t\tassertArrayEquals("Lehrer".getBytes(), actual);
\t}
\t
\t//decoding failed
\t@Test
\tpublic void test_example2() throws Exception {
\t\tQuotedPrintableCodec _cut = new QuotedPrintableCodec();
\t\tString quotedPrintable = "p=C/=A9dagogues";
\t\t//ExpectedException exception = ExpectedException.none();
\t\t//exception..expect(Exception.class);
\t\tbyte[] bytes = quotedPrintable.getBytes();
    try {
         byte[] actual = _cut.decodeQuotedPrintable(bytes);
         fail();
    } catch (Throwable e) {
    }

\t}
}
''')
            testClasses.put('588073cd-f6bc-4c0c-8cce-a0abf40d627a', '''
import org.junit.Test;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Exception;
import com.facebook.presto.hadoop.shaded.org.apache.commons.lang.StringEscapeUtils;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.StringEscapeUtils\'
 * Method Signature = 'escapeCsv(java.io.Writer,java.lang.String):void\'
 *
 * START Functionality Description
 * 
 *       <p>Writes a <code>String</code> value for a CSV column enclosed in double quotes,
 *       if required.</p>
 *      
 *       <p>If the value contains a comma, newline or double quote, then the
 *          String value is written enclosed in double quotes.</p>
 *       </p>
 *      
 *       <p>Any double quote characters in the value are escaped with another double quote.</p>
 *      
 *       <p>If the value does not contain a comma, newline or double quote, then the
 *          String value is written unchanged (null values are ignored).</p>
 *       </p>
 *      
 *       see <a href="http://en.wikipedia.org/wiki/Comma-separated_values">Wikipedia</a> and
 *       <a href="http://tools.ietf.org/html/rfc4180">RFC 4180</a>.
 *      
 *       @param str the input CSV column String, may be null
 *       @param out Writer to write input string to, enclosed in double quotes if it contains
 *       a comma, newline or double quote
 *       @throws IOException if error occurs on underlying Writer
 *       @since 2.4
 *      
 * END Functionality Description
 *
 * Doc ID: 588073cd-f6bc-4c0c-8cce-a0abf40d627a
 * 
 */
public class StringEscapeUtilsTest {
\t
\t//csvColumn does not contain a comma, newline or double quote
\t@Test
\tpublic void test_example0() throws Exception {
\t\tStringEscapeUtils _cut = new StringEscapeUtils();
\t\tString csv = "Coffee";
\t\tWriter writer = new StringWriter();
\t\t_cut.escapeCsv(writer, csv);
\t\tString actual = writer.toString();
\t\tassertEquals(csv, actual);
\t}

\t//csvColumn contains double quotes
\t@Test
\tpublic void test_example1() throws Exception {
\t\tStringEscapeUtils _cut = new StringEscapeUtils();
\t\tString csv = "Super \\"luxurious\\" truck";
\t\tWriter writer = new StringWriter();
\t\t_cut.escapeCsv(writer, csv);
\t\tString actual = writer.toString();
\t\tassertEquals("\\"Super \\"\\"luxurious\\"\\" truck\\"", actual);
\t}

\t//csvColumn contains a comma
\t@Test
\tpublic void test_example2() throws Exception {
\t\tStringEscapeUtils _cut = new StringEscapeUtils();
\t\tString csv = "Super, luxurious truck";
\t\tWriter writer = new StringWriter();
\t\t_cut.escapeCsv(writer, csv);
\t\tString actual = writer.toString();
\t\tassertEquals("\\"Super, luxurious truck\\"", actual);
\t}
\t
\t//csvColumn is null
\t@Test
\tpublic void test_example3() throws Exception {
\t\tStringEscapeUtils _cut = new StringEscapeUtils();
\t\tString csv = null;
\t\tWriter writer = new StringWriter();
\t\t_cut.escapeCsv(writer, csv);
\t\tString actual = writer.toString();
\t\tassertEquals("", actual);
\t}
\t
\t//csvColumn is empty
\t@Test
\tpublic void test_example4() throws Exception {
\t\tStringEscapeUtils _cut = new StringEscapeUtils();
\t\tString csv = "";
\t\tWriter writer = new StringWriter();
\t\t_cut.escapeCsv(writer, csv);
\t\tString actual = writer.toString();
\t\tassertEquals("", actual);
\t}
\t
\t//csvColumn contains a new line
\t@Test
\tpublic void test_example5() throws Exception {
\t\tStringEscapeUtils _cut = new StringEscapeUtils();
\t\tString csv = "Super \\n luxurious truck";
\t\tWriter writer = new StringWriter();
\t\t_cut.escapeCsv(writer, csv);
\t\tString actual = writer.toString();
\t\tassertEquals("\\"Super \\n luxurious truck\\"", actual);
\t}
\t
\t//csvColumn contains a comma and a new line
\t@Test
\tpublic void test_example6() throws Exception {
\t\tStringEscapeUtils _cut = new StringEscapeUtils();
\t\tString csv = "Super \\n luxurious, truck";
\t\tWriter writer = new StringWriter();
\t\t_cut.escapeCsv(writer, csv);
\t\tString actual = writer.toString();
\t\tassertEquals("\\"Super \\n luxurious, truck\\"", actual);
\t}
}
''')
            testClasses.put('4847cb82-fc0f-4647-acf6-2a5bf2967e08', '''
import org.junit.Test;
import java.lang.Exception;
import org.xwiki.xml.XMLUtils;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.XMLUtils\'
 * Method Signature = 'escapeXMLComment(java.lang.String):java.lang.String\'
 *
 * START Functionality Description
 *
 *       XML comment does not support some characters inside its content but there is no official escaping/unescaping for
 *       it so we made our own.
 *       <ul>
 *         <li>1) Escape existing \\</li>
 *         <li>2) Escape --</li>
 *         <li>3) Add {@code \\} (unescaped as {@code ""}) at the end if the last char is {@code -}</li>
 *       </ul>
 *      
 *       @param content the XML comment content to escape
 *       @return the escaped content.
 *       @since 1.9M2
 *      
 * END Functionality Description
 *
 * Doc ID: 4847cb82-fc0f-4647-acf6-2a5bf2967e08
 * 
 */
public class XMLUtilsTest {

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.XMLUtils\'
   * Method Signature = 'escapeXMLComment(java.lang.String):java.lang.String\'
   */
  //case 1) '\\\'
  @Test
  public void test_example0() throws Exception {
    XMLUtils _cut = new XMLUtils();
    String xmlComment = "\\\\";
    String actual = _cut.escapeXMLComment(xmlComment);
    assertEquals("\\\\\\\\", actual);
  }

  //case 2) '--\'
  @Test
  public void test_example1() throws Exception {
    XMLUtils _cut = new XMLUtils();
    String xmlComment = "--";
    String actual = _cut.escapeXMLComment(xmlComment);
    assertEquals("-\\\\-\\\\", actual);
  }
  
  //comment not containing any character that has to be escaped 
  @Test
  public void test_example2() throws Exception {
    XMLUtils _cut = new XMLUtils();
    String xmlComment = "foo";
    String actual = _cut.escapeXMLComment(xmlComment);
    assertEquals("foo", actual);
  }
  
  //case 3) '-' after other characters ('-' is last character)
  @Test
  public void test_example3() throws Exception {
    XMLUtils _cut = new XMLUtils();
    String xmlComment = "foo-";
    String actual = _cut.escapeXMLComment(xmlComment);
    assertEquals("foo-\\\\", actual);
  }
  
  //case 3) last character '-\'
  @Test
  public void test_example4() throws Exception {
    XMLUtils _cut = new XMLUtils();
    String xmlComment = "-";
    String actual = _cut.escapeXMLComment(xmlComment);
    assertEquals("-\\\\", actual);
  }
  
  //comment contains '-' but '-' is not the last character
  @Test
  public void test_example5() throws Exception {
    XMLUtils _cut = new XMLUtils();
    String xmlComment = "foo-foo";
    String actual = _cut.escapeXMLComment(xmlComment);
    assertEquals("foo-foo", actual);
  }
  
  //empty comment
  @Test
  public void test_example6() throws Exception {
    XMLUtils _cut = new XMLUtils();
    String xmlComment = "";
    String actual = _cut.escapeXMLComment(xmlComment);
    assertEquals("", actual);
  }
  
  //comment is null
  @Test
  public void test_example7() throws Exception {
    XMLUtils _cut = new XMLUtils();
    String xmlComment = null;

    try {
         String actual = _cut.escapeXMLComment(xmlComment);
         fail();
    } catch (Throwable e) {
    }
  }
  
  //'\\' within a sequence of characters
  @Test
  public void test_example8() throws Exception {
    XMLUtils _cut = new XMLUtils();
    String xmlComment = "foo\\\\foo";
    String actual = _cut.escapeXMLComment(xmlComment);
    assertEquals("foo\\\\\\\\foo", actual);
  }
}
''')
            testClasses.put('36091083-91d7-4869-a8a2-982f185ac25a', '''
import org.junit.Test;
import java.lang.Exception;
import edu.cmu.sphinx.alignment.tokenizer.NumberExpander;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.NumberExpander\'
 * Method Signature = 'expandRoman(java.lang.String):int\'
 *
 * START Functionality Description
 *
 *       Returns the integer value of the given string of Roman numerals.
 *      
 *       @param roman the string of Roman numbers
 *      
 *       @return the integer value
 *      
 * END Functionality Description
 *
 * Doc ID: 36091083-91d7-4869-a8a2-982f185ac25a
 * 
 */
public class NumberExpanderTest {
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.NumberExpander\'
   * Method Signature = 'expandRoman(java.lang.String):int\'
   */
  //roman numeral consisting of more than one capitalized letter
  @Test
  public void test_example0() throws Exception {
    NumberExpander _cut = new NumberExpander();
    String romanNumber = "XIX";
    int actual = _cut.expandRoman(romanNumber);
    assertEquals(19, actual);
  }
  
  //"roman numeral" in lower case
  @Test
  public void test_example1() throws Exception {
    NumberExpander _cut = new NumberExpander();
    String romanNumber = "xix";
    int actual = _cut.expandRoman(romanNumber);
    assertEquals(0, actual);
  }
  
  //empty string
  @Test
  public void test_example2() throws Exception {
    NumberExpander _cut = new NumberExpander();
    String romanNumber = "";
    int actual = _cut.expandRoman(romanNumber);
    assertEquals(0, actual);
  }
  
  //roman numeral consisting of one capitalized letter
  @Test
  public void test_example3() throws Exception {
    NumberExpander _cut = new NumberExpander();
    String romanNumber = "I";
    int actual = _cut.expandRoman(romanNumber);
    assertEquals(1, actual);
  }
  
  //string is no roman numeral
  @Test
  public void test_example4() throws Exception {
    NumberExpander _cut = new NumberExpander();
    String romanNumber = "Q";
    int actual = _cut.expandRoman(romanNumber);
    assertEquals(0, actual);
  }
  
  //invalid sequence of the characters
  @Test
  public void test_example5() throws Exception {
    NumberExpander _cut = new NumberExpander();
    String romanNumber = "IXX";
    int actual = _cut.expandRoman(romanNumber);
    assertEquals(19, actual);
  }
}
''')
            testClasses.put('7688ec91-4436-4a53-bcb7-76f86303199e', '''
import org.junit.Test;
import java.lang.Exception;
import org.apache.poi.ss.util.NumberComparer;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.NumberComparer\'
 * Method Signature = 'compare(double,double):int\'
 *
 * START Functionality Description
 *
 * \t  This class attempts to reproduce Excel\'s behaviour for comparing numbers.  Results are
 * \t  mostly the same as those from {@link Double#compare(double, double)} but with some
 * \t  rounding.  For numbers that are very close, this code converts to a format having 15
 * \t  decimal digits of precision and a decimal exponent, before completing the comparison.
 * \t  <p/>
 * \t  In Excel formula evaluation, expressions like "(0.06-0.01)=0.05" evaluate to "TRUE" even
 * \t  though the equivalent java expression is <code>false</code>.  In examples like this,
 * \t  Excel achieves the effect by having additional logic for comparison operations.
 * \t  <p/>
 * \t  <p/>
 * \t  Note - Excel also gives special treatment to expressions like "0.06-0.01-0.05" which
 * \t  evaluates to "0" (in java, rounding anomalies give a result of 6.9E-18).  The special
 * \t  behaviour here is for different reasons to the example above:  If the last operator in a
 * \t  cell formula is '+' or '-' and the result is less than 2<sup>50</sup> times smaller than
 * \t  first operand, the result is rounded to zero.
 * \t  Needless to say, the two rules are not consistent and it is relatively easy to find
 * \t  examples that satisfy<br/>
 * \t  "A=B" is "TRUE" but "A-B" is not "0"<br/>
 * \t  and<br/>
 * \t  "A=B" is "FALSE" but "A-B" is "0"<br/>
 * \t  <br/>
 * \t  This rule (for rounding the result of a final addition or subtraction), has not been
 * \t  implemented in POI (as of Jul-2009).
 * \t 
 * \t  @return <code>negative, 0, or positive</code> according to the standard Excel comparison
 * \t  of values <tt>a</tt> and <tt>b</tt>.
 * \t 
 * END Functionality Description
 *
 * Doc ID: 7688ec91-4436-4a53-bcb7-76f86303199e
 * 
 */
public class NumberComparerTest {

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.NumberComparer\'
   * Method Signature = 'compare(double,double):int\'
   */
  //"normal" behaviour of compare method: first argument is greater than second argument
  @Test
  public void test_example0() throws Exception {
    NumberComparer _cut = new NumberComparer();
    int actual = _cut.compare(0.05, 0.04);
    assertEquals(1, actual);
  }
  
  //"normal" behaviour of compare method: first argument is smaller than second argument
  @Test
  public void test_example1() throws Exception {
    NumberComparer _cut = new NumberComparer();
    int actual = _cut.compare(0.04, 0.05);
    assertEquals(-1, actual);
  }
  
  
  //"normal behaviour of compare method: first argument is equal to the second argument"
  @Test
  public void test_example2() throws Exception {
    NumberComparer _cut = new NumberComparer();
    int actual = _cut.compare(0.05, 0.05);
    assertEquals(0, actual);
  }
  
  //"A=B" is "TRUE" but "A-B" is not "0"
  //A=0.06-0.01, B=0.05
  @Test
  public void test_example3() throws Exception {
    NumberComparer _cut = new NumberComparer();
    int actual = _cut.compare(0.06-0.01, 0.05);
    assertEquals(0, actual);
  }
  
  //no example found for "A=B" is "FALSE" but "A-B" is "0"
}
''')
            testClasses.put('61c1aa07-ba6f-4dd8-8746-580e33d2aa2a', '''
import org.junit.Test;
import java.lang.Exception;
import com.tectonica.xmlchunk.XmlUtil;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.XmlUtil\'
 * Method Signature = 'unescape(java.lang.String):java.lang.String\'
 *
 * START Functionality Description
 * 
 * \t  Transforms an escaped XML into the original, "un-escaped" value (for example turn &amp;lt;Hello&amp;gt; into
 * \t  &lt;Hello&gt;)
 * \t  
 * \t  @param escaped
 * \t             the escaped XML string
 * \t  @return the un-escaped XML string
 * \t  @throws XMLStreamException
 * \t 
 * END Functionality Description
 *
 * Doc ID: 61c1aa07-ba6f-4dd8-8746-580e33d2aa2a
 * 
 */
public class XmlUtilTest {
  
  /**
   *  String length: 
                   null
\t\t             0
\t\t            >1


         hasTag:         true,false



   */
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.XmlUtil\'
   * Method Signature = 'unescape(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example() throws Exception {
    // class under test
    XmlUtil _cut = new XmlUtil();
    // method under test
    java.lang.String actual = _cut.unescape("aa");

    assertEquals("aa", actual);
    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    // e.g. assertEquals(expected, actual)
  }
  
  
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.XmlUtil\'
   * Method Signature = 'unescape(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_exampleOne() throws Exception {
    // class under test
    XmlUtil _cut = new XmlUtil();
    // method under test
    java.lang.String actual = _cut.unescape("&amp;lt;Hello&amp;gt;");

    assertEquals("&lt;Hello&gt;", actual);
    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    // e.g. assertEquals(expected, actual)
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.XmlUtil\'
   * Method Signature = 'unescape(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_exampleTwo() throws Exception {
    // class under test
    XmlUtil _cut = new XmlUtil();
    // method under test
    java.lang.String actual = _cut.unescape("");

    assertEquals("", actual);
    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    // e.g. assertEquals(expected, actual)
  }
  
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.XmlUtil\'
   * Method Signature = 'unescape(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_exampleThree() throws Exception {
    // class under test
    XmlUtil _cut = new XmlUtil();
    // method under test
    java.lang.String actual = _cut.unescape(null);

    assertEquals(null, actual);
    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    // e.g. assertEquals(expected, actual)
  }
}
''')
            testClasses.put('478068b0-9cf1-458d-8ab3-f70e6d3d8855', '''
import org.junit.Test;
import java.lang.Exception;
import com.facebook.presto.hadoop.shaded.org.apache.commons.lang.StringUtils;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.StringUtils\'
 * Method Signature = 'indexOfDifference(java.lang.String,java.lang.String):int\'
 *
 * START Functionality Description
 *
 *       <p>Compares two Strings, and returns the index at which the
 *       Strings begin to differ.</p>
 *      
 *       <p>For example,
 *       <code>indexOfDifference("i am a machine", "i am a robot") -> 7</code></p>
 *      
 *       <pre>
 *       StringUtils.indexOfDifference(null, null) = -1
 *       StringUtils.indexOfDifference("", "") = -1
 *       StringUtils.indexOfDifference("", "abc") = 0
 *       StringUtils.indexOfDifference("abc", "") = 0
 *       StringUtils.indexOfDifference("abc", "abc") = -1
 *       StringUtils.indexOfDifference("ab", "abxyz") = 2
 *       StringUtils.indexOfDifference("abcde", "abxyz") = 2
 *       StringUtils.indexOfDifference("abcde", "xyz") = 0
 *       </pre>
 *      
 *       @param str1  the first String, may be null
 *       @param str2  the second String, may be null
 *       @return the index where str2 and str1 begin to differ; -1 if they are equal
 *       @since 2.0
 *      
 * END Functionality Description
 *
 * Doc ID: 478068b0-9cf1-458d-8ab3-f70e6d3d8855
 * 
 */
public class StringUtilsTest {

  /**
   * 
   * C1: Parameter1 length
     B1: null
     B2: 0
     B3: >0

   * C2: Parameter1
     B1: null
     B2: 0
     B3: >0


   * C3: Relation from S1 to S2
       isEqual  
       B1: true
       B2: false

   * C4: Relation from S1 to S2
       isDisjoint 
       B1: true
       B2: false
   
   * C5: Relation from S1 to S2
         startsWithSameSubSet
       
       
    


   */
  /**
   * JUnit Test Case Example   >0,>0,false,true,false
   *
   *
   * Class Under Test = 'com.XXX.adapter.StringUtils\'
   * Method Signature = 'indexOfDifference(java.lang.String,java.lang.String):int\'
   */
  @Test
  public void test_example() throws Exception {
    // class under test
    StringUtils _cut = new StringUtils();
    
    String parameter1 = "a";
    String parameter2 = "b";
    // method under test
   int actual = _cut.indexOfDifference(parameter1,parameter2);
   
   assertEquals(0, actual);

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    // e.g. assertEquals(expected, actual)
  }
  
  
  
 /**
  * JUnit Test Case Example 
  *
  *
  * Class Under Test = 'com.XXX.adapter.StringUtils\'
  * Method Signature = 'indexOfDifference(java.lang.String,java.lang.String):int\'
  */
 @Test
 public void test_exampleTwo() throws Exception {
   // class under test
   StringUtils _cut = new StringUtils();
   
   String parameter1 = "a";
   String parameter2 = "";
   // method under test
  int actual = _cut.indexOfDifference(parameter1,parameter2);
  
  assertEquals(0, actual);

   // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
   // e.g. assertEquals(expected, actual)
 }
 
 
 /**
  * JUnit Test Case Example   >0,>0,false,true,false
  *
  *
  * Class Under Test = 'com.XXX.adapter.StringUtils\'
  * Method Signature = 'indexOfDifference(java.lang.String,java.lang.String):int\'
  */
 @Test
 public void test_exampleThree() throws Exception {
   // class under test
   StringUtils _cut = new StringUtils();
   
   String parameter1 = "a";
   String parameter2 = null;
   // method under test
  int actual = _cut.indexOfDifference(parameter1,parameter2);
  
  assertEquals(0, actual);

   // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
   // e.g. assertEquals(expected, actual)
 }
 
 
 /**
  * JUnit Test Case Example   >0,>0,false,true,false
  *
  *
  * Class Under Test = 'com.XXX.adapter.StringUtils\'
  * Method Signature = 'indexOfDifference(java.lang.String,java.lang.String):int\'
  */
 @Test
 public void test_exampleFour() throws Exception {
   // class under test
   StringUtils _cut = new StringUtils();
   
   String parameter1 = "";
   String parameter2 = "a";
   // method under test
  int actual = _cut.indexOfDifference(parameter1,parameter2);
  
  assertEquals(0, actual);

   // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
   // e.g. assertEquals(expected, actual)
 }
 
 /**
  * JUnit Test Case Example   >0,>0,false,true,false
  *
  *
  * Class Under Test = 'com.XXX.adapter.StringUtils\'
  * Method Signature = 'indexOfDifference(java.lang.String,java.lang.String):int\'
  */
 @Test
 public void test_exampleFive() throws Exception {
   // class under test
   StringUtils _cut = new StringUtils();
   
   String parameter1 = null;
   String parameter2 = "a";
   // method under test
  int actual = _cut.indexOfDifference(parameter1,parameter2);
  
  assertEquals(0, actual);

   // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
   // e.g. assertEquals(expected, actual)
 }
 /**
  * JUnit Test Case Example   >0,>0,false,true,false
  *
  *
  * Class Under Test = 'com.XXX.adapter.StringUtils\'
  * Method Signature = 'indexOfDifference(java.lang.String,java.lang.String):int\'
  */
 @Test
 public void test_exampleSix() throws Exception {
   // class under test
   StringUtils _cut = new StringUtils();
   
   String parameter1 = "a";
   String parameter2 = "a";
   // method under test
  int actual = _cut.indexOfDifference(parameter1,parameter2);
  
  assertEquals(-1, actual);

   // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
   // e.g. assertEquals(expected, actual)
 }
 
 /**
  * JUnit Test Case Example   >0,>0,false,true,false
  *
  *
  * Class Under Test = 'com.XXX.adapter.StringUtils\'
  * Method Signature = 'indexOfDifference(java.lang.String,java.lang.String):int\'
  */
 @Test
 public void test_exampleSeven() throws Exception {
   // class under test
   StringUtils _cut = new StringUtils();
   
   String parameter1 = "a";
   String parameter2 = "ab";
   // method under test
  int actual = _cut.indexOfDifference(parameter1,parameter2);
  
  assertEquals(1, actual);

   // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
   // e.g. assertEquals(expected, actual)
 }
}
''')
            testClasses.put('85e5461c-cdce-4d75-9fdc-a990220ae32c', '''
import org.junit.Test;
import java.lang.Exception;
import com.github.bordertech.wcomponents.WebUtilities;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.WebUtilities\'
 * Method Signature = 'escapeForUrl(java.lang.String):java.lang.String\'
 *
 * START Functionality Description
 *
 * \t  Escapes the given string to make it presentable in a URL. This follows RFC 3986, with some extensions for UTF-8.
 * \t 
 * \t  @param input the String to escape.
 * \t  @return an escaped copy of the string.
 * \t 
 * END Functionality Description
 *
 * Doc ID: 85e5461c-cdce-4d75-9fdc-a990220ae32c
 * 
 */
public class WebUtilitiesTest {

  /**
   *  String length: 
              null
\t\t     0
\t\t     >1

  hasReserveredCharacter:  true,false

  hasUnreservedCharacter: true,false

  ValidInput: true,false
   */

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.WebUtilities\'
   * Method Signature = 'escapeForUrl(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example() throws Exception {
    WebUtilities _cut = new WebUtilities();
    // method under test
    java.lang.String actual = _cut.escapeForUrl("a");

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    assertEquals("a", actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.WebUtilities\'
   * Method Signature = 'escapeForUrl(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_exampleOne() throws Exception {
    WebUtilities _cut = new WebUtilities();
    // method under test
    java.lang.String actual = _cut.escapeForUrl("äöü");

    System.out.println(actual);
    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    assertEquals("%c3%a4%c3%b6%c3%bc", actual);
  }
  
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.WebUtilities\'
   * Method Signature = 'escapeForUrl(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_exampleTwo() throws Exception {
    WebUtilities _cut = new WebUtilities();
    // method under test
    java.lang.String actual = _cut.escapeForUrl("ABazc_");

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    assertEquals("ABazc_", actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.WebUtilities\'
   * Method Signature = 'escapeForUrl(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_exampleThree() throws Exception {
    WebUtilities _cut = new WebUtilities();
    // method under test
    java.lang.String actual = _cut.escapeForUrl("!(");

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    assertEquals("%21%28", actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.WebUtilities\'
   * Method Signature = 'escapeForUrl(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_exampleFour() throws Exception {
    WebUtilities _cut = new WebUtilities();
    // method under test
    java.lang.String actual = _cut.escapeForUrl("");

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    assertEquals("", actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.WebUtilities\'
   * Method Signature = 'escapeForUrl(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_exampleFive() throws Exception {
    WebUtilities _cut = new WebUtilities();
    // method under test
    java.lang.String actual = _cut.escapeForUrl(null);

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    assertEquals(null, actual);
  }
}
''')
            testClasses.put('8b091522-8139-4a7b-91ce-ed694d96910f', '''
import org.junit.Test;
import java.lang.Exception;
import com.groupbyinc.common.apache.commons.lang3.StringUtils;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.StringUtils\'
 * Method Signature = 'swapCase(java.lang.String):java.lang.String\'
 *
 * START Functionality Description
 *
 *       <p>Swaps the case of a String changing upper and title case to
 *       lower case, and lower case to upper case.</p>
 *      
 *       <ul>
 *        <li>Upper case character converts to Lower case</li>
 *        <li>Title case character converts to Lower case</li>
 *        <li>Lower case character converts to Upper case</li>
 *       </ul>
 *      
 *       <p>For a word based algorithm, see {@link org.apache.commons.lang3.text.WordUtils#swapCase(String)}.
 *       A {@code null} input String returns {@code null}.</p>
 *      
 *       <pre>
 *       StringUtils.swapCase(null)                 = null
 *       StringUtils.swapCase("")                   = ""
 *       StringUtils.swapCase("The dog has a BONE") = "tHE DOG HAS A bone"
 *       </pre>
 *      
 *       <p>NOTE: This method changed in Lang version 2.0.
 *       It no longer performs a word based algorithm.
 *       If you only use ASCII, you will notice no change.
 *       That functionality is available in org.apache.commons.lang3.text.WordUtils.</p>
 *      
 *       @param str  the String to swap case, may be null
 *       @return the changed String, {@code null} if null String input
 *      
 * END Functionality Description
 *
 * Doc ID: 8b091522-8139-4a7b-91ce-ed694d96910f
 * 
 */
public class StringUtilsTest {
  
  /**
   * C1:String length
          null
\t        0
\t        >1

\t  C2:hasLowerCase:    true,false

      C3:hasUpperCase:    true,false

      C4:firstLetterIsBig:  true,false
   */

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.StringUtils\'
   * Method Signature = 'swapCase(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_example() throws Exception {
    StringUtils _cut = new StringUtils();
    // method under test
    java.lang.String actual = _cut.swapCase("TAaaaBBB");

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    assertEquals("taAAAbbb", actual);
  }
  
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.StringUtils\'
   * Method Signature = 'swapCase(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_exampleOne() throws Exception {
    StringUtils _cut = new StringUtils();
    // method under test
    java.lang.String actual = _cut.swapCase("iahB");

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    assertEquals("IAHb", actual);
  }
  
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.StringUtils\'
   * Method Signature = 'swapCase(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_exampleTwo() throws Exception {
    StringUtils _cut = new StringUtils();
    // method under test
    java.lang.String actual = _cut.swapCase("Iaaaaaaa");

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    assertEquals("iAAAAAAA", actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.StringUtils\'
   * Method Signature = 'swapCase(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_exampleThree() throws Exception {
    StringUtils _cut = new StringUtils();
    // method under test
    java.lang.String actual = _cut.swapCase("AAAAAAA");

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    assertEquals("aaaaaaa", actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.StringUtils\'
   * Method Signature = 'swapCase(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_exampleFour() throws Exception {
    StringUtils _cut = new StringUtils();
    // method under test
    java.lang.String actual = _cut.swapCase("");

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    assertEquals("", actual);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.StringUtils\'
   * Method Signature = 'swapCase(java.lang.String):java.lang.String\'
   */
  @Test
  public void test_exampleFive() throws Exception {
    StringUtils _cut = new StringUtils();
    // method under test
    java.lang.String actual = _cut.swapCase(null);

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    assertEquals(null, actual);
  }
}
''')
            testClasses.put('20af3b70-4dce-4c28-be08-306c3ce4db25', '''
import org.junit.Test;
import java.lang.Exception;
import org.apache.openejb.math.util.MathUtils;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.MathUtils\'
 * Method Signature = 'factorialDouble(int):double\'
 *
 * START Functionality Description
 *
 *       Returns n!. Shorthand for <code>n</code> <a
 *       href="http://mathworld.wolfram.com/Factorial.html"> Factorial</a>, the
 *       product of the numbers <code>1,...,n</code> as a <code>double</code>.
 *       <p>
 *       <Strong>Preconditions</strong>:
 *       <ul>
 *       <li> <code>n >= 0</code> (otherwise
 *       <code>IllegalArgumentException</code> is thrown)</li>
 *       <li> The result is small enough to fit into a <code>double</code>. The
 *       largest value of <code>n</code> for which <code>n!</code> <
 *       Double.MAX_VALUE</code> is 170. If the computed value exceeds
 *       Double.MAX_VALUE, Double.POSITIVE_INFINITY is returned</li>
 *       </ul>
 *       </p>
 *      
 *       @param n argument
 *       @return <code>n!</code>
 *       @throws IllegalArgumentException if n < 0
 *      
 * END Functionality Description
 *
 * Doc ID: 20af3b70-4dce-4c28-be08-306c3ce4db25
 * 
 */
public class MathUtilsTest {

  /***
   * Partition:   Relation from n
   * B1 n<0          [-1...Integer.MinValue]
   * B2 n=0          [0]
   * B3 171<n>0      [1..170]
   * B4 n<171        [171..Integer.MaxValue]
   */

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.MathUtils\'
   * Method Signature = 'factorialDouble(int):double\'
   */
  @Test
  public void test_nB1() throws Exception {
    MathUtils _cut = new MathUtils();
    // method under test
    

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    try {
         double actual = _cut.factorialDouble(-5);
         fail();
    } catch (Throwable e) {
    }
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.MathUtils\'
   * Method Signature = 'factorialDouble(int):double\'
   */
  @Test
  public void test_nB2() throws Exception {
    MathUtils _cut = new MathUtils();
    // method under test
    double actual = _cut.factorialDouble(0);

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
     assertEquals(1, actual,0d);
  }
  
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.MathUtils\'
   * Method Signature = 'factorialDouble(int):double\'
   */
  @Test
  public void test_nB3() throws Exception {
    MathUtils _cut = new MathUtils();
    // method under test
    double actual = _cut.factorialDouble(10);

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
     assertEquals(3628800, actual,0d);
  }
  
  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.MathUtils\'
   * Method Signature = 'factorialDouble(int):double\'
   */
  @Test
  public void test_nB4() throws Exception {
    MathUtils _cut = new MathUtils();
    // method under test
    double actual = _cut.factorialDouble(171);

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
     assertEquals(Double.POSITIVE_INFINITY, actual,0d);
  }
}
''')
            testClasses.put('a4119c3b-91d0-4478-b278-6cef8af57480', '''
import org.junit.Test;
import java.util.Arrays;
import com.sibvisions.util.ArrayUtil;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.ArrayUtil\'
 * Method Signature = 'removeRange(double[],int,int):double[]\'
 *
 * START Functionality Description
 * 
 *       Removes all elements between fromIndex (inclusive) and toIndex (exclusive of given array).
 *      
 *       @param pSourceArray array in which to delete.
 *       @param pFromIndex   the first index.
 *       @param pToIndex     the last index (not included).
 *       @return returns a new array if needed.
 *      
 * END Functionality Description
 *
 * Doc ID: a4119c3b-91d0-4478-b278-6cef8af57480
 * 
 */
public class ArrayUtilTest {
    
  //Valid: länge>2 & index nicht gleich
  //unvalid: 
  //array länge{ 0,1,}
  //toIndex,FromIndey: {gleich unterschiedlich, toindex> länge}

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.ArrayUtil\'
   * Method Signature = 'removeRange(double[],int,int):double[]\'
   */
  @Test
  public void test_example0() throws Exception {
    // class under test
    ArrayUtil _cut = new ArrayUtil();
    // method under test
    double[] input = new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8,0.9, 1.0};
    
    double[] actual = _cut.removeRange(input,3,6);

    double[] expected = new double[]{0.1, 0.2,0.3,  0.7, 0.8,0.9, 1.0};
    
    assertArrayEquals(actual, expected, 0.1);
    }
  
  //toIndex > länge
  @Test
  public void test_example1() throws Exception {
    // class under test
    ArrayUtil _cut = new ArrayUtil();
    // method under test
    double[] input = new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8,0.9, 1.0};

    try {
         double[] actual = _cut.removeRange(input,3,12);
         fail();
    } catch (Throwable e) {
    }
    }
  
  // index ist gleich
  @Test
  public void test_example2() throws Exception {
    // class under test
    ArrayUtil _cut = new ArrayUtil();
    // method under test
    double[] input = new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8,0.9, 1.0};
    
    try {
         double[] actual = _cut.removeRange(input,3,3);
         fail();
    } catch (Throwable e) {
    }
  }

  //länge0  index=0
  @Test
  public void test_example3() throws Exception {
    // class under test
    ArrayUtil _cut = new ArrayUtil();
    // method under test
    double[] input = new double[]{};
    
    try {
         double[] actual = _cut.removeRange(input,0,0);
         fail();
    } catch (Throwable e) {
    }
  }
  
  
//länge1  index=0
  @Test
  public void test_example4() throws Exception {
    // class under test
    ArrayUtil _cut = new ArrayUtil();
    // method under test
    double[] input = new double[]{1};
    
    double[] actual = _cut.removeRange(input,0,1);
    double[] expected = new double[]{};
    boolean pass=false;
    if(Arrays.equals(actual, expected)||Arrays.equals(actual, null)){
    \tpass= true;
    }
      
    assertTrue(pass);

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    // e.g. assertEquals(expected, actual)
  }
  
//länge2  index=0-1
  @Test
  public void test_example5() throws Exception {
    // class under test
    ArrayUtil _cut = new ArrayUtil();
    // method under test
    double[] input = new double[]{1,2};
    
    double[] actual = _cut.removeRange(input,0,1);
    double[] expected = new double[]{2};
    boolean pass=false;
    if(Arrays.equals(actual, expected)||Arrays.equals(actual, null)){
    \tpass= true;
    }
      
    assertTrue(pass);

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    // e.g. assertEquals(expected, actual)
  }
}
''')
            testClasses.put('aa0f848f-cc98-4d0e-a866-62b8703834a5', '''
import org.junit.Test;
import java.lang.Exception;
import org.modeshape.schematic.internal.document.JsonReader;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.JsonReader\'
 * Method Signature = 'parseNumber(java.lang.String):java.lang.Number\'
 *
 * START Functionality Description
 *
 *       Parse the number represented by the supplied (unquoted) JSON field value.
 *      
 *       @param value the string representation of the value
 *       @return the number, or null if the value could not be parsed
 *      
 * END Functionality Description
 *
 * Doc ID: aa0f848f-cc98-4d0e-a866-62b8703834a5
 * 
 */
public class JsonReaderTest {

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.JsonReader\'
   * Method Signature = 'parseNumber(java.lang.String):java.lang.Number\'
   */
  
  //valid
  //eingabetyp:{mit E,int, mit.}
  
  
  
  @Test
\tpublic void test_example0() throws Exception {
\t\t// class under test
\t\tJsonReader _cut = new JsonReader();
\t\t// method under test
\t\tjava.lang.Number actual = _cut.parseNumber("5.2");
\t\tdouble expected = 5.2;
\t\tassertEquals((double) actual, expected, 0.1);

\t\t// Available assertion methods: org.junit.Assert.* and/or
\t\t// org.hamcrest.CoreMatchers.*
\t\t// e.g. assertEquals(expected, actual)
\t}

\t@Test
\tpublic void test_example1() throws Exception {
\t\t// class under test
\t\tJsonReader _cut = new JsonReader();
\t\t// method under test
\t\tjava.lang.Number actual = _cut.parseNumber("5e+2");
\t\tdouble expected = 500;
\t\tassertEquals((double) actual, expected,0.1);

\t\t// Available assertion methods: org.junit.Assert.* and/or
\t\t// org.hamcrest.CoreMatchers.*
\t\t// e.g. assertEquals(expected, actual)
\t}
\t
\t
\t@Test
\tpublic void test_example2() throws Exception {
\t\t// class under test
\t\tJsonReader _cut = new JsonReader();
\t\t// method under test
\t\tjava.lang.Number actual = _cut.parseNumber("1000");
\t\tint expected = 1000;
\t\tassertEquals((int) actual, expected,0.1);

\t\t// Available assertion methods: org.junit.Assert.* and/or
\t\t// org.hamcrest.CoreMatchers.*
\t\t// e.g. assertEquals(expected, actual)
\t}
\t
\t
\t 
\t
\t@Test
\tpublic void test_example3() throws Exception {
\t\t// class under test
\t\tJsonReader _cut = new JsonReader();
\t\t// method under test
\t\tjava.lang.Number actual = _cut.parseNumber("923377036854775807");
\t\tlong expected = 923377036854775807L;
\t\tassertEquals((long) actual, expected,0.1);

\t\t// Available assertion methods: org.junit.Assert.* and/or
\t\t// org.hamcrest.CoreMatchers.*
\t\t// e.g. assertEquals(expected, actual)
\t}
\t
\t
\t//kombi e mit . und-
\t@Test
\tpublic void test_example4() throws Exception {
\t\t// class under test
\t\tJsonReader _cut = new JsonReader();
\t\t// method under test
\t\tjava.lang.Number actual = _cut.parseNumber("-5.2e-2");
\t\tdouble expected = -0.052;
\t\tassertEquals((double) actual, expected,0.1);

\t\t// Available assertion methods: org.junit.Assert.* and/or
\t\t// org.hamcrest.CoreMatchers.*
\t\t// e.g. assertEquals(expected, actual)
\t}
\t
\t
}
''')
            testClasses.put('570386ad-99a1-43fb-b854-d1679e269d1e', '''
import org.junit.Test;
import java.lang.Exception;
import java.math.BigInteger;
import com.google.common.math.LongMath;
import static org.junit.Assert.*;


/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.LongMath\'
 * Method Signature = 'pow(long,int):long\'
 *
 * START Functionality Description
 *
 *     Returns {@code b} to the {@code k}th power. Even if the result overflows, it will be equal to
 *     {@code BigInteger.valueOf(b).pow(k).longValue()}. This implementation runs in {@code O(log k)}
 *     time.
 *    
 *     @throws IllegalArgumentException if {@code k < 0}
 *    
 * END Functionality Description
 *
 * Doc ID: 570386ad-99a1-43fb-b854-d1679e269d1e
 * 
 */
public class LongMathTest {

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.LongMath\'
   * Method Signature = 'pow(long,int):long\'
   */
  
  
  // ValidA: {Overflow, normal}
  // ValidB: {EXPO =0, EXPO&Base =0,Base=0}
  //unvalid: {exponent is negative}
  
  

  //normal
  @Test
  public void test_example0() throws Exception {
    // class under test
    LongMath _cut = new LongMath();
    // method under test
    
    
    long actual = _cut.pow(123456L,2);
    long expected= 15241383936L;
    
    
    assertEquals(expected, actual);

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    // e.g. assertEquals(expected, actual)
  }
  
//  negative expo
  @Test
  public void test_example1() throws Exception {
    // class under test
    LongMath _cut = new LongMath();
    // method under test

    try {
         long actual = _cut.pow(123456L,-2);
         fail();
    } catch (Throwable e) {
    }
  
    
    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    // e.g. assertEquals(expected, actual)
    
  
}
  
  //overflow
  @Test
  public void test_example2() throws Exception {
\t    // class under test
\t    LongMath _cut = new LongMath();
\t    // method under test
\t    
\t    
\t    long actual = _cut.pow(9223372036854775802L,2);
\t}
  
  //expo =0
  @Test
  public void test_example3() throws Exception {
    // class under test
    LongMath _cut = new LongMath();
    // method under test
    
    
    long actual = _cut.pow(123456L,0);
    long expected= 1L;
    
    
    assertEquals(expected, actual);

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    // e.g. assertEquals(expected, actual)
  }
  
  //expo&base =0
  @Test
  public void test_example4() throws Exception {
    // class under test
    LongMath _cut = new LongMath();
    // method under test
    
    
    long actual = _cut.pow(0L,0);
    long expected= 1L;
    
    
    assertEquals(expected, actual);

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    // e.g. assertEquals(expected, actual)
  }
  //base=0
  @Test
  public void test_example5() throws Exception {
    // class under test
    LongMath _cut = new LongMath();
    // method under test
    
    
    long actual = _cut.pow(0L,1237238);
    long expected= 0L;
    
    
    assertEquals(expected, actual);

    // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
    // e.g. assertEquals(expected, actual)
  }
}
''')
            testClasses.put('a86a5b98-58b6-44d1-99ec-a9f79a0f8c83', '''
import org.junit.Test;
import java.lang.Exception;
import tk.memin.dm.math.Arrays;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.Arrays\'
 * Method Signature = 'medianOf(double[][]):double\'
 *
 * START Functionality Description
 *
 * \t  Finds the median of a similarity matrix. It assumes the matrix is symmetric
 * \t  and it ignores the values on the diagonal.
 * \t  
 * \t  @param similarityMatrix
 * \t           A symmetric square matrix.
 * \t  @return Median of the values in the upper right triangle of the matrix,
 * \t          excluding the diagonal.
 * \t 
 * END Functionality Description
 *
 * Doc ID: a86a5b98-58b6-44d1-99ec-a9f79a0f8c83
 * 
 */
public class ArraysTest {

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.Arrays\'
   * Method Signature = 'medianOf(double[][]):double\'
   */
  
  //valid: 
//  länge: symetrisch
//  zahlen: {positiv,negativ,0}
  
  //invalid

  @Test
  public void test_example0() throws Exception {
    // class under test
    Arrays _cut = new Arrays();
    // method under test
    
    double[][] input= new double[][]{{0.1, 0.2, 0.3 ,0.4},{0.1, 0.2, 0.3, 0.4},{0.1, 0.2, 0.3 ,0.4},{0.1, 0.2, 0.3 ,0.4}};
    
    double actual = _cut.medianOf(input);
    
    double expected =0.4;

    assertEquals(expected, actual,0.1);
  }
  
  
  
//Test nicht symetrisch ,spalten>zeilen
  @Test
  public void test_example1() throws Exception {
    // class under test
    Arrays _cut = new Arrays();
    // method under test
    
    double[][] input= new double[][]{{0.1, 0.2, 0.3 ,0.4},{0.1, 0.2, 0.3,0.4},{0.1, 0.2, 0.3 ,0.4}};

    double actual = _cut.medianOf(input);
    
    double expected = 0.3;
    
    assertEquals(expected, actual,0.1);
  }

//Test mit ungleicher zeilen länge
  @Test
  public void test_example2() throws Exception {
    // class under test
    Arrays _cut = new Arrays();
    // method under test

    double[][] input= new double[][]{{0.1, 0.2, 0.3 ,0.4},{0.1, 0.2, 0.3},{0.1, 0.2, 0.3 ,0.4},{0.1, 0.2, 0.3 ,0.4}};

    // Test nicht symetrisch d.h. nicht gleich viele spalten und zeilen

    try {
         double actual = _cut.medianOf(input);
         fail();
    } catch (Throwable e) {
    }
  }
  
//  0
  public void test_example3() throws Exception {
\t    // class under test
\t    Arrays _cut = new Arrays();
\t    // method under test
\t    
\t    double[][] input= new double[][]{{0,-0.5},{1,22}};

\t    double actual = _cut.medianOf(input);
\t    
\t    double expected =0;

\t    assertEquals(expected, actual,0.1);
\t  }
  
  @Test  
  public void test_example4() throws Exception {
\t    // class under test
\t    Arrays _cut = new Arrays();
\t    // method under test
\t    
\t    double[][] input= new double[][]{{-0.1, 0, 0.3 ,0.4},{-0.1, 0, 0.3, 0.4},{-0.1, 0, 0.3 ,0.4},{-0.1, 0, 0.3 ,0.4}};

\t    double actual = _cut.medianOf(input);
\t    
\t    double expected = 0.4;

\t    assertEquals(expected, actual,0.1);
  }
}
''')
            testClasses.put('2114889a-fd89-4ab5-9a4c-f1062cb71545', '''
import org.junit.Test;
import java.lang.Exception;
import com.groupbyinc.flux.common.spatial4j.core.distance.DistanceUtils;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.DistanceUtils\'
 * Method Signature = 'distVincentyRAD(double,double,double,double):double\'
 *
 * START Functionality Description
 *
 *     Calculates the great circle distance using the Vincenty Formula, simplified for a spherical model. This formula
 *     is accurate for any pair of points. The equation
 *     was taken from <a href="http://en.wikipedia.org/wiki/Great-circle_distance">Wikipedia</a>.
 *     <p/>
 *     The arguments are in radians, and the result is in radians.
 *    
 * END Functionality Description
 *
 * Doc ID: 2114889a-fd89-4ab5-9a4c-f1062cb71545
 * 
 */
public class DistanceUtilsTest {  
  
// Partion über Punkte:
//  gleiche Punkte
//unterschiedliche Punkte
//A ->B  ==  B-> A  
  
  


//unterschiedliche Punkte
@Test
public void test_example0() throws Exception {
  // class under test
  DistanceUtils _cut = new DistanceUtils();
  // method under test
  // double actual = _cut.distVincentyRAD(double,double,double,double);

  // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
  // e.g. assertEquals(expected, actual)
  
 
  double l1 = 52.517;
  double b1= 13.40;
  double l2 = 35.70;
  double b2= 139.767;
  
  l1 = Math.toRadians(l1);
  b1 = Math.toRadians(b1);
  l2 = Math.toRadians(l2);
  b2 = Math.toRadians(b2);
  double actual= _cut.distVincentyRAD(l1, b1, l2, b2);
  double expected =80.212;
  expected =Math.toRadians(expected);
  assertEquals(expected, actual,0.1);

  
  
}


//A ->B  ==  B-> A  
@Test
public void test_example1() throws Exception {
// class under test
DistanceUtils _cut = new DistanceUtils();
// method under test
// double actual = _cut.distVincentyRAD(double,double,double,double);

// Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
// e.g. assertEquals(expected, actual)



// Punkte von Test 0 umgedreht
double l2 = 52.517;
double b2= 13.40;
double l1 = 35.70;
double b1= 139.767;

l1 = Math.toRadians(l1);
b1 = Math.toRadians(b1);
l2 = Math.toRadians(l2);
b2 = Math.toRadians(b2);
double actual= _cut.distVincentyRAD(l1, b1, l2, b2);
System.out.println(actual);

double l2b = 52.517;
double b2b= 13.40;
double l1b = 35.70;
double b1b= 139.767;

l1b = Math.toRadians(l1b);
b1b = Math.toRadians(b1b);
l2b = Math.toRadians(l2b);
b2b = Math.toRadians(b2b);
double actual2= _cut.distVincentyRAD(l1b, b1b, l2b, b2b);
System.out.println(actual2);


assertEquals(actual2, actual,0.1);



}


// Test gleicher Punkt
@Test
public void test_example2() throws Exception {
  // class under test
  DistanceUtils _cut = new DistanceUtils();
  // method under test
  // double actual = _cut.distVincentyRAD(double,double,double,double);

  // Available assertion methods: org.junit.Assert.* and/or org.hamcrest.CoreMatchers.* 
  // e.g. assertEquals(expected, actual)
   
 
  
 
  double l1 = 52.517;
  double b1= 13.40;
  double l2 = 52.517;
  double b2= 13.40;
  
  l1 = Math.toRadians(l1);
  b1 = Math.toRadians(b1);
  l2 = Math.toRadians(l2);
  b2 = Math.toRadians(b2);
  double actual= _cut.distVincentyRAD(l1, b1, l2, b2);
  double expected =0;
  expected =Math.toRadians(expected);
  assertEquals(expected, actual,0.1);

  
  
}
  
  
  
  
}
''')
            testClasses.put('d672e4b2-db58-40a8-812b-487938299463', '''
import org.junit.Test;
import java.lang.Exception;
import jodd.util.StringUtil;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.StringUtil\'
 * Method Signature = 'findCommonPrefix(java.lang.String[]):java.lang.String\'
 *
 * START Functionality Description
 *
 * \t  Finds common prefix for several strings. Returns an empty string if
 * \t  arguments do not have a common prefix.
 * \t 
 * END Functionality Description
 *
 * Doc ID: d672e4b2-db58-40a8-812b-487938299463
 * 
 */
public class StringUtilTest {

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.StringUtil\'
   * Method Signature = 'findCommonPrefix(java.lang.String[]):java.lang.String\'
   */
  @Test
  public void testNormal() throws Exception {
  StringUtil _cut = new StringUtil();
\tString[] test = new String[]{"Hallsss", "Halloxxxxxx", "Hallo"};
\tString actual = _cut.findCommonPrefix(test);
\t//System.out.println(_cut.findCommonPrefix(test) + "HALLO");
    assertEquals("Hall", actual);
  }
  
  @Test
  public void testOne() throws Exception {
  StringUtil _cut = new StringUtil();
\tString[] test = new String[]{"Hall"};
\tString actual = _cut.findCommonPrefix(test);
    assertEquals("Hall", actual);
  }
  
  @Test
  public void testNull() throws Exception {
  StringUtil _cut = new StringUtil();
\tString[] test = null;
    try {
         String actual = _cut.findCommonPrefix(test);
         fail();
    } catch (Throwable e) {
    }
  }
  
  @Test
  public void testEmpty() throws Exception {
  StringUtil _cut = new StringUtil();
\tString[] test = new String[]{""};
\tString actual = _cut.findCommonPrefix(test);
    assertEquals("", actual);
  }
  
  @Test
  public void testPartNull() throws Exception {
  StringUtil _cut = new StringUtil();
\tString[] test = new String[]{"Hallsss", "Halloxxxxxx", null};

    try {
         String actual = _cut.findCommonPrefix(test);
         fail();
    } catch (Throwable e) {
    }
  }
  
  @Test
  public void testSpecials() throws Exception {
  StringUtil _cut = new StringUtil();
\tString[] test = new String[]{"#", "#", "#"};
\tString actual = _cut.findCommonPrefix(test);
    assertEquals("#", actual);
  }
}
''')
            testClasses.put('d3d9ab9e-983b-49ce-9ad7-7ed49a6da957', '''
import org.junit.Test;
import java.lang.Exception;
import org.semanticweb.yars.nx.util.NxUtil;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.NxUtil' Method Signature =
 * 'removeDotSegments(java.lang.String):java.lang.String\'
 *
 * START Functionality Description
 *
 * Remove Dot segments from IRI path as described in RFC3987 5.3.2.4 e.g.
 * "/a/b/c/./../../g" becomes "/a/g" and "" becomes "/"
 * 
 * @param path
 *            IRI path to normalize
 * @return Normalized path
 * 
 *         END Functionality Description
 *
 *         Doc ID: d3d9ab9e-983b-49ce-9ad7-7ed49a6da957 
 */
public class NxUtilTest {

\t/**
\t * JUnit Test Case Example
\t *
\t *
\t * Class Under Test = 'com.XXX.adapter.NxUtil' Method Signature =
\t * 'removeDotSegments(java.lang.String):java.lang.String\'
\t */

\t@Test
\tpublic void testJumpBack() throws Exception {
NxUtil _cut = new NxUtil();
\t\t// method under test
\t\tString test = "/bla/blub/..";
\t\tString actual = _cut.removeDotSegments(test);
\t\tassertEquals("/bla/", actual);
\t}

\t@Test
\tpublic void testJumpForward() throws Exception {
NxUtil _cut = new NxUtil();
\t\t// method under test
\t\tString test = "/bla/./blub/";
\t\tString actual = _cut.removeDotSegments(test);
\t\tassertEquals("/bla/blub", actual);
\t}

\t@Test
\tpublic void testInputNull() throws Exception {
NxUtil _cut = new NxUtil();
\t\t// method under test
\t\tString test = null;
    try {
         String actual = _cut.removeDotSegments(test);
         fail();
    } catch (Throwable e) {
    }
\t}

\t@Test
\tpublic void testInputEmpty() throws Exception {
NxUtil _cut = new NxUtil();
\t\t// method under test
\t\tString test = "";
\t\tString actual = _cut.removeDotSegments(test);
\t\tassertEquals("/", actual);
\t}



}
''')
            testClasses.put('d654f45f-1a77-42fc-95ea-d517a7c2cf69', '''
import org.junit.Test;
import java.lang.Exception;
import com.sun.grizzly.util.http.HttpMessages;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.HttpMessages\'
 * Method Signature = 'filter(java.lang.String):java.lang.String\'
 *
 * START Functionality Description
 *
 *       Filter the specified message string for characters that are sensitive
 *       in HTML.  This avoids potential attacks caused by including JavaScript
 *       codes in the request URL that is often reported in error messages.
 *      
 *       @param message The message string to be filtered
 *      
 * END Functionality Description
 *
 * Doc ID: d654f45f-1a77-42fc-95ea-d517a7c2cf69
 * 
 */
public class HttpMessagesTest {

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.HttpMessages\'
   * Method Signature = 'filter(java.lang.String):java.lang.String\'
   */
  
  @Test
  public void testValidString() throws Exception {
    // class under test
    HttpMessages _cut = new HttpMessages();
    String test = "hallo";
    String actual = _cut.filter(test); 
    assertEquals(test, actual);
  }
  
  @Test
  public void testNull() throws Exception {
    // class under test
    HttpMessages _cut = new HttpMessages();
    String test = "null";
    String actual = _cut.filter(test); 
    assertEquals(test, actual);
  }
  
  @Test
  public void testEmpty() throws Exception {
    // class under test
    HttpMessages _cut = new HttpMessages();
    String test = "";
    String actual = _cut.filter(test); 
    assertEquals(test, actual);
  }
  
  
  @Test
  public void testEquals() throws Exception {
    // class under test
    HttpMessages _cut = new HttpMessages();
    String test = ">";
    String actual = _cut.filter(test); 
    assertEquals("&gt;", actual);
  }
  
  @Test
  public void testAnd() throws Exception {
    // class under test
    HttpMessages _cut = new HttpMessages();
    String test = "&";
    String actual = _cut.filter(test); 
    assertEquals("&amp;", actual);
  }
  
}
''')
            testClasses.put('d083e652-739d-44a5-b435-07f0289893eb', '''
import org.junit.Test;
import java.lang.Exception;
import org.sakaiproject.tool.assessment.util.StringParseUtils;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.StringParseUtils' Method Signature =
 * 'getLastNameFromName(java.lang.String):java.lang.String\'
 *
 * START Functionality Description
 *
 * utility.
 * 
 * @param name
 *            full name
 * @return (hopefully) last name
 * 
 *         END Functionality Description
 *
 *         Doc ID: d083e652-739d-44a5-b435-07f0289893eb 
 */
public class StringParseUtilsTest {

\t/**
\t * JUnit Test Case Example
\t *
\t *
\t * Class Under Test = 'com.XXX.adapter.StringParseUtils' Method
\t * Signature = 'getLastNameFromName(java.lang.String):java.lang.String\'
\t */

\t@Test
\tpublic void testInputNull() throws Exception {
StringParseUtils _cut = new StringParseUtils();
\t\t// method under test
\t\tString fullName = null;
    try {
         String actual = _cut.getLastNameFromName(fullName);
         fail();
    } catch (Throwable e) {
    }
\t}

\t@Test
\tpublic void testInputEmpty() throws Exception {
StringParseUtils _cut = new StringParseUtils();
\t\t// method under test
\t\tString fullName = "";
\t\tString actual = _cut.getLastNameFromName(fullName);
\t\tassertEquals("", actual);
\t}

\t@Test
\tpublic void testInputOneString() throws Exception {
StringParseUtils _cut = new StringParseUtils();
\t\t// method under test
\t\tString fullName = "Marko";
\t\tString actual = _cut.getLastNameFromName(fullName);
\t\tassertEquals("Marko", actual);
\t}
\t
\t@Test
\tpublic void testInputExactTwoString() throws Exception {
StringParseUtils _cut = new StringParseUtils();
\t\t// method under test
\t\tString fullName = "Marko Jeftic";
\t\tString actual = _cut.getLastNameFromName(fullName);
\t\tassertNotEquals("Marko", actual);
\t}
\t
\t@Test
\tpublic void testInputManyManyString() throws Exception {
StringParseUtils _cut = new StringParseUtils();
\t\t// method under test
\t\tString fullName = "Marko Jeftic Jeftic Jeftic Jeftic Paul Franz";
\t\tString actual = _cut.getLastNameFromName(fullName);
\t\tassertEquals("Franz", actual);
\t}
\t
\t@Test
\tpublic void testOutPutLenght() throws Exception {
StringParseUtils _cut = new StringParseUtils();
\t\t// method under test
\t\tString fullName = "Marko Jeftic";
\t\tString actual = _cut.getLastNameFromName(fullName);
\t\tassertEquals(6, actual.length());
\t}
}
''')
            testClasses.put('ca07ef4f-5576-4e8b-8328-6e7bd648ff03', '''
import org.junit.Test;
import java.lang.Exception;
import org.keycloak.models.utils.Base32;
import static org.junit.Assert.*;

/**
 * JUnit4 Test Class
 *
 * Class Under Test = 'com.XXX.adapter.Base32\'
 * Method Signature = 'encode(byte[]):java.lang.String\'
 *
 * START Functionality Description
 *
 *       Encodes byte array to Base32 String.
 *      
 *       @param bytes Bytes to encode.
 *       @return Encoded byte array <code>bytes</code> as a String.
 *      
 *      
 * END Functionality Description
 *
 * Doc ID: ca07ef4f-5576-4e8b-8328-6e7bd648ff03
 * 
 */
public class Base32Test {

  /**
   * JUnit Test Case Example 
   *
   *
   * Class Under Test = 'com.XXX.adapter.Base32\'
   * Method Signature = 'encode(byte[]):java.lang.String\'
   */
  

  
  //Length = 32
  //Type = String
  
  //Input paramters
  @Test
  public void testStringLength() throws Exception {
  byte[] base32Lookup = new byte[]{ 26, 27, 28, 29, 30, 31, -1,
      -1, -1, -1, -1, -1, -1, -1,
      -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
      15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
      -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
      15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25
  };  
  
    Base32 _cut = new Base32();
    String value = new String(base32Lookup);
    
    String actual = _cut.encode(base32Lookup);
    
    assertTrue(value.length() < actual.length());
  }
  
  @Test
  public void testIsString() throws Exception {
  byte[] base32Lookup = new byte[]{ 26, 27, 28, 29, 30, 31, -1,
      -1, -1, -1, -1, -1, -1, -1,
      -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
      15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
      -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
      15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25
  };  
  
    Base32 _cut = new Base32();
    
    String actual = _cut.encode(base32Lookup);
    
    assertTrue(actual instanceof String);
  }
  
  @Test
  public void testNull() throws Exception {
    Base32 _cut = new Base32();
    
    try {
         String actual = _cut.encode(null);
         fail();
    } catch (Throwable e) {
    }
  }
  
  @Test
  public void testIsEmpty() throws Exception {
    Base32 _cut = new Base32();
    byte[] testByteArray = new byte[]{};
    
    String actual = _cut.encode(testByteArray);
    
    assertEquals("", actual);
  }
  
}
''')
    }
}
