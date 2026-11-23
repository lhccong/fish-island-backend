# 后端开发约束

后端数据库查询和写入统一使用 MyBatis / MyBatis-Plus Mapper。业务和测试代码禁止直接使用 JDBC，包括 `JdbcTemplate`、`NamedParameterJdbcTemplate` 及 `java.sql` API。

常规 CRUD 复用现有 Mapper，复杂 SQL 放在 `src/main/resources/mapper/` 的 Mapper XML 中，使用 `#{}` 绑定参数。保留 Spring 事务边界，并验证并发锁、版本检查和主键回填等受影响的行为。数据库连接配置、驱动及 Spring 基础设施沿用项目配置。
