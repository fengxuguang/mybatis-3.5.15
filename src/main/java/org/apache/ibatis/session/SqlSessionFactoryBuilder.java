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
package org.apache.ibatis.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;

/**
 * 整个 Mybatis 入口
 * <p>
 * 提供建造者工厂, 包装 XML 解析处理, 并返回对应 SqlSessionFactory 处理类
 *
 * Builds {@link SqlSession} instances.
 *
 * @author Clinton Begin
 */
public class SqlSessionFactoryBuilder {

	public SqlSessionFactory build(Reader reader) {
		return build(reader, null, null);
	}

	public SqlSessionFactory build(Reader reader, String environment) {
		return build(reader, environment, null);
	}

	public SqlSessionFactory build(Reader reader, Properties properties) {
		return build(reader, null, properties);
	}

	public SqlSessionFactory build(Reader reader, String environment, Properties properties) {
		try {
			XMLConfigBuilder parser = new XMLConfigBuilder(reader, environment, properties);
			return build(parser.parse());
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error building SqlSession.", e);
		} finally {
			ErrorContext.instance().reset();
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				// Intentionally ignore. Prefer previous error.
			}
		}
	}

	public SqlSessionFactory build(InputStream inputStream) {
		return build(inputStream, null, null);
	}

	public SqlSessionFactory build(InputStream inputStream, String environment) {
		return build(inputStream, environment, null);
	}

	public SqlSessionFactory build(InputStream inputStream, Properties properties) {
		return build(inputStream, null, properties);
	}

	public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
		try {
			// 获取 XML 配置解析器 XMLConfigBuilder: 将配置文件加载到内存中并生成一个 document 对象, 同时初始化 Configuration 对象
			XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);

			// 解析配置并创建 SqlSessionFactory 对象
			return build(parser.parse());
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error building SqlSession.", e);
		} finally {
			ErrorContext.instance().reset();
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException e) {
				// Intentionally ignore. Prefer previous error.
			}
		}
	}

    /**
     * 默认返回 DefaultSqlSessionFactory
     */
	public SqlSessionFactory build(Configuration config) {
		return new DefaultSqlSessionFactory(config);
	}

}
