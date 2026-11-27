package dev.zyon.platform;
import java.lang.reflect.*;
public final class ReflectionHelper {
 public static Field findField(Class<?> type,String... names){for(String name:names)for(Class<?> c=type;c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalArgumentException("Field not found: "+type.getName()+java.util.Arrays.toString(names));}
 public static Method findMethod(Class<?> type,Object target,String[] names,Class<?>... args){for(String name:names)for(Class<?> c=type;c!=null;c=c.getSuperclass())try{Method m=c.getDeclaredMethod(name,args);m.setAccessible(true);return m;}catch(NoSuchMethodException ignored){}throw new IllegalArgumentException("Method not found: "+type.getName()+java.util.Arrays.toString(names));}
}