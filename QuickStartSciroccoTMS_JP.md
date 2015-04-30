# Quick Start sciroccoTMS #
SsciroccoTMSはRubyOnRailsで実装されたWebベースのテスト管理システムです.

このガイドでは、SciroccoTMSを使ってSciroccoのテスト報告書をWeb上で共有する方法を説明します.
<br>
<br>
<h1>必要な環境</h1>
<h3>OS</h3>
SciroccoTMSはたいていのUNIX, Linux, Mac, Windowsシステム等、Rubyが利用可能なシステムで実行できます.<br>
<br>
<h3>RubyおよびRuby on rails</h3>
SciroccoTMSで必要となるRubyとRailsのバージョンは以下の通りです.<br>
<br>
<table><thead><th> <b>Ruby</b> </th><th> <b>Rails</b> </th></thead><tbody>
<tr><td> 1.8.6 </td><td> 2.3.5 </td></tr></tbody></table>

<h3>データベース</h3>
sqlite 3<br>
<br>
<h1>インストール</h1>
Mac OS XにSciroccoTMSをインストールする手順を説明します。<br>
<br>
<h3>rvmインストール</h3>
(RVMはUNIX系の環境で，複数のRuby処理系をインストール，共存させることができるツールです)<br>
<br>
<pre><code>mkdir -p ~/.rvm/src<br>
cd .rvm/src/<br>
git clone git://github.com/wayneeseguin/rvm.git<br>
cd rvm/<br>
./install<br>
vim ~/.bashrc<br>
  (.bashrcファイルの末尾に以下を記述)<br>
  # This loads RVM into a shell session.<br>
  [[ -s "$HOME/.rvm/scripts/rvm" ]] &amp;&amp; . "$HOME/.rvm/scripts/rvm"<br>
source ~/.bashrc<br>
</code></pre>
<h3>ruby1.8.6 と rails2.3.5をインストール</h3>
<pre><code>rvm install 1.8.6<br>
rvm use 1.8.6 --default<br>
rvm gemset create rails2<br>
rvm use 1.8.6@rails2 --default<br>
gem install rails -v2.3.5<br>
</code></pre>

<h3>必要なライブラリをインストール</h3>
<pre><code>gem install sqlite3-ruby -v 1.2.5<br>
gem install zipruby<br>
gem install hpricot<br>
</code></pre>
<h3>SciroccoTMSをインストール</h3>
<pre><code>svn checkout http://scirocco.googlecode.com/svn/trunk/scirocco_tms scirocco_tms<br>
</code></pre>
<h3>WEBrickのwebサーバを起動し、動作確認</h3>
<pre><code>cd scirocco_tms<br>
ruby script/server<br>
</code></pre>
WEBrickが起動したら、ブラウザで <a href='http://localhost:3000/'>http://localhost:3000/</a> を開いてください。SciroccoTMSのトップページが表示されるはずです。<br>
<br>
注意: Webrickは通常は開発時に使用するものであり、通常の運用には適していません。動作確認以外には使用しないでください。本番運用においてはPassenger (mod_rails) の利用を検討してください。<br>
<br>
<h3>EclipseでSciroccoTMSのURLを設定</h3>
Eclipseを起動し、メニューバーから Eclipce > 環境設定　> Scirocco sciorcco TMS URLに<a href='http://localhost:3000を指定してOKボタンをクリック'>http://localhost:3000を指定してOKボタンをクリック</a>

<h3>Sciroccoでテストを実行し、SciroccoTMSにデータが登録される事を確認</h3>
ブラウザで <a href='http://localhost:3000/'>http://localhost:3000/</a> を開いてください。SciroccoTMSのトップページに、テストプロジェクトの名前が表示されるはずです。