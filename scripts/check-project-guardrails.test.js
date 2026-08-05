const assert = require("node:assert/strict");
const { execFileSync, spawnSync } = require("node:child_process");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");

const {
  extractReadmeReleaseRows,
  extractSpecReleaseVersion,
  findViolations,
  hashNormalizedSpecContent,
  normalizeSpecContent,
  parsePatchDocument,
  parseMode,
  parseSpecManifest,
  splitNullSeparated,
  verifyPatchSnapshot,
  verifySpecReleaseMetadata,
} = require("./check-project-guardrails");

const PATCH_SCAFFOLD = {
  "docs/spec-patches/README.md": "# Specification Patch governance\n",
  "docs/spec-patches/TEMPLATE.md": "# Specification Patch template\n",
  "docs/spec-patches/proposed/.gitkeep": "",
  "docs/spec-patches/archive/.gitkeep": "",
};

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
    "docs/specs/README.md": [
      "# Product specification",
      "",
      "| Item | Value |",
      "| --- | --- |",
      "| Release | `3.0.0` |",
      "",
      "## Release history",
      "",
      "| Version | Date |",
      "| --- | --- |",
      "| `3.0.0` | 2026-08-05 |",
      "",
    ].join("\n"),
    "docs/specs/API_SPEC.md":
      "# API contract\r\n\r\n| Release | `3.0.0` |\r\n\r\nProtected.\r\n",
    "docs/specs/DECISIONS.md":
      "# Decisions\n\n| Release | `3.0.0` |\n\nProtected.\n",
    "docs/specs/REQUIREMENTS.md":
      "# Requirements\n\n| Release | `3.0.0` |\n\nProtected acceptance criteria.\n",
    "docs/specs/SPEC_TRACEABILITY.md":
      "# Traceability\n\n| Release | `3.0.0` |\n\nProtected.\n",
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

function formatPatchScalar(value) {
  if (value === null) return "null";
  if (typeof value === "number") return String(value);
  return JSON.stringify(value);
}

function createPatchDocument(overrides = {}, options = {}) {
  const metadata = {
    patch_id: "SPEC-205-01",
    author: "flamingo7562",
    status: "proposed",
    issue: 205,
    created_at: "2026-08-05",
    base_spec_version: "3.0.0",
    base_commit: "a".repeat(40),
    change_type: "additive",
    targets: [{ requirement: "WALLET-003" }],
    depends_on: [],
    supersedes: null,
    superseded_by: null,
    applied_in_version: null,
    applied_by_pr: null,
    ...overrides,
  };
  const omitted = new Set(options.omit ?? []);
  const lines = ["---"];

  for (const [key, value] of Object.entries(metadata)) {
    if (omitted.has(key)) continue;
    if (key === "targets") {
      lines.push("targets:");
      for (const target of value) {
        const [type, targetValue] = Object.entries(target)[0];
        lines.push(`  - ${type}: ${formatPatchScalar(targetValue)}`);
      }
    } else if (key === "depends_on") {
      if (value.length === 0) {
        lines.push("depends_on: []");
      } else {
        lines.push("depends_on:");
        for (const dependency of value) {
          lines.push(`  - ${formatPatchScalar(dependency)}`);
        }
      }
    } else {
      lines.push(`${key}: ${formatPatchScalar(value)}`);
    }
  }

  lines.push(
    "---",
    "",
    "## 변경 요약과 필요성",
    "",
    "지갑 계약의 명세 추적 조건을 명확히 기록한다.",
    "",
    "## 현재 명세와 문제",
    "",
    "현재 계약은 추적 대상과 검증 조건을 함께 확인해야 한다.",
    "",
    "## 제안할 최종 규범 문장 또는 Before/After",
    "",
    "지갑 계약의 변경은 관련 요구사항과 검증 조건을 함께 추적한다.",
    "",
    "## 영향 분석",
    "",
    "### 요구사항",
    "영향을 기록한다.",
    "",
    "### API",
    "영향 없음.",
    "",
    "### 데이터 및 Migration",
    "영향 없음.",
    "",
    "### 보안",
    "영향 없음.",
    "",
    "### Frontend",
    "영향 없음.",
    "",
    "### Backend",
    "영향 없음.",
    "",
    "### 테스트",
    "수용 조건을 검증한다.",
    "",
    "## 검증 가능한 수용 조건",
    "",
    "- 지갑 계약과 검증 조건의 연결을 확인할 수 있다.",
    "",
    "## 미결 사항",
    "",
    options.openQuestions ?? "- 없음",
    "",
    "## 관련 Issue·PR·의존 Patch",
    "",
    "- Issue #205",
    "",
  );

  if (options.bodySuffix) lines.push(options.bodySuffix);
  return lines.join("\n");
}

function createPatchSnapshot(documents = {}) {
  return new Map(Object.entries({ ...PATCH_SCAFFOLD, ...documents }));
}

function patchPath(summary, directory = "proposed", revision = 1) {
  return `docs/spec-patches/${directory}/flamingo7562_issue-205_${summary}_patch_v${revision}.md`;
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

test("requires canonical spec Markdown release metadata to stay aligned", () => {
  const files = new Map(
    Object.entries({
      "docs/specs/README.md": [
        "# Spec",
        "",
        "Unrelated version `9.9.9` must not become release metadata.",
        "",
        "| Release | `3.0.1` |",
        "",
        "## Release history",
        "",
        "| Version | Date |",
        "| --- | --- |",
        "| `3.0.1` | today |",
      ].join("\n"),
      "docs/specs/REQUIREMENTS.md": "# Requirements\n\n| Release | `3.0.1` |\n",
      "docs/specs/API_SPEC.md": "# API\n\n| Release | `3.0.1` |\n",
      "docs/specs/DECISIONS.md": "# Decisions\n\n| Release | `3.0.1` |\n",
      "docs/specs/SPEC_TRACEABILITY.md":
        "# Traceability\n\n| Release | `3.0.1` |\n",
    }),
  );

  assert.equal(
    extractSpecReleaseVersion(files.get("docs/specs/README.md")),
    "3.0.1",
  );
  assert.deepEqual(
    extractReadmeReleaseRows(files.get("docs/specs/README.md")),
    ["3.0.1"],
  );
  assert.deepEqual(verifySpecReleaseMetadata(files), []);

  assert.equal(
    extractSpecReleaseVersion("# API\n\n## 본문\n\n| Release | `3.0.1` |\n"),
    null,
  );

  files.set("docs/specs/API_SPEC.md", "# API\n\n| Release | `3.0.0` |\n");
  assert.match(
    verifySpecReleaseMetadata(files).join("\n"),
    /release versions must match/,
  );

  files.set("docs/specs/API_SPEC.md", "# API\n\n| Release | `3.0.1` |\n");
  files.set(
    "docs/specs/README.md",
    "# Spec\n\n| Release | `3.0.1` |\n\n## Release history\n\n| Version | Date |\n| --- | --- |\n| `3.0.0` | yesterday |\n",
  );
  assert.match(
    verifySpecReleaseMetadata(files).join("\n"),
    /latest release row \(3\.0\.0\).*header \(3\.0\.1\)/,
  );
});

test("validates Patch paths, metadata, enums, and duplicate IDs", () => {
  const validPath = patchPath("wallet-contract");
  assert.deepEqual(
    parsePatchDocument(validPath, createPatchDocument()).errors,
    [],
  );

  assert.match(
    parsePatchDocument(
      "docs/spec-patches/proposed/INVALID.md",
      createPatchDocument(),
    ).errors.join("\n"),
    /must match/,
  );

  const invalidMetadata = createPatchDocument(
    { status: "reviewing", change_type: "rewrite" },
    { omit: ["base_spec_version", "base_commit"] },
  );
  const metadataErrors = parsePatchDocument(
    validPath,
    invalidMetadata,
  ).errors.join("\n");
  assert.match(metadataErrors, /missing required metadata: base_spec_version/);
  assert.match(metadataErrors, /missing required metadata: base_commit/);
  assert.match(metadataErrors, /status must be one of/);
  assert.match(metadataErrors, /change_type must be one of/);

  const missingAppliedMetadata = parsePatchDocument(
    patchPath("wallet-contract", "archive"),
    createPatchDocument({ status: "applied" }),
  ).errors.join("\n");
  assert.match(missingAppliedMetadata, /requires applied_in_version SemVer/);
  assert.match(missingAppliedMetadata, /requires a positive applied_by_pr/);

  const secondPath = patchPath("wallet-contract-followup");
  const duplicateResult = verifyPatchSnapshot({
    changedFiles: new Set([validPath, secondPath]),
    currentFiles: createPatchSnapshot({
      [validPath]: createPatchDocument(),
      [secondPath]: createPatchDocument(),
    }),
    previousFiles: new Map(),
  });
  assert.match(duplicateResult.errors.join("\n"), /Patch ID is duplicated/);
});

test("blocks invalid Patch transitions and accepted or terminal rewrites", () => {
  const file = patchPath("wallet-contract");
  const previousProposed = createPatchSnapshot({
    [file]: createPatchDocument({ status: "proposed" }),
  });
  const currentDraft = createPatchSnapshot({
    [file]: createPatchDocument({ status: "draft" }),
  });
  const backwards = verifyPatchSnapshot({
    changedFiles: new Set([file]),
    currentFiles: currentDraft,
    previousFiles: previousProposed,
  });
  assert.match(
    backwards.errors.join("\n"),
    /disallows Patch status transition proposed -> draft/,
  );

  const acceptedDocument = createPatchDocument({ status: "accepted" });
  const acceptedRewrite = verifyPatchSnapshot({
    changedFiles: new Set([file]),
    currentFiles: createPatchSnapshot({
      [file]: `${acceptedDocument}\nEditorial rewrite.\n`,
    }),
    previousFiles: createPatchSnapshot({ [file]: acceptedDocument }),
  });
  assert.match(
    acceptedRewrite.errors.join("\n"),
    /accepted Patch is immutable/,
  );
});

test("requires the Patch governance scaffold even when every file is deleted", () => {
  const result = verifyPatchSnapshot({
    changedFiles: new Set(Object.keys(PATCH_SCAFFOLD)),
    currentFiles: new Map(),
    previousFiles: createPatchSnapshot(),
  });

  for (const file of Object.keys(PATCH_SCAFFOLD)) {
    assert.match(
      result.errors.join("\n"),
      new RegExp(file.replaceAll(".", "\\.")),
    );
  }
});

test("requires both sides of a superseded Patch relationship to exist", () => {
  const proposedPath = patchPath("wallet-contract");
  const archivePath = patchPath("wallet-contract", "archive");
  const result = verifyPatchSnapshot({
    changedFiles: new Set([proposedPath, archivePath]),
    currentFiles: createPatchSnapshot({
      [archivePath]: createPatchDocument({
        status: "superseded",
        superseded_by: "SPEC-205-02",
      }),
    }),
    previousFiles: createPatchSnapshot({
      [proposedPath]: createPatchDocument({ status: "accepted" }),
    }),
  });

  assert.match(
    result.errors.join("\n"),
    /superseded_by references missing Patch SPEC-205-02/,
  );
});

test("blocks placeholders and unresolved questions in accepted Patches", () => {
  const file = patchPath("wallet-contract");
  const document = createPatchDocument(
    { status: "accepted" },
    { bodySuffix: "TODO: replace <stable-id>.", openQuestions: "- 미정" },
  );
  const result = parsePatchDocument(file, document);

  assert.match(result.errors.join("\n"), /must not contain placeholders/);
  assert.match(result.errors.join("\n"), /must resolve "미결 사항"/);

  const templateSentinels = createPatchDocument({
    status: "accepted",
    base_spec_version: "0.0.0",
    base_commit: "0".repeat(40),
    targets: [{ requirement: "REQUIREMENT-ID" }],
  })
    .replace(
      "지갑 계약의 명세 추적 조건을 명확히 기록한다.",
      "최소 계약 변경을 제안한다.",
    )
    .replace(
      "- 지갑 계약과 검증 조건의 연결을 확인할 수 있다.",
      "- [ ] 수용 조건을 작성한다.",
    );
  assert.match(
    parsePatchDocument(file, templateSentinels).errors.join("\n"),
    /must replace every TEMPLATE sentinel/,
  );

  const templateHeading = createPatchDocument(
    { status: "accepted" },
    { bodySuffix: "# SPEC-000-01: 명세 Patch 제목" },
  );
  assert.match(
    parsePatchDocument(file, templateHeading).errors.join("\n"),
    /must replace every TEMPLATE sentinel/,
  );
});

test("blocks mixed Patch proposal scope but permits an atomic Controller release", () => {
  const proposedPath = patchPath("wallet-contract");
  const proposal = verifyPatchSnapshot({
    changedFiles: new Set([
      proposedPath,
      "frontend/src/wallet.js",
      "backend/src/main/resources/db/migration/V1__wallet.sql",
      "docs/specs/REQUIREMENTS.md",
    ]),
    currentFiles: createPatchSnapshot({
      [proposedPath]: createPatchDocument(),
    }),
    previousFiles: new Map(),
  });
  const proposalErrors = proposal.errors.join("\n");
  assert.match(
    proposalErrors,
    /must not include application, Migration, or DDL/,
  );
  assert.match(proposalErrors, /must not include protected spec changes/);

  const archivePath = patchPath("wallet-contract", "archive");
  const appliedTargets = [
    { requirement: "WALLET-003" },
    { decision: "DEC-WALLET" },
    { operation: "POST /api/wallet/charge" },
    { traceability: "WALLET-003" },
  ];
  const appliedDocument = createPatchDocument({
    status: "applied",
    applied_in_version: "3.0.1",
    applied_by_pr: 209,
    targets: appliedTargets,
  });
  const previousFiles = createPatchSnapshot({
    [proposedPath]: createPatchDocument({
      status: "accepted",
      targets: appliedTargets,
    }),
  });
  const currentFiles = createPatchSnapshot({ [archivePath]: appliedDocument });
  const incompleteRelease = verifyPatchSnapshot({
    changedFiles: new Set([proposedPath, archivePath]),
    currentFiles,
    previousFiles,
  });
  assert.match(
    incompleteRelease.errors.join("\n"),
    /atomically update docs\/specs\/SPEC_LOCK\.json/,
  );
  assert.match(
    incompleteRelease.errors.join("\n"),
    /atomically update docs\/specs\/README\.md/,
  );
  assert.match(
    incompleteRelease.errors.join("\n"),
    /atomically update docs\/specs\/REQUIREMENTS\.md/,
  );
  assert.match(
    incompleteRelease.errors.join("\n"),
    /atomically update docs\/specs\/DECISIONS\.md/,
  );
  assert.match(
    incompleteRelease.errors.join("\n"),
    /atomically update docs\/specs\/API_SPEC\.md/,
  );
  assert.match(
    incompleteRelease.errors.join("\n"),
    /atomically update docs\/specs\/SPEC_TRACEABILITY\.md/,
  );

  const renamedArchivePath = patchPath("wallet-contract-renamed", "archive", 2);
  const renamedRelease = verifyPatchSnapshot({
    changedFiles: new Set([proposedPath, renamedArchivePath]),
    currentFiles: createPatchSnapshot({
      [renamedArchivePath]: appliedDocument,
    }),
    previousFiles,
  });
  assert.match(
    renamedRelease.errors.join("\n"),
    /accepted Patch filename and revision are immutable/,
  );

  const completeRelease = verifyPatchSnapshot({
    changedFiles: new Set([
      proposedPath,
      archivePath,
      "docs/specs/SPEC_LOCK.json",
      "docs/specs/README.md",
      "docs/specs/REQUIREMENTS.md",
      "docs/specs/DECISIONS.md",
      "docs/specs/API_SPEC.md",
      "docs/specs/SPEC_TRACEABILITY.md",
    ]),
    canonicalSpecVersion: "3.0.1",
    previousCanonicalSpecVersion: "3.0.0",
    currentFiles,
    previousFiles,
  });
  assert.deepEqual(completeRelease.errors, []);

  const unchangedRelease = verifyPatchSnapshot({
    changedFiles: new Set([
      proposedPath,
      archivePath,
      "docs/specs/SPEC_LOCK.json",
      "docs/specs/README.md",
      "docs/specs/REQUIREMENTS.md",
      "docs/specs/DECISIONS.md",
      "docs/specs/API_SPEC.md",
      "docs/specs/SPEC_TRACEABILITY.md",
    ]),
    canonicalSpecVersion: "3.0.1",
    previousCanonicalSpecVersion: "3.0.1",
    currentFiles,
    previousFiles,
  });
  assert.match(
    unchangedRelease.errors.join("\n"),
    /must advance the canonical release beyond 3\.0\.1/,
  );
});

test("dry-runs the team-authored Issue #153 proposal through the full Patch lifecycle", () => {
  const proposedPath =
    "docs/spec-patches/proposed/donnyeonglee_issue-153_work-case-invitation-contract_patch_v1.md";
  const archivePath =
    "docs/spec-patches/archive/donnyeonglee_issue-153_work-case-invitation-contract_patch_v1.md";
  const sourceComment =
    "https://github.com/Flamingo7562/KB-PJT-24-2/issues/153#issuecomment-5176096637";
  const createTeamFixture = (overrides = {}) =>
    createPatchDocument({
      patch_id: "SPEC-153-01",
      author: "donnyeonglee",
      issue: 153,
      base_commit: "d01d307dae26ce816aa386f3fcf9f7ec514475fc",
      change_type: "clarification",
      targets: [{ decision: "DEC-OPEN-WORK-CASE-RESPONSE-SHAPES" }],
      ...overrides,
    })
      .replace(
        "지갑 계약의 명세 추적 조건을 명확히 기록한다.",
        "Issue #153 팀원 제안을 축약해 Patch 상태 기록 형식만 검증한다.",
      )
      .replace(
        "현재 계약은 추적 대상과 검증 조건을 함께 확인해야 한다.",
        "원문 제안은 GitHub 댓글에 있으며 이 fixture는 제품 의미를 승인하지 않는다.",
      )
      .replace(
        "지갑 계약의 변경은 관련 요구사항과 검증 조건을 함께 추적한다.",
        "제품 문장을 적용하지 않고 Guardrail 상태 전이만 검증한다.",
      )
      .replace("- Issue #205", `- Source: ${sourceComment}`);

  // 실제 제품 승인과 분리한 구조 시뮬레이션으로 세 상태의 저장 위치와 원자 적용만 확인한다.
  const proposedDocument = createTeamFixture({ status: "proposed" });
  const proposed = verifyPatchSnapshot({
    changedFiles: new Set([proposedPath]),
    currentFiles: createPatchSnapshot({
      [proposedPath]: proposedDocument,
    }),
    previousFiles: createPatchSnapshot(),
  });
  assert.deepEqual(proposed, { errors: [], warnings: [] });

  const acceptedDocument = createTeamFixture({ status: "accepted" });
  const accepted = verifyPatchSnapshot({
    changedFiles: new Set([proposedPath]),
    currentFiles: createPatchSnapshot({
      [proposedPath]: acceptedDocument,
    }),
    previousFiles: createPatchSnapshot({
      [proposedPath]: proposedDocument,
    }),
  });
  assert.deepEqual(accepted, { errors: [], warnings: [] });

  const appliedDocument = createTeamFixture({
    status: "applied",
    applied_in_version: "3.0.1",
    applied_by_pr: 999,
  });
  const applied = verifyPatchSnapshot({
    canonicalSpecVersion: "3.0.1",
    previousCanonicalSpecVersion: "3.0.0",
    changedFiles: new Set([
      proposedPath,
      archivePath,
      "docs/specs/DECISIONS.md",
      "docs/specs/README.md",
      "docs/specs/SPEC_LOCK.json",
    ]),
    currentFiles: createPatchSnapshot({
      [archivePath]: appliedDocument,
    }),
    previousFiles: createPatchSnapshot({
      [proposedPath]: acceptedDocument,
    }),
  });
  assert.deepEqual(applied, { errors: [], warnings: [] });
});

test("warns about stale active bases and shared active targets", () => {
  const firstPath = patchPath("wallet-contract");
  const secondPath = patchPath("wallet-contract-followup", "proposed", 2);
  const result = verifyPatchSnapshot({
    canonicalSpecVersion: "3.0.1",
    changedFiles: new Set([firstPath, secondPath]),
    currentDevCommit: "b".repeat(40),
    currentFiles: createPatchSnapshot({
      [firstPath]: createPatchDocument({
        targets: [{ operation: "POST /api/wallet/charge" }],
      }),
      [secondPath]: createPatchDocument({
        patch_id: "SPEC-205-02",
        targets: [{ rest_operation: "POST /api/wallet/charge" }],
      }),
    }),
    previousFiles: new Map(),
  });

  assert.equal(result.errors.length, 0);
  assert.match(result.warnings.join("\n"), /stale base_spec_version/);
  assert.match(result.warnings.join("\n"), /stale base_commit/);
  assert.match(result.warnings.join("\n"), /target conflict/);
});

test("all mode checks committed Patch PR scope from the origin/dev merge base", () => {
  const temporaryRepository = fs.mkdtempSync(
    path.join(os.tmpdir(), "gighub-patch-pr-scope-"),
  );
  const script = path.resolve(__dirname, "check-project-guardrails.js");

  try {
    execFileSync("git", ["init", "--quiet"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    execFileSync("git", ["config", "user.name", "Guardrail Test"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    execFileSync("git", ["config", "user.email", "guardrail@example.com"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    writeSpecFixture(temporaryRepository);
    for (const [file, content] of Object.entries(PATCH_SCAFFOLD)) {
      writeRepositoryFile(temporaryRepository, file, content);
    }
    execFileSync("git", ["add", "."], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    execFileSync("git", ["commit", "--quiet", "-m", "baseline"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    const baseCommit = execFileSync("git", ["rev-parse", "HEAD"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    }).trim();
    const missingBase = spawnSync(process.execPath, [script, "--all"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });
    assert.equal(missingBase.status, 1);
    assert.match(missingBase.stderr, /requires refs\/remotes\/origin\/dev/);
    execFileSync("git", ["update-ref", "refs/remotes/origin/dev", baseCommit], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });

    const file = patchPath("wallet-contract");
    writeRepositoryFile(
      temporaryRepository,
      file,
      createPatchDocument({ base_commit: baseCommit }),
    );
    writeRepositoryFile(
      temporaryRepository,
      "frontend/src/wallet.js",
      "export const wallet = 'mixed';\n",
    );
    execFileSync("git", ["add", "."], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    execFileSync("git", ["commit", "--quiet", "-m", "mixed patch"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });

    const result = spawnSync(process.execPath, [script, "--all"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });
    assert.equal(result.status, 1);
    assert.match(
      result.stderr,
      /Patch change must not include application, Migration, or DDL: frontend\/src\/wallet\.js/,
    );
  } finally {
    fs.rmSync(temporaryRepository, { recursive: true, force: true });
  }
});

test("all mode fails closed after Patch governance was deleted from HEAD", () => {
  const temporaryRepository = fs.mkdtempSync(
    path.join(os.tmpdir(), "gighub-patch-deletion-base-"),
  );
  const script = path.resolve(__dirname, "check-project-guardrails.js");

  try {
    execFileSync("git", ["init", "--quiet"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    execFileSync("git", ["config", "user.name", "Guardrail Test"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    execFileSync("git", ["config", "user.email", "guardrail@example.com"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    writeSpecFixture(temporaryRepository);
    for (const [file, content] of Object.entries(PATCH_SCAFFOLD)) {
      writeRepositoryFile(temporaryRepository, file, content);
    }
    execFileSync("git", ["add", "."], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    execFileSync("git", ["commit", "--quiet", "-m", "baseline"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });

    fs.rmSync(path.join(temporaryRepository, "docs", "spec-patches"), {
      recursive: true,
      force: true,
    });
    execFileSync("git", ["add", "-A"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    execFileSync("git", ["commit", "--quiet", "-m", "delete governance"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });

    const result = spawnSync(process.execPath, [script, "--all"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });
    assert.equal(result.status, 1);
    assert.match(result.stderr, /requires refs\/remotes\/origin\/dev/);
  } finally {
    fs.rmSync(temporaryRepository, { recursive: true, force: true });
  }
});

test("all mode accepts proposed then accepted commits but rejects a skipped proposal", () => {
  const temporaryRepository = fs.mkdtempSync(
    path.join(os.tmpdir(), "gighub-patch-lifecycle-"),
  );
  const script = path.resolve(__dirname, "check-project-guardrails.js");

  try {
    execFileSync("git", ["init", "--quiet"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    execFileSync("git", ["config", "user.name", "Guardrail Test"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    execFileSync("git", ["config", "user.email", "guardrail@example.com"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    writeSpecFixture(temporaryRepository);
    for (const [file, content] of Object.entries(PATCH_SCAFFOLD)) {
      writeRepositoryFile(temporaryRepository, file, content);
    }
    execFileSync("git", ["add", "."], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    execFileSync("git", ["commit", "--quiet", "-m", "baseline"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    const baseCommit = execFileSync("git", ["rev-parse", "HEAD"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    }).trim();
    execFileSync("git", ["update-ref", "refs/remotes/origin/dev", baseCommit], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });

    const file = patchPath("wallet-contract");
    writeRepositoryFile(
      temporaryRepository,
      file,
      createPatchDocument({ status: "proposed", base_commit: baseCommit }),
    );
    execFileSync("git", ["add", "."], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    execFileSync("git", ["commit", "--quiet", "-m", "propose patch"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });

    writeRepositoryFile(
      temporaryRepository,
      file,
      createPatchDocument({ status: "accepted", base_commit: baseCommit }),
    );
    execFileSync("git", ["add", "."], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    execFileSync("git", ["commit", "--quiet", "-m", "accept patch"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });

    const valid = spawnSync(process.execPath, [script, "--all"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });
    assert.equal(valid.status, 0, valid.stderr);

    const skippedFile = patchPath("skipped-proposal", "proposed", 2);
    writeRepositoryFile(
      temporaryRepository,
      skippedFile,
      createPatchDocument({
        patch_id: "SPEC-205-02",
        status: "accepted",
        base_commit: baseCommit,
      }),
    );
    execFileSync("git", ["add", "."], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });
    execFileSync("git", ["commit", "--quiet", "-m", "skip proposal"], {
      cwd: temporaryRepository,
      stdio: "ignore",
    });

    const invalid = spawnSync(process.execPath, [script, "--all"], {
      cwd: temporaryRepository,
      encoding: "utf8",
    });
    assert.equal(invalid.status, 1);
    assert.match(invalid.stderr, /new Patch must start in draft or proposed/);
  } finally {
    fs.rmSync(temporaryRepository, { recursive: true, force: true });
  }
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
