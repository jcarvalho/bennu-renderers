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
package pt.ist.fenixWebFramework.servlets.filters;

import java.io.IOException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Objects;
import java.util.Vector;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.commons.i18n.I18N;

/**
 * 17/Fev/2003
 * 
 * @author jpvl
 */
public class RequestWrapperFilter implements Filter {

    @Override
    public void init(FilterConfig config) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        chain.doFilter(getFenixHttpServletRequestWrapper(httpServletRequest), response);
        setSessionTimeout(httpServletRequest);
    }

    private static void updateLocaleForStruts(HttpServletRequest request) {
        Locale locale = I18N.getLocale();
        HttpSession session = request.getSession(false);
        if (session != null && !Objects.equals(session.getAttribute(Globals.LOCALE_KEY), locale)) {
            session.setAttribute(Globals.LOCALE_KEY, locale);
        }
        request.setAttribute(Globals.LOCALE_KEY, locale);
    }

    private void setSessionTimeout(final HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        if (session != null) {
            session.setMaxInactiveInterval(7200);
        }
    }

    public static FenixHttpServletRequestWrapper getFenixHttpServletRequestWrapper(final HttpServletRequest httpServletRequest) {
        updateLocaleForStruts(httpServletRequest);
        return new FenixHttpServletRequestWrapper(httpServletRequest);
    }

    public static class FenixHttpServletRequestWrapper extends HttpServletRequestWrapper {

        private static final String PAGE_DEFAULT = "0";

        private static final String[] PAGE_DEFAULT_ARRAY = { PAGE_DEFAULT };

        public FenixHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public Enumeration getParameterNames() {
            final Vector params = new Vector();

            final Enumeration paramEnum = super.getParameterNames();
            boolean gotPageParameter = false;
            while (paramEnum.hasMoreElements()) {
                final String parameterName = (String) paramEnum.nextElement();
                if (paramEnum.equals("page")) {
                    gotPageParameter = true;
                }
                params.add(parameterName);
            }
            if (!gotPageParameter) {
                params.add("page");
            }

            return params.elements();
        }

        @Override
        public String[] getParameterValues(final String parameter) {
            final String[] parameterValues = super.getParameterValues(parameter);
            return parameterValues == null && parameter.equals("page") ? PAGE_DEFAULT_ARRAY : parameterValues;
        }

        @Override
        public String getParameter(final String parameter) {
            final String parameterValue = super.getParameter(parameter);
            return parameterValue == null && parameter.equals("page") ? PAGE_DEFAULT : parameterValue;
        }

        @Override
        public boolean isUserInRole(String role) {
            return Group.parse(role).isMember(Authenticate.getUser());
        }

        @Override
        public String getRemoteUser() {
            final User user = Authenticate.getUser();
            return user == null ? super.getRemoteUser() : user.getUsername();
        }

        @Override
        public Principal getUserPrincipal() {
            return Authenticate.getUser();
        }

    }

}