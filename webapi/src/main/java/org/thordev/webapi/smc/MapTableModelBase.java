/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thordev.webapi.smc;

import java.util.Map;
import javax.swing.table.AbstractTableModel;

public abstract class MapTableModelBase<T> extends AbstractTableModel {
	private final String[] columns;
	private final Map<String, T> items;
	public MapTableModelBase(Map<String, T> items, String[] columns) {
		this.items = items;
		this.columns = columns;
	}
	
	public Map.Entry<String, T> get(int rowIndex) {
		Object[] array = items.entrySet().toArray();
		Map.Entry<String, T> entry = (Map.Entry<String, T>)array[rowIndex];
		return entry;
	}
	
	public int getKeyIndex(String key) {
		Object[] array = items.entrySet().toArray();
		for (int i = 0; i < array.length; i++) {
			Map.Entry<String, T> entry = (Map.Entry<String, T>)array[i];
			if (entry.getKey().equals(key))
				return i;
		}
		return -1;
	}
	
	@Override
	public int getRowCount() {
		return items.size();
	}

	@Override
	public int getColumnCount() {
		return columns.length;
	}
	
	protected abstract Object getColValue(int columnIndex, Map.Entry<String, T> entry);

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return getColValue(columnIndex, get(rowIndex));
	}
	
	@Override
	public String getColumnName(int col) {
		return columns[col];
    }
}
