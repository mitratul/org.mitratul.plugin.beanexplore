package org.mitratul.plugin.beanexplore.core;

import java.io.File;
import java.util.Iterator;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.mitratul.plugin.beanexplore.handlers.BeanExploreActivator;

public class JdtHelper {
	private static final String FILE_EXTENTION_JAR = ".jar";

	private BeanIndex mIndex;

	public JdtHelper() {
		mIndex = new BeanIndex();
	}

	/**
	 * Index all the projects with java nature (JavaCore.NATURE_ID) in the workspace.
	 */
	public void indexWorkspace() {
		//* get all the projects in the workspace
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			//* If it is of java nature, index it.
			try {
				if (project.hasNature(JavaCore.NATURE_ID)) {
					indexProject(JavaCore.create(project));
				} //* if project is not of java nature, skip it.
			} catch (CoreException e) {
				//* If the project doesn't or it is closed, print an warning, 
				//  and proceed with the rest.
				System.err.printf("Indexing failed for project %s. " +
						"It is either closed or doesn't exist.", project.getName());
			}
		}
	}

	public void indexSelectedProject(IStructuredSelection selections) 
			throws ExecutionException {

		Iterator<?> selectionIterator = selections.iterator();
		while (selectionIterator.hasNext()) {
			Object selection = selectionIterator.next();
			try {
				if (selection instanceof IJavaProject) {
					indexProject((IJavaProject) selection);
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}

	private void indexProject(IJavaProject pJavaProject) throws JavaModelException {
		IClasspathEntry[] classpathEntries = pJavaProject.getRawClasspath();
		System.out.println("\n\n\n");

		for (IClasspathEntry classpathEntry : classpathEntries) {
			System.out.println(classpathEntry.getPath());
			if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY 
					&& classpathEntry.getPath().toString().endsWith(FILE_EXTENTION_JAR)) {

				mIndex.indexJar(getAbsolutePath(pJavaProject, classpathEntry));

			} //* The src and other entries (CPE_LIBRARY) or class dirs are out of scope
			//* TODO: think about dependent projects
		}
	}

	private File getAbsolutePath(IJavaProject javaProject,
			IClasspathEntry classpathEntry) {
		File absoluteFile = null;
		if (classpathEntry.getPath().toString().startsWith("/")) {
			absoluteFile = javaProject.getProject().getFile(
					classpathEntry.getPath().lastSegment()
					).getLocation().toFile();
		} else {
			absoluteFile = classpathEntry.getPath().toFile();
		}

		return absoluteFile;
	}

	public void displayDefinitionXml(String aBeanId) {
		String xmlPath = mIndex.getXmlLocation(aBeanId);

		String extractedXmlPath = null;
		if (xmlPath != null) {
			//* If bean ID is found, extract the definition XML.
			extractedXmlPath = BeanExploreActivator.getIOHelper().extractXml(
					mIndex.decodeJarXmlPath(xmlPath)[0], 
					mIndex.decodeJarXmlPath(xmlPath)[1]);
		} else {
			//* If bean ID not found, display error.
			MessageDialog.openInformation(
					null,
					"Search for bean definition",
					"No bean definition XML found for bean ID " + aBeanId);
		}

		if (extractedXmlPath != null) {
			//* If xml extraction successful, open it in editor
			openXmlInEditor(extractedXmlPath);
		} else {
			//* If xml extraction fails, display error
			MessageDialog.openInformation(
					null,
					"Search for bean definition",
					"Could not extract " + mIndex.decodeJarXmlPath(xmlPath)[1] 
							+ " from " + mIndex.decodeJarXmlPath(xmlPath)[0]);
		}
	}

	public void openXmlInEditor(String pXmlPath) {
		File fileToOpen = new File(pXmlPath);
		if (fileToOpen.isFile()) {
			IFileStore fileStore = EFS.getLocalFileSystem().getStore(fileToOpen.toURI());
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			try {
				IDE.openEditorOnFileStore( page, fileStore );
			} catch ( PartInitException e ) {
				//Put your exception handler here if you wish to
			}
		} else {

		}
	}

	public String getSearchBeanFromUser() {
		//* read the user input - the bean ID
		InputDialog dialog = new InputDialog(
				null, 
				"Search for bean definition", 
				"Enter the bean ID", "<SAMPLE_BEAN_1>", (IInputValidator)null);

		//* return null for cancelled search.
		return Dialog.OK == dialog.open() ? dialog.getValue() : null;
	}
}