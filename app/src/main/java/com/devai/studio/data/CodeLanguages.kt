package com.devai.studio.data

import android.content.Context
import org.json.JSONObject

data class CodeLanguage(
    val id: String,
    val name: String,
    val short: String,
    val extension: String,
    val comment: String,
    val testFramework: String,
    val description: String,
    val example: String,
    val order: Int = 0,
)

object CodeLanguages {
    const val DEFAULT_ID = "kotlin"

    private val builtIn: List<CodeLanguage> = listOf(
        CodeLanguage(
            id = "kotlin",
            name = "Kotlin",
            short = "Kotlin",
            extension = "kt",
            comment = "//",
            testFramework = "JUnit 5",
            description = "JVM, Android, Jetpack Compose",
            example = "fun main() {\n    println(\"Hello, DevAI!\")\n}",
        ),
        CodeLanguage(
            id = "java",
            name = "Java",
            short = "Java",
            extension = "java",
            comment = "//",
            testFramework = "JUnit 5",
            description = "JVM, Spring, Android",
            example = "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, DevAI!\");\n    }\n}",
        ),
        CodeLanguage(
            id = "python",
            name = "Python",
            short = "Python",
            extension = "py",
            comment = "#",
            testFramework = "pytest",
            description = "Скрипты, бэкенд, ML и автоматизация",
            example = "def greet(name: str) -> str:\n    return f\"Hello, {name}!\"\n\nprint(greet(\"DevAI\"))",
        ),
        CodeLanguage(
            id = "javascript",
            name = "JavaScript",
            short = "JS",
            extension = "js",
            comment = "//",
            testFramework = "Jest",
            description = "Веб, Node.js и фронтенд",
            example = "function greet(name) {\n  return `Hello, \${name}!`;\n}\nconsole.log(greet(\"DevAI\"));",
        ),
        CodeLanguage(
            id = "typescript",
            name = "TypeScript",
            short = "TS",
            extension = "ts",
            comment = "//",
            testFramework = "Vitest",
            description = "Типизированный JavaScript",
            example = "function greet(name: string): string {\n  return `Hello, \${name}!`;\n}\nconsole.log(greet(\"DevAI\"));",
        ),
        CodeLanguage(
            id = "dart",
            name = "Dart",
            short = "Dart",
            extension = "dart",
            comment = "//",
            testFramework = "package:test",
            description = "Flutter и мобильные приложения",
            example = "void main() {\n  print('Hello, DevAI!');\n}",
        ),
        CodeLanguage(
            id = "swift",
            name = "Swift",
            short = "Swift",
            extension = "swift",
            comment = "//",
            testFramework = "XCTest",
            description = "iOS, macOS и SwiftUI",
            example = "let name = \"DevAI\"\nprint(\"Hello, \\(name)!\")",
        ),
        CodeLanguage(
            id = "objectivec",
            name = "Objective-C",
            short = "Obj-C",
            extension = "m",
            comment = "//",
            testFramework = "XCTest",
            description = "Старые iOS/macOS проекты",
            example = "#import <Foundation/Foundation.h>\n\nint main() {\n    @autoreleasepool {\n        NSLog(@\"Hello, DevAI!\");\n    }\n    return 0;\n}",
        ),
        CodeLanguage(
            id = "c",
            name = "C",
            short = "C",
            extension = "c",
            comment = "//",
            testFramework = "Unity / CMocka",
            description = "Системное ПО и встраиваемые системы",
            example = "#include <stdio.h>\n\nint main(void) {\n    printf(\"Hello, DevAI!\\n\");\n    return 0;\n}",
        ),
        CodeLanguage(
            id = "cpp",
            name = "C++",
            short = "C++",
            extension = "cpp",
            comment = "//",
            testFramework = "GoogleTest",
            description = "Игры, highload и системное ПО",
            example = "#include <iostream>\n\nint main() {\n    std::cout << \"Hello, DevAI!\" << std::endl;\n    return 0;\n}",
        ),
        CodeLanguage(
            id = "csharp",
            name = "C#",
            short = "C#",
            extension = "cs",
            comment = "//",
            testFramework = "xUnit",
            description = ".NET, Unity и Windows",
            example = "using System;\n\nclass Program {\n    static void Main() {\n        Console.WriteLine(\"Hello, DevAI!\");\n    }\n}",
        ),
        CodeLanguage(
            id = "go",
            name = "Go",
            short = "Go",
            extension = "go",
            comment = "//",
            testFramework = "testing",
            description = "Бэкенд, CLI и микросервисы",
            example = "package main\n\nimport \"fmt\"\n\nfunc main() {\n    fmt.Println(\"Hello, DevAI!\")\n}",
        ),
        CodeLanguage(
            id = "php",
            name = "PHP",
            short = "PHP",
            extension = "php",
            comment = "//",
            testFramework = "PHPUnit",
            description = "Веб-бэкенд и WordPress",
            example = "<?php\n\$name = \"DevAI\";\necho \"Hello, \$name!\";",
        ),
        CodeLanguage(
            id = "ruby",
            name = "Ruby",
            short = "Ruby",
            extension = "rb",
            comment = "#",
            testFramework = "RSpec",
            description = "Веб, Rails и скрипты",
            example = "name = \"DevAI\"\nputs \"Hello, #{name}!\" ",
        ),
        CodeLanguage(
            id = "scala",
            name = "Scala",
            short = "Scala",
            extension = "scala",
            comment = "//",
            testFramework = "ScalaTest",
            description = "JVM, данные и функциональный код",
            example = "object Main extends App {\n  println(\"Hello, DevAI!\")\n}",
        ),
        CodeLanguage(
            id = "groovy",
            name = "Groovy",
            short = "Groovy",
            extension = "groovy",
            comment = "//",
            testFramework = "Spock",
            description = "JVM-скрипты, Gradle и Jenkins",
            example = "println \"Hello, DevAI!\"",
        ),
        CodeLanguage(
            id = "bash",
            name = "Bash",
            short = "Bash",
            extension = "sh",
            comment = "#",
            testFramework = "Bats",
            description = "Linux, CI и автоматизация",
            example = "#!/usr/bin/env bash\nname=\"DevAI\"\necho \"Hello, \$name!\"",
        ),
        CodeLanguage(
            id = "powershell",
            name = "PowerShell",
            short = "PS",
            extension = "ps1",
            comment = "#",
            testFramework = "Pester",
            description = "Windows и автоматизация",
            example = "\$name = \"DevAI\"\nWrite-Host \"Hello, \$name!\"",
        ),
        CodeLanguage(
            id = "sql",
            name = "SQL",
            short = "SQL",
            extension = "sql",
            comment = "--",
            testFramework = "pgTAP",
            description = "Запросы, аналитика и базы данных",
            example = "SELECT 'Hello, DevAI!' AS greeting;",
        ),
        CodeLanguage(
            id = "html",
            name = "HTML",
            short = "HTML",
            extension = "html",
            comment = "<!--",
            testFramework = "Playwright",
            description = "Разметка веб-страниц",
            example = "<!doctype html>\n<html lang=\"ru\">\n  <body>\n    <h1>Hello, DevAI!</h1>\n  </body>\n</html>",
        ),
        CodeLanguage(
            id = "css",
            name = "CSS",
            short = "CSS",
            extension = "css",
            comment = "/*",
            testFramework = "Playwright",
            description = "Стили и адаптивная вёрстка",
            example = "body {\n  font-family: system-ui, sans-serif;\n  background: #0f172a;\n  color: #e2e8f0;\n}",
        ),
        CodeLanguage(
            id = "json",
            name = "JSON",
            short = "JSON",
            extension = "json",
            comment = "//",
            testFramework = "JSON Schema",
            description = "Конфиги и обмен данными",
            example = "{\n  \"app\": \"DevAI Studio\",\n  \"language\": \"JSON\"\n}",
        ),
        CodeLanguage(
            id = "yaml",
            name = "YAML",
            short = "YAML",
            extension = "yaml",
            comment = "#",
            testFramework = "yamllint",
            description = "Конфиги, CI и Kubernetes",
            example = "app: DevAI Studio\nlanguage: YAML\nfeatures:\n  - chat\n  - tools",
        ),
        CodeLanguage(
            id = "xml",
            name = "XML",
            short = "XML",
            extension = "xml",
            comment = "<!--",
            testFramework = "xmllint",
            description = "Разметка, Android и конфиги",
            example = "<app>\n  <name>DevAI Studio</name>\n  <language>XML</language>\n</app>",
        ),
        CodeLanguage(
            id = "lua",
            name = "Lua",
            short = "Lua",
            extension = "lua",
            comment = "--",
            testFramework = "busted",
            description = "Игры, Neovim и встраиваемые скрипты",
            example = "local name = \"DevAI\"\nprint(\"Hello, \" .. name .. \"!\")",
        ),
        CodeLanguage(
            id = "perl",
            name = "Perl",
            short = "Perl",
            extension = "pl",
            comment = "#",
            testFramework = "Test::More",
            description = "Тексты, логи и системные скрипты",
            example = "use strict;\nuse warnings;\nmy \$name = \"DevAI\";\nprint \"Hello, \$name!\\n\";",
        ),
        CodeLanguage(
            id = "r",
            name = "R",
            short = "R",
            extension = "r",
            comment = "#",
            testFramework = "testthat",
            description = "Статистика и анализ данных",
            example = "name <- \"DevAI\"\ncat(\"Hello,\", name, \"!\\n\")",
        ),
        CodeLanguage(
            id = "matlab",
            name = "MATLAB",
            short = "MATLAB",
            extension = "m",
            comment = "%",
            testFramework = "matlab.unittest",
            description = "Инженерия и научные расчёты",
            example = "name = 'DevAI';\nfprintf('Hello, %s!\\n', name);",
        ),
        CodeLanguage(
            id = "julia",
            name = "Julia",
            short = "Julia",
            extension = "jl",
            comment = "#",
            testFramework = "Test",
            description = "Научные вычисления и ML",
            example = "name = \"DevAI\"\nprintln(\"Hello, \$name!\")",
        ),
        CodeLanguage(
            id = "haskell",
            name = "Haskell",
            short = "Haskell",
            extension = "hs",
            comment = "--",
            testFramework = "Hspec",
            description = "Функциональное программирование",
            example = "main :: IO ()\nmain = putStrLn \"Hello, DevAI!\"",
        ),
        CodeLanguage(
            id = "elixir",
            name = "Elixir",
            short = "Elixir",
            extension = "ex",
            comment = "#",
            testFramework = "ExUnit",
            description = "Отказоустойчивые бэкенды",
            example = "defmodule Main do\n  def hello(name), do: \"Hello, #{name}!\"\nend\n\nIO.puts(Main.hello(\"DevAI\"))",
        ),
        CodeLanguage(
            id = "erlang",
            name = "Erlang",
            short = "Erlang",
            extension = "erl",
            comment = "%",
            testFramework = "EUnit",
            description = "Телеком и распределённые системы",
            example = "-module(main).\n-export([start/0]).\n\nstart() ->\n    io:format(\"Hello, DevAI!~n\").",
        ),
        CodeLanguage(
            id = "clojure",
            name = "Clojure",
            short = "Clojure",
            extension = "clj",
            comment = ";",
            testFramework = "clojure.test",
            description = "JVM и функциональный код",
            example = "(println \"Hello, DevAI!\")",
        ),
        CodeLanguage(
            id = "fsharp",
            name = "F#",
            short = "F#",
            extension = "fs",
            comment = "//",
            testFramework = "xUnit",
            description = ".NET и функциональный код",
            example = "printfn \"Hello, DevAI!\"",
        ),
        CodeLanguage(
            id = "vbnet",
            name = "Visual Basic .NET",
            short = "VB.NET",
            extension = "vb",
            comment = "'",
            testFramework = "xUnit",
            description = ".NET и Windows",
            example = "Module Program\n    Sub Main()\n        Console.WriteLine(\"Hello, DevAI!\")\n    End Sub\nEnd Module",
        ),
        CodeLanguage(
            id = "assembly",
            name = "Assembly",
            short = "ASM",
            extension = "asm",
            comment = ";",
            testFramework = "—",
            description = "Низкоуровневый код и реверс",
            example = "global _start\n\nsection .text\n_start:\n    mov rax, 1\n    mov rdi, 1\n    lea rsi, [rel msg]\n    mov rdx, 12\n    syscall\n    mov rax, 60\n    xor rdi, rdi\n    syscall\n\nsection .rodata\nmsg: db \"Hello, DevAI\", 10",
        ),
        CodeLanguage(
            id = "solidity",
            name = "Solidity",
            short = "Solidity",
            extension = "sol",
            comment = "//",
            testFramework = "Foundry",
            description = "Смарт-контракты EVM",
            example = "// SPDX-License-Identifier: MIT\npragma solidity ^0.8.20;\n\ncontract Greeter {\n    function greet() external pure returns (string memory) {\n        return \"Hello, DevAI!\";\n    }\n}",
        ),
        CodeLanguage(
            id = "zig",
            name = "Zig",
            short = "Zig",
            extension = "zig",
            comment = "//",
            testFramework = "zig test",
            description = "Системное программирование",
            example = "const std = @import(\"std\");\n\npub fn main() !void {\n    std.debug.print(\"Hello, DevAI!\\n\", .{});\n}",
        ),
        CodeLanguage(
            id = "nim",
            name = "Nim",
            short = "Nim",
            extension = "nim",
            comment = "#",
            testFramework = "unittest",
            description = "Системное ПО и скрипты",
            example = "echo \"Hello, DevAI!\"",
        ),
        CodeLanguage(
            id = "crystal",
            name = "Crystal",
            short = "Crystal",
            extension = "cr",
            comment = "#",
            testFramework = "spec",
            description = "Ruby-подобный компилируемый язык",
            example = "puts \"Hello, DevAI!\"",
        ),
        CodeLanguage(
            id = "d",
            name = "D",
            short = "D",
            extension = "d",
            comment = "//",
            testFramework = "unittest",
            description = "Системное программирование",
            example = "import std.stdio;\n\nvoid main() {\n    writeln(\"Hello, DevAI!\");\n}",
        ),
        CodeLanguage(
            id = "fortran",
            name = "Fortran",
            short = "Fortran",
            extension = "f90",
            comment = "!",
            testFramework = "pFUnit",
            description = "Научные и инженерные расчёты",
            example = "program hello\n    print *, \"Hello, DevAI!\"\nend program hello",
        ),
        CodeLanguage(
            id = "cobol",
            name = "COBOL",
            short = "COBOL",
            extension = "cob",
            comment = "*>",
            testFramework = "GnuCOBOL",
            description = "Банковские и legacy-системы",
            example = "       IDENTIFICATION DIVISION.\n       PROGRAM-ID. HELLO.\n       PROCEDURE DIVISION.\n           DISPLAY \"Hello, DevAI!\".\n           STOP RUN.",
        ),
        CodeLanguage(
            id = "prolog",
            name = "Prolog",
            short = "Prolog",
            extension = "pl",
            comment = "%",
            testFramework = "PLUnit",
            description = "Логика и экспертные системы",
            example = "greet(Name) :- format(\"Hello, ~w!~n\", [Name]).\n:- greet(\"DevAI\").",
        ),
        CodeLanguage(
            id = "scheme",
            name = "Scheme",
            short = "Scheme",
            extension = "scm",
            comment = ";",
            testFramework = "SRFI-64",
            description = "Lisp и функциональное программирование",
            example = "(display \"Hello, DevAI!\")\n(newline)",
        ),
        CodeLanguage(
            id = "ocaml",
            name = "OCaml",
            short = "OCaml",
            extension = "ml",
            comment = "(*",
            testFramework = "Alcotest",
            description = "Функциональный код и компиляторы",
            example = "print_endline \"Hello, DevAI!\"",
        ),
        CodeLanguage(
            id = "pascal",
            name = "Pascal",
            short = "Pascal",
            extension = "pas",
            comment = "//",
            testFramework = "FPCUnit",
            description = "Delphi и legacy-проекты",
            example = "program Hello;\nbegin\n  writeln('Hello, DevAI!');\nend.",
        ),
        CodeLanguage(
            id = "ada",
            name = "Ada",
            short = "Ada",
            extension = "adb",
            comment = "--",
            testFramework = "AUnit",
            description = "Надёжные встраиваемые системы",
            example = "with Ada.Text_IO;\n\nprocedure Hello is\nbegin\n   Ada.Text_IO.Put_Line(\"Hello, DevAI!\");\nend Hello;",
        ),
        CodeLanguage(
            id = "vhdl",
            name = "VHDL",
            short = "VHDL",
            extension = "vhd",
            comment = "--",
            testFramework = "GHDL",
            description = "ПЛИС и цифровая логика",
            example = "entity hello is end entity;\n\narchitecture rtl of hello is\nbegin\n  -- report \"Hello, DevAI!\" severity note;\nend architecture;",
        ),
        CodeLanguage(
            id = "verilog",
            name = "Verilog",
            short = "Verilog",
            extension = "v",
            comment = "//",
            testFramework = "cocotb",
            description = "ПЛИС и цифровая логика",
            example = "module hello;\n  initial begin\n    \$display(\"Hello, DevAI!\");\n  end\nendmodule",
        ),
    )

    /** Каталог языков: по умолчанию встроенный, после [load] - из JSON-файлов assets/languages. */
    var all: List<CodeLanguage> = builtIn
        private set

    /** Читает языки из файлов сборки. Если файлов нет, остаётся встроенный список. */
    fun load(ctx: Context) {
        val loaded = runCatching {
            ctx.assets.list("languages").orEmpty()
                .filter { it.endsWith(".json") }
                .mapNotNull { file ->
                    runCatching {
                        ctx.assets.open("languages/$file").use { input ->
                            val json = JSONObject(input.bufferedReader().readText())
                            val id = json.optString("id").trim()
                            if (id.isBlank()) return@use null
                            CodeLanguage(
                                id = id,
                                name = json.optString("name", id),
                                short = json.optString("short", id),
                                extension = json.optString("extension"),
                                comment = json.optString("comment", "//"),
                                testFramework = json.optString("testFramework", "—"),
                                description = json.optString("description"),
                                example = json.optString("example"),
                                order = json.optInt("order", 0),
                            )
                        }
                    }.getOrNull()
                }
                .sortedWith(compareBy({ it.order }, { it.name }))
        }.getOrNull()
        if (!loaded.isNullOrEmpty()) all = loaded
    }

    val default: CodeLanguage
        get() = all.firstOrNull { it.id == DEFAULT_ID } ?: all.first()

    private val aliases: Map<String, String> = mapOf(
        "js" to "javascript", "jsx" to "javascript", "mjs" to "javascript",
        "ts" to "typescript", "tsx" to "typescript",
        "py" to "python", "python3" to "python",
        "kt" to "kotlin", "kts" to "kotlin",
        "c++" to "cpp", "cc" to "cpp", "cxx" to "cpp", "hpp" to "cpp",
        "c#" to "csharp", "cs" to "csharp",
        "objc" to "objectivec", "objective-c" to "objectivec",
        "sh" to "bash", "shell" to "bash", "zsh" to "bash",
        "pwsh" to "powershell", "ps1" to "powershell",
        "yml" to "yaml", "rb" to "ruby",
        "fs" to "fsharp", "fsx" to "fsharp",
        "vb" to "vbnet", "vb.net" to "vbnet",
        "asm" to "assembly", "nasm" to "assembly",
        "sol" to "solidity", "pl" to "perl", "pm" to "perl",
        "hs" to "haskell", "lhs" to "haskell",
        "ex" to "elixir", "exs" to "elixir",
        "erl" to "erlang", "hrl" to "erlang",
        "clj" to "clojure", "cljs" to "clojure",
        "ml" to "ocaml", "mli" to "ocaml",
        "scm" to "scheme", "lisp" to "scheme",
        "pas" to "pascal", "pp" to "pascal",
        "vhd" to "vhdl", "vhdl" to "vhdl",
        "v" to "verilog", "sv" to "verilog",
        "f90" to "fortran", "f95" to "fortran",
        "cob" to "cobol", "cbl" to "cobol",
        "text" to "", "plaintext" to "", "txt" to "",
        "hx" to "haxe", "bal" to "ballerina", "rkt" to "racket",
        "mk" to "makefile", "docker" to "dockerfile", "tex" to "latex",
        "gd" to "gdscript", "vy" to "vyper", "purs" to "purescript",
        "re" to "reasonml", "res" to "rescript",
    )

    val supportedExtensions: Set<String>
        get() = buildSet {
        all.forEach { add(it.extension.lowercase()) }
        addAll(listOf("kts", "jsx", "mjs", "tsx", "cc", "cxx", "hpp", "h", "hh", "cs", "fsx", "vb", "asm", "s", "sol", "pl", "pm", "hs", "lhs", "exs", "erl", "hrl", "clj", "cljs", "ml", "mli", "scm", "lisp", "pas", "pp", "vhd", "v", "sv", "f90", "f95", "cob", "cbl", "yml", "zsh", "hx", "vala", "bal", "rkt", "tcl", "awk", "mk", "dockerfile", "cmake", "tex", "glsl", "hlsl", "wgsl", "gd", "qml", "vy", "move", "cairo", "elm", "purs", "re", "res", "vue", "svelte", "astro"))
    }

    fun byId(id: String?): CodeLanguage? {
        val key = id?.trim()?.lowercase()?.removePrefix("language-") ?: return null
        if (key.isBlank()) return null
        return all.firstOrNull { it.id == key }
            ?: all.firstOrNull { aliases[key] == it.id }
            ?: all.firstOrNull { it.name.equals(key, ignoreCase = true) || it.short.equals(key, ignoreCase = true) }
    }

    fun fromFileName(name: String): CodeLanguage? {
        when (name.lowercase()) {
            "dockerfile" -> return byId("dockerfile")
            "makefile" -> return byId("makefile")
            "cmakelists.txt" -> return byId("cmake")
        }
        val extension = name.substringAfterLast('.', "").lowercase()
        if (extension.isBlank()) return null
        return all.firstOrNull { it.extension.lowercase() == extension }
            ?: all.firstOrNull { aliases[extension] == it.id }
    }

    fun selected(ctx: Context): CodeLanguage =
        byId(AppPrefs.codeLanguage(ctx)) ?: default

    fun instruction(language: CodeLanguage): String =
        "Основной язык программирования: ${language.name}. " +
            "Пиши код, тесты и команды на ${language.name}, если пользователь явно не попросил другой язык. " +
            "Расширение файлов: ${language.extension}. Тестовый фреймворк: ${language.testFramework}."
}
