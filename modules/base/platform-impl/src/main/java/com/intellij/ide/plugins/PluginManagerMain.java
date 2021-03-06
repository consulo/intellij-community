/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.plugins;

import com.intellij.CommonBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.sorters.SortByStatusAction;
import com.intellij.ide.ui.search.SearchUtil;
import com.intellij.ide.ui.search.SearchableOptionsRegistrar;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.util.ui.update.UiNotifyConnector;
import com.intellij.xml.util.XmlStringUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.disposer.Disposable;
import consulo.ide.updateSettings.UpdateSettings;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TableModelListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import java.awt.*;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

import static com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces;

/**
 * @author stathik
 * @since Dec 25, 2003
 */
public abstract class PluginManagerMain implements Disposable {
  public static Logger LOG = Logger.getInstance(PluginManagerMain.class);

  private static final String TEXT_SUFFIX = "</body></html>";
  private static final String HTML_PREFIX = "<a href=\"";
  private static final String HTML_SUFFIX = "</a>";

  private boolean requireShutdown = false;

  private Wrapper myRoot;

  private JEditorPane myDescriptionTextArea;

  private PluginHeaderPanel myPluginHeaderPanel;

  protected JPanel myTablePanel;
  protected PluginTableModel myPluginsModel;
  protected PluginTable myPluginTable;

  protected final MyPluginsFilter myFilter = new MyPluginsFilter();
  private boolean myDisposed = false;
  private boolean myBusy = false;

  private InstalledPluginsManagerMain myInstalledTab;
  private AvailablePluginsManagerMain myAvailableTab;

  public PluginManagerMain() {
  }

  protected void init() {
    myRoot = new Wrapper();

    OnePixelSplitter splitter = new OnePixelSplitter(false, 0.5f);
    myRoot.setContent(splitter);

    myDescriptionTextArea = new JEditorPane("text/html", "");
    myDescriptionTextArea.setEditorKit(UIUtil.getHTMLEditorKit());
    myDescriptionTextArea.setEditable(false);
    myDescriptionTextArea.addHyperlinkListener(new MyHyperlinkListener());
    myDescriptionTextArea.setBackground(UIUtil.getTextFieldBackground());

    myPluginHeaderPanel = new PluginHeaderPanel(this, getPluginTable());

    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setBackground(UIUtil.getTextFieldBackground());

    myPluginHeaderPanel.getPanel().setBackground(UIUtil.getTextFieldBackground());
    myPluginHeaderPanel.getPanel().setOpaque(true);
    myPluginHeaderPanel.getPanel().setBorder(JBUI.Borders.empty(5));

    headerPanel.add(myPluginHeaderPanel.getPanel(), BorderLayout.NORTH);
    headerPanel.add(ScrollPaneFactory.createScrollPane(myDescriptionTextArea, true), BorderLayout.CENTER);
    splitter.setSecondComponent(headerPanel);

    myTablePanel = new JPanel(new BorderLayout());
    splitter.setFirstComponent(myTablePanel);

    PluginTable table = createTable();
    myPluginTable = table;

    installTableActions();
    myTablePanel.add(ScrollPaneFactory.createScrollPane(table, true), BorderLayout.CENTER);

    final JPanel header = new JPanel(new BorderLayout()) {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(UIUtil.getPanelBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
      }
    };
    header.setBorder(new CustomLineBorder(0, 0, JBUI.scale(1), 0));

    LabelPopup sortLabel = new LabelPopup("Sort by:", labelPopup -> createSortersGroup());

    header.add(myFilter, BorderLayout.CENTER);
    JPanel rightHelpPanel = new JPanel(new HorizontalLayout(JBUI.scale(5)));
    rightHelpPanel.add(sortLabel);
    addCustomFilters(rightHelpPanel::add);

    BorderLayoutPanel botton = new BorderLayoutPanel();
    botton.setBorder(new CustomLineBorder(JBUI.scale(1), 0, 0, 0));
    header.add(botton.addToRight(rightHelpPanel), BorderLayout.SOUTH);

    myTablePanel.add(header, BorderLayout.NORTH);

    final TableModelListener modelListener = e -> {
      String text = "";
      if (myPluginsModel.isSortByStatus()) {
        text += "status,";
      }
      if (myPluginsModel.isSortByRating()) {
        text += "rating,";
      }
      if (myPluginsModel.isSortByDownloads()) {
        text += "downloads,";
      }
      if (myPluginsModel.isSortByUpdated()) {
        text += "updated,";
      }
      text += "name";
      sortLabel.setPrefixedText(text);
    };
    myPluginTable.getModel().addTableModelListener(modelListener);
    modelListener.tableChanged(null);
  }

  protected void addCustomFilters(Consumer<JComponent> adder) {

  }

  @Nonnull
  protected abstract PluginTable createTable();

  @Override
  public void dispose() {
    myDisposed = true;
  }

  public boolean isDisposed() {
    return myDisposed;
  }

  public void filter(String filter) {
    myFilter.setSelectedItem(filter);
  }

  public void reset() {
    UiNotifyConnector.doWhenFirstShown(getPluginTable(), () -> {
      requireShutdown = false;
      TableUtil.ensureSelectionExists(getPluginTable());
    });
  }

  public PluginTable getPluginTable() {
    return myPluginTable;
  }

  public PluginTableModel getPluginsModel() {
    return myPluginsModel;
  }

  protected void installTableActions() {
    myPluginTable.getSelectionModel().addListSelectionListener(e -> refresh());

    //PopupHandler.installUnknownPopupHandler(pluginTable, getActionGroup(false), ActionManager.getInstance());

    new MySpeedSearchBar(myPluginTable);
  }

  @RequiredUIAccess
  public void refresh() {
    final PluginDescriptor[] descriptors = myPluginTable.getSelectedObjects();
    pluginInfoUpdate(descriptors != null && descriptors.length == 1 ? descriptors[0] : null, myFilter.getFilter(), myDescriptionTextArea, myPluginHeaderPanel, this);
    //myActionToolbar.updateActionsImmediately();
    //final JComponent parent = (JComponent)myHeader.getParent();
    //parent.revalidate();
    //parent.repaint();
  }

  public void setRequireShutdown(boolean val) {
    requireShutdown |= val;
  }

  public List<PluginDescriptor> getDependentList(PluginDescriptor pluginDescriptor) {
    return myPluginsModel.dependent(pluginDescriptor);
  }

  protected void modifyPluginsList(List<PluginDescriptor> list) {
    PluginDescriptor[] selected = myPluginTable.getSelectedObjects();
    myPluginsModel.updatePluginsList(list);
    myPluginsModel.filter(myFilter.getFilter().toLowerCase());
    if (selected != null) {
      select(selected);
    }
  }

  public abstract ActionGroup getActionGroup();

  @Nonnull
  protected PluginManagerMain getAvailable() {
    return Objects.requireNonNull(myAvailableTab);
  }

  @Nonnull
  protected PluginManagerMain getInstalled() {
    return Objects.requireNonNull(myInstalledTab);
  }

  public void setAvailableTab(AvailablePluginsManagerMain availableTab) {
    myAvailableTab = availableTab;
  }

  public void setInstalledTab(InstalledPluginsManagerMain installedTab) {
    myInstalledTab = installedTab;
  }

  public JPanel getMainPanel() {
    return myRoot;
  }

  /**
   * Start a new thread which downloads new list of plugins from the site in
   * the background and updates a list of plugins in the table.
   */
  protected void loadPluginsFromHostInBackground() {
    setDownloadStatus(true);

    Application.get().executeOnPooledThread(() -> {
      Ref<List<PluginDescriptor>> ref = Ref.create();
      List<String> errorMessages = new ArrayList<>();

      try {
        ref.set(RepositoryHelper.loadOnlyPluginsFromRepository(null, UpdateSettings.getInstance().getChannel()));
      }
      catch (Throwable e) {
        LOG.info(e);
        errorMessages.add(e.getMessage());
      }

      UIUtil.invokeLaterIfNeeded(() -> {
        setDownloadStatus(false);
        List<PluginDescriptor> list = ref.get();

        if (list != null) {
          modifyPluginsList(list);
          propagateUpdates(list);
        }
        if (!errorMessages.isEmpty()) {
          if (Messages.showOkCancelDialog(IdeBundle.message("error.list.of.plugins.was.not.loaded", StringUtil.join(errorMessages, ", ")), IdeBundle.message("title.plugins"),
                                          CommonBundle.message("button.retry"), CommonBundle.getCancelButtonText(), Messages.getErrorIcon()) == Messages.OK) {
            loadPluginsFromHostInBackground();
          }
        }
      });
    });
  }

  protected abstract void propagateUpdates(List<PluginDescriptor> list);

  protected void setDownloadStatus(boolean status) {
    myPluginTable.setPaintBusy(status);
    myBusy = status;
  }

  protected void loadAvailablePlugins() {
    loadPluginsFromHostInBackground();
  }

  public boolean isRequireShutdown() {
    return requireShutdown;
  }

  public void ignoreChanges() {
    requireShutdown = false;
  }

  public static void pluginInfoUpdate(PluginDescriptor plugin, @Nullable String filter, @Nonnull JEditorPane descriptionTextArea, @Nonnull PluginHeaderPanel header, PluginManagerMain manager) {

    if (plugin == null) {
      setTextValue(null, filter, descriptionTextArea);
      header.getPanel().setVisible(false);
      return;
    }
    StringBuilder sb = new StringBuilder();
    header.setPlugin(plugin);
    String description = plugin.getDescription();
    if (!isEmptyOrSpaces(description)) {
      sb.append(description);
    }

    String changeNotes = plugin.getChangeNotes();
    if (!isEmptyOrSpaces(changeNotes)) {
      sb.append("<h4>Change Notes</h4>");
      sb.append(changeNotes);
    }

    if (!PluginIds.isPlatformPlugin(plugin.getPluginId())) {
      String vendor = plugin.getVendor();
      String vendorEmail = plugin.getVendorEmail();
      String vendorUrl = plugin.getVendorUrl();
      if (!isEmptyOrSpaces(vendor) || !isEmptyOrSpaces(vendorEmail) || !isEmptyOrSpaces(vendorUrl)) {
        sb.append("<h4>Vendor</h4>");

        if (!isEmptyOrSpaces(vendor)) {
          sb.append(vendor);
        }
        if (!isEmptyOrSpaces(vendorUrl)) {
          sb.append("<br>").append(composeHref(vendorUrl));
        }
        if (!isEmptyOrSpaces(vendorEmail)) {
          sb.append("<br>").append(HTML_PREFIX).append("mailto:").append(vendorEmail).append("\">").append(vendorEmail).append(HTML_SUFFIX);
        }
      }

      String pluginDescriptorUrl = plugin.getUrl();
      if (!isEmptyOrSpaces(pluginDescriptorUrl)) {
        sb.append("<h4>Plugin homepage</h4>").append(composeHref(pluginDescriptorUrl));
      }

      String size = plugin instanceof PluginNode ? ((PluginNode)plugin).getSize() : null;
      if (!isEmptyOrSpaces(size)) {
        sb.append("<h4>Size</h4>").append(PluginManagerColumnInfo.getFormattedSize(size));
      }
    }

    setTextValue(sb, filter, descriptionTextArea);
  }

  private static void setTextValue(@Nullable StringBuilder text, @Nullable String filter, JEditorPane pane) {
    if (text != null) {
      text.insert(0, getTextPrefix());
      text.append(TEXT_SUFFIX);
      pane.setText(SearchUtil.markup(text.toString(), filter).trim());
      pane.setCaretPosition(0);
    }
    else {
      pane.setText(getTextPrefix() + TEXT_SUFFIX);
    }
  }

  private static String getTextPrefix() {
    String string = "<html><head>\n" +
                    "    <style type=\"text/css\">\n" +
                    "        p {\n font-family: Arial,serif; font-size: %dpt; margin: %dpx %dpx\n" +
                    "        }\n" +
                    "    </style>\n" +
                    "</head><body style=\"font-family: Arial,serif; font-size: %dpt; margin: %dpx %dpx;\">";
    int font = JBUI.scale(12);
    int margin5 = JBUI.scale(5);
    int margin2 = JBUI.scale(2);
    return String.format(string, font, margin2, margin2, font, margin5, margin5);
  }

  private static String composeHref(String vendorUrl) {
    return HTML_PREFIX + vendorUrl + "\">" + vendorUrl + HTML_SUFFIX;
  }

  public boolean isModified() {
    if (requireShutdown) return true;
    return false;
  }

  public String apply() {
    final String applyMessage = canApply();
    if (applyMessage != null) return applyMessage;
    setRequireShutdown(true);
    return null;
  }

  @Nullable
  protected String canApply() {
    return null;
  }

  protected DefaultActionGroup createSortersGroup() {
    final DefaultActionGroup group = new DefaultActionGroup("Sort by", true);
    group.addAction(new SortByStatusAction(myPluginTable, myPluginsModel));
    return group;
  }

  public static class MyHyperlinkListener implements HyperlinkListener {
    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        JEditorPane pane = (JEditorPane)e.getSource();
        if (e instanceof HTMLFrameHyperlinkEvent) {
          HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent)e;
          HTMLDocument doc = (HTMLDocument)pane.getDocument();
          doc.processHTMLFrameHyperlinkEvent(evt);
        }
        else {
          URL url = e.getURL();
          if (url != null) {
            BrowserUtil.browse(url);
          }
        }
      }
    }
  }

  private static class MySpeedSearchBar extends SpeedSearchBase<PluginTable> {
    public MySpeedSearchBar(PluginTable cmp) {
      super(cmp);
    }

    @Override
    protected int convertIndexToModel(int viewIndex) {
      return getComponent().convertRowIndexToModel(viewIndex);
    }

    @Override
    public int getSelectedIndex() {
      return myComponent.getSelectedRow();
    }

    @Override
    public Object[] getAllElements() {
      return myComponent.getElements();
    }

    @Override
    public String getElementText(Object element) {
      return ((PluginDescriptor)element).getName();
    }

    @Override
    public void selectElement(Object element, String selectedText) {
      for (int i = 0; i < myComponent.getRowCount(); i++) {
        if (myComponent.getObjectAt(i).getName().equals(((PluginDescriptor)element).getName())) {
          myComponent.setRowSelectionInterval(i, i);
          TableUtil.scrollSelectionToVisible(myComponent);
          break;
        }
      }
    }
  }

  public void select(PluginDescriptor... descriptors) {
    myPluginTable.select(descriptors);
  }

  protected static boolean isAccepted(String filter, Set<String> search, PluginDescriptor descriptor) {
    if (StringUtil.isEmpty(filter)) return true;
    if (isAccepted(search, filter, descriptor.getName())) {
      return true;
    }
    else {
      final String description = descriptor.getDescription();
      if (description != null && isAccepted(search, filter, description)) {
        return true;
      }
      final String category = descriptor.getCategory();
      if (isAccepted(search, filter, category)) {
        return true;
      }
      final String changeNotes = descriptor.getChangeNotes();
      if (changeNotes != null && isAccepted(search, filter, changeNotes)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isAccepted(final Set<String> search, @Nonnull final String filter, @Nonnull final String description) {
    if (StringUtil.containsIgnoreCase(description, filter)) return true;
    final SearchableOptionsRegistrar optionsRegistrar = SearchableOptionsRegistrar.getInstance();
    final HashSet<String> descriptionSet = new HashSet<>(search);
    descriptionSet.removeAll(optionsRegistrar.getProcessedWords(description));
    if (descriptionSet.isEmpty()) {
      return true;
    }
    return false;
  }


  public static void notifyPluginsWereInstalled(@Nonnull Collection<? extends PluginDescriptor> installed, Project project) {
    String pluginName = installed.size() == 1 ? installed.iterator().next().getName() : null;
    notifyPluginsWereUpdated(pluginName != null ? "Plugin \'" + pluginName + "\' was successfully installed" : "Plugins were installed", project);
  }

  public static void notifyPluginsWereUpdated(final String title, final Project project) {
    final ApplicationEx app = (ApplicationEx)Application.get();
    final boolean restartCapable = app.isRestartCapable();
    String message = restartCapable
                     ? IdeBundle.message("message.idea.restart.required", ApplicationNamesInfo.getInstance().getFullProductName())
                     : IdeBundle.message("message.idea.shutdown.required", ApplicationNamesInfo.getInstance().getFullProductName());
    message += "<br><a href=";
    message += restartCapable ? "\"restart\">Restart now" : "\"shutdown\">Shutdown";
    message += "</a>";
    new NotificationGroup("Plugins Lifecycle Group", NotificationDisplayType.STICKY_BALLOON, true)
            .createNotification(title, XmlStringUtil.wrapInHtml(message), NotificationType.INFORMATION, new NotificationListener() {
              @Override
              public void hyperlinkUpdate(@Nonnull Notification notification, @Nonnull HyperlinkEvent event) {
                notification.expire();
                if (restartCapable) {
                  app.restart(true);
                }
                else {
                  app.exit(true, true);
                }
              }
            }).notify(project);
  }

  public class MyPluginsFilter extends FilterComponent {

    public MyPluginsFilter() {
      super("PLUGIN_FILTER", 5);
      getTextEditor().setBorder(JBUI.Borders.empty(2));
    }

    @Override
    public void filter() {
      myPluginsModel.filter(getFilter().toLowerCase());
      TableUtil.ensureSelectionExists(getPluginTable());
    }
  }

  protected class RefreshAction extends DumbAwareAction {
    public RefreshAction() {
      super("Reload List of Plugins", "Reload list of plugins", AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      loadAvailablePlugins();
      myFilter.setFilter("");
    }

    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setEnabled(!myBusy);
    }
  }
}
