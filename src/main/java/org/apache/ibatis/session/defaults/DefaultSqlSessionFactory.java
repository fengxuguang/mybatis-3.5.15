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
package org.apache.ibatis.session.defaults;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;

/**
 * @author Clinton Begin
 */
public class DefaultSqlSessionFactory implements SqlSessionFactory {

	private final Configuration configuration;

	public DefaultSqlSessionFactory(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * 获取数据库会话, 创建数据库连接会话对象 SqlSession
	 *
	 * @return SqlSession
	 */
	@Override
	public SqlSession openSession() {
		return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
	}

	@Override
	public SqlSession openSession(boolean autoCommit) {
		return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, autoCommit);
	}

	@Override
	public SqlSession openSession(ExecutorType execType) {
		return openSessionFromDataSource(execType, null, false);
	}

	@Override
	public SqlSession openSession(TransactionIsolationLevel level) {
		return openSessionFromDataSource(configuration.getDefaultExecutorType(), level, false);
	}

	@Override
	public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
		return openSessionFromDataSource(execType, level, false);
	}

	@Override
	public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
		return openSessionFromDataSource(execType, null, autoCommit);
	}

	@Override
	public SqlSession openSession(Connection connection) {
		return openSessionFromConnection(configuration.getDefaultExecutorType(), connection);
	}

	@Override
	public SqlSession openSession(ExecutorType execType, Connection connection) {
		return openSessionFromConnection(execType, connection);
	}

	@Override
	public Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * 从数据源中获取会话对象, 返回 SqlSession
	 *
	 * @return SqlSession
	 */
	private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level,
	                                             boolean autoCommit) {
		Transaction tx = null;
		try {
			// 获取 mybatis-config.xml 配置文件中配置的 environment 对象
			final Environment environment = configuration.getEnvironment();

			// 获取 TransactionFactory 对象
			final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);

			// 创建 Transaction 对象
			tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);

			// 根据配置创建 Executor 对象
			final Executor executor = configuration.newExecutor(tx, execType);

			// 创建 DefaultSqlSession 对象
			return new DefaultSqlSession(configuration, executor, autoCommit);
		} catch (Exception e) {
			// 清空错误上下文
			closeTransaction(tx); // may have fetched a connection so lets call close()
			throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
		} finally {
			ErrorContext.instance().reset();
		}
	}

	private SqlSession openSessionFromConnection(ExecutorType execType, Connection connection) {
		try {
			boolean autoCommit;
			try {
				autoCommit = connection.getAutoCommit();
			} catch (SQLException e) {
				// Failover to true, as most poor drivers
				// or databases won't support transactions
				autoCommit = true;
			}
			final Environment environment = configuration.getEnvironment();
			final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
			final Transaction tx = transactionFactory.newTransaction(connection);
			final Executor executor = configuration.newExecutor(tx, execType);
			return new DefaultSqlSession(configuration, executor, autoCommit);
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
		} finally {
			ErrorContext.instance().reset();
		}
	}

	/**
	 * 从 environment 对象中获取 TransactionFactory 对象
	 *
	 * @param environment environment 对象
	 * @return TransactionFactory 对象
	 */
	private TransactionFactory getTransactionFactoryFromEnvironment(Environment environment) {
		// 如果没有配置事务工厂, 则返回 ManagedTransactionFactory 对象
		if (environment == null || environment.getTransactionFactory() == null) {
			return new ManagedTransactionFactory();
		}

		// 返回配置的事务工厂对象
		return environment.getTransactionFactory();
	}

	private void closeTransaction(Transaction tx) {
		if (tx != null) {
			try {
				tx.close();
			} catch (SQLException ignore) {
				// Intentionally ignore. Prefer previous error.
			}
		}
	}

}
