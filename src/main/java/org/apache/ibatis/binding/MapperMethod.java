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
package org.apache.ibatis.binding;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 * @author Kazuki Shimizu
 */
public class MapperMethod {

	/**
	 * 存储 SQL 语句相关信息, SQL 语句的名称和类型
	 */
	private final SqlCommand command;

	/**
	 * 存储 Mapper 接口中方法相关信息
	 */
	private final MethodSignature method;

    /**
     * 构造函数
     */
	public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
		this.command = new SqlCommand(config, mapperInterface, method);
		this.method = new MethodSignature(config, mapperInterface, method);
	}

	public Object execute(SqlSession sqlSession, Object[] args) {
		Object result;

		// 根据 SQL 语句的类型调用 SqlSession 对应的方法
		switch (command.getType()) {
			// 新增
			case INSERT: {
				// 使用 ParamNameResolver 处理 args 数组, 将用户传入的实参与指定参数名称关联起来
				Object param = method.convertArgsToSqlCommandParam(args);
				// 调用 SqlSession.insert() 方法, rowCountResult 方法会根据 method 字段中记录的方法的返回值类型对结果进行转换
				result = rowCountResult(sqlSession.insert(command.getName(), param));
				break;
			}
			// 更新
			case UPDATE: {
				Object param = method.convertArgsToSqlCommandParam(args);
				result = rowCountResult(sqlSession.update(command.getName(), param));
				break;
			}
			// 删除
			case DELETE: {
				Object param = method.convertArgsToSqlCommandParam(args);
				result = rowCountResult(sqlSession.delete(command.getName(), param));
				break;
			}
			// 查询
			case SELECT:
				// 处理返回值为 void & ResultSet 是通过 ResultHandler 处理的方法
				if (method.returnsVoid() && method.hasResultHandler()) {
					// 如果有结果处理器
					executeWithResultHandler(sqlSession, args);
					result = null;
				} else if (method.returnsMany()) {
					// 如果结果有多条记录
					result = executeForMany(sqlSession, args);
				} else if (method.returnsMap()) {
					// 处理返回值为 Map 的方法
					result = executeForMap(sqlSession, args);
				} else if (method.returnsCursor()) {
					// 处理返回值为 cursor 的方法
					result = executeForCursor(sqlSession, args);
				} else {
					// 处理返回值为单一对象的方法

					// 获取方法参数值
					Object param = method.convertArgsToSqlCommandParam(args);
					// 根据 类全限定名 + 方法名、方法参数, 执行 SQL
					result = sqlSession.selectOne(command.getName(), param);
					if (method.returnsOptional() && (result == null || !method.getReturnType().equals(result.getClass()))) {
						result = Optional.ofNullable(result);
					}
				}
				break;
			case FLUSH:
				result = sqlSession.flushStatements();
				break;
			default:
				throw new BindingException("Unknown execution method for: " + command.getName());
		}
		if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
			throw new BindingException("Mapper method '" + command.getName()
					                           + "' attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
		}
		return result;
	}

	private Object rowCountResult(int rowCount) {
		final Object result;
		if (method.returnsVoid()) {
			result = null;
		} else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
			result = rowCount;
		} else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
			result = (long) rowCount;
		} else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
			result = rowCount > 0;
		} else {
			throw new BindingException(
					"Mapper method '" + command.getName() + "' has an unsupported return type: " + method.getReturnType());
		}
		return result;
	}

	private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
		MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
		if (!StatementType.CALLABLE.equals(ms.getStatementType())
				    && void.class.equals(ms.getResultMaps().get(0).getType())) {
			throw new BindingException(
					"method " + command.getName() + " needs either a @ResultMap annotation, a @ResultType annotation,"
							+ " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
		}
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			RowBounds rowBounds = method.extractRowBounds(args);
			sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
		} else {
			sqlSession.select(command.getName(), param, method.extractResultHandler(args));
		}
	}

	private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
		List<E> result;
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			RowBounds rowBounds = method.extractRowBounds(args);
			result = sqlSession.selectList(command.getName(), param, rowBounds);
		} else {
			result = sqlSession.selectList(command.getName(), param);
		}
		// issue #510 Collections & arrays support
		if (!method.getReturnType().isAssignableFrom(result.getClass())) {
			if (method.getReturnType().isArray()) {
				return convertToArray(result);
			}
			return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
		}
		return result;
	}

	private <T> Cursor<T> executeForCursor(SqlSession sqlSession, Object[] args) {
		Cursor<T> result;
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			RowBounds rowBounds = method.extractRowBounds(args);
			result = sqlSession.selectCursor(command.getName(), param, rowBounds);
		} else {
			result = sqlSession.selectCursor(command.getName(), param);
		}
		return result;
	}

	private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
		Object collection = config.getObjectFactory().create(method.getReturnType());
		MetaObject metaObject = config.newMetaObject(collection);
		metaObject.addAll(list);
		return collection;
	}

	@SuppressWarnings("unchecked")
	private <E> Object convertToArray(List<E> list) {
		Class<?> arrayComponentType = method.getReturnType().getComponentType();
		Object array = Array.newInstance(arrayComponentType, list.size());
		if (!arrayComponentType.isPrimitive()) {
			return list.toArray((E[]) array);
		}
		for (int i = 0; i < list.size(); i++) {
			Array.set(array, i, list.get(i));
		}
		return array;
	}

	private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
		Map<K, V> result;
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			RowBounds rowBounds = method.extractRowBounds(args);
			result = sqlSession.selectMap(command.getName(), param, method.getMapKey(), rowBounds);
		} else {
			result = sqlSession.selectMap(command.getName(), param, method.getMapKey());
		}
		return result;
	}

	public static class ParamMap<V> extends HashMap<String, V> {

		private static final long serialVersionUID = -2212268410512043556L;

		@Override
		public V get(Object key) {
			if (!super.containsKey(key)) {
				throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
			}
			return super.get(key);
		}

	}

	public static class SqlCommand {

		/**
		 * 全限定接口 + 方法名
		 */
		private final String name;
		/**
		 * 当前 SQL 语句类型, 用于辨别增删改查语句
		 */
		private final SqlCommandType type;

		public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
			// 获取执行方法名称
			final String methodName = method.getName();
			// 获取接口的 Class
			final Class<?> declaringClass = method.getDeclaringClass();

			// 获取 MappedStatement 对象, 包含 xml 配置文件、全限定定 + 方法名、返回结果类型、sqlCommand
			MappedStatement ms = resolveMappedStatement(mapperInterface, methodName, declaringClass, configuration);
			if (ms == null) {
				if (method.getAnnotation(Flush.class) == null) {
					throw new BindingException(
							"Invalid bound statement (not found): " + mapperInterface.getName() + "." + methodName);
				}
				name = null;
				type = SqlCommandType.FLUSH;
			} else {
				// 初始化 name 和 type
				name = ms.getId();
				type = ms.getSqlCommandType();
				if (type == SqlCommandType.UNKNOWN) {
					throw new BindingException("Unknown execution method for: " + name);
				}
			}
		}

		public String getName() {
			return name;
		}

		public SqlCommandType getType() {
			return type;
		}

		/**
		 * 解析接口信息获取 MappedStatement 对象
		 */
		private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName, Class<?> declaringClass,
		                                               Configuration configuration) {
			// statementId = 全限定名 + 模板方法名称
			String statementId = mapperInterface.getName() + "." + methodName;
			// 全局配置类是否包含该 SQL 标识 id
			if (configuration.hasStatement(statementId)) {
				// 从 configuration.mappedStatements 集合中查找对应的 MappedStatement 对象,
				// MappedStatement 对象中封装了 SQL 语句相关的信息, 在 mybatis 初始化时创建
				return configuration.getMappedStatement(statementId);
			}
			if (mapperInterface.equals(declaringClass)) {
				return null;
			}
			for (Class<?> superInterface : mapperInterface.getInterfaces()) {
				if (declaringClass.isAssignableFrom(superInterface)) {
					MappedStatement ms = resolveMappedStatement(superInterface, methodName, declaringClass, configuration);
					if (ms != null) {
						return ms;
					}
				}
			}
			return null;
		}
	}

	public static class MethodSignature {

		/**
		 * 返回值类型是否为 Collection 类型或者数组类型
		 */
		private final boolean returnsMany;
		/**
		 * 返回值类型是否为 Map 类型
		 */
		private final boolean returnsMap;
		/**
		 * 返回值类型是否为 void
		 */
		private final boolean returnsVoid;
		/**
		 * 返回值类型是否为 Cursor 类型
		 */
		private final boolean returnsCursor;
		/**
		 * 返回值类型是否为 Optional 类型
		 */
		private final boolean returnsOptional;

		/**
		 * 返回值类型
		 */
		private final Class<?> returnType;
		/**
		 * 如果返回值类型是 Map, 则该字段记录了作为 key 的别名
		 */
		private final String mapKey;
		/**
		 * 用来标记该方法参数列表中 ResultHandler 类型参数的位置
		 */
		private final Integer resultHandlerIndex;
		/**
		 * 用来标记该方法参数列表中 RowBounds 类型参数的位置
		 */
		private final Integer rowBoundsIndex;
		/**
		 * 该方法对应的 ParamNameResolver 对象
		 */
		private final ParamNameResolver paramNameResolver;

		/**
		 * 构造函数
		 */
		public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
			// 解析方法的返回值类型
			Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
			if (resolvedReturnType instanceof Class<?>) {
				this.returnType = (Class<?>) resolvedReturnType;
			} else if (resolvedReturnType instanceof ParameterizedType) {
				this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
			} else {
				this.returnType = method.getReturnType();
			}
			// 初始化 returnsVoid、returnsMany、returnsCursor、returnOptional 参数
			this.returnsVoid = void.class.equals(this.returnType);
			this.returnsMany = configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray();
			this.returnsCursor = Cursor.class.equals(this.returnType);
			this.returnsOptional = Optional.class.equals(this.returnType);

			// 若 MethodSignature 对应方法的返回值是 Map & 限定了 @MapKey 注解, 则使用 getMapKey 方法处理
			this.mapKey = getMapKey(method);
			this.returnsMap = this.mapKey != null;
			// 初始化 rowBoundsIndex、resultHandlerIndex 参数
			this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
			this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);

			// 创建 ParamNameResolver 对象
			this.paramNameResolver = new ParamNameResolver(configuration, method);
		}

		public Object convertArgsToSqlCommandParam(Object[] args) {
			return paramNameResolver.getNamedParams(args);
		}

		public boolean hasRowBounds() {
			return rowBoundsIndex != null;
		}

		public RowBounds extractRowBounds(Object[] args) {
			return hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null;
		}

		public boolean hasResultHandler() {
			return resultHandlerIndex != null;
		}

		public ResultHandler extractResultHandler(Object[] args) {
			return hasResultHandler() ? (ResultHandler) args[resultHandlerIndex] : null;
		}

		public Class<?> getReturnType() {
			return returnType;
		}

		public boolean returnsMany() {
			return returnsMany;
		}

		public boolean returnsMap() {
			return returnsMap;
		}

		public boolean returnsVoid() {
			return returnsVoid;
		}

		public boolean returnsCursor() {
			return returnsCursor;
		}

		/**
		 * return whether return type is {@code java.util.Optional}.
		 *
		 * @return return {@code true}, if return type is {@code java.util.Optional}
		 * @since 3.5.0
		 */
		public boolean returnsOptional() {
			return returnsOptional;
		}

		private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
			Integer index = null;
			final Class<?>[] argTypes = method.getParameterTypes();
			for (int i = 0; i < argTypes.length; i++) {
				if (paramType.isAssignableFrom(argTypes[i])) {
					if (index != null) {
						throw new BindingException(
								method.getName() + " cannot have multiple " + paramType.getSimpleName() + " parameters");
					}
					index = i;
				}
			}
			return index;
		}

		public String getMapKey() {
			return mapKey;
		}

		private String getMapKey(Method method) {
			String mapKey = null;
			if (Map.class.isAssignableFrom(method.getReturnType())) {
				final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
				if (mapKeyAnnotation != null) {
					mapKey = mapKeyAnnotation.value();
				}
			}
			return mapKey;
		}
	}

}
