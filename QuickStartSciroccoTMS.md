# Quick Start SciroccoTMS #
Scirocco is a web-based test management system which is made by RubyOnRails.

<br>
<br>
<h1>Needed Environment</h1>
<h3>OS</h3>
SciroccoTMS can run on Windows, UNIX, Linux etc. as long as it is able to run Ruby.<br>
<br>
<h3>Ruby and Ruby on Rails</h3>
Needed Ruby and Rails version are as below.<br>
<table><thead><th> <b>Ruby</b> </th><th> <b>Rails</b> </th></thead><tbody>
<tr><td> 1.8.6 </td><td> 2.3.5 </td></tr></tbody></table>

<h3>Database</h3>
sqlite 3<br>
<br>
<br>
<br>
<h1>How to install SciroccoTMS</h1>
<h2>(Mac OS X)</h2>


<h3>install RVM</h3>
(RVM is a command-line tool to manage multiple Ruby versions.)<br>
<pre><code>mkdir -p ~/.rvm/src<br>
cd .rvm/src/<br>
git clone git://github.com/wayneeseguin/rvm.git<br>
cd rvm/<br>
./install<br>
vim ~/.bashrc<br>
  (add following messages at the end of .bashrc file<br>
  # This loads RVM into a shell session.<br>
  [[ -s "$HOME/.rvm/scripts/rvm" ]] &amp;&amp; . "$HOME/.rvm/scripts/rvm"<br>
source ~/.bashrc<br>
</code></pre>
<h3>install ruby1.8.6 and rails2.3.5</h3>
<pre><code>rvm install 1.8.6<br>
rvm use 1.8.6 --default<br>
rvm gemset create rails2<br>
rvm use 1.8.6@rails2 --default<br>
gem install rails -v2.3.5<br>
</code></pre>
<h3>install necessary Libraries</h3>
<pre><code>gem install sqlite3-ruby -v 1.2.5<br>
gem install zipruby<br>
gem install hpricot<br>
</code></pre>
<h3>install SciroccoTMS</h3>
<pre><code>svn checkout http://scirocco.googlecode.com/svn/trunk/scirocco_tms scirocco_tms<br>
</code></pre>
<h3>CHECK</h3>
<pre><code>cd scirocco_tms<br>
ruby script/server<br>
</code></pre>

<ol><li>Start WEBrick<br>
</li><li>Then access <a href='http://localhost:3000/'>http://localhost:3000/</a> by browser.<br>
</li><li>If you can access successfully TOP PAGE of sciroccoTMS</li></ol>


<h3>SciroccoTMS URL setting</h3>
<ol><li>Start Eclipse<br>
</li><li>Select <code>[Eclipse]</code> from menu bar<br>
</li><li>Click Preferences from pull-down menu<br>
</li><li>Click scirocco<br>
</li><li>Enter <a href='http://localhost:3000'>http://localhost:3000</a> in sciorcco TMS URL field<br>
</li><li>Click OK button</li></ol>

<h3>Finishing installation</h3>
<ol><li>access <a href='http://localhost:3000/'>http://localhost:3000/</a> by browser<br>
</li><li>If sciroccoTMS is running properly, project name which you tested is there.