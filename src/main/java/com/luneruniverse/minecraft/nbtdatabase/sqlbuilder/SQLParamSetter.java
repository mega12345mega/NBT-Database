package com.luneruniverse.minecraft.nbtdatabase.sqlbuilder;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface SQLParamSetter<T> {
	public void setParameter(PreparedStatement sql, int parameterIndex, T x) throws SQLException;
	public default SQLParamSetterWithValue bindValue(T x) {
		return (sql, parameterIndex) -> setParameter(sql, parameterIndex, x);
	}
}
