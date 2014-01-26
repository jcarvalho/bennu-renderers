package pt.ist.fenixWebFramework.renderers.plugin;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.RequestProcessor;
import org.fenixedu.bennu.portal.RenderersAnnotationProcessor;
import org.fenixedu.bennu.portal.domain.MenuContainer;
import org.fenixedu.bennu.portal.domain.MenuFunctionality;
import org.fenixedu.bennu.portal.domain.MenuItem;
import org.fenixedu.bennu.portal.domain.PortalConfiguration;
import org.fenixedu.bennu.portal.servlet.BennuPortalDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixWebFramework._development.LogLevel;
import pt.ist.fenixWebFramework.renderers.components.state.ComponentLifeCycle;
import pt.ist.fenixWebFramework.renderers.components.state.EditRequest.ViewStateUserChangedException;
import pt.ist.fenixWebFramework.renderers.components.state.IViewState;
import pt.ist.fenixWebFramework.renderers.components.state.ViewDestination;
import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;

/**
 * The standard renderers request processor. This processor is responsible for
 * handling any viewstate present in the request. It will parse the request,
 * retrieve all viewstates, and start the necessary lifecycle associated with
 * them before continuing with the standard struts processing.
 * 
 * <p>
 * If any exception is thrown during the processing of a viewstate it will be handled by struts like if the exceptions occured in
 * the destiny action. This default behaviour can be overriden by making the destiny action implement the
 * {@link pt.ist.fenixWebFramework.renderers.plugin.ExceptionHandler} interface.
 * 
 * <p>
 * The processor ensures that the current request and context are available through {@link #getCurrentRequest()} and
 * {@link #getCurrentContext()} respectively during the entire request lifetime. The processor also process multipart requests to
 * allow any renderer to retrieve on uploaded file with {@link #getUploadedFile(String)}.
 * 
 * <p>
 * This processor extends the tiles processor to easily integrate in an application that uses the tiles plugin.
 * 
 * @author cfgi
 */
public class SimpleRenderersRequestProcessor extends RequestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SimpleRenderersRequestProcessor.class);

    @Override
    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        RenderersRequestProcessorImpl.currentRequest.set(request);
        RenderersRequestProcessorImpl.currentContext.set(getServletContext());

        try {
            super.process(request, response);
        } finally {
            RenderersRequestProcessorImpl.currentRequest.set(null);
            RenderersRequestProcessorImpl.currentContext.set(null);
            RenderersRequestProcessorImpl.fileItems.set(null);
        }
    }

    @Override
    protected Action processActionCreate(HttpServletRequest request, HttpServletResponse response, ActionMapping mapping)
            throws IOException {
        Action action = super.processActionCreate(request, response, mapping);

        if (action == null) {
            return new VoidAction();
        }

        return action;
    }

    @Override
    protected ActionForward processActionPerform(HttpServletRequest request, HttpServletResponse response, Action action,
            ActionForm form, ActionMapping mapping) throws IOException, ServletException {
        chooseSelectedFunctionality(request, action);
        RenderersRequestProcessorImpl.currentRequest.set(RenderersRequestProcessorImpl.parseMultipartRequest(request, form));
        HttpServletRequest initialRequest = RenderersRequestProcessorImpl.currentRequest.get();

        if (RenderersRequestProcessorImpl.hasViewState(initialRequest)) {
            try {
                RenderersRequestProcessorImpl.setViewStateProcessed(request);

                ActionForward forward = ComponentLifeCycle.execute(initialRequest);
                if (forward != null) {
                    return forward;
                }

                return super.processActionPerform(request, response, action, form, mapping);
            } catch (ViewStateUserChangedException e) {
                throw e;
            } catch (Exception e) {
                if (LogLevel.WARN) {
                    System.out.println(SimpleDateFormat.getInstance().format(new Date()));
                }
                e.printStackTrace();

                if (action instanceof ExceptionHandler) {
                    ExceptionHandler handler = (ExceptionHandler) action;

                    ActionForward input = null;

                    IViewState viewState = RenderUtils.getViewState();
                    if (viewState != null) {
                        ViewDestination destination = viewState.getInputDestination();
                        input = destination.getActionForward();
                    }

                    ActionForward forward = handler.processException(request, mapping, input, e);
                    if (forward != null) {
                        return forward;
                    } else {
//			return processException(request, response, e, form, mapping);
                    }
                } else {
//		    return processException(request, response, e, form, mapping);
                }
                throw new ServletException(e);
            }
        } else {
            return super.processActionPerform(request, response, action, form, mapping);
        }

    }

    private void chooseSelectedFunctionality(HttpServletRequest request, Action action) {
        if (request.getAttribute(BennuPortalDispatcher.SELECTED_FUNCTIONALITY) == null) {
            MenuFunctionality functionality =
                    findFunctionalityWithKey(PortalConfiguration.getInstance().getMenu(), RenderersAnnotationProcessor
                            .getFunctionalityForType(action.getClass()).getKey());
            logger.debug("Selected MenuFunctionality {}", functionality);
            request.setAttribute(BennuPortalDispatcher.SELECTED_FUNCTIONALITY, functionality);
        }
    }

    private MenuFunctionality findFunctionalityWithKey(MenuContainer container, String key) {
        for (MenuItem item : container.getChildSet()) {
            if (item instanceof MenuFunctionality) {
                MenuFunctionality functionality = (MenuFunctionality) item;
                if (functionality.getItemKey().equals(key)) {
                    return functionality;
                }
            } else {
                MenuFunctionality functionality = findFunctionalityWithKey((MenuContainer) item, key);
                if (functionality != null) {
                    return functionality;
                }
            }
        }
        throw new RuntimeException("Trying to access a non installed functionality!");
    }

}
