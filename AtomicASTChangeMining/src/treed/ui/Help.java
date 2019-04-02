/**
 * 
 */
package treed.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * @author Hoan Nguyen
 *
 */
public class Help extends Composite {
	
	public Help(Composite parent, int style) {
		super(parent, style);
		setBackgroundMode(SWT.INHERIT_DEFAULT);
		Composite composite = null;
		
		GridLayout gridLayout = new GridLayout();
		super.setLayout(gridLayout);
		gridLayout = new GridLayout();
		//gridLayout.numColumns = 1;
		/*composite = new Composite(this, SWT.NONE);
		composite.setLayout(gridLayout);
		Label lblOverviewTitle = new Label(composite, SWT.NONE), lblOverviewContent = new Label(composite, SWT.NONE);
		lblOverviewTitle.setText("Overview");
		lblOverviewContent.setText("PTrack works as an origin analysis tool that tracks the changes of program entities between versions.");*/
		
		Label lblTermsTittle = null, lblTermDescription = null;
		
		/*composite = new Composite(this,SWT.NONE);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		composite.setLayout(gridLayout);
		
		lblTermsTittle = new Label(composite, SWT.NONE);
		lblTermsTittle.setText("Terminologies");
		lblTermsTittle = new Label(composite, SWT.NONE);
		lblTermName = new Label(composite, SWT.NONE);
		lblTermDescription = new Label(composite, SWT.NONE);
		lblTermName.setImage(ASTTreedViewer.IMG_PACKAGE);
		lblTermDescription.setText("Package");
		lblTermName = new Label(composite, SWT.NONE); lblTermDescription = new Label(composite, SWT.NONE);
		lblTermName.setImage(ASTTreedViewer.IMG_CLASS);
		lblTermDescription.setText("Class");
		lblTermName = new Label(composite, SWT.NONE); lblTermDescription = new Label(composite, SWT.NONE);
		lblTermName.setImage(ASTTreedViewer.IMG_INTERFACE);
		lblTermDescription.setText("Interface");
		lblTermName = new Label(composite, SWT.NONE); lblTermDescription = new Label(composite, SWT.NONE);
		lblTermName.setImage(ASTTreedViewer.IMG_METHOD);
		lblTermDescription.setText("Method");
		lblTermName = new Label(composite, SWT.NONE); lblTermDescription = new Label(composite, SWT.NONE);
		lblTermName.setText("Internal usages");
		lblTermDescription.setText("Internal usages of a program entity are program entities which are used in its implementation");
		lblTermName = new Label(composite, SWT.NONE); lblTermDescription = new Label(composite, SWT.NONE);
		lblTermName.setText("External usages");
		lblTermDescription.setText("External usages of a program entity are program entities which use it");*/
		
		composite = new Composite(this,SWT.NONE);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		composite.setLayout(gridLayout);
		
		lblTermsTittle = new Label(composite, SWT.NONE);
		lblTermsTittle.setText("Color");
//		lblTermName = new Label(composite, SWT.NONE);
//		lblTermName.setText("No icon");
		lblTermDescription = new Label(composite, SWT.NONE);
		lblTermDescription.setText("No changes");
//		lblTermName = new Label(composite, SWT.NONE);
//		lblTermName.setImage(ASTTreedViewer.IMG_DEL);
		lblTermDescription = new Label(composite, SWT.NONE);
		lblTermDescription.setText("Deleted");
		lblTermDescription.setForeground(ASTTreedViewer.COLOR_ADD);
//		lblTermName = new Label(composite, SWT.NONE);
//		lblTermName.setImage(ASTTreedViewer.IMG_ADD);
		lblTermDescription = new Label(composite, SWT.NONE);
		lblTermDescription.setText("Added");
		lblTermDescription.setForeground(ASTTreedViewer.COLOR_ADD);
//		lblTermName = new Label(composite, SWT.NONE);
//		lblTermName.setImage(ASTTreedViewer.IMG_MOD_EX);
		lblTermDescription = new Label(composite, SWT.NONE);
		lblTermDescription.setForeground(ASTTreedViewer.COLOR_MOD_EX);
		lblTermDescription.setText("Relabeled");
//		lblTermName = new Label(composite, SWT.NONE);
//		lblTermName.setImage(ASTTreedViewer.IMG_MOD_IN);
		lblTermDescription = new Label(composite, SWT.NONE);
		lblTermDescription.setForeground(ASTTreedViewer.COLOR_MOD_IN);
		lblTermDescription.setText("Moved");
		
	}

}
