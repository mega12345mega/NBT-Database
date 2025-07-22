package com.luneruniverse.minecraft.nbtdatabase.sqlbuilder;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface SQLParamSetterWithValue {
	public void setParameter(PreparedStatement sql, int parameterIndex) throws SQLException;
}
