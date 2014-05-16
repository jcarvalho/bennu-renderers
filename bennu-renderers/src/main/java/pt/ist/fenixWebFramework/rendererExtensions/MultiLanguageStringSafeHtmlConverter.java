package pt.ist.fenixWebFramework.rendererExtensions;

import java.util.Locale;

import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixWebFramework.rendererExtensions.MultiLanguageStringInputRenderer.MultiLanguageStringConverter;
import pt.ist.fenixWebFramework.rendererExtensions.htmlEditor.JsoupSafeHtmlConverter;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;
import pt.utl.ist.fenix.tools.util.i18n.MultiLanguageString;

public class MultiLanguageStringSafeHtmlConverter extends Converter {

    private final boolean mathJaxEnabled;

    public MultiLanguageStringSafeHtmlConverter(final boolean mathJaxEnabled) {
        this.mathJaxEnabled = mathJaxEnabled;
    }

    @Override
    public Object convert(Class type, Object value) {
        // SafeHtmlConverter safeConverter = new SafeHtmlConverter();
        Converter safeConverter = new JsoupSafeHtmlConverter(mathJaxEnabled);
        MultiLanguageStringConverter mlsConverter = new MultiLanguageStringConverter();

        LocalizedString mls = getLocalized(mlsConverter.convert(type, value));

        if (mls == null) {
            return null;
        }

        if (mls.getLocales().isEmpty()) {
            return null;
        }

        for (Locale locale : mls.getLocales()) {
            String text = (String) safeConverter.convert(String.class, mls.getContent(locale));

            if (text == null) {
                mls = mls.without(locale);
            } else {
                mls = mls.with(locale, text);
            }
        }

        return type == MultiLanguageString.class ? MultiLanguageString.fromLocalizedString(mls) : mls;
    }

    protected LocalizedString getLocalized(Object object) {
        if (object instanceof LocalizedString) {
            return (LocalizedString) object;
        } else if (object instanceof MultiLanguageString) {
            return ((MultiLanguageString) object).toLocalizedString();
        } else {
            return null;
        }
    }

}
