package services;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class HeadQuarters {
/*
    private enum ViewKind {subjectShares, gearSubject, none, viewDir, viewFile, viewLabel, viewSearchedDir, viewSearchedFile, viewSearchedLabel, searchResults}

    private static final Comparator<TFile> sorter = (o1, o2) -> {
        final int res = Boolean.compare(o2.isDir(), o1.isDir());
        return res != 0 ? res : o1.getName().compareTo(o2.getName());
    };

    @Inject
    private Config config;

    @Inject
    private TfsService fsService;

    @Inject
    private UserService userService;

    @Inject
    private TgApi api;

    public void doCommand(final Command command, final User user) {
        TFile subject = fsService.get(user.getSubjectId(), user);
        ViewKind viewKind = ViewKind.none;

        switch (command.type) {
            case backToSearch:
                subject = fsService.get(user.getSearchDirId(), user);
                viewKind = ViewKind.searchResults;
                break;
            case cancel:
            case cancelSearch:
                viewKind = subject.isDir() ? ViewKind.viewDir : subject.isLabel() ? ViewKind.viewLabel : ViewKind.viewFile;
                user.resetState();
                break;
            case changeGrantRw:
                fsService.changeShareRo(((Share) byIdx(command.elementIdx, subject, user)).getId(), user);
                viewKind = ViewKind.subjectShares;
                break;
            case doSearch:
                user.setQuery(command.input);
                user.setViewOffset(0);
                user.resetState();
                user.setSearching();
                if (!subject.isDir()) {
                    user.setSubjectId(user.getRootId());
                    subject = fsService.findRoot(user.getId());
                }
                user.setSearchDirId(subject.getId());
                viewKind = ViewKind.searchResults;
                break;
            case dropFile:
            case dropDir:
            case dropLabel: // может объединить?
                fsService.rm(subject, user);
                user.setSubjectId(subject.getParentId());
                user.setViewOffset(0);
                user.resetState();
                subject = fsService.get(subject.getParentId(), user);
                viewKind = ViewKind.viewDir;
                break;
            case dropEntryLink:
                fsService.dropEntryLink(subject.getId(), user);
                viewKind = ViewKind.subjectShares;
                break;
            case dropGrant:
                fsService.dropShare(((Share) byIdx(command.elementIdx, subject, user)).getId(), user);
                viewKind = ViewKind.subjectShares;
                break;
            case editLabel:
                if (!isEmpty(command.input)) {
                    if (!subject.getName().equals(command.input) && fsService.entryMissed(command.input, user)) {
                        subject.setName(command.input);
                        fsService.updateMeta(subject, user);
                    }
                    user.resetState();
                    viewKind = ViewKind.viewLabel;
                } else {
                    api.dialog(LangMap.Value.TYPE_LABEL, user);
                    user.setWaitLabelEditInput();
                }
                break;
            case rewind:
            case forward:
                user.deltaSearchOffset(command.type == CommandType.rewind ? -10 : 10);
                viewKind = user.isSearching() ? ViewKind.searchResults : ViewKind.viewDir;
                break;
            case gear:
                user.setGearing();
                viewKind = ViewKind.gearSubject;
                break;
            case openParent:
                user.resetState();
                user.setSubjectId(subject.getParentId());
                user.setViewOffset(0);
                subject = fsService.get(subject.getParentId(), user);
                viewKind = ViewKind.viewDir;
                break;
            case joinPublicShare:
                final Share share;
                if (!isEmpty(command.input) && (share = fsService.getPublicShare(command.input)) != null && share.getOwner() != user.getId()) {
                    final TFile dir = fsService.applyShareByLink(share, user);

                    if (dir != null) {
                        user.setSubjectId(dir.getId());
                        user.setViewOffset(0);
                        subject = dir;
                    }
                }
                user.resetState();
                viewKind = ViewKind.viewDir;
                break;
            case makeEntryLink:
                fsService.makeShare(subject.getName(), user, subject.getId(), 0, null);
                viewKind = ViewKind.subjectShares;
                break;
            case mkDir:
                user.resetState();
                if (!isEmpty(command.input)) {
                    if (fsService.entryMissed(command.input, user)) {
                        final TFile n = fsService.mk(TFileFactory.dir(command.input, subject.getId(), user.getId()));

                        if (n != null) {
                            subject = n;
                            user.setViewOffset(0);
                            user.setSubjectId(subject.getId());
                            user.resetState();
                            viewKind = ViewKind.viewDir;
                        }
                    }
                } else {
                    api.dialog(LangMap.Value.TYPE_FOLDER, user);
                    user.setWaitDirInput();
                    viewKind = ViewKind.none;
                }
                break;
            case mkGrant:
                api.dialog(subject.isDir() ? LangMap.Value.SEND_CONTACT_DIR : LangMap.Value.SEND_CONTACT_FILE, user, subject.getName());
                user.setWaitFileGranting();
                viewKind = ViewKind.none;
                break;
            case grantAccess:
                final User contact = new User();
                contact.setId(command.file.getOwner());
                contact.name = command.file.getName();
                contact.setLang(user.getLang());

                final User target = userService.resolveUser(contact);

                if (fsService.shareMissed(subject.getId(), target.getId(), user))
                    fsService.makeShare(command.file.getName(), user, subject.getId(), target.getId(), notNull(target.getLang(), "en"));

                viewKind = ViewKind.subjectShares;
                break;
            case mkLabel:
                user.resetState();
                if (!isEmpty(command.input)) {
                    if (fsService.entryMissed(command.input, user))
                        fsService.mk(TFileFactory.label(command.input, subject.getId(), user.getId()));

                    viewKind = ViewKind.viewDir;
                } else {
                    api.dialog(LangMap.Value.TYPE_LABEL, user);
                    user.setWaitLabelInput();
                    viewKind = ViewKind.none;
                }
                break;
            case unlockFile:
            case unlockDir:
                if (fsService.passwordFailed(user.getContestId(), command.input))
                    viewKind = user.isSearching() ? ViewKind.searchResults : ViewKind.viewDir;
                else {
                    subject = fsService.get(user.getContestId(), user);
                    user.setSubjectId(user.getContestId());
                    user.setViewOffset(0);

                    viewKind = command.type == unlockDir
                            ? (user.isSearching() ? ViewKind.viewSearchedDir : ViewKind.viewDir)
                            : (user.isSearching() ? ViewKind.viewSearchedFile : ViewKind.viewFile);
                }
                user.setContestId(null);
                break;
            case openDir:
                user.resetState();
                final TFile dir = byIdx(command.elementIdx, subject, user);

                if (breakOnLock(dir, user, User.Optz.UnlockDirInputWait))
                    break;

                user.setViewOffset(0);
                subject = dir;
                viewKind = ViewKind.viewDir;
                break;
            case openFile:
                user.resetState();
                final TFile file = byIdx(command.elementIdx, subject, user);

                if (breakOnLock(file, user, User.Optz.UnlockFileInputWait))
                    break;

                subject = file;
                viewKind = ViewKind.viewFile;
                break;
            case openLabel:
                final TFile label = byIdx(command.elementIdx, subject, user);
                user.resetState();
                subject = label;
                user.setSubjectId(subject.getId());
                viewKind = ViewKind.viewLabel;
                break;
            case openSearchedDir:
                final TFile sd = byIdx(command.elementIdx, subject, user);

                if (breakOnLock(sd, user, User.Optz.UnlockDirInputWait))
                    break;

                subject = sd;
                user.setViewOffset(0);
                viewKind = ViewKind.viewSearchedDir;
                break;
            case openSearchedFile:
                final TFile sf = byIdx(command.elementIdx, subject, user);

                if (breakOnLock(sf, user, User.Optz.UnlockFileInputWait))
                    break;

                subject = sf;
                viewKind = ViewKind.viewSearchedFile;
                break;
            case openSearchedLabel:
                subject = byIdx(command.elementIdx, subject, user);
                user.setSubjectId(subject.getId());
                viewKind = ViewKind.viewSearchedLabel;
                break;
            case renameDir:
            case renameFile:
                if (!isEmpty(command.input)) {
                    user.resetState();
                    if (!subject.getName().equals(command.input) && fsService.entryMissed(command.input, user)) {
                        subject.setName(command.input);
                        subject.setPath(Paths.get(subject.getPath()).getParent().resolve(command.input).toString());
                        fsService.updateMeta(subject, user);
                    }
                    viewKind = command.type == renameFile ? ViewKind.viewFile : ViewKind.viewDir;
                } else {
                    api.dialog(LangMap.Value.TYPE_RENAME, user, subject.getName());
                    if (command.type == renameFile)
                        user.setWaitFileRenameInput();
                    else
                        user.setWaitDirRenameInput();
                }
                break;
            case resetToRoot:
                user.setOptions(0);
                user.setQuery(null);
                user.setSubjectId(user.getRootId());
                if (user.getLastMessageId() > 0)
                    api.deleteMessage(user.getLastMessageId(), user.getId());
                user.setLastMessageId(0);
                subject = fsService.findRoot(user.getId());
                viewKind = ViewKind.viewDir;
                break;
            case share:
                user.resetState();
                user.setSharing();
                viewKind = ViewKind.subjectShares;
                break;
            case uploadFile:
                command.file.setParentId(subject.isDir() ? subject.getId() : subject.getParentId());
                command.file.setOwner(user.getId());

                fsService.mk(command.file);

                viewKind = subject.isDir() ? ViewKind.viewDir : ViewKind.none;
                break;
            case contextHelp:
                doHelp(subject, user);
                break;
            case unlock:
                fsService.unlockEntry(subject);
                viewKind = subject.isDir() ? ViewKind.gearSubject : ViewKind.viewFile;
                break;
            case lock:
                if (!isEmpty(command.input)) {
                    if (subject.getOwner() == user.getId()) {
                        final String salt = new BigInteger(130, TextUtils.rnd).toString(32);
                        final String password = hash256(salt + command.input);

                        fsService.lockEntry(subject, salt, password);
                    }

                    viewKind = subject.isDir() ? ViewKind.gearSubject : ViewKind.viewFile;
                } else {
                    api.dialog(subject.isDir() ? LangMap.Value.TYPE_LOCK_DIR : LangMap.Value.TYPE_LOCK_FILE, user, subject.getName());
                    user.setWaitLockInput();
                }
        }

        if (viewKind != ViewKind.none)
            doView(subject, viewKind, user);

        userService.update(user);
    }

    private void doHelp(final TFile subject, final User user) {
        if (user.isSearching())
            api.dialogUnescaped(LangMap.Value.SEARCHED_HELP, user, TgApi.voidKbd);
        else if (user.isSharing())
            api.dialogUnescaped(subject.isDir() ? LangMap.Value.SHARE_DIR_HELP : LangMap.Value.SHARE_FILE_HELP, user, TgApi.voidKbd);
        else if (user.isGearing() && !user.isOnTop())
            api.dialogUnescaped(LangMap.Value.GEAR_HELP, user, TgApi.voidKbd);
        else if (subject.isLabel())
            api.dialogUnescaped(LangMap.Value.LABEL_HELP, user, TgApi.voidKbd);
        else if (subject.isFile())
            api.dialogUnescaped(LangMap.Value.FILE_HELP, user, TgApi.voidKbd);
        else if (user.isOnTop())
            api.dialogUnescaped(LangMap.Value.ROOT_HELP, user, TgApi.voidKbd);
        else
            api.dialogUnescaped(LangMap.Value.LS_HELP, user, TgApi.voidKbd);
    }

    private void doView(final TFile subject, final ViewKind viewKind, final User user) {
        final TgApi.Keyboard kbd = new TgApi.Keyboard();
        final StringBuilder body = new StringBuilder(16);
        TFile file = null;
        String format = ParseMode.md2;

        try {
            switch (viewKind) {
                case gearSubject: {
                    final List<TFile> scope = scope(subject, user);
                    body.append(escapeMd(v(LangMap.Value.GEARING, user, notNull(subject.getPath(), "/"))));

                    if (!user.isOnTop()) {
                        if (subject.getOwner() == user.getId()) {
                            kbd.button(subject.isLocked() ? CommandType.unlock.b() : CommandType.lock.b());
                            kbd.button(CommandType.share.b());
                        }
                        if (subject.isRw())
                            kbd.button(CommandType.renameDir.b());
                        if (subject.getOwner() == user.getId())
                            kbd.button(CommandType.dropDir.b());
                    }

                    kbd.button(CommandType.cancel.b());

                    for (int i = 0; i < scope.size(); i++) {
                        kbd.newLine();
                        kbd.button(scope.get(i).toButton(i));
                    }
                }
                break;
                case searchResults: {
                    final List<TFile> scope = scope(subject, user);

                    body.append(escapeMd(v(LangMap.Value.SEARCHED, user, user.getQuery(), notNull(subject.getPath(), "/"))));
                    kbd.button(CommandType.cancelSearch.b());

                    if (scope.isEmpty())
                        body.append("\n_").append(v(LangMap.Value.NO_RESULTS, user)).append("_");
                    else {
                        body.append("\n_").append(escapeMd(v(LangMap.Value.RESULTS_FOUND, user, scope.size()))).append("_");

                        final List<TFile> sorted = scope.stream().sorted(sorter).collect(Collectors.toList());
                        final int skip = notNull(subject.getPath(), "/").length();

                        for (int i = user.getViewOffset(); i < Math.min(user.getViewOffset() + 10, sorted.size()); i++) {
                            kbd.newLine();
                            kbd.button(sorted.get(i).toSearchedButton(skip, scope.indexOf(sorted.get(i))));
                        }

                        if (user.getViewOffset() > 0 || scope.stream().filter(e -> e.getType() != ContentType.LABEL).count() > 10) {
                            kbd.newLine();

                            if (user.getViewOffset() > 0)
                                kbd.button(CommandType.rewind.b());
                            if (scope.stream().filter(e -> e.getType() != ContentType.LABEL).count() > 10)
                                kbd.button(CommandType.forward.b());
                        }
                    }
                }
                break;
                case subjectShares: {
                    final List<Share> scope = scope(subject, user);
                    final Share glob = scope.stream().filter(s -> s.getSharedTo() == 0).findAny().orElse(null);
                    final long countPers = scope.stream().filter(s -> s.getSharedTo() > 0).count();

                    body.append(v(subject.isDir() ? LangMap.Value.DIR_ACCESS : LangMap.Value.FILE_ACCESS, user, "*" + escapeMd(subject.getPath()) + "*"))
                            .append("\n\n")
                            .append(Strings.Uni.link).append(": ").append("_")
                            .append(escapeMd((glob != null ? "https://t.me/" + config.getString("service.bot.nick") + "?start=shared-" + glob.getId() :
                                    v(LangMap.Value.NO_GLOBAL_LINK, user)))).append("_\n");

                    if (countPers <= 0)
                        body.append(Strings.Uni.People + ": _").append(escapeMd(v(LangMap.Value.NO_PERSONAL_GRANTS, user))).append("_");

                    kbd.button(glob != null ? CommandType.dropEntryLink.b() : CommandType.makeEntryLink.b());
                    kbd.button(CommandType.mkGrant.b());
                    kbd.button(CommandType.cancel.b());

                    final AtomicInteger counter = new AtomicInteger(0);

                    scope.stream()
                            .filter(s -> !s.isGlobal())
                            .sorted(Comparator.comparing(Share::getName))
                            .forEach(s -> {
                                kbd.newLine();
                                kbd.button(CommandType.changeGrantRw.b(v(s.isReadWrite() ? LangMap.Value.SHARE_RW : LangMap.Value.SHARE_RO, user, s.getName()), counter.get()));
                                kbd.button(CommandType.dropGrant.b(counter.getAndIncrement()));
                            });
                }
                break;
                case viewDir: {
                    final List<TFile> scope = scope(subject, user);
                    body.append(notNull(escapeMd(subject.getPath()), "/"));

                    final StringBuilder ls = new StringBuilder();
                    scope.stream().filter(TFile::isLabel).sorted(Comparator.comparing(TFile::getName)).forEach(l -> ls.append('\n').append("```\n").append(escapeMd(l.name)).append("```\n"));

                    if (ls.length() > 0)
                        body.append(ls.toString());
                    else if (scope.isEmpty())
                        body.append("\n_").append(escapeMd(v(LangMap.Value.NO_CONTENT, user))).append("_");

                    if (!user.isOnTop())
                        kbd.button(CommandType.openParent.b());
                    if (subject.isRw()) {
                        kbd.button(CommandType.mkLabel.b());
                        kbd.button(CommandType.mkDir.b());
                        kbd.button(CommandType.gear.b());
                    }

                    final List<TFile> toButtons = scope.stream().sorted(sorter).filter(e -> e.getType() != ContentType.LABEL).collect(Collectors.toList());

                    for (int i = user.getViewOffset(); i < Math.min(user.getViewOffset() + 10, toButtons.size()); i++) {
                        kbd.newLine();
                        kbd.button(toButtons.get(i).toButton(scope.indexOf(toButtons.get(i))));
                    }

                    if (user.getViewOffset() > 0 || scope.stream().filter(e -> e.getType() != ContentType.LABEL).count() > 10) {
                        kbd.newLine();

                        if (user.getViewOffset() > 0)
                            kbd.button(CommandType.rewind.b());
                        if (scope.stream().filter(e -> e.getType() != ContentType.LABEL).count() > 10)
                            kbd.button(CommandType.forward.b());
                    }
                }
                break;
                case viewFile: {
                    file = subject;

                    body.append(notNull(escapeMd(file.getPath()), "/"));

                    kbd.button(CommandType.openParent.b());
                    if (file.isRw()) {
                        if (file.getOwner() == user.getId()) {
                            kbd.button(file.isLocked() ? CommandType.unlock.b() : CommandType.lock.b());
                            kbd.button(CommandType.share.b());
                        }

                        kbd.button(CommandType.renameFile.b(), CommandType.dropFile.b());
                    }
                }
                break;
                case viewLabel: {
                    body.append('*').append(notNull(escapeMd(subject.parentPath()), "/")).append("*\n\n").append(escapeMd(subject.name));

                    kbd.button(CommandType.openParent.b());

                    if (subject.isRw())
                        kbd.button(CommandType.editLabel.b(), CommandType.dropLabel.b());
                }
                break;
                case viewSearchedDir: {
                    final List<TFile> scope = fsService.list(subject.getId(), user);

                    body.append("_").append(escapeMd(v(LangMap.Value.SEARCHED, user, user.getQuery(), subject.parentPath()))).append("_\n");
                    body.append(escapeMd(subject.getName()));

                    final StringBuilder ls = new StringBuilder();
                    scope.stream().filter(TFile::isLabel).sorted(Comparator.comparing(TFile::getName)).forEach(l -> ls.append('\n').append("```\n").append(escapeMd(l.name)).append("```\n"));

                    if (ls.length() > 0)
                        body.append(ls.toString());
                    else if (scope.isEmpty())
                        body.append("\n_").append(escapeMd(v(LangMap.Value.NO_CONTENT, user))).append("_");

                    kbd.button(CommandType.backToSearch.b());
                    kbd.button(CommandType.mkLabel.b());
                    kbd.button(CommandType.mkDir.b());
                    kbd.button(CommandType.gear.b());

                    final List<TFile> toButtons = scope.stream().sorted(sorter).filter(e -> e.getType() != ContentType.LABEL)
                            .collect(Collectors.toList());

                    for (int i = user.getViewOffset(); i < Math.min(user.getViewOffset() + 10, toButtons.size()); i++) {
                        kbd.newLine();
                        kbd.button(toButtons.get(i).toButton(scope.indexOf(toButtons.get(i))));
                    }

                    if (user.getViewOffset() > 0 || scope.stream().filter(e -> e.getType() != ContentType.LABEL).count() > 10) {
                        kbd.newLine();

                        if (user.getViewOffset() > 0)
                            kbd.button(CommandType.rewind.b());
                        if (scope.stream().filter(e -> e.getType() != ContentType.LABEL).count() > 10)
                            kbd.button(CommandType.forward.b());
                    }
                }
                break;
                case viewSearchedFile: {
                    file = subject;

                    body.append("_").append(escapeMd(v(LangMap.Value.SEARCHED, user, user.getQuery(), subject.parentPath()))).append("_\n");
                    body.append(escapeMd(subject.getName()));

                    kbd.button(CommandType.backToSearch.b(), CommandType.share.b(), CommandType.renameFile.b(), CommandType.dropFile.b());
                }
                break;
                case viewSearchedLabel: {
                    body.append("_").append(escapeMd(v(LangMap.Value.SEARCHED, user, user.getQuery(), subject.parentPath()))).append("_\n");
                    body.append("```\n").append(subject.name).append("\n```");

                    kbd.button(CommandType.backToSearch.b(), CommandType.editLabel.b(), CommandType.dropLabel.b());
                }
                break;
            }
        } finally {
            api.sendContent(file, body.toString(), format, kbd, user);
        }
    }

    private boolean breakOnLock(final TFile entry, final User user, final User.Optz optz) {
        if (!entry.isLocked()) {
            user.setSubjectId(entry.getId());
            return false;
        }

        user.setContestId(entry.getId());
        api.dialog(entry.isDir() ? LangMap.Value.TYPE_PASSWORD_DIR : LangMap.Value.TYPE_PASSWORD_FILE, user, entry.getName());

        optz.set(user);

        return true;
    }

    private <T> List<T> scope(final TFile subject, final User user) {
        if (user.isSharing())
            return (List<T>) fsService.listShares(subject.getId(), user).stream().sorted(Comparator.comparing(Share::getName)).collect(Collectors.toList());

        if (user.isSearching())
            return (List<T>) fsService.search(user).stream().sorted(sorter).collect(Collectors.toList());

        if (user.isGearing())
            return (List<T>) fsService.listTyped(subject.getId(), ContentType.LABEL, user);

        return (List<T>) fsService.list(subject.getId(), user);
    }

    private <T> T byIdx(final int idx, final TFile subject, final User user) {
        return (T) scope(subject, user).get(idx);
    }
*/
}
