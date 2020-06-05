package model;

import utils.Optioned;

/**
 * @author Denis Danilin | denis@danilin.name
 * 04.06.2020
 * tfs ☭ sweat and blood
 */
public interface Stater extends Optioned {
    default void setSharing() { User.Optz.StateSharing.set(this); }
    default void setMoving() { User.Optz.StateMoving.set(this); }
    default void setGearing() { User.Optz.StateGearing.set(this); }
    default void setSearching() { User.Optz.StateSearching.set(this); }
    default void setFileViewing() { User.Optz.StateFileViewing.set(this); }

    default void unsetSharing() { User.Optz.StateSharing.remove(this); }
    default void unsetMoving() { User.Optz.StateMoving.remove(this); }
    default void unsetGearing() { User.Optz.StateGearing.remove(this); }
    default void unsetSearching() { User.Optz.StateSearching.remove(this); }
    default void unsetFileViewing() { User.Optz.StateFileViewing.remove(this); }

    default boolean isSharing() { return User.Optz.StateSharing.is(this); }
    default boolean isMoving() { return User.Optz.StateMoving.is(this); }
    default boolean isSearching() { return User.Optz.StateSearching.is(this); }
    default boolean isGearing() { return User.Optz.StateGearing.is(this); }
    default boolean isFileViewing() { return User.Optz.StateFileViewing.is(this); }

    default boolean isJustWatching() {
        return !isGearing() && !isMoving() && !isSearching() && !isSharing() && !isFileViewing();
    }

    default void resetState() {
        User.Optz.StateGearing.remove(this);
        User.Optz.StateMoving.remove(this);
        User.Optz.StateSharing.remove(this);
        User.Optz.StateSearching.remove(this);
        User.Optz.StateFileViewing.remove(this);
    }
}
