package org.example;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.mockito.Mockito;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class JerryCat {
    private static final String WEB_CLASSES_PATH = "/WEB-INF/classes/";
    private final String webProjectPath;

    private final Map<String, Servlet> servletMap = new HashMap<>();

    private final WebAppClassloader appClassloader;

    public JerryCat(String webProjectPath) {
        this.webProjectPath = webProjectPath;
        this.appClassloader = new WebAppClassloader(JerryCat.class.getClassLoader(), Paths.get(webProjectPath, WEB_CLASSES_PATH).toString());
        collectorServlet();
        initServlet();
    }
    private HttpServletRequest createHttpServletRequest(HttpExchange httpExchange) {
        HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);

        httpExchange.getRequestHeaders().forEach((s, strings) -> Mockito.when(httpServletRequest.getHeader(s)).thenReturn(strings.toString()));
        Headers requestHeaders = httpExchange.getRequestHeaders();
        Mockito.when(httpServletRequest.getHeaderNames()).thenReturn(Collections.enumeration(requestHeaders.keySet()));
        Mockito.when(httpServletRequest.getMethod()).thenReturn(httpExchange.getRequestMethod());
        Mockito.when(httpServletRequest.getServletPath()).thenReturn(httpExchange.getRequestURI().toString());
        Mockito.when(httpServletRequest.getProtocol()).thenReturn(httpExchange.getProtocol());
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(httpExchange.getRequestURI().toString());
        return httpServletRequest;
    }

    public void start() {
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(4040), 10);
            httpServer.createContext("/", httpExchange -> {
                Servlet servlet = servletMap.get(httpExchange.getRequestURI().toString());
                JerryCatHttpServletResponse httpServletResponse = new JerryCatHttpServletResponse(Mockito.mock(HttpServletResponse.class));
                HttpServletRequest httpServletRequest = createHttpServletRequest(httpExchange);
                if (servlet != null) {
                    try {
                        servlet.service(httpServletRequest, httpServletResponse);
                        byte[] responseByte = httpServletResponse.getResponseByte();
                        httpExchange.sendResponseHeaders(200, responseByte.length);
                        httpExchange.getResponseBody().write(responseByte);
                        httpExchange.getResponseBody().flush();
                    } catch (ServletException e) {
                        e.printStackTrace();
                    }
                }
            });
            httpServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    private void initServlet() {
        BaseServletConfig baseServletConfig = new BaseServletConfig();
        for (Servlet value : servletMap.values()) {
            try {
                value.init(baseServletConfig);
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @description: 从项目目录下收集所有Servlet信息
     */
    private void collectorServlet() {
        try {
            final Set<String> classFileSet = new HashSet<>();
            Files.walkFileTree(Paths.get(this.webProjectPath, WEB_CLASSES_PATH), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".class")) classFileSet.add(file.toString());
                    return super.visitFile(file, attrs);
                }
            });
            ClassNode classNode = new ClassNode();
            for (String classFile : classFileSet) {
                ClassReader classReader = new ClassReader(Files.newInputStream(Paths.get(classFile)));
                classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
                List<AnnotationNode> visibleAnnotations = classNode.visibleAnnotations;
                for (AnnotationNode visibleAnnotation : visibleAnnotations) {
                    if ("Ljavax/servlet/annotation/WebServlet;".equalsIgnoreCase(visibleAnnotation.desc)) {
                        Map<String, Object> annotationValues = ClassUtils.getAnnotationValues(visibleAnnotation.values);
                        Object o = loaderClass(classReader.getClassName());
                        servletMap.put(annotationValues.get("value").toString(), ((HttpServlet) o));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Object loaderClass(String name) {
        try {
            Class<?> aClass = appClassloader.loadClass(name);
            return aClass.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
