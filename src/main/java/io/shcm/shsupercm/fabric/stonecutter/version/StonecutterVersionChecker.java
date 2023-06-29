package io.shcm.shsupercm.fabric.stonecutter.version;

import org.gradle.api.Project;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.function.Function;
import java.util.function.Predicate;

public interface StonecutterVersionChecker {
    Object parseVersion(String versionString) throws Exception;

    Predicate<Object> parseChecker(String predicateString) throws Exception;

    default boolean check(Object version, String predicateString) {
        try {
            return parseChecker(predicateString).test(version);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    Function<Project, StonecutterVersionChecker> FABRIC_LOADER_API = project -> {
        File loaderCopy = new File(project.getRootDir(), ".gradle/stonecutter");
        loaderCopy.mkdirs();
        loaderCopy = new File(loaderCopy, "fabric-loader.jar");

        if (loaderCopy.exists())
            try {
                return new FabricLoaderAPIVersionChecker(new URLClassLoader(new URL[]{ loaderCopy.toURI().toURL() }, StonecutterVersionChecker.class.getClassLoader()));
            } catch (Exception ignored) {}

        project.getLogger().error("Could not create default fabric loader api version checker!");
        return new StonecutterVersionChecker() {
            @Override
            public Object parseVersion(String versionString) throws Exception {
                return versionString;
            }

            @Override
            public Predicate<Object> parseChecker(String predicateString) throws Exception {
                return o -> false;
            }
        };
    };
}
