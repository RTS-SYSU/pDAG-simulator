package uk.ac.york.mocha.simulator.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * conveniently save and restore experimental settings
 */
public class StaticFieldBackup {
    private final Class<?> targetClass;
    private final Map<String, Object> backupMap = new HashMap<>();

    public StaticFieldBackup(Class<?> clazz) {
        this.targetClass = clazz;
        for (Field field : clazz.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (Modifier.isStatic(mods) && !Modifier.isFinal(mods)) {
                field.setAccessible(true);
                try {
                    backupMap.put(field.getName(), field.get(null));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void restore() {
        for (Field field : targetClass.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (Modifier.isStatic(mods) && !Modifier.isFinal(mods)) {
                field.setAccessible(true);
                try {
                    Object oldValue = backupMap.get(field.getName());
                    if (oldValue != null) {
                        field.set(null, oldValue);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
