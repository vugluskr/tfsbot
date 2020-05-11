package utils;

import model.TFile;
import model.User;
import model.UserAlias;
import model.telegram.api.UpdateRef;
import play.libs.Json;
import services.CmdService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static utils.TextUtils.*;

public class Test {

    public static void main(final String[] argz) throws Exception {
        final UpdateRef ref = Json.fromJson(Json.parse("{\"update_id\":125898848,\"message\":{\"message_id\":1118,\"from\":{\"id\":69172785,\"is_bot\":false," +
                "\"first_name\":\"Denis\",\"last_name\":\"Danilin\",\"username\":\"piu_piu_laser\",\"language_code\":\"ru\"},\"chat\":{\"id\":69172785,\"first_name\":\"Denis\"," +
                "\"last_name\":\"Danilin\",\"username\":\"piu_piu_laser\",\"type\":\"private\"},\"date\":1589213755,\"text\":\"rm \\\"проверка лейбла\\\"\"}}\n"), UpdateRef.class);

//        final String[] parts = splitCmd("rm 'проверка лейбла'");
        final String[] parts = splitCmd(ref.getMessage().getText());
        System.out.println(Arrays.toString(parts));
        final User user = new User();
//        new CmdService().handleCmd(ref.getMessage().getText(), user);
        user.setPwd("/test1");

        System.out.println(strings2paths(user.getPwd(), parts));
    }

    public static void mainsdf0935(final String[] argz) throws Exception {
        final String cmd = ">> проверка лейбла";
        final User user = new User();
        user.aliases.add(new UserAlias(">>", "label"));

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
            final TFile monoFile = monoPath == null ? null : findPath(monoPath, user);

            if (monoPath == null || monoFile == null || monoFile.isLabel())
                return;

            if (monoFile.isDir())
                doCd(monoFile, user);
            else
                doGet(monoFile, user);
        }
    }

    private static TFile findPath(final String path, final User user) {
        return new TFile();
    }

    private static void handleCmd(final String cmd, final User user) {
        System.out.println("Exec cmd: " + cmd);
    }

    private static void doCd(final TFile file,  final User user) {
        System.out.println("Exec CD to " + file.getPath());
    }

    private static void doGet(final TFile file,  final User user) {
        System.out.println("Exec GET  " + file.getPath());
    }

    public static void main234235(final String[] args) throws Exception {
        String input = "ls «software term» on the fly and synchron";
//        String[] terms = input.split("\"?( |$)(?=(([^\"]*\"){2})*[^\"]*$)\"?");
        String[] terms = input//.split("'?( |$)(?=(([^']*'){2})*[^']*$)'?");
                .split("«?( |$)(?=(«[^«»]*»)*[^»]*$)»?");
//                .split("«?( |$)(?=[^«»]*[^«»]*$)»?");

        System.out.println(Arrays.toString(terms));
    }

    public static void main234(final String[] args) throws Exception {
        final MdPadTable md = new MdPadTable("Test table", new String[]{"Size", "Date", "", "", "Name"});
        md.setAligns(MdPadTable.Align.RIGHT, MdPadTable.Align.LEFT, MdPadTable.Align.RIGHT, MdPadTable.Align.LEFT, MdPadTable.Align.LEFT);
        md.add("4096");md.add("мая");md.add("3");md.add("10:32");md.add("app");
        md.add("1018");md.add("мая");md.add("5");md.add("10:54");md.add("build.sbt");
        md.add("4096");md.add("мая");md.add("5");md.add("13:09");md.add("conf/");
        md.add("688");md.add("мая");md.add("4");md.add("14:31");md.add("manage.html");
        md.add("396");md.add("мая");md.add("5");md.add("20:05");md.add("systemd-private-f61c8650abdf4ca48cbae88a3d944b5c-colord.service-nNeN3c");
        md.add("4096");md.add("мая");md.add("1");md.add("13:38");md.add("project/");
        md.add("4096");md.add("мая");md.add("5");md.add("16:30");md.add("target/");
        md.add("4096");md.add("мая");md.add("5");md.add("13:14");md.add("test");
        md.add("0");md.add("мая");md.add("4");md.add("17:24");md.add("test.html");

        System.out.println(md);
    }

    public static void main3(final String[] args) throws Exception {
//        TextUtils.strings2paths(Paths.get("/tmp"), "a/b/*", "/c/*/d").forEach(System.out::println);
        System.out.println(escapeMd("*bold \\\\*text*\n" +
                "_italic \\\\*text_\n" +
                "__underline__\n" +
                "~strikethrough~\n" +
                "*bold _italic bold ~italic bold strikethrough~ __underline italic bold___ bold*\n" +
                "[inline URL](http://www.example.com/)\n" +
                "[inline mention of a user](tg://user?id=123456789)\n" +
                "`inline fixed-width code`\n" +
                "```\n" +
                "pre-formatted fixed-width code block\n" +
                "```\n" +
                "```python\n" +
                "pre-formatted fixed-width code block written in the Python programming language\n" +
                "```\n"));
    }

    public static void main2(final String[] args) throws Exception {
//        final String input = "mv /test/more/parts/to/go/with/../upto/.././././.././////mkdior/firs/../ t last/compare/lost/file /some/real/file.txt /tmp";
        final String input = "mv hola /tmp \t ";

        final String[] parts = input.split("\\s");

        final Path current = Paths.get("/here/we");

        final List<Path> paths = Arrays.stream(Arrays.copyOfRange(parts, 1, parts.length))
                .map(s -> {
                    final Path p = Paths.get(s);

                    return p.isAbsolute() ? p : current.resolve(p);
                })
                .map(p -> {
                    try {
                        return Paths.get(p.toFile().getCanonicalPath());
                    } catch (IOException ignore) { }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        for (final Path path : paths)
            System.out.println(path + " : " + path.isAbsolute() + " | " + path.getParent() + " | " + path.getFileName());
    }
}
