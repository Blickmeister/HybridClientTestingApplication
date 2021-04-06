package cz.fim.uhk.thesis.hybrid_client_test_app.modularity;

import dalvik.system.DexClassLoader;

public class MyDexClassLoader extends DexClassLoader {
    public MyDexClassLoader(String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if("cz.fim.uhk.thesis.libraryforofflinemode.MainClass".equals(name)) return LibraryLoaderInterface.class;
        return super.findClass(name);
    }
}
