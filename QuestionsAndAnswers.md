## Common Questions ##
### Q. Which versions of Eclipse does Scirocco support? ###
A. Scirocco officially supports Eclipse 3.6 (Helios). If your Eclipse version is not latest, we recommend you to update your eclipse.
<br>
<h3>Q. Which versions of Android does Scirocco support?</h3>
A. Scirocco officially supports Android 1.6 and up.<br>
<br>
<br>

<h2>Troubleshooting</h2>
<h3>Q. Why "(Name of button) is not found!" error occur?</h3>
A. Name of widgets which you want to test must be defined by string.xml file.<br>
<br>
e.g. Testing Create button<br>
<br>
string.xml<br>
<pre><code> &lt;string name="CreateButton"&gt;Create&lt;/string&gt;<br>
</code></pre>
<br>
layout.xml<br>
<pre><code> &lt;Button<br>
   android:text="@string/CreateButton"<br>
   ・・・<br>
   ・・・&gt;<br>
 &lt;/Button&gt;<br>
</code></pre>