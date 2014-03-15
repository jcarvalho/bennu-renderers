package pt.ist.fenixWebFramework.servlets;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import pt.ist.fenixWebFramework.renderers.plugin.ConfigurationReader;

@WebListener
public class RenderersInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ConfigurationReader.readAll(sce.getServletContext());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

}
