package gov.usgs.cida.pubs.springinit;

import gov.usgs.cida.pubs.filter.CORSFilter;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

public class SpringSecurityInitializer extends AbstractSecurityWebApplicationInitializer {

	@Override
	public void beforeSpringSecurityFilterChain(ServletContext servletContext) {		
		FilterRegistration corsFilter = servletContext.addFilter("corsFilter", CORSFilter.class);
		corsFilter.addMappingForUrlPatterns(null, false, "/*");
	}

}
