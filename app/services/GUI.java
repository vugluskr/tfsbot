package services;

import model.TFile;
import model.User;
import model.telegram.ContentType;
import model.telegram.api.InlineButton;
import model.telegram.api.InlineKeyboard;
import model.telegram.api.TextRef;
import model.telegram.commands.*;
import utils.LangMap;
import utils.Uni;
import utils.UserMode;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static utils.LangMap.v;
import static utils.TextUtils.escapeMd;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class GUI {
    private static final InlineButton
            simpleCancel = new InlineButton(Uni.cancel, SimpleCancel.mnemonic()),
            label = new InlineButton(Uni.label, CreateLabelReq.mnemonic()),
            search = new InlineButton(Uni.search, Search.mnemonic()),
            mkdir = new InlineButton(Uni.mkdir, CreateDirReq.mnemonic()),
            gear = new InlineButton(Uni.gear, EditMode.mnemonic()),
            exitMode = new InlineButton(Uni.cancel, ExitMode.mnemonic());

    @Inject
    private TgApi tgApi;

    @Inject
    private FsService fsService;

    public void makeFileDialog(final TFile entry, final long chatId, final Consumer<Long> dialogIdConsumer) {
        tgApi.sendMedia(entry, entry.getPath(), new InlineKeyboard(Collections.singletonList(new ArrayList<InlineButton>(3) {{
            add(new InlineButton(Uni.rename, RenameEntry.mnemonic(entry.getId())));
            add(new InlineButton(Uni.move, MoveEntry.mnemonic(entry.getId())));
            add(new InlineButton(Uni.drop, DropEntry.mnemonic(entry.getId())));
            add(simpleCancel);
        }})), chatId, dialogIdConsumer);
    }

    public void makeMainView(final User user, final Consumer<Long> sentMsgIdConsumer) {
        final TextRef box = new TextRef(user.getPwd(), user.getId());

        final TFile dir = fsService.get(user.getDirId(), user);
        final List<TFile> scope;

        switch (user.getUserMode()) {
            case Normal: // всё разрешено
                scope = fsService.list(user.getDirId(), user);
                if (user.getDirId() > 1) box.headRow(new InlineButton(Uni.updir, OpenEntry.mnemonic(dir.getParentId())));
                box.headRow(label);
                box.headRow(search);
                box.headRow(mkdir);
                box.headRow(gear);

                if (scope.isEmpty())
                    box.setMd2().setText(escapeMd(user.getPwd()) + "\n\n_" + escapeMd(v(LangMap.Names.NO_CONTENT, user)) + "_");

                break;
            case GearSearch: // переходы запрещены, только оффсет директории, из верхних кнопок только "сброс"
                scope = fsService.getSelection(user);
                box.headRow(exitMode);
                break;
            case Gear: // переходы запрещены, только оффсет директории, из верхних кнопок только "сброс"
                scope = fsService.list(user.getDirId(), user);
                box.headRow(exitMode);
                break;
            case Moving: // смотрим только директории, из верхних кнопок только "назад", "положить" и "сброс"
                scope = fsService.listFolders(user.getDirId(), user);
                if (user.getDirId() > 1) box.headRow(new InlineButton(Uni.updir, OpenEntry.mnemonic(dir.getParentId())));
                box.headRow(new InlineButton(Uni.put, MoveDestination.mnemonic(user.getDirId())));
                box.headRow(exitMode);
                break;
            case Searching: // из верхних кнопок только "поиск" (заново) и "сброс", переход в дир отменяет режим
                scope = fsService.getFound(user);
                box.headRow(search);
                box.headRow(gear);
                box.headRow(exitMode);
                box.setMd2().setText(escapeMd("_" + escapeMd(v(scope.isEmpty() ? LangMap.Names.NO_RESULTS : LangMap.Names.SEARCHED, user, user.getLastSearch())) + "_"));
                break;
            default:
                return;
        }

        if (user.getUserMode() == UserMode.Gear || user.getUserMode() == UserMode.GearSearch) {
            // без выделения лейблов
            scope.stream()
                    .sorted((o1, o2) -> {
                        final int res = Boolean.compare(o2.isDir(), o1.isDir());
                        return res != 0 ? res : o1.getName().compareTo(o2.getName());
                    })
                    .skip(user.getOffset())
                    .limit(10)
                    .forEach(f -> box.row(new InlineButton((f.isDir() ? Uni.folder + " " : "") + f.getName() + (f.isSelected() ?
                            " " + Uni.checked : ""), InverseSelection.mnemonic(f.getId()))));

            final List<InlineButton> pageRow = new ArrayList<>();

            if (user.getOffset() > 0)
                pageRow.add(new InlineButton(Uni.rew, Rewind.mnemonic()));
            if (user.getOffset() + 10 < scope.size())
                pageRow.add(new InlineButton(Uni.fwd, Forward.mnemonic()));

            final List<Long> selectedIds = scope.stream().filter(TFile::isSelected).map(TFile::getId).collect(Collectors.toList());
            if (selectedIds.size() > 0) {
                if (selectedIds.size() == 1)
                    pageRow.add(new InlineButton(Uni.rename + " (" + selectedIds.size() + ")", RenameEntry.mnemonic(selectedIds.get(0))));
                pageRow.add(new InlineButton(Uni.move + " (" + selectedIds.size() + ")", MoveSelection.mnemonic()));
                pageRow.add(new InlineButton(Uni.drop + " (" + selectedIds.size() + ")", DropSelection.mnemonic()));
            }

            if (!pageRow.isEmpty())
                box.row(pageRow);
        } else {
            if (scope.stream().anyMatch(e -> e.getType() == ContentType.LABEL)) {
                final StringBuilder labels = new StringBuilder(escapeMd(user.getPwd()));
                labels.append("\n\n");

                scope.stream().filter(e -> e.getType() == ContentType.LABEL)
                        .forEach(e -> labels.append('`').append(escapeMd(e.getName())).append("`\n\n"));

                box.setMd2().setText(labels.toString());
            }

            final long count = scope.stream().filter(e -> e.getType() != ContentType.LABEL).count();

            if (count > 0) {
                scope.stream().filter(e -> e.getType() != ContentType.LABEL)
                        .sorted((o1, o2) -> {
                            final int res = Boolean.compare(o2.isDir(), o1.isDir());
                            return res != 0 ? res : o1.getName().compareTo(o2.getName());
                        })
                        .skip(user.getOffset())
                        .limit(10)
                        .forEach(f -> box.row(new InlineButton((f.isDir() ? Uni.folder + "  " : "") + f.getName(), OpenEntry.mnemonic(f.getId()))));
                if (count > 10) {
                    final List<InlineButton> pageRow = new ArrayList<>();

                    if (user.getOffset() > 0)
                        pageRow.add(new InlineButton(Uni.rew, Rewind.mnemonic()));
                    if (user.getOffset() + 10 < count)
                        pageRow.add(new InlineButton(Uni.fwd, Forward.mnemonic()));

                    box.row(pageRow);
                }
            }
        }

        tgApi.sendOrUpdate(box.getText(), box.getParseMode(), box.getReplyMarkup(), user.getLastMessageId(), user.getId(), sentMsgIdConsumer);
    }
}
