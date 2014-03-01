package org.mitratul.plugin.beanexplore.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.mitratul.plugin.beanexplore.core.JdtHelper;

public class CreateIndexCommandHandler extends AbstractHandler {

	private JdtHelper mJdtHelper = new JdtHelper();

	public CreateIndexCommandHandler() {
		mJdtHelper = new JdtHelper();
//		new InputDialog(IWorkbenchWindow., dialogTitle, dialogMessage, initialValue, validator)
	}
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selections = (IStructuredSelection) 
				HandlerUtil.getActiveMenuSelection(event);
		
		if (selections.isEmpty()) {
			//* If any project is selected, index only them.
			mJdtHelper.indexWorkspace();
		} else {
			//* else index the all the projects in the workspace.
			mJdtHelper.indexSelectedProject(selections);
		}

		return null;
	}
}
