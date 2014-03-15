package pt.ist.fenixWebFramework.renderers.model;

import java.beans.PropertyDescriptor;
import java.util.Collection;

import org.apache.commons.beanutils.PropertyUtils;

import pt.ist.fenixWebFramework.renderers.schemas.Schema;
import pt.ist.fenixWebFramework.renderers.schemas.SchemaSlotDescription;
import pt.ist.fenixframework.DomainObject;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.dml.DomainClass;
import pt.ist.fenixframework.dml.Slot;

public final class SchemaFactory {

    public static Schema create(Object object) {
        return create(object == null ? Object.class : object.getClass());
    }

    public static Schema create(Class<?> type) {
        if (DomainObject.class.isAssignableFrom(type)) {
            return getSchemaForDomainObject(type);
        }

        Schema schema = new Schema(type);
        if (Collection.class.isAssignableFrom(type)) {
            return schema;
        }
        for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(type)) {
            if (!descriptor.getName().equals("class")) {
                schema.addSlotDescription(new SchemaSlotDescription(descriptor.getName()));
            }
        }
        return schema;
    }

    private static Schema getSchemaForDomainObject(Class<?> type) {
        DomainClass domainClass = FenixFramework.getDomainModel().findClass(type.getName());
        Schema schema = new Schema(type);
        while (domainClass != null) {
            for (Slot slot : domainClass.getSlotsList()) {
                schema.addSlotDescription(new SchemaSlotDescription(slot.getName()));
            }
            domainClass = (DomainClass) domainClass.getSuperclass();
        }
        return schema;
    }
}
