package org.fenixedu.bennu.portal;

import org.fenixedu.bennu.portal.servlet.PortalBackend;
import org.fenixedu.bennu.portal.servlet.SemanticURLHandler;

public class StrutsPortalBackend implements PortalBackend {

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

}
