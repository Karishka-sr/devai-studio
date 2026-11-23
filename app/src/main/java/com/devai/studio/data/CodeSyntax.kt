package com.devai.studio.data

enum class SyntaxKind {
    KEYWORD,
    TYPE,
    STRING,
    COMMENT,
    NUMBER,
    ANNOTATION,
    FUNCTION,
}

data class SyntaxSpan(val start: Int, val end: Int, val kind: SyntaxKind)

private data class SyntaxProfile(
    val keywords: Set<String>,
    val types: Set<String> = emptySet(),
    val lineComments: List<String> = listOf("//"),
    val blockComments: List<Pair<String, String>> = listOf("/*" to "*/"),
    val strings: List<String> = listOf("\"", "'"),
    val caseInsensitive: Boolean = false,
    val markup: Boolean = false,
)

private fun words(source: String, lower: Boolean): Set<String> =
    source.trim().split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .map { if (lower) it.lowercase() else it }
        .toSet()

private fun profile(
    keywords: String,
    types: String = "",
    lineComments: List<String> = listOf("//"),
    blockComments: List<Pair<String, String>> = listOf("/*" to "*/"),
    strings: List<String> = listOf("\"", "'"),
    caseInsensitive: Boolean = false,
    markup: Boolean = false,
): SyntaxProfile = SyntaxProfile(
    keywords = words(keywords, caseInsensitive),
    types = words(types, caseInsensitive),
    lineComments = lineComments,
    blockComments = blockComments,
    strings = strings.sortedByDescending { it.length },
    caseInsensitive = caseInsensitive,
    markup = markup,
)

object CodeSyntax {
    private val generic = SyntaxProfile(emptySet(), blockComments = emptyList())

    private val profiles: Map<String, SyntaxProfile> = mapOf(
        "kotlin" to profile(
            """fun val var if else when for while do return class object interface data sealed enum package import try catch finally throw is as in out by companion init constructor override open abstract private public protected internal suspend inline noinline crossinline reified lateinit const typealias this super null true false break continue""",
            """Int Long Short Byte Double Float Boolean Char String Unit Any Nothing List Map Set Array""",
            strings = listOf("\"\"\"", "\"", "'"),
        ),
        "java" to profile(
            """abstract assert boolean break byte case catch char class const continue default do double else enum extends final finally float for goto if implements import instanceof int interface long native new package private protected public return short static strictfp super switch synchronized this throw throws transient try void volatile while var record sealed permits true false null""",
            """String Object Integer Long Short Byte Double Float Boolean Character List Map Set""",
        ),
        "python" to profile(
            """and as assert async await break class continue def del elif else except finally for from global if import in is lambda nonlocal not or pass raise return try while with yield match case True False None self""",
            """int float str bool list dict set tuple bytes object""",
            lineComments = listOf("#"),
            blockComments = emptyList(),
            strings = listOf("\"\"\"", "'''", "\"", "'"),
        ),
        "javascript" to profile(
            """break case catch class const continue debugger default delete do else export extends finally for function if import in instanceof let new return static super switch this throw try typeof var void while with yield async await of from true false null undefined""",
            """Array Boolean Date Error Function JSON Map Math Number Object Promise RegExp Set String Symbol""",
            strings = listOf("\"", "'", "`"),
        ),
        "typescript" to profile(
            """break case catch class const continue debugger default delete do else export extends finally for function if import in instanceof let new return static super switch this throw try typeof var void while with yield async await of from true false null undefined interface type enum implements namespace declare readonly abstract as any unknown never keyof infer is asserts satisfies""",
            """Array Boolean Date Error Function JSON Map Math Number Object Promise RegExp Set String Symbol Partial Record Pick Omit""",
            strings = listOf("\"", "'", "`"),
        ),
        "dart" to profile(
            """abstract as assert async await break case catch class const continue covariant default deferred do dynamic else enum export extends extension external factory false final finally for Function get hide if implements import in interface is late library mixin new null on operator part required rethrow return set show static super switch sync this throw true try typedef var void while with yield""",
            """int double num bool String List Map Set Object Future Stream""",
            strings = listOf("\"\"\"", "'''", "\"", "'"),
        ),
        "swift" to profile(
            """associatedtype class deinit enum extension fileprivate func import init inout internal let open operator private protocol public rethrows static struct subscript typealias var break case continue default defer do else fallthrough for guard if in repeat return switch where while as Any catch false is nil super self Self throw throws true try""",
            """Int Double Float Bool String Array Dictionary Set Optional Result""",
            strings = listOf("\"\"\"", "\""),
        ),
        "objectivec" to profile(
            """auto break case char const continue default do double else enum extern float for goto if inline int long register restrict return short signed sizeof static struct switch typedef union unsigned void volatile while interface implementation end property synthesize dynamic protocol class selector try catch finally throw autoreleasepool synchronized public private protected package""",
            """id NSObject NSString NSArray NSDictionary NSNumber BOOL NSInteger CGFloat""",
        ),
        "c" to profile(
            """auto break case char const continue default do double else enum extern float for goto if inline int long register restrict return short signed sizeof static struct switch typedef union unsigned void volatile while _Bool _Complex _Atomic""",
            """size_t FILE int8_t int16_t int32_t int64_t uint8_t uint16_t uint32_t uint64_t""",
        ),
        "cpp" to profile(
            """alignas alignof and and_eq asm auto bitand bitor bool break case catch char char16_t char32_t class compl const constexpr const_cast continue decltype default delete do double dynamic_cast else enum explicit export extern false float for friend goto if inline int long mutable namespace new noexcept not not_eq nullptr operator or or_eq private protected public register reinterpret_cast return short signed sizeof static static_assert static_cast struct switch template this thread_local throw true try typedef typeid typename union unsigned using virtual void volatile wchar_t while xor xor_eq""",
            """string vector map set unordered_map unique_ptr shared_ptr size_t""",
        ),
        "csharp" to profile(
            """abstract as base bool break byte case catch char checked class const continue decimal default delegate do double else enum event explicit extern false finally fixed float for foreach goto if implicit in int interface internal is lock long namespace new null object operator out override params private protected public readonly ref return sbyte sealed short sizeof stackalloc static string struct switch this throw true try typeof uint ulong unchecked unsafe ushort using virtual void volatile while async await record init""",
            """String Object Int32 Int64 Double Boolean List Dictionary Task""",
        ),
        "go" to profile(
            """break case chan const continue default defer else fallthrough for func go goto if import interface map package range return select struct switch type var true false nil""",
            """string int int8 int16 int32 int64 uint uint8 uint16 uint32 uint64 float32 float64 bool byte rune error any""",
            strings = listOf("\"", "'", "`"),
        ),
        "php" to profile(
            """abstract and array as break callable case catch class clone const continue declare default die do echo else elseif empty enddeclare endfor endforeach endif endswitch endwhile eval exit extends final finally fn for foreach function global goto if implements include include_once instanceof insteadof interface isset list match namespace new or print private protected public readonly require require_once return static switch throw trait try unset use var while xor yield true false null""",
            """int float string bool array object iterable void mixed never""",
            lineComments = listOf("//", "#"),
        ),
        "ruby" to profile(
            """BEGIN END alias and begin break case class def defined do else elsif end ensure false for if in module next nil not or redo rescue retry return self super then true undef unless until when while yield""",
            """String Integer Float Array Hash Symbol Range Proc""",
            lineComments = listOf("#"),
            blockComments = emptyList(),
        ),
        "scala" to profile(
            """abstract case catch class def do else extends false final finally for forSome if implicit import lazy match new null object override package private protected return sealed super this throw trait try true type val var while with yield given using enum export""",
            """Int Long Double Float Boolean String List Map Set Option Either Future""",
            strings = listOf("\"\"\"", "\"", "'"),
        ),
        "groovy" to profile(
            """abstract as assert break case catch class const continue def default do else enum extends final finally for goto if implements import in instanceof interface native new package private protected public return static strictfp super switch synchronized this throw throws trait transient try void volatile while true false null""",
            """String Integer Long Double Float Boolean List Map Set Object""",
            strings = listOf("\"\"\"", "'''", "\"", "'"),
        ),
        "bash" to profile(
            """if then else elif fi for while do done case esac function in select until time coproc return exit local export readonly declare typeset unset shift source alias echo printf read cd pwd test true false""",
            lineComments = listOf("#"),
            blockComments = emptyList(),
            strings = listOf("\"", "'", "`"),
        ),
        "powershell" to profile(
            """begin break catch class continue data define do dynamicparam else elseif end enum exit filter finally for foreach from function hidden if in param process return static switch throw trap try until using var while workflow true false null""",
            """string int bool array hashtable psobject""",
            lineComments = listOf("#"),
            blockComments = listOf("<#" to "#>"),
        ),
        "sql" to profile(
            """select from where insert into values update set delete create table alter drop index view join inner left right full outer on group by order having limit offset union all distinct as and or not null is in like between exists case when then else end primary key foreign references default unique check constraint database schema procedure function trigger begin commit rollback""",
            """int integer bigint smallint decimal numeric float double varchar char text date datetime timestamp boolean""",
            lineComments = listOf("--"),
            strings = listOf("'"),
            caseInsensitive = true,
        ),
        "html" to profile(
            """html head body title meta link script style div span p a img ul ol li table tr td th form input button label select option textarea h1 h2 h3 h4 h5 h6 header footer main nav section article aside canvas svg""",
            lineComments = emptyList(),
            blockComments = listOf("<!--" to "-->"),
            markup = true,
        ),
        "css" to profile(
            """color background display position margin padding border font flex grid width height top left right bottom z-index opacity transform transition animation media keyframes import""",
            lineComments = emptyList(),
            blockComments = listOf("/*" to "*/"),
        ),
        "json" to profile(
            """true false null""",
            strings = listOf("\""),
        ),
        "yaml" to profile(
            """true false null yes no on off""",
            lineComments = listOf("#"),
            blockComments = emptyList(),
        ),
        "xml" to profile(
            """xml version encoding standalone""",
            lineComments = emptyList(),
            blockComments = listOf("<!--" to "-->"),
            markup = true,
        ),
        "lua" to profile(
            """and break do else elseif end false for function goto if in local nil not or repeat return then true until while""",
            lineComments = listOf("--"),
            blockComments = listOf("--[[" to "]]"),
        ),
        "perl" to profile(
            """my our local sub if elsif else unless while for foreach last next redo return package use require BEGIN END and or not xor eq ne lt gt le ge cmp""",
            lineComments = listOf("#"),
            blockComments = emptyList(),
        ),
        "r" to profile(
            """if else repeat while function for in next break TRUE FALSE NULL NA Inf NaN""",
            """numeric integer character logical list data.frame matrix vector factor""",
            lineComments = listOf("#"),
            blockComments = emptyList(),
        ),
        "matlab" to profile(
            """break case catch classdef continue else elseif end for function global if otherwise parfor persistent return switch try while true false""",
            """double single int8 int16 int32 int64 uint8 uint16 uint32 uint64 char string logical cell struct table""",
            lineComments = listOf("%"),
            blockComments = listOf("%{" to "%}"),
        ),
        "julia" to profile(
            """baremodule begin break catch const continue do else elseif end export false finally for function global if import let local macro module quote return struct true try using while""",
            """Int Float64 Float32 Bool String Char Array Vector Matrix Dict Tuple""",
            lineComments = listOf("#"),
            blockComments = listOf("#=" to "=#"),
            strings = listOf("\"\"\"", "\"", "'"),
        ),
        "haskell" to profile(
            """case class data default deriving do else if import in infix infixl infixr instance let module newtype of then type where qualified as hiding""",
            """Int Integer Float Double Bool Char String Maybe Either IO""",
            lineComments = listOf("--"),
            blockComments = listOf("{-" to "-}"),
        ),
        "elixir" to profile(
            """def defp defmodule defmacro do end fn case cond if unless else try catch rescue after raise receive send spawn import alias use require true false nil when and or not in""",
            """Integer Float Boolean String Atom List Map Tuple""",
            lineComments = listOf("#"),
            blockComments = emptyList(),
            strings = listOf("\"\"\"", "\"", "'"),
        ),
        "erlang" to profile(
            """after begin case catch cond end fun if let of query receive try when and or not xor bsl bsr div rem true false""",
            """integer float boolean string list tuple map atom""",
            lineComments = listOf("%"),
            blockComments = emptyList(),
        ),
        "clojure" to profile(
            """def defn defmacro let fn if do loop recur cond case when unless try catch finally throw ns require import quote var set! new""",
            lineComments = listOf(";"),
            blockComments = emptyList(),
            strings = listOf("\""),
        ),
        "fsharp" to profile(
            """let rec and in fun function match with if then else elif for to downto while do done try finally raise exception type module namespace open use mutable async await yield return true false null""",
            """int int64 float float32 bool string char list array seq option result""",
            blockComments = listOf("(*" to "*)"),
            strings = listOf("\"\"\"", "\"", "'"),
        ),
        "vbnet" to profile(
            """AddHandler AddressOf Alias And AndAlso As Boolean ByRef Byte ByVal Call Case Catch CBool CByte CChar CDate CDbl CDec Char CInt Class CLng CObj Const Continue CSByte CShort CSng CStr CType CUInt CULng CUShort Date Decimal Declare Default Delegate Dim DirectCast Do Double Each Else ElseIf End EndIf Enum Erase Error Event Exit False Finally For Friend Function Get GetType Global GoSub GoTo Handles If Implements Imports In Inherits Integer Interface Is IsNot Let Lib Like Long Loop Me Mod Module MustInherit MustOverride MyBase MyClass Namespace Narrowing New Next Not Nothing NotInheritable NotOverridable Object Of On Operator Option Optional Or OrElse Out Overloads Overridable Overrides ParamArray Partial Private Property Protected Public RaiseEvent ReadOnly ReDim REM RemoveHandler Resume Return SByte Select Set Shadows Shared Short Single Static Step Stop String Structure Sub SyncLock Then Throw To True Try TryCast TypeOf UInteger ULong UShort Using Variant Wend When While Widening With WithEvents WriteOnly Xor""",
            lineComments = listOf("'"),
            blockComments = emptyList(),
            strings = listOf("\""),
            caseInsensitive = true,
        ),
        "assembly" to profile(
            """mov add sub mul div push pop call ret jmp je jne jz jnz cmp test lea and or xor not shl shr inc dec int syscall section global extern db dw dd dq""",
            lineComments = listOf(";"),
            blockComments = emptyList(),
        ),
        "solidity" to profile(
            """pragma solidity contract interface library function modifier event error constructor if else for while do break continue return returns public private internal external view pure payable memory storage calldata mapping address bool string bytes uint int true false import is using new delete emit revert require assert""",
            """uint256 uint128 uint64 uint32 uint8 int256 address bytes32 string bool""",
        ),
        "zig" to profile(
            """const var fn struct enum union error if else switch while for break continue return defer errdefer try catch unreachable async await suspend resume export extern pub inline noinline comptime test true false null undefined""",
            """i8 i16 i32 i64 u8 u16 u32 u64 f32 f64 bool usize isize""",
        ),
        "nim" to profile(
            """proc func method iterator template macro type var let const if elif else when case of for while break continue return yield import from export include object enum tuple ref ptr distinct static true false nil""",
            """int int8 int16 int32 int64 uint float float32 float64 bool string char seq array""",
            lineComments = listOf("#"),
            blockComments = listOf("#[" to "]#"),
            strings = listOf("\"\"\"", "\"", "'"),
        ),
        "crystal" to profile(
            """def class module struct enum union alias lib fun macro if elsif else unless case when while until for in do end begin rescue ensure return yield break next true false nil self""",
            """Int32 Int64 Float32 Float64 Bool String Char Array Hash Symbol""",
            lineComments = listOf("#"),
            blockComments = emptyList(),
        ),
        "d" to profile(
            """abstract alias align asm assert auto body bool break byte case cast catch char class const continue dchar debug default delegate delete deprecated do double else enum export extern false final finally float for foreach foreach_reverse function goto idouble if ifloat immutable import in inout int interface invariant ireal is lazy long macro mixin module new nothrow null out override package pragma private protected public real ref return scope shared short static struct super switch synchronized template this throw true try typeid typeof ubyte ucent uint ulong union unittest ushort version void volatile wchar while with""",
            """string wstring dstring size_t ptrdiff_t""",
            blockComments = listOf("/*" to "*/", "/+" to "+/"),
        ),
        "fortran" to profile(
            """program module subroutine function end if then else elseif do while for call return integer real complex logical character double precision implicit none parameter dimension allocatable pointer intent in out inout use contains interface""",
            lineComments = listOf("!"),
            blockComments = emptyList(),
            caseInsensitive = true,
        ),
        "cobol" to profile(
            """IDENTIFICATION DIVISION PROGRAM-ID ENVIRONMENT DATA WORKING-STORAGE PROCEDURE DIVISION SECTION MOVE TO ADD SUBTRACT MULTIPLY DIVIDE COMPUTE IF ELSE END-IF PERFORM UNTIL VARYING FROM BY DISPLAY ACCEPT STOP RUN PIC PICTURE VALUE OCCURS REDEFINES COPY""",
            lineComments = listOf("*>"),
            blockComments = emptyList(),
            caseInsensitive = true,
        ),
        "prolog" to profile(
            """is mod div rem true fail not consult assert retract findall forall""",
            lineComments = listOf("%"),
            blockComments = listOf("/*" to "*/"),
        ),
        "scheme" to profile(
            """define lambda let let* letrec if cond case else and or not begin do set! quote quasiquote unquote car cdr cons list""",
            lineComments = listOf(";"),
            blockComments = emptyList(),
            strings = listOf("\""),
        ),
        "ocaml" to profile(
            """and as assert begin class constraint do done downto else end exception external false for fun function functor if in include inherit initializer lazy let match method module mutable new nonrec object of open or private rec sig struct then to true try type val virtual when while with""",
            """int float bool char string list array option result""",
            blockComments = listOf("(*" to "*)"),
        ),
        "pascal" to profile(
            """and array begin case const div do downto else end file for function goto if in label mod nil not of or packed procedure program record repeat set then to type until var while with""",
            """integer real boolean char string byte word longint single double""",
            blockComments = listOf("{" to "}", "(*" to "*)"),
            strings = listOf("'"),
            caseInsensitive = true,
        ),
        "ada" to profile(
            """abort abs abstract accept access aliased all and array at begin body case constant declare delay delta digits do else elsif end entry exception exit for function generic goto if in interface is limited loop mod new not null of or others out overriding package pragma private procedure protected raise range record rem renames requeue return reverse select separate some subtype synchronized tagged task terminate then type until use when while with xor""",
            """Integer Float Boolean Character String Natural Positive""",
            lineComments = listOf("--"),
            blockComments = emptyList(),
            strings = listOf("\""),
            caseInsensitive = true,
        ),
        "vhdl" to profile(
            """abs access after alias all and architecture array assert assume attribute begin block body buffer bus case component configuration constant disconnect downto else elsif end entity exit file for function generate generic group guarded if impure in inertial inout is label library linkage literal loop map mod nand new next nor not null of on open or others out package port postponed procedure process pure range record register reject rem report return rol ror select severity shared signal sla sll sra srl subtype then to transport type unaffected units until use variable wait when while with xnor xor""",
            """std_logic std_logic_vector integer natural boolean signed unsigned""",
            lineComments = listOf("--"),
            blockComments = emptyList(),
            strings = listOf("\""),
            caseInsensitive = true,
        ),
        "verilog" to profile(
            """always and assign automatic begin buf case casex casez default defparam disable do edge else end endcase endfunction endgenerate endmodule endtask event for force forever fork function generate genvar if initial inout input integer join localparam module nand negedge nor not or output parameter posedge primitive real reg repeat signed task time unsigned wait while wire xnor xor""",
            """wire reg logic bit byte int integer real time""",
        ),
    )

    fun languageId(tag: String, code: String): String? {
        val key = tag.trim().lowercase()
            .removePrefix("language-")
            .substringBefore(',')
            .substringBefore(' ')
            .trim()
        if (key.isNotBlank()) {
            CodeLanguages.byId(key)?.id?.let { return it }
            when (key) {
                "html5" -> return "html"
                "svg" -> return "xml"
                "console", "shell-session" -> return "bash"
                "jsonc" -> return "json"
                "objective-c" -> return "objectivec"
            }
        }
        return detectId(code)
    }

    fun displayName(tag: String, code: String): String {
        val id = languageId(tag, code)
        return CodeLanguages.byId(id)?.name ?: tag.trim().ifBlank { "code" }
    }

    fun highlight(tag: String, code: String): List<SyntaxSpan> {
        val id = languageId(tag, code) ?: return emptyList()
        val syntax = profiles[id] ?: generic
        if (syntax.markup) return highlightMarkup(code)
        if (id == "yaml") return merge(lex(code, syntax) + yamlKeys(code))
        return lex(code, syntax)
    }

    private fun lex(code: String, syntax: SyntaxProfile): List<SyntaxSpan> {
        val spans = mutableListOf<SyntaxSpan>()
        var i = 0
        while (i < code.length) {
            val lineComment = syntax.lineComments.firstOrNull { code.startsWith(it, i) }
            if (lineComment != null) {
                val end = code.indexOf('\n', i).let { if (it < 0) code.length else it }
                spans.add(SyntaxSpan(i, end, SyntaxKind.COMMENT))
                i = end
                continue
            }
            val blockComment = syntax.blockComments.firstOrNull { code.startsWith(it.first, i) }
            if (blockComment != null) {
                val end = code.indexOf(blockComment.second, i + blockComment.first.length)
                    .let { if (it < 0) code.length else it + blockComment.second.length }
                spans.add(SyntaxSpan(i, end, SyntaxKind.COMMENT))
                i = end
                continue
            }
            val quote = syntax.strings.firstOrNull { code.startsWith(it, i) }
            if (quote != null) {
                val end = scanString(code, i, quote)
                spans.add(SyntaxSpan(i, end, SyntaxKind.STRING))
                i = end
                continue
            }
            val c = code[i]
            if (c == '@' && (i == 0 || !isIdentifierPart(code[i - 1]))) {
                var j = i + 1
                while (j < code.length && isIdentifierPart(code[j])) j++
                if (j > i + 1) {
                    spans.add(SyntaxSpan(i, j, SyntaxKind.ANNOTATION))
                    i = j
                    continue
                }
            }
            if (c.isDigit() || (c == '.' && i + 1 < code.length && code[i + 1].isDigit())) {
                val end = scanNumber(code, i)
                spans.add(SyntaxSpan(i, end, SyntaxKind.NUMBER))
                i = end
                continue
            }
            if (isIdentifierStart(c)) {
                var j = i + 1
                while (j < code.length && isIdentifierPart(code[j])) j++
                val word = code.substring(i, j)
                val key = if (syntax.caseInsensitive) word.lowercase() else word
                val kind = when {
                    key in syntax.keywords -> SyntaxKind.KEYWORD
                    key in syntax.types -> SyntaxKind.TYPE
                    nextNonSpace(code, j) == '(' -> SyntaxKind.FUNCTION
                    else -> null
                }
                if (kind != null) spans.add(SyntaxSpan(i, j, kind))
                i = j
                continue
            }
            i++
        }
        return merge(spans)
    }

    private fun yamlKeys(code: String): List<SyntaxSpan> {
        val key = Regex("^[ \\t]*(?:-[ \\t]+)?([A-Za-z0-9_.-]+)[ \\t]*:", RegexOption.MULTILINE)
        return key.findAll(code).map { match ->
            val group = match.groups[1]!!
            SyntaxSpan(group.range.first, group.range.last + 1, SyntaxKind.TYPE)
        }.toList()
    }

    private fun highlightMarkup(code: String): List<SyntaxSpan> {
        val spans = mutableListOf<SyntaxSpan>()
        var i = 0
        while (i < code.length) {
            if (code.startsWith("<!--", i)) {
                val end = code.indexOf("-->", i + 4).let { if (it < 0) code.length else it + 3 }
                spans.add(SyntaxSpan(i, end, SyntaxKind.COMMENT))
                i = end
                continue
            }
            if (code[i] == '<') {
                i = highlightTag(code, i, spans)
                continue
            }
            i++
        }
        return merge(spans)
    }

    private fun highlightTag(code: String, start: Int, spans: MutableList<SyntaxSpan>): Int {
        var i = start + 1
        if (i < code.length && code[i] == '/') i++
        if (i < code.length && code[i] == '!') return code.indexOf('>', i).let { if (it < 0) code.length else it + 1 }
        val nameStart = i
        while (i < code.length && (code[i].isLetterOrDigit() || code[i] in "-_:")) i++
        if (i > nameStart) spans.add(SyntaxSpan(nameStart, i, SyntaxKind.KEYWORD))
        while (i < code.length) {
            while (i < code.length && code[i].isWhitespace()) i++
            if (i >= code.length) break
            if (code[i] == '>' || code[i] == '/') return i + 1
            val quote = if (code[i] == '"' || code[i] == '\'') code[i].toString() else null
            if (quote != null) {
                val end = scanString(code, i, quote)
                spans.add(SyntaxSpan(i, end, SyntaxKind.STRING))
                i = end
                continue
            }
            val attrStart = i
            while (i < code.length && (code[i].isLetterOrDigit() || code[i] in "-_:@.")) i++
            if (i > attrStart) spans.add(SyntaxSpan(attrStart, i, SyntaxKind.TYPE))
            while (i < code.length && code[i].isWhitespace()) i++
            if (i < code.length && code[i] == '=') {
                i++
                while (i < code.length && code[i].isWhitespace()) i++
            }
        }
        return code.length
    }

    private fun scanString(code: String, start: Int, quote: String): Int {
        var i = start + quote.length
        while (i < code.length) {
            if (code.startsWith(quote, i)) return i + quote.length
            if (quote.length == 1 && code[i] == '\\') {
                i += 2
                continue
            }
            i++
        }
        return code.length
    }

    private fun scanNumber(code: String, start: Int): Int {
        var i = start
        if (code.startsWith("0x", i, ignoreCase = true) || code.startsWith("0b", i, ignoreCase = true)) {
            i += 2
            while (i < code.length && (code[i].isLetterOrDigit() || code[i] == '_')) i++
            return i
        }
        while (i < code.length && (code[i].isDigit() || code[i] == '_')) i++
        if (i < code.length && code[i] == '.') {
            i++
            while (i < code.length && (code[i].isDigit() || code[i] == '_')) i++
        }
        if (i < code.length && (code[i] == 'e' || code[i] == 'E')) {
            i++
            if (i < code.length && (code[i] == '+' || code[i] == '-')) i++
            while (i < code.length && code[i].isDigit()) i++
        }
        while (i < code.length && code[i] in "fFdDlLuU") i++
        return i
    }

    private fun nextNonSpace(code: String, start: Int): Char? {
        var i = start
        while (i < code.length && code[i].isWhitespace()) i++
        return if (i < code.length) code[i] else null
    }

    private fun isIdentifierStart(c: Char): Boolean = c.isLetter() || c == '_' || c == '$'

    private fun isIdentifierPart(c: Char): Boolean = c.isLetterOrDigit() || c == '_' || c == '$'

    private fun merge(spans: List<SyntaxSpan>): List<SyntaxSpan> {
        if (spans.isEmpty()) return emptyList()
        val out = mutableListOf(spans.first())
        for (span in spans.drop(1)) {
            val last = out.last()
            if (last.kind == span.kind && last.end == span.start) {
                out[out.lastIndex] = last.copy(end = span.end)
            } else {
                out.add(span)
            }
        }
        return out
    }

    private fun detectId(code: String): String? {
        val text = code.trim()
        if (text.isEmpty()) return null
        if (text.startsWith("<?php")) return "php"
        if (text.startsWith("<!DOCTYPE", ignoreCase = true) || text.startsWith("<html", ignoreCase = true)) return "html"
        if (text.startsWith("<") && text.contains(">")) return "html"
        if (text.startsWith("<?xml")) return "xml"
        if (text.startsWith("#!/")) return "bash"
        if (text.startsWith("SELECT ", ignoreCase = true) || text.startsWith("INSERT ", ignoreCase = true) || text.startsWith("CREATE TABLE", ignoreCase = true)) return "sql"
        if (text.startsWith("{") && text.contains("\":")) return "json"
        if (Regex("^\\s*[\\w.-]+:\\s", RegexOption.MULTILINE).containsMatchIn(text) && !text.contains("{")) return "yaml"
        if (text.contains("package ") && Regex("\\bfun\\b").containsMatchIn(text)) return "kotlin"
        if (Regex("\\bpublic\\s+(static\\s+)?(class|void|int|String)\\b").containsMatchIn(text)) return "java"
        if (Regex("^\\s*(def|class)\\s+\\w+.*:", RegexOption.MULTILINE).containsMatchIn(text)) return "python"
        if (Regex("\\b(import|export)\\b.*\\bfrom\\b|\\bconst\\b.*=>").containsMatchIn(text)) return "javascript"
        if (text.contains("#include")) return if (Regex("\\bstd::|\\bclass\\b|\\btemplate\\b").containsMatchIn(text)) "cpp" else "c"
        if (Regex("\\busing System\\s*;|\\bnamespace\\b.*\\{").containsMatchIn(text)) return "csharp"
        if (Regex("\\bpackage\\s+\\w+").containsMatchIn(text) && Regex("\\bfunc\\b").containsMatchIn(text)) return "go"
        if (Regex("^\\s*#\\s*\\w+", RegexOption.MULTILINE).containsMatchIn(text)) return "bash"
        if (Regex("\\bdef\\b.*\\bend\\b", RegexOption.DOT_MATCHES_ALL).containsMatchIn(text)) return "ruby"
        if (Regex("\\bfunction\\b|\\$\\w+").containsMatchIn(text)) return "php"
        if (Regex("\\b(always|assign|module)\\b").containsMatchIn(text)) return "verilog"
        if (Regex("\\b(library|entity|architecture)\\b").containsMatchIn(text)) return "vhdl"
        if (Regex("\\b(program|procedure|begin)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) return "pascal"
        return null
    }
}
