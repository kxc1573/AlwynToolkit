/**
 * leverage the Java reflection mechanism to get and set all attributes types and values of a Class.
 */
package com.alwyn.javautils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;

public class ReflectUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ReflectUtil.class);

    /**
     *
     * @param obj
     * @param fieldName
     * @return
     * @throws Exception
     */
    public static Object getObjectValueByField(Object obj, String fieldName) throws Exception {

        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            if (field.getName().equals(fieldName)) {
                return field.get(obj);
            }
        }

        return null;
    }

    /**
     * !!! only support those data type which has been implemented by Field.
     * @param obj
     * @param fieldName
     * @param value
     * @throws Exception
     */
    public static void setObjectValueByField(Object obj, String fieldName, Object value) throws Exception {
        if (value != null) {
            Class<?> clazz = obj.getClass();
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                if (field.getName().equals(fieldName)) {
                    // Special for "XMLGregorianCalendar to Date"
                    // But the Field doesn't have function "setDate", so it still fails.
                    if (value instanceof XMLGregorianCalendar) {
                        field.set(obj, ((XMLGregorianCalendar) value).toGregorianCalendar().getTime());
                    } else {
                        field.set(obj, value);
                    }
                }
            }
        }
    }

    /**
     * Imitate Getter
     * @param obj
     * @param fieldName
     * @return
     * @throws Exception
     */
    public static Object getObjectValueByMethod(Object obj, String fieldName) throws Exception {
        String methodName = getMethodName(fieldName);
        Method m = obj.getClass().getMethod("get" + methodName);
        Object value = m.invoke(obj);
        return value;
    }

    /**
     * Imitate Setter
     * !!! INVALID if don't specify a specific property type during getMethod() with argument.
     * For example: it will raise error: "ids for this class must be manually assigned before calling save()"
     * @param obj
     * @param fieldName
     * @param fieldValue
     * @throws Exception
     */
    public static void setObjectValueByMethod(Object obj, String fieldName, Object fieldValue) throws Exception {
        if (fieldValue != null) {
            String methodName = getMethodName(fieldName);
            Method m;

            // TODO: if have other data type, just append required "else if".
            if (fieldValue instanceof String) {
                m = obj.getClass().getMethod("set" + methodName, String.class);
                m.invoke(obj, fieldValue);
            } else if (fieldValue instanceof Integer) {
                m = obj.getClass().getMethod("set" + methodName, Integer.class);
                m.invoke(obj, fieldValue);
            } else if (fieldValue instanceof XMLGregorianCalendar) {
                m = obj.getClass().getMethod("set" + methodName, Date.class);
                m.invoke(obj, ((XMLGregorianCalendar) fieldValue).toGregorianCalendar().getTime());
            } else if (fieldValue instanceof Long) {
                m = obj.getClass().getMethod("set" + methodName, Long.class);
                m.invoke(obj, fieldValue);
            } else {
                LOG.warn("mismatch type: " + methodName + " with " + fieldValue.getClass().toString());
                m = obj.getClass().getMethod("set" + methodName, Object.class);
                m.invoke(obj, fieldValue);
            }
        }
    }

    private static String getMethodName(String fieldName) {
        String firstLetter = fieldName.substring(0, 1).toUpperCase();
        return firstLetter + fieldName.substring(1);
    }
}
