package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.actions.DetectDuplicatesAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.AbstractClientJob;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class ReviewExtractedItemsPage extends AbstractWizardPage implements ImportAction.Context
{
    private TableViewer tableViewer;
    private TableViewer errorTableViewer;

    private Label lblPrimaryPortfolio;
    private ComboViewer primaryPortfolio;
    private Label lblSecondaryPortfolio;
    private ComboViewer secondaryPortfolio;
    private Label lblPrimaryAccount;
    private ComboViewer primaryAccount;
    private Label lblSecondaryAccount;
    private ComboViewer secondaryAccount;

    private final Client client;
    private final Extractor extractor;
    private List<File> files;

    private List<ExtractedEntry> allEntries = new ArrayList<ExtractedEntry>();

    public ReviewExtractedItemsPage(Client client, Extractor extractor, List<File> files)
    {
        super("reviewitems"); //$NON-NLS-1$

        this.client = client;
        this.extractor = extractor;
        this.files = files;

        setTitle(extractor.getLabel());
        setDescription(Messages.PDFImportWizardDescription);
    }

    public List<ExtractedEntry> getEntries()
    {
        return allEntries;
    }

    @Override
    public Portfolio getPortfolio()
    {
        return (Portfolio) ((IStructuredSelection) primaryPortfolio.getSelection()).getFirstElement();
    }

    @Override
    public Portfolio getSecondaryPortfolio()
    {
        return (Portfolio) ((IStructuredSelection) secondaryPortfolio.getSelection()).getFirstElement();
    }

    @Override
    public Account getAccount()
    {
        return (Account) ((IStructuredSelection) primaryAccount.getSelection()).getFirstElement();
    }

    @Override
    public Account getSecondaryAccount()
    {
        return (Account) ((IStructuredSelection) secondaryAccount.getSelection()).getFirstElement();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new FormLayout());

        Composite targetContainer = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(4).applyTo(targetContainer);

        lblPrimaryAccount = new Label(targetContainer, SWT.NONE);
        lblPrimaryAccount.setText(Messages.ColumnAccount);
        Combo cmbAccount = new Combo(targetContainer, SWT.READ_ONLY);
        primaryAccount = new ComboViewer(cmbAccount);
        primaryAccount.setContentProvider(ArrayContentProvider.getInstance());
        primaryAccount.setInput(client.getActiveAccounts());
        primaryAccount.addSelectionChangedListener(e -> markDuplicatesAndRefresh(allEntries));
        cmbAccount.select(0);

        lblSecondaryAccount = new Label(targetContainer, SWT.NONE);
        lblSecondaryAccount.setText(Messages.LabelTransferTo);
        lblSecondaryAccount.setVisible(false);
        Combo cmbAccountTarget = new Combo(targetContainer, SWT.READ_ONLY);
        secondaryAccount = new ComboViewer(cmbAccountTarget);
        secondaryAccount.setContentProvider(ArrayContentProvider.getInstance());
        secondaryAccount.setInput(client.getActiveAccounts());
        secondaryAccount.getControl().setVisible(false);
        cmbAccountTarget.select(0);

        lblPrimaryPortfolio = new Label(targetContainer, SWT.NONE);
        lblPrimaryPortfolio.setText(Messages.ColumnPortfolio);
        Combo cmbPortfolio = new Combo(targetContainer, SWT.READ_ONLY);
        primaryPortfolio = new ComboViewer(cmbPortfolio);
        primaryPortfolio.setContentProvider(ArrayContentProvider.getInstance());
        primaryPortfolio.setInput(client.getActivePortfolios());
        primaryPortfolio.addSelectionChangedListener(e -> markDuplicatesAndRefresh(allEntries));
        cmbPortfolio.select(0);

        lblSecondaryPortfolio = new Label(targetContainer, SWT.NONE);
        lblSecondaryPortfolio.setText(Messages.LabelTransferTo);
        lblSecondaryPortfolio.setVisible(false);
        Combo cmbPortfolioTarget = new Combo(targetContainer, SWT.READ_ONLY);
        secondaryPortfolio = new ComboViewer(cmbPortfolioTarget);
        secondaryPortfolio.setContentProvider(ArrayContentProvider.getInstance());
        secondaryPortfolio.setInput(client.getActivePortfolios());
        secondaryPortfolio.getControl().setVisible(false);
        cmbPortfolioTarget.select(0);

        Composite compositeTable = new Composite(container, SWT.NONE);
        Composite errorTable = new Composite(container, SWT.NONE);

        //
        // form layout
        //

        FormData data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        targetContainer.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(targetContainer, 10);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        data.bottom = new FormAttachment(70, 0);
        compositeTable.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(compositeTable, 10);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        data.bottom = new FormAttachment(100, 0);
        errorTable.setLayoutData(data);

        //
        // table & columns
        //

        TableColumnLayout layout = new TableColumnLayout();
        compositeTable.setLayout(layout);

        tableViewer = new TableViewer(compositeTable, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        tableViewer.setContentProvider(new SimpleListContentProvider());

        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        addColumns(tableViewer, layout);
        attachContextMenu(table);

        layout = new TableColumnLayout();
        errorTable.setLayout(layout);
        errorTableViewer = new TableViewer(errorTable, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        errorTableViewer.setContentProvider(new SimpleListContentProvider());

        table = errorTableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        addColumnsExceptionTable(errorTableViewer, layout);
    }

    private void addColumnsExceptionTable(TableViewer viewer, TableColumnLayout layout)
    {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnErrorMessages);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Exception) element).getMessage();
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnWeightData(100, true));
    }

    private void addColumns(TableViewer viewer, TableColumnLayout layout)
    {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnStatus);
        column.setLabelProvider(new FormattedLabelProvider()
        {
            @Override
            public Image getImage(ExtractedEntry element)
            {
                Images image = null;
                switch (element.getMaxCode())
                {
                    case WARNING:
                        image = Images.WARNING;
                        break;
                    case ERROR:
                        image = Images.ERROR;
                        break;
                    case OK:
                    default:
                }
                return image != null ? image.image() : null;
            }

            @Override
            public String getText(ExtractedEntry entry)
            {
                return ""; //$NON-NLS-1$
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(22, true));

        column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnDate);
        column.setLabelProvider(new FormattedLabelProvider()
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                LocalDate date = entry.getItem().getDate();
                return date != null ? Values.Date.format(date) : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(80, true));

        column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnTransactionType);
        column.setLabelProvider(new FormattedLabelProvider()
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                return entry.getItem().getTypeInformation();
            }

            @Override
            public Image getImage(ExtractedEntry entry)
            {
                Annotated subject = entry.getItem().getSubject();
                if (subject instanceof AccountTransaction)
                    return Images.ACCOUNT.image();
                else if (subject instanceof PortfolioTransaction)
                    return Images.PORTFOLIO.image();
                else if (subject instanceof Security)
                    return Images.SECURITY.image();
                else if (subject instanceof BuySellEntry)
                    return Images.PORTFOLIO.image();
                else if (subject instanceof AccountTransferEntry)
                    return Images.ACCOUNT.image();
                else if (subject instanceof PortfolioTransferEntry)
                    return Images.PORTFOLIO.image();
                else
                    return null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100, true));

        column = new TableViewerColumn(viewer, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnAmount);
        column.setLabelProvider(new FormattedLabelProvider()
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                return Values.Amount.formatNonZero(entry.getItem().getAmount());
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(80, true));

        column = new TableViewerColumn(viewer, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnShares);
        column.setLabelProvider(new FormattedLabelProvider()
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                return Values.Share.formatNonZero(entry.getItem().getShares());
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(80, true));

        column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnSecurity);
        column.setLabelProvider(new FormattedLabelProvider()
        {
            @Override
            public String getText(ExtractedEntry entry)
            {
                Security security = entry.getItem().getSecurity();
                return security != null ? security.getName() : null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(250, true));
    }

    private void attachContextMenu(final Table table)
    {
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> showContextMenu(manager));

        final Menu contextMenu = menuMgr.createContextMenu(table.getShell());
        table.setMenu(contextMenu);

        table.addDisposeListener(e -> {
            if (contextMenu != null && !contextMenu.isDisposed())
                contextMenu.dispose();
        });
    }

    private void showContextMenu(IMenuManager manager)
    {
        IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();

        boolean atLeastOneImported = false;
        boolean atLeastOneNotImported = false;

        for (Object element : selection.toList())
        {
            ExtractedEntry entry = (ExtractedEntry) element;

            // an entry will be imported if it is marked as to be
            // imported *and* not a duplicate
            atLeastOneImported = atLeastOneImported || entry.isImported();

            // an entry will not be imported if it marked as not to be
            // imported *or* if it is marked as duplicate
            atLeastOneNotImported = atLeastOneNotImported || !entry.isImported();
        }

        // provide a hint to the user why the entry is struck out
        if (selection.size() == 1)
        {
            ExtractedEntry entry = (ExtractedEntry) selection.getFirstElement();
            entry.getStatus() //
                            .filter(s -> s.getCode() != ImportAction.Status.Code.OK) //
                            .forEach(s -> {
                                Images image = s.getCode() == ImportAction.Status.Code.WARNING ? //
                                                Images.WARNING : Images.ERROR;
                                manager.add(new LabelOnly(s.getMessage(), image.descriptor()));
                            });
        }

        if (atLeastOneImported)
        {
            manager.add(new Action(Messages.LabelDoNotImport)
            {
                @Override
                public void run()
                {
                    for (Object element : ((IStructuredSelection) tableViewer.getSelection()).toList())
                        ((ExtractedEntry) element).setImported(false);

                    tableViewer.refresh();
                }
            });
        }

        if (atLeastOneNotImported)
        {
            manager.add(new Action(Messages.LabelDoImport)
            {
                @Override
                public void run()
                {
                    for (Object element : ((IStructuredSelection) tableViewer.getSelection()).toList())
                        ((ExtractedEntry) element).setImported(true);

                    tableViewer.refresh();
                }
            });
        }
    }

    @Override
    public void beforePage()
    {
        try
        {
            new AbstractClientJob(client, extractor.getLabel())
            {
                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    monitor.beginTask(Messages.PDFImportWizardMsgExtracting, files.size());

                    final List<Exception> errors = new ArrayList<Exception>();
                    List<ExtractedEntry> entries = extractor //
                                    .extract(files, errors).stream() //
                                    .map(i -> new ExtractedEntry(i)) //
                                    .collect(Collectors.toList());

                    // Logging them is not a bad idea if the whole method fails
                    PortfolioPlugin.log(errors);

                    Display.getDefault().asyncExec(() -> setResults(entries, errors));

                    return Status.OK_STATUS;
                }
            }.schedule();
        }
        catch (Exception e)
        {
            throw new UnsupportedOperationException(e);
        }
    }

    private void setResults(List<ExtractedEntry> entries, List<Exception> errors)
    {
        markDuplicates(entries);

        allEntries.addAll(entries);
        tableViewer.setInput(allEntries);
        errorTableViewer.setInput(errors);

        for (ExtractedEntry entry : entries)
        {
            if (entry.getItem() instanceof Extractor.AccountTransferItem)
            {
                lblSecondaryAccount.setVisible(true);
                secondaryAccount.getControl().setVisible(true);
            }
            else if (entry.getItem() instanceof Extractor.PortfolioTransferItem)
            {
                lblSecondaryPortfolio.setVisible(true);
                secondaryPortfolio.getControl().setVisible(true);
            }
        }
    }

    private void markDuplicatesAndRefresh(List<ExtractedEntry> entries)
    {
        markDuplicates(entries);
        tableViewer.refresh();
    }

    private void markDuplicates(List<ExtractedEntry> entries)
    {
        DetectDuplicatesAction action = new DetectDuplicatesAction();
        for (ExtractedEntry entry : entries)
        {
            entry.clearStatus();
            entry.addStatus(entry.getItem().apply(action, this));
        }
    }

    static class FormattedLabelProvider extends StyledCellLabelProvider
    {
        private static Styler strikeoutStyler = new Styler()
        {
            @Override
            public void applyStyles(TextStyle textStyle)
            {
                textStyle.strikeout = true;
            }
        };

        public String getText(ExtractedEntry element)
        {
            return null;
        }

        public Image getImage(ExtractedEntry element)
        {
            return null;
        }

        @Override
        public void update(ViewerCell cell)
        {
            ExtractedEntry entry = (ExtractedEntry) cell.getElement();
            String text = getText(entry);
            if (text == null)
                text = ""; //$NON-NLS-1$

            boolean strikeout = !entry.isImported();
            StyledString styledString = new StyledString(text, strikeout ? strikeoutStyler : null);

            cell.setText(styledString.toString());
            cell.setStyleRanges(styledString.getStyleRanges());
            cell.setImage(getImage(entry));

            super.update(cell);
        }
    }
}
