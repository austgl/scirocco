/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ide.eclipse.scirocco.internal.launch.junit;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.swt.widgets.Display;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ide.eclipse.scirocco.AdtPlugin;
import com.android.ide.eclipse.scirocco.internal.launch.DelayedLaunchInfo;
import com.android.ide.eclipse.scirocco.internal.launch.IAndroidLaunchAction;
import com.android.ide.eclipse.scirocco.internal.launch.LaunchMessages;
import com.android.ide.eclipse.scirocco.internal.launch.junit.runtime.AndroidJUnitLaunchInfo;
import com.android.ide.eclipse.scirocco.internal.launch.junit.runtime.RemoteAdtTestRunner;

/**
 * A launch action that executes a instrumentation test run on an Android device.
 */
class AndroidJUnitLaunchAction implements IAndroidLaunchAction {

    private final AndroidJUnitLaunchInfo mLaunchInfo;
    
    /**
     * Creates a AndroidJUnitLaunchAction.
     * 
     * @param launchInfo the {@link AndroidJUnitLaunchInfo} for the JUnit run 
     */
    public AndroidJUnitLaunchAction(AndroidJUnitLaunchInfo launchInfo) {
        mLaunchInfo = launchInfo;
    }
    
    /**
     * Launch a instrumentation test run on given Android device. 
     * Reuses JDT JUnit launch delegate so results can be communicated back to JDT JUnit UI.
     * <p/>
     * Note: Must be executed on non-UI thread.
     * 
     * @see IAndroidLaunchAction#doLaunchAction(DelayedLaunchInfo, IDevice)
     */
    public boolean doLaunchAction(DelayedLaunchInfo info, IDevice device) {
        String msg = String.format(LaunchMessages.AndroidJUnitLaunchAction_LaunchInstr_2s,
                mLaunchInfo.getRunner(), device.getSerialNumber());
        AdtPlugin.printToConsole(info.getProject(), msg);
        
        try {
           mLaunchInfo.setDebugMode(info.isDebugMode());
           mLaunchInfo.setDevice(info.getDevice());
           JUnitLaunchDelegate junitDelegate = new JUnitLaunchDelegate(mLaunchInfo);
           final String mode = info.isDebugMode() ? ILaunchManager.DEBUG_MODE : 
               ILaunchManager.RUN_MODE; 

           junitDelegate.launch(info.getLaunch().getLaunchConfiguration(), mode, info.getLaunch(),
                   info.getMonitor());

           // TODO: need to add AMReceiver-type functionality somewhere
        } catch (CoreException e) {
            AdtPlugin.printErrorToConsole(info.getProject(),
                    LaunchMessages.AndroidJUnitLaunchAction_LaunchFail);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public String getLaunchDescription() {
        return String.format(LaunchMessages.AndroidJUnitLaunchAction_LaunchDesc_s,
                mLaunchInfo.getRunner());
    }

    /**
     * Extends the JDT JUnit launch delegate to allow for JUnit UI reuse. 
     */
    private static class JUnitLaunchDelegate extends JUnitLaunchConfigurationDelegate {
        
        private AndroidJUnitLaunchInfo mLaunchInfo;

        public JUnitLaunchDelegate(AndroidJUnitLaunchInfo launchInfo) {
            mLaunchInfo = launchInfo;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
         */
        @Override
        public synchronized void launch(ILaunchConfiguration configuration, String mode,
                ILaunch launch, IProgressMonitor monitor) throws CoreException {
            // TODO: is progress monitor adjustment needed here?
            super.launch(configuration, mode, launch, monitor);
        }

        /**
         * {@inheritDoc}
         * @see org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate#verifyMainTypeName(org.eclipse.debug.core.ILaunchConfiguration)
         */
        @Override
        public String verifyMainTypeName(ILaunchConfiguration configuration) {
            return "com.android.ide.eclipse.scirocco.junit.internal.runner.RemoteAndroidTestRunner"; //$NON-NLS-1$
        }

        /**
         * Overrides parent to return a VM Runner implementation which launches a thread, rather
         * than a separate VM process
         */
        @Override
        public IVMRunner getVMRunner(ILaunchConfiguration configuration, String mode) {
            return new VMTestRunner(mLaunchInfo);
        }

        /**
         * {@inheritDoc}
         * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate#getLaunch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
         */
        @Override
        public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) {
            return mLaunchInfo.getLaunch();
        }     
    }

    /**
     * Provides a VM runner implementation which starts a inline implementation of a launch process
     */
    private static class VMTestRunner implements IVMRunner {
        
        private final AndroidJUnitLaunchInfo mJUnitInfo;
        
        VMTestRunner(AndroidJUnitLaunchInfo info) {
            mJUnitInfo = info;
        }

        /**
         * {@inheritDoc}
         * @throws CoreException
         */
        public void run(final VMRunnerConfiguration config, ILaunch launch,
                IProgressMonitor monitor) throws CoreException {
            
            TestRunnerProcess runnerProcess = 
                new TestRunnerProcess(config, mJUnitInfo);
            launch.addProcess(runnerProcess);
            runnerProcess.run();
        }
    }

    /**
     * Launch process that executes the tests.
     */
    private static class TestRunnerProcess implements IProcess  {

        private final VMRunnerConfiguration mRunConfig;
        private final AndroidJUnitLaunchInfo mJUnitInfo;
        private RemoteAdtTestRunner mTestRunner = null;
        private boolean mIsTerminated = false;
        
        TestRunnerProcess(VMRunnerConfiguration runConfig, AndroidJUnitLaunchInfo info) {
            mRunConfig = runConfig;
            mJUnitInfo = info;
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.debug.core.model.IProcess#getAttribute(java.lang.String)
         */
        public String getAttribute(String key) {
            return null;
        }

        /**
         * {@inheritDoc}
         * @see org.eclipse.debug.core.model.IProcess#getExitValue()
         */
        public int getExitValue() {
            return 0;
        }

        /* (non-Javadoc)
         * @see org.eclipse.debug.core.model.IProcess#getLabel()
         */
        public String getLabel() {
            return mJUnitInfo.getLaunch().getLaunchMode();
        }

        /* (non-Javadoc)
         * @see org.eclipse.debug.core.model.IProcess#getLaunch()
         */
        public ILaunch getLaunch() {
            return mJUnitInfo.getLaunch();
        }

        /* (non-Javadoc)
         * @see org.eclipse.debug.core.model.IProcess#getStreamsProxy()
         */
        public IStreamsProxy getStreamsProxy() {
            return null;
        }

        /* (non-Javadoc)
         * @see org.eclipse.debug.core.model.IProcess#setAttribute(java.lang.String, 
         * java.lang.String)
         */
        public void setAttribute(String key, String value) {
            // ignore           
        }

        /* (non-Javadoc)
         * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
         */
        @SuppressWarnings("unchecked")
        public Object getAdapter(Class adapter) {
            return null;
        }

        /* (non-Javadoc)
         * @see org.eclipse.debug.core.model.ITerminate#canTerminate()
         */
        public boolean canTerminate() {
            return true;
        }

        /* (non-Javadoc)
         * @see org.eclipse.debug.core.model.ITerminate#isTerminated()
         */
        public boolean isTerminated() {
            return mIsTerminated;
        }

        /**
         * {@inheritDoc}
         * @see org.eclipse.debug.core.model.ITerminate#terminate()
         */
        public void terminate() {
            if (mTestRunner != null) {
                mTestRunner.terminate();
            }    
            mIsTerminated = true;
        } 

        
        //テスト結果格納用のディレクトリ作成
		private void createSciroccoDir() throws CoreException {
			IProject project = mJUnitInfo.getProject();
			IFolder sciroccoTestResultFolder = project.getFolder(new Path("scirocco"));
			if (!sciroccoTestResultFolder.exists()){
				sciroccoTestResultFolder.create(true, true, null);
			}
		}
			
	private static final String IMAGES_DIRECTORY = "images/";
	private static final String IMAGE_SCIROCCO_LOGO = "scirocco_logo_small.png";
	private static final String IMAGE_BG = "bg.gif";
	/**
	 * Adds Image files to the project.
	 * 
	 * @param project
	 *            The Java Project to update.
	 * @param monitor
	 *            An existing monitor.
	 * @throws CoreException
	 *             if the method fails to update the project.
	 */
	private void addImages() throws CoreException {
		IProject project = mJUnitInfo.getProject();
		IFolder sciroccoTestResultFolder = project.getFolder(new Path(SCIROCCO_TEST_RESULT_FOLDER));
		IFolder imagesFolder = sciroccoTestResultFolder.getFolder(new Path(IMAGES_DIRECTORY));
		if (!imagesFolder.exists()) {
			// imagesディレクトリを作成
			imagesFolder.create(false, true, null);
		}

		// do all 4 css files.
		IFile file;
		file = imagesFolder.getFile(IMAGE_BG);
		if (!file.exists()) {
			addFile(file, AdtPlugin.readEmbeddedFile(IMAGES_DIRECTORY + IMAGE_BG));
		}

		file = imagesFolder.getFile(IMAGE_SCIROCCO_LOGO);
		if (!file.exists()) {
			addFile(file, AdtPlugin.readEmbeddedFile(IMAGES_DIRECTORY + IMAGE_SCIROCCO_LOGO));
		}
	}
	
	
		private static final String CSS_960 = "960.css";
		private static final String CSS_KEEP_IT_SIMPLE = "KeepItSimple.css";
		private static final String CSS_RESET = "reset.css";
		private static final String CSS_SCREEN = "screen.css";
		private static final String SCIROCCO_TEST_RESULT_FOLDER = "scirocco";
		private static final String CSS_DIRECTORY = "css/";
	/**
	 * Adds CSS files to the project.
	 * 
	 * @param project
	 *            The Java Project to update.
	 * @param monitor
	 *            An existing monitor.
	 * @throws CoreException
	 *             if the method fails to update the project.
	 */
	private void addCSS() throws CoreException {
		IProject project = mJUnitInfo.getProject();
		IFolder sciroccoTestResultFolder = project.getFolder(new Path(SCIROCCO_TEST_RESULT_FOLDER));
		IFolder cssFolder = sciroccoTestResultFolder.getFolder(new Path(CSS_DIRECTORY));
		if (!cssFolder.exists()) {
			// CSSディレクトリを作成
			cssFolder.create(false, true, null);
		}

		// do all 4 css files.
		IFile file;
		file = cssFolder.getFile(CSS_960);
		if (!file.exists()) {
			addFile(file, AdtPlugin.readEmbeddedFile(CSS_DIRECTORY + CSS_960));
		}

		file = cssFolder.getFile(CSS_KEEP_IT_SIMPLE);
		if (!file.exists()) {
			addFile(file, AdtPlugin.readEmbeddedFile(CSS_DIRECTORY + CSS_KEEP_IT_SIMPLE));
		}

		file = cssFolder.getFile(CSS_RESET);
		if (!file.exists()) {
			addFile(file, AdtPlugin.readEmbeddedFile(CSS_DIRECTORY + CSS_RESET));
		}

		file = cssFolder.getFile(CSS_SCREEN);
		if (!file.exists()) {
			addFile(file, AdtPlugin.readEmbeddedFile(CSS_DIRECTORY + CSS_SCREEN));
		}
	}

	    /**
	     * Creates a file from a data source.
	     * @param dest the file to write
	     * @param source the content of the file.
	     * @param monitor the progress monitor
	     * @throws CoreException
	     */
	    private void addFile(IFile dest, byte[] source) throws CoreException {
	        if (source != null) {
	            // Save in the project
	            InputStream stream = new ByteArrayInputStream(source);
	            dest.create(stream, false /* force */,null);
	        }
	    }
	    
        /**
         * Launches a test runner that will communicate results back to JDT JUnit UI.
         * <p/>
         * Must be executed on a non-UI thread.
         */
        public void run() {
            if (Display.getCurrent() != null) {
                AdtPlugin.log(IStatus.ERROR, "Adt test runner executed on UI thread");
                AdtPlugin.printErrorToConsole(mJUnitInfo.getProject(),
                        "Test launch failed due to internal error: Running tests on UI thread");
                terminate();
                return;
            }
            
            try {
				createSciroccoDir();
				addCSS();
				addImages();
				//TODO addImage
			} catch (CoreException e) {
				e.printStackTrace();
			}
            
//			//TODO 全機種を選択した場合
			if (AdtPlugin.hasSelectedAllDevices()) {
				IDevice[] devices = AndroidDebugBridge.getBridge().getDevices();
//				
				//TODO フラグが立っていたら、削除して再インストールする
				for (IDevice device : devices) {
					mTestRunner = new RemoteAdtTestRunner();
					mJUnitInfo.setDevice(device);
					mTestRunner.runTests(mRunConfig.getProgramArguments(),
							mJUnitInfo);
				}
//			//TODO 一機種のみ選択した場合
			} else {
				RemoteAdtTestRunner testRunner = new RemoteAdtTestRunner();
				testRunner.runTests(mRunConfig.getProgramArguments(), mJUnitInfo);
			}
        }
    }
}

