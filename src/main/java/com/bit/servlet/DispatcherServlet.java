package com.bit.servlet;

import com.bit.anno.*;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class DispatcherServlet extends HttpServlet {

    List<String> classNames = new ArrayList<>();
    Map<String, Object> beans = new HashMap<>();
    Map<String, Object> mappingMap = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getRequestURI();
        StringBuffer url = req.getRequestURL();
        System.out.println("uri:" + uri + ",url:" + url);
        String contextPath = req.getContextPath();
        String path = uri.replace(contextPath, "");
        Method method = (Method) mappingMap.get(path);
        Object instance = beans.get("/" + path.split("/")[1]);
        Object[] args=handle(req,resp,method);
        try {
            method.invoke(instance,args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private Object[] handle(HttpServletRequest req, HttpServletResponse resp, Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = (Object[]) Array.newInstance(Object.class, parameterTypes.length);
        int args_i=0;
        int index=0;
        for (Class<?> parameterType : parameterTypes) {

            if (ServletRequest.class.isAssignableFrom(parameterType)) {
                args[args_i++] = req;
            }
            if (ServletResponse.class.isAssignableFrom(parameterType)) {
                args[args_i++] = resp;
            }
            Annotation[] paramsAns = method.getParameterAnnotations()[index];
            for (Annotation paramsAn : paramsAns) {
                if (RequestParam.class.isAssignableFrom(paramsAn.getClass())) {
                    RequestParam requestParam = (RequestParam) paramsAn;
                    args[args_i++] = req.getParameter(requestParam.value());
                }
            }
            index++;
        }
        return args;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        scanPackage("com.bit");
        doInstance();
        doAutowired();
        urlMapping();
    }

    private void urlMapping() {
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object instance = entry.getValue();
            Class<?> clazz = instance.getClass();
            if (clazz.isAnnotationPresent(Controller.class)) {
                String classPath = "";
                if (clazz.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping annotation = clazz.getAnnotation(RequestMapping.class);
                    classPath = annotation.value();
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                        String methodPath = annotation.value();
                        mappingMap.put(classPath + methodPath, method);
                    }
                }
            }
        }
    }

    private void doAutowired() {
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object instance = entry.getValue();
            Class<?> clazz = instance.getClass();
            if (clazz.isAnnotationPresent(Controller.class)) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    if (field.isAnnotationPresent(Autowired.class)) {
                        Autowired annotation = field.getAnnotation(Autowired.class);
                        String key = annotation.value();
                        field.setAccessible(true);
                        try {
                            field.set(instance,beans.get(key));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void doInstance() {
        for (String className : classNames) {
            String cn = className.replace(".class", "");
            try {
                Class<?> clazz = this.getClass().getClassLoader().loadClass(cn);
                if (clazz.isAnnotationPresent(Controller.class)) {
                    Object object = clazz.newInstance();
                    if (clazz.isAnnotationPresent(RequestMapping.class)) {
                        beans.put(clazz.getAnnotation(RequestMapping.class).value(), object);
                    }

                }
                if (clazz.isAnnotationPresent(Service.class)) {
                    Object object = clazz.newInstance();
                    beans.put(clazz.getAnnotation(Service.class).value(), object);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    private void scanPackage(String basePackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + basePackage.replaceAll("\\.", "/"));
        String urlFile = url.getFile();
        File file = new File(urlFile);
        String[] fileStr = file.list();
        for (String path : fileStr) {
            File filePath = new File(urlFile + path);
            if (filePath.isDirectory()) {
                scanPackage(basePackage + "." + path);
            } else {
                classNames.add(basePackage + "." + filePath.getName());
            }
        }
    }
}
