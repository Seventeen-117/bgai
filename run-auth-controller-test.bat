@echo off
echo Running AuthControllerTest...

REM 编译测试类
call mvnw clean test-compile -DskipTests

REM 设置系统参数，禁用不必要的Spring组件
set JAVA_OPTS=-Dspring.main.web-application-type=none -Dseata.enabled=false -Dspringdoc.api-docs.enabled=false -Dspringdoc.swagger-ui.enabled=false -Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration,org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration,org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration,org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration

REM 执行指定测试类
call mvnw test -Dtest=AuthControllerTest %JAVA_OPTS%

echo.
if %ERRORLEVEL% EQU 0 (
    echo Test run successful!
) else (
    echo Test run failed!
)

pause 