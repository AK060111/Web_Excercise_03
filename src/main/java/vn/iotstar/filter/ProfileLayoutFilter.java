package vn.iotstar.filter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.annotation.WebFilter;

import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.ConfigurableSiteMeshFilter;
import org.sitemesh.webapp.DispatchMode;

@WebFilter(
    filterName = "profileSiteMeshFilter",
    urlPatterns = "/profile",
    dispatcherTypes = DispatcherType.REQUEST
)
public class ProfileLayoutFilter extends ConfigurableSiteMeshFilter {

    @Override
    protected void applyCustomConfiguration(SiteMeshFilterBuilder builder) {

        // Capture only the original request. The controller forwards to the fragment;
        // SiteMesh includes the decorator after capture (safe after Tomcat 11's forward).
        builder
            .setDispatchMode(DispatchMode.INCLUDE)
            .setDecoratorPrefix("")
            .addDecoratorPath(
                "/profile",
                "/WEB-INF/decorators/main.jsp"
            );
    }
}
