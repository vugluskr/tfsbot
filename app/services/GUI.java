package services;

import model.TFile;
import model.User;
import model.telegram.api.InlineButton;
import model.telegram.api.InlineKeyboard;
import model.telegram.api.TextRef;
import utils.FlowBox;
import utils.LangMap;
import utils.Strings.Callback;
import utils.Strings.Uni;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static utils.LangMap.v;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class GUI {
    public interface Buttons {
        InlineButton mkLabelButton = new InlineButton(Uni.label, Callback.mkLabel);
        InlineButton searchButton = new InlineButton(Uni.search, Callback.search);
        InlineButton mkDirButton = new InlineButton(Uni.mkdir, Callback.mkDir);
        InlineButton gearButton = new InlineButton(Uni.gear, Callback.gearLs);
        InlineButton cancelButton = new InlineButton(Uni.cancel, Callback.cancelCb);
        InlineButton goUpButton = new InlineButton(Uni.updir, Callback.goUp);
        InlineButton rewindButton = new InlineButton(Uni.rewind, Callback.rewind);
        InlineButton forwardButton = new InlineButton(Uni.forward, Callback.forward);
        InlineButton renameButton = new InlineButton(Uni.rename, Callback.rename);
        InlineButton putButton = new InlineButton(Uni.put, Callback.put);
        InlineButton moveButton = new InlineButton(Uni.move, Callback.move);
        InlineButton dropButton = new InlineButton(Uni.drop, Callback.drop);
        InlineButton checkAllButton = new InlineButton(Uni.checkAll, Callback.checkAll);
        InlineButton shareButton = new InlineButton(Uni.share, Callback.share);
        InlineButton mkLinkButton = new InlineButton(Uni.Link, Callback.mkLink);
        InlineButton mkGrantButton = new InlineButton(Uni.Person, Callback.mkGrant);
        InlineButton saveButton = new InlineButton(Uni.save, Callback.save);
        InlineButton okButton = new InlineButton(Uni.put, Callback.ok);

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

    public void yesNoPrompt(final LangMap.Value question, final User user, final Object... args) {
        final List<InlineButton> buttons = new ArrayList<>(0);
        buttons.add(Buttons.okButton);
        buttons.add(Buttons.cancelButton);

        tgApi.sendOrUpdate(v(question, user, args), "MarkdownV2",
                new InlineKeyboard(Collections.singletonList(buttons)), 0, user.getId(), dlgId -> {
                    if (user.getLastDialogId() > 0)
                        tgApi.deleteMessage(user.getLastDialogId(), user.getId());

                    user.setLastDialogId(dlgId);
                });
    }

    public void dialog(final LangMap.Value question, final User user, final Object... args) {
        tgApi.ask(question, user, dlgId -> {
            if (user.getLastDialogId() > 0)
                tgApi.deleteMessage(user.getLastDialogId(), user.getId());

            user.setLastDialogId(dlgId);
        }, args);
    }

    public void notify(final LangMap.Value text, final User user, final Object... args) {
        tgApi.sendPlainText(text, user, dlgId -> {
            if (user.getLastDialogId() > 0)
                tgApi.deleteMessage(user.getLastDialogId(), user.getId());

            user.setLastDialogId(dlgId);
        }, args);
    }


    public void makeFileDialog(final TFile entry, final User user) {
        tgApi.sendMedia(entry, (entry.isShared() ? Uni.share + " " : "") + entry.getPath(), fileKbd, user.getId(), dlg -> {
            if (user.getLastDialogId() > 0)
                tgApi.deleteMessage(user.getLastDialogId(), user.getId());

            user.setLastDialogId(dlg);
        });
    }

    @SuppressWarnings("ConstantConditions")
    public void sendBox(final FlowBox box, final User user) {
        final TextRef ref = new TextRef();
        ref.setParseMode(box.format);
        ref.setText(box.body.toString());


        if (!box.rows.isEmpty()) {
            while (box.rows.get(box.rows.size() - 1).isEmpty())
                box.rows.remove(box.rows.size() - 1);

            if (!box.rows.isEmpty())
                ref.setReplyMarkup(new InlineKeyboard(box.rows));
        }
        send(ref, user.getLastMessageId(), user.getId(), user::setLastMessageId);
    }

    public void send(final TextRef box, final long lastMessageId, final long chatId, final Consumer<Long> sentMsgIdConsumer) {
        tgApi.sendOrUpdate(box.getText(), box.getParseMode(), box.getReplyMarkup(), lastMessageId, chatId, sentMsgIdConsumer);
    }
}
