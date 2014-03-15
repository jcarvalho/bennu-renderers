package pt.ist.fenixWebFramework.renderers.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pt.ist.fenixWebFramework.renderers.exceptions.NoRendererException;
import pt.ist.fenixWebFramework.renderers.exceptions.NoSuchSchemaException;
import pt.ist.fenixWebFramework.renderers.schemas.Schema;
import pt.ist.fenixWebFramework.renderers.schemas.SchemaSlotDescription;
import pt.ist.fenixWebFramework.renderers.schemas.Signature;
import pt.ist.fenixWebFramework.renderers.schemas.SignatureParameter;
import pt.ist.fenixWebFramework.renderers.utils.RenderKit;
import pt.ist.fenixWebFramework.renderers.utils.RenderKit.RenderMode;
import pt.ist.fenixWebFramework.renderers.utils.RendererPropertyUtils;
import pt.ist.fenixWebFramework.renderers.validators.HtmlValidator;
import pt.ist.fenixWebFramework.renderers.validators.RequiredValidator;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.core.Project;
import pt.utl.ist.fenix.tools.util.Pair;

public class ConfigurationReader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationReader.class);

    public static void readSchemas(ServletContext context, InputStream schemaConfig) {
        Element root = readConfigRootElement(context, schemaConfig);

        if (root != null) {
            NodeList schemaElements = root.getElementsByTagName("schema");

            for (int i = 0; i < schemaElements.getLength(); i++) {
                Element schemaElement = (Element) schemaElements.item(i);

                String schemaName = schemaElement.getAttribute("name");
                String typeName = schemaElement.getAttribute("type");
                String extendedSchemaName = schemaElement.getAttribute("extends");
                String refinedSchemaName = schemaElement.getAttribute("refines");
                String schemaBundle = schemaElement.getAttribute("bundle");
                String constructor = schemaElement.getAttribute("constructor");

                if (RenderKit.hasSchema(schemaName)) {
                    logger.error("schema '{}' was already defined", schemaName);
                    continue;
                }

                Class<?> type;
                try {
                    type = getClassForType(typeName, true);
                } catch (ClassNotFoundException e) {
                    logger.error("schema '{}' was defined for the undefined type '{}'", schemaName, typeName);
                    continue;
                }

                if (extendedSchemaName != null && refinedSchemaName != null) {
                    logger.error("schema '{}' cannot extend '{}' and refine '{}' at the same time", schemaName,
                            extendedSchemaName, refinedSchemaName);
                    continue;
                }

                Schema extendedSchema;
                try {
                    extendedSchema = RenderKit.getInstance().findSchema(extendedSchemaName);
                } catch (NoSuchSchemaException e) {
                    logger.error("schema '{}' cannot extend '{}', schema not found", schemaName, extendedSchemaName);
                    continue;
                }

                Schema refinedSchema;
                try {
                    refinedSchema = RenderKit.getInstance().findSchema(refinedSchemaName);
                } catch (NoSuchSchemaException e) {
                    logger.error("schema '{}' cannot refine '{}', schema not found", schemaName, refinedSchemaName);
                    continue;
                }

                if (extendedSchema != null && !extendedSchema.getType().isAssignableFrom(type)) {
                    logger.warn("schema '{}' is defined for type '{}' that is not a subclass of the type '{}' "
                            + "specified in the extended schema", schemaName, typeName, extendedSchema.getType().getName());
                }

                Schema schema;
                if (extendedSchema != null) {
                    schema = new Schema(schemaName, type, extendedSchema);
                } else if (refinedSchema != null) {
                    schema = refinedSchema;
                    schema.setType(type);
                } else {
                    schema = new Schema(schemaName, type);
                }

                NodeList removeElements = schemaElement.getElementsByTagName("remove");
                if (extendedSchemaName == null && refinedSchema == null && removeElements.getLength() > 0) {
                    logger.warn("schema '{}' specifies slots to be removed but it does not extend or refine schema", schemaName);
                } else {
                    for (int j = 0; j < removeElements.getLength(); j++) {
                        Element removeElement = (Element) removeElements.item(j);

                        String name = removeElement.getAttribute("name");

                        SchemaSlotDescription slotDescription = schema.getSlotDescription(name);
                        if (slotDescription == null) {
                            logger.warn(
                                    "schema '{}' specifies that slot '{}' is to be removed but it is not defined in the extended schema",
                                    schemaName, name);
                            continue;
                        }

                        schema.removeSlotDescription(slotDescription);
                    }
                }

                NodeList slotElements = schemaElement.getElementsByTagName("slot");
                for (int j = 0; j < slotElements.getLength(); j++) {
                    Element slotElement = (Element) slotElements.item(j);

                    String slotName = slotElement.getAttribute("name");
                    String layout = slotElement.getAttribute("layout");
                    String key = slotElement.getAttribute("key");
                    String arg0 = slotElement.getAttribute("arg0");
                    String bundle = slotElement.getAttribute("bundle");
                    String slotSchema = slotElement.getAttribute("schema");
                    String validatorName = slotElement.getAttribute("validator");
                    String requiredValue = slotElement.getAttribute("required");
                    String defaultValue = slotElement.getAttribute("default");
                    String converterName = slotElement.getAttribute("converter");
                    String readOnlyValue = slotElement.getAttribute("read-only");
                    String hiddenValue = slotElement.getAttribute("hidden");
                    String helpLabelValue = slotElement.getAttribute("help");

                    String description = slotElement.getAttribute("description");
                    String descriptionFormat = slotElement.getAttribute("descriptionFormat");

                    Properties properties = getPropertiesFromElement(slotElement);

                    // Validators
                    List<Pair<Class<HtmlValidator>, Properties>> validators =
                            new ArrayList<Pair<Class<HtmlValidator>, Properties>>();
                    if (validatorName != null) {
                        try {
                            Class<HtmlValidator> validator = getClassForType(validatorName, true);
                            validators.add(new Pair<Class<HtmlValidator>, Properties>(validator, new Properties()));
                        } catch (ClassNotFoundException e) {
                            logger.error("in schema '{}': validator '{}' was not found", schemaName, validatorName);
                            continue;
                        }

                    }

                    boolean required = requiredValue == null ? false : Boolean.parseBoolean(requiredValue);
                    if (required) {
                        Class validator = RequiredValidator.class;
                        validators.add(new Pair<Class<HtmlValidator>, Properties>(validator, new Properties()));
                    }

                    NodeList validatorElements = slotElement.getElementsByTagName("validator");
                    for (int k = 0; k < validatorElements.getLength(); k++) {
                        Element validatorElement = (Element) validatorElements.item(k);
                        Properties validatorProperties;

                        validatorProperties = getPropertiesFromElement(validatorElement);
                        validatorName = validatorElement.getAttribute("class");

                        Class<HtmlValidator> validator = null;
                        if (validatorName != null) {
                            try {
                                validator = getClassForType(validatorName, true);
                            } catch (ClassNotFoundException e) {
                                logger.error("in schema '{}': validator '{}' was not found", schemaName, validatorName);
                                continue;
                            }
                        }

                        validators.add(new Pair<Class<HtmlValidator>, Properties>(validator, validatorProperties));
                    }

                    Class converter = null;
                    if (converterName != null) {
                        try {
                            converter = getClassForType(converterName, true);
                        } catch (ClassNotFoundException e) {
                            logger.error("in schema '{}': converter '{}' was not found", schemaName, converterName);
                            continue;
                        }
                    }

                    boolean readOnly = readOnlyValue == null ? false : Boolean.parseBoolean(readOnlyValue);
                    boolean hidden = hiddenValue == null ? false : Boolean.parseBoolean(hiddenValue);

                    if (bundle == null) {
                        bundle = schemaBundle;
                    }

                    SchemaSlotDescription slotDescription = new SchemaSlotDescription(slotName);

                    slotDescription.setLayout(layout);
                    slotDescription.setKey(key);
                    slotDescription.setArg0(arg0);
                    slotDescription.setBundle(bundle);
                    slotDescription.setProperties(properties);
                    slotDescription.setSchema(slotSchema);
                    slotDescription.setValidators(validators);
                    slotDescription.setConverter(converter);
                    slotDescription.setDefaultValue(defaultValue);
                    slotDescription.setReadOnly(readOnly);
                    slotDescription.setHidden(hidden);
                    slotDescription.setHelpLabel(helpLabelValue);

                    slotDescription.setDescription(description);
                    slotDescription.setDescriptionFormat(descriptionFormat);

                    schema.addSlotDescription(slotDescription);
                }

                Signature construtorSignature = null;
                if (constructor != null) {
                    construtorSignature = parseSignature(schema, constructor);

                    if (construtorSignature != null) {
                        for (SignatureParameter parameter : construtorSignature.getParameters()) {
                            SchemaSlotDescription slotDescription = parameter.getSlotDescription();

                            if (parameter.getSlotDescription() != null) {
                                slotDescription.setSetterIgnored(true);
                            }
                        }
                    }
                }

                schema.setConstructor(construtorSignature);

                NodeList setterElements = schemaElement.getElementsByTagName("setter");

                if (setterElements.getLength() > 0) {
                    schema.getSpecialSetters().clear();
                }

                for (int l = 0; l < setterElements.getLength(); l++) {
                    Element setterElement = (Element) setterElements.item(l);

                    String signature = setterElement.getAttribute("signature");

                    Signature setterSignature = parseSignature(schema, signature);
                    if (setterSignature != null) {
                        for (SignatureParameter parameter : setterSignature.getParameters()) {
                            parameter.getSlotDescription().setSetterIgnored(true);
                        }

                        schema.addSpecialSetter(setterSignature);
                    }
                }

                if (refinedSchema != null) {
                    schema = new Schema(schemaName, type, refinedSchema);
                    schema.setConstructor(refinedSchema.getConstructor());
                }

                logger.debug("adding new schema: {}", schema.getName());
                RenderKit.getInstance().registerSchema(schema);
            }
        }
    }

    private static Signature parseSignature(Schema schema, String signature) {

        String name;
        String parameters;

        int indexOfStartParent = signature.indexOf("(");
        if (indexOfStartParent != -1) {
            name = signature.substring(0, indexOfStartParent).trim();

            int indexOfCloseParen = signature.indexOf(")", indexOfStartParent);

            if (indexOfCloseParen == -1) {
                logger.error("in schema {}: malformed signature '{}', missing ')'", schema.getName(), signature);
                return null;
            }

            parameters = signature.substring(indexOfStartParent + 1, indexOfCloseParen);
        } else {
            name = null;
            parameters = signature.trim();
        }

        Signature programmaticSignature = new Signature(name);
        if (parameters.trim().length() == 0) {
            return programmaticSignature;
        }

        String[] allParameters = parameters.split(",");
        for (String allParameter : allParameters) {
            String singleParameter = allParameter.trim();

            String slotName;
            String typeName;

            int index = singleParameter.indexOf(":");
            if (index != -1) {
                slotName = singleParameter.substring(0, index).trim();
                typeName = singleParameter.substring(index + 1).trim();
            } else {
                slotName = singleParameter;
                typeName = null;
            }

            SchemaSlotDescription slotDescription = schema.getSlotDescription(slotName);
            if (slotDescription == null) {
                logger.error("in schema {}: malformed signature '{}', slot '{}' is not defined", schema.getName(), signature,
                        slotName);
            }

            Class slotType;

            if (typeName != null) {
                try {
                    slotType = getClassForType(typeName, false);
                } catch (ClassNotFoundException e) {
                    logger.error("in schema {}: malformed signature '{}', could not find type '{}'", schema.getName(), signature,
                            typeName);
                    return null;
                }
            } else {
                slotType = RendererPropertyUtils.getPropertyType(schema.getType(), slotName);
            }

            programmaticSignature.addParameter(slotDescription, slotType);
        }

        return programmaticSignature;
    }

    private static Properties getPropertiesFromElement(Element element) {
        Properties properties = new Properties();

        NodeList propertyElements = element.getElementsByTagName("property");
        for (int i = 0; i < propertyElements.getLength(); i++) {
            Element propertyElement = (Element) propertyElements.item(i);

            String name = propertyElement.getAttribute("name");
            String value = propertyElement.getAttribute("value");

            if (value == null && !propertyElement.getTextContent().isEmpty()) {
                value = propertyElement.getTextContent();
            }

            if (value != null) {
                properties.setProperty(name, value);
            }
        }

        return properties;
    }

    public static void readRenderers(ServletContext context, InputStream renderConfig) {
        Element root = readConfigRootElement(context, renderConfig);

        if (root != null) {
            NodeList renderers = root.getElementsByTagName("renderer");

            for (int i = 0; i < renderers.getLength(); i++) {
                Element rendererElement = (Element) renderers.item(i);

                String type = rendererElement.getAttribute("type");
                String layout = rendererElement.getAttribute("layout");
                String className = rendererElement.getAttribute("class");

                Properties rendererProperties = getPropertiesFromElement(rendererElement);

                try {
                    Class<?> objectClass = getClassForType(type, true);
                    Class rendererClass = Class.forName(className);

                    String modeName = rendererElement.getAttribute("mode");
                    if (modeName.isEmpty()) {
                        modeName = "output";
                    }

                    RenderMode mode = RenderMode.valueOf(modeName.toUpperCase());

                    if (hasRenderer(layout, objectClass, mode)) {
                        logger.warn("[{}] duplicated definition for type '{}' and layout '{}'", modeName, objectClass, layout);
                    }

                    logger.debug("[{}] adding new renderer: {}/{}/{}/{}", modeName, objectClass, layout, rendererClass,
                            rendererProperties);
                    RenderKit.getInstance().registerRenderer(mode, objectClass, layout, rendererClass, rendererProperties);
                } catch (ClassNotFoundException e) {
                    logger.error("could not register new renderer: " + e);
                }
            }
        }
    }

    private static boolean hasRenderer(String layout, Class objectClass, RenderMode mode) {
        try {
            return RenderKit.getInstance().getExactRendererDescription(mode, objectClass, layout) != null;
        } catch (NoRendererException e) {
            return false;
        }
    }

    private static Class getClassForType(String type, boolean prefixedLangPackage) throws ClassNotFoundException {
        String[] primitiveTypesNames = { "void", "boolean", "byte", "short", "int", "long", "char", "float", "double" };
        Class[] primitiveTypesClass =
                { Void.TYPE, Boolean.TYPE, Byte.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE, Character.TYPE, Float.TYPE,
                        Double.TYPE };

        for (int i = 0; i < primitiveTypesNames.length; i++) {
            if (type.equals(primitiveTypesNames[i])) {
                return primitiveTypesClass[i];
            }
        }

        if (!prefixedLangPackage && type.indexOf(".") == -1) {
            return Class.forName("java.lang." + type);
        }
        return Class.forName(type);
    }

    private static Element readConfigRootElement(final ServletContext context, InputStream config) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(config).getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void readAll(ServletContext context) {
        RendererPropertyUtils.initCache();

        for (Project project : FenixFramework.getProject().getProjects()) {
            InputStream renderConfig = context.getResourceAsStream("/WEB-INF/" + project.getName() + "/renderers-config.xml");
            if (renderConfig != null) {
                ConfigurationReader.readRenderers(context, renderConfig);
            }
            InputStream schemaConfig = context.getResourceAsStream("/WEB-INF/" + project.getName() + "/schemas-config.xml");
            if (schemaConfig != null) {
                ConfigurationReader.readSchemas(context, schemaConfig);
            }
        }

        RendererPropertyUtils.destroyCache();
    }
}
