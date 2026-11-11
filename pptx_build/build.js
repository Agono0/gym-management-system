const pptxgen = require("pptxgenjs");

// ---------- palette (calm teal / slate) ----------
const DARK = "16323E";      // dark teal-slate (cover / dividers / closing)
const PRIMARY = "2E6B7A";   // calm teal
const PRIMARY_MID = "5A93A1";
const TINT = "E7F0F2";      // light teal tint (cards)
const TINT2 = "F4F8F9";     // lighter tint
const ACCENT = "D98E3B";    // muted amber (sparing)
const TEXT = "1F3540";
const MUTED = "5E7480";
const WHITE = "FFFFFF";
const DARKMUTED = "A9C3CC"; // muted text on dark bg

const F = "Segoe UI";       // Arabic-capable, ships with Windows
const FC = "Consolas";      // code terms

const W = 13.33, H = 7.5, M = 0.5;
let pageNo = 0;

const pres = new pptxgen();
pres.layout = "LAYOUT_WIDE";
pres.author = "Gym-OS Team";
pres.title = "Gym-OS Team Guide";

// ---------- helpers ----------
const bu = () => ({ code: "2022", indent: 10 });
const shadow = () => ({ type: "outer", color: "1F3540", blur: 7, offset: 2, angle: 90, opacity: 0.14 });

function slideBase(bg) {
  const s = pres.addSlide();
  s.background = { color: bg || WHITE };
  pageNo++;
  return s;
}

function pageNum(s, dark) {
  s.addText(String(pageNo).padStart(2, "0"), {
    x: M, y: H - 0.42, w: 0.8, h: 0.3, fontSize: 12, fontFace: F,
    color: dark ? DARKMUTED : MUTED, align: "left", margin: 0,
  });
}

function header(s, titleAr, tagEn) {
  s.addText(titleAr, {
    x: M, y: 0.32, w: W - 2 * M, h: 0.62, fontSize: 25, bold: true,
    fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0,
  });
  if (tagEn) {
    s.addText(tagEn, {
      x: M, y: 0.40, w: 4.6, h: 0.4, fontSize: 12, fontFace: FC,
      color: MUTED, align: "left", margin: 0,
    });
  }
}

function card(s, x, y, w, h, fill, withShadow) {
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x, y, w, h, fill: { color: fill || TINT }, rectRadius: 0.07,
    line: { type: "none" }, shadow: withShadow ? shadow() : undefined,
  });
}

// Arabic bullet list (rich array)
function bulletsAr(s, items, x, y, w, h, opts) {
  const o = opts || {};
  const arr = items.map((it, i) => ({
    text: it,
    options: { bullet: bu(), breakLine: true, rtlMode: true, align: "right" },
  }));
  s.addText(arr, {
    x, y, w, h, fontSize: o.fontSize || 15, fontFace: F, color: o.color || TEXT,
    valign: o.valign || "top", paraSpaceAfter: o.spaceAfter == null ? 8 : o.spaceAfter,
    margin: 0, rtlMode: true,
  });
}

// big-number stat block
function stat(s, x, y, w, num, labelAr, numColor) {
  s.addText(num, {
    x, y, w, h: 0.85, fontSize: 40, bold: true, fontFace: F,
    color: numColor || PRIMARY, align: "center", margin: 0,
  });
  s.addText(labelAr, {
    x, y: y + 0.85, w, h: 0.55, fontSize: 13, fontFace: F,
    color: MUTED, align: "center", rtlMode: true, margin: 0,
  });
}

// divider slide
function divider(num, titleAr, subEn) {
  const s = slideBase(DARK);
  s.addText(num, {
    x: 0.7, y: 1.7, w: 4.4, h: 2.6, fontSize: 150, bold: true, fontFace: F,
    color: PRIMARY_MID, align: "left", margin: 0, transparency: 25,
  });
  s.addText(titleAr, {
    x: 3.4, y: 2.9, w: W - 3.4 - M, h: 1.1, fontSize: 38, bold: true,
    fontFace: F, color: WHITE, align: "right", rtlMode: true, margin: 0,
  });
  s.addText(subEn, {
    x: 3.4, y: 4.0, w: W - 3.4 - M, h: 0.5, fontSize: 15, fontFace: FC,
    color: DARKMUTED, align: "right", margin: 0,
  });
  pageNum(s, true);
  return s;
}

// simple table
function table(s, rows, x, y, w, colW, opts) {
  const o = opts || {};
  s.addTable(rows, {
    x, y, w, colW, fontFace: F, fontSize: o.fontSize || 12.5,
    color: TEXT, valign: "middle", align: "right", rtlMode: true,
    border: { pt: 0.75, color: "D5E2E6" },
    fill: { color: WHITE },
    rowH: o.rowH || 0.34, margin: 0.06,
  });
}

function th(text, fillCol) {
  return { text, options: { bold: true, color: WHITE, fill: { color: fillCol || PRIMARY }, align: "right", rtlMode: true, fontFace: F } };
}
function td(text, opt) {
  return { text, options: Object.assign({ align: "right", rtlMode: true, fontFace: F }, opt || {}) };
}
function tdc(text, opt) { // code cell (LTR)
  return { text, options: Object.assign({ align: "left", fontFace: FC, fontSize: 11.5, color: PRIMARY }, opt || {}) };
}

// ============================================================
// 1 · COVER
// ============================================================
{
  const s = slideBase(DARK);
  // soft decorative circles (motif)
  s.addShape(pres.shapes.OVAL, { x: -1.6, y: -1.8, w: 4.6, h: 4.6, fill: { color: PRIMARY, transparency: 55 }, line: { type: "none" } });
  s.addShape(pres.shapes.OVAL, { x: 10.6, y: 4.6, w: 4.4, h: 4.4, fill: { color: PRIMARY_MID, transparency: 70 }, line: { type: "none" } });
  s.addShape(pres.shapes.OVAL, { x: 11.6, y: 0.7, w: 1.5, h: 1.5, fill: { color: ACCENT, transparency: 35 }, line: { type: "none" } });

  s.addText("Gym-OS", {
    x: M, y: 1.45, w: W - 2 * M, h: 1.0, fontSize: 60, bold: true,
    fontFace: F, color: WHITE, align: "center", margin: 0,
  });
  s.addText("نظام إدارة الجيم — دليل الفريق لفهم المشروع بالكامل", {
    x: M, y: 2.55, w: W - 2 * M, h: 0.7, fontSize: 26, bold: true,
    fontFace: F, color: DARKMUTED, align: "center", rtlMode: true, margin: 0,
  });
  s.addText("Gym Management System — Console Application in Java", {
    x: M, y: 3.35, w: W - 2 * M, h: 0.45, fontSize: 15, fontFace: FC, color: DARKMUTED, align: "center", margin: 0,
  });

  // chips
  const chips = ["Java 21", "SQLite (JDBC)", "55 Java Files", "13 DB Tables", "19 Business Rules", "3 Login Roles"];
  const cw = 2.05, gap = 0.18, total = chips.length * cw + (chips.length - 1) * gap;
  let cx = (W - total) / 2;
  chips.forEach((c) => {
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: cx, y: 4.45, w: cw, h: 0.52, rectRadius: 0.26, fill: { color: "204554" }, line: { color: PRIMARY_MID, width: 0.75 } });
    s.addText(c, { x: cx, y: 4.45, w: cw, h: 0.52, fontSize: 12.5, fontFace: FC, color: WHITE, align: "center", valign: "middle", margin: 0 });
    cx += cw + gap;
  });

  s.addText("شرح مبسط لكل ملف · قواعد العمل 1–19 · الإصلاحات التي تمت · أسئلة متوقعة وأجوبتها", {
    x: M, y: 5.6, w: W - 2 * M, h: 0.5, fontSize: 15, fontFace: F, color: DARKMUTED, align: "center", rtlMode: true, margin: 0,
  });
  s.addNotes("افتحوا العرض بالتعريف: تطبيق إدارة جيم يعمل من الطرفية (Terminal) بلغة Java مع قاعدة بيانات SQLite. الهدف من هذا العرض أن كل فرد في الفريق يفهم البنية وكل ملف، ويستطيع الإجابة عن أي سؤال في المناقشة.");
}

// ============================================================
// 2 · AGENDA
// ============================================================
{
  const s = slideBase();
  header(s, "محتويات الدليل", "AGENDA");
  const items = [
    ["1", "نظرة عامة على المشروع والبنية (Architecture)", "Overview & Architecture"],
    ["2", "الملفات الأساسية: الدخول، القوائم، قواعد العمل", "Core files: entry, menus, rules"],
    ["3", "قواعد العمل التسعة عشر (Rules 1–19)", "Business rules"],
    ["4", "طبقة البيانات: DAO + SQLite + الموديلات", "DAO layer, DB, models"],
    ["5", "ماذا أصلحنا ولماذا (ملخص الإصلاحات)", "What we fixed & why"],
    ["6", "أسئلة متوقعة وأجوبة قصيرة + سيناريو العرض", "Q&A prep + demo script"],
  ];
  let y = 1.5;
  items.forEach((it) => {
    card(s, M, y, W - 2 * M, 0.82, TINT2);
    s.addShape(pres.shapes.OVAL, { x: W - M - 0.85, y: y + 0.14, w: 0.55, h: 0.55, fill: { color: PRIMARY }, line: { type: "none" } });
    s.addText(it[0], { x: W - M - 0.85, y: y + 0.14, w: 0.55, h: 0.55, fontSize: 18, bold: true, fontFace: F, color: WHITE, align: "center", valign: "middle", margin: 0 });
    s.addText(it[1], { x: 2.2, y: y + 0.08, w: 9.25, h: 0.42, fontSize: 16.5, bold: true, fontFace: F, color: TEXT, align: "right", rtlMode: true, margin: 0 });
    s.addText(it[2], { x: 2.2, y: y + 0.46, w: 9.25, h: 0.3, fontSize: 11.5, fontFace: FC, color: MUTED, align: "right", margin: 0 });
    y += 0.95;
  });
  s.addNotes("استعرضوا المحاور الستة بسرعة. الوقت المقترح: 25-35 دقيقة للعرض كاملاً، أو جزء محدد حسب السؤال.");
}

// ============================================================
// 3 · DIVIDER 1
// ============================================================
divider("01", "نظرة عامة والبنية الأساسية", "OVERVIEW & ARCHITECTURE");

// ============================================================
// 4 · OVERVIEW
// ============================================================
{
  const s = slideBase();
  header(s, "ما هو المشروع؟", "PROJECT OVERVIEW");
  s.addText("تطبيق لإدارة جيم كامل يعمل من الطرفية (Terminal) — بدون واجهات رسومية. كل شيء عن طريق قوائم مرقمة يتفاعل معها المستخدم، وكل البيانات تُخزن في قاعدة بيانات SQLite واحدة (gym.db).", {
    x: M, y: 1.25, w: W - 2 * M, h: 0.75, fontSize: 15.5, fontFace: F, color: TEXT, align: "right", rtlMode: true, margin: 0,
  });
  // stats row
  const stats = [["55", "ملف Java"], ["13", "جدول في قاعدة البيانات"], ["19", "قاعدة عمل"], ["3", "أدوار دخول (Roles)"], ["22", "سيناريو تحقق آلي"]];
  const sw = 2.36, gap = 0.15;
  let x = W - M - sw;
  stats.forEach((st) => {
    card(s, x, 2.2, sw, 1.55, TINT);
    stat(s, x, 2.32, sw, st[0], st[1]);
    x -= sw + gap;
  });
  // features
  s.addText("الوظائف الرئيسية:", { x: M, y: 4.1, w: W - 2 * M, h: 0.4, fontSize: 16, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  const feats = [
    "إدارة الأعضاء وملفاتهم الطبية والتقدم (Members / Medical / Progress)",
    "المدربون ومواعيدهم المتاحة والاشتراكات (Trainers / Slots / Subscriptions)",
    "العضويات والمدفوعات والاستبدال (Memberships / Payments / Void)",
    "الحجز وجدول التدريب وبرامج التمرين (Sessions / Workout HUD)",
    "الحضور والمعدات وخريطة الصالة (Attendance / Equipment / Floor Map)",
    "حسابات الموظفين والصلاحيات والنسخ الاحتياطي (Staff RBAC / Backup)",
  ];
  bulletsAr(s, feats, M, 4.55, W - 2 * M, 2.4, { fontSize: 14.5, spaceAfter: 6 });
  s.addNotes("رسالة أساسية: المشروع متكامل لكن بسيط الفهم — كل شيء يمر عبر قوائم الطرفية. الأرقام فوق تعطي حجم المشروع الحقيقي: 55 ملف، 13 جدول، 19 قاعدة عمل.");
}

// ============================================================
// 5 · ARCHITECTURE DIAGRAM
// ============================================================
{
  const s = slideBase();
  header(s, "البنية: أربع طبقات واضحة", "ARCHITECTURE");
  const layers = [
    ["المستخدم — Terminal", "User types numbers in the console", TINT2, TEXT],
    ["طبقة الواجهة — com.gym.cli", "12 files: GymCliApp + 6 menus + helpers + suite", TINT, PRIMARY],
    ["قواعد العمل — GymRulesHelper", "Coordinate cross-entity rules 1-19", "F5EFE3", "A9702B"],
    ["طبقة البيانات — com.gym.dao", "13 DAO classes: insert / update / delete / find", TINT, PRIMARY],
    ["قاعدة البيانات — SQLite", "Single file: gym.db (13 tables)", DARK, WHITE],
  ];
  let y = 1.35;
  const bw = 8.6, bx = W - M - bw;
  layers.forEach((L, i) => {
    card(s, bx, y, bw, 0.88, L[2], i === 4);
    s.addText(L[0], { x: bx + 0.2, y: y + 0.1, w: bw - 0.4, h: 0.42, fontSize: 16, bold: true, fontFace: F, color: L[3], align: "right", rtlMode: true, margin: 0 });
    s.addText(L[1], { x: bx + 0.2, y: y + 0.5, w: bw - 0.4, h: 0.3, fontSize: 11.5, fontFace: FC, color: L[3] === WHITE ? DARKMUTED : MUTED, align: "right", margin: 0 });
    if (i < layers.length - 1) {
      s.addShape(pres.shapes.LINE, { x: bx + bw / 2, y: y + 0.88, w: 0, h: 0.24, line: { color: PRIMARY_MID, width: 2.25, endArrowType: "triangle" } });
    }
    y += 1.12;
  });
  // side card
  card(s, M, 1.35, 3.4, 4.4, TINT2);
  s.addText("قاعدة التصميم", { x: M + 0.15, y: 1.5, w: 3.1, h: 0.4, fontSize: 14.5, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "القوائم لا تلمس SQL مباشرة — كل قراءة/كتابة تمر عبر DAO.",
    "قواعد العمل المعقدة (تحتاج أكثر من جدول) في GymRulesHelper.",
    "كل DAO يتحقق من صحة الحقول في validate() قبل الكتابة.",
    "الأخطاء ترجع كـ ValidationException أو DatabaseException وتُطبع برسالة ودودة.",
  ], M + 0.15, 1.95, 3.1, 3.7, { fontSize: 12.5, spaceAfter: 8 });
  s.addNotes("اشرحوا اتجاه البيانات: المستخدم → القائمة → GymRulesHelper (إذا كانت العملية تحتاج قواعد) → DAO → SQLite. المفتاح: كل طبقة لها دور واحد فقط — هذا هو مبدأ Separation of Concerns.");
}

// ============================================================
// 6 · PACKAGE STRUCTURE
// ============================================================
{
  const s = slideBase();
  header(s, "حزم المشروع الخمس (Packages)", "PACKAGE STRUCTURE");
  const pkgs = [
    ["com.gym.cli", "12", "ملف", "كل ما يراه المستخدم: الدخول، القوائم الست، أدوات الإدخال، بذور البيانات، حزمة التحقق."],
    ["com.gym.dao", "13", "ملف", "Data Access Objects: صنف لكل جدول — insert / update / delete / findById / findAll + validate."],
    ["com.gym.model", "25", "ملف", "14 كيان (Customer, Trainer, Payment...) + 11 نوع تعدادي (enum) للحالات."],
    ["com.gym.exception", "3", "ملفات", "GymException الأب + ValidationException + DatabaseException."],
    ["com.gym.util", "2", "ملفان", "DatabaseConnection (إنشاء الجداول والاتصال) + SerializationUtil (نسخ .ser)."],
  ];
  let y = 1.35;
  pkgs.forEach((p) => {
    card(s, M, y, W - 2 * M, 0.98, TINT2);
    s.addText(p[0], { x: W - M - 3.4, y: y + 0.09, w: 3.2, h: 0.42, fontSize: 16, bold: true, fontFace: FC, color: PRIMARY, align: "right", margin: 0 });
    s.addText(p[2], { x: W - M - 3.4, y: y + 0.53, w: 3.2, h: 0.35, fontSize: 12, fontFace: F, color: MUTED, align: "right", rtlMode: true, margin: 0 });
    s.addShape(pres.shapes.OVAL, { x: W - M - 4.35, y: y + 0.2, w: 0.62, h: 0.62, fill: { color: "204554" }, line: { type: "none" } });
    s.addText(p[1], { x: W - M - 4.35, y: y + 0.2, w: 0.62, h: 0.62, fontSize: 16, bold: true, fontFace: F, color: WHITE, align: "center", valign: "middle", margin: 0 });
    s.addText(p[3], { x: M + 0.2, y: y + 0.12, w: W - 2 * M - 4.75, h: 0.78, fontSize: 13, fontFace: F, color: TEXT, align: "right", valign: "middle", rtlMode: true, margin: 0 });
    y += 1.12;
  });
  s.addNotes("55 ملفاً موزعين على 5 حزم فقط. لاحظوا التوحيد: كل DAO بنفس البنية، كل Model بنفس النمط — هذا يسهل تفسير أي ملف بسرعة.");
}

// ============================================================
// 7 · run.bat
// ============================================================
{
  const s = slideBase();
  header(s, "كيف يُبنى ويُشغَّل المشروع؟", "run.bat — BUILD & RUN");
  const steps = [
    ["1", "الانتقال إلى مجلد المشروع", "cd /d \"%~dp0\" — يضمن أن gym.db يُقرأ من مكان صحيح دائماً"],
    ["2", "جمع كل ملفات المصدر", "dir /s /b src\\*.java > out\\sources.txt (55 ملفاً)"],
    ["3", "الترجمة (Compile)", "javac --release 21 -encoding UTF-8 -Xlint:all -cp \"lib\\*\" -d out @out\\sources.txt"],
    ["4", "التشغيل (Run)", "java -cp \"out;lib\\*\" com.gym.cli.GymCliApp — النقطة الرئيسية main"],
  ];
  let y = 1.4;
  steps.forEach((st) => {
    card(s, M, y, W - 2 * M, 1.0, TINT2);
    s.addShape(pres.shapes.OVAL, { x: W - M - 0.9, y: y + 0.22, w: 0.58, h: 0.58, fill: { color: ACCENT }, line: { type: "none" } });
    s.addText(st[0], { x: W - M - 0.9, y: y + 0.22, w: 0.58, h: 0.58, fontSize: 17, bold: true, fontFace: F, color: WHITE, align: "center", valign: "middle", margin: 0 });
    s.addText(st[1], { x: M + 0.2, y: y + 0.1, w: W - 2 * M - 1.25, h: 0.38, fontSize: 15, bold: true, fontFace: F, color: TEXT, align: "right", rtlMode: true, margin: 0 });
    s.addText(st[2], { x: M + 0.2, y: y + 0.5, w: W - 2 * M - 1.25, h: 0.42, fontSize: 11.5, fontFace: FC, color: MUTED, align: "right", margin: 0 });
    y += 1.14;
  });
  card(s, M, y + 0.02, W - 2 * M, 0.9, TINT);
  s.addText("مكتبات مجلد lib: sqlite-jdbc (تعريف JDBC لـ SQLite) و slf4j-api (سجلات المكتبة). و --enable-native-access تُسكِت تحذير الوصول للكود الأصلي في مشغل SQLite.", {
    x: M + 0.2, y: y + 0.1, w: W - 2 * M - 0.4, h: 0.75, fontSize: 13, fontFace: F, color: TEXT, align: "right", valign: "middle", rtlMode: true, margin: 0,
  });
  s.addNotes("إن سُئلتم: لماذا لا نستخدم Maven/Gradle؟ لأن المشروع مُسلَّم كملفات مصدر بسيطة، وrun.bat يغني عن أي أداة بناء — javac و java يكفيان. المطلوب فقط JDK 21 أو أحدث على PATH.");
}

// ============================================================
// 8 · STARTUP FLOW
// ============================================================
{
  const s = slideBase();
  header(s, "تدفق بدء التشغيل — GymCliApp.main", "STARTUP FLOW");
  const steps = [
    ["initializeDatabase()", "ينشئ 13 جدولاً إذا لم تكن موجودة (CREATE TABLE IF NOT EXISTS)"],
    ["seedIfEmpty()", "يملأ بيانات تجريبية فقط إذا كان الجدول administrators فارغاً"],
    ["طباعة مسار قاعدة البيانات", "Using database file: <المسار الكامل> — شفافية: تعرف أي ملف gym.db يُستخدم"],
    ["شاشة الدخول (Login Gate)", "دخول يدوي أو Quick Login لثلاثة حسابات جاهزة"],
    ["لوحة القيادة (Dashboard)", "حلقة القوائم الرئيسية 1–10 + [V] + [L] خروج + [0] إنهاء"],
  ];
  let y = 1.4;
  steps.forEach((st, i) => {
    card(s, M, y, W - 2 * M, 0.92, i === 4 ? TINT : TINT2);
    s.addShape(pres.shapes.OVAL, { x: W - M - 0.9, y: y + 0.18, w: 0.56, h: 0.56, fill: { color: PRIMARY }, line: { type: "none" } });
    s.addText(String(i + 1), { x: W - M - 0.9, y: y + 0.18, w: 0.56, h: 0.56, fontSize: 16, bold: true, fontFace: F, color: WHITE, align: "center", valign: "middle", margin: 0 });
    s.addText(st[0], { x: M + 0.2, y: y + 0.08, w: W - 2 * M - 1.25, h: 0.4, fontSize: 14.5, bold: true, fontFace: FC, color: PRIMARY, align: "right", margin: 0 });
    s.addText(st[1], { x: M + 0.2, y: y + 0.48, w: W - 2 * M - 1.25, h: 0.38, fontSize: 13, fontFace: F, color: TEXT, align: "right", rtlMode: true, margin: 0 });
    y += 1.06;
  });
  s.addNotes("نقطة مهمة للحفظ: البداية دائماً آمنة — إن كانت القاعدة فارغة تُزرع بيانات، وإن كانت موجودة تبقى بياناتك. وإن فُتح التطبيق من مجلد آخر يطبع المسار الحقيقي للملف فلا 'تختفي' البيانات أبداً.");
}

// ============================================================
// 9 · GymCliApp.java
// ============================================================
{
  const s = slideBase();
  header(s, "GymCliApp.java — نقطة الانطلاق ولوحة القيادة", "src/com/gym/cli");
  // right: what it contains
  card(s, W - M - 6.1, 1.3, 6.1, 5.5, TINT2);
  s.addText("ماذا يفعل هذا الملف؟", { x: W - M - 5.9, y: 1.45, w: 5.7, h: 0.4, fontSize: 15, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "main() ينشئ كائن التطبيق ويستدعي start().",
    "start(): تهيئة القاعدة + البذر + حلقة الدخول/الخروج.",
    "showLoginGate(): التحقق من اسم المستخدم وكلمة المرور عبر rules.authenticate (Rule 16).",
    "runMainDashboardLoop(): يعرض القوائم العشر ويوجّه الاختيار.",
    "printLiveKpiBanner(): أرقام حية (أعضاء، حضور اليوم، الإيراد...) أعلى اللوحة.",
    "حماية عامة: أي خطأ غير متوقع يُطبع بوضوح ويعود للقائمة بدلاً من إنهاء التطبيق.",
  ], W - M - 5.9, 1.95, 5.7, 4.7, { fontSize: 13.5, spaceAfter: 9 });
  // left: menu tree
  card(s, M, 1.3, 6.1, 5.5, TINT);
  s.addText("لوحة القيادة الرئيسية", { x: M + 0.2, y: 1.45, w: 5.7, h: 0.4, fontSize: 15, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  const menu = [
    "[1] الأعضاء والملفات الطبية والتقدم",
    "[2] المدربون والمواعيد والاشتراكات",
    "[3] العضويات والخطط (بيع/تجميد/تجديد)",
    "[4] جدول الحصص والحجز",
    "[5] برامج التمرين ومحاكي HUD",
    "[6] الحضور (دخول/خروج + تقرير)",
    "[7] المدفوعات وسجل الإلغاء/الاستبدال",
    "[8] المعدات وخريطة الصالة",
    "[9] حسابات الموظفين [ADMIN فقط]",
    "[10] النسخ الاحتياطي وأدوات القاعدة",
    "[V] حزمة التحقق وقواعد العمل (1-19)",
  ];
  bulletsAr(s, menu, M + 0.2, 1.95, 5.7, 4.7, { fontSize: 13, spaceAfter: 5.5 });
  s.addNotes("لو سُئلتم عن الخيار [V]: حزمة تحقق ذاتية تشغل 22 سيناريو على قاعدة معزولة (sandbox) وتطبع تقرير نجاح/فشل — أداة عرض ممتازة في المناقشة، ولا تلمس بيانات gym.db الحقيقية.");
}

// ============================================================
// 10 · Helper trio
// ============================================================
{
  const s = slideBase();
  header(s, "ثلاثي الأدوات المساعدة في الحزمة cli", "CliInput · CliSession · CliTheme");
  const trio = [
    ["CliInput.java", "قراءة آمنة من لوحة المفاتيح", [
      "readInt / readDouble / readBigDecimal — ترفض غير الأرقام وتعيد المحاولة.",
      "readIsoDate / readIsoTime — تفرض صيغة yyyy-MM-dd و HH:mm.",
      "readRequiredString — يرفض الفراغ. readYesNo — يقبل y/n فقط.",
      "كل حلقة تقرأ تنتهي بأمان عند نهاية الإدخال (EOF).",
    ]],
    ["CliSession.java", "حالة الجلسة الحالية", [
      "currentUser: من المسجل دخوله الآن (دوره ورقمه).",
      "أعلام التنقل: logoutRequested / exitRequested / returnHome.",
      "shouldStopSubMenu(): سبب موحد لإنهاء أي قائمة فرعية.",
      "userLabel(): نص جاهز للعرض في ترويسة كل شاشة.",
    ]],
    ["CliTheme.java", "الطباعة المنسقة", [
      "printHeader / printFooterNav — ترويسة كل شاشة بشكل موحد.",
      "printTable — جداول نصية بأعمدة مرتبة وقص آمن للنصوص الطويلة.",
      "printSuccess / printValidationError / printDatabaseError — رسائل ملونة دلالتها واضحة.",
      "safeText — يمنع null من أن يظهر كنص في الجداول.",
    ]],
  ];
  const cw = 4.05, gap = 0.15;
  let x = W - M - cw;
  trio.forEach((t) => {
    card(s, x, 1.35, cw, 5.45, TINT2, true);
    s.addText(t[0], { x: x + 0.18, y: 1.5, w: cw - 0.36, h: 0.4, fontSize: 15.5, bold: true, fontFace: FC, color: PRIMARY, align: "right", margin: 0 });
    s.addText(t[1], { x: x + 0.18, y: 1.9, w: cw - 0.36, h: 0.38, fontSize: 13, bold: true, fontFace: F, color: TEXT, align: "right", rtlMode: true, margin: 0 });
    bulletsAr(s, t[2], x + 0.18, 2.4, cw - 0.36, 4.2, { fontSize: 12, spaceAfter: 8 });
    x -= cw + gap;
  });
  s.addNotes("هذه الملفات الثلاثة تجعل كل قائمة قصيرة: كل منطق الإدخال الآمن في مكان واحد (CliInput) وكل شكل الطباعة في مكان واحد (CliTheme). لو سُئلتم عن Scanner: يوجد Scanner واحد فقط مشترك على System.in.");
}

// ============================================================
// 11 · GymRulesHelper
// ============================================================
{
  const s = slideBase();
  header(s, "GymRulesHelper.java — العقل المدبر لقواعد العمل", "src/com/gym/cli");
  s.addText("صنف واحد يجمع العمليات التي تحتاج أكثر من جدول أو أكثر من قاعدة — القوائم تستدعيه بدلاً من تكرار المنطق.", {
    x: M, y: 1.2, w: W - 2 * M, h: 0.5, fontSize: 14.5, fontFace: F, color: TEXT, align: "right", rtlMode: true, margin: 0,
  });
  const groups = [
    ["الحسابات والصلاحيات", ["authenticate: دخول + رفض الحسابات غير النشطة (Rule 16)", "requireAdmin: حماية إجراءات الـ ADMIN (Rule 16)"]],
    ["الأعضاء والحذف", ["softDeleteCustomer: INACTIVE بدلاً من الحذف (Rule 4)", "hardDeleteCustomer/Trainer: يرفض إذا وُجدت مراجع (Rule 4)", "suspendCustomer: تجميد كل الخطط النشطة (Rule 14)"]],
    ["العضويات", ["freeze/unfreeze: نقل endDate بعدد أيام التجميد (Rule 7)", "renew: تجديد ويمنع CANCELLED (Rule 7)", "effectiveMembershipStatus: عرض EXPIRED بدون تعديل القاعدة"]],
    ["الحجز والجلسات", ["bookOpenSlot: حجز مباشر ضمن السعة (Rules 1/13/17)", "requestOutsideSlots: خارج المواعيد يبدأ REQUESTED (Rule 1)", "confirm/complete/markNoShow: آلة حالات صارمة (Rule 18)", "cancelSession: سبب إلزامي + قاعدة ساعتين (Rule 10)"]],
    ["المدفوعات والرصيد", ["computeOutstandingBalance: كل ما هو مستحق (Rule 6)", "voidAndReplacePayment: إلغاء + بديل بتحقق مسبق (Rule 19)"]],
    ["الحضور والملف الطبي", ["checkIn: عضو ACTIVE + خطة سارية + صف واحد/يوم (Rule 5)", "getMedicalProfileForTrainer: للمدرّب المكلف فقط (Rule 15)", "exportSnapshot / loadSnapshot: نسخ .ser كامل"]],
  ];
  const cw = 4.05, gap = 0.15;
  let x = W - M - cw;
  let col = 0;
  groups.forEach((g) => {
    card(s, x, 1.8 + col * 2.65, cw, 2.5, col % 2 === 0 ? TINT2 : TINT);
    s.addText(g[0], { x: x + 0.15, y: 1.9 + col * 2.65, w: cw - 0.3, h: 0.36, fontSize: 13.5, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
    bulletsAr(s, g[1], x + 0.15, 2.28 + col * 2.65, cw - 0.3, 2.0, { fontSize: 11.5, spaceAfter: 4 });
    col++;
    if (col === 2) { col = 0; x -= cw + gap; }
  });
  s.addNotes("الفكرة المعمارية: القائمة تجمع المدخلات فقط، وGymRulesHelper ينفذ القواعد ويستدعي الـ DAOs. لاحظوا الأنماط البسيطة المستخدمة: حلقات for عادية وtry/catch وشرط if — لا شيء معقد.");
}

// ============================================================
// 12 · DIVIDER 2
// ============================================================
divider("02", "قواعد العمل التسعة عشر", "BUSINESS RULES 1-19");

// ============================================================
// 13 · RULES 1-10
// ============================================================
{
  const s = slideBase();
  header(s, "قواعد العمل 1–10", "BUSINESS RULES (1/2)");
  const rows = [
    [th("القاعدة", PRIMARY), th("رقم", PRIMARY)],
    [td("الحجز داخل موعد منشور يبدأ CONFIRMED مباشرة، وخارجه يبدأ REQUESTED فينتظر التأكيد."), td("Rule 1", { bold: true, color: PRIMARY })],
    [td("تسجيل حضور المدرّب للجلسات الفائتة: حالة NO_SHOW تُحصى في إحصاءات كل مدرّب."), td("Rule 2", { bold: true, color: PRIMARY })],
    [td("ملف طبي واحد لكل عضو، يُنشأ عند الطلب فقط — التكرار مرفوض (عمود UNIQUE + تحقق)."), td("Rule 3", { bold: true, color: PRIMARY })],
    [td("الحذف الناعم يضع INACTIVE؛ والحذف النهائي ADMIN فقط ويُرفض وجود أي مراجع للعضو."), td("Rule 4", { bold: true, color: PRIMARY })],
    [td("الدخول للحضور: عضو ACTIVE + خطة سارية + صف واحد فقط لليوم الواحد (إعادة الاستخدام)."), td("Rule 5", { bold: true, color: PRIMARY })],
    [td("الرصيد المستحق = خطط + اشتراكات مدرّب + جلسات مدفوعة، ناقص المدفوعات غير الملغاة."), td("Rule 6", { bold: true, color: PRIMARY })],
    [td("التجميد للنشطة فقط، وإلغاء التجميد ينقل endDate بعدد الأيام، وتجديد الملغاة ممنوع."), td("Rule 7", { bold: true, color: PRIMARY })],
    [td("الموعد المتاح: وقت البداية قبل النهاية + حد أقصى للعملاء أكبر من صفر."), td("Rule 8", { bold: true, color: PRIMARY })],
    [td("كل التواريخ بصيغة ISO yyyy-MM-dd والأوقات HH:mm — تتحقق في كل DAO قبل الحفظ."), td("Rule 9", { bold: true, color: PRIMARY })],
    [td("الإلغاء يتطلب سبباً، والإلغاء قبل ساعتين مجاني وإلا يُسجل كإلغاء متأخر مع السبب."), td("Rule 10", { bold: true, color: PRIMARY })],
  ];
  table(s, rows, M, 1.3, W - 2 * M, [10.4, 1.93], { fontSize: 12.5, rowH: 0.5 });
  s.addNotes("كل قاعدة لها مكان تنفيذ واضح: أغلبها في validate() داخل الـ DAO (تحقق حقول ومراجع)، والمعقدة في GymRulesHelper. اطلبوا من كل عضو أن يحفظ القواعد القصيرة هذه بصيغته.");
}

// ============================================================
// 14 · RULES 11-19
// ============================================================
{
  const s = slideBase();
  header(s, "قواعد العمل 11–19", "BUSINESS RULES (2/2)");
  const rows = [
    [th("القاعدة", PRIMARY), th("رقم", PRIMARY)],
    [td("اشتراك المدرّب يجب أن يطابق المدرّب المكلف للعضو، والجلسة المشمولة بالاشتراك سعرها صفر (لا ازدواج فواتير)."), td("Rule 11", { bold: true, color: PRIMARY })],
    [td("كل دفعة مرتبطة بهدف واحد بالضبط (خطة أو اشتراك أو جلسة) + تطابق صاحب الدفعة + رقم إيصال فريد."), td("Rule 12", { bold: true, color: PRIMARY })],
    [td("حالة الموعد تُشتق تلقائياً: CLOSED إن أُغلق، FULL عند اكتمال المحجوزين المؤكدين."), td("Rule 13", { bold: true, color: PRIMARY })],
    [td("إيقاف العضو (SUSPENDED) يجمّد كل خططه النشطة تلقائياً."), td("Rule 14", { bold: true, color: PRIMARY })],
    [td("قراءة الملف الطبي للمدرّب المكلف بالعضو فقط — أي مدرّب آخر يُرفض."), td("Rule 15", { bold: true, color: PRIMARY })],
    [td("الصلاحيات: حساب غير نشط لا يدخل، وإجراءات حساسة تتطلب دور ADMIN."), td("Rule 16", { bold: true, color: PRIMARY })],
    [td("سعة الموعد: عدد الجلسات CONFIRMED فيه يجب ألا يتجاوز maxCustomers."), td("Rule 17", { bold: true, color: PRIMARY })],
    [td("آلة حالات الجلسة: REQUESTED ثم CONFIRMED ثم COMPLETED أو NO_SHOW أو CANCELLED — لا قفزات."), td("Rule 18", { bold: true, color: PRIMARY })],
    [td("المدفوعات لا تُحذف أبداً: تُلغى (void) بسبب وثاريخ، مع إمكانية إنشاء دفعة بديلة برقم إيصال جديد."), td("Rule 19", { bold: true, color: PRIMARY })],
  ];
  table(s, rows, M, 1.3, W - 2 * M, [10.4, 1.93], { fontSize: 12.5, rowH: 0.5 });
  s.addNotes("أهم قاعدتين للشرح العملي: 12 (الدفع المرتبط بهدف واحد) و18 (آلة الحالات). اذكروا أن الحذف المالي غير موجود أصلاً في الواجهة — Void/Replace هو الأسلوب الوحيد، وهذا يحمي السجل المالي.");
}

// ============================================================
// 15 · ValidationSuiteRunner
// ============================================================
{
  const s = slideBase();
  header(s, "ValidationSuiteRunner.java — حزمة التحقق [V]", "22-SCENARIO VERIFICATION");
  card(s, W - M - 6.2, 1.3, 6.2, 5.5, TINT2);
  s.addText("ماذا تفعل؟", { x: W - M - 6.0, y: 1.45, w: 5.8, h: 0.4, fontSize: 15, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "تشغل 22 سيناريو تحقق تلقائياً تغطي الكيانات وقواعد العمل 1-19 ومعالجة الأخطاء.",
    "تعمل على قاعدة معزولة gym_sandbox.db: تُنشأ قبل التشغيل وتُحذف بعده — gym.db لا تُلمس.",
    "تطبع PASS/FAIL لكل سيناريو ثم لوحة نتيجة: SCOREBOARD n / 22.",
    "وضع ثانٍ: اختيار فئة واحدة (1-11) لعرض قاعدة محددة أمام الحضور.",
    "كل سيناريو معزول في try/catch: فشل واحد لا يوقف البقية (runCheck).",
    "إن نقصت بيانات البذر تطبع Seed data missing بدل الانهيار.",
  ], W - M - 6.0, 1.95, 5.8, 4.7, { fontSize: 13.5, spaceAfter: 9 });
  card(s, M, 1.3, 6.2, 5.5, TINT);
  s.addText("أمثلة سيناريوهات", { x: M + 0.2, y: 1.45, w: 5.8, h: 0.4, fontSize: 15, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  const ex = [
    ["#04", "رفض اسم مستخدم مكرر (Rule 16)"],
    ["#08", "امتلاء الموعد ورفض الحجز الزائد (Rule 17)"],
    ["#11", "منع قفزة REQUESTED إلى COMPLETED (Rule 18)"],
    ["#15", "إعادة استخدام صف الحضور في نفس اليوم (Rule 5)"],
    ["#20", "الدفع بلا هدف مرفوض + Void يرفع الرصيد (Rules 12/19/6)"],
    ["#22", "نسخ .ser وتحميله والتقاط خطأ ملف مفقود"],
  ];
  let y = 2.0;
  ex.forEach((e) => {
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: M + 6.2 - 0.95, y: y, w: 0.75, h: 0.42, rectRadius: 0.1, fill: { color: PRIMARY }, line: { type: "none" } });
    s.addText(e[0], { x: M + 6.2 - 0.95, y: y, w: 0.75, h: 0.42, fontSize: 12, bold: true, fontFace: FC, color: WHITE, align: "center", valign: "middle", margin: 0 });
    s.addText(e[1], { x: M + 0.2, y: y + 0.01, w: 4.9, h: 0.4, fontSize: 12.5, fontFace: F, color: TEXT, align: "right", valign: "middle", rtlMode: true, margin: 0 });
    y += 0.62;
  });
  s.addText("ملاحظة: النتيجة الحالية بعد الإصلاحات: 22 / 22 PASS.", {
    x: M + 0.2, y: 5.9, w: 5.8, h: 0.4, fontSize: 13, bold: true, fontFace: F, color: ACCENT, align: "right", rtlMode: true, margin: 0,
  });
  s.addNotes("هذه أقوى أداة لديكم في العرض: شغلوا الخيار [V] ثم 1 أمام الحضور — دقيقة واحدة تثبت عمل 19 قاعدة تلقائياً. اذكروا أن السيناريوهات تُشغَّل على نسخة مؤقتة من قاعدة البيانات لضمان عدم المساس بالبيانات الحقيقية.");
}

// ============================================================
// 16 · DAO PATTERN
// ============================================================
{
  const s = slideBase();
  header(s, "طبقة DAO — نمط واحد يتكرر 13 مرة", "DATA ACCESS LAYER");
  // anatomy card
  card(s, W - M - 5.9, 1.3, 5.9, 5.5, TINT2);
  s.addText("تشريح أي DAO (مثال CustomerDao)", { x: W - M - 5.7, y: 1.45, w: 5.5, h: 0.4, fontSize: 14.5, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  const anat = [
    ["insert(obj)", "يُتحقق ثم INSERT + جلب المفتاح التلقائي"],
    ["update(obj)", "يُتحقق ثم UPDATE WHERE id=? + فحص عدد الصفوف"],
    ["delete(id)", "DELETE + فحص عدد الصفوف (بعضها يفحص المراجع أولاً)"],
    ["findById / findAll", "قراءة صف أو الكل عبر mapRow"],
    ["validate(obj)", "شرطات if بسيطة: فراغ، صيغة تاريخ/بريد، المراجع موجودة؟"],
    ["mapRow(rs)", "يحوّل صف ResultSet إلى كائن Model"],
  ];
  let y = 1.95;
  anat.forEach((a) => {
    s.addText(a[0], { x: W - M - 5.7, y: y, w: 5.5, h: 0.3, fontSize: 12, bold: true, fontFace: FC, color: PRIMARY, align: "left", valign: "top", margin: 0 });
    s.addText(a[1], { x: W - M - 5.7, y: y + 0.3, w: 5.5, h: 0.44, fontSize: 11.5, fontFace: F, color: TEXT, align: "right", valign: "top", rtlMode: true, margin: 0 });
    y += 0.82;
  });
  // habits card
  card(s, M, 1.3, 6.4, 5.5, TINT);
  s.addText("ثلاث عادات ثابتة في كل DAO (اسألوا عنها!)،", { x: M + 0.2, y: 1.45, w: 6.0, h: 0.4, fontSize: 14.5, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  const habits = [
    ["PreparedStatement + ? parameters", "لا دمج نصي لقيم المستخدم في SQL — حماية من SQL Injection وإدارة آمنة لعلامات الاقتباس (مثل اسم O'Brien)."],
    ["try-with-resources", "الاتصال والـ Statement والـ ResultSet يُغلقون تلقائياً — لا تسريب موارد حتى عند الخطأ."],
    ["RETURN_GENERATED_KEYS", "بعد INSERT نقرأ المفتاح التلقائي rs.getInt(1) بدل max(id)+1 الهش."],
  ];
  y = 2.0;
  habits.forEach((hb) => {
    s.addText(hb[0], { x: M + 0.2, y: y, w: 6.0, h: 0.38, fontSize: 13, bold: true, fontFace: FC, color: PRIMARY, align: "left", margin: 0 });
    s.addText(hb[1], { x: M + 0.2, y: y + 0.38, w: 6.0, h: 0.75, fontSize: 12.5, fontFace: F, color: TEXT, align: "right", rtlMode: true, margin: 0 });
    y += 1.32;
  });
  s.addText("نتيجة الفحص: كل عبارات SQL في المشروع بهذا النمط — لا استثناء واحد.", {
    x: M + 0.2, y: 6.15, w: 6.0, h: 0.5, fontSize: 12.5, bold: true, fontFace: F, color: ACCENT, align: "right", rtlMode: true, margin: 0,
  });
  s.addNotes("إذا سأل أحدكم 'ما الـ DAO؟': طبقة عزل بين المنطق وقاعدة البيانات — لو غيّرنا SQLite إلى MySQL نعدل هذه الطبقة فقط. الفحص الأمني الأهم: لا يوجد أي دمج نصي لقيم المستخدم في SQL.");
}

// ============================================================
// 17 · DAO MAP
// ============================================================
{
  const s = slideBase();
  header(s, "خريطة الـ DAOs الـ 13", "DAO PER TABLE");
  const rows = [
    [th("أبرز التحقق داخلها", PRIMARY), th("الجدول", PRIMARY), th("DAO", PRIMARY)],
    [td("تحقق كامل: اسم، بريد، هاتف 7 أرقام، تواريخ، وجود المدرّب المكلف"), tdc("customers"), tdc("CustomerDao")],
    [td("تحقق: بريد/هاتف، خبرة غير سالفة، أجر غير سالف"), tdc("trainers"), tdc("TrainerDao")],
    [td("Rule 7: منع إعادة تنشيط الملغاة + تاريخ النهاية بعد البداية"), tdc("memberships"), tdc("MembershipDao")],
    [td("Rule 8: بداية < نهاية + سعة > 0"), tdc("trainer_availability"), tdc("TrainerAvailabilityDao")],
    [td("Rule 11: مطابقة مدرّب الاشتراك مع المكلَّف"), tdc("trainer_subscriptions"), tdc("TrainerSubscriptionDao")],
    [td("Rules 9/11/18: صيغ + سعر صفر مع الاشتراك + آلة حالات"), tdc("sessions"), tdc("SessionDao")],
    [td("Rule 5: عضو ACTIVE + خطة سارية + صف واحد/يوم (حتى عند التعديل)"), tdc("attendance"), tdc("AttendanceDao")],
    [td("Rule 3: ملف واحد لكل عضو (UNIQUE + تحقق مزدوج)"), tdc("medical_profiles"), tdc("MedicalProfileDao")],
    [td("مدى صحيح: وزن > 0 و دهون 0-100 و هدف > 0"), tdc("progress_records"), tdc("ProgressRecordDao")],
    [td("Rule 12: هدف واحد بالضبط + تطابق المالك + إيصال فريد"), tdc("payments"), tdc("PaymentDao")],
    [td("نسخة فريدة لكل عضو (excludeId عند التعديل)"), tdc("workout_programs"), tdc("WorkoutProgramDao")],
    [td("صيانة بعد الشراء + حالات وأوضاع صحيحة"), tdc("equipment"), tdc("EquipmentDao")],
    [td("اسم مستخدم فريد (UNIQUE) + دور صحيح + حساب نشط"), tdc("administrators"), tdc("AdministratorDao")],
  ];
  table(s, rows, M, 1.28, W - 2 * M, [7.0, 2.6, 2.73], { fontSize: 11.5, rowH: 0.39 });
  s.addNotes("لا تحفظوا الجدول حرفياً — احفظوا الفكرة: لكل جدول DAO، وكل DAO يحمي قواعد جدوله بنفسه. هذا يفسر لماذا نجحت كل سيناريوهات الحزمة [V].");
}

// ============================================================
// 18 · DatabaseConnection
// ============================================================
{
  const s = slideBase();
  header(s, "DatabaseConnection.java — القلب التقني للقاعدة", "src/com/gym/util");
  card(s, W - M - 6.2, 1.3, 6.2, 5.5, TINT2);
  s.addText("المسؤوليات", { x: W - M - 6.0, y: 1.45, w: 5.8, h: 0.4, fontSize: 15, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "DEFAULT_DB_URL = jdbc:sqlite:gym.db — ملف واحد بجانب المشروع.",
    "initializeDatabase(): ينفذ 13 عبارة CREATE TABLE IF NOT EXISTS.",
    "getConnection(): اتصال جديد لكل عملية (نمط بسيط يناسب CLI أحادي المستخدم).",
    "busy_timeout = 5000: ينتظر حتى 5 ثوانٍ بدل الفشل الفوري لو الملف مقفول من عملية أخرى.",
    "setDbUrl / resetDbUrl: تُستخدم من حزمة التحقق للتحول للـ sandbox ثم العودة.",
  ], W - M - 6.0, 1.95, 5.8, 4.7, { fontSize: 13.5, spaceAfter: 9 });
  card(s, M, 1.3, 6.2, 5.5, TINT);
  s.addText("حقائق عن المخطط (Schema)", { x: M + 0.2, y: 1.45, w: 5.8, h: 0.4, fontSize: 15, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "كل جدول: id INTEGER PRIMARY KEY AUTOINCREMENT.",
    "قيود فريدة على مستوى القاعدة: username (موظفون)، receipt_no (مدفوعات)، customer_id (ملف طبي).",
    "التواريخ TEXT بصيغة ISO — لذلك المقارنة النصية (end_date >= ?) صحيحة.",
    "الأعلام true/false كـ INTEGER (0/1)، والأموال NUMERIC (تُقرأ BigDecimal).",
    "لا توجد FOREIGN KEY في SQLite هنا — البديل: فحوص وجود صريحة في الـ DAOs قبل الكتابة وقبل الحذف (Rule 4).",
  ], M + 0.2, 1.95, 5.8, 4.7, { fontSize: 13, spaceAfter: 9 });
  s.addNotes("سؤال متوقع: 'لماذا لا توجد Foreign Keys؟' — الجواب: SQLite يدعمها لكنها مطفأة افتراضياً وتتطلب PRAGMA؛ اخترنا حماية مكافئة في كود التطبيق: كل DAO يتحقق من وجود المرجع قبل الكتابة، والحذف يُرفض عند وجود مراجع.");
}

// ============================================================
// 19 · SampleDataSeeder
// ============================================================
{
  const s = slideBase();
  header(s, "SampleDataSeeder.java — بذور البيانات", "SEED DATA");
  s.addText("ملف واحد يزرع بيانات تجريبية متسقة تُظهر كل الحالات: نشط، موقوف، غير نشط، منتهية، مجمدة، ملغاة، غياب (NO_SHOW)، دفعة ملغاة...", {
    x: M, y: 1.2, w: W - 2 * M, h: 0.6, fontSize: 14.5, fontFace: F, color: TEXT, align: "right", rtlMode: true, margin: 0,
  });
  const seeds = [
    ["4", "حسابات موظفين", "أحدها غير نشط لإظهار Rule 16"],
    ["3", "مدربون", "تخصصات وأجور مختلفة"],
    ["5", "أعضاء", "ACTIVE / SUSPENDED / INACTIVE"],
    ["5", "عضويات", "ACTIVE / منتهية / FROZEN / CANCELLED"],
    ["3", "مواعيد متاحة", "بسعات 1-3 لعرض FULL"],
    ["4", "جلسات", "CONFIRMED / REQUESTED / COMPLETED / NO_SHOW"],
    ["6", "معدات", "بما فيها واحدة IN_MAINTENANCE"],
    ["4", "مدفوعات", "منها زوج void + بديل (Rule 19)"],
  ];
  const cw = 2.99, gap = 0.12;
  let x = W - M - cw, row = 0;
  seeds.forEach((sd, i) => {
    if (i === 4) { row = 1; x = W - M - cw; }
    const yy = 2.15 + row * 1.95;
    card(s, x, yy, cw, 1.8, (i + row) % 2 === 0 ? TINT : TINT2);
    s.addText(sd[0], { x: x + 0.15, y: yy + 0.08, w: cw - 0.3, h: 0.6, fontSize: 26, bold: true, fontFace: F, color: PRIMARY, align: "right", margin: 0 });
    s.addText(sd[1], { x: x + 0.15, y: yy + 0.66, w: cw - 0.3, h: 0.32, fontSize: 12.5, bold: true, fontFace: F, color: TEXT, align: "right", rtlMode: true, margin: 0 });
    s.addText(sd[2], { x: x + 0.15, y: yy + 1.0, w: cw - 0.3, h: 0.68, fontSize: 10.5, fontFace: F, color: MUTED, align: "right", rtlMode: true, margin: 0 });
    x -= cw + gap;
  });
  card(s, M, 6.35, W - 2 * M, 0.7, "F5EFE3");
  s.addText("دالتان فقط: seedIfEmpty() عند بدء التشغيل (إذا كانت القاعدة فارغة)، و resetAndSeed() من قائمة الأدوات [10] أو من حزمة التحقق بعد مسح الجداول.", {
    x: M + 0.2, y: 6.42, w: W - 2 * M - 0.4, h: 0.56, fontSize: 12.5, fontFace: F, color: TEXT, align: "right", valign: "middle", rtlMode: true, margin: 0,
  });
  s.addNotes("قيمة البذور: كل شاشة تبدأ ممتلئة بحالات واقعية، وحزمة التحقق تعتمد على ترتيب هذا البذر (مثل العضوية رقم 4 المجمدة). resetAndSeed يمسح الجداول بإلغاء sqlite_sequence لتعود الترقيمات من 1.");
}

// ============================================================
// 20 · Models
// ============================================================
{
  const s = slideBase();
  header(s, "طبقة الموديلات — 14 كياناً و 11 نوعاً", "com.gym.model");
  card(s, W - M - 5.9, 1.3, 5.9, 5.5, TINT2);
  s.addText("النمط الموحد لكل كيان", { x: W - M - 5.7, y: 1.45, w: 5.5, h: 0.4, fontSize: 14.5, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "حقول خاصة private + مُنشئ فارغ + مُنشئ كامل (يسهل البذر والاختبار).",
    "getters/setters عادية — لا منطق مخفي داخل الموديل.",
    "equals/hashCode على المعرّف id فقط — تتسق مع مفتاح الجدول.",
    "implements Serializable + serialVersionUID = 1L لدعم النسخ .ser.",
    "الأموال BigDecimal (دقة مالية) والتواريخ String بصيغة ISO.",
    "الحقول الاختيارية Integer (مثل trainerId) — null = غير محدد.",
  ], W - M - 5.7, 1.95, 5.5, 4.7, { fontSize: 13, spaceAfter: 9 });
  card(s, M, 1.3, 6.4, 5.5, TINT);
  s.addText("الأنواع التعدادية (Enums) = حالات صارمة", { x: M + 0.2, y: 1.45, w: 6.0, h: 0.4, fontSize: 14.5, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  const enums = [
    ["CustomerStatus", "ACTIVE · INACTIVE · SUSPENDED"],
    ["MembershipStatus", "ACTIVE · FROZEN · EXPIRED · CANCELLED"],
    ["SessionStatus", "REQUESTED · CONFIRMED · COMPLETED · NO_SHOW · CANCELLED"],
    ["AvailabilityStatus", "OPEN · FULL · CLOSED"],
    ["SubscriptionStatus", "ACTIVE · EXPIRED · CANCELLED"],
    ["StaffRole", "ADMIN · MANAGER · STAFF"],
    ["PlanType", "MONTHLY · QUARTERLY · YEARLY"],
    ["PaymentMethod", "CASH · CARD · TRANSFER"],
    ["EquipmentStatus / Condition", "AVAILABLE/IN_MAINTENANCE/RETIRED · NEW/GOOD/FAIR/POOR"],
  ];
  let y = 1.95;
  enums.forEach((e) => {
    s.addText(e[0], { x: M + 0.2, y: y, w: 2.6, h: 0.44, fontSize: 11.5, bold: true, fontFace: FC, color: PRIMARY, align: "left", valign: "top", margin: 0 });
    s.addText(e[1], { x: M + 2.9, y: y, w: 3.3, h: 0.5, fontSize: 11, fontFace: FC, color: TEXT, align: "right", valign: "top", margin: 0 });
    y += 0.53;
  });
  s.addNotes("لو سُئلتم 'لماذا enum بدل نص حر؟' — لأن الحالة غير الصحيحة تستحيل في الكود، وmapRow يحول النص من القاعدة إلى enum مع تعامل آمن مع قيمة غير معروفة.");
}

// ============================================================
// 21 · Exceptions
// ============================================================
{
  const s = slideBase();
  header(s, "نظام الأخطاء — صنفان فقط يغطيان كل شيء", "EXCEPTION HIERARCHY");
  // diagram
  card(s, W / 2 - 2.1, 1.35, 4.2, 0.85, DARK, true);
  s.addText("GymException (extends Exception)", { x: W / 2 - 2.0, y: 1.35, w: 4.0, h: 0.85, fontSize: 14, bold: true, fontFace: FC, color: WHITE, align: "center", valign: "middle", margin: 0 });
  s.addShape(pres.shapes.LINE, { x: W / 2 - 2.6, y: 2.2, w: 2.6, h: 0.55, line: { color: PRIMARY_MID, width: 2, endArrowType: "triangle" } });
  s.addShape(pres.shapes.LINE, { x: W / 2, y: 2.2, w: 2.6, h: 0.55, flipH: true, line: { color: PRIMARY_MID, width: 2, endArrowType: "triangle" } });
  card(s, W / 2 - 5.6, 2.8, 3.1, 1.5, TINT, true);
  s.addText("ValidationException", { x: W / 2 - 5.5, y: 2.9, w: 2.9, h: 0.4, fontSize: 13.5, bold: true, fontFace: FC, color: PRIMARY, align: "center", margin: 0 });
  s.addText("خطأ في المدخلات أو قاعدة عمل: بريد خاطئ، سعة ممتلئة، مكرر...", { x: W / 2 - 5.5, y: 3.3, w: 2.9, h: 0.9, fontSize: 11.5, fontFace: F, color: TEXT, align: "center", rtlMode: true, margin: 0 });
  card(s, W / 2 + 2.5, 2.8, 3.1, 1.5, TINT, true);
  s.addText("DatabaseException", { x: W / 2 + 2.6, y: 2.9, w: 2.9, h: 0.4, fontSize: 13.5, bold: true, fontFace: FC, color: PRIMARY, align: "center", margin: 0 });
  s.addText("خطأ تقني: فشل SQL، اتصال، قراءة/كتابة ملف .ser", { x: W / 2 + 2.6, y: 3.3, w: 2.9, h: 0.9, fontSize: 11.5, fontFace: F, color: TEXT, align: "center", rtlMode: true, margin: 0 });
  // flow
  card(s, M, 4.6, W - 2 * M, 2.2, TINT2);
  s.addText("دورة حياة الخطأ", { x: M + 0.2, y: 4.72, w: 6.0, h: 0.4, fontSize: 14, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "تُرمى من الـ DAO (تحقق) أو من GymRulesHelper (قواعد) مع رسالة واضحة.",
    "كل قائمة تلتقطها في catch وتطبعها بلون مناسب عبر CliTheme — دون انهيار.",
    "شبكة أمان أخيرة في لوحة القيادة: catch (Exception) يطبع الخطأ ويعود للقائمة.",
    "رسالة خطأ مفيدة دائماً: ماذا حدث + المعرف المتسبب (مثل: No trainer found with id: 9).",
  ], M + 0.2, 5.15, W - 2 * M - 0.4, 1.6, { fontSize: 13, spaceAfter: 6 });
  s.addNotes("الفكرة: صنفا استثناء مخصصان يحملان رسائل مقروءة للمستخدم، بدل SQLException التقنية. يتحقق 'checked exceptions' يجبر كل مستدعي على التعامل مع الفشل صراحة.");
}

// ============================================================
// 22 · Serialization
// ============================================================
{
  const s = slideBase();
  header(s, "النسخ الاحتياطي — SerializationUtil + GymBackupSnapshot", "BINARY .SER BACKUP");
  card(s, W - M - 6.2, 1.3, 6.2, 5.5, TINT2);
  s.addText("كيف يعمل؟", { x: W - M - 6.0, y: 1.45, w: 5.8, h: 0.4, fontSize: 15, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "GymBackupSnapshot: كائن واحد يحمل 13 قائمة (كل كيانات القاعدة) + تاريخ واسم المنشئ.",
    "saveToFile: ObjectOutputStream يكتب الكائن كاملاً في ملف .ser — ينشئ مجلد backups تلقائياً.",
    "loadFromFile: ObjectInputStream يقرأ ويتحقق instanceof GymBackupSnapshot.",
    "exportSnapshotToFile في GymRulesHelper يقرأ الـ 13 جدولاً عبر findAll ثم ينسخ.",
    "كل الموديلات Serializable — لذلك لا NotSerializableException.",
  ], W - M - 6.0, 1.95, 5.8, 4.7, { fontSize: 13.5, spaceAfter: 9 });
  card(s, M, 1.3, 6.2, 5.5, TINT);
  s.addText("الاستخدام والحدود", { x: M + 0.2, y: 1.45, w: 5.8, h: 0.4, fontSize: 15, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "القائمة [10]: تصدير نسخة، تحميل وفحصها، ومحاكاة أخطاء (ملف مفقود / مسار خاطئ) لعرض معالجة DatabaseException.",
    "serialVersionUID ثابت يجعل التحميل مستقراً بين التشغيلات.",
    "حد معروف: تعديل حقول أي موديل مستقبلاً يجعل النسخ القديمة غير قابلة للتحميل (طبيعة Java Serialization).",
    "بديل تعليمي ممكن: تصدير CSV نصي — أسهل للفحص اليدوي (قرار موثق في الملخص).",
  ], M + 0.2, 1.95, 5.8, 4.7, { fontSize: 13, spaceAfter: 9 });
  s.addNotes("لو سُئلتم 'لماذا .ser وليس CSV؟' — لأن التسلسل الثنائي يحفظ الكائنات بأكملها بجهد قليل جداً وهو جزء أصيل من منهج Java؛ وحدوده موثقة ومعروفة.");
}

// ============================================================
// 23 · MENUS MAP
// ============================================================
{
  const s = slideBase();
  header(s, "القوائم الست — خريطة سريعة", "MENU FILES MAP");
  const rows = [
    [th("أبرز الشاشات", PRIMARY), th("الملف", PRIMARY), th("القائمة", PRIMARY)],
    [tdc("MemberCliMenu.java", { fontSize: 12 }), tdc("[1] Members", {}), td("الأعضاء")],
    [tdc("TrainerCliMenu.java", { fontSize: 12 }), tdc("[2] Coaches", {}), td("المدربون")],
    [tdc("MembershipBillingCliMenu.java", { fontSize: 12 }), tdc("[3] & [7]", {}), td("العضويات والمدفوعات")],
    [tdc("ScheduleWorkoutCliMenu.java", { fontSize: 12 }), tdc("[4] & [5]", {}), td("الجدول والتمارين")],
    [tdc("FacilityAdminCliMenu.java", { fontSize: 12 }), tdc("[6] [8] [9] [10]", {}), td("المرافق والموظفون والأدوات")],
    [tdc("ValidationSuiteRunner.java", { fontSize: 12 }), tdc("[V]", {}), td("حزمة التحقق")],
  ];
  table(s, rows, M, 1.3, W - 2 * M, [5.2, 2.8, 4.33], { fontSize: 12.5, rowH: 0.5 });
  card(s, M, 5.2, W - 2 * M, 1.7, TINT2);
  s.addText("نمط موحد في كل قائمة:", { x: M + 0.2, y: 5.3, w: 6.0, h: 0.4, fontSize: 13.5, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "حلقة while مع shouldStopSubMenu + ترويسة + خيارات مرقمة + [0] رجوع / [H] الرئيسية / [L] خروج.",
    "كل عملية داخل try/catch للخطأين المعروفين + إظهار رسالة ودودة ثم pause.",
  ], M + 0.2, 5.72, W - 2 * M - 0.4, 1.1, { fontSize: 12.5, spaceAfter: 5 });
  s.addNotes("الخمسة ملفات قوائم تغطي 10 خيارات رئيسية — التوزيع منطقي وليس عشوائياً: كل ملف يجمع الشاشات المرتبطة بكياناته.");
}

// ============================================================
// 24 · MemberCliMenu
// ============================================================
{
  const s = slideBase();
  header(s, "MemberCliMenu.java — شاشات الأعضاء", "MENU [1]");
  card(s, W - M - 6.2, 1.3, 6.2, 5.5, TINT2);
  s.addText("الشاشات", { x: W - M - 6.0, y: 1.45, w: 5.8, h: 0.4, fontSize: 15, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "قائمة الأعضاء + بحث بالاسم/الهاتف/الرقم.",
    "Member 360 Profile: بطاقة تجمع كل ما يخص العضو (خطة، ملف طبي، تقدم، رصيد Rule 6).",
    "مدير الملف الطبي: إنشاء عند الطلب (Rule 3) وتعديل وحذف وتجربة وصول المدرّب (Rule 15).",
    "سجلات التقدم + رسم بياني ASCII لوزن العضو.",
    "إيقاف/إعادة تنشيط (Rule 14 يجمّد الخطط تلقائياً).",
  ], W - M - 6.0, 1.95, 5.8, 4.7, { fontSize: 13.5, spaceAfter: 9 });
  card(s, M, 1.3, 6.2, 5.5, TINT);
  s.addText("الحمايات هنا (بعد الإصلاح)", { x: M + 0.2, y: 1.45, w: 5.8, h: 0.4, fontSize: 15, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "التسجيل يسمح بفراغ المتعمَّد لعرض رسائل التحقق الودودة من CustomerDao.validate.",
    "الحذف الناعم الآن مع سؤال تأكيد قبل التنفيذ.",
    "حذف سجل تقدم يتطلب: موجود + يخص هذا العضو فقط + تأكيد — لا نجاح زائف أبداً.",
    "الحذف النهائي عبر rules.hardDeleteCustomer: ADMIN فقط + رفض عند وجود أي مراجع (Rule 4).",
  ], M + 0.2, 1.95, 5.8, 4.7, { fontSize: 13, spaceAfter: 9 });
  s.addNotes("مثال سيناريو للعرض: سجلوا عضواً ببريد خاطئ عمداً لتظهروا رسالة التحقق، ثم افتحوا ملفه 360 وأروا الرصيد المحسوب حياً.");
}

// ============================================================
// 25 · TrainerCliMenu
// ============================================================
{
  const s = slideBase();
  header(s, "TrainerCliMenu.java — شاشات المدربين", "MENU [2]");
  card(s, W - M - 6.2, 1.3, 6.2, 5.5, TINT2);
  s.addText("الشاشات", { x: W - M - 6.0, y: 1.45, w: 5.8, h: 0.4, fontSize: 15, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "قائمة المدربين مع عداد NO_SHOW لكل مدرب (Rule 2).",
    "ملف المدرب + قائمة الأعضاء المكلفين به (أساس Rule 15).",
    "المواعيد المتاحة: نشر، فتح/إغلاق (Rule 13)، تعديل السعة، حذف آمن.",
    "اشتراكات المدرب: إنشاء (Rule 11: مطابقة المكلَّف)، تجديد +30 يوم، إلغاء.",
    "جدول المواعيد يعرض المحجوز x/السعة والحالة المشتقة (FULL).",
  ], W - M - 6.0, 1.95, 5.8, 4.7, { fontSize: 13.5, spaceAfter: 9 });
  card(s, M, 1.3, 6.2, 5.5, TINT);
  s.addText("الحمايات هنا", { x: M + 0.2, y: 1.45, w: 5.8, h: 0.4, fontSize: 15, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "حذف موعد يُرفض إن فيه جلسات CONFIRMED — تُلغى أولاً (إصلاح يمنع جلسات يتيمة).",
    "تجديد الاشتراك يتعامل بأمان مع تاريخ نهاية تالف مخزّن (try/catch حول LocalDate.parse).",
    "إلغاء اشتراك غير موجود يطبع رسالة واضحة بدل الصمت.",
    "التحقق في TrainerDao يمنع أجراً سالباً وخبرة سالبة وبريداً خاطئاً.",
  ], M + 0.2, 1.95, 5.8, 4.7, { fontSize: 13, spaceAfter: 9 });
  s.addNotes("اربطوا بين المواعيد والسعة: الحجز من [4] يمر بـ bookOpenSlot الذي يفحص السعة (Rule 17) والحالة (CLOSED) وتاريخ الماضي وحالة العضو — كل حواجز الحجز في مكان واحد.");
}

// ============================================================
// 26 · MembershipBillingCliMenu
// ============================================================
{
  const s = slideBase();
  header(s, "MembershipBillingCliMenu.java — العضويات والمدفوعات", "MENUS [3] & [7]");
  card(s, W - M - 6.2, 1.3, 6.2, 5.5, TINT2);
  s.addText("شاشة العضويات [3]", { x: W - M - 6.0, y: 1.45, w: 5.8, h: 0.4, fontSize: 15, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "بيع خطة (MONTHLY/QUARTERLY/YEARLY) مع أسعار مقترحة ونهاية محسوبة تلقائياً.",
    "تجميد نشطة + إلغاء تجميد ينقل endDate (Rule 7).",
    "تجديد يرفض الملغاة، وإلغاء مباشر بحالة CANCELLED.",
    "الجدول يعرض الحالة الفعلية (Effective) المحسوبة ليوم اليوم — المنتهية تظهر EXPIRED دون تعديل القاعدة.",
  ], W - M - 6.0, 1.95, 5.8, 4.7, { fontSize: 13.5, spaceAfter: 8 });
  card(s, M, 1.3, 6.2, 5.5, TINT);
  s.addText("شاشة المدفوعات [7]", { x: M + 0.2, y: 1.45, w: 5.8, h: 0.4, fontSize: 15, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "تسجيل دفعة: هدف واحد بالضبط + تطابق المالك + إيصال فريد (Rule 12).",
    "إدخال يدوي بالروابط للمتقدمين — نفس التحقق يحميه.",
    "بحث برقم الإيصال + الرصيد المستحق التفصيلي (Rule 6).",
    "Void & Replace (Rule 19): تحقق من الإيصال البديل قبل إلغاء القديمة — لا نصف عملية أبداً.",
  ], M + 0.2, 1.95, 5.8, 4.7, { fontSize: 13.5, spaceAfter: 8 });
  s.addNotes("سيناريو جميل للعرض: ألغِ (void) دفعة عضو ثم اسأل عن رصيده — ستراه يرتفع مباشرة في Rule 6، وهذا يثبت تكامل القواعد 6/12/19 معاً.");
}

// ============================================================
// 27 · ScheduleWorkoutCliMenu
// ============================================================
{
  const s = slideBase();
  header(s, "ScheduleWorkoutCliMenu.java — الجدول والتمارين", "MENUS [4] & [5]");
  card(s, W - M - 6.2, 1.3, 6.2, 5.5, TINT2);
  s.addText("شاشة الجدول [4]", { x: W - M - 6.0, y: 1.45, w: 5.8, h: 0.4, fontSize: 15, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "حجز موعد منشور = CONFIRMED فوراً بضوابط 1/5/13/17.",
    "طلب خارج المواعيد = REQUESTED ثم تأكيد (Rule 18).",
    "حجز استثنائي للـ ADMIN يتجاوز السعة (Rules 1/16).",
    "إلغاء بقاعدة الساعتين وسبب إلزامي (Rule 10).",
    "عند حجز باشتراك: السعر يُفرض صفراً (Rule 11).",
  ], W - M - 6.0, 1.95, 5.8, 4.7, { fontSize: 13.5, spaceAfter: 8 });
  card(s, M, 1.3, 6.2, 5.5, TINT);
  s.addText("شاشة التمارين [5]", { x: M + 0.2, y: 1.45, w: 5.8, h: 0.4, fontSize: 15, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
  bulletsAr(s, [
    "إنشاء برنامج تمرين (تمارين نصية بتمارين وتكرارات).",
    "نشر نسخة جديدة v+1 — النسخة فريدة لكل عضو (تحقق excludeId).",
    "عرض التمارين وإعادة إسناد المدرب المنشئ.",
    "Live Workout HUD: محاكي تفاعلي يمر على التمارين بنِسَب تقدم.",
    "حذف برنامج مع وجود و تأكيد — لا نجاح زائف.",
  ], M + 0.2, 1.95, 5.8, 4.7, { fontSize: 13.5, spaceAfter: 8 });
  s.addNotes("آلة الحالات (Rule 18) منفذة في SessionDao.validateTransition وتعزز برسائل صريحة في GymRulesHelper — أي قفزة غير قانونية ترفض برسالة توضح الحالة الحالية.");
}

// ============================================================
// 28 · FacilityAdminCliMenu
// ============================================================
{
  const s = slideBase();
  header(s, "FacilityAdminCliMenu.java — المرافق والموظفون والأدوات", "MENUS [6] [8] [9] [10]");
  const cw = 2.99, gap = 0.12;
  const blocks = [
    ["[6] الحضور", ["دخول سريع بضوابط Rule 5", "خروج + تقرير يومي", "تصحيح وقت صف موجود (لا حذف — Rule 19)"]],
    ["[8] المعدات", ["إضافة/تعديل حالة ووضع", "حذف بوجود وتأكيد", "خريطة صالة ASCII بأربع مناطق"]],
    ["[9] الموظفون", ["إضافة حساب + أدوار (Rule 16)", "تنشيط/إيقاف + تعديل كلمة المرور", "منع تدمير حسابك بنفسك (إيقاف/حذف/تخفيض دور)"]],
    ["[10] الأدوات", ["تصدير/تحميل نسخة .ser", "محاكاة أخطاء قاعدة البيانات", "تصفير القاعدة وإعادة البذر"]],
  ];
  let x = W - M - cw;
  blocks.forEach((b, i) => {
    card(s, x, 1.5, cw, 5.0, i % 2 === 0 ? TINT : TINT2, true);
    s.addText(b[0], { x: x + 0.15, y: 1.65, w: cw - 0.3, h: 0.38, fontSize: 14, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
    bulletsAr(s, b[1], x + 0.15, 2.15, cw - 0.3, 4.2, { fontSize: 11.5, spaceAfter: 6 });
    x -= cw + gap;
  });
  s.addNotes("أبرز الحمايات: لا يمكنك تعطيل أو حذف حسابك الحالي أو تغيير دورك بعيداً عن ADMIN أثناء الجلسة — إصلاح يمنع قفل النظام على نفسه.");
}

// ============================================================
// 29 · DIVIDER 3
// ============================================================
divider("03", "الإصلاحات وأسئلة المناقشة", "FIXES & EXPECTED QUESTIONS");

// ============================================================
// 30 · FIXES 1
// ============================================================
{
  const s = slideBase();
  header(s, "ماذا أصلحنا ولماذا (1/2) — الأخطاء التي كانت تُنهي التطبيق", "FIX HIGHLIGHTS 1/2");
  const rows = [
    [th("الإصلاح", PRIMARY), th("الخطأ القديم", PRIMARY), th("المكان", PRIMARY)],
    [td("تحليل تواريخ محمي (parseStoredDate/Time) برسالة تحقق بدل انهيار DateTimeParseException"), td("تاريخ تالف في القاعدة كان يُسقط التطبيق كاملاً عند تجميد/تجديد/إلغاء"), tdc("GymRulesHelper")],
    [td("شبكة أمان catch (Exception) حول قوائم اللوحة — خطأ غير متوقع يُطبع ويعود للتطبيق"), td("أي RuntimeException كانت تُنهي main بكومة أخطاء"), tdc("GymCliApp")],
    [td("حماية Enter الفارغ في readDouble/readBigDecimal/readIsoDate/readIsoTime قبل المحاولة"), td("ضغط Enter بلا قيمة افتراضية = حلقة لا نهائية من رسالة خطأ"), tdc("CliInput")],
    [td("فحص null بعد كل findById في حزمة التحقق + عزل كل سيناريو (runCheck)"), td("نقص بيانات البذر كان يعني NPE وإنهاء التطبيق وتوقف اللوحة"), tdc("ValidationSuiteRunner")],
    [td("تجديد الاشتراك بـ try/catch حول LocalDate.parse"), td("نفس عائلة انهيار التواريخ في شاشة المدربين"), tdc("TrainerCliMenu")],
  ];
  table(s, rows, M, 1.3, W - 2 * M, [5.6, 3.4, 3.33], { fontSize: 11.5, rowH: 0.78 });
  s.addNotes("الخلاصة: قبل الإصلاحات كان بإمكان سطر واحد تالف في قاعدة البيانات أن يُسقط التطبيق. الآن: كل parse محمي، وكل حلقة قائمة لديها شبكة أمان، وحزمة التحقق معزولة السيناريوهات.");
}

// ============================================================
// 31 · FIXES 2
// ============================================================
{
  const s = slideBase();
  header(s, "ماذا أصلحنا ولماذا (2/2) — التحقق وسلامة البيانات", "FIX HIGHLIGHTS 2/2");
  const rows = [
    [th("الإصلاح", PRIMARY), th("الخطأ القديم", PRIMARY), th("المكان", PRIMARY)],
    [td("كل حذف يتحقق من الوجود أولاً (+ تأكيد) — ولن يطبع نجاحاً زائفاً بعد الآن"), td("'Deleted #999' تُطبع حتى للمعرفات غير الموجودة"), tdc("كل القوائم")],
    [td("حذف سجل التقدم يتطلب ملكية العضو نفسه؛ حذف موعد فيه CONFIRMED مرفوض"), td("عضو كان يحذف سجلات غيره؛ حذف موعد يتيم الجلسات"), tdc("Member/Trainer")],
    [td("Void & Replace يتحقق من إيصال البديل قبل الإلغاء + الإيصال إلزامي"), td("إلغاء يُنفذ ثم يفشل البديل = نصف عملية؛ ورسالة نجاح كاذبة"), tdc("Rule 19 flow")],
    [td("فحص Rule 5 حتى عند التعديل (sameDayRowExists مع excludeId)"), td("تعديل تاريخ حضور كان يصنع صفين ليوم واحد"), tdc("AttendanceDao")],
    [td("منع حجز موعد في الماضي وعضو غير ACTIVE + فحص حالات Rule 18 الصريح"), td("حجز الماضي والموقوف كان ممكناً"), tdc("GymRulesHelper")],
    [td("فحص عدد الصفوف في update/delete لكل الـ DAOs + رسائل ودودة لقيود UNIQUE + busy_timeout + طباعة مسار القاعدة"), td("عمليات صامتة بلا أثر وأخطاء تقنية غامضة"), tdc("13 DAO + DB")],
  ];
  table(s, rows, M, 1.3, W - 2 * M, [5.6, 3.4, 3.33], { fontSize: 11, rowH: 0.8 });
  s.addNotes("رسالة الفريق: هذه ليست مجرد إصلاحات — إنها أمثلة عملية على 'التفكير في ما يمكن أن يخطئ فيه المستخدم'. التحقق من الوجود والملكية قبل أي عملية هو جوهر سلامة البيانات.");
}

// ============================================================
// 32 · Q&A 1
// ============================================================
{
  const s = slideBase();
  header(s, "أسئلة متوقعة وأجوبة قصيرة (1/2)", "EXPECTED QUESTIONS 1/2");
  const qa = [
    ["لماذا استخدمنا نمط DAO؟", "لفصل الوصول للبيانات عن الواجهة. تغيير قاعدة البيانات يمس الـ DAOs فقط، والقواعد تُختبر بمعزل عن SQL."],
    ["لماذا PreparedStatement وليس نص SQL مدموج؟", "حماية من SQL Injection، وتعامل آمن مع الاقتباسات والأنواع، وقابلية إعادة استخدام العبارة."],
    ["ما الفرق بين ValidationException و DatabaseException؟", "الأول: خطأ مستخدم/قاعدة عمل (يراجع المستخدم مدخلاته). الثاني: خطأ تقني (SQL/ملف) يحتاج انتباه المطور."],
    ["لماذا try-with-resources؟", "يغلق Connection/Statement/ResultSet تلقائياً حتى عند الاستثناء — يمنع تسريب الموارد دون كتل finally يدوية."],
    ["كيف تُطبق Rule 17 (سعة الموعد)؟", "SELECT COUNT للجلسات CONFIRMED في الموعد، إن وصل maxCustomers تُشتق حالة FULL ويُرفض الحجز."],
    ["كيف تعمل آلة حالات الجلسة (Rule 18)؟", "validateTransition في SessionDao يرفض أي انتقال غير قانوني: مثلاً REQUESTED لا تصبح COMPLETED إلا عبر CONFIRMED."],
  ];
  let y = 1.3;
  qa.forEach((q, i) => {
    card(s, M, y, W - 2 * M, 0.88, i % 2 === 0 ? TINT2 : TINT);
    s.addText("س: " + q[0], { x: M + 0.2, y: y + 0.06, w: W - 2 * M - 0.4, h: 0.36, fontSize: 13.5, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
    s.addText("ج: " + q[1], { x: M + 0.2, y: y + 0.44, w: W - 2 * M - 0.4, h: 0.4, fontSize: 12.5, fontFace: F, color: TEXT, align: "right", rtlMode: true, margin: 0 });
    y += 0.98;
  });
  s.addNotes("وزعوا الأسئلة على الفريق: كل فرد يتقن جوابين على الأقل بكلماته. الأجوبة هنا قصيرة عمداً — وسّعوها بمثال من الشاشة إن طُلب.");
}

// ============================================================
// 33 · Q&A 2
// ============================================================
{
  const s = slideBase();
  header(s, "أسئلة متوقعة وأجوبة قصيرة (2/2)", "EXPECTED QUESTIONS 2/2");
  const qa = [
    ["كيف نمنع حذف بيانات مرتبطة؟", "حارس Rule 4 يعدّ المراجع في كل الجداول قبل الحذف — وجدت واحدة يُرفض الحذف ويقترح الحذف الناعم INACTIVE."],
    ["ما فائدة قاعدة sandbox في حزمة التحقق؟", "قاعدة مؤقتة gym_sandbox.db: تُنشأ وتُبذر وتُختبر ثم تُحذف — التحقق الشامل دون أي خطر على gym.db."],
    ["لماذا implements Serializable؟", "لحفظ نسخة كاملة (.ser) من كل الكيانات بكائن واحد (GymBackupSnapshot) عبر ObjectOutputStream/ObjectInputStream."],
    ["ماذا يحدث عندما تنتهي صلاحية خطة؟", "لا شيء يُعدل — effectiveMembershipStatus يحسب EXPIRED مقارنة endDate باليوم، فالعرض صادق والقاعدة نظيفة."],
    ["كيف يعمل التجميد (freeze)؟", "الحالة تصبح FROZEN مع عدد الأيام؛ وعند unfreeze ينتقل endDate للخلف بمقدار أيام التجميد فلا يخسر العضو يوماً."],
    ["لماذا التواريخ TEXT بصيغة ISO؟", "لأن yyyy-MM-dd يرتب نصياً كما يرتب زمنياً — فمقارنة end_date >= ? صحيحة بدون دوال تواريخ خاصة."],
  ];
  let y = 1.3;
  qa.forEach((q, i) => {
    card(s, M, y, W - 2 * M, 0.88, i % 2 === 0 ? TINT2 : TINT);
    s.addText("س: " + q[0], { x: M + 0.2, y: y + 0.06, w: W - 2 * M - 0.4, h: 0.36, fontSize: 13.5, bold: true, fontFace: F, color: PRIMARY, align: "right", rtlMode: true, margin: 0 });
    s.addText("ج: " + q[1], { x: M + 0.2, y: y + 0.44, w: W - 2 * M - 0.4, h: 0.4, fontSize: 12.5, fontFace: F, color: TEXT, align: "right", rtlMode: true, margin: 0 });
    y += 0.98;
  });
  s.addNotes("إن سُئلتم عن إصلاح محدد، افتحوا ملف BUGFIX_SUMMARY.md المرافق — فيه كل إصلاح: أين، ماذا، ولماذا، مع قائمة القرارات المتعمدة (مثل عدم استخدام transactions).");
}

// ============================================================
// 34 · DEMO + CLOSING (dark)
// ============================================================
{
  const s = slideBase(DARK);
  s.addShape(pres.shapes.OVAL, { x: 10.9, y: -1.4, w: 4.2, h: 4.2, fill: { color: PRIMARY, transparency: 60 }, line: { type: "none" } });
  s.addShape(pres.shapes.OVAL, { x: -1.2, y: 5.2, w: 3.6, h: 3.6, fill: { color: PRIMARY_MID, transparency: 70 }, line: { type: "none" } });
  s.addText("سيناريو العرض العملي — 4 دقائق", {
    x: M, y: 0.55, w: W - 2 * M, h: 0.6, fontSize: 26, bold: true, fontFace: F, color: WHITE, align: "center", rtlMode: true, margin: 0,
  });
  const steps = [
    ["1", "شغلوا run.bat — اعرضوا طباعة مسار gym.db ثم Quick Login كـ ADMIN"],
    ["2", "من [V] شغلوا الحزمة الكاملة (1) — SCOREBOARD 22/22 أمام الجميع"],
    ["3", "جولة سريعة: [1] عضو 360 + رصيد، [4] حجز موعد، [7] دفعة ثم Void"],
    ["4", "أنهِ بـ [0] ثم [0] — خروج نظيف بلا أي رسالة خطأ"],
  ];
  const cw = 2.95, gap = 0.15;
  let x = W - M - cw;
  steps.forEach((st) => {
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y: 1.55, w: cw, h: 2.5, rectRadius: 0.09, fill: { color: "1E4250" }, line: { color: PRIMARY_MID, width: 0.75 } });
    s.addText(st[0], { x: x + 0.18, y: 1.7, w: cw - 0.36, h: 0.6, fontSize: 30, bold: true, fontFace: F, color: ACCENT, align: "right", margin: 0 });
    s.addText(st[1], { x: x + 0.18, y: 2.35, w: cw - 0.36, h: 1.55, fontSize: 12.5, fontFace: F, color: WHITE, align: "right", valign: "top", rtlMode: true, margin: 0 });
    x -= cw + gap;
  });
  s.addText("ملخص كلمة أخيرة", { x: M, y: 4.55, w: W - 2 * M, h: 0.5, fontSize: 17, bold: true, fontFace: F, color: WHITE, align: "center", rtlMode: true, margin: 0 });
  s.addText("مشروع متكامل بأربع طبقات واضحة: قوائم تدخل، RulesHelper يقرر، DAO يحفظ، SQLite تخزن — و19 قاعدة تحمي البيانات في كل اتجاه. كل ملف له دور واحد يسهل شرحه.", {
    x: 1.6, y: 5.1, w: W - 3.2, h: 0.85, fontSize: 14.5, fontFace: F, color: DARKMUTED, align: "center", rtlMode: true, margin: 0,
  });
  s.addText("بالتوفيق في المناقشة!", {
    x: M, y: 6.2, w: W - 2 * M, h: 0.6, fontSize: 22, bold: true, fontFace: F, color: ACCENT, align: "center", rtlMode: true, margin: 0,
  });
  pageNum(s, true);
  s.addNotes("تدربوا على هذا السيناريو مرة واحدة على الأقل قبل المناقشة. الملفات المرافقة: BUGFIX_SUMMARY.md (تفصيل الإصلاحات) وهذا العرض.");
}

// ---------- write ----------
pres.writeFile({ fileName: "C:/Users/pc/Desktop/JAVA-Project/GymProject_Team_Guide.pptx" }).then(() => console.log("WRITTEN OK, slides:", pageNo));
