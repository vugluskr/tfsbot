package model;

import utils.Optioned;

/**
 * @author Denis Danilin | denis@danilin.name
 * 04.06.2020
 * tfs ☭ sweat and blood
 */
public interface InputWait extends Optioned {
    default void setWaitRenameInput() { User.Optz.RenameInputWait.set(this); }

    default void setWaitLabelInput() { User.Optz.LabelInputWait.set(this); }

    default void setWaitDirInput() { User.Optz.DirInputWait.set(this); }

    default void setWaitSearchInput() { User.Optz.SearchInputWait.set(this); }

    default boolean isWaitRenameInput() { return User.Optz.RenameInputWait.is(this); }

    default boolean isWaitLabelInput() { return User.Optz.LabelInputWait.is(this); }

    default boolean isWaitDirInput() { return User.Optz.DirInputWait.is(this); }

    default boolean isWaitSearchInput() { return User.Optz.SearchInputWait.is(this); }

    default boolean isWaitInput() {
        return isWaitDirInput() || isWaitLabelInput() || isWaitRenameInput() || isWaitSearchInput();
    }

    default void resetInputWait() {
        User.Optz.RenameInputWait.remove(this);
        User.Optz.LabelInputWait.remove(this);
        User.Optz.DirInputWait.remove(this);
        User.Optz.SearchInputWait.remove(this);
    }

}
