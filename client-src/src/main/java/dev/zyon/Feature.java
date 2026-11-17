// Java / Feature.java / Java 8 / Forge 1.8.9
package dev.zyon;
import java.util.*;
public final class Feature {
 public enum Category { COMBAT("Combate"),MOVEMENT("Movimento"),RENDER("Visual"),PLAYER("Jogador"),CLIENT("Interface");public final String label;Category(String label){this.label=label;} }
 public final String id,name,description;public final Category category;public final List<Setting> settings=new ArrayList<Setting>();public int key;private boolean enabled;
 public Feature(String id,String name,String description,Category category,Setting... settings){this.id=id;this.name=name;this.description=description;this.category=category;Collections.addAll(this.settings,settings);}
 public boolean enabled(){return enabled;}
 public void toggle(){setEnabled(!enabled);}
 public void setEnabled(boolean next){if(id.equals("theme")||next==enabled)return;enabled=next;Zyon.instance.engine.changed(this,next);}
 public Setting setting(String id){for(Setting s:settings)if(s.id.equals(id))return s;throw new IllegalArgumentException("Ajuste desconhecido: "+id);}
 public double number(String id){return setting(id).get();}public boolean flag(String id){return setting(id).on();}
}
