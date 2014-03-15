/**
 * 
 */
package pt.ist.fenixWebFramework.struts.plugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.ExceptionHandler;
import org.apache.struts.action.PlugIn;
import org.apache.struts.actions.DispatchAction;
import org.apache.struts.config.ExceptionConfig;
import org.apache.struts.config.FormBeanConfig;
import org.apache.struts.config.ModuleConfig;

import pt.ist.fenixWebFramework.struts.annotations.ExceptionHandling;
import pt.ist.fenixWebFramework.struts.annotations.Exceptions;
import pt.ist.fenixWebFramework.struts.annotations.Forward;
import pt.ist.fenixWebFramework.struts.annotations.Forwards;
import pt.ist.fenixWebFramework.struts.annotations.Mapping;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

/**
 * @author - Shezad Anavarali (shezad@ist.utl.pt)
 * 
 */
@SuppressWarnings("deprecation")
public class StrutsAnnotationsPlugIn implements PlugIn {

    private static final List<Class<?>> UPPER_BOUND_SUPERCLASSES = Arrays
            .asList(DispatchAction.class, Action.class, Object.class);

    private static final Set<Class<?>> actionClasses = new HashSet<Class<?>>();

    @Override
    public void destroy() {
    }

    @Override
    public void init(ActionServlet servlet, ModuleConfig config) throws ServletException {

        final String modulePrefix = CharMatcher.is('/').trimLeadingFrom(config.getPrefix());

        for (Class<?> actionClass : actionClasses) {
            Mapping mapping = actionClass.getAnnotation(Mapping.class);
            if (mapping == null || !modulePrefix.equals(mapping.module())) {
                continue;
            }

            final ActionMapping actionMapping = new ActionMapping();

            actionMapping.setPath(mapping.path());
            actionMapping.setType(actionClass.getName());
            actionMapping.setScope(mapping.scope());
            actionMapping.setParameter(mapping.parameter());
            actionMapping.setValidate(mapping.validate());

            if (mapping.formBeanClass() != ActionForm.class) {
                final String formName = mapping.formBeanClass().getName();
                createFormBeanConfigIfNecessary(config, mapping, formName);
                actionMapping.setName(formName);
            } else if (!mapping.formBean().isEmpty()) {
                actionMapping.setName(mapping.formBean());
            }

            if (mapping.input().isEmpty()) {
                actionMapping.setInput(mapping.path() + ".do?&method=prepare");
            } else {
                actionMapping.setInput(mapping.input());
            }

            Forwards forwards = actionClass.getAnnotation(Forwards.class);
            if (forwards != null) {
                for (final Forward forward : forwards.value()) {
                    registerForward(actionMapping, forward);
                }
            }
            registerSuperclassForwards(actionMapping, actionClass.getSuperclass());

            Exceptions exceptions = actionClass.getAnnotation(Exceptions.class);
            if (exceptions != null) {
                registerExceptionHandling(actionMapping, exceptions);
            }

            config.addActionConfig(actionMapping);
        }
    }

    private static void registerExceptionHandling(final ActionMapping actionMapping, Exceptions exceptions) {
        for (final ExceptionHandling exception : exceptions.value()) {
            final ExceptionConfig exceptionConfig = new ExceptionConfig();

            Class<? extends Exception> exClass = exception.type();
            Class<? extends ExceptionHandler> handlerClass = exception.handler();

            exceptionConfig.setKey(Strings.emptyToNull(exception.key()));
            exceptionConfig.setHandler(handlerClass.getName());
            exceptionConfig.setType(exClass.getName());

            if (!Strings.isNullOrEmpty(exception.path())) {
                exceptionConfig.setPath(exception.path());
            }

            if (!Strings.isNullOrEmpty(exception.scope())) {
                exceptionConfig.setScope(exception.scope());
            }

            actionMapping.addExceptionConfig(exceptionConfig);
        }
    }

    private static void registerSuperclassForwards(final ActionMapping actionMapping, Class<?> superclass) {
        if (UPPER_BOUND_SUPERCLASSES.contains(superclass)) {
            return;
        }
        Forwards forwards = superclass.getAnnotation(Forwards.class);
        if (forwards != null) {
            for (final Forward forward : forwards.value()) {
                try {
                    actionMapping.findForward(forward.name());
                } catch (NullPointerException ex) {
                    // Forward wasn't registered in any subclass, so register it.
                    registerForward(actionMapping, forward);
                }
            }
        }
        registerSuperclassForwards(actionMapping, superclass.getSuperclass());
    }

    private static void registerForward(ActionMapping actionMapping, Forward forward) {
        actionMapping.addForwardConfig(new ActionForward(forward.name(), forward.path(), forward.redirect(), forward
                .contextRelative()));
    }

    private void createFormBeanConfigIfNecessary(ModuleConfig config, Mapping mapping, final String formName) {
        FormBeanConfig formBeanConfig = config.findFormBeanConfig(formName);
        if (formBeanConfig == null) {
            formBeanConfig = new FormBeanConfig();
            formBeanConfig.setType(mapping.formBeanClass().getName());
            formBeanConfig.setName(formName);
            config.addFormBeanConfig(formBeanConfig);
        }
    }

    public static void registerMapping(Class<?> type) {
        actionClasses.add(type);
    }
}