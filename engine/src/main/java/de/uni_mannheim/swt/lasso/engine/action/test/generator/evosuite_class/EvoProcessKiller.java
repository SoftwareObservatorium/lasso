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
package de.uni_mannheim.swt.lasso.engine.action.test.generator.evosuite_class;

import de.uni_mannheim.swt.lasso.engine.action.maven.event.ProcessKiller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Simple process killer for EvoSuite client processes (only Linux-like killing supported).
 *
 * @author Marcus Kessel
 */
public class EvoProcessKiller {

    private static final Logger LOG = LoggerFactory
            .getLogger(EvoProcessKiller.class);

    public static void kill9(String fqName) throws IOException {
        File tempScript = createBashCommand(fqName);
        ProcessKiller.kill9(tempScript);
    }

    /**
     * Example process
     *
     * <pre>
     *     swtlasso  715576  203  3.1 6677536 1025844 ?     Sl   09:58   2:51 /docker-java-home/bin/java -XX:ErrorFile=hs_err_EvoSuite_client_p16518_t1642413518714 -Devosuite.log.appender=CLIENT -Dmaster_log_port=42123 -ea:... -cp /tmp/EvoSuite_pathingJar4277278930822710725.jar -DCP_file_path=/tmp/EvoSuite_classpathFile6570397231839748739.txt -Dspawn_process_manager_port=37635 -Dprocess_communication_port=16518 -Dinline=true -Djava.awt.headless=true -Dlogback.configurationFile=logback-ctg-entry.xml -Dlog4j.configuration=SUT.log4j.properties -Djava.library.path=lib -XX:MaxJavaStackTraceDepth=1000000 -XX:+StartAttachListener -Dnum_parallel_clients=1 -DTARGET_CLASS=gdv.xport.util.CsvFormatter -Dtest_comments=true -Dignore_missing_statistics=true -Dspawn_process_manager_port=37635 -Djunit_tests=true -Dsearch_budget=120 -Dconfiguration_id=0 -Dtest_scaffolding=false -Djunit_suffix=_0_Test -Dstopping_condition=MaxTime -Dinline=true -Duse_separate_classloader=false -Doutput_variables=configuration_id,TARGET_CLASS,criterion,Coverage,Total_Goals,Covered_Goals,Lines,Covered_Lines,LineCoverage,Statements_Executed,Total_Branches,Covered_Branches,BranchCoverage,CBranchCoverage,Total_Methods,Covered_Methods,Mutants,WeakMutationScore,MutationScore,Size,Result_Size,Length,Result_Length,Total_Time -Dcriterion=LINE:BRANCH:EXCEPTION:WEAKMUTATION:OUTPUT:METHOD:METHODNOEXCEPTION:CBRANCH -DCP_file_path=/tmp/EvoSuite_classpathFile4110453398250474123.txt -Dshow_progress=false -Dassertions=true -Xmx2048M -Dstrategy=MOSuite -Dselection_function=RANK_CROWD_DISTANCE_TOURNAMENT -Dalgorithm=DYNAMOSA -DTARGET_CLASS=gdv.xport.util.CsvFormatter -DPROJECT_PREFIX= org.evosuite.ClientProcess Client-0
     * </pre>
     *
     * @param fqName
     * @return
     * @throws IOException
     */
    public static File createBashCommand(String fqName) throws IOException {
        File tempScript = File.createTempFile("lasso_bash", null);

        try (Writer streamWriter = new OutputStreamWriter(new FileOutputStream(
                tempScript));
             PrintWriter printWriter = new PrintWriter(streamWriter)) {
            printWriter.println("#!/bin/bash");
            printWriter.println("ps aux | grep \"java\"");
            // sort by earliest start time, return first process only
            printWriter.println(
                    String.format("PROC_PID=$(ps aux --sort=start | grep \"org.evosuite.ClientProcess\" | grep \"%s\" | grep -v 'grep' | grep -v 'tail' | awk '{print $2}' | head -n 1)", fqName));
            printWriter.println("echo \"Killing $PROC_PID\"");
            printWriter.println("kill -9 $PROC_PID");
        }

        return tempScript;
    }
}
