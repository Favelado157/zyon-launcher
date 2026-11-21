package dev.zyon;
/** Captures exactly once and restores the user's brightness, including values from other settings. */
public final class BrightnessState {
 private boolean active;private float previous;
 public float apply(float current,boolean requested){if(requested){if(!active){previous=current;active=true;}return 16f;}if(active){active=false;return previous;}return current;}
 public boolean active(){return active;}
}
