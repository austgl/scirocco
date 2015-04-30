## よくある質問 ##
### Q. SciroccoがサポートしているOSは？ ###
A. SciroccoはEclipseのプラグインとなっております。Eclipseを実行可能であればOSを問わず導入する事が出来ます。

### Q. SciroccoがサポートしているEclipseのversionは？ ###
A. SciroccoはEclipse 3.6 (Helios)に対応しています。 もしお使いのPCに入っているEclipseが最新で無い場合はアップデートをお勧めします。(2011/05 現在)

### Q. SciroccoがサポートしているAndroidのversionは？ ###
A. SciroccoはAndroid 1.6以降の機種に対応しています。

### Q. Sciroccoを使用する際にかかる費用はどのくらいですか？ ###
A. Sciroccoはオープンソースソフトウェア(Apache 2.0)ですので無料でご利用頂けます。

### Q. テストコードはどのように記述するのですか？ ###
A. テストコードの書き方につきましては最新の[robotium-solo-javedoc](http://code.google.com/p/robotium/downloads/list) ファイルを参照して下さい。

### 二つ以上のアプリケーションにまたがるテストは可能ですか？ ###
A. robotium側で以下のように説明されています<br>
<h4>(原文)</h4>
No, that is not possible. In the <code>AndroidManifest.xml</code> you state which target application you want to test. An example of what it can look like: <br>
<pre><code>&lt;instrumentation android:targetPackage="com.example.android.notepad" android:name="android.test.InstrumentationTestRunner" /&gt;<br>
</code></pre>
That means that the test project is locked to the targetPackage. Going outside of that target package is not allowed by the Android platform. Therefore you will need 2 test projects, one for each application. <br>
<br>
<h4>(訳文)</h4>
出来ません。<code>AndroidManifest.xml</code>にテスト対象のアプリケーションを以下のように記述する様になっているからです。 <br>
<pre><code>&lt;instrumentation android:targetPackage="com.example.android.notepad" android:name="android.test.InstrumentationTestRunner" /&gt;<br>
</code></pre>
テストプロジェクトは一つのターゲットパッケージを指定する構造になっており、その外側(別のパッケージ)へのアクセスは許可されないという事です。そのためアプリケーションの数だけテストプロジェクトを作成する必要があります。 <br>

<h3>Q. apkファイルだけでテストの実行は可能ですか？</h3>
A. 出来ます。やり方につきましては、以下のリンクを参照して下さい。<br>
<a href='http://code.google.com/p/robotium/wiki/RobotiumForAPKFiles'>apkファイルのみでテストする方法(英語)</a>

<h3>Q. プリインストールアプリのテストは出来ますか？</h3>
A. 可能ですが、端末のルートを取る必要があります。<b>しかし、ルート化及びルート権限を利用した操作によって端末が起動しなくなる等の不具合が発生する事があり、また保証の対象外となりますのでメーカーのサポートも受けられなくなります。実行される場合は自己責任でお願いします</b>

<a href='http://code.google.com/p/robotium/wiki/RobotiumForPreInstalledApps'>プリインストールアプリのテスト方法(英語)</a>

<h2>操作・トラブル</h2>
<h3>Q.solo.clickOnButton()メソッドの引数に、ボタン名を渡したときに、「(ボタン名) is not found!」エラーが発生する</h3>
A.ボタン名はstring.xmlで定義してある必要があります。例えば、登録ボタンをタップするメソッド「solo.clickOnButton("登録");」メソッドを呼ぶには、以下のようにxmlが定義されている必要があります。<br>
<br>
string.xml<br>
<pre><code> &lt;string name="CreateButton"&gt;登録&lt;/string&gt;<br>
</code></pre>
<br>
layout.xml<br>
<pre><code> &lt;Button<br>
   android:text="@string/CreateButton"<br>
   ・・・(省略)・・・&gt;<br>
 &lt;/Button&gt;<br>
</code></pre>