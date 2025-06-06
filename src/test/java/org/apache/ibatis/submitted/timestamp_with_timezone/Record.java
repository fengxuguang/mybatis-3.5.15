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
package org.apache.ibatis.submitted.timestamp_with_timezone;

import java.time.OffsetDateTime;
import java.time.OffsetTime;

public class Record {

	private Integer id;

	private OffsetDateTime odt;

	private OffsetTime ot;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public OffsetDateTime getOdt() {
		return odt;
	}

	public void setOdt(OffsetDateTime odt) {
		this.odt = odt;
	}

	public OffsetTime getOt() {
		return ot;
	}

	public void setOt(OffsetTime ot) {
		this.ot = ot;
	}

}
