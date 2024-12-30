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
package org.apache.ibatis.debug.mapper;

import java.util.List;

import org.apache.ibatis.debug.entity.User;

/**
 * Created by fengxuguang on 2024/9/9 15:33
 */
public interface UserMapper {

	List<User> selectAll();

	/**
	 * 根据id查询用户信息
	 *
	 * @param id
	 * @return
	 */
	User selectUserById(String id);

	/**
	 * 新增用户
	 *
	 * @param user
	 * @return
	 */
	int insertUserInfo(User user);

	/**
	 * 更新用户
	 *
	 * @param user
	 * @return
	 */
	int updateUserInfo(User user);

	/**
	 * 删除用户
	 *
	 * @param user
	 * @return
	 */
	int deleteUserInfo(User user);

}
