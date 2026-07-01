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

A5:SQL Mk-2のコマンドラインユーティリティ（A5M2cmd.exe）でSQLを自動実行（データベースへ反映）させるには、/RunSQL コマンドを使用します。 [1] 

## 基本コマンド

```cmd
A5M2cmd.exe /RunSQL "/Connect=<接続文字列>" /FileName=<実行するSQLのパス> [追加オプション...]
```

実行例：

```cmd
A5M2cmd.exe /RunSQL "/Connect=__ConnectionType=Internal;ProviderName=PostgreSQL;UserName=db_user;Password=my_password;ServerName=localhost;Port=5432;Database=test_db" /InEncoding=UTF-8 /FileName=C:\work\model.sql
```

------------------------------
## 利用可能なオプション一覧
/RunSQL コマンドで指定できる主なオプションは以下の通りです。 [1] 
## 1. 必須および基本オプション

* /RunSQL
  * SQLスクリプトファイルを実行する機能（必須）。 [1] 
    * /Connect=<接続文字列>
     * データベースへの接続情報を指定します。非常に長い文字列になるため、引数全体をダブルクォーテーション "" で囲む必要があります。
     * ※接続文字列の簡単な作り方：GUI版のA5:SQL Mk-2のデータベース追加画面などから、接続文字列の形式に合わせたパラメータを取得するのがスムーズです。 [1, 2, 3] 
* /FileName=<ファイルパス>
  * 実行対象となるSQL（DDL）ファイルのパスを指定します。 [3, 4] 

## 2. 実行制御・エンコードオプション

* /InEncoding=<文字コード>
  * 読み込むSQLファイルの文字コードを指定します。
    * 指定値：UTF-8, Shift_JIS など [3] 
* /Separator=<セパレータ種類>
  * SQL文の区切り文字（セパレータ）を明示的に指定します。
    * 指定値：Semicolon（; 通常はこちら）, Slash（/ トリガーやストードプロシージャを含む場合など） [3] 
* /StopOnError
  * スクリプトの実行中にエラーが発生した場合、それ以降のSQL実行を中断します。初期のテーブル構築（DDL適用）時には付けておくことを推奨します。

------------------------------
## 💡 ER図からの出力とDB反映をワンクリックで行うバッチ（.bat）の例
毎回2つのコマンドを打つのは手間なため、以下のようにバッチファイル（.bat）にまとめておくと、「ER図からDDLを出力 ➡️ そのまま自動でDBに反映」 という一連のタスクを一瞬で自動化できます。 [5] 

```cmd
@echo off
set A5M2_PATH=C:\A5M2cmd\A5M2cmd.exe
set ERD_FILE=C:\work\model.a5er
set SQL_FILE=C:\work\model.sql

:: 接続文字列（環境に合わせて書き換えてください）
set DB_CONNECT=__ConnectionType=Internal;ProviderName=PostgreSQL;UserName=postgres;Password=password123;ServerName=localhost;Port=5432;Database=my_db

echo --- [1/2] ER図からDDLをエクスポート中... ---
"%A5M2_PATH%" /ERDDL /Encoding=UTF-8 /ERD="%ERD_FILE%" /OutFileName="%SQL_FILE%"

echo --- [2/2] データベースへDDLを反映中... ---
"%A5M2_PATH%" /RunSQL "/Connect=%DB_CONNECT%" /InEncoding=UTF-8 /FileName="%SQL_FILE%" /StopOnError

echo --- 反映が完了しました ---
pause
```

## 今後のステップ
自動化を進めるにあたって、以下のような詳細が必要であれば追加で案内が可能です。

* ご自身が利用しているRDBMS（MySQL, PostgreSQL, SQL Server, Oracleなど）に合わせた正確な/Connect（接続文字列）の書き方
* エラー発生時に自動でロールバックさせる手法（/RunSQL 自体は基本的にコミットされますが、トランザクション制御をSQL側に含める方法など） [2, 3] 

利用中のデータベースの種類や、バッチ化でつまずいている点などがあれば、お気軽にお知らせください。

[1] [https://future-architect.github.io](https://future-architect.github.io/articles/20221025a/)  
[2] [https://a5m2.mmatsubara.com](https://a5m2.mmatsubara.com/wp/?topic=a5sql-mk-2-%E3%82%B3%E3%83%9E%E3%83%B3%E3%83%89%E3%83%A9%E3%82%A4%E3%83%B3%E3%83%A6%E3%83%BC%E3%83%86%E3%82%A3%E3%83%AA%E3%83%86%E3%82%A3)  
[3] [https://github.com](https://github.com/szk302/dev-blog-content/blob/main/posts/A5M2%E3%81%A7%E4%BD%9C%E6%88%90%E3%81%97%E3%81%9FSQL%E3%82%92A5M2cmd%E3%81%A7%E3%82%B3%E3%83%9E%E3%83%B3%E3%83%89%E3%83%A9%E3%82%A4%E3%83%B3%E3%81%8B%E3%82%89%E5%AE%9F%E8%A1%8C%E3%81%99%E3%82%8B.adoc)  
[4] [https://qiita.com](https://qiita.com/akt_3310/items/8e3b1f124d5a1c58f23c)  
[5] [https://future-architect.github.io](https://future-architect.github.io/articles/20221025a/)  

A5:SQL Mk-2のコマンドラインユーティリティ（A5M2cmd.exe）を使い、ER図（.a5er）からHTML形式のテーブル定義書（エンティティ定義書）を自動出力するには、/ERHtml コマンドを使用します。 [1] 
以下に基本コマンド、オプション、そして前述の「DDL生成」や「DB反映」とすべて同時に行う最強の自動化バッチ例を解説します。

## 基本コマンド

```cmd
A5M2cmd.exe /ERHtml /ERD=<ER図ファイルのパス> /OutDir=<出力先フォルダのパス>
```

実行例：

```cmd
A5M2cmd.exe /ERHtml /ERD=C:\work\model.a5er /OutDir=C:\work\html_doc
```
※HTML出力の場合、複数のファイル（各テーブルの個別画面など）が生成されるため、ファイル名ではなく「出力先フォルダ（/OutDir）」を指定します。

------------------------------
## 利用可能なオプション一覧
/ERHtml コマンドで指定できるオプションは以下の通りです。

* /ERHtml
  * ER図からHTMLドキュメント（テーブル定義書）をエクスポートする必須コマンドです。
   * /ERD=<ファイルパス>
     * 対象となるER図ファイル（.a5er）のパスを指定します。 [1] 

* /OutDir=<フォルダパス>
  * HTMLファイル一式を出力する保存先フォルダを指定します。フォルダが存在しない場合は自動で作成されます。
    * /Encoding=<文字コード>
    * 出力されるHTMLファイルの文字コードを指定します。
    * 指定値：UTF-8, Shift_JIS （指定しない場合は環境のデフォルトになります）

------------------------------
## 💡 DDL出力・DB反映・HTML定義書を同時に行うフル自動バッチ
すべての処理（DDL抽出 ➡️ DB反映 ➡️ HTMLテーブル定義書出力）をワンクリックで同時に実行するバッチファイル（.bat）の構築例です。
これを実行すれば、ER図を修正した直後に、本番・検証環境の更新から最新ドキュメントの生成までが数秒で完結します。

```cmd
@echo off
set A5M2_PATH=C:\A5M2cmd\A5M2cmd.exe
set ERD_FILE=C:\work\model.a5er
set SQL_FILE=C:\work\model.sql
set HTML_DIR=C:\work\html_doc

:: データベース接続文字列
set DB_CONNECT=__ConnectionType=Internal;ProviderName=PostgreSQL;UserName=postgres;Password=password123;ServerName=localhost;Port=5432;Database=my_db

echo ===================================================
echo  [1/3] ER図からDDL（SQL）をエクスポート中...
echo ===================================================
"%A5M2_PATH%" /ERDDL /Encoding=UTF-8 /ERD="%ERD_FILE%" /OutFileName="%SQL_FILE%"

echo ===================================================
echo  [2/3] データベースへDDLを反映中...
echo ===================================================
"%A5M2_PATH%" /RunSQL "/Connect=%DB_CONNECT%" /InEncoding=UTF-8 /FileName="%SQL_FILE%" /StopOnError

echo ===================================================
echo  [3/3] 最新のHTMLテーブル定義書を出力中...
echo ===================================================
"%A5M2_PATH%" /ERHtml /Encoding=UTF-8 /ERD="%ERD_FILE%" /OutDir="%HTML_DIR%"

echo ===================================================
echo  すべての自動処理が正常に完了しました！
echo ===================================================
pause
```

## 注意点

* 出力される見た目（テーマカラー等）：コマンドラインからHTMLの細かなデザインやフォントを変更することはできません。これらはGUI版の A5:SQL Mk-2の「オプション」内にある設定 が引き継がれます。 [2] 
* Excelで出力したい場合：HTMLではなくExcel形式（.xlsx）で出力したい場合は、コマンドの先頭を /ERHtml から /ERExcel に変更し、/OutDir の代わりに /OutFileName=C:\work\doc.xlsx と指定すれば対応可能です。 [2, 3, 4] 

## 今後のステップ
この自動化をさらに社内環境へ適合させるため、以下のようなカスタマイズの提案が可能です。

* 生成されたHTML定義書を社内の共有サーバー（ファイルサーバーやWebサーバー）へ自動転送する記述の追加
* Gitなどのバージョン管理システムと連携させ、ER図変更時にこのバッチを自動キック（CI/CD化）する方法 [5] 

自動生成されたHTMLの確認方法や、他のファイル形式への変更などで気になる点があれば、お気軽にご相談ください。

[1] [https://yujiorama.hatenablog.com](https://yujiorama.hatenablog.com/entry/2021/04/19/075906)  
[2] [https://a5m2.mmatsubara.com](https://a5m2.mmatsubara.com/wp/?topic=er%E5%9B%B3%E3%81%8B%E3%82%89%E3%81%AEexcel%E3%82%A8%E3%83%B3%E3%83%86%E3%82%A3%E3%83%86%E3%82%A3%E5%AE%9A%E7%BE%A9%E6%9B%B8%E3%81%AE%E5%87%BA%E5%8A%9B%E3%83%95%E3%82%A9%E3%83%B3%E3%83%88)  
[3] [https://qiita.com](https://qiita.com/haruki_nakayama/items/c42c713a9eb69d17e547)  
[4] [https://www.zumuzack.com](https://www.zumuzack.com/zack/table-define-tool/how-to-use/tutorial-a5er/)  
[5] [https://nabedge.mixer2.org](https://nabedge.mixer2.org/2016/07/database-doc.html)  
