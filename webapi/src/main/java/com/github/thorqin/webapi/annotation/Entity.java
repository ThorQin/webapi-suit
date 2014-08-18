/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.thorqin.webapi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author nuo.qin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Entity {

	/**
	 *
	 * @author nuo.qin
	 */
	public static enum SourceType {
		QUERY_STRING,
		HTTP_BODY,
		EITHER
	}

	/**
	 *
	 * @author nuo.qin
	 */
	public static enum ParseEncoding {
		JSON,
		HTTP_FORM,
		EITHER
	}

	SourceType source() default SourceType.EITHER;

	ParseEncoding encoding() default ParseEncoding.EITHER;

}
