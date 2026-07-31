const assert = require("node:assert/strict");
const { execFileSync, spawnSync } = require("node:child_process");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");

const {
  findViolations,
  hashNormalizedSpecContent,
  normalizeSpecContent,
  parseMode,
  parseSpecManifest,
  splitNullSeparated,
} = require("./check-project-guardrails");

function writeRepositoryFile(repository, relativePath, content) {
  const absolutePath = path.join(repository, ...relativePath.split("/"));
  fs.mkdirSync(path.dirname(absolutePath), { recursive: true });
  fs.writeFileSync(absolutePath, content, "utf8");
}

function createSpecManifest(fileContents) {
  return `${JSON.stringify(
    {
      version: 1,
      algorithm: "sha256",
      normalization: "crlf-to-lf",
      files: Object.entries(fileContents)
        .sort(([left], [right]) => left.localeCompare(right))
        .map(([file, content]) => ({
          path: file,
          sha256: hashNormalizedSpecContent(content),
        })),
    },
    null,
    2,
  )}\n`;
}

function writeSpecFixture(
  repository,
  fileContents = {
    "docs/specs/API_SPEC.md": "# API contract\r\n\r\nProtected.\r\n",
    "docs/specs/REQUIREMENTS.md":
      "# Requirements\n\nProtected acceptance criteria.\n",
  },
) {
  for (const [file, content] of Object.entries(fileContents)) {
    writeRepositoryFile(repository, file, content);
  }
  writeRepositoryFile(
    repository,
    "docs/specs/SPEC_LOCK.json",
    createSpecManifest(fileContents),
  );
  return fileContents;
}

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

test("normalizes only CRLF before computing a stable SHA-256 digest", () => {
  assert.equal(normalizeSpecContent("first\r\nsecond\r\n"), "first\nsecond\n");
  assert.equal(
    hashNormalizedSpecContent("first\r\nsecond\r\n"),
    hashNormalizedSpecContent("first\nsecond\n"),
  );
  assert.notEqual(
    hashNormalizedSpecContent("first\rsecond"),
    hashNormalizedSpecContent("first\nsecond"),
  );
});

test("requires a strict sorted protected-spec manifest schema", () => {
  assert.match(parseSpecManifest("null").errors.join("\n"), /JSON object/);

  const validManifest = createSpecManifest({
    "docs/specs/API_SPEC.md": "api\n",
    "docs/specs/REQUIREMENTS.md": "requirements\n",
  });
  assert.deepEqual(parseSpecManifest(validManifest).errors, []);

  const invalidManifest = JSON.stringify({
    version: 1,
    algorithm: "sha256",
    normalization: "crlf-to-lf",
    files: [
      {
        path: "docs/specs/REQUIREMENTS.md",
        sha256: "a".repeat(64),
      },
      {
        path: "docs/specs/API_SPEC.md",
        sha256: "b".repeat(64),
      },
    ],
  });
  assert.match(
    parseSpecManifest(invalidManifest).errors.join("\n"),
    /sorted by path/,
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
    writeSpecFixture(temporaryRepository);
    fs.mkdirSync(sourceDirectory, { recursive: true });
    fs.writeFileSync(sourceFile, "import { ref } from 'vue';\n", "utf8");
    execFileSync("git", ["add", "frontend/src/example.js", "docs/specs"], {
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

test("fails clearly when the protected-spec manifest is absent", () => {
  const temporaryRepository = fs.mkdtempSync(
    path.join(os.tmpdir(), "gighub-spec-lock-missing-"),
  );
  const script = path.resolve(__dirname, "check-project-guardrails.js");

  try {
    execFileSync("git", ["init", "--quiet"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    writeRepositoryFile(
      temporaryRepository,
      "docs/specs/API_SPEC.md",
      "# Unlocked\n",
    );
    execFileSync("git", ["add", "docs/specs/API_SPEC.md"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });

    const workingResult = spawnSync(process.execPath, [script, "--all"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });
    const stagedResult = spawnSync(process.execPath, [script, "--staged"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });

    assert.equal(workingResult.status, 1);
    assert.match(workingResult.stderr, /manifest is missing/);
    assert.equal(stagedResult.status, 1);
    assert.match(stagedResult.stderr, /missing from the staged index/);
  } finally {
    fs.rmSync(temporaryRepository, { recursive: true, force: true });
  }
});

test("checks working tree and staged spec contents independently", () => {
  const temporaryRepository = fs.mkdtempSync(
    path.join(os.tmpdir(), "gighub-spec-lock-content-"),
  );
  const script = path.resolve(__dirname, "check-project-guardrails.js");
  const specPath = "docs/specs/API_SPEC.md";

  try {
    execFileSync("git", ["init", "--quiet"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    writeSpecFixture(temporaryRepository);
    execFileSync("git", ["add", "docs/specs"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });

    const initialWorking = spawnSync(process.execPath, [script, "--all"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });
    const initialStaged = spawnSync(process.execPath, [script, "--staged"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });
    assert.equal(initialWorking.status, 0);
    assert.equal(initialStaged.status, 0);

    writeRepositoryFile(
      temporaryRepository,
      specPath,
      "# API contract\n\nUnstaged divergence.\n",
    );

    const stagedStillLocked = spawnSync(
      process.execPath,
      [script, "--staged"],
      {
        cwd: temporaryRepository,
        encoding: "utf8",
      },
    );
    const workingMismatch = spawnSync(process.execPath, [script, "--all"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });

    assert.equal(stagedStillLocked.status, 0);
    assert.equal(workingMismatch.status, 1);
    assert.match(workingMismatch.stderr, /hash mismatch/);

    execFileSync("git", ["add", specPath], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    const stagedMismatch = spawnSync(process.execPath, [script, "--staged"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });
    assert.equal(stagedMismatch.status, 1);
    assert.match(stagedMismatch.stderr, /hash mismatch/);
  } finally {
    fs.rmSync(temporaryRepository, { recursive: true, force: true });
  }
});

test("rejects unlisted and deleted protected spec files", () => {
  const temporaryRepository = fs.mkdtempSync(
    path.join(os.tmpdir(), "gighub-spec-lock-shape-"),
  );
  const script = path.resolve(__dirname, "check-project-guardrails.js");
  const listedPath = "docs/specs/API_SPEC.md";
  const unlistedPath = "docs/specs/UNLISTED.md";

  try {
    execFileSync("git", ["init", "--quiet"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    writeSpecFixture(temporaryRepository);
    execFileSync("git", ["add", "docs/specs"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });

    writeRepositoryFile(temporaryRepository, unlistedPath, "# Unlisted\n");
    const unlistedWorking = spawnSync(process.execPath, [script, "--all"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });
    assert.equal(unlistedWorking.status, 1);
    assert.match(unlistedWorking.stderr, /not listed/);

    execFileSync("git", ["add", unlistedPath], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    const unlistedStaged = spawnSync(process.execPath, [script, "--staged"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });
    assert.equal(unlistedStaged.status, 1);
    assert.match(unlistedStaged.stderr, /not listed/);

    fs.rmSync(path.join(temporaryRepository, ...unlistedPath.split("/")));
    execFileSync("git", ["add", "-A", "docs/specs"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    fs.rmSync(path.join(temporaryRepository, ...listedPath.split("/")));

    const deletedWorking = spawnSync(process.execPath, [script, "--all"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });
    assert.equal(deletedWorking.status, 1);
    assert.match(deletedWorking.stderr, /missing or deleted/);

    execFileSync("git", ["add", "-A", "docs/specs"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    const deletedStaged = spawnSync(process.execPath, [script, "--staged"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });
    assert.equal(deletedStaged.status, 1);
    assert.match(deletedStaged.stderr, /missing or deleted/);
  } finally {
    fs.rmSync(temporaryRepository, { recursive: true, force: true });
  }
});
