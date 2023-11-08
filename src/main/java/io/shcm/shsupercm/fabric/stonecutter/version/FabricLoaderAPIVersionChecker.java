package io.shcm.shsupercm.fabric.stonecutter.version;

import java.lang.reflect.Method;
import java.util.function.Predicate;

public class FabricLoaderAPIVersionChecker implements StonecutterVersionChecker {
    private final Method methodSemanticVersionParse, methodVersionPredicateParse;

    public FabricLoaderAPIVersionChecker(ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        Class<?> classSemanticVersion = classLoader.loadClass("net.fabricmc.loader.api.SemanticVersion");
        this.methodSemanticVersionParse = classSemanticVersion.getDeclaredMethod("parse", String.class);
        this.methodVersionPredicateParse = classLoader.loadClass("net.fabricmc.loader.api.metadata.version.VersionPredicate").getDeclaredMethod("parse", String.class);
    }

    @Override
    public Object parseVersion(String versionString) throws Exception {
        return this.methodSemanticVersionParse.invoke(null, versionString);
    }

    @Override
    public Predicate<Object> parseChecker(String predicateString) throws Exception {
        //noinspection unchecked
        return (Predicate<Object>) this.methodVersionPredicateParse.invoke(null, predicateString);
    }
}
