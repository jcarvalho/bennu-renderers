package pt.ist.fenixWebFramework.renderers.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import pt.ist.fenixWebFramework.rendererExtensions.factories.CreationDomainMetaObject;
import pt.ist.fenixWebFramework.rendererExtensions.factories.DomainMetaObject;
import pt.ist.fenixWebFramework.rendererExtensions.factories.DomainMetaObjectCollection;
import pt.ist.fenixWebFramework.renderers.schemas.Schema;
import pt.ist.fenixWebFramework.renderers.schemas.SchemaSlotDescription;
import pt.ist.fenixWebFramework.renderers.schemas.Signature;
import pt.ist.fenixWebFramework.renderers.schemas.SignatureParameter;
import pt.ist.fenixWebFramework.renderers.utils.RenderKit;
import pt.ist.fenixframework.DomainObject;

public final class MetaObjectFactory {

    public static MetaObjectCollection createObjectCollection() {
        return new DomainMetaObjectCollection();
    }

    public static MetaObject createObject(Object object, Schema schema) {
        Schema usedSchema = schema;

        if (usedSchema == null && !(object instanceof Collection)) {
            usedSchema = SchemaFactory.create(object);
        }

        if (usedSchema == null && object instanceof Collection) {
            Collection<?> collection = (Collection<?>) object;
            if (!collection.isEmpty()) {
                usedSchema = SchemaFactory.create(collection.iterator().next());
            }
        }

        return createMetaObject(object, usedSchema);
    }

    public static MetaObject createObject(Class<?> type, Schema schema) {
        Schema usedSchema = schema;
        if (usedSchema == null) {
            usedSchema = SchemaFactory.create(type);
        }
        return createMetaObject(type, usedSchema);
    }

    private static MetaObject createMetaObject(Object object, Schema schema) {
        if (object instanceof Collection) {
            MetaObjectCollection multipleMetaObject = createObjectCollection();

            for (Object element : ((Collection<?>) object)) {
                multipleMetaObject.add(createOneMetaObject(element, schema));
            }

            return multipleMetaObject;
        } else {
            return createOneMetaObject(object, schema);
        }
    }

    private static MetaObject createOneMetaObject(Object object, Schema schema) {
        if (object instanceof DomainObject) {
            // persistent object
            return createDomainMetaObject(object, schema);
        }
        MetaObject result;

        if (isPrimitiveObject(object)) {
            result = new PrimitiveMetaObject(object);
        } else if (object != null && !(object instanceof Serializable)) {
            TransientMetaObject metaObject = new TransientMetaObject(object);
            addSlotDescriptions(schema, metaObject);
            result = metaObject;
        } else {
            SimpleMetaObject metaObject = new SimpleMetaObject(object);

            addSlotDescriptions(schema, metaObject);
            addCompositeSlotSetters(schema, metaObject);

            result = metaObject;
        }

        result.setSchema(schema);
        return result;
    }

    private static boolean isPrimitiveObject(Object object) {
        return object == null ? true : object.getClass().isPrimitive();
    }

    private static void setInstanceCreator(Class<?> type, Schema schema, MetaObject metaObject) {
        Signature signature = schema.getConstructor();

        if (signature != null) {
            InstanceCreator creator = new InstanceCreator(type);

            for (SignatureParameter parameter : signature.getParameters()) {
                SchemaSlotDescription description = parameter.getSlotDescription();

                for (MetaSlot slot : metaObject.getAllSlots()) {
                    if (slot.getName().equals(description.getSlotName())) {
                        creator.addArgument(slot, parameter.getType());
                    }
                }
            }

            metaObject.setInstanceCreator(creator);
        }
    }

    private static void addSlotDescriptions(Schema schema, MetaObject metaObject) {
        List<SchemaSlotDescription> slotDescriptions = schema.getSlotDescriptions();
        for (SchemaSlotDescription description : slotDescriptions) {
            MetaSlot metaSlot = createSlot(metaObject, description);

            if (!description.isHidden()) {
                metaObject.addSlot(metaSlot);
            } else {
                metaObject.addHiddenSlot(metaSlot);
            }
        }
    }

    private static void addCompositeSlotSetters(Schema schema, SimpleMetaObject metaObject) {
        for (Signature setterSignature : schema.getSpecialSetters()) {
            CompositeSlotSetter compositeSlotSetter = new CompositeSlotSetter(metaObject, setterSignature.getName());

            for (SignatureParameter parameter : setterSignature.getParameters()) {
                SchemaSlotDescription description = parameter.getSlotDescription();

                for (MetaSlot slot : metaObject.getAllSlots()) {
                    if (slot.getName().equals(description.getSlotName())) {
                        compositeSlotSetter.addArgument(slot, parameter.getType());
                    }
                }
            }

            metaObject.addCompositeSetter(compositeSlotSetter);
        }
    }

    private static MetaObject createMetaObject(Class<?> type, Schema schema) {
        if (DomainObject.class.isAssignableFrom(type)) {
            return createCreationDomainMetaObject(type, schema);
        }
        CreationMetaObject metaObject = new CreationMetaObject(type);

        metaObject.setSchema(schema);

        addSlotDescriptions(schema, metaObject);
        setInstanceCreator(type, schema, metaObject);
        addCompositeSlotSetters(schema, metaObject);

        return metaObject;
    }

    public static MetaSlot createSlot(MetaObject metaObject, SchemaSlotDescription slotDescription) {
        MetaSlot metaSlot;

        if (metaObject instanceof CreationDomainMetaObject) {
            metaSlot = new MetaSlotWithDefault(metaObject, slotDescription.getSlotName());
        } else if (metaObject instanceof DomainMetaObject) {
            metaSlot = new MetaSlot(metaObject, slotDescription.getSlotName());
        } else if (metaObject instanceof CreationMetaObject) {
            metaSlot = new MetaSlotWithDefault(metaObject, slotDescription.getSlotName());
        } else {
            metaSlot = new MetaSlot(metaObject, slotDescription.getSlotName());
        }

        metaSlot.setLabelKey(slotDescription.getKey());
        metaSlot.setLabelArg0(slotDescription.getArg0());
        metaSlot.setBundle(slotDescription.getBundle());
        metaSlot.setSchema(RenderKit.getInstance().findSchema(slotDescription.getSchema()));
        metaSlot.setLayout(slotDescription.getLayout());
        metaSlot.setValidators(slotDescription.getValidators());
        metaSlot.setDefaultValue(slotDescription.getDefaultValue());
        metaSlot.setProperties(slotDescription.getProperties());
        metaSlot.setConverter(slotDescription.getConverter());
        metaSlot.setReadOnly(slotDescription.isReadOnly());
        metaSlot.setHelpLabel(slotDescription.getHelpLabel());
        metaSlot.setDescription(slotDescription.getDescription());
        metaSlot.setDescriptionFormat(slotDescription.getDescriptionFormat());

        return metaSlot;
    }

    private static MetaObject createDomainMetaObject(Object object, Schema schema) {
        DomainMetaObject metaObject = new DomainMetaObject((DomainObject) object);

        metaObject.setSchema(schema);

        addSlotDescriptions(schema, metaObject);
        addCompositeSlotSetters(schema, metaObject);

        return metaObject;
    }

    private static MetaObject createCreationDomainMetaObject(Class<?> type, Schema schema) {
        CreationDomainMetaObject metaObject = new CreationDomainMetaObject(type);

        metaObject.setSchema(schema);

        addSlotDescriptions(schema, metaObject);
        setInstanceCreator(schema.getType(), schema, metaObject);
        addCompositeSlotSetters(schema, metaObject);

        return metaObject;
    }
}
