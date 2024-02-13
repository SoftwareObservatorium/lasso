/*
 * This file was automatically generated by EvoSuite
 * Tue Mar 02 18:16:41 GMT 2021
 */

package net.sf.okapi.common;

import org.junit.Test;
import static org.junit.Assert.*;
import net.sf.okapi.common.Base64;

public class Base64Test {

  @Test(timeout = 4000)
  public void test0()  throws Throwable  {
      byte[] byteArray0 = Base64.decode("");
  }

  @Test(timeout = 4000)
  public void test1()  throws Throwable  {
      // Undeclared exception!
      try { 
        Base64.decode("~jx-@|Unho}!aT");
      } catch(IllegalArgumentException e) {
         //
         // Illegal base64 character 7e
         //
      }
  }

  @Test(timeout = 4000)
  public void test2()  throws Throwable  {
      // Undeclared exception!
      try { 
        Base64.decode((String) null);
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
      }
  }

  @Test(timeout = 4000)
  public void test3()  throws Throwable  {
      byte[] byteArray0 = Base64.decode("jkuY");
  }

  @Test(timeout = 4000)
  public void test4()  throws Throwable  {
      Base64 base64_0 = new Base64();
  }
}
