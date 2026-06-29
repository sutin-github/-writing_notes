# 対象テーブルのテーブルコメントを更新

対象のテーブル名とコメント内容を定義したリストを読み込み、psqlコマンドを使用してCOMMENT ON TABLEを一括実行するシェルスクリプトです。  
設定値を変更することで、複数のテーブルに異なるコメントを効率的に設定できます。

## テーブルコメント一括更新シェルスクリプト (update_table_comments.sh)

```bash
#!/bin/bash
# ==========================================
# 設定情報
# ==========================================
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="your_database"
DB_USER="your_username"
export PGPASSWORD="your_password" # パスワードを環境変数に設定

# ==========================================
# 対象テーブルとコメントのリスト (テーブル名:コメント)
# ==========================================
TABLE_COMMENTS=(
    "m_users:会員情報を管理するマスタテーブル"
    "t_orders:注文履歴を記録するトランザクションテーブル"
    "m_products:商品情報を管理するマスタテーブル"
)
# ==========================================
# コメント更新処理
# ==========================================
for item in "${TABLE_COMMENTS[@]}"; do
    # コロンで分割
    IFS=":" read -r table_name comment <<< "$item"

    echo "コメントを更新中 -> テーブル: $table_name, コメント: $comment"

    # psqlでCOMMENT ON TABLE文を実行
    psql -h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME" -U "$DB_USER" -c \
        "COMMENT ON TABLE $table_name IS '$comment';"
    
    if [ $? -eq 0 ]; then
        echo "  -> 更新成功"
    else
        echo "  -> 更新失敗"
fi
done

echo "すべての処理が完了しました。"
```

## 使い方

   1. スクリプト内の 設定情報（ホスト名、DB名、ユーザー名、パスワード）をご利用の環境に合わせて変更します。
   2. TABLE_COMMENTS 配列に、"テーブル名:設定したいコメント" の形式で対象を追加・編集します。
   3. スクリプトに実行権限を付与して実行します。

   ```bash
   chmod +x update_table_comments.sh
   ./update_table_comments.sh
   ```
   
コメントを更新する対象が大量にある場合や、自動的に取得したい場合、以下のいずれかについて教えていただけますか？

* 外部のCSVファイルなどの一覧から読み込んで実行したいですか？
* 既存のコメントを一括で取得するSQLが知りたいですか？

PostgreSQLでテーブルにRLS（行レベルセキュリティ / Row Level Security）を適用するための基本的なSQL構文と、よく使われる具体例です。
RLSを有効にすると、ユーザーの権限やログイン情報（current_userなど）に応じて、同じテーブルでも見えたり操作できたりするデータをレコード（行）単位で制限できます。

------------------------------
## 1. RLSの基本手順（3ステップ）
RLSを適用するには、①テーブルのRLS機能を有効化し、②アクセス制限のルール（ポリシー）を作成します。

```sql
-- ステップ1: 対象テーブルのRLSを有効化する
ALTER TABLE m_users ENABLE ROW LEVEL SECURITY;
-- ステップ2: 参照（SELECT）用のポリシーを作成する
CREATE POLICY user_select_policy ON m_users
    FOR SELECT
    TO public -- すべてのロールを対象（特定のロール指定も可）
    USING (current_user = username); -- ログインユーザーと一致する行だけ許可
-- ステップ3: 更新（UPDATE）用のポリシーを作成する
CREATE POLICY user_update_policy ON m_users
    FOR UPDATE
    TO public
    USING (current_user = username)       -- 変更前のデータに対する条件
    WITH CHECK (current_user = username); -- 変更後のデータに対する条件
```

* USING: SELECTやDELETEなど、既存のデータを読み込む・削除する際のフィルター条件です。
* WITH CHECK: INSERTやUPDATEなど、新しくデータを書き込む・書き換える際、保存を許可するか判定する条件です。

------------------------------
## 2. よく使われる実用的なポリシー例## 例A: 自分の所属する「店舗（テナント）」のデータだけを許可する（マルチテナント型）
アプリケーションでよく使われる、セッション変数（アプリ側から一時的にセットする値）を利用してテナントを分離する手法です。

```sql
-- RLSの有効化
ALTER TABLE t_orders ENABLE ROW LEVEL SECURITY;
-- 「app.current_tenant_id」というカスタム変数と一致する注文データのみ参照・追加を許可
CREATE POLICY tenant_order_policy ON t_orders
    AS RESTRICTIVE -- 他のポリシーとAND条件で組み合わせる場合に指定（省略時はOR）
    FOR ALL        -- SELECT, INSERT, UPDATE, DELETE すべてに適用
    USING (tenant_id = current_setting('app.current_tenant_id'))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id'));
```

※アプリ側からSQLを実行する直前に SET LOCAL app.current_tenant_id = 'テナントID'; を実行して制御します。

## 例B: 管理者（Admin）にはすべてのアクセスを許可する
一般ユーザーには制限をかけつつ、管理者ロールには全データを操作させたい場合の記述です。

```sql
-- 管理者ロール（admin_role）にはすべての操作を無条件で許可
CREATE POLICY admin_all_policy ON m_users
    FOR ALL
    TO admin_role
    USING (true)
    WITH CHECK (true);
```

------------------------------
## 3. 設定の確認と解除

```sql
-- RLSが有効になっているかテーブル一覧を確認する
SELECT relname, relrowsecurity FROM pg_class WHERE relname = 'm_users';
-- 作成されたポリシーの一覧を確認する
SELECT * FROM pg_policies WHERE tablename = 'm_users';
-- RLSを一時的に無効化する
ALTER TABLE m_users DISABLE ROW LEVEL SECURITY;
```

💡 注意点
テーブルの所有者（オーナー）やスーパーユーザー（postgresなど）は、デフォルトでRLSの制限を無視（バイパス）します。  
動作テストを行う際は、必ず権限を制限した「一般ユーザー」に切り替えてSQLを実行してください。

設計中のシステムに合わせて最適なポリシーを提案できます。もしよろしければ以下について教えてください。

* どのような条件で行を制限したいですか？（例：ユーザーIDの一致、店舗コードの一致、ステータスによる制限など）
* アプリケーションの接続は、共通のDBユーザー（Connection Pool等）を使っていますか？それともユーザーごとにDBアカウントを分けていますか？


# 複数の対象テーブルにRLSを適用するSQLが知りたい

複数のテーブルに一括でRLS（行レベルセキュリティ）を適用する場合、手動で1テーブルずつSQLを書くのは大変なため、「PL/pgSQL（無名ブロック）」を使ってループ処理で一括定義するのが最も効率的です。
以下に、指定した複数のテーブルに対して「RLSの有効化」と「ポリシーの作成」を自動で行うSQLスクリプトを紹介します。

------------------------------
## 複数の対象テーブルに一括でRLSを適用するSQL
このスクリプトを実行すると、指定したテーブル配列（tables）を順に処理し、同じルール（例：tenant_idの一致）のRLSを自動で一括適用します。

```sql
DO $$
DECLARE
    -- RLSを適用したいテーブル名の配列を定義
    tables text[] := ARRAY['t_orders', 't_invoices', 'm_products', 't_stocks'];
    t text;
BEGIN
    -- 配列内の各テーブルをループ処理
    FOREACH t IN ARRAY tables LOOP
        
        -- 1. テーブルのRLSを有効化
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY;', t);
    
        -- 2. 既存の同名ポリシーがあれば削除（二重実行時のエラー防止）
        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation_policy ON %I;', t);
    
        -- 3. RLSポリシーを作成（例：セッション変数 app.current_tenant_id と tenant_id 列の一致）
        EXECUTE format('
            CREATE POLICY tenant_isolation_policy ON %I
            FOR ALL
            USING (tenant_id = current_setting(''app.current_tenant_id''))
            WITH CHECK (tenant_id = current_setting(''app.current_tenant_id''));
        ', t);
    
        RAISE NOTICE 'テーブル % に RLS を適用しました。', t;
    END LOOP;
END $$;
```

## SQLのポイント

* format('%I', t): テーブル名（識別子）を安全にエスケープしてSQL文に埋め込みます（SQLインジェクション対策）。
* current_setting(...): アプリケーション（Webサーバー等）から接続時に一時セットする変数（テナントIDなど）を利用して、全てのテーブルを動的に一括ガードします。

------------------------------
## 特定のスキーマの「全テーブル」に対象を広げたい場合
もしテーブル名を個別に指定せず、「publicスキーマにある、tenant_id というカラムを持つすべてのテーブル」に自動適用したい場合は、情報スキーマ（information_schema）から自動抽出する以下のSQLが便利です。

```
DO $$
DECLARE
    r RECORD;BEGIN
    -- 「public」スキーマ内で、かつ「tenant_id」というカラムを持つテーブルを自動抽出
    FOR r IN 
        SELECT table_name 
        FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND column_name = 'tenant_id'
    LOOP
        -- RLSの有効化
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY;', r.table_name);
        
        -- ポリシーの作成
        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation_policy ON %I;', r.table_name);
        EXECUTE format('
            CREATE POLICY tenant_isolation_policy ON %I
            FOR ALL
            USING (tenant_id = current_setting(''app.current_tenant_id''))
            WITH CHECK (tenant_id = current_setting(''app.current_tenant_id''));
        ', r.table_name);
        
        RAISE NOTICE '自動適用: テーブル % に RLS を適用しました。', r.table_name;
    END LOOP;
END $$;
```

一括適用するにあたり、ポリシーの「条件（USING句）」を実環境に合わせる必要があります。

* 今回一括適用したいテーブル群には、共通の絞り込みカラム（例：tenant_id や user_id）が存在しますか？
* それとも、テーブルによって絞り込みに使うカラム名がバラバラ、あるいは別テーブルと結合（JOIN）して判定する複雑なルールが必要ですか？

## 共通の絞り込みカラムが存在します。

共通の絞り込みカラム（例: tenant_id や company_id など）が存在するのであれば、SQLスクリプトをさらにシンプルかつ安全に共通化できます。
ここでは、絞り込みカラム名を変数として定義し、指定した複数のテーブルに一括でポリシーを適用する決定版のSQLスクリプト（PL/pgSQL）を用意しました。

------------------------------
## 共通カラムで一括処理するRLSスクリプト
以下のスクリプトの target_column と tables を書き換えて実行するだけで、対象テーブルすべてに同一ルールのセキュリティが適用されます。

```sql
DO $$
DECLARE
    -- 【設定】一括で絞り込みに使う共通のカラム名
    target_column text := 'tenant_id'; 
    
    -- 【設定】RLSを適用したいテーブル名の配列
    tables text[] := ARRAY['t_orders', 't_invoices', 'm_products', 't_stocks'];
    
    t text;
BEGIN
    -- 指定されたテーブルを順に処理
    FOREACH t IN ARRAY tables LOOP
        
        -- 1. テーブルのRLSを有効化
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY;', t);
        
        -- 2. 既存の同名ポリシーがあれば削除（重複エラー防止）
        EXECUTE format('DROP POLICY IF EXISTS common_rls_policy ON %I;', t);
        
        -- 3. 動的に共通カラムを指定してポリシーを作成
        -- セッション変数「app.current_id」にセットされた値とカラムの値が一致する行のみアクセスを許可
        EXECUTE format('
            CREATE POLICY common_rls_policy ON %I
            FOR ALL
            USING (%I = current_setting(''app.current_id''))
            WITH CHECK (%I = current_setting(''app.current_id''));
        ', t, target_column, target_column);
        
        RAISE NOTICE 'テーブル [%] のカラム [%] にRLSポリシーを適用しました。', t, target_column;
    END LOOP;
END $$;
```

## この運用の仕組み（アプリケーション側の実装）
このRLSスクリプトを適用した後は、システム（Webアプリケーションなど）側でDB接続直後（またはトランザクション開始直後）に、以下のSQLを1行実行するように実装します。

-- ログインしているユーザーのテナントID（例: 123）をセッションにセットSET LOCAL app.current_id = '123';
-- この状態でSQLを投げると、自動的に「WHERE tenant_id = '123'」が裏で強制適用されるSELECT * FROM t_orders; 

※ SET LOCAL を使用すると、そのトランザクション（COMMIT または ROLLBACK まで）の間だけ有効になるため、コネクションプールを使っていても安全です。

------------------------------
## 💡 運用上の注意点

   1. カラムのデータ型に注意
   current_setting() で取得できる値は常にテキスト型（text）になります。もし共通カラムが数値型（integer や bigint）の場合は、ポリシー内の記述を以下のようにキャスト（型変換）する必要があります。
   * 変更前：%I = current_setting(...)
   * 変更後：%I = current_setting(...)::integer （または ::bigint）

   2. テーブル所有者のバイパス
   前述の通り、マイグレーション等で使用するDBの「オーナー権限を持つユーザー」で接続すると、このRLSを素通りして全データが見えてしまいます。アプリからの接続は、必ず権限を絞った一般ユーザー（db_userなど）で行ってください。

一括設定の準備を進めるにあたり、以下の点を確認させていただけますか？

* 共通カラムのデータ型は 数値（整数） と 文字列（UUIDなど） のどちらでしょうか？（必要であればキャスト対応のSQLに修正します）
* 最初の質問にあった「シェルスクリプト」のように、このSQLもシェルから外部ファイルとして実行できるようにコマンドを用意しましょうか？

# 共通の絞り込みカラムが存在します。

共通の絞り込みカラム（例: tenant_id や company_id など）が存在するのであれば、SQLスクリプトをさらにシンプルかつ安全に共通化できます。
ここでは、絞り込みカラム名を変数として定義し、指定した複数のテーブルに一括でポリシーを適用する決定版のSQLスクリプト（PL/pgSQL）を用意しました。

------------------------------
## 共通カラムで一括処理するRLSスクリプト
以下のスクリプトの target_column と tables を書き換えて実行するだけで、対象テーブルすべてに同一ルールのセキュリティが適用されます。

```sql
DO $$
DECLARE
    -- 【設定】一括で絞り込みに使う共通のカラム名
    target_column text := 'tenant_id'; 
    
    -- 【設定】RLSを適用したいテーブル名の配列
    tables text[] := ARRAY['t_orders', 't_invoices', 'm_products', 't_stocks'];
    
    t text;
BEGIN
    -- 指定されたテーブルを順に処理
    FOREACH t IN ARRAY tables LOOP
        
        -- 1. テーブルのRLSを有効化
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY;', t);
        
        -- 2. 既存の同名ポリシーがあれば削除（重複エラー防止）
        EXECUTE format('DROP POLICY IF EXISTS common_rls_policy ON %I;', t);
        
        -- 3. 動的に共通カラムを指定してポリシーを作成
        -- セッション変数「app.current_id」にセットされた値とカラムの値が一致する行のみアクセスを許可
        EXECUTE format('
            CREATE POLICY common_rls_policy ON %I
            FOR ALL
            USING (%I = current_setting(''app.current_id''))
            WITH CHECK (%I = current_setting(''app.current_id''));
        ', t, target_column, target_column);
        
        RAISE NOTICE 'テーブル [%] のカラム [%] にRLSポリシーを適用しました。', t, target_column;
    END LOOP;
END $$;
```

## この運用の仕組み（アプリケーション側の実装）
このRLSスクリプトを適用した後は、システム（Webアプリケーションなど）側でDB接続直後（またはトランザクション開始直後）に、以下のSQLを1行実行するように実装します。

-- ログインしているユーザーのテナントID（例: 123）をセッションにセットSET LOCAL app.current_id = '123';
-- この状態でSQLを投げると、自動的に「WHERE tenant_id = '123'」が裏で強制適用されるSELECT * FROM t_orders; 

※ SET LOCAL を使用すると、そのトランザクション（COMMIT または ROLLBACK まで）の間だけ有効になるため、コネクションプールを使っていても安全です。

------------------------------
## 💡 運用上の注意点

   1. カラムのデータ型に注意
   current_setting() で取得できる値は常にテキスト型（text）になります。もし共通カラムが数値型（integer や bigint）の場合は、ポリシー内の記述を以下のようにキャスト（型変換）する必要があります。
   * 変更前：%I = current_setting(...)
      * 変更後：%I = current_setting(...)::integer （または ::bigint）
   2. テーブル所有者のバイパス
   前述の通り、マイグレーション等で使用するDBの「オーナー権限を持つユーザー」で接続すると、このRLSを素通りして全データが見えてしまいます。アプリからの接続は、必ず権限を絞った一般ユーザー（db_userなど）で行ってください。

一括設定の準備を進めるにあたり、以下の点を確認させていただけますか？

* 共通カラムのデータ型は 数値（整数） と 文字列（UUIDなど） のどちらでしょうか？（必要であればキャスト対応のSQLに修正します）
* 最初の質問にあった「シェルスクリプト」のように、このSQLもシェルから外部ファイルとして実行できるようにコマンドを用意しましょうか？

# 共通の絞り込みカラムがあるテーブルとないテーブルがある

「共通の絞り込みカラムがあるテーブル」と「ないテーブル」が混在しているのですね。
その場合、ポリシーの条件（USING句）を固定せず、テーブルごとに条件を分岐させるか、ないテーブルには「全ユーザーに全行許可（または拒否）」などの別ルールを適用する必要があります。
これを自動で安全に処理するために、「指定したカラムがテーブルに存在するかどうか」をSQL側で自動判定して処理を分けるスクリプトを作成しました。

------------------------------
## カラムの有無を自動判定してRLSを適用するSQL
このスクリプトは、指定されたテーブル群をループ処理し、

* 共通カラム（例: tenant_id）があるテーブル ➡️ カラム値で行を絞り込むポリシーを作成
* 共通カラムがないテーブル ➡️ 制限なしで全行アクセスを許可するポリシーを作成（または必要に応じて拒否に変更可能）
という処理を自動で行います。

```sql
DO $$
DECLARE
    -- 【設定】一括でチェックしたい共通のカラム名
    target_column text := 'tenant_id'; 
    
    -- 【設定】RLSを設定したいテーブル名の配列（カラムがあるもの・ないもの混在）
    tables text[] := ARRAY['t_orders', 't_invoices', 'm_products', 'm_categories'];
    
    t text;
    has_column boolean;BEGIN
    FOREACH t IN ARRAY tables LOOP
        
        -- 1. まずテーブルのRLSを有効化
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY;', t);
        EXECUTE format('DROP POLICY IF EXISTS auto_rls_policy ON %I;', t);
        
        -- 2. 対象テーブルに指定のカラムが存在するか確認
        SELECT EXISTS (
            SELECT 1 
            FROM information_schema.columns 
            WHERE table_schema = 'public' -- 必要に応じてスキーマ名を変更してください
              AND table_name = t 
              AND column_name = target_column
        ) INTO has_column;
        
        -- 3. カラムの有無によってポリシーの条件を分岐
        IF has_column THEN
            -- 【カラムがある場合】セッション変数と一致する行のみ許可
            EXECUTE format('
                CREATE POLICY auto_rls_policy ON %I FOR ALL
                USING (%I = current_setting(''app.current_id''))
                WITH CHECK (%I = current_setting(''app.current_id''));
            ', t, target_column, target_column);
            
            RAISE NOTICE '【制御対象】テーブル [%] にカラム [%] によるRLSを適用しました。', t, target_column;
        ELSE
            -- 【カラムがない場合】すべての行へのアクセスを許可（全体マスタなどの想定）
            EXECUTE format('
                CREATE POLICY auto_rls_policy ON %I FOR ALL
                USING (true)
                WITH CHECK (true);
            ', t);
            
            RAISE NOTICE '【全体共有】テーブル [%] はカラム [%] がないため、全行許可でRLSを適用しました。', t, target_column;
        END IF;

    END LOOP;
END $$;
```

------------------------------
## 設計上のポイント

   1. カラムがないテーブル（マスタ等）の扱い
   上記スクリプトでは、カラムがないテーブル（例: 全社共通の m_categories など）に対して USING (true)（全行許可）を設定しています。もし「カラムがないテーブルは一般ユーザーには一切見せたくない（アクセス拒否）」という場合は、USING (false) に書き換えてください。
   2. データ型の考慮
   もし共通カラムが数値型（integer や bigint）の場合は、先述の通り %I = current_setting(...) の部分を %I = current_setting(''app.current_id'')::integer のように型変換（キャスト）を挟むように記述を調整してください。

柔軟なルール設定ができる状態になりました。次のステップに向けて、以下について教えていただけますか？

* 共通カラムがないテーブル（マスタデータなど）は、ログインユーザーに関係なく「全行だれでも見られる（全行許可）」状態にして大丈夫ですか？
* 共通カラムのデータ型は、数値型（integerなど）と文字列型（textやUUIDなど）のどちらでしょうか？


# 共通カラムがないテーブルは、ログインユーザーに関係なく「全行だれでも見られる（全行許可）」状態にして大丈夫です。
共通カラムのデータ型は、bigintです。

条件が確定しました。共通カラムがないテーブルは「全行許可（USING (true)）」とし、共通カラムのデータ型が bigint であるため、  
テキスト型として取得されるセッション変数（current_setting）を ::bigint へキャスト（型変換）する 記述を追加します。
これにより、型エラーを防ぎつつ安全に動作するようになります。

------------------------------
## 型キャスト対応・カラム自動判定付き RLS適用SQL
以下のスクリプトを PostgreSQL で実行してください。

```sql
DO $$
DECLARE
    -- 【設定】一括でチェックしたい共通のカラム名
    target_column text := 'tenant_id'; 
    
    -- 【設定】RLSを設定したいテーブル名の配列（カラム混在）
    tables text[] := ARRAY['t_orders', 't_invoices', 'm_products', 'm_categories'];
    
    t text;
    has_column boolean;BEGIN
    FOREACH t IN ARRAY tables LOOP
        
        -- 1. テーブルのRLSを有効化
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY;', t);
        EXECUTE format('DROP POLICY IF EXISTS auto_rls_policy ON %I;', t);
        
        -- 2. 対象テーブルに指定のカラム（bigint型）が存在するか確認
        SELECT EXISTS (
            SELECT 1 
            FROM information_schema.columns 
            WHERE table_schema = 'public' -- 必要に応じてスキーマ名を変更してください
              AND table_name = t 
              AND column_name = target_column
        ) INTO has_column;
        
        -- 3. カラムの有無によってポリシーの条件を分岐
        IF has_column THEN
            -- 【カラムがある場合】セッション変数を bigint にキャストして比較
            EXECUTE format('
                CREATE POLICY auto_rls_policy ON %I FOR ALL
                USING (%I = current_setting(''app.current_id'')::bigint)
                WITH CHECK (%I = current_setting(''app.current_id'')::bigint);
            ', t, target_column, target_column);
            
            RAISE NOTICE '【制御対象】テーブル [%] にカラム [%] (bigint) によるRLSを適用しました。', t, target_column;
        ELSE
            -- 【カラムがない場合】すべての行へのアクセスを許可（全行許可）
            EXECUTE format('
                CREATE POLICY auto_rls_policy ON %I FOR ALL
                USING (true)
                WITH CHECK (true);
            ', t);
            
            RAISE NOTICE '【全行許可】テーブル [%] はカラム [%] がないため、制限なしでRLSを適用しました。', t, target_column;
        END IF;

    END LOOP;
END $$;
```

------------------------------
## アプリケーション側での実行方法（bigint対応）
アプリケーションからクエリを発行する際は、トランザクション開始直後にセッション変数をセットします。bigint の範囲内であれば、数値の文字列をそのまま渡して大丈夫です。

```sql
-- トランザクション開始
BEGIN;
-- ログイン中のテナントID（bigint想定の数値文字列）をセット
SET LOCAL app.current_id = '9223372036854775807';
-- 共通カラムがあるテーブル ➡️ 自動で「WHERE tenant_id = 9223372036854775807::bigint」が適用される
SELECT * FROM t_orders;
-- 共通カラムがないテーブル ➡️ RLSは有効ですが、全行がそのまま取得できる
SELECT * FROM m_categories;
-- トランザクション終了
COMMIT;
```

これで複数テーブルへの一括RLS適用スクリプトは完成です。

* このSQL文を実際のデータベース（本番環境やステージング環境）へ反映・適用する手順について、何かサポートが必要な部分はありますか？
* 必要であれば、最初の質問にあったようなシェルスクリプト（.sh）の形式にまとめて、コマンド一発で反映できるようにコードを出力することも可能ですが、いかがでしょうか？


# 共通カラムがないテーブルは、RLSを有効化する必要がないのではないか

その通りです。ご指摘の通り、共通カラムがない（＝全行だれでも見られる）テーブルは、わざわざRLSを有効化（ENABLE ROW LEVEL SECURITY）する必要はありません。
PostgreSQLでは、RLSが「無効（DISABLE）」のテーブルは、デフォルトで全行へのアクセスが許可される仕様になっているためです。
共通カラムがないテーブルに対してRLSを有効化して USING (true) のポリシーを作るのは、セキュリティ上の効果が同じであるにもかかわらず、データベースの内部処理（オーバーヘッド）を無駄に増やしてしまうデメリットしかありません。
したがって、「共通カラムがあるテーブルだけRLSを有効にしてポリシーを作り、ないテーブルは完全にスルーする」のが最もスマートで、パフォーマンス的にも最善の設計です。

------------------------------
## 改良版：必要なテーブルだけRLSを有効化するSQL
ご指摘を踏まえ、カラムが存在しないテーブルには一切触れない（RLSも有効化しない）ように処理を最適化したSQLです。

```sql
DO $$
DECLARE
    -- 【設定】一括でチェックしたい共通のカラム名
    target_column text := 'tenant_id'; 
    
    -- 【設定】RLSを設定したいテーブル名の配列（カラム混在）
    tables text[] := ARRAY['t_orders', 't_invoices', 'm_products', 'm_categories'];
    
    t text;
    has_column boolean;BEGIN
    FOREACH t IN ARRAY tables LOOP
        
        -- 1. 対象テーブルに指定のカラム（bigint型）が存在するか「だけ」をまず確認
        SELECT EXISTS (
            SELECT 1 
            FROM information_schema.columns 
            WHERE table_schema = 'public' -- 必要に応じてスキーマ名を変更してください
              AND table_name = t 
              AND column_name = target_column
        ) INTO has_column;
        
        -- 2. カラムが存在するテーブルだけ処理を行う
        IF has_column THEN
            -- テーブルのRLSを有効化
            EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY;', t);
            
            -- 既存の同名ポリシーがあれば削除（重複エラー防止）
            EXECUTE format('DROP POLICY IF EXISTS auto_rls_policy ON %I;', t);
            
            -- ポリシーを作成（bigintキャスト対応）
            EXECUTE format('
                CREATE POLICY auto_rls_policy ON %I FOR ALL
                USING (%I = current_setting(''app.current_id'')::bigint)
                WITH CHECK (%I = current_setting(''app.current_id'')::bigint);
            ', t, target_column, target_column);
            
            RAISE NOTICE '【RLS有効化】テーブル [%] にカラム [%] によるRLSを適用しました。', t, target_column;
        ELSE
            -- カラムがないテーブルは何もせずスキップ（デフォルトで全行許可のまま）
            RAISE NOTICE '【スキップ】テーブル [%] にはカラム [%] がないため、RLSは無効のままにします。', t, target_column;
        END IF;

    END LOOP;
END $$;
```

## この設計のメリット

* 無駄な処理の削減: カラムがないテーブル（m_categories など）へのアクセス時に、PostgreSQLがRLSのポリシーを評価するステップが省略されるため、クエリのパフォーマンスが低下しません。
* シンプルな管理: データベース内のポリシー数が必要最小限に抑えられるため、後からの確認やメンテナンスが楽になります。

これで無駄のない理想的な一括適用スクリプトになりました。

* 実際に運用するにあたり、この処理を定期的なマイグレーション（CI/CDやデプロイ手順）の一部として組み込む予定はありますか？
* もし必要であれば、最初に検討していたシェルスクリプト（.sh）の形式にこの最新SQLを埋め込んで、コマンドラインから一撃で実行できるように調整することも可能です。ご希望であればお申し付けください！


# 最初に検討していたシェルスクリプト（.sh）の形式にこの最新SQLを埋め込んで、コマンドラインから一撃で実行できるように

承知いたしました。最適化した最新のSQLロジックを埋め込み、コマンドラインから一撃で安全に実行できるシェルスクリプトを作成しました。
このスクリプトは、内部で psql のヒアドキュメント（<< 'EOF'）を使用しており、PostgreSQL側で安全にカラムの有無を判定しながら一括処理を行います。
## RLS一括適用シェルスクリプト (apply_rls_policies.sh)

```bash
#!/bin/bash
# ==========================================
# 1. データベース接続設定
# ==========================================
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="your_database"
DB_USER="your_username"
export PGPASSWORD="your_password" -- パスワードを環境変数に設定
# ==========================================
# 2. RLS設定
# ==========================================
-- 絞り込みを行う共通のカラム名
TARGET_COLUMN="tenant_id"
-- 対象とするテーブル名（スペース区切りで複数指定可能）
TARGET_TABLES=(
    "t_orders"
    "t_invoices"
    "m_products"
    "m_categories"
)
# ==========================================
# 3. 実行用SQLの組み立て
# ==========================================
-- シェルの配列を、PostgreSQLの配列形式（ARRAY['a', 'b']) に変換
TABLES_SQL_ARRAY=$(printf "'%s'," "${TARGET_TABLES[@]}" | sed 's/,$//')

echo "----------------------------------------"
echo " RLS (Row Level Security) 一括設定を開始"
echo "----------------------------------------"
echo "対象カラム: $TARGET_COLUMN"
echo "対象テーブル: ${TARGET_TABLES[*]}"
-- psqlを実行してPL/pgSQLを流し込む
psql -h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME" -U "$DB_USER" -a -e << EOF
DO \$\$
DECLARE
    target_column text := '$TARGET_COLUMN'; 
    tables text[] := ARRAY[$TABLES_SQL_ARRAY];
    t text;
    has_column boolean;
BEGIN
    FOREACH t IN ARRAY tables LOOP
        
        -- 対象テーブルに指定のカラムが存在するか確認
        SELECT EXISTS (
            SELECT 1 
            FROM information_schema.columns 
            WHERE table_schema = 'public'
              AND table_name = t 
              AND column_name = target_column
        ) INTO has_column;
        
        -- カラムが存在するテーブルだけRLSを適用
        IF has_column THEN
            -- RLSを有効化
            EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY;', t);
            
            -- 既存の同名ポリシーがあれば削除（二重実行時のエラー防止）
            EXECUTE format('DROP POLICY IF EXISTS auto_rls_policy ON %I;', t);
            
            -- ポリシーを作成（bigintキャスト対応）
            EXECUTE format('
                CREATE POLICY auto_rls_policy ON %I FOR ALL
                USING (%I = current_setting(''app.current_id'')::bigint)
                WITH CHECK (%I = current_setting(''app.current_id'')::bigint);
            ', t, target_column, target_column);
            
            RAISE NOTICE '【RLS有効化】テーブル [%] にカラム [%] によるRLSを適用しました。', t, target_column;
        ELSE
            -- カラムがないテーブルは何もせずスキップ
            RAISE NOTICE '【スキップ】テーブル [%] にはカラム [%] がないため、RLSは無効のままにします。', t, target_column;
        END IF;
    END LOOP;
END \$\$;
EOF
-- 実行結果のステータス確認
if [ $? -eq 0 ]; then
    echo "----------------------------------------"
    echo " 処理が正常に完了しました。"
    echo "----------------------------------------"
else
    echo "----------------------------------------"
    echo " エラーが発生しました。設定や接続情報を確認してください。"
    echo "----------------------------------------"
fi
```

## 使い方

   1. スクリプトの修正
   1. データベース接続設定 と 2. RLS設定 の項目を、お使いの環境に合わせて書き換えます。
   2. 実行権限の付与
   ターミナルでスクリプトに実行権限を与えます。

   ```bash
   chmod +x apply_rls_policies.sh
   ```

   3. 実行
   コマンドラインから実行します。

   ```bash   
   ./apply_rls_policies.sh
   ```
   
## スクリプトの工夫ポイント

* シェルからSQLへの配列渡し: シェルスクリプト側で定義したテーブル一覧の配列（TARGET_TABLES）を、自動で PostgreSQL の ARRAY['t_orders', 't_invoices'] 形式にパースして SQL に引き渡しています。これにより、テーブルの追加・削除はシェルの配列をいじるだけで完結します。
* 特殊文字のエスケープ: PL/pgSQLのブロック記号である $$ がシェルスクリプトの特殊変数（プロセスID）として誤認されないよう、\$\$ とエスケープを施して安全に実行できるようにしています。

これでコマンドラインから安全かつ一撃で、必要なテーブルにだけRLSを適用できるようになりました。

* 今回作成したポリシー（auto_rls_policy）が一般ユーザーで正しく動作するか（行が絞り込まれるか）をテストする確認用SQLは必要ですか？
* その他、このスクリプトをGitHub ActionsなどのCI/CDに組み込むにあたって不都合な点や、さらに調整したい部分はありますか？


# 一般ユーザーで正しく動作するか（行が絞り込まれるか）をテストする確認用SQLは必要

作成したRLSポリシーが、一般ユーザー（権限を制限したアカウント）で正しく動作するかをテストするための検証用SQLです。
テストを行う際は、スーパーユーザー（postgresなど）のままだとRLSがバイパス（無視）されてしまうため、テスト用の一般ユーザーに切り替えて実行するのがポイントです。

------------------------------
## RLS動作テスト用SQLスクリプト
以下のSQLを順番に、または一括で実行して動作を確認してください。

```sql
-- =====================================================================
-- 事前準備：テスト用ユーザーと検証データの作成（※管理者で実行）
-- =====================================================================
-- 1. テスト用の一般ユーザー「test_app_user」を作成し、権限を付与
DROP USER IF EXISTS test_app_user;
CREATE USER test_app_user WITH PASSWORD 'test_password';
-- テスト対象のテーブル群（例：t_orders）の全権限をテストユーザーに付与
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO test_app_user;
-- 2. 検証データの挿入（tenant_id = 1 と 2 のデータを準備）-- ※すでにデータがある場合は不要です
INSERT INTO t_orders (tenant_id, order_details) VALUES 
(1, 'テナント1の注文データA'),
(1, 'テナント1の注文データB'),
(2, 'テナント2の注文データC');

-- =====================================================================
-- ここからテスト開始（※セッションをテストユーザーに切り替える）
-- =====================================================================
SET ROLE test_app_user;
-- ---------------------------------------------------------------------
-- パターン①：セッション変数が「未設定」の場合の挙動確認
-- ---------------------------------------------------------------------
-- RLSが効いているため、変数をセットしていない状態では「0件（何も見えない）」になれば正常です。
-- （※設定によっては「設定されていません」というエラーになりますが、それも正常な防御反応です）
SELECT '【テスト1: 未設定】' AS test, * FROM t_orders;

-- ---------------------------------------------------------------------
-- パターン②：テナント「1」をセットした場合の挙動確認
-- ---------------------------------------------------------------------
BEGIN; 
-- SET LOCAL を使うためにトランザクションを開始します
-- テナントID = 1 をセット
SET LOCAL app.current_id = '1';
-- 検証SQL：tenant_id = 1 のデータだけ（2件）が取得できれば大成功です！
-- （tenant_id = 2 のデータは、WHERE句に書かなくても自動で除外されます）
SELECT '【テスト2: テナント1】' AS test, * FROM t_orders;
COMMIT;

-- ---------------------------------------------------------------------
-- パターン③：テナント「2」をセットした場合の挙動確認
-- ---------------------------------------------------------------------
BEGIN;
-- テナントID = 2 をセットSET LOCAL app.current_id = '2';
-- 検証SQL：tenant_id = 2 のデータだけ（1件）が取得できれば大成功です！
SELECT '【テスト3: テナント2】' AS test, * FROM t_orders;
COMMIT;

-- ---------------------------------------------------------------------
-- パターン④：不正な書き込みのブロック（INSERT/UPDATE）確認
-- ---------------------------------------------------------------------
BEGIN;
-- 現在はテナント「2」としてログインしていると仮定
SET LOCAL app.current_id = '2';
-- 検証SQL：テナント2のユーザーが、勝手に「テナント1」のデータを保存しようとする
-- 期待される結果：RLSの「WITH CHECK」に引っかかり、【新しく挿入された行が、行セキュリティポリシーに違反しています】というエラーでブロックされれば正常です。
INSERT INTO t_orders (tenant_id, order_details) VALUES (1, '不正なデータ挿入の試み');
ROLLBACK;

-- ---------------------------------------------------------------------
-- 後片付け：テストが終わったら元の管理者ロールに戻す
-- ---------------------------------------------------------------------
RESET ROLE;
```

------------------------------
## テスト実行時のチェックリスト

* SELECTの件数: パターン②、③で、指定した tenant_id 以外のデータが1件も混ざっていないことを確認します。
* エラーの発生: パターン④を実行した際に、SQL自体がエラー（insufficient_privilege / 行セキュリティポリシー違反）で止まり、データが保存されないことを確認します。
* カラムがないテーブルの確認: 共通カラムがないテーブル（例：m_categories）に対しても SELECT を投げ、ユーザーに関係なく全行が取得できることを合わせて確認してください。

このテストSQLでRLSの動作を1席ずつ目視確認できます。

* 動作確認が終わった後、実際にアプリケーション（Rails、Django、Next.js、Goなど）側で 「接続時（またはクエリ発行前）に SET LOCAL app.current_id を自動実行する実装」 について、コードの書き方や注意点で気になる部分はありますか？
* アプリの仕様によっては、Connection Pool（コネクションプール）との兼ね合いで注意すべき点がありますが、アプリケーションの言語やフレームワークは何を使用されていますか？


