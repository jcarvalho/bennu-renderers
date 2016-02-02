/**
 * Copyright © 2008 Instituto Superior Técnico
 *
 * This file is part of Bennu Renderers Framework.
 *
 * Bennu Renderers Framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bennu Renderers Framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Bennu Renderers Framework.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixWebFramework.renderers.components.state;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import org.fenixedu.bennu.core.domain.User;
import org.objenesis.strategy.StdInstantiatorStrategy;

import pt.ist.fenixWebFramework.RenderersConfigurationManager;
import pt.ist.fenixWebFramework.renderers.components.HtmlComponent;
import pt.ist.fenixWebFramework.renderers.contexts.InputContext;
import pt.ist.fenixWebFramework.renderers.model.MetaObject;
import pt.ist.fenixWebFramework.renderers.model.MetaSlot;
import pt.ist.fenixWebFramework.renderers.model.SimpleMetaObject;
import pt.ist.fenixframework.DomainObject;
import pt.ist.fenixframework.FenixFramework;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.UnsafeOutput;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.minlog.Log;
import com.google.common.base.Strings;

public class ViewState implements IViewState {

    private String id;

    private String layout;

    private Properties properties;

    private transient InputContext context;

    private Map<String, Object> attributes;

    transient private List<Message> messages;

    // Hidden slots, filled from context

    private List<HiddenSlot> hiddenSlots;

    // Destinations available 

    private ViewDestination inputDestination;

    private Map<String, ViewDestination> destinations;

    private ViewDestination currentDestination;

    // Properties set after each deserialization 

    private User user;

    transient private HtmlComponent component;

    transient private HttpServletRequest request;

    // Viewed object 

    private MetaObject metaObject;

    // Lifecycle properties

    private boolean visible;

    private boolean valid;

    private boolean skipUpdate;

    private boolean skipValidation;

    private boolean updateComponentTree;

    private boolean postBack;

    public ViewState() {
        super();

        this.valid = true;
        this.skipUpdate = false;
        this.skipValidation = false;
        this.updateComponentTree = true;
        this.postBack = false;
        this.visible = true;

        this.messages = new ArrayList<Message>();
    }

    public ViewState(String id) {
        this();

        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isPostBack() {
        return postBack;
    }

    @Override
    public void cancel() {
        this.id = "";
        this.inputDestination = null;
    }

    @Override
    public boolean isCanceled() {
        return this.id != null && this.id.equals("") && this.inputDestination == null;
    }

    @Override
    public void setPostBack(boolean isPostBack) {
        this.postBack = isPostBack;
    }

    @Override
    public HtmlComponent getComponent() {
        return this.component;
    }

    @Override
    public void setComponent(HtmlComponent component) {
        this.component = component;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void setValid(boolean isValid) {
        this.valid = isValid;
    }

    @Override
    public boolean skipUpdate() {
        return this.skipUpdate;
    }

    @Override
    public void setSkipUpdate(boolean skipUpdate) {
        this.skipUpdate = skipUpdate;
    }

    @Override
    public boolean skipValidation() {
        return this.skipValidation;
    }

    @Override
    public void setSkipValidation(boolean skipValidation) {
        this.skipValidation = skipValidation;
    }

    @Override
    public boolean isValid() {
        return this.valid;
    }

    @Override
    public void setUpdateComponentTree(boolean updateTree) {
        this.updateComponentTree = updateTree;
    }

    @Override
    public boolean getUpdateComponentTree() {
        return this.updateComponentTree;
    }

    @Override
    public void addDestination(String name, ViewDestination destination) {
        if (this.destinations == null) {
            this.destinations = new HashMap<>();
        }

        this.destinations.put(name, destination);
    }

    @Override
    public ViewDestination getDestination(String name) {
        if (this.destinations == null) {
            return null;
        } else {
            return this.destinations.get(name);
        }
    }

    @Override
    public void setInputDestination(ViewDestination destination) {
        this.inputDestination = destination;
    }

    @Override
    public ViewDestination getInputDestination() {
        return this.inputDestination;
    }

    @Override
    public void setCurrentDestination(String name) {
        this.currentDestination = getDestination(name);
    }

    @Override
    public void setCurrentDestination(ViewDestination destination) {
        this.currentDestination = destination;
    }

    @Override
    public ViewDestination getCurrentDestination() {
        return this.currentDestination;
    }

    @Override
    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public void setMetaObject(MetaObject object) {
        this.metaObject = object;
    }

    @Override
    public MetaObject getMetaObject() {
        return this.metaObject;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(User user) {
        this.user = user;

        if (getMetaObject() != null) {
            getMetaObject().setUser(user);
        }
    }

    @Override
    public String getLayout() {
        return layout;
    }

    @Override
    public void setLayout(String layout) {
        this.layout = layout;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void setLocalAttribute(String name, Object value) {
        setAttribute(name, value);
    }

    @Override
    public Object getLocalAttribute(String name) {
        return getAttribute(name);
    }

    @Override
    public void removeLocalAttribute(String name) {
        removeAttribute(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (this.attributes == null) {
            this.attributes = new HashMap<>();
        }

        this.attributes.put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        if (this.attributes == null) {
            return null;
        } else {
            return this.attributes.get(name);
        }
    }

    @Override
    public void removeAttribute(String name) {
        if (this.attributes != null) {
            this.attributes.remove(name);
        }
    }

    //
    // Serialization utils
    //

    public static String encodeToBase64(List<IViewState> viewStates) throws IOException {
        Log.TRACE();
        Kryo kryo = new Kryo();
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        kryo.setDefaultSerializer((k, type) -> {
            FieldSerializer<?> serializer = new FieldSerializer<>(k, type);
            serializer.setCopyTransient(false);
            return serializer;
        });
        kryo.register(ViewState.class);
        kryo.register(ViewDestination.class);
        kryo.register(SimpleMetaObject.class);
        kryo.register(MetaSlot.class);
        kryo.register(Properties.class);
        kryo.register(ArrayList.class);
        kryo.register(Hashtable.class);
        kryo.addDefaultSerializer(DomainObject.class, new Serializer<DomainObject>(false) {
            @Override
            public void write(Kryo kryo, Output output, DomainObject object) {
                output.writeString(object.getExternalId());
            }

            @Override
            public DomainObject read(Kryo kryo, Input input, Class type) {
                String id = input.readString();
                if (Strings.isNullOrEmpty(id)) {
                    return null;
                } else {
                    return FenixFramework.getDomainObject(id);
                }
            }
        });
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream stream = new ObjectOutputStream(new GZIPOutputStream(baos))) {
            stream.writeObject(viewStates);
        }
        byte[] bytes = baos.toByteArray();
        String value = Base64.getEncoder().encodeToString(bytes) + "_" + Base64.getEncoder().encodeToString(sign(bytes));
        System.out.println("Returning: " + value);
        return value;
    }

    private static final String ALGORITHM = "HmacSHA256";
    private static final SecretKeySpec key = new SecretKeySpec(
            RenderersConfigurationManager.getConfiguration().viewStateSignatureKey().getBytes(StandardCharsets.UTF_8), ALGORITHM);

    private static byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return mac.doFinal(payload);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Could not sign view state!", e);
        }
    }

    private static void validate(byte[] decodedForm, byte[] signature) {
        byte[] expected = sign(decodedForm);
        if (!Arrays.equals(expected, signature)) {
            throw invalidViewState();
        }
    }

    private static Object decodeObjectFromBase64(String encodedState) throws IOException, ClassNotFoundException {
        String[] parts = encodedState.split("_", 2);
        if (parts.length != 2) {
            throw invalidViewState();
        }
        byte[] decodedForm = Base64.getDecoder().decode(parts[0]);
        validate(decodedForm, Base64.getDecoder().decode(parts[1]));
        ObjectInputStream stream = new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(decodedForm)));
        return stream.readObject();
    }

    private static RuntimeException invalidViewState() {
        return new IllegalArgumentException("Invalid ViewState provided");
    }

    @SuppressWarnings("unchecked")
    public static List<IViewState> decodeFromBase64(String encodedState) throws IOException, ClassNotFoundException {
        return (List<IViewState>) decodeObjectFromBase64(encodedState);
    }

    @Override
    public void setContext(InputContext context) {
        this.context = context;
    }

    @Override
    public InputContext getContext() {
        return this.context;
    }

    @Override
    public void addHiddenSlot(HiddenSlot slot) {
        if (this.hiddenSlots == null) {
            this.hiddenSlots = new ArrayList<HiddenSlot>();
        }

        this.hiddenSlots.add(slot);
    }

    @Override
    public List<HiddenSlot> getHiddenSlots() {
        if (this.hiddenSlots == null) {
            this.hiddenSlots = new ArrayList<HiddenSlot>();
        }

        return this.hiddenSlots;
    }

    @Override
    public List<Message> setMessages(List<Message> messages) {
        ensureMessageList();
        return this.messages = messages;
    }

    @Override
    public List<Message> getMessages() {
        ensureMessageList();
        return this.messages;
    }

    @Override
    public void addMessage(Message message) {
        ensureMessageList();
        this.messages.add(message);
    }

    private void ensureMessageList() {
        if (this.messages == null) {
            this.messages = new ArrayList<Message>();
        }
    }

}
