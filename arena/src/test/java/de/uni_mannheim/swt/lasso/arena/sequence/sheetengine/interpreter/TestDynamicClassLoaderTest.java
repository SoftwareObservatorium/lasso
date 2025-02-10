package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * @author Marcus Kessel
 */
public class TestDynamicClassLoaderTest {

    private static final Logger LOG = LoggerFactory.getLogger(TestDynamicClassLoaderTest.class);

    private MavenRepository mavenRepository;

    public MavenRepository getMavenRepository() {
        // FIXME update
        if (mavenRepository == null) {
            String mavenRepoUrl = NexusInstance.LOCAL_URL;
            File localRepo = new File("/tmp/lalalamvn/local-repo");

            DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
            this.mavenRepository = new MavenRepository(resolver);
        }

        return mavenRepository;
    }

    @Test
    public void test_compileAndLoad() throws IOException, ClassNotFoundException {
        @Language("Java")
        String sourceCode = """
public class Base64Impl {
    public byte[] encode(byte[] bytes) {
        int len = bytes.length;
        int paddingLen = 3 - (len % 3);
        if (paddingLen == 3) paddingLen = 0;

        byte[] result = new byte[(len + paddingLen) * 4 / 3];
        for (int i = 0, j = 0; i < len;) {
            int value = ((bytes[i] & 0xFF) << 8) | (bytes[i + 1] & 0xFF);
            result[j++] = (byte) ('A' + (value >> 18));
            result[j++] = (byte) ('A' + ((value & 0x3F00) >> 12));
            value <<= 4;
            if (i + 2 < len) {
                value |= bytes[i + 2] & 0xFF;
            }
            result[j++] = (byte) ('A' + (value >> 12));
            i += 1 + (i + 2 < len ? 1 : 0);
        }

        return result;
    }

    public byte[] decode(String string) {
        int len = string.length();
        if ((len % 4 != 0)) throw new RuntimeException("Invalid base64 string");

        byte[] bytes = new byte[len / 4 * 3];
        for (int i = 0, j = 0; i < len;) {
            int value = ('A' & string.charAt(i++));
            value = (value - 'A') << 18;
            value += ('A' & string.charAt(i++)) << 12;
            value += ('A' & string.charAt(i++)) << 6;
            value += ('A' & string.charAt(i++));
            bytes[j++] = (byte) ((value >> 16) & 0xFF);
            if (i < len) {
                bytes[j++] = (byte) ((value >> 8) & 0xFF);
            }
            if (i < len) {
                bytes[j++] = (byte) (value & 0xFF);
            }
        }

        return bytes;
    }
}
                """;

        ClassUnderTest classUnderTest = DynamicClassLoader.loadBySource(sourceCode, "llm");

        CandidatePool pool = new CandidatePool(getMavenRepository(), new ArrayList<>(Arrays.asList(classUnderTest)));
        pool.initProjects();

        Class clazz = classUnderTest.getProject().getContainer().loadClass(classUnderTest.getClassName());

        assertNotNull(clazz);
        assertEquals(classUnderTest.getClassName(), clazz.getCanonicalName());
    }
}
