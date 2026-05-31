import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
	/**
	 * 注意
	 * jvm と plugin.spring のバージョンは合わせること
	 */
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
	/**
	 * detekt
	 *
	 * URL
	 * - https://github.com/detekt/detekt
	 * GradlePlugins(plugins.gradle.org)
	 * - https://plugins.gradle.org/plugin/io.gitlab.arturbosch.detekt
	 * Main用途
	 * - Linter/Formatter
	 * Sub用途
	 * - 無し
	 * 概要
	 * KotlinのLinter/Formatter
	 */
	id("io.gitlab.arturbosch.detekt") version "1.23.0"

	/**
	 * openapi.generator
	 *
	 * 公式ページ
	 * - https://openapi-generator.tech/
	 * GradlePlugins(plugins.gradle.org)
	 * - https://plugins.gradle.org/plugin/org.openapi.generator
	 * GitHub
	 * - https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-gradle-plugin
	 * Main用途
	 * - スキーマファイルからコード生成
	 * Sub用途
	 * - スキーマファイルからドキュメント生成
	 * 概要
	 * - スキーマ駆動開発するために使う
	 * - API仕様をスキーマファイル(yaml)に書いて、コード生成し、それを利用するようにする
	 * - 可能な限りプロダクトコードに依存しないようにする(生成したコードにプロダクトコードを依存させる)
	 */
	id("org.openapi.generator") version "7.22.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	/**
	 * Spring Boot Starter Validation
	 *
	 * MavenCentral
	 * - https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-validation
	 * Main 用途
	 * - OpenAPI Generator で作成されるコードで利用
	 * Sub 用途
	 * - 無し
	 * 概要
	 * - OpenAPI Generator で作成されるコードが import している
	 * - javax.validation* を利用するため
	 * [Spring-Boot-2.3ではjavax.validationを依存関係に追加しなければならない](https://qiita.com/tatetsujitomorrow/items/a397c311a95d66e4f955)
	 */
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	/**
	 * Swagger Annotations
	 * Swagger Models
	 * Jakarta Annotations API
	 *
	 * MavenCentral
	 * - https://mvnrepository.com/artifact/io.swagger.core.v3/swagger-annotations
	 * - https://mvnrepository.com/artifact/io.swagger.core.v3/swagger-models
	 * - https://mvnrepository.com/artifact/jakarta.annotation/jakarta.annotation-api
	 * Main 用途
	 * - OpenAPI Generator で作成されるコードで利用
	 * Sub 用途
	 * - なし
	 * 概要
	 * - OpenAPI Generator で作成されるコードが import している
	 * - 基本的にプロダクションコードでは使わない想定
	 */
	compileOnly("io.swagger.core.v3:swagger-annotations:2.2.50")
	compileOnly("io.swagger.core.v3:swagger-models:2.2.50")
	compileOnly("jakarta.annotation:jakarta.annotation-api:3.0.0")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

/**
 * OpenAPI Generatorを使ってAPIドキュメント生成
 * ./gradlew :generateApiDoc
 */
tasks.register<GenerateTask>("generateApiDoc") {
	/**
	 * Generators
	 * https://github.com/OpenAPITools/openapi-generator/blob/master/docs/generators/README.md
	 */
	generatorName.set("html2")
	inputSpec.set("$projectDir/openapi.yaml")
	outputDir.set(layout.buildDirectory.dir("openapi/doc/"))
}

/**
 * OpenAPI Generatorを使ってコード生成
 * ./gradlew :generateApiServer
 */
tasks.register<GenerateTask>("generateApiServer") {
	generatorName.set("kotlin-spring")
	inputSpec.set("$projectDir/openapi.yaml")
	outputDir.set(layout.buildDirectory.dir("openapi/server-code/")) // .gitignoreされているので注意(わざとここにあります)
	apiPackage.set("com.example.realworldkotlinspringbootjdbc.openapi.generated.controller")
	modelPackage.set("com.example.realworldkotlinspringbootjdbc.openapi.generated.model")
	/**
	 * OpenAPI GeneratorがサポートしているConfig
	 * URL
	 * - https://openapi-generator.tech/docs/generators/spring/
	 *
	 * useTags
	 * 概要
	 * - true
	 *   - スキーマファイルはグルーピング用にtagを利用できるが、そのtagを利用してファイル名を決定するようにする
	 *   - スキーマファイルでtagを複数指定すると重複して生成されるので注意(=tagは1つだけにする)
	 *   - 例: tags: ["User and Authentication"] -> UserAndAuthenticationApiの${operationId}メソッド
	 *
	 * - false (default)
	 *   - スキーマファイルで定義したルーティングパスがファイル名に採用される
	 *   - 例: /user/login -> UserApiの${operationId}メソッド or UserApiのloginメソッド
	 *     - operationIdが優先される
	 *
	 * スキーマファイルのtagsについてのドキュメントURL
	 * - https://spec.openapis.org/oas/v3.0.1#operation-object
	 */
	configOptions.set(
		mapOf(
			"interfaceOnly" to "true",
			"useTags" to "true",
			"useSpringBoot4" to "true"
		)
	)
}
/**
 * Kotlinをコンパイルする前に、generateApiServerタスクを実行
 * (必ずスキーマファイルから最新のコードが生成され、もし変更があったら、コンパイル時に失敗して気付ける)
 */
tasks.named("compileKotlin") {
	dependsOn("generateApiServer")
}
/**
 * OpenAPI Generatorによって生成されたコードをimportできるようにする
 */
kotlin {
	sourceSets {
		named("main") {
			kotlin.srcDir(layout.buildDirectory.dir("openapi/server-code/src/main"))
		}
	}
}
/**
 * Detektはコード品質のためのツールだが、今回はコード品質を担保するために利用するわけではないので、detektタスクは無効化する
 * (detektタスクを有効にしていると、生成されたコードもdetektの対象になってしまい、detektのルールに引っかかる可能性があるため)
 */
tasks.withType<io.gitlab.arturbosch.detekt.Detekt> {
    enabled = false
}