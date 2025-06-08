package com.luneruniverse.minecraft.nbtdatabase;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLSelectBuilder {
	
	public static String genParamList(int size) {
		StringBuilder output = new StringBuilder("(");
		if (size > 0)
			output.append('?');
		for (int i = 1; i < size; i++)
			output.append(", ?");
		output.append(')');
		return output.toString();
	}
	
	public interface SQLParamSetter<T> {
		public void setParameter(PreparedStatement sql, int parameterIndex, T x) throws SQLException;
	}
	private interface SQLParamSetterWithValue {
		public void setParameter(PreparedStatement sql, int parameterIndex) throws SQLException;
	}
	
	private final String columnsAndTable;
	private final List<String> joins;
	private final List<String> filters;
	private final List<String> groups;
	private final List<String> groupFilters;
	private final List<SQLParamSetterWithValue> params;
	private Integer limit;
	
	/**
	 * @param columnsAndTable <code>`column1`, `column2` FROM `table`</code>
	 */
	public SQLSelectBuilder(String columnsAndTable) {
		this.columnsAndTable = columnsAndTable;
		this.joins = new ArrayList<>();
		this.filters = new ArrayList<>();
		this.params = new ArrayList<>();
		this.groups = new ArrayList<>();
		this.groupFilters = new ArrayList<>();
	}
	
	/**
	 * @param join <code>JOIN `table` ON `table`.`column`=`table`.`column`</code>
	 */
	public void addJoin(String join) {
		joins.add(join);
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
	 * @param group <code>`column`</code>
	 */
	public void addGroup(String group) {
		groups.add(group);
	}
	
	/**
	 * @param groupFilter <code>COUNT(*)=?</code>
	 */
	public void addGroupFilter(String groupFilter) {
		groupFilters.add(groupFilter);
	}
	
	/**
	 * @param <T> <code>Integer</code>
	 * @param groupFilter <code>COUNT(*)=?</code>
	 * @param param <code>Statement::setInt</code>
	 * @param value <code>2</code>
	 */
	public <T> void addGroupFilter(String groupFilter, SQLParamSetter<T> param, T value) {
		addGroupFilter(groupFilter);
		addParam(param, value);
	}
	
	/**
	 * @param <T> <code>String</code>
	 * @param param <code>Statement::setString</code>
	 * @param value <code>"str"</code>
	 */
	public <T> void addParam(SQLParamSetter<T> param, T value) {
		params.add((sql, paramIndex) -> param.setParameter(sql, paramIndex, value));
	}
	
	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	public String toSQL() {
		StringBuilder sql = new StringBuilder("SELECT ");
		sql.append(columnsAndTable);
		
		for (String join : joins) {
			sql.append(" ");
			sql.append(join);
		}
		
		for (int i = 0; i < filters.size(); i++) {
			sql.append(i == 0 ? " WHERE " : " AND ");
			sql.append(filters.get(i));
		}
		
		for (int i = 0; i < groups.size(); i++) {
			sql.append(i == 0 ? " GROUP BY " : ", ");
			sql.append(groups.get(i));
		}
		
		for (int i = 0; i < groupFilters.size(); i++) {
			sql.append(i == 0 ? " HAVING " : " AND ");
			sql.append(groupFilters.get(i));
		}
		
		if (limit != null)
			sql.append(" LIMIT ?");
		
		return sql.toString();
	}
	
	public void setParams(PreparedStatement sql) throws SQLException {
		int paramIndex = 0;
		
		for (SQLParamSetterWithValue param : params)
			param.setParameter(sql, ++paramIndex);
		
		if (limit != null)
			sql.setInt(++paramIndex, limit);
	}
	
}
