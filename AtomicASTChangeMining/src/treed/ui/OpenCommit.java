/**
 * 
 */
package treed.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author Hoan Nguyen
 *
 */
public class OpenCommit extends Composite {
	String path, commit;
	
	public OpenCommit(Composite parent, int style, String filterPath) {
		super(parent, style);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		super.setLayout(gridLayout);
		
		Composite composite = new Composite(this,SWT.NONE);
		composite.setLayout(gridLayout);
		
		Label l1 = new Label(composite, SWT.NONE);
		l1.setText("Repository");
		Text txtPath = new Text(composite, SWT.BORDER);
		txtPath.setLayoutData(new GridData(500, txtPath.getLineHeight()));
		Button b = new Button(composite, SWT.PUSH);
		b.setText("&Browse");
		b.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				DirectoryDialog fd = new DirectoryDialog(getShell(), SWT.OPEN);
		        fd.setText("Open a commit from a repository");
		        fd.setFilterPath(filterPath);
		        String selected = fd.open();
		        if (selected != null) {
			        txtPath.setText(selected);
			        path = selected;
		        }
			}
		});
		Label l2 = new Label(composite, SWT.NONE);
		l2.setText("Commit");
		Text txtCommit = new Text(composite, SWT.BORDER);
		txtCommit.setLayoutData(new GridData(500, txtCommit.getLineHeight()));
		Button b2 = new Button(composite, SWT.PUSH);
		b2.setText("OK");
		b2.getShell().setDefaultButton(b2);
		b2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				path = txtPath.getText();
				commit = txtCommit.getText();
				b2.getShell().close();
			}
		});
	}

}
