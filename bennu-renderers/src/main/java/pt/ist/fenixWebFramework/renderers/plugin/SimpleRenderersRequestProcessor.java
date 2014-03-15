package pt.ist.fenixWebFramework.renderers.plugin;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.RequestProcessor;
import org.fenixedu.bennu.portal.StrutsPortalBackend;

import pt.ist.fenixWebFramework.RenderersConfigurationManager;
import pt.ist.fenixWebFramework.renderers.components.state.ComponentLifeCycle;
import pt.ist.fenixWebFramework.renderers.components.state.ComponentLifeCycle.ViewStateUserChangedException;
import pt.ist.fenixWebFramework.renderers.components.state.IViewState;
import pt.ist.fenixWebFramework.renderers.components.state.LifeCycleConstants;
import pt.ist.fenixWebFramework.renderers.components.state.ViewDestination;
import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixWebFramework.servlets.commons.UploadedFile;
import pt.ist.fenixWebFramework.servlets.filters.RequestWrapperFilter.FenixHttpServletRequestWrapper;

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
 * The processor ensures that the current request is available through {@link #getCurrentRequest()} during the entire request
 * lifetime. The processor also process multipart requests to allow any renderer to retrieve on uploaded file with
 * {@link #getUploadedFile(String)}.
 * 
 * @author cfgi
 */
public class SimpleRenderersRequestProcessor extends RequestProcessor {

    private static ThreadLocal<HttpServletRequest> currentRequest = new ThreadLocal<HttpServletRequest>();

    @Override
    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            currentRequest.set(request);
            super.process(request, response);
        } finally {
            currentRequest.remove();
        }
    }

    @Override
    protected Action processActionCreate(HttpServletRequest request, HttpServletResponse response, ActionMapping mapping)
            throws IOException {
        Action action = super.processActionCreate(request, response, mapping);
        return action == null ? new Action() : action;
    }

    @Override
    protected ActionForward processActionPerform(HttpServletRequest request, HttpServletResponse response, Action action,
            ActionForm form, ActionMapping mapping) throws IOException, ServletException {
        StrutsPortalBackend.chooseSelectedFunctionality(request, action.getClass());

        if (hasViewState(request)) {
            try {
                setViewStateProcessed(request);

                ActionForward forward = ComponentLifeCycle.execute(request);
                if (forward != null) {
                    return forward;
                }

                return super.processActionPerform(request, response, action, form, mapping);
            } catch (ViewStateUserChangedException e) {
                response.sendRedirect(RenderersConfigurationManager.getConfiguration().tamperingRedirect());
                return null;
            } catch (Exception e) {
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
                    }
                }
                throw new ServletException(e);
            }
        } else {
            return super.processActionPerform(request, response, action, form, mapping);
        }
    }

    public static HttpServletRequest getCurrentRequest() {
        return currentRequest.get();
    }

    /**
     * @return the form file associated with the given field name or <code>null</code> if no file exists
     */
    @SuppressWarnings("unchecked")
    public static UploadedFile getUploadedFile(String fieldName) {
        Map<String, UploadedFile> files =
                (Map<String, UploadedFile>) getCurrentRequest().getAttribute(FenixHttpServletRequestWrapper.ITEM_MAP_ATTRIBUTE);
        return files.get(fieldName);
    }

    protected static boolean hasViewState(HttpServletRequest request) {
        return request.getAttribute(LifeCycleConstants.PROCESSED_PARAM_NAME) == null
                && request.getParameterValues(LifeCycleConstants.VIEWSTATE_PARAM_NAME) != null;
    }

    protected static void setViewStateProcessed(HttpServletRequest request) {
        request.setAttribute(LifeCycleConstants.PROCESSED_PARAM_NAME, true);
    }

}
