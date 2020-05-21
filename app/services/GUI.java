package services;

import model.TFile;
import model.User;
import model.telegram.ContentType;
import model.telegram.api.InlineButton;
import model.telegram.api.InlineKeyboard;
import model.telegram.api.TextRef;
import utils.Strings.Callback;
import utils.Strings.Uni;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Consumer;

import static utils.TextUtils.escapeMd;
import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class GUI {
    public interface Buttons {
        InlineButton mkLabelButton = new InlineButton(Uni.label, Callback.mkLabel);
        InlineButton searchButton = new InlineButton(Uni.search, Callback.searchStateInit);
        InlineButton mkDirButton = new InlineButton(Uni.mkdir, Callback.mkDir);
        InlineButton gearButton = new InlineButton(Uni.gear, Callback.gearStateInit);
        InlineButton cancelButton = new InlineButton(Uni.cancel, Callback.cancel);
        InlineButton goUpButton = new InlineButton(Uni.updir, Callback.goUp);
        InlineButton rewindButton = new InlineButton(Uni.rewind, Callback.rewind);
        InlineButton forwardButton = new InlineButton(Uni.forward, Callback.forward);
        InlineButton renameButton = new InlineButton(Uni.rename, Callback.renameEntry);
        InlineButton putButton = new InlineButton(Uni.put, Callback.put);
        InlineButton moveButton = new InlineButton(Uni.move, Callback.move);
        InlineButton dropButton = new InlineButton(Uni.drop, Callback.drop);
        InlineButton checkAll = new InlineButton(Uni.checkAll, Callback.checkAll);
    }

    private static final InlineKeyboard fileKbd = new InlineKeyboard();

    static {
        final List<InlineButton> fileKbdRow = new ArrayList<>(3);
        fileKbdRow.add(Buttons.renameButton);
        fileKbdRow.add(Buttons.moveButton);
        fileKbdRow.add(Buttons.dropButton);
        fileKbdRow.add(Buttons.cancelButton);

        fileKbd.setKeyboard(Collections.singletonList(fileKbdRow));
    }

    @Inject
    private TgApi tgApi;

    public void makeFileDialog(final TFile entry, final long chatId, final Consumer<Long> dialogIdConsumer) {
        tgApi.sendMedia(entry, entry.getPath(), fileKbd, chatId, dialogIdConsumer);
    }

    public void makeMovingView(final String mdEscapedBody, final List<TFile> scope, final List<InlineButton> upper, final List<InlineButton> bottom,
                               final int offset, final User user, final Consumer<Long> sentMsgIdConsumer) {
        final TextRef box = new TextRef(mdEscapedBody, user.getId()).setMd2();

        if (!isEmpty(upper))
            upper.forEach(box::headRow);

        scope.stream()
                .sorted((o1, o2) -> {
                    final int res = Boolean.compare(o2.isDir(), o1.isDir());
                    return res != 0 ? res : o1.getName().compareTo(o2.getName());
                })
                .skip(offset)
                .limit(10)
                .forEach(f -> box.row(new InlineButton(Uni.folder + "  " + f.getName(), Callback.openEntry + f.getId())));

        if (!bottom.isEmpty())
            box.row(bottom);

        send(box, user.getLastMessageId(), user, sentMsgIdConsumer);
    }

    public void makeGearView(final String mdEscapedBody, final List<TFile> scope, final List<InlineButton> upper, final List<InlineButton> bottom,
                             final int offset, final User user, final Consumer<Long> sentMsgIdConsumer) {
        final TextRef box = new TextRef(mdEscapedBody, user.getId()).setMd2();
        if (!isEmpty(upper))
            upper.forEach(box::headRow);

        // без выделения лейблов
        scope.stream()
                .sorted((o1, o2) -> {
                    final int res = Boolean.compare(o2.isDir(), o1.isDir());
                    return res != 0 ? res : o1.getName().compareTo(o2.getName());
                })
                .skip(offset)
                .limit(10)
                .forEach(f -> box.row(new InlineButton((f.isDir() ? Uni.folder + " " : "") + f.getName() + (f.isSelected() ?
                        " " + Uni.checked : ""), Callback.inversCheck + f.getId())));

        if (!bottom.isEmpty())
            box.row(bottom);

        send(box, user.getLastMessageId(), user, sentMsgIdConsumer);
    }

    public void makeMainView(final String mdEscapedBody, final Collection<TFile> scope, final int offset, final List<InlineButton> upButtons,
                             final List<InlineButton> bottomButtons,
                             final long lastMessageId, final User user, final Consumer<Long> sentMsgIdConsumer) {
        final TextRef box = new TextRef(mdEscapedBody, user.getId()).setMd2();
        upButtons.forEach(box::headRow);

        if (scope.stream().anyMatch(e -> e.getType() == ContentType.LABEL)) {
            final StringBuilder labels = new StringBuilder(mdEscapedBody);
            labels.append("\n\n");

            scope.stream().filter(e -> e.getType() == ContentType.LABEL)
                    .forEach(e -> labels.append('`').append(escapeMd(e.getName())).append("`\n\n"));

            box.setText(labels.toString());
        }

        final long count = scope.stream().filter(e -> e.getType() != ContentType.LABEL).count();

        if (count > 0) {
            scope.stream().filter(e -> e.getType() != ContentType.LABEL)
                    .sorted((o1, o2) -> {
                        final int res = Boolean.compare(o2.isDir(), o1.isDir());
                        return res != 0 ? res : o1.getName().compareTo(o2.getName());
                    })
                    .skip(offset)
                    .limit(10)
                    .forEach(f -> box.row(new InlineButton((f.isDir() ? Uni.folder + "  " : "") + f.getName(), Callback.openEntry + f.getId())));

            if (!isEmpty(bottomButtons))
                box.row(bottomButtons);
        }

        send(box, lastMessageId, user, sentMsgIdConsumer);
    }

    private void send(final TextRef box, final long lastMessageId, final User user, final Consumer<Long> sentMsgIdConsumer) {
        tgApi.sendOrUpdate(box.getText(), box.getParseMode(), box.getReplyMarkup(), lastMessageId, user, sentMsgIdConsumer);
    }
}
