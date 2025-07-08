package com.luneruniverse.minecraft.nbtdatabase.sqlbuilder;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLUpdateBuilder {
	
	private final String table;
	private final List<String> columns;
	private final List<String> filters;
	private final List<SQLParamSetterWithValue> params;
	
	/**
	 * @param table <code>`table`</code>
	 */
	public SQLUpdateBuilder(String table) {
		this.table = table;
		this.columns = new ArrayList<>();
		this.filters = new ArrayList<>();
		this.params = new ArrayList<>();
	}
	
	/**
	 * @param column <code>`column`=?</code>
	 */
	public void addColumn(String column) {
		columns.add(column);
	}
	
	/**
	 * @param <T> <code>String</code>
	 * @param column <code>`column`=?</code>
	 * @param param <code>Statement::setString</code>
	 * @param value <code>"str"</code>
	 */
	public <T> void addColumn(String column, SQLParamSetter<T> param, T value) {
		addColumn(column);
		addParam(param, value);
	}
	
	/**
	 * @param filter <code>`column`=?</code>
	 */
	public void addFilter(String filter) {
		filters.add(filter);
	}
	
	/**
	 * @param <T> <code>String</code>
	 * @param filter <code>`column`=?</code>
	 * @param param <code>Statement::setString</code>
	 * @param value <code>"str"</code>
	 */
	public <T> void addFilter(String filter, SQLParamSetter<T> param, T value) {
		addFilter(filter);
		addParam(param, value);
	}
	
	/**
	 * @param <T> <code>String</code>
	 * @param param <code>Statement::setString</code>
	 * @param value <code>"str"</code>
	 */
	public <T> void addParam(SQLParamSetter<T> param, T value) {
		params.add(param.bindValue(value));
	}
	
	public boolean isValid() {
		return !columns.isEmpty();
	}
	
	public String toSQL() {
		StringBuilder sql = new StringBuilder("UPDATE ");
		sql.append(table);
		
		for (int i = 0; i < columns.size(); i++) {
			sql.append(i == 0 ? " SET " : ", ");
			sql.append(columns.get(i));
		}
		
		for (int i = 0; i < filters.size(); i++) {
			sql.append(i == 0 ? " WHERE " : " AND ");
			sql.append(filters.get(i));
		}
		
		return sql.toString();
	}
	
	public void setParams(PreparedStatement sql) throws SQLException {
		int paramIndex = 0;
		
		for (SQLParamSetterWithValue param : params)
			param.setParameter(sql, ++paramIndex);
	}
	
}
