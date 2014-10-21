/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.thorqin.webapi.database;

/**
 *
 * @author nuo.qin
 * @param <T>
 */

public interface DBTranscationFactory {
	DBTranscation newTranscation();
}
