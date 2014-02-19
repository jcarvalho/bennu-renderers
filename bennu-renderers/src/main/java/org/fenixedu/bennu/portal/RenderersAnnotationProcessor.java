package org.fenixedu.bennu.portal;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
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

    static final String DELEGATE_TO_PARENT = "$DELEGATE_TO_PARENT$";
    static final String INFER_VALUE = "$INFER_VALUE$";

    private static final Map<Class<?>, Functionality> functionalityClasses = new HashMap<Class<?>, Functionality>();

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext context) throws ServletException {
        PortalBackendRegistry.registerPortalBackend(new StrutsPortalBackend());

        if (classes != null) {
            Map<Class<?>, Application> applicationClasses = new HashMap<Class<?>, Application>();
            Set<Class<?>> actionsWithoutFunctionality = new HashSet<>();
            for (Class<?> type : classes) {
                Mapping mapping = type.getAnnotation(Mapping.class);
                if (mapping != null) {
                    StrutsAnnotationsPlugIn.registerMapping(type);
                    if (mapping.functionality() != Object.class) {
                        actionsWithoutFunctionality.add(type);
                    }
                }
                StrutsFunctionality functionality = type.getAnnotation(StrutsFunctionality.class);
                if (functionality != null) {
                    String bundle = "resources." + findBundleForFunctionality(type);
                    LocalizedString title = BundleUtil.getLocalizedString(bundle, functionality.titleKey());
                    LocalizedString description =
                            functionality.descriptionKey().equals(INFER_VALUE) ? title : BundleUtil.getLocalizedString(bundle,
                                    functionality.descriptionKey());
                    functionalityClasses.put(type, new Functionality(StrutsPortalBackend.BACKEND_KEY, computePath(type),
                            functionality.path(), functionality.accessGroup(), title, description));
                }
                StrutsApplication application = type.getAnnotation(StrutsApplication.class);
                if (application != null) {
                    String bundle = "resources." + application.bundle();
                    LocalizedString title = BundleUtil.getLocalizedString(bundle, application.titleKey());
                    LocalizedString description =
                            application.descriptionKey().equals(INFER_VALUE) ? title : BundleUtil.getLocalizedString(bundle,
                                    application.descriptionKey());
                    applicationClasses.put(type, new Application(type.getName(), application.path(), application.accessGroup(),
                            title, description));
                }
            }
            for (Entry<Class<?>, Functionality> entry : functionalityClasses.entrySet()) {
                Application app = applicationClasses.get(entry.getKey().getAnnotation(StrutsFunctionality.class).app());
                if (app == null) {
                    throw new Error("Functionality " + entry.getKey().getName() + " does not have a defined application");
                }
                app.addFunctionality(entry.getValue());
            }

            for (Application app : applicationClasses.values()) {
                ApplicationRegistry.registerApplication(app);
            }

            for (Class<?> type : actionsWithoutFunctionality) {
                Class<?> functionalityType = type.getAnnotation(Mapping.class).functionality();
                Functionality functionality = functionalityClasses.get(functionalityType);
                if (functionality == null) {
                    throw new Error("Action type " + type.getName() + " declares " + functionalityType.getName()
                            + " but it is not a functionality!");
                }
                functionalityClasses.put(type, functionality);
            }
        }
    }

    private String findBundleForFunctionality(Class<?> type) {
        StrutsFunctionality functionality = type.getAnnotation(StrutsFunctionality.class);
        if (functionality.bundle().equals(DELEGATE_TO_PARENT)) {
            return functionality.app().getAnnotation(StrutsApplication.class).bundle();
        } else {
            return functionality.bundle();
        }
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
