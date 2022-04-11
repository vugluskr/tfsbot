package services.impl;

import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.typesafe.config.Config;
import model.opds.OpdsBook;
import model.opds.OpdsPage;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import play.Logger;
import services.OpdsSearch;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static utils.TextUtils.getInt;
import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 06.04.2022 12:51
 * tfs ☭ sweat and blood
 */
@Singleton
public class OpdsSearchImpl implements OpdsSearch {
    private static final Logger.ALogger logger = Logger.of(OpdsSearch.class);
    private final String searchUrl;
    private final Function<String, String> urler;

    @Inject
    public OpdsSearchImpl(final Config config) {
        searchUrl = config.getString("service.opds");
        URL base = null;
        try {base = new URL(searchUrl);} catch (final Exception ignore) {}
        final URL b = base;
        urler = base == null ? null : path -> {
            try {
                final URL self = new URL(path);
                if (self.getProtocol().toLowerCase().startsWith("http"))
                    return self.toExternalForm();
            } catch (final Exception ignore) {}

            try {
                return new URL(b.getProtocol(), b.getHost(), b.getPort(), path).toExternalForm();
            } catch (MalformedURLException e) {
                logger.error(e.getMessage(), e);
            }

            return null;
        };
    }

    @Override
    public OpdsPage search(final String query, final int page) {
        String u = searchUrl;
        try {u = String.format(searchUrl, URLEncoder.encode(query, "UTF-8"), page);} catch (final Exception ignore) {}

        final OpdsPage p = new OpdsPage();
        p.setBooks(new ArrayList<>(0));
        p.setHasPrev(page > 0);

        try {
            final Document doc = getXml(u);
            final NodeList entries = doc.getElementsByTagName("entry");

            for (int i = 0; i < entries.getLength(); i++) {
                final Node el = entries.item(i);
                final OpdsBook b = new OpdsBook();

                for (int j = 0; j < el.getChildNodes().getLength(); j++) {
                    final Node child = el.getChildNodes().item(j);

                    switch (child.getNodeName()) {
                        case "category":
                            for (int z = 0; z < child.getAttributes().getLength(); z++)
                                if (child.getAttributes().item(z).getNodeName().equals("label")) {
                                    b.getGenres().add(child.getAttributes().item(z).getTextContent());
                                    break;
                                }

                            break;
                        case "author":
                            for (int z = 0; z < child.getChildNodes().getLength(); z++)
                                if (child.getChildNodes().item(z).getNodeName().equals("name")) {
                                    b.getAuthors().add(child.getChildNodes().item(z).getTextContent());
                                    break;
                                }

                            break;
                        case "dc:issued":
                            b.setYear(getInt(child.getTextContent()));
                            break;
                        case "id":
                            b.setId(child.getTextContent());
                            break;
                        case "title":
                            b.setTitle(child.getTextContent());
                            break;
                        case "link":
                            String type = "", href = "", rel = "";

                            for (int k = 0; k < child.getAttributes().getLength(); k++) {
                                final Node a = child.getAttributes().item(k);

                                switch (a.getNodeName()) {
                                    case "type":
                                        type = a.getNodeValue();
                                        break;
                                    case "href":
                                        href = a.getNodeValue();
                                        break;
                                    case "rel":
                                        rel = a.getNodeValue();
                                        break;
                                }
                            }

                            if (rel.equals("http://opds-spec.org/acquisition/open-access")) {
                                if (type.contains("application/fb2"))
                                    b.setFbLink(urler.apply(href));
                                else if (type.contains("application/epub"))
                                    b.setEpubLink(urler.apply(href));
                            }

                            break;
                    }
                }

                if (!isEmpty(b.getTitle()) && (!isEmpty(b.getFbLink()) || !isEmpty(b.getEpubLink()))) {
                    if (!isEmpty(b.getGenres())) {
                        final Set<String> cmtp = b.getGenres().stream().map(g -> {
                            if (ruNames.containsValue(g))
                                return ruNames.entrySet().stream().filter(e -> e.getValue().equalsIgnoreCase(g)).findAny().map(Map.Entry::getKey).orElse(null);
                            return null;
                        }).collect(Collectors.toSet());

                        b.getGenres().clear();
                        b.setGenres(cmtp);
                    }

                    p.getBooks().add(b);

                }
            }

            if (isEmpty(p.getBooks()))
                return p;

            final NodeList links = doc.getElementsByTagName("link");
            for (int i = 0; i < links.getLength(); i++)
                if (links.item(i).hasAttributes()) {
                    boolean next = false;
                    String href = "";

                    for (int j = 0; j < links.item(i).getAttributes().getLength(); j++) {
                        final Node n = links.item(i).getAttributes().item(j);

                        if (n.getNodeName().equals("rel") && n.getNodeValue().equals("next"))
                            next = true;
                        else if (n.getNodeName().equals("href"))
                            href = n.getNodeValue();
                    }

                    if (next && !isEmpty(href)) {
                        p.setHasNext(true);
                        break;
                    }
                }
        } catch (final Exception e) {
            logger.error("search error: " + e.getMessage(), e);
        }


        return p;
    }

    @Override
    public File loadFile(final OpdsBook book, final boolean fb2, final boolean epub) {
        try {
            final String url, ext;

            if (fb2) {
                url = book.getFbLink();
                ext = ".fb2.zip";
            } else if (epub) {
                url = book.getEpubLink();
                ext = ".epub";
            } else
                throw new IllegalArgumentException("Only fb2 | epub are supported");

            return loadFile(url, ext);
        } catch (final Exception e) {
            logger.error("File load error :: " + book + " ::\n" + e.getMessage(), e);
        }

        return null;
    }

    @Override
    public List<String> resolveGenrePath(final String path) {
        for (final Map.Entry<String, Set<String>> entry : genres.entrySet())
            if (entry.getValue().contains(path))
                return Arrays.asList(entry.getKey(), path);

        return resolveGenrePath("other");
    }

    @Override
    public String resolveGenreName(final String genreId) {
        return ruNames.getOrDefault(genreId, ruNames.get("other"));
    }

    private File loadFile(final String url, final String ext) throws Exception {
        final File tmp = File.createTempFile(String.valueOf(System.currentTimeMillis()), ext);
        try (final FileOutputStream bos = new FileOutputStream(tmp)) {
            getFile(url, bos);
        }

        return tmp;
    }

    private Document getXml(final String url) throws Exception {
        final Document d;
        try (final InputStream is = get(url)) {
            d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new InvalidXmlCharacterFilter(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))));
        }

        return d;
    }

    private void getFile(final String url, final FileOutputStream os) throws Exception {
        final InputStream is = get(url);

        int read;
        while ((read = is.read()) != -1)
            os.write(read);

        os.flush();

        try {is.close();} catch (final Exception ignore) {}
    }

    private InputStream get(final String url) throws Exception {
        final URLConnection cn = new URL(url).openConnection();

        cn.setDoInput(true);
        cn.setDoOutput(true);

        ((HttpURLConnection) cn).setRequestMethod("POST");
        ((HttpURLConnection) cn).setInstanceFollowRedirects(false);
        cn.setUseCaches(false);

        cn.setConnectTimeout(15000);
        cn.setReadTimeout(15000);

        cn.connect();

        final int code = ((HttpURLConnection) cn).getResponseCode();

        if (code / 200 == 1 && code % 200 < 100)
            return cn.getInputStream();

        if (code / 300 == 1 && code % 300 < 100)
            return get(cn.getHeaderField("location"));

        logger.warn("Not succeded code: " + code);

        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream(128); final InputStream is = ((HttpURLConnection) cn).getErrorStream()) {
            int read;
            while ((read = is.read()) != -1)
                bos.write(read);

            throw new Exception(url + " :: response error message: " + new String(bos.toByteArray(), StandardCharsets.UTF_8));
        }
    }

    private static class InvalidXmlCharacterFilter extends FilterReader {
        protected InvalidXmlCharacterFilter(Reader in) {
            super(in);
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int read = super.read(cbuf, off, len);
            if (read == -1) return read;

            for (int i = off; i < off + read; i++) {
                if (!XMLChar.isValid(cbuf[i])) cbuf[i] = '?';
            }
            return read;
        }
    }

    private static final Map<String, String> ruNames;
    private static final Map<String, Set<String>> genres;

    static {
        final Map<String, Set<String>> g = new HashMap<>(0);
        g.put("businnes", new HashSet<>(Arrays.asList("economics_ref", "popular_business", "org_behavior", "banking", "economics")));
        g.put("det", new HashSet<>(Arrays.asList("det_action", "detective", "det_irony", "det_history", "det_classic", "det_crime", "det_hard", "det_political", "det_police", "det_maniac", "det_su", "thriller", "det_espionage")));
        g.put("nonf", new HashSet<>(Arrays.asList("nonf_biography", "nonf_military", "military_special", "travel_notes", "nonfiction", "nonf_publicism")));
        g.put("tribe", new HashSet<>(Arrays.asList("auto_regulations", "home_sport", "home_pets", "home", "home_health", "home_collecting", "home_cooking", "sci_pedagogy", "home_entertain", "home_garden", "home_diy", "family", "home_sex", "home_crafts")));
        g.put("theater", new HashSet<>(Arrays.asList("drama_antique", "drama", "dramaturgy", "comedy", "vaudeville", "screenplays", "tragedy")));
        g.put("art", new HashSet<>(Arrays.asList("painting", "design", "art_criticism", "cine", "nonf_criticism", "sci_culture", "art_world_culture", "music", "notes", "architecture_book", "theatre")));
        g.put("comp", new HashSet<>(Arrays.asList("computers", "comp_hard", "comp_www", "comp_db", "tbg_computers")));
        g.put("child", new HashSet<>(Arrays.asList("children", "child_education", "child_det", "foreign_children", "prose_game", "child_classical", "child_prose", "child_tale_rus", "child_tale", "child_verse", "child_sf")));
        g.put("romance", new HashSet<>(Arrays.asList("love_history", "love_short", "love_sf", "love", "love_detective", "love_hard", "love_contemporary", "love_erotica")));
        g.put("sci", new HashSet<>(Arrays.asList("sci_medicine_alternative", "sci_theories", "sci_cosmos", "sci_biology", "sci_botany", "sci_veterinary", "military_history", "sci_oriental", "sci_geo", "sci_state", "sci_popular", "sci_zoo", "sci_history", "sci_philology", "sci_math", "sci_medicine", "science", "sci_social_studies", "sci_politics", "sci_psychology", "sci_phys", "sci_philosophy", "sci_chem", "sci_ecology", "sci_economy", "sci_juris", "sci_linguistic")));
        g.put("poet", new HashSet<>(Arrays.asList("palindromes", "poetry_for_classical", "poetry_classical", "poetry_rus_classical", "lyrics", "song_poetry", "poetry", "poetry_east", "poem", "poetry_for_modern", "poetry_modern", "poetry_rus_modern", "humor_verse")));
        g.put("adv", new HashSet<>(Arrays.asList("adv_story", "adv_indian", "adv_history", "adv_maritime", "adventure", "adv_modern", "child_adv", "adv_animal", "adv_geo", "tale_chivalry")));
        g.put("pros", new HashSet<>(Arrays.asList("aphorisms", "gothic_novel", "foreign_prose", "prose_history", "prose_classic", "literature_18", "literature_19", "literature_20", "prose_counter", "prose_magic", "story", "prose", "prose_military", "great_story", "prose_rus_classic", "prose_su_classics", "prose_contemporary", "foreign_antique", "prose_abs", "prose_neformatny", "epistolary_fiction")));
        g.put("left", new HashSet<>(Arrays.asList("periodic", "comics", "unfinished", "other", "network_literature", "fanfiction")));
        g.put("god", new HashSet<>(Arrays.asList("astrology", "religion_budda", "religion_hinduism", "religion_islam", "religion_judaism", "religion_catholicism", "religion_orthodoxy", "religion_protestantism", "sci_religion", "religion", "religion_self", "religion_christianity", "religion_esoterics", "religion_paganism")));
        g.put("ref", new HashSet<>(Arrays.asList("geo_guides", "ref_guide", "ref_dict", "reference", "ref_ref", "ref_encyc")));
        g.put("bc", new HashSet<>(Arrays.asList("antique", "antique_ant", "antique_east", "antique_russian", "antique_european")));
        g.put("tech", new HashSet<>(Arrays.asList("auto_business", "military_weapon", "equ_history", "sci_metal", "sci_radio", "sci_build", "sci_tech", "sci_transport")));
        g.put("tbg", new HashSet<>(Arrays.asList("sci_textbook", "tbg_higher", "tbg_secondary", "tbg_school")));
        g.put("sfict", new HashSet<>(Arrays.asList("sf_history", "sf_action", "sf_heroic", "sf_fantasy_city", "sf_detective", "sf_cyberpunk", "sf_space", "sf_litrpg", "sf_mystic", "fairy_fantasy", "sf", "popadancy", "sf_postapocalyptic", "russian_fantasy", "modern_tale", "sf_social", "sf_stimpank", "sf_technofantasy", "sf_horror", "sf_etc", "sf_fantasy", "hronoopera", "sf_epic", "sf_humor")));
        g.put("folk", new HashSet<>(Arrays.asList("epic", "child_folklore", "antique_myths", "folk_songs", "folk_tale", "proverbs", "folklore", "limerick")));
        g.put("laugh", new HashSet<>(Arrays.asList("humor_anecdote", "humor_satire", "humor", "humor_prose")));

        final Map<String, String> m = new HashMap<>(0);
        m.put("business", "Деловая литература");
        m.put("economics_ref", "Деловая литература");
        m.put("popular_business", "Карьера, кадры");
        m.put("org_behavior", "Маркетинг, PR");
        m.put("banking", "Финансы");
        m.put("economics", "Экономика");

        m.put("det", "Детективы и Триллеры");
        m.put("det_action", "Боевик");
        m.put("detective", "Детективы");
        m.put("det_irony", "Иронический детектив, дамский детективный роман");
        m.put("det_history", "Исторический детектив");
        m.put("det_classic", "Классический детектив");
        m.put("det_crime", "Криминальный детектив");
        m.put("det_hard", "Крутой детектив");
        m.put("det_political", "Политический детектив");
        m.put("det_police", "Полицейский детектив");
        m.put("det_maniac", "Про маньяков");
        m.put("det_su", "Советский детектив");
        m.put("thriller", "Триллер");
        m.put("det_espionage", "Шпионский детектив");

        m.put("nonf", "Документальная литература");
        m.put("nonf_biography", "Биографии и Мемуары");
        m.put("nonf_military", "Военная документалистика и аналитика");
        m.put("military_special", "Военное дело");
        m.put("travel_notes", "География, путевые заметки");
        m.put("nonfiction", "Документальная литература");
        m.put("nonf_publicism", "Публицистика");

        m.put("tribe", "Дом и семья");
        m.put("auto_regulations", "Автомобили и ПДД");
        m.put("home_sport", "Боевые искусства, спорт");
        m.put("home_pets", "Домашние животные");
        m.put("home", "Домоводство");
        m.put("home_health", "Здоровье");
        m.put("home_collecting", "Коллекционирование");
        m.put("home_cooking", "Кулинария");
        m.put("sci_pedagogy", "Педагогика, воспитание детей, литература для родителей");
        m.put("home_entertain", "Развлечения");
        m.put("home_garden", "Сад и огород");
        m.put("home_diy", "Сделай сам");
        m.put("family", "Семейные отношения");
        m.put("home_sex", "Семейные отношения, секс");
        m.put("home_crafts", "Хобби и ремесла");

        m.put("theater", "Драматургия");
        m.put("drama_antique", "Античная драма");
        m.put("drama", "Драма");
        m.put("dramaturgy", "Драматургия");
        m.put("comedy", "Комедия");
        m.put("vaudeville", "Мистерия, буффонада, водевиль");
        m.put("screenplays", "Сценарий");
        m.put("tragedy", "Трагедия");

        m.put("art", "Искусство, Искусствоведение, Дизайн");
        m.put("painting", "Живопись, альбомы, иллюстрированные каталоги");
        m.put("design", "Искусство и Дизайн");
        m.put("art_criticism", "Искусствоведение");
        m.put("cine", "Кино");
        m.put("nonf_criticism", "Критика");
        m.put("sci_culture", "Культурология");
        m.put("art_world_culture", "Мировая художественная культура");
        m.put("music", "Музыка");
        m.put("notes", "Партитуры");
        m.put("architecture_book", "Скульптура и архитектура");
        m.put("theatre", "Театр");

        m.put("comp", "Компьютеры и Интернет");
        m.put("computers", "Зарубежная компьютерная, околокомпьютерная литература");
        m.put("comp_hard", "Компьютерное 'железо' (аппаратное обеспечение), цифровая обработка сигналов");
        m.put("comp_www", "ОС и Сети, интернет");
        m.put("comp_db", "Программирование, программы, базы данных");
        m.put("tbg_computers", "Учебные пособия, самоучители");


        m.put("child", "Литература для детей");
        m.put("children", "Детская литература");
        m.put("child_education", "Детская образовательная литература");
        m.put("child_det", "Детская остросюжетная литература");
        m.put("foreign_children", "Зарубежная литература для детей");
        m.put("prose_game", "Игры, упражнения для детей");
        m.put("child_classical", "Классическая детская литература");
        m.put("child_prose", "Проза для детей");
        m.put("child_tale_rus", "Русские сказки");
        m.put("child_tale", "Сказки народов мира");
        m.put("child_verse", "Стихи для детей");
        m.put("child_sf", "Фантастика для детей");

        m.put("romance", "Любовные романы");
        m.put("love_history", "Исторические любовные романы");
        m.put("love_short", "Короткие любовные романы");
        m.put("love_sf", "Любовное фэнтези, любовно-фантастические романы");
        m.put("love", "Любовные романы");
        m.put("love_detective", "Остросюжетные любовные романы");
        m.put("love_hard", "Порно");
        m.put("love_contemporary", "Современные любовные романы");
        m.put("love_erotica", "Эротическая литература");

        m.put("sci", "Наука, Образование");
        m.put("sci_medicine_alternative", "Альтернативная медицина");
        m.put("sci_theories", "Альтернативные науки и научные теории");
        m.put("sci_cosmos", "Астрономия и Космос");
        m.put("sci_biology", "Биология, биофизика, биохимия");
        m.put("sci_botany", "Ботаника");
        m.put("sci_veterinary", "Ветеринария");
        m.put("military_history", "Военная история");
        m.put("sci_oriental", "Востоковедение");
        m.put("sci_geo", "Геология и география");
        m.put("sci_state", "Государство и право");
        m.put("sci_popular", "Зарубежная образовательная литература, зарубежная прикладная, научно-популярная литература");
        m.put("sci_zoo", "Зоология");
        m.put("sci_history", "История");
        m.put("sci_philology", "Литературоведение");
        m.put("sci_math", "Математика");
        m.put("sci_medicine", "Медицина");
        m.put("science", "Научная литература");
        m.put("sci_social_studies", "Обществознание, социология");
        m.put("sci_politics", "Политика");
        m.put("sci_psychology", "Психология и психотерапия");
        m.put("sci_phys", "Физика");
        m.put("sci_philosophy", "Философия");
        m.put("sci_chem", "Химия");
        m.put("sci_ecology", "Экология");
        m.put("sci_economy", "Экономика");
        m.put("sci_juris", "Юриспруденция");
        m.put("sci_linguistic", "Языкознание, иностранные языки");

        m.put("poet", "Поэзия");
        m.put("palindromes", "Визуальная и экспериментальная поэзия, верлибры, палиндромы");
        m.put("poetry_for_classical", "Классическая зарубежная поэзия");
        m.put("poetry_classical", "Классическая поэзия");
        m.put("poetry_rus_classical", "Классическая русская поэзия");
        m.put("lyrics", "Лирика");
        m.put("song_poetry", "Песенная поэзия");
        m.put("poetry", "Поэзия");
        m.put("poetry_east", "Поэзия Востока");
        m.put("poem", "Поэма, эпическая поэзия");
        m.put("poetry_for_modern", "Современная зарубежная поэзия");
        m.put("poetry_modern", "Современная поэзия");
        m.put("poetry_rus_modern", "Современная русская поэзия");
        m.put("humor_verse", "Юмористические стихи, басни");


        m.put("adv", "Приключения");
        m.put("adv_story", "Авантюрный роман");
        m.put("adv_indian", "Вестерн, про индейцев");
        m.put("adv_history", "Исторические приключения");
        m.put("adv_maritime", "Морские приключения");
        m.put("adventure", "Приключения");
        m.put("adv_modern", "Приключения в современном мире");
        m.put("child_adv", "Приключения для детей и подростков");
        m.put("adv_animal", "Природа и животные");
        m.put("adv_geo", "Путешествия и география");
        m.put("tale_chivalry", "Рыцарский роман");


        m.put("pros", "Проза");
        m.put("aphorisms", "Афоризмы, цитаты");
        m.put("gothic_novel", "Готический роман");
        m.put("foreign_prose", "Зарубежная классическая проза");
        m.put("prose_history", "Историческая проза");
        m.put("prose_classic", "Классическая проза");
        m.put("literature_18", "Классическая проза XVII-XVIII веков");
        m.put("literature_19", "Классическая проза ХIX века");
        m.put("literature_20", "Классическая проза ХX века");
        m.put("prose_counter", "Контркультура");
        m.put("prose_magic", "Магический реализм");
        m.put("story", "Малые литературные формы прозы: рассказы, эссе, новеллы, феерия");
        m.put("prose", "Проза");
        m.put("prose_military", "Проза о войне");
        m.put("great_story", "Роман, повесть");
        m.put("prose_rus_classic", "Русская классическая проза");
        m.put("prose_su_classics", "Советская классическая проза");
        m.put("prose_contemporary", "Современная русская и зарубежная проза");
        m.put("foreign_antique", "Средневековая классическая проза");
        m.put("prose_abs", "Фантасмагория, абсурдистская проза");
        m.put("prose_neformatny", "Экспериментальная, неформатная проза");
        m.put("epistolary_fiction", "Эпистолярная проза");


        m.put("left", "Прочее");
        m.put("periodic", "Журналы, газеты");
        m.put("comics", "Комиксы");
        m.put("unfinished", "Незавершенное");
        m.put("other", "Неотсортированное");
        m.put("network_literature", "Самиздат, сетевая литература");
        m.put("fanfiction", "Фанфик");

        m.put("god", "Религия, духовность, эзотерика");
        m.put("astrology", "Астрология и хиромантия");
        m.put("religion_budda", "Буддизм");
        m.put("religion_hinduism", "Индуизм");
        m.put("religion_islam", "Ислам");
        m.put("religion_judaism", "Иудаизм");
        m.put("religion_catholicism", "Католицизм");
        m.put("religion_orthodoxy", "Православие");
        m.put("religion_protestantism", "Протестантизм");
        m.put("sci_religion", "Религиоведение");
        m.put("religion", "Религия, религиозная литература");
        m.put("religion_self", "Самосовершенствование");
        m.put("religion_christianity", "Христианство");
        m.put("religion_esoterics", "Эзотерика, эзотерическая литература");
        m.put("religion_paganism", "Язычество");

        m.put("ref", "Справочная литература");
        m.put("geo_guides", "Путеводители, карты, атласы");
        m.put("ref_guide", "Руководства");
        m.put("ref_dict", "Словари");
        m.put("reference", "Справочная литература");
        m.put("ref_ref", "Справочники");
        m.put("ref_encyc", "Энциклопедии");


        m.put("bc", "Старинное");
        m.put("antique", "antique");
        m.put("antique_ant", "Античная литература");
        m.put("antique_east", "Древневосточная литература");
        m.put("antique_russian", "Древнерусская литература");
        m.put("antique_european", "Европейская старинная литература");

        m.put("tech", "Техника");
        m.put("auto_business", "Автодело");
        m.put("military_weapon", "Военное дело, военная техника и вооружение");
        m.put("equ_history", "История техники");
        m.put("sci_metal", "Металлургия");
        m.put("sci_radio", "Радиоэлектроника");
        m.put("sci_build", "Строительство и сопромат");
        m.put("sci_tech", "Технические науки");
        m.put("sci_transport", "Транспорт и авиация");

        m.put("tbg", "Учебники и пособия");
        m.put("sci_textbook", "Учебники и пособия");
        m.put("tbg_higher", "Учебники и пособия ВУЗов");
        m.put("tbg_secondary", "Учебники и пособия для среднего и специального образования");
        m.put("tbg_school", "Школьные учебники и пособия, рефераты, шпаргалки");


        m.put("sfict", "Фантастика");
        m.put("sf_history", "Альтернативная история");
        m.put("sf_action", "Боевая фантастика");
        m.put("sf_heroic", "Героическая фантастика");
        m.put("sf_fantasy_city", "Городское фэнтези");
        m.put("sf_detective", "Детективная фантастика");
        m.put("sf_cyberpunk", "Киберпанк");
        m.put("sf_space", "Космическая фантастика");
        m.put("sf_litrpg", "ЛитРПГ");
        m.put("sf_mystic", "Мистика");
        m.put("fairy_fantasy", "Мифологическое фэнтези");
        m.put("sf", "Научная Фантастика");
        m.put("popadancy", "Попаданцы");
        m.put("sf_postapocalyptic", "Постапокалипсис");
        m.put("russian_fantasy", "Славянское фэнтези");
        m.put("modern_tale", "Современная сказка");
        m.put("sf_social", "Социально-психологическая фантастика");
        m.put("sf_stimpank", "Стимпанк");
        m.put("sf_technofantasy", "Технофэнтези");
        m.put("sf_horror", "Ужасы");
        m.put("sf_etc", "Фантастика");
        m.put("sf_fantasy", "Фэнтези");
        m.put("hronoopera", "Хроноопера");
        m.put("sf_epic", "Эпическая фантастика");
        m.put("sf_humor", "Юмористическая фантастика");


        m.put("folk", "Фольклор");
        m.put("epic", "Былины, эпопея");
        m.put("child_folklore", "Детский фольклор");
        m.put("antique_myths", "Мифы. Легенды. Эпос");
        m.put("folk_songs", "Народные песни");
        m.put("folk_tale", "Народные сказки");
        m.put("proverbs", "Пословицы, поговорки");
        m.put("folklore", "Фольклор, загадки folklore");
        m.put("limerick", "Частушки, прибаутки, потешки");

        m.put("laugh", "Юмор");
        m.put("humor_anecdote", "Анекдоты");
        m.put("humor_satire", "Сатира");
        m.put("humor", "Юмор");
        m.put("humor_prose", "Юмористическая проза");

        ruNames = Collections.unmodifiableMap(m);
        genres = Collections.unmodifiableMap(g);
    }
}
