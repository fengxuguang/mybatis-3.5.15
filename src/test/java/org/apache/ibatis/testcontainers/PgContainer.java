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
package org.apache.ibatis.testcontainers;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

public class PgContainer {

	private static final String DB_NAME = "mybatis_test";
	private static final String USERNAME = "u";
	private static final String PASSWORD = "p";
	private static final String DRIVER = "org.postgresql.Driver";

	private static final PostgreSQLContainer<?> INSTANCE = initContainer();

	private static PostgreSQLContainer<?> initContainer() {
		@SuppressWarnings("resource")
		PostgreSQLContainer<?> container = new PostgreSQLContainer<>().withDatabaseName(DB_NAME).withUsername(USERNAME)
				                                   .withPassword(PASSWORD);
		container.start();
		return container;
	}

	public static DataSource getUnpooledDataSource() {
		return new UnpooledDataSource(PgContainer.DRIVER, INSTANCE.getJdbcUrl(), PgContainer.USERNAME,
				PgContainer.PASSWORD);
	}

	private PgContainer() {
	}
}
