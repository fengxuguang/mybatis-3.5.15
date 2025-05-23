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
package org.apache.ibatis.executor;

import static org.apache.ibatis.executor.ExecutionPlaceholder.EXECUTION_PLACEHOLDER;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.statement.StatementUtil;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.logging.jdbc.ConnectionLogger;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 */
public abstract class BaseExecutor implements Executor {

	private static final Log log = LogFactory.getLog(BaseExecutor.class);

	/**
	 * Transaction 对象, 实现事务的提交、回滚和关闭操作
	 */
	protected Transaction transaction;
	protected Executor wrapper;

	protected ConcurrentLinkedQueue<DeferredLoad> deferredLoads;
	protected PerpetualCache localCache;
	protected PerpetualCache localOutputParameterCache;
	protected Configuration configuration;

	protected int queryStack;
	private boolean closed;

	protected BaseExecutor(Configuration configuration, Transaction transaction) {
		this.transaction = transaction;
		this.deferredLoads = new ConcurrentLinkedQueue<>();
		this.localCache = new PerpetualCache("LocalCache");
		this.localOutputParameterCache = new PerpetualCache("LocalOutputParameterCache");
		this.closed = false;
		this.configuration = configuration;
		this.wrapper = this;
	}

	@Override
	public Transaction getTransaction() {
		if (closed) {
			throw new ExecutorException("Executor was closed.");
		}
		return transaction;
	}

	@Override
	public void close(boolean forceRollback) {
		try {
			try {
				rollback(forceRollback);
			} finally {
				if (transaction != null) {
					transaction.close();
				}
			}
		} catch (SQLException e) {
			// Ignore. There's nothing that can be done at this point.
			log.warn("Unexpected exception on closing transaction.  Cause: " + e);
		} finally {
			transaction = null;
			deferredLoads = null;
			localCache = null;
			localOutputParameterCache = null;
			closed = true;
		}
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public int update(MappedStatement ms, Object parameter) throws SQLException {
		ErrorContext.instance().resource(ms.getResource()).activity("executing an update").object(ms.getId());
		// 判断当前 Executor 是否已经关闭
		if (closed) {
			throw new ExecutorException("Executor was closed.");
		}
		// 清除一级缓存
		clearLocalCache();

		// 执行 SQL 语句
		return doUpdate(ms, parameter);
	}

	@Override
	public List<BatchResult> flushStatements() throws SQLException {
		return flushStatements(false);
	}

	public List<BatchResult> flushStatements(boolean isRollBack) throws SQLException {
		if (closed) {
			throw new ExecutorException("Executor was closed.");
		}
		return doFlushStatements(isRollBack);
	}

	@Override
	public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler)
			throws SQLException {
		BoundSql boundSql = ms.getBoundSql(parameter);
		CacheKey key = createCacheKey(ms, parameter, rowBounds, boundSql);
		return query(ms, parameter, rowBounds, resultHandler, key, boundSql);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler,
	                         CacheKey key, BoundSql boundSql) throws SQLException {
		ErrorContext.instance().resource(ms.getResource()).activity("executing a query").object(ms.getId());
		// 检测当前 Executor 是否已经关闭
		if (closed) {
			throw new ExecutorException("Executor was closed.");
		}
		// 非嵌套查询, 并且 select 节点配置的 flushCache 属性为 true 时, 才会清空一级缓存
		if (queryStack == 0 && ms.isFlushCacheRequired()) {
			clearLocalCache();
		}
		List<E> list;
		try {
			// 增加查询层数(处理嵌套查询场景)
			queryStack++;
			// 查询一级缓存
			list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
			// 针对存储过程调用的处理, 在一级缓存命中时, 获取缓存中保存的输出类型参数时, 并设置到用户传入的实参对象中
			if (list != null) {
				handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
			} else {
				// 调用 doQuery 方法完成数据库查询, 并得到映射后的结果对象
				list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
			}
		} finally {
			// 当前查询完成, 查询层数减一
			queryStack--;
		}
		if (queryStack == 0) {
			for (DeferredLoad deferredLoad : deferredLoads) {
				deferredLoad.load();
			}
			// issue #601
			deferredLoads.clear();
			if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
				// issue #482
				clearLocalCache();
			}
		}
		return list;
	}

	@Override
	public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException {
		BoundSql boundSql = ms.getBoundSql(parameter);
		return doQueryCursor(ms, parameter, rowBounds, boundSql);
	}

	@Override
	public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key,
	                      Class<?> targetType) {
		if (closed) {
			throw new ExecutorException("Executor was closed.");
		}
		DeferredLoad deferredLoad = new DeferredLoad(resultObject, property, key, localCache, configuration, targetType);
		if (deferredLoad.canLoad()) {
			deferredLoad.load();
		} else {
			deferredLoads.add(new DeferredLoad(resultObject, property, key, localCache, configuration, targetType));
		}
	}

	@Override
	public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
		// 检测当前 executor 是否已经关闭
		if (closed) {
			throw new ExecutorException("Executor was closed.");
		}
		// 创建 CacheKey 对象
		CacheKey cacheKey = new CacheKey();
		// 查询目标执行方法全路径限定名, 并添加到 cacheKey 对象中
		cacheKey.update(ms.getId());
		// 分页起始位置, 并添加到 cacheKey 对象中
		cacheKey.update(rowBounds.getOffset());
		// 每页记录数, 并添加到 cacheKey 对象摘
		cacheKey.update(rowBounds.getLimit());
		// 目标执行 SQL 添加到 cacheKey 对象中
		cacheKey.update(boundSql.getSql());

		// SQL 中的变量参数映射
		List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
		// 获取类型处理注册器
		TypeHandlerRegistry typeHandlerRegistry = ms.getConfiguration().getTypeHandlerRegistry();

		// mimic DefaultParameterHandler logic
		MetaObject metaObject = null;
		// 获取用户传入的实参, 并添加到 cacheKey 对象中
		for (ParameterMapping parameterMapping : parameterMappings) {
			if (parameterMapping.getMode() != ParameterMode.OUT) {
				Object value;
				// 获取 XML 文件中 SQL 中的变量 #{?}
				String propertyName = parameterMapping.getProperty();
				if (boundSql.hasAdditionalParameter(propertyName)) {
					value = boundSql.getAdditionalParameter(propertyName);
				} else if (parameterObject == null) {
					value = null;
				}
				// 用户传入实参是否有类型处理器
				else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
					value = parameterObject;
				} else {
					if (metaObject == null) {
						metaObject = configuration.newMetaObject(parameterObject);
					}
					value = metaObject.getValue(propertyName);
				}
				// 将实参添加到 cacheKey 对象中
				cacheKey.update(value);
			}
		}
		// 如果 environment 的 id 不为空, 则将其添加到 cacheKey 对象中
		if (configuration.getEnvironment() != null) {
			// issue #176
			cacheKey.update(configuration.getEnvironment().getId());
		}
		return cacheKey;
	}

	@Override
	public boolean isCached(MappedStatement ms, CacheKey key) {
		return localCache.getObject(key) != null;
	}

	@Override
	public void commit(boolean required) throws SQLException {
		if (closed) {
			throw new ExecutorException("Cannot commit, transaction is already closed");
		}
		clearLocalCache();
		flushStatements();
		if (required) {
			transaction.commit();
		}
	}

	@Override
	public void rollback(boolean required) throws SQLException {
		if (!closed) {
			try {
				clearLocalCache();
				flushStatements(true);
			} finally {
				if (required) {
					transaction.rollback();
				}
			}
		}
	}

	@Override
	public void clearLocalCache() {
		if (!closed) {
			localCache.clear();
			localOutputParameterCache.clear();
		}
	}

	protected abstract int doUpdate(MappedStatement ms, Object parameter) throws SQLException;

	protected abstract List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException;

	protected abstract <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds,
	                                       ResultHandler resultHandler, BoundSql boundSql) throws SQLException;

	protected abstract <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds,
	                                               BoundSql boundSql) throws SQLException;

	protected void closeStatement(Statement statement) {
		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				// ignore
			}
		}
	}

	/**
	 * Apply a transaction timeout.
	 *
	 * @param statement a current statement
	 * @throws SQLException if a database access error occurs, this method is called on a closed <code>Statement</code>
	 * @see StatementUtil#applyTransactionTimeout(Statement, Integer, Integer)
	 * @since 3.4.0
	 */
	protected void applyTransactionTimeout(Statement statement) throws SQLException {
		StatementUtil.applyTransactionTimeout(statement, statement.getQueryTimeout(), transaction.getTimeout());
	}

	private void handleLocallyCachedOutputParameters(MappedStatement ms, CacheKey key, Object parameter,
	                                                 BoundSql boundSql) {
		if (ms.getStatementType() == StatementType.CALLABLE) {
			final Object cachedParameter = localOutputParameterCache.getObject(key);
			if (cachedParameter != null && parameter != null) {
				final MetaObject metaCachedParameter = configuration.newMetaObject(cachedParameter);
				final MetaObject metaParameter = configuration.newMetaObject(parameter);
				for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
					if (parameterMapping.getMode() != ParameterMode.IN) {
						final String parameterName = parameterMapping.getProperty();
						final Object cachedValue = metaCachedParameter.getValue(parameterName);
						metaParameter.setValue(parameterName, cachedValue);
					}
				}
			}
		}
	}

    /**
     * 从 数据库 中查询数据
     */
	private <E> List<E> queryFromDatabase(MappedStatement ms, Object parameter, RowBounds rowBounds,
	                                      ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
		List<E> list;
		// 在一级缓存中添加占位符
		localCache.putObject(key, EXECUTION_PLACEHOLDER);
		try {
			// 完成数据库查询操作, 并返回结果对象, 默认 SIMPLE
			list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
		} finally {
			// 删除缓存占位符
			localCache.removeObject(key);
		}

		// 将真正的结果对象添加到一级缓存中
		localCache.putObject(key, list);
		// 存储过程的调用处理
		if (ms.getStatementType() == StatementType.CALLABLE) {
			// 缓存输出类型的参数
			localOutputParameterCache.putObject(key, parameter);
		}
		return list;
	}

	/**
	 * 获取 JDBC 连接
	 */
	protected Connection getConnection(Log statementLog) throws SQLException {
		Connection connection = transaction.getConnection();
		if (statementLog.isDebugEnabled()) {
			// 若需要打印日志, 返回一个 ConnectionLogger(代理模式, AOP)
			return ConnectionLogger.newInstance(connection, statementLog, queryStack);
		}
		// 不需要打印日志, 直接返回连接
		return connection;
	}

	@Override
	public void setExecutorWrapper(Executor wrapper) {
		this.wrapper = wrapper;
	}

	private static class DeferredLoad {

		private final MetaObject resultObject;
		private final String property;
		private final Class<?> targetType;
		private final CacheKey key;
		private final PerpetualCache localCache;
		private final ObjectFactory objectFactory;
		private final ResultExtractor resultExtractor;

		// issue #781
		public DeferredLoad(MetaObject resultObject, String property, CacheKey key, PerpetualCache localCache,
		                    Configuration configuration, Class<?> targetType) {
			this.resultObject = resultObject;
			this.property = property;
			this.key = key;
			this.localCache = localCache;
			this.objectFactory = configuration.getObjectFactory();
			this.resultExtractor = new ResultExtractor(configuration, objectFactory);
			this.targetType = targetType;
		}

		public boolean canLoad() {
			return localCache.getObject(key) != null && localCache.getObject(key) != EXECUTION_PLACEHOLDER;
		}

		public void load() {
			@SuppressWarnings("unchecked")
			// we suppose we get back a List
			List<Object> list = (List<Object>) localCache.getObject(key);
			Object value = resultExtractor.extractObjectFromList(list, targetType);
			resultObject.setValue(property, value);
		}

	}

}
