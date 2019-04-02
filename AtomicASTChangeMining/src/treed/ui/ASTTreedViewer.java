package treed.ui;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import repository.ChangedFile;
import repository.GitConnector;
import treed.TreedConstants;
import treed.TreedMapper;
import treed.TreedUtils;
import utils.FileIO;
import utils.JavaASTUtil;

import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class ASTTreedViewer extends Composite implements TreedConstants {
	public static final String DATA_LOCATION = "L", DATA_INDEX = "I", DATA_NODE = "Node";
	//public static final String srcDir = "G:\\Documents and Settings\\Hoan Nguyen\\My Documents\\projects\\migration\\OriginAnalysis";
	public static final String srcDir = ".";
	
	public static final String ICON_PTRack = srcDir + "\\icons\\PTrack.jpeg";
	public static final String ICON_HELP = srcDir + "\\icons\\help_contents.gif";
	public static final String ICON_PACKAGE = srcDir + "\\icons\\package_mode.gif";
	public static final String ICON_CLASS = srcDir + "\\icons\\class_obj.gif";
	public static final String ICON_INTERFACE = srcDir + "\\icons\\int_obj.gif";
	public static final String ICON_METHOD = srcDir + "\\icons\\methdef_obj.gif";
	public static final String ICON_ADD = srcDir + "\\icons\\add_obj.gif";
	public static final String ICON_DEL = srcDir + "\\icons\\delete_obj.gif";
	public static final String ICON_MOD_IN = srcDir + "\\icons\\write_obj.gif";
	public static final String ICON_MOD_EX = srcDir + "\\icons\\write_obj_disabled.gif";
	public static Image IMG_PACKAGE, IMG_CLASS, IMG_INTERFACE, IMG_METHOD, IMG_ADD, IMG_DEL, IMG_MOD_IN, IMG_MOD_EX;
	public static final int COL_ENTITY = 0, COL_STATUS = 1;
	
	static Color COLOR_ADD, COLOR_TXTHI, COLOR_DEL, COLOR_MOD_IN, COLOR_MOD_EX, COLOR_SELECT;
	
	private Menu menuBar, menuFile, menuRepo, menuView, menuHelp, menuChangeSub;
	private MenuItem menuFileHeader, menuRepoHeader, menuViewHeader, menuHelpHeader, menuExitItem, menuOpenRepoItem, menuOpenCommitItem, menuShowChangesItem, menuGetHelpItem, menuChangeAll, menuChangeDel, menuChangeAdd, menuChangeMod, menuChangeModIn, menuChangeModEx;
	private Text[] textPaths = new Text[2];
	private Tree[] astTrees = new Tree[2];
	private Tree commitTree, changedPathTree;
	private HashMap<TreeItem, ASTNode>[] tree2ASTs = new HashMap[2];
	private HashMap<ASTNode, TreeItem>[] ast2Trees = new HashMap[2];
	private StyledText[] txtSrcCodes = new StyledText[2];
	private String[] srcContents = new String[2];
	private ASTNode[] asts = new ASTNode[2];
	private GitConnector gc;
	private ArrayList<ChangedFile> changedFiles;
	private HashMap<TreeItem, ChangedFile> tree2File = new HashMap<>();
	private Clipboard clipboard;
	
	public static Label statusBar;
	
	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("Treed");
		shell.setImage(new Image(display, ASTTreedViewer.ICON_PTRack));
		shell.setLayout(new FillLayout());
		shell.setMaximized(true);
					
		ASTTreedViewer viewer = new ASTTreedViewer(shell, SWT.NONE);
		viewer.show();
		final Point minimum = shell.computeSize(SWT.DEFAULT,SWT.DEFAULT,true);
		shell.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				Shell shell = (Shell)e.widget;
				Point size = shell.getSize();
				boolean change = false; 
				if (size.x < minimum.x) {
					size.x = minimum.x;
					change = true; 
				}	
				if (size.y < minimum.y) {
					size.y = minimum.y;
					change = true; 
				}				
				if (change) 
					shell.setSize(size);
			}
		});
		shell.open();
					
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	public ASTTreedViewer(Composite parent,int style) {
		super(parent,style);
		ASTTreedViewer.COLOR_ADD = getDisplay().getSystemColor(SWT.COLOR_BLUE);
		ASTTreedViewer.COLOR_TXTHI = getDisplay().getSystemColor(SWT.COLOR_BLUE);
		ASTTreedViewer.COLOR_DEL = getDisplay().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND);
		ASTTreedViewer.COLOR_MOD_IN = getDisplay().getSystemColor(SWT.COLOR_MAGENTA);
		ASTTreedViewer.COLOR_MOD_EX = getDisplay().getSystemColor(SWT.COLOR_DARK_RED);
		ASTTreedViewer.COLOR_SELECT = getDisplay().getSystemColor(SWT.COLOR_GRAY);
		
		IMG_PACKAGE = new Image(getDisplay(), ICON_PACKAGE);
		IMG_CLASS = new Image(getDisplay(), ICON_CLASS);
		IMG_INTERFACE = new Image(getDisplay(), ICON_INTERFACE);
		IMG_METHOD = new Image(getDisplay(), ICON_METHOD);
		IMG_ADD = new Image(getDisplay(), ICON_ADD);
		IMG_DEL = new Image(getDisplay(), ICON_DEL);
		IMG_MOD_IN = new Image(getDisplay(), ICON_MOD_IN);
		IMG_MOD_EX = new Image(getDisplay(), ICON_MOD_EX);

		menuBar = new Menu(getShell(), SWT.BAR);
	    
		menuFileHeader = new MenuItem(menuBar, SWT.CASCADE);
	    menuFileHeader.setText("&File");

	    menuFile = new Menu(getShell(), SWT.DROP_DOWN);
	    menuFileHeader.setMenu(menuFile);

	    menuExitItem = new MenuItem(menuFile, SWT.PUSH);
	    menuExitItem.setText("E&xit");

	    menuRepoHeader = new MenuItem(menuBar, SWT.CASCADE);
	    menuRepoHeader.setText("&Repository");
	    
	    menuRepo = new Menu(getShell(), SWT.DROP_DOWN);
	    menuRepoHeader.setMenu(menuRepo);
	    
	    menuOpenRepoItem = new MenuItem(menuRepo, SWT.PUSH);
	    menuOpenRepoItem.setText("Open a repository");
	    menuOpenCommitItem = new MenuItem(menuRepo, SWT.PUSH);
	    menuOpenCommitItem.setText("Open a commit");
	    
	    menuViewHeader = new MenuItem(menuBar, SWT.CASCADE);
	    menuViewHeader.setText("&View");

	    menuView = new Menu(getShell(), SWT.DROP_DOWN);
	    menuViewHeader.setMenu(menuView);

	    menuShowChangesItem = new MenuItem(menuView, SWT.CASCADE);
	    menuShowChangesItem.setText("Show &Changes Only");
	    menuChangeSub = new Menu(getShell(), SWT.DROP_DOWN);
	    menuShowChangesItem.setMenu(menuChangeSub);
	    menuChangeAll = new MenuItem(menuChangeSub, SWT.CHECK);
	    menuChangeAll.setText("&All");
	    menuChangeDel = new MenuItem(menuChangeSub, SWT.CHECK);
	    menuChangeDel.setText("&Deleted");
	    menuChangeAdd = new MenuItem(menuChangeSub, SWT.CHECK);
	    menuChangeAdd.setText("&Added");
	    menuChangeMod = new MenuItem(menuChangeSub, SWT.CHECK);
	    menuChangeMod.setText("&Modified");
	    menuChangeModIn = new MenuItem(menuChangeSub, SWT.CHECK);
	    menuChangeModIn.setText("&Relabled");
	    menuChangeModEx = new MenuItem(menuChangeSub, SWT.CHECK);
	    menuChangeModEx.setText("&Moved");
	    
	    menuHelpHeader = new MenuItem(menuBar, SWT.CASCADE);
	    menuHelpHeader.setText("&Help");

	    menuHelp = new Menu(getShell(), SWT.DROP_DOWN);
	    menuHelpHeader.setMenu(menuHelp);

	    menuGetHelpItem = new MenuItem(menuHelp, SWT.PUSH);
	    menuGetHelpItem.setText("&Legend");
	    menuGetHelpItem.setImage(new Image(getDisplay(), ICON_HELP));
	    
	    menuExitItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getShell().close();
			    //getDisplay().dispose();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				getShell().close();
			    //getDisplay().dispose();
			}
		});
	    
	    menuGetHelpItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Display display = getDisplay();
				Shell shell = new Shell(display);
				shell.setText("Legend");
				shell.setImage(new Image(getDisplay(), ICON_HELP));
				shell.setLayout(new FillLayout());
				new Help(shell, SWT.NONE);
				shell.pack();
				shell.open();
				
				while (!shell.isDisposed()) {
					if (!display.readAndDispatch())
						display.sleep();
				}
			}@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				Display display = getDisplay();
				Shell shell = new Shell(display);
				shell.setText("Legend");
				shell.setLayout(new FillLayout());
				new Help(shell, SWT.NONE);
				shell.pack();
				shell.open();
				
				while (!shell.isDisposed()) {
					if (!display.readAndDispatch())
						display.sleep();
				}
			}
		});
	    parent.getShell().setMenuBar(menuBar);
	}
	
	public void show() {
		clipboard = new Clipboard(getDisplay());
		ast2Trees[0] = new HashMap<>(); ast2Trees[1] = new HashMap<>();
		tree2ASTs[0] = new HashMap<>(); tree2ASTs[1] = new HashMap<>();
		GridLayout gridLayout = new GridLayout();
		super.setLayout(gridLayout);
		
		SashForm sashFormVertical = new SashForm(this, SWT.VERTICAL);
		sashFormVertical.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | 
				GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
		
		SashForm sashUpFormHorizontal = new SashForm(sashFormVertical, SWT.HORIZONTAL);
		sashUpFormHorizontal.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | 
				GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
		SashForm sashLowFormHorizontal = new SashForm(sashFormVertical, SWT.HORIZONTAL);
		sashLowFormHorizontal.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | 
				GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
		
		this.commitTree = new Tree(sashUpFormHorizontal, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		commitTree.setHeaderVisible(true);
		commitTree.setLinesVisible(true);
		TreeColumn colId = new TreeColumn(commitTree, SWT.NONE);
		TreeColumn colMessage = new TreeColumn(commitTree, SWT.NONE);
		colId.setText("Commit id"); colMessage.setText("Short message");
		colId.setWidth(getDisplay().getBounds().width/8); colMessage.setWidth(getDisplay().getBounds().width/8-28);
		this.changedPathTree = new Tree(sashUpFormHorizontal, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		changedPathTree.setHeaderVisible(true);
		changedPathTree.setLinesVisible(true);
		colId = new TreeColumn(changedPathTree, SWT.NONE);
		colMessage = new TreeColumn(changedPathTree, SWT.NONE);
		colId.setText("New path"); colMessage.setText("Old Path");
		colId.setWidth(getDisplay().getBounds().width/8); colMessage.setWidth(getDisplay().getBounds().width/8-28);
	    
	    menuOpenRepoItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog fd = new DirectoryDialog(getShell(), SWT.OPEN);
		        fd.setText("Open a repository");
		        String path = "F:/";
		        if (gc != null)
		        	path = gc.getRepository().getDirectory().getAbsolutePath();
		        fd.setFilterPath(path);
		        path = fd.open();
		        if (path == null) return;
		        File file = new File(path);
		        if (!path.endsWith(".git") && new File(file, ".git").exists()) {
		        	file = new File(file, ".git");
		        	path = file.getAbsolutePath();
		        }
		        gc = new GitConnector(path);
		        if (gc.connect()) {
		        	commitTree.removeAll();
		        	changedPathTree.removeAll();
		        	clearASTs();
		        	clearCode();
		        	Iterable<RevCommit> commits = gc.log();
		        	for (RevCommit commit : commits) {
		        		if (commit.getParentCount() != 1) continue;
		        		TreeItem item = new TreeItem(commitTree, SWT.NONE);
		        		item.setText(commit.name());
		        		item.setText(1, commit.getFullMessage());
		        	}
					gc.close();
		        } else {
		        	MessageBox mb = new MessageBox(getShell(), SWT.ERROR | SWT.OK);
		        	mb.setMessage("Not a valid repository folder!!!");
		        	mb.open();
		        }
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	    
	    menuOpenCommitItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Display display = getDisplay();
				Shell shell = new Shell(display);
				shell.setText("Open a commit");
				shell.setImage(new Image(getDisplay(), ICON_HELP));
				shell.setLayout(new FillLayout());
		        String path = "F:/";
		        if (gc != null)
		        	path = gc.getRepository().getDirectory().getAbsolutePath();
				OpenCommit openCommit = new OpenCommit(shell, SWT.NONE, path);
				shell.pack();
				shell.open();
				
				while (!shell.isDisposed()) {
					if (!display.readAndDispatch())
						display.sleep();
				}
				path = openCommit.path;
				String commitId = openCommit.commit;
				if (path != null && commitId != null) {
			        gc = new GitConnector(path);
			        if (gc.connect()) {
			        	RevCommit commit = gc.getCommit(commitId);
			        	if (commit != null) {
			        		commitTree.removeAll();
			        		changedPathTree.removeAll();
			        		tree2File.clear();
				        	clearASTs();
				        	clearCode();
			        		TreeItem item = new TreeItem(commitTree, SWT.NONE);
			        		item.setText(commit.name());
			        		item.setText(1, commit.getFullMessage());
			        		changedFiles = gc.getChangedFiles(commit, ".java");
			        		for (ChangedFile cf : changedFiles) {
			        			item = new TreeItem(changedPathTree, SWT.NONE);
			        			item.setText(cf.newPath);
			        			item.setText(1, cf.oldPath);
			        			tree2File.put(item, cf);
			        		}
			        	} else {
				        	Iterable<RevCommit> commits = gc.log();
				        	for (RevCommit c : commits) {
				        		if (c.getParentCount() != 1) continue;
				        		if (c.getName().contains(commitId)) {
					        		TreeItem item = new TreeItem(commitTree, SWT.NONE);
					        		item.setText(c.name());
					        		item.setText(1, c.getFullMessage());
				        		}
				        	}
			        	}
						gc.close();
			        } else {
			        	MessageBox mb = new MessageBox(getShell(), SWT.ERROR | SWT.OK);
			        	mb.setMessage("Not a valid repository folder!!!");
			        	mb.open();
			        }
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	    
	    commitTree.addSelectionListener(new SelectionAdapter() {
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		String commitId = commitTree.getSelection()[0].getText();
	    		RevCommit commit = gc.getCommit(commitId );
	    		clipboard.setContents(new Object[]{commitId + "\n" + commit.getFullMessage()}, new Transfer[]{TextTransfer.getInstance()});
				changedFiles = gc.getChangedFiles(commit, ".java");
        		changedPathTree.removeAll();
        		tree2File.clear();
        		for (ChangedFile cf : changedFiles) {
        			TreeItem item = new TreeItem(changedPathTree, SWT.NONE);
        			item.setText(cf.newPath);
        			item.setText(1, cf.oldPath);
        			tree2File.put(item, cf);
        		}
	    	}
		});
	    
	    changedPathTree.addSelectionListener(new SelectionAdapter() {
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		TreeItem item = changedPathTree.getSelection()[0];
	    		clipboard.setContents(new Object[]{item.getText() + "\n" + item.getText(1)}, new Transfer[]{TextTransfer.getInstance()});
	    		ChangedFile cf = tree2File.get(item);
		        textPaths[0].setText(cf.oldPath);
		        srcContents[0] = cf.oldContent;
		        showCode(txtSrcCodes[0], srcContents[0]);
		        asts[0] = JavaASTUtil.parseSource(srcContents[0]);
		        showAST(0);
		        textPaths[1].setText(cf.newPath);
		        srcContents[1] = cf.newContent;
		        showCode(txtSrcCodes[1], srcContents[1]);
		        asts[1] = JavaASTUtil.parseSource(srcContents[1]);
		        showAST(1);
	    	}
		});
	    
	    Composite uprightcomposite = new Composite(sashUpFormHorizontal, SWT.NONE);
		gridLayout = new GridLayout();
		uprightcomposite.setLayout(gridLayout);
		uprightcomposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Composite composite = new Composite(uprightcomposite, SWT.NONE);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		composite.setLayout(gridLayout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		this.textPaths[0] = new Text(composite, SWT.BORDER | SWT.SINGLE);
		this.textPaths[0].setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		this.textPaths[0].setData(DATA_INDEX, 0);
		this.textPaths[0].addListener(SWT.MouseDoubleClick, new Listener() {
			@Override
			public void handleEvent(Event event) {
				openFile(0);
			}
		});
		Button b1 = new Button(composite, SWT.PUSH);
		b1.setText("Browse");
		b1.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		b1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				openFile(0);
			}
		});
		
		this.textPaths[1] = new Text(composite, SWT.BORDER | SWT.SINGLE);
		this.textPaths[1].setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		this.textPaths[1].setData(DATA_INDEX, 1);
		this.textPaths[1].addListener(SWT.MouseDoubleClick, new Listener() {
			@Override
			public void handleEvent(Event event) {
				openFile(1);
			}
		});
		Button b2 = new Button(composite, SWT.PUSH);
		b2.setText("Browse");
		b2.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		b2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				openFile(1);
			}
		});
		
		for(int i = 0; i < 2; i++) {
			this.astTrees[i] = new Tree(sashLowFormHorizontal, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
			this.astTrees[i].setLayoutData(new GridData(GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_FILL));
			astTrees[i].setHeaderVisible(true);
			astTrees[i].setLinesVisible(true);
			TreeColumn colEntity = new TreeColumn(astTrees[i], SWT.NONE);
			TreeColumn colStatus = new TreeColumn(this.astTrees[i], SWT.NONE);
			colEntity.setText("AST Node");
			colStatus.setText("Status");
			colEntity.setWidth(getDisplay().getBounds().width/2-85); colStatus.setWidth(55);
		}
		
		SashForm sashLowUpRightFormHorizontal = new SashForm(uprightcomposite, SWT.HORIZONTAL);
		sashLowUpRightFormHorizontal.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | 
				GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
		for(int i = 0; i < 2; i++) {
			TabFolder tabFolder = new TabFolder(sashLowUpRightFormHorizontal, SWT.BORDER);
			TabItem tabCode = new TabItem(tabFolder, SWT.NONE);
			tabCode.setText("Source code");
			this.txtSrcCodes[i] = new StyledText(tabFolder, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
			this.txtSrcCodes[i].setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
					| GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_FILL));
			StyledText text = this.txtSrcCodes[i];
			text.addLineStyleListener(new LineStyleListener() {
			    @Override
				public void lineGetStyle(LineStyleEvent e) {
			        //Set the line number
			        e.bulletIndex = text.getLineAtOffset(e.lineOffset);

			        //Set the style, 12 pixles wide for each digit
			        StyleRange style = new StyleRange();
			        style.metrics = new GlyphMetrics(0, 0, Integer.toString(text.getLineCount()+1).length()*12);

			        //Create and set the bullet
			        e.bullet = new Bullet(ST.BULLET_NUMBER,style);
			    }
			});
			text.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.stateMask == SWT.CTRL && e.keyCode == 'a') {
						text.selectAll();
						e.doit = false;
					}
				}
			});
			final int index = i;
			text.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					ASTNode ast = asts[index];
					Tree tree = astTrees[index];
					Point point = text.getSelection();
					ASTNode node = JavaASTUtil.getNode(ast, point.x, point.y);
					HashMap<ASTNode, TreeItem> ast2Tree = ast2Trees[index];
					TreeItem item = ast2Tree.get(node);
					tree.setSelection(item);
					tree.setTopItem(item);
				}
			});
			tabCode.setControl(this.txtSrcCodes[i]);
		}
		sashFormVertical.setWeights(new int[]{40, 60});
		sashUpFormHorizontal.setWeights(new int[]{25, 25, 50});
		
		for(int i = 0; i < 2; i++) {
			StyledText text = this.txtSrcCodes[i], otherText = this.txtSrcCodes[1-i];
			Tree tree = this.astTrees[i], otherTree = this.astTrees[1-i];
			final int index = i;
			tree.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					TreeItem item = tree.getSelection()[0];
					HashMap<TreeItem, ASTNode> tree2AST = tree2ASTs[index];
					ASTNode node = tree2AST .get(item);
					ASTNode mappedNode = (ASTNode) node.getProperty(PROPERTY_MAP);
					if (mappedNode != null) {
						HashMap<ASTNode, TreeItem> ast2Tree = ast2Trees[1-index];
						TreeItem mappedItem = ast2Tree.get(mappedNode);
						otherTree.setSelection(mappedItem);
					}
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {}
			});
			tree.addListener(SWT.MouseDoubleClick, new Listener() {
				@Override
				public void handleEvent(Event event) {
					TreeItem item = tree.getSelection()[0];
					HashMap<TreeItem, ASTNode> tree2AST = tree2ASTs[index];
					ASTNode node = tree2AST.get(item), mappedNode = (ASTNode) node.getProperty(PROPERTY_MAP);
					text.setSelection(node.getStartPosition(), node.getStartPosition() + node.getLength());
					if (mappedNode != null)
						otherText.setSelection(mappedNode.getStartPosition(), mappedNode.getStartPosition() + mappedNode.getLength());
				}
			});
		}
		
		composite = new Composite(this,SWT.NONE);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		composite.setLayout(gridLayout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL/* | GridData.HORIZONTAL_ALIGN_END*/));
		
		statusBar = new Label(composite, SWT.NONE);
		statusBar.setText("Done");
		statusBar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL));
		
		Button button = new Button(composite, SWT.PUSH);
		button.setText("Update ASTs");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
				        srcContents[0] = txtSrcCodes[0].getText();
				        asts[0] = JavaASTUtil.parseSource(srcContents[0]);
				        srcContents[1] = txtSrcCodes[1].getText();
				        asts[1] = JavaASTUtil.parseSource(srcContents[1]);
						showASTs();
					}
				});
			}
		});
		
		button = new Button(composite,SWT.PUSH);
		button.setText("Diff");
		button.getShell().setDefaultButton(button); 
		button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
				        clear(asts[0]);
				        clear(asts[1]);
						TreedMapper tm = new TreedMapper(asts[0], asts[1]);
						tm.map(true);
						showASTs();
					}

					private void clear(ASTNode ast) {
						ast.accept(new ASTVisitor(true) {
							@Override
							public void preVisit(ASTNode node) {
								node.setProperty(PROPERTY_MAP, null);
								node.setProperty(PROPERTY_STATUS, null);
							}
						});
					}
				});
			}
		});
	}

	public void openFile(int index) {
		FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
        fd.setText("Open");
//        fd.setFilterPath("D:/");
        String[] filterExt = { "*.java", "*.*" };
        fd.setFilterExtensions(filterExt);
        String path = fd.open();
        if (path == null) return;
        textPaths[index].setText(path);
        srcContents[index] = FileIO.readStringFromFile(path);
        showCode(txtSrcCodes[index], srcContents[index]);
        asts[index] = JavaASTUtil.parseSource(srcContents[index], new File(path).getName());
        showAST(index);
	}
	
	protected void clearCode() {
		showCode(txtSrcCodes[0], "");
		showCode(txtSrcCodes[1], "");
	}

	protected void clearASTs() {
		clearAST(0);
		clearAST(1);
	}

	private void clearAST(int i) {
		Tree tree = astTrees[i];
		tree.removeAll();
		this.ast2Trees[i].clear();
		this.tree2ASTs[i].clear();
	}

	protected void showASTs() {
		showAST(0);
		showAST(1);
	}
	
	protected void showAST(int i) {
		Tree tree = astTrees[i];
		ASTNode ast = asts[i];
		tree.removeAll();
		this.ast2Trees[i].clear();
		this.tree2ASTs[i].clear();
		if (ast == null)
			return;
		ast.accept(new ASTVisitor(true) {
			private int numOfChanges = 0;
			private TreeItem first = null;
			
			@Override
			public void preVisit(ASTNode node) {
				TreeItem item = null;
				if (node == ast) {
					item = new TreeItem(tree, SWT.NONE);
				} else {
					item = new TreeItem(ast2Trees[i].get(node.getParent()), SWT.NONE);
				}
				item.setText(TreedUtils.buildASTLabel(node));
				
				if (!(node instanceof Annotation) && node.getProperty(PROPERTY_STATUS) != null) {
					int status = (int) node.getProperty(PROPERTY_STATUS);
					if (status == STATUS_MOVED)
						item.setForeground(COLOR_MOD_IN);
					else if (status == STATUS_RELABELED)
						item.setForeground(COLOR_MOD_EX);
					else if (status == STATUS_UNMAPPED)
						item.setForeground(COLOR_ADD);
					if (status != STATUS_UNCHANGED) {
						item.setText(1, TreedMapper.getStatus(status));
						numOfChanges++;
						if (numOfChanges == 1)
							first = item;
						ASTNode pNode = node.getParent();
						int pstatus = (int) pNode.getProperty(PROPERTY_STATUS);
						if (pstatus != status)
							expand(item);
					}
				}
				ast2Trees[i].put(node, item);
				tree2ASTs[i].put(item, node);
			}
			
			private void expand(TreeItem item) {
				while (item != null) {
					item.setExpanded(true);
					TreeItem p = item.getParentItem();
					if (p == null) {
						item.setExpanded(true);
						break;
					}
					if (p.getExpanded()) {
						break;
					}
					item = p;
				}
			}
			
			@Override
			public void postVisit(ASTNode node) {
				if (node == ast) {
					if (first != null) {
						tree.setSelection(first);
						tree.setTopItem(first);
					} else {
						expandAll(tree.getItems());
					}
				}
			}

			private void expandAll(TreeItem[] items) {
				for (TreeItem item : items) {
					item.setExpanded(true);
					expandAll(item.getItems());
				}
			}
		});
	}

	protected void showCode(StyledText styledText, String content) {
		styledText.setText(content);
	}
	
	// do not allow layout modifications
	@Override
	public void setLayout(Layout layout) {
	}
}