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
package de.uni_mannheim.swt.lasso.engine;

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Marcus Kessel
 */
public class LassoUtilsTest {

    @Test
    public void test_bc2JavaSignature() {
        String bc = "org/glassfish/tyrus/core/Base64Utils.decode([B)[B";

        System.out.println(LassoUtils.bc2JavaSignature(bc));
    }

    @Test
    public void test_compactUUID() {
        assertThat(LassoUtils.compactUUID("f78613e9-8649-4c42-bffc-c3e9e8438bad"), is("f78613e986494c42bffcc3e9e8438bad"));
    }

    @Test
    public void test_decompactUUID() {
        assertThat(LassoUtils.decompactUUID("f78613e986494c42bffcc3e9e8438bad"), is("f78613e9-8649-4c42-bffc-c3e9e8438bad"));
    }

    @Disabled
    @Test
    public void test_resolveJarFromMavenRepository() throws IOException {
        File lassoWork = new File(SystemUtils.getUserHome(), "lasso-work");
        Workspace workspace = new Workspace();
        workspace.setLassoRoot(lassoWork);

        CodeUnit implementation = new CodeUnit();
        implementation.setGroupId("commons-lang");
        implementation.setArtifactId("commons-lang");
        implementation.setVersion("2.6");

        File jarFile = LassoUtils.resolveJarFromMavenRepository(workspace, implementation);

        assertTrue(jarFile.exists());

        System.out.println(jarFile.getCanonicalPath());
    }

    @Disabled
    @Test
    public void test_resolveJarFromMavenRepository_bug116() throws IOException {
        File lassoWork = new File(SystemUtils.getUserHome(), "lasso-work");
        Workspace workspace = new Workspace();
        workspace.setLassoRoot(lassoWork);

        CodeUnit implementation = new CodeUnit();
        implementation.setGroupId("org.eclipse.persistence");
        implementation.setArtifactId("org.eclipse.persistence.core");
        implementation.setVersion("2.7.4");

        File jarFile = LassoUtils.resolveJarFromMavenRepository(workspace, implementation);

        assertTrue(jarFile.exists());

        System.out.println(jarFile.getCanonicalPath());
    }

    @Test
    public void test_clean_desc() {
        assertThat(LassoUtils.shortenByteCodeNames("(I[BLcom/github/megatronking/stringfog/lib/Base64$1;)V"),
                equalTo("(I[BLBase64$1;)V"));

        assertThat(LassoUtils.shortenByteCodeNames("(I[BLcom/github/megatronking/stringfog/lib/Base64$1;Lcom/github/megatronking/stringfog/lib/Base64$1;)V"),
                equalTo("(I[BLBase64$1;LBase64$1;)V"));

        assertThat(LassoUtils.shortenByteCodeNames("(I[BLcom/github/megatronking/stringfog/lib/Base64$1;Lcom/github/megatronking/stringfog/lib/Base64$1;)Lcom/github/megatronking/stringfog/lib/Base64$1;"),
                equalTo("(I[BLBase64$1;LBase64$1;)LBase64$1;"));
    }
}
