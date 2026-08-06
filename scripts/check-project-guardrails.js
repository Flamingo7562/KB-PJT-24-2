#!/usr/bin/env node

const { execFileSync } = require("node:child_process");
const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");

const SPEC_ROOT = "docs/specs";
const SPEC_MANIFEST_PATH = `${SPEC_ROOT}/SPEC_LOCK.json`;
const SPEC_MANIFEST_VERSION = 1;
const SPEC_HASH_ALGORITHM = "sha256";
const SPEC_NORMALIZATION = "crlf-to-lf";
const CANONICAL_SPEC_MARKDOWN_PATHS = [
  `${SPEC_ROOT}/README.md`,
  `${SPEC_ROOT}/REQUIREMENTS.md`,
  `${SPEC_ROOT}/API_SPEC.md`,
  `${SPEC_ROOT}/DECISIONS.md`,
  `${SPEC_ROOT}/SPEC_TRACEABILITY.md`,
];
const PATCH_ROOT = "docs/spec-patches";
const PATCH_README_PATH = `${PATCH_ROOT}/README.md`;
const PATCH_TEMPLATE_PATH = `${PATCH_ROOT}/TEMPLATE.md`;
const PATCH_KEEP_PATHS = new Set([
  `${PATCH_ROOT}/proposed/.gitkeep`,
  `${PATCH_ROOT}/archive/.gitkeep`,
]);
const PATCH_REQUIRED_METADATA = [
  "patch_id",
  "author",
  "status",
  "issue",
  "created_at",
  "base_spec_version",
  "base_commit",
  "change_type",
  "targets",
  "depends_on",
  "supersedes",
  "superseded_by",
  "applied_in_version",
  "applied_by_pr",
];
const PATCH_OPTIONAL_METADATA = ["delivery_mode"];
const PATCH_SUPPORTED_METADATA = new Set([
  ...PATCH_REQUIRED_METADATA,
  ...PATCH_OPTIONAL_METADATA,
]);
const PATCH_REQUIRED_SECTIONS = [
  "변경 요약과 필요성",
  "현재 명세와 문제",
  "제안할 최종 규범 문장 또는 Before/After",
  "영향 분석",
  "검증 가능한 수용 조건",
  "미결 사항",
  "관련 Issue·PR·의존 Patch",
];
const PATCH_REQUIRED_IMPACT_SECTIONS = [
  "요구사항",
  "API",
  "데이터 및 Migration",
  "보안",
  "Frontend",
  "Backend",
  "테스트",
];
const PATCH_STATUSES = new Set([
  "draft",
  "proposed",
  "accepted",
  "applied",
  "rejected",
  "superseded",
]);
const PATCH_ACTIVE_STATUSES = new Set(["draft", "proposed", "accepted"]);
const PATCH_ARCHIVED_STATUSES = new Set(["applied", "rejected", "superseded"]);
const PATCH_CHANGE_TYPES = new Set(["additive", "clarification", "breaking"]);
const PATCH_DELIVERY_MODES = new Set(["implementation_bundled", "spec_first"]);
const PATCH_ALLOWED_TRANSITIONS = new Map([
  ["draft", new Set(["proposed"])],
  ["proposed", new Set(["accepted", "rejected", "superseded"])],
  ["accepted", new Set(["applied", "superseded"])],
  ["applied", new Set()],
  ["rejected", new Set()],
  ["superseded", new Set()],
]);
const PATCH_TARGET_SPEC_PATHS = new Map([
  ["requirement", `${SPEC_ROOT}/REQUIREMENTS.md`],
  ["api", `${SPEC_ROOT}/API_SPEC.md`],
  ["operation", `${SPEC_ROOT}/API_SPEC.md`],
  ["rest_operation", `${SPEC_ROOT}/API_SPEC.md`],
  ["rest-operation", `${SPEC_ROOT}/API_SPEC.md`],
  ["decision", `${SPEC_ROOT}/DECISIONS.md`],
  ["traceability", `${SPEC_ROOT}/SPEC_TRACEABILITY.md`],
]);
const PATCH_FILE_PATTERN = new RegExp(
  `^${PATCH_ROOT}/(proposed|archive)/` +
    "([a-z0-9]+(?:-[a-z0-9]+)*)_" +
    "issue-([1-9][0-9]*)_" +
    "([a-z0-9]+(?:-[a-z0-9]+)*)_patch_v([1-9][0-9]*)\\.md$",
);
const PATCH_ID_PATTERN = /^SPEC-([1-9][0-9]*)-(?!00$)([0-9]{2})$/;
const SEMVER_PATTERN = /^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$/;
const FULL_GIT_COMMIT_PATTERN = /^[0-9a-f]{40}$/;

function git(args, options = {}) {
  return execFileSync("git", args, {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "inherit"],
    ...options,
  });
}

function gitOptional(args) {
  try {
    return execFileSync("git", args, {
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"],
    });
  } catch {
    return null;
  }
}

function normalizePath(file) {
  return file.replace(/\\/g, "/");
}

function comparePaths(left, right) {
  if (left < right) return -1;
  if (left > right) return 1;
  return 0;
}

function compareSemverVersions(left, right) {
  const leftParts = left.split(".").map(Number);
  const rightParts = right.split(".").map(Number);
  for (let index = 0; index < 3; index += 1) {
    if (leftParts[index] !== rightParts[index]) {
      return leftParts[index] - rightParts[index];
    }
  }
  return 0;
}

function splitNullSeparated(output) {
  return output.split("\0").map(normalizePath).filter(Boolean);
}

function getStagedFiles() {
  return splitNullSeparated(
    git(["diff", "--cached", "--name-only", "-z", "--diff-filter=ACMR"]),
  );
}

function getAllComparisonBase() {
  const head = gitOptional(["rev-parse", "--verify", "HEAD"]);
  if (head === null) return null;

  const originDev = gitOptional([
    "rev-parse",
    "--verify",
    "refs/remotes/origin/dev",
  ]);
  if (originDev === null) {
    throw new Error(
      "Specification Patch --all validation requires refs/remotes/origin/dev; fetch latest origin/dev before continuing.",
    );
  }

  const mergeBase = gitOptional([
    "merge-base",
    "HEAD",
    "refs/remotes/origin/dev",
  ])?.trim();
  if (!mergeBase) {
    throw new Error(
      "Specification Patch --all validation requires a merge base with origin/dev; fetch full history before continuing.",
    );
  }
  return mergeBase;
}

function getChangedFiles(mode) {
  const changed = new Set();
  const diffArguments = [
    "--name-only",
    "-z",
    "--diff-filter=ACMRD",
    "--no-renames",
  ];
  const commands = [];

  if (mode === "staged") {
    commands.push(["diff", "--cached", ...diffArguments]);
  } else {
    const comparisonBase = getAllComparisonBase();

    // PR 전체 범위와 아직 커밋하지 않은 후보 상태를 한 번에 비교한다.
    if (comparisonBase) {
      commands.push(["diff", comparisonBase, ...diffArguments]);
    } else {
      commands.push(["diff", "--cached", ...diffArguments]);
      commands.push(["diff", ...diffArguments]);
    }
    commands.push(["ls-files", "--others", "--exclude-standard", "-z"]);
  }

  for (const args of commands) {
    const output = gitOptional(args);
    if (output === null) continue;
    for (const file of splitNullSeparated(output)) {
      changed.add(file);
    }
  }

  return changed;
}

function getWorkingTreeFiles() {
  return splitNullSeparated(
    git(["ls-files", "--cached", "--others", "--exclude-standard", "-z"]),
  );
}

function isIgnoredDocumentation(file) {
  return (
    file.startsWith("docs/") ||
    file.startsWith(".github/") ||
    file.startsWith(".husky/") ||
    file.endsWith(".md") ||
    file === ".gitmessage.txt"
  );
}

function isPackageManifest(file) {
  return /(^|\/)(package(-lock)?\.json|pnpm-lock\.yaml|yarn\.lock)$/.test(file);
}

function isFrontendSourceOrConfig(file) {
  return (
    (file.startsWith("frontend/") && /\.[cm]?[jt]sx?$|\.vue$/.test(file)) ||
    /(^|\/)vite\.config\.[cm]?[jt]s$/.test(file)
  );
}

function isBackendSourceOrBuild(file) {
  return (
    (file.startsWith("backend/") &&
      /(^|\/)(pom\.xml|build\.gradle(\.kts)?|settings\.gradle(\.kts)?|gradle\.properties|.*\.java|.*\.xml|.*\.toml)$/.test(
        file,
      )) ||
    /^(pom\.xml|build\.gradle(\.kts)?|settings\.gradle(\.kts)?|gradle\.properties|gradle\/.*\.toml)$/.test(
      file,
    )
  );
}

const rules = [
  {
    name: "React dependency",
    appliesTo: (file) =>
      isPackageManifest(file) || isFrontendSourceOrConfig(file),
    contentPattern:
      /("|'|`)(react|react-dom|@vitejs\/plugin-react)("|'|`)|from\s+("|'|`)react("|'|`)/i,
    message:
      "Frontend는 Vue.js를 사용해야 하므로 React 관련 의존성 또는 import를 추가할 수 없습니다.",
  },
  {
    name: "Spring Boot",
    appliesTo: isBackendSourceOrBuild,
    contentPattern: /spring-boot|org\.springframework\.boot/i,
    message:
      "Backend는 Spring Framework legacy를 사용해야 하므로 Spring Boot를 추가할 수 없습니다.",
  },
  {
    name: "JPA",
    appliesTo: isBackendSourceOrBuild,
    contentPattern:
      /JpaRepository|@Entity\b|javax\.persistence|jakarta\.persistence|hibernate-entitymanager|spring-data-jpa/i,
    message:
      "Persistence는 MyBatis를 사용해야 하므로 JPA 관련 코드를 추가할 수 없습니다.",
  },
];

function findViolations(entries) {
  const violations = [];

  for (const { file, content } of entries) {
    if (isIgnoredDocumentation(file)) {
      continue;
    }

    for (const rule of rules) {
      if (rule.appliesTo(file) && rule.contentPattern.test(content)) {
        violations.push({ file, rule });
      }
    }
  }

  return violations;
}

function readStagedEntries() {
  return getStagedFiles()
    .filter(
      (file) =>
        !isIgnoredDocumentation(file) &&
        rules.some((rule) => rule.appliesTo(file)),
    )
    .map((file) => ({
      file,
      content: git(["show", `:${file}`]),
    }));
}

function readWorkingTreeEntries() {
  const entries = [];

  for (const file of getWorkingTreeFiles()) {
    if (isIgnoredDocumentation(file) || !fs.existsSync(file)) {
      continue;
    }

    if (!rules.some((rule) => rule.appliesTo(file))) {
      continue;
    }

    entries.push({
      file,
      content: fs.readFileSync(file, "utf8"),
    });
  }

  return entries;
}

function normalizeSpecContent(content) {
  const text = Buffer.isBuffer(content)
    ? content.toString("utf8")
    : String(content);
  return text.replace(/\r\n/g, "\n");
}

function hashNormalizedSpecContent(content) {
  return crypto
    .createHash(SPEC_HASH_ALGORITHM)
    .update(normalizeSpecContent(content), "utf8")
    .digest("hex");
}

function isPlainObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function hasExactKeys(value, expectedKeys) {
  if (!isPlainObject(value)) return false;
  const actualKeys = Object.keys(value).sort(comparePaths);
  const sortedExpected = [...expectedKeys].sort(comparePaths);
  return (
    actualKeys.length === sortedExpected.length &&
    actualKeys.every((key, index) => key === sortedExpected[index])
  );
}

function isValidProtectedSpecPath(file) {
  if (typeof file !== "string" || file.length === 0) return false;
  if (file !== normalizePath(file)) return false;
  if (!file.startsWith(`${SPEC_ROOT}/`)) return false;
  if (file === SPEC_MANIFEST_PATH || file.endsWith("/")) return false;

  const segments = file.split("/");
  return !segments.includes(".") && !segments.includes("..");
}

function parseSpecManifest(content) {
  let parsed;
  try {
    parsed = JSON.parse(String(content));
  } catch (error) {
    return {
      entries: [],
      errors: [`${SPEC_MANIFEST_PATH} is not valid JSON: ${error.message}`],
    };
  }

  if (!isPlainObject(parsed)) {
    return {
      entries: [],
      errors: [`${SPEC_MANIFEST_PATH} root must be a JSON object.`],
    };
  }

  const errors = [];
  if (
    !hasExactKeys(parsed, ["algorithm", "files", "normalization", "version"])
  ) {
    errors.push(
      `${SPEC_MANIFEST_PATH} must contain exactly version, algorithm, normalization, and files.`,
    );
  }

  if (parsed.version !== SPEC_MANIFEST_VERSION) {
    errors.push(
      `${SPEC_MANIFEST_PATH} version must be ${SPEC_MANIFEST_VERSION}.`,
    );
  }
  if (parsed.algorithm !== SPEC_HASH_ALGORITHM) {
    errors.push(
      `${SPEC_MANIFEST_PATH} algorithm must be "${SPEC_HASH_ALGORITHM}".`,
    );
  }
  if (parsed.normalization !== SPEC_NORMALIZATION) {
    errors.push(
      `${SPEC_MANIFEST_PATH} normalization must be "${SPEC_NORMALIZATION}".`,
    );
  }
  if (!Array.isArray(parsed.files) || parsed.files.length === 0) {
    errors.push(`${SPEC_MANIFEST_PATH} files must be a non-empty array.`);
    return { entries: [], errors };
  }

  const entries = [];
  const seenPaths = new Set();

  parsed.files.forEach((entry, index) => {
    const label = `${SPEC_MANIFEST_PATH} files[${index}]`;
    if (!hasExactKeys(entry, ["path", "sha256"])) {
      errors.push(`${label} must contain exactly path and sha256.`);
      return;
    }
    if (!isValidProtectedSpecPath(entry.path)) {
      errors.push(
        `${label}.path must be a normalized repository-relative file under ${SPEC_ROOT}/ and must not be the manifest itself.`,
      );
      return;
    }
    if (!/^[0-9a-f]{64}$/.test(entry.sha256)) {
      errors.push(`${label}.sha256 must be 64 lowercase hexadecimal digits.`);
      return;
    }
    if (seenPaths.has(entry.path)) {
      errors.push(`${SPEC_MANIFEST_PATH} lists ${entry.path} more than once.`);
      return;
    }

    seenPaths.add(entry.path);
    entries.push({ path: entry.path, sha256: entry.sha256 });
  });

  const sortedPaths = entries.map((entry) => entry.path).sort(comparePaths);
  const manifestPaths = entries.map((entry) => entry.path);
  if (
    manifestPaths.length === sortedPaths.length &&
    manifestPaths.some((file, index) => file !== sortedPaths[index])
  ) {
    errors.push(`${SPEC_MANIFEST_PATH} files must be sorted by path.`);
  }

  return { entries, errors };
}

function collectWorkingTreeSpecSnapshot(cwd = process.cwd()) {
  const absoluteSpecRoot = path.join(cwd, ...SPEC_ROOT.split("/"));
  const absoluteManifest = path.join(cwd, ...SPEC_MANIFEST_PATH.split("/"));

  if (!fs.existsSync(absoluteManifest)) {
    throw new Error(
      `Required protected-spec manifest is missing: ${SPEC_MANIFEST_PATH}`,
    );
  }
  if (!fs.existsSync(absoluteSpecRoot)) {
    throw new Error(`Protected spec directory is missing: ${SPEC_ROOT}`);
  }

  const files = new Map();

  function visit(absoluteDirectory, relativeDirectory) {
    const directoryEntries = fs
      .readdirSync(absoluteDirectory, { withFileTypes: true })
      .sort((left, right) => comparePaths(left.name, right.name));

    for (const entry of directoryEntries) {
      const absoluteEntry = path.join(absoluteDirectory, entry.name);
      const relativeEntry = normalizePath(
        path.posix.join(relativeDirectory, entry.name),
      );

      if (entry.isSymbolicLink()) {
        throw new Error(
          `Protected spec paths must not be symbolic links: ${relativeEntry}`,
        );
      }
      if (entry.isDirectory()) {
        visit(absoluteEntry, relativeEntry);
        continue;
      }
      if (!entry.isFile()) {
        throw new Error(
          `Unsupported protected spec filesystem entry: ${relativeEntry}`,
        );
      }
      if (relativeEntry !== SPEC_MANIFEST_PATH) {
        files.set(relativeEntry, fs.readFileSync(absoluteEntry));
      }
    }
  }

  visit(absoluteSpecRoot, SPEC_ROOT);

  return {
    files,
    manifestContent: fs.readFileSync(absoluteManifest, "utf8"),
  };
}

function collectStagedSpecSnapshot() {
  const stagedSpecPaths = splitNullSeparated(
    git(["ls-files", "--cached", "-z", "--", `${SPEC_ROOT}/`]),
  ).sort(comparePaths);

  if (!stagedSpecPaths.includes(SPEC_MANIFEST_PATH)) {
    throw new Error(
      `Required protected-spec manifest is missing from the staged index: ${SPEC_MANIFEST_PATH}`,
    );
  }

  const files = new Map();
  for (const file of stagedSpecPaths) {
    if (file !== SPEC_MANIFEST_PATH) {
      files.set(file, git(["show", `:${file}`]));
    }
  }

  return {
    files,
    manifestContent: git(["show", `:${SPEC_MANIFEST_PATH}`]),
  };
}

function extractSpecReleaseVersion(content) {
  const normalized = normalizeSpecContent(content);
  const firstSectionIndex = normalized.search(/^##\s+/m);
  const header =
    firstSectionIndex === -1
      ? normalized
      : normalized.slice(0, firstSectionIndex);
  const semver = "((?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*))";
  const releaseRow = new RegExp(
    `^\\|\\s*(?:명세 릴리스|Release)\\s*\\|\\s*\\\`${semver}\\\`\\s*\\|`,
    "m",
  );
  return releaseRow.exec(header)?.[1] ?? null;
}

function extractReadmeReleaseRows(content) {
  const normalized = normalizeSpecContent(content);
  const releaseHistory =
    getMarkdownSection(normalized, "릴리스 기록", 2) ??
    getMarkdownSection(normalized, "Release history", 2);
  if (releaseHistory === null) return [];

  return [...releaseHistory.matchAll(/^\|\s*`([^`]+)`\s*\|/gm)].map(
    (match) => match[1],
  );
}

function verifySpecReleaseMetadata(files) {
  const errors = [];
  const versions = new Map();

  for (const file of CANONICAL_SPEC_MARKDOWN_PATHS) {
    if (!files.has(file)) {
      errors.push(`Canonical spec release file is missing: ${file}`);
      continue;
    }
    const version = extractSpecReleaseVersion(files.get(file));
    if (!version) {
      errors.push(
        `${file} must declare a complete SemVer release in its header.`,
      );
    } else {
      versions.set(file, version);
    }
  }

  const uniqueVersions = [...new Set(versions.values())];
  if (uniqueVersions.length > 1) {
    errors.push(
      `Canonical spec Markdown release versions must match: ${[...versions]
        .map(([file, version]) => `${file}=${version}`)
        .join(", ")}.`,
    );
  }

  const readmeVersion = versions.get(`${SPEC_ROOT}/README.md`);
  if (readmeVersion && files.has(`${SPEC_ROOT}/README.md`)) {
    const releaseRows = extractReadmeReleaseRows(
      files.get(`${SPEC_ROOT}/README.md`),
    );
    if (releaseRows.length === 0) {
      errors.push(`${SPEC_ROOT}/README.md must contain a release-history row.`);
    } else if (releaseRows[0] !== readmeVersion) {
      errors.push(
        `${SPEC_ROOT}/README.md latest release row (${releaseRows[0]}) must match its header (${readmeVersion}).`,
      );
    }
  }

  return errors;
}

function verifySpecSnapshot({ files, manifestContent }) {
  const { entries, errors } = parseSpecManifest(manifestContent);
  if (errors.length > 0) {
    return errors;
  }

  const expectedByPath = new Map(
    entries.map((entry) => [entry.path, entry.sha256]),
  );
  const verificationErrors = verifySpecReleaseMetadata(files);

  for (const expectedPath of expectedByPath.keys()) {
    if (!files.has(expectedPath)) {
      verificationErrors.push(
        `Protected spec file is missing or deleted: ${expectedPath}`,
      );
    }
  }

  for (const actualPath of [...files.keys()].sort(comparePaths)) {
    if (!expectedByPath.has(actualPath)) {
      verificationErrors.push(
        `Protected spec file is not listed in ${SPEC_MANIFEST_PATH}: ${actualPath}`,
      );
    }
  }

  for (const [expectedPath, expectedHash] of expectedByPath) {
    if (!files.has(expectedPath)) continue;
    const actualHash = hashNormalizedSpecContent(files.get(expectedPath));
    if (actualHash !== expectedHash) {
      verificationErrors.push(
        `Protected spec hash mismatch: ${expectedPath} (expected ${expectedHash}, actual ${actualHash})`,
      );
    }
  }

  return verificationErrors;
}

function validateSpecLock(mode) {
  try {
    const snapshot =
      mode === "staged"
        ? collectStagedSpecSnapshot()
        : collectWorkingTreeSpecSnapshot();
    return verifySpecSnapshot(snapshot);
  } catch (error) {
    return [error.message];
  }
}

function parsePatchPath(file) {
  const match = PATCH_FILE_PATTERN.exec(file);
  if (!match) return null;

  return {
    directory: match[1],
    author: match[2],
    issue: Number(match[3]),
    summary: match[4],
    revision: Number(match[5]),
  };
}

function parsePatchScalar(rawValue, label, errors) {
  const value = rawValue.trim();
  if (value === "null" || value === "~") return null;
  if (value === "[]") return [];
  if (/^[0-9]+$/.test(value)) return Number(value);

  if (value.startsWith('"') || value.endsWith('"')) {
    if (!(value.startsWith('"') && value.endsWith('"'))) {
      errors.push(`${label} has an unterminated double-quoted value.`);
      return value;
    }
    try {
      return JSON.parse(value);
    } catch (error) {
      errors.push(
        `${label} has an invalid double-quoted value: ${error.message}`,
      );
      return value;
    }
  }

  if (value.startsWith("'") || value.endsWith("'")) {
    if (!(value.startsWith("'") && value.endsWith("'"))) {
      errors.push(`${label} has an unterminated single-quoted value.`);
      return value;
    }
    return value.slice(1, -1).replace(/''/g, "'");
  }

  return value;
}

function parsePatchFrontMatter(content, file) {
  const normalized = normalizeSpecContent(content).replace(/^\uFEFF/, "");
  const lines = normalized.split("\n");
  const errors = [];

  if (lines[0] !== "---") {
    return {
      body: normalized,
      errors: [`${file} must start with YAML front matter delimited by ---.`],
      metadata: {},
    };
  }

  const closingIndex = lines.indexOf("---", 1);
  if (closingIndex === -1) {
    return {
      body: "",
      errors: [`${file} YAML front matter is missing its closing ---.`],
      metadata: {},
    };
  }

  const metadata = {};
  let collection = null;

  for (let index = 1; index < closingIndex; index += 1) {
    const line = lines[index];
    const label = `${file} front matter line ${index + 1}`;
    if (line.trim() === "") continue;

    const topLevel = /^([a-z][a-z0-9_]*):(?:\s*(.*))?$/.exec(line);
    if (topLevel) {
      const [, key, rawValue = ""] = topLevel;
      if (Object.prototype.hasOwnProperty.call(metadata, key)) {
        errors.push(`${file} metadata key appears more than once: ${key}`);
        collection = null;
        continue;
      }

      if (rawValue === "" && ["targets", "depends_on"].includes(key)) {
        metadata[key] = [];
        collection = key;
      } else if (rawValue === "") {
        errors.push(`${label} must provide a scalar value for ${key}.`);
        metadata[key] = "";
        collection = null;
      } else {
        metadata[key] = parsePatchScalar(rawValue, label, errors);
        collection = null;
      }
      continue;
    }

    if (collection === "targets") {
      const target = /^  - ([a-z][a-z0-9_-]*):\s*(.+)$/.exec(line);
      if (target) {
        const value = parsePatchScalar(target[2], label, errors);
        metadata.targets.push({ [target[1]]: value });
        continue;
      }
    }

    if (collection === "depends_on") {
      const dependency = /^  -\s+(.+)$/.exec(line);
      if (dependency) {
        metadata.depends_on.push(
          parsePatchScalar(dependency[1], label, errors),
        );
        continue;
      }
    }

    errors.push(`${label} is not supported Patch metadata syntax.`);
  }

  return {
    body: lines.slice(closingIndex + 1).join("\n"),
    errors,
    metadata,
  };
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function getMarkdownSection(body, title, level) {
  const marker = "#".repeat(level);
  const heading = new RegExp(`^${marker} ${escapeRegExp(title)}\\s*$`, "m");
  const match = heading.exec(body);
  if (!match) return null;

  const sectionStart = match.index + match[0].length;
  const remainder = body.slice(sectionStart);
  const nextHeading = new RegExp(`^#{1,${level}}\\s+`, "m").exec(remainder);
  const sectionEnd = nextHeading ? nextHeading.index : remainder.length;
  return remainder.slice(0, sectionEnd).trim();
}

function isValidCalendarDate(value) {
  if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return false;
  }
  const parsed = new Date(`${value}T00:00:00.000Z`);
  return (
    !Number.isNaN(parsed.valueOf()) && parsed.toISOString().startsWith(value)
  );
}

function isValidPatchReference(value) {
  return typeof value === "string" && PATCH_ID_PATTERN.test(value);
}

function hasAcceptedPlaceholder(content) {
  return /<[^>\n]+>|\b(?:TODO|TBD|FIXME)\b|작성\s*필요|추후\s*결정|미정/i.test(
    content,
  );
}

function hasTemplateSentinel(metadata, body, targets) {
  return (
    metadata.base_spec_version === "0.0.0" ||
    metadata.base_commit === "0".repeat(40) ||
    targets.some((target) =>
      /^(?:REQUIREMENT-ID|DECISION-ID|REST-OPERATION)$/i.test(target.value),
    ) ||
    /^#\s+SPEC-000-01(?::|\s|$)|^#\s+.*명세 Patch 제목\s*$|수용 조건을 작성한다|최소 계약 변경을 제안한다|현재 계약의 공백을 설명한다|최종 규범 문장을 제시한다|implementation_bundled 또는 spec_first 선택 이유를 적는다/im.test(
      body,
    )
  );
}

function hasResolvedOpenQuestions(body) {
  const section = getMarkdownSection(body, "미결 사항", 2);
  if (section === null) return false;
  const normalized = section
    .replace(/<!--[\s\S]*?-->/g, "")
    .trim()
    .replace(/^[-*]\s+/, "")
    .replace(/^`|`$/g, "")
    .trim();
  return /^(?:없음|해당 없음|none)[.!。]?$/i.test(normalized);
}

function parsePatchDocument(file, content) {
  const pathInfo = parsePatchPath(file);
  if (!pathInfo) {
    return {
      errors: [
        `${file} must match ${PATCH_ROOT}/proposed|archive/<github-id>_issue-<number>_<kebab-summary>_patch_v<revision>.md.`,
      ],
      patch: null,
    };
  }

  const { body, errors, metadata } = parsePatchFrontMatter(content, file);
  const metadataKeys = new Set(Object.keys(metadata));
  for (const key of PATCH_REQUIRED_METADATA) {
    if (!metadataKeys.has(key)) {
      errors.push(`${file} is missing required metadata: ${key}.`);
    }
  }
  for (const key of metadataKeys) {
    if (!PATCH_SUPPORTED_METADATA.has(key)) {
      errors.push(`${file} has unsupported metadata: ${key}.`);
    }
  }

  const idMatch =
    typeof metadata.patch_id === "string"
      ? PATCH_ID_PATTERN.exec(metadata.patch_id)
      : null;
  if (!idMatch) {
    errors.push(
      `${file} patch_id must match SPEC-<issue>-<two-digit-sequence>.`,
    );
  } else if (Number(idMatch[1]) !== metadata.issue) {
    errors.push(`${file} patch_id issue must match the issue metadata.`);
  }

  if (metadata.author !== pathInfo.author) {
    errors.push(`${file} author metadata must match the filename GitHub ID.`);
  }
  if (metadata.issue !== pathInfo.issue) {
    errors.push(`${file} issue metadata must match the filename issue number.`);
  }
  if (!PATCH_STATUSES.has(metadata.status)) {
    errors.push(
      `${file} status must be one of: ${[...PATCH_STATUSES].join(", ")}.`,
    );
  }
  if (!Number.isInteger(metadata.issue) || metadata.issue <= 0) {
    errors.push(`${file} issue must be a positive integer.`);
  }
  if (!isValidCalendarDate(metadata.created_at)) {
    errors.push(`${file} created_at must be a valid YYYY-MM-DD date.`);
  }
  if (
    typeof metadata.base_spec_version !== "string" ||
    !SEMVER_PATTERN.test(metadata.base_spec_version)
  ) {
    errors.push(`${file} base_spec_version must be a complete SemVer value.`);
  }
  if (
    typeof metadata.base_commit !== "string" ||
    !FULL_GIT_COMMIT_PATTERN.test(metadata.base_commit)
  ) {
    errors.push(`${file} base_commit must be a lowercase full 40-hex Git SHA.`);
  }
  if (!PATCH_CHANGE_TYPES.has(metadata.change_type)) {
    errors.push(
      `${file} change_type must be one of: ${[...PATCH_CHANGE_TYPES].join(", ")}.`,
    );
  }

  // 정책 변경 전에 만든 Patch는 기존의 보수적인 spec-first 흐름을 그대로 따른다.
  const deliveryMode = metadata.delivery_mode ?? "spec_first";
  if (!PATCH_DELIVERY_MODES.has(deliveryMode)) {
    errors.push(
      `${file} delivery_mode must be one of: ${[...PATCH_DELIVERY_MODES].join(", ")}.`,
    );
  }
  if (
    deliveryMode === "implementation_bundled" &&
    metadata.change_type === "breaking"
  ) {
    errors.push(`${file} breaking Patch must use spec_first delivery_mode.`);
  }

  const targets = [];
  if (!Array.isArray(metadata.targets) || metadata.targets.length === 0) {
    errors.push(`${file} targets must be a non-empty list of single-key maps.`);
  } else {
    const seenTargets = new Set();
    metadata.targets.forEach((entry, index) => {
      const keys = isPlainObject(entry) ? Object.keys(entry) : [];
      if (keys.length !== 1) {
        errors.push(`${file} targets[${index}] must contain exactly one key.`);
        return;
      }
      const type = keys[0];
      const value = entry[type];
      if (typeof value !== "string" || value.trim() === "") {
        errors.push(
          `${file} targets[${index}] must name a stable contract ID.`,
        );
        return;
      }
      const normalizedType = [
        "api",
        "operation",
        "rest_operation",
        "rest-operation",
      ].includes(type)
        ? "rest_operation"
        : type;
      const normalizedTarget = `${normalizedType}:${value.trim()}`;
      if (seenTargets.has(normalizedTarget)) {
        errors.push(`${file} lists target ${normalizedTarget} more than once.`);
        return;
      }
      seenTargets.add(normalizedTarget);
      targets.push({ type, value: value.trim(), key: normalizedTarget });
    });
  }

  if (!Array.isArray(metadata.depends_on)) {
    errors.push(`${file} depends_on must be a list (use [] when empty).`);
  } else {
    const seenDependencies = new Set();
    metadata.depends_on.forEach((dependency, index) => {
      if (!isValidPatchReference(dependency)) {
        errors.push(`${file} depends_on[${index}] must be a Patch ID.`);
      } else if (seenDependencies.has(dependency)) {
        errors.push(`${file} lists dependency ${dependency} more than once.`);
      }
      seenDependencies.add(dependency);
    });
  }

  for (const key of ["supersedes", "superseded_by"]) {
    if (metadata[key] !== null && !isValidPatchReference(metadata[key])) {
      errors.push(`${file} ${key} must be null or a Patch ID.`);
    }
    if (metadata[key] === metadata.patch_id) {
      errors.push(`${file} ${key} must not reference the Patch itself.`);
    }
  }

  if (metadata.status === "superseded") {
    if (!isValidPatchReference(metadata.superseded_by)) {
      errors.push(`${file} superseded status requires superseded_by.`);
    }
  } else if (metadata.superseded_by !== null) {
    errors.push(
      `${file} superseded_by must be null until status is superseded.`,
    );
  }

  if (metadata.status === "applied") {
    if (
      typeof metadata.applied_in_version !== "string" ||
      !SEMVER_PATTERN.test(metadata.applied_in_version)
    ) {
      errors.push(`${file} applied status requires applied_in_version SemVer.`);
    }
    if (
      !Number.isInteger(metadata.applied_by_pr) ||
      metadata.applied_by_pr <= 0
    ) {
      errors.push(`${file} applied status requires a positive applied_by_pr.`);
    }
  } else if (
    metadata.applied_in_version !== null ||
    metadata.applied_by_pr !== null
  ) {
    errors.push(
      `${file} applied_in_version and applied_by_pr must stay null before applied status.`,
    );
  }

  if (
    PATCH_ACTIVE_STATUSES.has(metadata.status) &&
    pathInfo.directory !== "proposed"
  ) {
    errors.push(`${file} active Patch status must be stored under proposed/.`);
  }
  if (
    PATCH_ARCHIVED_STATUSES.has(metadata.status) &&
    pathInfo.directory !== "archive"
  ) {
    errors.push(`${file} terminal Patch status must be stored under archive/.`);
  }

  for (const title of PATCH_REQUIRED_SECTIONS) {
    const section = getMarkdownSection(body, title, 2);
    if (section === null || section === "") {
      errors.push(`${file} requires a non-empty "## ${title}" section.`);
    }
  }
  if (metadataKeys.has("delivery_mode")) {
    const deliverySection = getMarkdownSection(
      body,
      "전달 방식과 위험 판정",
      2,
    );
    if (deliverySection === null || deliverySection === "") {
      errors.push(
        `${file} with delivery_mode requires a non-empty "## 전달 방식과 위험 판정" section.`,
      );
    }
  }
  const impact = getMarkdownSection(body, "영향 분석", 2);
  if (impact !== null) {
    for (const title of PATCH_REQUIRED_IMPACT_SECTIONS) {
      const section = getMarkdownSection(impact, title, 3);
      if (section === null || section === "") {
        errors.push(
          `${file} requires a non-empty "### ${title}" impact section.`,
        );
      }
    }
  }

  if (["accepted", "applied"].includes(metadata.status)) {
    if (hasAcceptedPlaceholder(`${JSON.stringify(metadata)}\n${body}`)) {
      errors.push(`${file} accepted Patch must not contain placeholders.`);
    }
    if (!hasResolvedOpenQuestions(body)) {
      errors.push(
        `${file} accepted Patch must resolve "미결 사항" explicitly as 없음.`,
      );
    }
    if (hasTemplateSentinel(metadata, body, targets)) {
      errors.push(
        `${file} accepted Patch must replace every TEMPLATE sentinel with reviewed content.`,
      );
    }
  }

  return {
    errors,
    patch: {
      body,
      content: normalizeSpecContent(content),
      deliveryMode,
      file,
      metadata,
      pathInfo,
      targets,
    },
  };
}

function isAllowedPatchSupportPath(file) {
  return (
    file === PATCH_README_PATH ||
    file === PATCH_TEMPLATE_PATH ||
    PATCH_KEEP_PATHS.has(file)
  );
}

function collectWorkingTreePatchSnapshot() {
  const files = new Map();
  for (const file of getWorkingTreeFiles()) {
    if (!file.startsWith(`${PATCH_ROOT}/`) || !fs.existsSync(file)) continue;
    files.set(file, fs.readFileSync(file, "utf8"));
  }
  return files;
}

function collectStagedPatchSnapshot() {
  const files = new Map();
  const paths = splitNullSeparated(
    git(["ls-files", "--cached", "-z", "--", `${PATCH_ROOT}/`]),
  );
  for (const file of paths) {
    files.set(file, git(["show", `:${file}`]));
  }
  return files;
}

function collectGitPatchSnapshot(ref) {
  if (!ref) return new Map();
  const output = gitOptional([
    "ls-tree",
    "-r",
    "--name-only",
    "-z",
    ref,
    "--",
    `${PATCH_ROOT}/`,
  ]);
  if (output === null) return new Map();

  const files = new Map();
  for (const file of splitNullSeparated(output)) {
    const content = gitOptional(["show", `${ref}:${file}`]);
    if (content !== null) files.set(file, content);
  }
  return files;
}

function collectPreviousPatchSnapshot(mode) {
  const ref = mode === "staged" ? "HEAD" : getAllComparisonBase();
  return collectGitPatchSnapshot(ref);
}

function indexPatchDocuments(files, errors, validateContent) {
  const patches = [];
  for (const [file, content] of files) {
    if (isAllowedPatchSupportPath(file)) continue;
    const parsed = parsePatchDocument(file, content);
    if (validateContent) errors.push(...parsed.errors);
    if (parsed.patch) patches.push(parsed.patch);
  }
  return patches;
}

function indexLifecyclePatches(files, errors) {
  const byId = new Map();
  for (const patch of indexPatchDocuments(files, [], false)) {
    const id = patch.metadata.patch_id;
    if (typeof id !== "string") continue;
    if (byId.has(id)) {
      errors.push(
        `Patch ID is duplicated: ${id} (${byId.get(id).file}, ${patch.file}).`,
      );
    } else {
      byId.set(id, patch);
    }
  }
  return byId;
}

function verifyPatchLifecycleSnapshots(previousFiles, currentFiles) {
  const errors = [];
  const previousById = indexLifecyclePatches(previousFiles, errors);
  const currentById = indexLifecyclePatches(currentFiles, errors);
  errors.push(...verifyPatchLifecycle(previousById, currentById));
  return errors;
}

function validateAllPatchLifecycle(currentFiles) {
  const comparisonBase = getAllComparisonBase();
  if (!comparisonBase) return [];

  const errors = [];
  let previousFiles = collectGitPatchSnapshot(comparisonBase);
  const commitOutput =
    gitOptional([
      "rev-list",
      "--reverse",
      "--first-parent",
      `${comparisonBase}..HEAD`,
    ]) ?? "";
  const commits = commitOutput.split(/\r?\n/).filter(Boolean);

  for (const commit of commits) {
    const commitFiles = collectGitPatchSnapshot(commit);
    errors.push(
      ...verifyPatchLifecycleSnapshots(previousFiles, commitFiles).map(
        (error) => `${commit.slice(0, 12)}: ${error}`,
      ),
    );
    previousFiles = commitFiles;
  }

  errors.push(...verifyPatchLifecycleSnapshots(previousFiles, currentFiles));
  return errors;
}

function isPatchContentChanged(previous, current) {
  return (
    !previous ||
    previous.file !== current.file ||
    previous.content !== current.content
  );
}

function getAcceptedTransitionContent(patch) {
  const metadata = { ...patch.metadata };
  for (const key of [
    "status",
    "superseded_by",
    "applied_in_version",
    "applied_by_pr",
  ]) {
    delete metadata[key];
  }
  return JSON.stringify({ body: patch.body, metadata });
}

function verifyPatchLifecycle(previousById, currentById) {
  const errors = [];
  const allIds = new Set([...previousById.keys(), ...currentById.keys()]);

  for (const id of allIds) {
    const previous = previousById.get(id);
    const current = currentById.get(id);
    if (previous && !current) {
      errors.push(
        `Patch audit record must not be deleted: ${id} (${previous.file}).`,
      );
      continue;
    }
    if (!current || !isPatchContentChanged(previous, current)) continue;
    if (!previous) {
      if (!["draft", "proposed"].includes(current.metadata.status)) {
        errors.push(
          `${current.file} new Patch must start in draft or proposed status.`,
        );
      }
      continue;
    }

    const previousStatus = previous.metadata.status;
    const currentStatus = current.metadata.status;
    if (previousStatus === currentStatus) {
      if (!["draft", "proposed"].includes(currentStatus)) {
        errors.push(
          `${current.file} ${currentStatus} Patch is immutable; create a new revision instead.`,
        );
      }
      continue;
    }

    if (!PATCH_ALLOWED_TRANSITIONS.get(previousStatus)?.has(currentStatus)) {
      errors.push(
        `${current.file} disallows Patch status transition ${previousStatus} -> ${currentStatus}.`,
      );
    } else if (previousStatus === "accepted") {
      if (
        path.posix.basename(previous.file) !== path.posix.basename(current.file)
      ) {
        errors.push(
          `${current.file} accepted Patch filename and revision are immutable; only its lifecycle directory may change.`,
        );
      }
      if (
        getAcceptedTransitionContent(previous) !==
        getAcceptedTransitionContent(current)
      ) {
        errors.push(
          `${current.file} accepted Patch content is immutable; only lifecycle metadata may change.`,
        );
      }
    }
  }

  return errors;
}

function isDdlPath(file) {
  return (
    file.startsWith("docs/database/") ||
    file === "docs/DATABASE_SCHEMA_ERD.md" ||
    /(^|\/)(?:ddl|schema)(?:[-_.\/]|$)/i.test(file) ||
    file.endsWith(".sql")
  );
}

function isApiContractPath(file) {
  return (
    /^backend\/src\/main\/java\/.+\/(?:controller|dto)\//.test(file) ||
    file.startsWith("frontend/src/services/")
  );
}

function verifyPatchSnapshot({
  currentFiles,
  previousFiles,
  changedFiles,
  canonicalSpecVersion = null,
  currentDevCommit = null,
  previousCanonicalSpecVersion = null,
  requireBundledAcceptance = false,
  requireBundledApplied = false,
  validateLifecycle = true,
}) {
  const errors = [];
  const warnings = [];

  if (
    currentFiles.size > 0 ||
    previousFiles.size > 0 ||
    [...changedFiles].some((file) => file.startsWith(`${PATCH_ROOT}/`))
  ) {
    for (const requiredPath of [
      PATCH_README_PATH,
      PATCH_TEMPLATE_PATH,
      ...PATCH_KEEP_PATHS,
    ]) {
      if (!currentFiles.has(requiredPath)) {
        errors.push(
          `Required Patch governance file is missing: ${requiredPath}`,
        );
      }
    }
  }

  const currentPatches = indexPatchDocuments(currentFiles, errors, true);
  const previousPatches = indexPatchDocuments(previousFiles, [], false);
  const currentById = new Map();
  const previousById = new Map();

  for (const patch of currentPatches) {
    const id = patch.metadata.patch_id;
    if (typeof id !== "string") continue;
    if (currentById.has(id)) {
      errors.push(
        `Patch ID is duplicated: ${id} (${currentById.get(id).file}, ${patch.file}).`,
      );
    } else {
      currentById.set(id, patch);
    }
  }
  for (const patch of previousPatches) {
    const id = patch.metadata.patch_id;
    if (typeof id === "string" && !previousById.has(id)) {
      previousById.set(id, patch);
    }
  }

  for (const [id, current] of currentById) {
    const supersedes = current.metadata.supersedes;
    if (supersedes === null || !isValidPatchReference(supersedes)) continue;
    const previousRevision = currentById.get(supersedes);
    if (!previousRevision) {
      errors.push(`${current.file} supersedes missing Patch ${supersedes}.`);
    } else if (
      previousRevision.metadata.status !== "superseded" ||
      previousRevision.metadata.superseded_by !== id
    ) {
      errors.push(
        `${current.file} and ${previousRevision.file} must keep bidirectional supersession references.`,
      );
    }
  }

  for (const [id, current] of currentById) {
    if (current.metadata.status !== "superseded") continue;
    const successorId = current.metadata.superseded_by;
    const successor = currentById.get(successorId);
    if (!successor) {
      errors.push(
        `${current.file} superseded_by references missing Patch ${successorId}.`,
      );
    } else if (successor.metadata.supersedes !== id) {
      errors.push(
        `${current.file} and ${successor.file} must keep bidirectional supersession references.`,
      );
    }
  }

  if (validateLifecycle) {
    errors.push(...verifyPatchLifecycle(previousById, currentById));
  }

  const targetOwners = new Map();
  for (const patch of currentPatches) {
    if (!PATCH_ACTIVE_STATUSES.has(patch.metadata.status)) continue;
    if (
      canonicalSpecVersion &&
      patch.metadata.base_spec_version !== canonicalSpecVersion
    ) {
      warnings.push(
        `Active Patch ${patch.metadata.patch_id} uses stale base_spec_version ${patch.metadata.base_spec_version}; current release is ${canonicalSpecVersion}.`,
      );
    }
    if (currentDevCommit && patch.metadata.base_commit !== currentDevCommit) {
      warnings.push(
        `Active Patch ${patch.metadata.patch_id} uses stale base_commit ${patch.metadata.base_commit}; current origin/dev is ${currentDevCommit}.`,
      );
    }
    for (const target of patch.targets) {
      const owners = targetOwners.get(target.key) ?? [];
      owners.push(patch.metadata.patch_id);
      targetOwners.set(target.key, owners);
    }
  }
  for (const [target, owners] of targetOwners) {
    const uniqueOwners = [...new Set(owners)];
    if (uniqueOwners.length > 1) {
      warnings.push(
        `Active Patch target conflict requires Controller review: ${target} (${uniqueOwners.join(", ")}).`,
      );
    }
  }

  const changedPatchPaths = [...changedFiles].filter((file) =>
    PATCH_FILE_PATTERN.test(file),
  );
  const scopeCurrentPatches = currentPatches.filter((patch) =>
    changedFiles.has(patch.file),
  );
  if (changedPatchPaths.length > 0) {
    const mixedApplication = [...changedFiles].filter(
      (file) => file.startsWith("frontend/") || file.startsWith("backend/"),
    );
    const mixedDdl = [...changedFiles].filter(isDdlPath);
    for (const file of new Set(mixedDdl)) {
      errors.push(`Patch change must not include Migration or DDL: ${file}`);
    }

    if (mixedApplication.length > 0) {
      // 코드 혼합은 모든 변경 Patch가 구현 동반형일 때만 허용해 서로 다른 계약의 오염을 막는다.
      const specFirstPatches = scopeCurrentPatches.filter(
        (patch) => patch.deliveryMode !== "implementation_bundled",
      );
      for (const patch of specFirstPatches) {
        errors.push(
          `${patch.file} spec_first Patch must not include application code in the same change.`,
        );
      }

      const bundledPatches = scopeCurrentPatches.filter(
        (patch) => patch.deliveryMode === "implementation_bundled",
      );
      if (requireBundledAcceptance) {
        for (const patch of bundledPatches) {
          if (patch.metadata.status !== "accepted") {
            errors.push(
              `${patch.file} implementation_bundled Patch must be accepted before its application change can merge.`,
            );
          }
        }
      }
      for (const patch of scopeCurrentPatches) {
        if (patch.metadata.status === "applied") {
          errors.push(
            `${patch.file} Controller release must not include application code.`,
          );
        }
      }
    }

    const protectedSpecChanges = [...changedFiles].filter((file) =>
      file.startsWith(`${SPEC_ROOT}/`),
    );
    if (protectedSpecChanges.length > 0) {
      const isControllerRelease =
        scopeCurrentPatches.some(
          (patch) => patch.metadata.status === "applied",
        ) &&
        scopeCurrentPatches.every((patch) =>
          PATCH_ARCHIVED_STATUSES.has(patch.metadata.status),
        );
      if (!isControllerRelease) {
        for (const file of protectedSpecChanges) {
          errors.push(
            `Patch proposal must not include protected spec changes: ${file}`,
          );
        }
      }
    }
  }

  if (changedPatchPaths.length === 0) {
    const apiBoundaryChanges = [...changedFiles].filter(isApiContractPath);
    if (apiBoundaryChanges.length > 0) {
      warnings.push(
        `API boundary change requires a Spec Patch or an explicit N/A rationale in the pull request: ${apiBoundaryChanges.join(", ")}.`,
      );
    }
  }

  if (requireBundledApplied) {
    for (const patch of currentPatches) {
      if (
        patch.deliveryMode === "implementation_bundled" &&
        patch.metadata.status === "accepted"
      ) {
        errors.push(
          `Approved release is blocked by accepted, unapplied implementation_bundled Patch ${patch.metadata.patch_id} (${patch.file}).`,
        );
      }
    }
  }

  for (const current of scopeCurrentPatches) {
    if (current.metadata.status !== "applied") continue;

    if (
      canonicalSpecVersion &&
      current.metadata.applied_in_version !== canonicalSpecVersion
    ) {
      errors.push(
        `${current.file} applied_in_version ${current.metadata.applied_in_version} must match canonical release ${canonicalSpecVersion}.`,
      );
    }
    if (
      canonicalSpecVersion &&
      previousCanonicalSpecVersion &&
      compareSemverVersions(
        canonicalSpecVersion,
        previousCanonicalSpecVersion,
      ) <= 0
    ) {
      errors.push(
        `${current.file} applied transition must advance the canonical release beyond ${previousCanonicalSpecVersion}.`,
      );
    }

    const requiredReleasePaths = new Set([
      SPEC_MANIFEST_PATH,
      `${SPEC_ROOT}/README.md`,
    ]);
    for (const target of current.targets) {
      const specPath = PATCH_TARGET_SPEC_PATHS.get(target.type);
      if (!specPath) {
        errors.push(
          `${current.file} applied target type has no canonical spec mapping: ${target.type}.`,
        );
      } else {
        requiredReleasePaths.add(specPath);
      }
    }
    for (const requiredPath of requiredReleasePaths) {
      if (!changedFiles.has(requiredPath)) {
        errors.push(
          `${current.file} applied transition must atomically update ${requiredPath}.`,
        );
      }
    }
  }

  return { errors, warnings };
}

function getCandidateSpecVersion(mode) {
  try {
    const snapshot =
      mode === "staged"
        ? collectStagedSpecSnapshot()
        : collectWorkingTreeSpecSnapshot();
    return extractSpecReleaseVersion(
      snapshot.files.get(`${SPEC_ROOT}/README.md`),
    );
  } catch {
    return null;
  }
}

function getPreviousCanonicalSpecVersion(mode) {
  const ref = mode === "staged" ? "HEAD" : getAllComparisonBase();
  if (!ref) return null;
  const readme = gitOptional(["show", `${ref}:${SPEC_ROOT}/README.md`]);
  return readme === null ? null : extractSpecReleaseVersion(readme);
}

function getOriginDevCommit() {
  return (
    gitOptional(["rev-parse", "--verify", "refs/remotes/origin/dev"])?.trim() ||
    null
  );
}

function validatePatchGovernance(mode) {
  try {
    const currentFiles =
      mode === "staged"
        ? collectStagedPatchSnapshot()
        : collectWorkingTreePatchSnapshot();
    const result = verifyPatchSnapshot({
      canonicalSpecVersion: getCandidateSpecVersion(mode),
      changedFiles: getChangedFiles(mode),
      currentFiles,
      currentDevCommit: getOriginDevCommit(),
      previousCanonicalSpecVersion: getPreviousCanonicalSpecVersion(mode),
      previousFiles: collectPreviousPatchSnapshot(mode),
      requireBundledAcceptance: mode !== "staged",
      requireBundledApplied: mode === "release",
      validateLifecycle: mode === "staged",
    });
    if (mode !== "staged") {
      result.errors.push(...validateAllPatchLifecycle(currentFiles));
    }
    return result;
  } catch (error) {
    return { errors: [error.message], warnings: [] };
  }
}

function parseMode(args) {
  if (
    args.length !== 1 ||
    !["--staged", "--all", "--release"].includes(args[0])
  ) {
    throw new Error(
      "Use one mode: node scripts/check-project-guardrails.js --staged|--all|--release",
    );
  }

  if (args[0] === "--staged") return "staged";
  if (args[0] === "--release") return "release";
  return "all";
}

function printViolations(violations) {
  console.error("\n프로젝트 기술 제약 위반이 감지되었습니다.\n");
  for (const violation of violations) {
    console.error(`- ${violation.file}: ${violation.rule.name}`);
    console.error(`  ${violation.rule.message}`);
  }
  console.error("\n허용 기술: Vue.js, Spring Framework legacy, MyBatis\n");
}

function printSpecLockErrors(errors) {
  console.error("\nProtected specification lock check failed.\n");
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  console.error(
    "\nProtected specs and their manifest may change only in a scoped administrative spec release.\n",
  );
}

function printPatchGovernanceErrors(errors) {
  console.error("\nSpecification Patch governance check failed.\n");
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  console.error(
    "\nPatch proposals and Controller releases must follow the documented lifecycle and atomic scope.\n",
  );
}

function printPatchGovernanceWarnings(warnings) {
  console.warn("\nSpecification Patch governance review warnings.\n");
  for (const warning of warnings) {
    console.warn(`- ${warning}`);
  }
  console.warn(
    "\nWarnings require Controller review but do not replace semantic approval.\n",
  );
}

function runGuardrails(mode) {
  const entries =
    mode === "staged" ? readStagedEntries() : readWorkingTreeEntries();
  const violations = findViolations(entries);
  const specLockErrors = validateSpecLock(mode);
  const patchGovernance = validatePatchGovernance(mode);

  if (violations.length > 0) {
    printViolations(violations);
  }
  if (specLockErrors.length > 0) {
    printSpecLockErrors(specLockErrors);
  }
  if (patchGovernance.errors.length > 0) {
    printPatchGovernanceErrors(patchGovernance.errors);
  }
  if (patchGovernance.warnings.length > 0) {
    printPatchGovernanceWarnings(patchGovernance.warnings);
  }
  if (
    violations.length > 0 ||
    specLockErrors.length > 0 ||
    patchGovernance.errors.length > 0
  ) {
    return 1;
  }

  const scope =
    mode === "staged"
      ? "staged index content"
      : mode === "release"
        ? "release candidate working tree content"
        : "working tree content";
  console.log(`Project guardrail check passed (${scope}).`);
  return 0;
}

function main() {
  try {
    return runGuardrails(parseMode(process.argv.slice(2)));
  } catch (error) {
    console.error(error.message);
    return 1;
  }
}

if (require.main === module) {
  process.exitCode = main();
}

module.exports = {
  CANONICAL_SPEC_MARKDOWN_PATHS,
  FULL_GIT_COMMIT_PATTERN,
  PATCH_CHANGE_TYPES,
  PATCH_DELIVERY_MODES,
  PATCH_FILE_PATTERN,
  PATCH_ID_PATTERN,
  PATCH_ROOT,
  PATCH_STATUSES,
  SPEC_HASH_ALGORITHM,
  SPEC_MANIFEST_PATH,
  SPEC_MANIFEST_VERSION,
  SPEC_NORMALIZATION,
  SPEC_ROOT,
  collectStagedSpecSnapshot,
  collectWorkingTreeSpecSnapshot,
  extractReadmeReleaseRows,
  extractSpecReleaseVersion,
  findViolations,
  hashNormalizedSpecContent,
  isBackendSourceOrBuild,
  isFrontendSourceOrConfig,
  isIgnoredDocumentation,
  isPackageManifest,
  normalizePath,
  normalizeSpecContent,
  parsePatchDocument,
  parseMode,
  parseSpecManifest,
  splitNullSeparated,
  validatePatchGovernance,
  validateSpecLock,
  verifyPatchSnapshot,
  verifySpecReleaseMetadata,
  verifySpecSnapshot,
};
