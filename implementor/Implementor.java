package info.kgeorgiy.ja.Shpileva.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class Implementor implements JarImpler {

    /**
     * Check necessity of type token implementation.
     *
     * @param token type token to implement.
     * @throws ImplerException if implementation not needed.
     */
    private void validateToken(Class<?> token) throws ImplerException {
        if (Modifier.isPrivate(token.getModifiers()) || token.isPrimitive()) {
            throw new ImplerException("Method id private or primitive");
        } else if (!token.isInterface()) {
            throw new ImplerException("Token is not an interface");
        }
    }

    /**
     * Check the need to generate a method.
     *
     * @param method given method.
     * @return need to generate or not.
     */
    private boolean validateMethod(Method method) {
        int modifiers = method.getModifiers();
        return method.isDefault() || Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers);
    }

    /**
     * Comparator for method comparing by returned type and parameters.
     */
    private static final Comparator<Method> METHOD_COMPARATOR = Comparator.comparing(Method::getName)
            .thenComparing(m -> Arrays.hashCode(m.getParameterTypes()));

    /**
     * Deletes all files and directory.
     */
    private static final SimpleFileVisitor<Path> VISITOR = new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    public static void main(final String[] args) {
        if (!checkArgs(args)) {
            System.err.printf("Usage: java %s [-jar] <Class name> <Target path>", Implementor.class.getSimpleName());
            System.err.println();
            return;
        }

        final Implementor implementor = new Implementor();
        try {
            if (args.length == 3) {
                implementor.implementJar(Class.forName(args[1]), Path.of(args[2]));
            } else {
                implementor.implement(Class.forName(args[0]), Path.of(args[1]));
            }
        } catch (final ClassNotFoundException e) {
            System.err.println("Class not found");
        } catch (final ImplerException e) {
            System.err.println("Fail implement this class " + e.getMessage());
        }
    }

    /**
     * Validate arguments correctness
     *
     * @param args arguments to validate
     * @return correct arguments or not
     */
    private static boolean checkArgs(final String[] args) {
        if (args == null) {
            return false;
        }
        if (args.length != 2 && args.length != 3) {
            return false;
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            return false;
        }

        return args.length != 3 || "-jar".equals(args[0]);
    }


    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (jarFile == null) {
            throw new ImplerException("JarFile path can not be null");
        }
        Path pathTemp;
        try {
            pathTemp = Files.createDirectory(Path.of("tmp" + jarFile.getParent()));
        } catch (IOException e) {
            throw new ImplerException("Can not create tmp dir " + e.getMessage());
        }
        try {
            implement(token, pathTemp);
            generateJar(jarFile, pathTemp, token);
        } finally {
            clearDir(pathTemp);
        }
    }

    /**
     * Deletes files and directories in this directory.
     *
     * @param dirPath directory path to clear.
     * @throws ImplerException if walkFileTree throws an exception
     */
    private void clearDir(Path dirPath) throws ImplerException {
        try {
            Files.walkFileTree(dirPath, VISITOR);
        } catch (IOException e) {
            throw new ImplerException("Can not delete temporary directory");
        }
    }

    /**
     * Generates jar file.
     *
     * @param jarFile path where generate Jar file
     * @param tmpPath path of temporary directory
     * @param token   type token to implement.
     * @throws ImplerException if can not compile class or create jar.
     */
    private void generateJar(Path jarFile, Path tmpPath, Class<?> token) throws ImplerException {
        String classPath = tmpPath + File.pathSeparator + getClassPath(token);
        Path filePath = getPath(token, tmpPath, ".java").toAbsolutePath();
        String[] args = new String[]{"-encoding", "UTF-8", "-cp", classPath, filePath.toString()};
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler.run(null, null, null, args) != 0) {
            throw new ImplerException("Can not compile implemented class");
        }

        Manifest manifest = versionedManifest();
        Path compilePath = getPath(token, tmpPath, ".class").toAbsolutePath();
        try (JarOutputStream outputStream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            String fullName = getFullName(token, ".class").replace("\\", "/");
            outputStream.putNextEntry(new ZipEntry(fullName));
            Files.copy(compilePath, outputStream);
        } catch (IOException e) {
            throw new ImplerException("Can not write JAR file " + e);
        }
    }

    /**
     * Returns class path to .jar file with {@link JarImpler} class.
     *
     * @param token token for get class path
     * @return class path to .jar file
     * @throws ImplerException if failed get class path for token
     */
    private Path getClassPath(Class<?> token) throws ImplerException {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new ImplerException("Can not get class path to compile code");
        }
    }

    /**
     * Creates manifest with set version attribute.
     *
     * @return manifest with version attribute.
     */
    private Manifest versionedManifest() {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        return manifest;
    }

    /**
     * Generate implementation by token and put this to root
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException if the given class cannot be implemented, because of these:
     *                         <ul>
     *                             <li>IO exception</li>
     *                             <li>Fail compile implementation</li>
     *                             <li>Token is not interface</li>
     *                             <li>Some method have private class on arguments, exceptions or return</li>
     *                         </ul>
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        validateToken(token);
        Path resultPath = getPath(token, root, ".java");
        StringBuilder result = new StringBuilder();
        try (BufferedWriter wr = Files.newBufferedWriter(resultPath, StandardCharsets.UTF_8)) {
            generateClassHeader(token, result);
            generateMethods(token, result);
            result.append("}");
            result.append(System.lineSeparator());
            String stringResult = String.valueOf(result);
            wr.write(stringResult);
        } catch (Exception e) {
            System.err.println("Can not generate interface implementation. " + e);
        }
    }

    /**
     * @param token
     * @param suffix
     * @return
     */
    private String getFullName(Class<?> token, String suffix) {
        return String.format("%s/%sImpl%s",
                token.getPackageName().replace('.', File.separatorChar),
                token.getSimpleName(),
                suffix);
    }

    /**
     * @param token
     * @param root
     * @param suffix
     * @return
     * @throws ImplerException
     */
    private Path getPath(Class<?> token, Path root, String suffix) throws ImplerException {
        Path fullPath;
        try {
            String fullName = getFullName(token, suffix);
            fullPath = root.resolve(Path.of(fullName));
            Path parent = fullPath.getParent();
            if (Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new ImplerException("Can not create parent directories", e);
        }
        return fullPath;
    }

    /**
     * Generate class name for parameter listing.
     *
     * @param clazz given type token
     * @return string class name for parameter listing.
     */
    private String getClassName(Class<?> clazz) {
        if (clazz.isArray()) {
            return clazz.getComponentType().getCanonicalName() + "[]";
        }
        return clazz.getCanonicalName();
    }

    /**
     * Add method parameters.
     *
     * @param m given method.
     */
    private String getParams(Method m) {
        Parameter[] params = m.getParameters();
        if (params.length == 0) {
            return "";
        }
        StringJoiner parametersType = new StringJoiner(", ");
        for (Parameter p : params) {
            parametersType.add(getClassName(p.getType()) + " " + p.getName());
        }
        return parametersType.toString();
    }

    /**
     * Returns listed method exceptions.
     *
     * @param m given method.
     */
    private String getExceptions(Method m) {
        StringJoiner exceptions = new StringJoiner(",");
        Class<?>[] exceptionTypes = m.getExceptionTypes();
        if (exceptionTypes.length == 0) {
            return "";
        }
        for (Class<?> e : exceptionTypes) {
            exceptions.add(e.getCanonicalName());
        }
        return "throws " + exceptions;
    }

    /**
     * Add interface methods.
     *
     * @param token  given interface.
     * @param result string builder to write the interface methods.
     */
    private void generateMethods(Class<?> token, StringBuilder result) {
        Set<Method> methods = new TreeSet<>(METHOD_COMPARATOR);
        methods.addAll(List.of(token.getMethods()));
        for (Class<?> c = token; c != null; c = c.getSuperclass()) {
            methods.addAll(List.of(c.getDeclaredMethods()));
        }
        for (Method method : methods) {
            generateMethod(method, result);
        }
    }


    /**
     * Add one method of interface
     *
     * @param method given method
     * @param result the result in which we write the interface
     */
    private void generateMethod(Method method, StringBuilder result) {
        if (validateMethod(method)) {
            return;
        }
        int modifiers = method.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT;
        Class<?> returnType = method.getReturnType();
        result.append(String.format(
                "%s %s %s(%s) %s",
                Modifier.toString(modifiers),
                returnType.getCanonicalName(),
                method.getName(),
                getParams(method),
                getExceptions(method)
        ));

        if (method.getReturnType() == void.class) {
            result.append(String.format(
                    "{} %s",
                    System.lineSeparator()
            ));
            return;
        }

        result.append(" { return");
        if (method.getReturnType() == boolean.class) {
            result.append(" false");
        } else if (!method.getReturnType().isPrimitive()) {
            result.append(" null");
        } else {
            result.append(" 0");
        }
        result.append(String.format(
                "; }%s",
                System.lineSeparator()
        ));
    }

    /**
     * Returns package by class type token.
     *
     * @param token generate package for.
     * @return string with package.
     */
    private String getPackage(Class<?> token) {
        if (!token.getPackageName().isEmpty()) {
            return String.format(
                    "package %s;%s",
                    token.getPackageName(),
                    System.lineSeparator()
            );
        }
        return "";
    }

    /**
     * Generates headers by class type token.
     *
     * @param token  generate header for.
     * @param result where to write result.
     */
    private void generateClassHeader(Class<?> token, StringBuilder result) {
        result.append(String.format(
                "%spublic class  %sImpl implements %s { %s",
                getPackage(token),
                token.getSimpleName(),
                token.getCanonicalName(),
                System.lineSeparator()
        ));
    }
}
