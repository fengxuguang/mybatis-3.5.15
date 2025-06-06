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
package org.apache.ibatis.scripting.defaults;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeException;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class DefaultParameterHandler implements ParameterHandler {

	private final TypeHandlerRegistry typeHandlerRegistry;

	private final MappedStatement mappedStatement;
    /**
     * 用户传入的实参对象
     */
	private final Object parameterObject;
	private final BoundSql boundSql;
	private final Configuration configuration;

	public DefaultParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
		this.mappedStatement = mappedStatement;
		this.configuration = mappedStatement.getConfiguration();
		this.typeHandlerRegistry = mappedStatement.getConfiguration().getTypeHandlerRegistry();
		this.parameterObject = parameterObject;
		this.boundSql = boundSql;
	}

	@Override
	public Object getParameterObject() {
		return parameterObject;
	}

	@Override
	public void setParameters(PreparedStatement ps) {
		ErrorContext.instance().activity("setting parameters").object(mappedStatement.getParameterMap().getId());
		// 取出 SQL 中的参数映射列表
		List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
		// 检测 parameterMappings 集合是否为空
		if (parameterMappings != null) {
			MetaObject metaObject = null;
			for (int i = 0; i < parameterMappings.size(); i++) {
				ParameterMapping parameterMapping = parameterMappings.get(i);
				// 过滤掉存储过程中的输出参数
				if (parameterMapping.getMode() != ParameterMode.OUT) {
					// 记录绑定的实参
					Object value;
					// 获取参数名称
					String propertyName = parameterMapping.getProperty();
					if (boundSql.hasAdditionalParameter(propertyName)) { // issue #448 ask first for additional params
						value = boundSql.getAdditionalParameter(propertyName);
					} else if (parameterObject == null) {
						value = null;
					} else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
						// 实参可以直接通过 TypeHandler 转换成 JdbcType
						value = parameterObject;
					} else {
						if (metaObject == null) {
							metaObject = configuration.newMetaObject(parameterObject);
						}
						value = metaObject.getValue(propertyName);
					}

					// 获取 parameterMapping 中设置的 TypeHandler 对象
					TypeHandler typeHandler = parameterMapping.getTypeHandler();
					JdbcType jdbcType = parameterMapping.getJdbcType();
					if (value == null && jdbcType == null) {
						// 不同类型的 set 方法不同, 所以委派给子类的 setParameter() 方法
						jdbcType = configuration.getJdbcTypeForNull();
					}
					try {
						// 调用 PreparedStatement.set* 方法为 SQL 语句绑定相应的实参
						typeHandler.setParameter(ps, i + 1, value, jdbcType);
					} catch (TypeException | SQLException e) {
						throw new TypeException("Could not set parameters for mapping: " + parameterMapping + ". Cause: " + e, e);
					}
				}
			}
		}
	}

}
