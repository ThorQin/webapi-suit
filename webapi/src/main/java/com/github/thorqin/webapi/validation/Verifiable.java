/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.thorqin.webapi.validation;

/**
 *
 * @author nuo.qin
 */
public interface Verifiable {
	/**
	 * If object do not pass the validation then throws ValidateException. 
	 * @throws com.github.thorqin.webapi.validation.ValidateException 
	 */
	void validate() throws ValidateException ;
}
