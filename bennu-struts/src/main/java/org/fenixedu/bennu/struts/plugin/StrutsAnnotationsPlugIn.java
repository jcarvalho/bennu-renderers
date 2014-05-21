/**
 * 
 */
package org.fenixedu.bennu.struts.plugin;

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
import org.fenixedu.bennu.struts.annotations.ExceptionHandling;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Mapping;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

/**
 * @author - Shezad Anavarali (shezad@ist.utl.pt)
 * 
 */
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
                actionMapping.setInput(mapping.path() + ".do?page=0&method=prepare");
            } else {
                actionMapping.setInput(mapping.input());
            }

            registerSuperclassForwards(actionMapping, actionClass);

            registerExceptionHandling(actionMapping, actionClass);

            config.addActionConfig(actionMapping);

        }
    }

    private static void registerExceptionHandling(final ActionMapping actionMapping, Class<?> actionClass) {
        for (final ExceptionHandling exception : actionClass.getAnnotationsByType(ExceptionHandling.class)) {
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

    @SuppressWarnings("deprecation")
    private static void registerSuperclassForwards(final ActionMapping actionMapping, Class<?> superclass) {
        if (UPPER_BOUND_SUPERCLASSES.contains(superclass)) {
            return;
        }
        for (final Forward forward : superclass.getAnnotationsByType(Forward.class)) {
            try {
                actionMapping.findForward(forward.name());
            } catch (NullPointerException ex) {
                // Forward wasn't registered in any subclass, so register it.
                actionMapping.addForwardConfig(new ActionForward(forward.name(), forward.path(), forward.redirect(), forward
                        .contextRelative()));
            }
        }
        registerSuperclassForwards(actionMapping, superclass.getSuperclass());
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