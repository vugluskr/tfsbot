package services;

import model.TFile;
import model.User;
import model.UserAlias;
import model.telegram.ContentType;
import model.telegram.api.TextRef;
import utils.MdPadTable;
import utils.UOpts;

import javax.inject.Inject;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static utils.TextUtils.*;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.05.2020
 * tfs ☭ sweat and blood
 */
public class CmdService {

    @Inject
    private FsService fsService;

    @Inject
    private UserService userService;

    @Inject
    private TgApi tgApi;

    private final String help;

    public CmdService() {
        final MdPadTable helpTab = new MdPadTable("Commands list", new String[]{"Command", "Description"});
        helpTab.add("cd <dir>");
        helpTab.add("Change directory");
        helpTab.add("get <file>");
        helpTab.add("Get previously stored file");
        helpTab.add("ls");
        helpTab.add("Listing of current directory");
        helpTab.add("mkdir <name>");
        helpTab.add("Make directory");
        helpTab.add("mv <src> <trg>");
        helpTab.add("Move source to target");
        helpTab.add("pwd");
        helpTab.add("Current directory full path");
        helpTab.add("rm <name>");
        helpTab.add("Remove file or dir (recursively)");
        helpTab.add("label <text>");
        helpTab.add("Add label to current dir");
        helpTab.add("alias <als>=<cmd>");
        helpTab.add("Alias any command");
        helpTab.setLastColUnformatted(true);
        help = helpTab.toString();
    }

    public void handleCmd(final String cmd, final User user) {
        switch (cmd.toLowerCase()) {
            case "gui":
                if (UOpts.Gui.is(user))
                    UOpts.Gui.clear(user);
                else
                    UOpts.Gui.set(user);
                userService.updateOpts(user);
                doPropmpt(user);
                break;
            case "/start":
            case "help":
            case "?":
            case "/?":
                tgApi.sendMessage(new TextRef(help, user.getId()).setMd2());
                return;
            case "./":
            case "./.":
            case "cd ./":
            case "cd ./.":
                doPropmpt(user);
                break;
            case "cd ..":
            case "cd ../":
            case "cd ../.":
                if (user.getDirId() > 1) {
                    final TFile dir = fsService.getParentOf(user.getDirId(), user);
                    user.setPwd(dir.getPath());
                    user.setDirId(dir.getId());
                    userService.updatePwd(user);
                }
                doPropmpt(user);
                break;
            case "/":
            case "/.":
            case "cd":
            case "cd ~/":
            case "cd ~/.":
            case "cd /":
            case "cd /.":
                if (user.getDirId() > 1) {
                    user.setPwd("/");
                    user.setDirId(1);
                    userService.updatePwd(user);
                }
                doPropmpt(user);
                break;
            case "l":
            case "ls":
            case "ls .":
            case "/ls":
                doLs(null, user);
                break;
            case "pwd":
            case "/pwd":
                tgApi.sendMessage(new TextRef("`" + user.getPwd() + "`", user.getId()).setMd());
                break;
            default:
                if (cmd.toLowerCase().startsWith("mkdir ") || cmd.toLowerCase().startsWith("rm ") || cmd.toLowerCase().startsWith("mv ") || cmd.toLowerCase().startsWith("ls ") || cmd.toLowerCase().startsWith("l ")) {
                    if (cmd.toLowerCase().startsWith("mkdir "))
                        doMkdir(cmd, user);
                    else if (cmd.toLowerCase().startsWith("rm "))
                        doRm(cmd, user);
                    else if (cmd.toLowerCase().startsWith("mv "))
                        doMv(cmd, user);
                    else
                        doLs(cmd, user);
                    break;
                } else if (cmd.toLowerCase().startsWith("alias ")) {
                    final String aliased = cmd.substring(6).trim();
                    final int spot = aliased.indexOf('=') ;
                    if (spot < 1 || spot + 1 == cmd.length())
                        return;

                    final UserAlias alias = new UserAlias(aliased.substring(0, spot).trim());

                    if (isEmpty(alias) || alias.getAlias().indexOf(' ') != -1 || !user.aliases.add(alias))
                        return;

                    alias.setCmd(aliased.substring(spot + 1).replace("alias ", ""));

                    if (isEmpty(alias.getCmd()))
                        return;

                    userService.insertAlias(alias, user);
                    doPropmpt(user);
                } else if (cmd.toLowerCase().startsWith("label ")) {
                    final String name = cmd.substring(6).trim();
                    if (name.isEmpty())
                        return;

                    final TFile file = new TFile();
                    file.setType(ContentType.LABEL);
                    file.setName(name);
                    file.setRefId("--");

                    fsService.upload(file, user);

                    doPropmpt(user);
                } else {
                    final String[] sa = cmd.split(" ");
                    final UserAlias alias = user.aliases.stream().filter(ua -> ua.getAlias().equals(sa[0])).findAny().orElse(null);

                    if (alias != null) {
                        final String args = sa.length > 1 ? " " + String.join(" ", Arrays.copyOfRange(sa, 1, sa.length)) : "";
                        final String[] cmds = alias.getCmd().contains(";") ? alias.getCmd().split(Pattern.quote(";")) : new String[] {alias.getCmd()};

                        for (final String cPart : cmds)
                            if (!isEmpty(cPart))
                                handleCmd(cPart + args, user);
                    } else {
                        final String monoPath = validatePath(cmd.toLowerCase().startsWith("cd ") || cmd.toLowerCase().startsWith("get ") ? cmd.substring(cmd.indexOf(' ') + 1).trim() : cmd, user.getPwd());
                        final TFile monoFile = monoPath == null ? null : fsService.findPath(monoPath, user);

                        if (monoPath == null || monoFile == null || monoFile.isLabel())
                            return;

                        if (monoFile.isDir())
                            doCd(monoFile, user);
                        else
                            doGet(monoFile, user);
                    }
                }
        }
    }

    private void doMkdir(final String cmd, final User user) {
        final String current = user.getPwd();
        final Map<String, TFile> checkCache = new HashMap<>();
        final Set<TFile> toCreate = new HashSet<>(0);

        Arrays.stream(splitCmd(cmd))
                .filter(s -> !isEmpty(s) && !isGlob(s))
                .map(s -> validatePath(s, current))
                .filter(Objects::nonNull)
                .forEach(path -> {
                    TFile parent = new TFile(1);
                    final StringBuilder pathHold = new StringBuilder(16);

                    for (final Path p : Paths.get(path)) {
                        pathHold.append("/").append(p);

                        if (!checkCache.containsKey(pathHold.toString()))
                            checkCache.put(pathHold.toString(), fsService.findPath(pathHold.toString(), user));

                        final TFile dir = checkCache.get(pathHold.toString());

                        if (dir != null) {
                            parent = dir;
                            continue;
                        }

                        final TFile tc = new TFile(parent.getId(), p.getFileName().toString());
                        parent = tc;
                        toCreate.add(tc);
                    }
                });

        if (!toCreate.isEmpty()) {
            fsService.mkdirs(toCreate, user);

            doPropmpt(user);
        }
    }

    private void doGet(final TFile file, final User user) {
        if (file.isDir())
            doCd(file, user);
        else
            tgApi.sendFile(file, user.getId());
    }

    private void doRm(final String cmd, final User user) {
        final String[] parts = splitCmd(cmd);
        if (isEmpty(parts))
            return;

        final List<TFile> toRm = globalize(parts, user);

        if (isEmpty(toRm))
            return;

        fsService.rm(toRm.stream().map(TFile::getId).collect(Collectors.toList()), user);
        doPropmpt(user);
    }

    private void doMv(final String cmd, final User user) {
        final String[] parts = splitCmd(cmd);

        if (isEmpty(parts) || parts.length < 2)
            return;

        final String current = user.getPwd();

        final String targetPath = validatePath(parts[parts.length - 1], current);

        if (targetPath == null)
            return;

        final TFile target = fsService.findPath(targetPath, user);

        if (parts.length > 2 && target == null)
            return;

        final List<TFile> toMove = globalize(Arrays.copyOfRange(parts, 0, parts.length - 1), user);

        if (toMove.size() > 1 && target == null)
            return;

        if (toMove.size() == 1) {
            if (target == null) {
                if (!Optional.ofNullable(Paths.get(targetPath).getParent()).orElse(Paths.get(user.getPwd())).equals(Paths.get(user.getPwd())))
                    return;

                toMove.get(0).setParentId(user.getDirId());
                toMove.get(0).setName(Paths.get(targetPath).getFileName().toString());
            } else {
                if (target.isDir()) // move into dir
                    toMove.get(0).setParentId(target.getParentId());
                else {
                    if (toMove.get(0).isDir())
                        return;

                    // file rename
                    toMove.get(0).setParentId(target.getParentId());
                    toMove.get(0).setName(target.getName());
                }
            }

            fsService.updateMeta(toMove.get(0), user);
        } else { // move into dir
            if (target == null)
                return;

            toMove.forEach(f -> f.setParentId(target.getId()));
            fsService.updateMetas(toMove, user);
        }

        doPropmpt(user);
    }

    private void doCd(final TFile dir, final User user) {
        if (dir == null)
            return;

        if (dir.isDir()) {
            if (user.getDirId() != dir.getId()) {
                user.setPwd(dir.getPath());
                user.setDirId(dir.getId());
                userService.updatePwd(user);
            }

            doPropmpt(user);
        } else
            doGet(dir, user);
    }

    private void doPropmpt(final User user) {
        tgApi.sendMessage(new TextRef(user.prompt(), user.getId()).setMd2());
    }

    private void doLs(final String cmd, final User user) {
        final StringBuilder s = new StringBuilder();

        if (isEmpty(cmd))
            s.append(handleDir(fsService.get(user.getDirId(), user), user));
        else {
            final List<TFile> toList = globalize(splitCmd(cmd), user);

            toList.stream()
                    .filter(f -> f != null && f.isDir())
                    .sorted(Comparator.comparing(TFile::getName))
                    .forEach(f -> s.append(handleDir(f, user)));

            final MdPadTable files = new MdPadTable(5);

            toList.stream()
                    .filter(f -> f != null && !f.isDir())
                    .sorted(Comparator.comparing(TFile::getName))
                    .forEach(f -> lsEntry(f, files));

            s.append(files);
        }

        tgApi.sendMessage(new TextRef(s.toString(), user.getId()).setMd2());
    }

    private List<TFile> globalize(final String[] parts, final User user) {
        final String current = notNull(user.getPwd(), "/");
        final Map<String, String> globParents = new HashMap<>();
        final List<String> cleanParts = new ArrayList<>(0);

        final List<TFile> toList = new ArrayList<>(0);

        for (final String part : parts)
            if (isGlob(part)) {
                final String path = validatePath(part, current);
                if (path == null)
                    continue;

                globParents.put(path, getGlobParent(path, current));
            } else
                cleanParts.add(validatePath(part, current));

        toList.addAll(fsService.findPaths(cleanParts, user));

        if (!globParents.isEmpty()) {
            final Map<String, TFile> dirs = fsService.findPaths(globParents.values(), user)
                    .stream().filter(TFile::isDir).collect(Collectors.toMap(TFile::getPath, f -> f));
            final Map<Long, List<TFile>> listCache = new HashMap<>();

            globParents.forEach((path, parent) -> {
                final TFile dir = dirs.get(parent);
                if (dir == null)
                    return;

                final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + path);

                if (!listCache.containsKey(dir.getId()))
                    listCache.put(dir.getId(), fsService.list(dir.getId(), user)
                            .stream()
                            .filter(f -> matcher.matches(Paths.get(f.getPath()))).collect(Collectors.toList()));

                toList.addAll(listCache.get(dir.getId()));
            });
        }

        return toList;
    }

    private String handleDir(final TFile dir, final User user) {
        final MdPadTable ls = new MdPadTable(5, escapeMd(dir.getId() == 1 ? "~" : dir.getPath()));
        final List<TFile> files = new ArrayList<>(0);

        files.addAll(fsService.list(dir.getId(), user));
        files.sort((o1, o2) -> {
            final int res = Boolean.compare(o2.isDir(), o1.isDir());

            return res != 0 ? res : o1.getName().compareTo(o2.getName());
        });

        ls.setAligns(MdPadTable.Align.RIGHT, MdPadTable.Align.LEFT, MdPadTable.Align.RIGHT, MdPadTable.Align.LEFT, MdPadTable.Align.LEFT);

        for (final TFile f : files)
            lsEntry(f, ls);

        return ls.toString();
    }

    private void lsEntry(final TFile f, final MdPadTable table) {
        final LocalDateTime ldt = ZonedDateTime.from(Instant.ofEpochMilli(f.getIndate()).atZone(ZoneId.systemDefault())).toLocalDateTime();
        table.add(f.isDir() || f.isLabel() ? "-" : f.getSize());
        table.add(ldt.getMonth().getDisplayName(TextStyle.SHORT, Locale.US));
        table.add(ldt.getDayOfMonth());
        table.add(ChronoUnit.MONTHS.between(ldt, LocalDateTime.now()) >= 3 ? ldt.getYear() : ldt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        table.add(f.getName() + (f.isDir() ? "/" : ""));
    }

    private boolean isGlob(final String path) {
        int tmp;
        return !isEmpty(path) && (
                path.contains("*")
                        || path.contains("?")
                        || ((tmp = path.indexOf('[')) != -1 && path.indexOf(']') > tmp)
                        || ((tmp = path.indexOf('{')) != -1 && path.indexOf('}') > tmp)
        );
    }

    private String getGlobParent(final String path, final String current) {
        if (path == null)
            return current;

        if (!path.contains("/"))
            return current;

        final int idx = IntStream.of(path.indexOf('*'), path.indexOf('?'), path.indexOf('{'), path.indexOf('['))
                .filter(a -> a != -1)
                .min()
                .orElse(-1);

        final String parent = idx == -1 ? current : path.substring(0, idx);

        return isEmpty(parent) || parent.equals("/") ? "/" : parent.contains("/") ? parent.substring(0, parent.lastIndexOf('/')) : current;
    }
}
