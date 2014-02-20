package org.fenixedu.bennu.portal;

import javax.servlet.http.HttpServletRequest;

import org.fenixedu.bennu.portal.domain.MenuFunctionality;
import org.fenixedu.bennu.portal.model.Functionality;
import org.fenixedu.bennu.portal.servlet.BennuPortalDispatcher;
import org.fenixedu.bennu.portal.servlet.PortalBackend;
import org.fenixedu.bennu.portal.servlet.SemanticURLHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrutsPortalBackend implements PortalBackend {

    private static final Logger logger = LoggerFactory.getLogger(StrutsPortalBackend.class);

    public static final String BACKEND_KEY = "struts";

    private final SemanticURLHandler handler = new StrutsSemanticURLHandler();

    @Override
    public SemanticURLHandler getSemanticURLHandler() {
        return handler;
    }

    @Override
    public boolean requiresServerSideLayout() {
        return true;
    }

    @Override
    public String getBackendKey() {
        return BACKEND_KEY;
    }

    public static void chooseSelectedFunctionality(HttpServletRequest request, Class<?> actionClass) {
        if (request.getAttribute(BennuPortalDispatcher.SELECTED_FUNCTIONALITY) == null) {
            Functionality model = RenderersAnnotationProcessor.getFunctionalityForType(actionClass);
            if (model == null) {
                logger.warn("Could not map {} to a functionality!", actionClass.getName());
                return;
            }
            MenuFunctionality functionality = MenuFunctionality.findFunctionality(BACKEND_KEY, model.getKey());
            if (functionality == null) {
                logger.warn("Trying to access a not installed functionality!");
            }
            logger.debug("Selected MenuFunctionality {}", functionality);
            request.setAttribute(BennuPortalDispatcher.SELECTED_FUNCTIONALITY, functionality);
        }
    }

}
