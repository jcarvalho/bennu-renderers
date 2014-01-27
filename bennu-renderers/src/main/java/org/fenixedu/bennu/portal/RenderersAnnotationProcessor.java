package org.fenixedu.bennu.portal;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import org.apache.struts.actions.DispatchAction;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.portal.model.Application;
import org.fenixedu.bennu.portal.model.ApplicationRegistry;
import org.fenixedu.bennu.portal.model.Functionality;
import org.fenixedu.bennu.portal.servlet.PortalBackendRegistry;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixWebFramework.struts.annotations.Mapping;
import pt.ist.fenixWebFramework.struts.plugin.StrutsAnnotationsPlugIn;

@HandlesTypes({ Mapping.class, StrutsApplication.class, StrutsFunctionality.class })
public class RenderersAnnotationProcessor implements ServletContainerInitializer {

    static final String DELEGATE_TO_PARENT = "DELEGATE_TO_PARENT";

    private static final Map<Class<?>, Functionality> functionalityClasses = new HashMap<Class<?>, Functionality>();

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext context) throws ServletException {
        PortalBackendRegistry.registerPortalBackend(new StrutsPortalBackend());

        if (classes != null) {
            Map<Class<?>, Application> applicationClasses = new HashMap<Class<?>, Application>();
            for (Class<?> type : classes) {
                Mapping mapping = type.getAnnotation(Mapping.class);
                if (mapping != null) {
                    StrutsAnnotationsPlugIn.registerMapping(type);
                }
                StrutsFunctionality functionality = type.getAnnotation(StrutsFunctionality.class);
                if (functionality != null) {
                    String bundle = findBundleForFunctionality(type);
                    LocalizedString title = BundleUtil.getLocalizedString(bundle, functionality.titleKey());
                    LocalizedString description = BundleUtil.getLocalizedString(bundle, functionality.descriptionKey());
                    functionalityClasses.put(type, new Functionality(StrutsPortalBackend.BACKEND_KEY, computePath(type),
                            functionality.path(), functionality.accessGroup(), title, description));
                }
                StrutsApplication application = type.getAnnotation(StrutsApplication.class);
                if (application != null) {
                    String bundle = findBundleForApplication(type);
                    LocalizedString title = BundleUtil.getLocalizedString(bundle, application.titleKey());
                    LocalizedString description = BundleUtil.getLocalizedString(bundle, application.descriptionKey());
                    applicationClasses.put(
                            type,
                            new Application(StrutsPortalBackend.BACKEND_KEY, type.getName(), application.path(), application
                                    .accessGroup(), title, description));
                }
            }
            for (Entry<Class<?>, Functionality> entry : functionalityClasses.entrySet()) {
                Application app = applicationClasses.get(entry.getKey().getAnnotation(StrutsFunctionality.class).application());
                if (app == null) {
                    throw new Error("Functionality " + entry.getKey().getName() + " does not have a defined application");
                }
                app.addFunctionality(entry.getValue());
            }

            for (Entry<Class<?>, Application> entry : applicationClasses.entrySet()) {
                Class<?> parent = entry.getKey().getAnnotation(StrutsApplication.class).parent();
                if (parent.equals(Object.class)) {
                    // Application has no parent
                    continue;
                }
                applicationClasses.get(parent).addSubApplication(entry.getValue());
            }

            for (Application app : applicationClasses.values()) {
                ApplicationRegistry.registerApplication(app);
            }

            // TODO: Finish filling the functionality map
        }
    }

    private String findBundleForApplication(Class<?> type) {
        StrutsApplication app = type.getAnnotation(StrutsApplication.class);
        if (app == null) {
            throw new Error("Cannot determine bundle for " + type.getName());
        }
        if (!app.bundle().equals(DELEGATE_TO_PARENT)) {
            return app.bundle();
        }
        return findBundleForApplication(app.parent());
    }

    private String findBundleForFunctionality(Class<?> type) {
        StrutsFunctionality functionality = type.getAnnotation(StrutsFunctionality.class);
        if (!functionality.bundle().equals(DELEGATE_TO_PARENT)) {
            return functionality.bundle();
        }
        return findBundleForApplication(functionality.application());
    }

    private String computePath(Class<?> type) {
        Mapping mapping = type.getAnnotation(Mapping.class);
        StringBuilder path = new StringBuilder();

        if (!mapping.module().equals("")) {
            path.append('/');
            path.append(mapping.module());
        }

        path.append(mapping.path());
        path.append(".do");

        if (DispatchAction.class.isAssignableFrom(type)) {
            path.append('?');
            path.append(mapping.parameter());
            path.append('=');
            path.append(findEntryPoint(type));
        }

        return path.toString();
    }

    private String findEntryPoint(Class<?> type) {
        while (type != DispatchAction.class) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(EntryPoint.class)) {
                    return method.getName();
                }
            }
            type = type.getSuperclass();
        }
        throw new Error("Functionality class " + type + " does not have a entry point!");
    }

    public static Functionality getFunctionalityForType(Class<?> actionClass) {
        return functionalityClasses.get(actionClass);
    }

}
