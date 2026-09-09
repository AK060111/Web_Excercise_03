package vn.iotstar.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class ApplicationLifecycle implements ServletContextListener {
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        JPAConfig.close();
    }
}
