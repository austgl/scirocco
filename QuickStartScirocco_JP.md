# クイックスタート scirocco #

このガイドでは、Sciroccoを使ってandroidアプリケーションのUIテストを自動化する方法を説明します.
<br>
<br>

<h1>Scirocco Plug-in のインストール</h1>

実際にEclipseにSciroccoを導入する手順は以下のようになります.<br>
<ol><li>Eclipse3.6 を起動 <code>*1)</code>
</li><li>メニューバーから<code>[ヘルプ]</code>を選択<br>
</li><li><code>[Install New Software]</code> をクリック<br>
</li><li>Work with 欄の右にある <code>[Add...]</code> ボタンをクリック<br>
</li><li>それぞれ下のテーブルのように入力してOKボタンをクリック<br>
</li><li>Select Allを選び、<code>[Next]</code> をクリック<br>
</li><li>リストに<code>SciroccoFeature</code>が有る事を確認して<code>[Next]</code> をクリック<br>
</li><li>I accept the terms of the license agreement を選んで Finish を押すとインストールが開始 <code>*2)</code><code>*3)</code>
<br>
<table><thead><th> <b>Name:</b> </th><th> Scirocco </th></thead><tbody>
<tr><td> <b>Location:</b> </td><td> <a href='http://184.73.200.19/android/eclipse'>http://184.73.200.19/android/eclipse</a> </td></tr></li></ol></tbody></table>

<code>*1</code> もしEclipseがまだインストールされていない場合、<a href='http://www.eclipse.org/helios/'>http://www.eclipse.org/helios/</a>へアクセスし、 Eclipse 3.6 (Helios) をインストールしてからSciroccoの導入作業を行って下さい. <br>
<code>*2</code> もしSecurity Warningのダイアログが表示された場合はOKを選択してインストール作業を続行して下さい下さい. <br>
<code>*3</code> もしSoftware Updatesのダイアログが表示された場合はRestart Nowを選択して、Eclipseを再起動して下さい.  <br>


<h2>Sciroccoの設定</h2>
<ol><li>Eclipseメニューの<code>[環境設定]</code> > <code>[Scirocco]</code> を選択し、Android SDK Locationを指定してOKをクリック<br>
Scirocco TMS URLの設定についてはSciroccoTMS導入時に行います.詳しくは<a href='http://code.google.com/p/scirocco/wiki/QuickStartSciroccoTMS_JP'>QuickStartSciroccoTMS_JP</a>を参照して下さい.<br>
<br>
<br></li></ol>

<h1>サンプルアプリを使った動作確認</h1>
<h2>サンプルアプリとサンプルテストコードのダウンロード</h2>
<ol><li>ブラウザから<a href='http://code.google.com/p/scirocco/downloads/list'>http://code.google.com/p/scirocco/downloads/list</a>へアクセス<br>
</li><li>リストから<code>ExampleApplicationProject_v1.0.zip</code>をダウンロードしてworkspaceへ置く<br>
</li><li><code>ExampleTestProject_v1.0.zip</code>も同様<br>
<br>
<h2>ファイルのインポート</h2>
</li><li>Eclipseを開く<br>
</li><li>メニューバーから<code>[File]</code>を選択<br>
</li><li><code>[import]</code>をクリック<br>
</li><li><code>[Existing Project into Workspace]</code>を選択して<code>[Next]</code> をクリック<br>
</li><li>Select achieve file欄の右にある<code>[Browse]</code> ボタンをクリック<br>
</li><li><code>ExampleApplicationProject_v1.0.zip</code>も同様<br>
<h4>注意</h4>
Mac 環境の場合はDLした際にファイルが自動解凍されている場合が有ります。その場合は手順5,6において、Select achieve fileではなくSelect root directoryを選んで、対象のファイルを<code>[import]</code>して下さい。<br>
<br></li></ol>

<h2>サンプルアプリの設定確認</h2>
アプリケーションプロジェクトの<code>AndroidManifest.xml</code>を開き、以下のパーミッションが設定されている事を確認してください。この設定はテスト時にスクリーンショットを取得する場合に必要です。<br>
<pre><code>	&lt;uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" /&gt;<br>
</code></pre>

<h2>サンプルテストコードの設定確認</h2>
<code>ExampleTestProject</code>のlibフォルダ内に以下のjarファイルが有る事を確認して下さい。<br>
<ul><li><code>robotium-solo-2.3-javadoc.jar</code>
</li><li><code>robotium-solo-2.3.jar</code>
</li><li><code>scirocco.jar</code>
<code>*</code>これらのファイルは<code>ExampleApplicationProject</code>には有りません。<br>
<br></li></ul>

<h2>Scirocco JUnit Test の実行</h2>
Scirocco によるテストの実行方法です。Scirocco JUnit Test は実機でもエミュレーターでも実行する事が出来ます。<br>
<br>
実行方法は以下のようになります。<br>
<ol><li>対象のテストクラスを右クリック<br>
</li><li>メニューの中から<code>[Run As]</code> > <code>[Scirocco JUnit Test]</code>
以上の手順だけでScirocco によるテストが開始します。<br>
テスト中は操作が自動で行われます。</li></ol>

テストの実行が終わると、テストプロジェクト内にsciroccoフォルダが作成されます。テスト結果が記述されたテスト報告書は、sciroccoフォルダ内に格納されています。<br>
<br>
<br>

<h1>テスト報告書をプロジェクトメンバーと共有する方法</h1>
テスト報告書をプロジェクトメンバーと共有したい場合は、scirocco TMSを使ってください。<br>
sciroccoTMSはテスト報告書をWeb上で共有するためのテスト管理システムです。<br>
設定方法は下記のリンク先を参照して下さい。<br>
<a href='http://code.google.com/p/scirocco/wiki/QuickStartSciroccoTMS_JP'>QuickStartSciroccoTMS</a>