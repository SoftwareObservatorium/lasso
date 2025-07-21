package de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.transformer;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;

import java.util.LinkedList;
import java.util.List;

/**
 * New impl. to transform JUnit test classes into stimulus sheets (including assertions).
 *
 * @author Marcus Kessel
 */
public class JUnitToSsnTransformer {

    @Deprecated
    public String transform(CompilationUnit cu) {
        StringBuilder allSheets = new StringBuilder();

        cu.findAll(MethodDeclaration.class).stream()
                .filter(m -> m.isAnnotationPresent("Test"))
                .forEach(testMethod -> {
                    System.out.println("Translating test method: " + testMethod.getNameAsString());
                    SsnMethodVisitor visitor = new SsnMethodVisitor();
                    SsnState state = new SsnState();
                    testMethod.getBody().ifPresent(body -> body.accept(visitor, state));
                    allSheets.append(state.toJsonL());
                    allSheets.append("\n# End of sheet for " + testMethod.getNameAsString() + "\n\n");
                });

        return allSheets.toString();
    }

    /**
     * JUnit test unit to list of sequence sheets.
     *
     * @param cu
     * @param lql
     * @param prefix
     * @return
     */
    public List<Sheet> transform(CompilationUnit cu, String lql, String prefix) {
        List<Sheet> sheets = new LinkedList<>();

        cu.findAll(MethodDeclaration.class).stream()
                .filter(m -> m.isAnnotationPresent("Test"))
                .forEach(testMethod -> {
                    SsnMethodVisitor visitor = new SsnMethodVisitor();
                    SsnState state = new SsnState();
                    testMethod.getBody().ifPresent(body -> body.accept(visitor, state));

                    Sheet sheet = new Sheet(prefix + "_" + testMethod.getNameAsString() + "()", state.toJsonL(), lql);

                    sheets.add(sheet);
                });

        return sheets;
    }

    /**
     * Parse code unit using JavaParser resolver
     *
     * @param sourceCode
     * @param classUnderTest
     * @return
     */
    public CompilationUnit create(String sourceCode, ClassUnderTest classUnderTest) {
        // --- Solver Configuration ---
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        if (classUnderTest != null) {
            typeSolver.add(new ClassLoaderTypeSolver(classUnderTest.getProject().getContainer()));
        } else {
            typeSolver.add(new ReflectionTypeSolver());
        }

        // Configure JavaParser to use the Symbol Solver.
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        ParserConfiguration parserConfig = new ParserConfiguration().setSymbolResolver(symbolSolver);

        // --- Transformation ---
        JavaParser configuredParser = new JavaParser(parserConfig);
        CompilationUnit cu = configuredParser.parse(sourceCode).getResult().orElseThrow();

        return cu;
    }
}