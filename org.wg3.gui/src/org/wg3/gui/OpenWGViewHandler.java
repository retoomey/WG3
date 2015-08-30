package org.wg3.gui;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Basic handler to open up the WG3 test window
 * @author Robert Toomey
 */
public class OpenWGViewHandler extends AbstractHandler {
	private int instanceNum = 0;
	private final String viewId = View.ID;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
		if (workbenchWindow != null) {
			IWorkbenchPage page = workbenchWindow.getActivePage();

			try {
				workbenchWindow.getActivePage().showView(View.ID,
						Integer.toString(instanceNum++),
						IWorkbenchPage.VIEW_ACTIVATE);
			} catch (PartInitException e) {
				MessageDialog.openError(workbenchWindow.getShell(), "Error",
						"Error opening view:" + e.getMessage());
			}
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
