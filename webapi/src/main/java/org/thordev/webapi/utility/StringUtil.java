/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thordev.webapi.utility;


/**
 *
 * @author nuo.qin
 */
public class StringUtil {
	public static boolean isIntegerOrNull(String text) {
		if (text == null)
			return true;
		else {
			try {
				Integer.parseInt(text.trim());
				return true;
			} catch (Exception ex) {
				return false;
			}
		}
	}
	public static boolean isIntegerOrEmpty(String text) {
		if (text == null || text.trim().isEmpty())
			return true;
		else {
			try {
				Integer.parseInt(text.trim());
				return true;
			} catch (Exception ex) {
				return false;
			}
		}
	}
	public static boolean isInteger(String text) {
		try {
			Integer.parseInt(text.trim());
			return true;
		} catch (Exception ex) {
			return false;
		}
	}
}
