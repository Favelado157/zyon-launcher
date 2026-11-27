package dev.zyon;
import java.util.*;
public final class CheatCatalog {
 public static String group(String id){if(Arrays.asList("killaura","triggerbot","autoclicker","aimassist").contains(id))return "Combate";if(Arrays.asList("autosprint","fly","speed","step","autojump","safewalk").contains(id))return "Movimento";return "Utilidades";}
 public static Set<String> ids(){return Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList("killaura","triggerbot","autoclicker","aimassist","autosprint","fly","speed","step","autojump","safewalk","esp","tracers","autotool","autoeat","fastplace")));}
}
