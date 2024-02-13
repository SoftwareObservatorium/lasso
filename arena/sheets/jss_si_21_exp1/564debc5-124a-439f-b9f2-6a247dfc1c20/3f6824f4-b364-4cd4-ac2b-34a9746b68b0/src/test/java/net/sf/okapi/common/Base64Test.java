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
