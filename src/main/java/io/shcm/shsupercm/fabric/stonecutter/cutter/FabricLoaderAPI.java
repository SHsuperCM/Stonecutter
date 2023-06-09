package io.shcm.shsupercm.fabric.stonecutter.cutter;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;
import java.util.function.Predicate;

public class FabricLoaderAPI {
    private final ClassLoader classLoader;
    private final Class<?> classSemanticVersion;
    private final Method methodSemanticVersionParse, methodVersionPredicateParse;

    public FabricLoaderAPI(URL... jar) throws Exception {
        this.classLoader = new URLClassLoader(jar, getClass().getClassLoader());

        this.classSemanticVersion = classLoader.loadClass("net.fabricmc.loader.api.SemanticVersion");
        this.methodSemanticVersionParse = classSemanticVersion.getDeclaredMethod("parse", String.class);
        this.methodVersionPredicateParse = classLoader.loadClass("net.fabricmc.loader.api.metadata.version.VersionPredicate").getDeclaredMethod("parse", String.class);
    }

    public Object parseVersion(String version) throws InvocationTargetException, IllegalAccessException {
        return this.methodSemanticVersionParse.invoke(null, version);
    }

    public Predicate<Object> parseVersionPredicate(String predicate) throws InvocationTargetException, IllegalAccessException {
        //noinspection unchecked
        return (Predicate<Object>) this.methodVersionPredicateParse.invoke(null, predicate);
    }

    public static FabricLoaderAPI fromDependencies(Project project) throws Exception {
        File fabricLoaderFile = null;
        loaderSearch: for (Configuration configuration : project.getConfigurations())
            for (Dependency dependency : configuration.getDependencies())
                if ("net.fabricmc".equals(dependency.getGroup()) && "fabric-loader".equals(dependency.getName()))
                    for (File file : configuration.getFiles())
                        if (file.getName().startsWith("fabric-loader")) {
                            fabricLoaderFile = file;
                            break loaderSearch;
                        }

        return new FabricLoaderAPI(Objects.requireNonNull(fabricLoaderFile).toURI().toURL());
    }
}
