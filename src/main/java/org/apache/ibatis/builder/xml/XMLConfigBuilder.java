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
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XMLConfigBuilder extends BaseBuilder {
    
    private boolean parsed;
    private final XPathParser parser;
    private String environment;
    private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();
    
    public XMLConfigBuilder(Reader reader) {
        this(reader, null, null);
    }
    
    public XMLConfigBuilder(Reader reader, String environment) {
        this(reader, environment, null);
    }
    
    public XMLConfigBuilder(Reader reader, String environment, Properties props) {
        this(Configuration.class, reader, environment, props);
    }
    
    public XMLConfigBuilder(Class<? extends Configuration> configClass, Reader reader, String environment,
                            Properties props) {
        this(configClass, new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
    }
    
    public XMLConfigBuilder(InputStream inputStream) {
        this(inputStream, null, null);
    }
    
    public XMLConfigBuilder(InputStream inputStream, String environment) {
        this(inputStream, environment, null);
    }
    
    public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
        this(Configuration.class, inputStream, environment, props);
    }
    
    public XMLConfigBuilder(Class<? extends Configuration> configClass, InputStream inputStream, String environment,
                            Properties props) {
        this(configClass, new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
    }
    
    private XMLConfigBuilder(Class<? extends Configuration> configClass, XPathParser parser, String environment,
                             Properties props) {
        // 调用父类构造函数, 初始化 configuration 对象
        super(newConfig(configClass));
        ErrorContext.instance().resource("SQL Mapper Configuration");
        
        // 将 Properties 全部设置到 configuration 对象中
        this.configuration.setVariables(props);
        
        // 设置是否解析的标志为 false
        this.parsed = false;
        
        // 初始化 environment
        this.environment = environment;
        
        // 初始化解析器
        this.parser = parser;
    }
    
    /**
     * 将配置内容解析成 Configuration 对象并返回
     * @return Configuration
     */
    public Configuration parse() {
        // 判断是否已经解析过, 若已经解析过了则抛出异常, 根据 parsed 变量的值判断是否已经完成了对 mybatis-confgi.xml 配置文件的解析
        if (parsed) {
            throw new BuilderException("Each XMLConfigBuilder can only be used once.");
        }
        
        // 解析标识设置为已解析
        parsed = true;
        
        // 在 mybatis-config.xml 配置文件中查找根节点 configuration 标签, 并开始解析
        parseConfiguration(parser.evalNode("/configuration"));
        
        // 返回解析
        return configuration;
    }
    
    /**
     * 解析全局配置文件 configuration 根标签下的内容, XMLConfiguration#parseConfiguration() 方法
     * @param root 根节点
     */
    private void parseConfiguration(XNode root) {
        try {
            // issue #117 read properties first
            // 解析 properties 标签元素
            propertiesElement(root.evalNode("properties"));
            // 解析 settings 标签元素
            Properties settings = settingsAsProperties(root.evalNode("settings"));
            // 文件读取
            loadCustomVfsImpl(settings);
            // 设置日志信息
            loadCustomLogImpl(settings);
            // 解析 typeAliases 标签元素
            typeAliasesElement(root.evalNode("typeAliases"));
            // 解析 plugins 标签元素, 插件
            pluginsElement(root.evalNode("plugins"));
            // 解析 objectFactory 标签元素, 对象工厂
            objectFactoryElement(root.evalNode("objectFactory"));
            // 解析 objectWrapperFactory 标签元素, 对象包装工厂
            objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
            // 解析 reflectorFactory 标签元素, 反射工厂
            reflectorFactoryElement(root.evalNode("reflectorFactory"));
            
            // settings 子标签赋值, 若未配置, 使用默认值
            settingsElement(settings);
            // 解析 environments 标签元素, 数据库连接信息创建
            // read it after objectFactory and objectWrapperFactory issue #631
            environmentsElement(root.evalNode("environments"));
            // 解析 databaseIdProvider 标签元素
            databaseIdProviderElement(root.evalNode("databaseIdProvider"));
            // 解析 typeHandlers 标签元素, 类型处理器
            typeHandlersElement(root.evalNode("typeHandlers"));
            // 解析 mappers 标签元素, mapper
            mappersElement(root.evalNode("mappers"));
        } catch (Exception e) {
            // 解析 xml 配置失败, 抛出异常
            throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
        }
    }
    
    /**
     * 解析 settings 标签为 Properties
     * @param context settings 节点文本
     * @return Properties
     */
    private Properties settingsAsProperties(XNode context) {
        // 未配置 settings 标签, 则不处理
        if (context == null) {
            return new Properties();
        }
        // 获取 settings 子节点内容, name 和 value 属性, 并返回 Properties 对象
        Properties props = context.getChildrenAsProperties();
        
        // Check that all settings are known to the configuration class
        // 获取 Configuration 中所有的已知的元数据信息 MetaClass
        MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
        // 检测 Configuration 中是否定义了 key 指定属性的 setter 方法
        for (Object key : props.keySet()) {
            if (!metaConfig.hasSetter(String.valueOf(key))) {
                throw new BuilderException(
                        "The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
            }
        }
        return props;
    }
    
    /**
     * 加载读取本地文件的配置
     * @param props 属性对象
     * @throws ClassNotFoundException 异常
     */
    private void loadCustomVfsImpl(Properties props) throws ClassNotFoundException {
        // 获取 vfsImpl 标签的 value 对象
        String value = props.getProperty("vfsImpl");
        if (value == null) {
            return;
        }
        // 支持多值配置, 使用 , 分割
        String[] clazzes = value.split(",");
        for (String clazz : clazzes) {
            if (!clazz.isEmpty()) {
                // 通过反射实例化 VFS 对象
                @SuppressWarnings("unchecked")
                Class<? extends VFS> vfsImpl = (Class<? extends VFS>) Resources.classForName(clazz);
                
                // 设置到 Configuration 对象的 vfsImpl 中
                configuration.setVfsImpl(vfsImpl);
            }
        }
    }
    
    /**
     * 加载自定义日志实现类
     * @param props 配置属性对象
     */
    private void loadCustomLogImpl(Properties props) {
        // 获取 <settings> 标签下 name 为 logImpl 的 value, 自定义日志加载实现
        Class<? extends Log> logImpl = resolveClass(props.getProperty("logImpl"));
        
        // 设置进 configuration 的 logImpl 属性
        configuration.setLogImpl(logImpl);
    }
    
    /**
     * 解析别名
     * @param context typeAliases 节点
     */
    private void typeAliasesElement(XNode context) {
        // 未配置 typeAliases 标签, 则不处理
        if (context == null) {
            return;
        }
        
        // 遍历 typeAliases 下的子节点
        for (XNode child : context.getChildren()) {
            // 处理 package 节点
            if ("package".equals(child.getName())) {
                // 获取指定的包名
                String typeAliasPackage = child.getStringAttribute("name");
                // 通过 TypeAliasRegistry 扫描指定包中所有的类, 并解析 @Alias 注解, 完成别名注册
                configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
            } else {
                // 处理 typeAlias 节点
                
                // 获取指定的别名
                String alias = child.getStringAttribute("alias");
                // 获取别名对应的类型
                String type = child.getStringAttribute("type");
                try {
                    Class<?> clazz = Resources.classForName(type);
                    // 若 alias 为空, 则使用 class 名字来注册类型别名
                    if (alias == null) {
                        // 扫描 @Alias 注解, 完成注册
                        typeAliasRegistry.registerAlias(clazz);
                    } else {
                        // 注册别名
                        typeAliasRegistry.registerAlias(alias, clazz);
                    }
                } catch (ClassNotFoundException e) {
                    throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
                }
            }
        }
    }
    
    /**
     * 解析插件
     * @param context plugins 节点
     * @throws Exception 异常
     */
    private void pluginsElement(XNode context) throws Exception {
        if (context != null) {
            // 遍历全部子节点
            for (XNode child : context.getChildren()) {
                // 获取 plugins 节点的 interceptor 属性
                String interceptor = child.getStringAttribute("interceptor");
                // 获取 plugins 节点下的 properties 配置信息, 并形成 Properties 对象
                Properties properties = child.getChildrenAsProperties();
                
                // 实例化 interceptor 对象
                Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).getDeclaredConstructor()
                        .newInstance();
                // 设置 interceptor 属性
                interceptorInstance.setProperties(properties);
                // 添加到 configuration 的 interceptor 属性中
                configuration.addInterceptor(interceptorInstance);
            }
        }
    }
    
    /**
     * 解析 objectFactory 节点
     * @param context objectFactory 节点
     * @throws Exception 异常
     */
    private void objectFactoryElement(XNode context) throws Exception {
        if (context != null) {
            // 获取 objectFactory 节点的 type 属性
            String type = context.getStringAttribute("type");
            // 获取 objectFactory 节点下的 properties 配置信息, 并形成 Properties 对象
            Properties properties = context.getChildrenAsProperties();
            
            // 实例化 objectFactory 对象
            ObjectFactory factory = (ObjectFactory) resolveClass(type).getDeclaredConstructor().newInstance();
            factory.setProperties(properties);
            // 设置到 configuration 的 objectFactory 属性中
            configuration.setObjectFactory(factory);
        }
    }
    
    /**
     * 解析 objectWrapperFactory 节点
     * @param context objectWrapperFactory 节点
     * @throws Exception 异常
     */
    private void objectWrapperFactoryElement(XNode context) throws Exception {
        if (context != null) {
            // 获取 objectWrapperFactory 节点的 type 属性
            String type = context.getStringAttribute("type");
            // 通过反射创建 ObjectWrapperFactory 对象
            ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).getDeclaredConstructor().newInstance();
            // 设置到 configuration 的 objectWrapperFactory 属性中
            configuration.setObjectWrapperFactory(factory);
        }
    }
    
    /**
     * 解析 reflectorFactory 节点
     * @param context reflectorFactory 节点
     * @throws Exception 异常
     */
    private void reflectorFactoryElement(XNode context) throws Exception {
        if (context != null) {
            // 获取 reflectorFactory 节点的 type 属性
            String type = context.getStringAttribute("type");
            // 通过反射创建 ReflectorFactory 对象
            ReflectorFactory factory = (ReflectorFactory) resolveClass(type).getDeclaredConstructor().newInstance();
            // 设置到 configuration 的 reflectorFactory 属性中
            configuration.setReflectorFactory(factory);
        }
    }
    
    /**
     * properties 标签解析
     * @param context properties 标签节点上下文
     * @throws Exception 异常
     */
    private void propertiesElement(XNode context) throws Exception {
        // properties 标签元素为空时不做处理
        if (context == null) {
            return;
        }
        
        // 解析 properties 子节点的 name 和 value 属性, 并记录到 Properties 中
        Properties defaults = context.getChildrenAsProperties();
        
        // 解析 properties 的 resource 和 url 属性, 这两个属性用于确定 properties 配置文件的位置
        String resource = context.getStringAttribute("resource");
        String url = context.getStringAttribute("url");
        // 若在 properties 标签中既设置了 url 又设置了 resource, 抛异常
        if (resource != null && url != null) {
            throw new BuilderException(
                    "The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
        }
        // resource 属性不为空
        if (resource != null) {
            // 将 resource 指定的资源文件解析成 Properties 对象, 添加进 properties 标签的容器对象 default 中
            defaults.putAll(Resources.getResourceAsProperties(resource));
            // url 属性不为空
        } else if (url != null) {
            // 将指定路径资源文件解析成 Properties 对象, 添加进 properties 标签的容器对象 default 中
            defaults.putAll(Resources.getUrlAsProperties(url));
        }
        
        // 获取 configuration 的 variables 属性并合并到 defaults 对象中
        Properties vars = configuration.getVariables();
        if (vars != null) {
            defaults.putAll(vars);
        }
        // 填充解析器的 variables 属性
        parser.setVariables(defaults);
        // 更新 XPathParser 和 Configuration 的 variables 字段
        configuration.setVariables(defaults);
    }
    
    /**
     * settings 标签对应的 Properties 对象处理
     * @param props Properties 对象
     */
    private void settingsElement(Properties props) {
        // 如何自动映射列到字段/属性
        configuration
                .setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
        // 自动映射未知的列
        configuration.setAutoMappingUnknownColumnBehavior(
                AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
        // 缓存
        configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
        // 延迟加载的核心技术: 代理模式, CGLIB/JAVASSIST 代理
        configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
        // 延迟加载
        configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
        // 延迟加载时, 每种属性是否还要按需加载
        configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));
        // 允不允许多种结果集从一个单独的语句中返回
        configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
        // 使用列标签代替列名
        configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
        // 允许 JDBC 支持生成的键
        configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
        // 配置默认的执行器
        configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
        // 配置默认的超时时间
        configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
        // 默认获取的结果条数
        configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
        // 默认的 ResultSet 类型
        configuration.setDefaultResultSetType(resolveResultSetType(props.getProperty("defaultResultSetType")));
        // 是否将 DB 字段自动映射到驼峰命名属性上(user_name --> userName)
        configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
        // 嵌套语句上使用 RowRounds
        configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
        // 默认使用 Session 级别的缓存
        configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
        // 为 NULL 值设置 jdbcType
        configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
        // Object 的哪些方法将触发延迟加载
        configuration.setLazyLoadTriggerMethods(
                stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
        // 使用安全的 ResultHandler
        configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
        // 动态 SQL 生成语言所使用的脚本语言
        configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
        // 枚举类型处理器
        configuration.setDefaultEnumTypeHandler(resolveClass(props.getProperty("defaultEnumTypeHandler")));
        // 当结果集中含有 NULL 值时是否执行映射对象的 setter 或者 Map 对象的 put 方法. 此设置对于原始类型的, 如: int、boolean 等无效
        configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
        // 是否使用实际参数名称
        configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));
        // 返回实例是否返回空对象
        configuration.setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));
        // logger 名字的前缀
        configuration.setLogPrefix(props.getProperty("logPrefix"));
        // 配置工厂
        configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
        // 是否在 SQL 中使用缩进
        configuration.setShrinkWhitespacesInSql(booleanValueOf(props.getProperty("shrinkWhitespacesInSql"), false));
        // 启用参数名称自动映射
        configuration.setArgNameBasedConstructorAutoMapping(
                booleanValueOf(props.getProperty("argNameBasedConstructorAutoMapping"), false));
        // 默认的 sql provider 类型
        configuration.setDefaultSqlProviderType(resolveClass(props.getProperty("defaultSqlProviderType")));
        // 默认的 forEach 节点是否允许 null
        configuration.setNullableOnForEach(booleanValueOf(props.getProperty("nullableOnForEach"), false));
    }
    
    /**
     * 解析 environments 节点
     * @param context environments 节点
     * @throws Exception 异常
     */
    private void environmentsElement(XNode context) throws Exception {
        // environments 标签为空时不处理
        if (context == null) {
            return;
        }
        // 判断环境变量是否为空, 若为空则使用 default 属性
        if (environment == null) {
            environment = context.getStringAttribute("default");
        }
        // 遍历子节点
        for (XNode child : context.getChildren()) {
            // 获取 environments 节点的 id 属性值
            String id = child.getStringAttribute("id");
            // 与 XMLConfigBuilder.environment 字段匹配
            if (isSpecifiedEnvironment(id)) {
                // 创建 TransactionFactory 事务工厂
                TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
                // 创建数据源工厂
                DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
                // 创建数据源
                DataSource dataSource = dsFactory.getDataSource();
                // 创建 Builder, 包含事务工厂与数据源
                Environment.Builder environmentBuilder = new Environment.Builder(id).transactionFactory(txFactory)
                        .dataSource(dataSource);
                // 将 Environment 对象添加到 Configuration 中
                configuration.setEnvironment(environmentBuilder.build());
                break;
            }
        }
    }
    
    /**
     * 解析 databaseIdProvider 节点
     * @param context databaseIdProvider 节点
     * @throws Exception 异常
     */
    private void databaseIdProviderElement(XNode context) throws Exception {
        // 全局配置中没有配置 databaseIdProvider 节点时不处理
        if (context == null) {
            return;
        }
        // 获取 databaseIdProvider 节点的 type 属性值
        String type = context.getStringAttribute("type");
        // awful patch to keep backward compatibility
        // 与老版本兼容
        if ("VENDOR".equals(type)) {
            type = "DB_VENDOR";
        }
        // 解析子节点配置信息
        Properties properties = context.getChildrenAsProperties();
        // 创建 DatabaseIdProvider 对象
        DatabaseIdProvider databaseIdProvider = (DatabaseIdProvider) resolveClass(type).getDeclaredConstructor()
                .newInstance();
        // 配置 DatabaseIdProvider, 完成初始化
        databaseIdProvider.setProperties(properties);
        Environment environment = configuration.getEnvironment();
        if (environment != null) {
            // 通过 dataSource 获取 databaseId 并记录到 configuration.databaseId 属性中
            String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
            configuration.setDatabaseId(databaseId);
        }
    }
    
    private TransactionFactory transactionManagerElement(XNode context) throws Exception {
        if (context != null) {
            String type = context.getStringAttribute("type");
            Properties props = context.getChildrenAsProperties();
            TransactionFactory factory = (TransactionFactory) resolveClass(type).getDeclaredConstructor().newInstance();
            factory.setProperties(props);
            return factory;
        }
        throw new BuilderException("Environment declaration requires a TransactionFactory.");
    }
    
    private DataSourceFactory dataSourceElement(XNode context) throws Exception {
        if (context != null) {
            String type = context.getStringAttribute("type");
            Properties props = context.getChildrenAsProperties();
            DataSourceFactory factory = (DataSourceFactory) resolveClass(type).getDeclaredConstructor().newInstance();
            factory.setProperties(props);
            return factory;
        }
        throw new BuilderException("Environment declaration requires a DataSourceFactory.");
    }
    
    /**
     * 解析 typeHandler 节点
     * @param context typeHandler 节点
     */
    private void typeHandlersElement(XNode context) {
        // 全局配置中没有配置 typeHandler 节点时不处理
        if (context == null) {
            return;
        }
        // 遍历子节点
        for (XNode child : context.getChildren()) {
            // 指定 package
            if ("package".equals(child.getName())) {
                String typeHandlerPackage = child.getStringAttribute("name");
                // 调用 TypeHandlerRegistry.register 方法去注册该包下所有类
                typeHandlerRegistry.register(typeHandlerPackage);
            } else {
                // 获取 Java 的数据类型名称
                String javaTypeName = child.getStringAttribute("javaType");
                // 获取 jdbc 的数据类型名称
                String jdbcTypeName = child.getStringAttribute("jdbcType");
                // 获取类型处理类的全限定名
                String handlerTypeName = child.getStringAttribute("handler");
                // 解析 java 数据类型的 Class 对象
                Class<?> javaTypeClass = resolveClass(javaTypeName);
                // 解析获取 JDBC 数据类型
                JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
                // 解析获取类型处理类的 Class 对象
                Class<?> typeHandlerClass = resolveClass(handlerTypeName);
                // 调用 TypeHandlerRegistry.register 的重载方法(以下 3 种方式)
                if (javaTypeClass != null) {
                    if (jdbcType == null) {
                        typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
                    } else {
                        typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
                    }
                } else {
                    typeHandlerRegistry.register(typeHandlerClass);
                }
            }
        }
    }
    
    private void mappersElement(XNode context) throws Exception {
        if (context == null) {
            return;
        }
        for (XNode child : context.getChildren()) {
            if ("package".equals(child.getName())) {
                String mapperPackage = child.getStringAttribute("name");
                configuration.addMappers(mapperPackage);
            } else {
                String resource = child.getStringAttribute("resource");
                String url = child.getStringAttribute("url");
                String mapperClass = child.getStringAttribute("class");
                if (resource != null && url == null && mapperClass == null) {
                    ErrorContext.instance().resource(resource);
                    try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
                        XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource,
                                configuration.getSqlFragments());
                        mapperParser.parse();
                    }
                } else if (resource == null && url != null && mapperClass == null) {
                    ErrorContext.instance().resource(url);
                    try (InputStream inputStream = Resources.getUrlAsStream(url)) {
                        XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url,
                                configuration.getSqlFragments());
                        mapperParser.parse();
                    }
                } else if (resource == null && url == null && mapperClass != null) {
                    Class<?> mapperInterface = Resources.classForName(mapperClass);
                    configuration.addMapper(mapperInterface);
                } else {
                    throw new BuilderException(
                            "A mapper element may only specify a url, resource or class, but not more than one.");
                }
            }
        }
    }
    
    private boolean isSpecifiedEnvironment(String id) {
        if (environment == null) {
            throw new BuilderException("No environment specified.");
        }
        if (id == null) {
            throw new BuilderException("Environment requires an id attribute.");
        }
        return environment.equals(id);
    }
    
    private static Configuration newConfig(Class<? extends Configuration> configClass) {
        try {
            return configClass.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new BuilderException("Failed to create a new Configuration instance.", ex);
        }
    }
    
}
