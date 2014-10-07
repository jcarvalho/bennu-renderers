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
package org.fenixedu.bennu.struts;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.RequestProcessor;
import org.fenixedu.bennu.struts.base.ExceptionHandler;
import org.fenixedu.bennu.struts.portal.StrutsPortalBackend;
import org.fenixedu.commons.i18n.I18N;

import pt.ist.fenixWebFramework.RenderersConfigurationManager;
import pt.ist.fenixWebFramework.renderers.components.state.ComponentLifeCycle;
import pt.ist.fenixWebFramework.renderers.components.state.EditRequest.ViewStateUserChangedException;
import pt.ist.fenixWebFramework.renderers.components.state.IViewState;
import pt.ist.fenixWebFramework.renderers.components.state.LifeCycleConstants;
import pt.ist.fenixWebFramework.renderers.components.state.ViewDestination;
import pt.ist.fenixWebFramework.renderers.plugin.RenderersRequestProcessorImpl;
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

    @Override
    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        RenderersRequestProcessorImpl.currentRequest.set(request);
        try {
            super.process(request, response);
        } finally {
            RenderersRequestProcessorImpl.currentRequest.set(null);
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
        if (!StrutsPortalBackend.chooseSelectedFunctionality(request, action.getClass())) {
            return new ActionForward("unauthorized", "/bennu-renderers/unauthorized.jsp", false, "");
        }

        updateLocaleForStruts(request);

        if (hasViewState(request)) {
            try {
                setViewStateProcessed(request);

                ViewDestination forward = ComponentLifeCycle.execute(request);
                if (forward != null) {
                    return forwardForDestination(forward);
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
                        input = forwardForDestination(destination);
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

    private ActionForward forwardForDestination(ViewDestination destination) {
        if (destination == null) {
            return null;
        }

        ActionForward forward = new ActionForward();

        forward.setModule(destination.getModule());
        forward.setPath(destination.getPath());
        forward.setRedirect(destination.getRedirect());
        return forward;
    }

    private static boolean hasViewState(HttpServletRequest request) {
        return viewStateNotProcessed(request)
                && (request.getParameterValues(LifeCycleConstants.VIEWSTATE_PARAM_NAME) != null || request
                        .getParameterValues(LifeCycleConstants.VIEWSTATE_LIST_PARAM_NAME) != null);
    }

    private static boolean viewStateNotProcessed(HttpServletRequest request) {
        return request.getAttribute(LifeCycleConstants.PROCESSED_PARAM_NAME) == null;
    }

    private static void setViewStateProcessed(HttpServletRequest request) {
        request.setAttribute(LifeCycleConstants.PROCESSED_PARAM_NAME, true);
    }

    private static void updateLocaleForStruts(HttpServletRequest request) {
        Locale locale = I18N.getLocale();
        HttpSession session = request.getSession(false);
        if (session != null && !Objects.equals(session.getAttribute(Globals.LOCALE_KEY), locale)) {
            session.setAttribute(Globals.LOCALE_KEY, locale);
        }
        request.setAttribute(Globals.LOCALE_KEY, locale);
    }

}
