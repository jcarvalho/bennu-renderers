package pt.ist.fenixWebFramework.rendererExtensions.util;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;

import com.google.common.base.Strings;

public class LabelFormatter implements Serializable {

    public final static String ENUMERATION_RESOURCES = "enum";

    public final static String APPLICATION_RESOURCES = "application";

    private static class Label implements Serializable {
        private String key;

        private String bundle;

        private final String[] args;

        public Label(String bundle, String key, String... args) {
            super();
            this.bundle = bundle;
            this.key = key;
            this.args = args;
        }

        public String getBundle() {
            return bundle;
        }

        public void setBundle(String bundle) {
            this.bundle = bundle;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public boolean isUseBundle() {
            return !StringUtils.isEmpty(this.bundle);
        }

        public String[] getArgs() {
            return this.args;
        }

    }

    private final List<Label> labels;

    public LabelFormatter() {
        this.labels = new ArrayList<Label>();
    }

    public LabelFormatter(String text) {
        this();
        appendLabel(text);
    }

    public LabelFormatter(final String key, final String bundle) {
        this();
        appendLabel(key, bundle);
    }

    public LabelFormatter(final String bundle, final String key, final String... args) {
        this();
        appendLabel(bundle, key, args);
    }

    public LabelFormatter appendLabel(String text) {
        this.labels.add(new Label(null, text));

        return this;
    }

    public LabelFormatter appendLabel(String key, String bundle) {
        this.labels.add(new Label(bundle, key));

        return this;
    }

    public LabelFormatter appendLabel(String bundle, String key, String... args) {
        this.labels.add(new Label(bundle, key, args));

        return this;
    }

    @Override
    public String toString() {
        return toString(new Properties());
    }

    public String toString(Properties properties) {
        final StringBuilder result = new StringBuilder();

        for (final Label label : getLabels()) {
            if (label.isUseBundle()) {
                result.append(getMessage(properties, label.getBundle(), label.getKey(), label.getArgs()));
            } else {
                result.append(label.getKey());
            }
        }

        return result.toString();

    }

    public String getMessage(Properties props, String bundle, String key, String... args) {
        if (props.containsKey(bundle)) {
            return format(RenderUtils.getResourceString(props.getProperty(bundle), key), args);
        } else {
            return format(RenderUtils.getResourceString(bundle, key), args);
        }
    }

    protected String format(final String value, final String[] args) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }

        if (args == null || args.length == 0) {
            return value;
        }

        return MessageFormat.format(value, (Object[]) args);

    }

    public List<Label> getLabels() {
        return Collections.unmodifiableList(this.labels);
    }

    public boolean isEmpty() {
        return this.labels.isEmpty();
    }

}