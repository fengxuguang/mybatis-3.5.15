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

import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cache.TransactionalCacheManager;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

/**
 * 二级缓存 执行器
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class CachingExecutor implements Executor {

	/**
	 * 一级缓存 执行器
	 */
	private final Executor delegate;
	/**
	 * 事务缓存管理器
	 */
	private final TransactionalCacheManager tcm = new TransactionalCacheManager();

	public CachingExecutor(Executor delegate) {
		// 设置一级缓存执行器, 如 SimpleExecutor、ReuseExecutor、BatchExecutor 等
		this.delegate = delegate;
		delegate.setExecutorWrapper(this);
	}

	@Override
	public Transaction getTransaction() {
		return delegate.getTransaction();
	}

	@Override
	public void close(boolean forceRollback) {
		try {
			// issues #499, #524 and #573
			if (forceRollback) {
				tcm.rollback();
			} else {
				tcm.commit();
			}
		} finally {
			delegate.close(forceRollback);
		}
	}

	@Override
	public boolean isClosed() {
		return delegate.isClosed();
	}

	@Override
	public int update(MappedStatement ms, Object parameterObject) throws SQLException {
		// 刷新缓存
		flushCacheIfRequired(ms);
		return delegate.update(ms, parameterObject);
	}

	@Override
	public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException {
		flushCacheIfRequired(ms);
		return delegate.queryCursor(ms, parameter, rowBounds);
	}

	@Override
	public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler)
			throws SQLException {
		// 创建 BoundSql 对象, 包含目标执行 SQL、参数信息
		BoundSql boundSql = ms.getBoundSql(parameterObject);
		// 创建 CacheKey 对象, 用来命中缓存
		CacheKey key = createCacheKey(ms, parameterObject, rowBounds, boundSql);

		// 查询
		return query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
	}

	@Override
	public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler,
	                         CacheKey key, BoundSql boundSql) throws SQLException {
		// 获取查询语句所在命名空间对应的的二级缓存
		Cache cache = ms.getCache();

		// 是否开启了二级缓存
		if (cache != null) {
			// 根据 select 节点的配置, 决定是否需要清空二级缓存
			flushCacheIfRequired(ms);
			// 检测 SQL 节点的 useCache 配置以及是否使用了 resultHandler 配置
			if (ms.isUseCache() && resultHandler == null) {
				// 二级缓存不能保存输出类型的参数, 如果查询操作调用了包含输出参数的存储过程, 则报错
				ensureNoOutParams(ms, boundSql);

				// 查询二级缓存
				@SuppressWarnings("unchecked")
				List<E> list = (List<E>) tcm.getObject(cache, key);
				if (list == null) {
					// 二级缓存没有相应的结果时, 调用封装的 Executor 对象的 query 方法(装饰器模式), 此时去一级缓存查询
					list = delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
					// 将查询结果保存到 TransactionalCache.entriesToAddOnCommit 集合中
					tcm.putObject(cache, key, list); // issue #578 and #116
				}
				return list;
			}
		}

		// 没有启动二级缓存, 直接调用底层 Executor 执行数据库查询操作
		return delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
	}

	@Override
	public List<BatchResult> flushStatements() throws SQLException {
		return delegate.flushStatements();
	}

	@Override
	public void commit(boolean required) throws SQLException {
		delegate.commit(required);
		tcm.commit();
	}

	@Override
	public void rollback(boolean required) throws SQLException {
		try {
			delegate.rollback(required);
		} finally {
			if (required) {
				tcm.rollback();
			}
		}
	}

	private void ensureNoOutParams(MappedStatement ms, BoundSql boundSql) {
		if (ms.getStatementType() == StatementType.CALLABLE) {
			for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
				if (parameterMapping.getMode() != ParameterMode.IN) {
					throw new ExecutorException(
							"Caching stored procedures with OUT params is not supported.  Please configure useCache=false in "
									+ ms.getId() + " statement.");
				}
			}
		}
	}

	@Override
	public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
		return delegate.createCacheKey(ms, parameterObject, rowBounds, boundSql);
	}

	@Override
	public boolean isCached(MappedStatement ms, CacheKey key) {
		return delegate.isCached(ms, key);
	}

	@Override
	public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key,
	                      Class<?> targetType) {
		delegate.deferLoad(ms, resultObject, property, key, targetType);
	}

	@Override
	public void clearLocalCache() {
		delegate.clearLocalCache();
	}

	private void flushCacheIfRequired(MappedStatement ms) {
		Cache cache = ms.getCache();
		if (cache != null && ms.isFlushCacheRequired()) {
			tcm.clear(cache);
		}
	}

	@Override
	public void setExecutorWrapper(Executor executor) {
		throw new UnsupportedOperationException("This method should not be called");
	}

}
