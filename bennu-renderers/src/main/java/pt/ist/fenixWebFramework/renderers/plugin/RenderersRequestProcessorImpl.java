/**
 * Copyright © 2008 Instituto Superior Técnico
 *
 * This file is part of Bennu Renderers Framework.
 *
 * Bennu Renderers Framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bennu Renderers Framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Bennu Renderers Framework.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixWebFramework.renderers.plugin;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import pt.ist.fenixWebFramework.renderers.components.state.LifeCycleConstants;
import pt.ist.fenixWebFramework.servlets.commons.PartUploadedFile;
import pt.ist.fenixWebFramework.servlets.commons.UploadedFile;

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
 * 
 * @author cfgi
 */
public class RenderersRequestProcessorImpl {

    static final ThreadLocal<HttpServletRequest> currentRequest = new ThreadLocal<>();

    public static HttpServletRequest getCurrentRequest() {
        return currentRequest.get();
    }

    /**
     * @return the form file associated with the given field name or <code>null</code> if no file exists
     * 
     * @deprecated Use {@link HttpServletRequest.getPart(String)} instead
     */
    @Deprecated
    public static UploadedFile getUploadedFile(String fieldName) {
        try {
            Part part = currentRequest.get().getPart(fieldName);
            return part == null ? null : new PartUploadedFile(part);
        } catch (IOException | ServletException e) {
            return null;
        }
    }

    public static String getCurrentEncoding() {
        HttpServletRequest currentRequest = getCurrentRequest();
        return currentRequest != null ? currentRequest.getCharacterEncoding() : null;
    }

    protected static boolean hasViewState(HttpServletRequest request) {
        return viewStateNotProcessed(request)
                && (request.getParameterValues(LifeCycleConstants.VIEWSTATE_PARAM_NAME) != null || request
                        .getParameterValues(LifeCycleConstants.VIEWSTATE_LIST_PARAM_NAME) != null);
    }

    protected static boolean viewStateNotProcessed(HttpServletRequest request) {
        return request.getAttribute(LifeCycleConstants.PROCESSED_PARAM_NAME) == null;
    }

    protected static void setViewStateProcessed(HttpServletRequest request) {
        request.setAttribute(LifeCycleConstants.PROCESSED_PARAM_NAME, true);
    }
}
