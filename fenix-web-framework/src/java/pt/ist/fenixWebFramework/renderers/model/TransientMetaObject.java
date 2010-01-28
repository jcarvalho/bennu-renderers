package pt.ist.fenixWebFramework.renderers.model;

import pt.ist.fenixWebFramework.renderers.utils.RendererPropertyUtils;

public class TransientMetaObject extends MetaObject {

    private transient Object object;
    private int code;

    public TransientMetaObject(Object object) {
	super();
	setObject(object);
    }

    protected void setObject(Object object) {
	this.object = object;
	this.code = object == null ? 0 : object.hashCode();
    }

    public Object getObject() {
	return this.object;
    }

    public Class getType() {
	return this.object.getClass();
    }

    public MetaObjectKey getKey() {
	return new MetaObjectKey(getType(), this.code);
    }

    public void commit() {
	for (MetaSlot slot : getAllSlots()) {
	    if (slot.isSetterIgnored()) {
		continue;
	    }

	    if (slot.isCached()) {
		Object value = slot.getObject();

		try {
		    setProperty(slot, value);
		} catch (Exception e) {
		    throw new RuntimeException("could not write property '" + slot.getName() + "' in object " + getObject(), e);
		}
	    }
	}
    }

    protected void setProperty(MetaSlot slot, Object value) {
	RendererPropertyUtils.setProperty(getObject(), slot.getName(), value, false);
    }
}