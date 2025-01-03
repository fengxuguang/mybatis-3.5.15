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

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

/**
 * 执行器
 *
 * @author Clinton Begin
 */
public interface Executor {

	/**
	 * 不需要 ResultHandler
	 */
	ResultHandler NO_RESULT_HANDLER = null;

	/**
	 * 执行 insert、update、delete 三种类型的 SQL 语句
	 */
	int update(MappedStatement ms, Object parameter) throws SQLException;

	/**
	 * 执行 select 类型的 SQL 语句, 返回值分为结果对象列表或游标对象
	 */
	<E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler,
	                  CacheKey cacheKey, BoundSql boundSql) throws SQLException;

	<E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler)
			throws SQLException;

	/**
	 * 通过游标查询
	 */
	<E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException;

	/**
	 * 批量执行 SQL 语句
	 */
	List<BatchResult> flushStatements() throws SQLException;

	/**
	 * 提交事务
	 */
	void commit(boolean required) throws SQLException;

	/**
	 * 回滚事务
	 */
	void rollback(boolean required) throws SQLException;

	/**
	 * 创建缓存中用到的 CacheKey 对象
	 */
	CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql);

	/**
	 * 根据 CacheKey 对象查找缓存
	 */
	boolean isCached(MappedStatement ms, CacheKey key);

	/**
	 * 清空一级缓存
	 */
	void clearLocalCache();

	/**
	 * 延迟加载一级缓存中的数据
	 */
	void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType);

	/**
	 * 获取事务对象
	 */
	Transaction getTransaction();

	/**
	 * 关闭 Executor 对象
	 */
	void close(boolean forceRollback);

	/**
	 * 检测 Executor 是否关闭
	 */
	boolean isClosed();

	/**
	 * 设置 Executor 对象的包装对象
	 */
	void setExecutorWrapper(Executor executor);

}
