package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import com.github.javaparser.JavaParser;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

/**
 *
 * @author Marcus Kessel
 */
public class DynamicClassLoader {

    public static ClassUnderTest loadBySource(String sourceCode, String defaultPkg) throws IOException {
        CodeUnit codeUnit = DynamicClassLoader.parse(sourceCode, defaultPkg);
        URL url = DynamicClassLoader.dynamicClass(codeUnit);

        ClassUnderTest classUnderTest = CutUtils.createExample(codeUnit);
        classUnderTest.setUrls(Arrays.asList(url));

        return classUnderTest;
    }

    public static ClassUnderTest loadBySource(CodeUnit codeUnit) throws IOException {
        URL url = DynamicClassLoader.dynamicClass(codeUnit);

        ClassUnderTest classUnderTest = CutUtils.createExample(codeUnit);
        classUnderTest.setUrls(Arrays.asList(url));

        return classUnderTest;
    }

    public static URL dynamicClass(CodeUnit codeUnit) throws IOException {
        Path tmpFile = Files.createTempDirectory(codeUnit.getName());
        File javaFile = new File(tmpFile.toFile(), codeUnit.getName() + ".java");

        FileWriter writer = new FileWriter(javaFile);
        writer.write(codeUnit.getContent());
        writer.close();

        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager standardJavaFileManager = javaCompiler.getStandardFileManager(null, null, null);
        File parentDir = tmpFile.toFile();
        standardJavaFileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(parentDir));
        Iterable<? extends JavaFileObject> compilationUnits = standardJavaFileManager.getJavaFileObjectsFromFiles(Arrays.asList(javaFile));
        javaCompiler.getTask(null, standardJavaFileManager, null, null, null, compilationUnits).call();
        standardJavaFileManager.close();

        return parentDir.toURI().toURL();
    }

    public static CodeUnit parse(String code, String pkg) {
        try {
            JavaParser javaParser = new JavaParser();
            com.github.javaparser.ast.CompilationUnit cu = javaParser.parse(code).getResult().get();

            // parse name
            CodeUnit unit = new CodeUnit();
            unit.setId(UUID.randomUUID().toString());
            unit.setName(cu.getType(0).getNameAsString());

            // add package name
            cu.setPackageDeclaration(pkg);

            unit.setPackagename(pkg);
            unit.setContent(cu.toString());
            unit.setUnitType(CodeUnit.CodeUnitType.CLASS);

            return unit;
        } catch (Throwable e) {
            return null;
        }
    }
}
