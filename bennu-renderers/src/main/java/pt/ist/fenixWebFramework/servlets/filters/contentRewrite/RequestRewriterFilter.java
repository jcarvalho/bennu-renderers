package pt.ist.fenixWebFramework.servlets.filters.contentRewrite;

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;

import pt.ist.fenixWebFramework.RenderersConfigurationManager;

@WebFilter("*")
public class RequestRewriterFilter implements Filter {

    private static final String RENDERERS_SESSION_SECRET = "RENDERERS_SESSION_SECRET";

    private static final ThreadLocal<String> currentSecret = new InheritableThreadLocal<>();

    @Override
    public void init(FilterConfig config) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain)
            throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        final ResponseWrapper responseWrapper = new ResponseWrapper(httpServletResponse);

        try {
            setSessionKey(httpServletRequest);
            filterChain.doFilter(httpServletRequest, responseWrapper);
            responseWrapper.writeRealResponse();
        } finally {
            currentSecret.remove();
        }
    }

    private void setSessionKey(HttpServletRequest request) {
        if (RenderersConfigurationManager.getConfiguration().filterRequestWithDigest()) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                SessionSecretWrapper secret = (SessionSecretWrapper) session.getAttribute(RENDERERS_SESSION_SECRET);
                if (secret != null) {
                    currentSecret.set(secret.secret);
                } else {
                    currentSecret.remove();
                }
            }
        }
    }

    static String getSessionSecret() {
        return currentSecret.get();
    }

    @WebListener
    public static final class LoggedUserListener implements HttpSessionAttributeListener {

        @Override
        public void attributeAdded(HttpSessionBindingEvent event) {
            if (event.getName().equals(Authenticate.LOGGED_USER_ATTRIBUTE)) {
                currentSecret.set(computeSecret(event.getSession(), (User) event.getValue()).secret);
            }
        }

        @Override
        public void attributeRemoved(HttpSessionBindingEvent event) {
        }

        @Override
        public void attributeReplaced(HttpSessionBindingEvent event) {

        }

        private SessionSecretWrapper computeSecret(HttpSession session, User user) {
            SessionSecretWrapper secret = new SessionSecretWrapper(user.getUsername() + UUID.randomUUID().toString());
            session.setAttribute(RENDERERS_SESSION_SECRET, secret);
            return secret;
        }

    }

    private static final class SessionSecretWrapper implements Serializable {
        private static final long serialVersionUID = 828957763368790412L;
        final String secret;

        SessionSecretWrapper(String secret) {
            this.secret = secret;
        }
    }

}
