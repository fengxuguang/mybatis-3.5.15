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
package org.apache.ibatis.submitted.discriminator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Reader;
import java.util.List;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DiscriminatorTest {

	private static SqlSessionFactory sqlSessionFactory;

	@BeforeAll
	static void setUp() throws Exception {
		// create an SqlSessionFactory
		try (
				Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/discriminator/mybatis-config.xml")) {
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
		}

		// populate in-memory database
		BaseDataTest.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
				"org/apache/ibatis/submitted/discriminator/CreateDB.sql");
	}

	@Test
	void shouldSwitchResultType() {
		try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
			Mapper mapper = sqlSession.getMapper(Mapper.class);
			List<Vehicle> vehicles = mapper.selectVehicles();
			assertEquals(Car.class, vehicles.get(0).getClass());
			assertEquals(Integer.valueOf(5), ((Car) vehicles.get(0)).getDoorCount());
			assertEquals(Truck.class, vehicles.get(1).getClass());
			assertEquals(Float.valueOf(1.5f), ((Truck) vehicles.get(1)).getCarryingCapacity());
		}
	}

	@Test
	void shouldInheritResultType() {
		// #486
		try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
			Mapper mapper = sqlSession.getMapper(Mapper.class);
			List<Owner> owners = mapper.selectOwnersWithAVehicle();
			assertEquals(Truck.class, owners.get(0).getVehicle().getClass());
			assertEquals(Car.class, owners.get(1).getVehicle().getClass());
		}
	}

	@Test
	void shouldBeAppliedToResultMapInConstructorArg() {
		try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
			Mapper mapper = sqlSession.getMapper(Mapper.class);
			List<Owner> owners = mapper.selectOwnersWithAVehicleConstructor();
			assertEquals(Truck.class, owners.get(0).getVehicle().getClass());
			assertEquals(Car.class, owners.get(1).getVehicle().getClass());
		}
	}

	@Test
	void shouldBeAppliedToResultMapInConstructorArgNested() {
		try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
			Mapper mapper = sqlSession.getMapper(Mapper.class);
			List<Contract> contracts = mapper.selectContracts();
			assertEquals(2, contracts.size());
			assertEquals(Truck.class, contracts.get(0).getOwner().getVehicle().getClass());
			assertEquals(Car.class, contracts.get(1).getOwner().getVehicle().getClass());
		}
	}

}
