package pt.ist.fenixWebFramework.renderers.model;

import pt.ist.fenixWebFramework.renderers.utils.RendererPropertyUtils;

public class MetaSlotWithDefault extends MetaSlot {

    private boolean createValue;

    public MetaSlotWithDefault(MetaObject metaObject, String name) {
        super(metaObject, name);

        this.createValue = true;
    }

    @Override
    public Object getObject() {
        if (this.createValue) {
            this.createValue = false;

            setObject(DefaultValues.createValue(getType(), getDefaultValue()));
        }

        return super.getObject();
    }

    @Override
    public void setObject(Object object) {
        super.setObject(object);
        this.createValue = false;
    }

    @Override
    public Class getType() {
        return RendererPropertyUtils.getPropertyType(getMetaObject().getType(), getName());
    }

}
