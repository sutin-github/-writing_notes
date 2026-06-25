## リフレッシュトークンとアクセストークン セッション

「アクセストークン」は API やサーバーへアクセスするための鍵、  
「リフレッシュトークン」はその鍵を再発行するための合鍵です。  
「セッション」はログイン状態を管理する期間や仕組みを指し、
これらトークンを使ってステートレスな状態管理を行います。[1, 2, 3, 4, 5, 6] 
各用語の役割と関係性は以下の通りです：

## 1. アクセストークン（Access Token）

* 役割: API などのリソースにアクセスするための「一時的な通行手形」です。
* 有効期限: 不正アクセス時の被害を抑えるため、数分〜数時間と非常に短く設定されます。
* 特徴: サーバー側は状態を保持せず、トークンの情報（JWTなど）だけで検証を行うため、  高いパフォーマンスと拡張性を持ちます。 [1, 3, 4, 5, 7] 

## 2. リフレッシュトークン（Refresh Token）

* 役割: 有効期限が切れたアクセストークンを新しく作り直す（再発行する）ための「合鍵」です。
* 有効期限: 数日〜数週間と長く設定されます。
* 特徴: アクセストークンとは別の専用ルート（エンドポイント）で使用され、  サーバー側のデータベースなどで管理されます。盗難された場合はサーバー側で無効化（失効）できます。 [1, 2, 4] 

## 3. セッション（Session）

* 役割: ユーザーがログインしてからブラウザを閉じる（または一定時間操作しなくなる）までの「一連の接続状態・期間」です。 [4] 
* 関係性:
* 伝統的なセッション: サーバー側で状態を管理し、Cookie を使ってユーザーを識別します。
   * トークン・セッション: サーバーは状態を持たず、アクセストークンとリフレッシュトークンを使ってクライアント側に状態を管理させる方式（ステートレス）です。 [4, 5] 

## なぜ両方のトークンが必要なのか？
もしアクセストークンしか存在しない場合、セキュリティを高めるためには有効期限を短くする必要があります。  
しかし、短すぎるとユーザーは頻繁にログイン画面に戻って ID とパスワードを再入力しなければならなくなります。 [1, 3, 4]   
そこで、有効期限が短いアクセストークンでセキュリティを担保しつつ、裏側でリフレッシュトークンを使って自動的に新しいアクセストークンを発行することで、  
ユーザーにストレス（再ログイン）を与えずに安全な状態（セッション）を維持しています。 [1, 3, 4] 
具体的なシステム実装（JWTやOAuth2など）や、ログイン維持（スライディングセッション）についてさらに詳しく知りたい場合は、お気軽にお知らせください。 [8] 

[1] [https://www.reddit.com](https://www.reddit.com/r/node/comments/1gjjdnw/why_use_refresh_access_tokens_for_jwt/?tl=ja)  
[2] [https://qiita.com](https://qiita.com/GeekMasahiro/items/4d7c58f1368cfd1dea62)  
[3] [https://www.sailpoint.com](https://www.sailpoint.com/ja/identity-library/access-token)  
[4] [https://zenn.dev](https://zenn.dev/dragon1208/articles/b2afd96c9110c9)  
[5] [https://qiita.com](https://qiita.com/purojyu/items/d054ec912add0d7b7c6c)  
[6] [https://zenn.dev](https://zenn.dev/takaha4k/articles/web-authentication-session-vs-jwt)  
[7] [https://www.fdc-inc.co.jp](https://www.fdc-inc.co.jp/sfsolution/blog/sfa-support/apicertification/)  
[8] [https://auth0.com](https://auth0.com/blog/jp-refresh-tokens-what-are-they-and-when-to-use-them/)  

## スライディングセッション

スライディングセッションとは、ユーザーが操作（リクエスト）を続ける限り、自動的にログインの有効期限が後ろへ延びていく（スライドする）仕組みのことです。
銀行アプリのように「操作を止めると一定時間で自動ログアウト」させつつ、ECサイトのように「使っている間はログイン状態をずっと維持したい」という場合に採用されます。

------------------------------
## スライディングセッションの具体的な仕組み
主に以下の2つのパターンで実装されます。

## パターンA：伝統的なセッション（Cookie）方式

   1. ユーザーが操作するたびに、サーバーがCookieの有効期限を「現在時刻 + 30分」に更新してブラウザに返します。
   2. 30分間全く操作がない場合、Cookieの期限が切れて自動的にログアウト状態になります。 [1] 

## パターンB：トークン方式（アクセストークン ＆ リフレッシュトークン）
トークンを使ってスライディングセッションを実現する場合、「リフレッシュトークン」を使い回すか、新しく発行し直すかで挙動が変わります。

   1. リフレッシュトークン自体の有効期限を延ばす方式（一般的）
   * アクセストークンが切れた際、リフレッシュトークンを使って新しいアクセストークンを要求します。
      * このとき、サーバー側でリフレッシュトークン自体の有効期限も「現在時刻 + 2週間」に延長します。
      * これにより、ユーザーが2週間に1回でもアプリを開いていれば、半永久的にログインが維持されます。 [2] 
   2. リフレッシュトークンをローテーション（再発行）する方式（より安全）
   * 古いリフレッシュトークンを使うと、新しいアクセストークンと一緒に「全く新しいリフレッシュトークン」が1つ発行され、古いものは無効化されます。
      * これにより、万が一トークンが盗まれても、犯人か本人のどちらかが先に使った時点で異常を検知（片方が使えなくなる）できます。 [3, 4, 5] 
   
------------------------------
## メリットとデメリット

* メリット
* 抜群のユーザー体験（UX）: アプリを頻繁に使うユーザーに、何度もID・パスワード入力を求めずに済みます。 [6] 
* デメリット（セキュリティリスク）
* セッションの乗っ取りリスク: デバイスを紛失したり、カフェの公共Wi-Fiなどでトークンを盗まれたりした場合、犯人が操作を続ければログイン状態がずっと維持されてしまいます。

------------------------------
# セキュリティを強化するためのベストプラクティス
スライディングセッションを採用する場合、以下の「安全策」を組み合わせるのが一般的です。

* 最大有効期限（絶対期限）の設定:
「どれだけ操作されていても、最初のログインから最大30日間が経過したら、一度強制ログアウトしてパスワード入力を求める」という制限を設けます。
* 重要な操作での再認証:
マイページでの個人情報変更や、決済画面に進むときだけは、ログイン状態であっても再度パスワードや生体認証（Face IDなど）を要求します。

もし現在構築されている、または検討しているシステムの言語（Node.js, Python, Goなど）やフレームワーク、あるいはバックエンドの構成（Firebase Auth, Auth0など）があれば教えてください。環境に合わせた具体的な実装イメージをご提案できます。

[1] [https://biz.kddi.com](https://biz.kddi.com/service/file-storage/function/)
[2] [https://www.serverion.com](https://www.serverion.com/ja/uncategorized/refresh-token-rotation-best-practices-for-developers/)
[3] [https://www.serverion.com](https://www.serverion.com/ja/uncategorized/refresh-token-rotation-best-practices-for-developers/)
[4] [https://tech-lab.sios.jp](https://tech-lab.sios.jp/archives/49895)
[5] [https://qiita.com](https://qiita.com/kirozero/items/322d1555d52733f294da)
[6] [https://www.ctc-saas.com](https://www.ctc-saas.com/security_column/useful/active_directory)

# アクセストークンやリフレッシュトークンはサーバーで保持する必要があるのか

結論から言うと、「アクセストークン」はサーバーで保持する必要はありません（ステートレス）が、「リフレッシュトークン」はサーバー側（データベースなど）で保持・管理するのが一般的であり安全です。 [1, 2, 3] 

それぞれの理由と管理方法の詳細は以下の通りです。

------------------------------
## 1. アクセストークン：保持する必要はない（ステートレス）
アクセストークン（特にJWT: JSON Web Tokenなど）は、トークン自体の中に「ユーザーID」「有効期限」「権限」などの情報が暗号署名されて含まれています。 [4] 

* 検証方法: サーバーは、トークンが「改ざんされていないか（署名の検証）」と「期限切れでないか」を数学的にチェックするだけで済みます。
* メリット: データベースに問い合わせる必要がないため、サーバーの負荷が非常に低く、高速に処理できます。 [5, 6, 7] 

## 2. リフレッシュトークン：サーバーでの保持が必要（ステートフル）
リフレッシュトークンは、有効期限が長く、新しいアクセストークンを何度も発行できる強力な鍵です。そのため、サーバー側で状態をコントロール（保持・管理）できるようにしておく必要があります。 [8, 9, 10] 

* なぜ保持するのか？（強制ログアウトの実装）
もしリフレッシュトークンをサーバーで保持していない場合、ユーザーが「スマホを紛失した」「パスワードを変更した」「全デバイスからログアウトしたい」と思ったときに、そのトークンを途中で無効化（失効）させることができなくなります。
* 管理方法: データベース（RedisやPostgreSQLなど）に「ユーザーID、トークン文字列（またはハッシュ値）、有効期限、失効フラグ」を保存しておきます。再発行リクエストが来たら、そのトークンが有効（ホワイトリスト内にあるか）を確認します。

------------------------------
## 例外：リフレッシュトークンも保持しない「完全ステートレス」は可能か？
技術的には、リフレッシュトークンもJWTにしてサーバーで保持しない運用は可能です。しかし、その場合は盗まれたトークンをサーバー側から無効化する手段がなくなります。 [11] 
そのため、セキュリティ要件が極めて低いシステムを除き、リフレッシュトークンはサーバー側で管理するのがベストプラクティスとされています。
------------------------------
## トークン管理のまとめ

| トークンの種類 [12, 13, 14, 15, 16] | サーバーでの保持 | 主な保存先（サーバー側） | 役割・理由 |
|---|---|---|---|
| アクセストークン | 不要 | なし（メモリ上での署名検証のみ） | 高速なAPI認可のため |
| リフレッシュトークン | 必要 | Redis, DB（ホワイトリスト形式） | 盗難時の無効化・強制ログアウトのため |

現在開発中、または設計中のシステムにおいて、データベース（RedisやRDBなど）の選定や、具体的なトークンの失効処理（ブラックリスト／ホワイトリスト方式）についてさらに詳しく知りたい部分はありますか？

[1] [https://www.serverion.com](https://www.serverion.com/ja/uncategorized/refresh-token-rotation-best-practices-for-developers/)
[2] [https://zenn.dev](https://zenn.dev/hyoni/articles/3cb1528ae45a8a)
[3] [https://auth0.com](https://auth0.com/docs/ja-jp/secure/security-guidance/data-security/token-storage)
[4] [https://zenn.dev](https://zenn.dev/indigo165e83/articles/8bc193bbffae8c)
[5] [https://www.reddit.com](https://www.reddit.com/r/djangolearning/comments/mscpt1/how_to_determine_the_optimal_access_and_refresh/?tl=ja)
[6] [https://www.reddit.com](https://www.reddit.com/r/reactjs/comments/1etjjy4/how_can_i_properly_use_an_access_token_and_a/?tl=ja)
[7] [https://www.reddit.com](https://www.reddit.com/r/brdev/comments/1ik45oi/duvida_sobre_jwt_token_e_refresh_token/?tl=ja)
[8] [https://www.reddit.com](https://www.reddit.com/r/node/comments/1em1g92/why_an_access_token_and_a_refresh_token/?tl=ja)
[9] [https://e-words.jp](https://e-words.jp/w/%E3%83%AA%E3%83%95%E3%83%AC%E3%83%83%E3%82%B7%E3%83%A5%E3%83%88%E3%83%BC%E3%82%AF%E3%83%B3.html)
[10] [https://blog.logto.io](https://blog.logto.io/ja/understanding-tokens-in-oidc)
[11] [https://zenn.dev](https://zenn.dev/hidechannu/books/oauth21-mcp-guide/viewer/07_refresh)
[12] [https://zenn.dev](https://zenn.dev/tell1124/articles/c914cdd2e30bee)
[13] [https://qiita.com](https://qiita.com/shikataro777/items/b043ae21c7314782e00b)
[14] [https://www.reddit.com](https://www.reddit.com/r/webdev/comments/1k1p0cd/how_do_i_implement_refresh_tokens_in_my_web_app/?tl=ja)
[15] [https://www.reddit.com](https://www.reddit.com/r/brdev/comments/1ik45oi/duvida_sobre_jwt_token_e_refresh_token/?tl=ja)
[16] [https://docs.snowflake.com](https://docs.snowflake.com/ja/user-guide/single-use-refresh-tokens)

## Amazon Cognito

Amazon Cognito（アマゾン コグニート）は、Webアプリやモバイルアプリに「ログイン（認証）」や「会員管理」の機能を簡単に組み込めるAWSのサービスです。 [1, 2, 3] 
先ほどまでお話ししていた「アクセストークン」「リフレッシュトークン」の発行・管理、および「セッション管理」を、開発者が自分で実装することなく、すべて自動で安全に処理してくれるのがこのCognitoです。 [4] 

[Amazon Cognito — Main Features, User Pools and Identity Pools ...](https://medium.com/aws-lambda-serverless-developer-guide-with-hands/amazon-cognito-main-features-user-pools-and-identity-pools-uses-cases-and-how-it-works-20fbe94b1905)
[AWS CLIで動かして学ぶCognito IDプールを利用したAWSの一時 ...](https://dev.classmethod.jp/articles/get-aws-temporary-security-credentials-with-cognito-id-pool-by-aws-cli/)
[Amazon Cognito | AWS Architecture Blog](https://aws.amazon.com/blogs/architecture/category/security-identity-compliance/amazon-cognito/)
[Architecture overview - AWS Documentation](https://docs.aws.amazon.com/solutions/latest/cognito-user-profiles-export-reference-architecture/architecture-overview.html)
[Amazon Cognito | AWS Architecture Blog](https://aws.amazon.com/blogs/architecture/category/security-identity-compliance/amazon-cognito/)
[What is Amazon Cognito? Benefits, Features, and Pricing](https://k21academy.com/aws-cloud/amazon-cognito/)
[AWS Cognito - Features, Architecture and Use Cases by SNDK Corp](https://www.sndkcorp.com/aws-cognito-features-architecture-use-cases)
[Amazon Cognito | AWS Architecture Blog](https://aws.amazon.com/blogs/architecture/category/security-identity-compliance/amazon-cognito/)

------------------------------
## Cognitoの2大機能
Cognitoは大きく分けて2つのコンポーネント（機能）で構成されており、それぞれ役割が異なります。 [5, 6] 
## 1. ユーザープール (User Pools) — 【認証：だれか？】
ユーザーの登録、ログイン、会員情報の管理を行うデータベース兼システムです。 [7, 8, 9] 

* 役割: サインアップ（新規登録）、サインイン（ログイン）、パスワードリセットなどの画面やバックエンド処理を提供します。
* トークンの発行: ログインが成功すると、アクセストークン、リフレッシュトークン、IDトークンの3つをクライアントへ自動発行します。
* 外部連携: Google、Apple、Facebookアカウントでのログイン（ソーシャルログイン）も簡単に設定できます。 [10, 11, 12, 13] 

## 2. IDプール (Identity Pools) — 【認可：何ができるか？】
ログインしたユーザーに対して、AWSのリソース（S3やDynamoDBなど）を直接操作するための「一時的なアクセス権（IAMロール）」を与える仕組みです。 [14, 15, 16] 

* 役割: ユーザープールなどでログインした情報を「AWS専用の鍵」に交換します。
* 例: 「ログインしたユーザーだけに、S3バケット内の自分専用フォルダへ画像をアップロードさせる」といった制御が可能になります。 [17] 

------------------------------
## 先ほどまでの話（トークン・セッション）との関係性
Cognitoを使うと、トークン管理のベストプラクティスが最初から組み込まれた状態になります。

* アクセストークンの検証:
Cognitoが発行するトークンはJWT（Json Web Token）形式です。そのため、自前のAPIサーバー（Node.jsやPythonなど）は、データベースを見に行かなくてもトークンの署名をチェックするだけで「ステートレス」に高速検証できます。
* リフレッシュトークンの管理:
リフレッシュトークンの安全な保存や有効期限の判定、トークンを使ったアクセストークンの自動再発行は、すべてAWS（Cognito）側が裏側で安全に処理してくれます。開発者がRedisなどでホワイトリストを作る必要はありません。
* スライディングセッションの実現:
Cognitoの設定画面（またはコード）で「リフレッシュトークンの有効期限（例: 30日間）」を設定するだけで、ログイン維持の期間を簡単にコントロールできます。 [18, 19] 

------------------------------
## なぜCognitoを使うのか？（メリット）

* 開発期間の短縮: ログイン画面やパスワード暗号化、多要素認証（MFA）を自作すると数週間かかりますが、Cognitoなら数日で導入できます。
* 高いセキュリティ: セキュリティ基準に準拠した強固な認証基盤をAWSが運用してくれるため、個人情報やパスワード漏洩のリスクを大幅に減らせます。
* 従量課金制: 月間アクティブユーザー数（MAU）が 5,000人までは完全無料 で使えます。 [20] 

Cognitoを導入する場合、「Next.jsやReactなどのフロントエンドから直接ログインさせたい」のか、あるいは「自前のAPIサーバー（Go, Pythonなど）と連携させたい」のか、どのようなシステム構成をイメージされていますか？

[1] [https://www.acrovision.jp](https://www.acrovision.jp/service/aws/?p=2114)
[2] [https://www.acrovision.jp](https://www.acrovision.jp/service/aws/?p=1881)
[3] [https://www.isoroot.jp](https://www.isoroot.jp/blog/7422/)
[4] [https://www.acrovision.jp](https://www.acrovision.jp/service/aws/?p=2568)
[5] [https://qiita.com](https://qiita.com/zumax/items/6937b3ecb501b6ca50bb)
[6] [https://dev.classmethod.jp](https://dev.classmethod.jp/articles/re-introduction-2020-amazon-cognito/)
[7] [https://smallit.co.jp](https://smallit.co.jp/blog/a1716/)
[8] [https://zenn.dev](https://zenn.dev/ttaniguchi/articles/cb4e665180fdb7)
[9] [https://jp.linkedin.com](https://jp.linkedin.com/pulse/complete-guide-implement-aws-cognito-using-amplify-nextjs-pettigrew-e1xic?tl=ja)
[10] [https://iret.media](https://iret.media/172774)
[11] [https://techblog.asia-quest.jp](https://techblog.asia-quest.jp/202503/amazon-cognito-authorizer-to-control-access-to-amazon-api-gateway)
[12] [https://iret.media](https://iret.media/172774)
[13] [https://cloudassist.jp](https://cloudassist.jp/knowledge/aws-intro/amazon-cognito/)
[14] [https://www.cloud-for-all.com](https://www.cloud-for-all.com/blog/aws-cognito-beginner-guide)
[15] [https://iret.media](https://iret.media/172774)
[16] [https://iret.media](https://iret.media/186768)
[17] [https://qiita.com](https://qiita.com/shiva_it/items/179c992a7ce068920329)
[18] [https://www.ragate.co.jp](https://www.ragate.co.jp/media/developer_blog/goj60di8h)
[19] [https://qiita.com](https://qiita.com/aiueo3303/items/01b82a16dda9c396bd2a)
[20] [https://dev.classmethod.jp](https://dev.classmethod.jp/articles/update-about-available-amazon-comprehend-in-tokyo-and-seoul-and-mumbai-regions/)

## 自前のAPIサーバー（kotlin ）と連携させたい」

Kotlin の自前 API サーバーと Amazon Cognito を連携させる場合、サーバーがやるべき最も重要な処理は「送られてきたアクセストークン（JWT）の検証」です。 [1, 2] 
自前サーバーは、データベースにユーザーのパスワードやトークンの状態を保存しておく必要はありません。Cognito が発行した署名（JWT）が正しいかどうかを、メモリ上で数学的にチェックするだけで認証が完了します（ステートレス認証）。 [1, 3] 
Kotlin の主要なフレームワークである Spring Boot と Ktor、それぞれの連携イメージと実装のポイントを解説します。
------------------------------
## 全体アーキテクチャ（処理の流れ）

   1. フロントエンド（React, Vue, iOS, Android など）が Cognito にログインリクエストを送る。
   2. Cognito がログインを認証し、フロントエンドに「アクセストークン（JWT）」を返す。
   3. フロントエンドは、Kotlin API サーバーを呼び出す際、リクエストの Authorization ヘッダーに Bearer <アクセストークン> を含めて送信する。
   4. Kotlin API サーバーは、Cognito から公開鍵（JWKS）を自動取得し、トークンが本物（改ざんされていないか・期限切れでないか）を検証して処理を実行する。 [1, 3, 4] 

------------------------------
## フレームワーク別の実装アプローチ## A. Spring Boot (Spring Security) を使う場合
Spring Boot には、Cognito のような OAuth2/OIDC プロバイダーと連携するための強力なエコシステム（spring-boot-starter-oauth2-resource-server）が用意されています。 [5, 6] 
Kotlin のコード自体に複雑な検証ロジックを自作する必要はなく、application.yml に Cognito の情報を数行定義するだけで、フィルターが自動的にトークンを検証してくれます。 [7] 
1. 設定ファイル (application.yml) の記述例

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # CognitoのユーザープールURL (iss)
          issuer-uri: https://cognito-idp.<リージョン>://<ユーザープールID>

※上記を設定するだけで、Spring Security は裏側で自動的に Cognito の公開鍵エンドポイント（/.well-known/jwks.json）にアクセスし、署名の検証を行います。 [7, 8] 
2. セキュリティ設定コードの例

@Configuration
@EnableWebSecurityclass SecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/public/**").permitAll() // 公開API
                    .anyRequest().authenticated()             // それ以外は要認証
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt(Customizer.withDefaults()) // JWT検証の有効化
            }
        return http.build()
    }
}

------------------------------
## B. Ktor を使う場合
軽量な Ktor フレームワークを使う場合は、ktor-server-auth-jwt プラグインを使用します。Ktor の場合も、認証プロバイダーの設定で Cognito の情報を定義するだけです。 [3, 9] 
Ktor での検証コード例

import io.ktor.server.application.*import io.ktor.server.auth.*import io.ktor.server.auth.jwt.*import com.auth0.jwk.JwkProviderBuilderimport java.net.URL
fun Application.configureAuthentication() {
    val issuer = "https://cognito-idp.<リージョン>://<ユーザープールID>"
    
    // Cognitoの公開鍵(JWK)を取得するプロバイダー
    val jwkProvider = JwkProviderBuilder(URL("$issuer/.well-known/jwks.json")).build()

    install(Authentication) {
        jwt("cognito-auth") {
            verifier(jwkProvider, issuer) {
                acceptLeeway(3) // わずかな時間のズレを許容
            }
            validate { credential ->
                // クライアントID(audまたはclient_id)の一致を検証
                if (credential.payload.getClaim("client_id").asString() == "<アプリクライアントID>") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}

------------------------------
## Kotlin サーバー開発における注意点・Tips

* IDトークンとアクセストークンの使い分け:
Cognito はログイン時に IDトークン と アクセストークン の両方を発行します。自前 API サーバーの認可（アクセス制限）に使うのは、必ず アクセストークン（Access Token） を採用してください。ID トークンはユーザーのプロフィール表示用（フロントエンド向け）です。 [2, 10] 
* ユーザー情報の取得方法:
トークンの中にユーザー識別子（sub クレーム）が含まれています。Kotlin サーバー側で「誰のリクエストか」を特定したい場合は、トークンをデコードして sub（UUID 形式の文字列）を取り出し、それをキーにして自前 DB のユーザーデータと紐付けます。 [1, 3] 
* リフレッシュトークンはサーバーを通過しない:
リフレッシュトークンによるアクセストークンの再発行は、通常フロントエンドから Cognito へ直接リクエストされます。そのため、Kotlin の API サーバー側でリフレッシュトークンのロジックを実装したり、セッションを管理したりする必要は一切ありません。 [11] 

現在、Kotlin のフレームワークは Spring Boot と Ktor のどちらを想定されていますか？ また、API サーバー内のデータベース（PostgreSQL や MySQL など）で管理するユーザー情報と Cognito をどう紐付けるかなど、設計についてさらに深掘りしたい点があれば教えてください。

[1] [https://repost.aws](https://repost.aws/knowledge-center/decode-verify-cognito-json-token)
[2] [https://stackoverflow.com](https://stackoverflow.com/questions/40302349/how-to-verify-jwt-from-aws-cognito-in-the-api-backend)
[3] [https://toranoana-lab.hatenablog.com](https://toranoana-lab.hatenablog.com/entry/2021/12/24/150000)
[4] [https://stackoverflow.com](https://stackoverflow.com/questions/49488538/how-do-i-validate-a-jwt-token-that-i-got-from-cognito)
[5] [https://qiita.com](https://qiita.com/k_tsurumaki/items/971a6119177cbd8bbb9f)
[6] [https://qiita.com](https://qiita.com/yosukenaruse/items/ed8e95ea0f1e6d80c59e)
[7] [https://stackoverflow.com](https://stackoverflow.com/questions/49488538/how-do-i-validate-a-jwt-token-that-i-got-from-cognito)
[8] [https://zenn.dev](https://zenn.dev/akring/articles/3a426f431fbe62)
[9] [https://toranoana-lab.hatenablog.com](https://toranoana-lab.hatenablog.com/entry/2021/12/24/150000)
[10] [https://zenn.dev](https://zenn.dev/kurimeg/articles/999a308705ff80)
[11] [https://github.com](https://github.com/aws-amplify/amplify-android/issues/2826)

