package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WebAppClassloader extends ClassLoader {
    private String root;

    public WebAppClassloader(ClassLoader parent, String root) {
        super(parent);
        this.root = root;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            System.out.println(name);
            byte[] bytes = loadClassData(name);
            return defineClass(name.replace("/","."), bytes, 0, bytes.length);
        } catch (IOException e) {
            return super.findClass(name);
        }
    }

    private byte[] loadClassData(String className) throws IOException {
        String path = className.replace('.', '/') + ".class";
        Path fullPath = Paths.get(root, path);
        if (Files.exists(fullPath)) return Files.readAllBytes(fullPath);
        throw new IOException();
    }

}
