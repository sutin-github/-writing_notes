A5:SQL Mk-2のコマンドラインユーティリティ（A5M2cmd.exe）を使い、  
ER図（.a5er）からDDLを出力する基本コマンドと、利用できるオプションの一覧です。 [1, 2] 

## 基本コマンド

```cmd
A5M2cmd.exe /ERDDL /ERD=<ER図ファイルのパス> /OutFileName=<出力DDLファイルのパス> [追加オプション...]
```

実行例：

```cmd
A5M2cmd.exe /ERDDL /Encoding=UTF-8 /ERD=C:\work\model.a5er /OutFileName=C:\work\model.sql
```

------------------------------
## 利用可能なすべてのオプション
/ERDDL コマンドで指定できるオプションは以下の通りです。
## 1. 必須および基本オプション

* /ERDDL
  * ER図からDDLを生成する機能を指定する必須コマンドです。 [1, 2] 

* /ERD=<ファイルパス>
  * 対象となるER図ファイル（.a5er）のパスを指定します。 [1, 2] 

* /OutFileName=<ファイルパス>
  * 出力するDDL（SQLファイル）の保存先パスを指定します。 [1, 2] 

## 2. 出力設定オプション

* /Encoding=<文字コード>
  * 出力されるSQLファイルの文字コードを指定します。
    * 指定値：UTF-8, Shift_JIS など [1, 3] 

* /TargetDBMS=<RDBMS名>
  * 特定のRDBMSに最適化されたDDLを出力する場合に指定します。
    * 指定値：Oracle, SQLServer, PostgreSQL, MySQL など（ER図側で定義されたターゲットに準拠します） [4, 5, 6] 

* /GenerateCommentStatement=<値>
  * テーブルやカラムに対するコメント（論理名や説明文）をDDL内にどう出力するかを制御します。
     * 指定値：
     * None（コメントを出力しない）
        * LName（論理名を出力する）
        * Description（説明文を出力する）
        * Both（論理名と説明文の両方を出力する） [7, 8] 

------------------------------
## 注意点

* 改行コードの制御：現行の A5M2cmd.exe の仕様では、DDL出力時の改行コード（CRLF / LF）をコマンドライン引数から直接変更することができません。  LFに変更したい場合は、出力後に外部ツールやスクリプト（sed や tr など）を用いて変換する必要があります。 [9] 
* より詳細な挙動（外部キーの出力有無など）は、原則としてER図ファイル（.a5er）内部に保存されている「プロパティ」や「DDL作成オプション」の進捗状態が引き継がれます。

## 今後のステップ
もし特定の自動化タスク（CI/CDへの組み込みなど）を検討されている場合、以下のような追加の提案が可能です。

* MakefileやPowerShellを用いた自動生成スクリプトの作成方法
* DDL出力後に自動でデータベースへ反映（実行）させるコマンド（/RunSQL）との組み合わせ
* ER図からテーブル定義書（Excel）を同時に自動出力する方法 [4, 7, 9, 10] 

ご希望の用途に合わせて具体的なスクリプト例を提示することもできますので、お気軽にお知らせください。

[1] [https://future-architect.github.io](https://future-architect.github.io/articles/20221025a/)  
[2] [https://yujiorama.hatenablog.com](https://yujiorama.hatenablog.com/entry/2021/04/19/075906)  
[3] [https://ja.astahblog.com](https://ja.astahblog.com/2016/12/02/encoding-at-sql-export/)  
[4] [https://fintan.jp](https://fintan.jp/page/17755/)  
[5] [https://a5m2.mmatsubara.com](https://a5m2.mmatsubara.com/wp/?topic=a5m2cmd-exe-%E3%81%AE%E3%82%BF%E3%82%A4%E3%83%A0%E3%82%A2%E3%82%A6%E3%83%88%E6%99%82%E9%96%93%E3%81%AB%E3%81%A4%E3%81%84%E3%81%A6)  
[6] [https://a5m2.mmatsubara.com](https://a5m2.mmatsubara.com/wp/?topic=a5m2cmd%E3%81%A7%E3%81%AE%E8%A4%87%E6%95%B0sql%E5%AE%9F%E8%A1%8C%E6%99%82%E3%81%AE%E5%87%BA%E5%8A%9B%E4%B8%8A%E9%99%90%E3%81%AB%E3%81%A4%E3%81%84%E3%81%A6)  
[7] [https://a5m2.mmatsubara.com](https://a5m2.mmatsubara.com/wp/?topic=ddl%E4%BD%9C%E6%88%90%E6%99%82%E3%81%AEview%E3%81%B8%E3%81%AE%E3%82%B3%E3%83%A1%E3%83%B3%E3%83%88%E7%99%BB%E9%8C%B2%E3%81%AB%E3%81%A4%E3%81%84%E3%81%A6)  
[8] [https://a5m2.mmatsubara.com](https://a5m2.mmatsubara.com/wp/?topic=ddl%E4%BD%9C%E6%88%90%E6%99%82%E3%81%AEview%E3%81%B8%E3%81%AE%E3%82%B3%E3%83%A1%E3%83%B3%E3%83%88%E7%99%BB%E9%8C%B2%E3%81%AB%E3%81%A4%E3%81%84%E3%81%A6)  
[9] [https://future-architect.github.io](https://future-architect.github.io/articles/20221025a/)
[10] [https://a5m2.mmatsubara.com](https://a5m2.mmatsubara.com/wp/?bbp_user=1&bbp_reps=1&paged=71)
