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
 * Wed Jul 19 14:52:57 GMT 2023
 */

package HumanEval_23_strlen;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.evosuite.runtime.EvoAssertions.*;
import HumanEval_23_strlen.Problem;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.evosuite.runtime.annotation.EvoSuiteClassExclude;
import org.evosuite.runtime.sandbox.Sandbox;
import org.evosuite.runtime.sandbox.Sandbox.SandboxMode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true) 
public class Problem_0_Test {

  @org.junit.Rule
  public org.evosuite.runtime.vnet.NonFunctionalRequirementRule nfr = new org.evosuite.runtime.vnet.NonFunctionalRequirementRule();

  private static final java.util.Properties defaultProperties = (java.util.Properties) java.lang.System.getProperties().clone(); 

  private org.evosuite.runtime.thread.ThreadStopper threadStopper =  new org.evosuite.runtime.thread.ThreadStopper (org.evosuite.runtime.thread.KillSwitchHandler.getInstance(), 3000);


  @BeforeClass
  public static void initEvoSuiteFramework() { 
    org.evosuite.runtime.RuntimeSettings.className = "HumanEval_23_strlen.Problem"; 
//    org.evosuite.runtime.GuiSupport.initialize(); 
    org.evosuite.runtime.RuntimeSettings.maxNumberOfThreads = 100; 
    org.evosuite.runtime.RuntimeSettings.maxNumberOfIterationsPerLoop = 10000; 
    org.evosuite.runtime.RuntimeSettings.mockSystemIn = true; 
    org.evosuite.runtime.RuntimeSettings.sandboxMode = org.evosuite.runtime.sandbox.Sandbox.SandboxMode.RECOMMENDED; 
    org.evosuite.runtime.sandbox.Sandbox.initializeSecurityManagerForSUT(); 
    org.evosuite.runtime.classhandling.JDKClassResetter.init();
    setSystemProperties();
    initializeClasses();
    org.evosuite.runtime.Runtime.getInstance().resetRuntime(); 
  } 

  @AfterClass
  public static void clearEvoSuiteFramework(){ 
    Sandbox.resetDefaultSecurityManager(); 
    java.lang.System.setProperties((java.util.Properties) defaultProperties.clone()); 
  } 

  @Before
  public void initTestCase(){ 
    threadStopper.storeCurrentThreads();
    threadStopper.startRecordingTime();
    org.evosuite.runtime.jvm.ShutdownHookHandler.getInstance().initHandler(); 
    org.evosuite.runtime.sandbox.Sandbox.goingToExecuteSUTCode(); 
    setSystemProperties(); 
    org.evosuite.runtime.GuiSupport.setHeadless(); 
    org.evosuite.runtime.Runtime.getInstance().resetRuntime(); 
    org.evosuite.runtime.agent.InstrumentingAgent.activate(); 
  } 

  @After
  public void doneWithTestCase(){ 
    threadStopper.killAndJoinClientThreads();
    org.evosuite.runtime.jvm.ShutdownHookHandler.getInstance().safeExecuteAddedHooks(); 
    org.evosuite.runtime.classhandling.JDKClassResetter.reset(); 
    resetClasses(); 
    org.evosuite.runtime.sandbox.Sandbox.doneWithExecutingSUTCode(); 
    org.evosuite.runtime.agent.InstrumentingAgent.deactivate(); 
    org.evosuite.runtime.GuiSupport.restoreHeadlessMode(); 
  } 

  public static void setSystemProperties() {
 
    java.lang.System.setProperties((java.util.Properties) defaultProperties.clone()); 
    java.lang.System.setProperty("user.dir", "/usr/src/mymaven/5842af23-51d1-4dcc-8398-daf8748aa914"); 
    java.lang.System.setProperty("java.io.tmpdir", "/tmp"); 
  }

  private static void initializeClasses() {
    org.evosuite.runtime.classhandling.ClassStateSupport.initializeClasses(Problem_0_Test.class.getClassLoader() ,
      "HumanEval_23_strlen.Problem"
    );
  } 

  private static void resetClasses() {
    org.evosuite.runtime.classhandling.ClassResetter.getInstance().setClassLoader(Problem_0_Test.class.getClassLoader()); 

    org.evosuite.runtime.classhandling.ClassStateSupport.resetClasses(
      "HumanEval_23_strlen.Problem"
    );
  }

  //Test case number: 0
  /*
   * 6 covered goals:
   * Goal 1. HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J: root-Branch
   * Goal 2. Branch HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J: root-Branch in context: HumanEval_23_strlen.Problem:strlen(Ljava/lang/String;)J
   * Goal 3. [Output]: HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J:Zero
   * Goal 4. HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J: Line 17
   * Goal 5. [METHOD] HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J
   * Goal 6. [METHODNOEX] HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J
   */

  @Test(timeout = 4000)
  public void test0()  throws Throwable  {
      long long0 = Problem.strlen("");
      assertEquals(0L, long0);
  }

  //Test case number: 1
  /*
   * 5 covered goals:
   * Goal 1. HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J: root-Branch
   * Goal 2. Branch HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J: root-Branch in context: HumanEval_23_strlen.Problem:strlen(Ljava/lang/String;)J
   * Goal 3. strlen(Ljava/lang/String;)J_java.lang.NullPointerException_IMPLICIT
   * Goal 4. HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J: Line 17
   * Goal 5. [METHOD] HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J
   */

  @Test(timeout = 4000)
  public void test1()  throws Throwable  {
      // Undeclared exception!
      try { 
        Problem.strlen((String) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("HumanEval_23_strlen.Problem", e);
      }
  }

  //Test case number: 2
  /*
   * 41 covered goals:
   * Goal 1. HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J: root-Branch
   * Goal 2. HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V: I3 Branch 1 IFNE L20 - false
   * Goal 3. HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V: I8 Branch 2 IFEQ L20 - true
   * Goal 4. HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V: I16 Branch 3 IFNE L21 - false
   * Goal 5. HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V: I21 Branch 4 IFEQ L21 - true
   * Goal 6. HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V: I29 Branch 5 IFNE L22 - false
   * Goal 7. HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V: I34 Branch 6 IFEQ L22 - true
   * Goal 8. Branch HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V: I8 Branch 2 IFEQ L20 - true in context: HumanEval_23_strlen.Problem:main([Ljava/lang/String;)V
   * Goal 9. Branch HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V: I3 Branch 1 IFNE L20 - false in context: HumanEval_23_strlen.Problem:main([Ljava/lang/String;)V
   * Goal 10. Branch HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V: I16 Branch 3 IFNE L21 - false in context: HumanEval_23_strlen.Problem:main([Ljava/lang/String;)V
   * Goal 11. Branch HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V: I21 Branch 4 IFEQ L21 - true in context: HumanEval_23_strlen.Problem:main([Ljava/lang/String;)V
   * Goal 12. Branch HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V: I34 Branch 6 IFEQ L22 - true in context: HumanEval_23_strlen.Problem:main([Ljava/lang/String;)V
   * Goal 13. Branch HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V: I29 Branch 5 IFNE L22 - false in context: HumanEval_23_strlen.Problem:main([Ljava/lang/String;)V
   * Goal 14. HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V: Line 20
   * Goal 15. HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V: Line 21
   * Goal 16. HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V: Line 22
   * Goal 17. HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V: Line 23
   * Goal 18. HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J: Line 17
   * Goal 19. [METHOD] HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V
   * Goal 20. [METHODNOEX] HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V
   * Goal 21. Weak Mutation 0: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:20 - InsertUnaryOp Negation
   * Goal 22. Weak Mutation 1: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:20 - ReplaceComparisonOperator != -> ==
   * Goal 23. Weak Mutation 2: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:20 - ReplaceConstant - 0 -> 1
   * Goal 24. Weak Mutation 3: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:20 - ReplaceConstant - 0 -> -1
   * Goal 25. Weak Mutation 4: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:20 - ReplaceComparisonOperator == -> -2
   * Goal 26. Weak Mutation 7: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:21 - InsertUnaryOp Negation
   * Goal 27. Weak Mutation 8: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:21 - ReplaceComparisonOperator != -> ==
   * Goal 28. Weak Mutation 9: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:21 - ReplaceConstant - x -> 
   * Goal 29. Weak Mutation 10: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:21 - ReplaceConstant - 1 -> 0
   * Goal 30. Weak Mutation 11: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:21 - ReplaceConstant - 1 -> -1
   * Goal 31. Weak Mutation 12: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:21 - ReplaceConstant - 1 -> 2
   * Goal 32. Weak Mutation 13: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:21 - ReplaceComparisonOperator == -> -2
   * Goal 33. Weak Mutation 16: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:22 - InsertUnaryOp Negation
   * Goal 34. Weak Mutation 17: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:22 - ReplaceComparisonOperator != -> ==
   * Goal 35. Weak Mutation 18: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:22 - ReplaceConstant - asdasnakj -> 
   * Goal 36. Weak Mutation 19: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:22 - ReplaceConstant - 9 -> 0
   * Goal 37. Weak Mutation 20: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:22 - ReplaceConstant - 9 -> 1
   * Goal 38. Weak Mutation 21: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:22 - ReplaceConstant - 9 -> -1
   * Goal 39. Weak Mutation 22: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:22 - ReplaceConstant - 9 -> 8
   * Goal 40. Weak Mutation 23: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:22 - ReplaceConstant - 9 -> 10
   * Goal 41. Weak Mutation 24: HumanEval_23_strlen.Problem.main([Ljava/lang/String;)V:22 - ReplaceComparisonOperator == -> -2
   */

  @Test(timeout = 4000)
  public void test2()  throws Throwable  {
      String[] stringArray0 = new String[2];
      Problem.main(stringArray0);
      assertEquals(2, stringArray0.length);
  }

  //Test case number: 3
  /*
   * 6 covered goals:
   * Goal 1. HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J: root-Branch
   * Goal 2. Branch HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J: root-Branch in context: HumanEval_23_strlen.Problem:strlen(Ljava/lang/String;)J
   * Goal 3. [Output]: HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J:Positive
   * Goal 4. HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J: Line 17
   * Goal 5. [METHOD] HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J
   * Goal 6. [METHODNOEX] HumanEval_23_strlen.Problem.strlen(Ljava/lang/String;)J
   */

  @Test(timeout = 4000)
  public void test3()  throws Throwable  {
      long long0 = Problem.strlen("&ekS+$,5]2&0qH:g=");
      assertEquals(17L, long0);
  }

  //Test case number: 4
  /*
   * 5 covered goals:
   * Goal 1. HumanEval_23_strlen.Problem.<init>()V: root-Branch
   * Goal 2. Branch HumanEval_23_strlen.Problem.<init>()V: root-Branch in context: 
   * Goal 3. HumanEval_23_strlen.Problem.<init>()V: Line 10
   * Goal 4. [METHOD] HumanEval_23_strlen.Problem.<init>()V
   * Goal 5. [METHODNOEX] HumanEval_23_strlen.Problem.<init>()V
   */

  @Test(timeout = 4000)
  public void test4()  throws Throwable  {
      Problem problem0 = new Problem();
  }
}
