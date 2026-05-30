package iuh.fit.se.modules.ai.application.service;

import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort.BookContext;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AiCatalogMatchSupport {

    private static final Pattern REQUESTED_BOOK_COUNT_PATTERN = Pattern.compile("\\b(\\d{1,2})\\b");
    private static final List<String> STRICT_TOPICS = List.of(
            "trinh tham",
            "van hoc",
            "kinh te",
            "ky nang song",
            "thieu nhi",
            "cong nghe thong tin",
            "lap trinh",
            "kinh doanh",
            "quan tri",
            "nau an",
            "am thuc",
            "kinh di");
    private static final Map<String, List<String>> JOB_AUDIENCE_KEYWORDS = Map.ofEntries(
            Map.entry("ky su phan mem",
                    List.of("lap trinh", "code", "web", "app", "java", "reactjs", "nodejs", "javascript", "typescript",
                            "html", "css", "spring boot", "api", "database", "sql", "mysql", "mongodb", "frontend",
                            "backend", "fullstack", "git", "github", "debug", "software engineer", "developer")),

            Map.entry("thiet ke ui ux",
                    List.of("giao dien", "trai nghiem nguoi dung", "figma", "prototype", "wireframe", "user flow",
                            "ux research", "ui design", "ux design", "mockup", "design system", "persona",
                            "customer journey", "responsive", "mobile app", "website", "adobe xd", "photoshop",
                            "usability testing", "interaction design")),

            Map.entry("digital marketing",
                    List.of("seo", "facebook ads", "google ads", "content marketing", "quang cao", "social media",
                            "tiktok", "youtube", "instagram", "email marketing", "branding", "kpi", "chien dich",
                            "google analytics", "landing page", "copywriting", "content", "viral", "truyen thong",
                            "marketing online")),

            Map.entry("ke toan",
                    List.of("so sach", "hoa don", "thue", "bao cao tai chinh", "kiem toan", "thu chi", "cong no",
                            "bang luong", "excel", "chung tu", "quyet toan", "ke khai thue", "doanh thu", "chi phi",
                            "loi nhuan", "vat", "erp", "tai chinh", "ngan sach")),

            Map.entry("nhan su",
                    List.of("tuyen dung", "phong van", "dao tao", "luong thuong", "hr", "hop dong lao dong", "bao hiem",
                            "cham cong", "nhan vien", "onboarding", "candidate", "headhunt", "van hoa cong ty",
                            "danh gia nhan su", "kpi", "quan ly nhan su", "noi quy", "phuc loi")),

            Map.entry("giao vien",
                    List.of("giang day", "hoc sinh", "bai giang", "kiem tra", "giao an", "lop hoc", "dao tao",
                            "su pham", "mon hoc", "cham bai", "thi cu", "phu huynh", "ky nang su pham", "elearning",
                            "day hoc online", "kien thuc", "huong dan", "truyen dat")),

            Map.entry("bac si",
                    List.of("kham benh", "dieu tri", "benh vien", "y te", "chan doan", "benh nhan", "toa thuoc",
                            "phau thuat", "suc khoe", "xet nghiem", "noi khoa", "ngoai khoa", "cap cuu",
                            "tu van suc khoe", "ho so benh an", "bac si chuyen khoa", "phong kham")),

            Map.entry("dieu duong",
                    List.of("cham soc benh nhan", "thuoc", "benh vien", "y te", "tiem thuoc", "truyen dich",
                            "do huyet ap", "theo doi suc khoe", "ho tro bac si", "cap cuu", "benh nhan", "phong benh",
                            "cham soc", "ho so y te", "dieu tri", "ve sinh y te")),

            Map.entry("duoc si",
                    List.of("thuoc", "nha thuoc", "duoc pham", "don thuoc", "tu van thuoc", "ban thuoc", "lieu dung",
                            "tac dung phu", "bao quan thuoc", "duoc lieu", "san xuat thuoc", "kiem nghiem thuoc",
                            "y te", "thuc pham chuc nang", "toa thuoc", "duoc hoc")),

            Map.entry("luat su",
                    List.of("phap luat", "tu van phap ly", "hop dong", "toa an", "tranh chap", "ho so phap ly",
                            "luat doanh nghiep", "luat lao dong", "luat dan su", "luat hinh su", "dai dien phap ly",
                            "kien tung", "cong chung", "bang chung", "van ban phap luat", "tu van hop dong")),

            Map.entry("kien truc su",
                    List.of("ban ve", "cong trinh", "thiet ke nha", "autocad", "kien truc", "mat bang", "noi that",
                            "ngoai that", "3d max", "sketchup", "revit", "quy hoach", "xay dung", "thiet ke cong trinh",
                            "ban ve ky thuat", "phoi canh", "nha o", "biet thu")),

            Map.entry("ky su xay dung",
                    List.of("thi cong", "cong trinh", "ket cau", "giam sat", "xay dung", "vat lieu xay dung", "ban ve",
                            "du toan", "cong truong", "be tong", "cot thep", "an toan lao dong", "quan ly du an",
                            "nghiem thu", "tien do", "ky thuat cong trinh", "ha tang")),

            Map.entry("cong nhan san xuat",
                    List.of("nha may", "day chuyen", "lap rap", "san pham", "san xuat", "may moc",
                            "kiem tra chat luong", "dong goi", "ca kip", "lao dong pho thong", "xuong san xuat",
                            "van hanh may", "nguyen lieu", "quy trinh", "an toan lao dong", "qc")),

            Map.entry("nhan vien ban hang",
                    List.of("tu van khach hang", "doanh so", "san pham", "ban hang", "sale", "chot don", "khach hang",
                            "tu van", "bao gia", "don hang", "thi truong", "khuyen mai", "cham soc khach hang",
                            "cua hang", "showroom", "kpi", "hoa hong")),

            Map.entry("cham soc khach hang",
                    List.of("cskh", "ho tro", "giai dap", "khieu nai", "tong dai", "khach hang", "call center",
                            "phan hoi", "tu van", "bao hanh", "dich vu", "ticket", "hotline", "chat support",
                            "email support", "giai quyet van de", "hai long khach hang")),

            Map.entry("nhan vien ngan hang",
                    List.of("tin dung", "vay von", "tai khoan", "tai chinh", "ngan hang", "the tin dung",
                            "gui tiet kiem", "giao dich", "khach hang", "lai suat", "ho so vay", "the atm",
                            "internet banking", "tu van tai chinh", "kiem soat rui ro", "thanh toan")),

            Map.entry("chuyen vien tai chinh",
                    List.of("dau tu", "chung khoan", "ngan sach", "phan tich", "tai chinh", "co phieu", "trai phieu",
                            "bao cao tai chinh", "quan ly von", "loi nhuan", "rui ro", "thi truong tai chinh",
                            "ke hoach tai chinh", "dinh gia", "excel", "forecast", "cash flow")),

            Map.entry("moi gioi bat dong san",
                    List.of("nha dat", "can ho", "du an", "moi gioi", "bat dong san", "dat nen", "chung cu", "nha pho",
                            "biet thu", "khach hang", "tu van", "phap ly nha dat", "hop dong mua ban", "san giao dich",
                            "cho thue", "mua ban nha", "gia dat")),

            Map.entry("huong dan vien du lich",
                    List.of("tour", "du lich", "diem tham quan", "khach du lich", "lich trinh", "thuyet minh",
                            "khach san", "ve may bay", "dia diem", "van hoa", "lich su", "tham quan", "du lich noi dia",
                            "du lich quoc te", "ngoai ngu", "dich vu du lich")),

            Map.entry("dau bep",
                    List.of("nau an", "nha hang", "thuc don", "mon an", "che bien", "bep", "nguyen lieu", "am thuc",
                            "gia vi", "an toan thuc pham", "bep truong", "so che", "trang tri mon an", "mon viet",
                            "mon au", "mon a", "quan an", "khach san")),

            Map.entry("nhan vien phuc vu",
                    List.of("order", "khach hang", "ban an", "phuc vu", "nha hang", "quan an", "cafe", "thuc don",
                            "don mon", "thu ngan", "don dep", "setup ban", "dich vu", "giao tiep", "ca lam",
                            "tiep don khach", "phuc vu ban")),

            Map.entry("tai xe",
                    List.of("lai xe", "van chuyen", "giao thong", "bang lai", "oto", "xe tai", "xe khach", "duong di",
                            "gps", "giao hang", "an toan giao thong", "bao duong xe", "tai xe cong nghe", "taxi",
                            "logistics", "don khach")),

            Map.entry("shipper",
                    List.of("giao hang", "don hang", "van chuyen", "ship", "lay hang", "tra hang", "cod", "khach hang",
                            "dia chi", "ung dung giao hang", "grab", "be", "shopeefood", "gojek", "don online",
                            "thanh toan", "giao nhanh")),

            Map.entry("phi cong",
                    List.of("hang khong", "may bay", "chuyen bay", "buong lai", "co truong", "co pho", "duong bay",
                            "an toan bay", "san bay", "kiem soat khong luu", "bay quoc te", "bay noi dia",
                            "ky thuat bay", "hang hang khong", "lich bay")),

            Map.entry("tiep vien hang khong",
                    List.of("hanh khach", "dich vu", "chuyen bay", "hang khong", "may bay", "an toan bay",
                            "phuc vu hanh khach", "ngoai ngu", "san bay", "boarding", "hanh ly", "quy trinh bay",
                            "giao tiep", "emergency", "flight attendant")),

            Map.entry("nha bao",
                    List.of("tin tuc", "phong van", "truyen thong", "bao chi", "bai viet", "su kien", "phong su",
                            "bien tap", "thoi su", "thong tin", "bao dien tu", "truyen hinh", "noi dung", "tac nghiep",
                            "nguon tin", "viet bao")),

            Map.entry("content creator",
                    List.of("tiktok", "youtube", "video", "sang tao noi dung", "content", "quay video", "edit video",
                            "livestream", "social media", "facebook", "instagram", "viral", "kịch bản", "trend",
                            "influencer", "blogger", "reels", "shorts")),

            Map.entry("mc",
                    List.of("dan chuong trinh", "su kien", "san khau", "micro", "giao tiep", "hoat ngon", "kich ban",
                            "truyen hinh", "event", "dam cuoi", "hoi thao", "talkshow", "livestream", "thuyet trinh",
                            "khai mac", "ket noi khan gia")),

            Map.entry("nhiep anh gia",
                    List.of("may anh", "chup hinh", "chinh sua anh", "photoshop", "lightroom", "studio", "anh cuoi",
                            "chan dung", "san pham", "su kien", "bo cuc", "anh nghe thuat", "lens", "anh sang",
                            "quay phim", "retouch", "camera")),

            Map.entry("thiet ke do hoa",
                    List.of("photoshop", "illustrator", "banner", "logo", "poster", "branding", "typography", "mau sac",
                            "bo cuc", "an pham", "social post", "profile cong ty", "catalogue", "brochure",
                            "thiet ke hinh anh", "visual design", "designer")),

            Map.entry("bien tap vien",
                    List.of("bien tap", "xuat ban", "noi dung", "chinh sua", "bai viet", "ban thao", "sach", "tap chi",
                            "bao chi", "noi dung so", "kiem loi", "ngon ngu", "truyen thong", "editor",
                            "content editor", "duyet noi dung")),

            Map.entry("tho dien",
                    List.of("dien dan dung", "sua chua", "lap dat", "day dien", "o cam", "cong tac", "tu dien",
                            "dien lanh", "bao tri", "an toan dien", "mach dien", "dien cong nghiep", "thiet bi dien",
                            "kiem tra dien", "sua dien", "he thong dien")),

            Map.entry("tho co khi",
                    List.of("may moc", "han", "tien", "gia cong", "co khi", "cat got", "lap rap", "bao tri may",
                            "ban ve ky thuat", "kim loai", "may tien", "may phay", "cnc", "sua chua may",
                            "xuong co khi", "che tao")),

            Map.entry("tho sua xe",
                    List.of("dong co", "bao duong", "phu tung", "sua xe", "xe may", "oto", "thay dau", "lop xe",
                            "phanh", "ac quy", "kiem tra xe", "bao tri", "may xe", "he thong dien xe", "gara",
                            "sua chua", "chan doan loi")),

            Map.entry("nong dan",
                    List.of("trong trot", "chan nuoi", "nong san", "ruong", "lua", "rau cu", "trai cay", "phan bon",
                            "thuoc tru sau", "may nong nghiep", "thu hoach", "gieo trong", "dat dai", "nong nghiep",
                            "vat nuoi", "tuoi tieu")),

            Map.entry("ky su nong nghiep",
                    List.of("cay trong", "phan bon", "nong nghiep", "giong cay", "chan nuoi", "dat trong", "sau benh",
                            "nang suat", "nong san", "cong nghe nong nghiep", "tuoi tieu", "thuoc bao ve thuc vat",
                            "trang trai", "quy trinh san xuat", "nong nghiep sach")),

            Map.entry("ngu dan",
                    List.of("danh bat ca", "tau thuyen", "hai san", "bien", "luoi ca", "thuy san", "ca", "tom", "muc",
                            "cang ca", "bao quan hai san", "di bien", "ngu cu", "khai thac thuy san", "thuyen vien",
                            "danh bat xa bo")),

            Map.entry("cong an",
                    List.of("an ninh", "dieu tra", "bao ve phap luat", "trat tu", "giao thong", "phong chong toi pham",
                            "ho khau", "can cuoc", "tuan tra", "phap luat", "xu ly vi pham", "cong an nhan dan",
                            "bao ve nguoi dan", "an toan xa hoi")),

            Map.entry("quan nhan",
                    List.of("quan doi", "quoc phong", "huan luyen", "bo doi", "chien si", "ky luat", "bao ve to quoc",
                            "doanh trai", "vu khi", "chien dau", "nhiem vu", "quoc phong an ninh", "luc luong vu trang",
                            "ren luyen")),

            Map.entry("bao ve",
                    List.of("an ninh", "giam sat", "tuan tra", "bao ve", "kiem soat ra vao", "camera", "ca truc",
                            "toa nha", "cong ty", "giu xe", "an toan", "bao cao su co", "kiem tra", "khach ra vao",
                            "noi quy")),

            Map.entry("thu ky",
                    List.of("van phong", "lich lam viec", "ho so", "sap xep lich", "cuoc hop", "ghi chep",
                            "soan thao van ban", "email", "tai lieu", "dat lich hen", "tro ly", "hanh chinh", "bao cao",
                            "giao tiep", "quan ly lich")),

            Map.entry("nhan vien hanh chinh",
                    List.of("van thu", "giay to", "quan ly ho so", "hanh chinh", "cong van", "hop dong", "luu tru",
                            "soan thao van ban", "van phong pham", "cham cong", "le tan", "dat phong hop",
                            "mua sam noi bo", "quan ly tai san", "ho tro nhan su")),

            Map.entry("nhan vien logistics",
                    List.of("kho van", "chuoi cung ung", "xuat nhap khau", "van tai", "kho hang", "don hang",
                            "giao nhan", "ton kho", "container", "shipping", "warehouse", "supply chain", "tracking",
                            "phieu xuat kho", "phieu nhap kho", "dieu phoi", "giao hang")),

            Map.entry("chuyen vien xuat nhap khau",
                    List.of("hai quan", "van tai quoc te", "chung tu", "xuat khau", "nhap khau", "invoice",
                            "packing list", "bill of lading", "co", "cq", "logistics", "incoterms", "container",
                            "khai bao hai quan", "forwarder", "thong quan", "hop dong ngoai thuong")),

            Map.entry("data analyst",
                    List.of("sql", "power bi", "thong ke", "phan tich du lieu", "excel", "python", "dashboard",
                            "report", "tableau", "data visualization", "business intelligence", "etl", "database",
                            "data cleaning", "data mining", "kpi", "forecast", "bao cao du lieu")),

            Map.entry("ky su ai",
                    List.of("machine learning", "deep learning", "python", "ai", "artificial intelligence", "nlp",
                            "computer vision", "chatbot", "llm", "tensorflow", "pytorch", "data science",
                            "neural network", "model training", "image recognition", "predictive model", "openai",
                            "gpt")),

            Map.entry("tester", List.of("kiem thu", "bug", "test case", "qa", "qc", "manual testing",
                    "automation testing", "selenium", "postman", "api testing", "test plan", "test script", "jira",
                    "quality assurance", "quality control", "bao cao loi", "regression test", "performance test")),

            Map.entry("product manager",
                    List.of("san pham", "roadmap", "agile", "scrum", "product owner", "backlog", "user story",
                            "khach hang", "thi truong", "ux", "business", "kpi", "mvp", "feature", "requirement",
                            "stakeholder", "quan ly san pham", "chien luoc san pham")),

            Map.entry("business analyst",
                    List.of("phan tich nghiep vu", "yeu cau", "quy trinh", "requirement", "use case", "user story",
                            "brd", "srs", "flowchart", "uml", "stakeholder", "gap analysis", "business process",
                            "tai lieu nghiep vu", "wireframe", "meeting", "phan tich he thong")),

            Map.entry("hoc sinh",
                    List.of("hoc sinh", "hoc tap", "truong hoc", "lop hoc", "bai tap", "kiem tra", "thi cu", "on tap",
                            "giao vien", "ban hoc", "sach giao khoa", "vo ghi", "diem so", "hoc ky", "tot nghiep",
                            "cap 1", "cap 2", "cap 3", "trung hoc co so", "trung hoc pho thong", "thuyet trinh",
                            "du an hoc tap", "ngoai khoa", "hoc nhom", "hoc online", "luyen thi", "de thi",
                            "ky nang mem", "hoc bai", "nghi giai lao")),

            Map.entry("sinh vien",
                    List.of("sinh vien", "dai hoc", "cao dang", "hoc tap", "tin chi", "giang vien", "chuyen nganh",
                            "hoc phi", "hoc bong", "nghien cuu", "bao cao", "do an", "khoa luan", "thuyet trinh",
                            "du an", "thuc tap", "intern", "fresher", "viec lam", "cau lac bo", "ky nang mem",
                            "lap trinh", "cong nghe thong tin", "kinh te", "quan tri kinh doanh", "marketing",
                            "tai chinh", "ke toan", "tot nghiep", "nghe nghiep")),

            Map.entry("freelancer",
                    List.of("lam viec tu do", "du an", "khach hang", "remote", "online", "hop dong", "deadline",
                            "portfolio", "thiet ke", "lap trinh", "viet content", "chinh sua video", "dich thuat",
                            "marketing", "upwork", "fiverr", "freelance job", "tu do thoi gian")));

    private AiCatalogMatchSupport() {
    }

    static int resolveRequestedBookCount(String userMessage, int defaultCount) {
        if (userMessage == null || userMessage.isBlank()) {
            return Math.max(defaultCount, 1);
        }

        Matcher matcher = REQUESTED_BOOK_COUNT_PATTERN.matcher(userMessage);
        if (matcher.find()) {
            return clampBookCount(Integer.parseInt(matcher.group(1)));
        }

        String normalized = normalize(userMessage);
        if (normalized.contains("mot "))
            return 1;
        if (normalized.contains("hai "))
            return 2;
        if (normalized.contains("ba "))
            return 3;
        if (normalized.contains("bon ") || normalized.contains("tu "))
            return 4;
        if (normalized.contains("nam "))
            return 5;
        return Math.max(defaultCount, 1);
    }

    static boolean hasRequestedBookCount(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        if (REQUESTED_BOOK_COUNT_PATTERN.matcher(userMessage).find()) {
            return true;
        }
        String normalized = normalize(userMessage);
        return normalized.contains("mot ")
                || normalized.contains("hai ")
                || normalized.contains("ba ")
                || normalized.contains("bon ")
                || normalized.contains("tu ")
                || normalized.contains("nam ");
    }

    static List<BookContext> keepOnlyExplicitTopicMatches(String userMessage, List<BookContext> books) {
        List<String> titleTerms = titleTermsIn(userMessage);
        if (!titleTerms.isEmpty()) {
            return books.stream()
                    .filter(book -> matchesAnyTitleTerm(book, titleTerms))
                    .toList();
        }

        List<String> authorTerms = authorTermsIn(userMessage);
        if (!authorTerms.isEmpty()) {
            return books.stream()
                    .filter(book -> matchesAnyAuthorTerm(book, authorTerms))
                    .toList();
        }

        List<String> audienceTerms = audienceTermsIn(userMessage);
        if (!audienceTerms.isEmpty()) {
            return books.stream()
                    .filter(book -> matchesAnyAudienceTerm(book, audienceTerms))
                    .toList();
        }

        List<String> topics = explicitTopicsIn(userMessage);
        if (topics.isEmpty()) {
            return books;
        }

        return books.stream()
                .filter(book -> matchesAnyTopic(book, topics))
                .toList();
    }

    static List<BookContext> keepOnlyCatalogIntentKeywordMatches(String userMessage, List<BookContext> books) {
        if (hasRankingIntent(userMessage)) {
            return books;
        }
        List<String> keywords = catalogIntentKeywordsIn(userMessage);
        if (keywords.isEmpty()) {
            return books;
        }

        return books.stream()
                .filter(book -> matchesAnyCatalogKeyword(book, keywords))
                .toList();
    }

    static boolean hasStrictTopic(String userMessage) {
        return !titleTermsIn(userMessage).isEmpty()
                || !authorTermsIn(userMessage).isEmpty()
                || !audienceTermsIn(userMessage).isEmpty()
                || !explicitTopicsIn(userMessage).isEmpty();
    }

    static boolean hasCatalogIntentKeywords(String userMessage) {
        return !catalogIntentKeywordsIn(userMessage).isEmpty();
    }

    static boolean hasRankingIntent(String userMessage) {
        String normalized = normalize(userMessage);
        return normalized.contains("ban chay")
                || normalized.contains("sach hot")
                || normalized.contains("hot")
                || normalized.contains("bestseller")
                || normalized.contains("xep hang")
                || normalized.contains("top sach")
                || normalized.contains("sach ban nhieu");
    }

    static boolean hasTitleSearchIntent(String userMessage) {
        return hasTitleSearchIntentNormalized(normalize(userMessage));
    }

    static boolean hasAuthorSearchIntent(String userMessage) {
        return !authorTermsIn(userMessage).isEmpty();
    }

    static boolean hasAudienceSearchIntent(String userMessage) {
        return !audienceTermsIn(userMessage).isEmpty();
    }

    static List<String> categoryTopicsIn(String userMessage) {
        if (hasTitleSearchIntent(userMessage)
                || hasAuthorSearchIntent(userMessage)
                || hasAudienceSearchIntent(userMessage)
                || hasRankingIntent(userMessage)) {
            return List.of();
        }
        List<String> explicitTopics = explicitTopicsIn(userMessage);
        if (!explicitTopics.isEmpty()) {
            return explicitTopics;
        }
        return catalogIntentKeywordsIn(userMessage);
    }

    static String productLink(Long bookId) {
        return bookId == null ? "/books" : "/books/" + bookId;
    }

    static String normalizeText(String value) {
        return normalize(value);
    }

    private static List<String> catalogIntentKeywordsIn(String userMessage) {
        if (hasStrictTopic(userMessage)) {
            return List.of();
        }
        String normalized = normalize(userMessage);
        if (normalized.isBlank() || !hasCatalogIntent(normalized)) {
            return List.of();
        }

        String cleaned = normalized;
        for (String phrase : List.of(
                "goi y cho toi",
                "goi y",
                "tim sach",
                "sach ve",
                "sach theo",
                "the loai",
                "thuoc the loai",
                "sach",
                "quyen",
                "cuon",
                "cho toi",
                "hay",
                "ve")) {
            cleaned = cleaned.replaceAll("\\b" + Pattern.quote(phrase) + "\\b", " ");
        }
        cleaned = cleaned.replaceAll("\\b\\d{1,2}\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.length() < 2) {
            return List.of();
        }
        return List.of(cleaned);
    }

    private static boolean hasCatalogIntent(String normalizedMessage) {
        return normalizedMessage.contains("sach")
                || normalizedMessage.contains("goi y")
                || normalizedMessage.contains("tim ");
    }

    private static int clampBookCount(int count) {
        if (count < 1) {
            return 1;
        }
        return Math.min(count, 5);
    }

    private static List<String> explicitTopicsIn(String userMessage) {
        if (!titleTermsIn(userMessage).isEmpty()) {
            return List.of();
        }
        String normalized = normalize(userMessage);
        return STRICT_TOPICS.stream()
                .filter(normalized::contains)
                .toList();
    }

    private static List<String> titleTermsIn(String userMessage) {
        String normalized = normalize(userMessage);
        if (!hasTitleSearchIntentNormalized(normalized)) {
            return List.of();
        }
        String extractedTerm = extractTitleTerm(normalized);
        if (!extractedTerm.isBlank()) {
            return List.of(extractedTerm);
        }
        return STRICT_TOPICS.stream()
                .filter(normalized::contains)
                .toList();
    }

    private static List<String> authorTermsIn(String userMessage) {
        String normalized = normalize(userMessage);
        for (String marker : List.of("tac gia", "cua tac gia", "sach cua", "sach viet boi", "viet boi")) {
            int index = normalized.indexOf(marker);
            if (index < 0) {
                continue;
            }
            String term = normalized.substring(index + marker.length()).trim();
            term = stripLeadingNoise(term);
            return term.isBlank() ? List.of() : List.of(term);
        }
        return List.of();
    }

    private static List<String> expandAudienceTerms(String audienceTerm) {
        String normalizedTerm = normalize(audienceTerm);
        if (normalizedTerm.isBlank()) {
            return List.of();
        }

        return JOB_AUDIENCE_KEYWORDS.entrySet().stream()
                .filter(entry -> normalizedTerm.contains(entry.getKey()) || entry.getKey().contains(normalizedTerm))
                .findFirst()
                .map(entry -> {
                    List<String> keywords = entry.getValue();
                    return java.util.stream.Stream.concat(
                            java.util.stream.Stream.of(normalizedTerm, entry.getKey()),
                            keywords.stream())
                            .distinct()
                            .toList();
                })
                .orElseGet(() -> List.of(normalizedTerm));
    }

    private static List<String> audienceTermsIn(String userMessage) {
        String normalized = normalize(userMessage);
        for (String marker : List.of("danh cho", "phu hop voi", "cho nguoi", "sach cho")) {
            int index = normalized.indexOf(marker);
            if (index < 0) {
                continue;
            }
            String term = normalized.substring(index + marker.length()).trim();
            term = stripLeadingNoise(term);
            return expandAudienceTerms(term);
        }
        return List.of();
    }

    private static String stripLeadingNoise(String term) {
        String cleaned = term;
        for (String prefix : List.of("toi", "minh", "em", "anh", "chi", "la", "ve")) {
            cleaned = cleaned.replaceFirst("^" + Pattern.quote(prefix) + "\\s+", "");
        }
        return cleaned.trim();
    }

    private static boolean hasTitleSearchIntentNormalized(String normalizedMessage) {
        return normalizedMessage.contains("trong ten")
                || normalizedMessage.contains("ten sach")
                || normalizedMessage.contains("co chu")
                || normalizedMessage.contains("chua chu")
                || normalizedMessage.contains("co tu")
                || normalizedMessage.contains("chua tu");
    }

    private static String extractTitleTerm(String normalizedMessage) {
        for (String marker : List.of("co chu", "chua chu", "co tu", "chua tu")) {
            int start = normalizedMessage.indexOf(marker);
            if (start < 0) {
                continue;
            }
            String tail = normalizedMessage.substring(start + marker.length()).trim();
            for (String endMarker : List.of("trong ten sach", "trong ten", "ten sach")) {
                int end = tail.indexOf(endMarker);
                if (end >= 0) {
                    return tail.substring(0, end).trim();
                }
            }
            return tail.trim();
        }
        return "";
    }

    private static boolean matchesAnyTitleTerm(BookContext book, List<String> titleTerms) {
        String title = normalize(book.title());
        return titleTerms.stream().anyMatch(title::contains);
    }

    private static boolean matchesAnyAuthorTerm(BookContext book, List<String> authorTerms) {
        String author = normalize(book.author());
        return authorTerms.stream()
                .map(AiCatalogMatchSupport::normalize)
                .filter(term -> !term.isBlank())
                .anyMatch(author::contains);
    }

    private static boolean matchesAnyCatalogKeyword(BookContext book, List<String> keywords) {
        String haystack = normalize(String.join(" ",
                nullToEmpty(book.title()),
                nullToEmpty(book.author()),
                nullToEmpty(book.description()),
                joinValues(book.keywords()),
                joinValues(book.categoryNames())));
        String paddedHaystack = " " + haystack + " ";
        return keywords.stream()
                .map(AiCatalogMatchSupport::normalize)
                .filter(keyword -> !keyword.isBlank())
                .anyMatch(keyword -> paddedHaystack.contains(" " + keyword + " "));
    }

    private static boolean matchesAnyAudienceTerm(BookContext book, List<String> keywords) {
        String strongHaystack = normalize(String.join(" ",
                nullToEmpty(book.title()),
                joinValues(book.keywords()),
                joinValues(book.categoryNames())));
        String paddedStrongHaystack = " " + strongHaystack + " ";
        boolean hasStrongMatch = keywords.stream()
                .map(AiCatalogMatchSupport::normalize)
                .filter(keyword -> !keyword.isBlank())
                .anyMatch(keyword -> paddedStrongHaystack.contains(" " + keyword + " "));
        if (hasStrongMatch) {
            return true;
        }

        String description = normalize(book.description());
        if (description.isBlank() || keywords.isEmpty()) {
            return false;
        }
        String audience = normalize(keywords.get(0));
        return description.contains("danh cho " + audience)
                || description.contains("phu hop voi " + audience)
                || description.contains("cho nguoi " + audience)
                || description.contains("sach cho " + audience);
    }

    private static boolean matchesAnyTopic(BookContext book, List<String> topics) {
        String haystack = normalize(String.join(" ",
                nullToEmpty(book.title()),
                nullToEmpty(book.author()),
                nullToEmpty(book.description()),
                joinValues(book.keywords()),
                joinValues(book.categoryNames())));
        return topics.stream().anyMatch(haystack::contains);
    }

    private static String joinValues(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(" ", values.stream().filter(Objects::nonNull).toList());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
