package org.drinkless.tdlib;

import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class M {
    static Set<Class<? extends TdApi.MessageContent>> subclasses;

    public static void main(String[] args) {
        Reflections reflections = new Reflections("org.drinkless.tdlib");
        subclasses = reflections.getSubTypesOf(TdApi.MessageContent.class);

        List<Class<? extends TdApi.MessageContent>> subs = new ArrayList<>();
        for (Class<? extends TdApi.MessageContent> subclass : subclasses) {
            if (searchForType(subclass))
                subs.add(subclass);
        }
    }

    public static boolean searchForType(Class<?> clazz) {

        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (field.getType().equals(TdApi.MessageText.class)) {
                return true;
            }
            if (searchForType(field.getClass())) {
                return true;
            }
        }
        return false;
    }
}
