# Quick Start Scirocco #

Way to use of Scirocco is as below.
<br>
<br>
<h1>Installing the Scirocco plugin</h1>

<ol><li>Start Eclipse3.6. <code>*1)</code>
</li><li>Select <code>[Help]</code> tab from menu bar.<br>
</li><li>Click <code>[Install New Software]</code> from pull-down menu.<br>
</li><li>Click <code>[Add...]</code> button at the right of 'Work with' field.<br>
</li><li>Enter 'Name' and 'Location' as table below.<br>
</li><li>Check 'Select All' and then click <code>[Next]</code> button.<br>
</li><li>Make sure Install details list have <code>SciroccoFeature</code>, and then click <code>[Next]</code> button.<br>
</li><li>Check 'I accept the terms of the license agreement' and click 'Finish' button, install will start. <code>*2)</code><code>*3)</code>
<table><thead><th> <b>Name:</b> </th><th> Scirocco </th></thead><tbody>
<tr><td> <b>Location:</b> </td><td> <a href='http://184.73.200.19/android/eclipse'>http://184.73.200.19/android/eclipse</a> </td></tr></li></ol></tbody></table>

<code>*1)</code> If Eclipse is not installed in your computer, access <a href='http://www.eclipse.org/helios/'>http://www.eclipse.org/helios/</a> and install Eclipse 3.6(Helios). <br>
<code>*2)</code> If pop-up dialog labeled 'Security Warning' is displayed, click 'ok' button. <br>
<code>*3)</code> If pop-up dialog labeled 'Software Updates' is displayed, click 'Restart Now' button. <br>
<h2>Scirocco Settings</h2>
<ol><li>Click <code>[Eclipse]</code> tab from menu bar, and then select<code>[Settings]</code> > <code>[Scirocco]</code>.<br>
</li><li>Set Android SDK Location and then Click '<a href='OK.md'>OK</a>' button.</li></ol>

<br>
<br>

<h1>Sample Application and Sample Test Code</h1>
<h2>Download sample application and test code</h2>
<ol><li>Access <a href='http://code.google.com/p/scirocco/downloads/list'>http://code.google.com/p/scirocco/downloads/list</a> by your web browser.<br>
</li><li>Download the <code>ExampleApplicationProject_v1.0.zip</code>and  to your computer with a web browser and unzip the package.<br>
</li><li>Download the <code>ExampleTestProject_v1.0.zip</code>, just do likewise.<br>
</li><li>After unzip it move them to your 'workspace' folder.<br>
<br></li></ol>

<h2>How to import</h2>
<ol><li>Start Eclipse.<br>
</li><li>Click <code>[File]</code> from menu bar.<br>
</li><li>Click <code>[import]</code>.<br>
</li><li>Select <code>[Existing Project into Workspace]</code>and then go<code>[Next]</code>.<br>
</li><li>Click <code>[Browse]</code> button at the right of the 'Select achieve file' field.<br>
</li><li>
</li><li>Import <code>ExampleApplicationProject_v1.0.zip</code>is the same way.<br>
<h2>Prepare Sample Application</h2>
Open an <code>AndroidManufest.xml</code> file of target application and make sure following permissions are set. It is necessary to capture screenshots.<br>
<pre><code>	&lt;uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" /&gt;<br>
</code></pre></li></ol>

<br>
<br>
<h2>Prepare Sample Test Code</h2>
Make sure your test project refers following jar files. It is necessary to perform test by scirocco.<br>
<ul><li>robotium-solo-2.3-javadoc.jar<br>
</li><li>robotium-solo-2.3.jar<br>
</li><li>scirocco.jar<br>
<br></li></ul>

<h2>Run Scirocco JUnit Test</h2>
Scirocco JUnit Test is able to run on android devices or emulator on your computer.<br>
<br>
<br>
<br>
<ol><li>Right click target test class.<br>
</li><li>Click <code>[Run As]</code> > <code>[Scirocco JUnit Test]</code> from menu and then Scirocco Unit Test will run.<br>
During the test, Scirocco work automatically.<br>
If Scirocco does not work, please return installing section and reinstall it.</li></ol>

Scirocco automatically do several works such as below.<br>
<ul><li>Running Test<br>
</li><li>Taking Screenshot<br>
</li><li>Making Test Report with result</li></ul>

When test finished, Scirocco create 'scirocco' folder in your test project. Your test report is stored in scirocco folder.<br>
<br>
<br>

<h1>How to share your test reports?</h1>
QuickStartSciroccoTMS is a test management system that provides easy share test reports with your project member.<br>
Please go to the below referenced web link for usage of Scirocco TMS. <br>
<a href='http://code.google.com/p/scirocco/wiki/QuickStartSciroccoTMS'>QuickStartSciroccoTMS</a>