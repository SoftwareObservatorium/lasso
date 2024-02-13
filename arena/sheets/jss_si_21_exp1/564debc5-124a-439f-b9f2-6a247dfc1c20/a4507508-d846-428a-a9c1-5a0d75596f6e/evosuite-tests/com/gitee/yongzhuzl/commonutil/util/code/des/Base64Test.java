/*
 * This file was automatically generated by EvoSuite
 * Tue Mar 02 18:19:14 GMT 2021
 */

package com.gitee.yongzhuzl.commonutil.util.code.des;

import org.junit.Test;
import static org.junit.Assert.*;
import com.gitee.yongzhuzl.commonutil.util.code.des.Base64;

public class Base64Test {

  @Test(timeout = 4000)
  public void test0()  throws Throwable  {
      // Undeclared exception!
      try { 
        Base64.decode("==");
      } catch(StringIndexOutOfBoundsException e) {
      }
  }

  @Test(timeout = 4000)
  public void test1()  throws Throwable  {
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
  public void test2()  throws Throwable  {
      // Undeclared exception!
      try { 
        Base64.decode(" oF%KKd:&8Yt}~H'");
      } catch(RuntimeException e) {
         //
         // unexpected code: %
         //
      }
  }

  @Test(timeout = 4000)
  public void test3()  throws Throwable  {
      byte[] byteArray0 = Base64.decode("");
  }

  @Test(timeout = 4000)
  public void test4()  throws Throwable  {
      byte[] byteArray0 = Base64.decode("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/");
  }

  @Test(timeout = 4000)
  public void test5()  throws Throwable  {
      Base64 base64_0 = new Base64();
  }

  @Test(timeout = 4000)
  public void test6()  throws Throwable  {
      byte[] byteArray0 = Base64.decode("gw=uZ12qe9}'y-");
  }

  @Test(timeout = 4000)
  public void test7()  throws Throwable  {
      byte[] byteArray0 = Base64.decode("gXw=uZ12qe}'y-");
  }

  @Test(timeout = 4000)
  public void test8()  throws Throwable  {
      // Undeclared exception!
      try { 
        Base64.decode("{U!l2yHj'P");
      } catch(RuntimeException e) {
         //
         // unexpected code: {
         //
      }
  }
}
