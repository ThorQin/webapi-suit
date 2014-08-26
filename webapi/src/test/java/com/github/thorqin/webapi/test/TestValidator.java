/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.thorqin.webapi.test;

import com.github.thorqin.webapi.utility.Serializer;
import com.github.thorqin.webapi.validation.ValidateException;
import com.github.thorqin.webapi.validation.Validator;
import com.github.thorqin.webapi.validation.annotation.ValidateCollection;
import com.github.thorqin.webapi.validation.annotation.ValidateString;
import java.io.IOException;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author nuo.qin
 */
class Item {
	@ValidateString
	public String name;
}

class MyData {
	@ValidateCollection(itemType = Item.class)
	public List<Item> items;
}

public class TestValidator {

	public TestValidator() {
	}
	
	@Test
	public void doTest() throws IOException, ValidateException {
		MyData data = Serializer.fromJson("{items:[{\"name\":\"\"}]}", MyData.class);
		Validator v = new Validator();
		v.validateObject(data, MyData.class, false);
	}
}
