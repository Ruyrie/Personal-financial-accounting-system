# 个人理财记账系统环境配置说明

## 需要上传到仓库的内容

这个项目是 Maven + Spring Boot 项目，正常上传时需要保留这些文件：

- `pom.xml`：必须上传。它定义了 Java 版本、Spring Boot 版本、MySQL 驱动、Thymeleaf、Security、Chart.js、Apache POI 等依赖。别人电脑上没有 `pom.xml` 就不能用 Maven 自动下载依赖，也不能正常打包运行。
- `src/main/java/`：后端 Java 源码。
- `src/main/resources/application.yml`：应用配置，支持用环境变量覆盖数据库和端口。
- `src/main/resources/schema.sql`：数据库表结构和演示初始数据。
- `src/main/resources/templates/`：Thymeleaf 页面模板。
- `src/main/resources/static/`：CSS、图片等静态资源。
- `.gitignore`：避免把本地编译产物、IDE 配置、日志、临时文件上传。

不需要上传的内容：

- `target/`：Maven 编译和打包产物，可以重新生成。
- `.idea/`、`.vscode/`、`*.iml`：本机 IDE 配置。
- `.gitnexus/`、`.agents/`、`.claude/`、`AGENTS.md`、`CLAUDE.md`：本地 AI/索引工具配置。
- `.env`、日志文件、临时文件：只属于本机环境。

## 运行环境要求

- JDK 21
- Maven 3.9 或以上
- MySQL 8.0 或以上
- Git

检查版本：

```bash
java -version
mvn -version
mysql --version
git --version
```

## 数据库准备

默认配置会连接本机 MySQL：

```text
数据库地址：jdbc:mysql://localhost:3306/finance_tracker
用户名：root
密码：123456
```

如果你的 MySQL root 密码就是 `123456`，一般不需要手动建库，项目启动时会自动创建 `finance_tracker` 数据库并执行 `schema.sql`。

如果你的 MySQL 密码不是 `123456`，推荐用环境变量覆盖，不要直接改代码。

Windows PowerShell：

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的MySQL密码"
$env:DB_URL="jdbc:mysql://localhost:3306/finance_tracker?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
```

macOS / Linux：

```bash
export DB_USERNAME=root
export DB_PASSWORD=你的MySQL密码
export DB_URL='jdbc:mysql://localhost:3306/finance_tracker?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
```

如果 MySQL 不允许自动创建数据库，可以先手动执行：

```sql
CREATE DATABASE IF NOT EXISTS finance_tracker
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

## 启动项目

在项目根目录执行：

```bash
mvn spring-boot:run
```

默认访问地址：

```text
http://localhost:8080
```

默认登录账号：

```text
用户名：admin
密码：admin123
```

如果 8080 端口被占用，可以临时换端口。

Windows PowerShell：

```powershell
$env:SERVER_PORT="8081"
mvn spring-boot:run
```

macOS / Linux：

```bash
SERVER_PORT=8081 mvn spring-boot:run
```

## 打包运行

生成 jar：

```bash
mvn -DskipTests package
```

运行 jar：

```bash
java -jar target/personal-finance-tracker-1.0.0.jar
```

## 常见问题

### pom.xml 可以不上传吗？

不可以。`pom.xml` 是 Maven 项目的核心配置文件，必须上传。它相当于项目依赖清单和打包说明，少了它别人无法直接构建项目。

### target 目录可以不上传吗？

可以，而且应该不上传。`target/` 是编译后自动生成的目录，别人执行 `mvn package` 或 `mvn spring-boot:run` 会重新生成。

### 数据库连接失败怎么办？

检查三件事：

- MySQL 服务是否已经启动。
- `DB_USERNAME` 和 `DB_PASSWORD` 是否和本机 MySQL 一致。
- MySQL 8 如果出现公钥相关错误，确认连接串里保留了 `allowPublicKeyRetrieval=true`。

### 中文乱码怎么办？

确认 MySQL 数据库字符集使用 `utf8mb4`，并且连接串里保留：

```text
useUnicode=true&characterEncoding=utf8
```

### 不想每次启动都插入演示数据怎么办？

当前配置是：

```yaml
spring:
  sql:
    init:
      mode: always
```

`schema.sql` 里的表和演示数据都使用了 `IF NOT EXISTS` 或重复判断，一般不会重复插入同一批演示数据。如果部署到正式环境，可以用环境变量关闭初始化：

```bash
SQL_INIT_MODE=never
```
