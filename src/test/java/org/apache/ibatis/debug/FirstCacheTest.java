package org.apache.ibatis.debug;

import org.apache.ibatis.debug.entity.User;
import org.apache.ibatis.debug.mapper.UserMapper;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * 一级缓存总结
 * 1. 与会话相关
 * 2. 参数条件相关
 * 3. 提交、修改都会清空
 *
 * SpringBoot 与 Mybatis 集成
 *
 * Created by fengxuguang on 2024/12/30 14:52
 */
public class FirstCacheTest {

	private SqlSessionFactory factory;
	private SqlSession sqlSession;

	@Before
	public void init() {
		// 获取构造器
		SqlSessionFactoryBuilder factoryBuilder = new SqlSessionFactoryBuilder();

		// 解析 XML 并构造会话工厂
		factory = factoryBuilder.build(FirstCacheTest.class.getResourceAsStream("/mybatis-config.xml"));

		sqlSession = factory.openSession();
	}

	/**
	 * 1. sql 和参数必须相同
	 * 2. 必须是相同的 statementID
	 * 3. sqlSession 必须一样 (会话级缓存)
	 * 4. RowBounds 返回行范围必须相同
	 */
	@Test
	public void test1() {
		UserMapper mapper = sqlSession.getMapper(UserMapper.class);
		User user = mapper.selectUserById("101");
		User user1 = mapper.selectUserById("101");
		RowBounds rowBounds = RowBounds.DEFAULT;
//		Object user2 = sqlSession.selectOne("org.apache.ibatis.debug.mapper.UserMapper.selectUserById", "101");
		List user2 = sqlSession.selectList("org.apache.ibatis.debug.mapper.UserMapper.selectUserById", "101", rowBounds);
		System.out.println(user == user1);
		System.out.println(user == user2.get(0));
	}

	/**
	 * 1. 未手动清空
	 * 2. 未调用 flushCache = true 的查询
	 * 3. 未执行 Update
	 * 4. 缓存作用域不是 STATEMENT
	 */
	@Test
	public void test2() {
		// 会话生命周期是短暂的
		UserMapper mapper = sqlSession.getMapper(UserMapper.class);
		User user = mapper.selectUserById("101");
//		mapper.setName("101", "ronny");
		User user1 = mapper.selectUserById("101");
		System.out.println(user == user1);
	}

	@Test
	public void test3() {
		// 会话生命周期是短暂的
		UserMapper mapper = sqlSession.getMapper(UserMapper.class);
		User user = mapper.selectUserById("101");
		User user1 = mapper.selectUserById("101");
		System.out.println(user == user1);
	}

}
