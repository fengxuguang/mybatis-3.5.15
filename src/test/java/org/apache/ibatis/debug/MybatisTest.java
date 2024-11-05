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
package org.apache.ibatis.debug;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.debug.entity.User;
import org.apache.ibatis.debug.mapper.UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by fengxuguang on 2024/9/9 15:39
 */
public class MybatisTest {

    private SqlSession sqlSession;

    private UserMapper userMapper;

    @Before
    public void before() throws Exception {
        // 加载配置文件
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");

        // 获取 SqlSessionFactory
        SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        // 获取 SqlSession
        sqlSession = sessionFactory.openSession();

        userMapper = sqlSession.getMapper(UserMapper.class);
    }

    @After
    public void after() {
        if (sqlSession != null) {
            sqlSession.close();
        }
    }

    @Test
    public void testSelectUserById() throws Exception {
        User user = userMapper.selectUserById("101");
        System.out.println("user = " + user);
    }

    @Test
    public void testInsert() {
        User user = new User();
        user.setId(105);
        user.setName("test0909");
        user.setCreateDate(new Date());
        user.setUpdateDate(new Date());
        int ret = userMapper.insertUserInfo(user);
        System.out.println("ret = " + ret);

        sqlSession.commit();
    }

    @Test
    public void testSelectAll() {
        List<User> users = userMapper.selectAll();
        users.forEach(System.out::println);
    }

}
