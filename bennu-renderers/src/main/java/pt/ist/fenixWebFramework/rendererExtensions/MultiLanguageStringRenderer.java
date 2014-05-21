package pt.ist.fenixWebFramework.rendererExtensions;

import java.util.Collection;
import java.util.Locale;
import java.util.Locale.Builder;

import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixWebFramework.renderers.StringRenderer;
import pt.ist.fenixWebFramework.renderers.components.HtmlBlockContainer;
import pt.ist.fenixWebFramework.renderers.components.HtmlComponent;
import pt.ist.fenixWebFramework.renderers.components.HtmlContainer;
import pt.ist.fenixWebFramework.renderers.components.HtmlInlineContainer;
import pt.ist.fenixWebFramework.renderers.components.HtmlText;
import pt.ist.fenixWebFramework.renderers.layouts.Layout;

/**
 * This renderer provides a standard way of presenting a {@link MultiLanguageString}. The <tt>MultiLanguageString</tt> is
 * presented as a simple string. The string to be presented
 * is determined by the logic in {@link MultiLanguageString#getContent()}. Additionally you
 * can override the language in which the content is to be displayed with the {@link #setLanguage(String) language} property. In
 * this case the content to be presented
 * will be determined by {@link MultiLanguageString#getContent(Locale)}
 *
 * @author cfgi
 * @author cgmp
 */
public class MultiLanguageStringRenderer extends StringRenderer {

    private String language;
    private boolean forceShowLanguage;
    private boolean languageShown;
    private boolean inline;
    private String languageClasses;

    public MultiLanguageStringRenderer() {
        super();

        setLanguageShown(true);
        setInline(true);
        setShowLanguageForced(false);
    }

    public String getLanguage() {
        return language;
    }

    /**
     * Allows you to override the language in wich the <tt>MultiLanguageString</tt> content
     * will be presented.
     *
     * @property
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isInline() {
        return this.inline;
    }

    /**
     * Allows you to choose if a span or a div will be generated around the multi language string.
     * This can be usefull if the multi-language string contains much information or html code.
     *
     * @property
     */
    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public boolean isLanguageShown() {
        return this.languageShown;
    }

    /**
     * Whenever a multi-language string is shown in a language that is not what the user requested
     * an annotation is added to shown in wich language the text is in. This property allows you
     * to override that behaviour.
     *
     * @property
     */
    public void setLanguageShown(boolean languageShown) {
        this.languageShown = languageShown;
    }

    public String getLanguageClasses() {
        return this.languageClasses;
    }

    /**
     * Choose the css class to apply to the annotation showing the value's
     * language when it isn't in the requested language.
     *
     * @property
     */
    public void setLanguageClasses(String languageClasses) {
        this.languageClasses = languageClasses;
    }

    public boolean isShowLanguageForced() {
        return this.forceShowLanguage;
    }

    /**
     * Force the diplay of the language of the text even when showing text in
     * the language requested by the user.
     *
     * @property
     */
    public void setShowLanguageForced(boolean forceShowLanguage) {
        this.forceShowLanguage = forceShowLanguage;
    }

    @Override
    protected HtmlComponent renderComponent(Layout layout, Object object, Class type) {
        if (object == null) {
            return super.renderComponent(layout, null, type);
        }

        LocalizedString mlString = (LocalizedString) object;
        Locale contentLocale = getContentLocale(mlString);
        String value = getRenderedText(mlString, contentLocale);

        HtmlComponent component = super.renderComponent(layout, value, type);

        if (mlString.getLocales().isEmpty()) {
            return component;
        }

        component.setLanguage(contentLocale.toLanguageTag());

        if (I18N.getLocale().equals(contentLocale) && !isShowLanguageForced()) {
            return component;
        }

        if (!isLanguageShown() && !isShowLanguageForced()) {
            return component;
        }

        HtmlContainer container = isInline() ? new HtmlInlineContainer() : new HtmlBlockContainer();
        container.addChild(component);
        container.setIndented(false);

        HtmlComponent languageComponent = contentLocale == null ? new HtmlText() : new HtmlText(contentLocale.getDisplayName());
        languageComponent.setClasses(getLanguageClasses());

        container.addChild(new HtmlText(" (", false));
        container.addChild(languageComponent);
        container.addChild(new HtmlText(")", false));

        return container;
    }

    protected Locale getContentLocale(LocalizedString str) {
        if (getLanguage() != null) {
            return new Builder().setLanguageTag(getLanguage()).build();
        }

        Collection<Locale> locales = str.getLocales();
        Locale locale = I18N.getLocale();
        if (locales.contains(locale)) {
            return locale;
        }

        Locale defaultLocale = Locale.getDefault();
        if (locales.contains(defaultLocale)) {
            return defaultLocale;
        }

        return locales.isEmpty() ? null : locales.iterator().next();
    }

    protected String getRenderedText(LocalizedString mlString, Locale locale) {
        return locale == null ? null : mlString.getContent(locale);
    }

}
