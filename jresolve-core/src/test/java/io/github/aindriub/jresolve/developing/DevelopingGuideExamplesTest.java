package io.github.aindriub.jresolve.developing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

/**
 * Every {@code java} code block in {@code DEVELOPING.md}, compiled and run. A
 * contributor guide that shows a comparator which no longer compiles is a
 * confident wrong answer, so the guide is not allowed to drift from the API.
 *
 * <p>The blocks are read out of the guide itself, not copied here, so editing a
 * block edits what is tested. Each block is one compilation unit. A class in a
 * block that has a {@code public static void main(String[])} has it invoked, and
 * a class with {@code @Test} methods has them invoked on a fresh instance; a
 * failure in either fails this test. XML and shell blocks are not read.
 */
class DevelopingGuideExamplesTest {

    private static final Path GUIDE = Paths.get("..", "DEVELOPING.md");
    private static final Pattern CLASS_NAME = Pattern.compile("(?m)^(?:public\\s+)?(?:final\\s+)?class\\s+(\\w+)");
    private static final Pattern PACKAGE = Pattern.compile("(?m)^package\\s+([\\w.]+);");

    /** One code block, with the name its compilation unit is filed under. */
    private static final class Block {
        private final String qualifiedName;
        private final String source;

        Block(String qualifiedName, String source) {
            this.qualifiedName = qualifiedName;
            this.source = source;
        }
    }

    @Test
    void theGuideHasJavaBlocksToCheck() throws IOException {
        // A wrong path must not look like a pass: reading nothing checks nothing.
        assertThat(javaBlocks()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void everyJavaBlockCompilesAgainstJava8() throws IOException {
        Path out = Files.createTempDirectory("developing-guide");

        assertThat(compile(javaBlocks(), out)).isEmpty();
    }

    @Test
    void everyJavaBlockRunsAndItsChecksHold() throws Exception {
        Path out = Files.createTempDirectory("developing-guide");
        List<Block> blocks = javaBlocks();
        assertThat(compile(blocks, out)).isEmpty();

        int executed = 0;
        try (URLClassLoader loader = new URLClassLoader(
                new URL[] {out.toUri().toURL()}, getClass().getClassLoader())) {
            for (Block block : blocks) {
                executed += run(loader.loadClass(block.qualifiedName));
            }
        }

        // Compiling and running nothing would also be green.
        assertThat(executed).isGreaterThanOrEqualTo(4);
    }

    // ------------------------------------------------------------- plumbing

    private static int run(Class<?> type) throws Exception {
        int executed = 0;
        for (Method method : type.getDeclaredMethods()) {
            boolean isMain = method.getName().equals("main")
                    && Arrays.equals(method.getParameterTypes(), new Class<?>[] {String[].class});
            boolean isTest = method.isAnnotationPresent(Test.class);
            if (!isMain && !isTest) {
                continue;
            }
            method.setAccessible(true);
            try {
                if (isMain) {
                    method.invoke(null, (Object) new String[0]);
                } else {
                    java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    method.invoke(constructor.newInstance());
                }
            } catch (InvocationTargetException e) {
                throw new AssertionError(type.getName() + "." + method.getName() + " failed", e.getCause());
            }
            executed++;
        }
        return executed;
    }

    private static List<String> compile(List<Block> blocks, Path out) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).as("the test JVM must be a JDK, not a JRE").isNotNull();

        List<JavaFileObject> units = new ArrayList<>();
        for (Block block : blocks) {
            units.add(new SourceUnit(block.qualifiedName, block.source));
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null)) {
            files.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(out.toFile()));
            List<String> options = Arrays.asList(
                    "--release", "8", "-Xlint:-options", "-proc:none",
                    "-classpath", System.getProperty("java.class.path"));
            compiler.getTask(null, files, diagnostics, options, null, units).call();
        }
        List<String> problems = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                problems.add(d.getSource().getName() + ":" + d.getLineNumber() + " " + d.getMessage(null));
            }
        }
        return problems;
    }

    private static List<Block> javaBlocks() throws IOException {
        List<String> lines = Files.readAllLines(GUIDE, StandardCharsets.UTF_8);
        List<Block> blocks = new ArrayList<>();
        StringBuilder current = null;
        for (String line : lines) {
            if (current == null) {
                if (line.trim().equals("```java")) {
                    current = new StringBuilder();
                }
            } else if (line.trim().equals("```")) {
                blocks.add(toBlock(current.toString()));
                current = null;
            } else {
                current.append(line).append('\n');
            }
        }
        return blocks;
    }

    private static Block toBlock(String source) {
        Matcher name = CLASS_NAME.matcher(source);
        assertThat(name.find()).as("a java block in the guide must declare a top-level class").isTrue();
        Matcher pkg = PACKAGE.matcher(source);
        String prefix = pkg.find() ? pkg.group(1) + "." : "";
        return new Block(prefix + name.group(1), source);
    }

    /** An in-memory source file, so nothing is written for the compiler to read. */
    private static final class SourceUnit extends SimpleJavaFileObject {
        private final String source;

        SourceUnit(String qualifiedName, String source) {
            super(URI.create("string:///" + qualifiedName.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
