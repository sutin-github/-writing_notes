# Spring Boot Actuator

Spring Boot Actuatorは、Spring Bootアプリケーションの稼働状態の監視や管理を行うための機能（本番対応機能）を提供するモジュールです。 [1, 2, 3] 
これを使用することで、アプリケーションの健全性、メトリクス、環境変数、ログレベルなどをHTTPエンドポイントやJMX（Java Management Extensions）経由で簡単に取得・操作できるようになります。 [1, 4] 
------------------------------
## 主な機能（主要なエンドポイント）
Actuatorを導入すると、/actuator/{エンドポイント名} の形式でさまざまな情報にアクセスできます。 [1, 5] 

* 
* health: アプリケーションの生存状態（UP/DOWN）や、データベース、ディスク容量などの健全性を確認できます。
* metrics: メモリ使用量、CPU使用率、HTTPリクエスト数などの詳細なパフォーマンス指標を収集します。
* env: アプリケーションに読み込まれている環境変数やプロパティの値を表示します。
* loggers: アプリケーションを再起動することなく、リアルタイムにログレベル（DEBUG、INFOなど）を確認・変更できます。
* mappings: コントローラーで定義されているすべてのURLマッピング（@RequestMapping）の一覧を取得できます。
* beans: アプリケーション内に登録されているすべてのSpring Beanとそのスコープを確認できます。 [1, 4, 5, 6, 7, 8] 
* 

------------------------------
## 基本的な使い方## 1. 依存関係の追加 [5] 
build.gradle（Gradleの場合）に以下の依存関係を追加するだけで、機能が有効になります。 [5] 

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}

## 2. エンドポイントの公開設定 [5] 
セキュリティ上の理由から、デフォルトでは health と info 以外のエンドポイントは外部に公開されていません。他のエンドポイントをWeb経由で公開するには、application.yml（または application.properties）で明示的に設定する必要があります。 [1, 5] 

# すべてのエンドポイントをWebに公開する場合management:
  endpoints:
    web:
      exposure:
        include: "*"

------------------------------
## 安全な運用のための注意点 ⚠️
Actuatorは非常に強力なツールですが、設定を誤ると重大なセキュリティリスクに繋がります。 [7, 9] 

* 
* 認証の必須化: /env や /beans などのエンドポイントには機密情報が含まれるため、インターネット上に公開してはいけません。必ず [Spring Security](https://spring.io/projects/spring-security/) などを組み合わせて管理者のみがアクセスできるように制限してください。 [1, 7] 
* 不要なエンドポイントの非公開: 外部からの死活監視（外形監視）の用途だけであれば、health のみを取り込み、他は非公開にするか、社内ネットワーク内のみにアクセスを限定するのが鉄則です。 [5] 
* 

------------------------------
Spring Boot Actuatorに関して、具体的に実装方法や特定の監視ツール（Prometheusなど）との連携方法、あるいはセキュリティ対策など、さらに詳しく知りたい情報はありますか？

[1] [https://qiita.com](https://qiita.com/HiroyaEnd/items/f640a6cd2657c42c69a2)
[2] [https://spring.pleiades.io](https://spring.pleiades.io/spring-boot/reference/actuator/)
[3] [https://dexall.co.jp](https://dexall.co.jp/articles/?p=451)
[4] [https://atmarkit.itmedia.co.jp](https://atmarkit.itmedia.co.jp/ait/articles/2412/13/news067.html)
[5] [https://qiita.com](https://qiita.com/b-yuko/items/25bd3ebb925237082289)
[6] [https://learn.microsoft.com](https://learn.microsoft.com/ja-jp/azure/spring-apps/basic-standard/concept-manage-monitor-app-spring-boot-actuator)
[7] [https://www.trendmicro.com](https://www.trendmicro.com/ja_jp/research/26/c/from-misconfigured-spring-boot-actuator-to-sharepoint-exfiltrati.html)
[8] [https://saycon.co.jp](https://saycon.co.jp/portal-for-newcomer/spring-boot/spring-boot3)
[9] [https://www.kotora.jp](https://www.kotora.jp/c/114401-2/)





Spring Boot Actuatorは、Spring Bootアプリケーションの稼働状態を監視・管理するための機能（プロダクションレディ機能）を提供するモジュールです。アプリケーションに組み込むだけで、メモリの使用状況、ヘルスチェック、設定情報などをHTTPエンドポイントやJMX経由でリアルタイムに取得できます。
開発時のデバッグから、本番環境での外形監視（ロードバランサーやKubernetesからの死活監視）まで幅広く活用されています。主なエンドポイント一覧デフォルトではセキュリティの観点から、/actuator/health 以外はWeb経由でのアクセスが制限されています。/actuator/health：アプリの死活状態やDB・ディスク等の健全性を表示。/actuator/metrics：メモリ、CPU、JVMの統計情報（メトリクス）を収集。/actuator/env：アプリケーションが認識している環境変数やプロパティの一覧を表示。/actuator/beans：DIコンテナ内に登録されているBeanオブジェクトの一覧。/actuator/mappings：コントローラーのURLマッピング（@RequestMapping）の一覧。/actuator/loggers：起動中のアプリケーションのログレベルをリアルタイムに確認・変更可能。


基本的な導入手順

1. 依存関係の追加Maven（pom.xml）またはGradle（build.gradle.kts）にスターターを追加します。kotlin// Gradle (Kotlin DSL) の例

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}

コードは注意してご使用ください。

2. 公開設定（application.yml）安全のために不要な情報は隠し、必要なエンドポイントだけを公開します。yamlmanagement:

  endpoints:
    web:
      exposure:
        # 必要最小限の公開（例: "health,info,metrics"）
        include: "health,metrics"
  endpoint:
    health:
      # 詳細情報を表示する（本番環境では注意）
      show-details: always
コードは注意してご使用ください。3. 動作確認設定後、アプリケーションを起動して /actuator/health にアクセスし、ヘルスチェック情報を確認します。
