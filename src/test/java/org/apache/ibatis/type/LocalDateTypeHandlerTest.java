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
package org.apache.ibatis.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class LocalDateTypeHandlerTest extends BaseTypeHandlerTest {

	private static final TypeHandler<LocalDate> TYPE_HANDLER = new LocalDateTypeHandler();
	private static final LocalDate LOCAL_DATE = LocalDate.now();

	@Override
	@Test
	public void shouldSetParameter() throws Exception {
		TYPE_HANDLER.setParameter(ps, 1, LOCAL_DATE, null);
		verify(ps).setObject(1, LOCAL_DATE);
	}

	@Override
	@Test
	public void shouldGetResultFromResultSetByName() throws Exception {
		when(rs.getObject("column", LocalDate.class)).thenReturn(LOCAL_DATE);
		assertEquals(LOCAL_DATE, TYPE_HANDLER.getResult(rs, "column"));
		verify(rs, never()).wasNull();
	}

	@Override
	@Test
	public void shouldGetResultNullFromResultSetByName() throws Exception {
		when(rs.getObject("column", LocalDate.class)).thenReturn(null);
		assertNull(TYPE_HANDLER.getResult(rs, "column"));
		verify(rs, never()).wasNull();
	}

	@Override
	@Test
	public void shouldGetResultFromResultSetByPosition() throws Exception {
		when(rs.getObject(1, LocalDate.class)).thenReturn(LOCAL_DATE);
		assertEquals(LOCAL_DATE, TYPE_HANDLER.getResult(rs, 1));
		verify(rs, never()).wasNull();
	}

	@Override
	@Test
	public void shouldGetResultNullFromResultSetByPosition() throws Exception {
		when(rs.getObject(1, LocalDate.class)).thenReturn(null);
		assertNull(TYPE_HANDLER.getResult(rs, 1));
		verify(rs, never()).wasNull();
	}

	@Override
	@Test
	public void shouldGetResultFromCallableStatement() throws Exception {
		when(cs.getObject(1, LocalDate.class)).thenReturn(LOCAL_DATE);
		assertEquals(LOCAL_DATE, TYPE_HANDLER.getResult(cs, 1));
		verify(cs, never()).wasNull();
	}

	@Override
	@Test
	public void shouldGetResultNullFromCallableStatement() throws Exception {
		when(cs.getObject(1, LocalDate.class)).thenReturn(null);
		assertNull(TYPE_HANDLER.getResult(cs, 1));
		verify(cs, never()).wasNull();
	}
}
