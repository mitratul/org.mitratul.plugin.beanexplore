package org.mitratul.plugin.beanexplore.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.mitratul.plugin.beanexplore.core.JdtHelper;

public class SearchBeanCommandHandler extends AbstractHandler {

	private JdtHelper mJdtHelper;

	public SearchBeanCommandHandler() {
		mJdtHelper = new JdtHelper();
	}

	@Override
	public Object execute(ExecutionEvent arg0) throws ExecutionException {

		//* read the user input - the bean ID
		String aBeanId = mJdtHelper.getSearchBeanFromUser();
		if (aBeanId != null && aBeanId.length() > 0) {
			//* If the user actually triggered the search - pressed OK
//			System.out.println("Searching for " + aBeanId);
			mJdtHelper.displayDefinitionXml(aBeanId);
		} //* else user has cancelled the search, or entered empty string

		return null;
	}

}
