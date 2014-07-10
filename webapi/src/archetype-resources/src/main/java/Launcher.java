#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.thordev.webapi.WebLauncher;
import org.thordev.webapi.annotation.UseDispatcher;
import org.thordev.webapi.annotation.UseSecurity;

@UseDispatcher(
	value="/api/*", 
	monitor="/WEB-INF/classes/ssi", 
	publish="/")
@UseSecurity
public class Launcher implements WebLauncher {
	@Override
	public void onStartup(ServletContext servletContext) 
			throws ServletException {
	}
}
