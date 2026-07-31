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

function git(args, options = {}) {
  return execFileSync("git", args, {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "inherit"],
    ...options,
  });
}

function normalizePath(file) {
  return file.replace(/\\/g, "/");
}

function comparePaths(left, right) {
  if (left < right) return -1;
  if (left > right) return 1;
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

function verifySpecSnapshot({ files, manifestContent }) {
  const { entries, errors } = parseSpecManifest(manifestContent);
  if (errors.length > 0) {
    return errors;
  }

  const expectedByPath = new Map(
    entries.map((entry) => [entry.path, entry.sha256]),
  );
  const verificationErrors = [];

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

function parseMode(args) {
  if (args.length !== 1 || !["--staged", "--all"].includes(args[0])) {
    throw new Error(
      "Use one mode: node scripts/check-project-guardrails.js --staged|--all",
    );
  }

  return args[0] === "--staged" ? "staged" : "all";
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

function runGuardrails(mode) {
  const entries =
    mode === "staged" ? readStagedEntries() : readWorkingTreeEntries();
  const violations = findViolations(entries);
  const specLockErrors = validateSpecLock(mode);

  if (violations.length > 0) {
    printViolations(violations);
  }
  if (specLockErrors.length > 0) {
    printSpecLockErrors(specLockErrors);
  }
  if (violations.length > 0 || specLockErrors.length > 0) {
    return 1;
  }

  const scope =
    mode === "staged" ? "staged index content" : "working tree content";
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
  SPEC_HASH_ALGORITHM,
  SPEC_MANIFEST_PATH,
  SPEC_MANIFEST_VERSION,
  SPEC_NORMALIZATION,
  SPEC_ROOT,
  collectStagedSpecSnapshot,
  collectWorkingTreeSpecSnapshot,
  findViolations,
  hashNormalizedSpecContent,
  isBackendSourceOrBuild,
  isFrontendSourceOrConfig,
  isIgnoredDocumentation,
  isPackageManifest,
  normalizePath,
  normalizeSpecContent,
  parseMode,
  parseSpecManifest,
  splitNullSeparated,
  validateSpecLock,
  verifySpecSnapshot,
};
