package model;

import utils.Optioned;

/**
 * @author Denis Danilin | denis@danilin.name
 * 04.06.2020
 * tfs ☭ sweat and blood
 */
public interface InputWait extends Optioned {
    default void setWaitDirRenameInput() { User.Optz.RenameDirInputWait.set(this); }
    default void setWaitFileRenameInput() { User.Optz.RenameFileInputWait.set(this); }
    default void setWaitLabelEditInput() { User.Optz.EditLabelInputWait.set(this); }
    default void setWaitLockInput() {User.Optz.PasswordInputWait.set(this); }

    default void setWaitLabelInput() { User.Optz.LabelInputWait.set(this); }

    default void setWaitDirInput() { User.Optz.DirInputWait.set(this); }

    default boolean isWaitPasswordInput() {return User.Optz.PasswordInputWait.is(this); }
    default boolean isWaitRenameDirInput() { return User.Optz.RenameDirInputWait.is(this); }
    default boolean isWaitRenameFileInput() { return User.Optz.RenameFileInputWait.is(this); }
    default boolean isWaitEditLabelInput() { return User.Optz.EditLabelInputWait.is(this); }
    default boolean isWaitUnlockDirInput() { return User.Optz.UnlockDirInputWait.is(this); }
    default boolean isWaitUnlockFileInput() { return User.Optz.UnlockFileInputWait.is(this); }

    default boolean isWaitLabelInput() { return User.Optz.LabelInputWait.is(this); }

    default boolean isWaitDirInput() { return User.Optz.DirInputWait.is(this); }

    default void resetInputWait() {
        User.Optz.LabelInputWait.remove(this);
        User.Optz.DirInputWait.remove(this);
        User.Optz.PasswordInputWait.remove(this);
        User.Optz.UnlockDirInputWait.remove(this);
        User.Optz.UnlockFileInputWait.remove(this);
    }
}
