#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.github.thorqin.webapi.WebApplication;
import com.github.thorqin.webapi.annotation.UseDispatcher;
import com.github.thorqin.webapi.annotation.UseSecurity;

@UseSecurity
@UseDispatcher(value="/api/*")
public class Application extends WebApplication {
	@Override
	public void onStartup() {
	}

	@Override
	public void onShutdown() {
	}
}

