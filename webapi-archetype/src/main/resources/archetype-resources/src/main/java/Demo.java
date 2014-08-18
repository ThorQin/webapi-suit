#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ${package};


import java.util.Date;
import com.github.thorqin.webapi.annotation.WebEntry;
import static com.github.thorqin.webapi.annotation.WebEntry.HttpMethod.GET;
import com.github.thorqin.webapi.annotation.WebModule;


@WebModule(path="/api")
public class Demo {

	@WebEntry(method=GET)
	String helloWorld() {
		return new Date().toString();
	}

}
