package pt.ist.fenixWebFramework.renderers.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import pt.ist.fenixWebFramework.renderers.utils.ClassHierarchyTable;
import pt.ist.fenixframework.DomainObject;

import com.google.common.base.Strings;

public class DefaultValues {

    private static interface ValueCreator<T> {
        T createValue(Class<T> type, String defaultValue);
    }

    private static final ClassHierarchyTable<ValueCreator<?>> defaultValues = new ClassHierarchyTable<>();

    static {
        defaultValues.put(Object.class, new ValueCreator<Object>() {
            @Override
            public Object createValue(Class<Object> type, String defaultValue) {
                return null;
            }
        });
        defaultValues.put(String.class, new ValueCreator<String>() {
            @Override
            public String createValue(Class<String> type, String defaultValue) {
                return defaultValue != null ? defaultValue : "";
            }
        });
        defaultValues.put(Number.class, new ValueCreator<Number>() {
            @Override
            public Number createValue(Class<Number> type, String defaultValue) {
                if (Strings.isNullOrEmpty(defaultValue)) {
                    return null;
                }

                try {
                    return new Integer(defaultValue != null ? defaultValue : "0");
                } catch (NumberFormatException e) {
                    try {
                        return new Float(defaultValue != null ? defaultValue : "0.0");
                    } catch (NumberFormatException e1) {
                    }
                }

                return new Integer(0);
            }
        });
        defaultValues.put(Integer.class, new ValueCreator<Integer>() {
            @Override
            public Integer createValue(Class<Integer> type, String defaultValue) {
                if (Strings.isNullOrEmpty(defaultValue)) {
                    return null;
                }

                try {
                    return new Integer(defaultValue != null ? defaultValue : "0");
                } catch (NumberFormatException e) {
                }

                return new Integer(0);
            }
        });
        defaultValues.put(Float.class, new ValueCreator<Float>() {
            @Override
            public Float createValue(Class<Float> type, String defaultValue) {
                if (Strings.isNullOrEmpty(defaultValue)) {
                    return null;
                }
                try {
                    return new Float(defaultValue != null ? defaultValue : "0.0");
                } catch (NumberFormatException e) {
                }

                return new Float(0.0f);
            }
        });
        defaultValues.put(Boolean.class, new ValueCreator<Boolean>() {
            @Override
            public Boolean createValue(Class<Boolean> type, String defaultValue) {
                if (Strings.isNullOrEmpty(defaultValue)) {
                    return null;
                }
                return Boolean.valueOf(defaultValue);
            }
        });
        defaultValues.put(Date.class, new ValueCreator<Date>() {
            @Override
            public Date createValue(Class<Date> type, String defaultValue) {
                if (Strings.isNullOrEmpty(defaultValue)) {
                    return null;
                }
                try {
                    return new SimpleDateFormat("dd/MM/yyyy").parse(defaultValue);
                } catch (ParseException e) {
                    return new Date();
                }
            }
        });
        defaultValues.put(Enum.class, new ValueCreator<Enum>() {
            @Override
            public Enum createValue(Class<Enum> type, String defaultValue) {
                if (Strings.isNullOrEmpty(defaultValue)) {
                    return null;
                }

                try {
                    return Enum.valueOf(type, defaultValue);
                } catch (Exception e) {
                    return null;
                }

            }
        });
        defaultValues.put(DomainObject.class, new ValueCreator<DomainObject>() {
            @Override
            public DomainObject createValue(Class<DomainObject> type, String defaultValue) {
                return null;
            }
        });
    }

    public static Object createValue(Class type, String defaultValue) {
        return defaultValues.get(type).createValue(type, defaultValue);
    }
}
