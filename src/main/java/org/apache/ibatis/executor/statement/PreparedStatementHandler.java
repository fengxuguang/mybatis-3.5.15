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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * 预处理 语句处理器
 *
 * @author Clinton Begin
 */
public class PreparedStatementHandler extends BaseStatementHandler {

	public PreparedStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter,
	                                RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
		super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
	}

	@Override
	public int update(Statement statement) throws SQLException {
		PreparedStatement ps = (PreparedStatement) statement;
        // 原生 JDBC 操作
		ps.execute();

        // 获取执行结果
		int rows = ps.getUpdateCount();
		Object parameterObject = boundSql.getParameterObject();
		KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
		keyGenerator.processAfter(executor, mappedStatement, ps, parameterObject);
		return rows;
	}

	@Override
	public void batch(Statement statement) throws SQLException {
		PreparedStatement ps = (PreparedStatement) statement;
		ps.addBatch();
	}

	@Override
	public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
		PreparedStatement ps = (PreparedStatement) statement;
		// JDBC 执行 SQL 查询
		ps.execute();

		// 处理结果集
		return resultSetHandler.handleResultSets(ps);
	}

	@Override
	public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
		PreparedStatement ps = (PreparedStatement) statement;
		ps.execute();
		return resultSetHandler.handleCursorResultSets(ps);
	}

    /**
     * 实例化 Statement
     */
	@Override
	protected Statement instantiateStatement(Connection connection) throws SQLException {
		// 获取待执行的 SQL 语句
		String sql = boundSql.getSql();
		// 根据 keyGenerator 字段的值, 创建 PreparedStatement 对象
		if (mappedStatement.getKeyGenerator() instanceof Jdbc3KeyGenerator) {
			String[] keyColumnNames = mappedStatement.getKeyColumns();
			if (keyColumnNames == null) {
				// 返回数据库生成的主键
				return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			} else {
				// 在 insert 语句执行完成之后, 会将 keyColumnName 指定的列返回
				return connection.prepareStatement(sql, keyColumnNames);
			}
		}

		// 默认创建普通的 PreparedStatement 对象
		if (mappedStatement.getResultSetType() == ResultSetType.DEFAULT) {
			return connection.prepareStatement(sql);
		} else {
			// 设置结果集是否可以滚动以及其游标是否可以上下移动, 设置结果集是否可更新
			return connection.prepareStatement(sql, mappedStatement.getResultSetType().getValue(),
					ResultSet.CONCUR_READ_ONLY);
		}
	}

	@Override
	public void parameterize(Statement statement) throws SQLException {
		parameterHandler.setParameters((PreparedStatement) statement);
	}

}
