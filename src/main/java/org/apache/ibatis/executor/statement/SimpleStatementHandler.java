/*
 *    Copyright 2009-2024 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * 简单语句处理器
 *
 * @author Clinton Begin
 */
public class SimpleStatementHandler extends BaseStatementHandler {

	public SimpleStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter,
	                              RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
		super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
	}

	@Override
	public int update(Statement statement) throws SQLException {
		// 获取 SQL 语句
		String sql = boundSql.getSql();
		// 获取参数对象
		Object parameterObject = boundSql.getParameterObject();
		KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
		int rows;
		if (keyGenerator instanceof Jdbc3KeyGenerator) {
			// 原生 JDBC 操作
			statement.execute(sql, Statement.RETURN_GENERATED_KEYS);
			// 获取执行结果
			rows = statement.getUpdateCount();
			keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
		} else if (keyGenerator instanceof SelectKeyGenerator) {
			// 原生 JDBC 操作
			statement.execute(sql);
			// 获取执行结果
			rows = statement.getUpdateCount();
			keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
		} else {
			// 原生 JDBC 操作
			statement.execute(sql);
			// 获取执行结果
			rows = statement.getUpdateCount();
		}
		return rows;
	}

	@Override
	public void batch(Statement statement) throws SQLException {
		String sql = boundSql.getSql();
		statement.addBatch(sql);
	}

	@Override
	public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
		String sql = boundSql.getSql();
		statement.execute(sql);
		return resultSetHandler.handleResultSets(statement);
	}

	@Override
	public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
		String sql = boundSql.getSql();
		statement.execute(sql);
		return resultSetHandler.handleCursorResultSets(statement);
	}

	@Override
	protected Statement instantiateStatement(Connection connection) throws SQLException {
		if (mappedStatement.getResultSetType() == ResultSetType.DEFAULT) {
			return connection.createStatement();
		}
		return connection.createStatement(mappedStatement.getResultSetType().getValue(), ResultSet.CONCUR_READ_ONLY);
	}

	@Override
	public void parameterize(Statement statement) {
		// N/A
	}

}
