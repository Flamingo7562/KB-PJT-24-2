const assert = require("node:assert/strict");
const { execFileSync, spawnSync } = require("node:child_process");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");

const {
  findViolations,
  parseMode,
  splitNullSeparated,
} = require("./check-project-guardrails");

test("parses explicit staged and all modes", () => {
  assert.equal(parseMode(["--staged"]), "staged");
  assert.equal(parseMode(["--all"]), "all");
  assert.throws(() => parseMode([]), /Use one mode/);
});

test("splits NUL-delimited Git output without losing unusual paths", () => {
  assert.deepEqual(
    splitNullSeparated("frontend/src/한글 파일.js\0backend/line\nbreak.java\0"),
    ["frontend/src/한글 파일.js", "backend/line\nbreak.java"],
  );
});

test("detects forbidden frontend dependencies without flagging Vue reactivity", () => {
  const violations = findViolations([
    {
      file: "frontend/src/invalid.js",
      content: "import library from 'react';",
    },
    {
      file: "frontend/src/valid.js",
      content: "import { reactive } from 'vue';",
    },
  ]);

  assert.deepEqual(
    violations.map(({ file, rule }) => `${file}:${rule.name}`),
    ["frontend/src/invalid.js:React dependency"],
  );
});

test("detects forbidden backend frameworks and persistence APIs", () => {
  const violations = findViolations([
    {
      file: "backend/build.gradle",
      content:
        "implementation 'org.springframework.boot:spring-boot-starter-web'",
    },
    {
      file: "backend/src/main/java/InvalidEntity.java",
      content: "import jakarta.persistence.Entity;",
    },
    {
      file: "backend/settings.gradle",
      content: "pluginManagement { id 'org.springframework.boot' }",
    },
    {
      file: "backend/gradle/libs.versions.toml",
      content: 'jpa = { module = "org.springframework.data:spring-data-jpa" }',
    },
  ]);

  assert.deepEqual(
    violations.map(({ rule }) => rule.name),
    ["Spring Boot", "JPA", "Spring Boot", "JPA"],
  );
});

test("allows forbidden technology names in documentation and hook files", () => {
  const entries = [
    { file: "docs/STACK.md", content: "React, Spring Boot, JPA" },
    { file: ".github/ISSUE_TEMPLATE/task.yml", content: "React" },
    { file: ".husky/pre-commit", content: "Spring Boot" },
  ];

  assert.deepEqual(findViolations(entries), []);
});

test("staged mode reads index content while all mode reads the working tree", () => {
  const temporaryRepository = fs.mkdtempSync(
    path.join(os.tmpdir(), "gighub-guardrails-"),
  );
  const script = path.resolve(__dirname, "check-project-guardrails.js");
  const sourceDirectory = path.join(temporaryRepository, "frontend", "src");
  const sourceFile = path.join(sourceDirectory, "example.js");

  try {
    execFileSync("git", ["init", "--quiet"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    fs.mkdirSync(sourceDirectory, { recursive: true });
    fs.writeFileSync(sourceFile, "import { ref } from 'vue';\n", "utf8");
    execFileSync("git", ["add", "frontend/src/example.js"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });

    fs.writeFileSync(sourceFile, "import library from 'react';\n", "utf8");

    const stagedSafe = spawnSync(process.execPath, [script, "--staged"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });
    const workingTreeViolation = spawnSync(
      process.execPath,
      [script, "--all"],
      {
        cwd: temporaryRepository,
        encoding: "utf8",
      },
    );

    assert.equal(stagedSafe.status, 0);
    assert.equal(workingTreeViolation.status, 1);

    execFileSync("git", ["add", "frontend/src/example.js"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    fs.writeFileSync(sourceFile, "import { ref } from 'vue';\n", "utf8");

    const stagedViolation = spawnSync(process.execPath, [script, "--staged"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });

    assert.equal(stagedViolation.status, 1);
    assert.match(stagedViolation.stderr, /React dependency/);
  } finally {
    fs.rmSync(temporaryRepository, { recursive: true, force: true });
  }
});
