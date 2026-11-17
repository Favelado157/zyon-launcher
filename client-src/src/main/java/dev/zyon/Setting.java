// Java / Setting.java / Java 8 / Forge 1.8.9
package dev.zyon;
import java.util.Locale;
public final class Setting {
 public enum Kind { BOOLEAN, NUMBER, CHOICE }
 public final String id,label; public final Kind kind;
 public final double min,max,step; public final String[] choices; private double value;
 private Setting(String id,String label,Kind kind,double value,double min,double max,double step,String[] choices) {
  this.id=id;this.label=label;this.kind=kind;this.min=min;this.max=max;this.step=step;this.choices=choices;set(value);
 }
 public static Setting bool(String id,String label,boolean value){return new Setting(id,label,Kind.BOOLEAN,value?1:0,0,1,1,new String[0]);}
 public static Setting number(String id,String label,double value,double min,double max,double step){return new Setting(id,label,Kind.NUMBER,value,min,max,step,new String[0]);}
 public static Setting choice(String id,String label,int value,String... choices){return new Setting(id,label,Kind.CHOICE,value,0,choices.length-1,1,choices);}
 public double get(){return value;} public boolean on(){return value>.5;} public int integer(){return (int)Math.round(value);}
 public void set(double next){if(Double.isNaN(next)||Double.isInfinite(next))return;value=Math.max(min,Math.min(max,min+Math.round((next-min)/step)*step));}
 public void cycle(){set(value>=max?min:value+step);}
 public String display(){if(kind==Kind.BOOLEAN)return on()?"ON":"OFF";if(kind==Kind.CHOICE)return choices[integer()];return step>=1?Integer.toString(integer()):String.format(Locale.ROOT,step<.1?"%.2f":"%.1f",value);}
}
