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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.ResultHandler;

/**
 * 语句处理器 接口
 *
 * @author Clinton Begin
 */
public interface StatementHandler {

    /**
     * 准备语句
     */
	Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException;

    /**
     * 参数化
     */
	void parameterize(Statement statement) throws SQLException;

    /**
     * 批量查询
     */
	void batch(Statement statement) throws SQLException;

    /**
     * 更新
     */
	int update(Statement statement) throws SQLException;

    /**
     * 执行查询
     */
	<E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException;

    /**
     * 游标查询
     */
	<E> Cursor<E> queryCursor(Statement statement) throws SQLException;

	BoundSql getBoundSql();

	ParameterHandler getParameterHandler();

}
