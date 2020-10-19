/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.ide.projectView.impl;

import com.intellij.ProjectTopics;
import com.intellij.history.LocalHistory;
import com.intellij.history.LocalHistoryAction;
import com.intellij.icons.AllIcons;
import com.intellij.ide.*;
import com.intellij.ide.impl.ProjectViewSelectInTarget;
import com.intellij.ide.projectView.HelpID;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.impl.nodes.*;
import com.intellij.ide.scopeView.ScopeViewPane;
import com.intellij.ide.ui.SplitterProportionsDataImpl;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.DeleteHandler;
import com.intellij.ide.util.DirectoryChooserUtil;
import com.intellij.ide.util.EditorHelper;
import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.ui.configuration.actions.ModuleDeleteProvider;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.SplitterProportionsData;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.PsiPackageHelper;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.AutoScrollFromSourceHandler;
import com.intellij.ui.AutoScrollToSourceHandler;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.components.JBList;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerAdapter;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.switcher.QuickActionProvider;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IJSwingUtilities;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.URLUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import consulo.disposer.Disposable;
import consulo.ide.projectView.ProjectViewEx;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.psi.PsiPackageSupportProviders;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.wm.impl.ToolWindowContentUI;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;

@Singleton
@State(name = "ProjectView", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
public class ProjectViewImpl implements ProjectViewEx, PersistentStateComponent<Element>, Disposable, QuickActionProvider, BusyObject {
  private static final Logger LOG = Logger.getInstance(ProjectViewImpl.class);
  private static final Key<String> ID_KEY = Key.create("pane-id");
  private static final Key<String> SUB_ID_KEY = Key.create("pane-sub-id");
  private final CopyPasteDelegator myCopyPasteDelegator;
  private boolean isInitialized;
  private boolean myExtensionsLoaded = false;
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final ProjectViewSharedSettings myProjectViewSharedSettings;

  // + options
  private final Map<String, Boolean> myFlattenPackages = new THashMap<>();
  private static final boolean ourFlattenPackagesDefaults = false;
  private final Map<String, Boolean> myShowMembers = new THashMap<>();
  private static final boolean ourShowMembersDefaults = false;
  private final Map<String, Boolean> myManualOrder = new THashMap<>();
  private static final boolean ourManualOrderDefaults = false;
  private final Map<String, Boolean> mySortByType = new THashMap<>();
  private static final boolean ourSortByTypeDefaults = false;
  private final Map<String, Boolean> myShowModules = new THashMap<>();
  private static final boolean ourShowModulesDefaults = true;
  private final Map<String, Boolean> myShowLibraryContents = new THashMap<>();
  private static final boolean ourShowLibraryContentsDefaults = true;
  private final Map<String, Boolean> myHideEmptyPackages = new THashMap<>();
  private static final boolean ourHideEmptyPackagesDefaults = true;
  private final Map<String, Boolean> myAbbreviatePackageNames = new THashMap<>();
  private static final boolean ourAbbreviatePackagesDefaults = false;
  private final Map<String, Boolean> myAutoscrollToSource = new THashMap<>();
  private final Map<String, Boolean> myAutoscrollFromSource = new THashMap<>();
  private static final boolean ourAutoscrollFromSourceDefaults = false;

  private boolean myFoldersAlwaysOnTop = true;

  private String myCurrentViewId;
  private String myCurrentViewSubId;
  // - options

  private final AutoScrollToSourceHandler myAutoScrollToSourceHandler;
  private final MyAutoScrollFromSourceHandler myAutoScrollFromSourceHandler;

  private final IdeView myIdeView = new MyIdeView();
  private final MyDeletePSIElementProvider myDeletePSIElementProvider = new MyDeletePSIElementProvider();
  private final ModuleDeleteProvider myDeleteModuleProvider = new ModuleDeleteProvider();

  private SimpleToolWindowPanel myPanel;
  private final Map<String, AbstractProjectViewPane> myId2Pane = new LinkedHashMap<>();
  private final Collection<AbstractProjectViewPane> myUninitializedPanes = new THashSet<>();

  static final Key<ProjectViewImpl> DATA_KEY = Key.create("com.intellij.ide.projectView.impl.ProjectViewImpl");

  private DefaultActionGroup myActionGroup;
  private String mySavedPaneId = ProjectViewPane.ID;
  private String mySavedPaneSubId;
  //private static final Icon COMPACT_EMPTY_MIDDLE_PACKAGES_ICON = IconLoader.getIcon("/objectBrowser/compactEmptyPackages.png");
  //private static final Icon HIDE_EMPTY_MIDDLE_PACKAGES_ICON = IconLoader.getIcon("/objectBrowser/hideEmptyPackages.png");
  @NonNls
  private static final String ELEMENT_NAVIGATOR = "navigator";
  @NonNls
  private static final String ELEMENT_PANES = "panes";
  @NonNls
  private static final String ELEMENT_PANE = "pane";
  @NonNls
  private static final String ATTRIBUTE_CURRENT_VIEW = "currentView";
  @NonNls
  private static final String ATTRIBUTE_CURRENT_SUBVIEW = "currentSubView";
  @NonNls
  private static final String ELEMENT_FLATTEN_PACKAGES = "flattenPackages";
  @NonNls
  private static final String ELEMENT_SHOW_MEMBERS = "showMembers";
  @NonNls
  private static final String ELEMENT_SHOW_MODULES = "showModules";
  @NonNls
  private static final String ELEMENT_SHOW_LIBRARY_CONTENTS = "showLibraryContents";
  @NonNls
  private static final String ELEMENT_HIDE_EMPTY_PACKAGES = "hideEmptyPackages";
  @NonNls
  private static final String ELEMENT_ABBREVIATE_PACKAGE_NAMES = "abbreviatePackageNames";
  @NonNls
  private static final String ELEMENT_AUTOSCROLL_TO_SOURCE = "autoscrollToSource";
  @NonNls
  private static final String ELEMENT_AUTOSCROLL_FROM_SOURCE = "autoscrollFromSource";
  @NonNls
  private static final String ELEMENT_SORT_BY_TYPE = "sortByType";
  @NonNls
  private static final String ELEMENT_FOLDERS_ALWAYS_ON_TOP = "foldersAlwaysOnTop";
  @NonNls
  private static final String ELEMENT_MANUAL_ORDER = "manualOrder";

  private static final String ATTRIBUTE_ID = "id";
  private JPanel myViewContentPanel;
  private static final Comparator<AbstractProjectViewPane> PANE_WEIGHT_COMPARATOR = (o1, o2) -> o1.getWeight() - o2.getWeight();
  private final FileEditorManager myFileEditorManager;
  private final MyPanel myDataProvider;
  private final SplitterProportionsData splitterProportions = new SplitterProportionsDataImpl();
  private final MessageBusConnection myConnection;
  private final Map<String, Element> myUninitializedPaneState = new HashMap<>();
  private final Map<String, SelectInTarget> mySelectInTargets = new LinkedHashMap<>();
  private ContentManager myContentManager;

  @Inject
  public ProjectViewImpl(@Nonnull Project project, FileEditorManager fileEditorManager, ToolWindowManager toolWindowManager, @Nonnull ProjectViewSharedSettings projectViewSharedSettings) {
    myProject = project;
    myProjectViewSharedSettings = projectViewSharedSettings;

    constructUi();

    myFileEditorManager = fileEditorManager;

    myConnection = project.getMessageBus().connect();
    myConnection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(ModuleRootEvent event) {
        refresh();
      }
    });

    myAutoScrollFromSourceHandler = new MyAutoScrollFromSourceHandler();

    myDataProvider = new MyPanel();
    myDataProvider.add(myPanel, BorderLayout.CENTER);
    myCopyPasteDelegator = new CopyPasteDelegator(myProject, myPanel) {
      @Override
      @Nonnull
      protected PsiElement[] getSelectedElements() {
        final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
        return viewPane == null ? PsiElement.EMPTY_ARRAY : viewPane.getSelectedPSIElements();
      }
    };
    myAutoScrollToSourceHandler = new AutoScrollToSourceHandler() {
      @Override
      protected boolean isAutoScrollMode() {
        return isAutoscrollToSource(myCurrentViewId);
      }

      @Override
      protected void setAutoScrollMode(boolean state) {
        setAutoscrollToSource(state, myCurrentViewId);
      }
    };
    ((ToolWindowManagerEx)toolWindowManager).addToolWindowManagerListener(new ToolWindowManagerListener() {
      private boolean toolWindowVisible;

      @Override
      public void stateChanged() {
        ToolWindow window = toolWindowManager.getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (window == null) return;
        if (window.isVisible() && !toolWindowVisible) {
          String id = getCurrentViewId();
          if (isAutoscrollToSource(id)) {
            AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();

            if (currentProjectViewPane != null) {
              myAutoScrollToSourceHandler.onMouseClicked(currentProjectViewPane.getTree());
            }
          }
          if (isAutoscrollFromSource(id)) {
            myAutoScrollFromSourceHandler.setAutoScrollEnabled(true);
          }
        }
        toolWindowVisible = window.isVisible();
      }
    });
  }

  private void constructUi() {
    myViewContentPanel = new JPanel();
    myPanel = new SimpleToolWindowPanel(true).setProvideQuickActions(false);
    myPanel.setContent(myViewContentPanel);
  }

  @Nonnull
  @Override
  public String getName() {
    return "Project";
  }

  @Nonnull
  @Override
  public List<AnAction> getActions(boolean originalProvider) {
    List<AnAction> result = new ArrayList<>();

    DefaultActionGroup views = new DefaultActionGroup("Change View", true);

    ChangeViewAction lastHeader = null;
    for (int i = 0; i < myContentManager.getContentCount(); i++) {
      Content each = myContentManager.getContent(i);
      if (each == null) continue;

      String id = each.getUserData(ID_KEY);
      String subId = each.getUserData(SUB_ID_KEY);
      ChangeViewAction newHeader = new ChangeViewAction(id, subId);

      if (lastHeader != null) {
        boolean lastHasKids = lastHeader.mySubId != null;
        boolean newHasKids = newHeader.mySubId != null;
        if (lastHasKids != newHasKids || lastHasKids && lastHeader.myId != newHeader.myId) {
          views.add(AnSeparator.getInstance());
        }
      }

      views.add(newHeader);
      lastHeader = newHeader;
    }
    result.add(views);
    result.add(AnSeparator.getInstance());

    if (myActionGroup != null) {
      List<AnAction> secondary = new ArrayList<>();
      for (AnAction each : myActionGroup.getChildren(null)) {
        if (myActionGroup.isPrimary(each)) {
          result.add(each);
        }
        else {
          secondary.add(each);
        }
      }

      result.add(AnSeparator.getInstance());
      result.addAll(secondary);
    }

    return result;
  }

  private class ChangeViewAction extends AnAction {
    @Nonnull
    private final String myId;
    @Nullable
    private final String mySubId;

    private ChangeViewAction(@Nonnull String id, @Nullable String subId) {
      myId = id;
      mySubId = subId;
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
      AbstractProjectViewPane pane = getProjectViewPaneById(myId);
      e.getPresentation().setText(mySubId != null ? pane.getPresentableSubIdName(mySubId) : pane.getTitle());
      e.getPresentation().setIcon(mySubId != null ? pane.getPresentableSubIdIcon(mySubId) : pane.getIcon());
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      changeView(myId, mySubId);
    }
  }

  @Override
  public synchronized void addProjectPane(@Nonnull final AbstractProjectViewPane pane) {
    myUninitializedPanes.add(pane);
    SelectInTarget selectInTarget = pane.createSelectInTarget();
    if (selectInTarget != null) {
      mySelectInTargets.put(pane.getId(), selectInTarget);
    }
    if (isInitialized) {
      doAddUninitializedPanes();
    }
  }

  @Override
  public synchronized void removeProjectPane(@Nonnull AbstractProjectViewPane pane) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myUninitializedPanes.remove(pane);
    //assume we are completely initialized here
    String idToRemove = pane.getId();

    if (!myId2Pane.containsKey(idToRemove)) return;
    for (int i = getContentManager().getContentCount() - 1; i >= 0; i--) {
      Content content = getContentManager().getContent(i);
      String id = content != null ? content.getUserData(ID_KEY) : null;
      if (id != null && id.equals(idToRemove)) {
        getContentManager().removeContent(content, true);
      }
    }
    myId2Pane.remove(idToRemove);
    mySelectInTargets.remove(idToRemove);
    viewSelectionChanged();
  }

  private synchronized void doAddUninitializedPanes() {
    for (AbstractProjectViewPane pane : myUninitializedPanes) {
      doAddPane(pane);
    }
    final Content[] contents = getContentManager().getContents();
    for (int i = 1; i < contents.length; i++) {
      Content content = contents[i];
      Content prev = contents[i - 1];
      if (!StringUtil.equals(content.getUserData(ID_KEY), prev.getUserData(ID_KEY)) && prev.getUserData(SUB_ID_KEY) != null && content.getSeparator() == null) {
        content.setSeparator("");
      }
    }

    String selectID = null;
    String selectSubID = null;

    // try to find saved selected view...
    for (Content content : contents) {
      final String id = content.getUserData(ID_KEY);
      final String subId = content.getUserData(SUB_ID_KEY);
      if (id != null && id.equals(mySavedPaneId) && StringUtil.equals(subId, mySavedPaneSubId)) {
        selectID = id;
        selectSubID = subId;
        mySavedPaneId = null;
        mySavedPaneSubId = null;
        break;
      }
    }

    // saved view not found (plugin disabled, ID changed etc.) - select first available view...
    if (selectID == null && contents.length > 0 && myCurrentViewId == null) {
      Content content = contents[0];
      selectID = content.getUserData(ID_KEY);
      selectSubID = content.getUserData(SUB_ID_KEY);
    }

    if (selectID != null) {
      changeView(selectID, selectSubID);
    }

    myUninitializedPanes.clear();
  }

  private void doAddPane(@Nonnull final AbstractProjectViewPane newPane) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    int index;
    final ContentManager manager = getContentManager();
    for (index = 0; index < manager.getContentCount(); index++) {
      Content content = manager.getContent(index);
      String id = content.getUserData(ID_KEY);
      AbstractProjectViewPane pane = myId2Pane.get(id);

      int comp = PANE_WEIGHT_COMPARATOR.compare(pane, newPane);
      LOG.assertTrue(comp != 0, "Project view pane " +
                                newPane +
                                " has the same weight as " +
                                pane +
                                ". Please make sure that you overload getWeight() and return a distinct weight value.");
      if (comp > 0) {
        break;
      }
    }
    final String id = newPane.getId();
    myId2Pane.put(id, newPane);
    String[] subIds = newPane.getSubIds();
    subIds = subIds.length == 0 ? new String[]{null} : subIds;
    boolean first = true;
    for (String subId : subIds) {
      final String title = subId != null ? newPane.getPresentableSubIdName(subId) : newPane.getTitle();
      final Content content = getContentManager().getFactory().createContent(getComponent(), title, false);
      content.setTabName(title);
      content.putUserData(ID_KEY, id);
      content.putUserData(SUB_ID_KEY, subId);
      content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
      Image icon = subId != null ? newPane.getPresentableSubIdIcon(subId) : newPane.getIcon();
      content.setIcon(icon);
      content.setPopupIcon(subId != null ? AllIcons.General.Bullet : newPane.getIcon());
      content.setPreferredFocusedComponent(() -> {
        final AbstractProjectViewPane current = getCurrentProjectViewPane();
        return current != null ? current.getComponentToFocus() : null;
      });
      content.setBusyObject(this);
      if (first && subId != null) {
        content.setSeparator(newPane.getTitle());
      }
      manager.addContent(content, index++);
      first = false;
    }
  }

  private void showPane(@Nonnull AbstractProjectViewPane newPane) {
    AbstractProjectViewPane currentPane = getCurrentProjectViewPane();
    PsiElement selectedPsiElement = null;
    if (currentPane != null) {
      if (currentPane != newPane) {
        currentPane.saveExpandedPaths();
      }
      final PsiElement[] elements = currentPane.getSelectedPSIElements();
      if (elements.length > 0) {
        selectedPsiElement = elements[0];
      }
    }
    myViewContentPanel.removeAll();
    JComponent component = newPane.createComponent();
    UIUtil.removeScrollBorder(component);
    myViewContentPanel.setLayout(new BorderLayout());
    myViewContentPanel.add(component, BorderLayout.CENTER);
    myCurrentViewId = newPane.getId();
    String newSubId = myCurrentViewSubId = newPane.getSubId();
    myViewContentPanel.revalidate();
    myViewContentPanel.repaint();
    createToolbarActions();

    myAutoScrollToSourceHandler.install(newPane.myTree);

    IdeFocusManager.getInstance(myProject).requestFocusInProject(newPane.getComponentToFocus(), myProject);

    newPane.restoreExpandedPaths();
    if (selectedPsiElement != null && newSubId != null) {
      final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(selectedPsiElement);
      ProjectViewSelectInTarget target = virtualFile == null ? null : getProjectViewSelectInTarget(newPane);
      if (target != null && target.isSubIdSelectable(newSubId, new SelectInContext() {
        @Override
        @Nonnull
        public Project getProject() {
          return myProject;
        }

        @Override
        @Nonnull
        public VirtualFile getVirtualFile() {
          return virtualFile;
        }

        @Override
        public Object getSelectorInFile() {
          return null;
        }
      })) {
        newPane.select(selectedPsiElement, virtualFile, true);
      }
    }
    myAutoScrollToSourceHandler.onMouseClicked(newPane.myTree);
  }

  @RequiredUIAccess
  @Override
  public void setupToolWindow(@Nonnull ToolWindow toolWindow, final boolean loadPaneExtensions) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myActionGroup = new DefaultActionGroup();

    myAutoScrollFromSourceHandler.install();

    myContentManager = toolWindow.getContentManager();
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      toolWindow.setDefaultContentUiType(ToolWindowContentUiType.COMBO);
      ((ToolWindowEx)toolWindow).setAdditionalGearActions(myActionGroup);
      toolWindow.getComponent().putClientProperty(ToolWindowContentUI.HIDE_ID_LABEL, "true");
    }

    GuiUtils.replaceJSplitPaneWithIDEASplitter(myPanel);
    SwingUtilities.invokeLater(() -> splitterProportions.restoreSplitterProportions(myPanel));

    if (loadPaneExtensions) {
      ensurePanesLoaded();
    }
    isInitialized = true;
    doAddUninitializedPanes();

    getContentManager().addContentManagerListener(new ContentManagerAdapter() {
      @Override
      public void selectionChanged(ContentManagerEvent event) {
        if (event.getOperation() == ContentManagerEvent.ContentOperation.add) {
          viewSelectionChanged();
        }
      }
    });
    viewSelectionChanged();
  }

  private void ensurePanesLoaded() {
    if (myExtensionsLoaded) return;
    myExtensionsLoaded = true;
    AbstractProjectViewPane[] extensions = Extensions.getExtensions(AbstractProjectViewPane.EP_NAME, myProject);
    Arrays.sort(extensions, PANE_WEIGHT_COMPARATOR);
    for (AbstractProjectViewPane pane : extensions) {
      if (myUninitializedPaneState.containsKey(pane.getId())) {
        try {
          pane.readExternal(myUninitializedPaneState.get(pane.getId()));
        }
        catch (InvalidDataException e) {
          // ignore
        }
        myUninitializedPaneState.remove(pane.getId());
      }
      if (pane.isInitiallyVisible() && !myId2Pane.containsKey(pane.getId())) {
        addProjectPane(pane);
      }
    }
  }

  private boolean viewSelectionChanged() {
    Content content = getContentManager().getSelectedContent();
    if (content == null) return false;
    final String id = content.getUserData(ID_KEY);
    String subId = content.getUserData(SUB_ID_KEY);
    if (content.equals(Pair.create(myCurrentViewId, myCurrentViewSubId))) return false;
    final AbstractProjectViewPane newPane = getProjectViewPaneById(id);
    if (newPane == null) return false;
    newPane.setSubId(subId);
    showPane(newPane);
    ProjectViewSelectInTarget target = getProjectViewSelectInTarget(newPane);
    if (target != null) target.setSubId(subId);
    if (isAutoscrollFromSource(id)) {
      myAutoScrollFromSourceHandler.scrollFromSource();
    }
    return true;
  }

  private void createToolbarActions() {
    if (myActionGroup == null) return;
    List<AnAction> titleActions = ContainerUtil.newSmartList();
    myActionGroup.removeAll();
    myActionGroup.addAction(new PaneOptionAction(myFlattenPackages, IdeBundle.message("action.flatten.packages"), IdeBundle.message("action.flatten.packages"),
                                                 AllIcons.ObjectBrowser.FlattenPackages, ourFlattenPackagesDefaults) {
      @Override
      public void setSelected(AnActionEvent event, boolean flag) {
        final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
        final SelectionInfo selectionInfo = SelectionInfo.create(viewPane);
        if (isGlobalOptions()) {
          setFlattenPackages(flag, viewPane.getId());
        }
        super.setSelected(event, flag);

        selectionInfo.apply(viewPane);
      }

      @Override
      public boolean isSelected(AnActionEvent event) {
        if (isGlobalOptions()) return getGlobalOptions().getFlattenPackages();
        return super.isSelected(event);
      }

      @Override
      @RequiredUIAccess
      public void update(AnActionEvent e) {
        super.update(e);
        Project project = e.getProject();
        assert project != null;
        final Presentation presentation = e.getPresentation();
        if (!PsiPackageSupportProviders.isPackageSupported(project)) {
          presentation.setVisible(false);
        }
      }
    }).setAsSecondary(true);

    class FlattenPackagesDependableAction extends PaneOptionAction {
      FlattenPackagesDependableAction(@Nonnull Map<String, Boolean> optionsMap,
                                      @Nonnull String text,
                                      @Nonnull String description,
                                      @Nonnull Image icon,
                                      boolean optionDefaultValue) {
        super(optionsMap, text, description, icon, optionDefaultValue);
      }

      @Override
      public void setSelected(AnActionEvent event, boolean flag) {
        if (isGlobalOptions()) {
          getGlobalOptions().setFlattenPackages(flag);
        }
        super.setSelected(event, flag);
      }

      @RequiredUIAccess
      @Override
      public void update(AnActionEvent e) {
        super.update(e);
        Project project = e.getProject();
        assert project != null;
        final Presentation presentation = e.getPresentation();
        if (!PsiPackageSupportProviders.isPackageSupported(project)) {
          presentation.setVisible(false);
        }
        else {
          presentation.setVisible(isFlattenPackages(myCurrentViewId));
        }
      }
    }
    myActionGroup.addAction(new HideEmptyMiddlePackagesAction()).setAsSecondary(true);
    myActionGroup.addAction(new FlattenPackagesDependableAction(myAbbreviatePackageNames, IdeBundle.message("action.abbreviate.qualified.package.names"),
                                                                IdeBundle.message("action.abbreviate.qualified.package.names"),
                                                                AllIcons.ObjectBrowser.AbbreviatePackageNames, ourAbbreviatePackagesDefaults) {
      @Override
      public boolean isSelected(AnActionEvent event) {
        return isFlattenPackages(myCurrentViewId) && isAbbreviatePackageNames(myCurrentViewId);
      }

      @Override
      public void setSelected(AnActionEvent event, boolean flag) {
        if (isGlobalOptions()) {
          setAbbreviatePackageNames(flag, myCurrentViewId);
        }
        setPaneOption(myOptionsMap, flag, myCurrentViewId, true);
      }

      @RequiredUIAccess
      @Override
      public void update(AnActionEvent e) {
        super.update(e);
        if (ScopeViewPane.ID.equals(myCurrentViewId)) {
          e.getPresentation().setEnabled(false);
        }
      }
    }).setAsSecondary(true);
    if (isShowMembersOptionSupported()) {
      myActionGroup.addAction(new PaneOptionAction(myShowMembers, IdeBundle.message("action.show.members"), IdeBundle.message("action.show.hide.members"),
                                                   AllIcons.ObjectBrowser.ShowMembers, ourShowMembersDefaults) {
        @Override
        public boolean isSelected(AnActionEvent event) {
          if (isGlobalOptions()) return getGlobalOptions().getShowMembers();
          return super.isSelected(event);
        }

        @Override
        public void setSelected(AnActionEvent event, boolean flag) {
          if (isGlobalOptions()) {
            getGlobalOptions().setShowMembers(flag);
          }
          super.setSelected(event, flag);
        }
      }).setAsSecondary(true);
    }
    myActionGroup.addAction(myAutoScrollToSourceHandler.createToggleAction()).setAsSecondary(true);
    myActionGroup.addAction(myAutoScrollFromSourceHandler.createToggleAction()).setAsSecondary(true);
    myActionGroup.addAction(new ManualOrderAction()).setAsSecondary(true);
    myActionGroup.addAction(new SortByTypeAction()).setAsSecondary(true);
    myActionGroup.addAction(new FoldersAlwaysOnTopAction()).setAsSecondary(true);

    if (!myAutoScrollFromSourceHandler.isAutoScrollEnabled()) {
      titleActions.add(new ScrollFromSourceAction());
    }
    AnAction collapseAllAction = CommonActionsManager.getInstance().createCollapseAllAction(new TreeExpander() {
      @Override
      public void expandAll() {

      }

      @Override
      public boolean canExpand() {
        return false;
      }

      @Override
      public void collapseAll() {
        AbstractProjectViewPane pane = getCurrentProjectViewPane();
        JTree tree = pane.myTree;
        if (tree != null) {
          TreeUtil.collapseAll(tree, 0);
        }
      }

      @Override
      public boolean canCollapse() {
        return true;
      }
    }, getComponent());

    collapseAllAction.getTemplatePresentation().setIcon(AllIcons.General.CollapseAll);
    titleActions.add(collapseAllAction);

    getCurrentProjectViewPane().addToolbarActionsImpl(myActionGroup);

    ToolWindowEx window = (ToolWindowEx)ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.PROJECT_VIEW);
    if (window != null) {
      window.setTitleActions(titleActions.toArray(new AnAction[titleActions.size()]));
    }
  }

  protected boolean isShowMembersOptionSupported() {
    return true;
  }

  @Override
  public AbstractProjectViewPane getProjectViewPaneById(String id) {
    if (!ApplicationManager.getApplication().isUnitTestMode()) {   // most tests don't need all panes to be loaded
      ensurePanesLoaded();
    }

    final AbstractProjectViewPane pane = myId2Pane.get(id);
    if (pane != null) {
      return pane;
    }
    for (AbstractProjectViewPane viewPane : myUninitializedPanes) {
      if (viewPane.getId().equals(id)) {
        return viewPane;
      }
    }
    return null;
  }

  @Override
  public AbstractProjectViewPane getCurrentProjectViewPane() {
    return getProjectViewPaneById(myCurrentViewId);
  }

  @Override
  public void refresh() {
    AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();
    if (currentProjectViewPane != null) {
      // may be null for e.g. default project
      currentProjectViewPane.updateFromRoot(false);
    }
  }

  @Override
  public void dispose() {
    myConnection.disconnect();
  }

  @Override
  public JComponent getComponent() {
    return myDataProvider;
  }

  @Override
  public String getCurrentViewId() {
    return myCurrentViewId;
  }

  private SelectInTarget getCurrentSelectInTarget() {
    return getSelectInTarget(getCurrentViewId());
  }

  private SelectInTarget getSelectInTarget(String id) {
    return mySelectInTargets.get(id);
  }

  private ProjectViewSelectInTarget getProjectViewSelectInTarget(AbstractProjectViewPane pane) {
    SelectInTarget target = getSelectInTarget(pane.getId());
    return target instanceof ProjectViewSelectInTarget ? (ProjectViewSelectInTarget)target : null;
  }

  @Override
  public PsiElement getParentOfCurrentSelection() {
    final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
    if (viewPane == null) {
      return null;
    }
    TreePath path = viewPane.getSelectedPath();
    if (path == null) {
      return null;
    }
    path = path.getParentPath();
    if (path == null) {
      return null;
    }
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
    Object userObject = node.getUserObject();
    if (userObject instanceof ProjectViewNode) {
      ProjectViewNode descriptor = (ProjectViewNode)userObject;
      Object element = descriptor.getValue();
      if (element instanceof PsiElement) {
        PsiElement psiElement = (PsiElement)element;
        if (!psiElement.isValid()) return null;
        return psiElement;
      }
      else {
        return null;
      }
    }
    return null;
  }

  public ContentManager getContentManager() {
    if (myContentManager == null) {
      ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.PROJECT_VIEW).getContentManager();
    }
    return myContentManager;
  }


  private class PaneOptionAction extends ToggleAction implements DumbAware {
    final Map<String, Boolean> myOptionsMap;
    private final boolean myOptionDefaultValue;

    PaneOptionAction(@Nonnull Map<String, Boolean> optionsMap, @Nonnull String text, @Nonnull String description, Image icon, boolean optionDefaultValue) {
      super(text, description, icon);
      myOptionsMap = optionsMap;
      myOptionDefaultValue = optionDefaultValue;
    }

    @Override
    public boolean isSelected(AnActionEvent event) {
      return getPaneOptionValue(myOptionsMap, myCurrentViewId, myOptionDefaultValue);
    }

    @Override
    public void setSelected(AnActionEvent event, boolean flag) {
      setPaneOption(myOptionsMap, flag, myCurrentViewId, true);
    }
  }

  @Override
  public void changeView() {
    final List<AbstractProjectViewPane> views = new ArrayList<>(myId2Pane.values());
    views.remove(getCurrentProjectViewPane());
    Collections.sort(views, PANE_WEIGHT_COMPARATOR);

    final JList list = new JBList(ArrayUtil.toObjectArray(views));
    list.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        AbstractProjectViewPane pane = (AbstractProjectViewPane)value;
        setText(pane.getTitle());
        return this;
      }
    });

    if (!views.isEmpty()) {
      list.setSelectedValue(views.get(0), true);
    }
    Runnable runnable = () -> {
      if (list.getSelectedIndex() < 0) return;
      AbstractProjectViewPane pane = (AbstractProjectViewPane)list.getSelectedValue();
      changeView(pane.getId());
    };

    new PopupChooserBuilder(list).
            setTitle(IdeBundle.message("title.popup.views")).
            setItemChoosenCallback(runnable).
            createPopup().showInCenterOf(getComponent());
  }

  @Override
  public void changeView(@Nonnull String viewId) {
    changeView(viewId, null);
  }

  @Override
  public void changeView(@Nonnull String viewId, @Nullable String subId) {
    changeViewCB(viewId, subId);
  }

  @Nonnull
  @Override
  public AsyncResult<Void> changeViewCB(@Nonnull String viewId, String subId) {
    AbstractProjectViewPane pane = getProjectViewPaneById(viewId);
    LOG.assertTrue(pane != null, "Project view pane not found: " + viewId + "; subId:" + subId + "; project: " + myProject);
    if (!viewId.equals(getCurrentViewId()) || subId != null && !subId.equals(pane.getSubId())) {
      for (Content content : getContentManager().getContents()) {
        if (viewId.equals(content.getUserData(ID_KEY)) && StringUtil.equals(subId, content.getUserData(SUB_ID_KEY))) {
          return getContentManager().setSelectedContentCB(content);
        }
      }
    }
    return AsyncResult.rejected();
  }

  private final class MyDeletePSIElementProvider implements DeleteProvider {
    @Override
    public boolean canDeleteElement(@Nonnull DataContext dataContext) {
      final PsiElement[] elements = getElementsToDelete();
      return DeleteHandler.shouldEnableDeleteAction(elements);
    }

    @Override
    public void deleteElement(@Nonnull DataContext dataContext) {
      List<PsiElement> allElements = Arrays.asList(getElementsToDelete());
      List<PsiElement> validElements = new ArrayList<>();
      for (PsiElement psiElement : allElements) {
        if (psiElement != null && psiElement.isValid()) validElements.add(psiElement);
      }
      final PsiElement[] elements = PsiUtilCore.toPsiElementArray(validElements);

      LocalHistoryAction a = LocalHistory.getInstance().startAction(IdeBundle.message("progress.deleting"));
      try {
        DeleteHandler.deletePsiElement(elements, myProject);
      }
      finally {
        a.finish();
      }
    }

    @Nonnull
    private PsiElement[] getElementsToDelete() {
      final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
      PsiElement[] elements = viewPane.getSelectedPSIElements();
      for (int idx = 0; idx < elements.length; idx++) {
        final PsiElement element = elements[idx];
        if (element instanceof PsiDirectory) {
          PsiDirectory directory = (PsiDirectory)element;
          if (isHideEmptyMiddlePackages(viewPane.getId()) && directory.getChildren().length == 0 && !BaseProjectViewDirectoryHelper.skipDirectory(directory)) {
            while (true) {
              PsiDirectory parent = directory.getParentDirectory();
              if (parent == null) break;
              if (BaseProjectViewDirectoryHelper.skipDirectory(parent) ||
                  PsiPackageHelper.getInstance(myProject).getQualifiedName(parent, false).length() == 0) {
                break;
              }
              PsiElement[] children = parent.getChildren();
              if (children.length == 0 || children.length == 1 && children[0] == directory) {
                directory = parent;
              }
              else {
                break;
              }
            }
            elements[idx] = directory;
          }
          final VirtualFile virtualFile = directory.getVirtualFile();
          final String path = virtualFile.getPath();
          if (path.endsWith(URLUtil.ARCHIVE_SEPARATOR)) {
            final VirtualFile vFile = LocalFileSystem.getInstance().findFileByPath(path.substring(0, path.length() - URLUtil.ARCHIVE_SEPARATOR.length()));
            if (vFile != null) {
              final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(vFile);
              if (psiFile != null) {
                elements[idx] = psiFile;
              }
            }
          }
        }
      }
      return elements;
    }

  }

  private final class MyPanel extends JPanel implements DataProvider {
    MyPanel() {
      super(new BorderLayout());
    }

    @Nullable
    private Object getSelectedNodeElement() {
      final AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();
      if (currentProjectViewPane == null) { // can happen if not initialized yet
        return null;
      }
      DefaultMutableTreeNode node = currentProjectViewPane.getSelectedNode();
      if (node == null) {
        return null;
      }
      Object userObject = node.getUserObject();
      if (userObject instanceof AbstractTreeNode) {
        return ((AbstractTreeNode)userObject).getValue();
      }
      if (!(userObject instanceof NodeDescriptor)) {
        return null;
      }
      return ((NodeDescriptor)userObject).getElement();
    }

    @Override
    public Object getData(@Nonnull Key<?> dataId) {
      final AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();
      if (currentProjectViewPane != null) {
        final Object paneSpecificData = currentProjectViewPane.getData(dataId);
        if (paneSpecificData != null) return paneSpecificData;
      }

      if (CommonDataKeys.PSI_ELEMENT == dataId) {
        if (currentProjectViewPane == null) return null;
        final PsiElement[] elements = currentProjectViewPane.getSelectedPSIElements();
        return elements.length == 1 ? elements[0] : null;
      }
      if (LangDataKeys.PSI_ELEMENT_ARRAY == dataId) {
        if (currentProjectViewPane == null) {
          return null;
        }
        PsiElement[] elements = currentProjectViewPane.getSelectedPSIElements();
        return elements.length == 0 ? null : elements;
      }
      if (LangDataKeys.MODULE == dataId) {
        VirtualFile[] virtualFiles = getDataUnchecked(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (virtualFiles == null || virtualFiles.length <= 1) return null;
        final Set<Module> modules = new HashSet<>();
        for (VirtualFile virtualFile : virtualFiles) {
          modules.add(ModuleUtilCore.findModuleForFile(virtualFile, myProject));
        }
        return modules.size() == 1 ? modules.iterator().next() : null;
      }
      if (LangDataKeys.TARGET_PSI_ELEMENT == dataId) {
        return null;
      }
      if (PlatformDataKeys.CUT_PROVIDER == dataId) {
        return myCopyPasteDelegator.getCutProvider();
      }
      if (PlatformDataKeys.COPY_PROVIDER == dataId) {
        return myCopyPasteDelegator.getCopyProvider();
      }
      if (PlatformDataKeys.PASTE_PROVIDER == dataId) {
        return myCopyPasteDelegator.getPasteProvider();
      }
      if (LangDataKeys.IDE_VIEW == dataId) {
        return myIdeView;
      }
      if (PlatformDataKeys.DELETE_ELEMENT_PROVIDER == dataId) {
        final Module[] modules = getSelectedModules();
        if (modules != null) {
          return myDeleteModuleProvider;
        }
        final LibraryOrderEntry orderEntry = getSelectedLibrary();
        if (orderEntry != null) {
          return new DeleteProvider() {
            @Override
            public void deleteElement(@Nonnull DataContext dataContext) {
              detachLibrary(orderEntry, myProject);
            }

            @Override
            public boolean canDeleteElement(@Nonnull DataContext dataContext) {
              return true;
            }
          };
        }
        return myDeletePSIElementProvider;
      }
      if (PlatformDataKeys.HELP_ID == dataId) {
        return HelpID.PROJECT_VIEWS;
      }
      if (ProjectViewImpl.DATA_KEY == dataId) {
        return ProjectViewImpl.this;
      }
      if (PlatformDataKeys.PROJECT_CONTEXT == dataId) {
        Object selected = getSelectedNodeElement();
        return selected instanceof Project ? selected : null;
      }
      if (LangDataKeys.MODULE_CONTEXT == dataId) {
        Object selected = getSelectedNodeElement();
        if (selected instanceof Module) {
          return !((Module)selected).isDisposed() ? selected : null;
        }
        else if (selected instanceof PsiDirectory) {
          return moduleBySingleContentRoot(((PsiDirectory)selected).getVirtualFile());
        }
        else if (selected instanceof VirtualFile) {
          return moduleBySingleContentRoot((VirtualFile)selected);
        }
        else {
          return null;
        }
      }

      if (LangDataKeys.MODULE_CONTEXT_ARRAY == dataId) {
        return getSelectedModules();
      }
      if (ModuleGroup.ARRAY_DATA_KEY == dataId) {
        final List<ModuleGroup> selectedElements = getSelectedElements(ModuleGroup.class);
        return selectedElements.isEmpty() ? null : selectedElements.toArray(new ModuleGroup[selectedElements.size()]);
      }
      if (LibraryGroupElement.ARRAY_DATA_KEY == dataId) {
        final List<LibraryGroupElement> selectedElements = getSelectedElements(LibraryGroupElement.class);
        return selectedElements.isEmpty() ? null : selectedElements.toArray(new LibraryGroupElement[selectedElements.size()]);
      }
      if (NamedLibraryElement.ARRAY_DATA_KEY == dataId) {
        final List<NamedLibraryElement> selectedElements = getSelectedElements(NamedLibraryElement.class);
        return selectedElements.isEmpty() ? null : selectedElements.toArray(new NamedLibraryElement[selectedElements.size()]);
      }

      if (QuickActionProvider.KEY == dataId) {
        return ProjectViewImpl.this;
      }

      return null;
    }

    @Nullable
    private LibraryOrderEntry getSelectedLibrary() {
      final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
      DefaultMutableTreeNode node = viewPane != null ? viewPane.getSelectedNode() : null;
      if (node == null) return null;
      DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
      if (parent == null) return null;
      Object userObject = parent.getUserObject();
      if (userObject instanceof LibraryGroupNode) {
        userObject = node.getUserObject();
        if (userObject instanceof NamedLibraryElementNode) {
          NamedLibraryElement element = ((NamedLibraryElementNode)userObject).getValue();
          OrderEntry orderEntry = element.getOrderEntry();
          return orderEntry instanceof LibraryOrderEntry ? (LibraryOrderEntry)orderEntry : null;
        }
        PsiDirectory directory = ((PsiDirectoryNode)userObject).getValue();
        VirtualFile virtualFile = directory.getVirtualFile();
        Module module = (Module)((AbstractTreeNode)((DefaultMutableTreeNode)parent.getParent()).getUserObject()).getValue();

        if (module == null) return null;
        ModuleFileIndex index = ModuleRootManager.getInstance(module).getFileIndex();
        OrderEntry entry = index.getOrderEntryForFile(virtualFile);
        if (entry instanceof LibraryOrderEntry) {
          return (LibraryOrderEntry)entry;
        }
      }

      return null;
    }

    private void detachLibrary(@Nonnull final LibraryOrderEntry orderEntry, @Nonnull Project project) {
      final Module module = orderEntry.getOwnerModule();
      String message = IdeBundle.message("detach.library.from.module", orderEntry.getPresentableName(), module.getName());
      String title = IdeBundle.message("detach.library");
      int ret = Messages.showOkCancelDialog(project, message, title, Messages.getQuestionIcon());
      if (ret != Messages.OK) return;
      CommandProcessor.getInstance().executeCommand(module.getProject(), () -> {
        final Runnable action = () -> {
          ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
          OrderEntry[] orderEntries = rootManager.getOrderEntries();
          ModifiableRootModel model = rootManager.getModifiableModel();
          OrderEntry[] modifiableEntries = model.getOrderEntries();
          for (int i = 0; i < orderEntries.length; i++) {
            OrderEntry entry = orderEntries[i];
            if (entry instanceof LibraryOrderEntry && ((LibraryOrderEntry)entry).getLibrary() == orderEntry.getLibrary()) {
              model.removeOrderEntry(modifiableEntries[i]);
            }
          }
          model.commit();
        };
        ApplicationManager.getApplication().runWriteAction(action);
      }, title, null);
    }

    @Nullable
    private Module[] getSelectedModules() {
      final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
      if (viewPane == null) return null;
      final Object[] elements = viewPane.getSelectedElements();
      ArrayList<Module> result = new ArrayList<>();
      for (Object element : elements) {
        if (element instanceof Module) {
          final Module module = (Module)element;
          if (!module.isDisposed()) {
            result.add(module);
          }
        }
        else if (element instanceof ModuleGroup) {
          Collection<Module> modules = ((ModuleGroup)element).modulesInGroup(myProject, true);
          result.addAll(modules);
        }
        else if (element instanceof PsiDirectory) {
          Module module = moduleBySingleContentRoot(((PsiDirectory)element).getVirtualFile());
          if (module != null) result.add(module);
        }
        else if (element instanceof VirtualFile) {
          Module module = moduleBySingleContentRoot((VirtualFile)element);
          if (module != null) result.add(module);
        }
      }

      if (result.isEmpty()) {
        return null;
      }
      else {
        return result.toArray(new Module[result.size()]);
      }
    }
  }

  /**
   * Project view has the same node for module and its single content root
   * => MODULE_CONTEXT data key should return the module when its content root is selected
   * When there are multiple content roots, they have different nodes under the module node
   * => MODULE_CONTEXT should be only available for the module node
   * otherwise VirtualFileArrayRule will return all module's content roots when just one of them is selected
   */
  @Nullable
  private Module moduleBySingleContentRoot(@Nonnull VirtualFile file) {
    if (ProjectRootsUtil.isModuleContentRoot(file, myProject)) {
      Module module = ProjectRootManager.getInstance(myProject).getFileIndex().getModuleForFile(file);
      if (module != null && !module.isDisposed() && ModuleRootManager.getInstance(module).getContentRoots().length == 1) {
        return module;
      }
    }

    return null;
  }

  @Nonnull
  private <T> List<T> getSelectedElements(@Nonnull Class<T> klass) {
    List<T> result = new ArrayList<>();
    final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
    if (viewPane == null) return result;
    final Object[] elements = viewPane.getSelectedElements();
    for (Object element : elements) {
      //element still valid
      if (element != null && klass.isAssignableFrom(element.getClass())) {
        result.add((T)element);
      }
    }
    return result;
  }

  private final class MyIdeView implements IdeView {
    @Override
    public void selectElement(PsiElement element) {
      selectPsiElement(element, false);
      boolean requestFocus = true;
      if (element != null) {
        final boolean isDirectory = element instanceof PsiDirectory;
        if (!isDirectory) {
          FileEditor editor = EditorHelper.openInEditor(element, false);
          if (editor != null) {
            ToolWindowManager.getInstance(myProject).activateEditorComponent();
            requestFocus = false;
          }
        }
      }

      if (requestFocus) {
        selectPsiElement(element, true);
      }
    }

    @Nonnull
    @Override
    public PsiDirectory[] getDirectories() {
      final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
      if (viewPane != null) {
        return viewPane.getSelectedDirectories();
      }

      return PsiDirectory.EMPTY_ARRAY;
    }

    @Override
    public PsiDirectory getOrChooseDirectory() {
      return DirectoryChooserUtil.getOrChooseDirectory(this);
    }
  }

  @Override
  public void selectPsiElement(PsiElement element, boolean requestFocus) {
    if (element == null) return;
    VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
    select(element, virtualFile, requestFocus);
  }


  private static void readOption(Element node, @Nonnull Map<String, Boolean> options) {
    if (node == null) return;
    for (Attribute attribute : node.getAttributes()) {
      options.put(attribute.getName(), Boolean.TRUE.toString().equals(attribute.getValue()) ? Boolean.TRUE : Boolean.FALSE);
    }
  }

  private static void writeOption(@Nonnull Element parentNode, @Nonnull Map<String, Boolean> optionsForPanes, @Nonnull String optionName) {
    Element e = new Element(optionName);
    for (Map.Entry<String, Boolean> entry : optionsForPanes.entrySet()) {
      final String key = entry.getKey();
      if (key != null) { //SCR48267
        e.setAttribute(key, Boolean.toString(entry.getValue().booleanValue()));
      }
    }

    parentNode.addContent(e);
  }

  @Override
  public void loadState(Element parentNode) {
    Element navigatorElement = parentNode.getChild(ELEMENT_NAVIGATOR);
    if (navigatorElement != null) {
      mySavedPaneId = navigatorElement.getAttributeValue(ATTRIBUTE_CURRENT_VIEW);
      mySavedPaneSubId = navigatorElement.getAttributeValue(ATTRIBUTE_CURRENT_SUBVIEW);
      if (mySavedPaneId == null) {
        mySavedPaneId = ProjectViewPane.ID;
        mySavedPaneSubId = null;
      }
      readOption(navigatorElement.getChild(ELEMENT_FLATTEN_PACKAGES), myFlattenPackages);
      readOption(navigatorElement.getChild(ELEMENT_SHOW_MEMBERS), myShowMembers);
      readOption(navigatorElement.getChild(ELEMENT_SHOW_MODULES), myShowModules);
      readOption(navigatorElement.getChild(ELEMENT_SHOW_LIBRARY_CONTENTS), myShowLibraryContents);
      readOption(navigatorElement.getChild(ELEMENT_HIDE_EMPTY_PACKAGES), myHideEmptyPackages);
      readOption(navigatorElement.getChild(ELEMENT_ABBREVIATE_PACKAGE_NAMES), myAbbreviatePackageNames);
      readOption(navigatorElement.getChild(ELEMENT_AUTOSCROLL_TO_SOURCE), myAutoscrollToSource);
      readOption(navigatorElement.getChild(ELEMENT_AUTOSCROLL_FROM_SOURCE), myAutoscrollFromSource);
      readOption(navigatorElement.getChild(ELEMENT_SORT_BY_TYPE), mySortByType);
      readOption(navigatorElement.getChild(ELEMENT_MANUAL_ORDER), myManualOrder);

      Element foldersElement = navigatorElement.getChild(ELEMENT_FOLDERS_ALWAYS_ON_TOP);
      if (foldersElement != null) myFoldersAlwaysOnTop = Boolean.valueOf(foldersElement.getAttributeValue("value"));

      try {
        splitterProportions.readExternal(navigatorElement);
      }
      catch (InvalidDataException e) {
        // ignore
      }
    }
    Element panesElement = parentNode.getChild(ELEMENT_PANES);
    if (panesElement != null) {
      readPaneState(panesElement);
    }
  }

  private void readPaneState(@Nonnull Element panesElement) {
    @SuppressWarnings({"unchecked"}) final List<Element> paneElements = panesElement.getChildren(ELEMENT_PANE);

    for (Element paneElement : paneElements) {
      String paneId = paneElement.getAttributeValue(ATTRIBUTE_ID);
      final AbstractProjectViewPane pane = myId2Pane.get(paneId);
      if (pane != null) {
        try {
          pane.readExternal(paneElement);
        }
        catch (InvalidDataException e) {
          // ignore
        }
      }
      else {
        myUninitializedPaneState.put(paneId, paneElement);
      }
    }
  }

  @Override
  public Element getState() {
    Element parentNode = new Element("projectView");
    Element navigatorElement = new Element(ELEMENT_NAVIGATOR);
    AbstractProjectViewPane currentPane = getCurrentProjectViewPane();
    if (currentPane != null) {
      navigatorElement.setAttribute(ATTRIBUTE_CURRENT_VIEW, currentPane.getId());
      String subId = currentPane.getSubId();
      if (subId != null) {
        navigatorElement.setAttribute(ATTRIBUTE_CURRENT_SUBVIEW, subId);
      }
    }
    writeOption(navigatorElement, myFlattenPackages, ELEMENT_FLATTEN_PACKAGES);
    writeOption(navigatorElement, myShowMembers, ELEMENT_SHOW_MEMBERS);
    writeOption(navigatorElement, myShowModules, ELEMENT_SHOW_MODULES);
    writeOption(navigatorElement, myShowLibraryContents, ELEMENT_SHOW_LIBRARY_CONTENTS);
    writeOption(navigatorElement, myHideEmptyPackages, ELEMENT_HIDE_EMPTY_PACKAGES);
    writeOption(navigatorElement, myAbbreviatePackageNames, ELEMENT_ABBREVIATE_PACKAGE_NAMES);
    writeOption(navigatorElement, myAutoscrollToSource, ELEMENT_AUTOSCROLL_TO_SOURCE);
    writeOption(navigatorElement, myAutoscrollFromSource, ELEMENT_AUTOSCROLL_FROM_SOURCE);
    writeOption(navigatorElement, mySortByType, ELEMENT_SORT_BY_TYPE);
    writeOption(navigatorElement, myManualOrder, ELEMENT_MANUAL_ORDER);

    Element foldersElement = new Element(ELEMENT_FOLDERS_ALWAYS_ON_TOP);
    foldersElement.setAttribute("value", Boolean.toString(myFoldersAlwaysOnTop));
    navigatorElement.addContent(foldersElement);

    splitterProportions.saveSplitterProportions(myPanel);
    try {
      splitterProportions.writeExternal(navigatorElement);
    }
    catch (WriteExternalException e) {
      // ignore
    }
    parentNode.addContent(navigatorElement);

    Element panesElement = new Element(ELEMENT_PANES);
    writePaneState(panesElement);
    parentNode.addContent(panesElement);
    return parentNode;
  }

  private void writePaneState(@Nonnull Element panesElement) {
    for (AbstractProjectViewPane pane : myId2Pane.values()) {
      Element paneElement = new Element(ELEMENT_PANE);
      paneElement.setAttribute(ATTRIBUTE_ID, pane.getId());
      try {
        pane.writeExternal(paneElement);
      }
      catch (WriteExternalException e) {
        continue;
      }
      panesElement.addContent(paneElement);
    }
    for (Element element : myUninitializedPaneState.values()) {
      panesElement.addContent(element.clone());
    }
  }

  boolean isGlobalOptions() {
    return Registry.is("ide.projectView.globalOptions");
  }

  public ProjectViewSharedSettings getGlobalOptions() {
    return myProjectViewSharedSettings;
  }

  @Override
  public boolean isAutoscrollToSource(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getAutoscrollToSource();
    }

    return getPaneOptionValue(myAutoscrollToSource, paneId, UISettings.getInstance().DEFAULT_AUTOSCROLL_TO_SOURCE);
  }

  public void setAutoscrollToSource(boolean autoscrollMode, String paneId) {
    if (isGlobalOptions()) {
      getGlobalOptions().setAutoscrollToSource(autoscrollMode);
    }
    myAutoscrollToSource.put(paneId, autoscrollMode);
  }

  @Override
  public boolean isAutoscrollFromSource(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getAutoscrollFromSource();
    }

    return getPaneOptionValue(myAutoscrollFromSource, paneId, ourAutoscrollFromSourceDefaults);
  }

  public void setAutoscrollFromSource(boolean autoscrollMode, String paneId) {
    if (isGlobalOptions()) {
      getGlobalOptions().setAutoscrollFromSource(autoscrollMode);
    }
    setPaneOption(myAutoscrollFromSource, autoscrollMode, paneId, false);
  }

  @Override
  public boolean isFlattenPackages(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getFlattenPackages();
    }

    return getPaneOptionValue(myFlattenPackages, paneId, ourFlattenPackagesDefaults);
  }

  public void setFlattenPackages(boolean flattenPackages, String paneId) {
    if (isGlobalOptions()) {
      getGlobalOptions().setFlattenPackages(flattenPackages);
      for (String pane : myFlattenPackages.keySet()) {
        setPaneOption(myFlattenPackages, flattenPackages, pane, true);
      }
    }
    setPaneOption(myFlattenPackages, flattenPackages, paneId, true);
  }

  public boolean isFoldersAlwaysOnTop() {
    if (isGlobalOptions()) {
      return getGlobalOptions().getFoldersAlwaysOnTop();
    }

    return myFoldersAlwaysOnTop;
  }

  public void setFoldersAlwaysOnTop(boolean foldersAlwaysOnTop) {
    if (isGlobalOptions()) {
      getGlobalOptions().setFoldersAlwaysOnTop(foldersAlwaysOnTop);
    }

    if (myFoldersAlwaysOnTop != foldersAlwaysOnTop) {
      myFoldersAlwaysOnTop = foldersAlwaysOnTop;
      for (AbstractProjectViewPane pane : myId2Pane.values()) {
        if (pane.getTree() != null) {
          pane.updateFromRoot(false);
        }
      }
    }
  }

  @Override
  public boolean isShowMembers(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getShowMembers();
    }

    return getPaneOptionValue(myShowMembers, paneId, ourShowMembersDefaults);
  }

  public void setShowMembers(boolean showMembers, String paneId) {
    setPaneOption(myShowMembers, showMembers, paneId, true);
  }

  @Override
  public boolean isHideEmptyMiddlePackages(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getHideEmptyPackages();
    }

    return getPaneOptionValue(myHideEmptyPackages, paneId, ourHideEmptyPackagesDefaults);
  }

  @Override
  public boolean isAbbreviatePackageNames(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getAbbreviatePackages();
    }

    return getPaneOptionValue(myAbbreviatePackageNames, paneId, ourAbbreviatePackagesDefaults);
  }

  @Override
  public boolean isShowLibraryContents(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getShowLibraryContents();
    }

    return getPaneOptionValue(myShowLibraryContents, paneId, ourShowLibraryContentsDefaults);
  }

  @Override
  public void setShowLibraryContents(boolean showLibraryContents, @Nonnull String paneId) {
    if (isGlobalOptions()) {
      getGlobalOptions().setShowLibraryContents(showLibraryContents);
    }
    setPaneOption(myShowLibraryContents, showLibraryContents, paneId, true);
  }

  @Nonnull
  public ActionCallback setShowLibraryContentsCB(boolean showLibraryContents, String paneId) {
    return setPaneOption(myShowLibraryContents, showLibraryContents, paneId, true);
  }

  @Override
  public boolean isShowModules(String paneId) {
    if (isGlobalOptions()) {
      return getGlobalOptions().getShowModules();
    }

    return getPaneOptionValue(myShowModules, paneId, ourShowModulesDefaults);
  }

  @Override
  public void setShowModules(boolean showModules, @Nonnull String paneId) {
    if (isGlobalOptions()) {
      getGlobalOptions().setShowModules(showModules);
    }
    setPaneOption(myShowModules, showModules, paneId, true);
  }

  @Override
  public void setHideEmptyPackages(boolean hideEmptyPackages, @Nonnull String paneId) {
    if (isGlobalOptions()) {
      getGlobalOptions().setHideEmptyPackages(hideEmptyPackages);
      for (String pane : myHideEmptyPackages.keySet()) {
        setPaneOption(myHideEmptyPackages, hideEmptyPackages, pane, true);
      }
    }
    setPaneOption(myHideEmptyPackages, hideEmptyPackages, paneId, true);
  }

  @Override
  public void setAbbreviatePackageNames(boolean abbreviatePackageNames, @Nonnull String paneId) {
    if (isGlobalOptions()) {
      getGlobalOptions().setAbbreviatePackages(abbreviatePackageNames);
    }
    setPaneOption(myAbbreviatePackageNames, abbreviatePackageNames, paneId, true);
  }

  @Nonnull
  private ActionCallback setPaneOption(@Nonnull Map<String, Boolean> optionsMap, boolean value, String paneId, final boolean updatePane) {
    if (paneId != null) {
      optionsMap.put(paneId, value);
      if (updatePane) {
        final AbstractProjectViewPane pane = getProjectViewPaneById(paneId);
        if (pane != null) {
          return pane.updateFromRoot(false);
        }
      }
    }
    return ActionCallback.DONE;
  }

  private static boolean getPaneOptionValue(@Nonnull Map<String, Boolean> optionsMap, String paneId, boolean defaultValue) {
    final Boolean value = optionsMap.get(paneId);
    return value == null ? defaultValue : value.booleanValue();
  }

  private class HideEmptyMiddlePackagesAction extends PaneOptionAction {
    private HideEmptyMiddlePackagesAction() {
      super(myHideEmptyPackages, "", "", null, ourHideEmptyPackagesDefaults);
    }

    @Override
    public void setSelected(AnActionEvent event, boolean flag) {
      final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
      final SelectionInfo selectionInfo = SelectionInfo.create(viewPane);

      if (isGlobalOptions()) {
        getGlobalOptions().setHideEmptyPackages(flag);
      }
      super.setSelected(event, flag);

      selectionInfo.apply(viewPane);
    }

    @Override
    public boolean isSelected(AnActionEvent event) {
      if (isGlobalOptions()) return getGlobalOptions().getHideEmptyPackages();
      return super.isSelected(event);
    }

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent e) {
      super.update(e);
      final Presentation presentation = e.getPresentation();
      Project project = e.getProject();
      assert project != null;
      if (!PsiPackageSupportProviders.isPackageSupported(project)) {
        presentation.setVisible(false);
        return;
      }
      if (isHideEmptyMiddlePackages(myCurrentViewId)) {
        presentation.setText(IdeBundle.message("action.hide.empty.middle.packages"));
        presentation.setDescription(IdeBundle.message("action.show.hide.empty.middle.packages"));
      }
      else {
        presentation.setText(IdeBundle.message("action.compact.empty.middle.packages"));
        presentation.setDescription(IdeBundle.message("action.show.compact.empty.middle.packages"));
      }
    }
  }

  private static class SelectionInfo {
    @Nonnull
    private final Object[] myElements;

    private SelectionInfo(@Nonnull Object[] elements) {
      myElements = elements;
    }

    public void apply(final AbstractProjectViewPane viewPane) {
      if (viewPane == null) {
        return;
      }
      AbstractTreeBuilder treeBuilder = viewPane.getTreeBuilder();
      JTree tree = viewPane.myTree;
      if (treeBuilder != null) {
        DefaultTreeModel treeModel = (DefaultTreeModel)tree.getModel();
        List<TreePath> paths = new ArrayList<>(myElements.length);
        for (final Object element : myElements) {
          DefaultMutableTreeNode node = treeBuilder.getNodeForElement(element);
          if (node == null) {
            treeBuilder.buildNodeForElement(element);
            node = treeBuilder.getNodeForElement(element);
          }
          if (node != null) {
            paths.add(new TreePath(treeModel.getPathToRoot(node)));
          }
        }
        if (!paths.isEmpty()) {
          tree.setSelectionPaths(paths.toArray(new TreePath[0]));
        }
      }
      else {
        List<TreeVisitor> visitors = AbstractProjectViewPane.createVisitors(myElements);
        if (1 == visitors.size()) {
          TreeUtil.promiseSelect(tree, visitors.get(0));
        }
        else if (!visitors.isEmpty()) {
          TreeUtil.promiseSelect(tree, visitors.stream());
        }
      }
    }

    @Nonnull
    public static SelectionInfo create(final AbstractProjectViewPane viewPane) {
      List<Object> selectedElements = Collections.emptyList();
      if (viewPane != null) {
        final TreePath[] selectionPaths = viewPane.getSelectionPaths();
        if (selectionPaths != null) {
          selectedElements = new ArrayList<>();
          for (TreePath path : selectionPaths) {
            NodeDescriptor descriptor = TreeUtil.getLastUserObject(NodeDescriptor.class, path);
            if (descriptor != null) selectedElements.add(descriptor.getElement());
          }
        }
      }
      return new SelectionInfo(selectedElements.toArray());
    }
  }

  private class MyAutoScrollFromSourceHandler extends AutoScrollFromSourceHandler {
    private MyAutoScrollFromSourceHandler() {
      super(ProjectViewImpl.this.myProject, myViewContentPanel, ProjectViewImpl.this);
    }

    @Override
    protected void selectElementFromEditor(@Nonnull FileEditor fileEditor) {
      if (myProject.isDisposed() || !myViewContentPanel.isShowing()) return;
      if (isAutoscrollFromSource(getCurrentViewId())) {
        if (fileEditor instanceof TextEditor) {
          Editor editor = ((TextEditor)fileEditor).getEditor();
          selectElementAtCaretNotLosingFocus(editor);
        }
        else {
          SelectInTarget target = getCurrentSelectInTarget();
          if (target != null) {
            final VirtualFile file = FileEditorManagerEx.getInstanceEx(myProject).getFile(fileEditor);
            if (file != null && file.isValid()) {
              final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
              if (psiFile != null) {
                final MySelectInContext selectInContext = new MySelectInContext(psiFile, null) {
                  @Override
                  public Object getSelectorInFile() {
                    return psiFile;
                  }
                };

                if (target.canSelect(selectInContext)) {
                  target.selectIn(selectInContext, false);
                }
              }
            }
          }
        }
      }
    }

    public void scrollFromSource() {
      final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
      final Editor selectedTextEditor = fileEditorManager.getSelectedTextEditor();
      if (selectedTextEditor != null) {
        selectElementAtCaret(selectedTextEditor);
        return;
      }
      final FileEditor[] editors = fileEditorManager.getSelectedEditors();
      for (FileEditor fileEditor : editors) {
        if (fileEditor instanceof TextEditor) {
          Editor editor = ((TextEditor)fileEditor).getEditor();
          selectElementAtCaret(editor);
          return;
        }
      }
      final VirtualFile[] selectedFiles = fileEditorManager.getSelectedFiles();
      if (selectedFiles.length > 0) {
        final PsiFile file = PsiManager.getInstance(myProject).findFile(selectedFiles[0]);
        if (file != null) {
          scrollFromFile(file, null);
        }
      }
    }

    private void selectElementAtCaretNotLosingFocus(@Nonnull Editor editor) {
      AbstractProjectViewPane pane = getCurrentProjectViewPane();
      if (pane != null && !IJSwingUtilities.hasFocus(pane.getComponentToFocus())) {
        selectElementAtCaret(editor);
      }
    }

    private void selectElementAtCaret(@Nonnull Editor editor) {
      final PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
      if (file == null) return;

      scrollFromFile(file, editor);
    }

    private void scrollFromFile(@Nonnull PsiFile file, @Nullable Editor editor) {
      SmartPsiElementPointer<PsiFile> pointer = SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(file);
      PsiDocumentManager.getInstance(myProject).performLaterWhenAllCommitted(() -> {
        SelectInTarget target = getCurrentSelectInTarget();
        if (target == null) return;

        PsiFile restoredPsi = pointer.getElement();
        if (restoredPsi == null) return;

        final MySelectInContext selectInContext = new MySelectInContext(restoredPsi, editor);

        if (target.canSelect(selectInContext)) {
          target.selectIn(selectInContext, false);
        }
      });
    }

    @Override
    protected boolean isAutoScrollEnabled() {
      return isAutoscrollFromSource(myCurrentViewId);
    }

    @Override
    protected void setAutoScrollEnabled(boolean state) {
      setAutoscrollFromSource(state, myCurrentViewId);
      if (state) {
        final Editor editor = myFileEditorManager.getSelectedTextEditor();
        if (editor != null) {
          selectElementAtCaretNotLosingFocus(editor);
        }
      }
      createToolbarActions();
    }

    private class MySelectInContext implements SelectInContext {
      @Nonnull
      private final PsiFile myPsiFile;
      @Nullable
      private final Editor myEditor;

      private MySelectInContext(@Nonnull PsiFile psiFile, @Nullable Editor editor) {
        myPsiFile = psiFile;
        myEditor = editor;
      }

      @Override
      @Nonnull
      public Project getProject() {
        return myProject;
      }

      @Nonnull
      private PsiFile getPsiFile() {
        return myPsiFile;
      }

      @Override
      @Nonnull
      public Supplier<FileEditor> getFileEditorProvider() {
        return () -> myFileEditorManager.openFile(myPsiFile.getContainingFile().getVirtualFile(), false)[0];
      }

      @Nonnull
      private PsiElement getPsiElement() {
        PsiElement e = null;
        if (myEditor != null) {
          final int offset = myEditor.getCaretModel().getOffset();
          if (PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments()) {
            PsiDocumentManager.getInstance(myProject).commitAllDocuments();
          }
          e = getPsiFile().findElementAt(offset);
        }
        if (e == null) {
          e = getPsiFile();
        }
        return e;
      }

      @Override
      @Nonnull
      public VirtualFile getVirtualFile() {
        return getPsiFile().getVirtualFile();
      }

      @Override
      public Object getSelectorInFile() {
        return getPsiElement();
      }
    }
  }

  @Override
  public boolean isManualOrder(String paneId) {
    return getPaneOptionValue(myManualOrder, paneId, ourManualOrderDefaults);
  }

  @Override
  public void setManualOrder(@Nonnull String paneId, final boolean enabled) {
    setPaneOption(myManualOrder, enabled, paneId, false);
    final AbstractProjectViewPane pane = getProjectViewPaneById(paneId);
    pane.installComparator();
  }

  @Override
  public boolean isSortByType(String paneId) {
    return getPaneOptionValue(mySortByType, paneId, ourSortByTypeDefaults);
  }

  @Override
  public void setSortByType(@Nonnull String paneId, final boolean sortByType) {
    setPaneOption(mySortByType, sortByType, paneId, false);
    final AbstractProjectViewPane pane = getProjectViewPaneById(paneId);
    pane.installComparator();
  }

  private class ManualOrderAction extends ToggleAction implements DumbAware {
    private ManualOrderAction() {
      super(IdeBundle.message("action.manual.order"), IdeBundle.message("action.manual.order"), AllIcons.ObjectBrowser.Sorted);
    }

    @Override
    public boolean isSelected(AnActionEvent event) {
      return isManualOrder(getCurrentViewId());
    }

    @Override
    public void setSelected(AnActionEvent event, boolean flag) {
      setManualOrder(getCurrentViewId(), flag);
    }

    @RequiredUIAccess
    @Override
    public void update(final AnActionEvent e) {
      super.update(e);
      final Presentation presentation = e.getPresentation();
      AbstractProjectViewPane pane = getCurrentProjectViewPane();
      presentation.setEnabledAndVisible(pane != null && pane.supportsManualOrder());
    }
  }

  private class SortByTypeAction extends ToggleAction implements DumbAware {
    private SortByTypeAction() {
      super(IdeBundle.message("action.sort.by.type"), IdeBundle.message("action.sort.by.type"), AllIcons.ObjectBrowser.SortByType);
    }

    @Override
    public boolean isSelected(AnActionEvent event) {
      return isSortByType(getCurrentViewId());
    }

    @Override
    public void setSelected(AnActionEvent event, boolean flag) {
      setSortByType(getCurrentViewId(), flag);
    }

    @RequiredUIAccess
    @Override
    public void update(final AnActionEvent e) {
      super.update(e);
      final Presentation presentation = e.getPresentation();
      AbstractProjectViewPane pane = getCurrentProjectViewPane();
      presentation.setVisible(pane != null && pane.supportsSortByType());
    }
  }

  private class FoldersAlwaysOnTopAction extends ToggleAction implements DumbAware {
    private FoldersAlwaysOnTopAction() {
      super("Folders Always on Top");
    }

    @Override
    public boolean isSelected(AnActionEvent event) {
      return isFoldersAlwaysOnTop();
    }

    @Override
    public void setSelected(AnActionEvent event, boolean flag) {
      setFoldersAlwaysOnTop(flag);
    }

    @RequiredUIAccess
    @Override
    public void update(final AnActionEvent e) {
      super.update(e);
      final Presentation presentation = e.getPresentation();
      AbstractProjectViewPane pane = getCurrentProjectViewPane();
      presentation.setEnabledAndVisible(pane != null && pane.supportsFoldersAlwaysOnTop());
    }
  }

  private class ScrollFromSourceAction extends AnAction implements DumbAware {
    private ScrollFromSourceAction() {
      super("Scroll from Source", "Select the file open in the active editor", PlatformIconGroup.generalLocate());
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      myAutoScrollFromSourceHandler.scrollFromSource();
    }
  }

  @Nonnull
  @Override
  public Collection<String> getPaneIds() {
    return Collections.unmodifiableCollection(myId2Pane.keySet());
  }

  @Nonnull
  @Override
  public Collection<SelectInTarget> getSelectInTargets() {
    ensurePanesLoaded();
    return mySelectInTargets.values();
  }

  @Nonnull
  @Override
  public AsyncResult<Void> getReady(@Nonnull Object requestor) {
    AbstractProjectViewPane pane = myId2Pane.get(myCurrentViewSubId);
    if (pane == null) {
      pane = myId2Pane.get(myCurrentViewId);
    }
    return pane != null ? pane.getReady(requestor) : AsyncResult.done(null);
  }
}
