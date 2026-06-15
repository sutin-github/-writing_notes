# kotlin spring boot openapi jpa クリーンアーキテクチャ

Kotlin、Spring Boot、OpenAPI、JPAを組み合わせたクリーンアーキテクチャは、依存性逆転の法則を活かし、ビジネスロジック（ドメイン）をフレームワークやデータベースから独立させる強力な構成です。 [1, 2] 
以下に、各技術の責務と具体的なディレクトリ構成のベストプラクティスを整理しました。
## 1. レイヤーごとの責務と役割

| レイヤー [1, 3, 4] | 役割 | 主な使用技術・クラス |
|---|---|---|
| Domain | ビジネスロジックの中核。他層への依存を持たない。 | Kotlin Data Class（Entity），Repositoryインターフェース |
| Use Case | Domain層のビジネスルールを呼び出し、ユースケースを処理する（アプリケーションサービス）。 | ユースケースクラス、入力・出力境界（DTO） |
| Adapter | 入出力を変換（マッピング）する層。OpenAPIのリクエストをUse Caseの入力に変換する。 | Controller（OpenAPI実装）、Presenter、Repository実装 |
| Infrastructure | フレームワーク固有の処理、DB接続、外部API連携。 | Spring Data JPA（Repository）、Entity（JPA用） |

## 2. 推奨されるディレクトリ構成

com.example.myapp
 ├── domain                     # ドメイン層: 純粋なKotlinで記述
 │    ├── model                 # ドメインエンティティ
 │    └── repository            # Repositoryインターフェース（Use Caseが利用）
 │
 ├── usecase                    # ユースケース層: アプリケーションの操作
 │    ├── UsecaseA.kt
 │    └── dto                   # ユースケース間の受け渡しデータ
 │
 ├── adapter                    # アダプター層: フレームワークとの接着
 │    ├── controller            # OpenAPIで自動生成されたAPIの受け口
 │    └── presenter             # 出力データの整形
 │
 └── infrastructure             # インフラ層: データベースやフレームワーク詳細
      ├── database              # Spring Data JPA
      │    ├── JpaUser.kt       # JPA専用エンティティ
      │    └── JpaUserRepository.kt # JpaRepositoryを継承したもの
      └── repository            # Domainのrepositoryを実装したDIクラス

## 3. 各技術の連携のポイント

* OpenAPI と Adapter
openapi-generatorを利用してAPIのインターフェース（Controller）を自動生成します。生成されたコントローラーをAdapterとして利用し、Use Caseを呼び出します。 [5] 
* JPA と Infrastructure
ドメインモデルとJPAのエンティティは必ず分離します。Domain層はSpringの@Entityや@Tableを知りません。Infrastructure層にJPA専用エンティティを定義し、Adapter層またはInfrastructure層のRepository実装でマッピングを行います。 [3] 
* 依存性の逆転 (Dependency Inversion)
Domain層にRepositoryのインターフェースを定義し、Infrastructure層でその実装クラスを作成します。これにより、ドメイン層はDBの変更に影響されなくなります。 [6] 

------------------------------
さらに具体的に設計を進めるにあたり、どのような点について詳しく知りたいですか？

   1. OpenAPI Generatorとルーティングの設定方法
   2. ドメインモデルからJPAエンティティへのマッピングの実装例
   3. Use CaseからRepositoryを呼び出すインターフェース分離の詳細
   4. この構成に基づいたサンプルコード全体の提示 [3, 5] 

進めたい方向性や、現在直面している課題があればぜひお知らせください。最適な設計を具体化するお手伝いをします。

[1] [https://qiita.com](https://qiita.com/nakawodayo/items/c5f8ea9913fbd3d7c1da)
[2] [https://qiita.com](https://qiita.com/nozomi2025/items/ac5b766e46e97e9fb1cc)
[3] [https://www.reddit.com](https://www.reddit.com/r/SpringBoot/comments/1n620bc/clean_arquitecture_with_springboot/?tl=ja)
[4] [https://jp.linkedin.com](https://jp.linkedin.com/pulse/practical-approach-clean-architecture-java-spring-boot-nakamura-na8if?tl=ja)
[5] [https://udomomo.hatenablog.com](https://udomomo.hatenablog.com/entry/2020/02/24/153551)
[6] [https://qiita.com](https://qiita.com/nakawodayo/items/c5f8ea9913fbd3d7c1da)

# 4を詳しく提示して

Kotlin、Spring Boot、OpenAPI、JPAを組み合わせたクリーンアーキテクチャの具体的なサンプルコードを提示します。
今回は「ユーザー(User)の登録機能」を例に、内側（Domain）から外側（Infrastructure）に向かってコードを構成しています。
------------------------------
## 1. Domain 層（ドメイン）
他のどの層にも依存しない、純粋なKotlinのビジネスロジックです。
## ドメインモデル

package com.example.myapp.domain.model
// 1. 純粋なKotlinデータクラス（JPAのアノテーションは一切つけない）data class User(
    val id: Long?,
    val name: String,
    val email: String
) {
    init {
        // ビジネスルール（バリデーション等）をここに閉じ込める
        require(name.isNotBlank()) { "ユーザー名は必須です" }
        require(email.contains("@")) { "不正なメールアドレス形式です" }
    }
}

## リポジトリインターフェース

package com.example.myapp.domain.repository
import com.example.myapp.domain.model.User
// UseCase層が利用する、データ永続化のための抽象的な境界interface UserRepository {
    fun save(user: User): User
    fun findByEmail(email: String): User?
}

------------------------------
## 2. Use Case 層（ユースケース）
ドメインモデルとリポジトリを組み合わせて、アプリケーションの機能を1つのストーリー（ユースケース）として実現します。
## ユースケース実装

package com.example.myapp.usecase
import com.example.myapp.domain.model.Userimport com.example.myapp.domain.repository.UserRepositoryimport org.springframework.stereotype.Service

@Serviceclass CreateUserUseCase(
    // 依存性逆転（DI）により、具体的なDB実装ではなくインターフェースに依存
    private val userRepository: UserRepository
) {
    fun execute(name: String, email: String): User {
        val existingUser = userRepository.findByEmail(email)
        if (existingUser != null) {
            throw IllegalArgumentException("このメールアドレスは既に登録されています")
        }

        val newUser = User(id = null, name = name, email = email)
        return userRepository.save(newUser)
    }
}

------------------------------
## 3. Adapter 層（アダプター）
外部（Web、OpenAPI）と内部（UseCase）のデータ形式を変換する橋渡しをします。
## API コントローラー
※OpenAPI Generatorが生成したインターフェース UsersApi を実装する想定です。

package com.example.myapp.adapter.controller
import com.example.myapp.usecase.CreateUserUseCaseimport org.openapitools.api.UsersApiimport org.openapitools.model.UserRequestimport org.openapitools.model.UserResponseimport org.springframework.http.HttpStatusimport org.springframework.http.ResponseEntityimport org.springframework.web.bind.annotation.RestController

@RestControllerclass UserController(
    private val createUserUseCase: CreateUserUseCase
) : UsersApi { // OpenAPIが自動生成したインターフェース

    override fun createUser(userRequest: UserRequest): ResponseEntity<UserResponse> {
        // 1. OpenAPIのRequestを分解し、UseCaseを呼び出す
        val domainUser = createUserUseCase.execute(
            name = userRequest.name,
            email = userRequest.email
        )

        // 2. ドメインモデルをOpenAPIのResponse（DTO）にマッピングして返す
        val response = UserResponse(
            id = domainUser.id ?: 0L,
            name = domainUser.name,
            email = domainUser.email
        )
        return ResponseEntity(response, HttpStatus.CREATED)
    }
}

------------------------------
## 4. Infrastructure 層（インフラストラクチャ）
データベースやフレームワークの具体的な技術詳細を記述します。
## JPA エンティティ

package com.example.myapp.infrastructure.database
import jakarta.persistence.*
// Spring Data JPA専用のエンティティ（DBのテーブル構造と1対1）
@Entity
@Table(name = "users")class JpaUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val name: String = "",

    @Column(nullable = false, unique = true)
    val email: String = ""
)

## Spring Data JPA リポジトリ

package com.example.myapp.infrastructure.database
import org.springframework.data.jpa.repository.JpaRepositoryimport org.springframework.stereotype.Repository

@Repositoryinterface SpringDataUserRepository : JpaRepository<JpaUser, Long> {
    fun findByEmail(email: String): JpaUser?
}

## リポジトリの実装（データマッパー）
Domain層の UserRepository をここで具体的に実装します。

package com.example.myapp.infrastructure.repository
import com.example.myapp.domain.model.Userimport com.example.myapp.domain.repository.UserRepositoryimport com.example.myapp.infrastructure.database.JpaUserimport com.example.myapp.infrastructure.database.SpringDataUserRepositoryimport org.springframework.stereotype.Component

@Componentclass UserRepositoryImpl(
    private val springDataUserRepository: SpringDataUserRepository
) : UserRepository {

    // Domainモデル ──> JPAエンティティへの変換と保存
    override fun save(user: User): User {
        val jpaUser = JpaUser(id = user.id, name = user.name, email = user.email)
        val savedJpaUser = springDataUserRepository.save(jpaUser)
        return toDomain(savedJpaUser)
    }

    // JPAエンティティ ──> Domainモデルへの変換
    override fun findByEmail(email: String): User? {
        val jpaUser = springDataUserRepository.findByEmail(email) ?: return null
        return toDomain(jpaUser)
    }

    // マッピング処理を共通化
    private fun toDomain(jpaUser: JpaUser): User {
        return User(
            id = jpaUser.id,
            name = jpaUser.name,
            email = jpaUser.email
        )
    }
}

------------------------------
## まとめとポイント

* JPAとの分離: User（ドメイン）と JpaUser（JPA）が完全に分かれているため、将来DBをNoSQLや外部APIに変えたくなっても、DomainやUseCaseには一切影響が出ません。
* ビジネスルールの保護: User データクラスの init ブロックにあるバリデーションは、Web（Controller）を経由しないテストやバッチ処理から呼び出された場合でも、必ず実行され不正なデータを防ぎます。

------------------------------
このサンプルコードをベースに、さらに以下のような応用的な実装について確認したい箇所はありますか？

* トランザクション（@Transactional） をUseCase層とJPA層のどちらに付与すべきか
* カスタム例外ハンドリング（IllegalArgumentException などをOpenAPI準拠のエラーレスポンスに変換する方法）
* ドメインモデルとJPA間のマッピングを自動化する MapStruct などのライブラリ導入方法

# トランザクション カスタム例外ハンドリング mapStruct の順に教えて

クリーンアーキテクチャの文脈におけるトランザクション管理、カスタム例外ハンドリング、そしてMapStructの導入について、順を追って解説します。
------------------------------
## 1. トランザクション管理（@Transactional）
クリーンアーキテクチャでは、トランザクションはUse Case（アプリケーションサービス）層で制御するのがベストプラクティスです。1つのユースケース（ビジネスのまとまり）が「すべて成功する」か「すべて失敗（ロールバック）する」かを保証するためです。
## 実装例（Use Case層）
Springの @Transactional をUse Caseクラスまたはメソッドに付与します。

package com.example.myapp.usecase
import com.example.myapp.domain.repository.UserRepositoryimport org.springframework.stereotype.Serviceimport org.springframework.transaction.annotation.Transactional

@Service// UseCase層に付与することで、一連のDB操作が1つのトランザクションになる
@Transactional(readOnly = false) class CreateUserUseCase(
    private val userRepository: UserRepository
) {
    fun execute(name: String, email: String): User {
        // 1. 重複チェック（読込）
        if (userRepository.findByEmail(email) != null) {
            // ここで投げた例外により、自動的にロールバックされる
            throw DuplicateEmailException("このメールアドレスは既に登録されています")
        }
        // 2. 登録（書込）
        val newUser = User(id = null, name = name, email = email)
        return userRepository.save(newUser)
    }
}


* ポイント: インフラ層（JPA）ではなく、ユースケース単位でコミット・ロールバックの境界を定義します。

------------------------------
## 2. カスタム例外ハンドリング
ビジネスルール違反（ドメイン例外）やユースケースの競合が起きた際、Java/Kotlinの例外をそのまま返すと500エラーや不要なスタックトレースが露出します。
Adapter（Controller）層に RestControllerAdvice を配置し、OpenAPIが定義したエラーレスポンス（DTO）へ変換します。
## ステップ1: ドメイン/ユースケース例外の定義

package com.example.myapp.domain.exception
// ドメインルールやユースケース特有のカスタム例外（KotlinのRuntimeExceptionを継承）class DuplicateEmailException(message: String) : RuntimeException(message)

## ステップ2: 全局例外ハンドラー（ControllerAdvice）の実装
OpenAPI Generatorが生成したエラー用DTO（例: ErrorResponse）にマッピングして返します。

package com.example.myapp.adapter.controller
import com.example.myapp.domain.exception.DuplicateEmailExceptionimport org.openapitools.model.ErrorResponse // OpenAPIが生成したDTOimport org.springframework.http.HttpStatusimport org.springframework.http.ResponseEntityimport org.springframework.web.bind.annotation.ExceptionHandlerimport org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdviceclass GlobalExceptionHandler {

    // 特定のカスタム例外をキャッチ
    @ExceptionHandler(DuplicateEmailException::class)
    fun handleDuplicateEmailException(ex: DuplicateEmailException): ResponseEntity<ErrorResponse> {
        val errorBody = ErrorResponse(
            status = HttpStatus.CONFLICT.value(), // 409 Conflict
            message = ex.message ?: "Conflict"
        )
        return ResponseEntity(errorBody, HttpStatus.CONFLICT)
    }

    // 予期せぬシステムエラーのキャッチ
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        val errorBody = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            message = "予期せぬエラーが発生しました"
        )
        return ResponseEntity(errorBody, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}

------------------------------
## 3. MapStruct の導入
クリーンアーキテクチャでは層ごとにモデル（OpenAPI DTO、Domain、JPA Entity）を分けるため、マッピングコードが大量に発生します。これを自動生成するのが MapStruct です。
Kotlinでは kapt または ksp（推奨）を利用してビルド時に型安全な実装クラスを自動生成します。
## Gradle設定 (build.gradle.kts)

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    // KSP（Kotlin Symbol Processing）プラグインを追加
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" 
}

dependencies {
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    ksp("org.mapstruct:mapstruct-processor:1.5.5.Final")
}

## マッパーインターフェースの定義（Infrastructure層）
JPA Entity と Domain Model の相互変換を行うマッパーを定義します。

package com.example.myapp.infrastructure.repository
import com.example.myapp.domain.model.Userimport com.example.myapp.infrastructure.database.JpaUserimport org.mapstruct.Mapperimport org.mapstruct.Mappingimport org.mapstruct.MappingConstants
// componentModel = "spring" を指定することで、SpringのBean（@Component）として登録される
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)interface UserDataMapper {

    // Domain -> JPA
    fun toJpaEntity(domain: User): JpaUser

    // JPA -> Domain
    fun toDomain(jpa: JpaUser): User
}

## マッパーを利用したRepository実装
手動の入れ替えコードがなくなり、非常にスッキリします。

package com.example.myapp.infrastructure.repository
import com.example.myapp.domain.model.Userimport com.example.myapp.domain.repository.UserRepositoryimport com.example.myapp.infrastructure.database.SpringDataUserRepositoryimport org.springframework.stereotype.Component

@Componentclass UserRepositoryImpl(
    private val springDataUserRepository: SpringDataUserRepository,
    private val userDataMapper: UserDataMapper // MapStructが自動生成したBeanを注入
) : UserRepository {

    override fun save(user: User): User {
        // MapStructで変換
        val jpaUser = userDataMapper.toJpaEntity(user)
        val savedJpaUser = springDataUserRepository.save(jpaUser)
        // MapStructで変換して返す
        return userDataMapper.toDomain(savedJpaUser)
    }

    override fun findByEmail(email: String): User? {
        val jpaUser = springDataUserRepository.findByEmail(email) ?: return null
        return userDataMapper.toDomain(jpaUser)
    }
}

------------------------------
## 次のステップへのご案内
これで「トランザクション」「例外ハンドリング」「マッピング自動化」という、プロダクション品質に必要な土台が揃いました。
次に深掘りしたいテーマや、実装にあたって気になる点はありますか？

* OpenAPIのDTOとDomain間のマッピングにもMapStructを適用する方法
* MapStructでフィールド名が異なる場合や、Kotlinのイミュータブル（val）特有の注意点
* これらの実装が正しく動いているか検証するためのテストコード（JUnit5 / MockK）の書き方

知りたい内容に合わせて具体的なコードを提示しますので、お気軽にお知らせください。

# OpenAPIのDTOとDomain間のマッピングにもMapStructを適用する方法 と MapStructでフィールド名が異なる場合や、Kotlinのイミュータブル（val）特有の注意点

OpenAPIのDTOとDomain間のマッピングにMapStructを適用する方法と、Kotlinのイミュータブル（val）やフィールド名不一致に関する注意点について解説します。
------------------------------
## 1. OpenAPI DTO ── Domain 間の MapStruct 適用
Controllerが属する Adapter層 にマッパーを配置します。
## マッパーインターフェースの定義
OpenAPIが生成した UserRequest/UserResponse と、ドメインモデル User を相互に変換します。

package com.example.myapp.adapter.controller
import com.example.myapp.domain.model.Userimport org.mapstruct.Mapperimport org.mapstruct.Mappingimport org.mapstruct.MappingConstantsimport org.openapitools.model.UserRequestimport org.openapitools.model.UserResponse

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)interface UserApiMapper {

    // Request (DTO) -> Domain
    // ドメイン側で自動採番されるIDは、リクエスト時点では存在しない（null）ため明示的に無視、またはマッピング対象外とする
    @Mapping(target = "id", ignore = true)
    fun toDomain(request: UserRequest): User

    // Domain -> Response (DTO)
    fun toResponse(domain: User): UserResponse
}

## Controllerでの利用例
手動での詰め替えがなくなり、Controllerの責務（ルーティングと変換）が非常にシンプルになります。

@RestControllerclass UserController(
    private val createUserUseCase: CreateUserUseCase,
    private val userApiMapper: UserApiMapper // マッパーをインジェクト
) : UsersApi {

    override fun createUser(userRequest: UserRequest): ResponseEntity<UserResponse> {
        // 1. DTOからDomainへの変換（ビジネスルールのバリデーションがここで走る）
        val userParam = userApiMapper.toDomain(userRequest)
        
        // 2. ユースケース実行（引数をドメインオブジェクトに変更）
        val domainUser = createUserUseCase.execute(userParam)

        // 3. DomainからResponse DTOへの変換
        val response = userApiMapper.toResponse(domainUser)
        return ResponseEntity(response, HttpStatus.CREATED)
    }
}

------------------------------
## 2. フィールド名が異なる場合のマッピング
APIの仕様（キャメルケース/スネークケースの違いなど）や、ドメイン上の表現とDBカラム表現の違いにより、フィールド名が一致しない場合は @Mapping アノテーションで明示的に紐付けます。 [1] 
## 例：フィールド名が異なる場合

* 変換元（DTO）：fullName
* 変換先（Domain）：name

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)interface UserApiMapper {

    // source に変換元の変数名、target に変換先の変数名を指定
    @Mapping(source = "fullName", target = "name")
    @Mapping(target = "id", ignore = true)
    fun toDomain(request: UserRequest): User
}

------------------------------
## 3. Kotlinのイミュータブル（val）特有の注意点と対策
MapStructはもともとJava向けに作られたライブラリであるため、Kotlin特有の「val による不変性（イミュータブル）」や「プライマリコンストラクタ」を扱う際にいくつかの罠があります。 [2] 
## 罠①：デフォルト値付きの val フィールドが無視される
Kotlinのデータクラスで以下のようにデフォルト値を設定している場合、MapStructがコンストラクタではなくJava的な「セッター（または引数なしコンストラクタ）」を探してしまい、ビルドエラーになるか、デフォルト値で上書きされてしまう問題が起きることがあります。

// ターゲットがこのようなデータクラスの場合data class User(
    val id: Long?,
    val name: String,
    val email: String = "default@example.com" // デフォルト値
)

## 対策：最新のMapStruct（1.5以上）とKSPの組み合わせ
MapStruct 1.5.0 以降、Kotlinのプライマリコンストラクタ（引数付きコンストラクタ）を型安全に呼び出す機能が大幅に強化されました。
正しく動作させるためには、以下の設定を必ず満たしているか確認してください。

   1. kapt ではなく ksp を使う（MapStructがKotlinのメタデータを正確に読み込めるため強く推奨）
   2. すべての引数に対応するマッピングが存在すること（存在しない場合は @Mapping(target = "xxx", ignore = true) や expression を明示する）

## 罠②：JPAエンティティと val の相性の悪さ
Domainモデルではなく、Infrastructure層のJPAエンティティ（JpaUser）を MapStruct で扱う場合はさらに注意が必要です。
JPA（Hibernate）はプロキシ生成や遅延読み込みの都合上、「引数なしのコンストラクタ」 と 「ミュータブルなプロパティ（var）」 を基本的には要求します。
そのため、JPAエンティティを MapStruct でDomainモデルに逆変換（JPA ──> Domain）する際は、以下のようにJPAエンティティ側を var と初期値付きで定義しておくのが安全です。

@Entity
@Table(name = "users")class JpaUser(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null, // varにしておく
    var name: String = "",
    var email: String = ""
)

MapStructは JpaUser から User（Domain）へマッピングする際、User が val のデータクラスであっても、コンストラクタの引数名（id, name, email）が JpaUser のプロパティ名と一致していれば、自動的にコンストラクタへ値を注入して生成してくれます。
------------------------------
## 次のステップへのご案内
これで各レイヤー間のデータ移動がMapStructによって高度に自動化されました。
次に進めたいテーマはありますか？

* ここまでの全レイヤーを結合してテストする 「統合テスト（@SpringBootTest）」の書き方
* DomainやUseCaseをモックして高速にテストする 「単体テスト（MockK / JUnit5）」の書き方
* OpenAPIの定義ファイル（yaml）から自動生成されるコードをクリーンアーキテクチャにうまく組み込む Gradle設定のコツ

現在の開発フェーズに合わせて選んで教えてください！

[1] [https://note.com](https://note.com/prog404/n/n9b7c31428204)
[2] [https://note.com](https://note.com/prog404/n/n9b7c31428204)

# OpenAPIの定義ファイル（yaml）から自動生成されるコードをクリーンアーキテクチャにうまく組み込む Gradle設定のコツ

OpenAPI Generator で自動生成されたコードをクリーンアーキテクチャへ綺麗に組み込むには、「生成先ディレクトリの分離」、「ビルドタスクの自動依存（Compile連動）」、そして「余計なコード（モデルやSpring不要コード）の生成抑止」が決定的なコツとなります。
これを実現するための build.gradle.kts の決定版設定と、その解説を提示します。
------------------------------
## 1. build.gradle.kts のベストプラクティス設定
openapi-generator-gradle-plugin（公式プラグイン）と KSP、Kotlin コンパイルタスクを連携させる設定例です。

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    id("org.openapi.generator") version "7.1.0" // OpenAPI Generator公式プラグイン
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}
// 1. 自動生成されたコードの出力先ディレクトリを定義val openapiGeneratedSourcesDir = "$buildDir/generated/openapi"

openApiGenerate {
    generatorName.set("spring") // Spring用のコードを生成
    inputSpec.set("$projectDir/src/main/resources/openapi/spec.yaml") // 定義ファイルのパス
    outputDir.set(openapiGeneratedSourcesDir)
    
    // パッケージ定義（Adapter層が依存しやすい名称にする）
    apiPackage.set("org.openapitools.api")
    modelPackage.set("org.openapitools.model")
    
    configOptions.set(mapOf(
        "interfaceOnly" to "true",         // コツ①: コントローラーの実装クラスを作らず、インターフェースだけ生成
        "useSpringBoot3" to "true",        // Spring Boot 3 (jakarta.*) に対応
        "useJakartaEe" to "true",
        "skipDefaultInterface" to "true",  // コツ②: defaultメソッドの自動生成をスキップ（Kotlinでクリーンに実装するため）
        "openApiNullable" to "false",      // 不要な JsonNullable ラッパーの生成を抑止
        "generatedAnnotation" to "false"   // 不要な @Generated アノテーションの除去
    ))
}
// 2. 自動生成コードをソースセット（SourceSet）に追加し、コンパイル対象にする
sourceSets {
    main {
        java {
            srcDir("$openapiGeneratedSourcesDir/src/main/java")
        }
    }
}

tasks {
    // 3. Kotlinのコンパイルタスクが走る前に、必ずOpenAPIの自動生成を実行させる（重要）
    val openApiGenerateTask = named("openApiGenerate")
    
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        dependsOn(openApiGenerateTask)
    }
    
    // 4. MapStruct（KSP）の解析タスクもOpenAPI生成に依存させる（マッパーが生成DTOを参照するため）
    withType<com.google.devtools.ksp.gradle.KspTaskJvm>().configureEach {
        dependsOn(openApiGenerateTask)
    }
}

------------------------------
## 2. クリーンアーキテクチャに組み込む 3つのコツ## ① interfaceOnly = true と skipDefaultInterface = true の指定
これを指定しない場合、OpenAPI Generator は UserController の具体的な骨組みやデフォルトの応答ロジック（ResponseEntity(HttpStatus.NOT_IMPLEMENTED) など）を自動生成してしまいます。
インターフェース（UsersApi）だけを生成させることで、Adapter層の実装クラス（UserController）が主導権を握り、内側の UseCase をスムーズに呼び出せるようになります。
## ② 生成先を src/main ではなく build/ 配下にする
自動生成されたコードを Git 管理（src/ 内）に含めてしまうと、差分管理が煩雑になり、アーキテクチャが汚れる原因になります。
上記のように $buildDir 配下に出力し、それを Gradle の sourceSets でクラスパスに通すのが王道です。これにより、./gradlew clean を実行した際に自動生成コードも綺麗に消去され、常に最新の yaml と同期されます。
## ③ タスクの依存関係（dependsOn）を明示する
Kotlin のビルドプロセスにおいて、ソースコードの解析・コンパイル順序は非常に繊細です。

   1. openApiGenerate (OpenAPI DTOの生成)
   2. kspKotlin (MapStructが、生成されたDTOを読み込んでマッパー実装を自動生成)
   3. compileKotlin (全体のコンパイル)

この順序を Gradle に明確に教えるため、tasks.withType で明示的に dependsOn(openApiGenerateTask) を記述しています。これを怠ると、「一度ビルドに失敗し、もう一度実行すると通る」という謎の不安定さに悩まされることになります。
------------------------------
## 次のステップへのご案内
これによって、yaml を書き換えるだけで自動的に DTO やインターフェースが更新され、MapStruct と連動する強力な CI/CD の基盤が整いました。
この後は、どのテーマについて詳しく知りたいですか？

* 
* ドメインロジックが意図通りに動いているかを検証する 「ドメイン/ユースケース層の単体テスト（JUnit5 + MockK）」の書き方
* OpenAPI からコントローラー、ユースケース、DBまでを通す 「インフラ/アダプター層も含めた統合テスト（@SpringBootTest）」の書き方
* 

ご希望に合わせて具体的な実装を解説します！

