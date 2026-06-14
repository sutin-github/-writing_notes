all-open（kotlin-allopen）は、Kotlinのクラスやメソッドに自動で open 修飾子を付与するコンパイラプラグインです。 [1] 
Kotlinではすべてのクラスとメソッドがデフォルトで final（継承・オーバーライド不可）ですが、Spring FrameworkやJPA（Hibernate）などのフレームワーク、または一部のモックテスト用ライブラリ（Mockitoなど）はクラスの継承やプロキシ生成を必要とします。これらを毎回手動で open にする手間を省くために使われます。 [2, 3] 
## 🛠️ 主な特徴とメリット

* 
* 自動 open 化: 指定したアノテーション（例: @Component, @Entity）が付いたクラスとそのメソッドを、コンパイル時に自動で open に変更します。
* Spring / JPA との相性が抜群: Spring用のプリセットプラグイン（kotlin("plugin.spring")）を使うと、Spring系のアノテーションが自動的に対象となります。
* コードのクリーン化: ソースコード上に不要な open キーワードを並べる必要がなくなり、Kotlinらしい簡潔な記述を維持できます。 [1, 2, 3] 
* 

## 💻 導入方法（Gradleの例）
[Kotlin公式ドキュメント](https://kotlinlang.org/docs/all-open-plugin.html)に基づき、build.gradle.kts に以下のように設定します。 [1] 

plugins {
    kotlin("plugin.allopen") version "2.4.0" // 使用するKotlinバージョンに合わせる
}

allOpen {
    // 自動でopenにしたいアノテーションを指定
    annotation("com.my.Annotation")
}

------------------------------
プログラミング言語の Kotlin に関する質問で合っていましたでしょうか？もし Spring Bootでの具体的な使い方 や、特定のテストフレームワークでの設定方法 などが知りたい場合は、お気軽にお知らせください！

[1] [https://kotlinlang.org](https://kotlinlang.org/docs/all-open-plugin.html)
[2] [https://www.baeldung.com](https://www.baeldung.com/kotlin/allopen-spring)
[3] [https://kcpoipoi.hatenablog.com](https://kcpoipoi.hatenablog.com/entry/2020/08/19/232240)
