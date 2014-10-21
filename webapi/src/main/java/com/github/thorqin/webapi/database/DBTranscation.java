/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.thorqin.webapi.database;

import com.github.thorqin.webapi.database.DBService.DBSession;
import java.sql.SQLException;

/**
 *
 * @author nuo.qin
 */
public interface DBTranscation extends AutoCloseable {
	void commit() throws SQLException ;
	void rollback() throws SQLException ;
	DBSession getSession();
}
