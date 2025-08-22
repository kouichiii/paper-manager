package com.example.demo.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenAPI bibOpenAPI() {
    return new OpenAPI().info(new Info()
        .title("Paper Manager API")
        .version("v1")
        .description("論文管理のREST API。作成・検索・進捗更新に対応。")
        .license(new License().name("MIT").url("https://example.com")));
  }

  // 必要ならグルーピング（/swagger-ui でタブに分かれる）
  @Bean
  public GroupedOpenApi papersGroup() {
    return GroupedOpenApi.builder()
        .group("papers")
        .packagesToScan("com.example.demo.web")
        .build();
  }
}
