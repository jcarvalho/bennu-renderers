package pt.ist.fenixWebFramework.servlets.filters.contentRewrite;

import java.io.Serializable;
import java.util.UUID;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;

public class RenderersSessionSecret {

    private static final String RENDERERS_SESSION_SECRET = "RENDERERS_SESSION_SECRET";

    static String computeSecretFromSession(HttpSession session) {
        if (session != null) {
            SessionSecretWrapper secret = (SessionSecretWrapper) session.getAttribute(RENDERERS_SESSION_SECRET);
            if (secret != null) {
                return secret.secret;
            } else {
                return null;
            }
        }
        return null;
    }

    private static final class SessionSecretWrapper implements Serializable {
        private static final long serialVersionUID = 828957763368790412L;
        final String secret;

        SessionSecretWrapper(String secret) {
            this.secret = secret;
        }
    }

    @WebListener
    public static final class LoggedUserListener implements HttpSessionAttributeListener {

        @Override
        public void attributeAdded(HttpSessionBindingEvent event) {
            if (event.getName().equals(Authenticate.LOGGED_USER_ATTRIBUTE)) {
                computeSecret(event.getSession(), (User) event.getValue());
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

}
