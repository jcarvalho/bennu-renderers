package pt.ist.fenixWebFramework.renderers.components.converters;

import java.io.Serializable;

public abstract class Converter implements Serializable {

    // public static final Converter IDENTITY_CONVERTER = (type, value) -> value;

    public abstract Object convert(Class type, Object value);

}
