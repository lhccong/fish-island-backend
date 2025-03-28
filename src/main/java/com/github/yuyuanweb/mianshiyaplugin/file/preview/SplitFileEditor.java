package com.github.yuyuanweb.mianshiyaplugin.file.preview;

import com.github.yuyuanweb.mianshiyaplugin.constant.ViewConstant;
import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.JBSplitter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import static com.intellij.openapi.fileEditor.TextEditorWithPreview.DEFAULT_LAYOUT_FOR_FILE;

/**
 * 分栏 文件编辑器
 *
 * @author pine
 */
public abstract class SplitFileEditor<E1 extends FileEditor, E2 extends FileEditor> extends UserDataHolderBase implements FileEditor {

    public static final Key<SplitFileEditor> PARENT_SPLIT_KEY = Key.create(ViewConstant.PARENT_SPLIT_KEY);
    private static final String MY_PROPORTION_KEY = ViewConstant.SPLIT_FILE_EDITOR + ViewConstant.SPLITTER;

    @NotNull
    protected final E1 myMainEditor;
    @NotNull
    protected final E2 mySecondEditor;
    @NotNull
    private final JComponent myComponent;
    @NotNull
    private SplitEditorLayout mySplitEditorLayout = SplitEditorLayout.SPLIT;

    @NotNull
    private final MyListenersMultimap myListenersGenerator = new MyListenersMultimap();

    private SplitEditorToolbar myToolbarWrapper;

    public SplitFileEditor(@NotNull E1 mainEditor, @NotNull E2 secondEditor) {
        myMainEditor = mainEditor;
        mySecondEditor = secondEditor;

        adjustDefaultLayout(mainEditor);
        myComponent = createComponent();

        if (myMainEditor instanceof TextEditor) {
            myMainEditor.putUserData(PARENT_SPLIT_KEY, this);
        }
        if (mySecondEditor instanceof TextEditor) {
            mySecondEditor.putUserData(PARENT_SPLIT_KEY, this);
        }
    }

    private void adjustDefaultLayout(E1 editor) {
        TextEditorWithPreview.Layout layout = getAndResetPredefinedLayoutForEditor(editor);
        if (layout != null) {
            switch (layout) {
                case SHOW_EDITOR:
                    mySplitEditorLayout = SplitEditorLayout.FIRST;
                    break;
                case SHOW_PREVIEW:
                    mySplitEditorLayout = SplitEditorLayout.SECOND;
                    break;
                case SHOW_EDITOR_AND_PREVIEW:
                    mySplitEditorLayout = SplitEditorLayout.SPLIT;
                    break;
            }
        }
    }

    @Nullable
    private static TextEditorWithPreview.Layout getAndResetPredefinedLayoutForEditor(FileEditor editor) {
        VirtualFile file = editor.getFile();
        if (file != null) {
            TextEditorWithPreview.Layout layout = file.getUserData(DEFAULT_LAYOUT_FOR_FILE);
            if (layout != null) {
                file.putUserData(DEFAULT_LAYOUT_FOR_FILE, null);
                return layout;
            }
        }

        return null;
    }

    @NotNull
    private JComponent createComponent() {
        boolean myVerticalSplitOption = true;
        JBSplitter mySplitter = new JBSplitter(!myVerticalSplitOption, 0.5f, 0.15f, 0.85f);
        mySplitter.setSplitterProportionKey(MY_PROPORTION_KEY);
        mySplitter.setFirstComponent(myMainEditor.getComponent());
        mySplitter.setSecondComponent(mySecondEditor.getComponent());
        mySplitter.setDividerWidth(3);

        final JPanel result = new JPanel(new BorderLayout());
        result.add(mySplitter, BorderLayout.CENTER);
        adjustEditorsVisibility();

        return result;
    }

    private void invalidateLayout(boolean requestFocus) {
        adjustEditorsVisibility();
        myToolbarWrapper.refresh();
        myComponent.repaint();

        if (!requestFocus) {
            return;
        }

        final JComponent focusComponent = getPreferredFocusedComponent();
        if (focusComponent != null) {
            IdeFocusManager.findInstanceByComponent(focusComponent).requestFocus(focusComponent, true);
        }
    }

    private void adjustEditorsVisibility() {
        myMainEditor.getComponent().setVisible(mySplitEditorLayout.showFirst);
        mySecondEditor.getComponent().setVisible(mySplitEditorLayout.showSecond);
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return myComponent;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        if (mySplitEditorLayout.showFirst) {
            return myMainEditor.getPreferredFocusedComponent();
        }
        if (mySplitEditorLayout.showSecond) {
            return mySecondEditor.getPreferredFocusedComponent();
        }
        return null;
    }

    @NotNull
    @Override
    public FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return new MyFileEditorState(mySplitEditorLayout.name(), myMainEditor.getState(level), mySecondEditor.getState(level));
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        if (state instanceof SplitFileEditor.MyFileEditorState) {
            SplitFileEditor.MyFileEditorState compositeState = (SplitFileEditor.MyFileEditorState) state;
            if (compositeState.getFirstState() != null) {
                myMainEditor.setState(compositeState.getFirstState());
            }
            if (compositeState.getSecondState() != null) {
                mySecondEditor.setState(compositeState.getSecondState());
            }
            if (compositeState.getSplitLayout() != null) {
                mySplitEditorLayout = SplitEditorLayout.valueOf(compositeState.getSplitLayout());
                invalidateLayout(true);
            }
        }
    }

    @Override
    public boolean isModified() {
        return myMainEditor.isModified() || mySecondEditor.isModified();
    }

    @Override
    public boolean isValid() {
        return myMainEditor.isValid() && mySecondEditor.isValid();
    }

    @Override
    public void selectNotify() {
        myMainEditor.selectNotify();
        mySecondEditor.selectNotify();
    }

    @Override
    public void deselectNotify() {
        myMainEditor.deselectNotify();
        mySecondEditor.deselectNotify();
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        myMainEditor.addPropertyChangeListener(listener);
        mySecondEditor.addPropertyChangeListener(listener);

        final DoublingEventListenerDelegate delegate = myListenersGenerator.addListenerAndGetDelegate(listener);
        myMainEditor.addPropertyChangeListener(delegate);
        mySecondEditor.addPropertyChangeListener(delegate);
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        myMainEditor.removePropertyChangeListener(listener);
        mySecondEditor.removePropertyChangeListener(listener);

        final DoublingEventListenerDelegate delegate = myListenersGenerator.removeListenerAndGetDelegate(listener);
        if (delegate != null) {
            myMainEditor.removePropertyChangeListener(delegate);
            mySecondEditor.removePropertyChangeListener(delegate);
        }
    }

    @Nullable
    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return myMainEditor.getBackgroundHighlighter();
    }

    @Nullable
    @Override
    public FileEditorLocation getCurrentLocation() {
        return myMainEditor.getCurrentLocation();
    }

    @Nullable
    @Override
    public StructureViewBuilder getStructureViewBuilder() {
        return myMainEditor.getStructureViewBuilder();
    }

    @Override
    public void dispose() {
        Disposer.dispose(myMainEditor);
        Disposer.dispose(mySecondEditor);
    }

    public static class MyFileEditorState implements FileEditorState {
        @Nullable
        private final String mySplitLayout;
        @Nullable
        private final FileEditorState myFirstState;
        @Nullable
        private final FileEditorState mySecondState;

        public MyFileEditorState(@Nullable String splitLayout, @Nullable FileEditorState firstState, @Nullable FileEditorState secondState) {
            mySplitLayout = splitLayout;
            myFirstState = firstState;
            mySecondState = secondState;
        }

        @Nullable
        public String getSplitLayout() {
            return mySplitLayout;
        }

        @Nullable
        public FileEditorState getFirstState() {
            return myFirstState;
        }

        @Nullable
        public FileEditorState getSecondState() {
            return mySecondState;
        }

        @Override
        public boolean canBeMergedWith(@NotNull FileEditorState otherState, @NotNull FileEditorStateLevel level) {
            return otherState instanceof SplitFileEditor.MyFileEditorState
                    && (myFirstState == null || myFirstState.canBeMergedWith(((MyFileEditorState) otherState).myFirstState, level))
                    && (mySecondState == null || mySecondState.canBeMergedWith(((MyFileEditorState) otherState).mySecondState, level));
        }
    }

    private class DoublingEventListenerDelegate implements PropertyChangeListener {
        @NotNull
        private final PropertyChangeListener myDelegate;

        private DoublingEventListenerDelegate(@NotNull PropertyChangeListener delegate) {
            myDelegate = delegate;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            myDelegate.propertyChange(new PropertyChangeEvent(SplitFileEditor.this, evt.getPropertyName(), evt.getOldValue(), evt.getNewValue()));
        }
    }

    private class MyListenersMultimap {
        private final Map<PropertyChangeListener, Pair<Integer, DoublingEventListenerDelegate>> myMap = new HashMap<>();

        @NotNull
        public DoublingEventListenerDelegate addListenerAndGetDelegate(@NotNull PropertyChangeListener listener) {
            if (!myMap.containsKey(listener)) {
                myMap.put(listener, Pair.create(1, new DoublingEventListenerDelegate(listener)));
            } else {
                myMap.computeIfPresent(listener, (k, oldPair) -> Pair.create(oldPair.getFirst() + 1, oldPair.getSecond()));
            }

            return myMap.get(listener).getSecond();
        }

        @Nullable
        public DoublingEventListenerDelegate removeListenerAndGetDelegate(@NotNull PropertyChangeListener listener) {
            final Pair<Integer, DoublingEventListenerDelegate> oldPair = myMap.get(listener);
            if (oldPair == null) {
                return null;
            }

            if (oldPair.getFirst() == 1) {
                myMap.remove(listener);
            } else {
                myMap.put(listener, Pair.create(oldPair.getFirst() - 1, oldPair.getSecond()));
            }
            return oldPair.getSecond();
        }
    }

    public enum SplitEditorLayout {
        FIRST(true, false, "editor only"),
        SECOND(false, true, "preview only"),
        SPLIT(true, true, "editor and preview");

        public final boolean showFirst;
        public final boolean showSecond;
        public final String presentationName;

        SplitEditorLayout(boolean showFirst, boolean showSecond, String presentationName) {
            this.showFirst = showFirst;
            this.showSecond = showSecond;
            this.presentationName = presentationName;
        }

        @Override
        public String toString() {
            return String.format("Show %s", presentationName);
        }
    }
}

